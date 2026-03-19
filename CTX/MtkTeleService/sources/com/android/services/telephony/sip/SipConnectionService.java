package com.android.services.telephony.sip;

import android.content.ComponentName;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.sip.SipAudioCall;
import android.net.sip.SipException;
import android.net.sip.SipManager;
import android.net.sip.SipProfile;
import android.os.Bundle;
import android.os.Handler;
import android.telecom.Connection;
import android.telecom.ConnectionRequest;
import android.telecom.ConnectionService;
import android.telecom.DisconnectCause;
import android.telecom.PhoneAccountHandle;
import android.util.Log;
import com.android.internal.telephony.CallStateException;
import com.android.internal.telephony.PhoneFactory;
import com.android.internal.telephony.PhoneInternalInterface;
import com.android.internal.telephony.sip.SipPhone;
import com.android.phone.R;
import com.android.services.telephony.DisconnectCauseUtil;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;

public final class SipConnectionService extends ConnectionService {
    private Handler mHandler;
    private SipProfileDb mSipProfileDb;

    private interface IProfileFinderCallback {
        void onFound(SipProfile sipProfile);
    }

    @Override
    public void onCreate() {
        this.mSipProfileDb = new SipProfileDb(this);
        this.mHandler = new Handler();
        super.onCreate();
    }

    @Override
    public Connection onCreateOutgoingConnection(PhoneAccountHandle phoneAccountHandle, final ConnectionRequest connectionRequest) {
        boolean z;
        Bundle extras = connectionRequest.getExtras();
        if (extras != null && extras.getString("android.telecom.extra.GATEWAY_PROVIDER_PACKAGE") != null) {
            return Connection.createFailedConnection(DisconnectCauseUtil.toTelecomDisconnectCause(20, "Cannot make a SIP call with a gateway number."));
        }
        PhoneAccountHandle accountHandle = connectionRequest.getAccountHandle();
        if (!Objects.equals(accountHandle.getComponentName(), new ComponentName(this, (Class<?>) SipConnectionService.class))) {
            return Connection.createFailedConnection(DisconnectCauseUtil.toTelecomDisconnectCause(43, "Did not match service connection"));
        }
        final SipConnection sipConnection = new SipConnection();
        sipConnection.setAddress(connectionRequest.getAddress(), 1);
        sipConnection.setInitializing();
        sipConnection.onAddedToCallService();
        if (SipUtil.isVoipSupported(this)) {
            z = true;
        } else {
            sipConnection.setDisconnected(new DisconnectCause(1, null, getString(R.string.no_voip), "VoIP unsupported"));
            z = false;
        }
        if (z && !isNetworkConnected()) {
            sipConnection.setDisconnected(new DisconnectCause(1, null, getString(SipManager.isSipWifiOnly(this) ? R.string.no_wifi_available : R.string.no_internet_available), "Network not connected"));
            z = false;
        }
        if (z) {
            findProfile(accountHandle.getId(), new IProfileFinderCallback() {
                @Override
                public void onFound(SipProfile sipProfile) {
                    if (sipProfile != null) {
                        com.android.internal.telephony.Connection connectionCreateConnectionForProfile = SipConnectionService.this.createConnectionForProfile(sipProfile, connectionRequest);
                        if (connectionCreateConnectionForProfile == null) {
                            sipConnection.setDisconnected(DisconnectCauseUtil.toTelecomDisconnectCause(43, "Connection failed."));
                            sipConnection.destroy();
                            return;
                        } else {
                            sipConnection.initialize(connectionCreateConnectionForProfile);
                            return;
                        }
                    }
                    sipConnection.setDisconnected(DisconnectCauseUtil.toTelecomDisconnectCause(43, "SIP profile not found."));
                    sipConnection.destroy();
                }
            });
        }
        return sipConnection;
    }

    @Override
    public Connection onCreateIncomingConnection(PhoneAccountHandle phoneAccountHandle, ConnectionRequest connectionRequest) {
        if (connectionRequest.getExtras() == null) {
            return Connection.createFailedConnection(DisconnectCauseUtil.toTelecomDisconnectCause(36, "No extras on request."));
        }
        Intent intent = (Intent) connectionRequest.getExtras().getParcelable("com.android.services.telephony.sip.incoming_call_intent");
        if (intent == null) {
            return Connection.createFailedConnection(DisconnectCauseUtil.toTelecomDisconnectCause(36, "No SIP intent."));
        }
        try {
            SipAudioCall sipAudioCallTakeAudioCall = SipManager.newInstance(this).takeAudioCall(intent, null);
            SipPhone sipPhoneFindPhoneForProfile = findPhoneForProfile(sipAudioCallTakeAudioCall.getLocalProfile());
            if (sipPhoneFindPhoneForProfile == null) {
                sipPhoneFindPhoneForProfile = createPhoneForProfile(sipAudioCallTakeAudioCall.getLocalProfile());
            }
            if (sipPhoneFindPhoneForProfile != null) {
                com.android.internal.telephony.Connection connectionTakeIncomingCall = sipPhoneFindPhoneForProfile.takeIncomingCall(sipAudioCallTakeAudioCall);
                if (connectionTakeIncomingCall != null) {
                    SipConnection sipConnection = new SipConnection();
                    sipConnection.initialize(connectionTakeIncomingCall);
                    sipConnection.onAddedToCallService();
                    return sipConnection;
                }
                return Connection.createCanceledConnection();
            }
            return Connection.createFailedConnection(DisconnectCauseUtil.toTelecomDisconnectCause(36));
        } catch (SipException e) {
            log("onCreateIncomingConnection, takeAudioCall exception: " + e);
            return Connection.createCanceledConnection();
        }
    }

    private com.android.internal.telephony.Connection createConnectionForProfile(SipProfile sipProfile, ConnectionRequest connectionRequest) {
        SipPhone sipPhoneFindPhoneForProfile = findPhoneForProfile(sipProfile);
        if (sipPhoneFindPhoneForProfile == null) {
            sipPhoneFindPhoneForProfile = createPhoneForProfile(sipProfile);
        }
        if (sipPhoneFindPhoneForProfile != null) {
            return startCallWithPhone(sipPhoneFindPhoneForProfile, connectionRequest);
        }
        return null;
    }

    private void findProfile(final String str, final IProfileFinderCallback iProfileFinderCallback) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                final SipProfile next;
                List<SipProfile> listRetrieveSipProfileList = SipConnectionService.this.mSipProfileDb.retrieveSipProfileList();
                if (listRetrieveSipProfileList != null) {
                    Iterator<SipProfile> it = listRetrieveSipProfileList.iterator();
                    while (it.hasNext()) {
                        next = it.next();
                        if (Objects.equals(str, next.getProfileName())) {
                            break;
                        }
                    }
                    next = null;
                } else {
                    next = null;
                }
                SipConnectionService.this.mHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        iProfileFinderCallback.onFound(next);
                    }
                });
            }
        }).start();
    }

    private SipPhone findPhoneForProfile(SipProfile sipProfile) {
        SipPhone phone;
        for (Connection connection : getAllConnections()) {
            if ((connection instanceof SipConnection) && (phone = ((SipConnection) connection).getPhone()) != null && phone.getSipUri().equals(sipProfile.getUriString())) {
                return phone;
            }
        }
        return null;
    }

    private SipPhone createPhoneForProfile(SipProfile sipProfile) {
        return PhoneFactory.makeSipPhone(sipProfile.getUriString());
    }

    private com.android.internal.telephony.Connection startCallWithPhone(SipPhone sipPhone, ConnectionRequest connectionRequest) {
        try {
            return sipPhone.dial(connectionRequest.getAddress().getSchemeSpecificPart(), new PhoneInternalInterface.DialArgs.Builder().setVideoState(connectionRequest.getVideoState()).build());
        } catch (CallStateException e) {
            log("startCallWithPhone, exception: " + e);
            return null;
        }
    }

    private boolean isNetworkConnected() {
        NetworkInfo activeNetworkInfo;
        ConnectivityManager connectivityManager = (ConnectivityManager) getSystemService("connectivity");
        if (connectivityManager == null || (activeNetworkInfo = connectivityManager.getActiveNetworkInfo()) == null || !activeNetworkInfo.isConnected()) {
            return false;
        }
        return activeNetworkInfo.getType() == 1 || !SipManager.isSipWifiOnly(this);
    }

    private static void log(String str) {
        Log.d("SIP", "[SipConnectionService] " + str);
    }
}
