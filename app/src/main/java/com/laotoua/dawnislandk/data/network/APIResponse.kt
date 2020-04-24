package com.laotoua.dawnislandk.data.network

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.ResponseBody
import org.apache.commons.text.StringEscapeUtils
import retrofit2.Call
import timber.log.Timber

sealed class APIResponse<out T> {
    companion object {
        suspend fun <T> create(
            call: Call<ResponseBody>,
            parser: (String) -> T
        ): APIResponse<T> {
            val response = withContext(Dispatchers.IO) { call.execute() }

            if (response.isSuccessful) {
                val body = response.body()
                body?.close()
                if (body == null || response.code() == 204) {
                    return APIEmptyResponse()
                }
                val resBody = withContext(Dispatchers.IO) { body.string() }
                return withContext(Dispatchers.Default) {
                    try {
                        Timber.i("Trying to parse JSON...")
                        APISuccessResponse(parser(resBody))
                    } catch (e: Exception) {
                        // server returns non json string
                        Timber.i("Response is non JSON data...")
                        APINoDataResponse<Nothing>(
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
                Timber.e(errorMsg)
                APIErrorResponse<Nothing>(errorMsg ?: "unknown error")
            }
        }
    }
}

/**
 * separate class for HTTP 204 responses so that we can make ApiSuccessResponse's body non-null.
 */
class APIEmptyResponse<T> : APIResponse<T>()
data class APINoDataResponse<T>(val errorMessage: String) : APIResponse<T>()
data class APIErrorResponse<T>(val errorMessage: String) : APIResponse<T>()
data class APISuccessResponse<T>(val data: T) : APIResponse<T>()
