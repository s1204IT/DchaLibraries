package com.android.internal.telephony.ims;

import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.Uri;
import android.os.Message;
import android.os.RemoteException;
import android.telephony.ims.ImsCallProfile;
import android.telephony.ims.ImsReasonInfo;
import android.telephony.ims.feature.CapabilityChangeRequest;
import android.telephony.ims.feature.ImsFeature;
import android.telephony.ims.feature.MmTelFeature;
import android.util.Log;
import com.android.ims.ImsConfigListener;
import com.android.ims.internal.IImsCallSession;
import com.android.ims.internal.IImsConfig;
import com.android.ims.internal.IImsEcbm;
import com.android.ims.internal.IImsMultiEndpoint;
import com.android.ims.internal.IImsRegistrationListener;
import com.android.ims.internal.IImsUt;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

public class MmTelFeatureCompatAdapter extends MmTelFeature {
    public static final String ACTION_IMS_INCOMING_CALL = "com.android.ims.IMS_INCOMING_CALL";
    public static final int FEATURE_DISABLED = 0;
    public static final int FEATURE_ENABLED = 1;
    public static final int FEATURE_TYPE_UNKNOWN = -1;
    public static final int FEATURE_TYPE_UT_OVER_LTE = 4;
    public static final int FEATURE_TYPE_UT_OVER_WIFI = 5;
    public static final int FEATURE_TYPE_VIDEO_OVER_LTE = 1;
    public static final int FEATURE_TYPE_VIDEO_OVER_WIFI = 3;
    public static final int FEATURE_TYPE_VOICE_OVER_LTE = 0;
    public static final int FEATURE_TYPE_VOICE_OVER_WIFI = 2;
    public static final int FEATURE_UNKNOWN = -1;
    private static final Map<Integer, Integer> REG_TECH_TO_NET_TYPE = new HashMap(2);
    private static final String TAG = "MmTelFeatureCompat";
    private static final int WAIT_TIMEOUT_MS = 2000;
    protected final MmTelInterfaceAdapter mCompatFeature;
    private ImsRegistrationCompatAdapter mRegCompatAdapter;
    protected int mSessionId = -1;
    protected IImsRegistrationListener mListener = new IImsRegistrationListener.Stub() {
        public void registrationConnected() throws RemoteException {
        }

        public void registrationProgressing() throws RemoteException {
        }

        public void registrationConnectedWithRadioTech(int i) throws RemoteException {
        }

        public void registrationProgressingWithRadioTech(int i) throws RemoteException {
        }

        public void registrationDisconnected(ImsReasonInfo imsReasonInfo) throws RemoteException {
            Log.i(MmTelFeatureCompatAdapter.TAG, "registrationDisconnected: resetting MMTEL capabilities.");
            MmTelFeatureCompatAdapter.this.notifyCapabilitiesStatusChanged(new MmTelFeature.MmTelCapabilities());
        }

        public void registrationResumed() throws RemoteException {
        }

        public void registrationSuspended() throws RemoteException {
        }

        public void registrationServiceCapabilityChanged(int i, int i2) throws RemoteException {
        }

        public void registrationFeatureCapabilityChanged(int i, int[] iArr, int[] iArr2) throws RemoteException {
            MmTelFeatureCompatAdapter.this.notifyCapabilitiesStatusChanged(MmTelFeatureCompatAdapter.this.convertCapabilities(iArr));
        }

        public void voiceMessageCountUpdate(int i) throws RemoteException {
            MmTelFeatureCompatAdapter.this.notifyVoiceMessageCountUpdate(i);
        }

        public void registrationAssociatedUriChanged(Uri[] uriArr) throws RemoteException {
        }

        public void registrationChangeFailed(int i, ImsReasonInfo imsReasonInfo) throws RemoteException {
        }
    };
    protected BroadcastReceiver mReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            Log.i(MmTelFeatureCompatAdapter.TAG, "onReceive");
            if (intent.getAction().equals(MmTelFeatureCompatAdapter.ACTION_IMS_INCOMING_CALL)) {
                Log.i(MmTelFeatureCompatAdapter.TAG, "onReceive : incoming call intent.");
                try {
                    MmTelFeatureCompatAdapter.this.notifyIncomingCallSession(MmTelFeatureCompatAdapter.this.mCompatFeature.getPendingCallSession(MmTelFeatureCompatAdapter.this.mSessionId, intent.getStringExtra("android:imsCallID")), intent.getExtras());
                } catch (RemoteException e) {
                    Log.w(MmTelFeatureCompatAdapter.TAG, "onReceive: Couldn't get Incoming call session.");
                }
            }
        }
    };

    static {
        REG_TECH_TO_NET_TYPE.put(0, 13);
        REG_TECH_TO_NET_TYPE.put(1, 18);
    }

    private static class ConfigListener extends ImsConfigListener.Stub {
        private final int mCapability;
        private final CountDownLatch mLatch;
        private final int mTech;

        public ConfigListener(int i, int i2, CountDownLatch countDownLatch) {
            this.mCapability = i;
            this.mTech = i2;
            this.mLatch = countDownLatch;
        }

        public void onGetFeatureResponse(int i, int i2, int i3, int i4) throws RemoteException {
            if (i == this.mCapability && i2 == this.mTech) {
                this.mLatch.countDown();
                getFeatureValueReceived(i3);
                return;
            }
            Log.i(MmTelFeatureCompatAdapter.TAG, "onGetFeatureResponse: response different than requested: feature=" + i + " and network=" + i2);
        }

        public void onSetFeatureResponse(int i, int i2, int i3, int i4) throws RemoteException {
            if (i == this.mCapability && i2 == this.mTech) {
                this.mLatch.countDown();
                setFeatureValueReceived(i3);
                return;
            }
            Log.i(MmTelFeatureCompatAdapter.TAG, "onSetFeatureResponse: response different than requested: feature=" + i + " and network=" + i2);
        }

        public void onGetVideoQuality(int i, int i2) throws RemoteException {
        }

        public void onSetVideoQuality(int i) throws RemoteException {
        }

        public void getFeatureValueReceived(int i) {
        }

        public void setFeatureValueReceived(int i) {
        }
    }

    private class ImsRegistrationListenerBase extends IImsRegistrationListener.Stub {
        private ImsRegistrationListenerBase() {
        }

        public void registrationConnected() throws RemoteException {
        }

        public void registrationProgressing() throws RemoteException {
        }

        public void registrationConnectedWithRadioTech(int i) throws RemoteException {
        }

        public void registrationProgressingWithRadioTech(int i) throws RemoteException {
        }

        public void registrationDisconnected(ImsReasonInfo imsReasonInfo) throws RemoteException {
        }

        public void registrationResumed() throws RemoteException {
        }

        public void registrationSuspended() throws RemoteException {
        }

        public void registrationServiceCapabilityChanged(int i, int i2) throws RemoteException {
        }

        public void registrationFeatureCapabilityChanged(int i, int[] iArr, int[] iArr2) throws RemoteException {
        }

        public void voiceMessageCountUpdate(int i) throws RemoteException {
        }

        public void registrationAssociatedUriChanged(Uri[] uriArr) throws RemoteException {
        }

        public void registrationChangeFailed(int i, ImsReasonInfo imsReasonInfo) throws RemoteException {
        }
    }

    public MmTelFeatureCompatAdapter(Context context, int i, MmTelInterfaceAdapter mmTelInterfaceAdapter) {
        initialize(context, i);
        this.mCompatFeature = mmTelInterfaceAdapter;
    }

    public boolean queryCapabilityConfiguration(int i, int i2) {
        int iConvertCapability = convertCapability(i, i2);
        CountDownLatch countDownLatch = new CountDownLatch(1);
        final int[] iArr = {-1};
        int iIntValue = REG_TECH_TO_NET_TYPE.getOrDefault(Integer.valueOf(i2), -1).intValue();
        try {
            this.mCompatFeature.getConfigInterface().getFeatureValue(iConvertCapability, iIntValue, new ConfigListener(iConvertCapability, iIntValue, countDownLatch) {
                @Override
                public void getFeatureValueReceived(int i3) {
                    iArr[0] = i3;
                }
            });
        } catch (RemoteException e) {
            Log.w(TAG, "queryCapabilityConfiguration");
        }
        try {
            countDownLatch.await(2000L, TimeUnit.MILLISECONDS);
        } catch (InterruptedException e2) {
            Log.w(TAG, "queryCapabilityConfiguration - error waiting: " + e2.getMessage());
        }
        return iArr[0] == 1;
    }

    public void changeEnabledCapabilities(CapabilityChangeRequest capabilityChangeRequest, final ImsFeature.CapabilityCallbackProxy capabilityCallbackProxy) {
        if (capabilityChangeRequest == null) {
            return;
        }
        try {
            IImsConfig configInterface = this.mCompatFeature.getConfigInterface();
            for (final CapabilityChangeRequest.CapabilityPair capabilityPair : capabilityChangeRequest.getCapabilitiesToDisable()) {
                CountDownLatch countDownLatch = new CountDownLatch(1);
                int iConvertCapability = convertCapability(capabilityPair.getCapability(), capabilityPair.getRadioTech());
                int iIntValue = REG_TECH_TO_NET_TYPE.getOrDefault(Integer.valueOf(capabilityPair.getRadioTech()), -1).intValue();
                Log.i(TAG, "changeEnabledCapabilities - cap: " + iConvertCapability + " radioTech: " + iIntValue + " disabled");
                configInterface.setFeatureValue(iConvertCapability, iIntValue, 0, new ConfigListener(iConvertCapability, iIntValue, countDownLatch) {
                    @Override
                    public void setFeatureValueReceived(int i) {
                        if (i != 0) {
                            if (capabilityCallbackProxy == null) {
                                return;
                            } else {
                                capabilityCallbackProxy.onChangeCapabilityConfigurationError(capabilityPair.getCapability(), capabilityPair.getRadioTech(), -1);
                            }
                        }
                        Log.i(MmTelFeatureCompatAdapter.TAG, "changeEnabledCapabilities - setFeatureValueReceived with value " + i);
                    }
                });
                countDownLatch.await(2000L, TimeUnit.MILLISECONDS);
            }
            for (final CapabilityChangeRequest.CapabilityPair capabilityPair2 : capabilityChangeRequest.getCapabilitiesToEnable()) {
                CountDownLatch countDownLatch2 = new CountDownLatch(1);
                int iConvertCapability2 = convertCapability(capabilityPair2.getCapability(), capabilityPair2.getRadioTech());
                int iIntValue2 = REG_TECH_TO_NET_TYPE.getOrDefault(Integer.valueOf(capabilityPair2.getRadioTech()), -1).intValue();
                Log.i(TAG, "changeEnabledCapabilities - cap: " + iConvertCapability2 + " radioTech: " + iIntValue2 + " enabled");
                configInterface.setFeatureValue(iConvertCapability2, iIntValue2, 1, new ConfigListener(iConvertCapability2, iIntValue2, countDownLatch2) {
                    @Override
                    public void setFeatureValueReceived(int i) {
                        if (i != 1) {
                            if (capabilityCallbackProxy == null) {
                                return;
                            } else {
                                capabilityCallbackProxy.onChangeCapabilityConfigurationError(capabilityPair2.getCapability(), capabilityPair2.getRadioTech(), -1);
                            }
                        }
                        Log.i(MmTelFeatureCompatAdapter.TAG, "changeEnabledCapabilities - setFeatureValueReceived with value " + i);
                    }
                });
                countDownLatch2.await(2000L, TimeUnit.MILLISECONDS);
            }
        } catch (RemoteException | InterruptedException e) {
            Log.w(TAG, "changeEnabledCapabilities: Error processing: " + e.getMessage());
        }
    }

    public ImsCallProfile createCallProfile(int i, int i2) {
        try {
            return this.mCompatFeature.createCallProfile(this.mSessionId, i, i2);
        } catch (RemoteException e) {
            throw new RuntimeException(e.getMessage());
        }
    }

    public IImsCallSession createCallSessionInterface(ImsCallProfile imsCallProfile) throws RemoteException {
        return this.mCompatFeature.createCallSession(this.mSessionId, imsCallProfile);
    }

    public IImsUt getUtInterface() throws RemoteException {
        return this.mCompatFeature.getUtInterface();
    }

    public IImsEcbm getEcbmInterface() throws RemoteException {
        return this.mCompatFeature.getEcbmInterface();
    }

    public IImsMultiEndpoint getMultiEndpointInterface() throws RemoteException {
        return this.mCompatFeature.getMultiEndpointInterface();
    }

    public int getFeatureState() {
        try {
            return this.mCompatFeature.getFeatureState();
        } catch (RemoteException e) {
            throw new RuntimeException(e.getMessage());
        }
    }

    public void setUiTtyMode(int i, Message message) {
        try {
            this.mCompatFeature.setUiTTYMode(i, message);
        } catch (RemoteException e) {
            throw new RuntimeException(e.getMessage());
        }
    }

    public void onFeatureRemoved() {
        this.mContext.unregisterReceiver(this.mReceiver);
        try {
            this.mCompatFeature.endSession(this.mSessionId);
            this.mCompatFeature.removeRegistrationListener(this.mListener);
            if (this.mRegCompatAdapter != null) {
                this.mCompatFeature.removeRegistrationListener(this.mRegCompatAdapter.getRegistrationListener());
            }
        } catch (RemoteException e) {
            Log.w(TAG, "onFeatureRemoved: Couldn't end session: " + e.getMessage());
        }
    }

    public void onFeatureReady() {
        Log.i(TAG, "onFeatureReady called!");
        this.mContext.registerReceiver(this.mReceiver, new IntentFilter(ACTION_IMS_INCOMING_CALL));
        try {
            this.mSessionId = this.mCompatFeature.startSession(createIncomingCallPendingIntent(), new ImsRegistrationListenerBase());
            this.mCompatFeature.addRegistrationListener(this.mListener);
            this.mCompatFeature.addRegistrationListener(this.mRegCompatAdapter.getRegistrationListener());
        } catch (RemoteException e) {
            Log.e(TAG, "Couldn't start compat feature: " + e.getMessage());
        }
    }

    public void enableIms() throws RemoteException {
        this.mCompatFeature.turnOnIms();
    }

    public void disableIms() throws RemoteException {
        this.mCompatFeature.turnOffIms();
    }

    public IImsConfig getOldConfigInterface() {
        try {
            return this.mCompatFeature.getConfigInterface();
        } catch (RemoteException e) {
            Log.w(TAG, "getOldConfigInterface(): " + e.getMessage());
            return null;
        }
    }

    public void addRegistrationAdapter(ImsRegistrationCompatAdapter imsRegistrationCompatAdapter) throws RemoteException {
        this.mRegCompatAdapter = imsRegistrationCompatAdapter;
    }

    protected MmTelFeature.MmTelCapabilities convertCapabilities(int[] iArr) {
        boolean[] zArr = new boolean[iArr.length];
        for (int i = 0; i <= 5 && i < iArr.length; i++) {
            if (iArr[i] == i) {
                zArr[i] = true;
            } else if (iArr[i] == -1) {
                zArr[i] = false;
            }
        }
        MmTelFeature.MmTelCapabilities mmTelCapabilities = new MmTelFeature.MmTelCapabilities();
        if (zArr[0] || zArr[2]) {
            mmTelCapabilities.addCapabilities(1);
        }
        if (zArr[1] || zArr[3]) {
            mmTelCapabilities.addCapabilities(2);
        }
        if (zArr[4] || zArr[5]) {
            mmTelCapabilities.addCapabilities(4);
        }
        Log.i(TAG, "convertCapabilities - capabilities: " + mmTelCapabilities);
        return mmTelCapabilities;
    }

    private PendingIntent createIncomingCallPendingIntent() {
        Intent intent = new Intent(ACTION_IMS_INCOMING_CALL);
        intent.setPackage("com.android.phone");
        return PendingIntent.getBroadcast(this.mContext, 0, intent, 134217728);
    }

    private int convertCapability(int i, int i2) {
        int i3 = 1;
        if (i2 == 0) {
            if (i == 4) {
                return 4;
            }
            switch (i) {
                case 1:
                    i3 = 0;
                    break;
                case 2:
                    break;
                default:
                    return -1;
            }
            return i3;
        }
        if (i2 != 1) {
            return -1;
        }
        if (i != 4) {
            switch (i) {
                case 1:
                    return 2;
                case 2:
                    return 3;
                default:
                    return -1;
            }
        }
        return 5;
    }
}
