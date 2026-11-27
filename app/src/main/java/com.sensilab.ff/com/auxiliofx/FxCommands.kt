package com.auxiliofx.app

import android.content.Context
import android.content.Intent

object FxCommands {

    const val PACKAGE_FF = "com.dts.freefireth"
    const val PACKAGE_FF_MAX = "com.dts.freefiremax"

    // Ajustes de sistema que o Shizuku executa com permissão de shell.
    fun boostSensi(): String = listOf(
        "settings put system pointer_speed 7",
        "settings put system accelerometer_rotation 0",
        "cmd game set --mode performance --downscale 0.85 " + PACKAGE_FF
    ).joinToString(" && ")

    fun performance(): String = listOf(
        "settings put global window_animation_scale 0.5",
        "settings put global transition_animation_scale 0.5",
        "settings put global animator_duration_scale 0.5",
        "cmd power set-fixed-performance-mode-enabled true",
        "cmd game set --mode performance --downscale 0.8 " + PACKAGE_FF,
        "am kill-all"
    ).joinToString(" && ")

    fun openGame(context: Context): String {
        val launcher = context.packageManager.getLaunchIntentForPackage(PACKAGE_FF)
            ?: context.packageManager.getLaunchIntentForPackage(PACKAGE_FF_MAX)
            ?: return "Free Fire não instalado"
        launcher.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(launcher)
        return "Abrindo Free Fire"
    }
}
