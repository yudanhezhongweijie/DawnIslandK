package com.laotoua.dawnislandk.ui.activity

import android.animation.ValueAnimator
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import android.graphics.Typeface
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.os.Handler
import android.view.WindowManager
import android.widget.SeekBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.afollestad.materialdialogs.MaterialDialog
import com.afollestad.materialdialogs.color.colorChooser
import com.afollestad.materialdialogs.customview.customView
import com.afollestad.materialdialogs.customview.getCustomView
import com.laotoua.dawnislandk.R
import com.laotoua.dawnislandk.databinding.ActivityDoodleBinding
import com.laotoua.dawnislandk.io.ImageUtil
import com.laotoua.dawnislandk.ui.util.LayoutUtil
import com.laotoua.dawnislandk.ui.util.ReadableTime
import com.laotoua.dawnislandk.ui.widget.DoodleView
import com.laotoua.dawnislandk.ui.widget.ThicknessPreviewView
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileDescriptor

/*
 * Copyright 2015 Hippo Seven
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

class DoodleActivity : AppCompatActivity(), DoodleView.Helper {
    private lateinit var binding: ActivityDoodleBinding
    private var mOutputFile: Uri? = null
    private var mExitWaitingDialog: MaterialDialog? = null
    private var mSideAnimator: ValueAnimator = ValueAnimator()
    private var mShowSide = true
    private var mHideSideRunnable: Runnable? = null
    private val handler = Handler()
    private var dialogThickness: MaterialDialog? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityDoodleBinding.inflate(layoutInflater)
        setContentView(binding.root)
        window.clearFlags(
            WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS
                    or WindowManager.LayoutParams.FLAG_TRANSLUCENT_NAVIGATION
        )

        val timeStamp: String = ReadableTime.getFilenamableTime(System.currentTimeMillis())
        val relativeLocation =
            Environment.DIRECTORY_PICTURES + File.separator + "Dawn"
        val name = "Doodle_$timeStamp"
        val ext = "png"
        mOutputFile = ImageUtil.addPlaceholderImageUriToGallery(this, name, ext, relativeLocation)

        binding.doodleView.setHelper(this)

        binding.side.setOnClickListener {
            hideSide()
        }

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
                val intent = Intent()
                intent.type = "image/*"
                intent.action = Intent.ACTION_GET_CONTENT
                startActivityForResult(
                    Intent.createChooser(
                        intent,
                        getString(R.string.select_picture)
                    ), REQUEST_CODE_SELECT_IMAGE
                )
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
            if (mOutputFile != null) {
                saveDoodle()
            } else {
                Toast.makeText(
                    this,
                    getString(R.string.cant_create_image_file),
                    Toast.LENGTH_SHORT
                ).show()
            }
        }

        binding.menu.setOnClickListener {
            if (mShowSide) {
                hideSide()
            } else {
                showSide()
            }
        }

        // TODO: fix below
        updateUndoRedo()

        if (mOutputFile == null) {
            Toast.makeText(
                this,
                getString(R.string.cant_create_image_file),
                Toast.LENGTH_SHORT
            ).show()
        }
        mSideAnimator.duration = 300
        mSideAnimator.addUpdateListener { animation ->
            binding.side.translationX =
                (animation.animatedValue as Float)
        }
        mHideSideRunnable = Runnable { hideSide() }
        handler.postDelayed(mHideSideRunnable!!, 3000)
    }

    override fun onBackPressed() {
        MaterialDialog(this).show {
            message(R.string.saving_image)
            positiveButton(R.string.save) {
                if (mOutputFile != null) {
                    saveDoodle()
                } else {
                    Toast.makeText(
                        this@DoodleActivity,
                        getString(R.string.cant_create_image_file),
                        Toast.LENGTH_SHORT
                    ).show()
                    finish()
                }
            }
            negativeButton(R.string.cancel, null)
            neutralButton(R.string.do_not_save) { finish() }
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQUEST_CODE_SELECT_IMAGE && resultCode == RESULT_OK) {
            val uri = data?.data ?: return
            try {
                contentResolver.openFileDescriptor(uri, "r")?.run {
                    val fileDescriptor: FileDescriptor = this.fileDescriptor
                    val bitmap: Bitmap =
                        BitmapFactory.decodeFileDescriptor(fileDescriptor)
                    binding.doodleView.insertBitmap(bitmap)
                    binding.image.isActivated = true
                    this.close()
                }

            } catch (e: OutOfMemoryError) {
                // Ignore
            }
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
            mSideAnimator.setFloatValues(binding.side.translationX, 0f)
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
            mSideAnimator.setFloatValues(binding.side.translationX, -binding.side.width.toFloat())
            mSideAnimator.start()
        }
    }

    private fun updateUndoRedo() {
        binding.undo.isEnabled = binding.doodleView.canUndo()
        binding.redo.isEnabled = binding.doodleView.canRedo()
    }

    private fun showPickColorDialog() {

        val colors = intArrayOf(
            binding.doodleView.paintColor,
            Color.BLACK,
            Color.DKGRAY,
            Color.RED,
            Color.GREEN,
            Color.BLUE,
            Color.YELLOW,
            Color.CYAN,
            Color.MAGENTA
        )
        MaterialDialog(this).show {
            title(R.string.pick_color)
            colorChooser(
                colors = colors,
//                subColors = subColors,
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
        if (dialogThickness == null) {
            dialogThickness = MaterialDialog(this).customView(R.layout.dialog_thickness)
            var thickness = binding.doodleView.paintThickness
            dialogThickness!!.getCustomView().apply {
                val previewView = findViewById<ThicknessPreviewView>(R.id.thicknessPreviewView)
                previewView.setColor(binding.doodleView.paintColor)
                previewView.setThickness(thickness)

                val thicknessTextView = findViewById<TextView>(R.id.thickness)
                thicknessTextView.text =
                    (LayoutUtil.pix2dp(context, thickness).toInt() + 1).toString()
                thicknessTextView.typeface = Typeface.DEFAULT_BOLD

                findViewById<SeekBar>(R.id.thicknessSlider).apply {
                    progress = LayoutUtil.pix2dp(context, thickness).toInt()
                    setOnSeekBarChangeListener(object :
                        SeekBar.OnSeekBarChangeListener {
                        override fun onProgressChanged(
                            seekBar: SeekBar?,
                            progress: Int,
                            fromUser: Boolean
                        ) {
                            thickness = LayoutUtil.dp2pix(context, progress.toFloat())
                            previewView.setThickness(thickness)
                            thicknessTextView.text = (progress + 1).toString()
                        }

                        override fun onStartTrackingTouch(seekBar: SeekBar?) {
                        }

                        override fun onStopTrackingTouch(seekBar: SeekBar?) {
                        }
                    })
                }
            }
            dialogThickness!!.positiveButton(R.string.submit) {
                binding.doodleView.paintThickness = thickness
            }
        }
        dialogThickness!!.show()
    }

    private fun saveDoodle() {
        if (mExitWaitingDialog == null) {

            mExitWaitingDialog = MaterialDialog(this).apply {
                customView(R.layout.dialog_progress)
                cancelable(false)
                title(R.string.saving_image)
            }
            mExitWaitingDialog!!.show()
            lifecycleScope.launch {
                binding.doodleView.save(this@DoodleActivity, mOutputFile!!)
            }
        }
        // Wait here, it might be fast click back button
    }

    override fun onStoreChange(view: DoodleView?) {
        updateUndoRedo()
    }

    override fun onSavingFinished(ok: Boolean) {
        if (mExitWaitingDialog != null) {
            mExitWaitingDialog!!.dismiss()
            mExitWaitingDialog = null
        }
        val intent = Intent()
        intent.data = mOutputFile
        setResult(RESULT_OK, intent)
        finish()
    }

    companion object {
        const val REQUEST_CODE_SELECT_IMAGE = 0
    }
}