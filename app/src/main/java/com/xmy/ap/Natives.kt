package com.xmy.ap

import android.os.Parcelable
import androidx.annotation.Keep
import androidx.compose.runtime.Immutable
import dalvik.annotation.optimization.FastNative
import kotlinx.parcelize.Parcelize

object Natives {
    init {
        System.loadLibrary("apjni")
    }

    @Immutable
    @Parcelize
    @Keep
    data class Profile(
        var uid: Int = 0,
        var toUid: Int = 0,
        var scontext: String = APApplication.DEFAULT_SCONTEXT,
    ) : Parcelable

    @Keep
    class KPMCtlRes {
        var rc: Long = 0
        var outMsg: String? = null

        constructor()

        constructor(rc: Long, outMsg: String?) {
            this.rc = rc
            this.outMsg = outMsg
        }
    }


    @FastNative
    private external fun nativeSu(rootKey: String, toUid: Int, scontext: String?): Long

    fun su(toUid: Int, scontext: String?): Boolean {
        return nativeSu(APApplication.ROOT_KEY, toUid, scontext) == 0L
    }

    fun su(): Boolean {
        return su(0, "")
    }

    @FastNative
    external fun nativeReady(rootKey: String): Boolean

    @FastNative
    private external fun nativeSuPath(rootKey: String): String

    fun suPath(): String {
        return nativeSuPath(APApplication.ROOT_KEY)
    }

    @FastNative
    private external fun nativeSuUids(rootKey: String): IntArray

    fun suUids(): IntArray {
        return nativeSuUids(APApplication.ROOT_KEY)
    }

    @FastNative
    private external fun nativeKernelPatchVersion(rootKey: String): Long
    fun kernelPatchVersion(): Long {
        return nativeKernelPatchVersion(APApplication.ROOT_KEY)
    }

    @FastNative
    private external fun nativeKernelPatchBuildTime(rootKey: String): String
    fun kernelPatchBuildTime(): String {
        return nativeKernelPatchBuildTime(APApplication.ROOT_KEY)
    }

    private external fun nativeLoadKernelPatchModule(
        rootKey: String, modulePath: String, args: String
    ): Long

    fun loadKernelPatchModule(modulePath: String, args: String): Long {
        return nativeLoadKernelPatchModule(APApplication.ROOT_KEY, modulePath, args)
    }

    private external fun nativeUnloadKernelPatchModule(rootKey: String, moduleName: String): Long
    fun unloadKernelPatchModule(moduleName: String): Long {
        return nativeUnloadKernelPatchModule(APApplication.ROOT_KEY, moduleName)
    }

    @FastNative
    private external fun nativeKernelPatchModuleNum(rootKey: String): Long

    fun kernelPatchModuleNum(): Long {
        return nativeKernelPatchModuleNum(APApplication.ROOT_KEY)
    }

    @FastNative
    private external fun nativeKernelPatchModuleList(rootKey: String): String
    fun kernelPatchModuleList(): String {
        return nativeKernelPatchModuleList(APApplication.ROOT_KEY)
    }

    @FastNative
    private external fun nativeKernelPatchModuleInfo(rootKey: String, moduleName: String): String
    fun kernelPatchModuleInfo(moduleName: String): String {
        return nativeKernelPatchModuleInfo(APApplication.ROOT_KEY, moduleName)
    }

    private external fun nativeControlKernelPatchModule(
        rootKey: String, modName: String, jctlargs: String
    ): KPMCtlRes

    fun kernelPatchModuleControl(moduleName: String, controlArg: String): KPMCtlRes {
        return nativeControlKernelPatchModule(APApplication.ROOT_KEY, moduleName, controlArg)
    }

    @FastNative
    private external fun nativeGrantSu(
        rootKey: String, uid: Int, toUid: Int, scontext: String?
    ): Long

    fun grantSu(uid: Int, toUid: Int, scontext: String?): Long {
        return nativeGrantSu(APApplication.ROOT_KEY, uid, toUid, scontext)
    }

    @FastNative
    private external fun nativeRevokeSu(rootKey: String, uid: Int): Long
    fun revokeSu(uid: Int): Long {
        return nativeRevokeSu(APApplication.ROOT_KEY, uid)
    }

    @FastNative
    private external fun nativeSetUidExclude(rootKey: String, uid: Int, exclude: Int): Int
    fun setUidExclude(uid: Int, exclude: Int): Int {
        return nativeSetUidExclude(APApplication.ROOT_KEY, uid, exclude)
    }

    @FastNative
    private external fun nativeGetUidExclude(rootKey: String, uid: Int): Int
    fun isUidExcluded(uid: Int): Int {
        return nativeGetUidExclude(APApplication.ROOT_KEY, uid)
    }

    @FastNative
    private external fun nativeSuProfile(rootKey: String, uid: Int): Profile
    fun suProfile(uid: Int): Profile {
        return nativeSuProfile(APApplication.ROOT_KEY, uid)
    }

    @FastNative
    private external fun nativeResetSuPath(rootKey: String, path: String): Boolean
    fun resetSuPath(path: String): Boolean {
        return nativeResetSuPath(APApplication.ROOT_KEY, path)
    }
}

