package com.laotoua.dawnislandk.screens

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.widget.Toast
import androidx.activity.viewModels
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.navigation.findNavController
import com.afollestad.materialdialogs.MaterialDialog
import com.afollestad.materialdialogs.checkbox.checkBoxPrompt
import com.afollestad.materialdialogs.checkbox.isCheckPromptChecked
import com.laotoua.dawnislandk.DawnApp.Companion.applicationDataStore
import com.laotoua.dawnislandk.R
import com.laotoua.dawnislandk.databinding.ActivityMainBinding
import com.laotoua.dawnislandk.screens.comments.CommentsFragment
import com.laotoua.dawnislandk.screens.comments.QuotePopup
import com.laotoua.dawnislandk.screens.util.ToolBar.immersiveToolbarInitialization
import dagger.android.support.DaggerAppCompatActivity
import kotlinx.coroutines.launch
import javax.inject.Inject


class MainActivity : DaggerAppCompatActivity(){

    private lateinit var binding: ActivityMainBinding

    @Inject
    lateinit var viewModelFactory: ViewModelProvider.Factory

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
}
