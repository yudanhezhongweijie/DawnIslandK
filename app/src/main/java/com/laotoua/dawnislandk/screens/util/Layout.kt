package com.laotoua.dawnislandk.screens.util

import android.content.Context
import android.util.DisplayMetrics
import android.util.TypedValue
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.laotoua.dawnislandk.screens.adapters.QuickAdapter
import com.laotoua.dawnislandk.util.EventPayload
import com.laotoua.dawnislandk.util.LoadingStatus
import me.dkzwm.widget.srl.SmoothRefreshLayout
import me.dkzwm.widget.srl.config.Constants

object Layout {
    fun dip2px(context: Context, dipValue: Float): Int {
        val displayMetrics: DisplayMetrics = context.resources.displayMetrics
        return TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_DIP,
            dipValue,
            displayMetrics
        ).toInt()
    }

    fun dp2pix(context: Context, dp: Float): Int {
        return (dp * context.resources.displayMetrics.density + 0.5f).toInt()
    }

    fun pix2dp(context: Context, pix: Int): Float {
        return pix / context.resources.displayMetrics.density
    }

    fun <AdapterType, PayloadType> Fragment.updateHeaderAndFooter(
        refreshLayout: SmoothRefreshLayout,
        mAdapter: QuickAdapter<AdapterType>,
        event: EventPayload<PayloadType>
    ) {
        when (event.loadingStatus) {
            LoadingStatus.FAILED -> {
                refreshLayout.refreshComplete(false, 5L)
                mAdapter.loadMoreModule.loadMoreFail()
                Toast.makeText(
                    context,
                    event.message,
                    Toast.LENGTH_LONG
                ).show()
            }
            LoadingStatus.NODATA -> {
                refreshLayout.refreshComplete(true, 100L)
                mAdapter.loadMoreModule.loadMoreEnd()
                if (event.message != null) {
                    Toast.makeText(
                        context,
                        event.message,
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
            LoadingStatus.SUCCESS -> {
                refreshLayout.refreshComplete(true, 100L)
                mAdapter.loadMoreModule.loadMoreComplete()
            }
            LoadingStatus.LOADING -> {
                // show indicator if applicable
                if (this.isVisible && !mAdapter.loadMoreModule.isLoading && !refreshLayout.isRefreshing) {
                    refreshLayout.autoRefresh(Constants.ACTION_NOTHING, false)
                }
            }

        }

    }
}