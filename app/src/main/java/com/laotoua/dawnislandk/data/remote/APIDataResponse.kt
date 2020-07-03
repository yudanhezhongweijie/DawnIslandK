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

package com.laotoua.dawnislandk.data.remote

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.ResponseBody
import org.apache.commons.text.StringEscapeUtils
import retrofit2.Call
import timber.log.Timber

sealed class APIDataResponse<out T> {
    abstract val message: String
    abstract val data: T?

    /**
     * separate class for HTTP 204 responses so that we can make ApiSuccessResponse's body non-null.
     */
    class APIEmptyDataResponse<T>(
        override val message: String = "EmptyResponse",
        override val data: Nothing? = null
    ) : APIDataResponse<T>()

    data class APIBlankDataResponse<T>(
        override val message: String,
        override val data: Nothing? = null
    ) : APIDataResponse<T>()

    data class APIErrorDataResponse<T>(
        override val message: String,
        override val data: Nothing? = null
    ) : APIDataResponse<T>()

    data class APISuccessDataResponse<T>(override val message: String, override val data: T) :
        APIDataResponse<T>()

    companion object {
        suspend fun <T> create(
            call: Call<ResponseBody>,
            parser: NMBJsonParser<T>
        ): APIDataResponse<T> {
            try {
                val response = withContext(Dispatchers.IO) { call.execute() }

                if (response.isSuccessful) {
                    val body = response.body()
                    body?.close()
                    if (body == null || response.code() == 204) {
                        return APIEmptyDataResponse()
                    }
                    val resBody = withContext(Dispatchers.IO) { body.string() }

                    return withContext(Dispatchers.Default) {
                        try {
                            Timber.d("Trying to parse response with supplied parser...")
                            APISuccessDataResponse("Parse success", parser.parse(resBody))
                        } catch (e: Exception) {
                            // server returns non json string
                            Timber.d("Response is non JSON data...")
                            APIBlankDataResponse<Nothing>(
                                StringEscapeUtils.unescapeJava(
                                    resBody.replace("\"", "")
                                )
                            )
                        }
                    }

                }

                return withContext(Dispatchers.IO) {
                    val msg = response.errorBody()?.string()
                    val errorMsg = if (msg.isNullOrEmpty()) {
                        response.message()
                    } else {
                        msg
                    }
                    APIErrorDataResponse<Nothing>(errorMsg ?: "unknown error")
                }
            } catch (e: Exception) {
                return APIErrorDataResponse<Nothing>(e.toString())
            }
        }
    }
}

