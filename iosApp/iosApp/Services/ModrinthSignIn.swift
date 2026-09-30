import Shared
import UIKit
import WebKit

@MainActor
final class ModrinthSignIn {

    enum Failure: LocalizedError {
        case noWindow
        var errorDescription: String? { "Couldn't show the sign-in page." }
    }

    private weak var presented: UIViewController?

    func run() async throws -> SignedInSession? {
        guard let presenter = Self.topViewController() else { throw Failure.noWindow }

        let token: String? = await withCheckedContinuation { continuation in
            let signIn = SignInWebViewController { token in continuation.resume(returning: token) }
            let navigation = UINavigationController(rootViewController: signIn)
            navigation.presentationController?.delegate = signIn
            presented = navigation
            presenter.present(navigation, animated: true)
        }

        presented?.dismiss(animated: true)
        presented = nil
        guard let token else { return nil }
        return try await LauncherRedirect.shared.verify(token: token)
    }

    func cancel() {
        (presented as? UINavigationController)?.viewControllers
            .compactMap { $0 as? SignInWebViewController }
            .first?
            .finish(with: nil)
    }

    private static func topViewController() -> UIViewController? {
        let window = UIApplication.shared.connectedScenes
            .compactMap { $0 as? UIWindowScene }
            .first { $0.activationState == .foregroundActive }?
            .keyWindow
        var top = window?.rootViewController
        while let next = top?.presentedViewController { top = next }
        return top
    }
}

private final class SignInWebViewController: UIViewController, WKNavigationDelegate, WKUIDelegate,
    UIAdaptivePresentationControllerDelegate {

    private let onFinish: (String?) -> Void
    private var finished = false
    private var webView: WKWebView!
    private var progressObservation: NSKeyValueObservation?
    private var urlObservation: NSKeyValueObservation?
    private let progress = UIProgressView(progressViewStyle: .bar)
    private let titleLabel = UILabel()
    private let failureView = UIStackView()
    private let failureLabel = UILabel()

    /// passkeys don't work without an AASA file, so hide it instead
    private static let hidePasskeyButtonScript = """
    (function () {
      function hide() {
        document.querySelectorAll('button, a').forEach(function (el) {
          if (/continue with passkey/i.test(el.textContent || '')) { el.style.display = 'none'; }
        });
      }
      new MutationObserver(hide).observe(document.documentElement, { childList: true, subtree: true });
      hide();
    })();
    """

    init(onFinish: @escaping (String?) -> Void) {
        self.onFinish = onFinish
        super.init(nibName: nil, bundle: nil)
    }

    required init?(coder: NSCoder) { fatalError("init(coder:) is not used") }

    override func viewDidLoad() {
        super.viewDidLoad()
        view.backgroundColor = .systemBackground
        navigationItem.leftBarButtonItem = UIBarButtonItem(
            systemItem: .cancel,
            primaryAction: UIAction { [weak self] _ in self?.finish(with: nil) }
        )
        navigationItem.titleView = titleLabel
        updateTitle(host: "modrinth.com")

        let configuration = WKWebViewConfiguration()
        configuration.websiteDataStore = .nonPersistent()
        configuration.userContentController.addUserScript(
            WKUserScript(source: Self.hidePasskeyButtonScript, injectionTime: .atDocumentEnd, forMainFrameOnly: true)
        )
        webView = WKWebView(frame: .zero, configuration: configuration)
        webView.navigationDelegate = self
        webView.uiDelegate = self
        webView.translatesAutoresizingMaskIntoConstraints = false

        progress.translatesAutoresizingMaskIntoConstraints = false
        configureFailureView()

        view.addSubview(webView)
        view.addSubview(failureView)
        view.addSubview(progress)

        NSLayoutConstraint.activate([
            webView.topAnchor.constraint(equalTo: view.safeAreaLayoutGuide.topAnchor),
            webView.leadingAnchor.constraint(equalTo: view.leadingAnchor),
            webView.trailingAnchor.constraint(equalTo: view.trailingAnchor),
            webView.bottomAnchor.constraint(equalTo: view.bottomAnchor),
            failureView.centerXAnchor.constraint(equalTo: view.centerXAnchor),
            failureView.centerYAnchor.constraint(equalTo: view.centerYAnchor),
            failureView.leadingAnchor.constraint(greaterThanOrEqualTo: view.leadingAnchor, constant: 32),
            failureView.trailingAnchor.constraint(lessThanOrEqualTo: view.trailingAnchor, constant: -32),
            progress.topAnchor.constraint(equalTo: view.safeAreaLayoutGuide.topAnchor),
            progress.leadingAnchor.constraint(equalTo: view.leadingAnchor),
            progress.trailingAnchor.constraint(equalTo: view.trailingAnchor),
        ])

        progressObservation = webView.observe(\.estimatedProgress) { [weak self] webView, _ in
            MainActor.assumeIsolated {
                self?.progress.progress = Float(webView.estimatedProgress)
                self?.progress.isHidden = webView.estimatedProgress >= 1
            }
        }

        urlObservation = webView.observe(\.url) { [weak self] webView, _ in
            MainActor.assumeIsolated {
                if let host = webView.url?.host { self?.updateTitle(host: host) }
            }
        }

        load()
    }

    private func load() {
        failureView.isHidden = true
        if let url = URL(string: LauncherRedirect.shared.SIGN_IN_URL) {
            webView.load(URLRequest(url: url))
        }
    }

    private func updateTitle(host: String) {
        let text = NSMutableAttributedString()
        let lock = NSTextAttachment(image: UIImage(systemName: "lock.fill")!.withTintColor(.secondaryLabel))
        lock.bounds = CGRect(x: 0, y: -1, width: 11, height: 12)
        text.append(NSAttributedString(attachment: lock))
        text.append(NSAttributedString(
            string: " \(host)",
            attributes: [.font: UIFont.preferredFont(forTextStyle: .subheadline), .foregroundColor: UIColor.label]
        ))
        titleLabel.attributedText = text
        titleLabel.sizeToFit()
    }

    private func configureFailureView() {
        failureView.axis = .vertical
        failureView.alignment = .center
        failureView.spacing = 12
        failureView.isHidden = true
        failureView.translatesAutoresizingMaskIntoConstraints = false

        failureLabel.font = .preferredFont(forTextStyle: .body)
        failureLabel.textColor = .secondaryLabel
        failureLabel.numberOfLines = 0
        failureLabel.textAlignment = .center

        var retry = UIButton.Configuration.filled()
        retry.title = "Try again"
        let button = UIButton(configuration: retry, primaryAction: UIAction { [weak self] _ in self?.load() })

        failureView.addArrangedSubview(failureLabel)
        failureView.addArrangedSubview(button)
    }

    private func showFailure(_ error: Error) {
        let code = (error as NSError).code
        guard code != NSURLErrorCancelled, code != 102 else { return }
        failureLabel.text = "Couldn't load the Modrinth sign-in page. Check your connection and try again."
        failureView.isHidden = false
    }

    func finish(with token: String?) {
        guard !finished else { return }
        finished = true
        webView?.stopLoading()
        onFinish(token)
    }

    func webView(
        _ webView: WKWebView,
        decidePolicyFor navigationAction: WKNavigationAction,
        decisionHandler: @escaping @MainActor (WKNavigationActionPolicy) -> Void
    ) {
        let url = navigationAction.request.url
        if let absolute = url?.absoluteString, let token = LauncherRedirect.shared.parse(url: absolute) {
            decisionHandler(.cancel)
            finish(with: token)
            return
        }
        decisionHandler(.allow)
    }

    func webView(_ webView: WKWebView, didStartProvisionalNavigation navigation: WKNavigation!) {
        failureView.isHidden = true
    }

    func webView(_ webView: WKWebView, didFailProvisionalNavigation navigation: WKNavigation!, withError error: Error) {
        showFailure(error)
    }

    func webView(_ webView: WKWebView, didFail navigation: WKNavigation!, withError error: Error) {
        showFailure(error)
    }

    // sign-in providers sometimes open a new window; keep them in this web view.
    func webView(
        _ webView: WKWebView,
        createWebViewWith configuration: WKWebViewConfiguration,
        for navigationAction: WKNavigationAction,
        windowFeatures: WKWindowFeatures
    ) -> WKWebView? {
        if navigationAction.targetFrame == nil { webView.load(navigationAction.request) }
        return nil
    }

    func presentationControllerDidDismiss(_ presentationController: UIPresentationController) {
        finish(with: nil)
    }
}
