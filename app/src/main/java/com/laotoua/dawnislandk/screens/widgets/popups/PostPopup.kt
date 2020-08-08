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

package com.laotoua.dawnislandk.screens.widgets.popups

import android.Manifest.permission
import android.annotation.SuppressLint
import android.content.Intent
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Shader
import android.net.Uri
import android.view.View
import android.view.animation.DecelerateInterpolator
import android.widget.*
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.content.ContextCompat.startActivity
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.afollestad.materialdialogs.MaterialDialog
import com.afollestad.materialdialogs.WhichButton
import com.afollestad.materialdialogs.actions.getActionButton
import com.afollestad.materialdialogs.callbacks.onDismiss
import com.afollestad.materialdialogs.checkbox.checkBoxPrompt
import com.afollestad.materialdialogs.checkbox.isCheckPromptChecked
import com.afollestad.materialdialogs.customview.customView
import com.afollestad.materialdialogs.list.listItemsSingleChoice
import com.google.android.material.button.MaterialButtonToggleGroup
import com.laotoua.dawnislandk.DawnApp.Companion.applicationDataStore
import com.laotoua.dawnislandk.R
import com.laotoua.dawnislandk.data.local.entity.Cookie
import com.laotoua.dawnislandk.screens.SharedViewModel
import com.laotoua.dawnislandk.screens.adapters.QuickAdapter
import com.laotoua.dawnislandk.util.DawnConstants
import com.laotoua.dawnislandk.util.ImageUtil
import com.laotoua.dawnislandk.util.IntentUtil
import com.laotoua.dawnislandk.util.lazyOnMainOnly
import com.lxj.xpopup.XPopup
import com.lxj.xpopup.core.BasePopupView
import com.lxj.xpopup.core.BottomPopupView
import com.lxj.xpopup.interfaces.SimpleCallback
import com.lxj.xpopup.util.KeyboardUtils
import kotlinx.coroutines.launch
import timber.log.Timber
import java.io.File


@SuppressLint("ViewConstructor")
class PostPopup(private val caller: FragmentActivity, private val sharedVM: SharedViewModel) :
    BottomPopupView(caller) {

    private var newPost = false
    private var targetId: String? = null
    var name = ""
    private var email = ""
    var title = ""
    var content = ""

    private var waterMark: String? = null
    private var imageFile: File? = null
    private var cookieHash = ""
    private var targetPage = 1
    private var targetFid = ""

    private var cookies = listOf<Cookie>()

    private var selectedCookie: Cookie? = null
    private var postCookie: Button? = null
    private var postForum: Button? = null

    private var toggleContainers: ConstraintLayout? = null
    private var expansionContainer: LinearLayout? = null
    private var attachmentContainer: ConstraintLayout? = null
    private var buttonToggleGroup: MaterialButtonToggleGroup? = null
    private var emojiContainer: RecyclerView? = null
    private val emojiAdapter by lazyOnMainOnly { QuickAdapter<String>(R.layout.grid_item_emoji) }
    private var luweiStickerContainer: ConstraintLayout? = null
    private val stickersWhite by lazyOnMainOnly {
        resources.getStringArray(R.array.LuweiStickersWhite).toMutableList()
    }
    private val stickersColor by lazyOnMainOnly {
        resources.getStringArray(R.array.LuweiStickersColor).toMutableList()
    }
    private val luweiStickerAdapter by lazyOnMainOnly { QuickAdapter<String>(R.layout.grid_item_luwei_sticker) }
    private var postContent: EditText? = null
    private var postImagePreview: ImageView? = null

    // keyboard height listener
    private var keyboardHolder: LinearLayout? = null

    private fun updateTitle(targetId: String?, newPost: Boolean) {
        findViewById<TextView>(R.id.postTitle).text =
            if (newPost) "${context.getString(R.string.new_post)} > ${getForumTitle(targetId!!)}"
            else "${context.getString(R.string.reply_comment)} > $targetId"
    }

    private fun getForumTitle(targetId: String): String {
        return if (targetId == "-1") ""
        else sharedVM.forumNameMapping[targetId] ?: ""
    }

    private fun updateForumButton(targetId: String?, newPost: Boolean) {
        findViewById<Button>(R.id.postForum).apply {
            visibility = if (!newPost) View.GONE else View.VISIBLE
            if (newPost && targetId != null && targetId != "-1") {
                text = getForumTitle(targetId)
            }
        }
    }

    private fun updateCookies() {
        cookies = applicationDataStore.cookies
        if (selectedCookie == null || cookies.isNullOrEmpty()) {
            findViewById<Button>(R.id.postCookie)?.run {
                text = if (cookies.isNullOrEmpty()) {
                    context.getString(R.string.missing_cookie)
                } else {
                    selectedCookie = cookies[0]
                    selectedCookie!!.cookieName
                }
            }
        }
    }

    fun updateView(targetId: String?, newPost: Boolean, quote: String?) {
        if (targetId != "-1") this.targetId = targetId // cannot post to timeline
        this.newPost = newPost
        updateTitle(targetId, newPost)
        updateCookies()
        updateForumButton(targetId, newPost)
        quote?.run { postContent?.text?.insert(0, quote) }
    }

    fun setupAndShow(
        targetId: String?,
        targetFid: String,
        newPost: Boolean = false,
        targetPage: Int = 1,
        quote: String? = null
    ) {
        this.targetPage = targetPage
        this.targetFid = targetFid
        XPopup.Builder(context)
            .setPopupCallback(object : SimpleCallback() {
                override fun beforeShow(popupView: BasePopupView?) {
                    super.beforeShow(popupView)
                    updateView(targetId, newPost, quote)
                }
            })
            .enableDrag(false)
            .moveUpToKeyboard(false)
            .asCustom(this)
            .show()
    }

    override fun getImplLayoutId(): Int {
        return R.layout.popup_post
    }

    override fun onShow() {
        super.onShow()

        KeyboardUtils.registerSoftInputChangedListener(caller.window, this) { height ->
            if (height > 0) {
                listOf(emojiContainer, luweiStickerContainer).map {
                    val lp = it?.layoutParams
                    lp?.height = height
                    it?.layoutParams = lp
                }
            }
            val lp = keyboardHolder?.layoutParams
            lp?.height = height
            keyboardHolder?.layoutParams = lp
            if (height > 0) {
                buttonToggleGroup?.uncheck(R.id.postFace)
                buttonToggleGroup?.uncheck(R.id.postLuwei)
            }
        }
    }

    override fun onCreate() {
        super.onCreate()

        postContent = findViewById<EditText>(R.id.postContent).apply {
            setOnClickListener { view -> KeyboardUtils.showSoftInput(view) }
        }

        toggleContainers = findViewById<ConstraintLayout>(R.id.toggleContainers).also {
            expansionContainer = findViewById(R.id.expansionContainer)
            keyboardHolder = findViewById(R.id.keyboardHolder)
            emojiContainer = findViewById(R.id.emojiContainer)
            emojiContainer!!.layoutManager = GridLayoutManager(context, 3)
            emojiContainer!!.adapter = emojiAdapter.also { adapter ->
                adapter.setOnItemClickListener { _, view, _ ->
                    postContent!!.text.insert(
                        postContent!!.selectionStart,
                        ((view as TextView).text)
                    )
                }
                adapter.setDiffNewData(resources.getStringArray(R.array.emoji).toMutableList())
            }

            // add luweiSticker
            luweiStickerContainer = findViewById(R.id.luweiStickerContainer)
            luweiStickerContainer.apply {
                findViewById<MaterialButtonToggleGroup>(R.id.luweiStickerToggle).addOnButtonCheckedListener { _, checkedId, isChecked ->
                    if (checkedId == R.id.luweiStickerWhite && isChecked) {
                        luweiStickerAdapter.setDiffNewData(stickersWhite)
                    } else if (checkedId == R.id.luweiStickerColor && isChecked) {
                        luweiStickerAdapter.setDiffNewData(stickersColor)
                    }
                }

                findViewById<Button>(R.id.luweiStickerColor).apply {
                    paint.shader = LinearGradient(
                        0f, 0f, paint.measureText(text.toString()), textSize, intArrayOf(
                            Color.parseColor("#F97C3C"),
                            Color.parseColor("#FDB54E"),
                            Color.parseColor("#64B678"),
                            Color.parseColor("#478AEA"),
                            Color.parseColor("#8446CC")
                        ), null, Shader.TileMode.CLAMP
                    )
                }
                findViewById<RecyclerView>(R.id.luweiStickerRecyclerView).apply {
                    layoutManager = GridLayoutManager(context, 3)
                    adapter = luweiStickerAdapter
                    luweiStickerAdapter.setOnItemClickListener { _, _, pos ->
                        val emojiId = luweiStickerAdapter.getItem(pos)
                        val resourceId: Int = context.resources.getIdentifier(
                            "le$emojiId", "drawable",
                            context.packageName
                        )
                        imageFile = ImageUtil.getFileFromDrawable(caller, emojiId, resourceId)
                        if (imageFile != null) {
                            postImagePreview!!.setImageResource(resourceId)
                            attachmentContainer!!.visibility = View.VISIBLE
                        } else {
                            Toast.makeText(
                                context,
                                R.string.cannot_load_image_file,
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    }

                }
            }
        }

        attachmentContainer = findViewById<ConstraintLayout>(R.id.attachmentContainer).apply {
            postImagePreview = findViewById(R.id.postImagePreview)
        }

        postForum = findViewById<Button>(R.id.postForum).apply {
            if (visibility == View.VISIBLE) {
                setOnClickListener {
                    KeyboardUtils.hideSoftInput(postContent!!)

                    MaterialDialog(context).show {
                        title(R.string.select_target_forum)
                        val mapping = sharedVM.forumNameMapping
                        //去除时间线
                        listItemsSingleChoice(items = mapping.values.drop(1)) { _, index, text ->
                            targetId = mapping.keys.drop(1).toList()[index]
                            targetFid = targetId!!
                            postForum!!.text = text
                        }
                    }.onDismiss {
                        if (targetId == null) return@onDismiss
                        postContent!!.hint =
                            applicationDataStore.luweiNotice?.nmbForums?.firstOrNull { f -> f.id == targetId }
                                ?.getPostRule()
                        updateTitle(targetId, newPost)
                        if (postForum!!.text == "值班室") {
                            MaterialDialog(context).show {
                                title(R.string.report_reasons)
                                listItemsSingleChoice(res = R.array.report_reasons) { _, _, text ->
                                    postContent!!.append("\n${context.getString(R.string.report_reasons)}: $text")
                                }
                                cancelOnTouchOutside(false)
                            }
                        }
                    }
                }
            }
        }

        findViewById<Button>(R.id.postSend).setOnClickListener {
            KeyboardUtils.hideSoftInput(postContent!!)
            send()
        }

        findViewById<MaterialButtonToggleGroup>(R.id.toggleButtonGroup).apply {
            buttonToggleGroup = this
            addOnButtonCheckedListener { toggleGroup, checkedId, isChecked ->
                when (checkedId) {
                    R.id.postExpand -> {
                        expansionContainer!!.visibility = if (isChecked) View.VISIBLE else View.GONE
                        toggleGroup.findViewById<Button>(R.id.postExpand)?.run {
                            if (isChecked) {
                                animate().setDuration(200)
                                    .setInterpolator(DecelerateInterpolator())
                                    .rotation(180f)
                                    .start()
                            } else {
                                animate().setDuration(200)
                                    .setInterpolator(DecelerateInterpolator())
                                    .rotation(0f)
                                    .start()
                            }
                        }
                    }

                    R.id.postFace -> {
                        if (isChecked) {
                            KeyboardUtils.hideSoftInput(postContent!!)
                        }
                        emojiContainer!!.visibility = if (isChecked) View.VISIBLE else View.GONE

                    }

                    R.id.postLuwei -> {
                        if (isChecked) {
                            KeyboardUtils.hideSoftInput(postContent!!)
                        }
                        luweiStickerContainer!!.visibility =
                            if (isChecked) View.VISIBLE else View.GONE
                    }
                }
            }
        }

        findViewById<Button>(R.id.postDoodle).setOnClickListener {
            if (!IntentUtil.checkAndRequestAllPermissions(
                    caller, arrayOf(
                        permission.READ_EXTERNAL_STORAGE,
                        permission.WRITE_EXTERNAL_STORAGE
                    )
                )
            ) {
                return@setOnClickListener
            }
            KeyboardUtils.hideSoftInput(postContent!!)
            IntentUtil.drawNewDoodle(caller) { uri ->
                Timber.d("Made a doodle. Prepare to upload...")
                compressAndPreviewImage(uri)
            }
        }

        // TODO: save draft
        findViewById<Button>(R.id.postSave).apply {
            visibility = View.GONE
        }

        postCookie = findViewById<Button>(R.id.postCookie).apply {
            setOnClickListener {
                if (!cookies.isNullOrEmpty()) {
                    KeyboardUtils.hideSoftInput(postContent!!)
                    MaterialDialog(context).show {
                        title(R.string.select_cookie)
                        listItemsSingleChoice(items = cookies.map { c -> c.cookieDisplayName }) { _, ind, text ->
                            selectedCookie = cookies[ind]
                            postCookie!!.text = text
                        }
                    }
                } else {
                    Toast.makeText(caller, R.string.missing_cookie, Toast.LENGTH_SHORT).show()
                }
            }
        }


        findViewById<Button>(R.id.postImage).setOnClickListener {
            if (!IntentUtil.checkAndRequestSinglePermission(
                    caller, permission.READ_EXTERNAL_STORAGE, true
                )
            ) {
                return@setOnClickListener
            }
            KeyboardUtils.hideSoftInput(postContent!!)
            IntentUtil.getImageFromGallery(caller, "image/*") { uri: Uri? ->
                Timber.d("Picked a local image. Prepare to upload...")
                compressAndPreviewImage(uri)
            }
        }

        findViewById<Button>(R.id.postImageDelete).setOnClickListener {
            imageFile = null
            postImagePreview!!.setImageResource(android.R.color.transparent)
            attachmentContainer!!.visibility = View.GONE
        }

        findViewById<Button>(R.id.postCamera).setOnClickListener {
            if (!IntentUtil.checkAndRequestSinglePermission(
                    caller,
                    permission.CAMERA,
                    true
                )
            ) {
                return@setOnClickListener
            }
            KeyboardUtils.hideSoftInput(postContent!!)
            IntentUtil.getImageFromCamera(caller) { uri: Uri ->
                Timber.d("Took a Picture. Prepare to upload...")
                compressAndPreviewImage(uri)
            }
        }

        findViewById<Button>(R.id.forumRule).setOnClickListener {
            MaterialDialog(context).show {
                val fid = if (newPost && targetId != null) targetId!! else targetFid
                val biId = if (fid.toInt() > 0) fid.toInt() else 1
                val resourceId: Int = context.resources.getIdentifier(
                    "bi_$biId", "drawable",
                    context.packageName
                )
                icon(resourceId)
                title(text = sharedVM.getForumDisplayName(fid))
                message(text = sharedVM.getForumMsg(fid)) { html() }
                positiveButton(R.string.acknowledge)
            }
        }

        findViewById<CheckBox>(R.id.postWater).setOnClickListener {
            waterMark = if ((it as CheckBox).isChecked) "true" else null
        }

        findViewById<Button>(R.id.postClose).setOnClickListener {
            KeyboardUtils.hideSoftInput(postContent!!)
            buttonToggleGroup?.clearChecked()
            dismiss()
        }

    }

    private fun compressAndPreviewImage(uri: Uri?) {
        if (uri == null) {
            Toast.makeText(context, R.string.cannot_load_image_file, Toast.LENGTH_SHORT).show()
            return
        }
        val compressDialog = MaterialDialog(context).show {
            title(R.string.compressing_oversize_image)
            customView(R.layout.dialog_progress)
            cancelable(false)
        }
        compressDialog.show()
        caller.lifecycleScope.launch {
            imageFile = ImageUtil.getCompressedImageFileFromUri(caller, uri)
            if (imageFile != null) {
                try {
                    ImageUtil.loadImageThumbnailToImageView(
                        caller,
                        uri,
                        150,
                        150,
                        postImagePreview!!
                    )
                    attachmentContainer!!.visibility = View.VISIBLE
                } catch (e: Exception) {
                    Timber.e(e, "Cannot load thumbnail from image...")
                }
            } else {
                Toast.makeText(
                    context,
                    R.string.compressing_oversize_image_error,
                    Toast.LENGTH_SHORT
                ).show()
            }
            compressDialog.dismiss()
        }
    }

    private fun clearEntries() {
        postContent!!.text.clear()
        findViewById<TextView>(R.id.formName).text = ""
        findViewById<TextView>(R.id.formEmail).text = ""
        findViewById<TextView>(R.id.formTitle).text = ""
        imageFile = null
        postImagePreview!!.setImageResource(0)
        attachmentContainer!!.visibility = View.GONE
        findViewById<MaterialButtonToggleGroup>(R.id.toggleButtonGroup).clearChecked()
        findViewById<MaterialButtonToggleGroup>(R.id.luweiStickerToggle).clearChecked()
    }

    private fun send() {
        if (!applicationDataStore.checkAcknowledgementPostingRule()) {
            MaterialDialog(context).show {
                title(R.string.please_comply_rules)
                cancelOnTouchOutside(false)
                checkBoxPrompt(R.string.acknowledge_post_rules) {
                    val uri = DawnConstants.nmbHost + "/forum"
                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse(uri))
                    if (intent.resolveActivity(caller.packageManager) != null) {
                        startActivity(caller, intent, null)
                    }
                    getActionButton(WhichButton.POSITIVE).isEnabled = isCheckPromptChecked()
                }
                positiveButton(R.string.acknowledge) {
                    applicationDataStore.acknowledgementPostingRule()
                }
                getActionButton(WhichButton.POSITIVE).isEnabled = false
            }
            return
        }

        if (selectedCookie == null) {
            Toast.makeText(caller, R.string.need_cookie_to_post, Toast.LENGTH_SHORT).show()
            return
        }
        if (targetId == null && newPost) {
            Toast.makeText(caller, R.string.please_select_target_forum, Toast.LENGTH_SHORT)
                .show()
            return
        }
        name = findViewById<TextView>(R.id.formName).text.toString()
        email = findViewById<TextView>(R.id.formEmail).text.toString()
        title = findViewById<TextView>(R.id.formTitle).text.toString()
        content = postContent!!.text.toString()
        if (content.isBlank()) {
            if (imageFile != null) {
                postContent!!.setText("分享图片")
                content = "分享图片"
            } else {
                Toast.makeText(caller, R.string.need_content_to_post, Toast.LENGTH_SHORT).show()
                return
            }
        }


        selectedCookie?.let { cookieHash = it.getApiHeaderCookieHash() }

        val postProgressDialog = MaterialDialog(context).show {
            title(R.string.sending)
            customView(R.layout.dialog_progress)
            cancelable(false)
        }
        Timber.i("Posting...")
        caller.lifecycleScope.launch {
            sharedVM.sendPost(
                newPost,
                targetId!!,
                name,
                email,
                title,
                content,
                waterMark,
                imageFile,
                cookieHash
            ).let { message ->
                postProgressDialog.dismiss()
                dismissWith {
                    if (message.substring(0, 2) == ":)") {
                        sharedVM.searchAndSavePost(
                            newPost,
                            targetId!!,
                            targetFid,
                            targetPage,
                            selectedCookie?.cookieName ?: "",
                            content
                        )
                        clearEntries()
                    }
                }
                Toast.makeText(caller, message, Toast.LENGTH_LONG).show()
            }
        }
    }
}