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

import android.app.Activity
import android.content.ContentResolver
import android.content.ContentValues
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import android.provider.OpenableColumns
import android.util.Size
import android.widget.ImageView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.lifecycleScope
import com.laotoua.dawnislandk.R
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import timber.log.Timber
import java.io.*


object ImageUtil {

    private val cachedImages = mutableSetOf<String>()
    fun imageExistInGalleryBasedOnFilenameAndExt(
        callerActivity: Activity,
        fileName: String,
        relativeLocation: String
    ): Boolean {
        if (cachedImages.contains(fileName)) return true
        val selection = "${MediaStore.Images.ImageColumns.DISPLAY_NAME}=?"
        val selectionArgs = arrayOf(fileName)
        val projection = arrayOf(MediaStore.Images.ImageColumns.DISPLAY_NAME)
        var res = false
        callerActivity.contentResolver.query(
            MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
            projection,
            selection,
            selectionArgs,
            null
        )?.use { cursor -> res = (cursor.count > 0) }

        if (res) cachedImages.add(fileName)
        return res
    }

    private fun copyFromFileToImageUri(callerActivity: Activity, uri: Uri, file: File): Boolean {
        try {
            val stream = callerActivity.contentResolver.openOutputStream(uri)
                ?: throw IOException("Failed to get output stream.")
            stream.write(file.readBytes())
            stream.close()
        } catch (e: Exception) {
            Timber.e(e)
            return false
        }
        return true
    }

    fun copyImageFileToGallery(
        callerActivity: Activity,
        fileName: String,
        relativeLocation: String,
        file: File
    ): Boolean {
        try {
            val uri = addPlaceholderImageUriToGallery(
                callerActivity,
                fileName,
                relativeLocation
            )
            return try {
                copyFromFileToImageUri(
                    callerActivity,
                    uri,
                    file
                )
            } catch (writeException: Exception) {
                Timber.e(writeException)
                removePlaceholderImageUriToGallery(callerActivity, uri)
                false
            }
        } catch (uriException: Exception) {
            Timber.e(uriException)
            return false
        }
    }

    fun writeBitmapToGallery(
        callerActivity: Activity, fileName: String, relativeLocation: String,
        bitmap: Bitmap
    ): Uri? {
        return try {
            val uri = addPlaceholderImageUriToGallery(
                callerActivity,
                fileName,
                relativeLocation
            )
            try {
                callerActivity.contentResolver.openOutputStream(uri)?.use {
                    bitmap.compress(Bitmap.CompressFormat.PNG, 100, it)
                }
                uri
            } catch (e: Exception) {
                removePlaceholderImageUriToGallery(callerActivity, uri)
                null
            }
        } catch (e: FileNotFoundException) {
            null
        }
    }

    fun addPlaceholderImageUriToGallery(
        callerActivity: Activity,
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

        return callerActivity.contentResolver.insert(
            MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
            contentValues
        ) ?: throw IOException("Failed to create new MediaStore record.")
    }

    fun removePlaceholderImageUriToGallery(
        callerActivity: Activity,
        uri: Uri
    ): Int {
        return callerActivity.contentResolver.delete(uri, null, null)
    }

    fun loadImageThumbnailToImageView(
        caller: Fragment,
        uri: Uri,
        width: Int,
        height: Int,
        imageView: ImageView
    ) {
        if (Build.VERSION.SDK_INT >= 29) {
            caller.requireActivity().contentResolver
                .loadThumbnail(uri, Size(width, height), null)
                .run { imageView.setImageBitmap(this) }
        } else {
            GlideApp.with(imageView)
                .load(uri).override(width, height).into(imageView)
        }
    }

    fun getImageFileFromUri(
        fragment: Fragment? = null,
        activity: Activity? = null,
        uri: Uri
    ): File? {
        val callerActivity = fragment?.requireActivity() ?: activity!!
        callerActivity.contentResolver.openFileDescriptor(uri, "r", null)?.use { pfd ->
            val filename = callerActivity.contentResolver.getFileName(uri)
            val file = File(callerActivity.cacheDir, filename)
            if (file.exists()) {
                Timber.i("File exists. Reusing the old file")
                return file
            }
            Timber.d("File not found. Making a new one...")
            // TODO: image compression may cause lag on main thread
            if (pfd.statSize >= DawnConstants.SERVER_FILE_SIZE_LIMIT) {
                Timber.d("Image is oversize: ${pfd.statSize}. Compressing...")
                val ratio = (DawnConstants.SERVER_FILE_SIZE_LIMIT * 100 / pfd.statSize).toInt()
                Toast.makeText(callerActivity, R.string.compressed_oversize_image, Toast.LENGTH_SHORT)
                    .show()
                compressImage(callerActivity, ratio, pfd.fileDescriptor)
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
    private fun compressImage(callerActivity: Activity, ratio: Int, fileDescriptor: FileDescriptor): InputStream {
        val pipedInputStream = PipedInputStream()
        (callerActivity as LifecycleOwner).lifecycleScope.launch(Dispatchers.IO) {
            try {
                PipedOutputStream(pipedInputStream).use {
                    val bmp = BitmapFactory.decodeFileDescriptor(fileDescriptor)
                    bmp.compress(Bitmap.CompressFormat.JPEG, ratio, it)
                }
            } catch (e: IOException) {
                // logging and exception handling should go here
                Timber.e(e, "Failed to compress image")
            }
        }
        return pipedInputStream
    }

    fun getFileFromDrawable(caller: Fragment, fileName: String, resId: Int): File {
        val file = File(
            caller.requireContext().cacheDir,
            "$fileName.png"
        )
        if (file.exists()) {
            Timber.i("File exists..Reusing the old file")
            return file
        }
        Timber.i("File not found. Making a new one...")
        val inputStream: InputStream = caller.requireContext().resources.openRawResource(resId)

        val outputStream = FileOutputStream(file)
        inputStream.copyTo(outputStream)
        inputStream.close()
        outputStream.close()
        return file
    }

    private fun ContentResolver.getFileName(fileUri: Uri): String {
        var name = ""
        val projection = arrayOf(MediaStore.Images.ImageColumns.DISPLAY_NAME)
        this.query(fileUri, projection, null, null, null)?.use {
            it.moveToFirst()
            name = it.getString(it.getColumnIndex(OpenableColumns.DISPLAY_NAME))
        }
        return name
    }
}