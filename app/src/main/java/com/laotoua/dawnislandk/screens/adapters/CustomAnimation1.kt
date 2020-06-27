package com.laotoua.dawnislandk.screens.adapters

import android.animation.Animator
import android.animation.ObjectAnimator
import android.view.View
import android.view.animation.DecelerateInterpolator
import com.chad.library.adapter.base.animation.BaseAnimation


class CustomAnimation1 : BaseAnimation {
    override fun animators(view: View): Array<Animator> {
        val scaleY: Animator = ObjectAnimator.ofFloat(view, "scaleY", 1.3f, 1f)
        val scaleX: Animator = ObjectAnimator.ofFloat(view, "scaleX", 1.3f, 1f)
        val alpha: Animator = ObjectAnimator.ofFloat(view, "alpha", 0f, 1f)
        scaleY.duration = 350
        scaleX.duration = 350
        alpha.duration = 350
        scaleY.interpolator = DecelerateInterpolator()
        scaleX.interpolator = DecelerateInterpolator()
        return arrayOf(scaleY, scaleX, alpha)
    }
}