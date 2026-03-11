package com.android.systemui.volume;

import android.R;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.media.AudioManager;
import android.util.Log;
import android.view.KeyEvent;
import com.android.systemui.statusbar.phone.SystemUIDialog;

public abstract class SafetyWarningDialog extends SystemUIDialog implements DialogInterface.OnClickListener, DialogInterface.OnDismissListener {
    private static final String TAG = Util.logTag(SafetyWarningDialog.class);
    private final AudioManager mAudioManager;
    private final Context mContext;
    private boolean mNewVolumeUp;
    private final BroadcastReceiver mReceiver;
    private long mShowTime;

    protected abstract void cleanUp();

    public SafetyWarningDialog(Context context, AudioManager audioManager) {
        super(context);
        this.mReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context2, Intent intent) {
                if ("android.intent.action.CLOSE_SYSTEM_DIALOGS".equals(intent.getAction())) {
                    if (D.BUG) {
                        Log.d(SafetyWarningDialog.TAG, "Received ACTION_CLOSE_SYSTEM_DIALOGS");
                    }
                    SafetyWarningDialog.this.cancel();
                    SafetyWarningDialog.this.cleanUp();
                }
            }
        };
        this.mContext = context;
        this.mAudioManager = audioManager;
        getWindow().setType(2010);
        setShowForAllUsers(true);
        setMessage(this.mContext.getString(R.string.lockscreen_access_pattern_start));
        setButton(-1, this.mContext.getString(R.string.yes), this);
        setButton(-2, this.mContext.getString(R.string.no), (DialogInterface.OnClickListener) null);
        setOnDismissListener(this);
        context.registerReceiver(this.mReceiver, new IntentFilter("android.intent.action.CLOSE_SYSTEM_DIALOGS"));
    }

    @Override
    public boolean onKeyDown(int i, KeyEvent keyEvent) {
        if (i == 24 && keyEvent.getRepeatCount() == 0) {
            this.mNewVolumeUp = true;
        }
        return super.onKeyDown(i, keyEvent);
    }

    @Override
    public boolean onKeyUp(int i, KeyEvent keyEvent) {
        if (i == 24 && this.mNewVolumeUp && System.currentTimeMillis() - this.mShowTime > 1000) {
            if (D.BUG) {
                Log.d(TAG, "Confirmed warning via VOLUME_UP");
            }
            this.mAudioManager.disableSafeMediaVolume();
            dismiss();
        }
        return super.onKeyUp(i, keyEvent);
    }

    @Override
    public void onClick(DialogInterface dialogInterface, int i) {
        this.mAudioManager.disableSafeMediaVolume();
    }

    @Override
    protected void onStart() {
        super.onStart();
        this.mShowTime = System.currentTimeMillis();
    }

    @Override
    public void onDismiss(DialogInterface dialogInterface) {
        this.mContext.unregisterReceiver(this.mReceiver);
        cleanUp();
    }
}
