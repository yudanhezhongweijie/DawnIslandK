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

package com.lxj.xpopup.enums;

/**
 * Description:
 * Create by dance, at 2018/12/9
 */
public enum PopupAnimation {
    // 缩放 + 透明渐变
    ScaleAlphaFromCenter, // 从中心进行缩放+透明渐变
    ScaleAlphaFromLeftTop, //从左上角进行缩放+透明渐变
    ScaleAlphaFromRightTop, //从右上角进行缩放+透明渐变
    ScaleAlphaFromLeftBottom, //从左下角进行缩放+透明渐变
    ScaleAlphaFromRightBottom, //从右下角进行缩放+透明渐变

    // 平移 + 透明渐变
    TranslateAlphaFromLeft,  // 从左平移进入
    TranslateAlphaFromRight, // 从右平移进入
    TranslateAlphaFromTop,   // 从上方平移进入
    TranslateAlphaFromBottom, // 从下方平移进入

    // 平移，不带透明渐变
    TranslateFromLeft,  // 从左平移进入
    TranslateFromRight, // 从右平移进入
    TranslateFromTop,   // 从上方平移进入
    TranslateFromBottom, // 从下方平移进入

    // 滑动 + 透明渐变
    ScrollAlphaFromLeft,
    ScrollAlphaFromLeftTop,
    ScrollAlphaFromTop,
    ScrollAlphaFromRightTop,
    ScrollAlphaFromRight,
    ScrollAlphaFromRightBottom,
    ScrollAlphaFromBottom,
    ScrollAlphaFromLeftBottom,

    //禁用动画
    NoAnimation
}
