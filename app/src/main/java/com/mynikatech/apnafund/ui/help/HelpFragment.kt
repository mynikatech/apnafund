package com.mynikatech.apnafund.ui.help

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.widget.addTextChangedListener
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.mynikatech.apnafund.databinding.FragmentHelpBinding

class HelpFragment : Fragment() {

    private var _binding: FragmentHelpBinding? = null
    private val binding get() = _binding!!

    private lateinit var adapter: HelpSectionAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentHelpBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {

        val sections = HelpRepository.getSections()

        adapter = HelpSectionAdapter(
            context = requireContext(),
            sections = sections,
            onItemClick = { item ->
                HelpDetailBottomSheet.newInstance(item)
                    .show(parentFragmentManager, "help_detail")
            }
        )

        binding.recyclerHelp.layoutManager = LinearLayoutManager(requireContext())
        binding.recyclerHelp.adapter = adapter

        // 🔍 Search (works across all sections)
        binding.editSearch.addTextChangedListener { text ->
            val query = text?.toString().orEmpty()
            adapter.filter(query)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}