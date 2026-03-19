package com.android.internal.policy;

import android.app.KeyguardManager;
import android.app.SearchManager;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.media.AudioManager;
import android.media.session.MediaSessionManager;
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
    MediaSessionManager mMediaSessionManager;
    SearchManager mSearchManager;
    TelephonyManager mTelephonyManager;
    View mView;

    public PhoneFallbackEventHandler(Context context) {
        this.mContext = context;
    }

    @Override
    public void setView(View view) {
        this.mView = view;
    }

    @Override
    public void preDispatchKeyEvent(KeyEvent keyEvent) {
        getAudioManager().preDispatchKeyEvent(keyEvent, Integer.MIN_VALUE);
    }

    @Override
    public boolean dispatchKeyEvent(KeyEvent keyEvent) {
        int action = keyEvent.getAction();
        int keyCode = keyEvent.getKeyCode();
        if (action == 0) {
            return onKeyDown(keyCode, keyEvent);
        }
        return onKeyUp(keyCode, keyEvent);
    }

    boolean onKeyDown(int r18, android.view.KeyEvent r19) {
        r3 = r17.mView.getKeyDispatcherState();
        if (r18 != 5) {
            if (r18 != 27) {
                if (r18 != 79 && r18 != 130) {
                    if (r18 != 164) {
                        if (r18 != 222) {
                            switch (r18) {
                                case 24:
                                case 25:
                                default:
                                    switch (r18) {
                                        case 84:
                                            if (!isNotInstantAppAndKeyguardRestricted(r3)) {
                                                if (r19.getRepeatCount() == 0) {
                                                    r3.startTracking(r19, r17);
                                                    break;
                                                } else {
                                                    if (r19.isLongPress() && r3.isTracking(r19)) {
                                                        r0 = r17.mContext.getResources().getConfiguration();
                                                        if (r0.keyboard == 1 || r0.hardKeyboardHidden == 2) {
                                                            if (isUserSetupComplete()) {
                                                                r0 = new android.content.Intent(android.content.Intent.ACTION_SEARCH_LONG_PRESS);
                                                                r0.setFlags(268435456);
                                                                try {
                                                                    r17.mView.performHapticFeedback(0);
                                                                    sendCloseSystemWindows();
                                                                    getSearchManager().stopSearch();
                                                                    r17.mContext.startActivity(r0);
                                                                    r3.performedLongPress(r19);
                                                                    break;
                                                                } catch (android.content.ActivityNotFoundException e) {
                                                                    break;
                                                                }
                                                            } else {
                                                                android.util.Log.i(com.android.internal.policy.PhoneFallbackEventHandler.TAG, "Not dispatching SEARCH long press because user setup is in progress.");
                                                                break;
                                                            }
                                                        }
                                                    }
                                                }
                                            }
                                            break;
                                        case 85:
                                            if (getTelephonyManager().getCallState() != 0) {
                                            }
                                            break;
                                        case 86:
                                        case 87:
                                        case 88:
                                        case 89:
                                        case 90:
                                        case 91:
                                            break;
                                        default:
                                            switch (r18) {
                                            }
                                    }
                            }
                            return true;
                        }
                    }
                    handleVolumeKeyEvent(r19);
                    return true;
                }
                handleMediaKeyEvent(r19);
                return true;
            } else {
                if (!isNotInstantAppAndKeyguardRestricted(r3)) {
                    if (r19.getRepeatCount() == 0) {
                        r3.startTracking(r19, r17);
                    } else {
                        if (r19.isLongPress() && r3.isTracking(r19)) {
                            r3.performedLongPress(r19);
                            if (isUserSetupComplete()) {
                                r17.mView.performHapticFeedback(0);
                                sendCloseSystemWindows();
                                r9 = new android.content.Intent(android.content.Intent.ACTION_CAMERA_BUTTON, null);
                                r9.addFlags(268435456);
                                r9.putExtra(android.content.Intent.EXTRA_KEY_EVENT, r19);
                                r17.mContext.sendOrderedBroadcastAsUser(r9, android.os.UserHandle.CURRENT_OR_SELF, null, null, null, 0, null, null);
                            } else {
                                android.util.Log.i(com.android.internal.policy.PhoneFallbackEventHandler.TAG, "Not dispatching CAMERA long press because user setup is in progress.");
                            }
                        }
                    }
                    return true;
                }
            }
        } else {
            if (!isNotInstantAppAndKeyguardRestricted(r3)) {
                if (r19.getRepeatCount() == 0) {
                    r3.startTracking(r19, r17);
                } else {
                    if (r19.isLongPress() && r3.isTracking(r19)) {
                        r3.performedLongPress(r19);
                        if (isUserSetupComplete()) {
                            r17.mView.performHapticFeedback(0);
                            r0 = new android.content.Intent(android.content.Intent.ACTION_VOICE_COMMAND);
                            r0.setFlags(268435456);
                            try {
                                sendCloseSystemWindows();
                                r17.mContext.startActivity(r0);
                            } catch (android.content.ActivityNotFoundException e) {
                                startCallActivity();
                            }
                        } else {
                            android.util.Log.i(com.android.internal.policy.PhoneFallbackEventHandler.TAG, "Not starting call activity because user setup is in progress.");
                        }
                    }
                }
                return true;
            }
        }
        return false;
    }

    private boolean isNotInstantAppAndKeyguardRestricted(KeyEvent.DispatcherState dispatcherState) {
        return !this.mContext.getPackageManager().isInstantApp() && (getKeyguardManager().inKeyguardRestrictedInputMode() || dispatcherState == null);
    }

    boolean onKeyUp(int i, KeyEvent keyEvent) {
        KeyEvent.DispatcherState keyDispatcherState = this.mView.getKeyDispatcherState();
        if (keyDispatcherState != null) {
            keyDispatcherState.handleUpEvent(keyEvent);
        }
        if (i == 5) {
            if (!isNotInstantAppAndKeyguardRestricted(keyDispatcherState)) {
                if (keyEvent.isTracking() && !keyEvent.isCanceled()) {
                    if (isUserSetupComplete()) {
                        startCallActivity();
                    } else {
                        Log.i(TAG, "Not starting call activity because user setup is in progress.");
                    }
                }
                return true;
            }
            return false;
        }
        if (i != 27) {
            if (i != 79 && i != 130) {
                if (i != 164) {
                    if (i != 222) {
                        switch (i) {
                            case 24:
                            case 25:
                                break;
                            default:
                                switch (i) {
                                    case 85:
                                    case 86:
                                    case 87:
                                    case 88:
                                    case 89:
                                    case 90:
                                    case 91:
                                        break;
                                    default:
                                        switch (i) {
                                            case 126:
                                            case 127:
                                                break;
                                            default:
                                                return false;
                                        }
                                        break;
                                }
                                break;
                        }
                    }
                }
                if (!keyEvent.isCanceled()) {
                    handleVolumeKeyEvent(keyEvent);
                }
                return true;
            }
            handleMediaKeyEvent(keyEvent);
            return true;
        }
        if (!isNotInstantAppAndKeyguardRestricted(keyDispatcherState)) {
            if (keyEvent.isTracking()) {
                keyEvent.isCanceled();
            }
            return true;
        }
        return false;
    }

    void startCallActivity() {
        sendCloseSystemWindows();
        Intent intent = new Intent(Intent.ACTION_CALL_BUTTON);
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
            this.mKeyguardManager = (KeyguardManager) this.mContext.getSystemService(Context.KEYGUARD_SERVICE);
        }
        return this.mKeyguardManager;
    }

    AudioManager getAudioManager() {
        if (this.mAudioManager == null) {
            this.mAudioManager = (AudioManager) this.mContext.getSystemService("audio");
        }
        return this.mAudioManager;
    }

    MediaSessionManager getMediaSessionManager() {
        if (this.mMediaSessionManager == null) {
            this.mMediaSessionManager = (MediaSessionManager) this.mContext.getSystemService(Context.MEDIA_SESSION_SERVICE);
        }
        return this.mMediaSessionManager;
    }

    void sendCloseSystemWindows() {
        PhoneWindow.sendCloseSystemWindows(this.mContext, null);
    }

    private void handleVolumeKeyEvent(KeyEvent keyEvent) {
        getMediaSessionManager().dispatchVolumeKeyEventAsSystemService(keyEvent, Integer.MIN_VALUE);
    }

    private void handleMediaKeyEvent(KeyEvent keyEvent) {
        getMediaSessionManager().dispatchMediaKeyEventAsSystemService(keyEvent);
    }

    private boolean isUserSetupComplete() {
        return Settings.Secure.getInt(this.mContext.getContentResolver(), Settings.Secure.USER_SETUP_COMPLETE, 0) != 0;
    }
}
