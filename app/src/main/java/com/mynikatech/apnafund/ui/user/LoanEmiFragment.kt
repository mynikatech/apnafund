package com.mynikatech.apnafund.ui.user

import android.os.Build
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.appbar.MaterialToolbar
import com.mynikatech.apnafund.R
import com.mynikatech.apnafund.databinding.FragmentLoanEmiBinding
import com.mynikatech.apnafund.ui.viewmodel.LoansViewModel
import kotlinx.coroutines.launch


class LoanEmiFragment : Fragment() {

    private lateinit var binding: FragmentLoanEmiBinding

    private val loanViewModel : LoansViewModel by viewModels()

    private val args: LoanEmiFragmentArgs by navArgs()

    private lateinit var adapter: LoanEmiAdapter


    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentLoanEmiBinding.inflate(inflater, container, false)
        return binding.root
    }

    @Suppress("DEPRECATION")
    @RequiresApi(Build.VERSION_CODES.O)
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        (requireActivity() as AppCompatActivity).supportActionBar?.apply {
            setDisplayHomeAsUpEnabled(true)
            setDisplayShowHomeEnabled(true)
            title = "Loan EMIs"
        }
        setHasOptionsMenu(true)
        val loanId = args.loanId
        val recyclerView = binding.recyclerLoanEmis
        recyclerView.layoutManager = LinearLayoutManager(requireContext())
        viewLifecycleOwner.lifecycleScope.launch {
            val loanEmis = loanViewModel.getAllLoanEmisForLoan(loanId)
            if(loanEmis  != null) {
                adapter = LoanEmiAdapter(loanEmis)
                binding.recyclerLoanEmis.adapter = adapter
            }
        }
        val toolbar = view.findViewById<MaterialToolbar>(R.id.user_loan_emi_toolbar)
        (activity as? AppCompatActivity)?.setSupportActionBar(toolbar)
        (activity as? AppCompatActivity)?.supportActionBar?.setDisplayShowTitleEnabled(false)

        // Enable back arrow
        val navController = findNavController()
        toolbar.setNavigationIcon(R.drawable.ic_back_arrow) // your back icon
        toolbar.setNavigationOnClickListener {
            navController.navigateUp()
        }
        toolbar.title = "Loan EMIs"
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        menu.clear()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        val activity = requireActivity() as AppCompatActivity
        activity.supportActionBar?.show()
    }

    @Suppress("DEPRECATION")
    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            android.R.id.home -> {
                requireActivity().onBackPressedDispatcher.onBackPressed()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }



}