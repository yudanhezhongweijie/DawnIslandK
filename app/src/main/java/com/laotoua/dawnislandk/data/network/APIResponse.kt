package com.laotoua.dawnislandk.data.network

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.ResponseBody
import org.apache.commons.text.StringEscapeUtils
import retrofit2.Call
import timber.log.Timber

sealed class APIResponse<out T> {
    abstract val message: String
    abstract val data: T?

    companion object {
        suspend fun <T> create(
            call: Call<ResponseBody>,
            parser: (String) -> T
        ): APIResponse<T> {
            try {
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
                            APISuccessResponse("JSON success", parser(resBody))
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
            } catch (e: Exception) {
                Timber.e(e)
                return APIErrorResponse<Nothing>(e.toString())
            }
        }
    }
}

/**
 * separate class for HTTP 204 responses so that we can make ApiSuccessResponse's body non-null.
 */
class APIEmptyResponse<T>(
    override val message: String = "EmptyResponse",
    override val data: Nothing? = null
) : APIResponse<T>()

data class APINoDataResponse<T>(override val message: String, override val data: Nothing? = null) :
    APIResponse<T>()

data class APIErrorResponse<T>(override val message: String, override val data: Nothing? = null) :
    APIResponse<T>()

data class APISuccessResponse<T>(override val message: String, override val data: T) :
    APIResponse<T>()
