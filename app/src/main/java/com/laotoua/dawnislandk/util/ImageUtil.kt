/*
 *  Copyright 2020 Fishballzzz
 *  *
 *  * Licensed under the Apache License, Version 2.0 (the "License");
 *  * you may not use this file except in compliance with the License.
 *  * You may obtain a copy of the License at
 *  *
 *  *     http://www.apache.org/licenses/LICENSE-2.0
 *  *
 *  * Unless required by applicable law or agreed to in writing, software
 *  * distributed under the License is distributed on an "AS IS" BASIS,
 *  * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  * See the License for the specific language governing permissions and
 *  * limitations under the License.
 *
 */

package com.laotoua.dawnislandk.util

import android.content.ContentValues
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.provider.MediaStore
import android.provider.OpenableColumns
import android.util.Size
import android.widget.ImageView
import android.widget.Toast
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.lifecycleScope
import com.laotoua.dawnislandk.R
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import timber.log.Timber
import java.io.*


object ImageUtil {

    private val cachedImages = mutableSetOf<String>()
    fun isImageInGallery(caller: FragmentActivity, fileName: String): Boolean {
        if (cachedImages.contains(fileName)) return true
        val selection = "${MediaStore.Images.ImageColumns.DISPLAY_NAME}=?"
        val selectionArgs = arrayOf(fileName)
        val projection = arrayOf(MediaStore.Images.ImageColumns.DISPLAY_NAME)
        var res = false
        caller.contentResolver.query(
            MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
            projection,
            selection,
            selectionArgs,
            null
        )?.use { cursor -> res = (cursor.count > 0) }

        if (res) cachedImages.add(fileName)
        return res
    }

    private fun copyImageFromFileToUri(caller: FragmentActivity, uri: Uri, file: File): Boolean {
        return try {
            val stream = caller.contentResolver.openOutputStream(uri)
                ?: throw IOException("Failed to get output stream.")
            stream.write(file.readBytes())
            stream.close()
            true
        } catch (e: Exception) {
            Timber.e(e)
            false
        }
    }

    fun copyImageFileToGallery(
        caller: FragmentActivity,
        fileName: String,
        relativeLocation: String,
        file: File
    ): Boolean {
        return try {
            val uri = addPlaceholderImageToGallery(caller, fileName, relativeLocation)
            try {
                copyImageFromFileToUri(caller, uri, file)
            } catch (writeException: Exception) {
                Timber.e(writeException)
                removePlaceholderImageInGallery(caller, uri)
                false
            }
        } catch (uriException: Exception) {
            Timber.e(uriException)
            false
        }
    }

    fun writeBitmapToGallery(
        caller: FragmentActivity,
        fileName: String,
        relativeLocation: String,
        bitmap: Bitmap
    ): Uri? {
        return try {
            val uri = addPlaceholderImageToGallery(caller, fileName, relativeLocation)
            try {
                caller.contentResolver.openOutputStream(uri)?.use {
                    bitmap.compress(Bitmap.CompressFormat.PNG, 100, it)
                }
                uri
            } catch (e: Exception) {
                removePlaceholderImageInGallery(caller, uri)
                null
            }
        } catch (e: FileNotFoundException) {
            null
        }
    }

    fun addPlaceholderImageToGallery(
        caller: FragmentActivity,
        fileName: String,
        relativeLocation: String
    ): Uri {
        val contentValues = ContentValues().apply {
            val mimeType = fileName.substringAfterLast(".")
            put(MediaStore.Images.ImageColumns.DISPLAY_NAME, fileName)
            put(MediaStore.MediaColumns.MIME_TYPE, "image/$mimeType")
            // without this part causes "Failed to create new MediaStore record" exception to be invoked (uri is null below)
            // https://stackoverflow.com/questions/56904485/how-to-save-an-image-in-android-q-using-mediastore
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                put(MediaStore.Images.ImageColumns.RELATIVE_PATH, relativeLocation)
            }
        }

        return caller.contentResolver.insert(
            MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
            contentValues
        ) ?: throw IOException("Failed to create new MediaStore record.")
    }

    fun removePlaceholderImageInGallery(caller: FragmentActivity, uri: Uri): Int {
        return caller.contentResolver.delete(uri, null, null)
    }

    fun loadImageThumbnailToImageView(
        caller: FragmentActivity,
        uri: Uri,
        width: Int,
        height: Int,
        imageView: ImageView
    ) {
        if (Build.VERSION.SDK_INT >= 29) {
            caller.contentResolver.loadThumbnail(uri, Size(width, height), null)
                .run { imageView.setImageBitmap(this) }
        } else {
            GlideApp.with(imageView).load(uri).override(width, height).into(imageView)
        }
    }

    // TODO: combine image compressing and image file return
    // image file should only return valid file if compressing is successful
    // TODO: combine get image file & load image preview
    fun getImageFileFromUri(caller: FragmentActivity, uri: Uri): File? {
        caller.contentResolver.openFileDescriptor(uri, "r", null)?.use { pfd ->
            val filename = getFileName(caller, uri)
            val file = File(caller.cacheDir, filename)
            if (file.exists()) {
                Timber.d("File exists. Reusing the old file")
                return file
            }
            Timber.d("File not found. Making a new one...")
            if (pfd.statSize >= DawnConstants.SERVER_FILE_SIZE_LIMIT) {
                Timber.d("Image is oversize: ${pfd.statSize}. Compressing...")
                val ratio = (DawnConstants.SERVER_FILE_SIZE_LIMIT * 100 / pfd.statSize).toInt()
                Toast.makeText(
                    caller,
                    R.string.compressing_oversize_image,
                    Toast.LENGTH_SHORT
                )
                    .show()
                compressImage(caller, ratio, pfd.fileDescriptor)
            } else {
                FileInputStream(pfd.fileDescriptor)
            }.use { inputStream ->
                FileOutputStream(file).use { outputStream ->
                    inputStream.copyTo(outputStream)
                }
            }
            return file
        }
        return null

    }

    // compression runs on a different thread
    private fun compressImage(
        caller: FragmentActivity,
        ratio: Int,
        fileDescriptor: FileDescriptor
    ): InputStream {
        val pipedInputStream = PipedInputStream()
        PipedOutputStream(pipedInputStream).use {
            caller.lifecycleScope.launch(Dispatchers.IO) {
                try {
                    val bmp = BitmapFactory.decodeFileDescriptor(fileDescriptor)
                    bmp.compress(Bitmap.CompressFormat.JPEG, ratio, it)
                } catch (e:Exception){
                    Handler(Looper.getMainLooper()).post {
                        Toast.makeText(caller, "压缩图片时发生错误", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }
        return pipedInputStream
    }

    fun getFileFromDrawable(caller: FragmentActivity, fileName: String, resId: Int): File {
        val file = File(
            caller.cacheDir,
            "$fileName.png"
        )
        if (file.exists()) {
            Timber.i("File exists..Reusing the old file")
            return file
        }
        Timber.i("File not found. Making a new one...")
        val inputStream: InputStream = caller.resources.openRawResource(resId)

        val outputStream = FileOutputStream(file)
        inputStream.copyTo(outputStream)
        inputStream.close()
        outputStream.close()
        return file
    }

    private fun getFileName(caller: FragmentActivity, fileUri: Uri): String {
        var name = ""
        val projection = arrayOf(MediaStore.Images.ImageColumns.DISPLAY_NAME)
        caller.contentResolver.query(fileUri, projection, null, null, null)?.use {
            it.moveToFirst()
            name = it.getString(it.getColumnIndex(OpenableColumns.DISPLAY_NAME))
        }
        return name
    }
}