package com.android.server;

import android.os.StrictMode;
import android.os.SystemProperties;
import android.util.Log;
import android.util.Slog;
import dalvik.system.SocketTagger;
import java.io.FileDescriptor;
import java.net.SocketException;

public final class NetworkManagementSocketTagger extends SocketTagger {
    private static final boolean LOGD = false;
    public static final String PROP_QTAGUID_ENABLED = "net.qtaguid_enabled";
    private static final String TAG = "NetworkManagementSocketTagger";
    private static ThreadLocal<SocketTags> threadSocketTags = new ThreadLocal<SocketTags>() {
        @Override
        protected SocketTags initialValue() {
            return new SocketTags();
        }
    };

    public static class SocketTags {
        public int statsTag = -1;
        public int statsUid = -1;
    }

    private static native int native_deleteTagData(int i, int i2);

    private static native int native_setCounterSet(int i, int i2);

    private static native int native_tagSocketFd(FileDescriptor fileDescriptor, int i, int i2);

    private static native int native_untagSocketFd(FileDescriptor fileDescriptor);

    public static void install() {
        SocketTagger.set(new NetworkManagementSocketTagger());
    }

    public static int setThreadSocketStatsTag(int i) {
        int i2 = threadSocketTags.get().statsTag;
        threadSocketTags.get().statsTag = i;
        return i2;
    }

    public static int getThreadSocketStatsTag() {
        return threadSocketTags.get().statsTag;
    }

    public static int setThreadSocketStatsUid(int i) {
        int i2 = threadSocketTags.get().statsUid;
        threadSocketTags.get().statsUid = i;
        return i2;
    }

    public static int getThreadSocketStatsUid() {
        return threadSocketTags.get().statsUid;
    }

    public void tag(FileDescriptor fileDescriptor) throws SocketException {
        SocketTags socketTags = threadSocketTags.get();
        if (socketTags.statsTag == -1 && StrictMode.vmUntaggedSocketEnabled()) {
            StrictMode.onUntaggedSocket();
        }
        tagSocketFd(fileDescriptor, socketTags.statsTag, socketTags.statsUid);
    }

    private void tagSocketFd(FileDescriptor fileDescriptor, int i, int i2) {
        int iNative_tagSocketFd;
        if ((i != -1 || i2 != -1) && SystemProperties.getBoolean(PROP_QTAGUID_ENABLED, false) && (iNative_tagSocketFd = native_tagSocketFd(fileDescriptor, i, i2)) < 0) {
            Log.i(TAG, "tagSocketFd(" + fileDescriptor.getInt$() + ", " + i + ", " + i2 + ") failed with errno" + iNative_tagSocketFd);
        }
    }

    public void untag(FileDescriptor fileDescriptor) throws SocketException {
        unTagSocketFd(fileDescriptor);
    }

    private void unTagSocketFd(FileDescriptor fileDescriptor) {
        int iNative_untagSocketFd;
        SocketTags socketTags = threadSocketTags.get();
        if ((socketTags.statsTag != -1 || socketTags.statsUid != -1) && SystemProperties.getBoolean(PROP_QTAGUID_ENABLED, false) && (iNative_untagSocketFd = native_untagSocketFd(fileDescriptor)) < 0) {
            Log.w(TAG, "untagSocket(" + fileDescriptor.getInt$() + ") failed with errno " + iNative_untagSocketFd);
        }
    }

    public static void setKernelCounterSet(int i, int i2) {
        int iNative_setCounterSet;
        if (SystemProperties.getBoolean(PROP_QTAGUID_ENABLED, false) && (iNative_setCounterSet = native_setCounterSet(i2, i)) < 0) {
            Log.w(TAG, "setKernelCountSet(" + i + ", " + i2 + ") failed with errno " + iNative_setCounterSet);
        }
    }

    public static void resetKernelUidStats(int i) {
        int iNative_deleteTagData;
        if (SystemProperties.getBoolean(PROP_QTAGUID_ENABLED, false) && (iNative_deleteTagData = native_deleteTagData(0, i)) < 0) {
            Slog.w(TAG, "problem clearing counters for uid " + i + " : errno " + iNative_deleteTagData);
        }
    }

    public static int kernelToTag(String str) {
        int length = str.length();
        if (length > 10) {
            return Long.decode(str.substring(0, length - 8)).intValue();
        }
        return 0;
    }
}
