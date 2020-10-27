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
 * Create by lxj, at 2019/3/4
 */
public enum ImageType {
    GIF(true),
    JPEG(false),
    RAW(false),
    /**
     * PNG type with alpha.
     */
    PNG_A(true),
    /**
     * PNG type without alpha.
     */
    PNG(false),
    /**
     * WebP type with alpha.
     */
    WEBP_A(true),
    /**
     * WebP type without alpha.
     */
    WEBP(false),
    /**
     * Unrecognized type.
     */
    UNKNOWN(false);

    private final boolean hasAlpha;

    ImageType(boolean hasAlpha) {
        this.hasAlpha = hasAlpha;
    }

    public boolean hasAlpha() {
        return hasAlpha;
    }
}