# Privacy Policy

**Effective date: September 25, 2026**

This Privacy Policy explains how Asterion ("Asterion," "the app," "we," "us") handles information when you use the app. Asterion is free, open-source software, and its complete source code is publicly available at [https://github.com/tomatopotato17265/Asterion](https://github.com/tomatopotato17265/Asterion) for anyone to inspect and verify the practices described below.

## Summary

Asterion does not collect, store, transmit, or sell any personal data. There are no analytics, advertising, or crash-reporting services built into the app. The only data Asterion handles is the data required to sign you in to your Modrinth account and to display your own Modrinth data back to you, and that data is exchanged directly between your device and Modrinth, not with us.

## Information We Do Not Collect

Asterion does not collect or transmit to us:

- Your name, email address, or other contact information
- Usage analytics, behavioral data, or advertising identifiers
- Crash reports or diagnostic telemetry
- Any data about your device beyond what is technically necessary to complete a network request (see "Third-Party Infrastructure" below)

We do not operate any database, analytics platform, or advertising network, and Asterion contains no third-party analytics or advertising software development kits (SDKs).

## Modrinth Account and Data

Asterion requires you to sign in with at least one Modrinth account to use the app. Signing in and all subsequent use of the app (viewing your projects, managing servers, reading notifications, and similar features) involves Asterion communicating directly with Modrinth's own API on your behalf, using the authorization you grant during sign-in.

This means:

- Your Modrinth account data (profile information, projects, servers, notifications, payouts, and any other data Modrinth's API exposes to an authorized client) is retrieved directly from Modrinth's servers to your device. Asterion does not have its own copy of this data, and does not store it on any server we operate.
- Modrinth's own collection, use, and handling of your account data is governed entirely by [Modrinth's Privacy Policy](https://modrinth.com/legal/privacy). We encourage you to read it, as Asterion has no control over, and no visibility into, how Modrinth processes your data on its own systems.

## Data Stored On Your Device

After you sign in, Asterion stores your Modrinth access token locally on your device, using your platform's secure storage (the system Keychain on iOS, and encrypted storage on Android). This token is used solely to authenticate your requests to Modrinth's API on your behalf.

This data:

- Never leaves your device except when sent directly to Modrinth's API (or relayed once through our token-exchange endpoint during initial sign-in, as described above).
- Is under your control at all times. You can remove it by signing out of your account(s) within the app, or by uninstalling the app.

## Children's Privacy

Asterion is a client for Modrinth and is not directed at children. Your ability to use Asterion is subject to Modrinth's own eligibility requirements for using its service, as described in Modrinth's Privacy Policy and Terms of Service.

## Changes to This Policy

If this Privacy Policy changes, the updated version will be published in this same location in the project's repository, with a revised effective date at the top of the document. Because Asterion is open source, you can also review the full history of changes to this policy at any time in the repository's commit history.

## Contact

If you have questions about this Privacy Policy, please open an [issue](https://github.com/tomatopotato17265/Asterion/issues/new/choose) in this repository.
