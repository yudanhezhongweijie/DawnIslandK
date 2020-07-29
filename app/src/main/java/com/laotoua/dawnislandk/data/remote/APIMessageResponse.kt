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

import com.laotoua.dawnislandk.util.LoadingStatus
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.ResponseBody
import org.apache.commons.text.StringEscapeUtils
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import retrofit2.Call
import timber.log.Timber

sealed class APIMessageResponse(
    val status: LoadingStatus,
    val message: String,
    val dom: Document? = null
) {

    /**
     * separate class for HTTP 204 responses so that we can make ApiSuccessResponse's body non-null.
     */
    class Empty : APIMessageResponse(LoadingStatus.NO_DATA, "EmptyResponse")

    class Error(message: String, dom: Document? = null) :
        APIMessageResponse(LoadingStatus.ERROR, message, dom)

    class Success(
        val messageType: MessageType,
        message: String,
        dom: Document? = null
    ) : APIMessageResponse(LoadingStatus.SUCCESS, message, dom)

    enum class MessageType {
        HTML,
        String
    }

    companion object {
        suspend fun create(call: Call<ResponseBody>): APIMessageResponse {
            try {
                // html test
                val regex = "[\\S\\s]*<html[\\S\\s]*>[\\S\\s]*</html[\\S\\s]*>[\\S\\s]*".toRegex()
                val response = withContext(Dispatchers.IO) { call.execute() }

                if (response.isSuccessful) {
                    val body = response.body()
                    body?.close()
                    if (body == null || response.code() == 204) {
                        return Empty()
                    }
                    val resBody = withContext(Dispatchers.IO) { body.string() }
                    return withContext(Dispatchers.Default) {
                        if (regex.containsMatchIn(resBody)) {
                            Success(
                                MessageType.HTML,
                                "HTML Response",
                                dom = Jsoup.parse(resBody)
                            )
                        } else {
                            Success(
                                MessageType.String, StringEscapeUtils.unescapeJava(
                                    resBody.replace("\"", "")
                                )
                            )
                        }
                    }
                }

                Timber.e("Response is unsuccessful...")
                return withContext(Dispatchers.IO) {
                    val msg = response.errorBody()?.string()
                    val errorMsg = if (msg.isNullOrEmpty()) {
                        response.message()
                    } else {
                        msg
                    }
                    Timber.e(errorMsg)
                    val dom = if (regex.containsMatchIn(errorMsg)) Jsoup.parse(errorMsg) else null
                    Error(errorMsg ?: "unknown error", dom)
                }
            } catch (e: Exception) {
                Timber.e(e)
                return Error(e.toString())
            }
        }
    }
}