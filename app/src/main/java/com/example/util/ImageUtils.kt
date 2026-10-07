package com.example.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.net.Uri
import android.util.Base64
import android.media.ExifInterface
import com.example.data.config.EcoConfig
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream

object ImageUtils {

    /**
     * Resizes bitmap to max dimension (width or height <= EcoConfig.IMAGE_MAX_WIDTH),
     * rotates according to EXIF, compresses to JPEG quality ~60%, and returns base64 + local file path.
     */
    fun processImage(context: Context, uri: Uri): ProcessedImageResult? {
        return try {
            val inputStream: InputStream? = context.contentResolver.openInputStream(uri)
            val originalBitmap = BitmapFactory.decodeStream(inputStream)
            inputStream?.close() ?: return null
            if (originalBitmap == null) return null

            // Read EXIF orientation
            var orientation = ExifInterface.ORIENTATION_NORMAL
            context.contentResolver.openInputStream(uri)?.use { stream ->
                val exif = ExifInterface(stream)
                orientation = exif.getAttributeInt(
                    ExifInterface.TAG_ORIENTATION,
                    ExifInterface.ORIENTATION_NORMAL
                )
            }

            val matrix = Matrix()
            when (orientation) {
                ExifInterface.ORIENTATION_ROTATE_90 -> matrix.postRotate(90f)
                ExifInterface.ORIENTATION_ROTATE_180 -> matrix.postRotate(180f)
                ExifInterface.ORIENTATION_ROTATE_270 -> matrix.postRotate(270f)
                ExifInterface.ORIENTATION_FLIP_HORIZONTAL -> matrix.postScale(-1f, 1f)
                ExifInterface.ORIENTATION_FLIP_VERTICAL -> matrix.postScale(1f, -1f)
            }

            // Calculate scale
            val maxDimension = EcoConfig.IMAGE_MAX_WIDTH
            val width = originalBitmap.width
            val height = originalBitmap.height
            val scale = if (width > maxDimension || height > maxDimension) {
                val scaleFactor = maxDimension.toFloat() / maxOf(width, height)
                scaleFactor
            } else {
                1f
            }
            matrix.postScale(scale, scale)

            val scaledBitmap = Bitmap.createBitmap(
                originalBitmap,
                0,
                0,
                width,
                height,
                matrix,
                true
            )

            // Compress to JPEG with quality 60
            val outputStream = ByteArrayOutputStream()
            scaledBitmap.compress(Bitmap.CompressFormat.JPEG, EcoConfig.JPEG_QUALITY, outputStream)
            val byteArray = outputStream.toByteArray()

            // Save a local cache file for the UI to display
            val cacheFile = File(context.cacheDir, "scan_${System.currentTimeMillis()}.jpg")
            FileOutputStream(cacheFile).use { fos ->
                fos.write(byteArray)
            }

            val base64String = Base64.encodeToString(byteArray, Base64.NO_WRAP)

            ProcessedImageResult(
                base64 = base64String,
                filePath = cacheFile.absolutePath,
                byteSize = byteArray.size
            )
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
}

data class ProcessedImageResult(
    val base64: String,
    val filePath: String,
    val byteSize: Int
)
