package com.laotoua.dawnislandk.screens.widget.popup

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.widget.Button
import android.widget.ImageView
import androidx.navigation.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.laotoua.dawnislandk.R
import com.laotoua.dawnislandk.data.local.entity.Community
import com.laotoua.dawnislandk.data.local.entity.Forum
import com.laotoua.dawnislandk.screens.PagerFragmentDirections
import com.laotoua.dawnislandk.screens.SharedViewModel
import com.laotoua.dawnislandk.screens.adapters.CommunityNodeAdapter
import com.laotoua.dawnislandk.util.GlideApp
import com.lxj.xpopup.XPopup
import com.lxj.xpopup.core.DrawerPopupView
import timber.log.Timber

@SuppressLint("ViewConstructor")
class ForumDrawerPopup(
    context: Context,
    private val sharedVM: SharedViewModel
) : DrawerPopupView(context) {
    override fun getImplLayoutId(): Int =R.layout.drawer_forum

    private val forumListAdapter = CommunityNodeAdapter(object : CommunityNodeAdapter.ForumClickListener {
        override fun onForumClick(forum: Forum) {
            Timber.d("Clicked on Forum ${forum.name}")
            sharedVM.setForum(forum)
            dismiss()
        }
    })

    private lateinit var reedImageView:ImageView

    fun setData(list:List<Community>){
        forumListAdapter.setData(list)
    }

    override fun focusAndProcessBackPress() {
        super.focusAndProcessBackPress()
// TODO: make sure back pressed works
//        if (binding.drawerLayout.isOpen) {
//            binding.drawerLayout.close()
//            return
//        }
    }

    fun setReedPicture(url:String){
        GlideApp.with(reedImageView)
            .load(url)
            .fitCenter()
            .into(reedImageView)
    }

    override fun onCreate() {
        super.onCreate()

        findViewById<Button>(R.id.forumRefresh).setOnClickListener {
            forumListAdapter.setData(emptyList())
            sharedVM.forumRefresh()
        }
        val imageLoader = ImageLoader()
        reedImageView = findViewById(R.id.reedImageView)
        reedImageView.setOnClickListener {
            val url = sharedVM.reedPictureUrl.value!!
            val viewerPopup =
                ImageViewerPopup(
                    url,
                    activity = context as Activity
                )
            viewerPopup.setXPopupImageLoader(imageLoader)
            viewerPopup.setSingleSrcView(reedImageView, url)
            XPopup.Builder(context)
                .asCustom(viewerPopup)
                .show()
        }
//
        findViewById<Button>(R.id.ReedPictureRefresh).setOnClickListener {
            reedImageView.setImageResource(R.drawable.drawer_placeholder)
            sharedVM.getRandomReedPicture()
        }

        findViewById<RecyclerView>(R.id.forumContainer).apply {
            layoutManager = LinearLayoutManager(context)
            adapter = forumListAdapter
        }

        findViewById<Button>(R.id.settings).setOnClickListener {
            dismiss()
            val action = PagerFragmentDirections.actionPagerFragmentToSettingsFragment()
            findNavController().navigate(action)
        }

        findViewById<Button>(R.id.browsingHistory).setOnClickListener {
            dismiss()
            val action = PagerFragmentDirections.actionPagerFragmentToBrowsingHistoryFragment()
            findNavController().navigate(action)
        }
    }
}