package com.android.server.telecom;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.UserHandle;

public final class TelecomGlobals {
    private CallsManager mCallsManager;
    private Context mContext;
    private MissedCallNotifier mMissedCallNotifier;
    private PhoneAccountRegistrar mPhoneAccountRegistrar;
    private final BroadcastReceiver mUserSwitchedReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            TelecomGlobals.this.mPhoneAccountRegistrar.setCurrentUserHandle(new UserHandle(intent.getIntExtra("android.intent.extra.user_handle", 0)));
        }
    };
    private static final String TAG = TelecomGlobals.class.getSimpleName();
    private static final IntentFilter USER_SWITCHED_FILTER = new IntentFilter("android.intent.action.USER_SWITCHED");
    private static final TelecomGlobals INSTANCE = new TelecomGlobals();

    static TelecomGlobals getInstance() {
        return INSTANCE;
    }

    void initialize(Context context) {
        if (this.mContext != null) {
            Log.e(TAG, (Throwable) new Exception(), "Attempting to intialize TelecomGlobals a second time.", new Object[0]);
            return;
        }
        Log.i(TAG, "TelecomGlobals initializing", new Object[0]);
        this.mContext = context.getApplicationContext();
        this.mMissedCallNotifier = new MissedCallNotifier(this.mContext);
        this.mPhoneAccountRegistrar = new PhoneAccountRegistrar(this.mContext);
        this.mCallsManager = new CallsManager(this.mContext, this.mMissedCallNotifier, this.mPhoneAccountRegistrar);
        CallsManager.initialize(this.mCallsManager);
        Log.i(this, "CallsManager initialized", new Object[0]);
        BluetoothPhoneService.start(this.mContext);
        this.mContext.registerReceiver(this.mUserSwitchedReceiver, USER_SWITCHED_FILTER);
    }

    MissedCallNotifier getMissedCallNotifier() {
        return this.mMissedCallNotifier;
    }

    PhoneAccountRegistrar getPhoneAccountRegistrar() {
        return this.mPhoneAccountRegistrar;
    }

    CallsManager getCallsManager() {
        return this.mCallsManager;
    }
}
