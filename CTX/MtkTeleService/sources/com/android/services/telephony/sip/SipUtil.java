package com.android.services.telephony.sip;

import android.R;
import android.app.PendingIntent;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.graphics.drawable.Icon;
import android.net.Uri;
import android.net.sip.SipManager;
import android.net.sip.SipProfile;
import android.os.Bundle;
import android.telecom.PhoneAccount;
import android.telecom.PhoneAccountHandle;
import android.telecom.TelecomManager;
import android.text.TextUtils;
import android.util.Log;
import com.android.phone.PhoneGlobals;
import com.android.server.sip.SipService;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class SipUtil {
    public static boolean isVoipSupported(Context context) {
        return SipManager.isVoipSupported(context) && context.getResources().getBoolean(R.^attr-private.colorAccentTertiaryVariant) && context.getResources().getBoolean(R.^attr-private.popupPromptView);
    }

    static PendingIntent createIncomingCallPendingIntent(Context context, String str) {
        Intent intent = new Intent(context, (Class<?>) SipIncomingCallReceiver.class);
        intent.setAction("com.android.phone.SIP_INCOMING_CALL");
        intent.putExtra("com.android.services.telephony.sip.phone_account", createAccountHandle(context, str));
        return PendingIntent.getBroadcast(context, 0, intent, 134217728);
    }

    public static boolean isPhoneIdle(Context context) {
        if (((TelecomManager) context.getSystemService("telecom")) != null) {
            return !r1.isInCall();
        }
        return true;
    }

    static PhoneAccountHandle createAccountHandle(Context context, String str) {
        return new PhoneAccountHandle(new ComponentName(context, (Class<?>) SipConnectionService.class), str);
    }

    static String getSipProfileNameFromPhoneAccount(PhoneAccountHandle phoneAccountHandle) {
        if (phoneAccountHandle == null) {
            return null;
        }
        String id = phoneAccountHandle.getId();
        if (TextUtils.isEmpty(id)) {
            return null;
        }
        return id;
    }

    static PhoneAccount createPhoneAccount(Context context, SipProfile sipProfile) {
        String str = sipProfile.getUserName() + "@" + sipProfile.getSipDomain();
        Uri uri = Uri.parse(sipProfile.getUriString());
        PhoneAccountHandle phoneAccountHandleCreateAccountHandle = createAccountHandle(context, sipProfile.getProfileName());
        ArrayList arrayList = new ArrayList();
        arrayList.add("sip");
        if (useSipForPstnCalls(context)) {
            arrayList.add("tel");
        }
        Bundle bundle = new Bundle();
        bundle.putBoolean("android.telecom.extra.ALWAYS_USE_VOIP_AUDIO_MODE", true);
        return PhoneAccount.builder(phoneAccountHandleCreateAccountHandle, sipProfile.getDisplayName()).setCapabilities(34).setAddress(uri).setShortDescription(str).setIcon(Icon.createWithResource(context.getResources(), com.android.phone.R.drawable.ic_dialer_sip_black_24dp)).setExtras(bundle).setSupportedUriSchemes(arrayList).build();
    }

    private static void possiblyMigrateSipDb(Context context) {
        SipProfileDb sipProfileDb = new SipProfileDb(context);
        sipProfileDb.accessDEStorageForMigration();
        List<SipProfile> listRetrieveSipProfileList = sipProfileDb.retrieveSipProfileList();
        if (listRetrieveSipProfileList.size() != 0) {
            Log.i("SIP", "Migrating SIP Profiles over!");
            SipProfileDb sipProfileDb2 = new SipProfileDb(context);
            for (SipProfile sipProfile : listRetrieveSipProfileList) {
                if (sipProfileDb2.retrieveSipProfileFromName(sipProfile.getProfileName()) == null) {
                    try {
                        sipProfileDb2.saveProfile(sipProfile);
                    } catch (IOException e) {
                        Log.w("SIP", "Error Migrating file to CE: " + sipProfile.getProfileName(), e);
                    }
                }
                Log.i("SIP", "(Migration) Deleting SIP profile: " + sipProfile.getProfileName());
                try {
                    sipProfileDb.deleteProfile(sipProfile);
                } catch (IOException e2) {
                    Log.w("SIP", "Error Deleting file: " + sipProfile.getProfileName(), e2);
                }
            }
        }
        sipProfileDb.cleanupUponMigration();
    }

    public static void startSipService() {
        PhoneGlobals phoneGlobals = PhoneGlobals.getInstance();
        possiblyMigrateSipDb(phoneGlobals);
        SipService.start(phoneGlobals);
    }

    private static boolean useSipForPstnCalls(Context context) {
        return new SipPreferences(context).getSipCallOption().equals("SIP_ALWAYS");
    }

    public static void useSipToReceiveIncomingCalls(Context context, boolean z) {
        SipProfileDb sipProfileDb = new SipProfileDb(context);
        Iterator<SipProfile> it = sipProfileDb.retrieveSipProfileList().iterator();
        while (it.hasNext()) {
            updateAutoRegistrationFlag(it.next(), sipProfileDb, z);
        }
    }

    private static void updateAutoRegistrationFlag(SipProfile sipProfile, SipProfileDb sipProfileDb, boolean z) {
        SipProfile sipProfileBuild = new SipProfile.Builder(sipProfile).setAutoRegistration(z).build();
        try {
            sipProfileDb.deleteProfile(sipProfile);
            sipProfileDb.saveProfile(sipProfileBuild);
        } catch (Exception e) {
            Log.d("SIP", "updateAutoRegistrationFlag, exception: " + e);
        }
    }
}
