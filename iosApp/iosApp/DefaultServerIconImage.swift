import Shared
import UIKit

extension UIImage {
    static let defaultServerIcon: UIImage = {
        guard let data = Data(base64Encoded: Shared.DefaultServerIcon.shared.base64),
              let image = UIImage(data: data)
        else {
            fatalError("Embedded default server icon failed to decode")
        }
        return image
    }()
}
