package android.media;

import android.app.ActivityThread;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.os.Parcel;
import android.os.PersistableBundle;
import android.util.Log;
import dalvik.system.CloseGuard;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;

public final class MediaDrm implements AutoCloseable {
    public static final int CERTIFICATE_TYPE_NONE = 0;
    public static final int CERTIFICATE_TYPE_X509 = 1;
    private static final int DRM_EVENT = 200;
    public static final int EVENT_KEY_EXPIRED = 3;
    public static final int EVENT_KEY_REQUIRED = 2;
    public static final int EVENT_PROVISION_REQUIRED = 1;
    public static final int EVENT_SESSION_RECLAIMED = 5;
    public static final int EVENT_VENDOR_DEFINED = 4;
    private static final int EXPIRATION_UPDATE = 201;
    public static final int HDCP_LEVEL_UNKNOWN = 0;
    public static final int HDCP_NONE = 1;
    public static final int HDCP_NO_DIGITAL_OUTPUT = Integer.MAX_VALUE;
    public static final int HDCP_V1 = 2;
    public static final int HDCP_V2 = 3;
    public static final int HDCP_V2_1 = 4;
    public static final int HDCP_V2_2 = 5;
    private static final int KEY_STATUS_CHANGE = 202;
    public static final int KEY_TYPE_OFFLINE = 2;
    public static final int KEY_TYPE_RELEASE = 3;
    public static final int KEY_TYPE_STREAMING = 1;
    private static final String PERMISSION = "android.permission.ACCESS_DRM_CERTIFICATES";
    public static final String PROPERTY_ALGORITHMS = "algorithms";
    public static final String PROPERTY_DESCRIPTION = "description";
    public static final String PROPERTY_DEVICE_UNIQUE_ID = "deviceUniqueId";
    public static final String PROPERTY_VENDOR = "vendor";
    public static final String PROPERTY_VERSION = "version";
    public static final int SECURITY_LEVEL_HW_SECURE_ALL = 5;
    public static final int SECURITY_LEVEL_HW_SECURE_CRYPTO = 3;
    public static final int SECURITY_LEVEL_HW_SECURE_DECODE = 4;
    public static final int SECURITY_LEVEL_MAX = 6;
    public static final int SECURITY_LEVEL_SW_SECURE_CRYPTO = 1;
    public static final int SECURITY_LEVEL_SW_SECURE_DECODE = 2;
    public static final int SECURITY_LEVEL_UNKNOWN = 0;
    private static final String TAG = "MediaDrm";
    private EventHandler mEventHandler;
    private long mNativeContext;
    private OnEventListener mOnEventListener;
    private EventHandler mOnExpirationUpdateEventHandler;
    private OnExpirationUpdateListener mOnExpirationUpdateListener;
    private EventHandler mOnKeyStatusChangeEventHandler;
    private OnKeyStatusChangeListener mOnKeyStatusChangeListener;
    private final AtomicBoolean mClosed = new AtomicBoolean();
    private final CloseGuard mCloseGuard = CloseGuard.get();

    @Retention(RetentionPolicy.SOURCE)
    public @interface ArrayProperty {
    }

    @Retention(RetentionPolicy.SOURCE)
    public @interface CertificateType {
    }

    @Retention(RetentionPolicy.SOURCE)
    public @interface DrmEvent {
    }

    @Retention(RetentionPolicy.SOURCE)
    public @interface HdcpLevel {
    }

    @Retention(RetentionPolicy.SOURCE)
    public @interface KeyType {
    }

    public interface OnEventListener {
        void onEvent(MediaDrm mediaDrm, byte[] bArr, int i, int i2, byte[] bArr2);
    }

    public interface OnExpirationUpdateListener {
        void onExpirationUpdate(MediaDrm mediaDrm, byte[] bArr, long j);
    }

    public interface OnKeyStatusChangeListener {
        void onKeyStatusChange(MediaDrm mediaDrm, byte[] bArr, List<KeyStatus> list, boolean z);
    }

    @Retention(RetentionPolicy.SOURCE)
    public @interface SecurityLevel {
    }

    @Retention(RetentionPolicy.SOURCE)
    public @interface StringProperty {
    }

    private static final native byte[] decryptNative(MediaDrm mediaDrm, byte[] bArr, byte[] bArr2, byte[] bArr3, byte[] bArr4);

    private static final native byte[] encryptNative(MediaDrm mediaDrm, byte[] bArr, byte[] bArr2, byte[] bArr3, byte[] bArr4);

    private native PersistableBundle getMetricsNative();

    private native ProvisionRequest getProvisionRequestNative(int i, String str);

    private static final native boolean isCryptoSchemeSupportedNative(byte[] bArr, String str);

    private static final native void native_init();

    private final native void native_setup(Object obj, byte[] bArr, String str);

    private native Certificate provideProvisionResponseNative(byte[] bArr) throws DeniedByServerException;

    private static final native void setCipherAlgorithmNative(MediaDrm mediaDrm, byte[] bArr, String str);

    private static final native void setMacAlgorithmNative(MediaDrm mediaDrm, byte[] bArr, String str);

    private static final native byte[] signNative(MediaDrm mediaDrm, byte[] bArr, byte[] bArr2, byte[] bArr3);

    private static final native byte[] signRSANative(MediaDrm mediaDrm, byte[] bArr, String str, byte[] bArr2, byte[] bArr3);

    private static final native boolean verifyNative(MediaDrm mediaDrm, byte[] bArr, byte[] bArr2, byte[] bArr3, byte[] bArr4);

    public native void closeSession(byte[] bArr);

    public native int getConnectedHdcpLevel();

    public native KeyRequest getKeyRequest(byte[] bArr, byte[] bArr2, String str, int i, HashMap<String, String> map) throws NotProvisionedException;

    public native int getMaxHdcpLevel();

    public native int getMaxSessionCount();

    public native int getOpenSessionCount();

    public native byte[] getPropertyByteArray(String str);

    public native String getPropertyString(String str);

    public native byte[] getSecureStop(byte[] bArr);

    public native List<byte[]> getSecureStopIds();

    public native List<byte[]> getSecureStops();

    public native int getSecurityLevel(byte[] bArr);

    public final native void native_release();

    public native byte[] openSession(int i) throws ResourceBusyException, NotProvisionedException;

    public native byte[] provideKeyResponse(byte[] bArr, byte[] bArr2) throws DeniedByServerException, NotProvisionedException;

    public native HashMap<String, String> queryKeyStatus(byte[] bArr);

    public native void releaseSecureStops(byte[] bArr);

    public native void removeAllSecureStops();

    public native void removeKeys(byte[] bArr);

    public native void removeSecureStop(byte[] bArr);

    public native void restoreKeys(byte[] bArr, byte[] bArr2);

    public native void setPropertyByteArray(String str, byte[] bArr);

    public native void setPropertyString(String str, String str2);

    public static final boolean isCryptoSchemeSupported(UUID uuid) {
        return isCryptoSchemeSupportedNative(getByteArrayFromUUID(uuid), null);
    }

    public static final boolean isCryptoSchemeSupported(UUID uuid, String str) {
        return isCryptoSchemeSupportedNative(getByteArrayFromUUID(uuid), str);
    }

    private static final byte[] getByteArrayFromUUID(UUID uuid) {
        long mostSignificantBits = uuid.getMostSignificantBits();
        long leastSignificantBits = uuid.getLeastSignificantBits();
        byte[] bArr = new byte[16];
        for (int i = 0; i < 8; i++) {
            int i2 = (7 - i) * 8;
            bArr[i] = (byte) (mostSignificantBits >>> i2);
            bArr[8 + i] = (byte) (leastSignificantBits >>> i2);
        }
        return bArr;
    }

    public MediaDrm(UUID uuid) throws UnsupportedSchemeException {
        Looper looperMyLooper = Looper.myLooper();
        if (looperMyLooper != null) {
            this.mEventHandler = new EventHandler(this, looperMyLooper);
        } else {
            Looper mainLooper = Looper.getMainLooper();
            if (mainLooper != null) {
                this.mEventHandler = new EventHandler(this, mainLooper);
            } else {
                this.mEventHandler = null;
            }
        }
        native_setup(new WeakReference(this), getByteArrayFromUUID(uuid), ActivityThread.currentOpPackageName());
        this.mCloseGuard.open("release");
    }

    public static final class MediaDrmStateException extends IllegalStateException {
        private final String mDiagnosticInfo;
        private final int mErrorCode;

        public MediaDrmStateException(int i, String str) {
            super(str);
            this.mErrorCode = i;
            this.mDiagnosticInfo = "android.media.MediaDrm.error_" + (i < 0 ? "neg_" : "") + Math.abs(i);
        }

        public int getErrorCode() {
            return this.mErrorCode;
        }

        public String getDiagnosticInfo() {
            return this.mDiagnosticInfo;
        }
    }

    public void setOnExpirationUpdateListener(OnExpirationUpdateListener onExpirationUpdateListener, Handler handler) {
        if (onExpirationUpdateListener != null) {
            Looper looper = handler != null ? handler.getLooper() : Looper.myLooper();
            if (looper != null && (this.mEventHandler == null || this.mEventHandler.getLooper() != looper)) {
                this.mEventHandler = new EventHandler(this, looper);
            }
        }
        this.mOnExpirationUpdateListener = onExpirationUpdateListener;
    }

    public void setOnKeyStatusChangeListener(OnKeyStatusChangeListener onKeyStatusChangeListener, Handler handler) {
        if (onKeyStatusChangeListener != null) {
            Looper looper = handler != null ? handler.getLooper() : Looper.myLooper();
            if (looper != null && (this.mEventHandler == null || this.mEventHandler.getLooper() != looper)) {
                this.mEventHandler = new EventHandler(this, looper);
            }
        }
        this.mOnKeyStatusChangeListener = onKeyStatusChangeListener;
    }

    public static final class KeyStatus {
        public static final int STATUS_EXPIRED = 1;
        public static final int STATUS_INTERNAL_ERROR = 4;
        public static final int STATUS_OUTPUT_NOT_ALLOWED = 2;
        public static final int STATUS_PENDING = 3;
        public static final int STATUS_USABLE = 0;
        private final byte[] mKeyId;
        private final int mStatusCode;

        @Retention(RetentionPolicy.SOURCE)
        public @interface KeyStatusCode {
        }

        KeyStatus(byte[] bArr, int i) {
            this.mKeyId = bArr;
            this.mStatusCode = i;
        }

        public int getStatusCode() {
            return this.mStatusCode;
        }

        public byte[] getKeyId() {
            return this.mKeyId;
        }
    }

    public void setOnEventListener(OnEventListener onEventListener) {
        this.mOnEventListener = onEventListener;
    }

    private class EventHandler extends Handler {
        private MediaDrm mMediaDrm;

        public EventHandler(MediaDrm mediaDrm, Looper looper) {
            super(looper);
            this.mMediaDrm = mediaDrm;
        }

        @Override
        public void handleMessage(Message message) {
            if (this.mMediaDrm.mNativeContext != 0) {
                switch (message.what) {
                    case 200:
                        if (MediaDrm.this.mOnEventListener != null && message.obj != null && (message.obj instanceof Parcel)) {
                            Parcel parcel = (Parcel) message.obj;
                            byte[] bArrCreateByteArray = parcel.createByteArray();
                            byte[] bArr = bArrCreateByteArray.length == 0 ? null : bArrCreateByteArray;
                            byte[] bArrCreateByteArray2 = parcel.createByteArray();
                            byte[] bArr2 = bArrCreateByteArray2.length == 0 ? null : bArrCreateByteArray2;
                            Log.i(MediaDrm.TAG, "Drm event (" + message.arg1 + "," + message.arg2 + ")");
                            MediaDrm.this.mOnEventListener.onEvent(this.mMediaDrm, bArr, message.arg1, message.arg2, bArr2);
                            break;
                        }
                        break;
                    case 201:
                        if (MediaDrm.this.mOnExpirationUpdateListener != null && message.obj != null && (message.obj instanceof Parcel)) {
                            Parcel parcel2 = (Parcel) message.obj;
                            byte[] bArrCreateByteArray3 = parcel2.createByteArray();
                            if (bArrCreateByteArray3.length > 0) {
                                long j = parcel2.readLong();
                                Log.i(MediaDrm.TAG, "Drm key expiration update: " + j);
                                MediaDrm.this.mOnExpirationUpdateListener.onExpirationUpdate(this.mMediaDrm, bArrCreateByteArray3, j);
                            }
                            break;
                        }
                        break;
                    case 202:
                        if (MediaDrm.this.mOnKeyStatusChangeListener != null && message.obj != null && (message.obj instanceof Parcel)) {
                            Parcel parcel3 = (Parcel) message.obj;
                            byte[] bArrCreateByteArray4 = parcel3.createByteArray();
                            if (bArrCreateByteArray4.length > 0) {
                                List<KeyStatus> listKeyStatusListFromParcel = MediaDrm.this.keyStatusListFromParcel(parcel3);
                                boolean z = parcel3.readInt() != 0;
                                Log.i(MediaDrm.TAG, "Drm key status changed");
                                MediaDrm.this.mOnKeyStatusChangeListener.onKeyStatusChange(this.mMediaDrm, bArrCreateByteArray4, listKeyStatusListFromParcel, z);
                            }
                            break;
                        }
                        break;
                    default:
                        Log.e(MediaDrm.TAG, "Unknown message type " + message.what);
                        break;
                }
            }
            Log.w(MediaDrm.TAG, "MediaDrm went away with unhandled events");
        }
    }

    private List<KeyStatus> keyStatusListFromParcel(Parcel parcel) {
        int i = parcel.readInt();
        ArrayList arrayList = new ArrayList(i);
        while (true) {
            int i2 = i - 1;
            if (i > 0) {
                arrayList.add(new KeyStatus(parcel.createByteArray(), parcel.readInt()));
                i = i2;
            } else {
                return arrayList;
            }
        }
    }

    private static void postEventFromNative(Object obj, int i, int i2, int i3, Object obj2) {
        MediaDrm mediaDrm = (MediaDrm) ((WeakReference) obj).get();
        if (mediaDrm != null && mediaDrm.mEventHandler != null) {
            mediaDrm.mEventHandler.sendMessage(mediaDrm.mEventHandler.obtainMessage(i, i2, i3, obj2));
        }
    }

    public byte[] openSession() throws ResourceBusyException, NotProvisionedException {
        return openSession(getMaxSecurityLevel());
    }

    public static final class KeyRequest {
        public static final int REQUEST_TYPE_INITIAL = 0;
        public static final int REQUEST_TYPE_NONE = 3;
        public static final int REQUEST_TYPE_RELEASE = 2;
        public static final int REQUEST_TYPE_RENEWAL = 1;
        public static final int REQUEST_TYPE_UPDATE = 4;
        private byte[] mData;
        private String mDefaultUrl;
        private int mRequestType;

        @Retention(RetentionPolicy.SOURCE)
        public @interface RequestType {
        }

        KeyRequest() {
        }

        public byte[] getData() {
            if (this.mData == null) {
                throw new RuntimeException("KeyRequest is not initialized");
            }
            return this.mData;
        }

        public String getDefaultUrl() {
            if (this.mDefaultUrl == null) {
                throw new RuntimeException("KeyRequest is not initialized");
            }
            return this.mDefaultUrl;
        }

        public int getRequestType() {
            return this.mRequestType;
        }
    }

    public static final class ProvisionRequest {
        private byte[] mData;
        private String mDefaultUrl;

        ProvisionRequest() {
        }

        public byte[] getData() {
            if (this.mData == null) {
                throw new RuntimeException("ProvisionRequest is not initialized");
            }
            return this.mData;
        }

        public String getDefaultUrl() {
            if (this.mDefaultUrl == null) {
                throw new RuntimeException("ProvisionRequest is not initialized");
            }
            return this.mDefaultUrl;
        }
    }

    public ProvisionRequest getProvisionRequest() {
        return getProvisionRequestNative(0, "");
    }

    public void provideProvisionResponse(byte[] bArr) throws DeniedByServerException {
        provideProvisionResponseNative(bArr);
    }

    public void releaseAllSecureStops() {
        removeAllSecureStops();
    }

    public static final int getMaxSecurityLevel() {
        return 6;
    }

    public PersistableBundle getMetrics() {
        return getMetricsNative();
    }

    public final class CryptoSession {
        private byte[] mSessionId;

        CryptoSession(byte[] bArr, String str, String str2) {
            this.mSessionId = bArr;
            MediaDrm.setCipherAlgorithmNative(MediaDrm.this, bArr, str);
            MediaDrm.setMacAlgorithmNative(MediaDrm.this, bArr, str2);
        }

        public byte[] encrypt(byte[] bArr, byte[] bArr2, byte[] bArr3) {
            return MediaDrm.encryptNative(MediaDrm.this, this.mSessionId, bArr, bArr2, bArr3);
        }

        public byte[] decrypt(byte[] bArr, byte[] bArr2, byte[] bArr3) {
            return MediaDrm.decryptNative(MediaDrm.this, this.mSessionId, bArr, bArr2, bArr3);
        }

        public byte[] sign(byte[] bArr, byte[] bArr2) {
            return MediaDrm.signNative(MediaDrm.this, this.mSessionId, bArr, bArr2);
        }

        public boolean verify(byte[] bArr, byte[] bArr2, byte[] bArr3) {
            return MediaDrm.verifyNative(MediaDrm.this, this.mSessionId, bArr, bArr2, bArr3);
        }
    }

    public CryptoSession getCryptoSession(byte[] bArr, String str, String str2) {
        return new CryptoSession(bArr, str, str2);
    }

    public static final class CertificateRequest {
        private byte[] mData;
        private String mDefaultUrl;

        CertificateRequest(byte[] bArr, String str) {
            this.mData = bArr;
            this.mDefaultUrl = str;
        }

        public byte[] getData() {
            return this.mData;
        }

        public String getDefaultUrl() {
            return this.mDefaultUrl;
        }
    }

    public CertificateRequest getCertificateRequest(int i, String str) {
        ProvisionRequest provisionRequestNative = getProvisionRequestNative(i, str);
        return new CertificateRequest(provisionRequestNative.getData(), provisionRequestNative.getDefaultUrl());
    }

    public static final class Certificate {
        private byte[] mCertificateData;
        private byte[] mWrappedKey;

        Certificate() {
        }

        public byte[] getWrappedPrivateKey() {
            if (this.mWrappedKey == null) {
                throw new RuntimeException("Cerfificate is not initialized");
            }
            return this.mWrappedKey;
        }

        public byte[] getContent() {
            if (this.mCertificateData == null) {
                throw new RuntimeException("Cerfificate is not initialized");
            }
            return this.mCertificateData;
        }
    }

    public Certificate provideCertificateResponse(byte[] bArr) throws DeniedByServerException {
        return provideProvisionResponseNative(bArr);
    }

    public byte[] signRSA(byte[] bArr, String str, byte[] bArr2, byte[] bArr3) {
        return signRSANative(this, bArr, str, bArr2, bArr3);
    }

    protected void finalize() throws Throwable {
        try {
            if (this.mCloseGuard != null) {
                this.mCloseGuard.warnIfOpen();
            }
            release();
        } finally {
            super.finalize();
        }
    }

    @Override
    public void close() {
        release();
    }

    @Deprecated
    public void release() {
        this.mCloseGuard.close();
        if (this.mClosed.compareAndSet(false, true)) {
            native_release();
        }
    }

    static {
        System.loadLibrary("media_jni");
        native_init();
    }

    public static final class MetricsConstants {
        public static final String CLOSE_SESSION_ERROR_COUNT = "drm.mediadrm.close_session.error.count";
        public static final String CLOSE_SESSION_ERROR_LIST = "drm.mediadrm.close_session.error.list";
        public static final String CLOSE_SESSION_OK_COUNT = "drm.mediadrm.close_session.ok.count";
        public static final String EVENT_KEY_EXPIRED_COUNT = "drm.mediadrm.event.KEY_EXPIRED.count";
        public static final String EVENT_KEY_NEEDED_COUNT = "drm.mediadrm.event.KEY_NEEDED.count";
        public static final String EVENT_PROVISION_REQUIRED_COUNT = "drm.mediadrm.event.PROVISION_REQUIRED.count";
        public static final String EVENT_SESSION_RECLAIMED_COUNT = "drm.mediadrm.event.SESSION_RECLAIMED.count";
        public static final String EVENT_VENDOR_DEFINED_COUNT = "drm.mediadrm.event.VENDOR_DEFINED.count";
        public static final String GET_DEVICE_UNIQUE_ID_ERROR_COUNT = "drm.mediadrm.get_device_unique_id.error.count";
        public static final String GET_DEVICE_UNIQUE_ID_ERROR_LIST = "drm.mediadrm.get_device_unique_id.error.list";
        public static final String GET_DEVICE_UNIQUE_ID_OK_COUNT = "drm.mediadrm.get_device_unique_id.ok.count";
        public static final String GET_KEY_REQUEST_ERROR_COUNT = "drm.mediadrm.get_key_request.error.count";
        public static final String GET_KEY_REQUEST_ERROR_LIST = "drm.mediadrm.get_key_request.error.list";
        public static final String GET_KEY_REQUEST_OK_COUNT = "drm.mediadrm.get_key_request.ok.count";
        public static final String GET_KEY_REQUEST_OK_TIME_MICROS = "drm.mediadrm.get_key_request.ok.average_time_micros";
        public static final String GET_PROVISION_REQUEST_ERROR_COUNT = "drm.mediadrm.get_provision_request.error.count";
        public static final String GET_PROVISION_REQUEST_ERROR_LIST = "drm.mediadrm.get_provision_request.error.list";
        public static final String GET_PROVISION_REQUEST_OK_COUNT = "drm.mediadrm.get_provision_request.ok.count";
        public static final String KEY_STATUS_EXPIRED_COUNT = "drm.mediadrm.key_status.EXPIRED.count";
        public static final String KEY_STATUS_INTERNAL_ERROR_COUNT = "drm.mediadrm.key_status.INTERNAL_ERROR.count";
        public static final String KEY_STATUS_OUTPUT_NOT_ALLOWED_COUNT = "drm.mediadrm.key_status_change.OUTPUT_NOT_ALLOWED.count";
        public static final String KEY_STATUS_PENDING_COUNT = "drm.mediadrm.key_status_change.PENDING.count";
        public static final String KEY_STATUS_USABLE_COUNT = "drm.mediadrm.key_status_change.USABLE.count";
        public static final String OPEN_SESSION_ERROR_COUNT = "drm.mediadrm.open_session.error.count";
        public static final String OPEN_SESSION_ERROR_LIST = "drm.mediadrm.open_session.error.list";
        public static final String OPEN_SESSION_OK_COUNT = "drm.mediadrm.open_session.ok.count";
        public static final String PROVIDE_KEY_RESPONSE_ERROR_COUNT = "drm.mediadrm.provide_key_response.error.count";
        public static final String PROVIDE_KEY_RESPONSE_ERROR_LIST = "drm.mediadrm.provide_key_response.error.list";
        public static final String PROVIDE_KEY_RESPONSE_OK_COUNT = "drm.mediadrm.provide_key_response.ok.count";
        public static final String PROVIDE_KEY_RESPONSE_OK_TIME_MICROS = "drm.mediadrm.provide_key_response.ok.average_time_micros";
        public static final String PROVIDE_PROVISION_RESPONSE_ERROR_COUNT = "drm.mediadrm.provide_provision_response.error.count";
        public static final String PROVIDE_PROVISION_RESPONSE_ERROR_LIST = "drm.mediadrm.provide_provision_response.error.list";
        public static final String PROVIDE_PROVISION_RESPONSE_OK_COUNT = "drm.mediadrm.provide_provision_response.ok.count";
        public static final String SESSION_END_TIMES_MS = "drm.mediadrm.session_end_times_ms";
        public static final String SESSION_START_TIMES_MS = "drm.mediadrm.session_start_times_ms";

        private MetricsConstants() {
        }
    }
}
