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
import com.afollestad.materialdialogs.lifecycle.lifecycleOwner
import com.afollestad.materialdialogs.list.listItemsSingleChoice
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
    ): View {
        binding = FragmentAboutBinding.inflate(inflater, container, false)

        binding!!.appFeedback.apply {
            key.setText(R.string.app_feed_back)
            root.setOnClickListener {
                if (activity == null || !isAdded) return@setOnClickListener
                MaterialDialog(requireContext()).show {
                    lifecycleOwner(this@AboutFragment)
                    title(R.string.app_feed_back)
                    icon(R.mipmap.ic_launcher)
                    val items = context.resources.getStringArray(R.array.app_feedback_options).toList()
                    listItemsSingleChoice(items = items, waitForPositiveButton = true) { _, index, _ ->
                        when (index) {
                            0 -> {
                                dismiss()
                                val navAction = MainNavDirections.actionGlobalCommentsFragment(DawnConstants.FEEDBACK_POST_ID, "117")
                                findNavController().navigate(navAction)
                            }
                            1 -> {
                                dismiss()
                                val intent = Intent(
                                    Intent.ACTION_VIEW,
                                    Uri.parse(DawnConstants.GITHUB_ADDRESS)
                                )
                                if (intent.resolveActivity(requireActivity().packageManager) != null) {
                                    startActivity(intent)
                                }
                            }
                            else -> {
                                dismiss()
                                val intent = Intent(Intent.ACTION_SEND).apply {
                                    type = "*/*"
                                    putExtra(
                                        Intent.EXTRA_EMAIL,
                                        arrayOf(DawnConstants.AUTHOR_EMAIL)
                                    )
                                    putExtra(
                                        Intent.EXTRA_SUBJECT,
                                        context.resources.getString(R.string.app_feed_back)
                                    )
                                    putExtra(
                                        Intent.EXTRA_TEXT,
                                        context.resources.getString(R.string.app_feed_back_template)
                                    )
                                }
                                if (intent.resolveActivity(requireActivity().packageManager) != null) {
                                    startActivity(intent)
                                }
                            }
                        }
                    }
                    positiveButton(R.string.submit)
                    negativeButton(R.string.cancel)
                }
            }
        }

        binding!!.appPrivacyAgreement.apply {
            key.setText(R.string.app_privacy_agreement)
            root.setOnClickListener {
                if (activity == null || !isAdded) return@setOnClickListener
                val waitingDialog = MaterialDialog(requireContext()).show {
                    lifecycleOwner(this@AboutFragment)
                    title(R.string.processing)
                    customView(R.layout.widget_loading)
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
                    if (activity == null || !isAdded) return@launch
                    MaterialDialog(requireContext()).show {
                        lifecycleOwner(this@AboutFragment)
                        title(res = R.string.app_privacy_agreement)
                        message(text = agreement) { html() }
                        positiveButton(R.string.acknowledge)
                    }
                }
            }
        }

        binding!!.nmbxdPrivacyAgreement.apply {
            key.setText(R.string.nmbxd_privacy_agreement)
            root.setOnClickListener {
                if (activity == null || !isAdded) return@setOnClickListener
                val navAction = MainNavDirections.actionGlobalCommentsFragment("11689471", "117")
                findNavController().navigate(navAction)
            }
        }

        binding!!.checkForUpdate.apply {
            key.setText(R.string.download_latest_version)
            root.setOnClickListener {
                if (activity == null || !isAdded) return@setOnClickListener
                MaterialDialog(requireContext()).show {
                    lifecycleOwner(this@AboutFragment)
                    title(R.string.download_latest_version)
                    icon(R.mipmap.ic_launcher)
                    listItemsSingleChoice(
                        R.array.download_options,
                        waitForPositiveButton = true
                    ) { _, index, _ ->
                        val uri = when (index) {
                            0 -> Uri.parse(DawnConstants.DOWNLOAD_NMBXD)
                            1 -> Uri.parse(DawnConstants.DOWNLOAD_GITHUB)
                            2 -> Uri.parse(DawnConstants.DOWNLOAD_GOOGLE_PLAY)
                            else -> Uri.parse("https://github.com/fishballzzz/DawnIslandK")
                        }
                        val intent = Intent(Intent.ACTION_VIEW, uri)
                        if (intent.resolveActivity(this@AboutFragment.requireActivity().packageManager) != null) {
                            startActivity(intent)
                        }
                    }
                    positiveButton(R.string.submit)
                    negativeButton(R.string.cancel)
                }
            }
        }

        binding!!.changeLog.apply {
            key.setText(R.string.change_log)
            root.setOnClickListener {
                if (activity == null || !isAdded) return@setOnClickListener
                val waitingDialog = MaterialDialog(requireContext()).show {
                    lifecycleOwner(this@AboutFragment)
                    title(R.string.processing)
                    customView(R.layout.widget_loading)
                    cancelOnTouchOutside(false)
                }
                lifecycleScope.launch {
                    val agreement = webNMBServiceClient.getChangeLog().run {
                        if (this is APIMessageResponse.Success) {
                            message
                        } else {
                            Timber.d(message)
                            ""
                        }
                    }

                    waitingDialog.dismiss()
                    if (activity == null || !isAdded) return@launch
                    MaterialDialog(requireContext()).show {
                        lifecycleOwner(this@AboutFragment)
                        title(res = R.string.change_log)
                        message(text = agreement)
                        positiveButton(R.string.acknowledge)
                    }
                }
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