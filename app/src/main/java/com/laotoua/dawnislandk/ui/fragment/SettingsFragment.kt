package com.laotoua.dawnislandk.ui.fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import com.laotoua.dawnislandk.R
import com.laotoua.dawnislandk.data.entity.Cookie
import com.laotoua.dawnislandk.data.state.AppState
import com.laotoua.dawnislandk.ui.popup.CookieManagerPopup
import com.laotoua.dawnislandk.viewmodel.SharedViewModel
import com.lxj.xpopup.XPopup
import com.tencent.mmkv.MMKV
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import timber.log.Timber


// TODO: update app bar
class SettingsFragment : PreferenceFragmentCompat() {

    private lateinit var cookies: List<Cookie>

    private val sharedVM: SharedViewModel by activityViewModels()

    init {
        lifecycleScope.launchWhenCreated { loadCookies() }
    }

    private val mmkv by lazy { MMKV.defaultMMKV() }

    private suspend fun loadCookies() {
        withContext(Dispatchers.IO) {
            AppState.loadCookies()
            cookies = AppState.cookies!!
            Timber.d("Loaded cookies: $cookies")
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        sharedVM.setFragment(this)
        return super.onCreateView(inflater, container, savedInstanceState)
    }

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.root_preferences, rootKey)

        findPreference<Preference>("cookie")?.setOnPreferenceClickListener {
            val cookiePopup = CookieManagerPopup(requireContext())
            cookiePopup.setCookies(cookies)
            XPopup.Builder(context)
                .asCustom(cookiePopup)
                .show()
                .dismissWith {
                    if (cookies != cookiePopup.cookies) {
                        lifecycleScope.launch(Dispatchers.IO) {
                            Timber.i("Updating cookie entries...")
                            Timber.i("Cookies: ${cookiePopup.cookies}")
                            AppState.DB.cookieDao().resetCookies(cookiePopup.cookies)
                            loadCookies()
                        }
                    }
                }
            true
        }

        findPreference<Preference>("feedId")?.apply {
            summary = mmkv.getString("feedId", "")

            setOnPreferenceClickListener {
                val feedId = mmkv.getString("feedId", "")
                XPopup.Builder(context)
                    .asInputConfirm("修改订阅ID", "", feedId, feedId) { text ->
                        mmkv.putString("feedId", text)
                        summary = text

                        Toast.makeText(
                            context,
                            R.string.restart_to_apply_setting,
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                    .show()
                true
            }
        }


        findPreference<Preference>("time_format")?.apply {
            val entries = resources.getStringArray(R.array.time_format_entries)
            val values = resources.getStringArray(R.array.time_format_values)
            summary = if (values.first() == mmkv.getString("time_format", "")) {
                entries.first()
            } else {
                entries.last()
            }

            setOnPreferenceClickListener {
                XPopup.Builder(context)
                    .asCenterList("修改时间显示格式", entries) { position, text ->
                        mmkv.putString("time_format", values[position])
                        summary = text
                        Toast.makeText(
                            context,
                            R.string.restart_to_apply_setting,
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                    .show()
                true
            }
        }

        findPreference<Preference>("sizes_customization")?.setOnPreferenceClickListener {
            val action =
                SettingsFragmentDirections.actionSettingsFragmentToSizeCustomizationFragment()
            findNavController().navigate(action)

            true
        }
    }
}