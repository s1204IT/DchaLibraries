package com.android.server.net;

import android.net.NetworkIdentity;
import android.util.proto.ProtoOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.HashSet;
import java.util.Iterator;

public class NetworkIdentitySet extends HashSet<NetworkIdentity> implements Comparable<NetworkIdentitySet> {
    private static final int VERSION_ADD_DEFAULT_NETWORK = 5;
    private static final int VERSION_ADD_METERED = 4;
    private static final int VERSION_ADD_NETWORK_ID = 3;
    private static final int VERSION_ADD_ROAMING = 2;
    private static final int VERSION_INIT = 1;

    public NetworkIdentitySet() {
    }

    public NetworkIdentitySet(DataInputStream dataInputStream) throws IOException {
        String optionalString;
        boolean z;
        boolean z2;
        int i = dataInputStream.readInt();
        int i2 = dataInputStream.readInt();
        for (int i3 = 0; i3 < i2; i3++) {
            if (i <= 1) {
                dataInputStream.readInt();
            }
            int i4 = dataInputStream.readInt();
            int i5 = dataInputStream.readInt();
            String optionalString2 = readOptionalString(dataInputStream);
            if (i >= 3) {
                optionalString = readOptionalString(dataInputStream);
            } else {
                optionalString = null;
            }
            String str = optionalString;
            if (i >= 2) {
                z = dataInputStream.readBoolean();
            } else {
                z = false;
            }
            if (i >= 4) {
                z2 = dataInputStream.readBoolean();
            } else {
                z2 = i4 == 0;
            }
            add(new NetworkIdentity(i4, i5, optionalString2, str, z, z2, i >= 5 ? dataInputStream.readBoolean() : true));
        }
    }

    public void writeToStream(DataOutputStream dataOutputStream) throws IOException {
        dataOutputStream.writeInt(5);
        dataOutputStream.writeInt(size());
        for (NetworkIdentity networkIdentity : this) {
            dataOutputStream.writeInt(networkIdentity.getType());
            dataOutputStream.writeInt(networkIdentity.getSubType());
            writeOptionalString(dataOutputStream, networkIdentity.getSubscriberId());
            writeOptionalString(dataOutputStream, networkIdentity.getNetworkId());
            dataOutputStream.writeBoolean(networkIdentity.getRoaming());
            dataOutputStream.writeBoolean(networkIdentity.getMetered());
            dataOutputStream.writeBoolean(networkIdentity.getDefaultNetwork());
        }
    }

    public boolean isAnyMemberMetered() {
        if (isEmpty()) {
            return false;
        }
        Iterator<NetworkIdentity> it = iterator();
        while (it.hasNext()) {
            if (it.next().getMetered()) {
                return true;
            }
        }
        return false;
    }

    public boolean isAnyMemberRoaming() {
        if (isEmpty()) {
            return false;
        }
        Iterator<NetworkIdentity> it = iterator();
        while (it.hasNext()) {
            if (it.next().getRoaming()) {
                return true;
            }
        }
        return false;
    }

    public boolean areAllMembersOnDefaultNetwork() {
        if (isEmpty()) {
            return true;
        }
        Iterator<NetworkIdentity> it = iterator();
        while (it.hasNext()) {
            if (!it.next().getDefaultNetwork()) {
                return false;
            }
        }
        return true;
    }

    private static void writeOptionalString(DataOutputStream dataOutputStream, String str) throws IOException {
        if (str != null) {
            dataOutputStream.writeByte(1);
            dataOutputStream.writeUTF(str);
        } else {
            dataOutputStream.writeByte(0);
        }
    }

    private static String readOptionalString(DataInputStream dataInputStream) throws IOException {
        if (dataInputStream.readByte() != 0) {
            return dataInputStream.readUTF();
        }
        return null;
    }

    @Override
    public int compareTo(NetworkIdentitySet networkIdentitySet) {
        if (isEmpty()) {
            return -1;
        }
        if (networkIdentitySet.isEmpty()) {
            return 1;
        }
        return iterator().next().compareTo(networkIdentitySet.iterator().next());
    }

    public void writeToProto(ProtoOutputStream protoOutputStream, long j) {
        long jStart = protoOutputStream.start(j);
        Iterator<NetworkIdentity> it = iterator();
        while (it.hasNext()) {
            it.next().writeToProto(protoOutputStream, 2246267895809L);
        }
        protoOutputStream.end(jStart);
    }
}
