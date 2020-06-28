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