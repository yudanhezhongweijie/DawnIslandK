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
import android.app.Activity
import android.app.Dialog
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.view.View
import android.widget.ImageView
import com.laotoua.dawnislandk.R
import com.laotoua.dawnislandk.io.ImageUtil
import com.laotoua.dawnislandk.ui.util.ReadableTime
import java.io.File

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

class DoodleActivity : Activity() {
    //    private var mDoodleView: DoodleView? = null
    private var mSide: View? = null
    private var mPalette: View? = null
    private var mThickness: View? = null
    private var mDrawAction: ImageView? = null
    private var mImage: ImageView? = null
    private var mUndo: ImageView? = null
    private var mRedo: ImageView? = null
    private var mClear: View? = null
    private var mOk: View? = null
    private var mMenu: View? = null
    private var mOutputFile: Uri? = null
    private var mExitWaitingDialog: Dialog? = null
    private var mSideAnimator: ValueAnimator? = null
    private var mShowSide = true
    private var mHideSideRunnable: Runnable? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
//        val dir: File = NMBAppConfig.getDoodleDir()
//        if (dir != null) {
        val timeStamp: String = ReadableTime.getFilenamableTime(System.currentTimeMillis())
        val relativeLocation =
            Environment.DIRECTORY_PICTURES + File.separator + "Dawn"
        val name = "Doodle_$timeStamp"
        val ext = "png"
        mOutputFile = ImageUtil.addPlaceholderImageUriToGallery(this, name, ext, relativeLocation)
//        }
//        setStatusBarColor(ResourcesUtils.getAttrColor(this, R.attr.colorPrimaryDark))
        setContentView(R.layout.activity_doodle)
//        mDoodleView = findViewById(R.id.doodle_view) as DoodleView?
//        mSide = findViewById(R.id.side)
//        mPalette = findViewById(R.id.palette)
//        mThickness = findViewById(R.id.thickness)
//        mDrawAction = findViewById(R.id.draw_action) as ImageView?
//        mImage = findViewById(R.id.image) as ImageView?
//        mUndo = findViewById(R.id.undo) as ImageView?
//        mRedo = findViewById(R.id.redo) as ImageView?
//        mClear = findViewById(R.id.clear)
//        mOk = findViewById(R.id.ok)
//        mMenu = findViewById(R.id.menu)
//        mDoodleView.setHelper(this)
//        val undoDrawable =
//            StateListDrawable()
//        undoDrawable.addState(
//            intArrayOf(-R.attr.state_enabled),
//            DrawableManager.getDrawable(this, R.drawable.v_undo_disabled_dark_x24)
//        )
//        undoDrawable.addState(
//            intArrayOf(),
//            DrawableManager.getDrawable(this, R.drawable.v_undo_default_dark_x24)
//        )
//        mUndo!!.setImageDrawable(undoDrawable)
//        val redoDrawable =
//            StateListDrawable()
//        redoDrawable.addState(
//            intArrayOf(-R.attr.state_enabled),
//            DrawableManager.getDrawable(this, R.drawable.v_redo_disabled_dark_x24)
//        )
//        redoDrawable.addState(
//            intArrayOf(),
//            DrawableManager.getDrawable(this, R.drawable.v_redo_default_dark_x24)
//        )
//        mRedo!!.setImageDrawable(redoDrawable)
//        val actionDrawable =
//            StateListDrawable()
//        actionDrawable.addState(
//            intArrayOf(R.attr.state_activated),
//            DrawableManager.getDrawable(this, R.drawable.v_eraser_dark_x24)
//        )
//        actionDrawable.addState(
//            intArrayOf(),
//            DrawableManager.getDrawable(this, R.drawable.v_brush_dark_x24)
//        )
//        mDrawAction!!.setImageDrawable(actionDrawable)
//        val imageDrawable =
//            StateListDrawable()
//        imageDrawable.addState(
//            intArrayOf(R.attr.state_activated),
//            DrawableManager.getDrawable(this, R.drawable.v_image_off_dark_x24)
//        )
//        imageDrawable.addState(
//            intArrayOf(),
//            DrawableManager.getDrawable(this, R.drawable.v_image_dark_x24)
//        )
//        mImage!!.setImageDrawable(imageDrawable)
//        Ripple.addRipple(mPalette, true)
//        Ripple.addRipple(mThickness, true)
//        Ripple.addRipple(mDrawAction, true)
//        Ripple.addRipple(mImage, true)
//        Ripple.addRipple(mUndo, true)
//        Ripple.addRipple(mRedo, true)
//        Ripple.addRipple(mClear, true)
//        Ripple.addRipple(mOk, true)
//        Ripple.addRipple(mMenu, ResourcesUtils.getAttrBoolean(this, R.attr.dark))
//        mSide!!.setOnClickListener(this)
//        mPalette!!.setOnClickListener(this)
//        mThickness!!.setOnClickListener(this)
//        mDrawAction!!.setOnClickListener(this)
//        mImage!!.setOnClickListener(this)
//        mUndo!!.setOnClickListener(this)
//        mRedo!!.setOnClickListener(this)
//        mClear!!.setOnClickListener(this)
//        mOk!!.setOnClickListener(this)
//        mMenu!!.setOnClickListener(this)
//        updateUndoRedo()
//        if (mOutputFile == null) {
//            Toast.makeText(
//                this,
//                R.string.cant_create_image_file,
//                Toast.LENGTH_SHORT
//            ).show()
//        }
//        mSideAnimator = ValueAnimator()
//        mSideAnimator!!.duration = 300
//        mSideAnimator!!.addUpdateListener { animation ->
//            mSide!!.translationX =
//                (animation.animatedValue as Float)
//        }
//        mHideSideRunnable = Runnable { hideSide() }
//        SimpleHandler.getInstance().postDelayed(mHideSideRunnable, 3000)
    }

    override fun onBackPressed() {
//        val listener =
//            DialogInterface.OnClickListener { dialog, which ->
//                if (which == DialogInterface.BUTTON_POSITIVE) {
//                    if (mOutputFile != null) {
//                        saveDoodle()
//                    } else {
//                        Toast.makeText(
//                            this@DoodleActivity,
//                            R.string.cant_create_image_file,
//                            Toast.LENGTH_SHORT
//                        ).show()
//                        finish()
//                    }
//                } else if (which == DialogInterface.BUTTON_NEUTRAL) {
//                    finish()
//                }
//            }
//        val dialog: AlertDialog = Builder(this).setMessage(R.string.save_doodle)
//            .setPositiveButton(R.string.save, listener)
//            .setNegativeButton(R.string.cancel, null)
//            .setNeutralButton(R.string.dont_save, listener)
//            .show()
//        val button: Button =
//            dialog.getButton(DialogInterface.BUTTON_NEUTRAL)
//        button.setTextColor(getResources().getColor(R.color.red_500))
    }

    override fun onActivityResult(
        requestCode: Int,
        resultCode: Int,
        data: Intent
    ) {
        if (requestCode == REQUEST_CODE_SELECT_IMAGE && resultCode == RESULT_OK) {
            val uri = data.data ?: return
            try {
//                val bitmap: Bitmap = BitmapUtils.decodeStream(
//                    UriInputStreamPipe(this, uri),
//                    mDoodleView.getWidth(), mDoodleView.getHeight()
//                )
//                if (bitmap != null) {
//                    mDoodleView.insertBitmap(bitmap)
//                    mImage!!.isActivated = true
//                }
            } catch (e: OutOfMemoryError) {
                // Ignore
            }
        }
    }

    private fun showSide() {
        if (mHideSideRunnable != null) {
//            SimpleHandler.getInstance().removeCallbacks(mHideSideRunnable)
            mHideSideRunnable = null
        }
        if (!mShowSide) {
            mShowSide = true
            mSideAnimator!!.cancel()
//            mSideAnimator!!.interpolator = AnimationUtils2.FAST_SLOW_INTERPOLATOR
            mSideAnimator!!.setFloatValues(mSide!!.translationX, 0f)
            mSideAnimator!!.start()
        }
    }

    private fun hideSide() {
        if (mHideSideRunnable != null) {
//            SimpleHandler.getInstance().removeCallbacks(mHideSideRunnable)
            mHideSideRunnable = null
        }
        if (mShowSide) {
            mShowSide = false
            mSideAnimator!!.cancel()
//            mSideAnimator!!.interpolator = AnimationUtils2.SLOW_FAST_INTERPOLATOR
            mSideAnimator!!.setFloatValues(mSide!!.translationX, -mSide!!.width.toFloat())
            mSideAnimator!!.start()
        }
    }
//
//    private fun updateUndoRedo() {
//        mUndo!!.isEnabled = mDoodleView.canUndo()
//        mRedo!!.isEnabled = mDoodleView.canRedo()
//    }

//    private inner class PickColorDialogHelper :
//        DialogInterface.OnClickListener {
//        private val mView: ColorPickerView
//        val view: View
//            get() = mView
//
//        override fun onClick(
//            dialog: DialogInterface,
//            which: Int
//        ) {
//            if (DialogInterface.BUTTON_POSITIVE == which) {
////                mDoodleView.setPaintColor(mView.getColor())
//            }
//        }
//
//        init {
//            mView = ColorPickerView(this@DoodleActivity)
//            mView.setColor(mDoodleView.getPaintColor())
//        }
//    }

//    private fun showPickColorDialog() {
//        val helper: com.hippo.nimingban.ui.DoodleActivity.PickColorDialogHelper =
//            com.hippo.nimingban.ui.DoodleActivity.PickColorDialogHelper()
//        Builder(this@DoodleActivity)
//            .setView(helper.getView())
//            .setPositiveButton(R.string.ok, helper)
//            .setNegativeButton(R.string.cancel, null)
//            .show()
//    }
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
//                mDoodleView.setPaintThickness(
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
//            mTpv.setThickness(mDoodleView.getPaintThickness())
//            mTpv.setColor(mDoodleView.getPaintColor())
//            mSlider.setProgress(
//                LayoutUtils.pix2dp(
//                    this@DoodleActivity,
//                    mDoodleView.getPaintThickness()
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

//    private fun saveDoodle() {
//        if (mExitWaitingDialog == null) {
//            mExitWaitingDialog = ProgressDialogBuilder(this)
//                .setTitle(R.string.please_wait)
//                .setMessage(R.string.saving)
//                .setCancelable(false)
//                .show()
//            mDoodleView.save(mOutputFile)
//        } else {
//            // Wait here, it might be fast click back button
//        }
//    }

//    override fun onClick(v: View) {
//        if (mSide === v) {
//            hideSide()
//        } else if (mPalette === v) {
//            showPickColorDialog()
//        } else if (mThickness === v) {
//            showThicknessDialog()
//        } else if (mDrawAction === v) {
//            val newActivated = !mDrawAction!!.isActivated
//            mDrawAction!!.isActivated = newActivated
//            mDoodleView.setEraser(newActivated)
//        } else if (mImage === v) {
//            if (mDoodleView.hasInsertBitmap()) {
//                mDoodleView.insertBitmap(null)
//                mImage!!.isActivated = false
//            } else {
//                val intent = Intent()
//                intent.type = "image/*"
//                intent.action = Intent.ACTION_GET_CONTENT
//                startActivityForResult(
//                    Intent.createChooser(
//                        intent,
//                        getString(R.string.select_picture)
//                    ), REQUEST_CODE_SELECT_IMAGE
//                )
//            }
//        } else if (mUndo === v) {
//            mDoodleView.undo()
//        } else if (mRedo === v) {
//            mDoodleView.redo()
//        } else if (mClear === v) {
//            mDoodleView.clear()
//        } else if (mOk === v) {
//            if (mOutputFile != null) {
//                saveDoodle()
//            } else {
//                Toast.makeText(
//                    this,
//                    R.string.cant_create_image_file,
//                    Toast.LENGTH_SHORT
//                ).show()
//            }
//        } else if (mMenu === v) {
//            if (mShowSide) {
//                hideSide()
//            } else {
//                showSide()
//            }
//        }
//    }
//
//    fun onStoreChange(view: DoodleView?) {
//        updateUndoRedo()
//    }
//
//    fun onSavingFinished(ok: Boolean) {
//        if (mExitWaitingDialog != null) {
//            mExitWaitingDialog!!.dismiss()
//            mExitWaitingDialog = null
//        }
//        val intent = Intent()
//        intent.data = Uri.fromFile(mOutputFile)
//        setResult(RESULT_OK, intent)
//        finish()
//    }
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