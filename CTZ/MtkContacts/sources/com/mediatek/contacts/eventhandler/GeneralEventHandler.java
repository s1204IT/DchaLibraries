package com.mediatek.contacts.eventhandler;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.text.TextUtils;
import com.mediatek.contacts.util.Log;
import java.util.ArrayList;
import java.util.List;

public class GeneralEventHandler {
    private static volatile GeneralEventHandler uniqueInstance;
    private Context mContext;
    private List<Listener> mListeners = new ArrayList();
    private boolean mRegistered = false;
    private BroadcastReceiver mPhbStateListener = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            boolean booleanExtra = intent.getBooleanExtra("ready", false);
            Log.i("GeneralEventHandler", "[PhbChangeState_onReceive]action: " + action + ",subId:" + intent.getIntExtra("subscription", -1000) + ",phbReady: " + booleanExtra);
            if ("mediatek.intent.action.PHB_STATE_CHANGED".equals(action)) {
                for (Listener listener : GeneralEventHandler.this.mListeners) {
                    if (listener != null) {
                        listener.onReceiveEvent("PhbChangeEvent", intent);
                    }
                }
            }
        }
    };
    private BroadcastReceiver mSdCardStateReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (action == null) {
                Log.e("GeneralEventHandler", "[SdCardState_onReceive] get action is null,return");
                return;
            }
            Log.i("GeneralEventHandler", "[SdCardState_onReceive] action = " + action);
            int sdCardMountedState = GeneralEventHandler.this.getSdCardMountedState(action);
            Intent intent2 = new Intent();
            intent2.putExtra("sdstate", sdCardMountedState);
            for (Listener listener : GeneralEventHandler.this.mListeners) {
                if (listener != null) {
                    listener.onReceiveEvent("SdStateChangeEvenet", intent2);
                }
            }
        }
    };

    public interface Listener {
        void onReceiveEvent(String str, Intent intent);
    }

    public static GeneralEventHandler getInstance(Context context) {
        if (uniqueInstance == null) {
            synchronized (GeneralEventHandler.class) {
                if (uniqueInstance == null) {
                    uniqueInstance = new GeneralEventHandler(context);
                }
            }
        }
        return uniqueInstance;
    }

    public synchronized void register(Listener listener) {
        Log.i("GeneralEventHandler", "[register] mContext: " + this.mContext + ",target: " + listener + ",mRegistered = " + this.mRegistered);
        if (listener != null && !this.mListeners.contains(listener)) {
            this.mListeners.add(listener);
            Log.i("GeneralEventHandler", "[register] currentListener: " + this.mListeners.toString());
        }
        if (!this.mRegistered) {
            registerBaseEventListener();
            this.mRegistered = true;
        }
    }

    public synchronized void unRegister(Listener listener) {
        Log.i("GeneralEventHandler", "[unRegister]target: " + listener + ",mRegistered = " + this.mRegistered);
        if (listener != null && this.mListeners.contains(listener)) {
            this.mListeners.remove(listener);
        }
        if (this.mListeners.isEmpty() && this.mRegistered) {
            unRegisterBaseEventListener();
            this.mRegistered = false;
        }
    }

    private GeneralEventHandler(Context context) {
        this.mContext = context.getApplicationContext();
        Log.i("GeneralEventHandler", "[GeneralEventHandler] get App Context: " + this.mContext);
    }

    private void registerBaseEventListener() {
        Log.i("GeneralEventHandler", "[registerBaseEventListener]");
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction("android.intent.action.MEDIA_EJECT");
        intentFilter.addAction("android.intent.action.MEDIA_MOUNTED");
        intentFilter.addDataScheme("file");
        this.mContext.registerReceiver(this.mSdCardStateReceiver, intentFilter);
        this.mContext.registerReceiver(this.mPhbStateListener, new IntentFilter("mediatek.intent.action.PHB_STATE_CHANGED"));
    }

    private void unRegisterBaseEventListener() {
        Log.i("GeneralEventHandler", "[unRegisterBaseEventListener]");
        this.mContext.unregisterReceiver(this.mSdCardStateReceiver);
        this.mContext.unregisterReceiver(this.mPhbStateListener);
    }

    private int getSdCardMountedState(String str) {
        int i = -1;
        if (TextUtils.isEmpty(str)) {
            Log.e("GeneralEventHandler", "[getSdCardMountedState] get action is null,return");
            return -1;
        }
        if ("android.intent.action.MEDIA_EJECT".equals(str)) {
            i = 1;
        } else if ("android.intent.action.MEDIA_MOUNTED".equals(str)) {
            i = 2;
        }
        Log.i("GeneralEventHandler", "[getSdCardMountedState] rst: " + i);
        return i;
    }
}
