# Asterion Contribution Guidelines

Thank you for wanting to contribute to Asterion! This app is fully open-source, so your contributions benefit everyone using this app.

If you would like to suggest a feature or report a bug without contributing code, please do so in the [Issues](https://github.com/tomatopotato17265/Asterion/issues) tab.

## Prerequisites

- The [latest stable IntelliJ IDEA](https://www.jetbrains.com/idea/) with the [Kotlin Multiplatform](https://plugins.jetbrains.com/plugin/14936-kotlin-multiplatform?_cl=Mjt7ImFsbEFsbG93ZWQiOnRydWUsImFsbG93U2FsZSI6ZmFsc2UsInJlZ2lvbiI6IlVTIiwiYW5zd2VyZWQiOnRydWUsInRpbWVzdGFtcCI6IjIwMjYtMDktMjRUMjE6Mzg6MzQuODE2WiJ9&_gl=1%2A11yxy0m%2A_gcl_au%2AMjM2NzYzMjA4LjE3OTAyODU5MTU.%2A_ga%2AMjMyOTE4Nzk4LjE3OTAyODU5MTM.%2A_ga_9J976DJZ68%2AczE3OTAzNzU3MzEkbzIkZzEkdDE3OTAzNzU3NTEkajQwJGwwJGgw) and the [Android](https://plugins.jetbrains.com/plugin/22989-android) plugins installed
  - You can use another IDE such as Android Studio, but Asterion was fully built from IntelliJ IDEA and we cannot assist with any issues you may have in another IDE
- [JDK 21](https://www.oracle.com/java/technologies/downloads/#java21) for your respective architecture and operating system
- [Android SDK Platform 36](https://developer.android.com/tools/sdkmanager), installable through IntelliJ IDEA's SDK Manager once the Android plugin above is installed

[Xcode 26 or later](https://developer.apple.com/xcode/) is also required if you want to contribute to the [iOS app](./iosApp).


## Setting up a local repository

Begin by cloning this repository in a directory of your choice:

```
git clone https://github.com/tomatopotato17265/Asterion
```

Then, open it in your IDE. Wait for the project to import and configure.

## Running the apps and tests

Use the run configurations provided by the run widget in your IDE's toolbar, or use the run button in your IDE's editor gutter for individual tests. Alternatively, you can also use these commands:

- Android app: `./gradlew :androidApp:assembleDebug`
- Android tests: `./gradlew :shared:testAndroidHostTest`
- iOS app: open the [/iosApp](./iosApp) directory in Xcode and run it from there.
- iOS tests: `./gradlew :shared:iosSimulatorArm64Test`

## Opening a PR

Once you are ready to share your changes, [fork this repository](https://github.com/tomatopotato17265/Asterion/fork) and open a [Pull Request](https://github.com/tomatopotato17265/Asterion/pulls). Thank you for contributing to Asterion!
