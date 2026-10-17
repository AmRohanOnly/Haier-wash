# 🧺 OpenWasher: Smart Laundry & Appliance Companion

OpenWasher is an open-source, community-oriented companion application for smart washing machines, dryers, and shoe cleaners. It provides direct Bluetooth Low Energy (BLE) appliance control, interactive wash/dry program configuration, live drum cycle countdown telemetry, and an open access model allowing either **100% Free Community Wash** or **Voluntary Contributions**.

---

## ✨ Features

- **📷 Machine Connect & QR Scanner:** Instantly connect to smart washers, dryers, and shoe cleaning units via camera QR scan, Bluetooth discovery, or manual machine code entry.
- **⚡ Multiple Wash & Dry Programs:**
  - `Quick Wash` (15 min • 30°C • 800 RPM)
  - `Standard Cotton` (35 min • 40°C • 1000 RPM)
  - `Heavy Duty & Bedding` (50 min • 60°C • 1200 RPM)
  - `Shoe Wash Cycle` (30 min • 30°C • 700 RPM)
  - `Hot Air Dry` (40 min • 60°C)
- **🌿 Flexible & Free Access Model:**
  - **Community Free Wash (₹ 0):** Completely free access mode. Run full cycles without mandatory paywalls.
  - **Voluntary Contribution (₹ 49 / Custom):** Optional support if users wish to contribute towards electricity and laundromat upkeep.
  - **Promo / Voucher Support:** Apply coupon codes (`FREEWASH`, `COMMUNITY`, etc.).
- **🌀 Live Visual Washer Drum & Countdown:**
  - Animated rotating washer drum visual.
  - Multi-stage cycle progress stepper:
    1. *Water Filling & Detergent Dissolving*
    2. *Main Wash Agitation*
    3. *Deep Foam Rinsing*
    4. *High Speed Centrifugal Spin (1000 RPM)*
    5. *Cycle Complete & Door Unlock Alert*
  - Remote Pause, Resume, and Emergency Stop controls.
- **📡 Dual-Mode Appliance Engine:**
  - **Hardware BLE Mode:** Connects to real Bluetooth GATT appliance controllers (Service UUID `0000FE60...`).
  - **High-Fidelity Simulator Mode:** Built-in hardware emulator allowing full cycle testing and demonstration on any phone or emulator without physical appliances.
- **📜 Wash History & Savings Tracker:**
  - Persistent order log with date, machine ID, program type, and cost.
  - Live savings counter tracking money saved via community free mode.

---

## 🛠️ Tech Stack & Architecture

- **Platform:** Android (Java / Android SDK 34 / Minimum SDK 24)
- **Architecture:** Clean Architecture / Model-View-Presenter with component separation:
  - `com.openwasher.app.model`: Domain entities (`WashProgram`, `MachineDevice`, `MachineOrder`).
  - `com.openwasher.app.ble`: Appliance communication protocols & GATT connection manager.
  - `com.openwasher.app.data`: Local JSON persistence via SharedPreferences.
  - `com.openwasher.app`: Activities, views, and animations.
- **Design:** Modern Dark Oceanic theme with Material design aesthetics.

---

## 📁 Project Structure

```
SmartWasherApp/
├── app/
│   ├── build.gradle
│   └── src/main/
│       ├── AndroidManifest.xml
│       ├── java/com/openwasher/app/
│       │   ├── MainActivity.java            # Main Dashboard & stats
│       │   ├── ScannerActivity.java         # QR Code & Machine ID input
│       │   ├── WashControlActivity.java     # Cycle settings & temperature
│       │   ├── PaymentActivity.java         # Free vs Voluntary choice
│       │   ├── CycleTimerActivity.java      # Live spinning drum & countdown
│       │   ├── OrderHistoryActivity.java    # Past wash cycles list
│       │   ├── ble/
│       │   │   ├── BleManager.java          # Bluetooth GATT & Simulator
│       │   │   └── WasherBleProtocol.java   # Appliance command packets
│       │   ├── data/
│       │   │   └── OrderStorage.java        # SharedPreferences persistence
│       │   └── model/
│       │       ├── MachineDevice.java
│       │       ├── MachineOrder.java
│       │       └── WashProgram.java
│       └── res/
│           ├── layout/                      # Clean XML layouts
│           ├── values/                      # Colors, strings, themes
│           └── drawable/                    # Custom shapes, drum gradients
├── build.gradle
├── settings.gradle
└── README.md
```

---

## 🚀 How to Build & Run

### In Android Studio:
1. Open **Android Studio**.
2. Select **File > Open...** and choose the `SmartWasherApp` folder.
3. Allow Gradle to sync dependencies.
4. Click **Run** (`Shift + F10`) to deploy to your Android device or emulator.

### Push to GitHub:
```bash
git init
git add .
git commit -m "Initial commit: OpenWasher smart laundry companion app"
git branch -M main
git remote add origin https://github.com/<your-username>/OpenWasher.git
git push -u origin main
```

---

## 📄 License
This project is open-source under the MIT License.
