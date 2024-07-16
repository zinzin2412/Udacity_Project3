package com.udacity

import android.content.res.ColorStateList
import android.os.Bundle
import androidx.annotation.ColorRes
import androidx.annotation.DrawableRes
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.os.bundleOf
import com.udacity.databinding.ActivityDetailBinding
import com.udacity.databinding.ContentDetailBinding
import com.udacity.download.DownloadStatus

class DetailActivity : AppCompatActivity() {

    private lateinit var binding: ActivityDetailBinding

    private val fileName: String by lazy {
        intent?.extras?.getString(EXTRA_FILE_NAME, unknownText) ?: unknownText
    }
    private val downloadStatus: String by lazy {
        intent?.extras?.getString(EXTRA_DOWNLOAD_STATUS, unknownText) ?: unknownText
    }

    private val unknownText: String by lazy { getString(R.string.unknown) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityDetailBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setSupportActionBar(binding.toolbar)
        setupView()
        setupActions()
    }

    private fun setupView() = with(binding.detailContent) {
        fileNameText.text = fileName
        downloadStatusText.text = downloadStatus
        updateDownloadStatus(downloadStatus)
    }

    private fun updateDownloadStatus(status: String) {
        when (status) {
            DownloadStatus.SUCCESSFUL.statusText -> {
                changeDownloadStatusImageTo(R.drawable.ic_check_circle_outline_24)
                changeDownloadStatusColorTo(R.color.colorPrimaryDark)
            }
            DownloadStatus.FAILED.statusText -> {
                changeDownloadStatusImageTo(R.drawable.ic_error_24)
                changeDownloadStatusColorTo(R.color.design_default_color_error)
            }
        }
    }

    private fun changeDownloadStatusImageTo(@DrawableRes imageRes: Int) = with(binding.detailContent){
        downloadStatusImage.setImageResource(imageRes)
    }

    private fun changeDownloadStatusColorTo(@ColorRes colorRes: Int) = with(binding.detailContent){
        val color = ContextCompat.getColor(this@DetailActivity, colorRes)
        downloadStatusImage.imageTintList = ColorStateList.valueOf(color)
        downloadStatusText.setTextColor(color)
    }

    private fun setupActions() = with(binding.detailContent) {
        okButton.setOnClickListener { finish() }
    }

    companion object {
        private const val EXTRA_FILE_NAME = "${BuildConfig.APPLICATION_ID}.FILE_NAME"
        private const val EXTRA_DOWNLOAD_STATUS = "${BuildConfig.APPLICATION_ID}.DOWNLOAD_STATUS"

        /**
         * Creates a [Bundle] with given parameters and pass as data to [DetailActivity].
         */
        fun bundleExtrasOf(fileName: String, downloadStatus: DownloadStatus) = bundleOf(
            EXTRA_FILE_NAME to fileName,
            EXTRA_DOWNLOAD_STATUS to downloadStatus.statusText
        )
    }
}
