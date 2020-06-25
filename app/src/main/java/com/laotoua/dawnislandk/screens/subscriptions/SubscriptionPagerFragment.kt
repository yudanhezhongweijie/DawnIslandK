package com.laotoua.dawnislandk.screens.subscriptions

import android.view.View
import android.widget.Toast
import com.afollestad.materialdialogs.MaterialDialog
import com.afollestad.materialdialogs.list.listItemsSingleChoice
import com.laotoua.dawnislandk.DawnApp
import com.laotoua.dawnislandk.R
import com.laotoua.dawnislandk.screens.widget.BaseNavFragment
import com.laotoua.dawnislandk.screens.widget.BasePagerFragment

class SubscriptionPagerFragment : BasePagerFragment() {
    private val pageIndices = DawnApp.applicationDataStore.getFeedPagerPageIndices()
    override val pageTitleResIds = mutableMapOf<Int,Int>().apply {
        put(pageIndices.first, R.string.trend)
        put(pageIndices.second, R.string.my_feed)
    }

    override val pageFragmentClass = mutableMapOf<Int, Class<out BaseNavFragment>>().apply {
        put(pageIndices.first, TrendsFragment::class.java)
        put(pageIndices.second, FeedsFragment::class.java)
    }

    override val pageEditorClickListener: View.OnClickListener = View.OnClickListener {
        val items = listOf(
            requireContext().getString(R.string.trend),
            requireContext().getString(R.string.my_feed)
        )
        MaterialDialog(requireContext()).show {
            title(R.string.edit_default_page)
            listItemsSingleChoice(items = items) { _, index, _ ->
                DawnApp.applicationDataStore.setFeedPagerDefaultPage(index, 1 - index)
                Toast.makeText(context, R.string.restart_to_apply_setting, Toast.LENGTH_SHORT)
                    .show()
            }
            positiveButton(R.string.submit)
            negativeButton(R.string.cancel)
        }
    }
}