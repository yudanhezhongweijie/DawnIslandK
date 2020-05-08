package com.laotoua.dawnislandk.ui.popup

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Environment
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.InputMethodManager
import android.widget.*
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.afollestad.materialdialogs.MaterialDialog
import com.afollestad.materialdialogs.callbacks.onDismiss
import com.afollestad.materialdialogs.customview.customView
import com.afollestad.materialdialogs.list.listItemsSingleChoice
import com.google.android.material.button.MaterialButtonToggleGroup
import com.laotoua.dawnislandk.R
import com.laotoua.dawnislandk.data.entity.Cookie
import com.laotoua.dawnislandk.data.network.APISuccessMessageResponse
import com.laotoua.dawnislandk.data.network.MessageType
import com.laotoua.dawnislandk.data.network.NMBServiceClient
import com.laotoua.dawnislandk.data.state.AppState
import com.laotoua.dawnislandk.io.FragmentIntentUtil
import com.laotoua.dawnislandk.io.ImageUtil
import com.laotoua.dawnislandk.ui.adapter.QuickAdapter
import com.laotoua.dawnislandk.ui.util.ReadableTime
import com.lxj.xpopup.XPopup
import com.lxj.xpopup.core.BottomPopupView
import com.lxj.xpopup.interfaces.SimpleCallback
import com.lxj.xpopup.util.KeyboardUtils
import kotlinx.coroutines.launch
import timber.log.Timber
import java.io.File


@SuppressLint("ViewConstructor")
class PostPopup(private val caller: Fragment, context: Context) :
    BottomPopupView(context) {

    companion object {
        fun show(
            caller: Fragment,
            postPopup: PostPopup,
            targetId: String,
            newPost: Boolean = false,
            forumNameMap: Map<String, String>? = null
        ) {
            XPopup.Builder(caller.context)

                .setPopupCallback(object : SimpleCallback() {
                    override fun beforeShow() {
                        postPopup.targetId = targetId
                        postPopup.newPost = newPost
                        postPopup.forumNameMap = forumNameMap
                        postPopup.updateView()
                        super.beforeShow()
                    }

                })
                .enableDrag(false)
                .moveUpToKeyboard(false)
                .asCustom(postPopup)
                .show()
        }
    }

    var newPost = false
    var targetId = ""
    var name = ""
    var email = ""
    var title = ""
    var content = ""
    var forumNameMap: Map<String, String>? = null

    var waterMark: String? = null
    var imageFile: File? = null
    var userHash = ""

    private var previewUri: Uri? = null

    private var cookies = listOf<Cookie>()

    private var selectedCookie: Cookie? = null

    private var toggleContainers: ConstraintLayout? = null
    private var expansionContainer: LinearLayout? = null
    private var attachmentContainer: ConstraintLayout? = null
    private var emojiContainer: RecyclerView? = null
    private val emojiAdapter: QuickAdapter by lazy { QuickAdapter(R.layout.grid_item_emoji) }
    private var luweiStickerContainer: RecyclerView? = null
    private val luweiStickerAdapter: QuickAdapter by lazy { QuickAdapter(R.layout.grid_item_luwei_sticker) }
    private var postContent: EditText? = null
    private var postImagePreview: ImageView? = null

    // keyboard height listener
    private var keyboardHeight = -1
    private var keyboardHolder: LinearLayout? = null

    private val postProgress by lazy {
        MaterialDialog(context).apply {
            customView(R.layout.dialog_progress)
            cancelable(false)
            title(R.string.sending)
        }
    }

    private val reportReasonPopup by lazy {
        XPopup.Builder(getContext())
            .dismissOnTouchOutside(false)
            .asCenterList(
                context.getString(R.string.report_reasons),
                resources.getStringArray(R.array.report_reasons)
            ) { _: Int, text: String? ->
                postContent!!.append("\n${context.getString(R.string.report_reasons)}: $text")
            }
    }

    private var progressBar: ProgressBar? = null

    private fun updateTitle(targetId: String, newPost: Boolean) {
        findViewById<TextView>(R.id.postTitle).text =
            if (newPost) context.getString(R.string.new_post)
            else "${context.getString(R.string.reply)} >> No. $targetId"
    }

    private fun updateForumButton() {
        findViewById<Button>(R.id.postForum).visibility = if (!newPost) View.GONE else View.VISIBLE
    }

    private fun updateCookies() {
        cookies = AppState.cookies!!
        if (selectedCookie == null || cookies.isNullOrEmpty()) {
            findViewById<TextView>(R.id.postCookie)?.run {
                text = if (cookies.isNullOrEmpty()) {
                    context.getString(R.string.missing_cookie)
                } else {
                    selectedCookie = cookies[0]
                    selectedCookie!!.cookieName
                }
            }
        }
    }

    fun updateView() {
        updateTitle(targetId, newPost)
        updateCookies()
        updateForumButton()
    }

    override fun getImplLayoutId(): Int {
        return R.layout.popup_post
    }

    override fun show(): BottomPopupView {
        if (parent != null) return this
        val activity = context as Activity
        popupInfo.decorView = activity.window.decorView as ViewGroup
        KeyboardUtils.registerSoftInputChangedListener(
            activity,
            this
        ) { height ->
            if (height > 0 && keyboardHeight != height) {
                keyboardHeight = height
                listOf(emojiContainer!!, luweiStickerContainer!!).map {
                    val lp = it.layoutParams
                    lp.height = keyboardHeight
                    it.layoutParams = lp
                }
            }
            val lp = keyboardHolder!!.layoutParams
            lp.height = height
            keyboardHolder!!.layoutParams = lp
        }
        // 1. add PopupView to its decorView after measured.
        popupInfo.decorView.post {
            if (parent != null) {
                (parent as ViewGroup).removeView(this)
            }
            popupInfo.decorView.addView(
                this, LayoutParams(
                    LayoutParams.MATCH_PARENT,
                    LayoutParams.MATCH_PARENT
                )
            )

            //2. do init，game start.
            init()
        }
        return this
    }

    override fun onCreate() {
        super.onCreate()
        postContent = findViewById<EditText>(R.id.postContent)
            .also {
                showSoftKeyboard(it)
            }

        toggleContainers = findViewById<ConstraintLayout>(R.id.toggleContainers).also {
            expansionContainer = findViewById(R.id.expansionContainer)

            progressBar = findViewById(R.id.progressBar)

            // add emoji
            emojiContainer = findViewById(R.id.emojiContainer)
            emojiContainer!!.layoutManager = GridLayoutManager(context, 3)
            emojiContainer!!.adapter = emojiAdapter.also { adapter ->
                adapter.setOnItemClickListener { _, view, _ ->
                    postContent!!.text.insert(
                        postContent!!.selectionStart,
                        ((view as TextView).text)
                    )
                }
                progressBar!!.visibility = View.VISIBLE
                caller.lifecycleScope.launch {
                    adapter.setDiffNewData(resources.getStringArray(R.array.emoji).toMutableList())
                    progressBar!!.visibility = View.GONE
                }
            }

            // add luweiSticker
            luweiStickerContainer = findViewById(R.id.luweiStickerContainer)
            luweiStickerContainer!!.layoutManager = GridLayoutManager(context, 3)
            luweiStickerContainer!!.adapter = luweiStickerAdapter.also { adapter ->
                adapter.setOnItemClickListener { _, _, pos ->
                    val emojiId = adapter.getItem(pos) as String
                    val resourceId: Int = context.resources.getIdentifier(
                        "le$emojiId", "drawable",
                        context.packageName
                    )
                    try {
                        imageFile =
                            ImageUtil.getFileFromDrawable(caller, emojiId, resourceId)
                        postImagePreview!!.setImageResource(resourceId)
                        attachmentContainer!!.visibility = View.VISIBLE
                    } catch (e: Exception) {
                        Timber.e(e)
                    }
                }

                progressBar!!.visibility = View.VISIBLE
                caller.lifecycleScope.launch {
                    adapter.setDiffNewData(
                        resources.getStringArray(R.array.LuweiStickers).toMutableList()
                    )
                    progressBar!!.visibility = View.GONE
                }
            }

            keyboardHolder = findViewById(R.id.keyboardHolder)
        }

        attachmentContainer = findViewById<ConstraintLayout>(R.id.attachmentContainer).also {
            postImagePreview = findViewById(R.id.postImagePreview)
        }

        findViewById<Button>(R.id.postForum).let { button ->
            button.setOnClickListener {
                MaterialDialog(context).show {
                    listItemsSingleChoice(items = forumNameMap!!.values.drop(1)) { _, index, text ->
                        targetId = forumNameMap!!.keys.drop(1).toList()[index]
                        button.text = text
                    }//去除时间线
                }.onDismiss {
                    if (button.text == "值班室") {
                        reportReasonPopup.show()
                    }
                }
            }
        }

        findViewById<Button>(R.id.postSend).setOnClickListener {
            hideKeyboardFrom(context, this)
            send()
        }

        findViewById<MaterialButtonToggleGroup>(R.id.toggleButtonGroup)
            .addOnButtonCheckedListener { _, checkedId, isChecked ->
                when (checkedId) {
                    R.id.postExpand -> {
                        expansionContainer!!.visibility = if (isChecked) View.VISIBLE else View.GONE
                    }

                    R.id.postFace -> {
                        hideKeyboardFrom(context, this)
                        emojiContainer!!.visibility = if (isChecked) View.VISIBLE else View.GONE
                    }

                    R.id.postLuwei -> {
                        hideKeyboardFrom(context, this)
                        luweiStickerContainer!!.visibility =
                            if (isChecked) View.VISIBLE else View.GONE
                    }
                }
            }

        findViewById<Button>(R.id.postDoodle).setOnClickListener {
            if (!FragmentIntentUtil.checkWriteStoragePermission(caller)
                || !FragmentIntentUtil.checkReadStoragePermission(caller)
            ) {
                return@setOnClickListener
            }
            FragmentIntentUtil.drawNewDoodle(caller) { uri ->
                uri?.let {
                    Timber.d("Made a doodle. Setting preview thumbnail...")
                    ImageUtil.loadImageThumbnailToImageView(
                        caller,
                        uri,
                        150,
                        150,
                        postImagePreview!!
                    )
                    attachmentContainer!!.visibility = View.VISIBLE
                }
            }
        }

        // TODO: save
        findViewById<Button>(R.id.postSave).apply {
            visibility = View.GONE
        }

        findViewById<Button>(R.id.postCookie).setOnClickListener {
            if (!cookies.isNullOrEmpty()) {
                hideKeyboardFrom(context, this)
                XPopup.Builder(context)
                    .atView(it) // 依附于所点击的View，内部会自动判断在上方或者下方显示
                    .asAttachList(
                        cookies.map { c -> c.cookieName }.toTypedArray(),
                        intArrayOf(
                            R.mipmap.ic_launcher, R.mipmap.ic_launcher, R.mipmap.ic_launcher,
                            R.mipmap.ic_launcher, R.mipmap.ic_launcher
                        )
                    ) { ind, _ ->
                        selectedCookie = cookies[ind]
                        findViewById<TextView>(R.id.postCookie).text = selectedCookie!!.cookieName
                    }
                    .show()
            } else {
                Toast.makeText(caller.context, R.string.missing_cookie, Toast.LENGTH_SHORT).show()
            }
        }


        findViewById<Button>(R.id.postImage).setOnClickListener {
            if (!FragmentIntentUtil.checkReadStoragePermission(caller)) {
                return@setOnClickListener
            }
            FragmentIntentUtil.getImageFromGallery(caller, "image/*") { uri: Uri? ->
                if (uri != null) {
                    imageFile = ImageUtil.getImageFileFromUri(caller, uri)
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
                }
            }
        }

        findViewById<Button>(R.id.postImageDelete).setOnClickListener {
            imageFile = null
            postImagePreview!!.setImageResource(android.R.color.transparent)
            attachmentContainer!!.visibility = View.GONE
        }

        findViewById<Button>(R.id.postCamera).setOnClickListener {
            if (!caller.requireActivity().packageManager.hasSystemFeature(PackageManager.FEATURE_CAMERA_ANY)) {
                Toast.makeText(context, R.string.need_camera, Toast.LENGTH_LONG).show()
                return@setOnClickListener
            }
            if (!FragmentIntentUtil.checkPermissions(caller)) {
                return@setOnClickListener
            }
            val timeStamp: String = ReadableTime.getFilenamableTime(System.currentTimeMillis())
            val relativeLocation =
                Environment.DIRECTORY_PICTURES + File.separator + "Dawn"
            val name = "DawnIsland_$timeStamp"
            val ext = "jpg"
            try {
                ImageUtil.addPlaceholderImageUriToGallery(
                    caller.requireActivity(),
                    name,
                    ext,
                    relativeLocation
                )
                    ?.run {
                        previewUri = this
                        FragmentIntentUtil.getImageFromCamera(caller, this)
                        { success: Boolean ->
                            if (success) {
                                Timber.d("Took a Picture. Setting preview thumbnail...")
                                ImageUtil.loadImageThumbnailToImageView(
                                    caller,
                                    previewUri!!,
                                    150,
                                    150,
                                    postImagePreview!!
                                )
                                attachmentContainer!!.visibility = View.VISIBLE
                            } else {
                                Timber.d("Didn't take a Picture. Removing placeholder Image...")
                                ImageUtil.removePlaceholderImageUriToGallery(
                                    caller.requireActivity(),
                                    this
                                )
                            }
                        }
                    }
            } catch (e: Exception) {
                Timber.e(e, "Failed to take a picture...")
            }
        }


        findViewById<CheckBox>(R.id.postWater).setOnClickListener {
            waterMark = if ((it as CheckBox).isChecked) "true" else null
        }

        findViewById<ImageView>(R.id.postClose).setOnClickListener {
            dismissWith {
                val lp = keyboardHolder!!.layoutParams
                lp.height = 0
                keyboardHolder!!.layoutParams = lp
            }
        }
    }

    private fun clearEntries() {
        postContent!!.text.clear()
        findViewById<TextView>(R.id.formName).text = ""
        findViewById<TextView>(R.id.formEmail).text = ""
        findViewById<TextView>(R.id.formTitle).text = ""
        imageFile = null
        postImagePreview!!.setImageResource(0)
        findViewById<MaterialButtonToggleGroup>(R.id.toggleButtonGroup).clearChecked()
    }

    private fun send() {
        if (selectedCookie == null) {
            Toast.makeText(caller.context, R.string.need_cookie_to_post, Toast.LENGTH_SHORT).show()
            return
        }
        name = findViewById<TextView>(R.id.formName).text.toString()
        email = findViewById<TextView>(R.id.formEmail).text.toString()
        title = findViewById<TextView>(R.id.formTitle).text.toString()
        content = postContent!!.text.toString()
        if (content == "" && imageFile == null) {
            Toast.makeText(caller.context, R.string.need_content_to_post, Toast.LENGTH_SHORT).show()
            return
        }

        userHash = selectedCookie?.cookieHash ?: ""

        postProgress.show()
        Timber.i("Sending...")
        caller.lifecycleScope.launch {
            NMBServiceClient.sendPost(
                newPost,
                targetId,
                name,
                email,
                title,
                content,
                waterMark,
                imageFile,
                userHash
            ).run {
                val message = when (this) {
                    is APISuccessMessageResponse -> {
                        if (this.messageType == MessageType.String) {
                            message
                        } else {
                            dom!!.getElementsByClass("system-message")
                                .first().children().not(".jump").text()
                        }
                    }
                    else -> {
                        Timber.e(message)
                        message
                    }
                }
                if (message.substring(0, 2) == ":)") {
                    clearEntries()
                }
                postProgress.dismiss()
                dismiss()
                Toast.makeText(caller.context, message, Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun hideKeyboardFrom(context: Context, view: View) {
        (context.getSystemService(Activity.INPUT_METHOD_SERVICE) as InputMethodManager)
            .hideSoftInputFromWindow(view.windowToken, 0)

    }

    private fun showSoftKeyboard(view: View) {
        if (view.requestFocus()) {
            (context.getSystemService(Activity.INPUT_METHOD_SERVICE) as InputMethodManager)
                .showSoftInput(view, InputMethodManager.SHOW_IMPLICIT)
        }
    }

}