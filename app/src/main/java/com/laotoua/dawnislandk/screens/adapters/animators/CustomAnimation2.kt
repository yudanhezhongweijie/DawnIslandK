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

package com.laotoua.dawnislandk.screens.adapters.animators

import android.animation.Animator
import android.animation.ObjectAnimator
import android.view.View
import android.view.animation.Interpolator

import com.chad.library.adapter.base.animation.BaseAnimation
import kotlin.math.PI
import kotlin.math.pow


class CustomAnimation2 : BaseAnimation {
    override fun animators(view: View): Array<Animator> {
        val translationX: Animator =
            ObjectAnimator.ofFloat(view, "translationX", -view.rootView.width.toFloat(), 0f)
        translationX.duration = 800
        translationX.interpolator = MyInterpolator2()
        return arrayOf(translationX)
    }

    internal inner class MyInterpolator2 : Interpolator {
        override fun getInterpolation(input: Float): Float {
            val factor = 0.7f
            return ((2.0.pow(-10.0 * input) * kotlin.math.sin((input - factor / 4) * (2 * PI) / factor) + 1).toFloat())
        }
    }
}