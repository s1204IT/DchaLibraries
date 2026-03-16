package com.android.bluetooth.gatt;

import android.os.IBinder;
import android.os.IInterface;
import android.os.RemoteException;
import android.util.Log;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.UUID;

class ContextMap<T> {
    private static final String TAG = "BtGatt.ContextMap";
    List<ContextMap<T>.App> mApps = new ArrayList();
    Set<ContextMap<T>.Connection> mConnections = new HashSet();

    ContextMap() {
    }

    class Connection {
        String address;
        int appId;
        int connId;

        Connection(int connId, String address, int appId) {
            this.connId = connId;
            this.address = address;
            this.appId = appId;
        }
    }

    class App {
        T callback;
        int id;
        private IBinder.DeathRecipient mDeathRecipient;
        UUID uuid;
        Boolean isCongested = false;
        private List<CallbackInfo> congestionQueue = new ArrayList();

        App(UUID uuid, T callback) {
            this.uuid = uuid;
            this.callback = callback;
        }

        void linkToDeath(IBinder.DeathRecipient deathRecipient) {
            try {
                IBinder binder = ((IInterface) this.callback).asBinder();
                binder.linkToDeath(deathRecipient, 0);
                this.mDeathRecipient = deathRecipient;
            } catch (RemoteException e) {
                Log.e(ContextMap.TAG, "Unable to link deathRecipient for app id " + this.id);
            }
        }

        void unlinkToDeath() {
            if (this.mDeathRecipient != null) {
                try {
                    IBinder binder = ((IInterface) this.callback).asBinder();
                    binder.unlinkToDeath(this.mDeathRecipient, 0);
                } catch (NoSuchElementException e) {
                    Log.e(ContextMap.TAG, "Unable to unlink deathRecipient for app id " + this.id);
                }
            }
        }

        void queueCallback(CallbackInfo callbackInfo) {
            this.congestionQueue.add(callbackInfo);
        }

        CallbackInfo popQueuedCallback() {
            if (this.congestionQueue.size() == 0) {
                return null;
            }
            return this.congestionQueue.remove(0);
        }
    }

    void add(UUID uuid, T callback) {
        synchronized (this.mApps) {
            this.mApps.add(new App(uuid, callback));
        }
    }

    void remove(UUID uuid) {
        synchronized (this.mApps) {
            Iterator<ContextMap<T>.App> i = this.mApps.iterator();
            while (true) {
                if (!i.hasNext()) {
                    break;
                }
                ContextMap<T>.App entry = i.next();
                if (entry.uuid.equals(uuid)) {
                    entry.unlinkToDeath();
                    i.remove();
                    break;
                }
            }
        }
    }

    void remove(int id) {
        synchronized (this.mApps) {
            Iterator<ContextMap<T>.App> i = this.mApps.iterator();
            while (true) {
                if (!i.hasNext()) {
                    break;
                }
                ContextMap<T>.App entry = i.next();
                if (entry.id == id) {
                    entry.unlinkToDeath();
                    i.remove();
                    break;
                }
            }
        }
    }

    void addConnection(int id, int connId, String address) {
        synchronized (this.mConnections) {
            ContextMap<T>.App entry = getById(id);
            if (entry != null) {
                this.mConnections.add(new Connection(connId, address, id));
            }
        }
    }

    void removeConnection(int id, int connId) {
        synchronized (this.mConnections) {
            Iterator<ContextMap<T>.Connection> i = this.mConnections.iterator();
            while (true) {
                if (!i.hasNext()) {
                    break;
                }
                ContextMap<T>.Connection connection = i.next();
                if (connection.connId == connId) {
                    i.remove();
                    break;
                }
            }
        }
    }

    ContextMap<T>.App getById(int id) {
        for (ContextMap<T>.App entry : this.mApps) {
            if (entry.id == id) {
                return entry;
            }
        }
        Log.e(TAG, "Context not found for ID " + id);
        return null;
    }

    ContextMap<T>.App getByUuid(UUID uuid) {
        for (ContextMap<T>.App entry : this.mApps) {
            if (entry.uuid.equals(uuid)) {
                return entry;
            }
        }
        Log.e(TAG, "Context not found for UUID " + uuid);
        return null;
    }

    Set<String> getConnectedDevices() {
        Set<String> addresses = new HashSet<>();
        for (ContextMap<T>.Connection connection : this.mConnections) {
            addresses.add(connection.address);
        }
        return addresses;
    }

    ContextMap<T>.App getByConnId(int connId) {
        for (ContextMap<T>.Connection connection : this.mConnections) {
            if (connection.connId == connId) {
                return getById(connection.appId);
            }
        }
        return null;
    }

    Integer connIdByAddress(int id, String address) {
        ContextMap<T>.App entry = getById(id);
        if (entry == null) {
            return null;
        }
        for (ContextMap<T>.Connection connection : this.mConnections) {
            if (connection.address.equals(address) && connection.appId == id) {
                return Integer.valueOf(connection.connId);
            }
        }
        return null;
    }

    String addressByConnId(int connId) {
        for (ContextMap<T>.Connection connection : this.mConnections) {
            if (connection.connId == connId) {
                return connection.address;
            }
        }
        return null;
    }

    List<ContextMap<T>.Connection> getConnectionByApp(int appId) {
        List<ContextMap<T>.Connection> currentConnections = new ArrayList<>();
        for (ContextMap<T>.Connection connection : this.mConnections) {
            if (connection.appId == appId) {
                currentConnections.add(connection);
            }
        }
        return currentConnections;
    }

    void clear() {
        synchronized (this.mApps) {
            Iterator<ContextMap<T>.App> i = this.mApps.iterator();
            while (i.hasNext()) {
                ContextMap<T>.App entry = i.next();
                entry.unlinkToDeath();
                i.remove();
            }
        }
        synchronized (this.mConnections) {
            this.mConnections.clear();
        }
    }

    void dump(StringBuilder sb) {
        sb.append("  Entries: " + this.mApps.size() + "\n");
        for (ContextMap<T>.App entry : this.mApps) {
            List<ContextMap<T>.Connection> connections = getConnectionByApp(entry.id);
            sb.append("\n  Application Id: " + entry.id + "\n");
            sb.append("  UUID: " + entry.uuid + "\n");
            sb.append("  Connections: " + connections.size() + "\n");
            for (ContextMap<T>.Connection connection : connections) {
                sb.append("    " + connection.connId + ": " + connection.address + "\n");
            }
        }
    }
}
