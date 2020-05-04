package com.laotoua.dawnislandk.ui.util

import android.app.Activity
import android.widget.Toast
import androidx.core.view.GravityCompat
import androidx.drawerlayout.widget.DrawerLayout
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.laotoua.dawnislandk.R
import com.laotoua.dawnislandk.ui.adapter.QuickAdapter
import com.laotoua.dawnislandk.ui.fragment.*
import com.laotoua.dawnislandk.viewmodel.EventPayload
import com.laotoua.dawnislandk.viewmodel.LoadingStatus
import kotlinx.android.synthetic.main.activity_main.*
import me.dkzwm.widget.srl.SmoothRefreshLayout
import me.dkzwm.widget.srl.config.Constants


object UIUtils {

    private fun updateTitleAndSubtitle(
        activity: Activity,
        title: String,
        subtitle: String? = "adnmb.com"
    ) {
        val toolbar = activity.toolbar
        toolbar.title = title
        toolbar.subtitle = subtitle
    }


    fun Activity.updateAppBarByFragment(
        callerFragment: Fragment,
        title: String,
        subtitle: String? = null
    ) {
        run {
            when (callerFragment) {
                is ThreadFragment, is FeedFragment, is TrendFragment -> {
                    drawerLayout.setDrawerLockMode(DrawerLayout.LOCK_MODE_UNLOCKED)
                    updateTitleAndSubtitle(
                        this,
                        title
                    )
                    toolbar.setNavigationIcon(R.drawable.ic_menu_white_24px)
                    toolbar.setNavigationOnClickListener(null)
                    toolbar.setNavigationOnClickListener {
                        drawerLayout.openDrawer(GravityCompat.START)
                    }
                }
                is ReplyFragment -> {
                    drawerLayout.setDrawerLockMode(DrawerLayout.LOCK_MODE_LOCKED_CLOSED)
                    updateTitleAndSubtitle(
                        this,
                        title,
                        subtitle
                    )
                    toolbar.setNavigationIcon(R.drawable.ic_arrow_back_white_24px)
                    toolbar.setNavigationOnClickListener(null)
                    toolbar.setNavigationOnClickListener {
                        callerFragment.findNavController().popBackStack()
                    }
                }
                is SettingsFragment -> {
                    drawerLayout.setDrawerLockMode(DrawerLayout.LOCK_MODE_LOCKED_CLOSED)
                    updateTitleAndSubtitle(
                        this,
                        title,
                        ""
                    )
                    toolbar.setNavigationIcon(R.drawable.ic_arrow_back_white_24px)
                    toolbar.setNavigationOnClickListener(null)
                    toolbar.setNavigationOnClickListener {
                        callerFragment.findNavController().popBackStack()
                    }
                }
                is SizeCustomizationFragment -> {
                    drawerLayout.setDrawerLockMode(DrawerLayout.LOCK_MODE_LOCKED_CLOSED)
                    updateTitleAndSubtitle(
                        this,
                        title,
                        ""
                    )
                    toolbar.setNavigationIcon(R.drawable.ic_arrow_back_white_24px)
                    toolbar.setNavigationOnClickListener(null)
                    toolbar.setNavigationOnClickListener {
                        callerFragment.findNavController().popBackStack()
                    }
                }
            }
        }
    }

    // update forum change when fragment is not changed
    fun Activity.updateAppBarTitleWithinFragment(title: String) {
        updateTitleAndSubtitle(this, title)
    }

    fun <T> Fragment.updateHeaderAndFooter(
        refreshLayout: SmoothRefreshLayout,
        mAdapter: QuickAdapter,
        event: EventPayload<T>
    ) {
        when (event.loadingStatus) {
            LoadingStatus.FAILED -> {
                refreshLayout.refreshComplete(false)
                mAdapter.loadMoreModule.loadMoreFail()
                Toast.makeText(
                    context,
                    event.message,
                    Toast.LENGTH_LONG
                ).show()
            }
            LoadingStatus.NODATA -> {
                refreshLayout.refreshComplete()
                mAdapter.loadMoreModule.loadMoreEnd()
                if (event.message != null) {
                    Toast.makeText(
                        context,
                        event.message,
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
            LoadingStatus.SUCCESS -> {
                refreshLayout.refreshComplete()
                mAdapter.loadMoreModule.loadMoreComplete()
            }
            LoadingStatus.LOADING -> {
                // show indicator if applicable
                if (this.isVisible && !mAdapter.loadMoreModule.isLoading && !refreshLayout.isRefreshing) {
                    refreshLayout.autoRefresh(Constants.ACTION_NOTHING, false)
                }
            }

        }

    }
}