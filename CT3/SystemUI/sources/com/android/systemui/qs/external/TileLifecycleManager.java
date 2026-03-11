package com.android.systemui.qs.external;

import android.app.AppGlobals;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.content.pm.ServiceInfo;
import android.net.Uri;
import android.os.Handler;
import android.os.IBinder;
import android.os.RemoteException;
import android.os.UserHandle;
import android.service.quicksettings.IQSService;
import android.service.quicksettings.IQSTileService;
import android.service.quicksettings.Tile;
import android.support.annotation.VisibleForTesting;
import android.util.ArraySet;
import android.util.Log;
import java.util.Set;
import libcore.util.Objects;

public class TileLifecycleManager extends BroadcastReceiver implements IQSTileService, ServiceConnection, IBinder.DeathRecipient {
    private int mBindTryCount;
    private boolean mBound;
    private TileChangeListener mChangeListener;
    private IBinder mClickBinder;
    private final Context mContext;
    private final Handler mHandler;
    private final Intent mIntent;
    private boolean mIsBound;
    private boolean mListening;
    private Set<Integer> mQueuedMessages = new ArraySet();

    @VisibleForTesting
    boolean mReceiverRegistered;
    private boolean mUnbindImmediate;
    private final UserHandle mUser;
    private QSTileServiceWrapper mWrapper;

    public interface TileChangeListener {
        void onTileChanged(ComponentName componentName);
    }

    public TileLifecycleManager(Handler handler, Context context, IQSService service, Tile tile, Intent intent, UserHandle user) {
        this.mContext = context;
        this.mHandler = handler;
        this.mIntent = intent;
        this.mIntent.putExtra("service", service.asBinder());
        this.mIntent.putExtra("android.service.quicksettings.extra.COMPONENT", intent.getComponent());
        this.mUser = user;
    }

    public ComponentName getComponent() {
        return this.mIntent.getComponent();
    }

    public boolean hasPendingClick() {
        boolean zContains;
        synchronized (this.mQueuedMessages) {
            zContains = this.mQueuedMessages.contains(2);
        }
        return zContains;
    }

    public boolean isActiveTile() {
        try {
            ServiceInfo info = this.mContext.getPackageManager().getServiceInfo(this.mIntent.getComponent(), 8320);
            if (info.metaData != null) {
                return info.metaData.getBoolean("android.service.quicksettings.ACTIVE_TILE", false);
            }
            return false;
        } catch (PackageManager.NameNotFoundException e) {
            return false;
        }
    }

    public void flushMessagesAndUnbind() {
        this.mUnbindImmediate = true;
        setBindService(true);
    }

    public void setBindService(boolean bind) {
        this.mBound = bind;
        if (bind) {
            if (this.mBindTryCount == 5) {
                startPackageListening();
                return;
            }
            if (!checkComponentState()) {
                return;
            }
            this.mBindTryCount++;
            try {
                this.mIsBound = this.mContext.bindServiceAsUser(this.mIntent, this, 33554433, this.mUser);
                return;
            } catch (SecurityException e) {
                Log.e("TileLifecycleManager", "Failed to bind to service", e);
                this.mIsBound = false;
                return;
            }
        }
        this.mBindTryCount = 0;
        this.mWrapper = null;
        if (!this.mIsBound) {
            return;
        }
        this.mContext.unbindService(this);
        this.mIsBound = false;
    }

    @Override
    public void onServiceConnected(ComponentName name, IBinder service) {
        this.mBindTryCount = 0;
        QSTileServiceWrapper wrapper = new QSTileServiceWrapper(IQSTileService.Stub.asInterface(service));
        try {
            service.linkToDeath(this, 0);
        } catch (RemoteException e) {
        }
        this.mWrapper = wrapper;
        handlePendingMessages();
    }

    @Override
    public void onServiceDisconnected(ComponentName name) {
        handleDeath();
    }

    private void handlePendingMessages() {
        ArraySet<Integer> queue;
        synchronized (this.mQueuedMessages) {
            queue = new ArraySet<>(this.mQueuedMessages);
            this.mQueuedMessages.clear();
        }
        if (queue.contains(0)) {
            onTileAdded();
        }
        if (this.mListening) {
            onStartListening();
        }
        if (queue.contains(2)) {
            if (!this.mListening) {
                Log.w("TileLifecycleManager", "Managed to get click on non-listening state...");
            } else {
                onClick(this.mClickBinder);
            }
        }
        if (queue.contains(3)) {
            if (!this.mListening) {
                Log.w("TileLifecycleManager", "Managed to get unlock on non-listening state...");
            } else {
                onUnlockComplete();
            }
        }
        if (queue.contains(1)) {
            if (this.mListening) {
                Log.w("TileLifecycleManager", "Managed to get remove in listening state...");
                onStopListening();
            }
            onTileRemoved();
        }
        if (!this.mUnbindImmediate) {
            return;
        }
        this.mUnbindImmediate = false;
        setBindService(false);
    }

    public void handleDestroy() {
        if (!this.mReceiverRegistered) {
            return;
        }
        stopPackageListening();
    }

    private void handleDeath() {
        if (this.mWrapper == null) {
            return;
        }
        this.mWrapper = null;
        if (!this.mBound || !checkComponentState()) {
            return;
        }
        this.mHandler.postDelayed(new Runnable() {
            @Override
            public void run() {
                if (!TileLifecycleManager.this.mBound) {
                    return;
                }
                TileLifecycleManager.this.setBindService(true);
            }
        }, 1000L);
    }

    private boolean checkComponentState() {
        PackageManager pm = this.mContext.getPackageManager();
        if (!isPackageAvailable(pm) || !isComponentAvailable(pm)) {
            startPackageListening();
            return false;
        }
        return true;
    }

    private void startPackageListening() {
        IntentFilter filter = new IntentFilter("android.intent.action.PACKAGE_ADDED");
        filter.addAction("android.intent.action.PACKAGE_CHANGED");
        filter.addDataScheme("package");
        this.mContext.registerReceiverAsUser(this, this.mUser, filter, null, this.mHandler);
        this.mContext.registerReceiverAsUser(this, this.mUser, new IntentFilter("android.intent.action.USER_UNLOCKED"), null, this.mHandler);
        this.mReceiverRegistered = true;
    }

    private void stopPackageListening() {
        this.mContext.unregisterReceiver(this);
        this.mReceiverRegistered = false;
    }

    public void setTileChangeListener(TileChangeListener changeListener) {
        this.mChangeListener = changeListener;
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        if (!"android.intent.action.USER_UNLOCKED".equals(intent.getAction())) {
            Uri data = intent.getData();
            String pkgName = data.getEncodedSchemeSpecificPart();
            if (!Objects.equal(pkgName, this.mIntent.getComponent().getPackageName())) {
                return;
            }
        }
        if ("android.intent.action.PACKAGE_CHANGED".equals(intent.getAction()) && this.mChangeListener != null) {
            this.mChangeListener.onTileChanged(this.mIntent.getComponent());
        }
        stopPackageListening();
        if (!this.mBound) {
            return;
        }
        setBindService(true);
    }

    private boolean isComponentAvailable(PackageManager pm) {
        this.mIntent.getComponent().getPackageName();
        try {
            ServiceInfo si = AppGlobals.getPackageManager().getServiceInfo(this.mIntent.getComponent(), 0, this.mUser.getIdentifier());
            return si != null;
        } catch (RemoteException e) {
            return false;
        }
    }

    private boolean isPackageAvailable(PackageManager pm) {
        String packageName = this.mIntent.getComponent().getPackageName();
        try {
            pm.getPackageInfoAsUser(packageName, 0, this.mUser.getIdentifier());
            return true;
        } catch (PackageManager.NameNotFoundException e) {
            Log.d("TileLifecycleManager", "Package not available: " + packageName);
            return false;
        }
    }

    private void queueMessage(int message) {
        synchronized (this.mQueuedMessages) {
            this.mQueuedMessages.add(Integer.valueOf(message));
        }
    }

    public void onTileAdded() {
        if (this.mWrapper != null && this.mWrapper.onTileAdded()) {
            return;
        }
        queueMessage(0);
        handleDeath();
    }

    public void onTileRemoved() {
        if (this.mWrapper != null && this.mWrapper.onTileRemoved()) {
            return;
        }
        queueMessage(1);
        handleDeath();
    }

    public void onStartListening() {
        this.mListening = true;
        if (this.mWrapper == null || this.mWrapper.onStartListening()) {
            return;
        }
        handleDeath();
    }

    public void onStopListening() {
        this.mListening = false;
        if (this.mWrapper == null || this.mWrapper.onStopListening()) {
            return;
        }
        handleDeath();
    }

    public void onClick(IBinder iBinder) {
        if (this.mWrapper != null && this.mWrapper.onClick(iBinder)) {
            return;
        }
        this.mClickBinder = iBinder;
        queueMessage(2);
        handleDeath();
    }

    public void onUnlockComplete() {
        if (this.mWrapper != null && this.mWrapper.onUnlockComplete()) {
            return;
        }
        queueMessage(3);
        handleDeath();
    }

    public IBinder asBinder() {
        if (this.mWrapper != null) {
            return this.mWrapper.asBinder();
        }
        return null;
    }

    @Override
    public void binderDied() {
        handleDeath();
    }
}
