package com.android.server.telecom.ui;

import android.R;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.telecom.Log;
import android.telecom.PhoneAccountHandle;
import android.telecom.VideoProfile;
import android.text.SpannableString;
import android.text.TextUtils;
import android.text.style.ForegroundColorSpan;
import android.util.ArraySet;
import com.android.internal.annotations.VisibleForTesting;
import com.android.server.telecom.Call;
import com.android.server.telecom.CallsManagerListenerBase;
import com.android.server.telecom.components.TelecomBroadcastReceiver;
import java.util.Optional;
import java.util.Set;
import java.util.function.Predicate;

public class IncomingCallNotifier extends CallsManagerListenerBase {

    @VisibleForTesting
    public static final int NOTIFICATION_INCOMING_CALL = 1;

    @VisibleForTesting
    public static final String NOTIFICATION_TAG = IncomingCallNotifier.class.getSimpleName();
    public final Call.ListenerBase mCallListener = new Call.ListenerBase() {
        @Override
        public void onCallerInfoChanged(Call call) {
            if (IncomingCallNotifier.this.mIncomingCall == call) {
                IncomingCallNotifier.this.showIncomingCallNotification(IncomingCallNotifier.this.mIncomingCall);
            }
        }
    };
    private final Set<Call> mCalls = new ArraySet();
    private CallsManagerProxy mCallsManagerProxy;
    private final Context mContext;
    private Call mIncomingCall;
    private final NotificationManager mNotificationManager;

    public interface CallsManagerProxy {
        Call getActiveCall();

        int getNumCallsForOtherPhoneAccount(PhoneAccountHandle phoneAccountHandle);

        boolean hasCallsForOtherPhoneAccount(PhoneAccountHandle phoneAccountHandle);
    }

    public IncomingCallNotifier(Context context) {
        this.mContext = context;
        this.mNotificationManager = (NotificationManager) this.mContext.getSystemService("notification");
    }

    public void setCallsManagerProxy(CallsManagerProxy callsManagerProxy) {
        this.mCallsManagerProxy = callsManagerProxy;
    }

    public Call getIncomingCall() {
        return this.mIncomingCall;
    }

    @Override
    public void onCallAdded(Call call) {
        if (!this.mCalls.contains(call)) {
            this.mCalls.add(call);
        }
        updateIncomingCall();
    }

    @Override
    public void onCallRemoved(Call call) {
        if (this.mCalls.contains(call)) {
            this.mCalls.remove(call);
        }
        updateIncomingCall();
    }

    @Override
    public void onCallStateChanged(Call call, int i, int i2) {
        updateIncomingCall();
    }

    private void updateIncomingCall() {
        Optional<Call> optionalFindFirst = this.mCalls.stream().filter(new Predicate() {
            @Override
            public final boolean test(Object obj) {
                return IncomingCallNotifier.lambda$updateIncomingCall$0((Call) obj);
            }
        }).findFirst();
        Call call = null;
        Call callOrElse = optionalFindFirst.orElse(null);
        if (callOrElse == null || this.mCallsManagerProxy == null || this.mCallsManagerProxy.hasCallsForOtherPhoneAccount(optionalFindFirst.get().getTargetPhoneAccount())) {
            call = callOrElse;
        }
        Log.i(this, "updateIncomingCall: foundIncomingcall = %s", new Object[]{call});
        boolean z = this.mIncomingCall != null;
        boolean z2 = call != null;
        if (call != this.mIncomingCall) {
            Call call2 = this.mIncomingCall;
            this.mIncomingCall = call;
            if (z2 && !z) {
                this.mIncomingCall.addListener(this.mCallListener);
                showIncomingCallNotification(this.mIncomingCall);
            } else if (z && !z2) {
                call2.removeListener(this.mCallListener);
                hideIncomingCallNotification();
            }
        }
    }

    static boolean lambda$updateIncomingCall$0(Call call) {
        return call.isSelfManaged() && call.isIncoming() && call.getState() == 4 && call.getHandoverState() == 1;
    }

    private void showIncomingCallNotification(Call call) {
        Log.i(this, "showIncomingCallNotification showCall = %s", new Object[]{call});
        this.mNotificationManager.notify(NOTIFICATION_TAG, 1, getNotificationBuilder(call, this.mCallsManagerProxy.getActiveCall()).build());
    }

    private void hideIncomingCallNotification() {
        Log.i(this, "hideIncomingCallNotification", new Object[0]);
        this.mNotificationManager.cancel(NOTIFICATION_TAG, 1);
    }

    private String getNotificationName(Call call) {
        String name = "";
        if (call.getCallerDisplayNamePresentation() == 1) {
            name = call.getCallerDisplayName();
        }
        if (TextUtils.isEmpty(name)) {
            name = call.getName();
        }
        if (TextUtils.isEmpty(name)) {
            return call.getPhoneNumber();
        }
        return name;
    }

    private Notification.Builder getNotificationBuilder(Call call, Call call2) {
        boolean zIsVideo;
        int numCallsForOtherPhoneAccount;
        String string;
        String string2;
        Bundle bundle = new Bundle();
        bundle.putString("android.substName", this.mContext.getString(R.string.PERSOSUBSTATE_RUIM_HRPD_SUCCESS));
        Intent intent = new Intent("com.android.server.telecom.ACTION_ANSWER_FROM_NOTIFICATION", null, this.mContext, TelecomBroadcastReceiver.class);
        Intent intent2 = new Intent("com.android.server.telecom.ACTION_REJECT_FROM_NOTIFICATION", null, this.mContext, TelecomBroadcastReceiver.class);
        String notificationName = getNotificationName(call);
        CharSequence targetPhoneAccountLabel = call.getTargetPhoneAccountLabel();
        boolean zIsVideo2 = VideoProfile.isVideo(call.getVideoState());
        if (call2 != null) {
            zIsVideo = VideoProfile.isVideo(call2.getVideoState());
        } else {
            zIsVideo = false;
        }
        if (call2 != null) {
            numCallsForOtherPhoneAccount = this.mCallsManagerProxy.getNumCallsForOtherPhoneAccount(call.getTargetPhoneAccount());
        } else {
            numCallsForOtherPhoneAccount = 1;
        }
        if (zIsVideo2) {
            string = this.mContext.getString(com.android.server.telecom.R.string.notification_incoming_video_call, targetPhoneAccountLabel, notificationName);
        } else {
            string = this.mContext.getString(com.android.server.telecom.R.string.notification_incoming_call, targetPhoneAccountLabel, notificationName);
        }
        if (call2 != null && call2.isSelfManaged()) {
            CharSequence targetPhoneAccountLabel2 = call2.getTargetPhoneAccountLabel();
            if (numCallsForOtherPhoneAccount > 1) {
                string2 = this.mContext.getString(com.android.server.telecom.R.string.answering_ends_other_calls, targetPhoneAccountLabel2);
            } else if (zIsVideo) {
                string2 = this.mContext.getString(com.android.server.telecom.R.string.answering_ends_other_video_call, targetPhoneAccountLabel2);
            } else {
                string2 = this.mContext.getString(com.android.server.telecom.R.string.answering_ends_other_call, targetPhoneAccountLabel2);
            }
        } else if (numCallsForOtherPhoneAccount > 1) {
            string2 = this.mContext.getString(com.android.server.telecom.R.string.answering_ends_other_managed_calls);
        } else if (zIsVideo) {
            string2 = this.mContext.getString(com.android.server.telecom.R.string.answering_ends_other_managed_video_call);
        } else {
            string2 = this.mContext.getString(com.android.server.telecom.R.string.answering_ends_other_managed_call);
        }
        Notification.Builder builder = new Notification.Builder(this.mContext);
        builder.setOngoing(true);
        builder.setExtras(bundle);
        builder.setPriority(1);
        builder.setCategory("call");
        builder.setContentTitle(string);
        builder.setContentText(string2);
        builder.setSmallIcon(com.android.server.telecom.R.drawable.ic_phone);
        builder.setChannelId("TelecomIncomingCalls");
        builder.setVibrate(new long[0]);
        builder.setColor(this.mContext.getResources().getColor(com.android.server.telecom.R.color.theme_color));
        builder.addAction(com.android.server.telecom.R.anim.on_going_call, getActionText(com.android.server.telecom.R.string.answer_incoming_call, com.android.server.telecom.R.color.notification_action_answer), PendingIntent.getBroadcast(this.mContext, 0, intent, 268435456));
        builder.addAction(com.android.server.telecom.R.drawable.ic_close_dk, getActionText(com.android.server.telecom.R.string.decline_incoming_call, com.android.server.telecom.R.color.notification_action_decline), PendingIntent.getBroadcast(this.mContext, 0, intent2, 268435456));
        return builder;
    }

    private CharSequence getActionText(int i, int i2) {
        CharSequence text = this.mContext.getText(i);
        if (text == null) {
            return "";
        }
        SpannableString spannableString = new SpannableString(text);
        spannableString.setSpan(new ForegroundColorSpan(this.mContext.getColor(i2)), 0, spannableString.length(), 0);
        return spannableString;
    }
}
