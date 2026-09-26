# Asterion

Asterion is an app for iOS and Android that lets you manage your Modrinth projects, accounts, servers, and more!

## Download

Asterion will be available on the App Store and Play Store.

## Features

- Log in with multiple Modrinth accounts
- View project details, such as description, project type, status, and more
- View project stats, such as downloads, followers, updates, and more
- View and manage payouts and integrate them with your OS's wallet app
- Manage servers and view details such as server logs
- Customize the UI to show exactly what you need and hide what you don't

## Contributing

Please refer to the [contributing guidelines](CONTRIBUTING.md) for more information on contributing to Asterion.

## Project Structure

Asterion is a Kotlin Multiplatform project targeting Android and iOS.

* [/iosApp](./iosApp/iosApp) contains an iOS application. This is where Asterion's SwiftUI code is stored.

* [/shared](./shared/src) is for code that will be shared across the Android and iOS apps. It contains
  several subfolders:
    - [commonMain](./shared/src/commonMain/kotlin) is for code that’s common for all targets.
    - Other folders are for Kotlin code that will be compiled for only the platform indicated in the folder name. For
      example, if you want to use Apple’s CoreCrypto for the iOS part of your Kotlin app,
      the [iosMain](./shared/src/iosMain/kotlin) folder would be the right place for such calls. Similarly, if you want
      to edit the Desktop (JVM) specific part, the [jvmMain](./shared/src/jvmMain/kotlin)
      folder is the appropriate location.

## License and Privacy Policy

Asterion is licensed under the GNU General Public License Version 3. Please refer to the [license](LICENSE) for more information.

Asterion does not collect any of your data or maintain a backend of its own; however, it requires at least one Modrinth account to function in accordance to [Modrinth's Privacy Policy](https://modrinth.com/legal/privacy). Please refer to [Asterion's Privacy Policy](PRIVACY.md) for more information.

## Credits

### Dependencies
- [Citadel](https://github.com/orlandos-nl/Citadel): An SSH Client & Server framework in Swift

### Acknowledgments
- Built with [Kotlin Multiplatform](https://kotlinlang.org/multiplatform/)
- Made possible thanks to the [Modrinth API](https://docs.modrinth.com/api/)
- Special thanks to **samalando1034** on Discord for providing his Modrinth server for testing