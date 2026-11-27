package com.auxiliofx.app

import android.app.Activity
import android.content.pm.PackageManager
import rikka.shizuku.Shizuku

object ShizukuBridge {

    private const val REQUEST_CODE = 4012
    private var host: Activity? = null

    private val listener = Shizuku.OnRequestPermissionResultListener { requestCode, grantResult ->
        if (requestCode == REQUEST_CODE && grantResult == PackageManager.PERMISSION_GRANTED) {
            host?.runOnUiThread { }
        }
    }

    fun init(activity: Activity) {
        host = activity
        runCatching { Shizuku.addRequestPermissionResultListener(listener) }
    }

    fun available(): Boolean = runCatching { Shizuku.pingBinder() }.getOrDefault(false)

    fun granted(): Boolean = available() && runCatching {
        Shizuku.checkSelfPermission() == PackageManager.PERMISSION_GRANTED
    }.getOrDefault(false)

    fun status(): String = when {
        !available() -> "unavailable"
        granted() -> "granted"
        else -> "denied"
    }

    fun request(activity: Activity): Boolean {
        if (!available()) return false
        if (granted()) return true
        return runCatching {
            Shizuku.requestPermission(REQUEST_CODE)
            true
        }.getOrDefault(false)
    }

    fun run(command: String): String {
        if (command.isBlank()) return "ERRO: comando vazio"
        if (!granted()) return "ERRO: abra o Shizuku e autorize o Auxílio FX"
        return runCatching {
            val process = Shizuku.newProcess(arrayOf("sh", "-c", command), null, null)
            val output = process.inputStream.bufferedReader().use { it.readText() }.trim()
            val error = process.errorStream.bufferedReader().use { it.readText() }.trim()
            process.waitFor()
            when {
                output.isNotEmpty() -> output
                error.isNotEmpty() -> error
                else -> "OK"
            }
        }.getOrDefault("ERRO: falha ao executar")
    }
}
