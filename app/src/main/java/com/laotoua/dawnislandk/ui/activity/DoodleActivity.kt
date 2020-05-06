package com.laotoua.dawnislandk.ui.activity

//import android.R
//import com.hippo.app.ProgressDialogBuilder
//import com.hippo.io.UriInputStreamPipe
//import com.hippo.nimingban.NMBAppConfig
//import com.hippo.nimingban.R
//import com.hippo.nimingban.util.BitmapUtils
//import com.hippo.nimingban.util.ReadableTime
//import com.hippo.nimingban.util.Settings
//import com.hippo.nimingban.widget.ColorPickerView
//import com.hippo.nimingban.widget.DoodleView
//import com.hippo.nimingban.widget.ThicknessPreviewView
//import com.hippo.ripple.Ripple
//import com.hippo.util.AnimationUtils2
//import com.hippo.util.DrawableManager
//import com.hippo.widget.Slider
//import com.hippo.yorozuya.LayoutUtils
//import com.hippo.yorozuya.ResourcesUtils
//import com.hippo.yorozuya.SimpleHandler
import android.animation.ValueAnimator
import android.app.Dialog
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.os.Handler
import android.view.WindowManager
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.afollestad.materialdialogs.MaterialDialog
import com.afollestad.materialdialogs.color.colorChooser
import com.laotoua.dawnislandk.R
import com.laotoua.dawnislandk.databinding.ActivityDoodleBinding
import com.laotoua.dawnislandk.io.ImageUtil
import com.laotoua.dawnislandk.ui.util.ReadableTime
import com.laotoua.dawnislandk.ui.widget.DoodleView
import timber.log.Timber
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
    private var mExitWaitingDialog: Dialog? = null
    private var mSideAnimator: ValueAnimator = ValueAnimator()
    private var mShowSide = true
    private var mHideSideRunnable: Runnable? = null
    private val handler = Handler()

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

        binding.thickness.setOnClickListener { Timber.d("thickness TODO") }
//            showThicknessDialog()
        binding.drawAction.setOnClickListener {
            binding.drawAction.apply {
                isActivated = isActivated.not()
                if (!isActivated) setImageResource(R.drawable.ic_brush_black_24px)
                else setImageResource(R.drawable.ic_eraser_24px)
                binding.doodleView.setEraser(isActivated)
            }
        }

        binding.image.setOnClickListener {
            Timber.d("clicked on image")
            if (binding.doodleView.hasInsertBitmap()) {
                binding.doodleView.insertBitmap(null)
            } else {
                val intent = Intent()
                intent.type = "image/*"
                intent.action = Intent.ACTION_GET_CONTENT
                startActivityForResult(
                    Intent.createChooser(
                        intent,
//                        getString(R.string.select_picture)
                        "R.string.select_picture"
                    ), REQUEST_CODE_SELECT_IMAGE
                )
                Timber.d("FIXED PICKING PICS")
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
            Timber.d("clicked on ok")
            if (mOutputFile != null) {
//                saveDoodle()
            } else {
                Toast.makeText(
                    this,
                    "R.string.cant_create_image_file",
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
                "R.string.cant_create_image_file",
                Toast.LENGTH_SHORT
            ).show()
        }
//        mSideAnimator = ValueAnimator()
        mSideAnimator.duration = 300
        mSideAnimator.addUpdateListener { animation ->
            binding.side.translationX =
                (animation.animatedValue as Float)
        }
        mHideSideRunnable = Runnable { hideSide() }
//        SimpleHandler.getInstance().postDelayed(mHideSideRunnable, 3000)
        handler.postDelayed(mHideSideRunnable!!, 3000)
    }

    override fun onBackPressed() {
        MaterialDialog(this).show {
            message(text = "R.string.save_doodle")
            positiveButton(R.string.save) {
                if (mOutputFile != null) {
//                        saveDoodle()
                } else {
                    Toast.makeText(
                        this@DoodleActivity,
                        "R.string.cant_create_image_file",
                        Toast.LENGTH_SHORT
                    ).show()
                    finish()
                }
            }
            negativeButton(R.string.cancel, null)
            neutralButton(text = "R.string.dont_save") { finish() }
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
//            SimpleHandler.getInstance().removeCallbacks(mHideSideRunnable)
            handler.removeCallbacks(mHideSideRunnable!!)
            mHideSideRunnable = null
        }
        if (!mShowSide) {
            mShowSide = true
            mSideAnimator.cancel()
//            mSideAnimator!!.interpolator = AnimationUtils2.FAST_SLOW_INTERPOLATOR
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
//            mSideAnimator!!.interpolator = AnimationUtils2.SLOW_FAST_INTERPOLATOR
            mSideAnimator.setFloatValues(binding.side.translationX, -binding.side.width.toFloat())
            mSideAnimator.start()
        }
    }

    //
    private fun updateUndoRedo() {
        binding.undo.isEnabled = binding.doodleView.canUndo()
        binding.redo.isEnabled = binding.doodleView.canRedo()
    }

    private fun showPickColorDialog() {

        val colors = intArrayOf(
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
            title(text = "R.string.colors")
            colorChooser(
                colors = colors,
//                subColors = subColors,
                allowCustomArgb = true,
                showAlphaSelector = true
            ) { _, color ->
                binding.doodleView.paintColor = color
            }
            positiveButton(text = "R.string.select")
        }
    }
//
//    private inner class ThicknessDialogHelper @SuppressLint("InflateParams") constructor() :
//        DialogInterface.OnClickListener, Slider.OnSetProgressListener {
//        val view: View
//        private val mTpv: ThicknessPreviewView
//        private val mSlider: Slider
//
//        override fun onClick(
//            dialog: DialogInterface,
//            which: Int
//        ) {
//            if (DialogInterface.BUTTON_POSITIVE == which) {
//                binding.doodleView.setPaintThickness(
//                    LayoutUtils.dp2pix(
//                        this@DoodleActivity,
//                        mSlider.getProgress()
//                    )
//                )
//            }
//        }
//
//        fun onSetProgress(
//            slider: Slider?,
//            newProgress: Int,
//            oldProgress: Int,
//            byUser: Boolean,
//            confirm: Boolean
//        ) {
//            mTpv.setThickness(LayoutUtils.dp2pix(this@DoodleActivity, newProgress))
//        }
//
//        fun onFingerDown() {}
//        fun onFingerUp() {}
//
//        init {
//            view = getLayoutInflater().inflate(R.layout.dialog_thickness, null)
//            mTpv =
//                view.findViewById<View>(R.id.thickness_preview_view) as ThicknessPreviewView
//            mSlider = view.findViewById<View>(R.id.slider) as Slider
//            mTpv.setThickness(binding.doodleView.getPaintThickness())
//            mTpv.setColor(binding.doodleView.getPaintColor())
//            mSlider.setProgress(
//                LayoutUtils.pix2dp(
//                    this@DoodleActivity,
//                    binding.doodleView.getPaintThickness()
//                ) as Int
//            )
//            mSlider.setOnSetProgressListener(this)
//        }
//    }

//    private fun showThicknessDialog() {
//        val helper: com.hippo.nimingban.ui.DoodleActivity.ThicknessDialogHelper =
//            com.hippo.nimingban.ui.DoodleActivity.ThicknessDialogHelper()
//        Builder(this@DoodleActivity)
//            .setView(helper.getView())
//            .setPositiveButton(R.string.ok, helper)
//            .show()
//    }

    private fun saveDoodle() {
//        if (mExitWaitingDialog == null) {
//            mExitWaitingDialog = ProgressDialogBuilder(this)
//                .setTitle(R.string.please_wait)
//                .setMessage(R.string.saving)
//                .setCancelable(false)
//                .show()
        Timber.d("FIXED ME")
//            binding.doodleView.save(mOutputFile)
//        } else {
        // Wait here, it might be fast click back button
//        }
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
//        intent.data = Uri.fromFile(mOutputFile)
        intent.data = mOutputFile
        setResult(RESULT_OK, intent)
        finish()
    }
/*
public static class DoodleLayout extends FrameLayout {

    public DoodleLayout(Context context) {
        super(context);
    }

    public DoodleLayout(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public DoodleLayout(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    @Override
    protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
        final int count = getChildCount();

        final int parentLeft = getPaddingLeft();
        final int parentRight = right - left - getPaddingRight();

        final int parentTop = getPaddingTop();
        final int parentBottom = bottom - top - getPaddingBottom();

        for (int i = 0; i < count; i++) {
            final View child = getChildAt(i);
            if (child.getVisibility() != GONE) {
                final LayoutParams lp = (LayoutParams) child.getLayoutParams();

                final int width = child.getMeasuredWidth();
                final int height = child.getMeasuredHeight();

                int childLeft;
                int childTop;

                int gravity = lp.gravity;
                if (gravity == -1) {
                    gravity = Gravity.TOP | Gravity.START;
                }

                final int verticalGravity = gravity & Gravity.VERTICAL_GRAVITY_MASK;
                switch (gravity & Gravity.HORIZONTAL_GRAVITY_MASK) {
                    case Gravity.CENTER_HORIZONTAL:
                        childLeft = parentLeft + (parentRight - parentLeft - width) / 2 +
                                lp.leftMargin - lp.rightMargin;
                        break;
                    case Gravity.RIGHT:
                        childLeft = parentRight - width - lp.rightMargin;
                        break;
                    case Gravity.LEFT:
                    default:
                        childLeft = parentLeft + lp.leftMargin;
                }

                switch (verticalGravity) {
                    case Gravity.TOP:
                        childTop = parentTop + lp.topMargin;
                        break;
                    case Gravity.CENTER_VERTICAL:
                        childTop = parentTop + (parentBottom - parentTop - height) / 2 +
                                lp.topMargin - lp.bottomMargin;
                        break;
                    case Gravity.BOTTOM:
                        childTop = parentBottom - height - lp.bottomMargin;
                        break;
                    default:
                        childTop = parentTop + lp.topMargin;
                }

                int offsetX = - (int) (lp.percent * width);
                child.layout(childLeft + offsetX, childTop, childLeft + width, childTop + height);
            }
        }
    }

    @Override
    public LayoutParams generateLayoutParams(AttributeSet attrs) {
        return new LayoutParams(getContext(), attrs);
    }

    @Override
    protected boolean checkLayoutParams(ViewGroup.LayoutParams p) {
        return p instanceof LayoutParams;
    }

    @Override
    protected ViewGroup.LayoutParams generateLayoutParams(ViewGroup.LayoutParams p) {
        return new LayoutParams(p);
    }

    public static class LayoutParams extends FrameLayout.LayoutParams {

        public float percent = 0.0f;

        public LayoutParams(Context c, AttributeSet attrs) {
            super(c, attrs);
        }

        public LayoutParams(ViewGroup.LayoutParams source) {
            super(source);
        }
    }
}
*/

    companion object {
        const val REQUEST_CODE_SELECT_IMAGE = 0
    }
}