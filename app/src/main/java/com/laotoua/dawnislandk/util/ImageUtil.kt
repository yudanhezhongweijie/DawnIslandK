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
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import android.provider.OpenableColumns
import android.util.Size
import android.widget.ImageView
import androidx.fragment.app.FragmentActivity
import id.zelory.compressor.Compressor
import id.zelory.compressor.constraint.format
import id.zelory.compressor.constraint.quality
import id.zelory.compressor.constraint.size
import timber.log.Timber
import java.io.*
import java.util.*


object ImageUtil {

    fun isImageInGallery(caller: FragmentActivity, fileName: String): Boolean {
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
        return res
    }

    private fun copyImageFromFileToUri(caller: FragmentActivity, uri: Uri, file: File): Boolean {
        return try {
            val inputStream = FileInputStream(file)
            val outputStream = caller.contentResolver.openOutputStream(uri)
                ?: throw IOException("Failed to get output stream.")
            inputStream.copyTo(outputStream)
            inputStream.close()
            outputStream.close()
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

    fun getImageFileFromUri(caller: FragmentActivity, uri: Uri): File? {
        return try {
            caller.contentResolver.openFileDescriptor(uri, "r", null)?.use { pfd ->
                val inputStream = FileInputStream(pfd.fileDescriptor)
                val file = File(caller.cacheDir, getFileName(caller, uri))
                if (file.exists()) {
                    Timber.d("File exists in cache. Reusing...")
                } else {
                    // TODO: rethink cache management
//                    val size: Long = getDirSize(caller.cacheDir)
//                    if (size + pfd.statSize > DawnConstants.CACHE_FILE_LIMIT) {
//                        Timber.d("Adding ${pfd.statSize} to cache. New size ${size + pfd.statSize} exceeds limit ${DawnConstants.CACHE_FILE_LIMIT}.")
//                        deleteBytesInDir(
//                            caller.cacheDir,
//                            pfd.statSize + size - DawnConstants.CACHE_FILE_LIMIT
//                        )
//                    }
                    Timber.d("File not found in cache. Copying...")
                    val outputStream = FileOutputStream(file)
                    inputStream.copyTo(outputStream)
                    inputStream.close()
                    outputStream.close()
                }
                file
            }
        } catch (e: Exception) {
            Timber.e(e, "Error in copying file $uri to cache!")
            null
        }
    }

    // clear enough spaces for new bytes added to cache
    // currently only deletes compressor files & cached images for uploads
    private fun deleteBytesInDir(dir: File, bytes: Long): Long {
        if (dir.isFile) throw Exception("Expect $dir to be a directory, however got a file.")
        var bytesDeleted: Long = 0
        val files = dir.listFiles() ?: emptyArray()
        for (file in files) {
            if (bytesDeleted >= bytes) {
                break
            } else if (file.isFile) {
                bytesDeleted += file.length()
                Timber.d("Deleting file ${file.name} with ${file.length()} bytes")
                file.delete()
            } else if (file.isDirectory && file.name == "compressor") {
                Timber.d("Found a directory in $dir. Clearing ${file.name}")
                bytesDeleted += deleteBytesInDir(file, bytes - bytesDeleted)
            }
        }
        Timber.d("Cleared $bytesDeleted bytes in $dir.")
        return bytesDeleted
    }

    private fun getDirSize(dir: File): Long {
        var size: Long = 0
        val files = dir.listFiles() ?: emptyArray()
        for (file in files) {
            if (file.isFile) {
                size += file.length()
            } else if (file.isDirectory && file.name == "compressor") {
                size += getDirSize(file)
            }
        }
        return size
    }

    suspend fun getCompressedImageFileFromUri(caller: FragmentActivity, uri: Uri): File? {
        val source = getImageFileFromUri(caller, uri)
        if (source == null || source.extension.toLowerCase(Locale.getDefault()) == "gif") {
            Timber.e("Did not get file from uri. Cannot compress...")
            return null
        }
        if (source.exists() && source.length() < DawnConstants.SERVER_FILE_SIZE_LIMIT) {
            Timber.d("File exists and under server size limit. Reusing the old file")
            return source
        }
        return Compressor.compress(caller, source) {
            quality(80)
            format(Bitmap.CompressFormat.JPEG)
            size(DawnConstants.SERVER_FILE_SIZE_LIMIT) // 2 MB
        }
    }

    fun getFileFromDrawable(caller: FragmentActivity, fileName: String, resId: Int): File? {
        return try {
            val file = File(caller.cacheDir, "$fileName.png")
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
            file
        } catch (e:Exception){
            Timber.e(e, "Did not get drawable file $fileName")
            null
        }
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