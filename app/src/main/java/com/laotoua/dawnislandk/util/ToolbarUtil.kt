package com.laotoua.dawnislandk.util

import android.app.Activity
import androidx.coordinatorlayout.widget.CoordinatorLayout
import androidx.core.view.GravityCompat
import androidx.drawerlayout.widget.DrawerLayout
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.google.android.material.appbar.AppBarLayout
import com.laotoua.dawnislandk.R
import kotlinx.android.synthetic.main.activity_main.*


object ToolbarUtil {
    private fun disableCollapse(activity: Activity, title: String, subtitle: String? = "") {
        val dawnAppbar = activity.dawnAppbar
        val collapsingToolbar = activity.collapsingToolbar
        val toolbar = activity.toolbar
        dawnAppbar.isActivated = false

        //you will need to hide also all content inside CollapsingToolbarLayout
        //plus you will need to hide title of it
//        backdrop.setVisibility(View.GONE)
//        shadow.setVisibility(View.GONE)
        collapsingToolbar.isTitleEnabled = false
        val p =
            collapsingToolbar.layoutParams as AppBarLayout.LayoutParams
        p.scrollFlags = 0
        collapsingToolbar.layoutParams = p
        collapsingToolbar.isActivated = false
        val lp =
            dawnAppbar.layoutParams as CoordinatorLayout.LayoutParams
        lp.height =
            activity.resources.getDimensionPixelSize(R.dimen.toolbar_height) + getStatusBarHeight(
                activity
            )
        dawnAppbar.requestLayout()

        //you also have to setTitle for toolbar
        toolbar.title = title
        toolbar.subtitle = subtitle
    }

    private fun enableCollapse(activity: Activity, title: String) {
        val dawnAppbar = activity.dawnAppbar
        val collapsingToolbar = activity.collapsingToolbar
        val toolbar = activity.toolbar
        dawnAppbar.isActivated = true
        collapsingToolbar.isActivated = true

        //you will need to show now all content inside CollapsingToolbarLayout
        //plus you will need to show title of it
//        backdrop.setVisibility(View.VISIBLE)
//        shadow.setVisibility(View.VISIBLE)

        collapsingToolbar.isTitleEnabled = true
        val p =
            collapsingToolbar.layoutParams as AppBarLayout.LayoutParams
        p.scrollFlags =
            AppBarLayout.LayoutParams.SCROLL_FLAG_SCROLL or AppBarLayout.LayoutParams.SCROLL_FLAG_EXIT_UNTIL_COLLAPSED
        collapsingToolbar.layoutParams = p
        val lp =
            activity.dawnAppbar.layoutParams as CoordinatorLayout.LayoutParams
        lp.height = activity.resources.getDimensionPixelSize(R.dimen.toolbar_expanded_height)
        dawnAppbar.setExpanded(false, false)
        activity.dawnAppbar.requestLayout()

        //you also have to setTitle for toolbar
        toolbar.title = title // or getSupportActionBar().setTitle(title);
    }

    private fun getStatusBarHeight(activity: Activity): Int {
        var result = 0
        val resourceId = activity.resources.getIdentifier("status_bar_height", "dimen", "android")
        if (resourceId > 0) {
            result = activity.resources.getDimensionPixelSize(resourceId)
        }
        return result
    }

    fun Activity.updateAppBarByFragment(
        callerFragment: Fragment,
        title: String,
        subtitle: String? = null
    ) {
        run {
            when (callerFragment.javaClass.simpleName) {
                "ThreadFragment" -> {
                    drawerLayout.setDrawerLockMode(DrawerLayout.LOCK_MODE_UNLOCKED)
                    enableCollapse(
                        this,
                        title
                    )
                    toolbar.setNavigationIcon(R.drawable.ic_menu_white_24dp)
                    toolbar.setNavigationOnClickListener(null)
                    toolbar.setNavigationOnClickListener {
                        drawerLayout.openDrawer(GravityCompat.START)
                    }
                }
                "ReplyFragment" -> {
                    drawerLayout.setDrawerLockMode(DrawerLayout.LOCK_MODE_LOCKED_CLOSED)
                    disableCollapse(
                        this,
                        title,
                        subtitle
                    )
                    toolbar.setNavigationIcon(R.drawable.ic_arrow_back_white_24dp)
                    toolbar.setNavigationOnClickListener(null)
                    toolbar.setNavigationOnClickListener {
                        callerFragment.findNavController().popBackStack()
                    }
                }
                "SettingsFragment" -> {
                    drawerLayout.setDrawerLockMode(DrawerLayout.LOCK_MODE_LOCKED_CLOSED)
                    disableCollapse(
                        this,
                        title
                    )
                    toolbar.setNavigationIcon(R.drawable.ic_arrow_back_white_24dp)
                    toolbar.setNavigationOnClickListener(null)
                    toolbar.setNavigationOnClickListener {
                        callerFragment.findNavController().popBackStack()
                    }
                }
                "SizeCustomizationFragment" -> {
                    drawerLayout.setDrawerLockMode(DrawerLayout.LOCK_MODE_LOCKED_CLOSED)
                    disableCollapse(
                        this,
                        title
                    )
                    toolbar.setNavigationIcon(R.drawable.ic_arrow_back_white_24dp)
                    toolbar.setNavigationOnClickListener(null)
                    toolbar.setNavigationOnClickListener {
                        callerFragment.findNavController().popBackStack()
                    }
                }
            }
        }
    }

    // special handler for forum change
    fun Activity.updateAppBarTitleWithinFragment(title: String) {
        collapsingToolbar.title = title
    }
}