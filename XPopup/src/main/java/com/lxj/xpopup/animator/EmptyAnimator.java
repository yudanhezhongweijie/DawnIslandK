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

package com.lxj.xpopup.animator;

import android.view.View;

import com.lxj.xpopup.XPopup;

/**
 * Description: 没有动画效果的动画器
 * Create by dance, at 2019/6/6
 */
public class EmptyAnimator extends PopupAnimator {
    public EmptyAnimator(View target) {
        super(target, null);
    }

    @Override
    public void initAnimator() {
        targetView.setAlpha(0);
    }

    @Override
    public void animateShow() {
        targetView.animate().alpha(1f).setDuration(XPopup.getAnimationDuration()).withLayer().start();
    }

    @Override
    public void animateDismiss() {
        targetView.animate().alpha(0f).setDuration(XPopup.getAnimationDuration()).withLayer().start();
    }
}
