package com.android.deskclock.controller;

import android.app.Activity;
import android.content.Context;
import android.support.annotation.StringRes;
import com.android.deskclock.Utils;
import com.android.deskclock.events.EventTracker;

public final class Controller {
    private static final Controller sController = new Controller();
    private Context mContext;
    private EventController mEventController;
    private ShortcutController mShortcutController;
    private VoiceController mVoiceController;

    private Controller() {
    }

    public static Controller getController() {
        return sController;
    }

    public void setContext(Context context) {
        if (this.mContext != context) {
            this.mContext = context.getApplicationContext();
            this.mEventController = new EventController();
            this.mVoiceController = new VoiceController();
            if (Utils.isNMR1OrLater()) {
                this.mShortcutController = new ShortcutController(this.mContext);
            }
        }
    }

    public void addEventTracker(EventTracker eventTracker) {
        Utils.enforceMainLooper();
        this.mEventController.addEventTracker(eventTracker);
    }

    public void removeEventTracker(EventTracker eventTracker) {
        Utils.enforceMainLooper();
        this.mEventController.removeEventTracker(eventTracker);
    }

    public void sendEvent(@StringRes int i, @StringRes int i2, @StringRes int i3) {
        this.mEventController.sendEvent(i, i2, i3);
    }

    public void notifyVoiceSuccess(Activity activity, String str) {
        this.mVoiceController.notifyVoiceSuccess(activity, str);
    }

    public void notifyVoiceFailure(Activity activity, String str) {
        this.mVoiceController.notifyVoiceFailure(activity, str);
    }

    public void updateShortcuts() {
        Utils.enforceMainLooper();
        if (this.mShortcutController != null) {
            this.mShortcutController.updateShortcuts();
        }
    }
}
