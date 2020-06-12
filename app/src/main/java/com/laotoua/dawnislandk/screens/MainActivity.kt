package com.laotoua.dawnislandk.screens

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
import com.laotoua.dawnislandk.screens.replys.QuotePopup
import com.laotoua.dawnislandk.screens.replys.ReplysFragment
import com.laotoua.dawnislandk.screens.util.ContentTransformation.htmlToSpanned
import com.laotoua.dawnislandk.screens.util.ToolBar.immersiveToolbarInitialization
import com.laotoua.dawnislandk.util.LoadingStatus
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
        Timber.i("Clicked on Forum ${forum.name}")
        sharedVM.setForum(forum)
        binding.drawerLayout.closeDrawers()
    }


    // initialize Global resources
    private suspend fun loadResources() {
        applicationDataStore.loadCookies()
        applicationDataStore.initializeFeedId()
        applicationDataStore.getNotice()?.let { notice ->
            MaterialDialog(this).show {
                cornerRadius(res = R.dimen.dialog_radius)
                checkBoxPrompt(R.string.acknowledge) {}
                message(text = htmlToSpanned(notice.content))
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

        // TODO: use luweiNotice. i.e. footer loading text
        applicationDataStore.getLuweiNotice()?.let { luweiNotice ->
            Timber.e(" use luweiNotice")
        }
    }

    override fun onBackPressed() {
        /**
         *  Catch for popup which failed to request focus
         */
        if (!QuotePopup.ensureQuotePopupDismissal()) return

        if (hideReply()) return

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

    fun showReply() {
        var replyFrag = supportFragmentManager.findFragmentByTag("reply")
        if (replyFrag == null) {
            replyFrag = ReplysFragment()
            supportFragmentManager.beginTransaction()
                .setCustomAnimations(R.anim.slide_in_right, R.anim.slide_out_right)
                .add(R.id.navHostFragment, replyFrag, "reply")
                .addToBackStack(null).commit()
        } else {
            supportFragmentManager.beginTransaction()
                .setCustomAnimations(R.anim.slide_in_right, R.anim.slide_out_right)
                .show(replyFrag)
                .runOnCommit { replyFrag.onResume() }
                .commit()
        }
    }

    fun hideReply(): Boolean {
        supportFragmentManager.findFragmentByTag("reply")?.let {
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
