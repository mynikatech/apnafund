package com.mynikatech.apnafund.ui.fund

import android.content.Context
import android.view.LayoutInflater
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.mynikatech.apnafund.databinding.BottomSheetExportOptionsBinding

class ExportOptionsBottomSheet(
    private val context: Context,
    private val listener: Listener
) {

    interface Listener {
        fun onShareImage()
        fun onSaveToGallery()
        fun onSaveAsPdf()
        fun onPrint()
    }

    fun show() {

        val dialog = BottomSheetDialog(context)

        val binding = BottomSheetExportOptionsBinding.inflate(
            LayoutInflater.from(context)
        )

        dialog.setContentView(binding.root)

        binding.layoutShareImage.setOnClickListener {
            dialog.dismiss()
            listener.onShareImage()
        }

        binding.layoutSaveGallery.setOnClickListener {
            dialog.dismiss()
            listener.onSaveToGallery()
        }

        binding.layoutSavePdf.setOnClickListener {
            dialog.dismiss()
            listener.onSaveAsPdf()
        }

        binding.layoutPrint.setOnClickListener {
            dialog.dismiss()
            listener.onPrint()
        }

        dialog.show()
    }
}