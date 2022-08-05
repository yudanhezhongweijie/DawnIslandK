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
import android.widget.Button
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import com.google.zxing.Result
import com.king.zxing.CaptureActivity
import com.king.zxing.DecodeConfig
import com.king.zxing.DecodeFormatManager
import com.king.zxing.analyze.MultiFormatAnalyzer
import com.king.zxing.util.CodeUtils
import com.king.zxing.util.LogUtils
import com.laotoua.dawnislandk.R
import com.laotoua.dawnislandk.screens.util.ToolBar.themeStatusBar
import com.laotoua.dawnislandk.util.ImageUtil
import com.laotoua.dawnislandk.util.IntentsHelper.Companion.CAMERA_SCAN_RESULT
import timber.log.Timber


class QRCookieActivity : CaptureActivity() {

    override fun getLayoutId(): Int {
        return R.layout.activity_qrcookie
    }

    private val getCookieImage =
        registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
            uri?.run {
                try {
                    val file =
                        ImageUtil.getImageFileFromUri(this@QRCookieActivity, uri)
                            ?: return@registerForActivityResult
                    val res = CodeUtils.parseQRCode(file.path)
                    if (res != null) {
                        val intent = Intent()
                        intent.putExtra(CAMERA_SCAN_RESULT, res)
                        setResult(Activity.RESULT_OK, intent)
                        finish()
                    } else {
                        Toast.makeText(
                            this@QRCookieActivity,
                            R.string.did_not_get_cookie_from_image,
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                } catch (e: Exception) {
                    // Ignore
                    Timber.e(e)
                }
            }
        }

    override fun onCreate(icicle: Bundle?) {
        super.onCreate(icicle)

        findViewById<Button>(R.id.selectFromGallery).setOnClickListener {
            getCookieImage.launch("image/*")
        }

        LogUtils.setPriority(LogUtils.ERROR)

        themeStatusBar()

        //初始化解码配置
        val decodeConfig = DecodeConfig()
        decodeConfig.setHints(DecodeFormatManager.QR_CODE_HINTS) //如果只有识别二维码的需求，这样设置效率会更高，不设置默认为DecodeFormatManager.DEFAULT_HINTS
            .setFullAreaScan(false) //设置是否全区域识别，默认false
            .setAreaRectRatio(0.8f) //设置识别区域比例，默认0.8，设置的比例最终会在预览区域裁剪基于此比例的一个矩形进行扫码识别
            .setAreaRectVerticalOffset(0).areaRectHorizontalOffset = 0 //设置识别区域水平方向偏移量，默认为0，为0表示居中，可以为负数

        //在启动预览之前，设置分析器，只识别二维码
        cameraScan
            .setVibrate(true) //设置是否震动，默认为false
            .setAnalyzer(MultiFormatAnalyzer(decodeConfig)) //设置分析器,如果内置实现的一些分析器不满足您的需求，你也可以自定义去实现

    }

    override fun onScanResultCallback(result: Result?): Boolean {
        val intent = Intent()
        if (result != null) {
            intent.putExtra(CAMERA_SCAN_RESULT, result.text)
        }
        setResult(Activity.RESULT_OK, intent)
        finish()
        return true
    }
}