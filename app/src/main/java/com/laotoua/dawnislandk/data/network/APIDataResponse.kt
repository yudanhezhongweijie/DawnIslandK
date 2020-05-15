package com.laotoua.dawnislandk.data.network

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.ResponseBody
import org.apache.commons.text.StringEscapeUtils
import retrofit2.Call
import timber.log.Timber

sealed class APIDataResponse<out T> {
    abstract val message: String
    abstract val data: T?

    companion object {
        suspend fun <T> create(
            call: Call<ResponseBody>,
            parser: ((String) -> T)
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
                            Timber.i("Trying to parse response with supplied parser...")
                            APISuccessDataResponse("Parse success", parser(resBody))
                        } catch (e: Exception) {
                            // server returns non json string
                            Timber.i("Response is non JSON data...")
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
) :
    APIDataResponse<T>()

data class APIErrorDataResponse<T>(
    override val message: String,
    override val data: Nothing? = null
) :
    APIDataResponse<T>()

data class APISuccessDataResponse<T>(override val message: String, override val data: T) :
    APIDataResponse<T>()
