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

package com.laotoua.dawnislandk.screens.profile

import android.animation.ValueAnimator
import android.graphics.Canvas
import android.os.Bundle
import android.view.*
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.afollestad.materialdialogs.MaterialDialog
import com.chad.library.adapter.base.BaseQuickAdapter
import com.chad.library.adapter.base.listener.OnItemDragListener
import com.chad.library.adapter.base.listener.OnItemSwipeListener
import com.chad.library.adapter.base.module.DraggableModule
import com.chad.library.adapter.base.viewholder.BaseViewHolder
import com.laotoua.dawnislandk.R
import com.laotoua.dawnislandk.data.local.entity.Community
import com.laotoua.dawnislandk.data.local.entity.Forum
import com.laotoua.dawnislandk.databinding.FragmentCommonForumsBinding
import com.laotoua.dawnislandk.screens.SharedViewModel
import com.laotoua.dawnislandk.screens.adapters.CommunityNodeAdapter
import com.laotoua.dawnislandk.screens.util.ContentTransformation
import com.laotoua.dawnislandk.screens.util.Layout.toast
import com.laotoua.dawnislandk.util.DataResource
import com.laotoua.dawnislandk.util.LoadingStatus
import dagger.android.support.DaggerFragment
import javax.inject.Inject

class CommonForumsFragment : DaggerFragment() {

    private var binding: FragmentCommonForumsBinding? = null

    @Inject
    lateinit var viewModelFactory: ViewModelProvider.Factory

    private val sharedVM: SharedViewModel by activityViewModels { viewModelFactory }

    private var commonForumAdapter: CommonForumAdapter? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setHasOptionsMenu(true)
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        inflater.inflate(R.menu.menu_fragment_settings_forum, menu)
        super.onCreateOptionsMenu(menu, inflater)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.help -> {
                if (activity == null || !isAdded) return true
                MaterialDialog(requireContext()).show {
                    title(R.string.common_forum_setting)
                    message(R.string.common_forum_setting_help)
                    positiveButton(R.string.acknowledge)
                }
                return true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = FragmentCommonForumsBinding.inflate(inflater, container, false)

        commonForumAdapter = CommonForumAdapter(R.layout.list_item_forum)

        binding?.commonForums?.apply {
            layoutManager = LinearLayoutManager(context)
            adapter = commonForumAdapter
            setHasFixedSize(true)
        }

        val allForumAdapter =
            CommunityNodeAdapter(object : CommunityNodeAdapter.ForumClickListener {
                override fun onForumClick(forum: Forum) {
                    if (commonForumAdapter?.data?.contains(forum) == false) {
                        commonForumAdapter?.addData(forum)
                        updateTitle()
                    }
                }
            })

        binding?.allForums?.apply {
            layoutManager = LinearLayoutManager(context)
            adapter = allForumAdapter
            setHasFixedSize(true)
        }


        sharedVM.communityList.observe(viewLifecycleOwner, Observer<DataResource<List<Community>>> {
            if (it.status == LoadingStatus.ERROR) {
                toast(it.message)
                return@Observer
            }
            if (it.data.isNullOrEmpty()) return@Observer
            val notCommon = it.data.filterNot { c -> c.isCommonForums() || c.isCommonPosts() }
            val common = it.data.firstOrNull { c -> c.isCommonForums() }?.forums ?: emptyList()
            allForumAdapter.setData(notCommon)
            commonForumAdapter?.setList(common.toMutableList())
            updateTitle()
        })
        updateTitle()

        // Inflate the layout for this fragment
        return binding!!.root
    }

    private fun updateTitle() {
        binding?.commonForumTitle?.communityName?.text =
            getString(R.string.common_forums_title, binding?.commonForums?.adapter?.itemCount)
    }

    override fun onDestroyView() {
        val common = Community.makeCommonForums(commonForumAdapter?.data ?: emptyList())
        sharedVM.saveCommonCommunity(common)
        toast(R.string.might_need_to_restart_to_apply_setting)
        super.onDestroyView()
    }

    inner class CommonForumAdapter(layoutResId: Int) :
        BaseQuickAdapter<Forum, BaseViewHolder>(layoutResId),
        DraggableModule {

        private val dragListener: OnItemDragListener = object : OnItemDragListener {
            override fun onItemDragStart(viewHolder: RecyclerView.ViewHolder, pos: Int) {
                val startColor: Int = android.R.color.transparent
                val endColor: Int = requireContext().resources.getColor(R.color.colorPrimary, null)
                ValueAnimator.ofArgb(startColor, endColor).apply {
                    addUpdateListener { animation ->
                        (viewHolder as BaseViewHolder).itemView.setBackgroundColor(
                            animation.animatedValue as Int
                        )
                    }
                    duration = 300
                }.start()
            }

            override fun onItemDragMoving(
                source: RecyclerView.ViewHolder,
                from: Int,
                target: RecyclerView.ViewHolder,
                to: Int
            ) {
            }

            override fun onItemDragEnd(viewHolder: RecyclerView.ViewHolder, pos: Int) {
                val startColor: Int =
                    requireContext().resources.getColor(R.color.colorPrimary, null)
                val endColor: Int = android.R.color.transparent
                ValueAnimator.ofArgb(startColor, endColor).apply {
                    addUpdateListener { animation ->
                        (viewHolder as BaseViewHolder).itemView.setBackgroundColor(
                            animation.animatedValue as Int
                        )
                    }
                    duration = 300
                }.start()
            }
        }

        private val swipeListener: OnItemSwipeListener = object : OnItemSwipeListener {
            override fun onItemSwipeStart(viewHolder: RecyclerView.ViewHolder, pos: Int) {}

            override fun clearView(viewHolder: RecyclerView.ViewHolder, pos: Int) {}

            override fun onItemSwiped(viewHolder: RecyclerView.ViewHolder, pos: Int) {
                updateTitle()
            }

            override fun onItemSwipeMoving(
                canvas: Canvas,
                viewHolder: RecyclerView.ViewHolder?,
                dX: Float,
                dY: Float,
                isCurrentlyActive: Boolean
            ) {
                canvas.drawColor(requireContext().getColor(R.color.lime_500))
            }
        }

        init {
            draggableModule.isSwipeEnabled = true
            draggableModule.isDragEnabled = true
            draggableModule.setOnItemDragListener(dragListener)
            draggableModule.setOnItemSwipeListener(swipeListener)
            draggableModule.itemTouchHelperCallback.setSwipeMoveFlags(ItemTouchHelper.START or ItemTouchHelper.END)
        }


        override fun convert(holder: BaseViewHolder, item: Forum) {
            val biId = if (item.id.toInt() > 0) item.id.toInt() else 1
            val resourceId: Int = context.resources.getIdentifier(
                "bi_$biId", "drawable",
                context.packageName
            )
            holder.setText(
                R.id.forumName,
                ContentTransformation.transformForumName(item.getDisplayName())
            )
            holder.setImageResource(R.id.forumIcon, resourceId)
        }
    }

}