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
 * Description:
 * Create by dance, at 2019/6/13
 */
public class SimpleCallback implements XPopupCallback {
    @Override
    public void onCreated(BasePopupView popupView) {

    }

    @Override
    public void beforeShow(BasePopupView popupView) {

    }

    @Override
    public void onShow(BasePopupView popupView) {

    }

    @Override
    public void onDismiss(BasePopupView popupView) {

    }

    @Override
    public void beforeDismiss(BasePopupView popupView) {

    }

    @Override
    public boolean onBackPressed(BasePopupView popupView) {
        return false;
    }
}
