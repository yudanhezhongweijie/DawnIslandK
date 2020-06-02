package com.laotoua.dawnislandk.screens.widget.popup

import android.Manifest.permission
import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.net.Uri
import android.os.Environment
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.afollestad.materialdialogs.MaterialDialog
import com.afollestad.materialdialogs.callbacks.onDismiss
import com.afollestad.materialdialogs.customview.customView
import com.afollestad.materialdialogs.list.listItemsSingleChoice
import com.google.android.material.button.MaterialButtonToggleGroup
import com.laotoua.dawnislandk.DawnApp.Companion.applicationDataStore
import com.laotoua.dawnislandk.R
import com.laotoua.dawnislandk.data.local.Cookie
import com.laotoua.dawnislandk.data.remote.APISuccessMessageResponse
import com.laotoua.dawnislandk.data.remote.MessageType
import com.laotoua.dawnislandk.data.remote.NMBServiceClient
import com.laotoua.dawnislandk.screens.adapters.QuickAdapter
import com.laotoua.dawnislandk.util.FragmentIntentUtil
import com.laotoua.dawnislandk.util.ImageUtil
import com.laotoua.dawnislandk.util.ReadableTime
import com.laotoua.dawnislandk.util.lazyOnMainOnly
import com.lxj.xpopup.XPopup
import com.lxj.xpopup.core.BottomPopupView
import com.lxj.xpopup.interfaces.SimpleCallback
import com.lxj.xpopup.util.KeyboardUtils
import dagger.android.support.DaggerFragment
import kotlinx.coroutines.launch
import timber.log.Timber
import java.io.File
import javax.inject.Inject


@SuppressLint("ViewConstructor")
class PostPopup(private val caller: DaggerFragment, context: Context) :
    BottomPopupView(context) {

    init {
        caller.androidInjector().inject(this)
    }

    @Inject
    lateinit var webServiceClient: NMBServiceClient

    var newPost = false
    var targetId: String? = null
    var name = ""
    private var email = ""
    var title = ""
    var content = ""
    var forumNameMap: Map<String, String>? = null

    private var waterMark: String? = null
    private var imageFile: File? = null
    private var userHash = ""

    private var previewUri: Uri? = null

    private var cookies = listOf<Cookie>()

    private var selectedCookie: Cookie? = null
    private var postCookie: Button? = null
    private var postForum: Button? = null

    private var toggleContainers: ConstraintLayout? = null
    private var expansionContainer: LinearLayout? = null
    private var attachmentContainer: ConstraintLayout? = null
    private var emojiContainer: RecyclerView? = null
    private val emojiAdapter by lazyOnMainOnly { QuickAdapter<String>(R.layout.grid_item_emoji) }
    private var luweiStickerContainer: RecyclerView? = null
    private val luweiStickerAdapter by lazyOnMainOnly { QuickAdapter<String>(R.layout.grid_item_luwei_sticker) }
    private var postContent: EditText? = null
    private var postImagePreview: ImageView? = null

    // keyboard height listener
    private var keyboardHeight = -1
    private var keyboardHolder: LinearLayout? = null
    private var afterPostTask: (() -> Unit)? = null

    private val postProgress by lazyOnMainOnly {
        MaterialDialog(context).apply {
            title(R.string.sending)
            customView(R.layout.dialog_progress)
            cancelable(false)
        }
    }

    private val reportReasonPopup by lazyOnMainOnly {
        MaterialDialog(context).apply {
            title(R.string.report_reasons)
            listItemsSingleChoice(res = R.array.report_reasons) { _, _, text ->
                postContent!!.append("\n${context.getString(R.string.report_reasons)}: $text")
            }
            cancelOnTouchOutside(false)
        }
    }

    private val cookiePopup by lazyOnMainOnly {
        MaterialDialog(context).apply {
            title(R.string.select_cookie)
            listItemsSingleChoice(items = cookies.map { c -> c.cookieName }) { _, ind, text ->
                selectedCookie = cookies[ind]
                postCookie!!.text = text
            }
        }
    }

    private val targetForumPopup by lazyOnMainOnly {
        MaterialDialog(context).show {
            title(R.string.select_target_forum)
            listItemsSingleChoice(items = forumNameMap!!.values.drop(1)) { _, index, text ->
                targetId = forumNameMap!!.keys.drop(1).toList()[index]
                postForum!!.text = text
            }//去除时间线
        }.onDismiss {
            if (postForum!!.text == "值班室") {
                reportReasonPopup.show()
            }
        }
    }

    private var progressBar: ProgressBar? = null

    private fun updateTitle(targetId: String?, newPost: Boolean) {
        findViewById<TextView>(R.id.postTitle).text =
            if (newPost) context.getString(R.string.post_new_thread)
            else "${context.getString(R.string.reply)} >> No. $targetId"
    }

    private fun updateForumButton() {
        findViewById<Button>(R.id.postForum).visibility = if (!newPost) View.GONE else View.VISIBLE
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

    fun bindAfterPostTask(task: (() -> Unit)) {
        afterPostTask = task
    }

    fun updateView(quote: String?) {
        updateTitle(targetId, newPost)
        updateCookies()
        updateForumButton()
        quote?.run {
            postContent!!.text.insert(
                0,
                quote
            )
        }
    }

    fun setupAndShow(
        targetId: String?,
        newPost: Boolean = false,
        forumNameMap: Map<String, String>? = null,
        quote: String? = null,
        task: (() -> Unit)? = null
    ) {
        XPopup.Builder(context)
            .setPopupCallback(object : SimpleCallback() {
                override fun beforeShow() {
                    this@PostPopup.targetId = targetId
                    this@PostPopup.newPost = newPost
                    this@PostPopup.forumNameMap = forumNameMap
                    this@PostPopup.updateView(quote)
                    task?.run { this@PostPopup.bindAfterPostTask(this) }
                    super.beforeShow()
                }

            })
//            .enableDrag(false)
//            .moveUpToKeyboard(false)
            .asCustom(this@PostPopup)
            .show()
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
            .apply {
                KeyboardUtils.showSoftInput(this)
                setOnClickListener { view -> KeyboardUtils.showSoftInput(view) }
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
                    val emojiId = adapter.getItem(pos)
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

        attachmentContainer = findViewById<ConstraintLayout>(R.id.attachmentContainer).apply {
            postImagePreview = findViewById(R.id.postImagePreview)
        }

        postForum = findViewById<Button>(R.id.postForum).apply {
            if (visibility == View.VISIBLE) {
                setOnClickListener {
                    KeyboardUtils.hideSoftInput(postContent!!)
                    targetForumPopup.show()
                }
            }
        }

        findViewById<Button>(R.id.postSend).setOnClickListener {
            KeyboardUtils.hideSoftInput(postContent!!)
            send()
        }

        findViewById<MaterialButtonToggleGroup>(R.id.toggleButtonGroup)
            .addOnButtonCheckedListener { _, checkedId, isChecked ->
                when (checkedId) {
                    R.id.postExpand -> {
                        expansionContainer!!.visibility = if (isChecked) View.VISIBLE else View.GONE
                    }

                    R.id.postFace -> {
                        KeyboardUtils.hideSoftInput(postContent!!)
                        emojiContainer!!.visibility = if (isChecked) View.VISIBLE else View.GONE
                    }

                    R.id.postLuwei -> {
                        KeyboardUtils.hideSoftInput(postContent!!)
                        luweiStickerContainer!!.visibility =
                            if (isChecked) View.VISIBLE else View.GONE
                    }
                }
            }

        findViewById<Button>(R.id.postDoodle).setOnClickListener {
            if (!FragmentIntentUtil.checkAndRequestAllPermissions(
                    caller, arrayOf(
                        permission.READ_EXTERNAL_STORAGE,
                        permission.WRITE_EXTERNAL_STORAGE
                    )
                )
            ) {
                return@setOnClickListener
            }
            FragmentIntentUtil.drawNewDoodle(caller) { uri ->
                uri?.let {
                    Timber.d("Made a doodle. Setting preview thumbnail...")
                    ImageUtil.loadImageThumbnailToImageView(
                        caller,
                        it,
                        150,
                        150,
                        postImagePreview!!
                    )
                    imageFile = ImageUtil.getImageFileFromUri(fragment = caller, uri = it)
                    attachmentContainer!!.visibility = View.VISIBLE
                }
            }
        }

        // TODO: save
        findViewById<Button>(R.id.postSave).apply {
            visibility = View.GONE
        }

        postCookie = findViewById<Button>(R.id.postCookie).apply {
            setOnClickListener {
                if (!cookies.isNullOrEmpty()) {
                    KeyboardUtils.hideSoftInput(postContent!!)
                    cookiePopup.show()
                } else {
                    Toast.makeText(caller.context, R.string.missing_cookie, Toast.LENGTH_SHORT)
                        .show()
                }
            }
        }


        findViewById<Button>(R.id.postImage).setOnClickListener {
            if (!FragmentIntentUtil.checkAndRequestSinglePermission(
                    caller, permission.READ_EXTERNAL_STORAGE, true
                )
            ) {
                return@setOnClickListener
            }
            FragmentIntentUtil.getImageFromGallery(caller, "image/*") { uri: Uri? ->
                if (uri != null) {
                    imageFile = ImageUtil.getImageFileFromUri(fragment = caller, uri = uri)
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
            if (!FragmentIntentUtil.checkAndRequestSinglePermission(
                    caller,
                    permission.CAMERA,
                    true
                )
            ) {
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
                                imageFile =
                                    ImageUtil.getImageFileFromUri(fragment = caller, uri = this)
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
            KeyboardUtils.hideSoftInput(postContent!!)
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
        if (targetId == null && newPost) {
            Toast.makeText(caller.context, R.string.please_select_target_forum, Toast.LENGTH_SHORT)
                .show()
            return
        }
        name = findViewById<TextView>(R.id.formName).text.toString()
        email = findViewById<TextView>(R.id.formEmail).text.toString()
        title = findViewById<TextView>(R.id.formTitle).text.toString()
        content = postContent!!.text.toString()
        if (content.isBlank() && imageFile == null) {
            Toast.makeText(caller.context, R.string.need_content_to_post, Toast.LENGTH_SHORT).show()
            return
        }

        userHash = selectedCookie?.cookieHash ?: ""

        postProgress.show()
        Timber.i("Sending...")
        caller.lifecycleScope.launch {
            webServiceClient.sendPost(
                newPost,
                targetId!!,
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

                postProgress.dismiss()
                dismissWith {
                    if (message.substring(0, 2) == ":)") {
                        clearEntries()
                        afterPostTask?.invoke()
                    }
                }
                Toast.makeText(caller.context, message, Toast.LENGTH_LONG).show()
            }
        }
    }
}