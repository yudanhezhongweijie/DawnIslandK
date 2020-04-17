package com.laotoua.dawnislandk.components

import android.Manifest
import android.annotation.SuppressLint
import android.content.ContentResolver
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Color
import android.net.Uri
import android.provider.OpenableColumns
import android.view.View
import android.widget.*
import androidx.activity.invoke
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.app.ActivityCompat.requestPermissions
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.google.android.flexbox.FlexboxLayout
import com.laotoua.dawnislandk.R
import com.laotoua.dawnislandk.entities.Cookie
import com.laotoua.dawnislandk.network.NMBServiceClient
import com.laotoua.dawnislandk.util.AppState
import com.lxj.xpopup.XPopup
import com.lxj.xpopup.core.BottomPopupView
import com.lxj.xpopup.interfaces.SimpleCallback
import com.lxj.xpopup.util.XPopupUtils
import kotlinx.coroutines.launch
import timber.log.Timber
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream


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

    // TODO
    var water = false
    var imageFile: File? = null
    var hash = ""

    // requestCode for permissions callback
    private val requestCode = 1

    private val getImage = caller.prepareCall(ActivityResultContracts.GetContent())
    { uri: Uri? -> if (uri != null) imageFile = getImagePathFromUri(uri) }

    private var cookies = listOf<Cookie>()

    private var selectedCookie: Cookie? = null

    private var expansionContainer: LinearLayout? = null
    private var attachmentContainer: FlexboxLayout? = null
    private var facesContainer: FlexboxLayout? = null
    private var postContent: EditText? = null

    private fun updateTitle(targetId: String, newPost: Boolean) {
        findViewById<TextView>(R.id.postTitle).text = if (newPost) "发布新串" else "回复 >No. $targetId"
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

    private fun updateSelector(buttonId: Int) {
        when (buttonId) {
            R.id.postExpand -> {
                expansionContainer!!.visibility = View.VISIBLE
                facesContainer!!.visibility = View.GONE
                attachmentContainer!!.visibility = View.GONE
            }

            R.id.postFace -> {
                expansionContainer!!.visibility = View.GONE
                facesContainer!!.visibility = View.VISIBLE
                attachmentContainer!!.visibility = View.GONE
            }
            R.id.postAttachment -> {
                expansionContainer!!.visibility = View.GONE
                facesContainer!!.visibility = View.GONE
                attachmentContainer!!.visibility = View.VISIBLE
            }
            else -> {
                Timber.e("Unhandled selector in post popup")
            }

        }
    }

    override fun getImplLayoutId(): Int {
        return R.layout.post_popup
    }

    override fun getMaxHeight(): Int {
        return (XPopupUtils.getWindowHeight(context) * .7f).toInt()
    }


    override fun onCreate() {
        super.onCreate()

        expansionContainer = findViewById(R.id.expansionContainer)
        // TODO: use
        attachmentContainer = findViewById(R.id.attachmentContainer)

        // add faces
        findViewById<FlexboxLayout>(R.id.facesContainer).let { ll ->
            facesContainer = ll
            resources.getStringArray(R.array.NMBFaces).map {
                Button(context).run {
                    text = it
                    setBackgroundColor(Color.TRANSPARENT)
                    setOnClickListener {
                        postContent!!.append(text)
                    }
                    ll.addView(this)
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

        findViewById<Button>(R.id.postAttachment).setOnClickListener {
            updateSelector(it.id)
            // TODO
            Timber.i("Attaching image...")
            checkStoragePermissions(context)

            getImage("image/*")
        }

        findViewById<Button>(R.id.postFace).setOnClickListener {
            updateSelector(it.id)
        }

        findViewById<Button>(R.id.postExpand).setOnClickListener {
            updateSelector(it.id)

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
        // TODO: if (water): add body
        // TODO: loading...
        // TODO: 值班室需要举报理由才能发送
        caller.lifecycleScope.launch {
            if (newPost) {
                NMBServiceClient.postThread(
                    targetId,
                    name,
                    email,
                    title,
                    content,
                    null,
                    imageFile,
                    hash
                ).run {
                    dismiss()
                    Toast.makeText(caller.context, this, Toast.LENGTH_LONG).show()
                }
            } else {
                NMBServiceClient.postReply(
                    targetId,
                    name,
                    email,
                    title,
                    content,
                    null,
                    imageFile,
                    hash
                ).run {
                    dismiss()
                    Toast.makeText(caller.context, this, Toast.LENGTH_LONG).show()
                }
            }
        }

    }


    private fun checkStoragePermissions(context: Context) {
        if (ContextCompat.checkSelfPermission(
                context, Manifest.permission.READ_EXTERNAL_STORAGE
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            requestPermissions(
                caller.requireActivity(),
                arrayOf(
                    Manifest.permission.WRITE_EXTERNAL_STORAGE,
                    Manifest.permission.READ_EXTERNAL_STORAGE
                ),
                requestCode
            )
        }
    }

    private fun getImagePathFromUri(uri: Uri): File? {
        val parcelFileDescriptor = context.contentResolver.openFileDescriptor(uri, "r", null)

        parcelFileDescriptor?.let {
            val inputStream = FileInputStream(parcelFileDescriptor.fileDescriptor)
            val file = File(context.cacheDir, context.contentResolver.getFileName(uri))
            val outputStream = FileOutputStream(file)
            inputStream.copyTo(outputStream)
            return file
        }
        return null
    }

    private fun ContentResolver.getFileName(fileUri: Uri): String {
        var name = ""
        val returnCursor = this.query(fileUri, null, null, null, null)
        if (returnCursor != null) {
            val nameIndex = returnCursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
            returnCursor.moveToFirst()
            name = returnCursor.getString(nameIndex)
            returnCursor.close()
        }
        return name
    }

}