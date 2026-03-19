package com.mediatek.location;

import com.mediatek.socket.base.SocketUtils;
import com.mediatek.socket.base.UdpClient;

public class Agps2FrameworkInterface {
    public static final int ACQUIRE_WAKE_LOCK = 1;
    public static final int IS_EXIST = 0;
    public static final int MAX_BUFF_SIZE = 271;
    public static final int PROTOCOL_TYPE = 301;
    public static final int RELEASE_DEDICATED_APN = 4;
    public static final int RELEASE_WAKE_LOCK = 2;
    public static final int REMOVE_GPS_ICON = 6;
    public static final int REQUEST_DEDICATED_APN_AND_DNS_QUERY = 3;
    public static final int REQUEST_GPS_ICON = 5;

    public static class Agps2FrameworkInterfaceSender {
        public boolean isExist(UdpClient client) {
            if (!client.connect()) {
                return false;
            }
            SocketUtils.BaseBuffer buff = client.getBuff();
            buff.putInt(Agps2FrameworkInterface.PROTOCOL_TYPE);
            buff.putInt(0);
            boolean _ret = client.write();
            client.close();
            return _ret;
        }

        public boolean acquireWakeLock(UdpClient client) {
            if (!client.connect()) {
                return false;
            }
            SocketUtils.BaseBuffer buff = client.getBuff();
            buff.putInt(Agps2FrameworkInterface.PROTOCOL_TYPE);
            buff.putInt(1);
            boolean _ret = client.write();
            client.close();
            return _ret;
        }

        public boolean releaseWakeLock(UdpClient client) {
            if (!client.connect()) {
                return false;
            }
            SocketUtils.BaseBuffer buff = client.getBuff();
            buff.putInt(Agps2FrameworkInterface.PROTOCOL_TYPE);
            buff.putInt(2);
            boolean _ret = client.write();
            client.close();
            return _ret;
        }

        public boolean requestDedicatedApnAndDnsQuery(UdpClient client, String fqdn, boolean isEmergencySupl, boolean isApnEnabled) {
            if (!client.connect()) {
                return false;
            }
            SocketUtils.BaseBuffer buff = client.getBuff();
            buff.putInt(Agps2FrameworkInterface.PROTOCOL_TYPE);
            buff.putInt(3);
            SocketUtils.assertSize(fqdn, 256, 0);
            buff.putString(fqdn);
            buff.putBool(isEmergencySupl);
            buff.putBool(isApnEnabled);
            boolean _ret = client.write();
            client.close();
            return _ret;
        }

        public boolean releaseDedicatedApn(UdpClient client) {
            if (!client.connect()) {
                return false;
            }
            SocketUtils.BaseBuffer buff = client.getBuff();
            buff.putInt(Agps2FrameworkInterface.PROTOCOL_TYPE);
            buff.putInt(4);
            boolean _ret = client.write();
            client.close();
            return _ret;
        }

        public boolean requestGpsIcon(UdpClient client) {
            if (!client.connect()) {
                return false;
            }
            SocketUtils.BaseBuffer buff = client.getBuff();
            buff.putInt(Agps2FrameworkInterface.PROTOCOL_TYPE);
            buff.putInt(5);
            boolean _ret = client.write();
            client.close();
            return _ret;
        }

        public boolean removeGpsIcon(UdpClient client) {
            if (!client.connect()) {
                return false;
            }
            SocketUtils.BaseBuffer buff = client.getBuff();
            buff.putInt(Agps2FrameworkInterface.PROTOCOL_TYPE);
            buff.putInt(6);
            boolean _ret = client.write();
            client.close();
            return _ret;
        }
    }

    public static abstract class Agps2FrameworkInterfaceReceiver implements SocketUtils.ProtocolHandler {
        public abstract void acquireWakeLock();

        public abstract void isExist();

        public abstract void releaseDedicatedApn();

        public abstract void releaseWakeLock();

        public abstract void removeGpsIcon();

        public abstract void requestDedicatedApnAndDnsQuery(String str, boolean z, boolean z2);

        public abstract void requestGpsIcon();

        public boolean readAndDecode(SocketUtils.UdpServerInterface server) {
            if (!server.read()) {
                return false;
            }
            return decode(server);
        }

        @Override
        public int getProtocolType() {
            return Agps2FrameworkInterface.PROTOCOL_TYPE;
        }

        @Override
        public boolean decode(SocketUtils.UdpServerInterface server) {
            SocketUtils.BaseBuffer buff = server.getBuff();
            buff.setOffset(4);
            int _type = buff.getInt();
            switch (_type) {
                case 0:
                    isExist();
                    break;
                case 1:
                    acquireWakeLock();
                    break;
                case 2:
                    releaseWakeLock();
                    break;
                case 3:
                    String fqdn = buff.getString();
                    boolean isEmergencySupl = buff.getBool();
                    boolean isApnEnabled = buff.getBool();
                    requestDedicatedApnAndDnsQuery(fqdn, isEmergencySupl, isApnEnabled);
                    break;
                case 4:
                    releaseDedicatedApn();
                    break;
                case 5:
                    requestGpsIcon();
                    break;
                case 6:
                    removeGpsIcon();
                    break;
            }
            return true;
        }
    }
}
