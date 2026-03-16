package com.android.services.telephony.sip;

import android.R;
import android.app.PendingIntent;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.net.sip.SipManager;
import android.net.sip.SipProfile;
import android.telecom.PhoneAccount;
import android.telecom.PhoneAccountHandle;
import android.telecom.TelecomManager;
import android.util.Log;
import java.util.ArrayList;
import java.util.List;

public class SipUtil {
    public static boolean isVoipSupported(Context context) {
        return SipManager.isVoipSupported(context) && context.getResources().getBoolean(R.^attr-private.floatingToolbarDividerColor) && context.getResources().getBoolean(R.^attr-private.externalRouteEnabledDrawable);
    }

    static PendingIntent createIncomingCallPendingIntent(Context context, String sipUri) {
        Intent intent = new Intent(context, (Class<?>) SipBroadcastReceiver.class);
        intent.setAction("com.android.phone.SIP_INCOMING_CALL");
        intent.putExtra("com.android.services.telephony.sip.phone_account", createAccountHandle(context, sipUri));
        return PendingIntent.getBroadcast(context, 0, intent, 134217728);
    }

    public static boolean isPhoneIdle(Context context) {
        TelecomManager manager = (TelecomManager) context.getSystemService("telecom");
        return manager == null || !manager.isInCall();
    }

    static PhoneAccountHandle createAccountHandle(Context context, String sipUri) {
        return new PhoneAccountHandle(new ComponentName(context, (Class<?>) SipConnectionService.class), sipUri);
    }

    static PhoneAccount createPhoneAccount(Context context, SipProfile profile) {
        PhoneAccountHandle accountHandle = createAccountHandle(context, profile.getUriString());
        ArrayList<String> supportedUriSchemes = new ArrayList<>();
        supportedUriSchemes.add("sip");
        if (useSipForPstnCalls(context)) {
            supportedUriSchemes.add("tel");
        }
        PhoneAccount.Builder builder = PhoneAccount.builder(accountHandle, profile.getDisplayName()).setCapabilities(34).setAddress(Uri.parse(profile.getUriString())).setShortDescription(profile.getDisplayName()).setIcon(context, com.android.phone.R.drawable.ic_dialer_sip_black_24dp).setSupportedUriSchemes(supportedUriSchemes);
        return builder.build();
    }

    private static boolean useSipForPstnCalls(Context context) {
        SipSharedPreferences sipSharedPreferences = new SipSharedPreferences(context);
        return sipSharedPreferences.getSipCallOption().equals("SIP_ALWAYS");
    }

    public static void useSipToReceiveIncomingCalls(Context context, boolean isEnabled) {
        SipProfileDb profileDb = new SipProfileDb(context);
        List<SipProfile> sipProfileList = profileDb.retrieveSipProfileList();
        for (SipProfile p : sipProfileList) {
            updateAutoRegistrationFlag(p, profileDb, isEnabled);
        }
    }

    private static void updateAutoRegistrationFlag(SipProfile p, SipProfileDb db, boolean isEnabled) {
        SipProfile newProfile = new SipProfile.Builder(p).setAutoRegistration(isEnabled).build();
        try {
            db.deleteProfile(p);
            db.saveProfile(newProfile);
        } catch (Exception e) {
            Log.d("SIP", "updateAutoRegistrationFlag, exception: " + e);
        }
    }
}
