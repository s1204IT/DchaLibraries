package com.android.systemui.qs.external;

import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.content.pm.ServiceInfo;
import android.os.Binder;
import android.os.Handler;
import android.os.IBinder;
import android.os.RemoteException;
import android.os.UserHandle;
import android.service.quicksettings.IQSService;
import android.service.quicksettings.IQSTileService;
import android.service.quicksettings.Tile;
import android.util.ArraySet;
import android.util.Log;
import java.util.Objects;
import java.util.Set;

/* loaded from: classes.dex */
public class TileLifecycleManager extends BroadcastReceiver implements ServiceConnection, IBinder.DeathRecipient, IQSTileService {
    private int mBindRetryDelay;
    private int mBindTryCount;
    private boolean mBound;
    private TileChangeListener mChangeListener;
    private IBinder mClickBinder;
    private final Context mContext;
    private final Handler mHandler;
    private final Intent mIntent;
    private boolean mIsBound;
    private boolean mListening;
    private final PackageManagerAdapter mPackageManagerAdapter;
    private Set<Integer> mQueuedMessages;
    boolean mReceiverRegistered;
    private final IBinder mToken;
    private boolean mUnbindImmediate;
    private final UserHandle mUser;
    private QSTileServiceWrapper mWrapper;

    public interface TileChangeListener {
        void onTileChanged(ComponentName componentName);
    }

    public TileLifecycleManager(Handler handler, Context context, IQSService iQSService, Tile tile, Intent intent, UserHandle userHandle) {
        this(handler, context, iQSService, tile, intent, userHandle, new PackageManagerAdapter(context));
    }

    TileLifecycleManager(Handler handler, Context context, IQSService iQSService, Tile tile, Intent intent, UserHandle userHandle, PackageManagerAdapter packageManagerAdapter) {
        this.mToken = new Binder();
        this.mQueuedMessages = new ArraySet();
        this.mBindRetryDelay = 1000;
        this.mContext = context;
        this.mHandler = handler;
        this.mIntent = intent;
        this.mIntent.putExtra("service", iQSService.asBinder());
        this.mIntent.putExtra("token", this.mToken);
        this.mUser = userHandle;
        this.mPackageManagerAdapter = packageManagerAdapter;
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
            ServiceInfo serviceInfo = this.mPackageManagerAdapter.getServiceInfo(this.mIntent.getComponent(), 8320);
            if (serviceInfo.metaData != null) {
                return serviceInfo.metaData.getBoolean("android.service.quicksettings.ACTIVE_TILE", false);
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

    public void setBindService(boolean z) {
        if (this.mBound && this.mUnbindImmediate) {
            this.mUnbindImmediate = false;
            return;
        }
        this.mBound = z;
        if (z) {
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
        if (this.mIsBound) {
            this.mContext.unbindService(this);
            this.mIsBound = false;
        }
    }

    @Override // android.content.ServiceConnection
    public void onServiceConnected(ComponentName componentName, IBinder iBinder) throws RemoteException {
        this.mBindTryCount = 0;
        QSTileServiceWrapper qSTileServiceWrapper = new QSTileServiceWrapper(IQSTileService.Stub.asInterface(iBinder));
        try {
            iBinder.linkToDeath(this, 0);
        } catch (RemoteException e) {
        }
        this.mWrapper = qSTileServiceWrapper;
        handlePendingMessages();
    }

    @Override // android.content.ServiceConnection
    public void onServiceDisconnected(ComponentName componentName) {
        handleDeath();
    }

    private void handlePendingMessages() {
        ArraySet arraySet;
        synchronized (this.mQueuedMessages) {
            arraySet = new ArraySet(this.mQueuedMessages);
            this.mQueuedMessages.clear();
        }
        if (arraySet.contains(0)) {
            onTileAdded();
        }
        if (this.mListening) {
            onStartListening();
        }
        if (arraySet.contains(2)) {
            if (!this.mListening) {
                Log.w("TileLifecycleManager", "Managed to get click on non-listening state...");
            } else {
                onClick(this.mClickBinder);
            }
        }
        if (arraySet.contains(3)) {
            if (!this.mListening) {
                Log.w("TileLifecycleManager", "Managed to get unlock on non-listening state...");
            } else {
                onUnlockComplete();
            }
        }
        if (arraySet.contains(1)) {
            if (this.mListening) {
                Log.w("TileLifecycleManager", "Managed to get remove in listening state...");
                onStopListening();
            }
            onTileRemoved();
        }
        if (this.mUnbindImmediate) {
            this.mUnbindImmediate = false;
            setBindService(false);
        }
    }

    public void handleDestroy() {
        if (this.mReceiverRegistered) {
            stopPackageListening();
        }
    }

    private void handleDeath() {
        if (this.mWrapper == null) {
            return;
        }
        this.mWrapper = null;
        if (this.mBound && checkComponentState()) {
            this.mHandler.postDelayed(new Runnable() { // from class: com.android.systemui.qs.external.TileLifecycleManager.1
                @Override // java.lang.Runnable
                public void run() {
                    if (TileLifecycleManager.this.mBound) {
                        TileLifecycleManager.this.setBindService(true);
                    }
                }
            }, this.mBindRetryDelay);
        }
    }

    private boolean checkComponentState() {
        if (!isPackageAvailable() || !isComponentAvailable()) {
            startPackageListening();
            return false;
        }
        return true;
    }

    private void startPackageListening() {
        IntentFilter intentFilter = new IntentFilter("android.intent.action.PACKAGE_ADDED");
        intentFilter.addAction("android.intent.action.PACKAGE_CHANGED");
        intentFilter.addDataScheme("package");
        this.mContext.registerReceiverAsUser(this, this.mUser, intentFilter, null, this.mHandler);
        this.mContext.registerReceiverAsUser(this, this.mUser, new IntentFilter("android.intent.action.USER_UNLOCKED"), null, this.mHandler);
        this.mReceiverRegistered = true;
    }

    private void stopPackageListening() {
        this.mContext.unregisterReceiver(this);
        this.mReceiverRegistered = false;
    }

    public void setTileChangeListener(TileChangeListener tileChangeListener) {
        this.mChangeListener = tileChangeListener;
    }

    @Override // android.content.BroadcastReceiver
    public void onReceive(Context context, Intent intent) {
        if (!"android.intent.action.USER_UNLOCKED".equals(intent.getAction()) && !Objects.equals(intent.getData().getEncodedSchemeSpecificPart(), this.mIntent.getComponent().getPackageName())) {
            return;
        }
        if ("android.intent.action.PACKAGE_CHANGED".equals(intent.getAction()) && this.mChangeListener != null) {
            this.mChangeListener.onTileChanged(this.mIntent.getComponent());
        }
        stopPackageListening();
        if (this.mBound) {
            setBindService(true);
        }
    }

    private boolean isComponentAvailable() {
        this.mIntent.getComponent().getPackageName();
        try {
            return this.mPackageManagerAdapter.getServiceInfo(this.mIntent.getComponent(), 0, this.mUser.getIdentifier()) != null;
        } catch (RemoteException e) {
            return false;
        }
    }

    private boolean isPackageAvailable() {
        String packageName = this.mIntent.getComponent().getPackageName();
        try {
            this.mPackageManagerAdapter.getPackageInfoAsUser(packageName, 0, this.mUser.getIdentifier());
            return true;
        } catch (PackageManager.NameNotFoundException e) {
            Log.d("TileLifecycleManager", "Package not available: " + packageName);
            return false;
        }
    }

    private void queueMessage(int i) {
        synchronized (this.mQueuedMessages) {
            this.mQueuedMessages.add(Integer.valueOf(i));
        }
    }

    public void onTileAdded() {
        if (this.mWrapper == null || !this.mWrapper.onTileAdded()) {
            queueMessage(0);
            handleDeath();
        }
    }

    public void onTileRemoved() {
        if (this.mWrapper == null || !this.mWrapper.onTileRemoved()) {
            queueMessage(1);
            handleDeath();
        }
    }

    public void onStartListening() {
        this.mListening = true;
        if (this.mWrapper != null && !this.mWrapper.onStartListening()) {
            handleDeath();
        }
    }

    public void onStopListening() {
        this.mListening = false;
        if (this.mWrapper != null && !this.mWrapper.onStopListening()) {
            handleDeath();
        }
    }

    public void onClick(IBinder iBinder) {
        if (this.mWrapper == null || !this.mWrapper.onClick(iBinder)) {
            this.mClickBinder = iBinder;
            queueMessage(2);
            handleDeath();
        }
    }

    public void onUnlockComplete() {
        if (this.mWrapper == null || !this.mWrapper.onUnlockComplete()) {
            queueMessage(3);
            handleDeath();
        }
    }

    public IBinder asBinder() {
        if (this.mWrapper != null) {
            return this.mWrapper.asBinder();
        }
        return null;
    }

    @Override // android.os.IBinder.DeathRecipient
    public void binderDied() {
        handleDeath();
    }

    public IBinder getToken() {
        return this.mToken;
    }

    public static boolean isTileAdded(Context context, ComponentName componentName) {
        return context.getSharedPreferences("tiles_prefs", 0).getBoolean(componentName.flattenToString(), false);
    }

    public static void setTileAdded(Context context, ComponentName componentName, boolean z) {
        context.getSharedPreferences("tiles_prefs", 0).edit().putBoolean(componentName.flattenToString(), z).commit();
    }
}
