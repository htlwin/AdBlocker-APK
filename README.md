# 🛡️ Ad Blocker APK

A free and open-source ad blocker for Android devices. Uses local VPN to block ads, trackers, and analytics without requiring root access.

## Features

- 🚫 **Block Ads** - Blocks ads in apps and browsers
- 🔍 **Block Trackers** - Prevents analytics and tracking
- 📊 **Statistics** - Track number of ads blocked
- 🔋 **Battery Saver** - Saves battery by blocking ad loading
- 💾 **Data Saver** - Reduces data usage
- 🔒 **No Root** - Works without root access
- 🌙 **Dark Mode** - Follows system theme

## How It Works

Ad Blocker creates a local VPN connection on your device. All network traffic is routed through this VPN, which filters out requests to known ad servers, trackers, and analytics services.

**No data is sent to external servers** - everything runs locally on your device.

## Installation

### Option 1: Build from Source

```bash
# Clone the repository
git clone https://github.com/htlwin/AdBlocker-APK.git
cd AdBlocker-APK

# Build with Gradle
./gradlew assembleRelease

# APK will be in app/build/outputs/apk/release/
```

### Option 2: Download Pre-built APK

Download the latest release from the [Releases](https://github.com/htlwin/AdBlocker-APK/releases) page.

## Usage

1. Open the Ad Blocker app
2. Tap **START** to begin blocking ads
3. Grant VPN permission when prompted
4. Ads will now be blocked system-wide

## Blocked Services

The app blocks requests to:

- Google Ads (AdMob, AdSense, DoubleClick)
- Facebook Ads & Analytics
- Twitter Ads
- Yahoo Ads
- Analytics services (Google Analytics, Firebase Analytics, etc.)
- Common ad networks (Unity Ads, Vungle, Chartboost, etc.)

## Permissions

- **INTERNET** - Required for VPN functionality
- **ACCESS_NETWORK_STATE** - Check network connectivity
- **FOREGROUND_SERVICE** - Run VPN service in background
- **RECEIVE_BOOT_COMPLETED** - Start on device boot (optional)

## Privacy

- No data collection
- No external servers
- All filtering done locally
- Open source - auditable code

## Requirements

- Android 7.0 (API 24) or higher
- VPN permission (granted at runtime)

## Building

```bash
# Debug build
./gradlew assembleDebug

# Release build
./gradlew assembleRelease
```

## Tech Stack

- **Language**: Java
- **Min SDK**: API 24 (Android 7.0)
- **Target SDK**: API 34 (Android 14)
- **UI**: Material Design Components

## License

MIT License - See [LICENSE](LICENSE) file for details.

## Disclaimer

This app is provided for educational purposes. Use responsibly and respect website/app terms of service.

## Support

- Issues: [GitHub Issues](https://github.com/htlwin/AdBlocker-APK/issues)
- Discussions: [GitHub Discussions](https://github.com/htlwin/AdBlocker-APK/discussions)
