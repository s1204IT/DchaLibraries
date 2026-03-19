package com.android.server.telecom;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.telecom.Log;
import android.telecom.Response;
import android.telephony.PhoneNumberUtils;
import android.telephony.SmsManager;
import android.telephony.SubscriptionManager;
import android.text.SpannableString;
import android.text.TextUtils;
import android.widget.Toast;
import com.android.internal.os.SomeArgs;
import com.android.server.telecom.TelecomSystem;
import java.util.ArrayList;
import java.util.List;

public class RespondViaSmsManager extends CallsManagerListenerBase {
    private final CallsManager mCallsManager;
    private final Handler mHandler = new Handler(Looper.getMainLooper()) {
        @Override
        public void handleMessage(Message message) {
            if (message.what == 2) {
                SomeArgs someArgs = (SomeArgs) message.obj;
                try {
                    RespondViaSmsManager.this.showMessageSentToast((String) someArgs.arg1, (Context) someArgs.arg2);
                } finally {
                    someArgs.recycle();
                }
            }
        }
    };
    private final TelecomSystem.SyncRoot mLock;

    public RespondViaSmsManager(CallsManager callsManager, TelecomSystem.SyncRoot syncRoot) {
        this.mCallsManager = callsManager;
        this.mLock = syncRoot;
    }

    public void loadCannedTextMessages(final Response<Void, List<String>> response, final Context context) {
        new Thread() {
            @Override
            public void run() {
                Log.d(RespondViaSmsManager.this, "loadCannedResponses() starting", new Object[0]);
                QuickResponseUtils.maybeMigrateLegacyQuickResponses(context);
                SharedPreferences sharedPreferences = context.getSharedPreferences("respond_via_sms_prefs", 4);
                Resources resources = context.getResources();
                ArrayList arrayList = new ArrayList(4);
                arrayList.add(0, sharedPreferences.getString("canned_response_pref_1", resources.getString(R.string.respond_via_sms_canned_response_1)));
                arrayList.add(1, sharedPreferences.getString("canned_response_pref_2", resources.getString(R.string.respond_via_sms_canned_response_2)));
                arrayList.add(2, sharedPreferences.getString("canned_response_pref_3", resources.getString(R.string.respond_via_sms_canned_response_3)));
                arrayList.add(3, sharedPreferences.getString("canned_response_pref_4", resources.getString(R.string.respond_via_sms_canned_response_4)));
                Log.d(RespondViaSmsManager.this, "loadCannedResponses() completed, found responses: %s", new Object[]{arrayList.toString()});
                synchronized (RespondViaSmsManager.this.mLock) {
                    response.onResult((Object) null, new List[]{arrayList});
                }
            }
        }.start();
    }

    @Override
    public void onIncomingCallRejected(Call call, boolean z, String str) {
        if (z && call.getHandle() != null && !call.can(4194304)) {
            rejectCallWithMessage(call.getContext(), call.getHandle().getSchemeSpecificPart(), str, this.mCallsManager.getPhoneAccountRegistrar().getSubscriptionIdForPhoneAccount(call.getTargetPhoneAccount()), call.getName());
        }
    }

    private void showMessageSentToast(String str, Context context) {
        String str2 = String.format(context.getResources().getString(R.string.respond_via_sms_confirmation_format), str);
        int iIndexOf = str2.indexOf(str);
        int length = str.length() + iIndexOf;
        SpannableString spannableString = new SpannableString(str2);
        PhoneNumberUtils.addTtsSpan(spannableString, iIndexOf, length);
        Toast.makeText(context, spannableString, 1).show();
    }

    private void rejectCallWithMessage(Context context, String str, String str2, int i, String str3) {
        if (TextUtils.isEmpty(str2)) {
            Log.w(this, "Couldn't send SMS message: empty text message. ", new Object[0]);
            return;
        }
        if (!SubscriptionManager.isValidSubscriptionId(i)) {
            Log.w(this, "Couldn't send SMS message: Invalid SubId: " + i, new Object[0]);
            return;
        }
        try {
            SmsManager.getSmsManagerForSubscriptionId(i).sendTextMessage(str, null, str2, null, null);
            SomeArgs someArgsObtain = SomeArgs.obtain();
            if (!TextUtils.isEmpty(str3)) {
                str = str3;
            }
            someArgsObtain.arg1 = str;
            someArgsObtain.arg2 = context;
            this.mHandler.obtainMessage(2, someArgsObtain).sendToTarget();
        } catch (IllegalArgumentException e) {
            Log.w(this, "Couldn't send SMS message: " + e.getMessage(), new Object[0]);
        }
    }
}
