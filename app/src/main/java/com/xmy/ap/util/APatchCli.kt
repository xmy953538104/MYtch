package com.xmy.ap.util

import android.content.ContentResolver
import android.content.Context
import android.content.pm.PackageManager
import android.content.pm.Signature
import android.database.Cursor
import android.net.Uri
import android.os.Build
import android.provider.OpenableColumns
import android.util.Base64
import android.util.Log
import com.topjohnwu.superuser.Shell
import com.topjohnwu.superuser.ShellUtils
import com.topjohnwu.superuser.internal.MainShell
import com.xmy.ap.APApplication
import com.xmy.ap.APApplication.Companion.SUPERCMD
import com.xmy.ap.BuildConfig
import com.xmy.ap.apApp
import java.io.File
import java.security.MessageDigest
import java.security.cert.CertificateFactory
import java.security.cert.X509Certificate
import java.util.zip.ZipFile

private const val TAG = "APatchCli"

private fun getKPatchPath(): String {
    return apApp.applicationInfo.nativeLibraryDir + File.separator + "libkpatch.so"
}

class RootShellInitializer : Shell.Initializer() {
    override fun onInit(context: Context, shell: Shell): Boolean {
        shell.newJob().add("export PATH=\$PATH:/system_ext/bin:/vendor/bin").exec()
        return true
    }
}

fun createRootShell(globalMnt: Boolean = false): Shell {
    Shell.enableVerboseLogging = BuildConfig.DEBUG
    val builder = Shell.Builder.create().setInitializers(RootShellInitializer::class.java)
    return try {
        builder.build(
            SUPERCMD, APApplication.ROOT_KEY, "-Z", APApplication.MAGISK_SCONTEXT
        )
    } catch (e: Throwable) {
        Log.e(TAG, "su failed: ", e)
        return try {
            Log.e(TAG, "retry compat kpatch su")
            if (globalMnt) {
                builder.build(
                    getKPatchPath(), APApplication.ROOT_KEY, "su", "-Z", APApplication.MAGISK_SCONTEXT, "--mount-master"
                )
            }else{
                builder.build(
                    getKPatchPath(), APApplication.ROOT_KEY, "su", "-Z", APApplication.MAGISK_SCONTEXT
                )
            }
        } catch (e: Throwable) {
            Log.e(TAG, "retry kpatch su failed: ", e)
            return try {
                Log.e(TAG, "retry su: ", e)
                if (globalMnt) {
                    builder.build("su","-mm")
                }else{
                    builder.build("su")
                }
            } catch (e: Throwable) {
                Log.e(TAG, "retry su failed: ", e)
                return builder.build("sh")
            }
        }
    }
}

private fun createMainRootShell() : Shell {
    val builder = Shell.Builder.create()
        .setInitializers(RootShellInitializer::class.java)
    val shell = try {
        builder.build(SUPERCMD, APApplication.ROOT_KEY, "-Z", APApplication.MAGISK_SCONTEXT)
    } catch (e: Throwable) {
        Log.e(TAG, "su failed: ", e)
        builder.setCommands(getKPatchPath(), APApplication.ROOT_KEY, "su", "-Z", APApplication.MAGISK_SCONTEXT)
        try {
            builder.build()
        } catch (e: Throwable) {
            Log.e(TAG, "retry kpatch su failed: ", e)
            builder.setCommands("su")
            try {
                builder.build()
            } catch (e: Throwable) {
                Log.e(TAG, "retry su failed: ", e)
                builder.setCommands("sh")
                builder.build()
            }
        }
    }

    MainShell.setBuilder(builder)
    return shell
}

object APatchCli {
    var SHELL: Shell = createMainRootShell()
    private var globalMntShell: Shell? = null

    fun getGlobalMntShell(): Shell {
        return globalMntShell ?: createRootShell(true).also {
            globalMntShell = it
        }
    }

    fun refresh() {
        val tmp = SHELL
        val tmpGlobal = globalMntShell
        globalMntShell = null

        val clazz = MainShell::class.java // reset MainShell
        clazz.getDeclaredField("isInitMain").apply {
            isAccessible = true
            setBoolean(null, false)
            isAccessible = false
        }

        clazz.getDeclaredField("mainShell").apply {
            isAccessible = true
            @Suppress("UNCHECKED_CAST")
            val arr = get(null) as Array<Any?>
            arr[0] = null
            isAccessible = false
        }

        clazz.getDeclaredField("mainBuilder").apply {
            isAccessible = true
            set(null, null)
            isAccessible = false
        }

        SHELL = createMainRootShell()
        tmp.close()
        tmpGlobal?.close()
    }
}

fun getRootShell(globalMnt: Boolean = false): Shell {

    return if (globalMnt) APatchCli.getGlobalMntShell() else {
        APatchCli.SHELL
    }
}

fun rootAvailable(): Boolean {
    val shell = getRootShell()
    return shell.isRoot
}

fun tryGetRootShell(): Shell {
    Shell.enableVerboseLogging = BuildConfig.DEBUG
    val builder = Shell.Builder.create()
    return try {
        builder.build(
            SUPERCMD, APApplication.ROOT_KEY, "-Z", APApplication.MAGISK_SCONTEXT
        )
    } catch (e: Throwable) {
        Log.e(TAG, "su failed: ", e)
        return try {
            Log.e(TAG, "retry compat kpatch su")
            builder.build(
                getKPatchPath(), APApplication.ROOT_KEY, "su", "-Z", APApplication.MAGISK_SCONTEXT
            )
        } catch (e: Throwable) {
            Log.e(TAG, "retry kpatch su failed: ", e)
            return try {
                Log.e(TAG, "retry su: ", e)
                builder.build("su")
            } catch (e: Throwable) {
                Log.e(TAG, "retry su failed: ", e)
                builder.build("sh")
            }
        }
    }
}

fun shellForResult(shell: Shell, vararg cmds: String): Shell.Result {
    val out = ArrayList<String>()
    val err = ArrayList<String>()
    return shell.newJob().add(*cmds).to(out, err).exec()
}

fun rootShellForResult(vararg cmds: String): Shell.Result {
    val out = ArrayList<String>()
    val err = ArrayList<String>()
    return getRootShell().newJob().add(*cmds).to(out, err).exec()
}

fun reboot(reason: String = "") {
    if (reason == "recovery") {
        // KEYCODE_POWER = 26, hide incorrect "Factory data reset" message
        getRootShell().newJob().add("/system/bin/input keyevent 26").exec()
    }
    getRootShell().newJob()
        .add("/system/bin/svc power reboot $reason || /system/bin/reboot $reason").exec()
}

fun isGlobalNamespaceEnabled(): Boolean {
    val shell = getRootShell()
    val result = ShellUtils.fastCmd(shell, "cat ${APApplication.GLOBAL_NAMESPACE_FILE}")
    Log.i(TAG, "is global namespace enabled: $result")
    return result == "1"
}

fun setGlobalNamespaceEnabled(value: String) {
    getRootShell().newJob().add("echo $value > ${APApplication.GLOBAL_NAMESPACE_FILE}")
        .submit { result ->
            Log.i(TAG, "setGlobalNamespaceEnabled result: ${result.isSuccess} [${result.out}]")
        }
}

fun getFileNameFromUri(context: Context, uri: Uri): String? {
    var fileName: String? = null
    val contentResolver: ContentResolver = context.contentResolver
    val cursor: Cursor? = contentResolver.query(uri, null, null, null, null)
    cursor?.use {
        if (it.moveToFirst()) {
            fileName = it.getString(it.getColumnIndexOrThrow(OpenableColumns.DISPLAY_NAME))
        }
    }
    return fileName
}

@Suppress("DEPRECATION")
private fun signatureFromAPI(context: Context): ByteArray? {
    return try {
        val packageInfo = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            context.packageManager.getPackageInfo(
                context.packageName, PackageManager.GET_SIGNING_CERTIFICATES
            )
        } else {
            context.packageManager.getPackageInfo(
                context.packageName,
                PackageManager.GET_SIGNATURES
            )
        }

        val signatures: Array<out Signature>? =
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                packageInfo.signingInfo?.apkContentsSigners
            } else {
                packageInfo.signatures
            }

        signatures?.firstOrNull()?.toByteArray()
    } catch (e: Exception) {
        e.printStackTrace()
        null
    }
}

private fun signatureFromAPK(context: Context): ByteArray? {
    var signatureBytes: ByteArray? = null
    try {
        ZipFile(context.packageResourcePath).use { zipFile ->
            val entries = zipFile.entries()
            while (entries.hasMoreElements() && signatureBytes == null) {
                val entry = entries.nextElement()
                if (entry.name.matches("(META-INF/.*)\\.(RSA|DSA|EC)".toRegex())) {
                    zipFile.getInputStream(entry).use { inputStream ->
                        val certFactory = CertificateFactory.getInstance("X509")
                        val x509Cert =
                            certFactory.generateCertificate(inputStream) as X509Certificate
                        signatureBytes = x509Cert.encoded
                    }
                }
            }
        }
    } catch (e: Exception) {
        e.printStackTrace()
    }
    return signatureBytes
}

private fun validateSignature(signatureBytes: ByteArray?, validSignature: String): Boolean {
    signatureBytes ?: return false
    val digest = MessageDigest.getInstance("SHA-256")
    val signatureHash = Base64.encodeToString(digest.digest(signatureBytes), Base64.NO_WRAP)
    return signatureHash == validSignature
}

fun verifyAppSignature(validSignature: String): Boolean {
    val context = apApp.applicationContext
    val apkSignature = signatureFromAPK(context)
    val apiSignature = signatureFromAPI(context)

    return validateSignature(apiSignature, validSignature) && validateSignature(
        apkSignature,
        validSignature
    )
}
