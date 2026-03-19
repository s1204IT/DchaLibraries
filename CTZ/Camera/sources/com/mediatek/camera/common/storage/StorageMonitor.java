package com.mediatek.camera.common.storage;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import com.mediatek.camera.common.debug.LogHelper;
import com.mediatek.camera.common.debug.LogUtil;
import com.mediatek.camera.common.storage.IStorageService;
import java.util.Iterator;
import java.util.concurrent.CopyOnWriteArrayList;

class StorageMonitor {
    private static final LogUtil.Tag TAG = new LogUtil.Tag(StorageMonitor.class.getSimpleName());
    private final Context mContext;
    private final CopyOnWriteArrayList<IStorageService.IStorageStateListener> mIStorageStateListener = new CopyOnWriteArrayList<>();
    private final BroadcastReceiver mReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            LogHelper.d(StorageMonitor.TAG, "mReceiver.onReceive(" + intent + ")");
            if (intent.getAction() == null) {
                LogHelper.d(StorageMonitor.TAG, "[mReceiver.onReceive] action is null");
                return;
            }
            switch (intent.getAction()) {
                case "android.intent.action.MEDIA_EJECT":
                    if (StorageMonitor.this.mStorage.isSameStorage(intent)) {
                        Iterator it = StorageMonitor.this.mIStorageStateListener.iterator();
                        while (it.hasNext()) {
                            ((IStorageService.IStorageStateListener) it.next()).onStateChanged(0, intent);
                        }
                        return;
                    }
                    return;
                case "android.intent.action.MEDIA_UNMOUNTED":
                case "android.intent.action.MEDIA_MOUNTED":
                case "android.intent.action.MEDIA_SCANNER_FINISHED":
                    StorageMonitor.this.mStorage.updateDefaultDirectory();
                    Iterator it2 = StorageMonitor.this.mIStorageStateListener.iterator();
                    while (it2.hasNext()) {
                        ((IStorageService.IStorageStateListener) it2.next()).onStateChanged(StorageMonitor.this.mStorage.getAvailableSpace() >= 0 ? 1 : 0, intent);
                    }
                    break;
                case "android.intent.action.MEDIA_CHECKING":
                case "android.intent.action.MEDIA_SCANNER_STARTED":
                    break;
                default:
                    return;
            }
            if (StorageMonitor.this.mStorage.isSameStorage(intent)) {
                Iterator it3 = StorageMonitor.this.mIStorageStateListener.iterator();
                while (it3.hasNext()) {
                    ((IStorageService.IStorageStateListener) it3.next()).onStateChanged(2, intent);
                }
            }
        }
    };
    private final Storage mStorage;

    public StorageMonitor(Context context, Storage storage) {
        this.mContext = context;
        this.mStorage = storage;
    }

    public void registerStorageStateListener(IStorageService.IStorageStateListener iStorageStateListener) {
        this.mStorage.updateDefaultDirectory();
        if (iStorageStateListener != null && !this.mIStorageStateListener.contains(iStorageStateListener)) {
            this.mIStorageStateListener.add(iStorageStateListener);
        }
    }

    public void unRegisterStorageStateListener(IStorageService.IStorageStateListener iStorageStateListener) {
        this.mIStorageStateListener.remove(iStorageStateListener);
    }

    public void registerIntentFilter() {
        IntentFilter intentFilter = new IntentFilter("android.intent.action.MEDIA_MOUNTED");
        intentFilter.addAction("android.intent.action.MEDIA_EJECT");
        intentFilter.addAction("android.intent.action.MEDIA_UNMOUNTED");
        intentFilter.addAction("android.intent.action.MEDIA_SCANNER_STARTED");
        intentFilter.addAction("android.intent.action.MEDIA_SCANNER_FINISHED");
        intentFilter.addAction("android.intent.action.MEDIA_CHECKING");
        intentFilter.addDataScheme("file");
        this.mContext.registerReceiver(this.mReceiver, intentFilter);
    }

    public void unregisterIntentFilter() {
        this.mContext.unregisterReceiver(this.mReceiver);
    }
}
