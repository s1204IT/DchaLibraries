package com.android.server.telecom;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.net.Uri;
import android.os.Handler;
import android.os.Message;
import android.telecom.AudioState;
import android.telecom.Response;
import android.telephony.SubscriptionManager;
import android.widget.Toast;
import com.android.internal.os.SomeArgs;
import com.android.internal.telephony.SmsApplication;
import java.util.ArrayList;
import java.util.List;

public class RespondViaSmsManager extends CallsManagerListenerBase {
    private static final RespondViaSmsManager sInstance = new RespondViaSmsManager();
    private final Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message message) {
            SomeArgs someArgs;
            switch (message.what) {
                case 1:
                    someArgs = (SomeArgs) message.obj;
                    try {
                        Response response = (Response) someArgs.arg1;
                        List list = (List) someArgs.arg2;
                        if (list != null) {
                            response.onResult((Object) null, new List[]{list});
                        } else {
                            response.onError((Object) null, 0, (String) null);
                        }
                        return;
                    } finally {
                    }
                case 2:
                    someArgs = (SomeArgs) message.obj;
                    try {
                        RespondViaSmsManager.this.showMessageSentToast((String) someArgs.arg1, (Context) someArgs.arg2);
                        return;
                    } finally {
                    }
                default:
                    return;
            }
        }
    };

    @Override
    public void onAudioStateChanged(AudioState audioState, AudioState audioState2) {
        super.onAudioStateChanged(audioState, audioState2);
    }

    @Override
    public void onCallAdded(Call call) {
        super.onCallAdded(call);
    }

    @Override
    public void onCallRemoved(Call call) {
        super.onCallRemoved(call);
    }

    @Override
    public void onCallStateChanged(Call call, int i, int i2) {
        super.onCallStateChanged(call, i, i2);
    }

    @Override
    public void onCanAddCallChanged(boolean z) {
        super.onCanAddCallChanged(z);
    }

    @Override
    public void onConnectionServiceChanged(Call call, ConnectionServiceWrapper connectionServiceWrapper, ConnectionServiceWrapper connectionServiceWrapper2) {
        super.onConnectionServiceChanged(call, connectionServiceWrapper, connectionServiceWrapper2);
    }

    @Override
    public void onForegroundCallChanged(Call call, Call call2) {
        super.onForegroundCallChanged(call, call2);
    }

    @Override
    public void onIncomingCallAnswered(Call call) {
        super.onIncomingCallAnswered(call);
    }

    @Override
    public void onIsConferencedChanged(Call call) {
        super.onIsConferencedChanged(call);
    }

    @Override
    public void onIsVoipAudioModeChanged(Call call) {
        super.onIsVoipAudioModeChanged(call);
    }

    @Override
    public void onRingbackRequested(Call call, boolean z) {
        super.onRingbackRequested(call, z);
    }

    @Override
    public void onVideoStateChanged(Call call) {
        super.onVideoStateChanged(call);
    }

    public static RespondViaSmsManager getInstance() {
        return sInstance;
    }

    private RespondViaSmsManager() {
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
                Log.d(RespondViaSmsManager.this, "loadCannedResponses() completed, found responses: %s", arrayList.toString());
                SomeArgs someArgsObtain = SomeArgs.obtain();
                someArgsObtain.arg1 = response;
                someArgsObtain.arg2 = arrayList;
                RespondViaSmsManager.this.mHandler.obtainMessage(1, someArgsObtain).sendToTarget();
            }
        }.start();
    }

    @Override
    public void onIncomingCallRejected(Call call, boolean z, String str) {
        if (z && call.getHandle() != null) {
            rejectCallWithMessage(call.getContext(), call.getHandle().getSchemeSpecificPart(), str, CallsManager.getInstance().getPhoneAccountRegistrar().getSubscriptionIdForPhoneAccount(call.getTargetPhoneAccount()));
        }
    }

    private void showMessageSentToast(String str, Context context) {
        Toast.makeText(context, String.format(context.getResources().getString(R.string.respond_via_sms_confirmation_format), str), 1).show();
    }

    private void rejectCallWithMessage(Context context, String str, String str2, int i) {
        ComponentName defaultRespondViaMessageApplication;
        if (str2 != null && (defaultRespondViaMessageApplication = SmsApplication.getDefaultRespondViaMessageApplication(context, true)) != null) {
            Intent intent = new Intent("android.intent.action.RESPOND_VIA_MESSAGE", Uri.fromParts("smsto", str, null));
            intent.putExtra("android.intent.extra.TEXT", str2);
            if (SubscriptionManager.isValidSubscriptionId(i)) {
                intent.putExtra("subscription", i);
            }
            SomeArgs someArgsObtain = SomeArgs.obtain();
            someArgsObtain.arg1 = str;
            someArgsObtain.arg2 = context;
            this.mHandler.obtainMessage(2, someArgsObtain).sendToTarget();
            intent.setComponent(defaultRespondViaMessageApplication);
            context.startService(intent);
        }
    }
}
