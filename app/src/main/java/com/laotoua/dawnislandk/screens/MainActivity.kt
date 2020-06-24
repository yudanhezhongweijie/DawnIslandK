package com.laotoua.dawnislandk.screens

import android.animation.Animator
import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.view.MenuItem
import android.widget.Toast
import androidx.activity.viewModels
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.navigation.findNavController
import com.afollestad.materialdialogs.MaterialDialog
import com.afollestad.materialdialogs.checkbox.checkBoxPrompt
import com.afollestad.materialdialogs.checkbox.isCheckPromptChecked
import com.google.android.material.animation.AnimationUtils
import com.laotoua.dawnislandk.DawnApp.Companion.applicationDataStore
import com.laotoua.dawnislandk.R
import com.laotoua.dawnislandk.data.local.entity.Community
import com.laotoua.dawnislandk.databinding.ActivityMainBinding
import com.laotoua.dawnislandk.screens.comments.CommentsFragment
import com.laotoua.dawnislandk.screens.comments.QuotePopup
import com.laotoua.dawnislandk.screens.util.ToolBar.immersiveToolbarInitialization
import com.laotoua.dawnislandk.screens.widget.popup.ForumDrawerPopup
import com.laotoua.dawnislandk.util.EventPayload
import com.laotoua.dawnislandk.util.LoadingStatus
import com.laotoua.dawnislandk.util.SingleLiveEvent
import com.laotoua.dawnislandk.util.lazyOnMainOnly
import com.lxj.xpopup.XPopup
import com.lxj.xpopup.enums.PopupPosition
import dagger.android.support.DaggerAppCompatActivity
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject


class MainActivity : DaggerAppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    @Inject
    lateinit var viewModelFactory: ViewModelProvider.Factory

    private val sharedVM: SharedViewModel by viewModels { viewModelFactory }

    private var doubleBackToExitPressedOnce = false
    private val mHandler = Handler()
    private val mRunnable = Runnable { doubleBackToExitPressedOnce = false }

    enum class NavScrollSate {
        UP,
        DOWN
    }

    private var currentState: NavScrollSate? = null
    private var currentAnimatorSet: AnimatorSet? = null


    private val forumDrawer by lazyOnMainOnly {
        ForumDrawerPopup(
            this,
            sharedVM
        )
    }

    init {
        // load Resources
        lifecycleScope.launch { loadResources() }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityMainBinding.inflate(layoutInflater)
        immersiveToolbarInitialization()
        setContentView(binding.root)

        var currentNavId = R.id.forum
        binding.bottomNavigation.setOnNavigationItemSelectedListener { item ->
            if (currentNavId == item.itemId) return@setOnNavigationItemSelectedListener true
            when (item.itemId) {
                R.id.forum -> {
                    Timber.d("clicked on forum")
                    currentNavId = R.id.forum
                    findNavController(R.id.navHostFragment).navigate(R.id.action_global_postsFragment)
                    true
                }
                R.id.feed -> {
                    Timber.d("clicked on feed")
                    currentNavId = R.id.feed
                    findNavController(R.id.navHostFragment).navigate(R.id.action_global_feedPagerFragment)
                    true
                }
                R.id.history -> {
                    Timber.d("clicked on history")
                    currentNavId = R.id.history
                    findNavController(R.id.navHostFragment).navigate(R.id.action_global_browsingHistoryFragment)
                    true
                }
                R.id.profile -> {
                    Timber.d("clicked on profile")
                    currentNavId = R.id.profile
                    findNavController(R.id.navHostFragment).navigate(R.id.action_global_settingsFragment)
                    true
                }
                else -> false
            }
        }

        binding.bottomNavigation.setOnNavigationItemReselectedListener { item: MenuItem ->
            if (item.itemId == R.id.forum) {
                showDrawer()
            }
        }
        sharedVM.communityList.observe(this, Observer<List<Community>> {
            if (it.isNullOrEmpty()) return@Observer
            forumDrawer.setData(it)
            Timber.i("Loaded ${it.size} communities to Adapter")
        })
        sharedVM.reedPictureUrl.observe(this, Observer<String> {
            forumDrawer.setReedPicture(it)
        })
        sharedVM.communityListLoadingStatus.observe(this, Observer<SingleLiveEvent<EventPayload<Nothing>>> {
            if (it.getContentIfNotHandled()?.loadingStatus == LoadingStatus.FAILED) {
                Toast.makeText(this, it.peekContent().message, Toast.LENGTH_LONG)
                    .show()
            }
        })
    }

    private fun showDrawer() {
        XPopup.Builder(this)
            .popupPosition(PopupPosition.Left)
            .asCustom(forumDrawer)
            .show()
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

    private val navSlideOutBottomAnimAnim by lazyOnMainOnly {
        ObjectAnimator.ofFloat(
            binding.bottomNavigation,
            "TranslationY",
            binding.bottomNavigation.height.toFloat()
        )
    }

    private val navAlphaOutAnim by lazyOnMainOnly {
        ObjectAnimator.ofFloat(binding.bottomNavigation, "alpha", 0f)
    }

    private val navSlideInBottomAnim by lazyOnMainOnly {
        ObjectAnimator.ofFloat(
            binding.bottomNavigation,
            "TranslationY",
            0f
        )
    }

    private val navAlphaInAnim by lazyOnMainOnly {
        ObjectAnimator.ofFloat(binding.bottomNavigation, "alpha", 1f)
    }

    fun hideNav() {
        if (currentState == NavScrollSate.DOWN) return
        if (currentAnimatorSet != null) {
            currentAnimatorSet!!.cancel()
        }
        currentState = NavScrollSate.DOWN
        currentAnimatorSet = AnimatorSet().apply {
            duration = 250
            interpolator = AnimationUtils.FAST_OUT_LINEAR_IN_INTERPOLATOR
            addListener(object : Animator.AnimatorListener {
                override fun onAnimationRepeat(animation: Animator?) {}
                override fun onAnimationEnd(animation: Animator?) {
                    currentAnimatorSet = null
                }

                override fun onAnimationCancel(animation: Animator?) {}
                override fun onAnimationStart(animation: Animator?) {}
            })
            playTogether(navSlideOutBottomAnimAnim, navAlphaOutAnim)
            start()
        }
    }

    fun showNav() {
        if (currentState == NavScrollSate.UP) return
        if (currentAnimatorSet != null) {
            currentAnimatorSet!!.cancel()
        }
        currentState = NavScrollSate.UP
        currentAnimatorSet = AnimatorSet().apply {
            duration = 250
            interpolator = AnimationUtils.LINEAR_OUT_SLOW_IN_INTERPOLATOR
            addListener(object : Animator.AnimatorListener {
                override fun onAnimationRepeat(animation: Animator?) {}
                override fun onAnimationEnd(animation: Animator?) {
                    currentAnimatorSet = null
                }

                override fun onAnimationCancel(animation: Animator?) {}
                override fun onAnimationStart(animation: Animator?) {}
            })
            playTogether(navSlideInBottomAnim, navAlphaInAnim)
            start()
        }
    }
}
