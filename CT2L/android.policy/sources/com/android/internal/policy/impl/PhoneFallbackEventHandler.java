package com.android.internal.policy.impl;

import android.app.KeyguardManager;
import android.app.SearchManager;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.content.res.Configuration;
import android.media.AudioManager;
import android.media.session.MediaSessionLegacyHelper;
import android.net.Uri;
import android.os.UserHandle;
import android.provider.Settings;
import android.telephony.TelephonyManager;
import android.util.Log;
import android.view.FallbackEventHandler;
import android.view.KeyEvent;
import android.view.View;

public class PhoneFallbackEventHandler implements FallbackEventHandler {
    private static final boolean DEBUG = false;
    private static String TAG = "PhoneFallbackEventHandler";
    AudioManager mAudioManager;
    Context mContext;
    KeyguardManager mKeyguardManager;
    SearchManager mSearchManager;
    TelephonyManager mTelephonyManager;
    View mView;

    public PhoneFallbackEventHandler(Context context) {
        this.mContext = context;
    }

    public void setView(View v) {
        this.mView = v;
    }

    public void preDispatchKeyEvent(KeyEvent event) {
        getAudioManager().preDispatchKeyEvent(event, Integer.MIN_VALUE);
    }

    public boolean dispatchKeyEvent(KeyEvent event) {
        int action = event.getAction();
        int keyCode = event.getKeyCode();
        return action == 0 ? onKeyDown(keyCode, event) : onKeyUp(keyCode, event);
    }

    boolean onKeyDown(int keyCode, KeyEvent event) {
        KeyEvent.DispatcherState dispatcher = this.mView.getKeyDispatcherState();
        switch (keyCode) {
            case 5:
                if (getKeyguardManager().inKeyguardRestrictedInputMode() || dispatcher == null) {
                    return false;
                }
                if (event.getRepeatCount() == 0) {
                    dispatcher.startTracking(event, this);
                } else {
                    if (event.isLongPress() && dispatcher.isTracking(event)) {
                        dispatcher.performedLongPress(event);
                        if (isUserSetupComplete()) {
                            this.mView.performHapticFeedback(0);
                            Intent intent = new Intent("android.intent.action.VOICE_COMMAND");
                            intent.setFlags(268435456);
                            try {
                                sendCloseSystemWindows();
                                this.mContext.startActivity(intent);
                            } catch (ActivityNotFoundException e) {
                                startCallActivity();
                            }
                        } else {
                            Log.i(TAG, "Not starting call activity because user setup is in progress.");
                        }
                    }
                    break;
                }
                return true;
            case 24:
            case 25:
            case 164:
                MediaSessionLegacyHelper.getHelper(this.mContext).sendVolumeKeyEvent(event, false);
                return true;
            case 27:
                if (getKeyguardManager().inKeyguardRestrictedInputMode() || dispatcher == null) {
                    return false;
                }
                if (event.getRepeatCount() == 0) {
                    dispatcher.startTracking(event, this);
                } else if (event.isLongPress() && dispatcher.isTracking(event)) {
                    dispatcher.performedLongPress(event);
                    if (isUserSetupComplete()) {
                        this.mView.performHapticFeedback(0);
                        sendCloseSystemWindows();
                        Intent intent2 = new Intent("android.intent.action.CAMERA_BUTTON", (Uri) null);
                        intent2.putExtra("android.intent.extra.KEY_EVENT", event);
                        this.mContext.sendOrderedBroadcastAsUser(intent2, UserHandle.CURRENT_OR_SELF, null, null, null, 0, null, null);
                    } else {
                        Log.i(TAG, "Not dispatching CAMERA long press because user setup is in progress.");
                    }
                }
                return true;
            case 79:
            case 86:
            case 87:
            case 88:
            case 89:
            case 90:
            case 91:
            case 130:
            case 222:
                break;
            case 84:
                if (getKeyguardManager().inKeyguardRestrictedInputMode() || dispatcher == null) {
                    return false;
                }
                if (event.getRepeatCount() == 0) {
                    dispatcher.startTracking(event, this);
                    return false;
                }
                if (!event.isLongPress() || !dispatcher.isTracking(event)) {
                    return false;
                }
                Configuration config = this.mContext.getResources().getConfiguration();
                if (config.keyboard != 1 && config.hardKeyboardHidden != 2) {
                    return false;
                }
                if (isUserSetupComplete()) {
                    Intent intent3 = new Intent("android.intent.action.SEARCH_LONG_PRESS");
                    intent3.setFlags(268435456);
                    try {
                        this.mView.performHapticFeedback(0);
                        sendCloseSystemWindows();
                        getSearchManager().stopSearch();
                        this.mContext.startActivity(intent3);
                        dispatcher.performedLongPress(event);
                        return true;
                    } catch (ActivityNotFoundException e2) {
                        return false;
                    }
                }
                Log.i(TAG, "Not dispatching SEARCH long press because user setup is in progress.");
                return false;
            case 85:
            case 126:
            case 127:
                if (getTelephonyManager().getCallState() != 0) {
                    return true;
                }
                break;
            default:
                return false;
        }
        handleMediaKeyEvent(event);
        return true;
    }

    boolean onKeyUp(int keyCode, KeyEvent event) {
        KeyEvent.DispatcherState dispatcher = this.mView.getKeyDispatcherState();
        if (dispatcher != null) {
            dispatcher.handleUpEvent(event);
        }
        switch (keyCode) {
            case 5:
                if (getKeyguardManager().inKeyguardRestrictedInputMode()) {
                    return false;
                }
                if (!event.isTracking() || event.isCanceled()) {
                    return true;
                }
                if (isUserSetupComplete()) {
                    startCallActivity();
                    return true;
                }
                Log.i(TAG, "Not starting call activity because user setup is in progress.");
                return true;
            case 24:
            case 25:
            case 164:
                if (event.isCanceled()) {
                    return true;
                }
                MediaSessionLegacyHelper.getHelper(this.mContext).sendVolumeKeyEvent(event, false);
                return true;
            case 27:
                if (getKeyguardManager().inKeyguardRestrictedInputMode()) {
                    return false;
                }
                if (!event.isTracking() || !event.isCanceled()) {
                }
                return true;
            case 79:
            case 85:
            case 86:
            case 87:
            case 88:
            case 89:
            case 90:
            case 91:
            case 126:
            case 127:
            case 130:
            case 222:
                handleMediaKeyEvent(event);
                return true;
            default:
                return false;
        }
    }

    void startCallActivity() {
        sendCloseSystemWindows();
        Intent intent = new Intent("android.intent.action.CALL_BUTTON");
        intent.setFlags(268435456);
        try {
            this.mContext.startActivity(intent);
        } catch (ActivityNotFoundException e) {
            Log.w(TAG, "No activity found for android.intent.action.CALL_BUTTON.");
        }
    }

    SearchManager getSearchManager() {
        if (this.mSearchManager == null) {
            this.mSearchManager = (SearchManager) this.mContext.getSystemService("search");
        }
        return this.mSearchManager;
    }

    TelephonyManager getTelephonyManager() {
        if (this.mTelephonyManager == null) {
            this.mTelephonyManager = (TelephonyManager) this.mContext.getSystemService("phone");
        }
        return this.mTelephonyManager;
    }

    KeyguardManager getKeyguardManager() {
        if (this.mKeyguardManager == null) {
            this.mKeyguardManager = (KeyguardManager) this.mContext.getSystemService("keyguard");
        }
        return this.mKeyguardManager;
    }

    AudioManager getAudioManager() {
        if (this.mAudioManager == null) {
            this.mAudioManager = (AudioManager) this.mContext.getSystemService("audio");
        }
        return this.mAudioManager;
    }

    void sendCloseSystemWindows() {
        PhoneWindowManager.sendCloseSystemWindows(this.mContext, null);
    }

    private void handleMediaKeyEvent(KeyEvent keyEvent) {
        MediaSessionLegacyHelper.getHelper(this.mContext).sendMediaButtonEvent(keyEvent, false);
    }

    private boolean isUserSetupComplete() {
        return Settings.Secure.getInt(this.mContext.getContentResolver(), "user_setup_complete", 0) != 0;
    }
}
