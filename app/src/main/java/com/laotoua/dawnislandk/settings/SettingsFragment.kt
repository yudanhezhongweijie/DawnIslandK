package com.laotoua.dawnislandk.settings

import android.os.Bundle
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import com.laotoua.dawnislandk.R
import com.laotoua.dawnislandk.components.CookieManagerPopup
import com.lxj.xpopup.XPopup
import timber.log.Timber


class SettingsFragment : PreferenceFragmentCompat() {
    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.root_preferences, rootKey)

        findPreference<Preference>("cookie")?.setOnPreferenceClickListener {
            Timber.i("Set cookie")
            val cookiePopup = CookieManagerPopup(requireContext())
            XPopup.Builder(context)
                .asCustom(cookiePopup)
                .show()

            true
        }
    }
}