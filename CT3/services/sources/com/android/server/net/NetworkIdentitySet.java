package com.android.server.net;

import android.net.NetworkIdentity;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.HashSet;

public class NetworkIdentitySet extends HashSet<NetworkIdentity> implements Comparable<NetworkIdentitySet> {
    private static final int VERSION_ADD_METERED = 4;
    private static final int VERSION_ADD_NETWORK_ID = 3;
    private static final int VERSION_ADD_ROAMING = 2;
    private static final int VERSION_INIT = 1;

    public NetworkIdentitySet() {
    }

    public NetworkIdentitySet(DataInputStream in) throws IOException {
        String optionalString;
        boolean z;
        boolean metered;
        int version = in.readInt();
        int size = in.readInt();
        for (int i = 0; i < size; i++) {
            if (version <= 1) {
                in.readInt();
            }
            int type = in.readInt();
            int subType = in.readInt();
            String subscriberId = readOptionalString(in);
            if (version >= 3) {
                optionalString = readOptionalString(in);
            } else {
                optionalString = null;
            }
            if (version >= 2) {
                z = in.readBoolean();
            } else {
                z = false;
            }
            if (version >= 4) {
                metered = in.readBoolean();
            } else {
                metered = type == 0;
            }
            add(new NetworkIdentity(type, subType, subscriberId, optionalString, z, metered));
        }
    }

    public void writeToStream(DataOutputStream out) throws IOException {
        out.writeInt(4);
        out.writeInt(size());
        for (NetworkIdentity ident : this) {
            out.writeInt(ident.getType());
            out.writeInt(ident.getSubType());
            writeOptionalString(out, ident.getSubscriberId());
            writeOptionalString(out, ident.getNetworkId());
            out.writeBoolean(ident.getRoaming());
            out.writeBoolean(ident.getMetered());
        }
    }

    public boolean isAnyMemberRoaming() {
        if (isEmpty()) {
            return false;
        }
        for (NetworkIdentity ident : this) {
            if (ident.getRoaming()) {
                return true;
            }
        }
        return false;
    }

    private static void writeOptionalString(DataOutputStream out, String value) throws IOException {
        if (value != null) {
            out.writeByte(1);
            out.writeUTF(value);
        } else {
            out.writeByte(0);
        }
    }

    private static String readOptionalString(DataInputStream in) throws IOException {
        if (in.readByte() != 0) {
            return in.readUTF();
        }
        return null;
    }

    @Override
    public int compareTo(NetworkIdentitySet another) {
        if (isEmpty()) {
            return -1;
        }
        if (another.isEmpty()) {
            return 1;
        }
        NetworkIdentity ident = iterator().next();
        NetworkIdentity anotherIdent = another.iterator().next();
        return ident.compareTo(anotherIdent);
    }
}
