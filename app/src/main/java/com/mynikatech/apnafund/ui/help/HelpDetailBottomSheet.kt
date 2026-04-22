package com.mynikatech.apnafund.ui.help

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.text.method.LinkMovementMethod
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.text.HtmlCompat
import androidx.navigation.findNavController
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.mynikatech.apnafund.R
import com.mynikatech.apnafund.databinding.BottomSheetHelpDetailBinding

class HelpDetailBottomSheet : BottomSheetDialogFragment() {

    private var _binding: BottomSheetHelpDetailBinding? = null
    private val binding get() = _binding!!

    private lateinit var item: HelpItem

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        item = requireArguments().getParcelable("item")!!
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = BottomSheetHelpDetailBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {

        binding.textTitle.text = getString(item.titleResId)

        binding.textDescription.text = HtmlCompat.fromHtml(
            getString(item.descriptionResId),
            HtmlCompat.FROM_HTML_MODE_LEGACY
        )
        binding.textDescription.movementMethod = LinkMovementMethod.getInstance()

        // 🎥 Video
        if (item.videoUrl != null) {
            binding.btnVideo.visibility = View.VISIBLE
            binding.btnVideo.setOnClickListener {
                startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(item.videoUrl)))
            }
        }

        // 🔗 Deep link
        if (item.deepLink != null) {
            binding.btnOpen.visibility = View.VISIBLE
            binding.btnOpen.setOnClickListener {
                dismiss()
                requireActivity().findNavController(R.id.nav_host_fragment)
                    .navigate(item.deepLink!!)
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        fun newInstance(item: HelpItem): HelpDetailBottomSheet {
            val sheet = HelpDetailBottomSheet()
            val args = Bundle()
            args.putParcelable("item", item)
            sheet.arguments = args
            return sheet
        }
    }
}