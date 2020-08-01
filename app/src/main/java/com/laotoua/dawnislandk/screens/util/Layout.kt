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

package com.laotoua.dawnislandk.screens.util

import android.content.Context
import android.util.DisplayMetrics
import android.util.TypedValue
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.chad.library.adapter.base.BaseQuickAdapter
import com.chad.library.adapter.base.viewholder.BaseViewHolder
import com.laotoua.dawnislandk.R
import com.laotoua.dawnislandk.databinding.ListItemPreferenceBinding
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
        mAdapter: BaseQuickAdapter<AdapterType, BaseViewHolder>,
        event: EventPayload<PayloadType>
    ) {
        val headerDismissalDelayDuration = 200L
        when (event.loadingStatus) {
            // TODO: stick failure message on header or footer instead of toast
            LoadingStatus.ERROR -> {
                refreshLayout.refreshComplete(false, headerDismissalDelayDuration)
                mAdapter.loadMoreModule.loadMoreFail()
                if (mAdapter.data.isNullOrEmpty()) {
                    if (!mAdapter.hasEmptyView()) mAdapter.setEmptyView(R.layout.view_no_data)
                    mAdapter.setDiffNewData(null)
                }
                if (!event.message.isNullOrBlank()) {
                    toast(event.message, Toast.LENGTH_LONG)
                }
            }
            LoadingStatus.NO_DATA -> {
                refreshLayout.refreshComplete(true, headerDismissalDelayDuration)
                mAdapter.loadMoreModule.loadMoreEnd()
                if (!event.message.isNullOrBlank()) {
                    toast(event.message)
                }
            }
            LoadingStatus.SUCCESS -> {
                refreshLayout.refreshComplete(true, headerDismissalDelayDuration)
                mAdapter.loadMoreModule.loadMoreComplete()
            }
            LoadingStatus.LOADING -> {
                // show indicator if applicable
                if (isVisible && !mAdapter.loadMoreModule.isLoading && !refreshLayout.isRefreshing) {
                    refreshLayout.autoRefresh(Constants.ACTION_NOTHING, false)
                }
                if (!event.message.isNullOrBlank()) {
                    toast(event.message, Toast.LENGTH_LONG)
                }
            }
        }
    }

    fun Fragment.toast(resId:Int, length:Int = Toast.LENGTH_SHORT){
        Toast.makeText(context, resId, length).show()
    }

    fun Fragment.toast(string: String, length:Int = Toast.LENGTH_SHORT){
        Toast.makeText(context, string, length).show()
    }

    fun ListItemPreferenceBinding.updateSwitchSummary(summaryOn: Int, summaryOff: Int) {
        if (preferenceSwitch.isChecked) {
            summary.setText(summaryOn)
        } else {
            summary.setText(summaryOff)
        }
    }
}