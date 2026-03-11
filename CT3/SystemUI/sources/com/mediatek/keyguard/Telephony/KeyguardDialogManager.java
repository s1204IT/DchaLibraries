package com.mediatek.keyguard.Telephony;

import android.content.Context;
import android.database.ContentObserver;
import android.os.Handler;
import android.provider.Settings;
import android.util.Log;
import com.android.keyguard.KeyguardUpdateMonitor;
import com.android.keyguard.KeyguardUtils;
import com.mediatek.keyguard.PowerOffAlarm.PowerOffAlarmManager;
import java.util.LinkedList;
import java.util.Queue;

public class KeyguardDialogManager {
    private static KeyguardDialogManager sInstance;
    private final Context mContext;
    private DialogSequenceManager mDialogSequenceManager = new DialogSequenceManager();
    private KeyguardUpdateMonitor mUpdateMonitor;

    public interface DialogShowCallBack {
        void show();
    }

    private KeyguardDialogManager(Context context) {
        this.mContext = context;
        this.mUpdateMonitor = KeyguardUpdateMonitor.getInstance(context);
    }

    public static KeyguardDialogManager getInstance(Context context) {
        if (sInstance == null) {
            sInstance = new KeyguardDialogManager(context);
        }
        return sInstance;
    }

    public void requestShowDialog(DialogShowCallBack callback) {
        this.mDialogSequenceManager.requestShowDialog(callback);
    }

    public void reportDialogClose() {
        this.mDialogSequenceManager.reportDialogClose();
    }

    private class DialogSequenceManager {
        private Queue<DialogShowCallBack> mDialogShowCallbackQueue;
        private boolean mInnerDialogShowing = false;
        private boolean mLocked = false;
        private ContentObserver mDialogSequenceObserver = new ContentObserver(new Handler()) {
            @Override
            public void onChange(boolean selfChange) {
                int value = DialogSequenceManager.this.queryDialogSequenceSeetings();
                Log.d("KeyguardDialogManager", "DialogSequenceManager DialogSequenceObserver--onChange()--dialog_sequence_settings = " + value);
                if (value == 0) {
                    DialogSequenceManager.this.setLocked(false);
                    DialogSequenceManager.this.handleShowDialog();
                } else {
                    if (value != 1) {
                        return;
                    }
                    DialogSequenceManager.this.setLocked(true);
                    DialogSequenceManager.this.handleShowDialog();
                }
            }
        };

        public DialogSequenceManager() {
            Log.d("KeyguardDialogManager", "DialogSequenceManager DialogSequenceManager()");
            this.mDialogShowCallbackQueue = new LinkedList();
            KeyguardDialogManager.this.mContext.getContentResolver().registerContentObserver(Settings.System.getUriFor("dialog_sequence_settings"), false, this.mDialogSequenceObserver);
        }

        public void requestShowDialog(DialogShowCallBack callback) {
            Log.d("KeyguardDialogManager", "DialogSequenceManager --requestShowDialog()");
            this.mDialogShowCallbackQueue.add(callback);
            handleShowDialog();
        }

        public void handleShowDialog() {
            Log.d("KeyguardDialogManager", "DialogSequenceManager --handleShowDialog()--enableShow() = " + enableShow());
            if (!enableShow()) {
                return;
            }
            if (getLocked()) {
                DialogShowCallBack dialogCallBack = this.mDialogShowCallbackQueue.poll();
                Log.d("KeyguardDialogManager", "DialogSequenceManager --handleShowDialog()--dialogCallBack = " + dialogCallBack);
                if (dialogCallBack == null) {
                    return;
                }
                dialogCallBack.show();
                setInnerDialogShowing(true);
                return;
            }
            Log.d("KeyguardDialogManager", "DialogSequenceManager --handleShowDialog()--System.putInt( dialog_sequence_settings value = 1");
            Settings.System.putInt(KeyguardDialogManager.this.mContext.getContentResolver(), "dialog_sequence_settings", 1);
        }

        public void reportDialogClose() {
            Log.d("KeyguardDialogManager", "DialogSequenceManager --reportDialogClose()--mDialogShowCallbackQueue.isEmpty() = " + this.mDialogShowCallbackQueue.isEmpty());
            setInnerDialogShowing(false);
            if (this.mDialogShowCallbackQueue.isEmpty()) {
                Log.d("KeyguardDialogManager", "DialogSequenceManager --reportDialogClose()--System.putInt( dialog_sequence_settings value = 0 --setLocked(false)--");
                Settings.System.putInt(KeyguardDialogManager.this.mContext.getContentResolver(), "dialog_sequence_settings", 0);
                setLocked(false);
                return;
            }
            handleShowDialog();
        }

        private boolean enableShow() {
            Log.d("KeyguardDialogManager", "DialogSequenceManager --enableShow()-- !mDialogShowCallbackQueue.isEmpty() = " + (!this.mDialogShowCallbackQueue.isEmpty()) + " !getInnerDialogShowing() = " + (!getInnerDialogShowing()) + " !isOtherModuleShowing() = " + (!isOtherModuleShowing()) + "!isAlarmBoot() = " + (!PowerOffAlarmManager.isAlarmBoot()) + " isDeviceProvisioned() = " + KeyguardDialogManager.this.mUpdateMonitor.isDeviceProvisioned());
            return (this.mDialogShowCallbackQueue.isEmpty() || getInnerDialogShowing() || isOtherModuleShowing() || PowerOffAlarmManager.isAlarmBoot() || !KeyguardDialogManager.this.mUpdateMonitor.isDeviceProvisioned() || KeyguardUtils.isSystemEncrypted()) ? false : true;
        }

        private boolean isOtherModuleShowing() {
            int value = queryDialogSequenceSeetings();
            Log.d("KeyguardDialogManager", "DialogSequenceManager --isOtherModuleShowing()--dialog_sequence_settings = " + value);
            return (value == 0 || value == 1) ? false : true;
        }

        private void setInnerDialogShowing(boolean show) {
            this.mInnerDialogShowing = show;
        }

        private boolean getInnerDialogShowing() {
            return this.mInnerDialogShowing;
        }

        public void setLocked(boolean locked) {
            this.mLocked = locked;
        }

        private boolean getLocked() {
            return this.mLocked;
        }

        public int queryDialogSequenceSeetings() {
            int value = Settings.System.getInt(KeyguardDialogManager.this.mContext.getContentResolver(), "dialog_sequence_settings", 0);
            return value;
        }
    }
}
