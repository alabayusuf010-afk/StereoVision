# StereoVision - Robotic Vision Project

This Android application implements a complete Stereo Vision pipeline using a single smartphone moved along a physical rail. This project was developed as a practical activity for the **Robotic Vision (Visão Robótica)** course at **UFLA (Universidade Federal de Lavras)**.

## 📱 Features
- **1. Calibration**: Real-time chessboard corner detection and camera calibration ($K$ and $distCoeffs$) using OpenCV. Saves results to `calib.yml`.
- **2. Capture**: Dual-mode capture (Left/Right) using CameraX to ensure consistent focus and exposure for the stereo pair.
- **3. Processing**: 
    - Image undistortion.
    - Feature detection and matching (ORB).
    - Fundamental Matrix ($F$) estimation via RANSAC.
    - Uncalibrated Stereo Rectification.
    - Disparity Map generation using `StereoBM`.
- **4. Depth / Results**: Calculation of depth $Z$ using focal length and baseline. Export of results to a `.ply` point cloud file.

## 🛠️ Technical Specifications
- **Android Studio**: Ladybug (or newer)
- **Minimum SDK**: 24 (Android 7.0)
- **Target SDK**: 36
- **Languages**: Kotlin with Jetpack Compose
- **Main Dependencies**:
    - [OpenCV 4.13.0](https://opencv.org/) (Static loading via Maven Central)
    - [CameraX](https://developer.android.com/training/camerax) (Core, Camera2, Lifecycle, View)
    - Jetpack Compose Material 3

## 🚀 Build Instructions
1. Clone this repository.
2. Open the project in **Android Studio**.
3. Allow Gradle to sync and download dependencies (OpenCV and CameraX).
4. Ensure a physical Android device is connected (Camera permission is required).
5. Click **Run** (`Shift + F10`).

## 📁 Project Structure
- `MainActivity.kt`: Main menu and permission handling.
- `CalibrationActivity.kt`: Implementation of Task T1.
- `CaptureActivity.kt`: Implementation of Task T2.
- `ProcessingActivity.kt`: Implementation of Tasks T3 to T7.
- `DepthActivity.kt`: Implementation of Task T8 and PLY export.
- `StereoUtils.kt`: Reusable computer vision utility functions.

## 👨‍🏫 Acknowledgments
- **Institution**: UFLA - Departamento de Automática
- **Professor**: Arthur de Miranda Neto

---
*Developed for the Robotic Vision practical assignment.*
