package com.laotoua.dawnislandk

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.appcompat.widget.Toolbar
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import androidx.lifecycle.Observer
import androidx.navigation.fragment.navArgs
import com.github.chrisbanes.photoview.PhotoView
import com.google.android.material.appbar.AppBarLayout
import com.laotoua.dawnislandk.databinding.ImageViewerFragmentBinding
import com.laotoua.dawnislandk.viewmodels.ImageViewerViewModel
import com.laotoua.dawnislandk.viewmodels.SharedViewModel


class ImageViewerFragment : Fragment() {
    var toolbar: Toolbar? = null
    private var _binding: ImageViewerFragmentBinding? = null
    val binding: ImageViewerFragmentBinding get() = _binding!!
    private val sharedVM: SharedViewModel by activityViewModels()
    private val viewModel: ImageViewerViewModel by viewModels()
    private val args: ImageViewerFragmentArgs by navArgs()


    companion object {
        fun newInstance() = ImageViewerFragment()
    }


    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {

        activity?.findViewById<AppBarLayout>(R.id.dawn_appbar)?.let {
            it.setExpanded(false)
//            visibility = View.GONE
        }


        _binding = ImageViewerFragmentBinding.inflate(inflater, container, false)
        val imgUrl: String = args.imgUrl
//       TODO save in tool bar
//        setupToolbar(imgUrl)

        // load image in Full Screen
        val photoView: PhotoView = binding.photoView
        viewModel.loadImage(this, photoView, imgUrl)

        viewModel.status.observe(viewLifecycleOwner, Observer {
            when (it) {
                true -> Toast.makeText(context, "Image saved in Pictures/Dawn", Toast.LENGTH_SHORT)
                    .show()
//                else -> //Toast.makeText(context, "Error in saving image", Toast.LENGTH_SHORT).show()
            }
        })
        return binding.root
    }


    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null

        activity?.findViewById<AppBarLayout>(R.id.dawn_appbar)?.let {
            it.visibility = View.VISIBLE
        }
    }

//    fun onSupportNavigateUp(): Boolean {
//        onBackPressed()
//        return true
//    }
//
//    // TODO: enable image collections(within same thread) view
//    private fun setupToolbar(imgUrl: String) {
//        toolbar = findViewById<Toolbar>(R.id.content_toolbar)
//        setSupportActionBar(toolbar)
//        getSupportActionBar().setDisplayHomeAsUpEnabled(true)
//        val saveButton: ImageButton = findViewById<ImageButton>(R.id.save_button)
//        saveButton.setOnClickListener { viewModel.addPicToGallery(imgUrl) }
//    }


}
