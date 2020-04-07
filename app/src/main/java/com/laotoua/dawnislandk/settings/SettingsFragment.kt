package com.laotoua.dawnislandk.settings

import android.os.Bundle
import androidx.preference.PreferenceFragmentCompat
import com.laotoua.dawnislandk.R

class SettingsFragment : PreferenceFragmentCompat() {
    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.root_preferences, rootKey)
    }
}