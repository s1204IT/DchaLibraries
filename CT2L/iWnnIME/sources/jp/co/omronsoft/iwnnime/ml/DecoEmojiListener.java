package jp.co.omronsoft.iwnnime.ml;

import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import java.util.List;
import jp.co.omronsoft.android.decoemojimanager.interfacedata.DecoEmojiAttrInfo;
import jp.co.omronsoft.android.decoemojimanager.interfacedata.IDecoEmojiConstant;
import jp.co.omronsoft.android.emoji.EmojiAssist;
import jp.co.omronsoft.iwnnime.ml.decoemoji.DecoEmojiOperationQueue;
import jp.co.omronsoft.iwnnime.ml.decoemoji.DecoEmojiUtil;

public class DecoEmojiListener extends BroadcastReceiver {
    private static final int ATTRINFO_MAX_OPERATE_CNT = 100;
    private static boolean DEBUG = false;
    public static final String PREF_KEY = "preferenceId";
    private static final String TAG = "DecoEmojiListener";
    private SharedPreferences mPref = null;

    @Override
    public void onReceive(Context context, Intent intent) {
        Bundle bundle;
        List<DecoEmojiAttrInfo> receivedatalist;
        if (DEBUG) {
            Log.d(TAG, "onReceive() Start");
        }
        EmojiAssist assist = EmojiAssist.getInstance();
        if (assist != null) {
            int functype = assist.getEmojiFunctionType();
            if (functype == 0) {
                return;
            }
        }
        synchronized (OnReceiveLock.lock) {
            this.mPref = context.getSharedPreferences(IWnnIME.FILENAME_NOT_RESET_SETTINGS_PREFERENCE, 0);
            String action = intent.getAction();
            boolean isValidAction = false;
            boolean isUpdatePreferenceId = false;
            if (action.equals("jp.co.omronsoft.android.decoemojimanager")) {
                isValidAction = true;
            } else if (action.equals(getClass().getPackage().getName())) {
                isValidAction = true;
                isUpdatePreferenceId = true;
            }
            if (isValidAction && (bundle = intent.getExtras()) != null) {
                int type = bundle.getInt(IDecoEmojiConstant.BROADCAST_TYPE_TAG);
                if (type == 4) {
                    DecoEmojiOperationQueue.getInstance().clearOperation();
                    reciveOperation(null, type, context, true);
                    SharedPreferences.Editor editor = this.mPref.edit();
                    editor.putInt(PREF_KEY, -1);
                    editor.commit();
                } else if (type != 9 && (receivedatalist = bundle.getParcelableArrayList(IDecoEmojiConstant.BROADCAST_DATA_TAG)) != null && !receivedatalist.isEmpty()) {
                    int len = receivedatalist.size();
                    DecoEmojiAttrInfo[] decoemojiattrinfo = new DecoEmojiAttrInfo[len];
                    receivedatalist.toArray(decoemojiattrinfo);
                    if (decoemojiattrinfo.length <= 100) {
                        reciveOperation(decoemojiattrinfo, type, context, isUpdatePreferenceId);
                        if (type == 0) {
                            boolean isSerialConfirmFlag = false;
                            int prefInt = IWnnIME.getIntFromNotResetSettingsPreference(context, PREF_KEY, 0);
                            for (DecoEmojiAttrInfo decoEmojiAttrInfo : decoemojiattrinfo) {
                                int newSerial = decoEmojiAttrInfo.getId();
                                if (prefInt < newSerial) {
                                    prefInt = newSerial;
                                    isSerialConfirmFlag = true;
                                }
                            }
                            if (isSerialConfirmFlag) {
                                updateConfirm(context, prefInt);
                            }
                        }
                    }
                }
            }
        }
        if (DEBUG) {
            Log.d(TAG, "onReceive() End");
        }
    }

    private void updateConfirm(Context context, int updatePrefInt) {
        if (DEBUG) {
            Log.d(TAG, "updateConfirm() Start");
        }
        Intent confirmIntent = new Intent();
        confirmIntent.setClassName("jp.co.omronsoft.android.decoemojimanager", DecoEmojiUtil.DECOEMOJIMANAGER_CLASSNAME);
        try {
            ComponentName retService = context.startService(confirmIntent);
            if (retService == null) {
                Log.w(TAG, "(Warning) Service does not exist!");
            } else if (DEBUG) {
                Log.d(TAG, "updateConfirm() End");
            }
        } catch (SecurityException se) {
            Log.e(TAG, "(Exception) startService Error!");
            se.printStackTrace();
        }
    }

    private void reciveOperation(DecoEmojiAttrInfo[] decoemojiattrinfo, int type, Context context, boolean isUpdatePreferenceId) throws Throwable {
        DecoEmojiOperationQueue.getInstance().enqueueOperation(decoemojiattrinfo, type, context, isUpdatePreferenceId);
        IWnnIME wnn = IWnnIME.getCurrentIme();
        if (wnn != null) {
            wnn.onEvent(new IWnnImeEvent(IWnnImeEvent.RECEIVE_DECOEMOJI));
        }
    }
}
