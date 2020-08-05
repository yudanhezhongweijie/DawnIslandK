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

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.afollestad.materialdialogs.MaterialDialog
import com.afollestad.materialdialogs.customview.customView
import com.afollestad.materialdialogs.list.listItems
import com.laotoua.dawnislandk.BuildConfig
import com.laotoua.dawnislandk.MainNavDirections
import com.laotoua.dawnislandk.R
import com.laotoua.dawnislandk.data.remote.APIMessageResponse
import com.laotoua.dawnislandk.data.remote.NMBServiceClient
import com.laotoua.dawnislandk.databinding.FragmentAboutBinding
import com.laotoua.dawnislandk.util.DawnConstants
import dagger.android.support.DaggerFragment
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

class AboutFragment : DaggerFragment() {

    @Inject
    lateinit var webNMBServiceClient: NMBServiceClient

    private var binding: FragmentAboutBinding? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = FragmentAboutBinding.inflate(inflater, container, false)

        binding!!.appFeedback.apply {
            key.setText(R.string.app_feed_back)
            root.setOnClickListener {
                if (activity == null || !isAdded) return@setOnClickListener
                MaterialDialog(requireContext()).show {
                    title(R.string.app_feed_back)
                    val items =
                        context.resources.getStringArray(R.array.app_feedback_options).toList()
                    listItems(items = items) { _, index, _ ->
                        when (index) {
                            0 -> {
                                val navAction = MainNavDirections.actionGlobalCommentsFragment(
                                    "28951798",
                                    "117"
                                )
                                findNavController().navigate(navAction)
                            }
                            1 -> {
                                val intent = Intent(
                                    Intent.ACTION_VIEW,
                                    Uri.parse(DawnConstants.GITHUB_ADDRESS)
                                )
                                if (intent.resolveActivity(requireActivity().packageManager) != null) {
                                    startActivity(intent)
                                }
                            }
                            else -> {
                                val intent = Intent(Intent.ACTION_SENDTO).apply {
                                    type = "*/*"
                                    data = Uri.parse("mailto:")
                                    putExtra(
                                        Intent.EXTRA_EMAIL,
                                        arrayOf(DawnConstants.AUTHOR_EMAIL)
                                    )
                                    putExtra(
                                        Intent.EXTRA_SUBJECT,
                                        context.resources.getString(R.string.app_feed_back)
                                    )
                                }
                                if (intent.resolveActivity(requireActivity().packageManager) != null) {
                                    startActivity(intent)
                                }
                            }
                        }
                    }
                }
            }
        }

        binding!!.appPrivacyAgreement.apply {
            key.setText(R.string.app_privacy_agreement)
            root.setOnClickListener {
                if (activity == null || !isAdded) return@setOnClickListener
                val waitingDialog = MaterialDialog(requireContext()).show {
                    title(R.string.processing)
                    customView(R.layout.dialog_progress)
                    cancelOnTouchOutside(false)
                }
                lifecycleScope.launch {
                    val agreement = webNMBServiceClient.getPrivacyAgreement().run {
                        if (this is APIMessageResponse.Success) {
                            dom!!.toString()
                        } else {
                            Timber.d(message)
                            ""
                        }
                    }

                    waitingDialog.dismiss()
                    MaterialDialog(requireContext()).show {
                        title(res = R.string.app_privacy_agreement)
                        message(text = agreement) { html() }
                        positiveButton(R.string.acknowledge)
                    }
                }
            }
        }

        binding!!.adnmbPrivacyAgreement.apply {
            key.setText(R.string.adnmb_privacy_agreement)
            root.setOnClickListener {
                if (activity == null || !isAdded) return@setOnClickListener
                val navAction = MainNavDirections.actionGlobalCommentsFragment("11689471", "")
                findNavController().navigate(navAction)
            }
        }

        binding!!.credit.apply {
            text = getString(R.string.credit, BuildConfig.VERSION_NAME)
        }
        // Inflate the layout for this fragment
        return binding!!.root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        binding = null
    }
}