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

import android.animation.FloatEvaluator;
import android.graphics.Bitmap;
import android.graphics.PorterDuff;
import android.graphics.drawable.BitmapDrawable;
import android.view.View;

import com.lxj.xpopup.XPopup;
import com.lxj.xpopup.util.XPopupUtils;

/**
 * Description: 背景Shadow动画器，负责执行半透明的渐入渐出动画
 * Create by dance, at 2018/12/9
 */
public class BlurAnimator extends PopupAnimator {

    private final FloatEvaluator evaluate = new FloatEvaluator();

    public BlurAnimator(View target) {
        super(target);
    }

    public Bitmap decorBitmap;
    public boolean hasShadowBg = false;

    public BlurAnimator() {
    }

    @Override
    public void initAnimator() {
        Bitmap blurBmp = XPopupUtils.renderScriptBlur(targetView.getContext(), decorBitmap, 25, true);
        BitmapDrawable drawable = new BitmapDrawable(targetView.getResources(), blurBmp);
        if (hasShadowBg) drawable.setColorFilter(XPopup.getShadowBgColor(), PorterDuff.Mode.SRC_OVER);
        targetView.setBackground(drawable);
    }

    @Override
    public void animateShow() {
//        ValueAnimator animator = ValueAnimator.ofFloat(0,1);
//        animator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
//            @Override
//            public void onAnimationUpdate(ValueAnimator animation) {
//                float fraction = animation.getAnimatedFraction();
//                Bitmap blurBmp = ImageUtils.renderScriptBlur(decorBitmap, evaluate.evaluate(0f, 25f, fraction), false);
//                targetView.setBackground(new BitmapDrawable(targetView.getResources(), blurBmp));
//            }
//        });
//        animator.setInterpolator(new LinearInterpolator());
//        animator.setDuration(XPopup.getAnimationDuration()).start();
    }

    @Override
    public void animateDismiss() {
//        ValueAnimator animator = ValueAnimator.ofFloat(1,0);
//        animator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
//            @Override
//            public void onAnimationUpdate(ValueAnimator animation) {
//                float fraction = animation.getAnimatedFraction();
//                Bitmap blurBmp = ImageUtils.renderScriptBlur(decorBitmap, evaluate.evaluate(0f, 25f, fraction), false);
//                targetView.setBackground(new BitmapDrawable(targetView.getResources(), blurBmp));
//            }
//        });
//        animator.setInterpolator(new LinearInterpolator());
//        animator.setDuration(XPopup.getAnimationDuration()).start();
    }


}
