package com.laotoua.dawnislandk.ui.util

import android.widget.Toast
import androidx.fragment.app.Fragment
import com.laotoua.dawnislandk.ui.adapter.QuickAdapter
import com.laotoua.dawnislandk.viewmodel.EventPayload
import com.laotoua.dawnislandk.viewmodel.LoadingStatus
import me.dkzwm.widget.srl.SmoothRefreshLayout
import me.dkzwm.widget.srl.config.Constants


object UIUtils {

    fun <T> Fragment.updateHeaderAndFooter(
        refreshLayout: SmoothRefreshLayout,
        mAdapter: QuickAdapter,
        event: EventPayload<T>
    ) {
        when (event.loadingStatus) {
            LoadingStatus.FAILED -> {
                refreshLayout.refreshComplete(false)
                mAdapter.loadMoreModule.loadMoreFail()
                Toast.makeText(
                    context,
                    event.message,
                    Toast.LENGTH_LONG
                ).show()
            }
            LoadingStatus.NODATA -> {
                refreshLayout.refreshComplete()
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
                refreshLayout.refreshComplete()
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