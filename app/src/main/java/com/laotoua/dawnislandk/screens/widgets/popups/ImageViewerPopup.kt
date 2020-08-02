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

package com.laotoua.dawnislandk.screens.widgets.popups

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.ArgbEvaluator
import android.animation.ValueAnimator
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Color
import android.graphics.Matrix
import android.graphics.Rect
import android.graphics.drawable.ColorDrawable
import android.os.Environment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.LinearInterpolator
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.FragmentActivity
import androidx.interpolator.view.animation.FastOutSlowInInterpolator
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Observer
import androidx.lifecycle.lifecycleScope
import androidx.transition.*
import androidx.viewpager.widget.PagerAdapter
import androidx.viewpager.widget.ViewPager.SimpleOnPageChangeListener
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.laotoua.dawnislandk.R
import com.laotoua.dawnislandk.data.local.entity.Comment
import com.laotoua.dawnislandk.data.local.entity.Post
import com.laotoua.dawnislandk.data.local.entity.PostHistory
import com.laotoua.dawnislandk.data.remote.SearchResult
import com.laotoua.dawnislandk.util.ImageUtil
import com.laotoua.dawnislandk.util.ReadableTime
import com.laotoua.dawnislandk.util.SingleLiveEvent
import com.lxj.xpopup.XPopup
import com.lxj.xpopup.core.BasePopupView
import com.lxj.xpopup.enums.PopupStatus
import com.lxj.xpopup.enums.PopupType
import com.lxj.xpopup.interfaces.OnDragChangeListener
import com.lxj.xpopup.interfaces.OnSrcViewUpdateListener
import com.lxj.xpopup.interfaces.XPopupImageLoader
import com.lxj.xpopup.photoview.PhotoView
import com.lxj.xpopup.util.XPopupUtils
import com.lxj.xpopup.widget.BlankView
import com.lxj.xpopup.widget.HackyViewPager
import com.lxj.xpopup.widget.PhotoViewContainer
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import timber.log.Timber
import java.io.File


class ImageViewerPopup(context: Context) : BasePopupView(context), OnDragChangeListener {
    private var container: FrameLayout = findViewById(R.id.container)
    private var photoViewContainer: PhotoViewContainer? = null
    private var placeholderView: BlankView? = null
    private var tvPagerIndicator: TextView? = null
    private var pager: HackyViewPager? = null
    private var argbEvaluator = ArgbEvaluator()
    private var urls: MutableList<Any> = mutableListOf()
    private var imageLoader: XPopupImageLoader? = null
    private var srcViewUpdateListener: OnSrcViewUpdateListener? = null
    private var position = 0
    private var rect: Rect? = null
    private var srcView: ImageView? = null //动画起始的View，如果为null，移动和过渡动画效果会没有，只有弹窗的缩放功能
    private var snapshotView: PhotoView? = null
    private var isShowPlaceholder = false //是否显示占位白色，当图片切换为大图时，原来的地方会有一个白色块
    private var placeholderColor = -1 //占位View的颜色
    private var placeholderStrokeColor = -1 // 占位View的边框色
    private var placeholderRadius = -1 // 占位View的圆角
    private var isShowSaveBtn = true //是否显示保存按钮
    private var isShowIndicator = true //是否页码指示器
    private var isInfinite = false //是否需要无限滚动
    private var customView: View? = null
    private var bgColor = Color.rgb(32, 36, 46) //弹窗的背景颜色，可以自定义
    private val toastMsg = MutableLiveData<SingleLiveEvent<Int>>()
    private var uiShown = true
    private var saveButton: FloatingActionButton? = null

    // preload previous, next page if about to reach edge pages
    private var prefetchItemCount = 2
    private var nextPageLoader: (() -> Unit)? = null
    private var nextPageLoading = false
    private var previousPageLoader: (() -> Unit)? = null
    private var previousPageLoading = false

    private val toastObs = Observer<SingleLiveEvent<Int>> { event ->
        event.getContentIfNotHandled()?.let {
            Toast.makeText(context as FragmentActivity, it, Toast.LENGTH_SHORT).show()
        }
    }

    init {
        if (implLayoutId > 0) {
            customView = LayoutInflater.from(getContext()).inflate(implLayoutId, container, false)
            customView!!.visibility = View.INVISIBLE
            customView!!.alpha = 0f
            container.addView(customView)
        }
    }

    override fun getPopupLayoutId(): Int {
        return R.layout.popup_image_viewer
    }

    override fun onCreate() {
        super.onCreate()
        setXPopupImageLoader(ImageLoader())
        popupInfo.popupType = PopupType.ImageViewer
    }


    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        toastMsg.observe(context as FragmentActivity, toastObs)
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        toastMsg.removeObserver(toastObs)
    }

    override fun initPopupContent() {
        super.initPopupContent()
        tvPagerIndicator = findViewById(R.id.tv_pager_indicator)
        saveButton = findViewById(R.id.save_button)
        saveButton!!.setOnClickListener {
            addPicToGallery(
                context as FragmentActivity,
                urls[position]
            )
        }
        placeholderView = findViewById(R.id.placeholderView)
        photoViewContainer = findViewById(R.id.photoViewContainer)
        photoViewContainer!!.setOnDragChangeListener(this)
        pager = findViewById(R.id.pager)
        pager?.apply {
            adapter = PhotoViewAdapter()
            //        pager.setOffscreenPageLimit(urls.size());
            currentItem = position
            visibility = View.INVISIBLE
            addOrUpdateSnapshot()
//            if (isInfinite) offscreenPageLimit = urls.size / 2
            addOnPageChangeListener(object : SimpleOnPageChangeListener() {
                override fun onPageSelected(i: Int) {
                    position = i
                    showPagerIndicator()
                    preloadImages(i)
                    //更新srcView
//                    if (srcViewUpdateListener != null) {
//                        srcViewUpdateListener!!.onSrcViewUpdate(this@ImageViewerPopup, i)
//                    }
                }
            })
        }
        if (!isShowIndicator) tvPagerIndicator!!.visibility = View.GONE
        if (!isShowSaveBtn) {
            saveButton!!.visibility = View.GONE
        } else {
            saveButton!!.visibility = View.VISIBLE
        }
    }

    private fun preloadImages(currentPos: Int) {
        if (urls.firstOrNull() !is Comment) return
        if (currentPos > urls.size - 1 - prefetchItemCount && !nextPageLoading) {
            nextPageLoader?.invoke()
            nextPageLoading = true
        } else if (currentPos < prefetchItemCount && !previousPageLoading) {
            previousPageLoader?.invoke()
            previousPageLoading = true
        }
    }

    fun setPreviousPageLoader(task: (() -> Unit)?) {
        previousPageLoader = task
    }

    fun setNextPageLoader(task: (() -> Unit)?) {
        nextPageLoader = task
    }

    fun clearLoaders() {
        nextPageLoader = null
        previousPageLoader = null
    }


    private fun setupPlaceholder() {
        placeholderView!!.visibility = if (isShowPlaceholder) View.VISIBLE else View.INVISIBLE
        if (isShowPlaceholder) {
            if (placeholderColor != -1) {
                placeholderView!!.color = placeholderColor
            }
            if (placeholderRadius != -1) {
                placeholderView!!.radius = placeholderRadius
            }
            if (placeholderStrokeColor != -1) {
                placeholderView!!.strokeColor = placeholderStrokeColor
            }
            XPopupUtils.setWidthHeight(placeholderView, rect!!.width(), rect!!.height())
            placeholderView!!.translationX = rect!!.left.toFloat()
            placeholderView!!.translationY = rect!!.top.toFloat()
            placeholderView!!.invalidate()
        }
    }

    private fun showPagerIndicator() {
        if (urls.size > 1) {
            val pos = if (isInfinite) position % urls.size else position
            tvPagerIndicator!!.text = resources.getString(R.string.count_text, (pos + 1), urls.size)
        }
        if (isShowSaveBtn && uiShown) saveButton!!.visibility = View.VISIBLE
    }

    private fun addOrUpdateSnapshot() {
        if (srcView == null) return
        if (snapshotView == null) {
            snapshotView = PhotoView(context)
            photoViewContainer!!.addView(snapshotView)
            snapshotView!!.scaleType = srcView!!.scaleType
            snapshotView!!.translationX = rect!!.left.toFloat()
            snapshotView!!.translationY = rect!!.top.toFloat()
            XPopupUtils.setWidthHeight(snapshotView, rect!!.width(), rect!!.height())
        }
        setupPlaceholder()
        snapshotView!!.setImageDrawable(srcView!!.drawable)
    }

    override fun doAfterShow() {
        //do nothing self.
    }

    public override fun doShowAnimation() {
        if (srcView == null) {
            resetViewStates()
            photoViewContainer!!.setBackgroundColor(bgColor)
            pager!!.visibility = View.VISIBLE
            showPagerIndicator()
            photoViewContainer!!.isReleasing = false
            super.doAfterShow()
            return
        }
        photoViewContainer!!.isReleasing = true
        if (customView != null) customView!!.visibility = View.VISIBLE
        snapshotView!!.visibility = View.VISIBLE
        snapshotView!!.post {
            TransitionManager.beginDelayedTransition(
                (snapshotView!!.parent as ViewGroup),
                TransitionSet()
                    .setDuration(duration.toLong())
                    .addTransition(ChangeBounds())
                    .addTransition(ChangeTransform())
                    .addTransition(ChangeImageTransform())
                    .setInterpolator(FastOutSlowInInterpolator())
                    .addListener(object : TransitionListenerAdapter() {
                        override fun onTransitionEnd(transition: Transition) {
                            pager!!.visibility = View.VISIBLE
                            snapshotView!!.visibility = View.INVISIBLE
                            showPagerIndicator()
                            photoViewContainer!!.isReleasing = false
                            super@ImageViewerPopup.doAfterShow()
                        }
                    })
            )
            snapshotView!!.translationY = 0f
            snapshotView!!.translationX = 0f
            snapshotView!!.scaleType = ImageView.ScaleType.FIT_CENTER
            XPopupUtils.setWidthHeight(
                snapshotView,
                photoViewContainer!!.width,
                photoViewContainer!!.height
            )

            // do shadow anim.
            animateShadowBg(bgColor)
            if (customView != null) customView!!.animate().alpha(1f)
                .setDuration(duration.toLong()).start()
        }
    }

    private fun animateShadowBg(endColor: Int) {
        val start = (photoViewContainer!!.background as ColorDrawable).color
        val animator = ValueAnimator.ofFloat(0f, 1f)
        animator.addUpdateListener { animation ->
            photoViewContainer!!.setBackgroundColor(
                (argbEvaluator.evaluate(
                    animation.animatedFraction,
                    start, endColor
                ) as Int)
            )
        }
        animator.setDuration(duration.toLong()).interpolator = LinearInterpolator()
        animator.start()
    }

    private val duration: Int
        get() = XPopup.getAnimationDuration() + 60

    public override fun doDismissAnimation() {
        if (srcView == null) {
            photoViewContainer!!.setBackgroundColor(Color.TRANSPARENT)
            photoViewContainer!!.isReleasing = true
            doAfterDismiss()
            pager!!.visibility = View.INVISIBLE
            placeholderView!!.visibility = View.INVISIBLE
            return
        }
        tvPagerIndicator!!.visibility = View.INVISIBLE
        saveButton!!.visibility = View.INVISIBLE
        pager!!.visibility = View.INVISIBLE
        photoViewContainer!!.isReleasing = true
        snapshotView!!.visibility = View.VISIBLE
        snapshotView!!.post {
            TransitionManager.beginDelayedTransition(
                (snapshotView!!.parent as ViewGroup),
                TransitionSet()
                    .setDuration(duration.toLong())
                    .addTransition(ChangeBounds())
                    .addTransition(ChangeTransform())
                    .addTransition(ChangeImageTransform())
                    .setInterpolator(FastOutSlowInInterpolator())
                    .addListener(object : TransitionListenerAdapter() {
                        override fun onTransitionEnd(transition: Transition) {
                            doAfterDismiss()
                            pager!!.visibility = View.INVISIBLE
                            snapshotView!!.visibility = View.VISIBLE
                            pager!!.scaleX = 1f
                            pager!!.scaleY = 1f
                            snapshotView!!.scaleX = 1f
                            snapshotView!!.scaleY = 1f
                            placeholderView!!.visibility = View.INVISIBLE
                        }
                    })
            )
            snapshotView!!.scaleX = 1f
            snapshotView!!.scaleY = 1f
            snapshotView!!.translationY = rect!!.top.toFloat()
            snapshotView!!.translationX = rect!!.left.toFloat()
            snapshotView!!.scaleType = srcView!!.scaleType
            XPopupUtils.setWidthHeight(snapshotView, rect!!.width(), rect!!.height())

            // do shadow anim.
            animateShadowBg(Color.TRANSPARENT)
            if (customView != null) customView!!.animate().alpha(0f)
                .setDuration(duration.toLong())
                .setListener(object : AnimatorListenerAdapter() {
                    override fun onAnimationEnd(animation: Animator) {
                        super.onAnimationEnd(animation)
                        if (customView != null) customView!!.visibility = View.INVISIBLE
                    }
                })
                .start()
        }
    }

    override fun getAnimationDuration(): Int {
        return 0
    }

    override fun dismiss() {
        if (popupStatus != PopupStatus.Show) return
        popupStatus = PopupStatus.Dismissing
        doDismissAnimation()
    }

    fun setImageUrls(urls: MutableList<Any>): ImageViewerPopup {
        val currentItem = this.urls.getOrNull(position)
        this.urls = urls
        if (currentItem != null) {
            position = urls.indexOf(currentItem).coerceAtLeast(0)
            pager?.adapter?.notifyDataSetChanged()
            pager?.setCurrentItem(position, false)
            // clear loading flag
            nextPageLoading = false
            previousPageLoading = false
        }
        if (this.isShow) {
            showPagerIndicator()
        }
        return this
    }

//    fun setSrcViewUpdateListener(srcViewUpdateListener: OnSrcViewUpdateListener?): ImageViewerPopup {
//        this.srcViewUpdateListener = srcViewUpdateListener
//        return this
//    }

    fun setXPopupImageLoader(imageLoader: XPopupImageLoader?): ImageViewerPopup {
        this.imageLoader = imageLoader
        return this
    }

    /**
     * 是否显示白色占位区块
     *
     * @param isShow
     * @return
     */
    fun isShowPlaceholder(isShow: Boolean): ImageViewerPopup {
        isShowPlaceholder = isShow
        return this
    }

    /**
     * 是否显示页码指示器
     *
     * @param isShow
     * @return
     */
    fun isShowIndicator(isShow: Boolean): ImageViewerPopup {
        isShowIndicator = isShow
        return this
    }

    /**
     * 是否显示保存按钮
     *
     * @param isShowSaveBtn
     * @return
     */
    fun isShowSaveButton(isShowSaveBtn: Boolean): ImageViewerPopup {
        this.isShowSaveBtn = isShowSaveBtn
        return this
    }

    fun isInfinite(isInfinite: Boolean): ImageViewerPopup {
        this.isInfinite = isInfinite
        return this
    }

    fun setPlaceholderColor(color: Int): ImageViewerPopup {
        placeholderColor = color
        return this
    }

    fun setPlaceholderRadius(radius: Int): ImageViewerPopup {
        placeholderRadius = radius
        return this
    }

    fun setPlaceholderStrokeColor(strokeColor: Int): ImageViewerPopup {
        placeholderStrokeColor = strokeColor
        return this
    }

    /**
     * 设置单个使用的源View。单个使用的情况下，无需设置url集合和SrcViewUpdateListener
     *
     * @param srcView
     * @return
     */
    fun setSingleSrcView(srcView: ImageView?, url: Any): ImageViewerPopup {
        urls.clear()
        urls.add(url)
        setSrcView(srcView, 0)
        return this
    }

    fun setSrcView(srcView: ImageView?, position: Int): ImageViewerPopup {
        this.srcView = srcView
        this.position = position
        if (srcView != null) {
            val locations = IntArray(2)
            this.srcView!!.getLocationInWindow(locations)
            rect = Rect(
                locations[0],
                locations[1],
                locations[0] + srcView.width,
                locations[1] + srcView.height
            )
        }
        // clear cache views when reusing the same popup in images
        pager?.adapter?.notifyDataSetChanged()
        pager?.setCurrentItem(position, false)
        return this
    }

    fun updateSrcView(srcView: ImageView?) {
        setSrcView(srcView, position)
        addOrUpdateSnapshot()
    }

    override fun onRelease() {
        dismiss()
    }

    override fun onDragChange(dy: Int, scale: Float, fraction: Float) {
        tvPagerIndicator!!.alpha = 1 - fraction
        if (customView != null) customView!!.alpha = 1 - fraction
        if (isShowSaveBtn) saveButton!!.alpha = 1 - fraction
        photoViewContainer!!.setBackgroundColor(
            (argbEvaluator.evaluate(
                fraction * .8f,
                bgColor,
                Color.TRANSPARENT
            ) as Int)
        )
    }

    // when reusing the same popup, need to reset the following attributes,
    // which are modified by the drag change listener
    private fun resetViewStates() {
        tvPagerIndicator?.alpha = 1f
        if (customView != null) customView!!.alpha = 1f
        if (isShowSaveBtn) saveButton?.alpha = 1f
        pager?.scaleX = 1f
        pager?.scaleY = 1f
    }

    override fun onDismiss() {
        super.onDismiss()
        srcView = null
        srcViewUpdateListener = null
    }

    open inner class PhotoViewAdapter : PagerAdapter() {
        override fun getCount(): Int {
            return if (isInfinite) Int.MAX_VALUE / 2 else urls.size
        }

        override fun isViewFromObject(view: View, o: Any): Boolean {
            return o === view
        }

        override fun instantiateItem(container: ViewGroup, position: Int): Any {
            val photoView = PhotoView(container.context)
            if (imageLoader != null) {
                imageLoader?.loadImage(
                    position,
                    getImageUrl(urls[if (isInfinite) position % urls.size else position]),
                    photoView
                )
            }
            photoView.setOnMatrixChangeListener {
                if (snapshotView != null) {
                    val matrix = Matrix()
                    photoView.getSuppMatrix(matrix)
                    snapshotView!!.setSuppMatrix(matrix)
                }
            }
            container.addView(photoView)
            photoView.setOnClickListener {
                uiShown = if (uiShown) {
                    saveButton?.hide()
                    saveButton?.isClickable = false
                    tvPagerIndicator?.visibility = View.GONE
                    false
                } else {
                    saveButton?.isClickable = true
                    saveButton?.show()
                    tvPagerIndicator?.visibility = View.VISIBLE
                    true
                }

            }
            return photoView
        }

        override fun destroyItem(container: ViewGroup, position: Int, `object`: Any) {
            container.removeView(`object` as View)
        }

        override fun getItemPosition(`object`: Any): Int {
            return POSITION_NONE
        }
    }


    private fun checkAndRequestExternalStoragePermission(caller: FragmentActivity): Boolean {
        if (caller.checkSelfPermission(android.Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            caller.registerForActivityResult(ActivityResultContracts.RequestPermission()) {
                if (it == false) {
                    Toast.makeText(
                        context,
                        R.string.need_write_storage_permission,
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }.launch(android.Manifest.permission.WRITE_EXTERNAL_STORAGE)
            return false
        }
        return true
    }

    private fun addPicToGallery(caller: FragmentActivity, urlObj: Any) {
        val imgUrl: String = getImageUrl(urlObj)
        val permission = checkAndRequestExternalStoragePermission(caller)
        if (!permission) return

        caller.lifecycleScope.launch(Dispatchers.IO) {
            Timber.i("Saving image $imgUrl to Gallery... ")
            val relativeLocation = Environment.DIRECTORY_PICTURES + File.separator + "Dawn"
            var fileName = imgUrl.substringAfter("/")
            val fileExist = ImageUtil.isImageInGallery(caller, fileName)
            if (fileExist) {
                // Inform user and renamed file when the filename is already taken
                val name = fileName.substringBeforeLast(".")
                val ext = fileName.substringAfterLast(".")
                fileName = "${name}_${ReadableTime.getCurrentTimeFileName()}.$ext"
            }
            val source = imageLoader?.getImageFile(context, imgUrl)
            if (source == null) {
                toastMsg.postValue(SingleLiveEvent.create(R.string.image_does_not_exist))
                return@launch
            }
            val saved = ImageUtil.copyImageFileToGallery(
                caller,
                fileName,
                relativeLocation,
                source
            )
            if (fileExist && saved) toastMsg.postValue(SingleLiveEvent.create(R.string.image_already_exists_in_picture))
            else if (saved) toastMsg.postValue(SingleLiveEvent.create(R.string.image_saved))
            else toastMsg.postValue(SingleLiveEvent.create(R.string.something_went_wrong))
        }
    }

    private fun getImageUrl(urlObj: Any): String {
        return when (urlObj) {
            is Post -> {
                urlObj.getImgUrl()
            }
            is Comment -> {
                urlObj.getImgUrl()
            }
            is SearchResult.Hit -> {
                urlObj.getImgUrl()
            }
            is PostHistory -> {
                urlObj.getImgUrl()
            }
            is String -> {
                urlObj.toString()
            }
            else -> {
                throw Exception("Unhandled url getter for type $urlObj")
            }
        }
    }
}