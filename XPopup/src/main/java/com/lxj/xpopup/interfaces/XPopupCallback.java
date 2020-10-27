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

package com.lxj.xpopup.interfaces;

import com.lxj.xpopup.core.BasePopupView;

/**
 * Description: XPopup显示和隐藏的回调接口，如果你不想重写3个方法，则可以使用SimpleCallback，
 * 它是一个默认实现类
 * Create by dance, at 2018/12/21
 */
public interface XPopupCallback {
    /**
     * 弹窗的onCreate方法执行完调用
     */
    void onCreated(BasePopupView popupView);

    /**
     * 在show之前执行，由于onCreated只执行一次，如果想多次更新数据可以在该方法中
     */
    void beforeShow(BasePopupView popupView);

    /**
     * 完全显示的时候执行
     */
    void onShow(BasePopupView popupView);

    /**
     * 完全消失的时候执行
     */
    void onDismiss(BasePopupView popupView);

    /**
     * 准备消失的时候执行
     */
    void beforeDismiss(BasePopupView popupView);

    /**
     * 暴漏返回按键的处理，如果返回true，XPopup不会处理；如果返回false，XPopup会处理，
     *
     * @return
     */
    boolean onBackPressed(BasePopupView popupView);
}
