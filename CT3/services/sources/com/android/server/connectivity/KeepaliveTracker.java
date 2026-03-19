package com.android.server.connectivity;

import android.net.NetworkUtils;
import android.net.util.IpUtils;
import android.os.Binder;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;
import android.util.Log;
import android.util.Pair;
import com.android.internal.util.HexDump;
import com.android.internal.util.IndentingPrintWriter;
import com.android.server.connectivity.KeepalivePacketData;
import java.net.InetAddress;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;

public class KeepaliveTracker {
    private static final boolean DBG = false;
    public static final String PERMISSION = "android.permission.PACKET_KEEPALIVE_OFFLOAD";
    private static final String TAG = "KeepaliveTracker";
    private final Handler mConnectivityServiceHandler;
    private final HashMap<NetworkAgentInfo, HashMap<Integer, KeepaliveInfo>> mKeepalives = new HashMap<>();

    public KeepaliveTracker(Handler handler) {
        this.mConnectivityServiceHandler = handler;
    }

    class KeepaliveInfo implements IBinder.DeathRecipient {
        public boolean isStarted;
        private final IBinder mBinder;
        private final int mInterval;
        private final Messenger mMessenger;
        private final NetworkAgentInfo mNai;
        private final KeepalivePacketData mPacket;
        private int mSlot = -1;
        private final int mPid = Binder.getCallingPid();
        private final int mUid = Binder.getCallingUid();

        public KeepaliveInfo(Messenger messenger, IBinder binder, NetworkAgentInfo nai, KeepalivePacketData packet, int interval) {
            this.mMessenger = messenger;
            this.mBinder = binder;
            this.mNai = nai;
            this.mPacket = packet;
            this.mInterval = interval;
            try {
                this.mBinder.linkToDeath(this, 0);
            } catch (RemoteException e) {
                binderDied();
            }
        }

        public NetworkAgentInfo getNai() {
            return this.mNai;
        }

        public String toString() {
            return new StringBuffer("KeepaliveInfo [").append(" network=").append(this.mNai.network).append(" isStarted=").append(this.isStarted).append(" ").append(IpUtils.addressAndPortToString(this.mPacket.srcAddress, this.mPacket.srcPort)).append("->").append(IpUtils.addressAndPortToString(this.mPacket.dstAddress, this.mPacket.dstPort)).append(" interval=" + this.mInterval).append(" data=" + HexDump.toHexString(this.mPacket.data)).append(" uid=").append(this.mUid).append(" pid=").append(this.mPid).append(" ]").toString();
        }

        void notifyMessenger(int slot, int err) {
            KeepaliveTracker.this.notifyMessenger(this.mMessenger, slot, err);
        }

        @Override
        public void binderDied() {
            KeepaliveTracker.this.mConnectivityServiceHandler.obtainMessage(528396, this.mSlot, -10, this.mNai.network).sendToTarget();
        }

        void unlinkDeathRecipient() {
            if (this.mBinder == null) {
                return;
            }
            this.mBinder.unlinkToDeath(this, 0);
        }

        private int checkNetworkConnected() {
            if (!this.mNai.networkInfo.isConnectedOrConnecting()) {
                return -20;
            }
            return 0;
        }

        private int checkSourceAddress() {
            for (InetAddress address : this.mNai.linkProperties.getAddresses()) {
                if (address.equals(this.mPacket.srcAddress)) {
                    return 0;
                }
            }
            return -21;
        }

        private int checkInterval() {
            return this.mInterval >= 20 ? 0 : -24;
        }

        private int isValid() {
            int error;
            synchronized (this.mNai) {
                error = checkInterval();
                if (error == 0) {
                    error = checkNetworkConnected();
                }
                if (error == 0) {
                    error = checkSourceAddress();
                }
            }
            return error;
        }

        void start(int slot) {
            int error = isValid();
            if (error == 0) {
                this.mSlot = slot;
                Log.d(KeepaliveTracker.TAG, "Starting keepalive " + this.mSlot + " on " + this.mNai.name());
                this.mNai.asyncChannel.sendMessage(528395, slot, this.mInterval, this.mPacket);
                return;
            }
            notifyMessenger(-1, error);
        }

        void stop(int reason) {
            int uid = Binder.getCallingUid();
            if (uid == this.mUid || uid != 1000) {
            }
            if (this.isStarted) {
                Log.d(KeepaliveTracker.TAG, "Stopping keepalive " + this.mSlot + " on " + this.mNai.name());
                this.mNai.asyncChannel.sendMessage(528396, this.mSlot);
            }
            notifyMessenger(this.mSlot, reason);
            unlinkDeathRecipient();
        }
    }

    void notifyMessenger(Messenger messenger, int slot, int err) {
        Message message = Message.obtain();
        message.what = 528397;
        message.arg1 = slot;
        message.arg2 = err;
        message.obj = null;
        try {
            messenger.send(message);
        } catch (RemoteException e) {
        }
    }

    private int findFirstFreeSlot(NetworkAgentInfo nai) {
        HashMap<Integer, KeepaliveInfo> map = this.mKeepalives.get(nai);
        if (map == null) {
            map = new HashMap<>();
            this.mKeepalives.put(nai, map);
        }
        int slot = 1;
        while (slot <= map.size()) {
            if (map.get(Integer.valueOf(slot)) != null) {
                slot++;
            } else {
                return slot;
            }
        }
        return slot;
    }

    public void handleStartKeepalive(Message message) {
        KeepaliveInfo ki = (KeepaliveInfo) message.obj;
        NetworkAgentInfo nai = ki.getNai();
        int slot = findFirstFreeSlot(nai);
        this.mKeepalives.get(nai).put(Integer.valueOf(slot), ki);
        ki.start(slot);
    }

    public void handleStopAllKeepalives(NetworkAgentInfo nai, int reason) {
        HashMap<Integer, KeepaliveInfo> networkKeepalives = this.mKeepalives.get(nai);
        if (networkKeepalives == null) {
            return;
        }
        for (KeepaliveInfo ki : networkKeepalives.values()) {
            ki.stop(reason);
        }
        networkKeepalives.clear();
        this.mKeepalives.remove(nai);
    }

    public void handleStopKeepalive(NetworkAgentInfo nai, int slot, int reason) {
        String networkName = nai == null ? "(null)" : nai.name();
        HashMap<Integer, KeepaliveInfo> networkKeepalives = this.mKeepalives.get(nai);
        if (networkKeepalives == null) {
            Log.e(TAG, "Attempt to stop keepalive on nonexistent network " + networkName);
            return;
        }
        KeepaliveInfo ki = networkKeepalives.get(Integer.valueOf(slot));
        if (ki == null) {
            Log.e(TAG, "Attempt to stop nonexistent keepalive " + slot + " on " + networkName);
            return;
        }
        ki.stop(reason);
        networkKeepalives.remove(Integer.valueOf(slot));
        if (!networkKeepalives.isEmpty()) {
            return;
        }
        this.mKeepalives.remove(nai);
    }

    public void handleCheckKeepalivesStillValid(NetworkAgentInfo nai) {
        HashMap<Integer, KeepaliveInfo> networkKeepalives = this.mKeepalives.get(nai);
        if (networkKeepalives == null) {
            return;
        }
        ArrayList<Pair<Integer, Integer>> invalidKeepalives = new ArrayList<>();
        Iterator slot$iterator = networkKeepalives.keySet().iterator();
        while (slot$iterator.hasNext()) {
            int slot = ((Integer) slot$iterator.next()).intValue();
            int error = networkKeepalives.get(Integer.valueOf(slot)).isValid();
            if (error != 0) {
                invalidKeepalives.add(Pair.create(Integer.valueOf(slot), Integer.valueOf(error)));
            }
        }
        for (Pair<Integer, Integer> slotAndError : invalidKeepalives) {
            handleStopKeepalive(nai, ((Integer) slotAndError.first).intValue(), ((Integer) slotAndError.second).intValue());
        }
    }

    public void handleEventPacketKeepalive(NetworkAgentInfo nai, Message message) {
        int slot = message.arg1;
        int reason = message.arg2;
        KeepaliveInfo ki = null;
        try {
            ki = this.mKeepalives.get(nai).get(Integer.valueOf(slot));
        } catch (NullPointerException e) {
        }
        if (ki == null) {
            Log.e(TAG, "Event for unknown keepalive " + slot + " on " + nai.name());
            return;
        }
        if (reason == 0 && !ki.isStarted) {
            ki.isStarted = true;
            ki.notifyMessenger(slot, reason);
        } else {
            ki.isStarted = false;
            if (reason == 0) {
            }
            handleStopKeepalive(nai, slot, reason);
        }
    }

    public void startNattKeepalive(NetworkAgentInfo nai, int intervalSeconds, Messenger messenger, IBinder binder, String srcAddrString, int srcPort, String dstAddrString, int dstPort) {
        if (nai == null) {
            notifyMessenger(messenger, -1, -20);
            return;
        }
        try {
            InetAddress srcAddress = NetworkUtils.numericToInetAddress(srcAddrString);
            InetAddress dstAddress = NetworkUtils.numericToInetAddress(dstAddrString);
            try {
                KeepalivePacketData packet = KeepalivePacketData.nattKeepalivePacket(srcAddress, srcPort, dstAddress, 4500);
                KeepaliveInfo ki = new KeepaliveInfo(messenger, binder, nai, packet, intervalSeconds);
                Log.d(TAG, "Created keepalive: " + ki.toString());
                this.mConnectivityServiceHandler.obtainMessage(528395, ki).sendToTarget();
            } catch (KeepalivePacketData.InvalidPacketException e) {
                notifyMessenger(messenger, -1, e.error);
            }
        } catch (IllegalArgumentException e2) {
            notifyMessenger(messenger, -1, -21);
        }
    }

    public void dump(IndentingPrintWriter pw) {
        pw.println("Packet keepalives:");
        pw.increaseIndent();
        for (NetworkAgentInfo nai : this.mKeepalives.keySet()) {
            pw.println(nai.name());
            pw.increaseIndent();
            Iterator slot$iterator = this.mKeepalives.get(nai).keySet().iterator();
            while (slot$iterator.hasNext()) {
                int slot = ((Integer) slot$iterator.next()).intValue();
                KeepaliveInfo ki = this.mKeepalives.get(nai).get(Integer.valueOf(slot));
                pw.println(slot + ": " + ki.toString());
            }
            pw.decreaseIndent();
        }
        pw.decreaseIndent();
    }
}
