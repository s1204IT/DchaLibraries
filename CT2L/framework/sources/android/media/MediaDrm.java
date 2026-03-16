package android.media;

import android.net.ProxyInfo;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.os.Parcel;
import android.util.Log;
import java.lang.ref.WeakReference;
import java.util.HashMap;
import java.util.List;
import java.util.UUID;

public final class MediaDrm {
    public static final int CERTIFICATE_TYPE_NONE = 0;
    public static final int CERTIFICATE_TYPE_X509 = 1;
    private static final int DRM_EVENT = 200;
    public static final int EVENT_KEY_EXPIRED = 3;
    public static final int EVENT_KEY_REQUIRED = 2;
    public static final int EVENT_PROVISION_REQUIRED = 1;
    public static final int EVENT_VENDOR_DEFINED = 4;
    public static final int KEY_TYPE_OFFLINE = 2;
    public static final int KEY_TYPE_RELEASE = 3;
    public static final int KEY_TYPE_STREAMING = 1;
    private static final String PERMISSION = "android.permission.ACCESS_DRM_CERTIFICATES";
    public static final String PROPERTY_ALGORITHMS = "algorithms";
    public static final String PROPERTY_DESCRIPTION = "description";
    public static final String PROPERTY_DEVICE_UNIQUE_ID = "deviceUniqueId";
    public static final String PROPERTY_VENDOR = "vendor";
    public static final String PROPERTY_VERSION = "version";
    private static final String TAG = "MediaDrm";
    private EventHandler mEventHandler;
    private long mNativeContext;
    private OnEventListener mOnEventListener;

    public interface OnEventListener {
        void onEvent(MediaDrm mediaDrm, byte[] bArr, int i, int i2, byte[] bArr2);
    }

    private static final native byte[] decryptNative(MediaDrm mediaDrm, byte[] bArr, byte[] bArr2, byte[] bArr3, byte[] bArr4);

    private static final native byte[] encryptNative(MediaDrm mediaDrm, byte[] bArr, byte[] bArr2, byte[] bArr3, byte[] bArr4);

    private native ProvisionRequest getProvisionRequestNative(int i, String str);

    private static final native boolean isCryptoSchemeSupportedNative(byte[] bArr, String str);

    private final native void native_finalize();

    private static final native void native_init();

    private final native void native_setup(Object obj, byte[] bArr);

    private native Certificate provideProvisionResponseNative(byte[] bArr) throws DeniedByServerException;

    private static final native void setCipherAlgorithmNative(MediaDrm mediaDrm, byte[] bArr, String str);

    private static final native void setMacAlgorithmNative(MediaDrm mediaDrm, byte[] bArr, String str);

    private static final native byte[] signNative(MediaDrm mediaDrm, byte[] bArr, byte[] bArr2, byte[] bArr3);

    private static final native byte[] signRSANative(MediaDrm mediaDrm, byte[] bArr, String str, byte[] bArr2, byte[] bArr3);

    private static final native boolean verifyNative(MediaDrm mediaDrm, byte[] bArr, byte[] bArr2, byte[] bArr3, byte[] bArr4);

    public native void closeSession(byte[] bArr);

    public native KeyRequest getKeyRequest(byte[] bArr, byte[] bArr2, String str, int i, HashMap<String, String> map) throws NotProvisionedException;

    public native byte[] getPropertyByteArray(String str);

    public native String getPropertyString(String str);

    public native byte[] getSecureStop(byte[] bArr);

    public native List<byte[]> getSecureStops();

    public native byte[] openSession() throws ResourceBusyException, NotProvisionedException;

    public native byte[] provideKeyResponse(byte[] bArr, byte[] bArr2) throws DeniedByServerException, NotProvisionedException;

    public native HashMap<String, String> queryKeyStatus(byte[] bArr);

    public final native void release();

    public native void releaseAllSecureStops();

    public native void releaseSecureStops(byte[] bArr);

    public native void removeKeys(byte[] bArr);

    public native void restoreKeys(byte[] bArr, byte[] bArr2);

    public native void setPropertyByteArray(String str, byte[] bArr);

    public native void setPropertyString(String str, String str2);

    public native void unprovisionDevice();

    public static final boolean isCryptoSchemeSupported(UUID uuid) {
        return isCryptoSchemeSupportedNative(getByteArrayFromUUID(uuid), null);
    }

    public static final boolean isCryptoSchemeSupported(UUID uuid, String mimeType) {
        return isCryptoSchemeSupportedNative(getByteArrayFromUUID(uuid), mimeType);
    }

    private static final byte[] getByteArrayFromUUID(UUID uuid) {
        long msb = uuid.getMostSignificantBits();
        long lsb = uuid.getLeastSignificantBits();
        byte[] uuidBytes = new byte[16];
        for (int i = 0; i < 8; i++) {
            uuidBytes[i] = (byte) (msb >>> ((7 - i) * 8));
            uuidBytes[i + 8] = (byte) (lsb >>> ((7 - i) * 8));
        }
        return uuidBytes;
    }

    public MediaDrm(UUID uuid) throws UnsupportedSchemeException {
        Looper looper = Looper.myLooper();
        if (looper != null) {
            this.mEventHandler = new EventHandler(this, looper);
        } else {
            Looper looper2 = Looper.getMainLooper();
            if (looper2 != null) {
                this.mEventHandler = new EventHandler(this, looper2);
            } else {
                this.mEventHandler = null;
            }
        }
        native_setup(new WeakReference(this), getByteArrayFromUUID(uuid));
    }

    public static final class MediaDrmStateException extends IllegalStateException {
        private final String mDiagnosticInfo;
        private final int mErrorCode;

        public MediaDrmStateException(int errorCode, String detailMessage) {
            super(detailMessage);
            this.mErrorCode = errorCode;
            String sign = errorCode < 0 ? "neg_" : ProxyInfo.LOCAL_EXCL_LIST;
            this.mDiagnosticInfo = "android.media.MediaDrm.error_" + sign + Math.abs(errorCode);
        }

        public int getErrorCode() {
            return this.mErrorCode;
        }

        public String getDiagnosticInfo() {
            return this.mDiagnosticInfo;
        }
    }

    public void setOnEventListener(OnEventListener listener) {
        this.mOnEventListener = listener;
    }

    private class EventHandler extends Handler {
        private MediaDrm mMediaDrm;

        public EventHandler(MediaDrm md, Looper looper) {
            super(looper);
            this.mMediaDrm = md;
        }

        @Override
        public void handleMessage(Message msg) {
            if (this.mMediaDrm.mNativeContext == 0) {
                Log.w(MediaDrm.TAG, "MediaDrm went away with unhandled events");
            }
            switch (msg.what) {
                case 200:
                    Log.i(MediaDrm.TAG, "Drm event (" + msg.arg1 + "," + msg.arg2 + ")");
                    if (MediaDrm.this.mOnEventListener != null && msg.obj != null && (msg.obj instanceof Parcel)) {
                        Parcel parcel = (Parcel) msg.obj;
                        byte[] sessionId = parcel.createByteArray();
                        if (sessionId.length == 0) {
                            sessionId = null;
                        }
                        byte[] data = parcel.createByteArray();
                        if (data.length == 0) {
                            data = null;
                        }
                        MediaDrm.this.mOnEventListener.onEvent(this.mMediaDrm, sessionId, msg.arg1, msg.arg2, data);
                        break;
                    }
                    break;
                default:
                    Log.e(MediaDrm.TAG, "Unknown message type " + msg.what);
                    break;
            }
        }
    }

    private static void postEventFromNative(Object mediadrm_ref, int eventType, int extra, Object obj) {
        MediaDrm md = (MediaDrm) ((WeakReference) mediadrm_ref).get();
        if (md != null && md.mEventHandler != null) {
            Message m = md.mEventHandler.obtainMessage(200, eventType, extra, obj);
            md.mEventHandler.sendMessage(m);
        }
    }

    public static final class KeyRequest {
        private byte[] mData;
        private String mDefaultUrl;

        KeyRequest() {
        }

        public byte[] getData() {
            return this.mData;
        }

        public String getDefaultUrl() {
            return this.mDefaultUrl;
        }
    }

    public static final class ProvisionRequest {
        private byte[] mData;
        private String mDefaultUrl;

        ProvisionRequest() {
        }

        public byte[] getData() {
            return this.mData;
        }

        public String getDefaultUrl() {
            return this.mDefaultUrl;
        }
    }

    public ProvisionRequest getProvisionRequest() {
        return getProvisionRequestNative(0, ProxyInfo.LOCAL_EXCL_LIST);
    }

    public void provideProvisionResponse(byte[] response) throws DeniedByServerException {
        provideProvisionResponseNative(response);
    }

    public final class CryptoSession {
        private MediaDrm mDrm;
        private byte[] mSessionId;

        CryptoSession(MediaDrm drm, byte[] sessionId, String cipherAlgorithm, String macAlgorithm) {
            this.mSessionId = sessionId;
            this.mDrm = drm;
            MediaDrm.setCipherAlgorithmNative(drm, sessionId, cipherAlgorithm);
            MediaDrm.setMacAlgorithmNative(drm, sessionId, macAlgorithm);
        }

        public byte[] encrypt(byte[] keyid, byte[] input, byte[] iv) {
            return MediaDrm.encryptNative(this.mDrm, this.mSessionId, keyid, input, iv);
        }

        public byte[] decrypt(byte[] keyid, byte[] input, byte[] iv) {
            return MediaDrm.decryptNative(this.mDrm, this.mSessionId, keyid, input, iv);
        }

        public byte[] sign(byte[] keyid, byte[] message) {
            return MediaDrm.signNative(this.mDrm, this.mSessionId, keyid, message);
        }

        public boolean verify(byte[] keyid, byte[] message, byte[] signature) {
            return MediaDrm.verifyNative(this.mDrm, this.mSessionId, keyid, message, signature);
        }
    }

    public CryptoSession getCryptoSession(byte[] sessionId, String cipherAlgorithm, String macAlgorithm) {
        return new CryptoSession(this, sessionId, cipherAlgorithm, macAlgorithm);
    }

    public static final class CertificateRequest {
        private byte[] mData;
        private String mDefaultUrl;

        CertificateRequest(byte[] data, String defaultUrl) {
            this.mData = data;
            this.mDefaultUrl = defaultUrl;
        }

        public byte[] getData() {
            return this.mData;
        }

        public String getDefaultUrl() {
            return this.mDefaultUrl;
        }
    }

    public CertificateRequest getCertificateRequest(int certType, String certAuthority) {
        ProvisionRequest provisionRequest = getProvisionRequestNative(certType, certAuthority);
        return new CertificateRequest(provisionRequest.getData(), provisionRequest.getDefaultUrl());
    }

    public static final class Certificate {
        private byte[] mCertificateData;
        private byte[] mWrappedKey;

        Certificate() {
        }

        public byte[] getWrappedPrivateKey() {
            return this.mWrappedKey;
        }

        public byte[] getContent() {
            return this.mCertificateData;
        }
    }

    public Certificate provideCertificateResponse(byte[] response) throws DeniedByServerException {
        return provideProvisionResponseNative(response);
    }

    public byte[] signRSA(byte[] sessionId, String algorithm, byte[] wrappedKey, byte[] message) {
        return signRSANative(this, sessionId, algorithm, wrappedKey, message);
    }

    protected void finalize() {
        native_finalize();
    }

    static {
        System.loadLibrary("media_jni");
        native_init();
    }
}
