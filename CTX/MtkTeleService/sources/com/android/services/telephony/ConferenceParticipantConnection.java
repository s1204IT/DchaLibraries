package com.android.services.telephony;

import android.net.Uri;
import android.telecom.ConferenceParticipant;
import android.telecom.Connection;
import android.telecom.DisconnectCause;
import android.telephony.PhoneNumberUtils;
import android.telephony.SubscriptionInfo;
import android.text.TextUtils;
import com.android.internal.annotations.VisibleForTesting;
import com.android.internal.telephony.Phone;
import com.mediatek.internal.telephony.imsphone.MtkImsPhone;

public class ConferenceParticipantConnection extends Connection {
    private static final String ANONYMOUS_INVALID_HOST = "anonymous.invalid";
    private final Uri mEndpoint;
    private final com.android.internal.telephony.Connection mParentConnection;
    private final Uri mUserEntity;

    public ConferenceParticipantConnection(com.android.internal.telephony.Connection connection, ConferenceParticipant conferenceParticipant) {
        Uri participantAddress;
        this.mParentConnection = connection;
        int participantPresentation = getParticipantPresentation(conferenceParticipant);
        if (participantPresentation != 1) {
            participantAddress = null;
        } else {
            participantAddress = getParticipantAddress(conferenceParticipant.getHandle(), getCountryIso(connection.getCall().getPhone()));
        }
        setAddress(participantAddress, participantPresentation);
        setCallerDisplayName(conferenceParticipant.getDisplayName(), participantPresentation);
        this.mUserEntity = conferenceParticipant.getHandle();
        this.mEndpoint = conferenceParticipant.getEndpoint();
        setCapabilities();
        setConnectionProperties(294912);
    }

    public void updateState(int i) {
        Log.v(this, "updateState endPoint: %s state: %s", Log.pii(this.mEndpoint), Connection.stateToString(i));
        if (i == getState()) {
        }
        if (i == 0) {
            setInitializing();
            return;
        }
        switch (i) {
            case 2:
                setRinging();
                break;
            case 3:
                setDialing();
                break;
            case 4:
                setActive();
                break;
            case 5:
                setOnHold();
                break;
            case 6:
                setDisconnected(new DisconnectCause(4));
                destroy();
                break;
            default:
                setActive();
                break;
        }
    }

    @Override
    public void onDisconnect() {
        this.mParentConnection.onDisconnectConferenceParticipant(this.mUserEntity);
    }

    public Uri getUserEntity() {
        return this.mUserEntity;
    }

    public Uri getEndpoint() {
        return this.mEndpoint;
    }

    private void setCapabilities() {
        int i;
        MtkImsPhone phone;
        if (this.mParentConnection != null && this.mParentConnection.getCall() != null && (phone = this.mParentConnection.getCall().getPhone()) != null && (phone instanceof MtkImsPhone) && phone.isFeatureSupported(MtkImsPhone.FeatureType.VOLTE_CONF_REMOVE_MEMBER)) {
            i = 8192;
        } else {
            i = 0;
        }
        setConnectionCapabilities(i);
    }

    private int getParticipantPresentation(ConferenceParticipant conferenceParticipant) {
        Uri handle = conferenceParticipant.getHandle();
        if (handle == null) {
            return 2;
        }
        String schemeSpecificPart = handle.getSchemeSpecificPart();
        if (TextUtils.isEmpty(schemeSpecificPart)) {
            return 2;
        }
        String[] strArrSplit = schemeSpecificPart.split("[;]")[0].split("[@]");
        return (strArrSplit.length == 2 && strArrSplit[1].equals(ANONYMOUS_INVALID_HOST)) ? 2 : 1;
    }

    @VisibleForTesting
    public static Uri getParticipantAddress(Uri uri, String str) {
        String numberToE164;
        if (uri == null) {
            return uri;
        }
        String schemeSpecificPart = uri.getSchemeSpecificPart();
        if (TextUtils.isEmpty(schemeSpecificPart)) {
            return uri;
        }
        String[] strArrSplit = schemeSpecificPart.split("[@;:]");
        if (strArrSplit.length == 0) {
            return uri;
        }
        String str2 = strArrSplit[0];
        if (!TextUtils.isEmpty(str)) {
            numberToE164 = PhoneNumberUtils.formatNumberToE164(str2, str);
        } else {
            numberToE164 = null;
        }
        if (numberToE164 != null) {
            str2 = numberToE164;
        }
        return Uri.fromParts("tel", str2, null);
    }

    private String getCountryIso(Phone phone) {
        if (phone == null) {
            return null;
        }
        SubscriptionInfo activeSubscriptionInfo = TelecomAccountRegistry.getInstance(null).getSubscriptionManager().getActiveSubscriptionInfo(phone.getSubId());
        if (activeSubscriptionInfo == null) {
            return null;
        }
        return activeSubscriptionInfo.getCountryIso().toUpperCase();
    }

    public String toString() {
        return "[ConferenceParticipantConnection objId:" + System.identityHashCode(this) + " endPoint:" + Log.pii(this.mEndpoint) + " address:" + Log.pii(getAddress()) + " addressPresentation:" + getAddressPresentation() + " parentConnection:" + Log.pii(this.mParentConnection.getAddress()) + " state:" + Connection.stateToString(getState()) + "]";
    }
}
