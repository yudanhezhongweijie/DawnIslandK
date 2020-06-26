package com.laotoua.dawnislandk.screens.profile

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
import androidx.fragment.app.viewModels
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
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
import com.laotoua.dawnislandk.data.local.entity.Cookie
import com.laotoua.dawnislandk.databinding.FragmentProfileBinding
import com.laotoua.dawnislandk.databinding.ListItemCookieBinding
import com.laotoua.dawnislandk.databinding.ListItemPreferenceBinding
import com.laotoua.dawnislandk.screens.util.ToolBar.immersiveToolbar
import com.laotoua.dawnislandk.util.*
import dagger.android.support.DaggerFragment
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import org.json.JSONObject
import timber.log.Timber
import javax.inject.Inject

class ProfileFragment : DaggerFragment() {

    @Inject
    lateinit var viewModelFactory: ViewModelProvider.Factory
    private val viewModel: ProfileViewModel by viewModels { viewModelFactory }

    private val getCookieImage =
        registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
            uri?.run {
                try {
                    val file =
                        ImageUtil.getImageFileFromUri(fragment = this@ProfileFragment, uri = uri)
                            ?: return@registerForActivityResult
                    val res = CodeUtils.parseQRCode(file.path)
                    if (res != null) {
                        saveCookieWithInputName(res)
                    } else {
                        Toast.makeText(
                            context,
                            R.string.did_not_get_cookie_from_image,
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                } catch (e: Exception) {
                    // Ignore
                    Timber.e(e)
                }
            }
        }

    private var _binding: FragmentProfileBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = FragmentProfileBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.toolbarLayout.toolbar.apply {
            immersiveToolbar()
            setTitle(R.string.settings)
            setSubtitle(R.string.toolbar_subtitle)
        }

        binding.feedId.apply {
            var feedId = applicationDataStore.feedId
            key.setText(R.string.feedId)
            summary.text = feedId
            root.setOnClickListener {
                MaterialDialog(requireContext()).show {
                    title(R.string.feedId)
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
            key.setText(R.string.time_display_format)
            val entries = resources.getStringArray(R.array.time_format_entries)
            val values = resources.getStringArray(R.array.time_format_values)
            summary.text =
                if (values.first() == applicationDataStore.displayTimeFormat) {
                    entries.first()
                } else {
                    entries.last()
                }

            root.setOnClickListener {
                MaterialDialog(requireContext()).show {
                    title(R.string.time_display_format)
                    listItems(R.array.time_format_entries) { _, index, text ->
                        applicationDataStore.setDisplayTimeFormat(values[index])
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
            key.setText(R.string.saves_reading_progress)
            preferenceSwitch.visibility = View.VISIBLE
            preferenceSwitch.isChecked = applicationDataStore.readingProgressStatus
            updateSwitchSummary(R.string.reading_progress_on, R.string.reading_progress_off)
            root.setOnClickListener {
                toggleSwitch(
                    applicationDataStore::setReadingProgressStatus,
                    R.string.reading_progress_on,
                    R.string.reading_progress_off
                )
                Toast.makeText(
                    context,
                    R.string.restart_to_apply_setting,
                    Toast.LENGTH_SHORT
                ).show()
            }
        }

        binding.animationSwitch.apply {
            key.setText(R.string.animation_on_off)
            preferenceSwitch.visibility = View.VISIBLE
            preferenceSwitch.isChecked = applicationDataStore.animationStatus
            updateSwitchSummary(R.string.animation_on, R.string.animation_off)
            root.setOnClickListener {
                toggleSwitch(
                    applicationDataStore::setAnimationStatus,
                    R.string.animation_on,
                    R.string.animation_off
                )
                Toast.makeText(
                    context,
                    R.string.restart_to_apply_setting,
                    Toast.LENGTH_SHORT
                ).show()
            }
        }

        val loadingDialog by lazyOnMainOnly {
            MaterialDialog(requireContext()).apply {
                title(R.string.processing)
                customView(R.layout.dialog_progress)
            }
        }
        viewModel.loadingStatus.observe(viewLifecycleOwner, Observer {
            it.getContentIfNotHandled()?.run {
                when (loadingStatus) {
                    LoadingStatus.LOADING -> {
                        loadingDialog.show()
                    }
                    LoadingStatus.FAILED -> {
                        loadingDialog.dismiss()
                        Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
                    }
                    else -> {
                        loadingDialog.dismiss()
                    }
                }
            }
        })

        viewModel.cookies.observe(viewLifecycleOwner, Observer { cookies ->
            binding.cookieList.removeAllViews()
            cookies.map { addCookieToLayout(it) }
        })

        binding.addCookie.setOnClickListener {
            MaterialDialog(requireContext()).show {
                title(R.string.add_cookie)
                listItems(R.array.cookie_addition_options) { _, index, _ ->
                    when (index) {
                        0 -> getCookieImage.launch("image/*")
                        1 -> {
                            if (FragmentIntentUtil.checkAndRequestSinglePermission(
                                    this@ProfileFragment,
                                    Manifest.permission.CAMERA,
                                    true
                                )
                            ) {
                                FragmentIntentUtil.getCookieFromQRCode(this@ProfileFragment) {
                                    it?.run {
                                        saveCookieWithInputName(it)
                                    }
                                }
                            }
                        }
                        2 -> MaterialDialog(requireContext()).show {
                            title(R.string.add_cookie)
                            customView(R.layout.dialog_cookie_addition)
                            val submitButton = getActionButton(WhichButton.POSITIVE)
                            val neuralButton = getActionButton(WhichButton.NEUTRAL)
                            val cookieName = findViewById<EditText>(R.id.cookieNameText)
                            val cookieHash = findViewById<EditText>(R.id.cookieHashText)
                            submitButton.isEnabled = false
                            neuralButton.isEnabled = false
                            positiveButton(R.string.submit) {
                                val cookieNameText = cookieName.text.toString()
                                val cookieHashText = cookieHash.text.toString()
                                if (cookieHashText.isNotBlank() && cookieNameText.isNotBlank()) {
                                    viewModel.addNewCookie(cookieHashText, cookieNameText)
                                }
                            }
                            negativeButton(R.string.cancel)
                            cookieName.doOnTextChanged { text, _, _, _ ->
                                submitButton.isEnabled =
                                    !text.isNullOrBlank() && !cookieHash.text.isNullOrBlank()
                            }
                            cookieHash.doOnTextChanged { text, _, _, _ ->
                                submitButton.isEnabled =
                                    !text.isNullOrBlank() && !cookieName.text.isNullOrBlank()
                                neuralButton.isEnabled = !text.isNullOrBlank()
                            }
                            neutralButton(R.string.default_cookie_name) {
                                val cookieHashText = cookieHash.text.toString()
                                if (cookieHashText.isNotBlank()) {
                                    viewModel.addNewCookie(cookieHashText)
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
                    ProfileFragmentDirections.actionSettingsFragmentToSizeCustomizationFragment()
                findNavController().navigate(action)
            }
        }

        binding.clearCommentCache.apply {
            key.setText(R.string.clear_comment_cache)
            root.setOnClickListener {
                MaterialDialog(requireContext()).show {
                    title(R.string.clear_comment_cache)
                    message(R.string.clear_comment_cache_confirm_message)
                    setActionButtonEnabled(WhichButton.POSITIVE, false)
                    checkBoxPrompt(R.string.acknowledge) { checked ->
                        setActionButtonEnabled(WhichButton.POSITIVE, checked)
                    }
                    positiveButton(R.string.submit) {
                        GlobalScope.launch {
                            applicationDataStore.nukeCommentTable()
                            Toast.makeText(
                                context,
                                R.string.cleared_comment_cache_message,
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
                    val items = listOf(
                        context.resources.getString(R.string.github),
                        context.resources.getString(R.string.email_author)
                    )
                    listItems(items = items) { _, index, _ ->
                        if (index == 0) {
                            val intent =
                                Intent(Intent.ACTION_VIEW, Uri.parse(Constants.GITHUB_ADDRESS))
                            if (intent.resolveActivity(requireActivity().packageManager) != null) {
                                startActivity(intent)
                            }
                        } else {
                            val intent = Intent(Intent.ACTION_SENDTO).apply {
                                type = "*/*"
                                data = Uri.parse("mailto:")
                                putExtra(Intent.EXTRA_EMAIL, arrayOf(Constants.AUTHOR_EMAIL))
                                putExtra(
                                    Intent.EXTRA_SUBJECT,
                                    context.resources.getString(R.string.app_feed_back)
                                )
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
        view.cookieName.text = cookie.cookieDisplayName
        view.edit.setOnClickListener {
            MaterialDialog(requireContext()).show {
                title(R.string.edit_cookie_remark)
                input(
                    prefill = cookie.cookieDisplayName,
                    hint = cookie.cookieDisplayName
                ) { _, text ->
                    // Text submitted with the action button
                    cookie.cookieDisplayName = text.toString()
                    viewModel.updateCookie(cookie)
                }
                positiveButton(R.string.submit)
                negativeButton(R.string.default_cookie_name) {
                    dismiss()
                    cookie.cookieDisplayName = cookie.cookieName
                    viewModel.updateCookie(cookie)
                }
            }
        }

        view.remove.setOnClickListener {
            viewModel.deleteCookie(cookie)
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

    private fun saveCookieWithInputName(cookieJson: String) {
        val cookieHash = JSONObject(cookieJson).getString("cookie")
        MaterialDialog(requireContext()).show {
            title(R.string.edit_cookie_remark)
            cancelable(false)
            input(hint = cookieHash) { _, text ->
                viewModel.addNewCookie(cookieHash, text.toString())
            }
            positiveButton(R.string.submit)
            negativeButton(R.string.default_cookie_name) {
                viewModel.addNewCookie(cookieHash)
            }
        }
    }

    private fun ListItemPreferenceBinding.toggleSwitch(
        toggleFunc: (Boolean) -> Unit,
        summaryOn: Int,
        summaryOff: Int
    ) {
        preferenceSwitch.toggle()
        toggleFunc(preferenceSwitch.isChecked)
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