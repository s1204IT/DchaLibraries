package com.android.bluetooth.gatt;

import android.os.Binder;
import android.os.IBinder;
import android.os.IInterface;
import android.os.RemoteException;
import android.os.SystemClock;
import android.os.WorkSource;
import android.util.Log;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.UUID;

class ContextMap<C, T> {
    private static final String TAG = "BtGatt.ContextMap";
    private List<ContextMap<C, T>.App> mApps = new ArrayList();
    HashMap<Integer, AppScanStats> mAppScanStats = new HashMap<>();
    Set<ContextMap<C, T>.Connection> mConnections = new HashSet();

    ContextMap() {
    }

    class Connection {
        public String address;
        public int appId;
        public int connId;
        public long startTime = SystemClock.elapsedRealtime();

        Connection(int i, String str, int i2) {
            this.connId = i;
            this.address = str;
            this.appId = i2;
        }
    }

    class App {
        public AppScanStats appScanStats;
        public C callback;
        boolean hasLocationPermisson;
        boolean hasPeersMacAddressPermission;
        public int id;
        public T info;
        public Boolean isCongested = false;
        private List<CallbackInfo> mCongestionQueue = new ArrayList();
        private IBinder.DeathRecipient mDeathRecipient;
        public String name;
        public UUID uuid;

        App(UUID uuid, C c, T t, String str, AppScanStats appScanStats) {
            this.uuid = uuid;
            this.callback = c;
            this.info = t;
            this.name = str;
            this.appScanStats = appScanStats;
        }

        void linkToDeath(IBinder.DeathRecipient deathRecipient) {
            if (this.callback == null) {
                return;
            }
            try {
                ((IInterface) this.callback).asBinder().linkToDeath(deathRecipient, 0);
                this.mDeathRecipient = deathRecipient;
            } catch (RemoteException e) {
                Log.e(ContextMap.TAG, "Unable to link deathRecipient for app id " + this.id);
            }
        }

        void unlinkToDeath() {
            if (this.mDeathRecipient != null) {
                try {
                    ((IInterface) this.callback).asBinder().unlinkToDeath(this.mDeathRecipient, 0);
                } catch (NoSuchElementException e) {
                    Log.e(ContextMap.TAG, "Unable to unlink deathRecipient for app id " + this.id);
                }
            }
        }

        void queueCallback(CallbackInfo callbackInfo) {
            this.mCongestionQueue.add(callbackInfo);
        }

        CallbackInfo popQueuedCallback() {
            if (this.mCongestionQueue.size() == 0) {
                return null;
            }
            return this.mCongestionQueue.remove(0);
        }
    }

    ContextMap<C, T>.App add(UUID uuid, WorkSource workSource, C c, T t, GattService gattService) {
        ContextMap<C, T>.App app;
        int callingUid = Binder.getCallingUid();
        String nameForUid = gattService.getPackageManager().getNameForUid(callingUid);
        if (nameForUid == null) {
            nameForUid = "Unknown App (UID: " + callingUid + ")";
        }
        String str = nameForUid;
        synchronized (this.mApps) {
            AppScanStats appScanStats = this.mAppScanStats.get(Integer.valueOf(callingUid));
            if (appScanStats == null) {
                appScanStats = new AppScanStats(str, workSource, this, gattService);
                this.mAppScanStats.put(Integer.valueOf(callingUid), appScanStats);
            }
            AppScanStats appScanStats2 = appScanStats;
            app = new App(uuid, c, t, str, appScanStats2);
            this.mApps.add(app);
            appScanStats2.isRegistered = true;
        }
        return app;
    }

    void remove(UUID uuid) {
        synchronized (this.mApps) {
            Iterator<ContextMap<C, T>.App> it = this.mApps.iterator();
            while (true) {
                if (!it.hasNext()) {
                    break;
                }
                ContextMap<C, T>.App next = it.next();
                if (next.uuid.equals(uuid)) {
                    next.unlinkToDeath();
                    next.appScanStats.isRegistered = false;
                    it.remove();
                    break;
                }
            }
        }
    }

    void remove(int i) {
        synchronized (this.mApps) {
            Iterator<ContextMap<C, T>.App> it = this.mApps.iterator();
            while (true) {
                if (!it.hasNext()) {
                    break;
                }
                ContextMap<C, T>.App next = it.next();
                if (next.id == i) {
                    removeConnectionsByAppId(i);
                    next.unlinkToDeath();
                    next.appScanStats.isRegistered = false;
                    it.remove();
                    break;
                }
            }
        }
    }

    List<Integer> getAllAppsIds() {
        ArrayList arrayList = new ArrayList();
        synchronized (this.mApps) {
            Iterator<ContextMap<C, T>.App> it = this.mApps.iterator();
            while (it.hasNext()) {
                arrayList.add(Integer.valueOf(it.next().id));
            }
        }
        return arrayList;
    }

    void addConnection(int i, int i2, String str) {
        synchronized (this.mConnections) {
            if (getById(i) != null) {
                this.mConnections.add(new Connection(i2, str, i));
            }
        }
    }

    void removeConnection(int i, int i2) {
        synchronized (this.mConnections) {
            Iterator<ContextMap<C, T>.Connection> it = this.mConnections.iterator();
            while (true) {
                if (!it.hasNext()) {
                    break;
                } else if (it.next().connId == i2) {
                    it.remove();
                    break;
                }
            }
        }
    }

    void removeConnectionsByAppId(int i) {
        Iterator<ContextMap<C, T>.Connection> it = this.mConnections.iterator();
        while (it.hasNext()) {
            if (it.next().appId == i) {
                it.remove();
            }
        }
    }

    ContextMap<C, T>.App getById(int i) {
        synchronized (this.mApps) {
            for (ContextMap<C, T>.App app : this.mApps) {
                if (app.id == i) {
                    return app;
                }
            }
            Log.e(TAG, "Context not found for ID " + i);
            return null;
        }
    }

    ContextMap<C, T>.App getByUuid(UUID uuid) {
        synchronized (this.mApps) {
            for (ContextMap<C, T>.App app : this.mApps) {
                if (app.uuid.equals(uuid)) {
                    return app;
                }
            }
            Log.e(TAG, "Context not found for UUID " + uuid);
            return null;
        }
    }

    ContextMap<C, T>.App getByName(String str) {
        synchronized (this.mApps) {
            for (ContextMap<C, T>.App app : this.mApps) {
                if (app.name.equals(str)) {
                    return app;
                }
            }
            Log.e(TAG, "Context not found for name " + str);
            return null;
        }
    }

    ContextMap<C, T>.App getByContextInfo(T t) {
        synchronized (this.mApps) {
            for (ContextMap<C, T>.App app : this.mApps) {
                if (app.info != null && app.info.equals(t)) {
                    return app;
                }
            }
            Log.e(TAG, "Context not found for info " + t);
            return null;
        }
    }

    AppScanStats getAppScanStatsById(int i) {
        ContextMap<C, T>.App byId = getById(i);
        if (byId != null) {
            return byId.appScanStats;
        }
        return null;
    }

    AppScanStats getAppScanStatsByUid(int i) {
        return this.mAppScanStats.get(Integer.valueOf(i));
    }

    Set<String> getConnectedDevices() {
        HashSet hashSet = new HashSet();
        Iterator<ContextMap<C, T>.Connection> it = this.mConnections.iterator();
        while (it.hasNext()) {
            hashSet.add(it.next().address);
        }
        return hashSet;
    }

    ContextMap<C, T>.App getByConnId(int i) {
        for (ContextMap<C, T>.Connection connection : this.mConnections) {
            if (connection.connId == i) {
                return getById(connection.appId);
            }
        }
        return null;
    }

    Integer connIdByAddress(int i, String str) {
        if (getById(i) == null) {
            return null;
        }
        for (ContextMap<C, T>.Connection connection : this.mConnections) {
            if (connection.address.equalsIgnoreCase(str) && connection.appId == i) {
                return Integer.valueOf(connection.connId);
            }
        }
        return null;
    }

    String addressByConnId(int i) {
        for (ContextMap<C, T>.Connection connection : this.mConnections) {
            if (connection.connId == i) {
                return connection.address;
            }
        }
        return null;
    }

    List<ContextMap<C, T>.Connection> getConnectionByApp(int i) {
        ArrayList arrayList = new ArrayList();
        for (ContextMap<C, T>.Connection connection : this.mConnections) {
            if (connection.appId == i) {
                arrayList.add(connection);
            }
        }
        return arrayList;
    }

    void clear() {
        synchronized (this.mApps) {
            Iterator<ContextMap<C, T>.App> it = this.mApps.iterator();
            while (it.hasNext()) {
                ContextMap<C, T>.App next = it.next();
                next.unlinkToDeath();
                next.appScanStats.isRegistered = false;
                it.remove();
            }
        }
        synchronized (this.mConnections) {
            this.mConnections.clear();
        }
    }

    Map<Integer, String> getConnectedMap() {
        HashMap map = new HashMap();
        for (ContextMap<C, T>.Connection connection : this.mConnections) {
            map.put(Integer.valueOf(connection.appId), connection.address);
        }
        return map;
    }

    void dump(StringBuilder sb) {
        sb.append("  Entries: " + this.mAppScanStats.size() + "\n\n");
        Iterator<Map.Entry<Integer, AppScanStats>> it = this.mAppScanStats.entrySet().iterator();
        while (it.hasNext()) {
            it.next().getValue().dumpToString(sb);
        }
    }
}
