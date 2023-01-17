package cz.csob.smartbanking.codebase.presentation.util

import android.content.res.Configuration
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.ImageFormat
import android.graphics.Matrix
import android.graphics.Rect
import android.graphics.YuvImage
import android.media.Image
import cz.eman.logger.logVerbose
import java.io.ByteArrayOutputStream

/**
 * Extensions connected to camera actions, fragments, views and other.
 *
 * @author eMan a.s.
 */

/**
 * Bitmap size enum containing scaling sizes for specific bitmaps.
 *
 * @property maxWidth max bitmap width
 * @property maxHeight max bitmap height
 */
enum class BitmapSize(val maxWidth: Int, val maxHeight: Int) {
    /**
     * This size is used for user document bitmaps. For example for driving licence, user
     * identification (op) or other. Max width is 1920 and height 1080 (depends on rotation of the
     * image)
     */
    SIZE_OP(1920, 1080),

    /**
     * This size is used for QR detections since sometimes the image needs to be downscaled to find
     * the QR code in it. Max with and height are set to 1280.
     */
    SIZE_QR(1280, 1280)
}

/**
 * Converts image to Bitmap. Based on how many planes the image has it either calls [toBitmapYuv]
 * which has 3 planes to convert. Else it calls [toBitmapJpeg] which should have only 1 plane to
 * take bitmap data from.
 *
 * @return [Bitmap] from YUV or jpeg
 */
fun Image.toBitmap(): Bitmap {
    return if (planes.size == 3) {
        this.toBitmapYuv()
    } else {
        this.toBitmapJpeg()
    }
}

/**
 * Creates a bitmap from YUV image. Takes data only from the first plane (Y) and third plane (VU)
 * which corresponds with YUV image data (usually returned from CameraX). Merges data from planes
 * buffers, creates [YuvImage] which is then compressed to jpeg using [YuvImage.compressToJpeg].
 * After that is just creates te bitmap from the data and downscales it to [BitmapSize.SIZE_OP].
 *
 * Based on: // https://stackoverflow.com/a/56812799/10169723.
 *
 * @return [Bitmap] from YUV
 */
fun Image.toBitmapYuv(): Bitmap {
    logVerbose { "toBitmapYuv(format = ${this.format})" }
    val yBuffer = planes[0].buffer // Y
    val vuBuffer = planes[2].buffer // VU

    val ySize = yBuffer.remaining()
    val vuSize = vuBuffer.remaining()

    val nv21 = ByteArray(ySize + vuSize)

    yBuffer.get(nv21, 0, ySize)
    vuBuffer.get(nv21, ySize, vuSize)

    val yuvImage = YuvImage(nv21, ImageFormat.NV21, this.width, this.height, null)
    val out = ByteArrayOutputStream()
    yuvImage.compressToJpeg(Rect(0, 0, yuvImage.width, yuvImage.height), 50, out)
    val imageBytes = out.toByteArray()
    return BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.size).downscale()
}

/**
 * Creates a bitmap from image. Takes data only from the first plane (0) which corresponds with jpeg
 * image data. Uses the planes buffer to decode bitmap from byte array. Creates the bitmap and then
 * it downscales is to [BitmapSize.SIZE_OP].
 *
 * @return [Bitmap] from Image
 */
fun Image.toBitmapJpeg(): Bitmap {
    logVerbose { "toBitmapJpeg(format = ${this.format})" }
    val buffer = planes[0].buffer
    buffer.rewind()
    val bytes = ByteArray(buffer.capacity())
    buffer.get(bytes)
    return BitmapFactory.decodeByteArray(bytes, 0, bytes.size).downscale()
}

/**
 * Downscales the bitmap based on [size] parameter. If the bitmap is already smaller it does nothing
 * and just returns the bitmap. Else it creates a new bitmap based on new size calculated using
 * [getScaledBitmapSize].
 *
 * @param size to be downscaled to
 */
fun Bitmap.downscale(size: BitmapSize = BitmapSize.SIZE_OP): Bitmap {
    logVerbose("rescale()")
    val newSize = this.getScaledBitmapSize(size)
    return if (this.width > newSize.first || this.height > newSize.second) {
        logVerbose(
            "rescale() - scale from (${this.width}x${this.height}) to " +
                "(${newSize.first}x${newSize.second})"
        )
        Bitmap.createScaledBitmap(this, newSize.first, newSize.second, true)
    } else {
        this
    }
}

/**
 * Converts image to mirrored Bitmap using ByteArray. Useful for picture taken by the camera in the
 * app. Mirroring is useful for front camera. Also enables to rotate the image by 90° which helps
 * when image was taken in portrait mode. Image will not be rotated if it was already rotated during
 * image to bitmap conversion.
 *
 * @param rotate set to true if image should be rotated by 90°
 */
fun Image.toMirroredBitmap(rotate: Boolean, orientation: Int): Bitmap {
    logVerbose { "toMirroredBitmap(rotate = $rotate)" }

    val bitmap = this.toBitmap()
    val rotateBitmap = rotate && shouldRotateBitmap(bitmap, orientation)
    val rotated = this.width == bitmap.height && this.height == bitmap.width
    val mtx = Matrix().apply {
        preScale(-1.0f, 1.0f)
        if (rotateBitmap && !rotated) {
            postRotate(90f)
        }
    }

    return Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, mtx, true)
}

/**
 * Crops bitmap image based on scale and ratio.
 *
 * @param scale used to calculate new width
 * @param ratio used to calculate new height
 */
fun Bitmap.cropImage(
    scale: Float,
    ratio: Double,
): Bitmap {
    logVerbose { "cropImage(scale = $scale, ratio = $ratio)" }

    if (scale < 1f || ratio < 1.0) {
        throw IllegalStateException(
            "Picture cannot be scaled upwards. Scale and ratio mus be higher than 1."
        )
    }

    val (newWidth, newHeight) = calculateNewWidthAndHeight(
        this.width,
        scale,
        ratio
    )

    return if (this.width == newWidth && this.height == newHeight) {
        this
    } else {
        val result = Bitmap.createBitmap(
            this,
            ((this.width - newWidth) / 2),
            ((this.height - newHeight) / 2),
            newWidth,
            newHeight
        )
        if (this != result) {
            this.recycle()
        }
        result
    }
}

/**
 * Rotate given bitmap by [degrees] only if it is not in requested [orientation]. Otherwise returns
 * original bitmap.
 *
 * @param bitmap bitmap to rotate
 * @param degrees defines how much we want to rotate the bitmap. Default value, rotate 270° clockwise
 * (or 90° anti-clockwise) is used because it is default rotation of vertical pictures taken by camera.
 * @param orientation requested orientation, defaults to landscape
 *
 *
 * @return rotated bitmap or original if no rotation is needed
 */
fun rotateBitmapIfNeeded(
    bitmap: Bitmap,
    degrees: Float = 270f,
    orientation: Int = Configuration.ORIENTATION_LANDSCAPE,
): Bitmap =
    if (shouldRotateBitmap(bitmap, orientation)) {
        val rotateMatrix = Matrix().apply {
            postRotate(degrees)
        }
        Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, rotateMatrix, true)
    } else {
        bitmap
    }

/**
 * Gets new bitmap size based on [size] parameter. New size must be lower or equal to the size
 * param.
 *
 * @param size to be downscaled to
 * @return new size pair
 */
private fun Bitmap.getScaledBitmapSize(size: BitmapSize = BitmapSize.SIZE_OP): Pair<Int, Int> {
    val (maxWidth, maxHeight) = if (width > height) {
        size.maxWidth to size.maxHeight
    } else {
        size.maxHeight to size.maxWidth
    }

    val ratioBitmap = width.toFloat() / height.toFloat()
    val ratioMax = maxWidth.toFloat() / maxHeight.toFloat()
    logVerbose { "getScaledBitmapSize() bitmap/max aspect ($ratioBitmap/$ratioMax)" }

    var finalWidth = maxWidth
    var finalHeight = maxHeight
    if (ratioMax > ratioBitmap) {
        finalWidth = (maxHeight.toFloat() * ratioBitmap).toInt()
    } else {
        finalHeight = (maxWidth.toFloat() / ratioBitmap).toInt()
    }

    return Pair(finalWidth, finalHeight)
}

/**
 * Checks if the bitmap should be rotated or not. If the orientation is portrait then the height
 * of the image should be higher and in landscape the width should be higher. If it is not the case
 * then the image should be rotated.
 *
 * @param bitmap used to check size of the bitmap
 * @param orientation of the screen
 * @return true when bitmap should be rotated
 */
private fun shouldRotateBitmap(bitmap: Bitmap, orientation: Int): Boolean {
    return (orientation == Configuration.ORIENTATION_PORTRAIT && bitmap.width > bitmap.height) ||
        (orientation == Configuration.ORIENTATION_LANDSCAPE && bitmap.width < bitmap.height)
}

/**
 * Calculates bitmap new width and new height based on [dimenSize] since we want to make sure new
 * values are not higher then original ones. Value [dimenSize] should be min of original width and
 * height.
 *
 * Calculates new width and then is uses it to calculate new height. It also checks if the new
 * values are not higher then original ones. If they are then original ones are used.
 *
 * @param dimenSize min of bitmap height and width
 * @param scale used to calculate new width
 * @param ratio used to calculate new height
 */
private fun Bitmap.calculateNewWidthAndHeight(
    dimenSize: Int,
    scale: Float,
    ratio: Double,
): Pair<Int, Int> {
    logVerbose {
        "calculateNewWidthAndHeight(dimenSize = $dimenSize, scale = $scale, ratio = $ratio)"
    }

    val newWidth = dimenSize / scale
    val newHeight = (newWidth / ratio).toInt()

    return if (this.height < newHeight) {
        calculateNewWidthAndHeight(
            this.height,
            scale,
            ratio
        )
    } else {
        newWidth.toInt() to newHeight
    }
}
