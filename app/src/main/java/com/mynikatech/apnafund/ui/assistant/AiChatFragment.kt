package com.mynikatech.apnafund.ui.assistant

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.appbar.MaterialToolbar
import com.mynikatech.apnafund.R
import com.mynikatech.apnafund.databinding.FragmentAiChatBinding
import com.mynikatech.apnafund.ui.viewmodel.AiChatViewModel

class AiChatFragment : Fragment() {

    private lateinit var binding: FragmentAiChatBinding
    private val adapter = AIChatAdapter()
    private val viewModel: AiChatViewModel by viewModels()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentAiChatBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {


        val toolbar = view.findViewById<MaterialToolbar>(R.id.ai_assitant_toolbar)
        val navController = findNavController()

        toolbar.setNavigationIcon(R.drawable.ic_back_arrow)
        toolbar.setNavigationOnClickListener { navController.navigateUp() }
        toolbar.title = "Ask Maya"

        binding.recyclerChat.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = this@AiChatFragment.adapter
        }

        viewModel.messages.observe(viewLifecycleOwner) {
            adapter.submitList(it)
            if (it.isNotEmpty()) {
                binding.recyclerChat.scrollToPosition(it.size - 1)
            }
        }

        binding.buttonSend.setOnClickListener {
            val message = binding.editMessage.text.toString().trim()

            if (message.isEmpty()) return@setOnClickListener

            viewModel.sendMessage(message)
            binding.editMessage.text?.clear()
        }
    }
}