package com.laotoua.dawnislandk.util

import android.app.Activity
import androidx.coordinatorlayout.widget.CoordinatorLayout
import com.google.android.material.appbar.AppBarLayout
import com.laotoua.dawnislandk.R
import kotlinx.android.synthetic.main.activity_main.*


object ToolbarUtil {
    fun disableCollapse(activity: Activity, title: String) {
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
    }

    fun enableCollapse(activity: Activity, title: String) {
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
}