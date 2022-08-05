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
import android.widget.EditText
import androidx.core.widget.doOnTextChanged
import androidx.fragment.app.viewModels
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.afollestad.materialdialogs.MaterialDialog
import com.afollestad.materialdialogs.WhichButton
import com.afollestad.materialdialogs.actions.getActionButton
import com.afollestad.materialdialogs.actions.setActionButtonEnabled
import com.afollestad.materialdialogs.checkbox.checkBoxPrompt
import com.afollestad.materialdialogs.customview.customView
import com.afollestad.materialdialogs.lifecycle.lifecycleOwner
import com.chad.library.adapter.base.BaseQuickAdapter
import com.chad.library.adapter.base.listener.OnItemDragListener
import com.chad.library.adapter.base.listener.OnItemSwipeListener
import com.chad.library.adapter.base.module.DraggableModule
import com.chad.library.adapter.base.viewholder.BaseViewHolder
import com.google.android.material.textfield.TextInputLayout
import com.laotoua.dawnislandk.R
import com.laotoua.dawnislandk.data.local.entity.Emoji
import com.laotoua.dawnislandk.databinding.FragmentEmojiSettingBinding
import com.laotoua.dawnislandk.screens.util.Layout
import dagger.android.support.DaggerFragment
import kotlinx.coroutines.launch
import javax.inject.Inject

class EmojiSettingFragment : DaggerFragment() {

    private var binding: FragmentEmojiSettingBinding? = null

    @Inject
    lateinit var viewModelFactory: ViewModelProvider.Factory

    private val viewModel: EmojiSettingViewModel by viewModels { viewModelFactory }

    private var emojiAdapter: EmojiAdapter? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setHasOptionsMenu(true)
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        inflater.inflate(R.menu.menu_fragment_emoji_setting, menu)
        context?.let {
            menu.findItem(R.id.help)?.icon?.setTint(Layout.getThemeInverseColor(it))
            menu.findItem(R.id.restore)?.icon?.setTint(Layout.getThemeInverseColor(it))
        }
        super.onCreateOptionsMenu(menu, inflater)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.help -> {
                if (activity == null || !isAdded) return true
                MaterialDialog(requireContext()).show {
                    lifecycleOwner(this@EmojiSettingFragment)
                    title(R.string.emoji_setting)
                    message(R.string.emoji_setting_help)
                    positiveButton(R.string.acknowledge)
                }
                return true
            }
            R.id.restore -> {
                if (activity == null || !isAdded) return true
                MaterialDialog(requireContext()).show {
                    lifecycleOwner(this@EmojiSettingFragment)
                    title(R.string.restore)
                    message(R.string.emoji_restore)
                    setActionButtonEnabled(WhichButton.POSITIVE, false)
                    checkBoxPrompt(R.string.acknowledge) { checked ->
                        setActionButtonEnabled(WhichButton.POSITIVE, checked)
                    }
                    positiveButton(R.string.submit) {
                        viewModel.resetEmoji()
                        this@EmojiSettingFragment.lifecycleScope.launch {
                            if (activity == null || !isAdded) return@launch
                            emojiAdapter?.setList(viewModel.getAllEmoji().toMutableList())
                        }
                    }
                    negativeButton(R.string.cancel)
                }
                return true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentEmojiSettingBinding.inflate(inflater, container, false)

        emojiAdapter = EmojiAdapter(R.layout.list_item_content_with_remark).apply {
            addChildClickViewIds(R.id.edit)

            setOnItemChildClickListener { _, view, position ->
                if (view.id == R.id.edit) {
                    if (activity == null || !isAdded) return@setOnItemChildClickListener
                    MaterialDialog(requireContext()).show {
                        lifecycleOwner(this@EmojiSettingFragment)
                        title(R.string.emoji_setting)
                        customView(R.layout.dialog_input_content_with_remark)
                        val submitButton = getActionButton(WhichButton.POSITIVE)
                        findViewById<TextInputLayout>(R.id.remark).hint =
                            resources.getString(R.string.emoji_remark)
                        findViewById<TextInputLayout>(R.id.content).hint =
                            resources.getString(R.string.emoji_value)
                        val remark = findViewById<EditText>(R.id.remarkText)
                        val content = findViewById<EditText>(R.id.contentText)
                        remark.setText(getItem(position).name)
                        content.setText(getItem(position).value)
                        positiveButton(R.string.submit) {
                            val remarkText = remark.text.toString()
                            val contentText = content.text.toString()
                            if (remarkText.isNotBlank() && contentText.isNotBlank()) {
                                val emoji = Emoji(remarkText, contentText, true)
                                emoji.id = emojiAdapter?.getItem(position)?.id ?: 0
                                emojiAdapter?.setData(position, emoji)
                            }
                        }
                        negativeButton(R.string.cancel)
                        remark.doOnTextChanged { text, _, _, _ ->
                            submitButton.isEnabled =
                                !text.isNullOrBlank() && !content.text.isNullOrBlank()
                        }
                        content.doOnTextChanged { text, _, _, _ ->
                            submitButton.isEnabled =
                                !text.isNullOrBlank() && !remark.text.isNullOrBlank()
                        }

                    }
                }
            }
        }

        binding?.emojiRV?.apply {
            layoutManager = LinearLayoutManager(context)
            adapter = emojiAdapter
            setHasFixedSize(true)
        }

        lifecycleScope.launch {
            emojiAdapter?.setList(viewModel.getAllEmoji().toMutableList())
        }

        binding?.addEmoji?.setOnClickListener {
            if (activity == null || !isAdded) return@setOnClickListener
            MaterialDialog(requireContext()).show {
                lifecycleOwner(this@EmojiSettingFragment)
                title(R.string.emoji_setting)
                customView(R.layout.dialog_input_content_with_remark)
                val submitButton = getActionButton(WhichButton.POSITIVE)
                findViewById<TextInputLayout>(R.id.remark).hint =
                    resources.getString(R.string.emoji_remark)
                findViewById<TextInputLayout>(R.id.content).hint =
                    resources.getString(R.string.emoji_value)
                val remark = findViewById<EditText>(R.id.remarkText)
                val content = findViewById<EditText>(R.id.contentText)
                submitButton.isEnabled = false
                positiveButton(R.string.submit) {
                    val remarkText = remark.text.toString()
                    val contentText = content.text.toString()
                    if (remarkText.isNotBlank() && contentText.isNotBlank()) {
                        emojiAdapter?.addData(Emoji(remarkText, contentText, true))
                    }
                }
                negativeButton(R.string.cancel)
                remark.doOnTextChanged { text, _, _, _ ->
                    submitButton.isEnabled = !text.isNullOrBlank() && !content.text.isNullOrBlank()
                }
                content.doOnTextChanged { text, _, _, _ ->
                    submitButton.isEnabled = !text.isNullOrBlank() && !remark.text.isNullOrBlank()
                }

            }
        }

        return binding!!.root
    }

    override fun onDestroyView() {
        emojiAdapter?.data?.let { viewModel.setEmojiList(it) }
        super.onDestroyView()
    }

    inner class EmojiAdapter(layoutResId: Int) :
        BaseQuickAdapter<Emoji, BaseViewHolder>(layoutResId),
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


        override fun convert(holder: BaseViewHolder, item: Emoji) {
            holder.setText(R.id.remark, item.name)
            holder.setText(R.id.content, item.value)
        }

    }
}