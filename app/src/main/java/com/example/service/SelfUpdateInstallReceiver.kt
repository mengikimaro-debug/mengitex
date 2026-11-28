package com.example.service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageInstaller
import android.os.Build

/**
 * Receives explicit PackageInstaller session callbacks so MengiTex can handle
 * in-place APK upgrades, user confirmation prompts, and conflict diagnostics cleanly.
 */
class SelfUpdateInstallReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != ACTION_INSTALL_STATUS) return

        val status = intent.getIntExtra(
            PackageInstaller.EXTRA_STATUS,
            PackageInstaller.STATUS_FAILURE
        )
        val sessionId = intent.getIntExtra(PackageInstaller.EXTRA_SESSION_ID, -1)
        val statusMessage = intent.getStringExtra(PackageInstaller.EXTRA_STATUS_MESSAGE).orEmpty()
        val versionTag = intent.getStringExtra(EXTRA_VERSION_TAG).orEmpty()

        val event = when (status) {
            PackageInstaller.STATUS_PENDING_USER_ACTION -> {
                val confirmIntent = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    intent.getParcelableExtra(Intent.EXTRA_INTENT, Intent::class.java)
                } else {
                    @Suppress("DEPRECATION")
                    intent.getParcelableExtra(Intent.EXTRA_INTENT)
                }
                if (confirmIntent != null) {
                    confirmIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    if (!GithubUpdateChecker.hasActiveInstallCollector()) {
                        runCatching {
                            context.startActivity(confirmIntent)
                        }
                    }
                    PackageInstallSessionEvent.PendingUserAction(
                        sessionId = sessionId,
                        versionTag = versionTag,
                        confirmIntent = confirmIntent
                    )
                } else {
                    PackageInstallSessionEvent.FailureError(
                        sessionId = sessionId,
                        status = status,
                        message = "Android confirmation intent missing."
                    )
                }
            }

            PackageInstaller.STATUS_SUCCESS -> {
                PackageInstallSessionEvent.Success(
                    sessionId = sessionId,
                    versionTag = versionTag
                )
            }

            PackageInstaller.STATUS_FAILURE_CONFLICT -> {
                PackageInstallSessionEvent.FailureConflict(
                    sessionId = sessionId,
                    versionTag = versionTag,
                    rawMessage = statusMessage
                )
            }

            PackageInstaller.STATUS_FAILURE_ABORTED -> {
                PackageInstallSessionEvent.FailureAborted(
                    sessionId = sessionId,
                    rawMessage = statusMessage
                )
            }

            PackageInstaller.STATUS_FAILURE_INCOMPATIBLE,
            PackageInstaller.STATUS_FAILURE_INVALID,
            PackageInstaller.STATUS_FAILURE_STORAGE,
            PackageInstaller.STATUS_FAILURE -> {
                PackageInstallSessionEvent.FailureError(
                    sessionId = sessionId,
                    status = status,
                    message = statusMessage
                )
            }

            else -> {
                PackageInstallSessionEvent.FailureError(
                    sessionId = sessionId,
                    status = status,
                    message = statusMessage
                )
            }
        }

        GithubUpdateChecker.emitInstallSessionEvent(event)
    }

    companion object {
        const val ACTION_INSTALL_STATUS = "com.example.service.ACTION_SELF_UPDATE_INSTALL_STATUS"
        const val EXTRA_VERSION_TAG = "extra_version_tag"
    }
}
