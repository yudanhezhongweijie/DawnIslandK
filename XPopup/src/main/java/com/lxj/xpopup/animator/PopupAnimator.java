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
import com.lxj.xpopup.enums.PopupAnimation;

/**
 * Description: 弹窗动画执行器
 * Create by dance, at 2018/12/9
 */
public abstract class PopupAnimator {
    public View targetView;
    public PopupAnimation popupAnimation; // 内置的动画

    public PopupAnimator() {
    }

    public PopupAnimator(View target) {
        this(target, null);
    }

    public PopupAnimator(View target, PopupAnimation popupAnimation) {
        this.targetView = target;
        this.popupAnimation = popupAnimation;
    }

    public abstract void initAnimator();

    public abstract void animateShow();

    public abstract void animateDismiss();

    public int getDuration() {
        return XPopup.getAnimationDuration();
    }
}
