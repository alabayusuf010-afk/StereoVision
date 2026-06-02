# StereoVision - Robotic Vision Project

This Android application implements a complete Stereo Vision pipeline using a single smartphone moved along a physical rail. This project was developed as a practical activity for the **Robotic Vision (Visão Robótica)** course at **UFLA (Universidade Federal de Lavras)**.

## 🛠️ Software Requirements & Organization
The implementation is realized entirely in **Android Studio**, utilizing the **OpenCV Android SDK 4.x**. Access to the camera is handled via the **CameraX** library, while all image processing tasks are performed using the native OpenCV module (`org.opencv.*`).

### Class / Activity Responsibilities
| Class / Activity | Responsibility |
| :--- | :--- |
| **MainActivity** | App home screen; navigates to Calibration, Capture, Processing, and Depth/Results. Verifies camera and storage permissions; loads the OpenCV library (`OpenCVLoader`). |
| **CalibrationActivity** | Executes **T1**: Camera preview, chessboard pattern detection, pose accumulation, and calibration. Saves the intrinsic matrix $K$ and distortion coefficients $distCoeffs$ to `files/calib.yml`. |
| **CaptureActivity** | Executes **T2**: CameraX `PreviewView` with focus and exposure lock. Includes a capture button to save the stereo pair (`I_L` and `I_R`) as JPEG in internal storage. |
| **ProcessingActivity** | Executes **T3 to T7**: Loads the image pair, performs undistortion, ORB feature detection and matching, Fundamental Matrix ($F$) estimation, rectification, and disparity map generation. Displays progress and results. |
| **DepthActivity** | Executes **T8**: Calculates the depth $Z$ from the disparity map and exports the result as a `.ply` point cloud file. |
| **StereoUtils** | Utility functions: Reading/writing `Mat` objects in `.yml`, `Mat` $\leftrightarrow$ `Bitmap` conversion, and drawing epipolar lines. |

## 🧪 Experimental Procedure
Follow these steps to reproduce the experiment:

### Phase 1: Physical Setup
1. Mount the smartphone on a stable tripod or rail system.
2. Prepare a printed **Chessboard Pattern** (e.g., 9x6 inner corners) on a flat surface.
3. Ensure adequate lighting and distinct scene features.

### Phase 2: Calibration (T1)
1. Open **1. Calibration**.
2. Capture at least **10-15 different poses** of the chessboard.
3. Click **"Calibrate"** to save the intrinsic parameters to `calib.yml`.

### Phase 3: Stereo Capture (T2)
1. Open **2. Capture**.
2. Capture the **Left** image (`I_L.jpg`), slide the phone by a fixed **Baseline** distance, and capture the **Right** image (`I_R.jpg`).

### Phase 4: Processing (T3-T7)
1. Open **3. Processing** and click **"Run Pipeline"**.
2. The app performs rectification and generates a **Disparity Map**.

### Phase 5: Depth & Export (T8)
1. Open **4. Depth / Results**.
2. Enter the **Baseline** distance used.
3. Click **"Generate Point Cloud (.ply)"** to export the 3D data.

## 📦 Mandatory Delivery Components
As per the course requirements, the project delivery includes:

1.  **Technical Report (PDF)**: Detailed article (IEEE/SBC format) covering the setup, pipeline, and results (including $K$, $F$, RANSAC inliers, and error analysis).
2.  **Git Repository**: Full source code with incremental commit history, `README.md`, and representative image captures.
3.  **Demo Video (YouTube)**: A 15-minute demonstration covering the physical setup, a live code pipeline run, and result discussion.
4.  **PowerPoint Presentation**: Slides used for the oral presentation.

## 🔗 Project Resources
- **Presentation Slides**: [Google Slides](https://docs.google.com/presentation/d/1ZIOWcIPi5HoriK5pGHp3YpO_tCx21WlC/edit?usp=sharing&ouid=105571612435093965131&rtpof=true&sd=true)
- **Final Report**: [Google Drive (PDF)](https://drive.google.com/file/d/1HNoWvnH6nAIgtGTBmBz4ybVmDG3nddeH/view?usp=sharing)
- **Demo Video**: [YouTube](https://youtu.be/ckCqr9awIRs)

## 🚀 Build Instructions
1. Clone the repository: `git clone https://github.com/alabayusuf010-afk/StereoVision.git`
2. Open in **Android Studio** (Ladybug or newer).
3. Sync Gradle and deploy to a physical device (Minimum SDK 24).

## 👨‍🏫 Acknowledgments
- **Institution**: UFLA - Departamento de Automática
- **Professor**: Arthur de Miranda Neto

---
*Developed for the Robotic Vision practical assignment.*
