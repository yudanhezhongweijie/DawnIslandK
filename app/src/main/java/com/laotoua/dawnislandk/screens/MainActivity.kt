package com.laotoua.dawnislandk.screens

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.widget.Toast
import androidx.activity.viewModels
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.navigation.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.afollestad.materialdialogs.MaterialDialog
import com.afollestad.materialdialogs.checkbox.checkBoxPrompt
import com.afollestad.materialdialogs.checkbox.isCheckPromptChecked
import com.laotoua.dawnislandk.DawnApp.Companion.applicationDataStore
import com.laotoua.dawnislandk.R
import com.laotoua.dawnislandk.data.local.Forum
import com.laotoua.dawnislandk.databinding.ActivityMainBinding
import com.laotoua.dawnislandk.screens.adapters.QuickNodeAdapter
import com.laotoua.dawnislandk.screens.comments.QuotePopup
import com.laotoua.dawnislandk.screens.comments.CommentsFragment
import com.laotoua.dawnislandk.screens.util.ToolBar.immersiveToolbarInitialization
import com.laotoua.dawnislandk.screens.widget.popup.ImageLoader
import com.laotoua.dawnislandk.screens.widget.popup.ImageViewerPopup
import com.laotoua.dawnislandk.util.GlideApp
import com.laotoua.dawnislandk.util.LoadingStatus
import com.lxj.xpopup.XPopup
import dagger.android.support.DaggerAppCompatActivity
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject


class MainActivity : DaggerAppCompatActivity(), QuickNodeAdapter.ForumClickListener {

    private lateinit var binding: ActivityMainBinding

    @Inject
    lateinit var viewModelFactory: ViewModelProvider.Factory

    private val communityVM: CommunityViewModel by viewModels { viewModelFactory }

    private val sharedVM: SharedViewModel by viewModels { viewModelFactory }

    private var doubleBackToExitPressedOnce = false
    private val mHandler = Handler()
    private val mRunnable = Runnable { doubleBackToExitPressedOnce = false }

    init {
        // load Resources
        lifecycleScope.launch { loadResources() }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityMainBinding.inflate(layoutInflater)
        immersiveToolbarInitialization()
        setContentView(binding.root)
        setUpForumDrawer()
    }


    // left forum drawer
    private fun setUpForumDrawer() {

        val mAdapter = QuickNodeAdapter(this)
        binding.forumRefresh.setOnClickListener {
            mAdapter.setData(emptyList())
            communityVM.refresh()
        }
        val imageLoader = ImageLoader()
        binding.reedImageView.setOnClickListener {
            val url = communityVM.reedPictureUrl.value!!
            val viewerPopup =
                ImageViewerPopup(
                    url,
                    activity = this
                )
            viewerPopup.setXPopupImageLoader(imageLoader)
            viewerPopup.setSingleSrcView(binding.reedImageView, url)
            XPopup.Builder(this)
                .asCustom(viewerPopup)
                .show()
        }

        binding.ReedPictureRefresh.setOnClickListener {
            binding.reedImageView.setImageResource(R.drawable.drawer_placeholder)
            communityVM.getRandomReedPicture()
        }

        binding.forumContainer.layoutManager = LinearLayoutManager(this)
        binding.forumContainer.adapter = mAdapter

        communityVM.communityList.observe(this, Observer {
            if (it.isNullOrEmpty()) return@Observer
            mAdapter.setData(it)
            Timber.i("Loaded ${it.size} communities to Adapter")
            sharedVM.setForumMappings(communityVM.getForums())
            // TODO: set default forum
            sharedVM.setForum(it[0].forums[0])
        })

        communityVM.reedPictureUrl.observe(this, Observer {
            GlideApp.with(binding.reedImageView)
                .load(it)
                .fitCenter()
                .into(binding.reedImageView)
        })

        communityVM.loadingStatus.observe(this, Observer {
            if (it.getContentIfNotHandled()?.loadingStatus == LoadingStatus.FAILED) {
                Toast.makeText(this, it.peekContent().message, Toast.LENGTH_LONG)
                    .show()
            }
        })

        binding.settings.setOnClickListener {
            val action = PagerFragmentDirections.actionPagerFragmentToSettingsFragment()
            findNavController(R.id.navHostFragment).navigate(action)
        }

    }

    // Forum Click
    override fun onForumClick(forum: Forum) {
        Timber.d("Clicked on Forum ${forum.name}")
        sharedVM.setForum(forum)
        binding.drawerLayout.closeDrawers()
    }


    // initialize Global resources
    private suspend fun loadResources() {
        applicationDataStore.getLatestRelease()?.let { release ->
            MaterialDialog(this).show {
                cornerRadius(res = R.dimen.dp_10)
                title(R.string.found_new_version)
                message(text = release.message) { html() }
                positiveButton(R.string.download_latest_version) {
                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse(release.downloadUrl))
                    if (intent.resolveActivity(packageManager) != null) {
                        startActivity(intent)
                    }
                }
                negativeButton(R.string.acknowledge) {
                    dismiss()
                }
            }
        }

        applicationDataStore.loadCookies()
        applicationDataStore.initializeFeedId()
        applicationDataStore.getNMBNotice()?.let { notice ->
            MaterialDialog(this).show {
                cornerRadius(res = R.dimen.dp_10)
                checkBoxPrompt(R.string.acknowledge) {}
                message(text = notice.content) { html() }
                positiveButton(R.string.close) {
                    notice.read = isCheckPromptChecked()
                    if (notice.read) lifecycleScope.launch {
                        applicationDataStore.readNMBNotice(
                            notice
                        )
                    }
                }
            }
        }

        applicationDataStore.getLuweiNotice()?.let { luweiNotice ->
            sharedVM.setLuweiLoadingBible(luweiNotice.loadingMsgs)
        }
    }

    override fun onBackPressed() {
        /**
         *  Catch for popup which failed to request focus
         */
        if (!QuotePopup.ensureQuotePopupDismissal()) return

        if (hideComment()) return

        if (binding.drawerLayout.isOpen) {
            binding.drawerLayout.close()
            return
        }

        if (!doubleBackToExitPressedOnce &&
            findNavController(R.id.navHostFragment).previousBackStackEntry == null
        ) {
            doubleBackToExitPressedOnce = true
            Toast.makeText(
                this,
                R.string.press_again_to_exit, Toast.LENGTH_SHORT
            ).show()
            mHandler.postDelayed(mRunnable, 2000)
            return
        }
        super.onBackPressed()
    }

    override fun onDestroy() {
        super.onDestroy()
        mHandler.removeCallbacks(mRunnable)
    }

    fun showComment() {
        var commentFrag = supportFragmentManager.findFragmentByTag("comment")
        if (commentFrag == null) {
            commentFrag = CommentsFragment()
            supportFragmentManager.beginTransaction()
                .setCustomAnimations(R.anim.slide_in_right, R.anim.slide_out_right)
                .add(R.id.navHostFragment, commentFrag, "comment")
                .addToBackStack(null).commit()
        } else {
            supportFragmentManager.beginTransaction()
                .setCustomAnimations(R.anim.slide_in_right, R.anim.slide_out_right)
                .show(commentFrag)
                .runOnCommit { commentFrag.onResume() }
                .commit()
        }
    }

    fun hideComment(): Boolean {
        supportFragmentManager.findFragmentByTag("comment")?.let {
            if (!it.isHidden) {
                it.onPause()
                supportFragmentManager.beginTransaction()
                    .setCustomAnimations(R.anim.slide_in_right, R.anim.slide_out_right)
                    .hide(it)
                    .commit()
                return true
            }
        }
        return false
    }
}
