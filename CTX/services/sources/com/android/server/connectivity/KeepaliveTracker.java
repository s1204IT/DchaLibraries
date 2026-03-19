package com.android.server.connectivity;

import android.net.KeepalivePacketData;
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

        public KeepaliveInfo(Messenger messenger, IBinder iBinder, NetworkAgentInfo networkAgentInfo, KeepalivePacketData keepalivePacketData, int i) {
            this.mMessenger = messenger;
            this.mBinder = iBinder;
            this.mNai = networkAgentInfo;
            this.mPacket = keepalivePacketData;
            this.mInterval = i;
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
            StringBuffer stringBuffer = new StringBuffer("KeepaliveInfo [");
            stringBuffer.append(" network=");
            stringBuffer.append(this.mNai.network);
            stringBuffer.append(" isStarted=");
            stringBuffer.append(this.isStarted);
            stringBuffer.append(" ");
            stringBuffer.append(IpUtils.addressAndPortToString(this.mPacket.srcAddress, this.mPacket.srcPort));
            stringBuffer.append("->");
            stringBuffer.append(IpUtils.addressAndPortToString(this.mPacket.dstAddress, this.mPacket.dstPort));
            stringBuffer.append(" interval=" + this.mInterval);
            stringBuffer.append(" packetData=" + HexDump.toHexString(this.mPacket.getPacket()));
            stringBuffer.append(" uid=");
            stringBuffer.append(this.mUid);
            stringBuffer.append(" pid=");
            stringBuffer.append(this.mPid);
            stringBuffer.append(" ]");
            return stringBuffer.toString();
        }

        void notifyMessenger(int i, int i2) {
            KeepaliveTracker.this.notifyMessenger(this.mMessenger, i, i2);
        }

        @Override
        public void binderDied() {
            KeepaliveTracker.this.mConnectivityServiceHandler.obtainMessage(528396, this.mSlot, -10, this.mNai.network).sendToTarget();
        }

        void unlinkDeathRecipient() {
            if (this.mBinder != null) {
                this.mBinder.unlinkToDeath(this, 0);
            }
        }

        private int checkNetworkConnected() {
            if (!this.mNai.networkInfo.isConnectedOrConnecting()) {
                return -20;
            }
            return 0;
        }

        private int checkSourceAddress() {
            Iterator it = this.mNai.linkProperties.getAddresses().iterator();
            while (it.hasNext()) {
                if (((InetAddress) it.next()).equals(this.mPacket.srcAddress)) {
                    return 0;
                }
            }
            return -21;
        }

        private int checkInterval() {
            return this.mInterval >= 10 ? 0 : -24;
        }

        private int isValid() {
            int iCheckInterval;
            synchronized (this.mNai) {
                iCheckInterval = checkInterval();
                if (iCheckInterval == 0) {
                    iCheckInterval = checkNetworkConnected();
                }
                if (iCheckInterval == 0) {
                    iCheckInterval = checkSourceAddress();
                }
            }
            return iCheckInterval;
        }

        void start(int i) {
            int iIsValid = isValid();
            if (iIsValid == 0) {
                this.mSlot = i;
                Log.d(KeepaliveTracker.TAG, "Starting keepalive " + this.mSlot + " on " + this.mNai.name());
                this.mNai.asyncChannel.sendMessage(528395, i, this.mInterval, this.mPacket);
                return;
            }
            notifyMessenger(-1, iIsValid);
        }

        void stop(int i) {
            if (Binder.getCallingUid() != this.mUid) {
            }
            if (this.isStarted) {
                Log.d(KeepaliveTracker.TAG, "Stopping keepalive " + this.mSlot + " on " + this.mNai.name());
                this.mNai.asyncChannel.sendMessage(528396, this.mSlot);
            }
            notifyMessenger(this.mSlot, i);
            unlinkDeathRecipient();
        }
    }

    void notifyMessenger(Messenger messenger, int i, int i2) {
        Message messageObtain = Message.obtain();
        messageObtain.what = 528397;
        messageObtain.arg1 = i;
        messageObtain.arg2 = i2;
        messageObtain.obj = null;
        try {
            messenger.send(messageObtain);
        } catch (RemoteException e) {
        }
    }

    private int findFirstFreeSlot(NetworkAgentInfo networkAgentInfo) {
        HashMap<Integer, KeepaliveInfo> map = this.mKeepalives.get(networkAgentInfo);
        if (map == null) {
            map = new HashMap<>();
            this.mKeepalives.put(networkAgentInfo, map);
        }
        int i = 1;
        while (i <= map.size()) {
            if (map.get(Integer.valueOf(i)) != null) {
                i++;
            } else {
                return i;
            }
        }
        return i;
    }

    public void handleStartKeepalive(Message message) {
        KeepaliveInfo keepaliveInfo = (KeepaliveInfo) message.obj;
        NetworkAgentInfo nai = keepaliveInfo.getNai();
        int iFindFirstFreeSlot = findFirstFreeSlot(nai);
        this.mKeepalives.get(nai).put(Integer.valueOf(iFindFirstFreeSlot), keepaliveInfo);
        keepaliveInfo.start(iFindFirstFreeSlot);
    }

    public void handleStopAllKeepalives(NetworkAgentInfo networkAgentInfo, int i) {
        HashMap<Integer, KeepaliveInfo> map = this.mKeepalives.get(networkAgentInfo);
        if (map != null) {
            Iterator<KeepaliveInfo> it = map.values().iterator();
            while (it.hasNext()) {
                it.next().stop(i);
            }
            map.clear();
            this.mKeepalives.remove(networkAgentInfo);
        }
    }

    public void handleStopKeepalive(NetworkAgentInfo networkAgentInfo, int i, int i2) {
        String strName = networkAgentInfo == null ? "(null)" : networkAgentInfo.name();
        HashMap<Integer, KeepaliveInfo> map = this.mKeepalives.get(networkAgentInfo);
        if (map == null) {
            Log.e(TAG, "Attempt to stop keepalive on nonexistent network " + strName);
            return;
        }
        KeepaliveInfo keepaliveInfo = map.get(Integer.valueOf(i));
        if (keepaliveInfo == null) {
            Log.e(TAG, "Attempt to stop nonexistent keepalive " + i + " on " + strName);
            return;
        }
        keepaliveInfo.stop(i2);
        map.remove(Integer.valueOf(i));
        if (map.isEmpty()) {
            this.mKeepalives.remove(networkAgentInfo);
        }
    }

    public void handleCheckKeepalivesStillValid(NetworkAgentInfo networkAgentInfo) {
        HashMap<Integer, KeepaliveInfo> map = this.mKeepalives.get(networkAgentInfo);
        if (map != null) {
            ArrayList<Pair> arrayList = new ArrayList();
            Iterator<Integer> it = map.keySet().iterator();
            while (it.hasNext()) {
                int iIntValue = it.next().intValue();
                int iIsValid = map.get(Integer.valueOf(iIntValue)).isValid();
                if (iIsValid != 0) {
                    arrayList.add(Pair.create(Integer.valueOf(iIntValue), Integer.valueOf(iIsValid)));
                }
            }
            for (Pair pair : arrayList) {
                handleStopKeepalive(networkAgentInfo, ((Integer) pair.first).intValue(), ((Integer) pair.second).intValue());
            }
        }
    }

    public void handleEventPacketKeepalive(NetworkAgentInfo networkAgentInfo, Message message) {
        KeepaliveInfo keepaliveInfo;
        int i = message.arg1;
        int i2 = message.arg2;
        try {
            keepaliveInfo = this.mKeepalives.get(networkAgentInfo).get(Integer.valueOf(i));
        } catch (NullPointerException e) {
            keepaliveInfo = null;
        }
        if (keepaliveInfo == null) {
            Log.e(TAG, "Event for unknown keepalive " + i + " on " + networkAgentInfo.name());
            return;
        }
        if (i2 == 0 && !keepaliveInfo.isStarted) {
            keepaliveInfo.isStarted = true;
            keepaliveInfo.notifyMessenger(i, i2);
        } else {
            keepaliveInfo.isStarted = false;
            handleStopKeepalive(networkAgentInfo, i, i2);
        }
    }

    public void startNattKeepalive(NetworkAgentInfo networkAgentInfo, int i, Messenger messenger, IBinder iBinder, String str, int i2, String str2, int i3) {
        if (networkAgentInfo == null) {
            notifyMessenger(messenger, -1, -20);
            return;
        }
        try {
            try {
                KeepaliveInfo keepaliveInfo = new KeepaliveInfo(messenger, iBinder, networkAgentInfo, KeepalivePacketData.nattKeepalivePacket(NetworkUtils.numericToInetAddress(str), i2, NetworkUtils.numericToInetAddress(str2), 4500), i);
                Log.d(TAG, "Created keepalive: " + keepaliveInfo.toString());
                this.mConnectivityServiceHandler.obtainMessage(528395, keepaliveInfo).sendToTarget();
            } catch (KeepalivePacketData.InvalidPacketException e) {
                notifyMessenger(messenger, -1, e.error);
            }
        } catch (IllegalArgumentException e2) {
            notifyMessenger(messenger, -1, -21);
        }
    }

    public void dump(IndentingPrintWriter indentingPrintWriter) {
        indentingPrintWriter.println("Packet keepalives:");
        indentingPrintWriter.increaseIndent();
        for (NetworkAgentInfo networkAgentInfo : this.mKeepalives.keySet()) {
            indentingPrintWriter.println(networkAgentInfo.name());
            indentingPrintWriter.increaseIndent();
            Iterator<Integer> it = this.mKeepalives.get(networkAgentInfo).keySet().iterator();
            while (it.hasNext()) {
                int iIntValue = it.next().intValue();
                indentingPrintWriter.println(iIntValue + ": " + this.mKeepalives.get(networkAgentInfo).get(Integer.valueOf(iIntValue)).toString());
            }
            indentingPrintWriter.decreaseIndent();
        }
        indentingPrintWriter.decreaseIndent();
    }
}
