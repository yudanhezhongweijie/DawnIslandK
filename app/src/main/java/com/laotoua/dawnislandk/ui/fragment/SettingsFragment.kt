package com.laotoua.dawnislandk.ui.fragment

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
import com.lxj.xpopup.XPopup
import com.lxj.xpopup.interfaces.SimpleCallback
import com.tencent.mmkv.MMKV
import kotlinx.coroutines.launch
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
                                FragmentIntentUtil.getCookieFromQRCode(this@SettingsFragment) {
                                    it?.run {
                                        saveCookieWithInputName(it)
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

        return binding.root
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
            AppState.DB.cookieDao().updateCookie(cookie)
        }
        AppState.cookies.first { it.cookieHash == cookie.cookieHash }.cookieName =
            cookie.cookieName
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

    private fun saveCookieWithInputName(cookieHash: String) {
        MaterialDialog(requireContext()).show {
            title(R.string.edit_cookie_remark)
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