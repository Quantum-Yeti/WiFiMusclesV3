# WiFi Muscles

**A lightweight Wi-Fi analyzer for Android designed for everyday users.**

Wi-Fi Muscles surfaces the information that actually matters — signal strength,
band, and connection quality — without overwhelming the user with raw network data.

---

## Table of Contents

- [Overview](#overview)
- [Features](#features)
- [Roadmap](#roadmap)
- [Requirements](#requirements)
- [Getting Started](#getting-started)
- [Architecture](#architecture)
- [Permissions](#permissions)
- [License](#license)
- [Privacy Policy](#privacy-policy)

---

## Overview

Most Wi-Fi analyzer apps are built for network engineers. Wi-Fi Muscles is built
for everyone else. The goal is a clean, fast, and honest answer to the question
most users actually have: *"Does my Wi-Fi suck?"*

---

## Features

- Real-time signal strength monitoring with a visual signal indicator
- Network name (SSID) and frequency band display (2.4 GHz, 5 GHz, 6 GHz / Wi-Fi 6E)
- Link speed formatted in Mbps or Gbps
- Plain-English signal quality rating — Excellent, Good, Fair, or Poor
- Bottom navigation across Home, Details, Scan, Speed Test, and Options

---

## Roadmap

| Feature                       | Status |
|-------------------------------|---|
| Real-time signal monitoring   | ✅ Complete |
| Frequency band detection      | ✅ Complete |
| Link speed display            | ✅ Complete |
| Signal quality rating         | ✅ Complete |
| Bottom navigation scaffold    | ✅ Complete |
| Network details screen        | ✅ Complete |
| Gateway ping / response time  | 📋 Planned |
| Channel congestion scanner    | 📋 Planned |
| Signal strength history graph | 📋 Planned |
| Contextual user tips          | 📋 Planned |
| Speed test                    | ✅ Complete |
| UI Refinement                 | 📋 Planned  |

---

## Requirements

- Android 5.0 (API Level 21) or higher
- Device with Wi-Fi hardware
- Location permission (see [Permissions](#permissions))

---

## Getting Started

### Prerequisites

- Android Studio Hedgehog or later
- JDK 11 or higher

### Installation

1. Clone the repository
```bash
   git clone https://github.com/Quantum-Yeti/WiFiMusclesV3.git
```

2. Open the project in Android Studio

3. Allow Gradle to sync dependencies

4. Build and run on a physical device or an emulator with Wi-Fi support

> **Note:** Wi-Fi SSID and signal data are not available on emulators.
> A physical device is recommended for full functionality.

---

## Architecture
Wi-Fi Muscles follows MVVM — fragments observe LiveData exposed by ViewModels, which pull from repository/helper classes. No business logic lives in the UI layer.

**Stack**

| Layer | Technology |
|---|---|
| Language | Java |
| Architecture | MVVM |
| Lifecycle | ViewModel + LiveData |
| UI | Fragments, ViewBinding, Bottom Navigation |
| Ads | Google AdMob |

---

## Permissions

| Permission | Reason |
|---|---|
| `ACCESS_FINE_LOCATION` | Required by Android OS to read SSID and BSSID |
| `ACCESS_WIFI_STATE` | Read current WiFi connection details |
| `CHANGE_WIFI_STATE` | Reserved for future channel scan functionality |

> Android requires location permission to access Wi-Fi network identifiers
> as of API Level 26. Wi-Fi Muscles does not collect or transmit location data.

---

## Contributing

Issues and feature requests are welcome.
Please open an issue to report anything.

---

## License

This project is licensed under the MIT License.
See [LICENSE](LICENSE.md) for details.

---

## Privacy Policy

The privacy policy can be found here: [Privacy Policy](https://quantum-yeti.github.io/privacypolicy/wifimuscles.html)

---

## Author

Developed and maintained by [quantum-yeti](https://github.com/quantum-yeti)