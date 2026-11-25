package com.example.security

import android.content.ComponentName
import android.content.Context
import android.content.pm.PackageManager

/**
 * Controls home-screen launcher visibility via Android activity-aliases:
 * - VISIBLE: standard SiriSMS icon
 * - DISGUISE_CALC: swaps icon/label to neutral "Kikokotoo" calculator
 * - HIDDEN: disables launcher aliases for stealth mode
 */
object AppIconManager {

    private const val MAIN_ALIAS = "com.example.MainLauncherAlias"
    private const val DISGUISE_ALIAS = "com.example.DisguiseLauncherAlias"

    fun applyMode(context: Context, mode: LauncherIconMode) {
        val pm = context.packageManager
        val mainComp = ComponentName(context.packageName, MAIN_ALIAS)
        val disguiseComp = ComponentName(context.packageName, DISGUISE_ALIAS)

        when (mode) {
            LauncherIconMode.VISIBLE -> {
                pm.setComponentEnabledSetting(
                    mainComp,
                    PackageManager.COMPONENT_ENABLED_STATE_ENABLED,
                    PackageManager.DONT_KILL_APP
                )
                pm.setComponentEnabledSetting(
                    disguiseComp,
                    PackageManager.COMPONENT_ENABLED_STATE_DISABLED,
                    PackageManager.DONT_KILL_APP
                )
            }
            LauncherIconMode.DISGUISE_CALC -> {
                pm.setComponentEnabledSetting(
                    disguiseComp,
                    PackageManager.COMPONENT_ENABLED_STATE_ENABLED,
                    PackageManager.DONT_KILL_APP
                )
                pm.setComponentEnabledSetting(
                    mainComp,
                    PackageManager.COMPONENT_ENABLED_STATE_DISABLED,
                    PackageManager.DONT_KILL_APP
                )
            }
            LauncherIconMode.HIDDEN -> {
                pm.setComponentEnabledSetting(
                    mainComp,
                    PackageManager.COMPONENT_ENABLED_STATE_DISABLED,
                    PackageManager.DONT_KILL_APP
                )
                pm.setComponentEnabledSetting(
                    disguiseComp,
                    PackageManager.COMPONENT_ENABLED_STATE_DISABLED,
                    PackageManager.DONT_KILL_APP
                )
            }
        }
    }
}
