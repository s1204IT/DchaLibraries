package com.android.services.telephony;

import android.content.ActivityNotFoundException;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.Message;
import android.os.RemoteException;
import android.os.SystemProperties;
import android.provider.Settings;
import android.telecom.Conference;
import android.telecom.Connection;
import android.telecom.ConnectionRequest;
import android.telecom.ConnectionService;
import android.telecom.ConnectionServiceAdapter;
import android.telecom.DisconnectCause;
import android.telecom.Logging.Session;
import android.telecom.ParcelableConference;
import android.telecom.ParcelableConnection;
import android.telecom.PhoneAccountHandle;
import android.telecom.VideoProfile;
import android.telephony.CarrierConfigManager;
import android.telephony.PhoneNumberUtils;
import android.telephony.RadioAccessFamily;
import android.telephony.SubscriptionManager;
import android.telephony.TelephonyManager;
import android.text.TextUtils;
import android.util.Pair;
import com.android.internal.annotations.VisibleForTesting;
import com.android.internal.os.SomeArgs;
import com.android.internal.telecom.IVideoProvider;
import com.android.internal.telephony.Call;
import com.android.internal.telephony.CallStateException;
import com.android.internal.telephony.GsmCdmaPhone;
import com.android.internal.telephony.IccCard;
import com.android.internal.telephony.IccCardConstants;
import com.android.internal.telephony.Phone;
import com.android.internal.telephony.PhoneConstants;
import com.android.internal.telephony.PhoneFactory;
import com.android.internal.telephony.SubscriptionController;
import com.android.internal.telephony.TelephonyDevController;
import com.android.internal.telephony.imsphone.ImsExternalCallTracker;
import com.android.internal.telephony.imsphone.ImsPhone;
import com.android.internal.telephony.imsphone.ImsPhoneConnection;
import com.android.phone.MMIDialogActivity;
import com.android.phone.PhoneUtils;
import com.android.phone.R;
import com.android.phone.TimeConsumingPreferenceActivity;
import com.android.services.telephony.RadioOnStateListener;
import com.android.services.telephony.TelephonyConnection;
import com.android.services.telephony.TelephonyConnectionService;
import com.mediatek.internal.telecom.IMtkConnectionService;
import com.mediatek.internal.telecom.IMtkConnectionServiceAdapter;
import com.mediatek.internal.telephony.MtkGsmCdmaPhone;
import com.mediatek.phone.ext.ExtensionManager;
import com.mediatek.services.telephony.MtkGsmCdmaConnection;
import com.mediatek.services.telephony.MtkTelephonyConnectionServiceUtil;
import com.mediatek.services.telephony.SwitchPhoneHelper;
import com.mediatek.telephony.MtkTelephonyManagerEx;
import java.lang.ref.WeakReference;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
import java.util.regex.Pattern;
import mediatek.telecom.MtkConnection;

public class TelephonyConnectionService extends ConnectionService {
    private static final Pattern CDMA_ACTIVATION_CODE_REGEX_PATTERN = Pattern.compile("\\*228[0-9]{0,2}");
    private static final int MSG_BLIND_ASSURED_ECT = 1005;
    private static final int MSG_CANCEL_DEVICE_SWITCH = 1007;
    private static final int MSG_CREATE_CONFERENCE = 1004;
    private static final int MSG_DEVICE_SWITCH = 1006;
    private static final int MSG_ECT = 1000;
    private static final int MSG_HANDLE_ORDERED_USER_OPERATION = 1002;
    private static final int MSG_HANGUP_ALL = 1001;
    private static final int MSG_INVITE_CONFERENCE_PARTICIPANTS = 1003;
    private static final int MTK_MSG_BASE = 1000;
    private static final String TAG = "TeleConnService";

    @VisibleForTesting
    public Pair<WeakReference<TelephonyConnection>, Queue<Phone>> mEmergencyRetryCache;
    private EmergencyTonePlayer mEmergencyTonePlayer;
    private HoldTracker mHoldTracker;
    private RadioOnHelper mRadioOnHelper;
    private SwitchPhoneHelper mSwitchPhoneHelper;
    private final TelephonyConnectionServiceProxy mTelephonyConnectionServiceProxy = new TelephonyConnectionServiceProxy() {
        @Override
        public Collection<Connection> getAllConnections() {
            return TelephonyConnectionService.this.getAllConnections();
        }

        @Override
        public void addConference(TelephonyConference telephonyConference) {
            TelephonyConnectionService.this.addConference(telephonyConference);
        }

        @Override
        public void addConference(ImsConference imsConference) {
            TelephonyConnectionService.this.addConference(imsConference);
        }

        @Override
        public void removeConnection(Connection connection) {
            TelephonyConnectionService.this.removeConnection(connection);
        }

        @Override
        public void addExistingConnection(PhoneAccountHandle phoneAccountHandle, Connection connection) {
            TelephonyConnectionService.this.addExistingConnection(phoneAccountHandle, connection);
        }

        @Override
        public void addExistingConnection(PhoneAccountHandle phoneAccountHandle, Connection connection, Conference conference) {
            TelephonyConnectionService.this.addExistingConnection(phoneAccountHandle, connection, conference);
        }

        @Override
        public void addConnectionToConferenceController(TelephonyConnection telephonyConnection) {
            TelephonyConnectionService.this.addConnectionToConferenceController(telephonyConnection);
        }

        @Override
        public void performImsConferenceSRVCC(Conference conference, ArrayList<com.android.internal.telephony.Connection> arrayList, String str) {
            TelephonyConnectionService.this.performImsConferenceSRVCC(conference, arrayList, str);
        }
    };
    private final Connection.Listener mConnectionListener = new Connection.Listener() {
        public void onConferenceChanged(Connection connection, Conference conference) {
            TelephonyConnectionService.this.mHoldTracker.updateHoldCapability(connection.getPhoneAccountHandle());
        }
    };
    private final TelephonyConferenceController mTelephonyConferenceController = new TelephonyConferenceController(this.mTelephonyConnectionServiceProxy);
    private final CdmaConferenceController mCdmaConferenceController = new CdmaConferenceController(this);
    private final ImsConferenceController mImsConferenceController = new ImsConferenceController(TelecomAccountRegistry.getInstance(this), this.mTelephonyConnectionServiceProxy);
    private ComponentName mExpectedComponentName = null;
    private IMtkConnectionServiceAdapter mMtkAdapter = null;
    private SubscriptionManagerProxy mSubscriptionManagerProxy = new SubscriptionManagerProxy() {
        @Override
        public int getDefaultVoicePhoneId() {
            return SubscriptionManager.getDefaultVoicePhoneId();
        }

        @Override
        public int getSimStateForSlotIdx(int i) {
            return SubscriptionManager.getSimStateForSlotIndex(i);
        }

        @Override
        public int getPhoneId(int i) {
            return SubscriptionManager.getPhoneId(i);
        }
    };
    private TelephonyManagerProxy mTelephonyManagerProxy = new TelephonyManagerProxy() {
        private final TelephonyManager sTelephonyManager = TelephonyManager.getDefault();

        @Override
        public int getPhoneCount() {
            return this.sTelephonyManager.getPhoneCount();
        }

        @Override
        public boolean hasIccCard(int i) {
            return this.sTelephonyManager.hasIccCard(i);
        }
    };
    private PhoneFactoryProxy mPhoneFactoryProxy = new PhoneFactoryProxy() {
        @Override
        public Phone getPhone(int i) {
            return PhoneFactory.getPhone(i);
        }

        @Override
        public Phone getDefaultPhone() {
            return PhoneFactory.getDefaultPhone();
        }

        @Override
        public Phone[] getPhones() {
            return PhoneFactory.getPhones();
        }
    };
    TelephonyDevController mTelDevController = TelephonyDevController.getInstance();
    private final TelephonyConnection.TelephonyConnectionListener mTelephonyConnectionListener = new TelephonyConnection.TelephonyConnectionListener() {
        @Override
        public void onOriginalConnectionConfigured(TelephonyConnection telephonyConnection) {
            TelephonyConnectionService.this.addConnectionToConferenceController(telephonyConnection);
        }

        @Override
        public void onOriginalConnectionRetry(TelephonyConnection telephonyConnection, boolean z) {
            TelephonyConnectionService.this.retryOutgoingOriginalConnection(telephonyConnection, z);
        }
    };
    private MtkConnectionServiceBinder mMtkBinder = null;
    private final Handler mMtkHandler = new Handler(Looper.getMainLooper()) {
        @Override
        public void handleMessage(Message message) {
            SomeArgs someArgs;
            switch (message.what) {
                case TimeConsumingPreferenceActivity.STK_CC_SS_TO_DIAL_VIDEO_ERROR:
                    TelephonyConnectionService.this.explicitCallTransfer((String) message.obj);
                    return;
                case TelephonyConnectionService.MSG_HANGUP_ALL:
                    TelephonyConnectionService.this.hangupAll((String) message.obj);
                    return;
                case TelephonyConnectionService.MSG_HANDLE_ORDERED_USER_OPERATION:
                    someArgs = (SomeArgs) message.obj;
                    try {
                        String str = (String) someArgs.arg1;
                        String str2 = (String) someArgs.arg2;
                        String str3 = (String) someArgs.arg3;
                        if ("mediatek.telecom.operation.DISCONNECT_CALL".equals(str2)) {
                            TelephonyConnectionService.this.disconnect(str, str3);
                            break;
                        }
                        return;
                    } finally {
                    }
                case TelephonyConnectionService.MSG_INVITE_CONFERENCE_PARTICIPANTS:
                    someArgs = (SomeArgs) message.obj;
                    try {
                        TelephonyConnectionService.this.inviteConferenceParticipants((String) someArgs.arg1, (List) someArgs.arg2);
                        return;
                    } finally {
                    }
                case TelephonyConnectionService.MSG_CREATE_CONFERENCE:
                    someArgs = (SomeArgs) message.obj;
                    try {
                        final PhoneAccountHandle phoneAccountHandle = (PhoneAccountHandle) someArgs.arg1;
                        final String str4 = (String) someArgs.arg2;
                        final ConnectionRequest connectionRequest = (ConnectionRequest) someArgs.arg3;
                        final List list = (List) someArgs.arg4;
                        final boolean z = someArgs.argi1 == 1;
                        final Session.Info info = (Session.Info) someArgs.arg5;
                        if (TelephonyConnectionService.this.mAreAccountsInitialized) {
                            TelephonyConnectionService.this.createConference(phoneAccountHandle, str4, connectionRequest, list, z, info);
                        } else {
                            Log.d(this, "Enqueueing pre-init request %s", str4);
                            TelephonyConnectionService.this.mPreInitializationConnectionRequests.add(new Runnable() {
                                @Override
                                public void run() {
                                    TelephonyConnectionService.this.createConference(phoneAccountHandle, str4, connectionRequest, list, z, info);
                                }
                            });
                        }
                        return;
                    } finally {
                    }
                case TelephonyConnectionService.MSG_BLIND_ASSURED_ECT:
                    someArgs = (SomeArgs) message.obj;
                    try {
                        TelephonyConnectionService.this.explicitCallTransfer((String) someArgs.arg1, (String) someArgs.arg2, someArgs.argi1);
                        return;
                    } finally {
                    }
                case TelephonyConnectionService.MSG_DEVICE_SWITCH:
                    someArgs = (SomeArgs) message.obj;
                    try {
                        TelephonyConnectionService.this.deviceSwitch((String) someArgs.arg1, (String) someArgs.arg2, (String) someArgs.arg3);
                        return;
                    } finally {
                    }
                case TelephonyConnectionService.MSG_CANCEL_DEVICE_SWITCH:
                    try {
                        TelephonyConnectionService.this.cancelDeviceSwitch((String) ((SomeArgs) message.obj).arg1);
                        return;
                    } finally {
                    }
                default:
                    Log.d(this, "mMtkHandler default return (msg.what=%d)", Integer.valueOf(message.what));
                    return;
            }
        }
    };

    public interface PhoneFactoryProxy {
        Phone getDefaultPhone();

        Phone getPhone(int i);

        Phone[] getPhones();
    }

    public interface SubscriptionManagerProxy {
        int getDefaultVoicePhoneId();

        int getPhoneId(int i);

        int getSimStateForSlotIdx(int i);
    }

    public interface TelephonyManagerProxy {
        int getPhoneCount();

        boolean hasIccCard(int i);
    }

    private static class SlotStatus {
        public int capabilities;
        public boolean isLocked = false;
        public int slotId;

        public SlotStatus(int i, int i2) {
            this.slotId = i;
            this.capabilities = i2;
        }
    }

    @VisibleForTesting
    public void setSubscriptionManagerProxy(SubscriptionManagerProxy subscriptionManagerProxy) {
        this.mSubscriptionManagerProxy = subscriptionManagerProxy;
    }

    @VisibleForTesting
    public void setTelephonyManagerProxy(TelephonyManagerProxy telephonyManagerProxy) {
        this.mTelephonyManagerProxy = telephonyManagerProxy;
    }

    @VisibleForTesting
    public void setPhoneFactoryProxy(PhoneFactoryProxy phoneFactoryProxy) {
        this.mPhoneFactoryProxy = phoneFactoryProxy;
    }

    private boolean hasC2kOverImsModem() {
        return (this.mTelDevController == null || this.mTelDevController.getModem(0) == null || !this.mTelDevController.getModem(0).hasC2kOverImsModem()) ? false : true;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        Log.initLogging(this);
        this.mExpectedComponentName = new ComponentName(this, getClass());
        this.mEmergencyTonePlayer = new EmergencyTonePlayer(this);
        TelecomAccountRegistry.getInstance(this).setTelephonyConnectionService(this);
        this.mHoldTracker = new HoldTracker();
        MtkTelephonyConnectionServiceUtil.getInstance().setService(this);
    }

    @Override
    public void onDestroy() {
        MtkTelephonyConnectionServiceUtil.getInstance().setInEcc(false);
        MtkTelephonyConnectionServiceUtil.getInstance().unsetService();
        this.mCdmaConferenceController.onDestroy();
        if (this.mSwitchPhoneHelper != null) {
            this.mSwitchPhoneHelper.onDestroy();
        }
        if (this.mRadioOnHelper != null) {
            this.mRadioOnHelper.cleanup();
        }
        super.onDestroy();
    }

    @Override
    public Connection onCreateOutgoingConnection(PhoneAccountHandle phoneAccountHandle, final ConnectionRequest connectionRequest) {
        String schemeSpecificPart;
        Phone phoneForAccount;
        boolean z;
        Uri uriFromParts;
        boolean z2;
        boolean zIsLocalEmergencyNumber;
        boolean z3;
        Phone phoneForAccount2;
        Log.i(this, "onCreateOutgoingConnection, request: " + connectionRequest, new Object[0]);
        log("onCreateOutgoingConnection, handle:" + connectionRequest.getAccountHandle());
        Uri address = connectionRequest.getAddress();
        if (address == null) {
            Log.d(this, "onCreateOutgoingConnection, handle is null", new Object[0]);
            return Connection.createFailedConnection(DisconnectCauseUtil.toTelecomDisconnectCause(38, "No phone number supplied"));
        }
        if (connectionRequest.getAccountHandle() == null) {
            log("onCreateOutgoingConnection, PhoneAccountHandle is null");
            return Connection.createFailedConnection(DisconnectCauseUtil.toTelecomDisconnectCause(38, "No phone number supplied"));
        }
        int phoneId = -1;
        if (MtkTelephonyConnectionServiceUtil.getInstance().isEccRetryOn()) {
            try {
            } catch (NumberFormatException e) {
            } catch (Throwable th) {
                if (PhoneFactory.getPhone(-1) != null) {
                    throw th;
                }
            }
        }
        String scheme = address.getScheme();
        if ("voicemail".equals(scheme)) {
            Phone phoneForAccount3 = getPhoneForAccount(connectionRequest.getAccountHandle(), false);
            if (phoneForAccount3 == null) {
                Log.d(this, "onCreateOutgoingConnection, phone is null", new Object[0]);
                if (MtkTelephonyConnectionServiceUtil.getInstance().isEccRetryOn()) {
                    Log.i(this, "ECC retry: clear ECC param", new Object[0]);
                    MtkTelephonyConnectionServiceUtil.getInstance().clearEccRetryParams();
                }
                return Connection.createFailedConnection(DisconnectCauseUtil.toTelecomDisconnectCause(18, "Phone is null"));
            }
            schemeSpecificPart = phoneForAccount3.getVoiceMailNumber();
            if (TextUtils.isEmpty(schemeSpecificPart)) {
                Log.d(this, "onCreateOutgoingConnection, no voicemail number set.", new Object[0]);
                return Connection.createFailedConnection(DisconnectCauseUtil.toTelecomDisconnectCause(40, "Voicemail scheme provided but no voicemail number set."));
            }
            address = Uri.fromParts("tel", schemeSpecificPart, null);
        } else {
            if (!"tel".equals(scheme) && !"sip".equals(scheme)) {
                Log.d(this, "onCreateOutgoingConnection, Handle %s is not type tel", scheme);
                return Connection.createFailedConnection(DisconnectCauseUtil.toTelecomDisconnectCause(7, "Handle scheme is not type tel"));
            }
            schemeSpecificPart = address.getSchemeSpecificPart();
            if (TextUtils.isEmpty(schemeSpecificPart)) {
                Log.d(this, "onCreateOutgoingConnection, unable to parse number", new Object[0]);
                return Connection.createFailedConnection(DisconnectCauseUtil.toTelecomDisconnectCause(7, "Unable to parse number"));
            }
            if (!MtkTelephonyConnectionServiceUtil.getInstance().isEccRetryOn()) {
                phoneForAccount = getPhoneForAccount(connectionRequest.getAccountHandle(), false);
            } else {
                phoneForAccount = null;
            }
            if (phoneForAccount != null && CDMA_ACTIVATION_CODE_REGEX_PATTERN.matcher(schemeSpecificPart).matches()) {
                CarrierConfigManager carrierConfigManager = (CarrierConfigManager) phoneForAccount.getContext().getSystemService("carrier_config");
                if (carrierConfigManager != null) {
                    z = carrierConfigManager.getConfigForSubId(phoneForAccount.getSubId()).getBoolean("disable_cdma_activation_code_bool");
                } else {
                    z = false;
                }
                if (z) {
                    return Connection.createFailedConnection(DisconnectCauseUtil.toTelecomDisconnectCause(49, "Tried to dial *228"));
                }
            }
        }
        boolean zIsLocalEmergencyNumber2 = PhoneNumberUtils.isLocalEmergencyNumber(this, schemeSpecificPart);
        if (zIsLocalEmergencyNumber2 || ((phoneForAccount2 = getPhoneForAccount(connectionRequest.getAccountHandle(), false)) != null && phoneForAccount2.getServiceState().getState() == 0)) {
            uriFromParts = address;
            z2 = false;
        } else {
            String strConvertToEmergencyNumber = PhoneNumberUtils.convertToEmergencyNumber(this, schemeSpecificPart);
            if (!TextUtils.equals(strConvertToEmergencyNumber, schemeSpecificPart)) {
                Log.i(this, "onCreateOutgoingConnection, converted to emergency number", new Object[0]);
                uriFromParts = Uri.fromParts("tel", strConvertToEmergencyNumber, null);
                schemeSpecificPart = strConvertToEmergencyNumber;
                z2 = true;
            }
        }
        if (z2) {
            zIsLocalEmergencyNumber = PhoneNumberUtils.isLocalEmergencyNumber(this, schemeSpecificPart);
        } else {
            zIsLocalEmergencyNumber = zIsLocalEmergencyNumber2;
        }
        if (!zIsLocalEmergencyNumber && MtkTelephonyConnectionServiceUtil.getInstance().isEccRetryOn()) {
            Log.i(this, "ECC retry: clear ECC param due to SIM state/phone type change, not ECC", new Object[0]);
            MtkTelephonyConnectionServiceUtil.getInstance().clearEccRetryParams();
            MtkTelephonyConnectionServiceUtil.getInstance().setInEcc(false);
            Log.i(this, "onCreateOutgoingConnection, phone is null", new Object[0]);
            return Connection.createFailedConnection(DisconnectCauseUtil.toTelecomDisconnectCause(18, "Phone is null"));
        }
        if (zIsLocalEmergencyNumber) {
            MtkTelephonyConnectionServiceUtil.getInstance().setInEcc(true);
            boolean zIsEmergencyNumber = PhoneNumberUtils.isEmergencyNumber(schemeSpecificPart);
            MtkTelephonyConnectionServiceUtil.getInstance().setEmergencyNumber(schemeSpecificPart);
            Connection connectionSwitchPhoneIfNeeded = switchPhoneIfNeeded(connectionRequest, uriFromParts, schemeSpecificPart);
            if (connectionSwitchPhoneIfNeeded != null) {
                return connectionSwitchPhoneIfNeeded;
            }
            z3 = zIsEmergencyNumber;
        } else {
            z3 = true;
        }
        boolean z4 = (zIsLocalEmergencyNumber && (!isRadioOn() || (Settings.Global.getInt(getContentResolver(), "airplane_mode_on", 0) > 0))) || isRadioPowerDownOnBluetooth();
        if (zIsLocalEmergencyNumber) {
            Phone phoneForAccount4 = getPhoneForAccount(connectionRequest.getAccountHandle(), true, schemeSpecificPart);
            z4 |= !phoneForAccount4.isRadioOn();
            phoneId = phoneForAccount4.getPhoneId();
        }
        int i = phoneId;
        if (z4) {
            final int phoneType = PhoneFactory.getDefaultPhone().getPhoneType();
            final Connection telephonyConnection = getTelephonyConnection(connectionRequest, schemeSpecificPart, zIsLocalEmergencyNumber, uriFromParts, PhoneFactory.getDefaultPhone());
            if (!(telephonyConnection instanceof TelephonyConnection)) {
                Log.i(this, "onCreateOutgoingConnection, create emergency connection failed", new Object[0]);
                return telephonyConnection;
            }
            Phone phoneForAccount5 = getPhoneForAccount(connectionRequest.getAccountHandle(), zIsLocalEmergencyNumber, schemeSpecificPart);
            if (phoneForAccount5 instanceof MtkGsmCdmaPhone) {
                MtkGsmCdmaPhone mtkGsmCdmaPhone = (MtkGsmCdmaPhone) phoneForAccount5;
                if (mtkGsmCdmaPhone.shouldProcessSelfActivation()) {
                    notifyEccToSelfActivationSM(mtkGsmCdmaPhone);
                }
            }
            if (this.mRadioOnHelper == null) {
                this.mRadioOnHelper = new RadioOnHelper(this);
            }
            if (!z3) {
                Log.d(this, "RadioOnHelper setEccByNormalPhoneId (phoneId=" + i + ")", new Object[0]);
                this.mRadioOnHelper.setEccByNormalPhoneId(i);
            } else {
                this.mRadioOnHelper.resetEccByNormalPhoneId();
            }
            final boolean z5 = zIsLocalEmergencyNumber;
            final String str = schemeSpecificPart;
            final Uri uri = uriFromParts;
            this.mRadioOnHelper.triggerRadioOnAndListen(new RadioOnStateListener.Callback() {
                @Override
                public void onComplete(RadioOnStateListener radioOnStateListener, boolean z6) {
                    TelephonyConnectionService.this.handleOnComplete(z6, z5, telephonyConnection, connectionRequest, str, uri, phoneType);
                }

                @Override
                public boolean isOkToCall(Phone phone, int i2) {
                    return z5 ? phone.getState() == PhoneConstants.State.OFFHOOK || phone.getServiceStateTracker().isRadioOn() : phone.getState() == PhoneConstants.State.OFFHOOK || i2 == 0;
                }
            });
            return telephonyConnection;
        }
        if (!canAddCall() && !zIsLocalEmergencyNumber) {
            Log.d(this, "onCreateOutgoingConnection, cannot add call .", new Object[0]);
            return Connection.createFailedConnection(new DisconnectCause(1, getApplicationContext().getText(R.string.incall_error_cannot_add_call), getApplicationContext().getText(R.string.incall_error_cannot_add_call), "Add call restricted due to ongoing video call"));
        }
        Phone phoneForAccount6 = getPhoneForAccount(connectionRequest.getAccountHandle(), zIsLocalEmergencyNumber, schemeSpecificPart);
        Connection telephonyConnection2 = getTelephonyConnection(connectionRequest, schemeSpecificPart, zIsLocalEmergencyNumber, uriFromParts, phoneForAccount6);
        if (telephonyConnection2 instanceof TelephonyConnection) {
            if (connectionRequest.getExtras() != null && connectionRequest.getExtras().getBoolean("android.telecom.extra.USE_ASSISTED_DIALING", false)) {
                ((TelephonyConnection) telephonyConnection2).setIsUsingAssistedDialing(true);
            }
            if (zIsLocalEmergencyNumber) {
                if (!hasC2kOverImsModem() && (MtkTelephonyManagerEx.getDefault().useVzwLogic() || MtkTelephonyManagerEx.getDefault().useATTLogic())) {
                    MtkTelephonyConnectionServiceUtil.getInstance().enterEmergencyMode(phoneForAccount6, 0);
                }
                if (!MtkTelephonyConnectionServiceUtil.getInstance().isEccRetryOn() && z3) {
                    Log.i(this, "ECC retry: set param with Intial ECC.", new Object[0]);
                    MtkTelephonyConnectionServiceUtil.getInstance().setEccRetryParams(connectionRequest, phoneForAccount6.getPhoneId());
                }
            }
            placeOutgoingConnection((TelephonyConnection) telephonyConnection2, phoneForAccount6, connectionRequest, zIsLocalEmergencyNumber);
        } else if (MtkTelephonyConnectionServiceUtil.getInstance().isEccRetryOn()) {
            Log.i(this, "ECC retry: clear ECC param", new Object[0]);
            MtkTelephonyConnectionServiceUtil.getInstance().clearEccRetryParams();
            MtkTelephonyConnectionServiceUtil.getInstance().setInEcc(false);
        }
        return telephonyConnection2;
    }

    private boolean isRadioPowerDownOnBluetooth() {
        Context applicationContext = getApplicationContext();
        return applicationContext.getResources().getBoolean(R.bool.config_allowRadioPowerDownOnBluetooth) && Settings.Global.getInt(applicationContext.getContentResolver(), "cell_on", 1) == 1 && !isRadioOn();
    }

    private void handleOnComplete(boolean z, boolean z2, Connection connection, ConnectionRequest connectionRequest, String str, Uri uri, int i) {
        if (connection.getState() == 6) {
            Log.i(this, "Call disconnected before the outgoing call was placed. Skipping call placement.", new Object[0]);
            MtkTelephonyConnectionServiceUtil.getInstance().setInEcc(false);
            return;
        }
        if (z) {
            Phone phoneForAccount = getPhoneForAccount(connectionRequest.getAccountHandle(), z2, str);
            boolean zIsEmergencyNumber = PhoneNumberUtils.isEmergencyNumber(str);
            if (!MtkTelephonyConnectionServiceUtil.getInstance().isEccRetryOn() && zIsEmergencyNumber) {
                Log.d(this, "ECC Retry : set param with Intial ECC.", new Object[0]);
                MtkTelephonyConnectionServiceUtil.getInstance().setEccRetryParams(connectionRequest, phoneForAccount.getPhoneId());
            }
            if (MtkTelephonyConnectionServiceUtil.getInstance().isDataOnlyMode(phoneForAccount)) {
                Log.i(this, "enableEmergencyCalling, phoneId=" + phoneForAccount.getPhoneId() + " is in TDD data only mode.", new Object[0]);
                if (MtkTelephonyConnectionServiceUtil.getInstance().isEccRetryOn()) {
                    Log.i(this, "ECC retry: clear ECC param", new Object[0]);
                    MtkTelephonyConnectionServiceUtil.getInstance().clearEccRetryParams();
                }
                connection.setDisconnected(DisconnectCauseUtil.toTelecomDisconnectCause(44, null));
                connection.destroy();
                MtkTelephonyConnectionServiceUtil.getInstance().setInEcc(false);
                return;
            }
            if (!MtkTelephonyConnectionServiceUtil.getInstance().isEccRetryOn()) {
                Log.i(this, "ECC retry: set param with Intial ECC.", new Object[0]);
                MtkTelephonyConnectionServiceUtil.getInstance().setEccRetryParams(connectionRequest, phoneForAccount.getPhoneId());
            }
            if (TelephonyManager.getDefault().getPhoneCount() > 1) {
                MtkTelephonyConnectionServiceUtil.getInstance().enterEmergencyMode(phoneForAccount, 0);
            }
            if (phoneForAccount.getPhoneType() != i) {
                Connection telephonyConnection = getTelephonyConnection(connectionRequest, str, z2, uri, phoneForAccount);
                boolean z3 = telephonyConnection instanceof TelephonyConnection;
                if (z3) {
                    addExistingConnection(PhoneUtils.makePstnPhoneAccountHandle(phoneForAccount), telephonyConnection);
                    resetTreatAsEmergencyCall(connection);
                    connection.setDisconnected(DisconnectCauseUtil.toTelecomDisconnectCause(44, "Reconnecting outgoing Emergency Call."));
                } else {
                    if (MtkTelephonyConnectionServiceUtil.getInstance().isEccRetryOn()) {
                        Log.i(this, "ECC retry: clear ECC param", new Object[0]);
                        MtkTelephonyConnectionServiceUtil.getInstance().clearEccRetryParams();
                    }
                    connection.setDisconnected(telephonyConnection.getDisconnectCause());
                    MtkTelephonyConnectionServiceUtil.getInstance().setInEcc(false);
                }
                connection.destroy();
                if (z3) {
                    placeOutgoingConnection((TelephonyConnection) telephonyConnection, phoneForAccount, connectionRequest, z2);
                    return;
                }
                return;
            }
            placeOutgoingConnection((TelephonyConnection) connection, phoneForAccount, connectionRequest, z2);
            return;
        }
        if (MtkTelephonyConnectionServiceUtil.getInstance().isEccRetryOn()) {
            Log.i(this, "ECC retry: clear ECC param", new Object[0]);
            MtkTelephonyConnectionServiceUtil.getInstance().clearEccRetryParams();
        }
        Log.w(this, "onCreateOutgoingConnection, failed to turn on radio", new Object[0]);
        connection.setDisconnected(DisconnectCauseUtil.toTelecomDisconnectCause(17, "Failed to turn on radio."));
        MtkTelephonyConnectionServiceUtil.getInstance().setInEcc(false);
        connection.destroy();
    }

    private boolean canAddCall() {
        for (Connection connection : getAllConnections()) {
            if (connection.getExtras() != null && connection.getExtras().getBoolean("android.telecom.extra.DISABLE_ADD_CALL", false)) {
                return false;
            }
        }
        return true;
    }

    private PhoneAccountHandle makePstnPhoneAccountHandleForEcc(Phone phone) {
        PhoneAccountHandle phoneAccountHandleMakePstnPhoneAccountHandle;
        String fullIccSerialNumber = phone.getFullIccSerialNumber();
        if (SubscriptionController.getInstance().getSlotIndex(phone.getSubId()) != -1 && TextUtils.isEmpty(fullIccSerialNumber)) {
            fullIccSerialNumber = TelephonyManager.getDefault().getSimSerialNumber(phone.getSubId());
        }
        if (TextUtils.isEmpty(fullIccSerialNumber)) {
            phoneAccountHandleMakePstnPhoneAccountHandle = PhoneUtils.makePstnPhoneAccountHandle(Integer.toString(phone.getPhoneId()));
        } else {
            phoneAccountHandleMakePstnPhoneAccountHandle = PhoneUtils.makePstnPhoneAccountHandle(fullIccSerialNumber);
        }
        log("Ecc PhoneAccountHandle mId: " + phoneAccountHandleMakePstnPhoneAccountHandle.getId() + ", iccId: " + fullIccSerialNumber);
        return phoneAccountHandleMakePstnPhoneAccountHandle;
    }

    private Connection getTelephonyConnection(ConnectionRequest connectionRequest, String str, boolean z, Uri uri, Phone phone) {
        boolean z2;
        int dataNetworkType;
        if (phone == null) {
            Context applicationContext = getApplicationContext();
            if (applicationContext.getResources().getBoolean(R.bool.config_checkSimStateBeforeOutgoingCall)) {
                IccCard iccCard = this.mPhoneFactoryProxy.getDefaultPhone().getIccCard();
                IccCardConstants.State state = IccCardConstants.State.UNKNOWN;
                if (iccCard != null) {
                    state = iccCard.getState();
                }
                if (state == IccCardConstants.State.PIN_REQUIRED) {
                    String string = applicationContext.getResources().getString(R.string.config_simUnlockUiPackage);
                    String string2 = applicationContext.getResources().getString(R.string.config_simUnlockUiClass);
                    if (string != null && string2 != null) {
                        Intent component = new Intent().setComponent(new ComponentName(string, string2));
                        component.addFlags(268435456);
                        try {
                            applicationContext.startActivity(component);
                        } catch (ActivityNotFoundException e) {
                            Log.e(this, e, "Unable to find SIM unlock UI activity.", new Object[0]);
                        }
                    }
                    return Connection.createFailedConnection(DisconnectCauseUtil.toTelecomDisconnectCause(18, "SIM_STATE_PIN_REQUIRED"));
                }
            }
            Log.d(this, "onCreateOutgoingConnection, phone is null", new Object[0]);
            log("onCreateOutgoingConnection, use default phone for cellConnMgr");
            if (MtkTelephonyConnectionServiceUtil.getInstance().cellConnMgrShowAlerting(PhoneFactory.getDefaultPhone().getSubId())) {
                log("onCreateOutgoingConnection, cellConnMgrShowAlerting() check fail");
                return Connection.createFailedConnection(DisconnectCauseUtil.toTelecomDisconnectCause(1041, "cellConnMgrShowAlerting() check fail"));
            }
            return Connection.createFailedConnection(DisconnectCauseUtil.toTelecomDisconnectCause(18, "Phone is null"));
        }
        boolean z3 = Settings.Global.getInt(phone.getContext().getContentResolver(), "airplane_mode_on", 0) > 0;
        if (!z3 && MtkTelephonyConnectionServiceUtil.getInstance().isDataOnlyMode(phone)) {
            if (MtkTelephonyConnectionServiceUtil.getInstance().isEccRetryOn()) {
                Log.i(this, "4G data only, ECC retry: clear ECC param", new Object[0]);
                MtkTelephonyConnectionServiceUtil.getInstance().clearEccRetryParams();
            }
            MtkTelephonyConnectionServiceUtil.getInstance().setInEcc(false);
            Log.i(this, "getTelephonyConnection, phoneId=" + phone.getPhoneId() + " is in TDD data only mode.", new Object[0]);
            return Connection.createFailedConnection(DisconnectCauseUtil.toTelecomDisconnectCause(44, null));
        }
        int state2 = phone.getServiceState().getState();
        if (state2 == 1 && ((dataNetworkType = phone.getServiceState().getDataNetworkType()) == 13 || dataNetworkType == 19)) {
            state2 = phone.getServiceState().getDataRegState();
        }
        boolean zIsWifiCallingEnabled = phone.isWifiCallingEnabled();
        log("WFC: phoneId: " + phone.getPhoneId() + " isWfcEnabled: " + zIsWifiCallingEnabled + " isRadioOn: " + phone.isRadioOn());
        if (!phone.isRadioOn() && zIsWifiCallingEnabled) {
            state2 = 0;
        }
        Log.d(this, "Service state:" + state2 + ", isAirplaneModeOn:" + z3, new Object[0]);
        if (!z && phone.isInEcm()) {
            CarrierConfigManager carrierConfigManager = (CarrierConfigManager) phone.getContext().getSystemService("carrier_config");
            if (carrierConfigManager != null) {
                z2 = carrierConfigManager.getConfigForSubId(phone.getSubId()).getBoolean("allow_non_emergency_calls_in_ecm_bool");
            } else {
                z2 = true;
            }
            if (!z2) {
                return Connection.createFailedConnection(DisconnectCauseUtil.toTelecomDisconnectCause(34, "Cannot make non-emergency call in ECM mode."));
            }
        }
        if (!z) {
            if (MtkTelephonyConnectionServiceUtil.getInstance().shouldOpenDataConnection(str, phone)) {
                log("onCreateOutgoingConnection, shouldOpenDataConnection() check fail");
                return Connection.createFailedConnection(DisconnectCauseUtil.toTelecomDisconnectCause(1044, "disconnect.reason.volte.ss.data.off"));
            }
            if (z3 && (phone instanceof MtkGsmCdmaPhone) && ((MtkGsmCdmaPhone) phone).shouldProcessSelfActivation()) {
                Log.d(this, "[Self-activation] Bypass Dial in flightmode.", new Object[0]);
            } else if (MtkTelephonyConnectionServiceUtil.getInstance().cellConnMgrShowAlerting(phone.getSubId())) {
                log("onCreateOutgoingConnection, cellConnMgrShowAlerting() check fail");
                return Connection.createFailedConnection(DisconnectCauseUtil.toTelecomDisconnectCause(1041, "cellConnMgrShowAlerting() check fail"));
            }
            switch (state2) {
                case 0:
                case 2:
                    if (!canDial(connectionRequest.getAccountHandle(), str)) {
                        log("onCreateOutgoingConnection, canDial() check fail");
                        return Connection.createFailedConnection(DisconnectCauseUtil.toTelecomDisconnectCause(43, "canDial() check fail"));
                    }
                    break;
                case 1:
                    if (phone.isUtEnabled() && str.endsWith("#")) {
                        Log.d(this, "onCreateOutgoingConnection dial for UT", new Object[0]);
                    } else if (SystemProperties.getInt("vendor.gsm.gcf.testmode", 0) != 2) {
                        if ((phone instanceof MtkGsmCdmaPhone) && ((MtkGsmCdmaPhone) phone).isCdmaLessDevice()) {
                            Log.d(this, "onCreateOutgoingConnection dial even OOS", new Object[0]);
                        } else {
                            return Connection.createFailedConnection(DisconnectCauseUtil.toTelecomDisconnectCause(18, "ServiceState.STATE_OUT_OF_SERVICE"));
                        }
                    }
                    if (!canDial(connectionRequest.getAccountHandle(), str)) {
                    }
                    break;
                case 3:
                    if (!isRadioPowerDownOnBluetooth()) {
                        if ((phone instanceof MtkGsmCdmaPhone) && ((MtkGsmCdmaPhone) phone).shouldProcessSelfActivation()) {
                            Log.d(this, "POWER_OF and need to do self activation", new Object[0]);
                        } else {
                            return Connection.createFailedConnection(DisconnectCauseUtil.toTelecomDisconnectCause(17, "ServiceState.STATE_POWER_OFF"));
                        }
                    }
                    if (!canDial(connectionRequest.getAccountHandle(), str)) {
                    }
                    break;
                default:
                    Log.d(this, "onCreateOutgoingConnection, unknown service state: %d", Integer.valueOf(state2));
                    return Connection.createFailedConnection(DisconnectCauseUtil.toTelecomDisconnectCause(43, "Unknown service state " + state2));
            }
        }
        Context applicationContext2 = getApplicationContext();
        if (VideoProfile.isVideo(connectionRequest.getVideoState()) && isTtyModeEnabled(applicationContext2) && !z) {
            return Connection.createFailedConnection(DisconnectCauseUtil.toTelecomDisconnectCause(50));
        }
        Connection connectionCheckAdditionalOutgoingCallLimits = checkAdditionalOutgoingCallLimits(phone);
        if (connectionCheckAdditionalOutgoingCallLimits != null) {
            return connectionCheckAdditionalOutgoingCallLimits;
        }
        if (blockCallForwardingNumberWhileRoaming(phone, str)) {
            return Connection.createFailedConnection(DisconnectCauseUtil.toTelecomDisconnectCause(57, "Call forwarding while roaming"));
        }
        TelephonyConnection telephonyConnectionCreateConnectionFor = createConnectionFor(phone, null, true, connectionRequest.getAccountHandle(), connectionRequest.getTelecomCallId(), connectionRequest.getAddress(), connectionRequest.getVideoState());
        if (telephonyConnectionCreateConnectionFor == null) {
            if (MtkTelephonyConnectionServiceUtil.getInstance().isEccRetryOn()) {
                Log.i(this, "Fail to create connection, ECC retry: clear ECC param", new Object[0]);
                MtkTelephonyConnectionServiceUtil.getInstance().clearEccRetryParams();
            }
            MtkTelephonyConnectionServiceUtil.getInstance().setInEcc(false);
            return Connection.createFailedConnection(DisconnectCauseUtil.toTelecomDisconnectCause(43, "Invalid phone type"));
        }
        if (telephonyConnectionCreateConnectionFor instanceof TelephonyConnection) {
            telephonyConnectionCreateConnectionFor.setEmergencyCall(z);
        }
        if (z) {
            telephonyConnectionCreateConnectionFor.setAccountHandle(makePstnPhoneAccountHandleForEcc(phone));
        }
        telephonyConnectionCreateConnectionFor.setAddress(uri, 1);
        telephonyConnectionCreateConnectionFor.setInitializing();
        telephonyConnectionCreateConnectionFor.setVideoState(connectionRequest.getVideoState());
        telephonyConnectionCreateConnectionFor.setRttTextStream(connectionRequest.getRttTextStream());
        return telephonyConnectionCreateConnectionFor;
    }

    @Override
    public Connection onCreateIncomingConnection(PhoneAccountHandle phoneAccountHandle, ConnectionRequest connectionRequest) {
        boolean z;
        Log.i(this, "onCreateIncomingConnection, request: " + connectionRequest, new Object[0]);
        PhoneAccountHandle accountHandle = connectionRequest.getAccountHandle();
        if (accountHandle == null || !PhoneUtils.EMERGENCY_ACCOUNT_HANDLE_ID.equals(accountHandle.getId())) {
            z = false;
        } else {
            Log.i(this, "Emergency PhoneAccountHandle is being used for incoming call... Treat as an Emergency Call.", new Object[0]);
            z = true;
        }
        Phone phoneForAccount = getPhoneForAccount(accountHandle, z);
        if (phoneForAccount == null) {
            return Connection.createFailedConnection(DisconnectCauseUtil.toTelecomDisconnectCause(36, "Phone is null"));
        }
        Call ringingCall = phoneForAccount.getRingingCall();
        if (!ringingCall.getState().isRinging()) {
            Log.i(this, "onCreateIncomingConnection, no ringing call", new Object[0]);
            return Connection.createFailedConnection(DisconnectCauseUtil.toTelecomDisconnectCause(1, "Found no ringing call"));
        }
        com.android.internal.telephony.Connection latestConnection = ringingCall.getState() == Call.State.WAITING ? ringingCall.getLatestConnection() : ringingCall.getEarliestConnection();
        if (isOriginalConnectionKnown(latestConnection)) {
            Log.i(this, "onCreateIncomingConnection, original connection already registered", new Object[0]);
            return Connection.createCanceledConnection();
        }
        TelephonyConnection telephonyConnectionCreateConnectionFor = createConnectionFor(phoneForAccount, latestConnection, false, connectionRequest.getAccountHandle(), connectionRequest.getTelecomCallId(), connectionRequest.getAddress(), latestConnection != null ? latestConnection.getVideoState() : 0);
        handleIncomingRtt(connectionRequest, latestConnection);
        if (telephonyConnectionCreateConnectionFor == null) {
            return Connection.createCanceledConnection();
        }
        return telephonyConnectionCreateConnectionFor;
    }

    private void handleIncomingRtt(ConnectionRequest connectionRequest, com.android.internal.telephony.Connection connection) {
        if (connection == null || connection.getPhoneType() != 5) {
            if (connectionRequest.isRequestingRtt()) {
                Log.w(this, "Requesting RTT on non-IMS call, ignoring", new Object[0]);
                return;
            }
            return;
        }
        ImsPhoneConnection imsPhoneConnection = (ImsPhoneConnection) connection;
        if (!connectionRequest.isRequestingRtt()) {
            if (imsPhoneConnection.isRttEnabledForCall()) {
                Log.w(this, "Incoming call requested RTT but we did not get a RttTextStream", new Object[0]);
                return;
            }
            return;
        }
        Log.i(this, "Setting RTT stream on ImsPhoneConnection in case we need it later", new Object[0]);
        imsPhoneConnection.setCurrentRttTextStream(connectionRequest.getRttTextStream());
        if (!imsPhoneConnection.isRttEnabledForCall()) {
            if (connectionRequest.isRequestingRtt()) {
                Log.w(this, "Incoming call processed as RTT but did not come in as one. Ignoring", new Object[0]);
            }
        } else {
            Log.i(this, "Setting the call to be answered with RTT on.", new Object[0]);
            imsPhoneConnection.getImsCall().setAnswerWithRtt();
        }
    }

    public void onCreateConnectionComplete(Connection connection) {
        if (connection instanceof TelephonyConnection) {
            maybeSendInternationalCallEvent((TelephonyConnection) connection);
        }
    }

    public void triggerConferenceRecalculate() {
        if (this.mTelephonyConferenceController.shouldRecalculate()) {
            this.mTelephonyConferenceController.recalculate();
        }
    }

    public Connection onCreateUnknownConnection(PhoneAccountHandle phoneAccountHandle, ConnectionRequest connectionRequest) {
        boolean z;
        com.android.internal.telephony.Connection connection;
        com.android.internal.telephony.Connection connectionById;
        Log.i(this, "onCreateUnknownConnection, request: " + connectionRequest, new Object[0]);
        PhoneAccountHandle accountHandle = connectionRequest.getAccountHandle();
        if (accountHandle == null || !PhoneUtils.EMERGENCY_ACCOUNT_HANDLE_ID.equals(accountHandle.getId())) {
            z = false;
        } else {
            Log.i(this, "Emergency PhoneAccountHandle is being used for unknown call... Treat as an Emergency Call.", new Object[0]);
            z = true;
        }
        Phone phoneForAccount = getPhoneForAccount(accountHandle, z);
        if (phoneForAccount == null) {
            return Connection.createFailedConnection(DisconnectCauseUtil.toTelecomDisconnectCause(36, "Phone is null"));
        }
        Bundle extras = connectionRequest.getExtras();
        ArrayList arrayList = new ArrayList();
        if (phoneForAccount.getImsPhone() != null && extras != null && extras.containsKey("android.telephony.ImsExternalCallTracker.extra.EXTERNAL_CALL_ID")) {
            ImsExternalCallTracker externalCallTracker = phoneForAccount.getImsPhone().getExternalCallTracker();
            int i = extras.getInt("android.telephony.ImsExternalCallTracker.extra.EXTERNAL_CALL_ID", -1);
            if (externalCallTracker != null && (connectionById = externalCallTracker.getConnectionById(i)) != null) {
                arrayList.add(connectionById);
            }
        }
        if (arrayList.isEmpty()) {
            Call ringingCall = phoneForAccount.getRingingCall();
            if (ringingCall.hasConnections()) {
                arrayList.addAll(ringingCall.getConnections());
            }
            Call foregroundCall = phoneForAccount.getForegroundCall();
            if (foregroundCall.getState() != Call.State.DISCONNECTED && foregroundCall.hasConnections()) {
                arrayList.addAll(foregroundCall.getConnections());
            }
            if (phoneForAccount.getImsPhone() != null) {
                Call foregroundCall2 = phoneForAccount.getImsPhone().getForegroundCall();
                if (foregroundCall2.getState() != Call.State.DISCONNECTED && foregroundCall2.hasConnections()) {
                    arrayList.addAll(foregroundCall2.getConnections());
                }
            }
            if (phoneForAccount.getBackgroundCall().hasConnections()) {
                arrayList.addAll(phoneForAccount.getBackgroundCall().getConnections());
            }
        }
        Iterator it = arrayList.iterator();
        while (true) {
            if (it.hasNext()) {
                com.android.internal.telephony.Connection connection2 = (com.android.internal.telephony.Connection) it.next();
                if (!isOriginalConnectionKnown(connection2)) {
                    Log.d(this, "onCreateUnknownConnection: conn = " + connection2, new Object[0]);
                    connection = connection2;
                    break;
                }
            } else {
                connection = null;
                break;
            }
        }
        if (connection == null) {
            Log.i(this, "onCreateUnknownConnection, did not find previously unknown connection.", new Object[0]);
            return Connection.createCanceledConnection();
        }
        TelephonyConnection telephonyConnectionCreateConnectionFor = createConnectionFor(phoneForAccount, connection, !connection.isIncoming(), connectionRequest.getAccountHandle(), connectionRequest.getTelecomCallId(), connectionRequest.getAddress(), connection != null ? connection.getVideoState() : 0);
        if (telephonyConnectionCreateConnectionFor == null) {
            return Connection.createCanceledConnection();
        }
        telephonyConnectionCreateConnectionFor.updateState();
        return telephonyConnectionCreateConnectionFor;
    }

    @Override
    public void onConference(Connection connection, Connection connection2) {
        if (connection instanceof TelephonyConnection) {
            ((TelephonyConnection) connection).performConference(connection2);
        } else if (connection2 instanceof TelephonyConnection) {
            ((TelephonyConnection) connection2).performConference(connection);
        } else {
            Log.w(this, "onConference - cannot merge connections Connection1: %s, Connection2: %2", connection, connection2);
        }
    }

    public void onConnectionAdded(Connection connection) {
        if ((connection instanceof Holdable) && !isExternalConnection(connection)) {
            connection.addConnectionListener(this.mConnectionListener);
            this.mHoldTracker.addHoldable(connection.getPhoneAccountHandle(), (Holdable) connection);
        }
    }

    public void onConnectionRemoved(Connection connection) {
        if ((connection instanceof Holdable) && !isExternalConnection(connection)) {
            this.mHoldTracker.removeHoldable(connection.getPhoneAccountHandle(), (Holdable) connection);
        }
    }

    public void onConferenceAdded(Conference conference) {
        if (conference instanceof Holdable) {
            this.mHoldTracker.addHoldable(conference.getPhoneAccountHandle(), (Holdable) conference);
        }
    }

    public void onConferenceRemoved(Conference conference) {
        if (conference instanceof Holdable) {
            this.mHoldTracker.removeHoldable(conference.getPhoneAccountHandle(), (Holdable) conference);
        }
    }

    private boolean isExternalConnection(Connection connection) {
        return (connection.getConnectionProperties() & 16) == 16;
    }

    private boolean blockCallForwardingNumberWhileRoaming(Phone phone, String str) {
        if (phone == null || TextUtils.isEmpty(str) || !phone.getServiceState().getRoaming()) {
            return false;
        }
        String[] stringArray = null;
        CarrierConfigManager carrierConfigManager = (CarrierConfigManager) phone.getContext().getSystemService("carrier_config");
        if (carrierConfigManager != null) {
            stringArray = carrierConfigManager.getConfigForSubId(phone.getSubId()).getStringArray("call_forwarding_blocks_while_roaming_string_array");
        }
        if (stringArray != null) {
            for (String str2 : stringArray) {
                if (str.startsWith(str2)) {
                    return true;
                }
            }
        }
        return false;
    }

    private boolean isRadioOn() {
        boolean zIsRadioOn = false;
        for (Phone phone : this.mPhoneFactoryProxy.getPhones()) {
            zIsRadioOn |= phone.isRadioOn();
        }
        return zIsRadioOn;
    }

    private Pair<WeakReference<TelephonyConnection>, Queue<Phone>> makeCachedConnectionPhonePair(TelephonyConnection telephonyConnection) {
        return new Pair<>(new WeakReference(telephonyConnection), new LinkedList(Arrays.asList(this.mPhoneFactoryProxy.getPhones())));
    }

    private void updateCachedConnectionPhonePair(TelephonyConnection telephonyConnection, boolean z) {
        if (this.mEmergencyRetryCache == null) {
            Log.i(this, "updateCachedConnectionPhonePair, cache is null. Generating new cache", new Object[0]);
            this.mEmergencyRetryCache = makeCachedConnectionPhonePair(telephonyConnection);
        } else if (((WeakReference) this.mEmergencyRetryCache.first).get() != telephonyConnection) {
            Log.i(this, "updateCachedConnectionPhonePair, cache is stale. Regenerating.", new Object[0]);
            this.mEmergencyRetryCache = makeCachedConnectionPhonePair(telephonyConnection);
        }
        Queue queue = (Queue) this.mEmergencyRetryCache.second;
        Phone phone = telephonyConnection.getPhone();
        if (phone == null) {
            return;
        }
        queue.remove(phone);
        Log.i(this, "updateCachedConnectionPhonePair, isPermanentFailure:" + z, new Object[0]);
        if (!z) {
            queue.offer(phone);
        }
    }

    @VisibleForTesting
    public void retryOutgoingOriginalConnection(TelephonyConnection telephonyConnection, boolean z) {
        int phoneId = telephonyConnection.getPhone() == null ? -1 : telephonyConnection.getPhone().getPhoneId();
        updateCachedConnectionPhonePair(telephonyConnection, z);
        Phone phone = this.mEmergencyRetryCache.second != null ? (Phone) ((Queue) this.mEmergencyRetryCache.second).peek() : null;
        if (phone != null) {
            int videoState = telephonyConnection.getVideoState();
            Bundle extras = telephonyConnection.getExtras();
            Log.i(this, "retryOutgoingOriginalConnection, redialing on Phone Id: " + phone, new Object[0]);
            telephonyConnection.clearOriginalConnection();
            if (phoneId != phone.getPhoneId()) {
                updatePhoneAccount(telephonyConnection, phone);
            }
            placeOutgoingConnection(telephonyConnection, phone, videoState, extras);
            return;
        }
        Log.i(this, "retryOutgoingOriginalConnection, no more Phones to use. Disconnecting.", new Object[0]);
        telephonyConnection.setDisconnected(new DisconnectCause(1));
        telephonyConnection.clearOriginalConnection();
        telephonyConnection.destroy();
    }

    private void updatePhoneAccount(TelephonyConnection telephonyConnection, Phone phone) {
        PhoneAccountHandle phoneAccountHandleMakePstnPhoneAccountHandle = PhoneUtils.makePstnPhoneAccountHandle(phone);
        Log.i(this, "updatePhoneAccount setPhoneAccountHandle, account = " + phoneAccountHandleMakePstnPhoneAccountHandle, new Object[0]);
        telephonyConnection.setPhoneAccountHandle(phoneAccountHandleMakePstnPhoneAccountHandle);
    }

    private void placeOutgoingConnection(TelephonyConnection telephonyConnection, Phone phone, ConnectionRequest connectionRequest) {
        placeOutgoingConnection(telephonyConnection, phone, connectionRequest, PhoneNumberUtils.isLocalEmergencyNumber(this, telephonyConnection.getAddress().getSchemeSpecificPart()));
    }

    private void placeOutgoingConnection(TelephonyConnection telephonyConnection, Phone phone, ConnectionRequest connectionRequest, boolean z) {
        placeOutgoingConnection(telephonyConnection, phone, connectionRequest.getVideoState(), connectionRequest.getExtras(), z);
    }

    private void placeOutgoingConnection(TelephonyConnection telephonyConnection, Phone phone, int i, Bundle bundle) {
        placeOutgoingConnection(telephonyConnection, phone, i, bundle, PhoneNumberUtils.isLocalEmergencyNumber(this, telephonyConnection.getAddress().getSchemeSpecificPart()));
    }

    protected void placeOutgoingConnection(TelephonyConnection telephonyConnection, Phone phone, int i, Bundle bundle, boolean z) {
        String schemeSpecificPart = telephonyConnection.getAddress().getSchemeSpecificPart();
        if (z) {
            PhoneAccountHandle phoneAccountHandleMakePstnPhoneAccountHandleForEcc = makePstnPhoneAccountHandleForEcc(phone);
            log("placeOutgoingConnection, set back account mId: " + phoneAccountHandleMakePstnPhoneAccountHandleForEcc.getId());
            telephonyConnection.setPhoneAccountHandle(phoneAccountHandleMakePstnPhoneAccountHandleForEcc);
            MtkTelephonyConnectionServiceUtil.getInstance().setEccPhoneType(phone.getPhoneType());
        }
        com.android.internal.telephony.Connection connectionDial = null;
        int i2 = 43;
        if (phone != null) {
            try {
                connectionDial = phone.dial(schemeSpecificPart, new ImsPhone.ImsDialArgs.Builder().setVideoState(i).setIntentExtras(ExtensionManager.getDigitsUtilExt().putLineNumberToExtras(bundle, getApplicationContext())).setRttTextStream(telephonyConnection.getRttTextStream()).build());
            } catch (CallStateException e) {
                Log.e((Object) this, (Throwable) e, "placeOutgoingConnection, phone.dial exception: " + e, new Object[0]);
                if (e.getError() == 1) {
                    i2 = 18;
                } else if (e.getError() == 2) {
                    i2 = 17;
                }
                if (MtkTelephonyConnectionServiceUtil.getInstance().isEccRetryOn()) {
                    Log.i(this, "ECC retry: clear ECC param", new Object[0]);
                    MtkTelephonyConnectionServiceUtil.getInstance().clearEccRetryParams();
                }
                telephonyConnection.setDisconnected(DisconnectCauseUtil.toTelecomDisconnectCause(i2, e.getMessage()));
                MtkTelephonyConnectionServiceUtil.getInstance().setInEcc(false);
                telephonyConnection.clearOriginalConnection();
                telephonyConnection.destroy();
                return;
            }
        }
        if (connectionDial == null) {
            if (phone.getPhoneType() == 1) {
                Log.d(this, "dialed MMI code", new Object[0]);
                int subId = phone.getSubId();
                Log.d(this, "subId: " + subId, new Object[0]);
                i2 = 39;
                Intent intent = new Intent(this, (Class<?>) MMIDialogActivity.class);
                intent.setFlags(276824064);
                if (SubscriptionManager.isValidSubscriptionId(subId)) {
                    intent.putExtra("subscription", subId);
                }
                startActivity(intent);
            }
            Log.d(this, "placeOutgoingConnection, phone.dial returned null", new Object[0]);
            if (MtkTelephonyConnectionServiceUtil.getInstance().isEccRetryOn()) {
                Log.i(this, "ECC retry: clear ECC param", new Object[0]);
                MtkTelephonyConnectionServiceUtil.getInstance().clearEccRetryParams();
            }
            telephonyConnection.setDisconnected(DisconnectCauseUtil.toTelecomDisconnectCause(i2, "Connection is null"));
            MtkTelephonyConnectionServiceUtil.getInstance().setInEcc(false);
            telephonyConnection.clearOriginalConnection();
            telephonyConnection.destroy();
            return;
        }
        telephonyConnection.setOriginalConnection(connectionDial);
    }

    public void createConnectionInternal(String str, ConnectionRequest connectionRequest) {
        PhoneAccountHandle accountHandle;
        Log.i(this, "createConnectionInternal, callId=" + str + ", request=" + connectionRequest, new Object[0]);
        IVideoProvider iVideoProvider = null;
        Connection connectionOnCreateOutgoingConnection = onCreateOutgoingConnection(null, connectionRequest);
        Log.i(this, "createConnectionInternal, connection=", connectionOnCreateOutgoingConnection);
        if (connectionOnCreateOutgoingConnection == null) {
            connectionOnCreateOutgoingConnection = Connection.createFailedConnection(new DisconnectCause(1));
        }
        if (connectionOnCreateOutgoingConnection instanceof TelephonyConnection) {
            accountHandle = ((TelephonyConnection) connectionOnCreateOutgoingConnection).getAccountHandle();
        } else {
            accountHandle = null;
        }
        if (accountHandle == null) {
            accountHandle = connectionRequest.getAccountHandle();
        } else {
            Log.i(this, "createConnectionInternal, set back phone account=" + accountHandle, new Object[0]);
        }
        PhoneAccountHandle phoneAccountHandle = accountHandle;
        connectionOnCreateOutgoingConnection.setTelecomCallId(str);
        if (connectionOnCreateOutgoingConnection.getState() != 6) {
            addConnection(phoneAccountHandle, str, connectionOnCreateOutgoingConnection);
        }
        Uri address = connectionOnCreateOutgoingConnection.getAddress();
        Log.i(this, "createConnectionInternal, number=" + Connection.toLogSafePhoneNumber(address == null ? "null" : address.getSchemeSpecificPart()) + ", state=" + Connection.stateToString(connectionOnCreateOutgoingConnection.getState()) + ", capabilities=" + MtkConnection.capabilitiesToString(connectionOnCreateOutgoingConnection.getConnectionCapabilities()) + ", properties=" + MtkConnection.propertiesToString(connectionOnCreateOutgoingConnection.getConnectionProperties()), new Object[0]);
        StringBuilder sb = new StringBuilder();
        sb.append("createConnectionInternal, calling handleCreateConnectionComplete for callId=");
        sb.append(str);
        Log.i(this, sb.toString(), new Object[0]);
        ConnectionServiceAdapter connectionServiceAdapter = this.mAdapter;
        int state = connectionOnCreateOutgoingConnection.getState();
        int connectionCapabilities = connectionOnCreateOutgoingConnection.getConnectionCapabilities();
        int connectionProperties = connectionOnCreateOutgoingConnection.getConnectionProperties();
        int supportedAudioRoutes = connectionOnCreateOutgoingConnection.getSupportedAudioRoutes();
        Uri address2 = connectionOnCreateOutgoingConnection.getAddress();
        int addressPresentation = connectionOnCreateOutgoingConnection.getAddressPresentation();
        String callerDisplayName = connectionOnCreateOutgoingConnection.getCallerDisplayName();
        int callerDisplayNamePresentation = connectionOnCreateOutgoingConnection.getCallerDisplayNamePresentation();
        if (connectionOnCreateOutgoingConnection.getVideoProvider() != null) {
            iVideoProvider = connectionOnCreateOutgoingConnection.getVideoProvider().getInterface();
        }
        connectionServiceAdapter.handleCreateConnectionComplete(str, connectionRequest, new ParcelableConnection(phoneAccountHandle, state, connectionCapabilities, connectionProperties, supportedAudioRoutes, address2, addressPresentation, callerDisplayName, callerDisplayNamePresentation, iVideoProvider, connectionOnCreateOutgoingConnection.getVideoState(), connectionOnCreateOutgoingConnection.isRingbackRequested(), connectionOnCreateOutgoingConnection.getAudioModeIsVoip(), connectionOnCreateOutgoingConnection.getConnectTimeMillis(), connectionOnCreateOutgoingConnection.getConnectElapsedTimeMillis(), connectionOnCreateOutgoingConnection.getStatusHints(), connectionOnCreateOutgoingConnection.getDisconnectCause(), createIdList(connectionOnCreateOutgoingConnection.getConferenceables()), connectionOnCreateOutgoingConnection.getExtras()));
    }

    protected String removeConnectionInternal(Connection connection) {
        String str = (String) this.mIdByConnection.get(connection);
        connection.unsetConnectionService(this);
        connection.removeConnectionListener(((ConnectionService) this).mConnectionListener);
        this.mConnectionById.remove(this.mIdByConnection.get(connection));
        this.mIdByConnection.remove(connection);
        onConnectionRemoved(connection);
        Log.i(this, "removeConnectionInternal, callId=" + str + ", connection=" + connection, new Object[0]);
        return str;
    }

    private TelephonyConnection createConnectionFor(Phone phone, com.android.internal.telephony.Connection connection, boolean z, PhoneAccountHandle phoneAccountHandle, String str, Uri uri, int i) {
        MtkGsmCdmaConnection mtkGsmCdmaConnection = new MtkGsmCdmaConnection(phone.getPhoneType(), connection, str, this.mEmergencyTonePlayer, allowsMute(phone), z);
        mtkGsmCdmaConnection.addTelephonyConnectionListener(this.mTelephonyConnectionListener);
        mtkGsmCdmaConnection.setVideoPauseSupported(TelecomAccountRegistry.getInstance(this).isVideoPauseSupported(phoneAccountHandle));
        mtkGsmCdmaConnection.setManageImsConferenceCallSupported(TelecomAccountRegistry.getInstance(this).isManageImsConferenceCallSupported(phoneAccountHandle));
        mtkGsmCdmaConnection.setShowPreciseFailedCause(TelecomAccountRegistry.getInstance(this).isShowPreciseFailedCause(phoneAccountHandle));
        return mtkGsmCdmaConnection;
    }

    private boolean isOriginalConnectionKnown(com.android.internal.telephony.Connection connection) {
        for (Connection connection2 : getAllConnections()) {
            if ((connection2 instanceof TelephonyConnection) && ((TelephonyConnection) connection2).getOriginalConnection() == connection) {
                return true;
            }
        }
        return false;
    }

    private Phone getPhoneForAccount(PhoneAccountHandle phoneAccountHandle, boolean z) {
        Phone firstPhoneForEmergencyCall;
        int subIdForPhoneAccountHandle = PhoneUtils.getSubIdForPhoneAccountHandle(phoneAccountHandle);
        if (subIdForPhoneAccountHandle != -1) {
            firstPhoneForEmergencyCall = this.mPhoneFactoryProxy.getPhone(this.mSubscriptionManagerProxy.getPhoneId(subIdForPhoneAccountHandle));
        } else {
            firstPhoneForEmergencyCall = null;
        }
        if (z && (firstPhoneForEmergencyCall == null || firstPhoneForEmergencyCall.getServiceState().getState() != 0)) {
            Log.d(this, "getPhoneForAccount: phone for phone acct handle %s is out of service or invalid for emergency call.", phoneAccountHandle);
            firstPhoneForEmergencyCall = getFirstPhoneForEmergencyCall();
            StringBuilder sb = new StringBuilder();
            sb.append("getPhoneForAccount: using subId: ");
            sb.append(firstPhoneForEmergencyCall == null ? "null" : Integer.valueOf(firstPhoneForEmergencyCall.getSubId()));
            Log.d(this, sb.toString(), new Object[0]);
        }
        return firstPhoneForEmergencyCall;
    }

    private Phone getPhoneForAccount(PhoneAccountHandle phoneAccountHandle, boolean z, String str) {
        Phone phoneForAccount = getPhoneForAccount(phoneAccountHandle, z);
        if (z) {
            return MtkTelephonyConnectionServiceUtil.getInstance().selectPhoneBySpecialEccRule(phoneAccountHandle, str, phoneForAccount);
        }
        return phoneForAccount;
    }

    @VisibleForTesting
    public Phone getFirstPhoneForEmergencyCall() {
        Phone phone;
        int defaultVoicePhoneId = this.mSubscriptionManagerProxy.getDefaultVoicePhoneId();
        if (defaultVoicePhoneId != -1 && (phone = this.mPhoneFactoryProxy.getPhone(defaultVoicePhoneId)) != null && isAvailableForEmergencyCalls(phone)) {
            return phone;
        }
        int phoneCount = this.mTelephonyManagerProxy.getPhoneCount();
        ArrayList arrayList = new ArrayList(phoneCount);
        final Phone phone2 = null;
        for (int i = 0; i < phoneCount; i++) {
            Phone phone3 = this.mPhoneFactoryProxy.getPhone(i);
            if (phone3 != null) {
                if (isAvailableForEmergencyCalls(phone3)) {
                    Log.i(this, "getFirstPhoneForEmergencyCall, radio on & in service, Phone Id:" + i, new Object[0]);
                    return phone3;
                }
                int radioAccessFamily = phone3.getRadioAccessFamily();
                SlotStatus slotStatus = new SlotStatus(i, radioAccessFamily);
                arrayList.add(slotStatus);
                Log.i(this, "getFirstPhoneForEmergencyCall, RAF:" + Integer.toHexString(radioAccessFamily) + " saved for Phone Id:" + i, new Object[0]);
                int simStateForSlotIdx = this.mSubscriptionManagerProxy.getSimStateForSlotIdx(i);
                if (simStateForSlotIdx == 2 || simStateForSlotIdx == 3) {
                    slotStatus.isLocked = true;
                }
                if (phone2 == null && this.mTelephonyManagerProxy.hasIccCard(i)) {
                    Log.i(this, "getFirstPhoneForEmergencyCall, SIM card inserted, Phone Id:" + phone3.getPhoneId(), new Object[0]);
                    phone2 = phone3;
                }
            }
        }
        if (phone2 == null && arrayList.isEmpty()) {
            Log.i(this, "getFirstPhoneForEmergencyCall, return default phone", new Object[0]);
            return this.mPhoneFactoryProxy.getDefaultPhone();
        }
        final int phoneId = this.mPhoneFactoryProxy.getDefaultPhone().getPhoneId();
        if (!arrayList.isEmpty()) {
            if (arrayList.size() > 1) {
                Collections.sort(arrayList, new Comparator() {
                    @Override
                    public final int compare(Object obj, Object obj2) {
                        return TelephonyConnectionService.lambda$getFirstPhoneForEmergencyCall$0(phone2, phoneId, (TelephonyConnectionService.SlotStatus) obj, (TelephonyConnectionService.SlotStatus) obj2);
                    }
                });
            }
            int i2 = ((SlotStatus) arrayList.get(arrayList.size() - 1)).slotId;
            Log.i(this, "getFirstPhoneForEmergencyCall, Using Phone Id: " + i2 + "with highest capability", new Object[0]);
            return this.mPhoneFactoryProxy.getPhone(i2);
        }
        return phone2;
    }

    static int lambda$getFirstPhoneForEmergencyCall$0(Phone phone, int i, SlotStatus slotStatus, SlotStatus slotStatus2) {
        if (slotStatus.isLocked && !slotStatus2.isLocked) {
            return -1;
        }
        if (slotStatus2.isLocked && !slotStatus.isLocked) {
            return 1;
        }
        int iBitCount = Integer.bitCount(slotStatus.capabilities) - Integer.bitCount(slotStatus2.capabilities);
        if (iBitCount == 0 && (iBitCount = RadioAccessFamily.getHighestRafCapability(slotStatus.capabilities) - RadioAccessFamily.getHighestRafCapability(slotStatus2.capabilities)) == 0) {
            if (phone != null) {
                if (slotStatus.slotId == phone.getPhoneId()) {
                    return 1;
                }
                if (slotStatus2.slotId == phone.getPhoneId()) {
                    return -1;
                }
            } else {
                if (slotStatus.slotId == i) {
                    return 1;
                }
                if (slotStatus2.slotId == i) {
                    return -1;
                }
            }
        }
        return iBitCount;
    }

    private boolean isAvailableForEmergencyCalls(Phone phone) {
        return phone.getServiceState().getState() == 0 || phone.getServiceState().isEmergencyOnly();
    }

    private boolean allowsMute(Phone phone) {
        if (phone.getPhoneType() == 2 && phone.isInEcm()) {
            return false;
        }
        return true;
    }

    public void removeConnection(Connection connection) {
        boolean z;
        if (MtkTelephonyConnectionServiceUtil.getInstance().isEccRetryOn() && (connection instanceof TelephonyConnection) && ((TelephonyConnection) connection).shouldTreatAsEmergencyCall()) {
            z = true;
        } else {
            z = false;
        }
        if (z) {
            Log.i(this, "ECC retry: remove connection.", new Object[0]);
            MtkTelephonyConnectionServiceUtil.getInstance().setEccRetryCallId(removeConnectionInternal(connection));
        } else {
            super.removeConnection(connection);
        }
        if (connection instanceof TelephonyConnection) {
            ((TelephonyConnection) connection).removeTelephonyConnectionListener(this.mTelephonyConnectionListener);
        }
    }

    public void addConnectionToConferenceController(TelephonyConnection telephonyConnection) {
        int phoneType;
        if (telephonyConnection instanceof MtkGsmCdmaConnection) {
            phoneType = ((MtkGsmCdmaConnection) telephonyConnection).getPhoneType();
        } else {
            phoneType = 0;
        }
        if (telephonyConnection.isImsConnection()) {
            Log.d(this, "Adding IMS connection to conference controller: " + telephonyConnection, new Object[0]);
            this.mImsConferenceController.add(telephonyConnection);
            this.mTelephonyConferenceController.remove(telephonyConnection);
            if (phoneType == 2) {
                this.mCdmaConferenceController.remove((MtkGsmCdmaConnection) telephonyConnection);
                return;
            }
            return;
        }
        if (telephonyConnection.getCall() == null || telephonyConnection.getCall().getPhone() == null) {
            Log.d(this, "Connection died, no need to add to conference controller", new Object[0]);
            return;
        }
        int phoneType2 = telephonyConnection.getCall().getPhone().getPhoneType();
        if (phoneType2 == 1) {
            Log.d(this, "Adding GSM connection to conference controller: " + telephonyConnection, new Object[0]);
            this.mTelephonyConferenceController.add(telephonyConnection);
            if (phoneType == 2) {
                this.mCdmaConferenceController.remove((MtkGsmCdmaConnection) telephonyConnection);
            }
        } else if (phoneType2 == 2 && phoneType == 2) {
            Log.d(this, "Adding CDMA connection to conference controller: " + telephonyConnection, new Object[0]);
            this.mCdmaConferenceController.add((MtkGsmCdmaConnection) telephonyConnection);
            this.mTelephonyConferenceController.remove(telephonyConnection);
        }
        Log.d(this, "Removing connection from IMS conference controller: " + telephonyConnection, new Object[0]);
        this.mImsConferenceController.remove(telephonyConnection);
    }

    private Connection checkAdditionalOutgoingCallLimits(Phone phone) {
        if (phone.getPhoneType() == 2) {
            for (Conference conference : getAllConferences()) {
                if ((conference instanceof CdmaConference) && ((CdmaConference) conference).can(4)) {
                    return Connection.createFailedConnection(new DisconnectCause(8, null, getResources().getString(R.string.callFailed_cdma_call_limit), "merge-capable call exists, prevent flash command."));
                }
            }
        }
        return null;
    }

    private boolean isTtyModeEnabled(Context context) {
        return Settings.Secure.getInt(context.getContentResolver(), "preferred_tty_mode", 0) != 0;
    }

    private void maybeSendInternationalCallEvent(TelephonyConnection telephonyConnection) {
        if (telephonyConnection == null || telephonyConnection.getPhone() == null || telephonyConnection.getPhone().getDefaultPhone() == null) {
            return;
        }
        GsmCdmaPhone defaultPhone = telephonyConnection.getPhone().getDefaultPhone();
        if (defaultPhone instanceof GsmCdmaPhone) {
            GsmCdmaPhone gsmCdmaPhone = defaultPhone;
            if (telephonyConnection.isOutgoingCall() && gsmCdmaPhone.isNotificationOfWfcCallRequired(telephonyConnection.getOriginalConnection().getOrigDialString())) {
                Log.i(this, "placeOutgoingConnection - sending international call on WFC confirmation event", new Object[0]);
                telephonyConnection.sendConnectionEvent("android.telephony.event.EVENT_NOTIFY_INTERNATIONAL_CALL_ON_WFC", null);
            }
        }
    }

    protected IBinder getConnectionServiceBinder() {
        if (this.mMtkBinder == null) {
            Log.d(this, "init MtkConnectionServiceBinder", new Object[0]);
            this.mMtkBinder = new MtkConnectionServiceBinder();
        }
        return this.mMtkBinder;
    }

    private class MtkConnectionServiceBinder extends IMtkConnectionService.Stub {
        private MtkConnectionServiceBinder() {
        }

        public IBinder getBinder() {
            return TelephonyConnectionService.this.mBinder;
        }

        public void addMtkConnectionServiceAdapter(IMtkConnectionServiceAdapter iMtkConnectionServiceAdapter) {
            Log.d(this, "MtkConnectionServiceBinder add IMtkConnectionServiceAdapter", new Object[0]);
            TelephonyConnectionService.this.mMtkAdapter = iMtkConnectionServiceAdapter;
        }

        public void clearMtkConnectionServiceAdapter() {
            Log.d(this, "MtkConnectionServiceBinder clear IMtkConnectionServiceAdapter", new Object[0]);
            TelephonyConnectionService.this.mMtkAdapter = null;
        }

        public void hangupAll(String str) {
            TelephonyConnectionService.this.mMtkHandler.obtainMessage(TelephonyConnectionService.MSG_HANGUP_ALL, str).sendToTarget();
        }

        public void handleOrderedOperation(String str, String str2, String str3) {
            SomeArgs someArgsObtain = SomeArgs.obtain();
            someArgsObtain.arg1 = str;
            someArgsObtain.arg2 = str2;
            someArgsObtain.arg3 = str3;
            TelephonyConnectionService.this.mMtkHandler.obtainMessage(TelephonyConnectionService.MSG_HANDLE_ORDERED_USER_OPERATION, someArgsObtain).sendToTarget();
        }

        public void explicitCallTransfer(String str) {
            TelephonyConnectionService.this.mMtkHandler.obtainMessage(TimeConsumingPreferenceActivity.STK_CC_SS_TO_DIAL_VIDEO_ERROR, str).sendToTarget();
        }

        public void blindAssuredEct(String str, String str2, int i) {
            SomeArgs someArgsObtain = SomeArgs.obtain();
            someArgsObtain.arg1 = str;
            someArgsObtain.arg2 = str2;
            someArgsObtain.argi1 = i;
            TelephonyConnectionService.this.mMtkHandler.obtainMessage(TelephonyConnectionService.MSG_BLIND_ASSURED_ECT, someArgsObtain).sendToTarget();
        }

        public void inviteConferenceParticipants(String str, List<String> list) {
            SomeArgs someArgsObtain = SomeArgs.obtain();
            someArgsObtain.arg1 = str;
            someArgsObtain.arg2 = list;
            TelephonyConnectionService.this.mMtkHandler.obtainMessage(TelephonyConnectionService.MSG_INVITE_CONFERENCE_PARTICIPANTS, someArgsObtain).sendToTarget();
        }

        public void createConference(PhoneAccountHandle phoneAccountHandle, String str, ConnectionRequest connectionRequest, List<String> list, boolean z, Session.Info info) {
            SomeArgs someArgsObtain = SomeArgs.obtain();
            someArgsObtain.arg1 = phoneAccountHandle;
            someArgsObtain.arg2 = str;
            someArgsObtain.arg3 = connectionRequest;
            someArgsObtain.arg4 = list;
            someArgsObtain.argi1 = z ? 1 : 0;
            someArgsObtain.arg5 = info;
            TelephonyConnectionService.this.mMtkHandler.obtainMessage(TelephonyConnectionService.MSG_CREATE_CONFERENCE, someArgsObtain).sendToTarget();
        }

        public void deviceSwitch(String str, String str2, String str3) {
            SomeArgs someArgsObtain = SomeArgs.obtain();
            someArgsObtain.arg1 = str;
            someArgsObtain.arg2 = str2;
            someArgsObtain.arg3 = str3;
            TelephonyConnectionService.this.mMtkHandler.obtainMessage(TelephonyConnectionService.MSG_DEVICE_SWITCH, someArgsObtain).sendToTarget();
        }

        public void cancelDeviceSwitch(String str) {
            SomeArgs someArgsObtain = SomeArgs.obtain();
            someArgsObtain.arg1 = str;
            TelephonyConnectionService.this.mMtkHandler.obtainMessage(TelephonyConnectionService.MSG_CANCEL_DEVICE_SWITCH, someArgsObtain).sendToTarget();
        }
    }

    protected void createConnection(PhoneAccountHandle phoneAccountHandle, String str, ConnectionRequest connectionRequest, boolean z, boolean z2) {
        Connection connectionOnCreateIncomingConnection;
        PhoneAccountHandle accountHandle;
        PhoneAccountHandle phoneAccountHandle2;
        boolean z3 = connectionRequest.getExtras() != null && connectionRequest.getExtras().getBoolean("android.telecom.extra.IS_HANDOVER", false);
        boolean z4 = connectionRequest.getExtras() != null && connectionRequest.getExtras().getBoolean("android.telecom.extra.IS_HANDOVER_CONNECTION", false);
        Log.d(this, "createConnection, callManagerAccount: %s, callId: %s, request: %s, isIncoming: %b, isUnknown: %b, isLegacyHandover: %b, isHandover: %b", phoneAccountHandle, str, connectionRequest, Boolean.valueOf(z), Boolean.valueOf(z2), Boolean.valueOf(z3), Boolean.valueOf(z4));
        if (!isAdaptersAvailable()) {
            Log.i(this, "createConnection, adapter not available, call should have been aborted", new Object[0]);
            return;
        }
        IVideoProvider iVideoProvider = null;
        if (z4) {
            if (connectionRequest.getExtras() != null) {
                phoneAccountHandle2 = (PhoneAccountHandle) connectionRequest.getExtras().getParcelable("android.telecom.extra.HANDOVER_FROM_PHONE_ACCOUNT");
            } else {
                phoneAccountHandle2 = null;
            }
            if (!z) {
                connectionOnCreateIncomingConnection = onCreateOutgoingHandoverConnection(phoneAccountHandle2, connectionRequest);
            } else {
                connectionOnCreateIncomingConnection = onCreateIncomingHandoverConnection(phoneAccountHandle2, connectionRequest);
            }
        } else if (z2) {
            connectionOnCreateIncomingConnection = onCreateUnknownConnection(phoneAccountHandle, connectionRequest);
        } else {
            connectionOnCreateIncomingConnection = z ? onCreateIncomingConnection(phoneAccountHandle, connectionRequest) : onCreateOutgoingConnection(phoneAccountHandle, connectionRequest);
        }
        Log.d(this, "createConnection, connection: %s", connectionOnCreateIncomingConnection);
        if (connectionOnCreateIncomingConnection == null) {
            Log.i(this, "createConnection, implementation returned null connection.", new Object[0]);
            connectionOnCreateIncomingConnection = Connection.createFailedConnection(new DisconnectCause(1, "IMPL_RETURNED_NULL_CONNECTION"));
        }
        if (connectionOnCreateIncomingConnection instanceof TelephonyConnection) {
            accountHandle = ((TelephonyConnection) connectionOnCreateIncomingConnection).getAccountHandle();
        } else {
            accountHandle = null;
        }
        if (accountHandle == null) {
            accountHandle = connectionRequest.getAccountHandle();
        } else {
            Log.d(this, "createConnection, set back phone account:%s", accountHandle);
        }
        connectionOnCreateIncomingConnection.setTelecomCallId(str);
        if (connectionOnCreateIncomingConnection.getState() != 6) {
            addConnection(accountHandle, str, connectionOnCreateIncomingConnection);
        }
        Uri address = connectionOnCreateIncomingConnection.getAddress();
        Log.v(this, "createConnection, number: %s, state: %s, capabilities: %s, properties: %s", Connection.toLogSafePhoneNumber(address == null ? "null" : address.getSchemeSpecificPart()), Connection.stateToString(connectionOnCreateIncomingConnection.getState()), MtkConnection.capabilitiesToString(connectionOnCreateIncomingConnection.getConnectionCapabilities()), MtkConnection.propertiesToString(connectionOnCreateIncomingConnection.getConnectionProperties()));
        Log.d(this, "createConnection, calling handleCreateConnectionSuccessful %s", str);
        ConnectionServiceAdapter connectionServiceAdapter = this.mAdapter;
        int state = connectionOnCreateIncomingConnection.getState();
        int connectionCapabilities = connectionOnCreateIncomingConnection.getConnectionCapabilities();
        int connectionProperties = connectionOnCreateIncomingConnection.getConnectionProperties();
        int supportedAudioRoutes = connectionOnCreateIncomingConnection.getSupportedAudioRoutes();
        Uri address2 = connectionOnCreateIncomingConnection.getAddress();
        int addressPresentation = connectionOnCreateIncomingConnection.getAddressPresentation();
        String callerDisplayName = connectionOnCreateIncomingConnection.getCallerDisplayName();
        int callerDisplayNamePresentation = connectionOnCreateIncomingConnection.getCallerDisplayNamePresentation();
        if (connectionOnCreateIncomingConnection.getVideoProvider() != null) {
            iVideoProvider = connectionOnCreateIncomingConnection.getVideoProvider().getInterface();
        }
        connectionServiceAdapter.handleCreateConnectionComplete(str, connectionRequest, new ParcelableConnection(accountHandle, state, connectionCapabilities, connectionProperties, supportedAudioRoutes, address2, addressPresentation, callerDisplayName, callerDisplayNamePresentation, iVideoProvider, connectionOnCreateIncomingConnection.getVideoState(), connectionOnCreateIncomingConnection.isRingbackRequested(), connectionOnCreateIncomingConnection.getAudioModeIsVoip(), connectionOnCreateIncomingConnection.getConnectTimeMillis(), connectionOnCreateIncomingConnection.getConnectElapsedTimeMillis(), connectionOnCreateIncomingConnection.getStatusHints(), connectionOnCreateIncomingConnection.getDisconnectCause(), createIdList(connectionOnCreateIncomingConnection.getConferenceables()), connectionOnCreateIncomingConnection.getExtras()));
        if (z && connectionRequest.shouldShowIncomingCallUi() && (connectionOnCreateIncomingConnection.getConnectionProperties() & 128) == 128) {
            connectionOnCreateIncomingConnection.onShowIncomingCallUi();
        }
        if (z2) {
            triggerConferenceRecalculate();
        }
        if (connectionOnCreateIncomingConnection.getState() != 6) {
            forceSuppMessageUpdate(connectionOnCreateIncomingConnection);
        }
    }

    public TelephonyConnection getFgConnection() {
        Call.State state;
        for (Connection connection : getAllConnections()) {
            if (connection instanceof TelephonyConnection) {
                TelephonyConnection telephonyConnection = (TelephonyConnection) connection;
                if (telephonyConnection.getCall() != null && ((state = telephonyConnection.getCall().getState()) == Call.State.ACTIVE || state == Call.State.DIALING || state == Call.State.ALERTING)) {
                    return telephonyConnection;
                }
            }
        }
        return null;
    }

    protected List<TelephonyConnection> getBgConnection() {
        ArrayList arrayList = new ArrayList();
        for (Connection connection : getAllConnections()) {
            if (connection instanceof TelephonyConnection) {
                TelephonyConnection telephonyConnection = (TelephonyConnection) connection;
                if (telephonyConnection.getCall() != null && telephonyConnection.getCall().getState() == Call.State.HOLDING) {
                    arrayList.add(telephonyConnection);
                }
            }
        }
        return arrayList;
    }

    protected List<TelephonyConnection> getRingingConnection() {
        ArrayList arrayList = new ArrayList();
        for (Connection connection : getAllConnections()) {
            if (connection instanceof TelephonyConnection) {
                TelephonyConnection telephonyConnection = (TelephonyConnection) connection;
                if (telephonyConnection.getCall() != null && telephonyConnection.getCall().getState().isRinging()) {
                    arrayList.add(telephonyConnection);
                }
            }
        }
        return arrayList;
    }

    protected int getFgCallCount() {
        if (getFgConnection() != null) {
            return 1;
        }
        return 0;
    }

    protected int getBgCallCount() {
        return getBgConnection().size();
    }

    protected int getRingingCallCount() {
        return getRingingConnection().size();
    }

    public boolean canDial(PhoneAccountHandle phoneAccountHandle, String str) {
        Call call;
        boolean z = getRingingCallCount() > 0;
        boolean z2 = getFgCallCount() > 0;
        boolean zIsInCallMmiCommands = isInCallMmiCommands(str);
        Call.State state = Call.State.IDLE;
        Phone phoneForAccount = getPhoneForAccount(phoneAccountHandle, false);
        TelephonyConnection fgConnection = getFgConnection();
        if (zIsInCallMmiCommands && z2 && fgConnection != null && phoneForAccount != null && phoneForAccount != fgConnection.getPhone() && phoneForAccount.getImsPhone() != null && phoneForAccount.getImsPhone() != fgConnection.getPhone()) {
            log("phone is different, set bIsInCallMmiCommands to false");
            zIsInCallMmiCommands = false;
        }
        if (fgConnection != null && (call = fgConnection.getCall()) != null) {
            state = call.getState();
        }
        boolean zIsECCExists = MtkTelephonyConnectionServiceUtil.getInstance().isECCExists();
        boolean z3 = !zIsECCExists && (!z || zIsInCallMmiCommands) && state != Call.State.DISCONNECTING;
        if (!z3) {
            log("canDial hasRingingCall=" + z + " hasActiveCall=" + z2 + " fgCallState=" + state + " getFgConnection=" + fgConnection + " getRingingConnection=" + getRingingConnection() + " bECCExists=" + zIsECCExists);
        }
        return z3;
    }

    private boolean isInCallMmiCommands(String str) {
        switch (str.charAt(0)) {
            case '0':
            case '3':
            case '4':
            case '5':
                if (str.length() != 1) {
                    return false;
                }
                return true;
            case '1':
            case '2':
                if (str.length() != 1 && str.length() != 2) {
                    return false;
                }
                return true;
            default:
                return false;
        }
    }

    private void hangupAll(String str) {
        Log.d(this, "hangupAll %s", str);
        if (this.mConnectionById.containsKey(str)) {
            ((TelephonyConnection) findConnectionForAction(str, "hangupAll")).onHangupAll();
            return;
        }
        Conference conferenceFindConferenceForAction = findConferenceForAction(str, "hangupAll");
        if (conferenceFindConferenceForAction instanceof TelephonyConference) {
            ((TelephonyConference) conferenceFindConferenceForAction).onHangupAll();
        } else if (conferenceFindConferenceForAction instanceof CdmaConference) {
            ((CdmaConference) conferenceFindConferenceForAction).onHangupAll();
        } else if (conferenceFindConferenceForAction instanceof ImsConference) {
            ((ImsConference) conferenceFindConferenceForAction).onHangupAll();
        }
    }

    private void disconnect(String str, String str2) {
        Log.d(this, "disconnect %s, pending call action %s", str, str2);
        if (this.mConnectionById.containsKey(str)) {
            ((TelephonyConnection) findConnectionForAction(str, "mediatek.telecom.operation.DISCONNECT_CALL")).onDisconnect();
            return;
        }
        Conference conferenceFindConferenceForAction = findConferenceForAction(str, "mediatek.telecom.operation.DISCONNECT_CALL");
        if (conferenceFindConferenceForAction instanceof TelephonyConference) {
            ((TelephonyConference) conferenceFindConferenceForAction).onDisconnect(str2);
        } else if (conferenceFindConferenceForAction instanceof CdmaConference) {
            ((CdmaConference) conferenceFindConferenceForAction).onDisconnect();
        } else if (conferenceFindConferenceForAction instanceof ImsConference) {
            ((ImsConference) conferenceFindConferenceForAction).onDisconnect();
        }
    }

    protected void addConnection(PhoneAccountHandle phoneAccountHandle, String str, Connection connection) {
        connection.setTelecomCallId(str);
        this.mConnectionById.put(str, connection);
        this.mIdByConnection.put(connection, str);
        connection.addConnectionListener(((ConnectionService) this).mConnectionListener);
        connection.setConnectionService(this);
        connection.setPhoneAccountHandle(phoneAccountHandle);
        onConnectionAdded(connection);
        if (connection instanceof TelephonyConnection) {
            ((TelephonyConnection) connection).fireOnCallState();
        }
    }

    private void log(String str) {
        Log.d(TAG, str, new Object[0]);
    }

    protected void forceSuppMessageUpdate(Connection connection) {
        MtkTelephonyConnectionServiceUtil.getInstance().forceSuppMessageUpdate((TelephonyConnection) connection);
    }

    private void explicitCallTransfer(String str) {
        if (!canTransfer((Connection) this.mConnectionById.get(str))) {
            Log.d(this, "explicitCallTransfer %s fail", str);
        } else {
            Log.d(this, "explicitCallTransfer %s", str);
            ((TelephonyConnection) findConnectionForAction(str, "explicitCallTransfer")).onExplicitCallTransfer();
        }
    }

    private void explicitCallTransfer(String str, String str2, int i) {
        if (!canBlindAssuredTransfer((Connection) this.mConnectionById.get(str))) {
            Log.d(this, "explicitCallTransfer %s fail", str);
        } else {
            Log.d(this, "explicitCallTransfer %s %s %d", str, str2, Integer.valueOf(i));
            ((TelephonyConnection) findConnectionForAction(str, "explicitCallTransfer")).onExplicitCallTransfer(str2, i);
        }
    }

    private void deviceSwitch(String str, String str2, String str3) {
        Log.d(this, "deviceSwitch %s %s %s", str, str2, str3);
        ((TelephonyConnection) findConnectionForAction(str, "deviceSwitch")).onDeviceSwitch(str2, str3);
    }

    private void cancelDeviceSwitch(String str) {
        Log.d(this, "cancelDeviceSwitch %s", str);
        ((TelephonyConnection) findConnectionForAction(str, "cancelDeviceSwitch")).onCancelDeviceSwitch();
    }

    public boolean canTransfer(Connection connection) {
        Phone phone;
        if (connection == null) {
            log("canTransfer: connection is null");
            return false;
        }
        if (!(connection instanceof TelephonyConnection)) {
            log("canTransfer: the connection isn't telephonyConnection");
            return false;
        }
        TelephonyConnection telephonyConnection = (TelephonyConnection) connection;
        TelephonyConnection fgConnection = getFgConnection();
        Phone phone2 = null;
        if (fgConnection != null) {
            phone = fgConnection.getPhone();
        } else {
            phone = null;
        }
        if (connection != null) {
            phone2 = telephonyConnection.getPhone();
        }
        return phone2 == phone && phone != null && phone.canTransfer();
    }

    private void resetTreatAsEmergencyCall(Connection connection) {
        if (connection instanceof TelephonyConnection) {
            ((TelephonyConnection) connection).resetTreatAsEmergencyCall();
        }
    }

    private Connection switchPhoneIfNeeded(final ConnectionRequest connectionRequest, final Uri uri, final String str) {
        if (hasC2kOverImsModem() || MtkTelephonyManagerEx.getDefault().useVzwLogic() || MtkTelephonyManagerEx.getDefault().useATTLogic()) {
            return null;
        }
        if (this.mSwitchPhoneHelper == null) {
            this.mSwitchPhoneHelper = new SwitchPhoneHelper(this, str);
        }
        if (!this.mSwitchPhoneHelper.needToPrepareForDial()) {
            return null;
        }
        final Connection telephonyConnection = getTelephonyConnection(connectionRequest, str, true, uri, this.mPhoneFactoryProxy.getDefaultPhone());
        if (!(telephonyConnection instanceof TelephonyConnection)) {
            Log.i(this, "onCreateOutgoingConnection, create emergency connection failed", new Object[0]);
            return telephonyConnection;
        }
        this.mSwitchPhoneHelper.prepareForDial(new SwitchPhoneHelper.Callback() {
            @Override
            public void onComplete(boolean z) {
                if (telephonyConnection.getState() == 6) {
                    Log.d(this, "prepareForDial, connection disconnect", new Object[0]);
                    MtkTelephonyConnectionServiceUtil.getInstance().setInEcc(false);
                    return;
                }
                if (z) {
                    Log.d(this, "startTurnOnRadio", new Object[0]);
                    TelephonyConnectionService.this.startTurnOnRadio(telephonyConnection, connectionRequest, uri, str);
                    return;
                }
                if (MtkTelephonyConnectionServiceUtil.getInstance().isEccRetryOn()) {
                    Log.d(this, "ECC retry: clear ECC param", new Object[0]);
                    MtkTelephonyConnectionServiceUtil.getInstance().clearEccRetryParams();
                }
                Log.d(this, "prepareForDial, failed to turn on radio", new Object[0]);
                telephonyConnection.setDisconnected(DisconnectCauseUtil.toTelecomDisconnectCause(17, "Failed to turn on radio."));
                MtkTelephonyConnectionServiceUtil.getInstance().setInEcc(false);
                telephonyConnection.destroy();
            }
        });
        return telephonyConnection;
    }

    private void startTurnOnRadio(final Connection connection, final ConnectionRequest connectionRequest, final Uri uri, final String str) {
        final int phoneType = PhoneFactory.getDefaultPhone().getPhoneType();
        if (this.mRadioOnHelper == null) {
            this.mRadioOnHelper = new RadioOnHelper(this);
        }
        this.mRadioOnHelper.triggerRadioOnAndListen(new RadioOnStateListener.Callback() {
            @Override
            public void onComplete(RadioOnStateListener radioOnStateListener, boolean z) {
                if (connection.getState() == 6) {
                    Log.i(this, "startTurnOnRadio, connection disconnect", new Object[0]);
                    MtkTelephonyConnectionServiceUtil.getInstance().setInEcc(false);
                    return;
                }
                if (z) {
                    Phone phoneForAccount = TelephonyConnectionService.this.getPhoneForAccount(connectionRequest.getAccountHandle(), true, str);
                    if (MtkTelephonyConnectionServiceUtil.getInstance().isDataOnlyMode(phoneForAccount)) {
                        Log.i(this, "startTurnOnRadio, 4G data only", new Object[0]);
                        if (MtkTelephonyConnectionServiceUtil.getInstance().isEccRetryOn()) {
                            Log.i(this, "ECC retry: clear ECC param", new Object[0]);
                            MtkTelephonyConnectionServiceUtil.getInstance().clearEccRetryParams();
                        }
                        connection.setDisconnected(DisconnectCauseUtil.toTelecomDisconnectCause(44, null));
                        MtkTelephonyConnectionServiceUtil.getInstance().setInEcc(false);
                        connection.destroy();
                        return;
                    }
                    boolean zIsEmergencyNumber = PhoneNumberUtils.isEmergencyNumber(str);
                    if (!MtkTelephonyConnectionServiceUtil.getInstance().isEccRetryOn() && zIsEmergencyNumber) {
                        Log.i(this, "ECC Retry : set param with Intial ECC.", new Object[0]);
                        MtkTelephonyConnectionServiceUtil.getInstance().setEccRetryParams(connectionRequest, phoneForAccount.getPhoneId());
                    }
                    if (phoneForAccount.getPhoneType() != phoneType) {
                        Connection telephonyConnection = TelephonyConnectionService.this.getTelephonyConnection(connectionRequest, str, true, uri, phoneForAccount);
                        boolean z2 = telephonyConnection instanceof TelephonyConnection;
                        if (z2) {
                            TelephonyConnectionService.this.addExistingConnection(PhoneUtils.makePstnPhoneAccountHandle(phoneForAccount), telephonyConnection);
                            TelephonyConnectionService.this.resetTreatAsEmergencyCall(connection);
                            connection.setDisconnected(DisconnectCauseUtil.toTelecomDisconnectCause(44, "Reconnecting outgoing Emergency Call."));
                        } else {
                            if (MtkTelephonyConnectionServiceUtil.getInstance().isEccRetryOn()) {
                                Log.d(this, "ECC retry: clear ECC param", new Object[0]);
                                MtkTelephonyConnectionServiceUtil.getInstance().clearEccRetryParams();
                            }
                            connection.setDisconnected(telephonyConnection.getDisconnectCause());
                            MtkTelephonyConnectionServiceUtil.getInstance().setInEcc(false);
                        }
                        connection.destroy();
                        if (z2) {
                            TelephonyConnectionService.this.placeOutgoingConnection((TelephonyConnection) telephonyConnection, phoneForAccount, connectionRequest);
                            return;
                        }
                        return;
                    }
                    TelephonyConnectionService.this.placeOutgoingConnection((TelephonyConnection) connection, phoneForAccount, connectionRequest);
                    return;
                }
                if (MtkTelephonyConnectionServiceUtil.getInstance().isEccRetryOn()) {
                    Log.i(this, "ECC retry: clear ECC param", new Object[0]);
                    MtkTelephonyConnectionServiceUtil.getInstance().clearEccRetryParams();
                }
                Log.i(this, "startTurnOnRadio, failed to turn on radio", new Object[0]);
                connection.setDisconnected(DisconnectCauseUtil.toTelecomDisconnectCause(17, "Failed to turn on radio."));
                MtkTelephonyConnectionServiceUtil.getInstance().setInEcc(false);
                connection.destroy();
            }

            @Override
            public boolean isOkToCall(Phone phone, int i) {
                return phone.getState() == PhoneConstants.State.OFFHOOK || phone.getServiceStateTracker().isRadioOn();
            }
        });
    }

    private boolean isAdaptersAvailable() {
        Object objInvoke;
        try {
            Field declaredField = this.mAdapter.getClass().getDeclaredField("mAdapters");
            declaredField.setAccessible(true);
            Object obj = declaredField.get(this.mAdapter);
            if (obj != null && (objInvoke = obj.getClass().getMethod("size", new Class[0]).invoke(obj, new Object[0])) != null && (objInvoke instanceof Integer) && ((Integer) objInvoke).intValue() == 0) {
                Log.w(this, "isAdaptersAvailable, " + obj + ", " + objInvoke, new Object[0]);
                return false;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return true;
    }

    public void inviteConferenceParticipants(String str, List<String> list) {
        Log.d(this, "inviteConferenceParticipants %s", str);
        if (this.mConferenceById.containsKey(str)) {
            Conference conferenceFindConferenceForAction = findConferenceForAction(str, "inviteConferenceParticipants");
            if (conferenceFindConferenceForAction instanceof ImsConference) {
                ((ImsConference) conferenceFindConferenceForAction).onInviteConferenceParticipants(list);
            }
        }
    }

    private void createConference(PhoneAccountHandle phoneAccountHandle, String str, ConnectionRequest connectionRequest, List<String> list, boolean z, Session.Info info) {
        Log.d(this, "createConference, callManagerAccount: %s, conferenceCallId: %s, request: %s, numbers: %s, isIncoming: %b", phoneAccountHandle, str, connectionRequest, list, Boolean.valueOf(z));
        Conference conferenceOnCreateConference = onCreateConference(phoneAccountHandle, str, connectionRequest, list, z, info);
        if (conferenceOnCreateConference == null) {
            Log.d(this, "Fail to create conference!", new Object[0]);
            conferenceOnCreateConference = getNullConference();
        } else if (conferenceOnCreateConference.getState() != 6) {
            if (this.mIdByConference.containsKey(conferenceOnCreateConference)) {
                Log.d(this, "Re-adding an existing conference: %s.", conferenceOnCreateConference);
            } else {
                this.mConferenceById.put(str, conferenceOnCreateConference);
                this.mIdByConference.put(conferenceOnCreateConference, str);
                conferenceOnCreateConference.addListener(this.mConferenceListener);
            }
        }
        ParcelableConference parcelableConference = new ParcelableConference(conferenceOnCreateConference.getPhoneAccountHandle(), conferenceOnCreateConference.getState(), conferenceOnCreateConference.getConnectionCapabilities(), conferenceOnCreateConference.getConnectionProperties(), (List) null, conferenceOnCreateConference.getVideoProvider() == null ? null : conferenceOnCreateConference.getVideoProvider().getInterface(), conferenceOnCreateConference.getVideoState(), conferenceOnCreateConference.getConnectTimeMillis(), conferenceOnCreateConference.getConnectionStartElapsedRealTime(), conferenceOnCreateConference.getStatusHints(), conferenceOnCreateConference.getExtras());
        if (this.mMtkAdapter != null) {
            try {
                this.mMtkAdapter.handleCreateConferenceComplete(str, connectionRequest, parcelableConference, conferenceOnCreateConference.getDisconnectCause());
            } catch (RemoteException e) {
                e.printStackTrace();
            }
        }
    }

    protected Conference onCreateConference(PhoneAccountHandle phoneAccountHandle, String str, ConnectionRequest connectionRequest, List<String> list, boolean z, Session.Info info) {
        if (str == null || ((!list.isEmpty() && !canDial(connectionRequest.getAccountHandle(), list.get(0))) || this.mImsConferenceController.isConferenceExist())) {
            Log.d(this, "onCreateConference(), canDial check fail", new Object[0]);
            return MtkTelephonyConnectionServiceUtil.getInstance().createFailedConference(43, "canDial() check fail");
        }
        Phone phoneForAccount = getPhoneForAccount(connectionRequest.getAccountHandle(), false);
        if (!z && list.size() > 5) {
            Log.d(this, "onCreateConference(), more than 5 numbers", new Object[0]);
            if (phoneForAccount != null) {
                ImsConference.toastWhenConferenceIsFull(phoneForAccount.getContext());
            }
            return MtkTelephonyConnectionServiceUtil.getInstance().createFailedConference(43, "more than 5 numbers");
        }
        Conference conferenceCreateConference = MtkTelephonyConnectionServiceUtil.getInstance().createConference(this.mImsConferenceController, phoneForAccount, connectionRequest, list, z);
        Log.d(this, "onCreateConference(), add conference to HoldTracker, conference: " + conferenceCreateConference, new Object[0]);
        onConferenceAdded(conferenceCreateConference);
        return conferenceCreateConference;
    }

    void performImsConferenceSRVCC(Conference conference, ArrayList<com.android.internal.telephony.Connection> arrayList, String str) {
        if (conference == null) {
            Log.e((Object) this, (Throwable) new CallStateException(), "performImsConferenceSRVCC(): abnormal case, imsConf is null", new Object[0]);
            return;
        }
        if (arrayList == null || arrayList.size() < 2) {
            Log.e((Object) this, (Throwable) new CallStateException(), "performImsConferenceSRVCC(): abnormal case, newConnections is null", new Object[0]);
            return;
        }
        if (arrayList.get(0) == null || arrayList.get(0).getCall() == null || arrayList.get(0).getCall().getPhone() == null) {
            Log.e((Object) this, (Throwable) new CallStateException(), "performImsConferenceSRVCC(): abnormal case, can't get phone instance", new Object[0]);
            return;
        }
        PhoneAccountHandle phoneAccountHandleMakePstnPhoneAccountHandle = PhoneUtils.makePstnPhoneAccountHandle(arrayList.get(0).getCall().getPhone());
        TelephonyConference telephonyConference = new TelephonyConference(phoneAccountHandleMakePstnPhoneAccountHandle);
        replaceConference(conference, telephonyConference);
        if (this.mTelephonyConferenceController instanceof TelephonyConferenceController) {
            this.mTelephonyConferenceController.setHandoveredConference(telephonyConference);
        }
        ArrayList arrayList2 = new ArrayList();
        for (com.android.internal.telephony.Connection connection : arrayList) {
            MtkGsmCdmaConnection mtkGsmCdmaConnection = new MtkGsmCdmaConnection(1, null, str, null, false, false);
            mtkGsmCdmaConnection.setAddress(Uri.fromParts("tel", connection.getAddress(), null), 1);
            arrayList2.add(mtkGsmCdmaConnection);
            addExistingConnection(phoneAccountHandleMakePstnPhoneAccountHandle, mtkGsmCdmaConnection);
            mtkGsmCdmaConnection.addTelephonyConnectionListener(this.mTelephonyConnectionListener);
        }
        for (int i = 0; i < arrayList2.size(); i++) {
            ((TelephonyConnection) arrayList2.get(i)).setOriginalConnection(arrayList.get(i));
        }
    }

    protected void replaceConference(Conference conference, Conference conference2) {
        Log.d(this, "SRVCC: oldConf= %s , newConf= %s", conference, conference2);
        if (conference != conference2 && this.mIdByConference.containsKey(conference)) {
            Log.d(this, "SRVCC: start to do replacement", new Object[0]);
            conference.removeListener(this.mConferenceListener);
            String str = (String) this.mIdByConference.get(conference);
            this.mConferenceById.remove(str);
            this.mIdByConference.remove(conference);
            this.mConferenceById.put(str, conference2);
            this.mIdByConference.put(conference2, str);
            conference2.addListener(this.mConferenceListener);
        }
    }

    public boolean canBlindAssuredTransfer(Connection connection) {
        if (connection == null) {
            Log.d(this, "canBlindAssuredTransfer: connection is null", new Object[0]);
            return false;
        }
        if (!(connection instanceof TelephonyConnection)) {
            Log.d(this, "canBlindAssuredTransfer: the connection isn't telephonyConnection", new Object[0]);
            return false;
        }
        if (!((TelephonyConnection) connection).isImsConnection()) {
            Log.d(this, "canBlindAssuredTransfer: the connection is not an IMS connection", new Object[0]);
            return false;
        }
        if (canTransfer(connection)) {
            Log.d(this, "canBlindAssuredTransfer: the connection has consultative ECT capability", new Object[0]);
            return false;
        }
        return true;
    }

    void notifyEccToSelfActivationSM(MtkGsmCdmaPhone mtkGsmCdmaPhone) {
        Log.d(this, "notifyEccToSelfActivationSM()", new Object[0]);
        Bundle bundle = new Bundle();
        bundle.putInt("key_mo_call_type", 1);
        mtkGsmCdmaPhone.getSelfActivationInstance().selfActivationAction(1, bundle);
    }
}
