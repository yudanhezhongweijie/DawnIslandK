package com.laotoua.dawnislandk.settings

import android.os.Bundle
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import com.laotoua.dawnislandk.R
import com.laotoua.dawnislandk.components.CookieManagerPopup
import com.laotoua.dawnislandk.entities.Cookie
import com.laotoua.dawnislandk.util.AppState
import com.lxj.xpopup.XPopup
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import timber.log.Timber


// TODO: update app bar
class SettingsFragment : PreferenceFragmentCompat() {

    private lateinit var cookies: List<Cookie>

    init {
        lifecycleScope.launchWhenCreated { loadCookies() }
    }

    private suspend fun loadCookies() {
        withContext(Dispatchers.IO) {
            AppState.loadCookies()
            cookies = AppState.cookies!!
            Timber.i("Loaded cookies: $cookies")
        }
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


        findPreference<Preference>("sizes_customization")?.setOnPreferenceClickListener {
            val action =
                SettingsFragmentDirections.actionSettingsFragmentToSizeCustomizationFragment()
            findNavController().navigate(action)

            true
        }
    }
}