package android.media;

import android.media.MediaCodecInfo;
import android.util.Log;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Map;

public final class MediaCodecList {
    public static final int ALL_CODECS = 1;
    public static final int REGULAR_CODECS = 0;
    private static final String TAG = "MediaCodecList";
    private static MediaCodecInfo[] sAllCodecInfos;
    private static Map<String, Object> sGlobalSettings;
    private static Object sInitLock = new Object();
    private static MediaCodecInfo[] sRegularCodecInfos;
    private MediaCodecInfo[] mCodecInfos;

    static final native int findCodecByName(String str);

    static final native MediaCodecInfo.CodecCapabilities getCodecCapabilities(int i, String str);

    static final native String getCodecName(int i);

    static final native String[] getSupportedTypes(int i);

    static final native boolean isEncoder(int i);

    private static final native int native_getCodecCount();

    static final native Map<String, Object> native_getGlobalSettings();

    private static final native void native_init();

    public static final int getCodecCount() {
        initCodecList();
        return sRegularCodecInfos.length;
    }

    public static final MediaCodecInfo getCodecInfoAt(int i) {
        initCodecList();
        if (i < 0 || i > sRegularCodecInfos.length) {
            throw new IllegalArgumentException();
        }
        return sRegularCodecInfos[i];
    }

    static final Map<String, Object> getGlobalSettings() {
        synchronized (sInitLock) {
            if (sGlobalSettings == null) {
                sGlobalSettings = native_getGlobalSettings();
            }
        }
        return sGlobalSettings;
    }

    static {
        System.loadLibrary("media_jni");
        native_init();
    }

    private static final void initCodecList() {
        synchronized (sInitLock) {
            if (sRegularCodecInfos == null) {
                int iNative_getCodecCount = native_getCodecCount();
                ArrayList arrayList = new ArrayList();
                ArrayList arrayList2 = new ArrayList();
                for (int i = 0; i < iNative_getCodecCount; i++) {
                    try {
                        MediaCodecInfo newCodecInfoAt = getNewCodecInfoAt(i);
                        arrayList2.add(newCodecInfoAt);
                        MediaCodecInfo mediaCodecInfoMakeRegular = newCodecInfoAt.makeRegular();
                        if (mediaCodecInfoMakeRegular != null) {
                            arrayList.add(mediaCodecInfoMakeRegular);
                        }
                    } catch (Exception e) {
                        Log.e(TAG, "Could not get codec capabilities", e);
                    }
                }
                sRegularCodecInfos = (MediaCodecInfo[]) arrayList.toArray(new MediaCodecInfo[arrayList.size()]);
                sAllCodecInfos = (MediaCodecInfo[]) arrayList2.toArray(new MediaCodecInfo[arrayList2.size()]);
            }
        }
    }

    private static MediaCodecInfo getNewCodecInfoAt(int i) {
        String[] supportedTypes = getSupportedTypes(i);
        MediaCodecInfo.CodecCapabilities[] codecCapabilitiesArr = new MediaCodecInfo.CodecCapabilities[supportedTypes.length];
        int length = supportedTypes.length;
        int i2 = 0;
        int i3 = 0;
        while (i2 < length) {
            codecCapabilitiesArr[i3] = getCodecCapabilities(i, supportedTypes[i2]);
            i2++;
            i3++;
        }
        return new MediaCodecInfo(getCodecName(i), isEncoder(i), codecCapabilitiesArr);
    }

    public static MediaCodecInfo getInfoFor(String str) {
        initCodecList();
        return sAllCodecInfos[findCodecByName(str)];
    }

    private MediaCodecList() {
        this(0);
    }

    public MediaCodecList(int i) {
        initCodecList();
        if (i == 0) {
            this.mCodecInfos = sRegularCodecInfos;
        } else {
            this.mCodecInfos = sAllCodecInfos;
        }
    }

    public final MediaCodecInfo[] getCodecInfos() {
        return (MediaCodecInfo[]) Arrays.copyOf(this.mCodecInfos, this.mCodecInfos.length);
    }

    public final String findDecoderForFormat(MediaFormat mediaFormat) {
        return findCodecForFormat(false, mediaFormat);
    }

    public final String findEncoderForFormat(MediaFormat mediaFormat) {
        return findCodecForFormat(true, mediaFormat);
    }

    private String findCodecForFormat(boolean z, MediaFormat mediaFormat) {
        String string = mediaFormat.getString(MediaFormat.KEY_MIME);
        for (MediaCodecInfo mediaCodecInfo : this.mCodecInfos) {
            if (mediaCodecInfo.isEncoder() == z) {
                try {
                    MediaCodecInfo.CodecCapabilities capabilitiesForType = mediaCodecInfo.getCapabilitiesForType(string);
                    if (capabilitiesForType != null && capabilitiesForType.isFormatSupported(mediaFormat)) {
                        return mediaCodecInfo.getName();
                    }
                } catch (IllegalArgumentException e) {
                }
            }
        }
        return null;
    }
}
