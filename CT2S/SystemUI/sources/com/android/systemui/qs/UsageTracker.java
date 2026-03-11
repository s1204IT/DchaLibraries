package com.android.systemui.qs;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import com.android.systemui.R;
import com.android.systemui.statusbar.phone.SystemUIDialog;
import com.android.systemui.statusbar.policy.Listenable;

public class UsageTracker implements Listenable {
    private final Context mContext;
    private final String mPrefKey;
    private BroadcastReceiver mReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (UsageTracker.this.mResetAction.equals(intent.getAction())) {
                UsageTracker.this.reset();
            }
        }
    };
    private boolean mRegistered;
    private final String mResetAction;
    private final long mTimeToShowTile;

    public UsageTracker(Context context, Class<?> tile, int timeoutResource) {
        this.mContext = context;
        this.mPrefKey = tile.getSimpleName() + "LastUsed";
        this.mTimeToShowTile = 86400000 * ((long) this.mContext.getResources().getInteger(timeoutResource));
        this.mResetAction = "com.android.systemui.qs." + tile.getSimpleName() + ".usage_reset";
    }

    @Override
    public void setListening(boolean listen) {
        if (listen && !this.mRegistered) {
            this.mContext.registerReceiver(this.mReceiver, new IntentFilter(this.mResetAction));
            this.mRegistered = true;
        } else if (!listen && this.mRegistered) {
            this.mContext.unregisterReceiver(this.mReceiver);
            this.mRegistered = false;
        }
    }

    public boolean isRecentlyUsed() {
        long lastUsed = getSharedPrefs().getLong(this.mPrefKey, 0L);
        return System.currentTimeMillis() - lastUsed < this.mTimeToShowTile;
    }

    public void trackUsage() {
        getSharedPrefs().edit().putLong(this.mPrefKey, System.currentTimeMillis()).commit();
    }

    public void reset() {
        getSharedPrefs().edit().remove(this.mPrefKey).commit();
    }

    public void showResetConfirmation(String title, final Runnable onConfirmed) {
        SystemUIDialog d = new SystemUIDialog(this.mContext);
        d.setTitle(title);
        d.setMessage(this.mContext.getString(R.string.quick_settings_reset_confirmation_message));
        d.setNegativeButton(android.R.string.cancel, null);
        d.setPositiveButton(R.string.quick_settings_reset_confirmation_button, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                UsageTracker.this.reset();
                if (onConfirmed != null) {
                    onConfirmed.run();
                }
            }
        });
        d.setCanceledOnTouchOutside(true);
        d.show();
    }

    private SharedPreferences getSharedPrefs() {
        return this.mContext.getSharedPreferences(this.mContext.getPackageName(), 0);
    }
}
