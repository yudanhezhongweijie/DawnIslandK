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

import android.animation.ValueAnimator
import android.content.Context
import android.graphics.drawable.ColorDrawable
import android.os.Environment
import android.view.*
import android.view.animation.LinearInterpolator
import android.widget.*
import androidx.fragment.app.FragmentActivity
import androidx.interpolator.view.animation.FastOutSlowInInterpolator
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Observer
import androidx.lifecycle.lifecycleScope
import androidx.transition.*
import androidx.viewpager.widget.ViewPager.SimpleOnPageChangeListener
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.laotoua.dawnislandk.DawnApp
import com.laotoua.dawnislandk.R
import com.laotoua.dawnislandk.data.local.entity.Comment
import com.laotoua.dawnislandk.data.local.entity.Post
import com.laotoua.dawnislandk.data.local.entity.PostHistory
import com.laotoua.dawnislandk.data.remote.SearchResult
import com.laotoua.dawnislandk.screens.MainActivity
import com.laotoua.dawnislandk.util.ImageUtil
import com.laotoua.dawnislandk.util.ReadableTime
import com.laotoua.dawnislandk.util.SingleLiveEvent
import com.lxj.xpopup.core.ImageViewerPopupView
import com.lxj.xpopup.photoview.PhotoView
import com.lxj.xpopup.util.SmartGlideImageLoader
import com.lxj.xpopup.util.XPopupUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import timber.log.Timber
import java.io.File


class ImageViewerPopup(context: Context) : ImageViewerPopupView(context) {
    private var tvPagerIndicator: TextView? = null

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

    private var onPageChangeListener: SimpleOnPageChangeListener = object : SimpleOnPageChangeListener() {
        override fun onPageSelected(i: Int) {
            position = i
            showPageIndicator()
            preloadImages(i)
            //更新srcView
            //一定要post，因为setCurrentItem内部实现是RecyclerView.scrollTo()，这个是异步的
            pager?.post { //由于ViewPager2内部是包裹了一个RecyclerView，而RecyclerView始终维护一个子View
                val fl = pager.getChildAt(0) as FrameLayout
                this@ImageViewerPopup.updateSrcView(fl.getChildAt(0) as ImageView)
            }

        }
    }

    override fun getImplLayoutId(): Int {
        return R.layout.popup_image_viewer
    }

    override fun onCreate() {
        super.onCreate()
        isShowSaveBtn = false
        isShowIndicator = false
        isShowPlaceholder = false
        tv_pager_indicator.visibility = View.GONE
        tv_save.visibility = View.GONE
        customView.visibility = View.VISIBLE
        setXPopupImageLoader(SmartGlideImageLoader())
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
        tv_pager_indicator = findViewById(com.lxj.xpopup.R.id.tv_pager_indicator)
        tv_save = findViewById(com.lxj.xpopup.R.id.tv_save)
        placeholderView = findViewById(com.lxj.xpopup.R.id.placeholderView)
        photoViewContainer = findViewById(com.lxj.xpopup.R.id.photoViewContainer)
        photoViewContainer.setOnDragChangeListener(this)
        pager = findViewById(com.lxj.xpopup.R.id.pager)
        tv_pager_indicator.visibility = View.GONE
        tv_save.visibility = View.GONE
        tvPagerIndicator = findViewById(R.id.page_indicator)
        saveButton = findViewById(R.id.save_button)

        saveButton?.setOnClickListener {
            if (!isShow) return@setOnClickListener
            addPicToGallery(context as MainActivity, urls[position])
        }

        pager.apply {
            adapter = PhotoViewAdapter()
            currentItem = position
            visibility = View.INVISIBLE
            addOrUpdateSnapshot()
            offscreenPageLimit = urls.size
            addOnPageChangeListener(onPageChangeListener)
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
        placeholderView.visibility = if (isShowPlaceholder) VISIBLE else INVISIBLE
        if (isShowPlaceholder) {
            if (placeholderColor != -1) {
                placeholderView.color = placeholderColor
            }
            if (placeholderRadius != -1) {
                placeholderView.radius = placeholderRadius
            }
            if (placeholderStrokeColor != -1) {
                placeholderView.strokeColor = placeholderStrokeColor
            }
            XPopupUtils.setWidthHeight(placeholderView, rect.width(), rect.height())
            placeholderView.translationX = rect.left.toFloat()
            placeholderView.translationY = rect.top.toFloat()
            placeholderView.invalidate()
        }
    }

    private fun showPageIndicator() {
        if (urls.size > 1) {
            val pos = if (isInfinite) position % urls.size else position
            tvPagerIndicator?.text = resources.getString(R.string.count_text, (pos + 1), urls.size)
        }
    }

    override fun onClick(v: View) {}

    private fun addOrUpdateSnapshot() {
        if (srcView == null) return
        if (snapshotView == null) {
            snapshotView = PhotoView(context)
            snapshotView.isEnabled = false
            photoViewContainer.addView(snapshotView)
            snapshotView.scaleType = srcView.scaleType
            snapshotView.translationX = rect.left.toFloat()
            snapshotView.translationY = rect.top.toFloat()
            XPopupUtils.setWidthHeight(snapshotView, rect.width(), rect.height())
        }
        snapshotView.tag = realPosition
        if (srcView != null && srcView.drawable != null && this.urls.size == 1) {
            try {
                snapshotView.setImageDrawable(srcView.drawable.constantState!!.newDrawable())
            } catch (e: java.lang.Exception) {
            }
        }
        setupPlaceholder()
        imageLoader?.loadSnapshot(urls[realPosition], snapshotView)
    }

    override fun doShowAnimation() {
        if (srcView == null) {
            photoViewContainer.setBackgroundColor(bgColor)
            pager.visibility = VISIBLE
            showPageIndicator()
            photoViewContainer.isReleasing = false
            doAfterShow()
            customView?.alpha = 1f
            customView?.visibility = VISIBLE
            return
        }
        photoViewContainer.isReleasing = true
        customView?.visibility = VISIBLE

        snapshotView.visibility = VISIBLE
        doAfterShow()
        snapshotView.post {
            TransitionManager.beginDelayedTransition(
                (snapshotView.parent as ViewGroup), TransitionSet()
                    .setDuration(animationDuration.toLong())
                    .addTransition(ChangeBounds())
                    .addTransition(ChangeTransform())
                    .addTransition(ChangeImageTransform())
                    .setInterpolator(FastOutSlowInInterpolator())
                    .addListener(object : TransitionListenerAdapter() {
                        override fun onTransitionEnd(transition: Transition) {
                            pager.visibility = VISIBLE
                            snapshotView.visibility = INVISIBLE
                            showPageIndicator()
                            photoViewContainer.isReleasing = false
                        }
                    })
            )
            snapshotView.translationY = 0f
            snapshotView.translationX = 0f
            snapshotView.scaleType = ImageView.ScaleType.FIT_CENTER
            XPopupUtils.setWidthHeight(snapshotView, photoViewContainer.width, photoViewContainer.height)

            // do shadow anim.
            animateShadowBg(bgColor)
//            customView?.animate()?.alpha(1f)?.setDuration(animationDuration.toLong())?.start()
            customView?.apply {
                visibility = View.VISIBLE
                animate()
                    .alpha(1f)
                    .setDuration(animationDuration.toLong())
                    .setListener(null)
                    .start()
            }
        }
    }

    private fun animateShadowBg(endColor: Int) {
        val start = (photoViewContainer.background as ColorDrawable).color
        val animator = ValueAnimator.ofFloat(0f, 1f)
        animator.addUpdateListener { animation ->
            photoViewContainer.setBackgroundColor(
                (argbEvaluator.evaluate(
                    animation.animatedFraction,
                    start, endColor
                ) as Int)
            )
        }
        animator.setDuration(animationDuration.toLong()).interpolator = LinearInterpolator()
        animator.start()
    }

    override fun setImageUrls(urls: MutableList<Any>): ImageViewerPopup {
        val currentItem = this.urls.getOrNull(position)
        this.urls = urls
        if (currentItem != null) {
            position = this.urls.indexOf(currentItem).coerceAtLeast(0)
            pager?.adapter?.notifyDataSetChanged()
            pager?.setCurrentItem(position, false)
            pager?.post { //由于ViewPager2内部是包裹了一个RecyclerView，而RecyclerView始终维护一个子View
                val fl = pager.getChildAt(0) as FrameLayout
                this@ImageViewerPopup.updateSrcView(fl.getChildAt(0) as ImageView)
            }
            // clear loading flag
            nextPageLoading = false
            previousPageLoading = false
        }
        if (this.isShow) {
            showPageIndicator()
        }
        return this
    }


    /**
     * 设置单个使用的源View。单个使用的情况下，无需设置url集合和SrcViewUpdateListener
     *
     * @param srcView
     * @return
     */
    override fun setSingleSrcView(srcView: ImageView?, url: Any): ImageViewerPopup {
        urls.clear()
        urls.add(url)
        setSrcView(srcView, 0)
        return this
    }

    override fun setSrcView(srcView: ImageView?, position: Int): ImageViewerPopup {
        super.setSrcView(srcView, position)
        // clear cache views when reusing the same popup in images
        pager?.adapter?.notifyDataSetChanged()
        pager?.setCurrentItem(position, false)
        return this
    }

    override fun updateSrcView(srcView: ImageView?) {
        setSrcView(srcView, position)
        addOrUpdateSnapshot()
    }

    override fun destroy() {
        pager.removeOnPageChangeListener(onPageChangeListener)
        imageLoader = null
    }

    private fun showUI() {
        customView.visibility = View.VISIBLE
        saveButton?.alpha = 1f
        saveButton?.isClickable = true
        saveButton?.show()
        tvPagerIndicator?.visibility = View.VISIBLE
        tvPagerIndicator?.alpha = 1f
        uiShown = true
    }

    private fun hideUI() {
        saveButton?.hide()
        saveButton?.isClickable = false
        tvPagerIndicator?.visibility = View.GONE
        uiShown = false
    }

    inner class PhotoViewAdapter : ImageViewerPopupView.PhotoViewAdapter() {
        override fun getItemPosition(`object`: Any): Int {
            return POSITION_NONE
        }

        override fun instantiateItem(container: ViewGroup, position: Int): Any {
            val realPosition = if (isInfinite) position % urls.size else position
            //1. build container
            val fl = buildContainer(container.context)
            val progressBar = buildProgressBar(container.context)

            //2. add ImageView，maybe PhotoView or SubsamplingScaleImageView
            val view = imageLoader.loadImage(realPosition, getImageUrl(urls[realPosition]), this@ImageViewerPopup, snapshotView, progressBar)
            view.setOnClickListener {
                if (!isShow) return@setOnClickListener
                if (uiShown) {
                    hideUI()
                } else {
                    showUI()
                }

            }
            //3. add View
            fl.addView(view, LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT))

            //4. add ProgressBar
            fl.addView(progressBar)

            container.addView(fl)
            return fl
        }

        private fun buildContainer(context: Context): FrameLayout {
            val fl = FrameLayout(context)
            fl.layoutParams = LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
            return fl
        }

        private fun buildProgressBar(context: Context): ProgressBar {
            val progressBar = ProgressBar(context)
            progressBar.isIndeterminate = true
            val size = XPopupUtils.dp2px(container.context, 40f)
            val params = LayoutParams(size, size)
            params.gravity = Gravity.CENTER
            progressBar.layoutParams = params
            progressBar.visibility = GONE
            return progressBar
        }
    }

    private fun addPicToGallery(caller: MainActivity, urlObj: Any) {
        val imgUrl: String = getImageUrl(urlObj)
        if (!caller.intentsHelper.checkAndRequestSinglePermission(caller, android.Manifest.permission.WRITE_EXTERNAL_STORAGE, true)) return

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
        val uri = when (urlObj) {
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

        return if (uri.startsWith("http")) uri else DawnApp.currentImgCDN + uri
    }
}