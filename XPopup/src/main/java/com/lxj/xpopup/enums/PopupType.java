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
 * Description: 窗体显示的位置类型
 * Create by lxj, at 2018/12/7
 */
public enum PopupType {
    Center, // 中间显示类型
    Bottom, // 底部弹出的类型
    AttachView,  // 依附于指定View或者指定Point的类型
    ImageViewer,  // 大图浏览类型
    Position  // 自由定位弹窗
}
