package com.android.systemui.qs;

import android.app.ActivityManager;
import android.content.Context;
import android.database.ContentObserver;
import android.os.Handler;
import android.provider.Settings;
import com.android.systemui.statusbar.policy.Listenable;

public abstract class SecureSetting extends ContentObserver implements Listenable {
    private final Context mContext;
    private boolean mListening;
    private int mObservedValue;
    private final String mSettingName;
    private int mUserId;

    protected abstract void handleValueChanged(int i, boolean z);

    public SecureSetting(Context context, Handler handler, String settingName) {
        super(handler);
        this.mObservedValue = 0;
        this.mContext = context;
        this.mSettingName = settingName;
        this.mUserId = ActivityManager.getCurrentUser();
    }

    public int getValue() {
        return Settings.Secure.getIntForUser(this.mContext.getContentResolver(), this.mSettingName, 0, this.mUserId);
    }

    public void setValue(int value) {
        Settings.Secure.putIntForUser(this.mContext.getContentResolver(), this.mSettingName, value, this.mUserId);
    }

    @Override
    public void setListening(boolean listening) {
        if (listening != this.mListening) {
            this.mListening = listening;
            if (listening) {
                this.mObservedValue = getValue();
                this.mContext.getContentResolver().registerContentObserver(Settings.Secure.getUriFor(this.mSettingName), false, this, this.mUserId);
            } else {
                this.mContext.getContentResolver().unregisterContentObserver(this);
                this.mObservedValue = 0;
            }
        }
    }

    @Override
    public void onChange(boolean selfChange) {
        int value = getValue();
        handleValueChanged(value, value != this.mObservedValue);
        this.mObservedValue = value;
    }

    public void setUserId(int userId) {
        this.mUserId = userId;
        if (this.mListening) {
            setListening(false);
            setListening(true);
        }
    }
}
