package com.laotoua.dawnislandk.ui.fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.findNavController
import com.laotoua.dawnislandk.databinding.SubscriptionFragmentBinding
import com.laotoua.dawnislandk.viewmodel.SubscriptionViewModel
import timber.log.Timber


class SubscriptionFragment : Fragment() {

    private var _binding: SubscriptionFragmentBinding? = null
    private val binding: SubscriptionFragmentBinding get() = _binding!!

    private lateinit var viewModel: SubscriptionViewModel

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = SubscriptionFragmentBinding.inflate(inflater, container, false)

        binding.textView.setOnClickListener {
            Timber.i("CLicked!!!!")
            val action =
                PagerFragmentDirections.actionPagerFragmentToImageViewerFragment(
                    "https://elitemailorderbrides.com/wp-content/uploads/2019/12/asian4.jpg"
                )
            findNavController().navigate(action)
        }
        return binding.root
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // TODO: Use the ViewModel
        viewModel = ViewModelProvider(this).get(SubscriptionViewModel::class.java)
    }

}
