package com.example.stereovision

import android.graphics.Bitmap
import org.opencv.android.Utils
import org.opencv.calib3d.Calib3d
import org.opencv.core.CvType
import org.opencv.core.Mat
import org.opencv.core.Point
import org.opencv.core.Scalar
import org.opencv.imgproc.Imgproc
import java.io.File
import kotlin.math.cos
import kotlin.math.sin

object StereoUtils {
    /**
     * Converts Euler angles to a 3x3 Rotation Matrix.
     */
    fun eulerToRotationMatrix(rx: Float, ry: Float, rz: Float): Mat {
        val rMatrix = Mat.eye(3, 3, CvType.CV_64F)

        // Simplified rotation matrix from Euler angles (Pitch, Yaw, Roll)
        val cx = cos(rx.toDouble())
        val sx = sin(rx.toDouble())
        val cy = cos(ry.toDouble())
        val sy = sin(ry.toDouble())
        val cz = cos(rz.toDouble())
        val sz = sin(rz.toDouble())

        // Rz * Ry * Rx
        rMatrix.put(0, 0, cy * cz)
        rMatrix.put(0, 1, (sx * sy * cz) - (cx * sz))
        rMatrix.put(0, 2, (cx * sy * cz) + (sx * sz))
        rMatrix.put(1, 0, cy * sz)
        rMatrix.put(1, 1, (sx * sy * sz) + (cx * cz))
        rMatrix.put(1, 2, (cx * sy * sz) - (sx * cz))
        rMatrix.put(2, 0, -sy)
        rMatrix.put(2, 1, sx * cy)
        rMatrix.put(2, 2, cx * cy)

        return rMatrix
    }

    /**
     * Performs Stereo Rectification using the IMU-provided Relative Pose (R, t).
     * This replaces the "mechanical rail" by using VIO logic.
     */
    fun rectifyWithIMU(
        grayL: Mat,
        grayR: Mat,
        cameraMatrix: Mat,
        dist: Mat,
        rotation: Mat,
        translation: Mat,
    ): Pair<Mat, Mat> {
        val r1 = Mat()
        val r2 = Mat()
        val p1 = Mat()
        val p2 = Mat()
        val q = Mat()

        // R and t are from the 1st to the 2nd camera position
        Calib3d.stereoRectify(
            cameraMatrix, dist, cameraMatrix, dist,
            grayL.size(), rotation, translation,
            r1, r2, p1, p2, q,
            Calib3d.CALIB_ZERO_DISPARITY, -1.0, grayL.size()
        )

        val map1x = Mat()
        val map1y = Mat()
        val map2x = Mat()
        val map2y = Mat()

        Calib3d.initUndistortRectifyMap(cameraMatrix, dist, r1, p1, grayL.size(), CvType.CV_32FC1, map1x, map1y)
        Calib3d.initUndistortRectifyMap(cameraMatrix, dist, r2, p2, grayL.size(), CvType.CV_32FC1, map2x, map2y)

        val rectL = Mat()
        val rectR = Mat()
        Imgproc.remap(grayL, rectL, map1x, map1y, Imgproc.INTER_LINEAR)
        Imgproc.remap(grayR, rectR, map2x, map2y, Imgproc.INTER_LINEAR)

        return Pair(rectL, rectR)
    }

    fun matToBitmap(mat: Mat): Bitmap {
        val bitmap = Bitmap.createBitmap(mat.cols(), mat.rows(), Bitmap.Config.ARGB_8888)
        Utils.matToBitmap(mat, bitmap)
        return bitmap
    }

    fun bitmapToMat(bitmap: Bitmap): Mat {
        val mat = Mat()
        Utils.bitmapToMat(bitmap, mat)
        return mat
    }

    /**
     * Draws epipolar lines on an image.
     */
    fun drawEpipolarLines(image: Mat, lines: Mat) {
        val cols = image.cols()
        for (i in 0 until lines.rows()) {
            val l = lines.get(i, 0)
            val x0 = 0.0
            val y0 = -l[2] / l[1]
            val x1 = cols.toDouble()
            val y1 = -(l[2] + (l[0] * x1)) / l[1]
            Imgproc.line(image, Point(x0, y0), Point(x1, y1), Scalar(0.0, 255.0, 0.0), 1)
        }
    }

    /**
     * Saves Camera Matrix K and Distortion Coefficients to a simple file.
     */
    fun saveCalibration(file: File, cameraMatrix: Mat, distCoeffs: Mat) {
        val sb = StringBuilder()
        // Save Camera Matrix
        for (i in 0..2) {
            for (j in 0..2) {
                sb.append(cameraMatrix.get(i, j)[0]).append(",")
            }
        }
        sb.append("\n")
        // Save Dist Coeffs
        val rows = distCoeffs.rows()
        val cols = distCoeffs.cols()
        if (rows >= cols) {
            for (i in 0 until rows) {
                sb.append(distCoeffs.get(i, 0)[0]).append(",")
            }
        } else {
            for (j in 0 until cols) {
                sb.append(distCoeffs.get(0, j)[0]).append(",")
            }
        }
        file.writeText(sb.toString())
    }

    fun loadCalibration(file: File): Pair<Mat, Mat>? {
        if (!file.exists()) return null
        return try {
            val lines = file.readLines()
            if (lines.size < 2) return null

            val kValues = lines[0].split(",").filter { it.isNotEmpty() }.map { it.toDouble() }
            val cameraMatrix = Mat(3, 3, CvType.CV_64F)
            var idx = 0
            for (i in 0..2) {
                for (j in 0..2) {
                    cameraMatrix.put(i, j, kValues[idx++])
                }
            }

            val dValues = lines[1].split(",").filter { it.isNotEmpty() }.map { it.toDouble() }
            val distCoeffs = Mat(1, dValues.size, CvType.CV_64F)
            for (i in dValues.indices) {
                distCoeffs.put(0, i, dValues[i])
            }

            Pair(cameraMatrix, distCoeffs)
        } catch (e: Exception) {
            null
        }
    }
}
