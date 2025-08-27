package com.android.systemui.statusbar.policy;

import android.content.Context;
import android.content.res.Resources;
import android.database.ContentObserver;
import android.net.Uri;
import android.os.Handler;
import android.provider.Settings;
import android.util.KeyValueListParser;
import android.util.Log;
import com.android.systemui.R;

/* loaded from: classes.dex */
public final class SmartReplyConstants extends ContentObserver {
    private final Context mContext;
    private final boolean mDefaultEnabled;
    private final int mDefaultMaxSqueezeRemeasureAttempts;
    private final boolean mDefaultRequiresP;
    private boolean mEnabled;
    private int mMaxSqueezeRemeasureAttempts;
    private final KeyValueListParser mParser;
    private boolean mRequiresTargetingP;

    public SmartReplyConstants(Handler handler, Context context) {
        super(handler);
        this.mParser = new KeyValueListParser(',');
        this.mContext = context;
        Resources resources = this.mContext.getResources();
        this.mDefaultEnabled = resources.getBoolean(R.bool.config_smart_replies_in_notifications_enabled);
        this.mDefaultRequiresP = resources.getBoolean(R.bool.config_smart_replies_in_notifications_requires_targeting_p);
        this.mDefaultMaxSqueezeRemeasureAttempts = resources.getInteger(R.integer.config_smart_replies_in_notifications_max_squeeze_remeasure_attempts);
        this.mContext.getContentResolver().registerContentObserver(Settings.Global.getUriFor("smart_replies_in_notifications_flags"), false, this);
        updateConstants();
    }

    @Override // android.database.ContentObserver
    public void onChange(boolean z, Uri uri) {
        updateConstants();
    }

    private void updateConstants() {
        synchronized (this) {
            try {
                this.mParser.setString(Settings.Global.getString(this.mContext.getContentResolver(), "smart_replies_in_notifications_flags"));
            } catch (IllegalArgumentException e) {
                Log.e("SmartReplyConstants", "Bad smart reply constants", e);
            }
            this.mEnabled = this.mParser.getBoolean("enabled", this.mDefaultEnabled);
            this.mRequiresTargetingP = this.mParser.getBoolean("requires_targeting_p", this.mDefaultRequiresP);
            this.mMaxSqueezeRemeasureAttempts = this.mParser.getInt("max_squeeze_remeasure_attempts", this.mDefaultMaxSqueezeRemeasureAttempts);
        }
    }

    public boolean isEnabled() {
        return this.mEnabled;
    }

    public boolean requiresTargetingP() {
        return this.mRequiresTargetingP;
    }

    public int getMaxSqueezeRemeasureAttempts() {
        return this.mMaxSqueezeRemeasureAttempts;
    }
}
