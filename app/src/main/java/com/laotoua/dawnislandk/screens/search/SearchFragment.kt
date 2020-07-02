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

package com.laotoua.dawnislandk.screens.search

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.viewModels
import com.afollestad.materialdialogs.MaterialDialog
import com.afollestad.materialdialogs.customview.customView
import com.laotoua.dawnislandk.R
import com.laotoua.dawnislandk.databinding.FragmentSearchBinding
import com.laotoua.dawnislandk.screens.MainActivity
import com.laotoua.dawnislandk.screens.util.ToolBar.immersiveToolbar
import com.laotoua.dawnislandk.screens.widgets.BaseNavFragment


class SearchFragment : BaseNavFragment() {

    companion object {
        fun newInstance() = SearchFragment()
    }

    private val viewModel: SearchViewModel by viewModels { viewModelFactory }
    private var _binding: FragmentSearchBinding? = null
    private val binding: FragmentSearchBinding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = FragmentSearchBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.toolbar.apply {
            immersiveToolbar()
            setTitle(R.string.search)
            setSubtitle(R.string.toolbar_subtitle)
        }

        binding.search.setOnClickListener {
            MaterialDialog(requireContext()).show {
                title(R.string.search)
                customView(R.layout.dialog_search, noVerticalPadding = true).apply {
                    findViewById<Button>(R.id.search).setOnClickListener {
                        val query = findViewById<TextView>(R.id.searchInputText).text.toString()
                        viewModel.search(query)
//                        Toast.makeText(context, "还没做。。。", Toast.LENGTH_SHORT).show()
//                        lifecycleScope.launch {
//                            val res = viewModel.NMBSearch(query)
//                            Timber.d("status: ${res.message}")
//                            if (res is APIMessageResponse.APISuccessMessageResponse){
//                                Timber.d("dom: ${res.dom}")
//                            }
//                        }
                    }

                    findViewById<Button>(R.id.jumpToPost).setOnClickListener {
                        val threadId = findViewById<TextView>(R.id.searchInputText).text
                            .filter { it.isDigit() }.toString()
                        if (threadId.isNotEmpty()) {
                            // Does not have fid here. fid will be generated when data comes back in reply
                            sharedVM.setPost(threadId, "")
                            dismiss()
                            (requireActivity() as MainActivity).showComment()
                        } else {
                            Toast.makeText(
                                context,
                                R.string.please_input_valid_text,
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    }
                }
            }
        }
    }


}