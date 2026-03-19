package com.android.internal.os;

import android.os.Trace;
import android.system.ErrnoException;
import android.system.Os;
import dalvik.system.ZygoteHooks;

public final class Zygote {
    public static final int API_ENFORCEMENT_POLICY_MASK = 12288;
    public static final String CHILD_ZYGOTE_SOCKET_NAME_ARG = "--zygote-socket=";
    public static final int DEBUG_ALWAYS_JIT = 64;
    public static final int DEBUG_ENABLE_ASSERT = 4;
    public static final int DEBUG_ENABLE_CHECKJNI = 2;
    public static final int DEBUG_ENABLE_JDWP = 1;
    public static final int DEBUG_ENABLE_JNI_LOGGING = 16;
    public static final int DEBUG_ENABLE_SAFEMODE = 8;
    public static final int DEBUG_GENERATE_DEBUG_INFO = 32;
    public static final int DEBUG_GENERATE_MINI_DEBUG_INFO = 2048;
    public static final int DEBUG_JAVA_DEBUGGABLE = 256;
    public static final int DEBUG_NATIVE_DEBUGGABLE = 128;
    public static final int DISABLE_VERIFIER = 512;
    public static final int MOUNT_EXTERNAL_DEFAULT = 1;
    public static final int MOUNT_EXTERNAL_NONE = 0;
    public static final int MOUNT_EXTERNAL_READ = 2;
    public static final int MOUNT_EXTERNAL_WRITE = 3;
    public static final int ONLY_USE_SYSTEM_OAT_FILES = 1024;
    public static final int PROFILE_SYSTEM_SERVER = 16384;
    public static final int API_ENFORCEMENT_POLICY_SHIFT = Integer.numberOfTrailingZeros(12288);
    private static final ZygoteHooks VM_HOOKS = new ZygoteHooks();

    protected static native void nativeAllowFileAcrossFork(String str);

    private static native int nativeForkAndSpecialize(int i, int i2, int[] iArr, int i3, int[][] iArr2, int i4, String str, String str2, int[] iArr3, int[] iArr4, boolean z, String str3, String str4);

    private static native int nativeForkSystemServer(int i, int i2, int[] iArr, int i3, int[][] iArr2, long j, long j2);

    static native void nativePreApplicationInit();

    static native void nativeSecurityInit();

    protected static native void nativeUnmountStorageOnInit();

    private Zygote() {
    }

    public static int forkAndSpecialize(int i, int i2, int[] iArr, int i3, int[][] iArr2, int i4, String str, String str2, int[] iArr3, int[] iArr4, boolean z, String str3, String str4) {
        VM_HOOKS.preFork();
        resetNicePriority();
        int iNativeForkAndSpecialize = nativeForkAndSpecialize(i, i2, iArr, i3, iArr2, i4, str, str2, iArr3, iArr4, z, str3, str4);
        if (iNativeForkAndSpecialize == 0) {
            Trace.setTracingEnabled(true, i3);
            Trace.traceBegin(64L, "PostFork");
        }
        VM_HOOKS.postForkCommon();
        return iNativeForkAndSpecialize;
    }

    public static int forkSystemServer(int i, int i2, int[] iArr, int i3, int[][] iArr2, long j, long j2) {
        VM_HOOKS.preFork();
        resetNicePriority();
        int iNativeForkSystemServer = nativeForkSystemServer(i, i2, iArr, i3, iArr2, j, j2);
        if (iNativeForkSystemServer == 0) {
            Trace.setTracingEnabled(true, i3);
        }
        VM_HOOKS.postForkCommon();
        return iNativeForkSystemServer;
    }

    private static void callPostForkChildHooks(int i, boolean z, boolean z2, String str) {
        VM_HOOKS.postForkChild(i, z, z2, str);
    }

    static void resetNicePriority() {
        Thread.currentThread().setPriority(5);
    }

    public static void execShell(String str) {
        String[] strArr = {"/system/bin/sh", "-c", str};
        try {
            Os.execv(strArr[0], strArr);
        } catch (ErrnoException e) {
            throw new RuntimeException(e);
        }
    }

    public static void appendQuotedShellArgs(StringBuilder sb, String[] strArr) {
        for (String str : strArr) {
            sb.append(" '");
            sb.append(str.replace("'", "'\\''"));
            sb.append("'");
        }
    }
}
