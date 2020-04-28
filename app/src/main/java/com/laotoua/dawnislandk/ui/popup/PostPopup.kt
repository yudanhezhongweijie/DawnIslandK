package com.laotoua.dawnislandk.ui.popup

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.net.Uri
import android.os.Environment
import android.util.TypedValue
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.InputMethodManager
import android.widget.*
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.afollestad.materialdialogs.MaterialDialog
import com.afollestad.materialdialogs.list.listItemsSingleChoice
import com.google.android.flexbox.FlexboxLayout
import com.google.android.material.button.MaterialButton
import com.google.android.material.button.MaterialButtonToggleGroup
import com.laotoua.dawnislandk.R
import com.laotoua.dawnislandk.data.entity.Cookie
import com.laotoua.dawnislandk.data.network.APISuccessMessageResponse
import com.laotoua.dawnislandk.data.network.MessageType
import com.laotoua.dawnislandk.data.network.NMBServiceClient
import com.laotoua.dawnislandk.data.state.AppState
import com.laotoua.dawnislandk.io.FragmentIntentUtil
import com.laotoua.dawnislandk.io.ImageUtil
import com.lxj.xpopup.XPopup
import com.lxj.xpopup.core.BottomPopupView
import com.lxj.xpopup.interfaces.SimpleCallback
import com.lxj.xpopup.util.KeyboardUtils
import kotlinx.coroutines.launch
import timber.log.Timber
import java.io.File
import java.text.SimpleDateFormat
import java.util.*


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
    var hash = ""

    // TODO: temp solution for TakePicture error
    private var previewUri: Uri? = null

    private var cookies = listOf<Cookie>()

    private var selectedCookie: Cookie? = null

    private var toggleContainers: ConstraintLayout? = null
    private var expansionContainer: LinearLayout? = null
    private var attachmentContainer: ConstraintLayout? = null
    private var facesContainer: ScrollView? = null
    private var luweiContainer: ScrollView? = null
    private var postContent: EditText? = null
    private var postImagePreview: ImageView? = null

    // keyboard height listener
    private var keyboardHeight = -1
    private var keyboardHolder: LinearLayout? = null

    private val postProgress by lazy {
        XPopup.Builder(getContext())
            .asLoading("正在发送中")
    }

    // default button background
    private val btnBackground by lazy {
        TypedValue().apply {
            context.theme
                .resolveAttribute(android.R.attr.selectableItemBackground, this, true)
        }.resourceId
    }

    private var progressBar: ProgressBar? = null

    private fun updateTitle(targetId: String, newPost: Boolean) {
        findViewById<TextView>(R.id.postTitle).text = if (newPost) "发布新串" else "回复 >> No. $targetId"
    }

    private fun updateForumButton() {
        findViewById<Button>(R.id.postForum).visibility = if (!newPost) View.GONE else View.VISIBLE
    }

    private fun updateCookies() {
        cookies = AppState.cookies!!
        if (selectedCookie == null || cookies.isNullOrEmpty()) {
            findViewById<TextView>(R.id.postCookie)?.run {
                text = if (cookies.isNullOrEmpty()) {
                    "没有饼干"
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
        return R.layout.post_popup
    }

    override fun show(): BottomPopupView {
        if (parent != null) return this
        val activity = context as Activity
        popupInfo.decorView = activity.window.decorView as ViewGroup
        KeyboardUtils.registerSoftInputChangedListener(
            activity,
            this
        ) { height ->
            if (keyboardHeight < 0) {
                keyboardHeight = height
                listOf(facesContainer!!, luweiContainer!!).map {
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
        postContent = findViewById(R.id.postContent)

        toggleContainers = findViewById<ConstraintLayout>(R.id.toggleContainers).also {
            expansionContainer = findViewById(R.id.expansionContainer)

            // add faces
            facesContainer = findViewById(R.id.facesContainer)

            // add luwei
            luweiContainer = findViewById(R.id.luweiContainer)

            progressBar = findViewById(R.id.progressBar)

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
                }
            }
        }

        findViewById<Button>(R.id.postSend).setOnClickListener {
            Timber.i("Sending...")
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
                        facesContainer!!.visibility = if (isChecked) View.VISIBLE else View.GONE
                        (facesContainer!!.getChildAt(0) as FlexboxLayout).let { flexBox ->
                            if (flexBox.childCount == 0) {
                                progressBar!!.visibility = View.VISIBLE
                                caller.lifecycleScope.launch {
                                    addFacesButtons(flexBox)
                                    progressBar!!.visibility = View.GONE
                                }
                            }
                        }
                    }
                    R.id.postAttachment -> {
                        hideKeyboardFrom(context, this)
                        attachmentContainer!!.visibility =
                            if (isChecked) View.VISIBLE else View.GONE
                    }

                    R.id.postLuwei -> {
                        hideKeyboardFrom(context, this)
                        luweiContainer!!.visibility =
                            if (isChecked) View.VISIBLE else View.GONE
                        (luweiContainer!!.getChildAt(0) as FlexboxLayout).let { flexBox ->
                            if (flexBox.childCount == 0) {
                                progressBar!!.visibility = View.VISIBLE
                                caller.lifecycleScope.launch {
                                    addLuweiButtons(flexBox)
                                    progressBar!!.visibility = View.GONE
                                }
                            }
                        }
                    }
                    // TODO: doodle
                    R.id.postDoodle -> {
                        Toast.makeText(context, "postDoodle TODO....", Toast.LENGTH_LONG).show()
                    }
                    // TODO: save
                    R.id.postSave -> {
                        Toast.makeText(context, "postSave TODO....", Toast.LENGTH_LONG).show()
                    }
                    else -> {
                        Timber.e("Unhandled selector in post popup")
                    }

                }
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
                Toast.makeText(caller.context, "没有饼干", Toast.LENGTH_SHORT).show()
            }
        }


        findViewById<Button>(R.id.postImage).setOnClickListener {
            if (FragmentIntentUtil.checkPermissions(caller)) {
                FragmentIntentUtil.getImageFromGallery(caller, "image/*") { uri: Uri? ->
                    if (uri != null) {
                        imageFile = ImageUtil.getImagePathFromUri(caller, uri)
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
        }

        findViewById<Button>(R.id.postImageDelete).setOnClickListener {
            imageFile = null
            postImagePreview!!.setImageResource(android.R.color.transparent)
            attachmentContainer!!.visibility = View.GONE
        }

        // TODO: camera
        findViewById<Button>(R.id.postCamera).setOnClickListener {
            if (!caller.requireActivity().packageManager.hasSystemFeature(PackageManager.FEATURE_CAMERA_ANY)) {
                Toast.makeText(context, "你没有相机？？？", Toast.LENGTH_LONG).show()
                return@setOnClickListener
            }
            if (FragmentIntentUtil.checkPermissions(caller)) {
                val timeStamp: String = SimpleDateFormat("yyyyMMdd_HHmmss").format(Date())
                val relativeLocation =
                    Environment.DIRECTORY_PICTURES + File.separator + "Dawn"
                val name = "DawnIsland_$timeStamp"
                val ext = "jpg"
                try {
                    ImageUtil.addPlaceholderImageUriToGallery(caller, name, ext, relativeLocation)
                        ?.run {
                            // TODO: previewUri is temporary because intent return null thumbnail
                            previewUri = this
                            FragmentIntentUtil.getImageFromCamera(caller, this)
                            { bitmap: Bitmap? ->
//                            bitmap?.run {
//                                postPhotoPreview!!.setImageBitmap(this)
//                            }
//                            if (bitmap == null) {
//                                Timber.i("didn't get thumbnail")
//                            }
                                ImageUtil.loadImageThumbnailToImageView(
                                    caller,
                                    previewUri!!,
                                    150,
                                    150,
                                    postImagePreview!!
                                )
                                attachmentContainer!!.visibility = View.VISIBLE
                            }
                        }
                } catch (e: Exception) {
                    Timber.e(e, "Failed to take a picture...")
                }
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
        /**
         * 取消缩放功能
         */

//        constraintLayout = findViewById(R.id.baseContainer)
//        findViewById<Button>(R.id.postFullScreen).setOnClickListener {
//            TransitionManager.beginDelayedTransition(constraintLayout!!)
//            fullScreen = if (fullScreen) {
//                val constraintSet = ConstraintSet()
//                constraintSet.clone(constraintLayout)
//                constraintSet.clear(R.id.dialogContainer, ConstraintSet.TOP)
//                constraintSet.connect(
//                    R.id.dialogContainer,
//                    ConstraintSet.TOP,
//                    R.id.guideline_half,
//                    ConstraintSet.TOP,
//                    0
//                )
//                constraintSet.applyTo(constraintLayout)
//                false
//            } else {
//                val constraintSet = ConstraintSet()
//                constraintSet.clone(constraintLayout)
//                constraintSet.clear(R.id.dialogContainer, ConstraintSet.TOP)
//                constraintSet.connect(
//                    R.id.dialogContainer,
//                    ConstraintSet.TOP,
//                    R.id.guideline_full,
//                    ConstraintSet.TOP,
//                    0
//                )
//                constraintSet.applyTo(constraintLayout)
//                true
//            }
//        }
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
            Toast.makeText(caller.context, "没有饼干不能发串哦。。", Toast.LENGTH_SHORT).show()
            return
        }
        postProgress.show()
        name = findViewById<TextView>(R.id.formName).text.toString()
        email = findViewById<TextView>(R.id.formEmail).text.toString()
        title = findViewById<TextView>(R.id.formTitle).text.toString()
        content = postContent!!.text.toString()

        hash = selectedCookie?.cookieHash ?: ""

        // TODO: loading...
        // TODO: 值班室需要举报理由才能发送
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
                hash
            ).run {
                // TODO, need to confirm success
//                clearEntries()

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
                        Timber.e("Response type: ${this.javaClass.simpleName}")
                        Timber.e(message)
                        message
                    }
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

    private fun addFacesButtons(flexBox: FlexboxLayout) {
        resources.getStringArray(R.array.NMBFaces).map {
            MaterialButton(
                context,
                null,
                R.style.Widget_MaterialComponents_Button_TextButton
            ).run {
                // clear color filled background
                setBackgroundResource(btnBackground)
                textAlignment = View.TEXT_ALIGNMENT_CENTER
                text = it
                setOnClickListener {
                    postContent!!.append(text)
                }
                flexBox.addView(this)
                val lp = layoutParams as FlexboxLayout.LayoutParams
                lp.flexBasisPercent = 0.2f
                lp.setMargins(0, 2, 0, 2)
                layoutParams = lp
            }
        }
    }

    private fun addLuweiButtons(flexBox: FlexboxLayout) {
        resources.getStringArray(R.array.LuweiEmojis).map { emojiId ->
            val resourceId: Int = context.resources.getIdentifier(
                emojiId, "drawable",
                context.packageName
            )
            ImageView(context).run {
                setImageResource(resourceId)
                setBackgroundResource(btnBackground)
                layoutParams = LayoutParams(150, 150)
                setOnClickListener {
                    try {
                        imageFile =
                            ImageUtil.getFileFromDrawable(caller, emojiId, resourceId)
                        postImagePreview!!.setImageResource(resourceId)
                        attachmentContainer!!.visibility = View.VISIBLE
                    } catch (e: Exception) {
                        Timber.e(e)
                    }
                }
                flexBox.addView(this)
                val lp = layoutParams as FlexboxLayout.LayoutParams
                lp.flexBasisPercent = 0.2f
                lp.setMargins(0, 2, 0, 2)
                layoutParams = lp
            }
        }
    }
}