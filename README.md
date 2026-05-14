# StereoVision - Robotic Vision Project

This Android application implements a complete Stereo Vision pipeline using a single smartphone moved along a physical rail. This project was developed as a practical activity for the **Robotic Vision (Visão Robótica)** course at **UFLA (Universidade Federal de Lavras)**.

## 📱 Features
- **1. Calibration**: Real-time chessboard corner detection and camera calibration ($K$ and $distCoeffs$) using OpenCV. Saves results to `calib.yml`.
- **2. Capture**: Dual-mode capture (Left/Right) using CameraX to ensure consistent focus and exposure for the stereo pair.
- **3. Processing**: 
    - Image undistortion using calibration data.
    - Feature detection and matching (ORB).
    - Fundamental Matrix ($F$) estimation via RANSAC.
    - Uncalibrated Stereo Rectification.
    - Disparity Map generation using `StereoBM`.
- **4. Depth / Results**: Calculation of depth $Z$ using focal length and baseline. Export of results to a `.ply` point cloud file.

## 🧪 Experimental Procedure

Follow these steps to reproduce the experiment as described in the course material:

### Phase 1: Physical Setup
1. Mount your smartphone on a sliding rail or a stable tripod.
2. Prepare a printed **Chessboard Pattern** (typically 10x7 squares, providing 9x6 inner corners) on a flat surface.
3. Ensure the scene has adequate lighting and distinct features for better matching.

### Phase 2: Camera Calibration (Task T1)
1. Open the **1. Calibration** menu in the app.
2. Point the camera at the chessboard from various angles and distances.
3. When the board is detected, click **"Add Pose"**. Collect at least **10 to 15 different poses**.
4. Click **"Calibrate"**. The app will calculate the intrinsic parameters and save them to `calib.yml` in the app's internal storage.

### Phase 3: Stereo Capture (Task T2)
1. Open the **2. Capture** menu.
2. Slide the phone to the **Left** position on your rail. Click **"CAPTURE LEFT"** (saves as `I_L.jpg`).
3. Slide the phone to the **Right** position by a known distance (the Baseline). Click **"CAPTURE RIGHT"** (saves as `I_R.jpg`).

### Phase 4: Image Processing (Tasks T3-T7)
1. Open the **3. Processing** menu.
2. Click **"Run Pipeline"**.
3. The app will automatically load the pair, perform undistortion (if calibration exists), detect ORB features, estimate the Fundamental Matrix ($F$), and rectify the images.
4. A **Disparity Map** will be generated and displayed on the screen.

### Phase 5: Depth Calculation & Export (Task T8)
1. Open the **4. Depth / Results** menu.
2. Enter the **Baseline** distance (in meters) that you used during capture.
3. Click **"Generate Point Cloud (.ply)"**.
4. The app calculates the 3D coordinates ($X, Y, Z$) and saves a `cloud.ply` file to your device's external storage.
5. **Visualization**: Transfer the `.ply` file to a PC and open it with **MeshLab** or **CloudCompare** to view the 3D reconstruction.

## 🛠️ Technical Specifications
- **Android Studio**: Ladybug (or newer)
- **Minimum SDK**: 24 (Android 7.0)
- **Target SDK**: 36
- **Languages**: Kotlin with Jetpack Compose
- **Main Dependencies**:
    - [OpenCV 4.13.0](https://opencv.org/) (Static loading via Maven Central)
    - [CameraX](https://developer.android.com/training/camerax)
    - Jetpack Compose Material 3

## 🚀 Build Instructions
1. Clone this repository: `git clone https://github.com/alabayusuf010-afk/StereoVision.git`
2. Open in **Android Studio**.
3. Sync Gradle and build the project.
4. Deploy to a physical Android device.

## 📁 Project Structure
- `MainActivity.kt`: Main menu and permissions.
- `CalibrationActivity.kt`: Board detection and K/dist estimation (T1).
- `CaptureActivity.kt`: Consistent stereo pair capture (T2).
- `ProcessingActivity.kt`: ORB matching, F estimation, and Disparity (T3-T7).
- `DepthActivity.kt`: Z calculation and PLY export (T8).
- `StereoUtils.kt`: CV utility functions and file I/O.

## 👨‍🏫 Acknowledgments
- **Institution**: UFLA - Departamento de Automática
- **Professor**: Arthur de Miranda Neto

---
*Developed for the Robotic Vision practical assignment.*
