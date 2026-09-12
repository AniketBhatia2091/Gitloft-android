# Gitloft Android 🚀

Gitloft is the developer portfolio & hiring platform built for modern engineers and technical recruiters. Curate your GitHub achievements, spotlight your best repositories, generate shareable showcases, and connect directly with hiring managers.

---

## ✨ Features

- **Cyberpunk / Minimal Dark UI**: Built with 100% Jetpack Compose following high-contrast cyber-aesthetic design system (`#0A0A0A`, `#CCFF00` Volt green, `#00E5FF` Cyan).
- **Curator & Recruiter Roles**:
  - **Developer Mode**: Spotlight pinned repositories, inspect Language DNA statistics, customize developer bio/headline, and publish live showcases to `gitloft.app/u/{username}`.
  - **Hiring / Talent Acquisition Mode**: Discover developers, filter by programming languages, inspect live project READMEs & code metrics, and manage talent shortlists.
  - **Instant Role Switching**: Toggle between Developer and Recruiter personas anytime via Settings.
- **GitHub Integration**:
  - GitHub OAuth 2.0 Web flow with deep link interception (`gitloft://oauth-callback` & `https://gitloft.vercel.app/oauth-callback`).
  - Personal Access Token (PAT) authentication (`ghp_...` & `github_pat_...`) with real-time token validation.
- **Cloud Showcase Sync**:
  - Supabase backend persistence for profiles, pinned repositories, language stats, and showcase status.
  - Built-in analytics event logging for profile impressions and showcase views.
- **Deep Linking**:
  - Universal link support for `gitloft://u/{username}` and `https://gitloft.vercel.app/u/{username}` to open candidate profiles directly in the app.

---

## 🛠 Tech Stack & Architecture

- **Language**: Kotlin 1.9
- **UI Framework**: Jetpack Compose + Material 3
- **Architecture**: Clean Architecture with MVVM, StateFlow, and Coroutines
- **Networking**: Retrofit 2 + kotlinx.serialization
- **Data Persistence**: Android DataStore + EncryptedSharedPreferences (Security Crypto)
- **Image Loading**: Coil Compose
- **Custom Tabs**: AndroidX Browser Custom Tabs for secure OAuth authentication
- **Barcode / QR**: ZXing Android Embedded for portfolio sharing

---

## 📂 Project Structure

```
gitloft-android/
├── app/
│   ├── src/
│   │   ├── main/
│   │   │   ├── AndroidManifest.xml
│   │   │   ├── kotlin/com/example/gitloftandroid/
│   │   │   │   ├── MainActivity.kt
│   │   │   │   ├── data/
│   │   │   │   │   ├── model/         # Profile, Repository, Language models
│   │   │   │   │   ├── network/       # GitHub API & Supabase REST client
│   │   │   │   │   └── repository/    # Local DataStore & GitHub Repo
│   │   │   │   ├── ui/
│   │   │   │   │   ├── analytics/     # Analytics & profile statistics
│   │   │   │   │   ├── auth/          # Authentication sheets & OAuth dialogs
│   │   │   │   │   ├── components/    # Cyberpunk design system components
│   │   │   │   │   ├── curator/       # Developer showcase curation dashboard
│   │   │   │   │   ├── hiring/        # Recruiter talent discovery dashboard
│   │   │   │   │   ├── onboarding/    # Role selection & welcome screens
│   │   │   │   │   ├── share/         # Portfolio share & QR code generation
│   │   │   │   │   ├── showcase/      # Candidate profile & pinned work view
│   │   │   │   │   ├── theme/         # Color palettes, typography & themes
│   │   │   │   │   └── viewmodel/     # SessionViewModel & CuratorViewModel
│   │   │   │   └── util/              # TokenStorage & InputValidator
│   │   │   └── res/                   # Drawables, mipmaps, and XML configs
│   │   └── test/                      # Unit tests (InputValidator, etc.)
│   └── build.gradle
├── gradle/
├── build.gradle
└── settings.gradle
```

---

## 🚀 Getting Started

### Prerequisites
- Android Studio Iguana / Jellyfish or newer
- JDK 17
- Android SDK 34

### Building & Running
1. Clone the repository:
   ```bash
   git clone https://github.com/AniketBhatia2091/Gitloft-android.git
   cd Gitloft-android
   ```
2. Build the debug APK:
   ```bash
   ./gradlew assembleDebug
   ```
3. Install on a connected Android device or emulator:
   ```bash
   adb install -r app/build/outputs/apk/debug/app-debug.apk
   ```

---

## 🔒 Security & Privacy

- No sensitive personal access tokens, client secrets, or private keys are stored in version control.
- GitHub tokens and session JWTs are stored locally using Android Jetpack `EncryptedSharedPreferences` backed by the Android Keystore.
- Strict input validation prevents unauthenticated identity impersonation.

---

## 📄 License

This project is licensed under the MIT License.
