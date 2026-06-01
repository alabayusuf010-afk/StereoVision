package com.example.stereovision

import android.graphics.Bitmap
import org.opencv.android.Utils
import org.opencv.core.CvType
import org.opencv.core.Mat
import org.opencv.core.Point
import org.opencv.core.Scalar
import org.opencv.imgproc.Imgproc
import java.io.File

object StereoUtils {
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
    fun drawEpipolarLines(image: Mat, lines: Mat, points: Mat) {
        val rows = image.rows()
        val cols = image.cols()
        for (i in 0 until lines.rows()) {
            val l = lines.get(i, 0)
            val x0 = 0.0
            val y0 = -l[2] / l[1]
            val x1 = cols.toDouble()
            val y1 = -(l[2] + l[0] * x1) / l[1]
            Imgproc.line(image, Point(x0, y0), Point(x1, y1), Scalar(0.0, 255.0, 0.0), 1)
        }
    }

    /**
     * Saves Camera Matrix K and Distortion Coefficients to a simple file.
     * OpenCV FileStorage in Java can be tricky with file paths, so we use a simple text format
     * or a serialized Mat approach if needed. Here we'll use a simple CSV-like format for K and dist.
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
        try {
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

            return Pair(cameraMatrix, distCoeffs)
        } catch (e: Exception) {
            return null
        }
    }
}
