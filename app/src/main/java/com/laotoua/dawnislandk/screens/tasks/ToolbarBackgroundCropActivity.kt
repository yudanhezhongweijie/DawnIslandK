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
import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.net.toUri
import com.laotoua.dawnislandk.R
import com.laotoua.dawnislandk.util.IntentUtil
import com.yalantis.ucrop.UCrop
import com.yalantis.ucrop.model.AspectRatio
import timber.log.Timber
import java.io.File


class ToolbarBackgroundCropActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        IntentUtil.getImageFromGallery(this,"image/*"){ uri: Uri? ->
            if (uri == null) {
                Toast.makeText(this, R.string.cannot_load_image_file, Toast.LENGTH_SHORT)
                    .show()
            } else {
                try {
                    val file = File(this.filesDir, "toolbar_bg.png")
                    val width = intent.getFloatExtra("w", 0f)
                    val height = intent.getFloatExtra("h", 0f)
                    val options = UCrop.Options()
                    options.setFreeStyleCropEnabled(true)
                    options.setAspectRatioOptions(0, AspectRatio("默认", width, height))
                    UCrop.of(uri, file.toUri())
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
        if (resultCode == Activity.RESULT_OK && requestCode == UCrop.REQUEST_CROP) {
            val intent = Intent()
            val resultUri = UCrop.getOutput(data!!)
            intent.data = resultUri
            setResult(RESULT_OK, intent)
            finish()
        } else if (resultCode == UCrop.RESULT_ERROR) {
            val cropError = UCrop.getError(data!!)
            Timber.e(cropError)
            val intent = Intent()
            setResult(RESULT_CANCELED, intent)
            finish()
        }
    }
}