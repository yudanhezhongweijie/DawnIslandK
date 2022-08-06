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

import android.animation.ValueAnimator
import android.annotation.SuppressLint
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.graphics.ColorUtils
import androidx.lifecycle.lifecycleScope
import com.afollestad.materialdialogs.MaterialDialog
import com.afollestad.materialdialogs.WhichButton
import com.afollestad.materialdialogs.actions.getActionButton
import com.afollestad.materialdialogs.color.colorChooser
import com.afollestad.materialdialogs.customview.customView
import com.afollestad.materialdialogs.lifecycle.lifecycleOwner
import com.google.android.material.slider.Slider
import com.laotoua.dawnislandk.R
import com.laotoua.dawnislandk.databinding.ActivityDoodleBinding
import com.laotoua.dawnislandk.screens.util.Layout
import com.laotoua.dawnislandk.screens.util.ToolBar.themeStatusBar
import com.laotoua.dawnislandk.screens.widgets.DoodleView
import com.laotoua.dawnislandk.screens.widgets.ThicknessPreviewView
import com.laotoua.dawnislandk.util.lazyOnMainOnly
import kotlinx.coroutines.launch
import timber.log.Timber
import java.io.FileDescriptor

class DoodleActivity : AppCompatActivity(), DoodleView.Helper {
    private lateinit var binding: ActivityDoodleBinding
    private var mExitWaitingDialog: MaterialDialog? = null
    private var mSideAnimator: ValueAnimator = ValueAnimator()
    private var mShowSide = true
    private var mHideSideRunnable: Runnable? = null
    private val handler = Handler(Looper.getMainLooper())

    private val getImageBackground =
        registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
            // Handle the returned Uri
            uri?.run {
                try {
                    contentResolver.openFileDescriptor(this, "r")?.run {
                        val fileDescriptor: FileDescriptor = this.fileDescriptor
                        val bitmap: Bitmap =
                            BitmapFactory.decodeFileDescriptor(fileDescriptor)
                        binding.doodleView.insertBitmap(bitmap)
                        binding.image.isActivated = true
                        this.close()
                    }

                } catch (e: Exception) {
                    // Ignore
                    Timber.e(e)
                }
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityDoodleBinding.inflate(layoutInflater)
        setContentView(binding.root)

        themeStatusBar()
        binding.doodleView.setHelper(this)

        binding.palette.setOnClickListener {
            showPickColorDialog()
        }

        binding.thickness.setOnClickListener {
            showThicknessDialog()
        }

        binding.drawAction.setOnClickListener {
            binding.drawAction.apply {
                isActivated = isActivated.not()
                if (!isActivated) setImageResource(R.drawable.ic_brush_black_24px)
                else setImageResource(R.drawable.ic_eraser_24px)
                binding.doodleView.setEraser(isActivated)
            }
        }

        binding.image.setOnClickListener {
            if (binding.doodleView.hasInsertBitmap()) {
                binding.doodleView.insertBitmap(null)
            } else {
                getImageBackground.launch("image/*")
            }
            binding.image.apply {
                isActivated = !isActivated
                if (!isActivated) setImageResource(R.drawable.ic_insert_image_24px)
                else setImageResource(R.drawable.ic_remove_image_24px)
            }
        }

        binding.undo.setOnClickListener {
            binding.doodleView.undo()
            updateUndoRedo()
        }
        binding.redo.setOnClickListener {
            binding.doodleView.redo()
            updateUndoRedo()
        }
        binding.clear.setOnClickListener {
            binding.doodleView.clear()
        }
        binding.ok.setOnClickListener {
            saveDoodle()
        }

        binding.menu.setOnClickListener {
            if (mShowSide) {
                hideSide()
            } else {
                showSide()
            }
        }

        updateUndoRedo()

        mSideAnimator.duration = 300
        mSideAnimator.addUpdateListener { animation ->
            binding.sideMenu.translationX =
                (animation.animatedValue as Float)
        }
        mHideSideRunnable = Runnable { hideSide() }
        handler.postDelayed(mHideSideRunnable!!, 3000)
    }

    override fun onBackPressed() {
        MaterialDialog(this).show {
            lifecycleOwner(this@DoodleActivity)
            message(R.string.save_image_selection)
            positiveButton(R.string.save) {
                saveDoodle()
            }
            negativeButton(R.string.cancel)
            @Suppress("DEPRECATION")
            neutralButton(R.string.do_not_save) {
                finish()
            }
            getActionButton(WhichButton.NEUTRAL).updateTextColor(Color.RED)
        }
    }

    private fun showSide() {
        if (mHideSideRunnable != null) {
            handler.removeCallbacks(mHideSideRunnable!!)
            mHideSideRunnable = null
        }
        if (!mShowSide) {
            mShowSide = true
            mSideAnimator.cancel()
            mSideAnimator.setFloatValues(binding.sideMenu.translationX, 0f)
            mSideAnimator.start()
        }
    }

    private fun hideSide() {
        if (mHideSideRunnable != null) {
            handler.removeCallbacks(mHideSideRunnable!!)
            mHideSideRunnable = null
        }
        if (mShowSide) {
            mShowSide = false
            mSideAnimator.cancel()
            mSideAnimator.setFloatValues(
                binding.sideMenu.translationX,
                -binding.sideMenu.width.toFloat()
            )
            mSideAnimator.start()
        }
    }

    private fun updateUndoRedo() {
        binding.undo.apply {
            isEnabled = binding.doodleView.canUndo()
            alpha = if (!isEnabled) {
                0.2f
            } else {
                1f
            }
        }

        binding.redo.apply {
            isEnabled = binding.doodleView.canRedo()
            alpha = if (!isEnabled) {
                0.2f
            } else {
                1f
            }
        }
    }

    private val defaultColors by lazyOnMainOnly {
        arrayListOf(
            binding.doodleView.paintColor,
            Color.WHITE,
            Color.BLACK,
            Color.GRAY,
            ColorUtils.compositeColors(getColor(R.color.red_500), Color.WHITE),
            ColorUtils.compositeColors(getColor(R.color.pink_500), Color.WHITE),
            ColorUtils.compositeColors(getColor(R.color.purple_500), Color.WHITE),
            ColorUtils.compositeColors(getColor(R.color.indigo_500), Color.WHITE),
            ColorUtils.compositeColors(getColor(R.color.blue_500), Color.WHITE),
            ColorUtils.compositeColors(getColor(R.color.light_blue_500), Color.WHITE),
            ColorUtils.compositeColors(getColor(R.color.cyan_500), Color.WHITE),
            ColorUtils.compositeColors(getColor(R.color.green_500), Color.WHITE),
            ColorUtils.compositeColors(getColor(R.color.green_ntr), Color.WHITE),
            ColorUtils.compositeColors(getColor(R.color.light_green_500), Color.WHITE),
            ColorUtils.compositeColors(getColor(R.color.lime_500), Color.WHITE),
            ColorUtils.compositeColors(getColor(R.color.yellow_500), Color.WHITE),
            ColorUtils.compositeColors(getColor(R.color.amber_500), Color.WHITE),
            ColorUtils.compositeColors(getColor(R.color.orange_500), Color.WHITE),
            ColorUtils.compositeColors(getColor(R.color.deep_orange_500), Color.WHITE)
        )
    }

    @SuppressLint("CheckResult")
    private fun showPickColorDialog() {
        defaultColors[0] = binding.doodleView.paintColor
        MaterialDialog(this).show {
            lifecycleOwner(this@DoodleActivity)
            title(R.string.pick_color)
            colorChooser(
                colors = defaultColors.toIntArray(),
                allowCustomArgb = true,
                initialSelection = binding.doodleView.paintColor
            ) { _, color ->
                binding.doodleView.paintColor = color
            }
            positiveButton(R.string.submit)
            negativeButton(R.string.cancel)
        }
    }

    private fun showThicknessDialog() {
        MaterialDialog(this).show {
            lifecycleOwner(this@DoodleActivity)
            customView(R.layout.dialog_thickness)
            var thickness = binding.doodleView.paintThickness
            val previewView = findViewById<ThicknessPreviewView>(R.id.thicknessPreviewView)
            previewView.setColor(binding.doodleView.paintColor)
            previewView.setThickness(thickness)

            findViewById<Slider>(R.id.thicknessSlider).addOnChangeListener { _, value, _ ->
                thickness = Layout.dp2pix(context, value)
                previewView.setThickness(thickness)
            }
            positiveButton(R.string.submit) {
                binding.doodleView.paintThickness = thickness
            }
        }
    }

    private fun saveDoodle() {
        if (mExitWaitingDialog == null) {
            mExitWaitingDialog = MaterialDialog(this).apply {
                lifecycleOwner(this@DoodleActivity)
                customView(R.layout.widget_loading)
                cancelable(false)
                title(R.string.saving_image)
            }
            mExitWaitingDialog!!.show()
            lifecycleScope.launch {
                binding.doodleView.save(this@DoodleActivity)
            }
        }
        // Wait here, it might be fast click back button
    }

    override fun onStoreChange(view: DoodleView?) {
        updateUndoRedo()
    }

    override fun onSavingFinished(savedUri: Uri?) {
        if (mExitWaitingDialog != null) {
            mExitWaitingDialog!!.dismiss()
            mExitWaitingDialog = null
        }
        val intent = Intent()
        intent.data = savedUri
        setResult(if (savedUri != null) RESULT_OK else RESULT_CANCELED, intent)
        finish()
    }

}