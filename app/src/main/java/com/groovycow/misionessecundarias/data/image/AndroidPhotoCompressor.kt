package com.groovycow.misionessecundarias.data.image

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.media.ExifInterface
import android.net.Uri
import com.groovycow.misionessecundarias.domain.model.ProfileError
import com.groovycow.misionessecundarias.domain.model.ProfileException
import com.groovycow.misionessecundarias.domain.repository.PhotoCompressor
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.io.IOException
import kotlin.coroutines.cancellation.CancellationException

/**
 * [PhotoCompressor] sobre APIs de Android: acepta solo JPEG/PNG, escala el lado mayor a 1080 px y
 * codifica JPEG con calidad decreciente hasta 2 MB (reglas en [PhotoCompressionPolicy]).
 *
 * @property context Contexto de aplicación para el `ContentResolver`.
 */
class AndroidPhotoCompressor(private val context: Context) : PhotoCompressor {
    @Suppress("TooGenericExceptionCaught", "SwallowedException")
    override suspend fun compressToJpeg(uri: String): ByteArray =
        withContext(Dispatchers.IO) {
            val parsed = Uri.parse(uri)
            if (!PhotoCompressionPolicy.isSupportedMime(context.contentResolver.getType(parsed))) {
                throw ProfileException(ProfileError.UnsupportedPhotoFormat)
            }
            try {
                val bitmap = decodeScaled(parsed)
                try {
                    PhotoCompressionPolicy.encodeWithinLimit { quality -> bitmap.toJpeg(quality) }
                } finally {
                    bitmap.recycle()
                }
            } catch (e: ProfileException) {
                throw e
            } catch (e: CancellationException) {
                throw e
            } catch (e: IOException) {
                throw ProfileException(ProfileError.ServerError)
            } catch (e: RuntimeException) {
                throw ProfileException(ProfileError.ServerError)
            }
        }

    private fun decodeScaled(uri: Uri): Bitmap {
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        openStream(uri).use { BitmapFactory.decodeStream(it, null, bounds) }
        if (bounds.outWidth <= 0 || bounds.outHeight <= 0) throw ProfileException(ProfileError.UnsupportedPhotoFormat)

        val options = BitmapFactory.Options().apply { inSampleSize = PhotoCompressionPolicy.sampleSize(bounds.outWidth, bounds.outHeight) }
        val decoded =
            openStream(uri).use { BitmapFactory.decodeStream(it, null, options) }
                ?: throw ProfileException(ProfileError.UnsupportedPhotoFormat)
        val (targetWidth, targetHeight) = PhotoCompressionPolicy.targetSize(bounds.outWidth, bounds.outHeight)
        val scaled = if (decoded.width > targetWidth) Bitmap.createScaledBitmap(decoded, targetWidth, targetHeight, true) else decoded
        if (scaled !== decoded) decoded.recycle()
        return rotateIfNeeded(uri, scaled)
    }

    private fun rotateIfNeeded(
        uri: Uri,
        bitmap: Bitmap,
    ): Bitmap {
        val degrees =
            runCatching {
                openStream(uri).use { stream ->
                    when (ExifInterface(stream).getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL)) {
                        ExifInterface.ORIENTATION_ROTATE_90 -> ROTATE_90
                        ExifInterface.ORIENTATION_ROTATE_180 -> ROTATE_180
                        ExifInterface.ORIENTATION_ROTATE_270 -> ROTATE_270
                        else -> 0f
                    }
                }
            }.getOrDefault(0f)
        if (degrees == 0f) return bitmap
        val rotated = Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, Matrix().apply { postRotate(degrees) }, true)
        if (rotated !== bitmap) bitmap.recycle()
        return rotated
    }

    private fun openStream(uri: Uri) = context.contentResolver.openInputStream(uri) ?: throw IOException("No se pudo abrir la imagen")

    private fun Bitmap.toJpeg(quality: Int): ByteArray =
        ByteArrayOutputStream().also { compress(Bitmap.CompressFormat.JPEG, quality, it) }.toByteArray()

    private companion object {
        const val ROTATE_90 = 90f
        const val ROTATE_180 = 180f
        const val ROTATE_270 = 270f
    }
}
