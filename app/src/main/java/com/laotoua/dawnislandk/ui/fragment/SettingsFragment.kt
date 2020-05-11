package com.laotoua.dawnislandk.ui.fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.drawerlayout.widget.DrawerLayout
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.laotoua.dawnislandk.R
import com.laotoua.dawnislandk.data.entity.Cookie
import com.laotoua.dawnislandk.data.state.AppState
import com.laotoua.dawnislandk.databinding.FragmentSettingsBinding
import com.lxj.xpopup.XPopup
import com.tencent.mmkv.MMKV
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class SettingsFragment : Fragment() {

    private lateinit var cookies: List<Cookie>

    init {
        lifecycleScope.launchWhenCreated { loadCookies() }
    }

    private val mmkv by lazy { MMKV.defaultMMKV() }

    private suspend fun loadCookies() {
        withContext(Dispatchers.IO) {
            AppState.loadCookies()
            cookies = AppState.cookies!!
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val binding = FragmentSettingsBinding.inflate(inflater, container, false)
        binding.toolbarLayout.toolbar.apply {
            setTitle(R.string.settings)
            subtitle = ""
            val drawerLayout = requireActivity().findViewById<DrawerLayout>(R.id.drawerLayout)
            drawerLayout.setDrawerLockMode(DrawerLayout.LOCK_MODE_LOCKED_CLOSED)
            setNavigationIcon(R.drawable.ic_arrow_back_white_24px)
            setNavigationOnClickListener {
                findNavController().popBackStack()
            }
        }

        binding.feedId.apply {
            key.setText(R.string.feedId)
            summary.text = mmkv.getString("feedId", "")
            root.setOnClickListener {
                val feedId = mmkv.getString("feedId", "")
                XPopup.Builder(context)
                    .asInputConfirm("修改订阅ID", "", feedId, feedId) { text ->
                        mmkv.putString("feedId", text)
                        summary.text = text
                        Toast.makeText(
                            context,
                            R.string.restart_to_apply_setting,
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                    .show()
            }
        }

        binding.timeFormat.apply {
            key.setText(R.string.time_display_format)
            val entries = resources.getStringArray(R.array.time_format_entries)
            val values = resources.getStringArray(R.array.time_format_values)
            summary.text = if (values.first() == mmkv.getString("time_format", "")) {
                entries.first()
            } else {
                entries.last()
            }

            root.setOnClickListener {
                XPopup.Builder(context)
                    .asCenterList("修改时间显示格式", entries) { position, text ->
                        mmkv.putString("time_format", values[position])
                        summary.text = text
                        Toast.makeText(
                            context,
                            R.string.restart_to_apply_setting,
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                    .show()
            }
        }

//        binding.cookie.apply {
//            key.setText(R.string.cookie_management)
//            root.setOnClickListener {
//                val cookiePopup = CookieManagerPopup(this@SettingsFragment, requireContext())
//                cookiePopup.setCookies(cookies)
//                XPopup.Builder(context)
//                    .asCustom(cookiePopup)
//                    .show()
//                    .dismissWith {
//                        if (cookies != cookiePopup.cookies) {
//                            lifecycleScope.launch(Dispatchers.IO) {
//                                Timber.i("Updating cookie entries...")
//                                Timber.i("Cookies: ${cookiePopup.cookies}")
//                                AppState.DB.cookieDao().resetCookies(cookiePopup.cookies)
//                                loadCookies()
//                            }
//                        }
//                    }
//            }
//        }

        binding.sizeCustomization.apply {
            key.setText(R.string.size_customization_settings)
            root.setOnClickListener {
                val action =
                    SettingsFragmentDirections.actionSettingsFragmentToSizeCustomizationFragment()
                findNavController().navigate(action)
            }

        }

        return binding.root
    }

}