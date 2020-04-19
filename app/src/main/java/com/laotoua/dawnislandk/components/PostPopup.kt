package com.laotoua.dawnislandk.components

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.net.Uri
import android.os.Environment
import android.view.View
import android.view.inputmethod.InputMethodManager
import android.widget.*
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.github.chrisbanes.photoview.PhotoView
import com.google.android.flexbox.FlexboxLayout
import com.google.android.material.button.MaterialButtonToggleGroup
import com.laotoua.dawnislandk.R
import com.laotoua.dawnislandk.entities.Cookie
import com.laotoua.dawnislandk.network.NMBServiceClient
import com.laotoua.dawnislandk.util.AppState
import com.laotoua.dawnislandk.util.FragmentIntentUtil
import com.laotoua.dawnislandk.util.ImageUtil
import com.lxj.xpopup.XPopup
import com.lxj.xpopup.core.BottomPopupView
import com.lxj.xpopup.interfaces.SimpleCallback
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

    private var expansionContainer: LinearLayout? = null
    private var attachmentContainer: ConstraintLayout? = null
    private var facesContainer: FlexboxLayout? = null
    private var postContent: EditText? = null
    private var postPhotoPreview: PhotoView? = null

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

    override fun onCreate() {
        super.onCreate()

        findViewById<LinearLayout>(R.id.toggleContainers).run {
            expansionContainer = findViewById(R.id.expansionContainer)

            attachmentContainer = findViewById<ConstraintLayout>(R.id.attachmentContainer).also {
                postPhotoPreview = findViewById(R.id.postPhotoPreview)
            }

            // TODO: faces do not display properly in button
            // add faces
            facesContainer = findViewById<FlexboxLayout>(R.id.facesContainer).also { flexBox ->
                resources.getStringArray(R.array.NMBFaces).map {
                    Button(context).run {
                        text = it
                        setOnClickListener {
                            postContent!!.append(text)
                        }
                        flexBox.addView(this)
                    }
                }
            }

        }

        postContent = findViewById<EditText>(R.id.postContent)

        findViewById<Button>(R.id.postForum).run {
            setOnClickListener {
                XPopup.Builder(context)
                    .atView(it) // 依附于所点击的View，内部会自动判断在上方或者下方显示
                    .asAttachList(
                        forumNameMap!!.values.drop(1).toTypedArray(),//去除时间线
                        intArrayOf()
                    ) { ind, forumName ->
                        targetId = forumNameMap!!.keys.drop(1).toList()[ind]
                        this.text = forumName
                    }
                    .show()
            }
        }

        findViewById<Button>(R.id.postSend).setOnClickListener {
            Timber.i("Sending...")
            send()
        }

        findViewById<MaterialButtonToggleGroup>(R.id.toggleButton)
            .addOnButtonCheckedListener { _, checkedId, isChecked ->
                when (checkedId) {
                    R.id.postExpand -> {
                        expansionContainer!!.visibility = if (isChecked) View.VISIBLE else View.GONE
                    }

                    R.id.postFace -> {
                        hideKeyboardFrom(context, this)
                        facesContainer!!.visibility = if (isChecked) View.VISIBLE else View.GONE
                    }
                    R.id.postAttachment -> {
                        hideKeyboardFrom(context, this)
                        attachmentContainer!!.visibility =
                            if (isChecked) View.VISIBLE else View.GONE
                    }
                    // TODO: luweiniang
                    R.id.postLuwei -> {
                        hideKeyboardFrom(context, this)
                        Toast.makeText(context, "postLuwei TODO....", Toast.LENGTH_LONG).show()
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
                                postPhotoPreview!!
                            )
                        } catch (e: Exception) {
                            Timber.e(e, "Cannot load thumbnail from image...")
                        }
                    }
                }
            }
        }

        findViewById<Button>(R.id.postImageRemove).setOnClickListener {
            imageFile = null
            postPhotoPreview!!.setImageResource(android.R.color.transparent)
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
                                    postPhotoPreview!!
                                )
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
    }

    // TODO: post new thread
    private fun send() {
        if (selectedCookie == null) {
            Toast.makeText(caller.context, "没有饼干不能发串哦。。", Toast.LENGTH_SHORT).show()
            return
        }
        name = findViewById<TextView>(R.id.formName).text.toString()
        email = findViewById<TextView>(R.id.formEmail).text.toString()
        title = findViewById<TextView>(R.id.formTitle).text.toString()
        content = postContent!!.text.toString()

        hash = selectedCookie?.cookieHash ?: ""

        // TODO: test
        targetId = "17735544"
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
                dismiss()
                Toast.makeText(caller.context, this, Toast.LENGTH_LONG).show()
            }

        }

    }


    private fun hideKeyboardFrom(
        context: Context,
        view: View
    ) {
        (context.getSystemService(Activity.INPUT_METHOD_SERVICE) as InputMethodManager)
            .hideSoftInputFromWindow(view.windowToken, 0)
    }
}