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
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.afollestad.materialdialogs.MaterialDialog
import com.afollestad.materialdialogs.checkbox.checkBoxPrompt
import com.afollestad.materialdialogs.checkbox.isCheckPromptChecked
import com.afollestad.materialdialogs.lifecycle.lifecycleOwner
import com.afollestad.materialdialogs.list.listItemsSingleChoice
import com.laotoua.dawnislandk.DawnApp.Companion.applicationDataStore
import com.laotoua.dawnislandk.R
import com.laotoua.dawnislandk.databinding.FragmentDisplaySettingBinding
import com.laotoua.dawnislandk.screens.MainActivity
import com.laotoua.dawnislandk.screens.util.Layout.toast
import com.laotoua.dawnislandk.screens.util.Layout.updateSwitchSummary


class DisplaySettingFragment : Fragment() {

    private var binding: FragmentDisplaySettingBinding? = null
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = FragmentDisplaySettingBinding.inflate(inflater, container, false)


        binding?.defaultTheme?.apply {
            key.setText(R.string.default_theme_setting)
            val entries = resources.getStringArray(R.array.default_theme_options).toList()
            summary.text = entries[applicationDataStore.defaultTheme]

            root.setOnClickListener {
                if (activity == null || !isAdded) return@setOnClickListener
                MaterialDialog(requireContext()).show {
                    lifecycleOwner(this@DisplaySettingFragment)
                    title(R.string.default_theme_setting)
                    listItemsSingleChoice(items = entries) { _, index, text ->
                        applicationDataStore.setDefaultTheme(index)
                        summary.text = text
                    }
                }
            }
        }

        binding?.timeFormat?.apply {
            key.setText(R.string.time_display_format)
            val entries = resources.getStringArray(R.array.time_format_options).toList()
            summary.text = entries[applicationDataStore.displayTimeFormat]
            root.setOnClickListener {
                if (activity == null || !isAdded) return@setOnClickListener
                MaterialDialog(requireContext()).show {
                    lifecycleOwner(this@DisplaySettingFragment)
                    title(R.string.time_display_format)
                    listItemsSingleChoice(items = entries) { _, index, text ->
                        applicationDataStore.setDisplayTimeFormat(index)
                        summary.text = text
                        toast(R.string.restart_to_apply_setting)
                    }
                }
            }
        }

        binding?.animationSwitch?.apply {
            key.setText(R.string.animation_settings)
            val options = resources.getStringArray(R.array.adapter_animation_options)
            summary.text = options[applicationDataStore.animationOption]
            root.setOnClickListener {
                if (activity == null || !isAdded) return@setOnClickListener
                MaterialDialog(requireContext()).show {
                    lifecycleOwner(this@DisplaySettingFragment)
                    title(res = R.string.animation_settings)
                    checkBoxPrompt(res = R.string.animation_first_only) {}
                    listItemsSingleChoice(items = options.toList()) { _, index, _ ->
                        applicationDataStore.setAnimationOption(index)
                        applicationDataStore.setAnimationFirstOnly(isCheckPromptChecked())
                        toast(R.string.restart_to_apply_setting)
                        summary.text = options[index]
                    }
                    positiveButton(R.string.submit)
                    negativeButton(R.string.cancel)
                }
            }
        }

        binding?.layoutCustomization?.apply {
            key.setText(R.string.layout_customization)
            preferenceSwitch.visibility = View.VISIBLE
            preferenceSwitch.isClickable = true
            preferenceSwitch.isChecked = applicationDataStore.getLayoutCustomizationStatus()
            updateSwitchSummary(R.string.layout_customization_on, R.string.layout_customization_off)
            preferenceSwitch.setOnCheckedChangeListener { _, isChecked ->
                applicationDataStore.setLayoutCustomizationStatus(isChecked)
                updateSwitchSummary(
                    R.string.layout_customization_on,
                    R.string.layout_customization_off
                )
                toast(R.string.restart_to_apply_setting)
            }
            root.setOnClickListener {
                if (activity == null || !isAdded) return@setOnClickListener
                val action =
                    DisplaySettingFragmentDirections.actionDisplaySettingFragmentToSizeCustomizationFragment()
                findNavController().navigate(action)
            }
        }

        binding?.toolbarCustomization?.apply {
            key.setText(R.string.toolbar_customization)
            preferenceSwitch.visibility = View.VISIBLE
            preferenceSwitch.isClickable = true
            preferenceSwitch.isChecked = applicationDataStore.getCustomToolbarImageStatus()
            updateSwitchSummary(
                R.string.toolbar_customization_on,
                R.string.toolbar_customization_off
            )
            preferenceSwitch.setOnCheckedChangeListener { _, isChecked ->
                applicationDataStore.setCustomToolbarImageStatus(isChecked)
                updateSwitchSummary(
                    R.string.toolbar_customization_on,
                    R.string.toolbar_customization_off
                )
                toast(R.string.restart_to_apply_setting)
            }
            root.setOnClickListener {
                if (activity == null || !isAdded) return@setOnClickListener
                val caller = requireActivity() as MainActivity
                if (!caller.intentsHelper.checkAndRequestAllPermissions(
                        caller,
                        arrayOf(
                            Manifest.permission.READ_EXTERNAL_STORAGE,
                            Manifest.permission.WRITE_EXTERNAL_STORAGE
                        )
                    )
                ) {
                    return@setOnClickListener
                }
                caller.intentsHelper.setToolbarBackgroundImage(caller)
            }
        }

        return binding!!.root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        binding = null
    }
}