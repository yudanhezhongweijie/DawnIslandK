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

package com.laotoua.dawnislandk.screens

import android.animation.Animator
import android.animation.TypeEvaluator
import android.animation.ValueAnimator
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.view.ViewPropertyAnimator
import android.widget.ImageView
import android.widget.Toast
import androidx.activity.viewModels
import androidx.core.net.toUri
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.navigation.NavDestination
import androidx.navigation.findNavController
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.setupActionBarWithNavController
import androidx.navigation.ui.setupWithNavController
import com.afollestad.materialdialogs.MaterialDialog
import com.afollestad.materialdialogs.checkbox.checkBoxPrompt
import com.afollestad.materialdialogs.checkbox.isCheckPromptChecked
import com.google.android.material.animation.AnimationUtils
import com.laotoua.dawnislandk.DawnApp.Companion.applicationDataStore
import com.laotoua.dawnislandk.MainNavDirections
import com.laotoua.dawnislandk.R
import com.laotoua.dawnislandk.data.local.entity.Community
import com.laotoua.dawnislandk.databinding.ActivityMainBinding
import com.laotoua.dawnislandk.screens.util.ToolBar.immersiveToolbar
import com.laotoua.dawnislandk.screens.util.ToolBar.immersiveToolbarInitialization
import com.laotoua.dawnislandk.screens.widgets.DoubleClickListener
import com.laotoua.dawnislandk.screens.widgets.popups.ForumDrawerPopup
import com.laotoua.dawnislandk.util.DataResource
import com.laotoua.dawnislandk.util.LoadingStatus
import com.laotoua.dawnislandk.util.lazyOnMainOnly
import com.lxj.xpopup.XPopup
import com.lxj.xpopup.core.BasePopupView
import com.lxj.xpopup.enums.PopupPosition
import com.lxj.xpopup.interfaces.SimpleCallback
import dagger.android.support.DaggerAppCompatActivity
import kotlinx.coroutines.launch
import timber.log.Timber
import java.net.URLDecoder
import javax.inject.Inject
import kotlin.math.max


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
    private var currentAnimatorSet: ViewPropertyAnimator? = null

    private val forumDrawer by lazyOnMainOnly {
        ForumDrawerPopup(
            this,
            sharedVM
        )
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.menu_activity_main, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            android.R.id.home -> {
                findNavController(R.id.navHostFragment).popBackStack()
            }
        }
        return false
    }

    init {
        // load Resources
        lifecycleScope.launchWhenCreated { loadResources() }
    }

    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
        handleIntentFilterNavigation(intent)
    }

    // uses to display fab menu if it exists
    private var currentFragmentId: Int = 0
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityMainBinding.inflate(layoutInflater)
        binding.toolbar.apply {
            immersiveToolbar()
            setSubtitle(R.string.toolbar_subtitle)
        }
        immersiveToolbarInitialization()
        customToolbarBackground()
        setContentView(binding.root)
        setSupportActionBar(binding.toolbar)


        bindNavBarAndNavController()

        handleIntentFilterNavigation(intent)

        sharedVM.communityList.observe(this, Observer<DataResource<List<Community>>> {
            if (it.status == LoadingStatus.ERROR) {
                Toast.makeText(this, it.message, Toast.LENGTH_LONG).show()
                return@Observer
            }
            if (it.data.isNullOrEmpty()) return@Observer
            forumDrawer.setData(it.data)
            sharedVM.setForumMappings(it.data)
            if (sharedVM.selectedForumId.value == null) sharedVM.setForumId(applicationDataStore.getDefaultForumId())
            Timber.i("Loaded ${it.data.size} communities to Adapter")
        })

        sharedVM.reedPictureUrl.observe(this, Observer<String> {
            forumDrawer.setReedPicture(it)
        })

        sharedVM.selectedForumId.observe(this, Observer<String> {
            if (currentFragmentId == R.id.postsFragment) {
                setToolbarTitle(sharedVM.getForumDisplayName(it))
            }
        })
    }

    private fun handleIntentFilterNavigation(intent: Intent?) {
        val action: String? = intent?.action
        val data: Uri? = intent?.data
        if (action == Intent.ACTION_VIEW && data != null) {
            val path = data.path
            if (path.isNullOrBlank()) return
            val count = path.filter { it == '/' }.count()
            val raw = data.toString().substringAfterLast("/")
            if (raw.isNotBlank()) {
                val id = if (raw.contains("?")) raw.substringBefore("?") else raw
                if (count == 1) {
                    sharedVM.setForumId(id)
                } else if (count == 2) {
                    if (path[1] == 't') {
                        val navAction = MainNavDirections.actionGlobalCommentsFragment(id, "")
                        val navHostFragment =
                            supportFragmentManager.findFragmentById(R.id.navHostFragment)
                        if (navHostFragment is NavHostFragment) {
                            navHostFragment.navController.navigate(navAction)
                        }
                    } else if (path[1] == 'f') {
                        val fid = sharedVM.getForumIdByName(URLDecoder.decode(id, "UTF-8"))
                        sharedVM.setForumId(fid)
                    }
                }
            }
        }
    }

    fun showDrawer() {
        XPopup.Builder(this)
            .setPopupCallback(object : SimpleCallback() {
                override fun beforeShow(popupView: BasePopupView?) {
                    super.beforeShow(popupView)
                    forumDrawer.loadReedPicture()
                }
            })
            .popupPosition(PopupPosition.Left)
            .asCustom(forumDrawer)
            .show()
    }

    // initialize Global resources
    private suspend fun loadResources() {
        applicationDataStore.getLatestRelease()?.let { release ->
            if (this.isFinishing) return@let
            MaterialDialog(this).show {
                title(R.string.found_new_version)
                icon(R.mipmap.ic_launcher)
                message(text = release.message) { html() }
                positiveButton(R.string.download_from_github) {
                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse(release.downloadUrl))
                    if (intent.resolveActivity(packageManager) != null) {
                        startActivity(intent)
                    }
                }
                negativeButton(R.string.download_from_google_play) {
                    val intent = Intent(
                        Intent.ACTION_VIEW,
                        Uri.parse("https://play.google.com/store/apps/details?id=com.laotoua.dawnislandk")
                    )
                    if (intent.resolveActivity(packageManager) != null) {
                        startActivity(intent)
                    }
                }

                @Suppress("DEPRECATION")
                neutralButton(R.string.acknowledge) {
                    dismiss()
                }
            }
        }

        applicationDataStore.loadCookies()
        applicationDataStore.getLatestNMBNotice()?.let { notice ->
            if (this.isFinishing) return@let
            MaterialDialog(this).show {
                title(res = R.string.announcement)
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

        applicationDataStore.getLatestLuweiNotice()?.let { luweiNotice ->
            sharedVM.setLuweiLoadingBible(luweiNotice.loadingMsgs)
        }

        // first time app entry
        applicationDataStore.getFirstTimeUse().let {
            if (it) {
                if (this.isFinishing) return@let
                MaterialDialog(this).show {
                    title(res = R.string.announcement)
                    checkBoxPrompt(R.string.acknowledge) {}
                    cancelOnTouchOutside(false)
                    message(R.string.entry_message)
                    positiveButton(R.string.close) {
                        if (isCheckPromptChecked()) {
                            applicationDataStore.setFirstTimeUse()
                        }
                    }
                }
            }
        }
    }

    private fun bindNavBarAndNavController() {
        val navHostFragment = supportFragmentManager.findFragmentById(R.id.navHostFragment)
        if (navHostFragment is NavHostFragment) {
            val navController = navHostFragment.navController
            navController.addOnDestinationChangedListener { _, destination, _ ->
                currentFragmentId = destination.id
                updateTitleAndBottomNav(destination)
            }
            binding.bottomNavBar.setOnNavigationItemReselectedListener { item: MenuItem ->
                if (item.itemId == R.id.postsFragment && currentFragmentId == R.id.postsFragment) showDrawer()
            }
            binding.bottomNavBar.setupWithNavController(navController)
            // up button
            val appBarConfiguration = AppBarConfiguration(
                setOf(
                    R.id.postsFragment,
                    R.id.subscriptionPagerFragment,
                    R.id.historyPagerFragment,
                    R.id.profileFragment
                ),
                null
            )
            setupActionBarWithNavController(navController, appBarConfiguration)
        }
    }

    override fun onBackPressed() {
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

    fun hideNav() {
        if (currentState == NavScrollSate.DOWN) return
        if (currentAnimatorSet != null) {
            currentAnimatorSet!!.cancel()
        }
        currentState = NavScrollSate.DOWN
        currentAnimatorSet = binding.bottomNavBar.animate().apply {
            alpha(0f)
            translationY(binding.bottomNavBar.height.toFloat())
            duration = 250
            interpolator = AnimationUtils.FAST_OUT_LINEAR_IN_INTERPOLATOR
            setListener(object : Animator.AnimatorListener {
                override fun onAnimationRepeat(animation: Animator?) {}
                override fun onAnimationEnd(animation: Animator?) {
                    currentAnimatorSet = null
                    binding.bottomNavBar.visibility = View.GONE
                }

                override fun onAnimationCancel(animation: Animator?) {}
                override fun onAnimationStart(animation: Animator?) {}
            })
        }
        currentAnimatorSet!!.start()
    }

    fun showNav() {
        if (currentState == NavScrollSate.UP) return
        if (currentAnimatorSet != null) {
            currentAnimatorSet!!.cancel()
        }
        currentState = NavScrollSate.UP
        binding.bottomNavBar.visibility = View.VISIBLE
        currentAnimatorSet = binding.bottomNavBar.animate().apply {
            alpha(1f)
            translationY(0f)
            duration = 250
            interpolator = AnimationUtils.LINEAR_OUT_SLOW_IN_INTERPOLATOR
            setListener(object : Animator.AnimatorListener {
                override fun onAnimationRepeat(animation: Animator?) {}
                override fun onAnimationEnd(animation: Animator?) {
                    currentAnimatorSet = null
                }

                override fun onAnimationCancel(animation: Animator?) {}
                override fun onAnimationStart(animation: Animator?) {}
            })
        }
        currentAnimatorSet!!.start()
    }

    fun setToolbarClickListener(listener: () -> Unit) {
        binding.toolbar.setOnClickListener(
            DoubleClickListener(callback = object : DoubleClickListener.DoubleClickCallBack {
                override fun doubleClicked() {
                    listener.invoke()
                }
            })
        )
    }

    private var oldTitle = ""
    private var toolbarAnim: Animator? = null
    fun setToolbarTitle(newTitle: String) {
        if (oldTitle == newTitle) return
        toolbarAnim?.cancel()
        val animCharCount = max(oldTitle.length, newTitle.length)
        toolbarAnim = ValueAnimator.ofObject(
            ToolbarTitleEvaluator(animCharCount),
            StringBuilder(oldTitle),
            StringBuilder(newTitle)
        ).apply {
            duration = animCharCount.toLong() * 80
            addUpdateListener {
                binding.toolbar.title = it.animatedValue.toString()
                binding.toolbar.invalidate()
            }
        }
        oldTitle = newTitle
        toolbarAnim?.start()
    }

    fun setToolbarTitle(resId: Int) {
        val text = getText(resId).toString()
        setToolbarTitle(text)
    }

    private class ToolbarTitleEvaluator(private val animCharCount: Int) : TypeEvaluator<StringBuilder> {
        override fun evaluate(
            fraction: Float,
            startValue: StringBuilder,
            endValue: StringBuilder
        ): StringBuilder {
            val ind = (fraction * animCharCount).toInt()
            for (i in 0 .. ind) {
                val newChar = if (i >= endValue.length) ' ' else endValue[i]
                if (i < startValue.length) startValue.setCharAt(i, newChar)
                else startValue.append(newChar)
            }
            return startValue
        }
    }

    private fun updateTitleAndBottomNav(destination: NavDestination) {
        return when (destination.id) {
            R.id.postsFragment -> {
                sharedVM.selectedForumId.value.let {
                    if (it != null) setToolbarTitle(sharedVM.getForumDisplayName(it))
                }
                showNav()
            }
            R.id.historyPagerFragment, R.id.subscriptionPagerFragment -> {
                showNav()
            }
            R.id.searchFragment -> {
                setToolbarTitle(R.string.search)
                showNav()
            }
            R.id.commentsFragment -> {
                hideNav()
            }

            R.id.aboutFragment -> {
                setToolbarTitle(R.string.about)
                hideNav()
            }
            R.id.commonForumsFragment -> {
                setToolbarTitle(R.string.common_forum_setting)
                hideNav()
            }
            R.id.commonPostsFragment -> {
                setToolbarTitle(R.string.common_posts_setting)
                hideNav()
            }
            R.id.customSettingFragment -> {
                setToolbarTitle(R.string.custom_settings)
                hideNav()
            }
            R.id.displaySettingFragment -> {
                setToolbarTitle(R.string.display_settings)
                hideNav()
            }
            R.id.generalSettingFragment -> {
                setToolbarTitle(R.string.general_settings)
                hideNav()
            }
            R.id.profileFragment -> {
                setToolbarTitle(R.string.my_profile)
                showNav()
            }
            R.id.sizeCustomizationFragment -> {
                setToolbarTitle(R.string.layout_customization)
                hideNav()
            }
            R.id.notificationFragment -> {
                setToolbarTitle(R.string.feed_notification)
                hideNav()
            }
            else -> {
                Timber.e("Unhandled destination navigation $destination")
            }
        }
    }


    private fun customToolbarBackground() {
        if (applicationDataStore.getCustomToolbarImageStatus()) {
            try {
                val path = applicationDataStore.getCustomToolbarImagePath().toUri()
                binding.imageView.setImageURI(path)
                binding.imageView.scaleType = ImageView.ScaleType.CENTER_CROP
            } catch (e:Exception){
                Toast.makeText(this, R.string.toolbar_customization_error, Toast.LENGTH_SHORT).show()
            }
        }
    }
}
