package com.mynikatech.apnafund.ui.admin.feedback

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.mynikatech.apnafund.R
import com.mynikatech.apnafund.databinding.FragmentAdminFeedbackBinding
import com.mynikatech.apnafund.ui.viewmodel.AdminViewModel
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class AdminFeedbackFragment : Fragment() {

    private val adminViewModel: AdminViewModel by viewModels()
    private lateinit var binding: FragmentAdminFeedbackBinding

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentAdminFeedbackBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        val adapter = FeedbackAdapter()
        binding.recyclerViewFeedback.adapter = adapter
        binding.recyclerViewFeedback.layoutManager = LinearLayoutManager(requireContext())

        lifecycleScope.launch {
            try {
                adminViewModel.feedbackWithUserGroup.collectLatest { feedbackList ->
                    Log.d("AdminFeedback", "Collected ${feedbackList.size} items")

                    adapter.submitList(feedbackList)

                    binding.textEmptyFeedback.visibility =
                        if (feedbackList.isEmpty()) View.VISIBLE else View.GONE
                }
            } catch (e: Exception) {
                Log.e("AdminFeedback", "Error collecting feedback", e)

                // Optional: show error UI
                binding.textEmptyFeedback.visibility = View.VISIBLE
                binding.textEmptyFeedback.text = getString(R.string.text_empty_feedback_error)
            }
        }
    }
}