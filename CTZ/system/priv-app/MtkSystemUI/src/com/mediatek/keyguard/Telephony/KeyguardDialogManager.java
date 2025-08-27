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

/* loaded from: classes.dex */
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

    public void requestShowDialog(DialogShowCallBack dialogShowCallBack) {
        this.mDialogSequenceManager.requestShowDialog(dialogShowCallBack);
    }

    public void reportDialogClose() {
        this.mDialogSequenceManager.reportDialogClose();
    }

    private class DialogSequenceManager {
        private Queue<DialogShowCallBack> mDialogShowCallbackQueue;
        private boolean mInnerDialogShowing = false;
        private boolean mLocked = false;
        private ContentObserver mDialogSequenceObserver = new ContentObserver(new Handler()) { // from class: com.mediatek.keyguard.Telephony.KeyguardDialogManager.DialogSequenceManager.1
            @Override // android.database.ContentObserver
            public void onChange(boolean z) {
                int iQueryDialogSequenceSeetings = DialogSequenceManager.this.queryDialogSequenceSeetings();
                Log.d("KeyguardDialogManager", "DialogSequenceManager DialogSequenceObserver--onChange()--dialog_sequence_settings = " + iQueryDialogSequenceSeetings);
                if (iQueryDialogSequenceSeetings == 0) {
                    DialogSequenceManager.this.setLocked(false);
                    DialogSequenceManager.this.handleShowDialog();
                } else if (iQueryDialogSequenceSeetings == 1) {
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

        public void requestShowDialog(DialogShowCallBack dialogShowCallBack) {
            Log.d("KeyguardDialogManager", "DialogSequenceManager --requestShowDialog()");
            this.mDialogShowCallbackQueue.add(dialogShowCallBack);
            handleShowDialog();
        }

        public void handleShowDialog() {
            Log.d("KeyguardDialogManager", "DialogSequenceManager --handleShowDialog()--enableShow() = " + enableShow());
            if (enableShow()) {
                if (getLocked()) {
                    DialogShowCallBack dialogShowCallBackPoll = this.mDialogShowCallbackQueue.poll();
                    Log.d("KeyguardDialogManager", "DialogSequenceManager --handleShowDialog()--dialogCallBack = " + dialogShowCallBackPoll);
                    if (dialogShowCallBackPoll != null) {
                        dialogShowCallBackPoll.show();
                        setInnerDialogShowing(true);
                        return;
                    }
                    return;
                }
                Log.d("KeyguardDialogManager", "DialogSequenceManager --handleShowDialog()--System.putInt( dialog_sequence_settings value = 1");
                Settings.System.putInt(KeyguardDialogManager.this.mContext.getContentResolver(), "dialog_sequence_settings", 1);
            }
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
            StringBuilder sb = new StringBuilder();
            sb.append("DialogSequenceManager --enableShow()-- !mDialogShowCallbackQueue.isEmpty() = ");
            sb.append(!this.mDialogShowCallbackQueue.isEmpty());
            sb.append(" !getInnerDialogShowing() = ");
            sb.append(!getInnerDialogShowing());
            sb.append(" !isOtherModuleShowing() = ");
            sb.append(!isOtherModuleShowing());
            sb.append("!isAlarmBoot() = ");
            sb.append(!PowerOffAlarmManager.isAlarmBoot());
            sb.append(" isDeviceProvisioned() = ");
            sb.append(KeyguardDialogManager.this.mUpdateMonitor.isDeviceProvisioned());
            Log.d("KeyguardDialogManager", sb.toString());
            return (this.mDialogShowCallbackQueue.isEmpty() || getInnerDialogShowing() || isOtherModuleShowing() || PowerOffAlarmManager.isAlarmBoot() || !KeyguardDialogManager.this.mUpdateMonitor.isDeviceProvisioned() || KeyguardUtils.isSystemEncrypted()) ? false : true;
        }

        private boolean isOtherModuleShowing() {
            int iQueryDialogSequenceSeetings = queryDialogSequenceSeetings();
            Log.d("KeyguardDialogManager", "DialogSequenceManager --isOtherModuleShowing()--dialog_sequence_settings = " + iQueryDialogSequenceSeetings);
            return (iQueryDialogSequenceSeetings == 0 || iQueryDialogSequenceSeetings == 1) ? false : true;
        }

        private void setInnerDialogShowing(boolean z) {
            this.mInnerDialogShowing = z;
        }

        private boolean getInnerDialogShowing() {
            return this.mInnerDialogShowing;
        }

        private void setLocked(boolean z) {
            this.mLocked = z;
        }

        private boolean getLocked() {
            return this.mLocked;
        }

        private int queryDialogSequenceSeetings() {
            return Settings.System.getInt(KeyguardDialogManager.this.mContext.getContentResolver(), "dialog_sequence_settings", 0);
        }
    }
}
