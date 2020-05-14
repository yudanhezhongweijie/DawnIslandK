package com.laotoua.dawnislandk.ui.fragment

import android.Manifest
import android.annotation.SuppressLint
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.drawerlayout.widget.DrawerLayout
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.afollestad.materialdialogs.MaterialDialog
import com.afollestad.materialdialogs.input.input
import com.afollestad.materialdialogs.list.listItems
import com.king.zxing.util.CodeUtils
import com.laotoua.dawnislandk.R
import com.laotoua.dawnislandk.data.entity.Cookie
import com.laotoua.dawnislandk.data.state.AppState
import com.laotoua.dawnislandk.databinding.FragmentSettingsBinding
import com.laotoua.dawnislandk.databinding.ListItemCookieBinding
import com.laotoua.dawnislandk.io.FragmentIntentUtil
import com.laotoua.dawnislandk.io.ImageUtil
import com.laotoua.dawnislandk.ui.popup.CookieAdditionPopup
import com.laotoua.dawnislandk.ui.util.ToolBarUtil.immersiveToolbar
import com.lxj.xpopup.XPopup
import com.lxj.xpopup.interfaces.SimpleCallback
import com.tencent.mmkv.MMKV
import kotlinx.coroutines.launch
import org.json.JSONObject
import timber.log.Timber

class SettingsFragment : Fragment() {

    private val cookies get() = AppState.cookies

    private val mmkv by lazy { MMKV.defaultMMKV() }

    private val cookieAdditionPopup: CookieAdditionPopup by lazy {
        CookieAdditionPopup(
            requireContext()
        )
    }

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

        binding.toolbarLayout.toolbar.apply {
            immersiveToolbar()
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
            var feedId = mmkv.getString("feedId", "")
            key.setText(R.string.feedId)
            summary.text = feedId
            root.setOnClickListener {
                MaterialDialog(requireContext()).show {
                    title(R.string.feedId)
                    cornerRadius(res = R.dimen.dialog_radius)
                    input(hint = feedId, prefill = feedId) { _, text ->
                        feedId = text.toString()
                        mmkv.putString("feedId", feedId)
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
            key.setText(R.string.time_display_format)
            val entries = resources.getStringArray(R.array.time_format_entries)
            val values = resources.getStringArray(R.array.time_format_values)
            summary.text = if (values.first() == mmkv.getString("time_format", "")) {
                entries.first()
            } else {
                entries.last()
            }

            root.setOnClickListener {
                MaterialDialog(requireContext()).show {
                    title(R.string.time_display_format)
                    cornerRadius(res = R.dimen.dialog_radius)
                    listItems(R.array.time_format_entries) { _, index, text ->
                        mmkv.putString("time_format", values[index])
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

        cookies.map { addCookieToLayout(it) }

        binding.addCookie.apply {
            setOnClickListener {
                MaterialDialog(context).show {
                    title(R.string.add_cookie)
                    cornerRadius(res = R.dimen.dialog_radius)
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
                            2 -> XPopup.Builder(context)
                                .setPopupCallback(object : SimpleCallback() {
                                    override fun beforeShow() {
                                        cookieAdditionPopup.clearEntries()
                                        super.beforeShow()
                                    }
                                })
                                .asCustom(cookieAdditionPopup)
                                .show()
                                .dismissWith {
                                    if (cookieAdditionPopup.cookieHash != "") {
                                        addCookie(
                                            Cookie(
                                                cookieAdditionPopup.cookieHash,
                                                cookieAdditionPopup.cookieName
                                            )
                                        )
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
                    SettingsFragmentDirections.actionSettingsFragmentToSizeCustomizationFragment()
                findNavController().navigate(action)
            }
        }
    }

    @SuppressLint("SetTextI18n")
    private fun addCookieToLayout(cookie: Cookie) {
        val view = ListItemCookieBinding.inflate(layoutInflater)
        view.cookieName.text = cookie.cookieName
        view.edit.setOnClickListener {
            MaterialDialog(requireContext()).show {
                title(R.string.edit_cookie_remark)
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
            binding.cookieSummary.text = "${binding.cookieList.childCount} / 5"
        }

        binding.cookieList.addView(view.root)

        binding.cookieSummary.text = "${binding.cookieList.childCount} / 5"
        if (binding.cookieList.childCount >= 5) {
            binding.addCookie.isEnabled = false
        }
    }

    private fun updateCookie(cookie: Cookie) {
        lifecycleScope.launch {
            AppState.updateCookie(cookie)
        }
    }

    private fun addCookie(cookie: Cookie) {
        lifecycleScope.launch {
            AppState.addCookie(cookie)
        }
        addCookieToLayout(cookie)
    }

    private fun deleteCookie(cookie: Cookie) {
        lifecycleScope.launch {
            AppState.deleteCookies(cookie)
        }
    }

    private fun saveCookieWithInputName(cookieJson: String) {
        val cookieHash = JSONObject(cookieJson).getString("cookie")
        MaterialDialog(requireContext()).show {
            title(R.string.edit_cookie_remark)
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

}