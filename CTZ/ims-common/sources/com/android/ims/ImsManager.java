package com.android.ims;

import android.R;
import android.content.Context;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.os.Parcel;
import android.os.PersistableBundle;
import android.os.RemoteException;
import android.os.SystemProperties;
import android.telecom.TelecomManager;
import android.telephony.CarrierConfigManager;
import android.telephony.Rlog;
import android.telephony.SubscriptionManager;
import android.telephony.TelephonyManager;
import android.telephony.ims.ImsCallProfile;
import android.telephony.ims.ImsCallSession;
import android.telephony.ims.ImsReasonInfo;
import android.telephony.ims.aidl.IImsConfig;
import android.telephony.ims.aidl.IImsSmsListener;
import android.telephony.ims.feature.CapabilityChangeRequest;
import android.telephony.ims.feature.ImsFeature;
import android.telephony.ims.feature.MmTelFeature;
import android.telephony.ims.stub.ImsRegistrationImplBase;
import android.util.Log;
import com.android.ims.ImsCall;
import com.android.ims.ImsManager;
import com.android.ims.MmTelFeatureConnection;
import com.android.ims.internal.IImsCallSession;
import com.android.ims.internal.IImsEcbm;
import com.android.ims.internal.IImsMultiEndpoint;
import com.android.ims.internal.IImsUt;
import com.android.internal.annotations.VisibleForTesting;
import java.io.FileDescriptor;
import java.io.PrintWriter;
import java.lang.reflect.Constructor;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Set;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.function.Consumer;

public class ImsManager {
    public static final String ACTION_IMS_INCOMING_CALL = "com.android.ims.IMS_INCOMING_CALL";
    public static final String ACTION_IMS_REGISTRATION_ERROR = "com.android.ims.REGISTRATION_ERROR";
    public static final String ACTION_IMS_SERVICE_DOWN = "com.android.ims.IMS_SERVICE_DOWN";
    public static final String ACTION_IMS_SERVICE_UP = "com.android.ims.IMS_SERVICE_UP";
    private static final boolean DBG = true;
    public static final String EXTRA_CALL_ID = "android:imsCallID";
    public static final String EXTRA_IS_UNKNOWN_CALL = "android:isUnknown";
    public static final String EXTRA_PHONE_ID = "android:phone_id";
    public static final String EXTRA_SERVICE_ID = "android:imsServiceId";
    public static final String EXTRA_USSD = "android:ussd";
    public static final String FALSE = "false";
    public static final int INCOMING_CALL_RESULT_CODE = 101;
    private static final int MAX_RECENT_DISCONNECT_REASONS = 16;
    public static final String PROPERTY_DBG_ALLOW_IMS_OFF_OVERRIDE = "persist.dbg.allow_ims_off";
    public static final int PROPERTY_DBG_ALLOW_IMS_OFF_OVERRIDE_DEFAULT = 0;
    public static final String PROPERTY_DBG_VOLTE_AVAIL_OVERRIDE = "persist.dbg.volte_avail_ovr";
    public static final int PROPERTY_DBG_VOLTE_AVAIL_OVERRIDE_DEFAULT = 0;
    public static final String PROPERTY_DBG_VT_AVAIL_OVERRIDE = "persist.dbg.vt_avail_ovr";
    public static final int PROPERTY_DBG_VT_AVAIL_OVERRIDE_DEFAULT = 0;
    public static final String PROPERTY_DBG_WFC_AVAIL_OVERRIDE = "persist.dbg.wfc_avail_ovr";
    public static final int PROPERTY_DBG_WFC_AVAIL_OVERRIDE_DEFAULT = 0;
    protected static final int SUB_PROPERTY_NOT_INITIALIZED = -1;
    protected static final int SYSTEM_PROPERTY_NOT_SET = -1;
    private static final String TAG = "ImsManager";
    public static final String TRUE = "true";
    private static HashMap<Integer, ImsManager> sImsManagerInstances = new HashMap<>();
    private static HashMap<Integer, Handler> sMainThreadHandlerInstances = new HashMap<>();
    private final boolean mConfigDynamicBind;
    private CarrierConfigManager mConfigManager;
    protected Context mContext;
    protected ImsConfigListener mImsConfigListener;
    protected int mPhoneId;
    protected MmTelFeatureConnection mMmTelFeatureConnection = null;
    private boolean mConfigUpdated = false;
    private ImsUt mUt = null;
    protected ImsEcbm mEcbm = null;
    private ImsMultiEndpoint mMultiEndpoint = null;
    private Set<MmTelFeatureConnection.IFeatureUpdate> mStatusCallbacks = new CopyOnWriteArraySet();
    private ConcurrentLinkedDeque<ImsReasonInfo> mRecentDisconnectReasons = new ConcurrentLinkedDeque<>();

    public static class Connector extends Handler {
        private static final int CEILING_SERVICE_RETRY_COUNT = 6;
        private static final int IMS_RETRY_STARTING_TIMEOUT_MS = 500;
        protected final Context mContext;
        private final Runnable mGetServiceRunnable;
        private ImsManager mImsManager;
        private final Listener mListener;
        private final Object mLock;
        private Handler mMainHandler;
        private final Runnable mNotifyStateChangedRunnable;
        private MmTelFeatureConnection.IFeatureUpdate mNotifyStatusChangedCallback;
        private final Runnable mNotifyUnavailableRunnable;
        protected final int mPhoneId;
        private int mRetryCount;

        @VisibleForTesting
        public RetryTimeout mRetryTimeout;

        public interface Listener {
            void connectionReady(ImsManager imsManager) throws ImsException;

            void connectionUnavailable();
        }

        @VisibleForTesting
        public interface RetryTimeout {
            int get();
        }

        public static void lambda$new$0(Connector connector) {
            try {
                connector.getImsService();
            } catch (ImsException e) {
                connector.retryGetImsService();
            }
        }

        public static int lambda$new$1(Connector connector) {
            int i;
            synchronized (connector.mLock) {
                i = (1 << connector.mRetryCount) * IMS_RETRY_STARTING_TIMEOUT_MS;
                if (connector.mRetryCount <= CEILING_SERVICE_RETRY_COUNT) {
                    connector.mRetryCount++;
                }
            }
            return i;
        }

        public Connector(Context context, int i, Listener listener) {
            this.mGetServiceRunnable = new Runnable() {
                @Override
                public final void run() {
                    ImsManager.Connector.lambda$new$0(this.f$0);
                }
            };
            this.mNotifyStatusChangedCallback = new MmTelFeatureConnection.IFeatureUpdate() {
                @Override
                public void notifyStateChanged() {
                    ImsManager.log("post a runnable for state changed notification");
                    Connector.this.mMainHandler.post(Connector.this.mNotifyStateChangedRunnable);
                }

                @Override
                public void notifyUnavailable() {
                    ImsManager.log("post a runnable for unavailable notification");
                    Connector.this.mMainHandler.post(Connector.this.mNotifyUnavailableRunnable);
                }
            };
            this.mLock = new Object();
            this.mRetryCount = 0;
            this.mRetryTimeout = new RetryTimeout() {
                @Override
                public final int get() {
                    return ImsManager.Connector.lambda$new$1(this.f$0);
                }
            };
            this.mMainHandler = null;
            this.mNotifyStateChangedRunnable = new Runnable() {
                @Override
                public final void run() {
                    ImsManager.Connector.lambda$new$2(this.f$0);
                }
            };
            this.mNotifyUnavailableRunnable = new Runnable() {
                @Override
                public final void run() {
                    ImsManager.Connector.lambda$new$3(this.f$0);
                }
            };
            this.mContext = context;
            this.mPhoneId = i;
            this.mListener = listener;
            this.mMainHandler = ImsManager.getMainThreadHandler(context, i);
        }

        public Connector(Context context, int i, Listener listener, Looper looper) {
            super(looper);
            this.mGetServiceRunnable = new Runnable() {
                @Override
                public final void run() {
                    ImsManager.Connector.lambda$new$0(this.f$0);
                }
            };
            this.mNotifyStatusChangedCallback = new MmTelFeatureConnection.IFeatureUpdate() {
                @Override
                public void notifyStateChanged() {
                    ImsManager.log("post a runnable for state changed notification");
                    Connector.this.mMainHandler.post(Connector.this.mNotifyStateChangedRunnable);
                }

                @Override
                public void notifyUnavailable() {
                    ImsManager.log("post a runnable for unavailable notification");
                    Connector.this.mMainHandler.post(Connector.this.mNotifyUnavailableRunnable);
                }
            };
            this.mLock = new Object();
            this.mRetryCount = 0;
            this.mRetryTimeout = new RetryTimeout() {
                @Override
                public final int get() {
                    return ImsManager.Connector.lambda$new$1(this.f$0);
                }
            };
            this.mMainHandler = null;
            this.mNotifyStateChangedRunnable = new Runnable() {
                @Override
                public final void run() {
                    ImsManager.Connector.lambda$new$2(this.f$0);
                }
            };
            this.mNotifyUnavailableRunnable = new Runnable() {
                @Override
                public final void run() {
                    ImsManager.Connector.lambda$new$3(this.f$0);
                }
            };
            this.mContext = context;
            this.mPhoneId = i;
            this.mListener = listener;
            this.mMainHandler = ImsManager.getMainThreadHandler(context, i);
        }

        public void connect() {
            this.mRetryCount = 0;
            post(this.mGetServiceRunnable);
        }

        public void disconnect() {
            removeCallbacks(this.mGetServiceRunnable);
            synchronized (this.mLock) {
                if (this.mImsManager != null) {
                    this.mImsManager.removeNotifyStatusChangedCallback(this.mNotifyStatusChangedCallback);
                }
            }
            notifyNotReady();
        }

        private void retryGetImsService() {
            synchronized (this.mLock) {
                this.mImsManager.removeNotifyStatusChangedCallback(this.mNotifyStatusChangedCallback);
                this.mImsManager = null;
            }
            ImsManager.loge("Connector: Retrying getting ImsService...");
            removeCallbacks(this.mGetServiceRunnable);
            postDelayed(this.mGetServiceRunnable, this.mRetryTimeout.get());
        }

        private void getImsService() throws ImsException {
            ImsManager.log("Connector: getImsService");
            synchronized (this.mLock) {
                this.mImsManager = ImsManager.getInstance(this.mContext, this.mPhoneId);
                this.mImsManager.addNotifyStatusChangedCallbackIfAvailable(this.mNotifyStatusChangedCallback);
            }
            this.mNotifyStatusChangedCallback.notifyStateChanged();
        }

        private void notifyReady() throws ImsException {
            ImsManager imsManager;
            synchronized (this.mLock) {
                imsManager = this.mImsManager;
            }
            try {
                this.mListener.connectionReady(imsManager);
                synchronized (this.mLock) {
                    this.mRetryCount = 0;
                }
            } catch (ImsException e) {
                Log.w(ImsManager.TAG, "Connector: notifyReady exception: " + e.getMessage());
                throw e;
            }
        }

        private void notifyNotReady() {
            this.mListener.connectionUnavailable();
        }

        public static void lambda$new$2(Connector connector) {
            int imsServiceState = 0;
            try {
                synchronized (connector.mLock) {
                    if (connector.mImsManager != null) {
                        imsServiceState = connector.mImsManager.getImsServiceState();
                    }
                }
                ImsManager.log("Status Changed: " + imsServiceState);
                switch (imsServiceState) {
                    case 0:
                    case 1:
                        connector.notifyNotReady();
                        return;
                    case 2:
                        connector.notifyReady();
                        return;
                    default:
                        ImsManager.log("Unexpected State!");
                        return;
                }
            } catch (ImsException e) {
                connector.notifyNotReady();
                connector.retryGetImsService();
            }
        }

        public static void lambda$new$3(Connector connector) {
            ImsManager.log("mNotifyUnavailableRunnable start!");
            connector.notifyNotReady();
            connector.retryGetImsService();
        }
    }

    private static Handler getMainThreadHandler(Context context, int i) {
        Handler handler;
        synchronized (sMainThreadHandlerInstances) {
            if (sMainThreadHandlerInstances.containsKey(Integer.valueOf(i))) {
                return sMainThreadHandlerInstances.get(Integer.valueOf(i));
            }
            if (context != null) {
                log("[" + i + "] Create main thread handler w/ context");
                handler = new Handler(context.getMainLooper());
            } else {
                log("[" + i + "] Create main thread handler w/o context");
                handler = new Handler(Looper.getMainLooper());
            }
            sMainThreadHandlerInstances.put(Integer.valueOf(i), handler);
            return handler;
        }
    }

    public static ImsManager getInstance(Context context, int i) {
        ImsManager imsManager;
        synchronized (sImsManagerInstances) {
            if (sImsManagerInstances.containsKey(Integer.valueOf(i))) {
                ImsManager imsManager2 = sImsManagerInstances.get(Integer.valueOf(i));
                if (imsManager2 != null) {
                    imsManager2.connectIfServiceIsAvailable();
                }
                return imsManager2;
            }
            try {
                Constructor<?> declaredConstructor = getMtkImsManager().getDeclaredConstructor(Context.class, Integer.TYPE);
                log("constructor function = " + declaredConstructor);
                declaredConstructor.setAccessible(DBG);
                imsManager = (ImsManager) declaredConstructor.newInstance(context, Integer.valueOf(i));
            } catch (NoSuchMethodException e) {
                loge("MtkImsManager Constructor not found! Use AOSP instead!");
                imsManager = new ImsManager(context, i);
            } catch (Exception e2) {
                loge("Exception at init MtkImsManager! Use AOSP for instead!");
                imsManager = new ImsManager(context, i);
            }
            sImsManagerInstances.put(Integer.valueOf(i), imsManager);
            return imsManager;
        }
    }

    public static boolean isEnhanced4gLteModeSettingEnabledByUser(Context context) {
        ImsManager imsManager = getInstance(context, SubscriptionManager.getDefaultVoicePhoneId());
        if (imsManager != null) {
            return imsManager.isEnhanced4gLteModeSettingEnabledByUser();
        }
        loge("isEnhanced4gLteModeSettingEnabledByUser: ImsManager null, returning default value.");
        return false;
    }

    public boolean isEnhanced4gLteModeSettingEnabledByUser() {
        int integerSubscriptionProperty = SubscriptionManager.getIntegerSubscriptionProperty(getSubId(), "volte_vt_enabled", -1, this.mContext);
        boolean booleanCarrierConfig = getBooleanCarrierConfig("enhanced_4g_lte_on_by_default_bool");
        if (!getBooleanCarrierConfig("editable_enhanced_4g_lte_bool") || integerSubscriptionProperty == -1) {
            return booleanCarrierConfig;
        }
        if (integerSubscriptionProperty == 1) {
            return DBG;
        }
        return false;
    }

    public static void setEnhanced4gLteModeSetting(Context context, boolean z) {
        ImsManager imsManager = getInstance(context, SubscriptionManager.getDefaultVoicePhoneId());
        if (imsManager != null) {
            imsManager.setEnhanced4gLteModeSetting(z);
        }
        loge("setEnhanced4gLteModeSetting: ImsManager null, value not set.");
    }

    public void setEnhanced4gLteModeSetting(boolean z) {
        int i;
        if (!getBooleanCarrierConfig("editable_enhanced_4g_lte_bool")) {
            z = getBooleanCarrierConfig("enhanced_4g_lte_on_by_default_bool");
        }
        int integerSubscriptionProperty = SubscriptionManager.getIntegerSubscriptionProperty(getSubId(), "volte_vt_enabled", -1, this.mContext);
        if (z) {
            i = 1;
        } else {
            i = 0;
        }
        if (integerSubscriptionProperty != i || shouldForceUpdated()) {
            SubscriptionManager.setSubscriptionProperty(getSubId(), "volte_vt_enabled", booleanToPropertyString(z));
            if (isNonTtyOrTtyOnVolteEnabled()) {
                try {
                    setAdvanced4GMode(z);
                } catch (ImsException e) {
                }
            }
        }
    }

    protected boolean shouldForceUpdated() {
        return false;
    }

    public static boolean isNonTtyOrTtyOnVolteEnabled(Context context) {
        ImsManager imsManager = getInstance(context, SubscriptionManager.getDefaultVoicePhoneId());
        if (imsManager != null) {
            return imsManager.isNonTtyOrTtyOnVolteEnabled();
        }
        loge("isNonTtyOrTtyOnVolteEnabled: ImsManager null, returning default value.");
        return false;
    }

    public boolean isNonTtyOrTtyOnVolteEnabled() {
        if (getBooleanCarrierConfig("carrier_volte_tty_supported_bool")) {
            return DBG;
        }
        TelecomManager telecomManager = (TelecomManager) this.mContext.getSystemService("telecom");
        if (telecomManager == null) {
            Log.w(TAG, "isNonTtyOrTtyOnVolteEnabled: telecom not available");
            return DBG;
        }
        if (telecomManager.getCurrentTtyMode() == 0) {
            return DBG;
        }
        return false;
    }

    public static boolean isVolteEnabledByPlatform(Context context) {
        ImsManager imsManager = getInstance(context, SubscriptionManager.getDefaultVoicePhoneId());
        if (imsManager != null) {
            return imsManager.isVolteEnabledByPlatform();
        }
        loge("isVolteEnabledByPlatform: ImsManager null, returning default value.");
        return false;
    }

    public boolean isVolteEnabledByPlatform() {
        if (SystemProperties.getInt(PROPERTY_DBG_VOLTE_AVAIL_OVERRIDE + Integer.toString(this.mPhoneId), -1) == 1 || SystemProperties.getInt(PROPERTY_DBG_VOLTE_AVAIL_OVERRIDE, -1) == 1) {
            return DBG;
        }
        if (this.mContext.getResources().getBoolean(R.^attr-private.dotActivatedColor) && getBooleanCarrierConfig("carrier_volte_available_bool") && isGbaValid()) {
            return DBG;
        }
        return false;
    }

    public static boolean isVolteProvisionedOnDevice(Context context) {
        ImsManager imsManager = getInstance(context, SubscriptionManager.getDefaultVoicePhoneId());
        if (imsManager != null) {
            return imsManager.isVolteProvisionedOnDevice();
        }
        loge("isVolteProvisionedOnDevice: ImsManager null, returning default value.");
        return DBG;
    }

    public boolean isVolteProvisionedOnDevice() {
        if (getBooleanCarrierConfig("carrier_volte_provisioning_required_bool")) {
            return isVolteProvisioned();
        }
        return DBG;
    }

    public static boolean isWfcProvisionedOnDevice(Context context) {
        ImsManager imsManager = getInstance(context, SubscriptionManager.getDefaultVoicePhoneId());
        if (imsManager != null) {
            return imsManager.isWfcProvisionedOnDevice();
        }
        loge("isWfcProvisionedOnDevice: ImsManager null, returning default value.");
        return DBG;
    }

    public boolean isWfcProvisionedOnDevice() {
        if (getBooleanCarrierConfig("carrier_volte_override_wfc_provisioning_bool") && !isVolteProvisionedOnDevice()) {
            return false;
        }
        if (getBooleanCarrierConfig("carrier_volte_provisioning_required_bool")) {
            return isWfcProvisioned();
        }
        return DBG;
    }

    public static boolean isVtProvisionedOnDevice(Context context) {
        ImsManager imsManager = getInstance(context, SubscriptionManager.getDefaultVoicePhoneId());
        if (imsManager != null) {
            return imsManager.isVtProvisionedOnDevice();
        }
        loge("isVtProvisionedOnDevice: ImsManager null, returning default value.");
        return DBG;
    }

    public boolean isVtProvisionedOnDevice() {
        if (getBooleanCarrierConfig("carrier_volte_provisioning_required_bool")) {
            return isVtProvisioned();
        }
        return DBG;
    }

    public static boolean isVtEnabledByPlatform(Context context) {
        ImsManager imsManager = getInstance(context, SubscriptionManager.getDefaultVoicePhoneId());
        if (imsManager != null) {
            return imsManager.isVtEnabledByPlatform();
        }
        loge("isVtEnabledByPlatform: ImsManager null, returning default value.");
        return false;
    }

    public boolean isVtEnabledByPlatform() {
        if (SystemProperties.getInt(PROPERTY_DBG_VT_AVAIL_OVERRIDE + Integer.toString(this.mPhoneId), -1) == 1 || SystemProperties.getInt(PROPERTY_DBG_VT_AVAIL_OVERRIDE, -1) == 1) {
            return DBG;
        }
        if (this.mContext.getResources().getBoolean(R.^attr-private.dotColor) && getBooleanCarrierConfig("carrier_vt_available_bool") && isGbaValid()) {
            return DBG;
        }
        return false;
    }

    public static boolean isVtEnabledByUser(Context context) {
        ImsManager imsManager = getInstance(context, SubscriptionManager.getDefaultVoicePhoneId());
        if (imsManager != null) {
            return imsManager.isVtEnabledByUser();
        }
        loge("isVtEnabledByUser: ImsManager null, returning default value.");
        return false;
    }

    public boolean isVtEnabledByUser() {
        int integerSubscriptionProperty = SubscriptionManager.getIntegerSubscriptionProperty(getSubId(), "vt_ims_enabled", -1, this.mContext);
        if (integerSubscriptionProperty == -1 || integerSubscriptionProperty == 1) {
            return DBG;
        }
        return false;
    }

    public static void setVtSetting(Context context, boolean z) {
        ImsManager imsManager = getInstance(context, SubscriptionManager.getDefaultVoicePhoneId());
        if (imsManager != null) {
            imsManager.setVtSetting(z);
        }
        loge("setVtSetting: ImsManager null, can not set value.");
    }

    public void setVtSetting(boolean z) {
        SubscriptionManager.setSubscriptionProperty(getSubId(), "vt_ims_enabled", booleanToPropertyString(z));
        try {
            changeMmTelCapability(2, 0, z);
            if (z) {
                log("setVtSetting(b) : turnOnIms");
                turnOnIms();
            } else if (isTurnOffImsAllowedByPlatform() && (!isVolteEnabledByPlatform() || !isEnhanced4gLteModeSettingEnabledByUser())) {
                log("setVtSetting(b) : imsServiceAllowTurnOff -> turnOffIms");
                turnOffIms();
            }
        } catch (ImsException e) {
            loge("setVtSetting(b): ", e);
        }
    }

    protected static boolean isTurnOffImsAllowedByPlatform(Context context) {
        ImsManager imsManager = getInstance(context, SubscriptionManager.getDefaultVoicePhoneId());
        if (imsManager != null) {
            return imsManager.isTurnOffImsAllowedByPlatform();
        }
        loge("isTurnOffImsAllowedByPlatform: ImsManager null, returning default value.");
        return DBG;
    }

    protected boolean isTurnOffImsAllowedByPlatform() {
        StringBuilder sb = new StringBuilder();
        sb.append(PROPERTY_DBG_ALLOW_IMS_OFF_OVERRIDE);
        sb.append(Integer.toString(this.mPhoneId));
        return (SystemProperties.getInt(sb.toString(), -1) == 1 || SystemProperties.getInt(PROPERTY_DBG_ALLOW_IMS_OFF_OVERRIDE, -1) == 1) ? DBG : getBooleanCarrierConfig("carrier_allow_turnoff_ims_bool");
    }

    public static boolean isWfcEnabledByUser(Context context) {
        ImsManager imsManager = getInstance(context, SubscriptionManager.getDefaultVoicePhoneId());
        if (imsManager != null) {
            return imsManager.isWfcEnabledByUser();
        }
        loge("isWfcEnabledByUser: ImsManager null, returning default value.");
        return DBG;
    }

    public boolean isWfcEnabledByUser() {
        int integerSubscriptionProperty = SubscriptionManager.getIntegerSubscriptionProperty(getSubId(), "wfc_ims_enabled", -1, this.mContext);
        if (integerSubscriptionProperty == -1) {
            return getBooleanCarrierConfig("carrier_default_wfc_ims_enabled_bool");
        }
        if (integerSubscriptionProperty == 1) {
            return DBG;
        }
        return false;
    }

    public static void setWfcSetting(Context context, boolean z) {
        ImsManager imsManager = getInstance(context, SubscriptionManager.getDefaultVoicePhoneId());
        if (imsManager != null) {
            imsManager.setWfcSetting(z);
        }
        loge("setWfcSetting: ImsManager null, can not set value.");
    }

    public void setWfcSetting(boolean z) {
        SubscriptionManager.setSubscriptionProperty(getSubId(), "wfc_ims_enabled", booleanToPropertyString(z));
        setWfcNonPersistent(z, getWfcMode(((TelephonyManager) this.mContext.getSystemService("phone")).isNetworkRoaming(getSubId())));
    }

    public void setWfcNonPersistent(boolean z, int i) {
        if (!z) {
            i = 1;
        }
        try {
            changeMmTelCapability(1, 1, z);
            if (z) {
                log("setWfcSetting() : turnOnIms");
                turnOnIms();
            } else if (isTurnOffImsAllowedByPlatform() && (!isVolteEnabledByPlatform() || !isEnhanced4gLteModeSettingEnabledByUser())) {
                log("setWfcSetting() : imsServiceAllowTurnOff -> turnOffIms");
                turnOffIms();
            }
            setWfcModeInternal(i);
        } catch (ImsException e) {
            loge("setWfcSetting(): ", e);
        }
    }

    public static int getWfcMode(Context context) {
        ImsManager imsManager = getInstance(context, SubscriptionManager.getDefaultVoicePhoneId());
        if (imsManager != null) {
            return imsManager.getWfcMode();
        }
        loge("getWfcMode: ImsManager null, returning default value.");
        return 0;
    }

    public int getWfcMode() {
        return getWfcMode(false);
    }

    public static void setWfcMode(Context context, int i) {
        ImsManager imsManager = getInstance(context, SubscriptionManager.getDefaultVoicePhoneId());
        if (imsManager != null) {
            imsManager.setWfcMode(i);
        }
        loge("setWfcMode: ImsManager null, can not set value.");
    }

    public void setWfcMode(int i) {
        log("setWfcMode(i) - setting=" + i);
        SubscriptionManager.setSubscriptionProperty(getSubId(), "wfc_ims_mode", Integer.toString(i));
        setWfcModeInternal(i);
    }

    public static int getWfcMode(Context context, boolean z) {
        ImsManager imsManager = getInstance(context, SubscriptionManager.getDefaultVoicePhoneId());
        if (imsManager != null) {
            return imsManager.getWfcMode(z);
        }
        loge("getWfcMode: ImsManager null, returning default value.");
        return 0;
    }

    public int getWfcMode(boolean z) {
        int settingFromSubscriptionManager;
        if (!z) {
            if (!getBooleanCarrierConfig("editable_wfc_mode_bool")) {
                settingFromSubscriptionManager = getIntCarrierConfig("carrier_default_wfc_ims_mode_int");
            } else {
                settingFromSubscriptionManager = getSettingFromSubscriptionManager("wfc_ims_mode", "carrier_default_wfc_ims_mode_int");
            }
            log("getWfcMode - setting=" + settingFromSubscriptionManager);
            return settingFromSubscriptionManager;
        }
        int settingFromSubscriptionManager2 = getSettingFromSubscriptionManager("wfc_ims_roaming_mode", "carrier_default_wfc_ims_roaming_mode_int");
        log("getWfcMode (roaming) - setting=" + settingFromSubscriptionManager2);
        return settingFromSubscriptionManager2;
    }

    private int getSettingFromSubscriptionManager(String str, String str2) {
        int integerSubscriptionProperty = SubscriptionManager.getIntegerSubscriptionProperty(getSubId(), str, -1, this.mContext);
        if (integerSubscriptionProperty == -1) {
            return getIntCarrierConfig(str2);
        }
        return integerSubscriptionProperty;
    }

    public static void setWfcMode(Context context, int i, boolean z) {
        ImsManager imsManager = getInstance(context, SubscriptionManager.getDefaultVoicePhoneId());
        if (imsManager != null) {
            imsManager.setWfcMode(i, z);
        }
        loge("setWfcMode: ImsManager null, can not set value.");
    }

    public void setWfcMode(int i, boolean z) {
        if (!z) {
            log("setWfcMode(i,b) - setting=" + i);
            SubscriptionManager.setSubscriptionProperty(getSubId(), "wfc_ims_mode", Integer.toString(i));
        } else {
            log("setWfcMode(i,b) (roaming) - setting=" + i);
            SubscriptionManager.setSubscriptionProperty(getSubId(), "wfc_ims_roaming_mode", Integer.toString(i));
        }
        if (z == ((TelephonyManager) this.mContext.getSystemService("phone")).isNetworkRoaming(getSubId())) {
            setWfcModeInternal(i);
        }
    }

    protected int getSubId() {
        int[] subId = SubscriptionManager.getSubId(this.mPhoneId);
        if (subId != null && subId.length >= 1) {
            return subId[0];
        }
        return -1;
    }

    protected void setWfcModeInternal(final int i) {
        new Thread(new Runnable() {
            @Override
            public final void run() {
                this.f$0.getConfigInterface().setConfig(27, i);
            }
        }).start();
    }

    public static boolean isWfcRoamingEnabledByUser(Context context) {
        ImsManager imsManager = getInstance(context, SubscriptionManager.getDefaultVoicePhoneId());
        if (imsManager != null) {
            return imsManager.isWfcRoamingEnabledByUser();
        }
        loge("isWfcRoamingEnabledByUser: ImsManager null, returning default value.");
        return false;
    }

    public boolean isWfcRoamingEnabledByUser() {
        int integerSubscriptionProperty = SubscriptionManager.getIntegerSubscriptionProperty(getSubId(), "wfc_ims_roaming_enabled", -1, this.mContext);
        if (integerSubscriptionProperty == -1) {
            return getBooleanCarrierConfig("carrier_default_wfc_ims_roaming_enabled_bool");
        }
        if (integerSubscriptionProperty == 1) {
            return DBG;
        }
        return false;
    }

    public static void setWfcRoamingSetting(Context context, boolean z) {
        ImsManager imsManager = getInstance(context, SubscriptionManager.getDefaultVoicePhoneId());
        if (imsManager != null) {
            imsManager.setWfcRoamingSetting(z);
        }
        loge("setWfcRoamingSetting: ImsManager null, value not set.");
    }

    public void setWfcRoamingSetting(boolean z) {
        SubscriptionManager.setSubscriptionProperty(getSubId(), "wfc_ims_roaming_enabled", booleanToPropertyString(z));
        setWfcRoamingSettingInternal(z);
    }

    protected void setWfcRoamingSettingInternal(boolean z) {
        final int i;
        if (z) {
            i = 1;
        } else {
            i = 0;
        }
        new Thread(new Runnable() {
            @Override
            public final void run() {
                this.f$0.getConfigInterface().setConfig(26, i);
            }
        }).start();
    }

    public static boolean isWfcEnabledByPlatform(Context context) {
        ImsManager imsManager = getInstance(context, SubscriptionManager.getDefaultVoicePhoneId());
        if (imsManager != null) {
            return imsManager.isWfcEnabledByPlatform();
        }
        loge("isWfcEnabledByPlatform: ImsManager null, returning default value.");
        return false;
    }

    public boolean isWfcEnabledByPlatform() {
        if (SystemProperties.getInt(PROPERTY_DBG_WFC_AVAIL_OVERRIDE + Integer.toString(this.mPhoneId), -1) == 1 || SystemProperties.getInt(PROPERTY_DBG_WFC_AVAIL_OVERRIDE, -1) == 1) {
            return DBG;
        }
        if (this.mContext.getResources().getBoolean(R.^attr-private.dotSize) && getBooleanCarrierConfig("carrier_wfc_ims_available_bool") && isGbaValid()) {
            return DBG;
        }
        return false;
    }

    protected boolean isGbaValid() {
        boolean booleanCarrierConfig = getBooleanCarrierConfig("carrier_ims_gba_required_bool");
        boolean z = DBG;
        if (!booleanCarrierConfig) {
            return DBG;
        }
        String isimIst = new TelephonyManager(this.mContext, getSubId()).getIsimIst();
        if (isimIst == null) {
            loge("isGbaValid - ISF is NULL");
            return DBG;
        }
        if (isimIst == null || isimIst.length() <= 1 || (2 & ((byte) isimIst.charAt(1))) == 0) {
            z = false;
        }
        log("isGbaValid - GBA capable=" + z + ", ISF=" + isimIst);
        return z;
    }

    private boolean getProvisionedBool(ImsConfig imsConfig, int i) throws ImsException {
        if (imsConfig.getProvisionedValue(i) == -1) {
            throw new ImsException("getProvisionedBool failed with error for item: " + i, 103);
        }
        if (imsConfig.getProvisionedValue(i) == 1) {
            return DBG;
        }
        return false;
    }

    private boolean getProvisionedBoolNoException(int i) {
        try {
            return getProvisionedBool(getConfigInterface(), i);
        } catch (ImsException e) {
            return false;
        }
    }

    public static void updateImsServiceConfig(Context context, int i, boolean z) {
        ImsManager imsManager = getInstance(context, i);
        if (imsManager != null) {
            imsManager.updateImsServiceConfig(z);
        }
        loge("updateImsServiceConfig: ImsManager null, returning without update.");
    }

    public void updateImsServiceConfig(boolean z) {
        if (!z && new TelephonyManager(this.mContext, getSubId()).getSimState() != 5) {
            log("updateImsServiceConfig: SIM not ready");
            return;
        }
        if (!this.mConfigUpdated || z) {
            try {
                if ((updateVolteFeatureValue() | updateWfcFeatureAndProvisionedValues() | updateVideoCallFeatureValue()) || !isTurnOffImsAllowedByPlatform()) {
                    log("updateImsServiceConfig: turnOnIms");
                    turnOnIms();
                } else {
                    log("updateImsServiceConfig: turnOffIms");
                    turnOffIms();
                }
                this.mConfigUpdated = DBG;
            } catch (ImsException e) {
                loge("updateImsServiceConfig: ", e);
                this.mConfigUpdated = false;
            }
        }
    }

    protected boolean updateVolteFeatureValue() throws ImsException {
        boolean zIsVolteEnabledByPlatform = isVolteEnabledByPlatform();
        boolean zIsEnhanced4gLteModeSettingEnabledByUser = isEnhanced4gLteModeSettingEnabledByUser();
        boolean zIsNonTtyOrTtyOnVolteEnabled = isNonTtyOrTtyOnVolteEnabled();
        boolean z = zIsVolteEnabledByPlatform && zIsEnhanced4gLteModeSettingEnabledByUser && zIsNonTtyOrTtyOnVolteEnabled;
        log("updateVolteFeatureValue: available = " + zIsVolteEnabledByPlatform + ", enabled = " + zIsEnhanced4gLteModeSettingEnabledByUser + ", nonTTY = " + zIsNonTtyOrTtyOnVolteEnabled);
        changeMmTelCapability(1, 0, z);
        return z;
    }

    protected boolean updateVideoCallFeatureValue() throws ImsException {
        boolean zIsVtEnabledByPlatform = isVtEnabledByPlatform();
        boolean zIsVtEnabledByUser = isVtEnabledByUser();
        boolean zIsNonTtyOrTtyOnVolteEnabled = isNonTtyOrTtyOnVolteEnabled();
        boolean zIsDataEnabled = isDataEnabled();
        boolean z = (zIsVtEnabledByPlatform && zIsVtEnabledByUser && zIsNonTtyOrTtyOnVolteEnabled && (getBooleanCarrierConfig("ignore_data_enabled_changed_for_video_calls") || zIsDataEnabled)) ? DBG : false;
        log("updateVideoCallFeatureValue: available = " + zIsVtEnabledByPlatform + ", enabled = " + zIsVtEnabledByUser + ", nonTTY = " + zIsNonTtyOrTtyOnVolteEnabled + ", data enabled = " + zIsDataEnabled);
        changeMmTelCapability(2, 0, z);
        return z;
    }

    protected boolean updateWfcFeatureAndProvisionedValues() throws ImsException {
        boolean zIsNetworkRoaming = new TelephonyManager(this.mContext, getSubId()).isNetworkRoaming();
        boolean zIsWfcEnabledByPlatform = isWfcEnabledByPlatform();
        boolean zIsWfcEnabledByUser = isWfcEnabledByUser();
        int wfcMode = getWfcMode(zIsNetworkRoaming);
        boolean zIsWfcRoamingEnabledByUser = isWfcRoamingEnabledByUser();
        boolean z = zIsWfcEnabledByPlatform && zIsWfcEnabledByUser;
        log("updateWfcFeatureAndProvisionedValues: available = " + zIsWfcEnabledByPlatform + ", enabled = " + zIsWfcEnabledByUser + ", mode = " + wfcMode + ", roaming = " + zIsWfcRoamingEnabledByUser);
        changeMmTelCapability(1, 1, z);
        if (!z) {
            zIsWfcRoamingEnabledByUser = false;
            wfcMode = 1;
        }
        setWfcModeInternal(wfcMode);
        setWfcRoamingSettingInternal(zIsWfcRoamingEnabledByUser);
        return z;
    }

    @VisibleForTesting
    public ImsManager(Context context, int i) {
        this.mContext = context;
        this.mPhoneId = i;
        this.mConfigDynamicBind = this.mContext.getResources().getBoolean(R.^attr-private.findOnPagePreviousDrawable);
        this.mConfigManager = (CarrierConfigManager) context.getSystemService("carrier_config");
        createImsService();
    }

    public boolean isDynamicBinding() {
        return this.mConfigDynamicBind;
    }

    public boolean isServiceAvailable() {
        if (((TelephonyManager) this.mContext.getSystemService("phone")).isResolvingImsBinding()) {
            Log.d(TAG, "isServiceAvailable: resolving IMS binding, returning false");
            return false;
        }
        connectIfServiceIsAvailable();
        return this.mMmTelFeatureConnection.isBinderAlive();
    }

    public boolean isServiceReady() {
        connectIfServiceIsAvailable();
        return this.mMmTelFeatureConnection.isBinderReady();
    }

    public void connectIfServiceIsAvailable() {
        if (this.mMmTelFeatureConnection == null || !this.mMmTelFeatureConnection.isBinderAlive()) {
            createImsService();
        }
    }

    public void setConfigListener(ImsConfigListener imsConfigListener) {
        this.mImsConfigListener = imsConfigListener;
    }

    @VisibleForTesting
    public void addNotifyStatusChangedCallbackIfAvailable(MmTelFeatureConnection.IFeatureUpdate iFeatureUpdate) throws ImsException {
        if (!this.mMmTelFeatureConnection.isBinderAlive()) {
            throw new ImsException("Binder is not active!", 106);
        }
        if (iFeatureUpdate != null) {
            this.mStatusCallbacks.add(iFeatureUpdate);
        }
    }

    void removeNotifyStatusChangedCallback(MmTelFeatureConnection.IFeatureUpdate iFeatureUpdate) {
        if (iFeatureUpdate != null) {
            this.mStatusCallbacks.remove(iFeatureUpdate);
        } else {
            Log.w(TAG, "removeNotifyStatusChangedCallback: callback is null!");
        }
    }

    public void open(MmTelFeature.Listener listener) throws ImsException {
        checkAndThrowExceptionIfServiceUnavailable();
        if (listener == null) {
            throw new NullPointerException("listener can't be null");
        }
        try {
            this.mMmTelFeatureConnection.openConnection(listener);
        } catch (RemoteException e) {
            throw new ImsException("open()", e, 106);
        }
    }

    public void addRegistrationListener(int i, ImsConnectionStateListener imsConnectionStateListener) throws ImsException {
        addRegistrationListener(imsConnectionStateListener);
    }

    public void addRegistrationListener(final ImsConnectionStateListener imsConnectionStateListener) throws ImsException {
        if (imsConnectionStateListener == null) {
            throw new NullPointerException("listener can't be null");
        }
        addRegistrationCallback(imsConnectionStateListener);
        addCapabilitiesCallback(new ImsFeature.CapabilityCallback() {
            public void onCapabilitiesStatusChanged(ImsFeature.Capabilities capabilities) {
                imsConnectionStateListener.onFeatureCapabilityChangedAdapter(ImsManager.this.getRegistrationTech(), capabilities);
            }
        });
        log("Registration Callback registered.");
    }

    public void addRegistrationCallback(ImsRegistrationImplBase.Callback callback) throws ImsException {
        if (callback == null) {
            throw new NullPointerException("registration callback can't be null");
        }
        try {
            this.mMmTelFeatureConnection.addRegistrationCallback(callback);
            log("Registration Callback registered.");
        } catch (RemoteException e) {
            throw new ImsException("addRegistrationCallback(IRIB)", e, 106);
        }
    }

    public void removeRegistrationListener(ImsRegistrationImplBase.Callback callback) throws ImsException {
        if (callback == null) {
            throw new NullPointerException("registration callback can't be null");
        }
        try {
            this.mMmTelFeatureConnection.removeRegistrationCallback(callback);
            log("Registration callback removed.");
        } catch (RemoteException e) {
            throw new ImsException("removeRegistrationCallback(IRIB)", e, 106);
        }
    }

    public void addCapabilitiesCallback(ImsFeature.CapabilityCallback capabilityCallback) throws ImsException {
        if (capabilityCallback == null) {
            throw new NullPointerException("capabilities callback can't be null");
        }
        checkAndThrowExceptionIfServiceUnavailable();
        try {
            this.mMmTelFeatureConnection.addCapabilityCallback(capabilityCallback);
            log("Capability Callback registered.");
        } catch (RemoteException e) {
            throw new ImsException("addCapabilitiesCallback(IF)", e, 106);
        }
    }

    public void removeRegistrationListener(ImsConnectionStateListener imsConnectionStateListener) throws ImsException {
        if (imsConnectionStateListener == null) {
            throw new NullPointerException("listener can't be null");
        }
        checkAndThrowExceptionIfServiceUnavailable();
        try {
            this.mMmTelFeatureConnection.removeRegistrationCallback(imsConnectionStateListener);
            log("Registration Callback/Listener registered.");
        } catch (RemoteException e) {
            throw new ImsException("addRegistrationCallback()", e, 106);
        }
    }

    public int getRegistrationTech() {
        try {
            return this.mMmTelFeatureConnection.getRegistrationTech();
        } catch (RemoteException e) {
            Log.w(TAG, "getRegistrationTech: no connection to ImsService.");
            return -1;
        }
    }

    public void close() {
        if (this.mMmTelFeatureConnection != null) {
            this.mMmTelFeatureConnection.closeConnection();
        }
        this.mUt = null;
        this.mEcbm = null;
        this.mMultiEndpoint = null;
    }

    public ImsUtInterface getSupplementaryServiceConfiguration() throws ImsException {
        if (this.mUt != null && this.mUt.isBinderAlive()) {
            return this.mUt;
        }
        checkAndThrowExceptionIfServiceUnavailable();
        try {
            IImsUt utInterface = this.mMmTelFeatureConnection.getUtInterface();
            if (utInterface == null) {
                throw new ImsException("getSupplementaryServiceConfiguration()", 801);
            }
            this.mUt = new ImsUt(utInterface);
            return this.mUt;
        } catch (RemoteException e) {
            throw new ImsException("getSupplementaryServiceConfiguration()", e, 106);
        }
    }

    public ImsCallProfile createCallProfile(int i, int i2) throws ImsException {
        checkAndThrowExceptionIfServiceUnavailable();
        try {
            return this.mMmTelFeatureConnection.createCallProfile(i, i2);
        } catch (RemoteException e) {
            throw new ImsException("createCallProfile()", e, 106);
        }
    }

    public ImsCall makeCall(ImsCallProfile imsCallProfile, String[] strArr, ImsCall.Listener listener) throws ImsException {
        log("makeCall :: profile=" + imsCallProfile);
        checkAndThrowExceptionIfServiceUnavailable();
        ImsCall imsCall = new ImsCall(this.mContext, imsCallProfile);
        imsCall.setListener(listener);
        ImsCallSession imsCallSessionCreateCallSession = createCallSession(imsCallProfile);
        if (strArr != null && strArr.length == 1) {
            imsCall.start(imsCallSessionCreateCallSession, strArr[0]);
        } else {
            imsCall.start(imsCallSessionCreateCallSession, strArr);
        }
        return imsCall;
    }

    public ImsCall takeCall(IImsCallSession iImsCallSession, Bundle bundle, ImsCall.Listener listener) throws ImsException {
        log("takeCall :: incomingCall=" + bundle);
        checkAndThrowExceptionIfServiceUnavailable();
        if (bundle == null) {
            throw new ImsException("Can't retrieve session with null intent", INCOMING_CALL_RESULT_CODE);
        }
        if (getCallId(bundle) == null) {
            throw new ImsException("Call ID missing in the incoming call intent", INCOMING_CALL_RESULT_CODE);
        }
        try {
            if (iImsCallSession == null) {
                throw new ImsException("No pending session for the call", 107);
            }
            ImsCall imsCall = new ImsCall(this.mContext, iImsCallSession.getCallProfile());
            imsCall.attachSession(new ImsCallSession(iImsCallSession));
            imsCall.setListener(listener);
            return imsCall;
        } catch (Throwable th) {
            throw new ImsException("takeCall()", th, 0);
        }
    }

    public ImsConfig getConfigInterface() throws ImsException {
        checkAndThrowExceptionIfServiceUnavailable();
        try {
            IImsConfig configInterface = this.mMmTelFeatureConnection.getConfigInterface();
            if (configInterface == null) {
                throw new ImsException("getConfigInterface()", 131);
            }
            return new ImsConfig(configInterface);
        } catch (RemoteException e) {
            throw new ImsException("getConfigInterface()", e, 106);
        }
    }

    public void changeMmTelCapability(int i, int i2, boolean z) throws ImsException {
        checkAndThrowExceptionIfServiceUnavailable();
        CapabilityChangeRequest capabilityChangeRequest = new CapabilityChangeRequest();
        if (z) {
            capabilityChangeRequest.addCapabilitiesToEnableForTech(i, i2);
        } else {
            capabilityChangeRequest.addCapabilitiesToDisableForTech(i, i2);
        }
        try {
            this.mMmTelFeatureConnection.changeEnabledCapabilities(capabilityChangeRequest, null);
            if (this.mImsConfigListener != null) {
                this.mImsConfigListener.onSetFeatureResponse(i, this.mMmTelFeatureConnection.getRegistrationTech(), z ? 1 : 0, -1);
            }
        } catch (RemoteException e) {
            throw new ImsException("changeMmTelCapability()", e, 106);
        }
    }

    public void setRttEnabled(final boolean z) {
        boolean z2;
        final int i = 1;
        if (!z) {
            try {
                z2 = isEnhanced4gLteModeSettingEnabledByUser();
            } catch (ImsException e) {
                Log.e(ImsManager.class.getSimpleName(), "Unable to set RTT enabled to " + z + ": " + e);
                return;
            }
        }
        setAdvanced4GMode(z2);
        if (!z) {
            i = 0;
        }
        new Thread(new Runnable() {
            @Override
            public final void run() {
                ImsManager.lambda$setRttEnabled$2(this.f$0, z, i);
            }
        }).start();
    }

    public static void lambda$setRttEnabled$2(ImsManager imsManager, boolean z, int i) {
        try {
            Log.i(ImsManager.class.getSimpleName(), "Setting RTT enabled to " + z);
            imsManager.getConfigInterface().setProvisionedValue(66, i);
        } catch (ImsException e) {
            Log.e(ImsManager.class.getSimpleName(), "Unable to set RTT enabled to " + z + ": " + e);
        }
    }

    public void setTtyMode(int i) throws ImsException {
        if (!getBooleanCarrierConfig("carrier_volte_tty_supported_bool")) {
            setAdvanced4GMode((i == 0 && isEnhanced4gLteModeSettingEnabledByUser()) ? DBG : false);
        }
    }

    public void setUiTTYMode(Context context, int i, Message message) throws ImsException {
        checkAndThrowExceptionIfServiceUnavailable();
        try {
            this.mMmTelFeatureConnection.setUiTTYMode(i, message);
        } catch (RemoteException e) {
            throw new ImsException("setTTYMode()", e, 106);
        }
    }

    private ImsReasonInfo makeACopy(ImsReasonInfo imsReasonInfo) {
        Parcel parcelObtain = Parcel.obtain();
        imsReasonInfo.writeToParcel(parcelObtain, 0);
        parcelObtain.setDataPosition(0);
        ImsReasonInfo imsReasonInfo2 = (ImsReasonInfo) ImsReasonInfo.CREATOR.createFromParcel(parcelObtain);
        parcelObtain.recycle();
        return imsReasonInfo2;
    }

    public ArrayList<ImsReasonInfo> getRecentImsDisconnectReasons() {
        ArrayList<ImsReasonInfo> arrayList = new ArrayList<>();
        Iterator<ImsReasonInfo> it = this.mRecentDisconnectReasons.iterator();
        while (it.hasNext()) {
            arrayList.add(makeACopy(it.next()));
        }
        return arrayList;
    }

    public int getImsServiceState() throws ImsException {
        return this.mMmTelFeatureConnection.getFeatureState();
    }

    public boolean getBooleanCarrierConfig(String str) {
        int i;
        int[] subId = SubscriptionManager.getSubId(this.mPhoneId);
        if (subId != null && subId.length >= 1) {
            i = subId[0];
        } else {
            i = -1;
        }
        PersistableBundle configForSubId = this.mConfigManager != null ? this.mConfigManager.getConfigForSubId(i) : null;
        if (configForSubId != null) {
            return configForSubId.getBoolean(str);
        }
        return CarrierConfigManager.getDefaultConfig().getBoolean(str);
    }

    protected int getIntCarrierConfig(String str) {
        int i;
        int[] subId = SubscriptionManager.getSubId(this.mPhoneId);
        if (subId != null && subId.length >= 1) {
            i = subId[0];
        } else {
            i = -1;
        }
        PersistableBundle configForSubId = this.mConfigManager != null ? this.mConfigManager.getConfigForSubId(i) : null;
        if (configForSubId != null) {
            return configForSubId.getInt(str);
        }
        return CarrierConfigManager.getDefaultConfig().getInt(str);
    }

    protected static String getCallId(Bundle bundle) {
        if (bundle == null) {
            return null;
        }
        return bundle.getString(EXTRA_CALL_ID);
    }

    private void checkAndThrowExceptionIfServiceUnavailable() throws ImsException {
        if (this.mMmTelFeatureConnection == null || !this.mMmTelFeatureConnection.isBinderAlive()) {
            createImsService();
            if (this.mMmTelFeatureConnection == null) {
                throw new ImsException("Service is unavailable", 106);
            }
        }
    }

    protected void createImsService() {
        Rlog.i(TAG, "Creating ImsService");
        this.mMmTelFeatureConnection = MmTelFeatureConnection.create(this.mContext, this.mPhoneId);
        this.mMmTelFeatureConnection.setStatusCallback(new MmTelFeatureConnection.IFeatureUpdate() {
            @Override
            public void notifyStateChanged() {
                ImsManager.this.mStatusCallbacks.forEach(new Consumer() {
                    @Override
                    public final void accept(Object obj) {
                        ((MmTelFeatureConnection.IFeatureUpdate) obj).notifyStateChanged();
                    }
                });
            }

            @Override
            public void notifyUnavailable() {
                ImsManager.this.mStatusCallbacks.forEach(new Consumer() {
                    @Override
                    public final void accept(Object obj) {
                        ((MmTelFeatureConnection.IFeatureUpdate) obj).notifyUnavailable();
                    }
                });
            }
        });
    }

    protected ImsCallSession createCallSession(ImsCallProfile imsCallProfile) throws ImsException {
        try {
            return new ImsCallSession(this.mMmTelFeatureConnection.createCallSession(imsCallProfile));
        } catch (RemoteException e) {
            Rlog.w(TAG, "CreateCallSession: Error, remote exception: " + e.getMessage());
            throw new ImsException("createCallSession()", e, 106);
        }
    }

    private static void log(String str) {
        Rlog.d(TAG, str);
    }

    private static void loge(String str) {
        Rlog.e(TAG, str);
    }

    private static void loge(String str, Throwable th) {
        Rlog.e(TAG, str, th);
    }

    protected void turnOnIms() throws ImsException {
        ((TelephonyManager) this.mContext.getSystemService("phone")).enableIms(this.mPhoneId);
    }

    private boolean isImsTurnOffAllowed() {
        if (!isTurnOffImsAllowedByPlatform() || (isWfcEnabledByPlatform() && isWfcEnabledByUser())) {
            return false;
        }
        return DBG;
    }

    protected void setLteFeatureValues(boolean z) {
        log("setLteFeatureValues: " + z);
        CapabilityChangeRequest capabilityChangeRequest = new CapabilityChangeRequest();
        boolean z2 = DBG;
        if (z) {
            capabilityChangeRequest.addCapabilitiesToEnableForTech(1, 0);
        } else {
            capabilityChangeRequest.addCapabilitiesToDisableForTech(1, 0);
        }
        if (isVolteEnabledByPlatform()) {
            boolean booleanCarrierConfig = getBooleanCarrierConfig("ignore_data_enabled_changed_for_video_calls");
            if (!z || !isVtEnabledByUser() || (!booleanCarrierConfig && !isDataEnabled())) {
                z2 = false;
            }
            if (z2) {
                capabilityChangeRequest.addCapabilitiesToEnableForTech(2, 0);
            } else {
                capabilityChangeRequest.addCapabilitiesToDisableForTech(2, 0);
            }
        }
        try {
            this.mMmTelFeatureConnection.changeEnabledCapabilities(capabilityChangeRequest, null);
        } catch (RemoteException e) {
            Log.e(TAG, "setLteFeatureValues: Exception: " + e.getMessage());
        }
    }

    protected void setAdvanced4GMode(boolean z) throws ImsException {
        checkAndThrowExceptionIfServiceUnavailable();
        if (z) {
            setLteFeatureValues(z);
            log("setAdvanced4GMode: turnOnIms");
            turnOnIms();
        } else {
            if (isImsTurnOffAllowed()) {
                log("setAdvanced4GMode: turnOffIms");
                turnOffIms();
            }
            setLteFeatureValues(z);
        }
    }

    protected void turnOffIms() throws ImsException {
        ((TelephonyManager) this.mContext.getSystemService("phone")).disableIms(this.mPhoneId);
    }

    private void addToRecentDisconnectReasons(ImsReasonInfo imsReasonInfo) {
        if (imsReasonInfo == null) {
            return;
        }
        while (this.mRecentDisconnectReasons.size() >= MAX_RECENT_DISCONNECT_REASONS) {
            this.mRecentDisconnectReasons.removeFirst();
        }
        this.mRecentDisconnectReasons.addLast(imsReasonInfo);
    }

    public ImsEcbm getEcbmInterface() throws ImsException {
        if (this.mEcbm != null && this.mEcbm.isBinderAlive()) {
            return this.mEcbm;
        }
        checkAndThrowExceptionIfServiceUnavailable();
        try {
            IImsEcbm ecbmInterface = this.mMmTelFeatureConnection.getEcbmInterface();
            if (ecbmInterface == null) {
                throw new ImsException("getEcbmInterface()", 901);
            }
            this.mEcbm = new ImsEcbm(ecbmInterface);
            return this.mEcbm;
        } catch (RemoteException e) {
            throw new ImsException("getEcbmInterface()", e, 106);
        }
    }

    public void sendSms(int i, int i2, String str, String str2, boolean z, byte[] bArr) throws ImsException {
        try {
            this.mMmTelFeatureConnection.sendSms(i, i2, str, str2, z, bArr);
        } catch (RemoteException e) {
            throw new ImsException("sendSms()", e, 106);
        }
    }

    public void acknowledgeSms(int i, int i2, int i3) throws ImsException {
        try {
            this.mMmTelFeatureConnection.acknowledgeSms(i, i2, i3);
        } catch (RemoteException e) {
            throw new ImsException("acknowledgeSms()", e, 106);
        }
    }

    public void acknowledgeSmsReport(int i, int i2, int i3) throws ImsException {
        try {
            this.mMmTelFeatureConnection.acknowledgeSmsReport(i, i2, i3);
        } catch (RemoteException e) {
            throw new ImsException("acknowledgeSmsReport()", e, 106);
        }
    }

    public String getSmsFormat() throws ImsException {
        try {
            return this.mMmTelFeatureConnection.getSmsFormat();
        } catch (RemoteException e) {
            throw new ImsException("getSmsFormat()", e, 106);
        }
    }

    public void setSmsListener(IImsSmsListener iImsSmsListener) throws ImsException {
        try {
            this.mMmTelFeatureConnection.setSmsListener(iImsSmsListener);
        } catch (RemoteException e) {
            throw new ImsException("setSmsListener()", e, 106);
        }
    }

    public void onSmsReady() throws ImsException {
        try {
            this.mMmTelFeatureConnection.onSmsReady();
        } catch (RemoteException e) {
            throw new ImsException("onSmsReady()", e, 106);
        }
    }

    public int shouldProcessCall(boolean z, String[] strArr) throws ImsException {
        try {
            return this.mMmTelFeatureConnection.shouldProcessCall(z, strArr);
        } catch (RemoteException e) {
            throw new ImsException("shouldProcessCall()", e, 106);
        }
    }

    public ImsMultiEndpoint getMultiEndpointInterface() throws ImsException {
        if (this.mMultiEndpoint != null && this.mMultiEndpoint.isBinderAlive()) {
            return this.mMultiEndpoint;
        }
        checkAndThrowExceptionIfServiceUnavailable();
        try {
            IImsMultiEndpoint multiEndpointInterface = this.mMmTelFeatureConnection.getMultiEndpointInterface();
            if (multiEndpointInterface == null) {
                throw new ImsException("getMultiEndpointInterface()", 902);
            }
            this.mMultiEndpoint = new ImsMultiEndpoint(multiEndpointInterface);
            return this.mMultiEndpoint;
        } catch (RemoteException e) {
            throw new ImsException("getMultiEndpointInterface()", e, 106);
        }
    }

    public static void factoryReset(Context context) {
        ImsManager imsManager = getInstance(context, SubscriptionManager.getDefaultVoicePhoneId());
        if (imsManager != null) {
            imsManager.factoryReset();
        }
        loge("factoryReset: ImsManager null.");
    }

    public void factoryReset() {
        SubscriptionManager.setSubscriptionProperty(getSubId(), "volte_vt_enabled", booleanToPropertyString(getBooleanCarrierConfig("enhanced_4g_lte_on_by_default_bool")));
        SubscriptionManager.setSubscriptionProperty(getSubId(), "wfc_ims_enabled", booleanToPropertyString(getBooleanCarrierConfig("carrier_default_wfc_ims_enabled_bool")));
        SubscriptionManager.setSubscriptionProperty(getSubId(), "wfc_ims_mode", Integer.toString(getIntCarrierConfig("carrier_default_wfc_ims_mode_int")));
        SubscriptionManager.setSubscriptionProperty(getSubId(), "wfc_ims_roaming_enabled", booleanToPropertyString(getBooleanCarrierConfig("carrier_default_wfc_ims_roaming_enabled_bool")));
        SubscriptionManager.setSubscriptionProperty(getSubId(), "vt_ims_enabled", booleanToPropertyString(DBG));
        updateImsServiceConfig(DBG);
    }

    protected boolean isDataEnabled() {
        return new TelephonyManager(this.mContext, getSubId()).isDataCapable();
    }

    private boolean isVolteProvisioned() {
        return getProvisionedBoolNoException(10);
    }

    private boolean isWfcProvisioned() {
        return getProvisionedBoolNoException(28);
    }

    private boolean isVtProvisioned() {
        return getProvisionedBoolNoException(11);
    }

    protected static String booleanToPropertyString(boolean z) {
        return z ? "1" : "0";
    }

    public void dump(FileDescriptor fileDescriptor, PrintWriter printWriter, String[] strArr) {
        printWriter.println("ImsManager:");
        printWriter.println("  mPhoneId = " + this.mPhoneId);
        printWriter.println("  mConfigUpdated = " + this.mConfigUpdated);
        printWriter.println("  mImsServiceProxy = " + this.mMmTelFeatureConnection);
        printWriter.println("  mDataEnabled = " + isDataEnabled());
        printWriter.println("  ignoreDataEnabledChanged = " + getBooleanCarrierConfig("ignore_data_enabled_changed_for_video_calls"));
        printWriter.println("  isGbaValid = " + isGbaValid());
        printWriter.println("  isImsTurnOffAllowed = " + isImsTurnOffAllowed());
        printWriter.println("  isNonTtyOrTtyOnVolteEnabled = " + isNonTtyOrTtyOnVolteEnabled());
        printWriter.println("  isVolteEnabledByPlatform = " + isVolteEnabledByPlatform());
        printWriter.println("  isVolteProvisionedOnDevice = " + isVolteProvisionedOnDevice());
        printWriter.println("  isEnhanced4gLteModeSettingEnabledByUser = " + isEnhanced4gLteModeSettingEnabledByUser());
        printWriter.println("  isVtEnabledByPlatform = " + isVtEnabledByPlatform());
        printWriter.println("  isVtEnabledByUser = " + isVtEnabledByUser());
        printWriter.println("  isWfcEnabledByPlatform = " + isWfcEnabledByPlatform());
        printWriter.println("  isWfcEnabledByUser = " + isWfcEnabledByUser());
        printWriter.println("  getWfcMode = " + getWfcMode());
        printWriter.println("  isWfcRoamingEnabledByUser = " + isWfcRoamingEnabledByUser());
        printWriter.println("  isVtProvisionedOnDevice = " + isVtProvisionedOnDevice());
        printWriter.println("  isWfcProvisionedOnDevice = " + isWfcProvisionedOnDevice());
        printWriter.flush();
    }

    private static Class<?> getMtkImsManager() {
        try {
            return Class.forName("com.mediatek.ims.internal.MtkImsManager");
        } catch (Exception e) {
            loge("MtkImsManager not found!");
            return null;
        }
    }
}
