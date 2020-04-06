package com.laotoua.dawnislandk

import android.os.Bundle
import android.view.*
import android.widget.Toast
import androidx.appcompat.widget.Toolbar
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import androidx.lifecycle.Observer
import androidx.navigation.fragment.navArgs
import com.github.chrisbanes.photoview.PhotoView
import com.laotoua.dawnislandk.databinding.ImageViewerFragmentBinding
import com.laotoua.dawnislandk.viewmodels.ImageViewerViewModel
import com.laotoua.dawnislandk.viewmodels.SharedViewModel
import timber.log.Timber


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

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setHasOptionsMenu(true)

    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        inflater.inflate(R.menu.menu_image, menu)
        super.onCreateOptionsMenu(menu, inflater)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {

        sharedVM.setFragment(this.javaClass.simpleName)

        _binding = ImageViewerFragmentBinding.inflate(inflater, container, false)
        val imgUrl: String = args.imgUrl

        // load image in Full Screen
        val photoView: PhotoView = binding.photoView
        viewModel.loadImage(this, photoView, imgUrl)

        viewModel.status.observe(viewLifecycleOwner, Observer {
            when (it) {
                true -> Toast.makeText(context, "Image saved in Pictures/Dawn", Toast.LENGTH_SHORT)
                    .show()
                else -> Toast.makeText(context, "Error in saving image", Toast.LENGTH_SHORT).show()
            }
        })
        return binding.root
    }


    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null

        }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {

        // Handle item selection
        return when (item.itemId) {
            R.id.save_image -> {
                viewModel.addPicToGallery(requireParentFragment(), args.imgUrl)
                true
            }

            else -> {
                Timber.e("Unhandled item click")
                true
            }
        }
    }


}
