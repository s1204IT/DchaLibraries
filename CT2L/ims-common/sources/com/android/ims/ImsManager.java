package com.android.ims;

import android.R;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.Registrant;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.os.SystemProperties;
import android.provider.Settings;
import android.telephony.Rlog;
import android.telephony.SubscriptionManager;
import com.android.ims.ImsCall;
import com.android.ims.internal.IImsCallSession;
import com.android.ims.internal.IImsCallSessionListener;
import com.android.ims.internal.IImsConfig;
import com.android.ims.internal.IImsEcbm;
import com.android.ims.internal.IImsRegistrationListener;
import com.android.ims.internal.IImsService;
import com.android.ims.internal.IImsSms;
import com.android.ims.internal.IImsUt;
import com.android.ims.internal.ImsCallSession;
import java.util.HashMap;

public class ImsManager {
    public static final String ACTION_IMS_INCOMING_CALL = "com.android.ims.IMS_INCOMING_CALL";
    public static final String ACTION_IMS_SERVICE_DOWN = "com.android.ims.IMS_SERVICE_DOWN";
    public static final String ACTION_IMS_SERVICE_UP = "com.android.ims.IMS_SERVICE_UP";
    private static final boolean DBG = true;
    public static final String EXTRA_CALL_ID = "android:imsCallID";
    public static final String EXTRA_PHONE_ID = "android:phone_id";
    public static final String EXTRA_SERVICE_ID = "android:imsServiceId";
    public static final String EXTRA_USSD = "android:ussd";
    private static final String IMS_SERVICE = "ims";
    public static final int INCOMING_CALL_RESULT_CODE = 101;
    public static final String PROPERTY_DBG_VOLTE_AVAIL_OVERRIDE = "persist.dbg.volte_avail_ovr";
    public static final int PROPERTY_DBG_VOLTE_AVAIL_OVERRIDE_DEFAULT = 1;
    public static final String PROPERTY_DBG_VT_AVAIL_OVERRIDE = "persist.dbg.vt_avail_ovr";
    public static final int PROPERTY_DBG_VT_AVAIL_OVERRIDE_DEFAULT = 1;
    private static final String TAG = "ImsManager";
    private Context mContext;
    private int mPhoneId;
    private static HashMap<Integer, ImsManager> sImsManagerInstances = new HashMap<>();
    private static ImsSms mImsSms = null;
    private static Registrant mImsSmsReadyRegistrant = null;
    private IImsService mImsService = null;
    private ImsServiceDeathRecipient mDeathRecipient = new ImsServiceDeathRecipient();
    private ImsUt mUt = null;
    private ImsConfig mConfig = null;
    private ImsEcbm mEcbm = null;

    public static ImsManager getInstance(Context context, int phoneId) {
        synchronized (sImsManagerInstances) {
            if (sImsManagerInstances.containsKey(Integer.valueOf(phoneId))) {
                return sImsManagerInstances.get(Integer.valueOf(phoneId));
            }
            ImsManager mgr = new ImsManager(context, phoneId);
            sImsManagerInstances.put(Integer.valueOf(phoneId), mgr);
            return mgr;
        }
    }

    public static boolean isEnhanced4gLteModeSettingEnabledByUser(Context context) {
        int enabled = Settings.Global.getInt(context.getContentResolver(), "volte_vt_enabled", 1);
        if (enabled == 1) {
            return DBG;
        }
        return false;
    }

    public static void setEnhanced4gLteModeSetting(Context context, boolean enabled) {
        ImsManager imsManager;
        int value = enabled ? 1 : 0;
        Settings.Global.putInt(context.getContentResolver(), "volte_vt_enabled", value);
        if (isNonTtyOrTtyOnVolteEnabled(context) && (imsManager = getInstance(context, SubscriptionManager.getDefaultVoicePhoneId())) != null) {
            try {
                imsManager.setAdvanced4GMode(enabled);
            } catch (ImsException e) {
            }
        }
    }

    public static boolean isNonTtyOrTtyOnVolteEnabled(Context context) {
        if (context.getResources().getBoolean(R.^attr-private.lightRadius) || Settings.Secure.getInt(context.getContentResolver(), "preferred_tty_mode", 0) == 0) {
            return DBG;
        }
        return false;
    }

    public static boolean isVolteEnabledByPlatform(Context context) {
        if (SystemProperties.getInt(PROPERTY_DBG_VOLTE_AVAIL_OVERRIDE, 1) == 1) {
            return DBG;
        }
        boolean disabledByGlobalSetting = Settings.Global.getInt(context.getContentResolver(), "volte_feature_disabled", 0) == 1;
        if (context.getResources().getBoolean(R.^attr-private.layout_removeBorders) && context.getResources().getBoolean(R.^attr-private.leftToRight) && !disabledByGlobalSetting) {
            return DBG;
        }
        return false;
    }

    public static boolean isVolteProvisionedOnDevice(Context context) {
        if (!context.getResources().getBoolean(R.^attr-private.legacyLayout)) {
            return DBG;
        }
        ImsManager mgr = getInstance(context, SubscriptionManager.getDefaultVoiceSubId());
        if (mgr == null) {
            return false;
        }
        try {
            ImsConfig config = mgr.getConfigInterface();
            if (config == null) {
                return false;
            }
            boolean isProvisioned = config.getVolteProvisioned();
            return isProvisioned;
        } catch (ImsException e) {
            return false;
        }
    }

    public static boolean isVtEnabledByPlatform(Context context) {
        if (SystemProperties.getInt(PROPERTY_DBG_VT_AVAIL_OVERRIDE, 1) == 1) {
            return DBG;
        }
        if (context.getResources().getBoolean(R.^attr-private.lightY) && context.getResources().getBoolean(R.^attr-private.lightZ)) {
            return DBG;
        }
        return false;
    }

    private ImsManager(Context context, int phoneId) {
        this.mContext = context;
        this.mPhoneId = phoneId;
        createImsService(DBG);
    }

    public int open(int serviceClass, PendingIntent incomingCallPendingIntent, ImsConnectionStateListener listener) throws ImsException {
        checkAndThrowExceptionIfServiceUnavailable();
        if (incomingCallPendingIntent == null) {
            throw new NullPointerException("incomingCallPendingIntent can't be null");
        }
        if (listener == null) {
            throw new NullPointerException("listener can't be null");
        }
        try {
            int result = this.mImsService.open(this.mPhoneId, serviceClass, incomingCallPendingIntent, createRegistrationListenerProxy(serviceClass, listener));
            if (result <= 0) {
                throw new ImsException("open()", result * (-1));
            }
            return result;
        } catch (RemoteException e) {
            throw new ImsException("open()", e, 106);
        }
    }

    public void close(int i) throws ImsException {
        checkAndThrowExceptionIfServiceUnavailable();
        try {
            try {
                this.mImsService.close(i);
            } catch (RemoteException e) {
                throw new ImsException("close()", e, 106);
            }
        } finally {
            this.mUt = null;
            this.mConfig = null;
            this.mEcbm = null;
        }
    }

    public ImsUtInterface getSupplementaryServiceConfiguration(int serviceId) throws ImsException {
        if (this.mUt == null) {
            checkAndThrowExceptionIfServiceUnavailable();
            try {
                IImsUt iUt = this.mImsService.getUtInterface(serviceId);
                if (iUt == null) {
                    throw new ImsException("getSupplementaryServiceConfiguration()", 801);
                }
                this.mUt = new ImsUt(iUt);
            } catch (RemoteException e) {
                throw new ImsException("getSupplementaryServiceConfiguration()", e, 106);
            }
        }
        return this.mUt;
    }

    public boolean isConnected(int serviceId, int serviceType, int callType) throws ImsException {
        checkAndThrowExceptionIfServiceUnavailable();
        try {
            return this.mImsService.isConnected(serviceId, serviceType, callType);
        } catch (RemoteException e) {
            throw new ImsException("isServiceConnected()", e, 106);
        }
    }

    public boolean isOpened(int serviceId) throws ImsException {
        checkAndThrowExceptionIfServiceUnavailable();
        try {
            return this.mImsService.isOpened(serviceId);
        } catch (RemoteException e) {
            throw new ImsException("isOpened()", e, 106);
        }
    }

    public ImsCallProfile createCallProfile(int serviceId, int serviceType, int callType) throws ImsException {
        checkAndThrowExceptionIfServiceUnavailable();
        try {
            return this.mImsService.createCallProfile(serviceId, serviceType, callType);
        } catch (RemoteException e) {
            throw new ImsException("createCallProfile()", e, 106);
        }
    }

    public ImsCall makeCall(int serviceId, ImsCallProfile profile, String[] callees, ImsCall.Listener listener) throws ImsException {
        log("makeCall :: serviceId=" + serviceId + ", profile=" + profile + ", callees=" + callees);
        checkAndThrowExceptionIfServiceUnavailable();
        ImsCall call = new ImsCall(this.mContext, profile);
        call.setListener(listener);
        ImsCallSession session = createCallSession(serviceId, profile);
        if (callees != null && callees.length == 1) {
            call.start(session, callees[0]);
        } else {
            call.start(session, callees);
        }
        return call;
    }

    public ImsCall takeCall(int serviceId, Intent incomingCallIntent, ImsCall.Listener listener) throws ImsException {
        log("takeCall :: serviceId=" + serviceId + ", incomingCall=" + incomingCallIntent);
        checkAndThrowExceptionIfServiceUnavailable();
        if (incomingCallIntent == null) {
            throw new ImsException("Can't retrieve session with null intent", INCOMING_CALL_RESULT_CODE);
        }
        int incomingServiceId = getServiceId(incomingCallIntent);
        if (serviceId != incomingServiceId) {
            throw new ImsException("Service id is mismatched in the incoming call intent", INCOMING_CALL_RESULT_CODE);
        }
        String callId = getCallId(incomingCallIntent);
        if (callId == null) {
            throw new ImsException("Call ID missing in the incoming call intent", INCOMING_CALL_RESULT_CODE);
        }
        try {
            IImsCallSession session = this.mImsService.getPendingCallSession(serviceId, callId);
            if (session == null) {
                throw new ImsException("No pending session for the call", 107);
            }
            ImsCall call = new ImsCall(this.mContext, session.getCallProfile());
            call.attachSession(new ImsCallSession(session));
            call.setListener(listener);
            return call;
        } catch (Throwable t) {
            throw new ImsException("takeCall()", t, 0);
        }
    }

    public ImsConfig getConfigInterface() throws ImsException {
        if (this.mConfig == null) {
            checkAndThrowExceptionIfServiceUnavailable();
            try {
                IImsConfig config = this.mImsService.getConfigInterface(this.mPhoneId);
                if (config == null) {
                    throw new ImsException("getConfigInterface()", 131);
                }
                this.mConfig = new ImsConfig(config, this.mContext);
            } catch (RemoteException e) {
                throw new ImsException("getConfigInterface()", e, 106);
            }
        }
        log("getConfigInterface(), mConfig= " + this.mConfig);
        return this.mConfig;
    }

    public void setUiTTYMode(Context context, int serviceId, int uiTtyMode, Message onComplete) throws ImsException {
        checkAndThrowExceptionIfServiceUnavailable();
        try {
            this.mImsService.setUiTTYMode(serviceId, uiTtyMode, onComplete);
            if (!context.getResources().getBoolean(R.^attr-private.lightRadius)) {
                setAdvanced4GMode((uiTtyMode == 0 && isEnhanced4gLteModeSettingEnabledByUser(context)) ? DBG : false);
            }
        } catch (RemoteException e) {
            throw new ImsException("setTTYMode()", e, 106);
        }
    }

    private static String getCallId(Intent incomingCallIntent) {
        if (incomingCallIntent == null) {
            return null;
        }
        return incomingCallIntent.getStringExtra(EXTRA_CALL_ID);
    }

    private static int getServiceId(Intent incomingCallIntent) {
        if (incomingCallIntent == null) {
            return -1;
        }
        return incomingCallIntent.getIntExtra(EXTRA_SERVICE_ID, -1);
    }

    private void checkAndThrowExceptionIfServiceUnavailable() throws ImsException {
        if (this.mImsService == null) {
            createImsService(DBG);
            if (this.mImsService == null) {
                throw new ImsException("Service is unavailable", 106);
            }
        }
    }

    private static String getImsServiceName(int phoneId) {
        return IMS_SERVICE;
    }

    private void createImsService(boolean checkService) {
        if (checkService) {
            IBinder binder = ServiceManager.checkService(getImsServiceName(this.mPhoneId));
            if (binder == null) {
                return;
            }
        }
        IBinder b = ServiceManager.getService(getImsServiceName(this.mPhoneId));
        if (b != null) {
            try {
                b.linkToDeath(this.mDeathRecipient, 0);
            } catch (RemoteException e) {
            }
        }
        this.mImsService = IImsService.Stub.asInterface(b);
    }

    private ImsCallSession createCallSession(int serviceId, ImsCallProfile profile) throws ImsException {
        try {
            return new ImsCallSession(this.mImsService.createCallSession(serviceId, profile, (IImsCallSessionListener) null));
        } catch (RemoteException e) {
            return null;
        }
    }

    private ImsRegistrationListenerProxy createRegistrationListenerProxy(int serviceClass, ImsConnectionStateListener listener) {
        ImsRegistrationListenerProxy proxy = new ImsRegistrationListenerProxy(serviceClass, listener);
        return proxy;
    }

    private void log(String s) {
        Rlog.d(TAG, s);
    }

    private void loge(String s) {
        Rlog.e(TAG, s);
    }

    private void loge(String s, Throwable t) {
        Rlog.e(TAG, s, t);
    }

    public void turnOnIms() throws ImsException {
        checkAndThrowExceptionIfServiceUnavailable();
        try {
            this.mImsService.turnOnIms(this.mPhoneId);
        } catch (RemoteException e) {
            throw new ImsException("turnOnIms() ", e, 106);
        }
    }

    private void setAdvanced4GMode(boolean turnOn) throws ImsException {
        checkAndThrowExceptionIfServiceUnavailable();
        ImsConfig config = getConfigInterface();
        if (config != null) {
            config.setFeatureValue(0, 13, turnOn ? 1 : 0, null);
            if (isVtEnabledByPlatform(this.mContext)) {
                config.setFeatureValue(1, 13, turnOn ? 1 : 0, null);
            }
        }
        if (turnOn) {
            turnOnIms();
        } else if (this.mContext.getResources().getBoolean(R.^attr-private.layout_maxHeight)) {
            log("setAdvanced4GMode() : imsServiceAllowTurnOff -> turnOffIms");
            turnOffIms();
        }
    }

    public void turnOffIms() throws ImsException {
        checkAndThrowExceptionIfServiceUnavailable();
        try {
            this.mImsService.turnOffIms(this.mPhoneId);
        } catch (RemoteException e) {
            throw new ImsException("turnOffIms() ", e, 106);
        }
    }

    private class ImsServiceDeathRecipient implements IBinder.DeathRecipient {
        private ImsServiceDeathRecipient() {
        }

        @Override
        public void binderDied() {
            ImsManager.this.mImsService = null;
            ImsManager.this.mUt = null;
            ImsManager.this.mConfig = null;
            ImsManager.this.mEcbm = null;
            ImsSms unused = ImsManager.mImsSms = null;
            if (ImsManager.this.mContext != null) {
                Intent intent = new Intent(ImsManager.ACTION_IMS_SERVICE_DOWN);
                intent.putExtra(ImsManager.EXTRA_PHONE_ID, ImsManager.this.mPhoneId);
                ImsManager.this.mContext.sendBroadcast(new Intent(intent));
            }
        }
    }

    private class ImsRegistrationListenerProxy extends IImsRegistrationListener.Stub {
        private ImsConnectionStateListener mListener;
        private int mServiceClass;

        public ImsRegistrationListenerProxy(int serviceClass, ImsConnectionStateListener listener) {
            this.mServiceClass = serviceClass;
            this.mListener = listener;
        }

        public boolean isSameProxy(int serviceClass) {
            if (this.mServiceClass == serviceClass) {
                return ImsManager.DBG;
            }
            return false;
        }

        public void registrationConnected() {
            ImsManager.this.log("registrationConnected ::");
            if (this.mListener != null) {
                this.mListener.onImsConnected();
            }
        }

        public void registrationDisconnected() {
            ImsManager.this.log("registrationDisconnected ::");
            if (this.mListener != null) {
                this.mListener.onImsDisconnected();
            }
        }

        public void registrationResumed() {
            ImsManager.this.log("registrationResumed ::");
            if (this.mListener != null) {
                this.mListener.onImsResumed();
            }
        }

        public void registrationSuspended() {
            ImsManager.this.log("registrationSuspended ::");
            if (this.mListener != null) {
                this.mListener.onImsSuspended();
            }
        }

        public void registrationServiceCapabilityChanged(int serviceClass, int event) {
            ImsManager.this.log("registrationServiceCapabilityChanged :: serviceClass=" + serviceClass + ", event=" + event);
            if (this.mListener != null) {
                this.mListener.onImsConnected();
            }
        }

        public void registrationFeatureCapabilityChanged(int serviceClass, int[] enabledFeatures, int[] disabledFeatures) {
            ImsManager.this.log("registrationFeatureCapabilityChanged :: serviceClass=" + serviceClass);
            if (this.mListener != null) {
                this.mListener.onFeatureCapabilityChanged(serviceClass, enabledFeatures, disabledFeatures);
            }
        }
    }

    public ImsEcbm getEcbmInterface(int serviceId) throws ImsException {
        if (this.mEcbm == null) {
            checkAndThrowExceptionIfServiceUnavailable();
            try {
                IImsEcbm iEcbm = this.mImsService.getEcbmInterface(serviceId);
                if (iEcbm == null) {
                    throw new ImsException("getEcbmInterface()", 901);
                }
                this.mEcbm = new ImsEcbm(iEcbm);
            } catch (RemoteException e) {
                throw new ImsException("getEcbmInterface()", e, 106);
            }
        }
        return this.mEcbm;
    }

    public ImsSms getSmsInterface(int serviceId) throws ImsException {
        if (mImsSms == null) {
            checkAndThrowExceptionIfServiceUnavailable();
            try {
                IImsSms iImsSms = this.mImsService.getSmsInterface(serviceId);
                if (iImsSms == null) {
                    throw new ImsException("getSmsInterface()", 1001);
                }
                mImsSms = new ImsSms(this.mContext, iImsSms);
                log("getSmsInterface mImsSms=" + mImsSms + " mImsSmsReadyRegistrant=" + mImsSmsReadyRegistrant);
                if (mImsSmsReadyRegistrant != null) {
                    mImsSmsReadyRegistrant.notifyResult(Boolean.TRUE);
                }
            } catch (RemoteException e) {
                throw new ImsException("getSmsInterface()", e, 106);
            }
        }
        return mImsSms;
    }

    public ImsSms getSmsInterface() {
        return mImsSms;
    }

    public void registerForImsSmsReady(Handler h, int what, Object obj) {
        mImsSmsReadyRegistrant = new Registrant(h, what, obj);
    }

    public void unregisterForImsSmsReady(Handler h) {
        mImsSmsReadyRegistrant.clear();
    }
}
