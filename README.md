# AuraFit - Futuristic Gym & Nutrition AI Companion

AuraFit is a premium, offline-first, state-of-the-art workout and nutrition tracking application. Featuring a modern, high-contrast dark visual aesthetic centered around a **glowing neon-blue theme**, AuraFit delivers a fluid, responsive, and distraction-free tracking environment.

The app combines traditional local workout/body metric logging with advanced **Gemini-powered artificial intelligence** to offer an automated nutrition tracker and an intelligent gym coach.

---

## 🌟 Key Features

### 🏋️‍♂️ Dynamic Workout Tracker
- **Workout Diary**: Create, modify, and start workout routines.
- **Active Workout Screen**: Complete with a live timer, interactive set logging, previous weight/rep context, and animated checkmarks.
- **Exercise Database**: Fully searchable and category-filtered directory of exercises.

### 📊 Metric & Body Tracker
- **Body Measurements**: Track body weight, chest, waist, biceps, and hip dimensions.
- **Progress History**: Visually follow physical progress over time with clean lists and local timestamps.

### 🥗 Aura Nutrition Tracker & AI Calorie Bot (New!)
- **Daily Food Diary**: Track calories and macronutrients (Protein, Carbs, Fats) against customized dynamic daily goals.
- **AuraFit Nutrition AI Bot**: Talk to a personal assistant. Tell it what you ate (e.g., *"I had 3 scrambled eggs, a cup of white rice, and 150g of cooked chicken breast for lunch"*), and the AI will estimate the names, calories, and macronutrient profile and log them automatically into your Room database.
- **Quick Manual Add**: Manually log food items with exact calorie and macro inputs.
- **Personalized Targets**: Edit and persist your customized daily caloric and macronutrient targets.

### 🤖 Aura Gym Coach
- Ask AI coach for tailored workout advice, form corrections, plateaus, and overall suggestions based on your personal workout history.

### 🔒 Privacy & Local-First Architecture
- Powering local state with a robust **SQLite Room Database** for fast, reliable, offline-first storage.
- Safe integration of the **Gemini API** via server-side secure proxies or client-side `BuildConfig` injections.

---

## 🚀 How to Run the App in Google AI Studio

### 1. Web Streaming Android Emulator
The Google AI Studio platform compiles your application instantly. Any code changes made will trigger an incremental rebuild.
- Once the build is successful, look at the **Streaming Android Emulator** on the right side of your browser screen.
- You can interact with the virtual device directly, log workouts, start active sessions, and navigate through the new **Nutrition** tab using the bottom navigation bar.

### 2. Configure the Gemini API Key
To unleash the power of the **AI Coach** and the **AI Calorie Bot**, you must provide a valid Gemini API key.
1. Locate the **Secrets panel** in the AI Studio sidebar.
2. Add a new secret with the name:
   `GEMINI_API_KEY`
3. Paste your Gemini API key (you can obtain one for free from Google AI Studio).
4. The application reads this key safely through `BuildConfig.GEMINI_API_KEY`, protecting your secrets from being hardcoded in code repositories.

---

## 📦 How to Generate and Download the APK

### Method A: Directly from Google AI Studio (Recommended)
You can easily build, sign, and download your production or debug APK directly through the browser platform:
1. Look at the top right header menu or sidebar settings in **Google AI Studio**.
2. Click on the **Download / Export** icon or open **Settings**.
3. Select **Generate APK / AAB**.
4. The system will trigger an optimized Gradle assemble task on the cloud environment.
5. Once complete, a prompt will appear letting you download the `.apk` file directly to your desktop or mobile device for manual installation.

### Method B: Build the APK inside this environment via Gradle
If you would like to pre-compile the APK within the terminal workspace:
- The system automatically triggers `compile_applet` which builds the debug APK.
- The compiled output is stored in the `.build-outputs/` directory or standard gradle output paths like `app/build/outputs/apk/debug/app-debug.apk`.

---

## 💻 Local Desktop Setup & Manual Run

If you want to download the source code as a ZIP and run it locally on your computer:

### Prerequisites
- **Java Development Kit (JDK 17)** or higher.
- A physical Android device with USB Debugging enabled, or an Android Virtual Device (AVD).

---

## 💻 Running the App in VS Code (Visual Studio Code)

While Android Studio is the default IDE for Android, you can perfectly develop, build, and run this Kotlin / Jetpack Compose project using **VS Code**!

### 1. VS Code Extension Setup
To get syntax highlighting, code completion, and direct build integration:
1. Open VS Code.
2. Go to the Extensions Marketplace (`Ctrl+Shift+X` or `Cmd+Shift+X`).
3. Install the following extensions:
   - **Kotlin** (by fwcd) — Provides Kotlin language support.
   - **Extension Pack for Java** (by Microsoft) — Helpful for configuring the underlying JVM/JDK.
   - **Gradle for Java** (by Microsoft) — Provides excellent tasks outline and build integration.

### 2. Configure JDK 17+ in VS Code
Ensure VS Code is using the correct JDK version for your project:
1. Open settings (`Ctrl+,` or `Cmd+,`).
2. Search for `java.jdt.ls.java.home` and set it to your local JDK 17 install directory.

### 3. Open and Build the App
1. Open the project root folder in VS Code (`File -> Open Folder...`).
2. Open a VS Code terminal (`Ctrl+` ` or `Cmd+` `).
3. Ensure you have the Gradle environment working by running:
   - **Windows PowerShell**: `.\gradlew tasks`
   - **Mac/Linux Terminal**: `./gradlew tasks`
4. Create your local environment secrets file named `.env` in the root folder of the project:
   ```env
   GEMINI_API_KEY=AIzaSyYourActualAPIKeyHere...
   ```

### 4. Deploy and Run from VS Code
- **With a connected physical device or emulator running**, execute the following command in the VS Code terminal to compile and install the application:
  ```bash
  # For Mac / Linux
  ./gradlew installDebug
  
  # For Windows (PowerShell)
  .\gradlew installDebug
  ```
- This will automatically build the debug APK, upload it to your connected device/emulator, and install it.

---

## 📱 Installing the App on a Physical Android Device

### Step 1: Enable Developer Options & USB Debugging on your phone
1. On your Android phone, go to **Settings** -> **About Phone**.
2. Tap **Build Number** 7 times consecutively until you see the toast message *"You are now a developer!"*.
3. Go back to the main Settings menu, search for **Developer Options**, and open it.
4. Turn on **USB Debugging**.

### Step 2: Install via USB Connection
1. Connect your Android phone to your computer using a USB cable.
2. A prompt will appear on your phone screen asking to allow USB Debugging from this computer. Tap **Allow** (or *Always Allow*).
3. Verify that your computer recognizes the device. Open a terminal and run (if you have the Android SDK installed):
   ```bash
   adb devices
   ```
4. Build and install directly from VS Code or your terminal:
   ```bash
   # In your project root terminal (VS Code or standard command line)
   ./gradlew installDebug
   ```

### Step 3: Install via Direct APK Download (No Computer Cable Required)
If you generated the APK using Google AI Studio's **Generate APK** option:
1. Download the generated `.apk` file onto your computer or directly on your phone.
2. If downloaded on a computer, transfer the file to your phone (via Google Drive, WhatsApp, email, or a USB transfer).
3. Open a File Manager app on your Android phone.
4. Locate the downloaded `.apk` file and tap on it.
5. If prompted with *"For your security, your phone is not allowed to install unknown apps from this source"*, tap **Settings** and toggle **Allow from this source** to ON.
6. Tap **Install** and then **Open** to enjoy AuraFit!

---

## 🛠️ Tech Stack & Architecture

- **UI Framework**: 100% Jetpack Compose (Kotlin) styled on Material Design 3 guidelines.
- **Database**: Android Jetpack Room with SQLite, utilizing custom DAO queries for workout logs, measurements, and food entries.
- **Serialization**: Kotlinx Serialization for type-safe parameters.
- **JSON Processing**: Moshi for resilient AI raw JSON response deserialization.
- **Networking**: OkHttp & Retrofit for seamless Gemini REST calls.
- **Multithreading**: Kotlin Coroutines and asynchronous StateFlows for fluid state propagation and lag-free UI experiences.
