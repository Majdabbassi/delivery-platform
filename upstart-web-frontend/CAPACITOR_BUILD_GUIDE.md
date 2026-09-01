# Upstart Capacitor - Build & Deployment Guide

## Prerequisites
- Node.js 18+
- Android Studio (for Android builds)
- Xcode 14+ (for iOS builds)
- git
- npm 9+

## Step 1: Build Angular for Production

```bash
cd C:\Users\majda\OneDrive\Desktop\majd\delivary\upstart-web-frontend

# Build optimized Angular app
ng build --configuration production

# Output: dist/del/ folder with optimized web assets
```

This creates the production build that will be wrapped by Capacitor.

## Step 2: Sync Web Assets to Capacitor

```bash
# Copy web assets to both iOS and Android projects
npx cap sync

# Or sync specific platform:
npx cap sync android
npx cap sync ios
```

## Step 3: Add Native Platforms

### Android
```bash
# Add Android native project
npx cap add android

# This creates:
# - android/ folder with full Android Studio project
# - capacitor.settings.gradle
# - Android manifests configured

# Open in Android Studio
npx cap open android

# OR open manually:
# File → Open → upstart-web-frontend/android/
```

### iOS
```bash
# Add iOS native project  
npx cap add ios

# This creates:
# - ios/ folder with full Xcode project
# - iOS build configuration
# - Pods dependencies

# Open in Xcode
npx cap open ios

# OR open manually:
# File → Open → upstart-web-frontend/ios/App/App.xcworkspace
```

## Step 4: Build Android APK/AAB

### Option A: Android Studio GUI (Recommended for first-time)

1. Open `npx cap open android`
2. In Android Studio menu:
   - **For Testing (APK)**: Build → Build Bundle(s) / APK(s) → Build APK(s)
   - **For Release (AAB)**: Build → Generate Signed Bundle / APK → Bundle
3. Select build variant (release)
4. Provide signing key (or create new keystore)
5. Click "Build"
6. APK appears in: `android/app/build/outputs/apk/release/`

### Option B: Command Line

```bash
cd upstart-web-frontend/android

# Build APK (for testing)
./gradlew assembleRelease

# Build AAB (for Play Store)
./gradlew bundleRelease

# Output locations:
# APK: app/build/outputs/apk/release/app-release.apk
# AAB: app/build/outputs/bundle/release/app-release.aab
```

### Debug APK (Development Testing)

```bash
# In Android Studio or:
./gradlew assembleDebug

# Output: app/build/outputs/apk/debug/app-debug.apk
# Install on connected device:
./gradlew installDebug
```

## Step 5: Build iOS IPA

### Option A: Xcode GUI (Recommended)

1. Open `npx cap open ios`
2. Open in Xcode: select "App.xcworkspace" (NOT App.xcodeproj)
3. Select scheme: "App" (top left dropdown)
4. Select device: Generic iOS Device (for release)
5. Product menu:
   - **Testing**: Product → Build
   - **Archive**: Product → Archive
6. For App Store:
   - Window → Organizer
   - Select app version
   - Click "Distribute App"
   - Choose "App Store Connect"
7. IPA is created at: `~/Library/Developer/Xcode/DerivedData/App.../Products/Release-iphoneos/App.app`

### Option B: Command Line (Xcode)

```bash
cd upstart-web-frontend/ios/App

# Build for device
xcodebuild -workspace App.xcworkspace \
  -scheme App \
  -configuration Release \
  -derivedDataPath build

# Create IPA
xcodebuild -exportArchive \
  -archivePath build/App.xcarchive \
  -exportPath ./build/release \
  -exportOptionsPlist ExportOptions.plist
```

## Step 6: Install & Test on Physical Devices

### Android
```bash
# With device connected via USB (USB debugging enabled):
adb install -r app-release.apk

# Or use Android Studio: Run → Run 'app' → select device
```

### iOS
```bash
# With device connected:
# Option 1: Use Xcode (Product → Run)
# Option 2: Use ios-deploy via npm

npm install -g ios-deploy
ios-deploy -b path/to/App.ipa --id DEVICE_UDID
```

## Step 7: Create Keystore for Signing (Android)

```bash
# Generate keystore (one-time)
keytool -genkey -v -keystore upstart-delivery-key.keystore \
  -alias upstart \
  -keyalg RSA \
  -keysize 2048 \
  -validity 10000

# Prompts for:
# - Password (remember this!)
# - Name
# - Organization
# - Country

# Move to safe location:
move upstart-delivery-key.keystore android/app/

# In Android Studio build dialog, select this keystore
# Or configure in build.gradle
```

## Step 8: Play Store Upload (Android)

1. Create Google Play Developer account ($25 one-time fee)
2. In Android Studio:
   - Build → Generate Signed Bundle / APK
   - Select "Bundle" type
   - Sign with your keystore
   - Upload to Google Play Console
3. Or upload AAB directly to Play Console:
   - https://play.google.com/console
   - Create app listing
   - Upload AAB
   - Set store listing, screenshots, pricing
   - Submit for review

## Step 9: App Store Upload (iOS)

1. Create Apple Developer account ($99/year)
2. In Xcode:
   - Product → Archive
   - Window → Organizer
   - Distribute App → App Store Connect
   - Follow prompts to upload
3. Or use Transporter app:
   - Download from Mac App Store
   - Sign in with Apple ID
   - Select IPA file
   - Click Deliver

## Testing Checklist

- [ ] **Responsive Layout**
  - Test on 6-inch phone (390x844)
  - Test on 5-inch phone (375x667)
  - Test on tablet (768x1024)
  - Landscape orientation
  - All 19 pages load correctly

- [ ] **Native Features**
  - Location tracking (grant permission, see live updates)
  - Camera (take photo, select from gallery)
  - Notifications (push and local)
  - Offline storage (add item offline, see when online)

- [ ] **Authentication**
  - Login works
  - Token refresh works
  - Logout works
  - Role-based access works

- [ ] **Real-time Updates**
  - WebSocket connects
  - Receive order updates
  - Location updates stream
  - Notifications received

- [ ] **Performance**
  - App launches < 3 seconds
  - Pages load < 2 seconds
  - No memory leaks (test after 30 mins)
  - Smooth scrolling

- [ ] **Permissions**
  - App requests location on first use
  - App requests camera on first use
  - App requests notification permission
  - All permissions handled gracefully

## Common Issues & Fixes

### "Failed to resolve @capacitor/core"
```bash
npm install --legacy-peer-deps
```

### Android build fails: "Could not get unknown property 'keyAlias'"
- Ensure keystore path is correct in build.gradle
- Or sign APK/AAB in Android Studio GUI instead

### iOS build fails: Pod install error
```bash
cd ios/App
pod install --repo-update
cd ../..
npx cap sync ios
```

### App crashes on launch
- Check browser console for errors: `ng serve`
- Ensure capacitor.config.ts is correct
- Check Android/iOS native logs

### Push notifications not received
- Verify FCM configuration
- Check device token registration with backend
- Ensure backend sends to correct token

### Location not updating
- Grant app location permission in settings
- Test on actual device (simulator may not have GPS)
- Check startTracking() is called in component

## File Structure After Build

```
upstart-web-frontend/
├── dist/                       # Production build output
│   ├── index.html
│   ├── styles.*.css
│   ├── main.*.js
│   └── ...
├── android/                    # Android project
│   ├── app/
│   │   └── build/
│   │       └── outputs/
│   │           └── apk/release/app-release.apk
│   └── build.gradle
├── ios/                        # iOS project
│   ├── App/
│   │   └── App.xcworkspace
│   └── Pods/
├── capacitor.config.ts
└── package.json
```

## Quick Commands Reference

```bash
# Development workflow
ng build --watch
npx cap sync
npx cap open android
npx cap open ios

# Production build
ng build --configuration production
npx cap sync

# Android
cd android && ./gradlew assembleRelease

# iOS  
cd ios/App && xcodebuild -workspace App.xcworkspace -scheme App -configuration Release

# Run on device
adb install app-release.apk          # Android
npx cap run ios --device             # iOS
```

## Support & Troubleshooting

- Capacitor Docs: https://capacitorjs.com
- Android Studio Docs: https://developer.android.com/studio
- Xcode Docs: https://developer.apple.com/xcode/
- Play Store Help: https://support.google.com/googleplay
- App Store Help: https://developer.apple.com/support/
