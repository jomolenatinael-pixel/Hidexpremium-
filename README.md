<div align="center">
<img width="1200" height="475" alt="Hidexpremium App Banner" src="https://ai.google.dev/static/site-assets/images/share-ais-513315318.png" />
</div>

# Hidexpremium - AI Studio App

A powerful Android application built with Kotlin, powered by Google's Gemini AI. This app brings advanced AI capabilities to your mobile device.

**Description:** New app  
**Repository:** [jomolenatinael-pixel/Hidexpremium-](https://github.com/jomolenatinael-pixel/Hidexpremium-)  
**Language:** Kotlin  
**License:** Open Source

---

## 📱 Download APK

The latest APK builds are available for download in the [Releases](https://github.com/jomolenatinael-pixel/Hidexpremium-/releases) section.

### Installation Instructions:
1. Download the latest APK file from the Releases page
2. On your Android device, enable "Install from Unknown Sources" in Settings
3. Open the APK file and follow the installation prompts
4. Launch the app and enjoy!

**Minimum Requirements:**
- Android 7.0 (API 24) or higher
- At least 100MB of free storage
- Internet connection for AI features

---

## 🚀 Features

- 🤖 Powered by Google Gemini AI
- ⚡ Fast and responsive interface
- 🔐 Secure API integration
- 🎨 Modern Android UI design
- 📱 Optimized for mobile devices

---

## 🛠️ Development

### Run Locally

**Prerequisites:** [Android Studio](https://developer.android.com/studio)

1. Open Android Studio
2. Select **Open** and choose the directory containing this project
3. Allow Android Studio to fix any incompatibilities as it imports the project
4. Create a file named `.env` in the project directory and set `GEMINI_API_KEY` to your Gemini API key (see `.env.example` for an example)
5. Remove this line from the app's `build.gradle.kts` file: `signingConfig = signingConfigs.getByName("debugConfig")`
6. Run the app on an emulator or physical device

### Building a Release APK

To build your own APK:

```bash
# Using Android Studio
1. Go to Build → Build Bundle(s) / APK(s) → Build APK(s)
2. The APK will be generated in app/release/

# Using Gradle
./gradlew assembleRelease
```

---

## 📋 Project Structure

- **app/** - Main application code
- **build.gradle.kts** - Build configuration
- **.env.example** - Environment variable template
- **README.md** - This file

---

## 🔧 Configuration

### Environment Variables

Create a `.env` file in your project root:

```env
GEMINI_API_KEY=your_api_key_here
```

Get your Gemini API key from [Google AI Studio](https://ai.google.dev/tutorials/setup)

---

## 📝 Version History

See [Releases](https://github.com/jomolenatinael-pixel/Hidexpremium-/releases) for detailed version history and changelog.

---

## 👤 Author

**jomolenatinael-pixel**
- GitHub: [@jomolenatinael-pixel](https://github.com/jomolenatinael-pixel)

---

## 📄 License

This project is open source and available under an open license.

---

## 🤝 Support

For issues, questions, or feature requests, please open an [Issue](https://github.com/jomolenatinael-pixel/Hidexpremium-/issues) on GitHub.

---

## 🔗 Useful Links

- [Google Gemini API Documentation](https://ai.google.dev/)
- [Android Developer Guide](https://developer.android.com/)
- [Kotlin Documentation](https://kotlinlang.org/)
- [View your app in AI Studio](https://ai.studio/apps/a5a6987c-7fff-42ab-bce4-b921d235c383)

---

**Last Updated:** July 12, 2026
