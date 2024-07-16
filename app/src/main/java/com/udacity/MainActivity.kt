package com.udacity

import android.Manifest
import android.annotation.SuppressLint
import android.app.DownloadManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.database.ContentObserver
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.View
import android.widget.RadioButton
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.net.toUri
import com.udacity.databinding.ActivityMainBinding
import com.udacity.download.DownloadNotificator
import com.udacity.download.DownloadStatus
import com.udacity.util.getDownloadManager
import timber.log.Timber
import com.udacity.ButtonState.Completed
import com.udacity.ButtonState.Loading

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    private var downloadFileName = ""
    private var downloadID: Long = 0L
    private var downloadContentObserver: ContentObserver? = null
    private var downloadNotificator: DownloadNotificator? = null

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        if (isGranted) {
            getDownloadNotificator().notify(downloadFileName, DownloadStatus.SUCCESSFUL)
        } else {
            Toast.makeText(this, "Notification permission denied", Toast.LENGTH_SHORT).show()
        }
    }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        binding.apply {
            setSupportActionBar(toolbar)
            registerReceiver(
                receiver,
                IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE)
            )
        }
        onLoadingButtonClicked()
    }

    private fun checkNotificationPermissionAndNotify(fileName: String, downloadStatus: DownloadStatus) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            when {
                ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.POST_NOTIFICATIONS
                ) == PackageManager.PERMISSION_GRANTED -> {
                    // Permission is granted
                    getDownloadNotificator().notify(fileName, downloadStatus)
                }
                shouldShowRequestPermissionRationale(Manifest.permission.POST_NOTIFICATIONS) -> {
                    // In an educational UI, explain to the user why your app requires this
                    // permission for a specific feature to behave as expected.
                    Toast.makeText(
                        this,
                        "Notification permission is required to show download status",
                        Toast.LENGTH_LONG
                    ).show()
                }
                else -> {
                    // Directly ask for the permission
                    requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                }
            }
        } else {
            // For devices below API level 33, notify directly
            getDownloadNotificator().notify(fileName, downloadStatus)
        }
    }

    private fun onLoadingButtonClicked() {
        with(binding.mainContent) {
            loadingButton.setOnClickListener {
                    when (downloadOptionRadioGroup.checkedRadioButtonId) {
                        View.NO_ID ->
                            Toast.makeText(
                                this@MainActivity,
                                "Please select the file to download",
                                Toast.LENGTH_SHORT
                            ).show()

                        else -> {
                            if (isInternetAvailable(this@MainActivity)) {
                                downloadFileName =
                                    findViewById<RadioButton>(downloadOptionRadioGroup.checkedRadioButtonId)
                                        .text.toString()
                                requestDownload()
                            }else {
                                Toast.makeText(
                                    this@MainActivity,
                                    "Device not connect into internet. Please try again!",
                                    Toast.LENGTH_SHORT
                                ).show()
                            }
                        }
                    }
            }
        }
    }

    @SuppressLint("ServiceCast")
    fun isInternetAvailable(context: Context): Boolean {
        val connectivityManager =
            context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val network = connectivityManager.activeNetwork ?: return false
            val activeNetwork = connectivityManager.getNetworkCapabilities(network) ?: return false

            return when {
                activeNetwork.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) -> true
                activeNetwork.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) -> true
                activeNetwork.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET) -> true
                else -> false
            }
        } else {
            val networkInfo = connectivityManager.activeNetworkInfo
            return networkInfo != null && networkInfo.isConnected
        }
    }

    private val receiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            intent?.getLongExtra(DownloadManager.EXTRA_DOWNLOAD_ID, -1)?.let { id ->
                val downloadStatus = getDownloadManager().queryStatus(id)
                Timber.d("Download $id completed with status: ${downloadStatus.statusText}")
                unregisterDownloadContentObserver()
                if (downloadStatus != DownloadStatus.UNKNOWN) {
                    checkNotificationPermissionAndNotify(downloadFileName, downloadStatus)
                }
            }
        }
    }

    private fun getDownloadNotificator(): DownloadNotificator {
        return downloadNotificator ?: DownloadNotificator(this, lifecycle).also { downloadNotificator = it }
    }


    @SuppressLint("Range")
    private fun DownloadManager.queryStatus(id: Long): DownloadStatus {
        query(DownloadManager.Query().setFilterById(id)).use { cursor ->
            return if (cursor != null && cursor.moveToFirst()) {
                when (cursor.getInt(cursor.getColumnIndex(DownloadManager.COLUMN_STATUS))) {
                    DownloadManager.STATUS_SUCCESSFUL -> DownloadStatus.SUCCESSFUL
                    DownloadManager.STATUS_FAILED -> DownloadStatus.FAILED
                    else -> DownloadStatus.UNKNOWN
                }
            } else {
                DownloadStatus.UNKNOWN
            }
        }
    }

    private fun requestDownload() {
        with(getDownloadManager()) {
            downloadID.takeIf { it != 0L }?.run {
                val downloadsCancelled = remove(downloadID)
                unregisterDownloadContentObserver()
                downloadID = 0L
                Timber.d("Number of downloads cancelled: $downloadsCancelled")
            }

            val request = DownloadManager.Request(Uri.parse(URL))
                .setTitle(getString(R.string.app_name))
                .setDescription(getString(R.string.app_description))
                .setRequiresCharging(false)
                .setAllowedOverMetered(true)
                .setAllowedOverRoaming(true)

            val downloadManager = getSystemService(DOWNLOAD_SERVICE) as DownloadManager
            downloadID =
                downloadManager.enqueue(request) // enqueue puts the download request in the queue.

            createAndRegisterDownloadContentObserver()
        }
    }

    private fun DownloadManager.createAndRegisterDownloadContentObserver() {
        object : ContentObserver(Handler(Looper.getMainLooper())) {
            override fun onChange(selfChange: Boolean) {
                downloadContentObserver?.run { queryProgress() }
            }
        }.also {
            downloadContentObserver = it
            contentResolver.registerContentObserver(
                "content://downloads/my_downloads".toUri(),
                true,
                downloadContentObserver!!
            )
        }
    }

    @SuppressLint("Range")
    private fun DownloadManager.queryProgress() {
        query(DownloadManager.Query().setFilterById(downloadID)).use {
            with(it) {
                if (this != null && moveToFirst()) {
                    val id = getInt(getColumnIndex(DownloadManager.COLUMN_ID))
                    when (getInt(getColumnIndex(DownloadManager.COLUMN_STATUS))) {
                        DownloadManager.STATUS_FAILED -> {
                            Timber.d("Download $id: failed")
                            binding.mainContent.loadingButton.changeButtonState(Completed)
                        }

                        DownloadManager.STATUS_PAUSED -> {
                            Timber.d("Download $id: paused")
                        }

                        DownloadManager.STATUS_PENDING -> {
                            Timber.d("Download $id: pending")
                        }

                        DownloadManager.STATUS_RUNNING -> {
                            Timber.d("Download $id: running")
                            binding.mainContent.loadingButton.changeButtonState(Loading)
                        }

                        DownloadManager.STATUS_SUCCESSFUL -> {
                            Timber.d("Download $id: successful")
                            binding.mainContent.loadingButton.changeButtonState(Completed)
                        }
                    }
                }
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        unregisterReceiver(receiver)
        unregisterDownloadContentObserver()
        downloadNotificator = null
    }

    private fun unregisterDownloadContentObserver() {
        downloadContentObserver?.let {
            contentResolver.unregisterContentObserver(it)
            downloadContentObserver = null
        }
    }

    companion object {
        private const val URL =
            "https://github.com/udacity/nd940-c3-advanced-android-programming-project-starter/archive/master.zip"
    }
}