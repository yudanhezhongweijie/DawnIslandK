package com.laotoua.dawnislandk.ui.util

import android.app.Activity
import android.view.WindowManager

object StatusBarUtil {
    fun immersiveStatusBar(activity: Activity) {
        /**
         * 新的状态栏透明方案
         */

        activity.window.clearFlags(
            WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS
                    or WindowManager.LayoutParams.FLAG_TRANSLUCENT_NAVIGATION
        )
        //设置布局能够延伸到状态栏(StatusBar)和导航栏(NavigationBar)里面
        activity.window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS)
        //设置状态栏(StatusBar)颜色透明
//        window.statusBarColor = Color.TRANSPARENT
    }
}