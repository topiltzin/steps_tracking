package app.steptracker.ui.permission

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.content.edit

sealed interface PermissionStatus {
    /** API < 29: no runtime permission is needed. */
    data object NotRequired : PermissionStatus
    data object Granted : PermissionStatus

    /** The permission has never been requested. Show the rationale first. */
    data object NeedsRationale : PermissionStatus

    /** Denied. [canPrompt] is false once the system will no longer show the dialog. */
    data class Denied(val canPrompt: Boolean) : PermissionStatus
}

val PermissionStatus.countsSteps: Boolean
    get() = this is PermissionStatus.Granted || this is PermissionStatus.NotRequired

class PermissionChecker(context: Context) {
    private val prefs = context.applicationContext.getSharedPreferences("permission", Context.MODE_PRIVATE)

    fun markAsked() {
        prefs.edit { putBoolean(KEY_ASKED, true) }
    }

    fun status(activity: Activity): PermissionStatus {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) return PermissionStatus.NotRequired
        val granted = ContextCompat.checkSelfPermission(activity, Manifest.permission.ACTIVITY_RECOGNITION) ==
            PackageManager.PERMISSION_GRANTED
        return when {
            granted -> PermissionStatus.Granted
            !prefs.getBoolean(KEY_ASKED, false) -> PermissionStatus.NeedsRationale
            else -> PermissionStatus.Denied(
                canPrompt = ActivityCompat.shouldShowRequestPermissionRationale(
                    activity,
                    Manifest.permission.ACTIVITY_RECOGNITION,
                ),
            )
        }
    }

    private companion object {
        const val KEY_ASKED = "activity_recognition_asked"
    }
}

fun openAppSettingsIntent(context: Context): Intent =
    Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.fromParts("package", context.packageName, null))
        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
