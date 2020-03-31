package com.laotoua.dawnislandk.util

import android.os.Bundle
import androidx.fragment.app.FragmentManager
import com.laotoua.dawnislandk.ImageViewerFragment
import com.laotoua.dawnislandk.ReplyFragment
import com.laotoua.dawnislandk.ThreadFragment
import timber.log.Timber

/** TODO: when fragment is pop from stack, it is destroyed.
 *  If want to retain the fragment, with its state, there needs to be a new solution
 */
fun GoToFragment(fm: FragmentManager, tag: String, frameId: Int, bundle: Bundle? = null) {
    Timber.i("Going to Fragment $tag")
    val transaction = fm.beginTransaction()
    var dest = fm.findFragmentByTag(tag)
    if (dest == null) {
        Timber.i("Fragment $tag not found. Making a new one")
        dest = when (tag) {
            "Thread" -> ThreadFragment()
            "Reply" -> ReplyFragment()
            "ImageViewer" -> ImageViewerFragment()
            else -> {
                Timber.e("Unhandled fragment construction")
                throw  Exception("Unhandled fragment construction")
            }
        }
        transaction.add(dest, tag)
    }

    dest.arguments = bundle
    transaction.replace(frameId, dest, tag)
        .addToBackStack(null)
        .commit()
}
