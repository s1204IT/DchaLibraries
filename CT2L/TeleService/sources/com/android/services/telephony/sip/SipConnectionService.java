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
    public Connection onCreateOutgoingConnection(PhoneAccountHandle connectionManagerAccount, final ConnectionRequest request) {
        Bundle extras = request.getExtras();
        if (extras != null && extras.getString("android.telecom.extra.GATEWAY_PROVIDER_PACKAGE") != null) {
            return Connection.createFailedConnection(DisconnectCauseUtil.toTelecomDisconnectCause(20, "Cannot make a SIP call with a gateway number."));
        }
        PhoneAccountHandle accountHandle = request.getAccountHandle();
        ComponentName sipComponentName = new ComponentName(this, (Class<?>) SipConnectionService.class);
        if (!Objects.equals(accountHandle.getComponentName(), sipComponentName)) {
            return Connection.createFailedConnection(DisconnectCauseUtil.toTelecomDisconnectCause(43, "Did not match service connection"));
        }
        final SipConnection connection = new SipConnection();
        connection.setAddress(request.getAddress(), 1);
        connection.setInitializing();
        connection.onAddedToCallService();
        boolean attemptCall = true;
        if (!SipUtil.isVoipSupported(this)) {
            CharSequence description = getString(R.string.no_voip);
            connection.setDisconnected(new DisconnectCause(1, null, description, "VoIP unsupported"));
            attemptCall = false;
        }
        if (attemptCall && !isNetworkConnected()) {
            boolean wifiOnly = SipManager.isSipWifiOnly(this);
            CharSequence description2 = getString(wifiOnly ? R.string.no_wifi_available : R.string.no_internet_available);
            connection.setDisconnected(new DisconnectCause(1, null, description2, "Network not connected"));
            attemptCall = false;
        }
        if (attemptCall) {
            String profileUri = accountHandle.getId();
            findProfile(profileUri, new IProfileFinderCallback() {
                @Override
                public void onFound(SipProfile profile) {
                    if (profile != null) {
                        com.android.internal.telephony.Connection chosenConnection = SipConnectionService.this.createConnectionForProfile(profile, request);
                        if (chosenConnection == null) {
                            connection.setDisconnected(DisconnectCauseUtil.toTelecomDisconnectCause(43, "Connection failed."));
                            connection.destroy();
                            return;
                        } else {
                            connection.initialize(chosenConnection);
                            return;
                        }
                    }
                    connection.setDisconnected(DisconnectCauseUtil.toTelecomDisconnectCause(43, "SIP profile not found."));
                    connection.destroy();
                }
            });
            return connection;
        }
        return connection;
    }

    @Override
    public Connection onCreateIncomingConnection(PhoneAccountHandle connectionManagerAccount, ConnectionRequest request) {
        if (request.getExtras() == null) {
            return Connection.createFailedConnection(DisconnectCauseUtil.toTelecomDisconnectCause(36, "No extras on request."));
        }
        Intent sipIntent = (Intent) request.getExtras().getParcelable("com.android.services.telephony.sip.incoming_call_intent");
        if (sipIntent == null) {
            return Connection.createFailedConnection(DisconnectCauseUtil.toTelecomDisconnectCause(36, "No SIP intent."));
        }
        try {
            SipAudioCall sipAudioCall = SipManager.newInstance(this).takeAudioCall(sipIntent, null);
            SipPhone phone = findPhoneForProfile(sipAudioCall.getLocalProfile());
            if (phone == null) {
                phone = createPhoneForProfile(sipAudioCall.getLocalProfile());
            }
            if (phone != null) {
                com.android.internal.telephony.Connection originalConnection = phone.takeIncomingCall(sipAudioCall);
                if (originalConnection != null) {
                    SipConnection sipConnection = new SipConnection();
                    sipConnection.initialize(originalConnection);
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

    private com.android.internal.telephony.Connection createConnectionForProfile(SipProfile profile, ConnectionRequest request) {
        SipPhone phone = findPhoneForProfile(profile);
        if (phone == null) {
            phone = createPhoneForProfile(profile);
        }
        if (phone != null) {
            return startCallWithPhone(phone, request);
        }
        return null;
    }

    private void findProfile(final String profileUri, final IProfileFinderCallback callback) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                SipProfile profileToUse = null;
                List<SipProfile> profileList = SipConnectionService.this.mSipProfileDb.retrieveSipProfileList();
                if (profileList != null) {
                    Iterator<SipProfile> it = profileList.iterator();
                    while (true) {
                        if (!it.hasNext()) {
                            break;
                        }
                        SipProfile profile = it.next();
                        if (Objects.equals(profileUri, profile.getUriString())) {
                            profileToUse = profile;
                            break;
                        }
                    }
                }
                final SipProfile profileFound = profileToUse;
                SipConnectionService.this.mHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        callback.onFound(profileFound);
                    }
                });
            }
        }).start();
    }

    private SipPhone findPhoneForProfile(SipProfile profile) {
        SipPhone phone;
        for (Connection connection : getAllConnections()) {
            if ((connection instanceof SipConnection) && (phone = ((SipConnection) connection).getPhone()) != null && phone.getSipUri().equals(profile.getUriString())) {
                return phone;
            }
        }
        return null;
    }

    private SipPhone createPhoneForProfile(SipProfile profile) {
        return PhoneFactory.makeSipPhone(profile.getUriString());
    }

    private com.android.internal.telephony.Connection startCallWithPhone(SipPhone phone, ConnectionRequest request) {
        String number = request.getAddress().getSchemeSpecificPart();
        try {
            return phone.dial(number, request.getVideoState());
        } catch (CallStateException e) {
            log("startCallWithPhone, exception: " + e);
            return null;
        }
    }

    private boolean isNetworkConnected() {
        NetworkInfo ni;
        ConnectivityManager cm = (ConnectivityManager) getSystemService("connectivity");
        if (cm == null || (ni = cm.getActiveNetworkInfo()) == null || !ni.isConnected()) {
            return false;
        }
        return ni.getType() == 1 || !SipManager.isSipWifiOnly(this);
    }

    private static void log(String msg) {
        Log.d("SIP", "[SipConnectionService] " + msg);
    }
}
