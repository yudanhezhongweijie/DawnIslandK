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
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.widget.doOnTextChanged
import androidx.fragment.app.viewModels
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.findNavController
import com.afollestad.materialdialogs.MaterialDialog
import com.afollestad.materialdialogs.WhichButton
import com.afollestad.materialdialogs.actions.getActionButton
import com.afollestad.materialdialogs.customview.customView
import com.afollestad.materialdialogs.input.input
import com.afollestad.materialdialogs.list.listItems
import com.google.android.material.animation.AnimationUtils
import com.google.android.material.textfield.TextInputLayout
import com.king.zxing.util.CodeUtils
import com.laotoua.dawnislandk.BuildConfig
import com.laotoua.dawnislandk.R
import com.laotoua.dawnislandk.data.local.entity.Cookie
import com.laotoua.dawnislandk.databinding.FragmentProfileBinding
import com.laotoua.dawnislandk.databinding.ListItemCookieBinding
import com.laotoua.dawnislandk.screens.util.Layout.toast
import com.laotoua.dawnislandk.util.ImageUtil
import com.laotoua.dawnislandk.util.IntentUtil
import com.laotoua.dawnislandk.util.LoadingStatus
import com.laotoua.dawnislandk.util.lazyOnMainOnly
import dagger.android.support.DaggerFragment
import org.json.JSONObject
import timber.log.Timber
import javax.inject.Inject

class ProfileFragment : DaggerFragment() {

    @Inject
    lateinit var viewModelFactory: ViewModelProvider.Factory
    private val viewModel: ProfileViewModel by viewModels { viewModelFactory }

    private val cookieLimit = 5

    private val getCookieImage =
        registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
            uri?.run {
                try {
                    val file = ImageUtil.getImageFileFromUri(requireActivity(), uri)
                        ?: return@registerForActivityResult
                    val res = CodeUtils.parseQRCode(file.path)
                    if (res != null) {
                        saveCookieWithInputName(res)
                    } else {
                        toast(R.string.did_not_get_cookie_from_image)
                    }
                } catch (e: Exception) {
                    // Ignore
                    Timber.e(e)
                }
            }
        }

    private var binding: FragmentProfileBinding? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = FragmentProfileBinding.inflate(inflater, container, false)

        binding!!.profileView.visibility = View.GONE
        binding!!.generalSettings.apply {
            text.setText(R.string.general_settings)
            icon.rotation = -90f
            root.setOnClickListener {
                if (activity == null || !isAdded) return@setOnClickListener
                val action = ProfileFragmentDirections.actionProfileFragmentToGeneralSettingFragment()
                findNavController().navigate(action)
            }
        }

        binding!!.displaySettings.apply {
            text.setText(R.string.display_settings)
            icon.rotation = -90f
            root.setOnClickListener {
                if (activity == null || !isAdded) return@setOnClickListener
                val action = ProfileFragmentDirections.actionProfileFragmentToDisplaySettingFragment()
                findNavController().navigate(action)
            }
        }

        binding!!.customSettings.apply {
            text.setText(R.string.custom_settings)
            icon.rotation = -90f
            root.setOnClickListener {
                if (activity == null || !isAdded) return@setOnClickListener
                val action = ProfileFragmentDirections.actionProfileFragmentToCustomSettingFragment()
                findNavController().navigate(action)
            }
        }
        binding!!.about.apply {
            text.setText(R.string.about)
            icon.rotation = -90f
            root.setOnClickListener {
                if (activity == null || !isAdded) return@setOnClickListener
                val action = ProfileFragmentDirections.actionProfileFragmentToAboutFragment()
                findNavController().navigate(action)
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
                    LoadingStatus.ERROR -> {
                        loadingDialog.dismiss()
                        toast("添加饼干失败\n$message")
                    }
                    else -> {
                        loadingDialog.dismiss()
                    }
                }
            }
        })

        viewModel.cookies.observe(viewLifecycleOwner, Observer { cookies ->
            binding?.cookieList?.removeAllViews()
            cookies.map { addCookieToLayout(it) }
        })

        binding!!.addCookie.setOnClickListener {
            if (activity == null || !isAdded) return@setOnClickListener
            MaterialDialog(requireContext()).show {
                title(R.string.add_cookie)
                listItems(R.array.cookie_addition_options) { _, index, _ ->
                    when (index) {
                        0 -> getCookieImage.launch("image/*")
                        1 -> {
                            if (IntentUtil.checkAndRequestSinglePermission(
                                    requireActivity(),
                                    Manifest.permission.CAMERA,
                                    true
                                )
                            ) {
                                IntentUtil.getCookieFromQRCode(requireActivity()) {
                                    it?.run {
                                        saveCookieWithInputName(it)
                                    }
                                }
                            }
                        }
                        2 -> MaterialDialog(requireContext()).show {
                            title(R.string.add_cookie)
                            customView(R.layout.dialog_input_content_with_remark)
                            val submitButton = getActionButton(WhichButton.POSITIVE)
                            val neuralButton = getActionButton(WhichButton.NEUTRAL)
                            findViewById<TextInputLayout>(R.id.remark).hint =
                                resources.getString(R.string.cookie_name)
                            findViewById<TextInputLayout>(R.id.content).hint =
                                resources.getString(R.string.cookie_hash)
                            val cookieName = findViewById<EditText>(R.id.remarkText)
                            val cookieHash = findViewById<EditText>(R.id.contentText)
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

        binding!!.credit.apply {
            text = getString(R.string.credit, BuildConfig.VERSION_NAME)
        }

        hideProgressBarAndShowSettings()
        return binding!!.root
    }

    private fun hideProgressBarAndShowSettings() {
        if (binding == null) return
        val progressBarAlphaOutAnim = ObjectAnimator.ofFloat(binding!!.progressBar, "alpha", 0f)
        val profileViewAlphaInAnim = ObjectAnimator.ofFloat(binding!!.profileView, "alpha", 1f)
        AnimatorSet().apply {
            duration = 250
            addListener(object : Animator.AnimatorListener {
                override fun onAnimationRepeat(animation: Animator?) {}
                override fun onAnimationEnd(animation: Animator?) {
                    binding?.profileView?.visibility = View.VISIBLE
                    binding?.progressBar?.visibility = View.GONE
                }

                override fun onAnimationCancel(animation: Animator?) {}
                override fun onAnimationStart(animation: Animator?) {}
            })
            interpolator = AnimationUtils.FAST_OUT_LINEAR_IN_INTERPOLATOR
            playTogether(progressBarAlphaOutAnim, profileViewAlphaInAnim)
            start()
        }
    }

    private fun addCookieToLayout(cookie: Cookie) {
        if (binding == null) return
        val view = ListItemCookieBinding.inflate(layoutInflater, binding!!.root, false)
        view.remark.text = cookie.cookieDisplayName
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
            if (activity == null || !isAdded || binding == null) return@setOnClickListener
            viewModel.deleteCookie(cookie)
            if (binding?.cookieList?.childCount ?: 0 < 5) {
                binding?.addCookie?.isEnabled = true
            }
            binding?.cookieSummary?.text =
                resources.getString(
                    R.string.count_text,
                    binding?.cookieList?.childCount ?: 0,
                    cookieLimit
                )
        }

        binding?.cookieList?.addView(view.root)

        binding?.cookieSummary?.text =
            resources.getString(R.string.count_text, binding?.cookieList?.childCount ?: 0, cookieLimit)
        if (binding?.cookieList?.childCount ?: 0 >= 5) {
            binding?.addCookie?.isEnabled = false
        }
    }

    private fun saveCookieWithInputName(cookieJson: String) {
        try {
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
        } catch (e:Exception){
            toast("没有读取到有效饼干。请检查图片有效性。如果确认图片为合理饼干，请通过软件反馈联系作者并附上$cookieJson")
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        binding = null
    }

}