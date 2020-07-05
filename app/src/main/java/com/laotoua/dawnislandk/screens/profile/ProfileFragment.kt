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

import android.Manifest
import android.animation.Animator
import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.widget.doOnTextChanged
import androidx.fragment.app.viewModels
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.afollestad.materialdialogs.MaterialDialog
import com.afollestad.materialdialogs.WhichButton
import com.afollestad.materialdialogs.actions.getActionButton
import com.afollestad.materialdialogs.actions.setActionButtonEnabled
import com.afollestad.materialdialogs.checkbox.checkBoxPrompt
import com.afollestad.materialdialogs.checkbox.isCheckPromptChecked
import com.afollestad.materialdialogs.customview.customView
import com.afollestad.materialdialogs.input.input
import com.afollestad.materialdialogs.list.listItems
import com.afollestad.materialdialogs.list.listItemsSingleChoice
import com.google.android.material.animation.AnimationUtils
import com.king.zxing.util.CodeUtils
import com.laotoua.dawnislandk.DawnApp.Companion.applicationDataStore
import com.laotoua.dawnislandk.R
import com.laotoua.dawnislandk.data.local.entity.Cookie
import com.laotoua.dawnislandk.databinding.FragmentProfileBinding
import com.laotoua.dawnislandk.databinding.ListItemCookieBinding
import com.laotoua.dawnislandk.databinding.ListItemPreferenceBinding
import com.laotoua.dawnislandk.screens.MainActivity
import com.laotoua.dawnislandk.util.*
import dagger.android.support.DaggerFragment
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import org.json.JSONObject
import timber.log.Timber
import javax.inject.Inject

class ProfileFragment : DaggerFragment() {

    @Inject
    lateinit var viewModelFactory: ViewModelProvider.Factory
    private val viewModel: ProfileViewModel by viewModels { viewModelFactory }

    private val getCookieImage =
        registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
            uri?.run {
                try {
                    val file =
                        ImageUtil.getImageFileFromUri(fragment = this@ProfileFragment, uri = uri)
                            ?: return@registerForActivityResult
                    val res = CodeUtils.parseQRCode(file.path)
                    if (res != null) {
                        saveCookieWithInputName(res)
                    } else {
                        Toast.makeText(
                            context,
                            R.string.did_not_get_cookie_from_image,
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                } catch (e: Exception) {
                    // Ignore
                    Timber.e(e)
                }
            }
        }

    private var _binding: FragmentProfileBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = FragmentProfileBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.settings.visibility = View.GONE

        binding.feedId.apply {
            var feedId = applicationDataStore.feedId
            key.setText(R.string.feedId)
            summary.text = feedId
            root.setOnClickListener {
                MaterialDialog(requireContext()).show {
                    title(R.string.feedId)
                    input(hint = feedId, prefill = feedId) { _, text ->
                        feedId = text.toString()
                        applicationDataStore.updateFeedId(feedId)
                        summary.text = feedId
                    }
                    positiveButton(R.string.submit)
                    negativeButton(R.string.cancel)
                }
            }
        }

        binding.timeFormat.apply {
            key.setText(R.string.time_display_format)
            val entries = resources.getStringArray(R.array.time_format_entries)
            val values = resources.getStringArray(R.array.time_format_values)
            summary.text =
                if (values.first() == applicationDataStore.displayTimeFormat) {
                    entries.first()
                } else {
                    entries.last()
                }

            root.setOnClickListener {
                MaterialDialog(requireContext()).show {
                    title(R.string.time_display_format)
                    listItems(R.array.time_format_entries) { _, index, text ->
                        applicationDataStore.setDisplayTimeFormat(values[index])
                        summary.text = text
                        displayRestartToApplySettingsToast()
                    }
                }
            }
        }

        binding.useReadingProgress.apply {
            key.setText(R.string.saves_reading_progress)
            preferenceSwitch.visibility = View.VISIBLE
            preferenceSwitch.isChecked = applicationDataStore.readingProgressStatus
            updateSwitchSummary(R.string.reading_progress_on, R.string.reading_progress_off)
            root.setOnClickListener {
                toggleSwitch(
                    applicationDataStore::setReadingProgressStatus,
                    R.string.reading_progress_on,
                    R.string.reading_progress_off
                )
                displayRestartToApplySettingsToast()
            }
        }

        binding.animationSwitch.apply {
            key.setText(R.string.animation_settings)
            val options =
                requireContext().resources.getStringArray(R.array.adapter_animation_options)
            summary.text = options[applicationDataStore.animationOption]
            root.setOnClickListener {
                MaterialDialog(requireContext()).show {
                    title(res = R.string.animation_settings)
                    checkBoxPrompt(res = R.string.animation_first_only) {}
                    listItemsSingleChoice(items = options.toList()) { _, index, _ ->
                        applicationDataStore.setAnimationOption(index)
                        applicationDataStore.setAnimationFirstOnly(isCheckPromptChecked())
                        displayRestartToApplySettingsToast()
                        summary.text = options[index]
                    }
                    positiveButton(R.string.submit)
                }
            }
        }

        val loadingDialog by lazyOnMainOnly {
            MaterialDialog(requireContext()).apply {
                title(R.string.processing)
                customView(R.layout.dialog_progress)
                cancelOnTouchOutside(false)
            }
        }
        viewModel.loadingStatus.observe(viewLifecycleOwner, Observer {
            it.getContentIfNotHandled()?.run {
                when (loadingStatus) {
                    LoadingStatus.LOADING -> {
                        loadingDialog.show()
                    }
                    LoadingStatus.FAILED -> {
                        loadingDialog.dismiss()
                        Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
                    }
                    else -> {
                        loadingDialog.dismiss()
                    }
                }
            }
        })

        viewModel.cookies.observe(viewLifecycleOwner, Observer { cookies ->
            binding.cookieList.removeAllViews()
            cookies.map { addCookieToLayout(it) }
        })

        binding.addCookie.setOnClickListener {
            MaterialDialog(requireContext()).show {
                title(R.string.add_cookie)
                listItems(R.array.cookie_addition_options) { _, index, _ ->
                    when (index) {
                        0 -> getCookieImage.launch("image/*")
                        1 -> {
                            if (FragmentIntentUtil.checkAndRequestSinglePermission(
                                    this@ProfileFragment,
                                    Manifest.permission.CAMERA,
                                    true
                                )
                            ) {
                                FragmentIntentUtil.getCookieFromQRCode(this@ProfileFragment) {
                                    it?.run {
                                        saveCookieWithInputName(it)
                                    }
                                }
                            }
                        }
                        2 -> MaterialDialog(requireContext()).show {
                            title(R.string.add_cookie)
                            customView(R.layout.dialog_cookie_addition)
                            val submitButton = getActionButton(WhichButton.POSITIVE)
                            val neuralButton = getActionButton(WhichButton.NEUTRAL)
                            val cookieName = findViewById<EditText>(R.id.cookieNameText)
                            val cookieHash = findViewById<EditText>(R.id.cookieHashText)
                            submitButton.isEnabled = false
                            neuralButton.isEnabled = false
                            positiveButton(R.string.submit) {
                                val cookieNameText = cookieName.text.toString()
                                val cookieHashText = cookieHash.text.toString()
                                if (cookieHashText.isNotBlank() && cookieNameText.isNotBlank()) {
                                    viewModel.addNewCookie(cookieHashText, cookieNameText)
                                }
                            }
                            negativeButton(R.string.cancel)
                            cookieName.doOnTextChanged { text, _, _, _ ->
                                submitButton.isEnabled =
                                    !text.isNullOrBlank() && !cookieHash.text.isNullOrBlank()
                            }
                            cookieHash.doOnTextChanged { text, _, _, _ ->
                                submitButton.isEnabled =
                                    !text.isNullOrBlank() && !cookieName.text.isNullOrBlank()
                                neuralButton.isEnabled = !text.isNullOrBlank()
                            }
                            @Suppress("DEPRECATION")
                            neutralButton(R.string.default_cookie_name) {
                                val cookieHashText = cookieHash.text.toString()
                                if (cookieHashText.isNotBlank()) {
                                    viewModel.addNewCookie(cookieHashText)
                                }
                            }
                        }
                    }
                }
            }
        }

        binding.sizeCustomization.apply {
            key.setText(R.string.size_customization_settings)
            root.setOnClickListener {
                val action =
                    ProfileFragmentDirections.actionSettingsFragmentToSizeCustomizationFragment()
                findNavController().navigate(action)
            }
        }

        binding.clearCommentCache.apply {
            key.setText(R.string.clear_comment_cache)
            root.setOnClickListener {
                MaterialDialog(requireContext()).show {
                    title(R.string.clear_comment_cache)
                    message(R.string.clear_comment_cache_confirm_message)
                    setActionButtonEnabled(WhichButton.POSITIVE, false)
                    checkBoxPrompt(R.string.acknowledge) { checked ->
                        setActionButtonEnabled(WhichButton.POSITIVE, checked)
                    }
                    positiveButton(R.string.submit) {
                        GlobalScope.launch { applicationDataStore.nukeCommentTable() }
                        Toast.makeText(
                            context,
                            R.string.cleared_comment_cache_message,
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                    negativeButton(R.string.cancel)
                }
            }
        }

        binding.appFeedback.apply {
            key.setText(R.string.app_feed_back)
            root.setOnClickListener {
                MaterialDialog(requireContext()).show {
                    title(R.string.app_feed_back)
                    val items = listOf(
                        context.resources.getString(R.string.github),
                        context.resources.getString(R.string.email_author)
                    )
                    listItems(items = items) { _, index, _ ->
                        if (index == 0) {
                            val intent =
                                Intent(Intent.ACTION_VIEW, Uri.parse(DawnConstants.GITHUB_ADDRESS))
                            if (intent.resolveActivity(requireActivity().packageManager) != null) {
                                startActivity(intent)
                            }
                        } else {
                            val intent = Intent(Intent.ACTION_SENDTO).apply {
                                type = "*/*"
                                data = Uri.parse("mailto:")
                                putExtra(Intent.EXTRA_EMAIL, arrayOf(DawnConstants.AUTHOR_EMAIL))
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

        binding.privacyAgreement.apply {
            key.setText(R.string.privacy_agreement)
            root.setOnClickListener {
                val waitingDialog = MaterialDialog(requireContext()).show {
                    title(R.string.processing)
                    customView(R.layout.dialog_progress)
                    cancelOnTouchOutside(false)
                }
                lifecycleScope.launch {
                    val agreement = viewModel.getPrivacyAgreement()
                    waitingDialog.dismiss()
                    MaterialDialog(this@ProfileFragment.requireContext()).show {
                        title(res = R.string.privacy_agreement)
                        message(text = agreement) { html() }
                        positiveButton(R.string.acknowledge)
                    }
                }
            }
        }

        hideProgressBarAndShowSettings()
    }

    private fun hideProgressBarAndShowSettings() {
        val progressBarAlphaOutAnim = ObjectAnimator.ofFloat(binding.progressBar, "alpha", 0f)
        val settingsAlphaInAnim = ObjectAnimator.ofFloat(binding.settings, "alpha", 1f)
        AnimatorSet().apply {
            duration = 250
            addListener(object : Animator.AnimatorListener {
                override fun onAnimationRepeat(animation: Animator?) {}
                override fun onAnimationEnd(animation: Animator?) {
                    binding.settings.visibility = View.VISIBLE
                    binding.progressBar.visibility = View.GONE
                }

                override fun onAnimationCancel(animation: Animator?) {}
                override fun onAnimationStart(animation: Animator?) {}
            })
            interpolator = AnimationUtils.FAST_OUT_LINEAR_IN_INTERPOLATOR
            playTogether(progressBarAlphaOutAnim, settingsAlphaInAnim)
            start()
        }
    }

    private fun addCookieToLayout(cookie: Cookie) {
        val view = ListItemCookieBinding.inflate(layoutInflater)
        view.cookieName.text = cookie.cookieDisplayName
        view.edit.setOnClickListener {
            MaterialDialog(requireContext()).show {
                title(R.string.edit_cookie_remark)
                input(
                    prefill = cookie.cookieDisplayName,
                    hint = cookie.cookieDisplayName
                ) { _, text ->
                    // Text submitted with the action button
                    cookie.cookieDisplayName = text.toString()
                    viewModel.updateCookie(cookie)
                }
                positiveButton(R.string.submit)
                negativeButton(R.string.default_cookie_name) {
                    dismiss()
                    cookie.cookieDisplayName = cookie.cookieName
                    viewModel.updateCookie(cookie)
                }
            }
        }

        view.remove.setOnClickListener {
            viewModel.deleteCookie(cookie)
            if (binding.cookieList.childCount < 5) {
                binding.addCookie.isEnabled = true
            }
            binding.cookieSummary.text =
                resources.getString(R.string.cookie_count, binding.cookieList.childCount)
        }

        binding.cookieList.addView(view.root)

        binding.cookieSummary.text =
            resources.getString(R.string.cookie_count, binding.cookieList.childCount)
        if (binding.cookieList.childCount >= 5) {
            binding.addCookie.isEnabled = false
        }
    }

    private fun saveCookieWithInputName(cookieJson: String) {
        val cookieHash = JSONObject(cookieJson).getString("cookie")
        MaterialDialog(requireContext()).show {
            title(R.string.edit_cookie_remark)
            cancelable(false)
            input(hint = cookieHash) { _, text ->
                viewModel.addNewCookie(cookieHash, text.toString())
            }
            positiveButton(R.string.submit)
            negativeButton(R.string.default_cookie_name) {
                viewModel.addNewCookie(cookieHash)
            }
        }
    }

    private fun ListItemPreferenceBinding.toggleSwitch(
        toggleFunc: (Boolean) -> Unit,
        summaryOn: Int,
        summaryOff: Int
    ) {
        preferenceSwitch.toggle()
        toggleFunc(preferenceSwitch.isChecked)
        updateSwitchSummary(summaryOn, summaryOff)
    }

    private fun ListItemPreferenceBinding.updateSwitchSummary(summaryOn: Int, summaryOff: Int) {
        if (preferenceSwitch.isChecked) {
            summary.setText(summaryOn)
        } else {
            summary.setText(summaryOff)
        }
    }

    private fun displayRestartToApplySettingsToast() {
        Toast.makeText(context, R.string.restart_to_apply_setting, Toast.LENGTH_SHORT).show()
    }

    override fun onResume() {
        super.onResume()
        (requireActivity() as MainActivity).setToolbarTitle(R.string.settings)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

}