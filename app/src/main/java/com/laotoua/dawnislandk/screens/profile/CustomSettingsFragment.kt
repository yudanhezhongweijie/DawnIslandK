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

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.laotoua.dawnislandk.R
import com.laotoua.dawnislandk.databinding.FragmentCustomSettingsBinding
import com.laotoua.dawnislandk.screens.MainActivity

class CustomSettingsFragment : Fragment() {

    private var binding: FragmentCustomSettingsBinding? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = FragmentCustomSettingsBinding.inflate(inflater,container,false)
        binding!!.forumSetting.apply {
            key.setText(R.string.common_forum_setting)
            root.setOnClickListener {
                val action = CustomSettingsFragmentDirections.actionCustomSettingsFragmentToForumSettingFragment()
                findNavController().navigate(action)
            }
        }

        // Inflate the layout for this fragment
        return binding!!.root
    }

    override fun onResume() {
        super.onResume()
        if (activity!=null && isAdded){
            (requireActivity() as MainActivity).setToolbarTitle(R.string.custom_settings)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        binding = null
    }
}