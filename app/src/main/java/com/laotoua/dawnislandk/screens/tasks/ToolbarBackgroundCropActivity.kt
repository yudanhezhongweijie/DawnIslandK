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

package com.laotoua.dawnislandk.screens.tasks

import android.app.Activity
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.net.toUri
import com.laotoua.dawnislandk.R
import com.laotoua.dawnislandk.util.DawnConstants
import com.laotoua.dawnislandk.util.IntentUtil
import com.yalantis.ucrop.UCrop
import com.yalantis.ucrop.model.AspectRatio
import timber.log.Timber
import java.io.File


class ToolbarBackgroundCropActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        IntentUtil.getImageFromGallery(this,"image/*"){ source: Uri? ->
            if (source == null) {
                val intent = Intent()
                setResult(RESULT_CANCELED, intent)
                finish()
            } else {
                try {
                    val width = intent.getFloatExtra("w", 0f)
                    val height = intent.getFloatExtra("h", 0f)
                    val options = UCrop.Options()
                    options.setFreeStyleCropEnabled(true)
                    options.setAspectRatioOptions(0, AspectRatio("默认", width, height))
                    options.setCompressionFormat(Bitmap.CompressFormat.JPEG)
                    UCrop.of(source, File(this.filesDir, DawnConstants.DEFAULT_TOOLBAR_IMAGE_NAME).toUri())
                        .withOptions(options)
                        .start(this)
                } catch (e: Exception) {
                    Toast.makeText(
                        this,
                        "${resources.getString(R.string.something_went_wrong)}\n$e",
                        Toast.LENGTH_SHORT
                    ).show()
                    Timber.e(e)
                    val intent = Intent()
                    setResult(RESULT_CANCELED, intent)
                    finish()
                }
            }
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == UCrop.REQUEST_CROP && resultCode == Activity.RESULT_OK) {
            val intent = Intent()
            val resultUri = UCrop.getOutput(data!!)
            intent.data = resultUri
            setResult(RESULT_OK, intent)
            finish()
        } else if (requestCode == UCrop.REQUEST_CROP &&
            (resultCode == UCrop.RESULT_ERROR || resultCode == Activity.RESULT_CANCELED)) {
            if (data != null ){
                Timber.e(UCrop.getError(data))
            }
            val intent = Intent()
            setResult(RESULT_CANCELED, intent)
            finish()
        } else {
            Timber.e("unhandled intent $requestCode $resultCode $data")
        }
    }
}