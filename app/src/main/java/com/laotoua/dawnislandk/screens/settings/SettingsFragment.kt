package com.laotoua.dawnislandk.screens.settings

import android.Manifest
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
import androidx.drawerlayout.widget.DrawerLayout
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.afollestad.materialdialogs.MaterialDialog
import com.afollestad.materialdialogs.WhichButton
import com.afollestad.materialdialogs.actions.getActionButton
import com.afollestad.materialdialogs.actions.setActionButtonEnabled
import com.afollestad.materialdialogs.checkbox.checkBoxPrompt
import com.afollestad.materialdialogs.customview.customView
import com.afollestad.materialdialogs.input.input
import com.afollestad.materialdialogs.list.listItems
import com.king.zxing.util.CodeUtils
import com.laotoua.dawnislandk.DawnApp.Companion.applicationDataStore
import com.laotoua.dawnislandk.R
import com.laotoua.dawnislandk.data.local.Cookie
import com.laotoua.dawnislandk.databinding.FragmentSettingsBinding
import com.laotoua.dawnislandk.databinding.ListItemCookieBinding
import com.laotoua.dawnislandk.databinding.ListItemPreferenceBinding
import com.laotoua.dawnislandk.screens.util.ToolBar.immersiveToolbar
import com.laotoua.dawnislandk.util.Constants
import com.laotoua.dawnislandk.util.FragmentIntentUtil
import com.laotoua.dawnislandk.util.ImageUtil
import com.laotoua.dawnislandk.util.lazyOnMainOnly
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import org.json.JSONObject
import timber.log.Timber

class SettingsFragment : Fragment() {

    private val cookies get() = applicationDataStore.cookies
    private val getCookieImage =
        registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
            uri?.run {
                try {
                    val file =
                        ImageUtil.getImageFileFromUri(fragment = this@SettingsFragment, uri = uri)
                            ?: return@registerForActivityResult
                    val res = CodeUtils.parseQRCode(file.path)
                    if (res != null) {
                        saveCookieWithInputName(res)
                    } else {
                        Toast.makeText(
                            context,
                            R.string.didnt_get_cookie_from_image,
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                } catch (e: Exception) {
                    // Ignore
                    Timber.e(e)
                }
            }
        }

    private var _binding: FragmentSettingsBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = FragmentSettingsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val cookieAdditionPopup: MaterialDialog by lazyOnMainOnly {
            MaterialDialog(requireContext()).apply {
                title(R.string.add_cookie)
                customView(R.layout.dialog_cookie_addition)
                cornerRadius(res = R.dimen.dp_10)
                positiveButton(R.string.submit) {
                    val cookieName = findViewById<EditText>(R.id.cookieNameText).text
                    val cookieHash = findViewById<EditText>(R.id.cookieHashText).text
                    if (cookieHash.toString().isNotBlank()) {
                        addCookie(
                            Cookie(
                                cookieHash.toString(),
                                cookieName.toString()
                            )
                        )
                        cookieHash.clear()
                        cookieName.clear()
                    }
                }
                negativeButton(R.string.cancel)
                findViewById<EditText>(R.id.cookieHashText).doOnTextChanged { text, _, _, _ ->
                    getActionButton(WhichButton.POSITIVE).isEnabled = !text.isNullOrBlank()
                }
            }
        }

        binding.toolbarLayout.toolbar.apply {
            immersiveToolbar()
            setTitle(R.string.settings)
            setSubtitle(R.string.toolbar_subtitle)
            val drawerLayout = requireActivity().findViewById<DrawerLayout>(R.id.drawerLayout)
            drawerLayout.setDrawerLockMode(DrawerLayout.LOCK_MODE_LOCKED_CLOSED)
            setNavigationIcon(R.drawable.ic_arrow_back_white_24px)
            setNavigationOnClickListener {
                findNavController().popBackStack()
            }
        }

        binding.feedId.apply {
            var feedId = applicationDataStore.feedId
            key.setText(R.string.feedId)
            summary.text = feedId
            root.setOnClickListener {
                MaterialDialog(requireContext()).show {
                    title(R.string.feedId)
                    cornerRadius(res = R.dimen.dp_10)
                    input(hint = feedId, prefill = feedId) { _, text ->
                        feedId = text.toString()
                        applicationDataStore.setFeedId(feedId)
                        summary.text = feedId
                        Toast.makeText(
                            context,
                            R.string.restart_to_apply_setting,
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                    positiveButton(R.string.submit)
                    negativeButton(R.string.cancel)
                }
            }
        }

        binding.timeFormat.apply {
            val mmkvKey = Constants.TIME_FORMAT
            key.setText(R.string.time_display_format)
            val entries = resources.getStringArray(R.array.time_format_entries)
            val values = resources.getStringArray(R.array.time_format_values)
            summary.text =
                if (values.first() == applicationDataStore.mmkv.getString(mmkvKey, "")) {
                    entries.first()
                } else {
                    entries.last()
                }

            root.setOnClickListener {
                MaterialDialog(requireContext()).show {
                    title(R.string.time_display_format)
                    cornerRadius(res = R.dimen.dp_10)
                    listItems(R.array.time_format_entries) { _, index, text ->
                        applicationDataStore.mmkv.putString(mmkvKey, values[index])
                        summary.text = text
                        Toast.makeText(
                            context,
                            R.string.restart_to_apply_setting,
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }
            }
        }

        binding.useReadingProgress.apply {
            val mmkvKey = Constants.READING_PROGRESS
            key.setText(R.string.saves_reading_progress)
            preferenceSwitch.visibility = View.VISIBLE
            preferenceSwitch.isChecked = applicationDataStore.mmkv.getBoolean(mmkvKey, false)
            updateSwitchSummary(R.string.reading_progress_on, R.string.reading_progress_off)
            root.setOnClickListener {
                toggleSwitch(mmkvKey, R.string.reading_progress_on, R.string.reading_progress_off)
                Toast.makeText(
                    context,
                    R.string.restart_to_apply_setting,
                    Toast.LENGTH_SHORT
                ).show()
            }
        }

        binding.animationSwitch.apply {
            val mmkvKey = Constants.ANIMATION
            key.setText(R.string.animation_on_off)
            preferenceSwitch.visibility = View.VISIBLE
            preferenceSwitch.isChecked = applicationDataStore.mmkv.getBoolean(mmkvKey, false)
            updateSwitchSummary(R.string.animation_on, R.string.animation_off)
            root.setOnClickListener {
                toggleSwitch(mmkvKey, R.string.animation_on, R.string.animation_off)
                Toast.makeText(
                    context,
                    R.string.restart_to_apply_setting,
                    Toast.LENGTH_SHORT
                ).show()
            }
        }

        cookies.map { addCookieToLayout(it) }

        binding.addCookie.apply {
            setOnClickListener {
                MaterialDialog(context).show {
                    title(R.string.add_cookie)
                    cornerRadius(res = R.dimen.dp_10)
                    listItems(R.array.cookie_addition_options) { _, index, _ ->
                        when (index) {
                            0 -> getCookieImage.launch("image/*")
                            1 -> {
                                if (FragmentIntentUtil.checkAndRequestSinglePermission(
                                        this@SettingsFragment,
                                        Manifest.permission.CAMERA,
                                        true
                                    )
                                ) {
                                    FragmentIntentUtil.getCookieFromQRCode(this@SettingsFragment) {
                                        it?.run {
                                            saveCookieWithInputName(it)
                                        }
                                    }
                                }
                            }
                            2 -> cookieAdditionPopup.show()
                        }
                    }
                }
            }
        }

        binding.sizeCustomization.apply {
            key.setText(R.string.size_customization_settings)
            root.setOnClickListener {
                val action =
                    SettingsFragmentDirections.actionSettingsFragmentToSizeCustomizationFragment()
                findNavController().navigate(action)
            }
        }

        binding.clearReplyCache.apply {
            key.setText(R.string.clear_reply_cache)
            root.setOnClickListener {
                MaterialDialog(requireContext()).show {
                    title(R.string.clear_reply_cache)
                    message(R.string.clear_reply_cache_confirm_message)
                    cornerRadius(res = R.dimen.dp_10)
                    setActionButtonEnabled(WhichButton.POSITIVE, false)
                    checkBoxPrompt(R.string.acknowledge) { checked ->
                        setActionButtonEnabled(WhichButton.POSITIVE, checked)
                    }
                    positiveButton(R.string.submit) {
                        GlobalScope.launch {
                            applicationDataStore.nukeReplyTable()
                            Toast.makeText(
                                context,
                                R.string.cleared_reply_cache_message,
                                Toast.LENGTH_SHORT
                            ).show()
                        }
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
                    cornerRadius(res = R.dimen.dp_10)
                    val items = listOf(
                        context.resources.getString(R.string.github),
                        context.resources.getString(R.string.email_author)
                    )
                    listItems(items = items) { _, index, _ ->
                        if (index == 0) {
                            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(Constants.GITHUB_ADDRESS))
                            if (intent.resolveActivity(requireActivity().packageManager) != null) {
                                startActivity(intent)
                            }
                        } else {
                            val intent = Intent(Intent.ACTION_SENDTO).apply {
                                type = "*/*"
                                data = Uri.parse("mailto:")
                                putExtra(Intent.EXTRA_EMAIL, arrayOf(Constants.AUTHOR_EMAIL))
                                putExtra(Intent.EXTRA_SUBJECT, context.resources.getString(R.string.app_feed_back))
                            }
                            if (intent.resolveActivity(requireActivity().packageManager) != null) {
                                startActivity(intent)
                            }

                        }
                    }
                }
            }
        }
    }

    private fun addCookieToLayout(cookie: Cookie) {
        val view = ListItemCookieBinding.inflate(layoutInflater)
        view.cookieName.text = cookie.cookieName
        view.edit.setOnClickListener {
            MaterialDialog(requireContext()).show {
                title(R.string.edit_cookie_remark)
                cornerRadius(res = R.dimen.dp_10)
                input(prefill = cookie.cookieName) { _, text ->
                    // Text submitted with the action button
                    cookie.cookieName = text.toString()
                    updateCookie(cookie)
                    view.cookieName.text = text.toString()
                }
                positiveButton(R.string.submit)
            }
        }

        view.remove.setOnClickListener {
            binding.cookieList.removeView(view.root)
            deleteCookie(cookie)
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

    private fun updateCookie(cookie: Cookie) {
        lifecycleScope.launch {
            applicationDataStore.updateCookie(cookie)
        }
    }

    private fun addCookie(cookie: Cookie) {
        lifecycleScope.launch {
            applicationDataStore.addCookie(cookie)
        }
        addCookieToLayout(cookie)
    }

    private fun deleteCookie(cookie: Cookie) {
        lifecycleScope.launch {
            applicationDataStore.deleteCookies(cookie)
        }
    }

    private fun saveCookieWithInputName(cookieJson: String) {
        val cookieHash = JSONObject(cookieJson).getString("cookie")
        MaterialDialog(requireContext()).show {
            title(R.string.edit_cookie_remark)
            cornerRadius(res = R.dimen.dp_10)
            cancelable(false)
            input(hint = cookieHash) { _, text ->
                addCookie(
                    Cookie(
                        cookieName = text.toString(),
                        cookieHash = cookieHash
                    )
                )
            }
            positiveButton(R.string.submit)
        }
    }

    private fun ListItemPreferenceBinding.toggleSwitch(
        mmkvKey: String,
        summaryOn: Int,
        summaryOff: Int
    ) {
        preferenceSwitch.toggle()
        applicationDataStore.mmkv.putBoolean(mmkvKey, preferenceSwitch.isChecked)
        updateSwitchSummary(summaryOn, summaryOff)
    }

    private fun ListItemPreferenceBinding.updateSwitchSummary(summaryOn: Int, summaryOff: Int) {
        if (preferenceSwitch.isChecked) {
            summary.setText(summaryOn)
        } else {
            summary.setText(summaryOff)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

}