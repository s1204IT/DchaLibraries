package android.telecom;

import android.annotation.SuppressLint;
import android.annotation.SystemApi;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.telephony.TelephonyManager;
import android.text.TextUtils;
import com.android.internal.telecom.ITelecomService;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class TelecomManager {
    public static final String ACTION_CHANGE_DEFAULT_DIALER = "android.telecom.action.CHANGE_DEFAULT_DIALER";
    public static final String ACTION_CHANGE_PHONE_ACCOUNTS = "android.telecom.action.CHANGE_PHONE_ACCOUNTS";
    public static final String ACTION_CONFIGURE_PHONE_ACCOUNT = "android.telecom.action.CONFIGURE_PHONE_ACCOUNT";
    public static final String ACTION_CURRENT_TTY_MODE_CHANGED = "android.telecom.action.CURRENT_TTY_MODE_CHANGED";
    public static final String ACTION_DEFAULT_DIALER_CHANGED = "android.telecom.action.DEFAULT_DIALER_CHANGED";
    public static final String ACTION_INCOMING_CALL = "android.telecom.action.INCOMING_CALL";
    public static final String ACTION_NEW_UNKNOWN_CALL = "android.telecom.action.NEW_UNKNOWN_CALL";
    public static final String ACTION_PHONE_ACCOUNT_REGISTERED = "android.telecom.action.PHONE_ACCOUNT_REGISTERED";
    public static final String ACTION_PHONE_ACCOUNT_UNREGISTERED = "android.telecom.action.PHONE_ACCOUNT_UNREGISTERED";
    public static final String ACTION_SHOW_CALL_ACCESSIBILITY_SETTINGS = "android.telecom.action.SHOW_CALL_ACCESSIBILITY_SETTINGS";
    public static final String ACTION_SHOW_CALL_SETTINGS = "android.telecom.action.SHOW_CALL_SETTINGS";
    public static final String ACTION_SHOW_MISSED_CALLS_NOTIFICATION = "android.telecom.action.SHOW_MISSED_CALLS_NOTIFICATION";
    public static final String ACTION_SHOW_RESPOND_VIA_SMS_SETTINGS = "android.telecom.action.SHOW_RESPOND_VIA_SMS_SETTINGS";
    public static final String ACTION_TTY_PREFERRED_MODE_CHANGED = "android.telecom.action.TTY_PREFERRED_MODE_CHANGED";
    public static final char DTMF_CHARACTER_PAUSE = ',';
    public static final char DTMF_CHARACTER_WAIT = ';';
    public static final ComponentName EMERGENCY_DIALER_COMPONENT = ComponentName.createRelative(TelephonyManager.PHONE_PROCESS_NAME, ".EmergencyDialer");
    public static final String EXTRA_CALL_AUDIO_STATE = "android.telecom.extra.CALL_AUDIO_STATE";

    @SystemApi
    public static final String EXTRA_CALL_BACK_INTENT = "android.telecom.extra.CALL_BACK_INTENT";
    public static final String EXTRA_CALL_BACK_NUMBER = "android.telecom.extra.CALL_BACK_NUMBER";
    public static final String EXTRA_CALL_CREATED_TIME_MILLIS = "android.telecom.extra.CALL_CREATED_TIME_MILLIS";
    public static final String EXTRA_CALL_DISCONNECT_CAUSE = "android.telecom.extra.CALL_DISCONNECT_CAUSE";
    public static final String EXTRA_CALL_DISCONNECT_MESSAGE = "android.telecom.extra.CALL_DISCONNECT_MESSAGE";
    public static final String EXTRA_CALL_EXTERNAL_RINGER = "android.telecom.extra.CALL_EXTERNAL_RINGER";
    public static final String EXTRA_CALL_SUBJECT = "android.telecom.extra.CALL_SUBJECT";
    public static final String EXTRA_CALL_TECHNOLOGY_TYPE = "android.telecom.extra.CALL_TECHNOLOGY_TYPE";
    public static final String EXTRA_CALL_TELECOM_ROUTING_END_TIME_MILLIS = "android.telecom.extra.CALL_TELECOM_ROUTING_END_TIME_MILLIS";
    public static final String EXTRA_CALL_TELECOM_ROUTING_START_TIME_MILLIS = "android.telecom.extra.CALL_TELECOM_ROUTING_START_TIME_MILLIS";
    public static final String EXTRA_CHANGE_DEFAULT_DIALER_PACKAGE_NAME = "android.telecom.extra.CHANGE_DEFAULT_DIALER_PACKAGE_NAME";

    @SystemApi
    public static final String EXTRA_CLEAR_MISSED_CALLS_INTENT = "android.telecom.extra.CLEAR_MISSED_CALLS_INTENT";

    @SystemApi
    public static final String EXTRA_CONNECTION_SERVICE = "android.telecom.extra.CONNECTION_SERVICE";
    public static final String EXTRA_CURRENT_TTY_MODE = "android.telecom.intent.extra.CURRENT_TTY_MODE";
    public static final String EXTRA_HANDOVER_FROM_PHONE_ACCOUNT = "android.telecom.extra.HANDOVER_FROM_PHONE_ACCOUNT";
    public static final String EXTRA_INCOMING_CALL_ADDRESS = "android.telecom.extra.INCOMING_CALL_ADDRESS";
    public static final String EXTRA_INCOMING_CALL_EXTRAS = "android.telecom.extra.INCOMING_CALL_EXTRAS";
    public static final String EXTRA_INCOMING_VIDEO_STATE = "android.telecom.extra.INCOMING_VIDEO_STATE";
    public static final String EXTRA_IS_HANDOVER = "android.telecom.extra.IS_HANDOVER";
    public static final String EXTRA_IS_HANDOVER_CONNECTION = "android.telecom.extra.IS_HANDOVER_CONNECTION";
    public static final String EXTRA_NEW_OUTGOING_CALL_CANCEL_TIMEOUT = "android.telecom.extra.NEW_OUTGOING_CALL_CANCEL_TIMEOUT";
    public static final String EXTRA_NOTIFICATION_COUNT = "android.telecom.extra.NOTIFICATION_COUNT";
    public static final String EXTRA_NOTIFICATION_PHONE_NUMBER = "android.telecom.extra.NOTIFICATION_PHONE_NUMBER";
    public static final String EXTRA_OUTGOING_CALL_EXTRAS = "android.telecom.extra.OUTGOING_CALL_EXTRAS";
    public static final String EXTRA_PHONE_ACCOUNT_HANDLE = "android.telecom.extra.PHONE_ACCOUNT_HANDLE";
    public static final String EXTRA_START_CALL_WITH_RTT = "android.telecom.extra.START_CALL_WITH_RTT";
    public static final String EXTRA_START_CALL_WITH_SPEAKERPHONE = "android.telecom.extra.START_CALL_WITH_SPEAKERPHONE";
    public static final String EXTRA_START_CALL_WITH_VIDEO_STATE = "android.telecom.extra.START_CALL_WITH_VIDEO_STATE";
    public static final String EXTRA_TTY_PREFERRED_MODE = "android.telecom.intent.extra.TTY_PREFERRED";
    public static final String EXTRA_UNKNOWN_CALL_HANDLE = "android.telecom.extra.UNKNOWN_CALL_HANDLE";
    public static final String EXTRA_USE_ASSISTED_DIALING = "android.telecom.extra.USE_ASSISTED_DIALING";
    public static final String GATEWAY_ORIGINAL_ADDRESS = "android.telecom.extra.GATEWAY_ORIGINAL_ADDRESS";
    public static final String GATEWAY_PROVIDER_PACKAGE = "android.telecom.extra.GATEWAY_PROVIDER_PACKAGE";
    public static final String METADATA_INCLUDE_EXTERNAL_CALLS = "android.telecom.INCLUDE_EXTERNAL_CALLS";
    public static final String METADATA_INCLUDE_SELF_MANAGED_CALLS = "android.telecom.INCLUDE_SELF_MANAGED_CALLS";
    public static final String METADATA_IN_CALL_SERVICE_CAR_MODE_UI = "android.telecom.IN_CALL_SERVICE_CAR_MODE_UI";
    public static final String METADATA_IN_CALL_SERVICE_RINGING = "android.telecom.IN_CALL_SERVICE_RINGING";
    public static final String METADATA_IN_CALL_SERVICE_UI = "android.telecom.IN_CALL_SERVICE_UI";
    public static final int PRESENTATION_ALLOWED = 1;
    public static final int PRESENTATION_PAYPHONE = 4;
    public static final int PRESENTATION_RESTRICTED = 2;
    public static final int PRESENTATION_UNKNOWN = 3;
    private static final String TAG = "TelecomManager";
    public static final int TTY_MODE_FULL = 1;
    public static final int TTY_MODE_HCO = 2;
    public static final int TTY_MODE_OFF = 0;
    public static final int TTY_MODE_VCO = 3;
    private final Context mContext;
    private final ITelecomService mTelecomServiceOverride;

    public static TelecomManager from(Context context) {
        return (TelecomManager) context.getSystemService(Context.TELECOM_SERVICE);
    }

    public TelecomManager(Context context) {
        this(context, null);
    }

    public TelecomManager(Context context, ITelecomService iTelecomService) {
        Context applicationContext = context.getApplicationContext();
        if (applicationContext != null) {
            this.mContext = applicationContext;
        } else {
            this.mContext = context;
        }
        this.mTelecomServiceOverride = iTelecomService;
    }

    public PhoneAccountHandle getDefaultOutgoingPhoneAccount(String str) {
        try {
            if (isServiceConnected()) {
                return getTelecomService().getDefaultOutgoingPhoneAccount(str, this.mContext.getOpPackageName());
            }
            return null;
        } catch (RemoteException e) {
            android.util.Log.e(TAG, "Error calling ITelecomService#getDefaultOutgoingPhoneAccount", e);
            return null;
        }
    }

    public PhoneAccountHandle getUserSelectedOutgoingPhoneAccount() {
        try {
            if (isServiceConnected()) {
                return getTelecomService().getUserSelectedOutgoingPhoneAccount();
            }
            return null;
        } catch (RemoteException e) {
            android.util.Log.e(TAG, "Error calling ITelecomService#getUserSelectedOutgoingPhoneAccount", e);
            return null;
        }
    }

    public void setUserSelectedOutgoingPhoneAccount(PhoneAccountHandle phoneAccountHandle) {
        try {
            if (isServiceConnected()) {
                getTelecomService().setUserSelectedOutgoingPhoneAccount(phoneAccountHandle);
            }
        } catch (RemoteException e) {
            android.util.Log.e(TAG, "Error calling ITelecomService#setUserSelectedOutgoingPhoneAccount");
        }
    }

    public PhoneAccountHandle getSimCallManager() {
        try {
            if (isServiceConnected()) {
                return getTelecomService().getSimCallManager();
            }
            return null;
        } catch (RemoteException e) {
            android.util.Log.e(TAG, "Error calling ITelecomService#getSimCallManager");
            return null;
        }
    }

    public PhoneAccountHandle getSimCallManager(int i) {
        try {
            if (isServiceConnected()) {
                return getTelecomService().getSimCallManagerForUser(i);
            }
            return null;
        } catch (RemoteException e) {
            android.util.Log.e(TAG, "Error calling ITelecomService#getSimCallManagerForUser");
            return null;
        }
    }

    @SystemApi
    public PhoneAccountHandle getConnectionManager() {
        return getSimCallManager();
    }

    @SystemApi
    public List<PhoneAccountHandle> getPhoneAccountsSupportingScheme(String str) {
        try {
            if (isServiceConnected()) {
                return getTelecomService().getPhoneAccountsSupportingScheme(str, this.mContext.getOpPackageName());
            }
        } catch (RemoteException e) {
            android.util.Log.e(TAG, "Error calling ITelecomService#getPhoneAccountsSupportingScheme", e);
        }
        return new ArrayList();
    }

    public List<PhoneAccountHandle> getCallCapablePhoneAccounts() {
        return getCallCapablePhoneAccounts(false);
    }

    public List<PhoneAccountHandle> getSelfManagedPhoneAccounts() {
        try {
            if (isServiceConnected()) {
                return getTelecomService().getSelfManagedPhoneAccounts(this.mContext.getOpPackageName());
            }
        } catch (RemoteException e) {
            android.util.Log.e(TAG, "Error calling ITelecomService#getSelfManagedPhoneAccounts()", e);
        }
        return new ArrayList();
    }

    public List<PhoneAccountHandle> getCallCapablePhoneAccounts(boolean z) {
        try {
            if (isServiceConnected()) {
                return getTelecomService().getCallCapablePhoneAccounts(z, this.mContext.getOpPackageName());
            }
        } catch (RemoteException e) {
            android.util.Log.e(TAG, "Error calling ITelecomService#getCallCapablePhoneAccounts(" + z + ")", e);
        }
        return new ArrayList();
    }

    @SystemApi
    @SuppressLint({"Doclava125"})
    public List<PhoneAccountHandle> getPhoneAccountsForPackage() {
        try {
            if (isServiceConnected()) {
                return getTelecomService().getPhoneAccountsForPackage(this.mContext.getPackageName());
            }
            return null;
        } catch (RemoteException e) {
            android.util.Log.e(TAG, "Error calling ITelecomService#getPhoneAccountsForPackage", e);
            return null;
        }
    }

    public PhoneAccount getPhoneAccount(PhoneAccountHandle phoneAccountHandle) {
        try {
            if (isServiceConnected()) {
                return getTelecomService().getPhoneAccount(phoneAccountHandle);
            }
            return null;
        } catch (RemoteException e) {
            android.util.Log.e(TAG, "Error calling ITelecomService#getPhoneAccount", e);
            return null;
        }
    }

    @SystemApi
    public int getAllPhoneAccountsCount() {
        try {
            if (isServiceConnected()) {
                return getTelecomService().getAllPhoneAccountsCount();
            }
            return 0;
        } catch (RemoteException e) {
            android.util.Log.e(TAG, "Error calling ITelecomService#getAllPhoneAccountsCount", e);
            return 0;
        }
    }

    @SystemApi
    public List<PhoneAccount> getAllPhoneAccounts() {
        try {
            if (isServiceConnected()) {
                return getTelecomService().getAllPhoneAccounts();
            }
        } catch (RemoteException e) {
            android.util.Log.e(TAG, "Error calling ITelecomService#getAllPhoneAccounts", e);
        }
        return Collections.EMPTY_LIST;
    }

    @SystemApi
    public List<PhoneAccountHandle> getAllPhoneAccountHandles() {
        try {
            if (isServiceConnected()) {
                return getTelecomService().getAllPhoneAccountHandles();
            }
        } catch (RemoteException e) {
            android.util.Log.e(TAG, "Error calling ITelecomService#getAllPhoneAccountHandles", e);
        }
        return Collections.EMPTY_LIST;
    }

    public void registerPhoneAccount(PhoneAccount phoneAccount) {
        try {
            if (isServiceConnected()) {
                getTelecomService().registerPhoneAccount(phoneAccount);
            }
        } catch (RemoteException e) {
            android.util.Log.e(TAG, "Error calling ITelecomService#registerPhoneAccount", e);
        }
    }

    public void unregisterPhoneAccount(PhoneAccountHandle phoneAccountHandle) {
        try {
            if (isServiceConnected()) {
                getTelecomService().unregisterPhoneAccount(phoneAccountHandle);
            }
        } catch (RemoteException e) {
            android.util.Log.e(TAG, "Error calling ITelecomService#unregisterPhoneAccount", e);
        }
    }

    @SystemApi
    @SuppressLint({"Doclava125"})
    public void clearPhoneAccounts() {
        clearAccounts();
    }

    @SystemApi
    @SuppressLint({"Doclava125"})
    public void clearAccounts() {
        try {
            if (isServiceConnected()) {
                getTelecomService().clearAccounts(this.mContext.getPackageName());
            }
        } catch (RemoteException e) {
            android.util.Log.e(TAG, "Error calling ITelecomService#clearAccounts", e);
        }
    }

    public void clearAccountsForPackage(String str) {
        try {
            if (isServiceConnected() && !TextUtils.isEmpty(str)) {
                getTelecomService().clearAccounts(str);
            }
        } catch (RemoteException e) {
            android.util.Log.e(TAG, "Error calling ITelecomService#clearAccountsForPackage", e);
        }
    }

    @SystemApi
    @SuppressLint({"Doclava125"})
    public ComponentName getDefaultPhoneApp() {
        try {
            if (isServiceConnected()) {
                return getTelecomService().getDefaultPhoneApp();
            }
            return null;
        } catch (RemoteException e) {
            android.util.Log.e(TAG, "RemoteException attempting to get the default phone app.", e);
            return null;
        }
    }

    public String getDefaultDialerPackage() {
        try {
            if (isServiceConnected()) {
                return getTelecomService().getDefaultDialerPackage();
            }
            return null;
        } catch (RemoteException e) {
            android.util.Log.e(TAG, "RemoteException attempting to get the default dialer package name.", e);
            return null;
        }
    }

    public boolean setDefaultDialer(String str) {
        try {
            if (isServiceConnected()) {
                return getTelecomService().setDefaultDialer(str);
            }
            return false;
        } catch (RemoteException e) {
            android.util.Log.e(TAG, "RemoteException attempting to set the default dialer.", e);
            return false;
        }
    }

    public String getSystemDialerPackage() {
        try {
            if (isServiceConnected()) {
                return getTelecomService().getSystemDialerPackage();
            }
            return null;
        } catch (RemoteException e) {
            android.util.Log.e(TAG, "RemoteException attempting to get the system dialer package name.", e);
            return null;
        }
    }

    public boolean isVoiceMailNumber(PhoneAccountHandle phoneAccountHandle, String str) {
        try {
            if (isServiceConnected()) {
                return getTelecomService().isVoiceMailNumber(phoneAccountHandle, str, this.mContext.getOpPackageName());
            }
            return false;
        } catch (RemoteException e) {
            android.util.Log.e(TAG, "RemoteException calling ITelecomService#isVoiceMailNumber.", e);
            return false;
        }
    }

    public String getVoiceMailNumber(PhoneAccountHandle phoneAccountHandle) {
        try {
            if (isServiceConnected()) {
                return getTelecomService().getVoiceMailNumber(phoneAccountHandle, this.mContext.getOpPackageName());
            }
            return null;
        } catch (RemoteException e) {
            android.util.Log.e(TAG, "RemoteException calling ITelecomService#hasVoiceMailNumber.", e);
            return null;
        }
    }

    public String getLine1Number(PhoneAccountHandle phoneAccountHandle) {
        try {
            if (isServiceConnected()) {
                return getTelecomService().getLine1Number(phoneAccountHandle, this.mContext.getOpPackageName());
            }
            return null;
        } catch (RemoteException e) {
            android.util.Log.e(TAG, "RemoteException calling ITelecomService#getLine1Number.", e);
            return null;
        }
    }

    public boolean isInCall() {
        try {
            if (isServiceConnected()) {
                return getTelecomService().isInCall(this.mContext.getOpPackageName());
            }
            return false;
        } catch (RemoteException e) {
            android.util.Log.e(TAG, "RemoteException calling isInCall().", e);
            return false;
        }
    }

    public boolean isInManagedCall() {
        try {
            if (isServiceConnected()) {
                return getTelecomService().isInManagedCall(this.mContext.getOpPackageName());
            }
            return false;
        } catch (RemoteException e) {
            android.util.Log.e(TAG, "RemoteException calling isInManagedCall().", e);
            return false;
        }
    }

    @SystemApi
    public int getCallState() {
        try {
            if (isServiceConnected()) {
                return getTelecomService().getCallState();
            }
            return 0;
        } catch (RemoteException e) {
            android.util.Log.d(TAG, "RemoteException calling getCallState().", e);
            return 0;
        }
    }

    @SystemApi
    public boolean isRinging() {
        try {
            if (isServiceConnected()) {
                return getTelecomService().isRinging(this.mContext.getOpPackageName());
            }
            return false;
        } catch (RemoteException e) {
            android.util.Log.e(TAG, "RemoteException attempting to get ringing state of phone app.", e);
            return false;
        }
    }

    @SystemApi
    public boolean endCall() {
        try {
            if (isServiceConnected()) {
                return getTelecomService().endCall(this.mContext.getPackageName());
            }
            return false;
        } catch (RemoteException e) {
            android.util.Log.e(TAG, "Error calling ITelecomService#endCall", e);
            return false;
        }
    }

    public void acceptRingingCall() {
        try {
            if (isServiceConnected()) {
                getTelecomService().acceptRingingCall(this.mContext.getPackageName());
            }
        } catch (RemoteException e) {
            android.util.Log.e(TAG, "Error calling ITelecomService#acceptRingingCall", e);
        }
    }

    public void acceptRingingCall(int i) {
        try {
            if (isServiceConnected()) {
                getTelecomService().acceptRingingCallWithVideoState(this.mContext.getPackageName(), i);
            }
        } catch (RemoteException e) {
            android.util.Log.e(TAG, "Error calling ITelecomService#acceptRingingCallWithVideoState", e);
        }
    }

    public void silenceRinger() {
        try {
            if (isServiceConnected()) {
                getTelecomService().silenceRinger(this.mContext.getOpPackageName());
            }
        } catch (RemoteException e) {
            android.util.Log.e(TAG, "Error calling ITelecomService#silenceRinger", e);
        }
    }

    @SystemApi
    public boolean isTtySupported() {
        try {
            if (isServiceConnected()) {
                return getTelecomService().isTtySupported(this.mContext.getOpPackageName());
            }
            return false;
        } catch (RemoteException e) {
            android.util.Log.e(TAG, "RemoteException attempting to get TTY supported state.", e);
            return false;
        }
    }

    public int getCurrentTtyMode() {
        try {
            if (isServiceConnected()) {
                return getTelecomService().getCurrentTtyMode(this.mContext.getOpPackageName());
            }
            return 0;
        } catch (RemoteException e) {
            android.util.Log.e(TAG, "RemoteException attempting to get the current TTY mode.", e);
            return 0;
        }
    }

    public void addNewIncomingCall(PhoneAccountHandle phoneAccountHandle, Bundle bundle) {
        try {
            if (isServiceConnected()) {
                if (bundle != null && bundle.getBoolean("android.telecom.extra.IS_HANDOVER") && this.mContext.getApplicationContext().getApplicationInfo().targetSdkVersion > 27) {
                    android.util.Log.e("TAG", "addNewIncomingCall failed. Use public api acceptHandover for API > O-MR1");
                }
                ITelecomService telecomService = getTelecomService();
                if (bundle == null) {
                    bundle = new Bundle();
                }
                telecomService.addNewIncomingCall(phoneAccountHandle, bundle);
            }
        } catch (RemoteException e) {
            android.util.Log.e(TAG, "RemoteException adding a new incoming call: " + phoneAccountHandle, e);
        }
    }

    @SystemApi
    public void addNewUnknownCall(PhoneAccountHandle phoneAccountHandle, Bundle bundle) {
        try {
            if (isServiceConnected()) {
                ITelecomService telecomService = getTelecomService();
                if (bundle == null) {
                    bundle = new Bundle();
                }
                telecomService.addNewUnknownCall(phoneAccountHandle, bundle);
            }
        } catch (RemoteException e) {
            android.util.Log.e(TAG, "RemoteException adding a new unknown call: " + phoneAccountHandle, e);
        }
    }

    public boolean handleMmi(String str) {
        ITelecomService telecomService = getTelecomService();
        if (telecomService != null) {
            try {
                return telecomService.handlePinMmi(str, this.mContext.getOpPackageName());
            } catch (RemoteException e) {
                android.util.Log.e(TAG, "Error calling ITelecomService#handlePinMmi", e);
                return false;
            }
        }
        return false;
    }

    public boolean handleMmi(String str, PhoneAccountHandle phoneAccountHandle) {
        ITelecomService telecomService = getTelecomService();
        if (telecomService != null) {
            try {
                return telecomService.handlePinMmiForPhoneAccount(phoneAccountHandle, str, this.mContext.getOpPackageName());
            } catch (RemoteException e) {
                android.util.Log.e(TAG, "Error calling ITelecomService#handlePinMmi", e);
                return false;
            }
        }
        return false;
    }

    public Uri getAdnUriForPhoneAccount(PhoneAccountHandle phoneAccountHandle) {
        ITelecomService telecomService = getTelecomService();
        if (telecomService != null && phoneAccountHandle != null) {
            try {
                return telecomService.getAdnUriForPhoneAccount(phoneAccountHandle, this.mContext.getOpPackageName());
            } catch (RemoteException e) {
                android.util.Log.e(TAG, "Error calling ITelecomService#getAdnUriForPhoneAccount", e);
            }
        }
        return Uri.parse("content://icc/adn");
    }

    public void cancelMissedCallsNotification() {
        ITelecomService telecomService = getTelecomService();
        if (telecomService != null) {
            try {
                telecomService.cancelMissedCallsNotification(this.mContext.getOpPackageName());
            } catch (RemoteException e) {
                android.util.Log.e(TAG, "Error calling ITelecomService#cancelMissedCallsNotification", e);
            }
        }
    }

    public void showInCallScreen(boolean z) {
        ITelecomService telecomService = getTelecomService();
        if (telecomService != null) {
            try {
                telecomService.showInCallScreen(z, this.mContext.getOpPackageName());
            } catch (RemoteException e) {
                android.util.Log.e(TAG, "Error calling ITelecomService#showCallScreen", e);
            }
        }
    }

    public void placeCall(Uri uri, Bundle bundle) {
        ITelecomService telecomService = getTelecomService();
        if (telecomService != null) {
            if (uri == null) {
                android.util.Log.w(TAG, "Cannot place call to empty address.");
            }
            if (bundle == null) {
                try {
                    bundle = new Bundle();
                } catch (RemoteException e) {
                    android.util.Log.e(TAG, "Error calling ITelecomService#placeCall", e);
                    return;
                }
            }
            telecomService.placeCall(uri, bundle, this.mContext.getOpPackageName());
        }
    }

    @SystemApi
    public void enablePhoneAccount(PhoneAccountHandle phoneAccountHandle, boolean z) {
        ITelecomService telecomService = getTelecomService();
        if (telecomService != null) {
            try {
                telecomService.enablePhoneAccount(phoneAccountHandle, z);
            } catch (RemoteException e) {
                android.util.Log.e(TAG, "Error enablePhoneAbbount", e);
            }
        }
    }

    @SystemApi
    public TelecomAnalytics dumpAnalytics() {
        ITelecomService telecomService = getTelecomService();
        if (telecomService != null) {
            try {
                return telecomService.dumpCallAnalytics();
            } catch (RemoteException e) {
                android.util.Log.e(TAG, "Error dumping call analytics", e);
            }
        }
        return null;
    }

    public Intent createManageBlockedNumbersIntent() {
        ITelecomService telecomService = getTelecomService();
        if (telecomService != null) {
            try {
                return telecomService.createManageBlockedNumbersIntent();
            } catch (RemoteException e) {
                android.util.Log.e(TAG, "Error calling ITelecomService#createManageBlockedNumbersIntent", e);
            }
        }
        return null;
    }

    public boolean isIncomingCallPermitted(PhoneAccountHandle phoneAccountHandle) {
        ITelecomService telecomService;
        if (phoneAccountHandle != null && (telecomService = getTelecomService()) != null) {
            try {
                return telecomService.isIncomingCallPermitted(phoneAccountHandle);
            } catch (RemoteException e) {
                android.util.Log.e(TAG, "Error isIncomingCallPermitted", e);
            }
        }
        return false;
    }

    public boolean isOutgoingCallPermitted(PhoneAccountHandle phoneAccountHandle) {
        ITelecomService telecomService = getTelecomService();
        if (telecomService != null) {
            try {
                return telecomService.isOutgoingCallPermitted(phoneAccountHandle);
            } catch (RemoteException e) {
                android.util.Log.e(TAG, "Error isOutgoingCallPermitted", e);
                return false;
            }
        }
        return false;
    }

    public void acceptHandover(Uri uri, int i, PhoneAccountHandle phoneAccountHandle) {
        try {
            if (isServiceConnected()) {
                getTelecomService().acceptHandover(uri, i, phoneAccountHandle);
            }
        } catch (RemoteException e) {
            android.util.Log.e(TAG, "RemoteException acceptHandover: " + e);
        }
    }

    private ITelecomService getTelecomService() {
        if (this.mTelecomServiceOverride != null) {
            return this.mTelecomServiceOverride;
        }
        return ITelecomService.Stub.asInterface(ServiceManager.getService(Context.TELECOM_SERVICE));
    }

    private boolean isServiceConnected() {
        boolean z = getTelecomService() != null;
        if (!z) {
            android.util.Log.w(TAG, "Telecom Service not found.");
        }
        return z;
    }
}
