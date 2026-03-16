package android.os;

import android.net.INetworkManagementEventObserver;
import android.net.InterfaceConfiguration;
import android.net.Network;
import android.net.NetworkStats;
import android.net.RouteInfo;
import android.net.UidRange;
import android.net.wifi.WifiConfiguration;
import android.os.INetworkActivityListener;
import java.util.List;

public interface INetworkManagementService extends IInterface {
    void addIdleTimer(String str, int i, int i2) throws RemoteException;

    void addInterfaceToLocalNetwork(String str, List<RouteInfo> list) throws RemoteException;

    void addInterfaceToNetwork(String str, int i) throws RemoteException;

    void addLegacyRouteForNetId(int i, RouteInfo routeInfo, int i2) throws RemoteException;

    void addRoute(int i, RouteInfo routeInfo) throws RemoteException;

    void addVpnUidRanges(int i, UidRange[] uidRangeArr) throws RemoteException;

    void allowProtect(int i) throws RemoteException;

    void attachPppd(String str, String str2, String str3, String str4, String str5) throws RemoteException;

    void clearDefaultNetId() throws RemoteException;

    void clearInterfaceAddresses(String str) throws RemoteException;

    void clearPermission(int[] iArr) throws RemoteException;

    void createPhysicalNetwork(int i) throws RemoteException;

    void createVirtualNetwork(int i, boolean z, boolean z2) throws RemoteException;

    void denyProtect(int i) throws RemoteException;

    void detachPppd(String str) throws RemoteException;

    void disableIpv6(String str) throws RemoteException;

    void disableNat(String str, String str2) throws RemoteException;

    void enableIpv6(String str) throws RemoteException;

    void enableNat(String str, String str2) throws RemoteException;

    void flushNetworkDnsCache(int i) throws RemoteException;

    String[] getDnsForwarders() throws RemoteException;

    boolean getIPv6ForwardingEnabled() throws RemoteException;

    InterfaceConfiguration getInterfaceConfig(String str) throws RemoteException;

    boolean getIpForwardingEnabled() throws RemoteException;

    NetworkStats getNetworkStatsDetail() throws RemoteException;

    NetworkStats getNetworkStatsSummaryDev() throws RemoteException;

    NetworkStats getNetworkStatsSummaryXt() throws RemoteException;

    NetworkStats getNetworkStatsTethering() throws RemoteException;

    NetworkStats getNetworkStatsUidDetail(int i) throws RemoteException;

    RouteInfo[] getRoutes(String str) throws RemoteException;

    boolean isBandwidthControlEnabled() throws RemoteException;

    boolean isClatdStarted(String str) throws RemoteException;

    boolean isFirewallEnabled() throws RemoteException;

    boolean isIPv6TetheringStarted() throws RemoteException;

    boolean isNetworkActive() throws RemoteException;

    boolean isTetheringStarted() throws RemoteException;

    String[] listInterfaces() throws RemoteException;

    String[] listTetheredInterfaces() throws RemoteException;

    String[] listTtys() throws RemoteException;

    void registerNetworkActivityListener(INetworkActivityListener iNetworkActivityListener) throws RemoteException;

    void registerObserver(INetworkManagementEventObserver iNetworkManagementEventObserver) throws RemoteException;

    void removeIdleTimer(String str) throws RemoteException;

    void removeInterfaceAlert(String str) throws RemoteException;

    void removeInterfaceFromLocalNetwork(String str) throws RemoteException;

    void removeInterfaceFromNetwork(String str, int i) throws RemoteException;

    void removeInterfaceQuota(String str) throws RemoteException;

    void removeNetwork(int i) throws RemoteException;

    void removeRoute(int i, RouteInfo routeInfo) throws RemoteException;

    void removeVpnUidRanges(int i, UidRange[] uidRangeArr) throws RemoteException;

    void setAccessPoint(WifiConfiguration wifiConfiguration, String str) throws RemoteException;

    void setDefaultNetId(int i) throws RemoteException;

    void setDnsForwarders(Network network, String[] strArr) throws RemoteException;

    void setDnsServersForNetwork(int i, String[] strArr, String str) throws RemoteException;

    void setFirewallEgressDestRule(String str, int i, boolean z) throws RemoteException;

    void setFirewallEgressSourceRule(String str, boolean z) throws RemoteException;

    void setFirewallEnabled(boolean z) throws RemoteException;

    void setFirewallInterfaceRule(String str, boolean z) throws RemoteException;

    void setFirewallUidRule(int i, boolean z) throws RemoteException;

    void setGlobalAlert(long j) throws RemoteException;

    void setIPv6ForwardingEnabled(boolean z) throws RemoteException;

    void setInterfaceAlert(String str, long j) throws RemoteException;

    void setInterfaceConfig(String str, InterfaceConfiguration interfaceConfiguration) throws RemoteException;

    void setInterfaceDown(String str) throws RemoteException;

    void setInterfaceIpv6NdOffload(String str, boolean z) throws RemoteException;

    void setInterfaceIpv6PrivacyExtensions(String str, boolean z) throws RemoteException;

    void setInterfaceQuota(String str, long j) throws RemoteException;

    void setInterfaceUp(String str) throws RemoteException;

    void setIpForwardingEnabled(boolean z) throws RemoteException;

    void setMtu(String str, int i) throws RemoteException;

    void setPermission(String str, int[] iArr) throws RemoteException;

    void setUidNetworkRules(int i, boolean z) throws RemoteException;

    void shutdown() throws RemoteException;

    void startAccessPoint(WifiConfiguration wifiConfiguration, String str) throws RemoteException;

    void startClatd(String str) throws RemoteException;

    void startIPv6Tethering(String str, String str2, String str3) throws RemoteException;

    void startTethering(String[] strArr) throws RemoteException;

    void stopAccessPoint(String str) throws RemoteException;

    void stopClatd(String str) throws RemoteException;

    void stopIPv6Tethering(String str, String str2, String str3) throws RemoteException;

    void stopTethering() throws RemoteException;

    void tetherInterface(String str) throws RemoteException;

    void unregisterNetworkActivityListener(INetworkActivityListener iNetworkActivityListener) throws RemoteException;

    void unregisterObserver(INetworkManagementEventObserver iNetworkManagementEventObserver) throws RemoteException;

    void untetherInterface(String str) throws RemoteException;

    void wifiFirmwareReload(String str, String str2) throws RemoteException;

    public static abstract class Stub extends Binder implements INetworkManagementService {
        private static final String DESCRIPTOR = "android.os.INetworkManagementService";
        static final int TRANSACTION_addIdleTimer = 49;
        static final int TRANSACTION_addInterfaceToLocalNetwork = 79;
        static final int TRANSACTION_addInterfaceToNetwork = 70;
        static final int TRANSACTION_addLegacyRouteForNetId = 72;
        static final int TRANSACTION_addRoute = 14;
        static final int TRANSACTION_addVpnUidRanges = 59;
        static final int TRANSACTION_allowProtect = 77;
        static final int TRANSACTION_attachPppd = 31;
        static final int TRANSACTION_clearDefaultNetId = 74;
        static final int TRANSACTION_clearInterfaceAddresses = 6;
        static final int TRANSACTION_clearPermission = 76;
        static final int TRANSACTION_createPhysicalNetwork = 67;
        static final int TRANSACTION_createVirtualNetwork = 68;
        static final int TRANSACTION_denyProtect = 78;
        static final int TRANSACTION_detachPppd = 32;
        static final int TRANSACTION_disableIpv6 = 10;
        static final int TRANSACTION_disableNat = 29;
        static final int TRANSACTION_enableIpv6 = 11;
        static final int TRANSACTION_enableNat = 28;
        static final int TRANSACTION_flushNetworkDnsCache = 52;
        static final int TRANSACTION_getDnsForwarders = 27;
        static final int TRANSACTION_getIPv6ForwardingEnabled = 81;
        static final int TRANSACTION_getInterfaceConfig = 4;
        static final int TRANSACTION_getIpForwardingEnabled = 18;
        static final int TRANSACTION_getNetworkStatsDetail = 39;
        static final int TRANSACTION_getNetworkStatsSummaryDev = 37;
        static final int TRANSACTION_getNetworkStatsSummaryXt = 38;
        static final int TRANSACTION_getNetworkStatsTethering = 41;
        static final int TRANSACTION_getNetworkStatsUidDetail = 40;
        static final int TRANSACTION_getRoutes = 13;
        static final int TRANSACTION_isBandwidthControlEnabled = 48;
        static final int TRANSACTION_isClatdStarted = 63;
        static final int TRANSACTION_isFirewallEnabled = 54;
        static final int TRANSACTION_isIPv6TetheringStarted = 85;
        static final int TRANSACTION_isNetworkActive = 66;
        static final int TRANSACTION_isTetheringStarted = 22;
        static final int TRANSACTION_listInterfaces = 3;
        static final int TRANSACTION_listTetheredInterfaces = 25;
        static final int TRANSACTION_listTtys = 30;
        static final int TRANSACTION_registerNetworkActivityListener = 64;
        static final int TRANSACTION_registerObserver = 1;
        static final int TRANSACTION_removeIdleTimer = 50;
        static final int TRANSACTION_removeInterfaceAlert = 45;
        static final int TRANSACTION_removeInterfaceFromLocalNetwork = 80;
        static final int TRANSACTION_removeInterfaceFromNetwork = 71;
        static final int TRANSACTION_removeInterfaceQuota = 43;
        static final int TRANSACTION_removeNetwork = 69;
        static final int TRANSACTION_removeRoute = 15;
        static final int TRANSACTION_removeVpnUidRanges = 60;
        static final int TRANSACTION_setAccessPoint = 36;
        static final int TRANSACTION_setDefaultNetId = 73;
        static final int TRANSACTION_setDnsForwarders = 26;
        static final int TRANSACTION_setDnsServersForNetwork = 51;
        static final int TRANSACTION_setFirewallEgressDestRule = 57;
        static final int TRANSACTION_setFirewallEgressSourceRule = 56;
        static final int TRANSACTION_setFirewallEnabled = 53;
        static final int TRANSACTION_setFirewallInterfaceRule = 55;
        static final int TRANSACTION_setFirewallUidRule = 58;
        static final int TRANSACTION_setGlobalAlert = 46;
        static final int TRANSACTION_setIPv6ForwardingEnabled = 82;
        static final int TRANSACTION_setInterfaceAlert = 44;
        static final int TRANSACTION_setInterfaceConfig = 5;
        static final int TRANSACTION_setInterfaceDown = 7;
        static final int TRANSACTION_setInterfaceIpv6NdOffload = 12;
        static final int TRANSACTION_setInterfaceIpv6PrivacyExtensions = 9;
        static final int TRANSACTION_setInterfaceQuota = 42;
        static final int TRANSACTION_setInterfaceUp = 8;
        static final int TRANSACTION_setIpForwardingEnabled = 19;
        static final int TRANSACTION_setMtu = 16;
        static final int TRANSACTION_setPermission = 75;
        static final int TRANSACTION_setUidNetworkRules = 47;
        static final int TRANSACTION_shutdown = 17;
        static final int TRANSACTION_startAccessPoint = 34;
        static final int TRANSACTION_startClatd = 61;
        static final int TRANSACTION_startIPv6Tethering = 83;
        static final int TRANSACTION_startTethering = 20;
        static final int TRANSACTION_stopAccessPoint = 35;
        static final int TRANSACTION_stopClatd = 62;
        static final int TRANSACTION_stopIPv6Tethering = 84;
        static final int TRANSACTION_stopTethering = 21;
        static final int TRANSACTION_tetherInterface = 23;
        static final int TRANSACTION_unregisterNetworkActivityListener = 65;
        static final int TRANSACTION_unregisterObserver = 2;
        static final int TRANSACTION_untetherInterface = 24;
        static final int TRANSACTION_wifiFirmwareReload = 33;

        public Stub() {
            attachInterface(this, DESCRIPTOR);
        }

        public static INetworkManagementService asInterface(IBinder obj) {
            if (obj == null) {
                return null;
            }
            IInterface iin = obj.queryLocalInterface(DESCRIPTOR);
            if (iin != null && (iin instanceof INetworkManagementService)) {
                return (INetworkManagementService) iin;
            }
            return new Proxy(obj);
        }

        @Override
        public IBinder asBinder() {
            return this;
        }

        @Override
        public boolean onTransact(int code, Parcel data, Parcel reply, int flags) throws RemoteException {
            RouteInfo _arg1;
            WifiConfiguration _arg0;
            WifiConfiguration _arg02;
            Network _arg03;
            RouteInfo _arg12;
            RouteInfo _arg13;
            InterfaceConfiguration _arg14;
            switch (code) {
                case 1:
                    data.enforceInterface(DESCRIPTOR);
                    INetworkManagementEventObserver _arg04 = INetworkManagementEventObserver.Stub.asInterface(data.readStrongBinder());
                    registerObserver(_arg04);
                    reply.writeNoException();
                    return true;
                case 2:
                    data.enforceInterface(DESCRIPTOR);
                    INetworkManagementEventObserver _arg05 = INetworkManagementEventObserver.Stub.asInterface(data.readStrongBinder());
                    unregisterObserver(_arg05);
                    reply.writeNoException();
                    return true;
                case 3:
                    data.enforceInterface(DESCRIPTOR);
                    String[] _result = listInterfaces();
                    reply.writeNoException();
                    reply.writeStringArray(_result);
                    return true;
                case 4:
                    data.enforceInterface(DESCRIPTOR);
                    String _arg06 = data.readString();
                    InterfaceConfiguration _result2 = getInterfaceConfig(_arg06);
                    reply.writeNoException();
                    if (_result2 != null) {
                        reply.writeInt(1);
                        _result2.writeToParcel(reply, 1);
                    } else {
                        reply.writeInt(0);
                    }
                    return true;
                case 5:
                    data.enforceInterface(DESCRIPTOR);
                    String _arg07 = data.readString();
                    if (data.readInt() != 0) {
                        _arg14 = InterfaceConfiguration.CREATOR.createFromParcel(data);
                    } else {
                        _arg14 = null;
                    }
                    setInterfaceConfig(_arg07, _arg14);
                    reply.writeNoException();
                    return true;
                case 6:
                    data.enforceInterface(DESCRIPTOR);
                    String _arg08 = data.readString();
                    clearInterfaceAddresses(_arg08);
                    reply.writeNoException();
                    return true;
                case 7:
                    data.enforceInterface(DESCRIPTOR);
                    String _arg09 = data.readString();
                    setInterfaceDown(_arg09);
                    reply.writeNoException();
                    return true;
                case 8:
                    data.enforceInterface(DESCRIPTOR);
                    String _arg010 = data.readString();
                    setInterfaceUp(_arg010);
                    reply.writeNoException();
                    return true;
                case 9:
                    data.enforceInterface(DESCRIPTOR);
                    String _arg011 = data.readString();
                    boolean _arg15 = data.readInt() != 0;
                    setInterfaceIpv6PrivacyExtensions(_arg011, _arg15);
                    reply.writeNoException();
                    return true;
                case 10:
                    data.enforceInterface(DESCRIPTOR);
                    String _arg012 = data.readString();
                    disableIpv6(_arg012);
                    reply.writeNoException();
                    return true;
                case 11:
                    data.enforceInterface(DESCRIPTOR);
                    String _arg013 = data.readString();
                    enableIpv6(_arg013);
                    reply.writeNoException();
                    return true;
                case 12:
                    data.enforceInterface(DESCRIPTOR);
                    String _arg014 = data.readString();
                    boolean _arg16 = data.readInt() != 0;
                    setInterfaceIpv6NdOffload(_arg014, _arg16);
                    reply.writeNoException();
                    return true;
                case 13:
                    data.enforceInterface(DESCRIPTOR);
                    String _arg015 = data.readString();
                    RouteInfo[] _result3 = getRoutes(_arg015);
                    reply.writeNoException();
                    reply.writeTypedArray(_result3, 1);
                    return true;
                case 14:
                    data.enforceInterface(DESCRIPTOR);
                    int _arg016 = data.readInt();
                    if (data.readInt() != 0) {
                        _arg13 = RouteInfo.CREATOR.createFromParcel(data);
                    } else {
                        _arg13 = null;
                    }
                    addRoute(_arg016, _arg13);
                    reply.writeNoException();
                    return true;
                case 15:
                    data.enforceInterface(DESCRIPTOR);
                    int _arg017 = data.readInt();
                    if (data.readInt() != 0) {
                        _arg12 = RouteInfo.CREATOR.createFromParcel(data);
                    } else {
                        _arg12 = null;
                    }
                    removeRoute(_arg017, _arg12);
                    reply.writeNoException();
                    return true;
                case 16:
                    data.enforceInterface(DESCRIPTOR);
                    String _arg018 = data.readString();
                    int _arg17 = data.readInt();
                    setMtu(_arg018, _arg17);
                    reply.writeNoException();
                    return true;
                case 17:
                    data.enforceInterface(DESCRIPTOR);
                    shutdown();
                    reply.writeNoException();
                    return true;
                case 18:
                    data.enforceInterface(DESCRIPTOR);
                    boolean _result4 = getIpForwardingEnabled();
                    reply.writeNoException();
                    reply.writeInt(_result4 ? 1 : 0);
                    return true;
                case 19:
                    data.enforceInterface(DESCRIPTOR);
                    boolean _arg019 = data.readInt() != 0;
                    setIpForwardingEnabled(_arg019);
                    reply.writeNoException();
                    return true;
                case 20:
                    data.enforceInterface(DESCRIPTOR);
                    String[] _arg020 = data.createStringArray();
                    startTethering(_arg020);
                    reply.writeNoException();
                    return true;
                case 21:
                    data.enforceInterface(DESCRIPTOR);
                    stopTethering();
                    reply.writeNoException();
                    return true;
                case 22:
                    data.enforceInterface(DESCRIPTOR);
                    boolean _result5 = isTetheringStarted();
                    reply.writeNoException();
                    reply.writeInt(_result5 ? 1 : 0);
                    return true;
                case 23:
                    data.enforceInterface(DESCRIPTOR);
                    String _arg021 = data.readString();
                    tetherInterface(_arg021);
                    reply.writeNoException();
                    return true;
                case 24:
                    data.enforceInterface(DESCRIPTOR);
                    String _arg022 = data.readString();
                    untetherInterface(_arg022);
                    reply.writeNoException();
                    return true;
                case 25:
                    data.enforceInterface(DESCRIPTOR);
                    String[] _result6 = listTetheredInterfaces();
                    reply.writeNoException();
                    reply.writeStringArray(_result6);
                    return true;
                case 26:
                    data.enforceInterface(DESCRIPTOR);
                    if (data.readInt() != 0) {
                        _arg03 = Network.CREATOR.createFromParcel(data);
                    } else {
                        _arg03 = null;
                    }
                    String[] _arg18 = data.createStringArray();
                    setDnsForwarders(_arg03, _arg18);
                    reply.writeNoException();
                    return true;
                case 27:
                    data.enforceInterface(DESCRIPTOR);
                    String[] _result7 = getDnsForwarders();
                    reply.writeNoException();
                    reply.writeStringArray(_result7);
                    return true;
                case 28:
                    data.enforceInterface(DESCRIPTOR);
                    String _arg023 = data.readString();
                    String _arg19 = data.readString();
                    enableNat(_arg023, _arg19);
                    reply.writeNoException();
                    return true;
                case 29:
                    data.enforceInterface(DESCRIPTOR);
                    String _arg024 = data.readString();
                    String _arg110 = data.readString();
                    disableNat(_arg024, _arg110);
                    reply.writeNoException();
                    return true;
                case 30:
                    data.enforceInterface(DESCRIPTOR);
                    String[] _result8 = listTtys();
                    reply.writeNoException();
                    reply.writeStringArray(_result8);
                    return true;
                case 31:
                    data.enforceInterface(DESCRIPTOR);
                    String _arg025 = data.readString();
                    String _arg111 = data.readString();
                    String _arg2 = data.readString();
                    String _arg3 = data.readString();
                    String _arg4 = data.readString();
                    attachPppd(_arg025, _arg111, _arg2, _arg3, _arg4);
                    reply.writeNoException();
                    return true;
                case 32:
                    data.enforceInterface(DESCRIPTOR);
                    String _arg026 = data.readString();
                    detachPppd(_arg026);
                    reply.writeNoException();
                    return true;
                case 33:
                    data.enforceInterface(DESCRIPTOR);
                    String _arg027 = data.readString();
                    String _arg112 = data.readString();
                    wifiFirmwareReload(_arg027, _arg112);
                    reply.writeNoException();
                    return true;
                case 34:
                    data.enforceInterface(DESCRIPTOR);
                    if (data.readInt() != 0) {
                        _arg02 = WifiConfiguration.CREATOR.createFromParcel(data);
                    } else {
                        _arg02 = null;
                    }
                    String _arg113 = data.readString();
                    startAccessPoint(_arg02, _arg113);
                    reply.writeNoException();
                    return true;
                case 35:
                    data.enforceInterface(DESCRIPTOR);
                    String _arg028 = data.readString();
                    stopAccessPoint(_arg028);
                    reply.writeNoException();
                    return true;
                case 36:
                    data.enforceInterface(DESCRIPTOR);
                    if (data.readInt() != 0) {
                        _arg0 = WifiConfiguration.CREATOR.createFromParcel(data);
                    } else {
                        _arg0 = null;
                    }
                    String _arg114 = data.readString();
                    setAccessPoint(_arg0, _arg114);
                    reply.writeNoException();
                    return true;
                case 37:
                    data.enforceInterface(DESCRIPTOR);
                    NetworkStats _result9 = getNetworkStatsSummaryDev();
                    reply.writeNoException();
                    if (_result9 != null) {
                        reply.writeInt(1);
                        _result9.writeToParcel(reply, 1);
                    } else {
                        reply.writeInt(0);
                    }
                    return true;
                case 38:
                    data.enforceInterface(DESCRIPTOR);
                    NetworkStats _result10 = getNetworkStatsSummaryXt();
                    reply.writeNoException();
                    if (_result10 != null) {
                        reply.writeInt(1);
                        _result10.writeToParcel(reply, 1);
                    } else {
                        reply.writeInt(0);
                    }
                    return true;
                case 39:
                    data.enforceInterface(DESCRIPTOR);
                    NetworkStats _result11 = getNetworkStatsDetail();
                    reply.writeNoException();
                    if (_result11 != null) {
                        reply.writeInt(1);
                        _result11.writeToParcel(reply, 1);
                    } else {
                        reply.writeInt(0);
                    }
                    return true;
                case 40:
                    data.enforceInterface(DESCRIPTOR);
                    int _arg029 = data.readInt();
                    NetworkStats _result12 = getNetworkStatsUidDetail(_arg029);
                    reply.writeNoException();
                    if (_result12 != null) {
                        reply.writeInt(1);
                        _result12.writeToParcel(reply, 1);
                    } else {
                        reply.writeInt(0);
                    }
                    return true;
                case 41:
                    data.enforceInterface(DESCRIPTOR);
                    NetworkStats _result13 = getNetworkStatsTethering();
                    reply.writeNoException();
                    if (_result13 != null) {
                        reply.writeInt(1);
                        _result13.writeToParcel(reply, 1);
                    } else {
                        reply.writeInt(0);
                    }
                    return true;
                case 42:
                    data.enforceInterface(DESCRIPTOR);
                    String _arg030 = data.readString();
                    long _arg115 = data.readLong();
                    setInterfaceQuota(_arg030, _arg115);
                    reply.writeNoException();
                    return true;
                case 43:
                    data.enforceInterface(DESCRIPTOR);
                    String _arg031 = data.readString();
                    removeInterfaceQuota(_arg031);
                    reply.writeNoException();
                    return true;
                case 44:
                    data.enforceInterface(DESCRIPTOR);
                    String _arg032 = data.readString();
                    long _arg116 = data.readLong();
                    setInterfaceAlert(_arg032, _arg116);
                    reply.writeNoException();
                    return true;
                case 45:
                    data.enforceInterface(DESCRIPTOR);
                    String _arg033 = data.readString();
                    removeInterfaceAlert(_arg033);
                    reply.writeNoException();
                    return true;
                case 46:
                    data.enforceInterface(DESCRIPTOR);
                    long _arg034 = data.readLong();
                    setGlobalAlert(_arg034);
                    reply.writeNoException();
                    return true;
                case 47:
                    data.enforceInterface(DESCRIPTOR);
                    int _arg035 = data.readInt();
                    boolean _arg117 = data.readInt() != 0;
                    setUidNetworkRules(_arg035, _arg117);
                    reply.writeNoException();
                    return true;
                case 48:
                    data.enforceInterface(DESCRIPTOR);
                    boolean _result14 = isBandwidthControlEnabled();
                    reply.writeNoException();
                    reply.writeInt(_result14 ? 1 : 0);
                    return true;
                case 49:
                    data.enforceInterface(DESCRIPTOR);
                    String _arg036 = data.readString();
                    int _arg118 = data.readInt();
                    int _arg22 = data.readInt();
                    addIdleTimer(_arg036, _arg118, _arg22);
                    reply.writeNoException();
                    return true;
                case 50:
                    data.enforceInterface(DESCRIPTOR);
                    String _arg037 = data.readString();
                    removeIdleTimer(_arg037);
                    reply.writeNoException();
                    return true;
                case 51:
                    data.enforceInterface(DESCRIPTOR);
                    int _arg038 = data.readInt();
                    String[] _arg119 = data.createStringArray();
                    String _arg23 = data.readString();
                    setDnsServersForNetwork(_arg038, _arg119, _arg23);
                    reply.writeNoException();
                    return true;
                case 52:
                    data.enforceInterface(DESCRIPTOR);
                    int _arg039 = data.readInt();
                    flushNetworkDnsCache(_arg039);
                    reply.writeNoException();
                    return true;
                case 53:
                    data.enforceInterface(DESCRIPTOR);
                    boolean _arg040 = data.readInt() != 0;
                    setFirewallEnabled(_arg040);
                    reply.writeNoException();
                    return true;
                case 54:
                    data.enforceInterface(DESCRIPTOR);
                    boolean _result15 = isFirewallEnabled();
                    reply.writeNoException();
                    reply.writeInt(_result15 ? 1 : 0);
                    return true;
                case 55:
                    data.enforceInterface(DESCRIPTOR);
                    String _arg041 = data.readString();
                    boolean _arg120 = data.readInt() != 0;
                    setFirewallInterfaceRule(_arg041, _arg120);
                    reply.writeNoException();
                    return true;
                case 56:
                    data.enforceInterface(DESCRIPTOR);
                    String _arg042 = data.readString();
                    boolean _arg121 = data.readInt() != 0;
                    setFirewallEgressSourceRule(_arg042, _arg121);
                    reply.writeNoException();
                    return true;
                case 57:
                    data.enforceInterface(DESCRIPTOR);
                    String _arg043 = data.readString();
                    int _arg122 = data.readInt();
                    boolean _arg24 = data.readInt() != 0;
                    setFirewallEgressDestRule(_arg043, _arg122, _arg24);
                    reply.writeNoException();
                    return true;
                case 58:
                    data.enforceInterface(DESCRIPTOR);
                    int _arg044 = data.readInt();
                    boolean _arg123 = data.readInt() != 0;
                    setFirewallUidRule(_arg044, _arg123);
                    reply.writeNoException();
                    return true;
                case 59:
                    data.enforceInterface(DESCRIPTOR);
                    int _arg045 = data.readInt();
                    UidRange[] _arg124 = (UidRange[]) data.createTypedArray(UidRange.CREATOR);
                    addVpnUidRanges(_arg045, _arg124);
                    reply.writeNoException();
                    return true;
                case 60:
                    data.enforceInterface(DESCRIPTOR);
                    int _arg046 = data.readInt();
                    UidRange[] _arg125 = (UidRange[]) data.createTypedArray(UidRange.CREATOR);
                    removeVpnUidRanges(_arg046, _arg125);
                    reply.writeNoException();
                    return true;
                case 61:
                    data.enforceInterface(DESCRIPTOR);
                    String _arg047 = data.readString();
                    startClatd(_arg047);
                    reply.writeNoException();
                    return true;
                case 62:
                    data.enforceInterface(DESCRIPTOR);
                    String _arg048 = data.readString();
                    stopClatd(_arg048);
                    reply.writeNoException();
                    return true;
                case 63:
                    data.enforceInterface(DESCRIPTOR);
                    String _arg049 = data.readString();
                    boolean _result16 = isClatdStarted(_arg049);
                    reply.writeNoException();
                    reply.writeInt(_result16 ? 1 : 0);
                    return true;
                case 64:
                    data.enforceInterface(DESCRIPTOR);
                    INetworkActivityListener _arg050 = INetworkActivityListener.Stub.asInterface(data.readStrongBinder());
                    registerNetworkActivityListener(_arg050);
                    reply.writeNoException();
                    return true;
                case 65:
                    data.enforceInterface(DESCRIPTOR);
                    INetworkActivityListener _arg051 = INetworkActivityListener.Stub.asInterface(data.readStrongBinder());
                    unregisterNetworkActivityListener(_arg051);
                    reply.writeNoException();
                    return true;
                case 66:
                    data.enforceInterface(DESCRIPTOR);
                    boolean _result17 = isNetworkActive();
                    reply.writeNoException();
                    reply.writeInt(_result17 ? 1 : 0);
                    return true;
                case 67:
                    data.enforceInterface(DESCRIPTOR);
                    int _arg052 = data.readInt();
                    createPhysicalNetwork(_arg052);
                    reply.writeNoException();
                    return true;
                case 68:
                    data.enforceInterface(DESCRIPTOR);
                    int _arg053 = data.readInt();
                    boolean _arg126 = data.readInt() != 0;
                    boolean _arg25 = data.readInt() != 0;
                    createVirtualNetwork(_arg053, _arg126, _arg25);
                    reply.writeNoException();
                    return true;
                case 69:
                    data.enforceInterface(DESCRIPTOR);
                    int _arg054 = data.readInt();
                    removeNetwork(_arg054);
                    reply.writeNoException();
                    return true;
                case 70:
                    data.enforceInterface(DESCRIPTOR);
                    String _arg055 = data.readString();
                    int _arg127 = data.readInt();
                    addInterfaceToNetwork(_arg055, _arg127);
                    reply.writeNoException();
                    return true;
                case 71:
                    data.enforceInterface(DESCRIPTOR);
                    String _arg056 = data.readString();
                    int _arg128 = data.readInt();
                    removeInterfaceFromNetwork(_arg056, _arg128);
                    reply.writeNoException();
                    return true;
                case 72:
                    data.enforceInterface(DESCRIPTOR);
                    int _arg057 = data.readInt();
                    if (data.readInt() != 0) {
                        _arg1 = RouteInfo.CREATOR.createFromParcel(data);
                    } else {
                        _arg1 = null;
                    }
                    int _arg26 = data.readInt();
                    addLegacyRouteForNetId(_arg057, _arg1, _arg26);
                    reply.writeNoException();
                    return true;
                case 73:
                    data.enforceInterface(DESCRIPTOR);
                    int _arg058 = data.readInt();
                    setDefaultNetId(_arg058);
                    reply.writeNoException();
                    return true;
                case 74:
                    data.enforceInterface(DESCRIPTOR);
                    clearDefaultNetId();
                    reply.writeNoException();
                    return true;
                case 75:
                    data.enforceInterface(DESCRIPTOR);
                    String _arg059 = data.readString();
                    int[] _arg129 = data.createIntArray();
                    setPermission(_arg059, _arg129);
                    reply.writeNoException();
                    return true;
                case 76:
                    data.enforceInterface(DESCRIPTOR);
                    int[] _arg060 = data.createIntArray();
                    clearPermission(_arg060);
                    reply.writeNoException();
                    return true;
                case 77:
                    data.enforceInterface(DESCRIPTOR);
                    int _arg061 = data.readInt();
                    allowProtect(_arg061);
                    reply.writeNoException();
                    return true;
                case 78:
                    data.enforceInterface(DESCRIPTOR);
                    int _arg062 = data.readInt();
                    denyProtect(_arg062);
                    reply.writeNoException();
                    return true;
                case 79:
                    data.enforceInterface(DESCRIPTOR);
                    String _arg063 = data.readString();
                    List<RouteInfo> _arg130 = data.createTypedArrayList(RouteInfo.CREATOR);
                    addInterfaceToLocalNetwork(_arg063, _arg130);
                    reply.writeNoException();
                    return true;
                case 80:
                    data.enforceInterface(DESCRIPTOR);
                    String _arg064 = data.readString();
                    removeInterfaceFromLocalNetwork(_arg064);
                    reply.writeNoException();
                    return true;
                case 81:
                    data.enforceInterface(DESCRIPTOR);
                    boolean _result18 = getIPv6ForwardingEnabled();
                    reply.writeNoException();
                    reply.writeInt(_result18 ? 1 : 0);
                    return true;
                case 82:
                    data.enforceInterface(DESCRIPTOR);
                    boolean _arg065 = data.readInt() != 0;
                    setIPv6ForwardingEnabled(_arg065);
                    reply.writeNoException();
                    return true;
                case 83:
                    data.enforceInterface(DESCRIPTOR);
                    String _arg066 = data.readString();
                    String _arg131 = data.readString();
                    String _arg27 = data.readString();
                    startIPv6Tethering(_arg066, _arg131, _arg27);
                    reply.writeNoException();
                    return true;
                case 84:
                    data.enforceInterface(DESCRIPTOR);
                    String _arg067 = data.readString();
                    String _arg132 = data.readString();
                    String _arg28 = data.readString();
                    stopIPv6Tethering(_arg067, _arg132, _arg28);
                    reply.writeNoException();
                    return true;
                case 85:
                    data.enforceInterface(DESCRIPTOR);
                    boolean _result19 = isIPv6TetheringStarted();
                    reply.writeNoException();
                    reply.writeInt(_result19 ? 1 : 0);
                    return true;
                case IBinder.INTERFACE_TRANSACTION:
                    reply.writeString(DESCRIPTOR);
                    return true;
                default:
                    return super.onTransact(code, data, reply, flags);
            }
        }

        private static class Proxy implements INetworkManagementService {
            private IBinder mRemote;

            Proxy(IBinder remote) {
                this.mRemote = remote;
            }

            @Override
            public IBinder asBinder() {
                return this.mRemote;
            }

            public String getInterfaceDescriptor() {
                return Stub.DESCRIPTOR;
            }

            @Override
            public void registerObserver(INetworkManagementEventObserver obs) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeStrongBinder(obs != null ? obs.asBinder() : null);
                    this.mRemote.transact(1, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override
            public void unregisterObserver(INetworkManagementEventObserver obs) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeStrongBinder(obs != null ? obs.asBinder() : null);
                    this.mRemote.transact(2, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override
            public String[] listInterfaces() throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    this.mRemote.transact(3, _data, _reply, 0);
                    _reply.readException();
                    String[] _result = _reply.createStringArray();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override
            public InterfaceConfiguration getInterfaceConfig(String iface) throws RemoteException {
                InterfaceConfiguration _result;
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeString(iface);
                    this.mRemote.transact(4, _data, _reply, 0);
                    _reply.readException();
                    if (_reply.readInt() != 0) {
                        _result = InterfaceConfiguration.CREATOR.createFromParcel(_reply);
                    } else {
                        _result = null;
                    }
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override
            public void setInterfaceConfig(String iface, InterfaceConfiguration cfg) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeString(iface);
                    if (cfg != null) {
                        _data.writeInt(1);
                        cfg.writeToParcel(_data, 0);
                    } else {
                        _data.writeInt(0);
                    }
                    this.mRemote.transact(5, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override
            public void clearInterfaceAddresses(String iface) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeString(iface);
                    this.mRemote.transact(6, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override
            public void setInterfaceDown(String iface) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeString(iface);
                    this.mRemote.transact(7, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override
            public void setInterfaceUp(String iface) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeString(iface);
                    this.mRemote.transact(8, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override
            public void setInterfaceIpv6PrivacyExtensions(String iface, boolean enable) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeString(iface);
                    _data.writeInt(enable ? 1 : 0);
                    this.mRemote.transact(9, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override
            public void disableIpv6(String iface) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeString(iface);
                    this.mRemote.transact(10, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override
            public void enableIpv6(String iface) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeString(iface);
                    this.mRemote.transact(11, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override
            public void setInterfaceIpv6NdOffload(String iface, boolean enable) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeString(iface);
                    _data.writeInt(enable ? 1 : 0);
                    this.mRemote.transact(12, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override
            public RouteInfo[] getRoutes(String iface) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeString(iface);
                    this.mRemote.transact(13, _data, _reply, 0);
                    _reply.readException();
                    RouteInfo[] _result = (RouteInfo[]) _reply.createTypedArray(RouteInfo.CREATOR);
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override
            public void addRoute(int netId, RouteInfo route) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(netId);
                    if (route != null) {
                        _data.writeInt(1);
                        route.writeToParcel(_data, 0);
                    } else {
                        _data.writeInt(0);
                    }
                    this.mRemote.transact(14, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override
            public void removeRoute(int netId, RouteInfo route) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(netId);
                    if (route != null) {
                        _data.writeInt(1);
                        route.writeToParcel(_data, 0);
                    } else {
                        _data.writeInt(0);
                    }
                    this.mRemote.transact(15, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override
            public void setMtu(String iface, int mtu) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeString(iface);
                    _data.writeInt(mtu);
                    this.mRemote.transact(16, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override
            public void shutdown() throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    this.mRemote.transact(17, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override
            public boolean getIpForwardingEnabled() throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    this.mRemote.transact(18, _data, _reply, 0);
                    _reply.readException();
                    boolean _result = _reply.readInt() != 0;
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override
            public void setIpForwardingEnabled(boolean enabled) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(enabled ? 1 : 0);
                    this.mRemote.transact(19, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override
            public void startTethering(String[] dhcpRanges) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeStringArray(dhcpRanges);
                    this.mRemote.transact(20, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override
            public void stopTethering() throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    this.mRemote.transact(21, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override
            public boolean isTetheringStarted() throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    this.mRemote.transact(22, _data, _reply, 0);
                    _reply.readException();
                    boolean _result = _reply.readInt() != 0;
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override
            public void tetherInterface(String iface) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeString(iface);
                    this.mRemote.transact(23, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override
            public void untetherInterface(String iface) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeString(iface);
                    this.mRemote.transact(24, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override
            public String[] listTetheredInterfaces() throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    this.mRemote.transact(25, _data, _reply, 0);
                    _reply.readException();
                    String[] _result = _reply.createStringArray();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override
            public void setDnsForwarders(Network network, String[] dns) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    if (network != null) {
                        _data.writeInt(1);
                        network.writeToParcel(_data, 0);
                    } else {
                        _data.writeInt(0);
                    }
                    _data.writeStringArray(dns);
                    this.mRemote.transact(26, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override
            public String[] getDnsForwarders() throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    this.mRemote.transact(27, _data, _reply, 0);
                    _reply.readException();
                    String[] _result = _reply.createStringArray();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override
            public void enableNat(String internalInterface, String externalInterface) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeString(internalInterface);
                    _data.writeString(externalInterface);
                    this.mRemote.transact(28, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override
            public void disableNat(String internalInterface, String externalInterface) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeString(internalInterface);
                    _data.writeString(externalInterface);
                    this.mRemote.transact(29, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override
            public String[] listTtys() throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    this.mRemote.transact(30, _data, _reply, 0);
                    _reply.readException();
                    String[] _result = _reply.createStringArray();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override
            public void attachPppd(String tty, String localAddr, String remoteAddr, String dns1Addr, String dns2Addr) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeString(tty);
                    _data.writeString(localAddr);
                    _data.writeString(remoteAddr);
                    _data.writeString(dns1Addr);
                    _data.writeString(dns2Addr);
                    this.mRemote.transact(31, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override
            public void detachPppd(String tty) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeString(tty);
                    this.mRemote.transact(32, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override
            public void wifiFirmwareReload(String wlanIface, String mode) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeString(wlanIface);
                    _data.writeString(mode);
                    this.mRemote.transact(33, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override
            public void startAccessPoint(WifiConfiguration wifiConfig, String iface) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    if (wifiConfig != null) {
                        _data.writeInt(1);
                        wifiConfig.writeToParcel(_data, 0);
                    } else {
                        _data.writeInt(0);
                    }
                    _data.writeString(iface);
                    this.mRemote.transact(34, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override
            public void stopAccessPoint(String iface) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeString(iface);
                    this.mRemote.transact(35, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override
            public void setAccessPoint(WifiConfiguration wifiConfig, String iface) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    if (wifiConfig != null) {
                        _data.writeInt(1);
                        wifiConfig.writeToParcel(_data, 0);
                    } else {
                        _data.writeInt(0);
                    }
                    _data.writeString(iface);
                    this.mRemote.transact(36, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override
            public NetworkStats getNetworkStatsSummaryDev() throws RemoteException {
                NetworkStats _result;
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    this.mRemote.transact(37, _data, _reply, 0);
                    _reply.readException();
                    if (_reply.readInt() != 0) {
                        _result = NetworkStats.CREATOR.createFromParcel(_reply);
                    } else {
                        _result = null;
                    }
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override
            public NetworkStats getNetworkStatsSummaryXt() throws RemoteException {
                NetworkStats _result;
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    this.mRemote.transact(38, _data, _reply, 0);
                    _reply.readException();
                    if (_reply.readInt() != 0) {
                        _result = NetworkStats.CREATOR.createFromParcel(_reply);
                    } else {
                        _result = null;
                    }
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override
            public NetworkStats getNetworkStatsDetail() throws RemoteException {
                NetworkStats _result;
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    this.mRemote.transact(39, _data, _reply, 0);
                    _reply.readException();
                    if (_reply.readInt() != 0) {
                        _result = NetworkStats.CREATOR.createFromParcel(_reply);
                    } else {
                        _result = null;
                    }
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override
            public NetworkStats getNetworkStatsUidDetail(int uid) throws RemoteException {
                NetworkStats _result;
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(uid);
                    this.mRemote.transact(40, _data, _reply, 0);
                    _reply.readException();
                    if (_reply.readInt() != 0) {
                        _result = NetworkStats.CREATOR.createFromParcel(_reply);
                    } else {
                        _result = null;
                    }
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override
            public NetworkStats getNetworkStatsTethering() throws RemoteException {
                NetworkStats _result;
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    this.mRemote.transact(41, _data, _reply, 0);
                    _reply.readException();
                    if (_reply.readInt() != 0) {
                        _result = NetworkStats.CREATOR.createFromParcel(_reply);
                    } else {
                        _result = null;
                    }
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override
            public void setInterfaceQuota(String iface, long quotaBytes) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeString(iface);
                    _data.writeLong(quotaBytes);
                    this.mRemote.transact(42, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override
            public void removeInterfaceQuota(String iface) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeString(iface);
                    this.mRemote.transact(43, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override
            public void setInterfaceAlert(String iface, long alertBytes) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeString(iface);
                    _data.writeLong(alertBytes);
                    this.mRemote.transact(44, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override
            public void removeInterfaceAlert(String iface) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeString(iface);
                    this.mRemote.transact(45, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override
            public void setGlobalAlert(long alertBytes) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeLong(alertBytes);
                    this.mRemote.transact(46, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override
            public void setUidNetworkRules(int uid, boolean rejectOnQuotaInterfaces) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(uid);
                    _data.writeInt(rejectOnQuotaInterfaces ? 1 : 0);
                    this.mRemote.transact(47, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override
            public boolean isBandwidthControlEnabled() throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    this.mRemote.transact(48, _data, _reply, 0);
                    _reply.readException();
                    boolean _result = _reply.readInt() != 0;
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override
            public void addIdleTimer(String iface, int timeout, int type) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeString(iface);
                    _data.writeInt(timeout);
                    _data.writeInt(type);
                    this.mRemote.transact(49, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override
            public void removeIdleTimer(String iface) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeString(iface);
                    this.mRemote.transact(50, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override
            public void setDnsServersForNetwork(int netId, String[] servers, String domains) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(netId);
                    _data.writeStringArray(servers);
                    _data.writeString(domains);
                    this.mRemote.transact(51, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override
            public void flushNetworkDnsCache(int netId) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(netId);
                    this.mRemote.transact(52, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override
            public void setFirewallEnabled(boolean enabled) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(enabled ? 1 : 0);
                    this.mRemote.transact(53, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override
            public boolean isFirewallEnabled() throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    this.mRemote.transact(54, _data, _reply, 0);
                    _reply.readException();
                    boolean _result = _reply.readInt() != 0;
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override
            public void setFirewallInterfaceRule(String iface, boolean allow) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeString(iface);
                    _data.writeInt(allow ? 1 : 0);
                    this.mRemote.transact(55, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override
            public void setFirewallEgressSourceRule(String addr, boolean allow) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeString(addr);
                    _data.writeInt(allow ? 1 : 0);
                    this.mRemote.transact(56, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override
            public void setFirewallEgressDestRule(String addr, int port, boolean allow) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeString(addr);
                    _data.writeInt(port);
                    _data.writeInt(allow ? 1 : 0);
                    this.mRemote.transact(57, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override
            public void setFirewallUidRule(int uid, boolean allow) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(uid);
                    _data.writeInt(allow ? 1 : 0);
                    this.mRemote.transact(58, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override
            public void addVpnUidRanges(int netId, UidRange[] ranges) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(netId);
                    _data.writeTypedArray(ranges, 0);
                    this.mRemote.transact(59, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override
            public void removeVpnUidRanges(int netId, UidRange[] ranges) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(netId);
                    _data.writeTypedArray(ranges, 0);
                    this.mRemote.transact(60, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override
            public void startClatd(String interfaceName) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeString(interfaceName);
                    this.mRemote.transact(61, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override
            public void stopClatd(String interfaceName) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeString(interfaceName);
                    this.mRemote.transact(62, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override
            public boolean isClatdStarted(String interfaceName) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeString(interfaceName);
                    this.mRemote.transact(63, _data, _reply, 0);
                    _reply.readException();
                    boolean _result = _reply.readInt() != 0;
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override
            public void registerNetworkActivityListener(INetworkActivityListener listener) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeStrongBinder(listener != null ? listener.asBinder() : null);
                    this.mRemote.transact(64, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override
            public void unregisterNetworkActivityListener(INetworkActivityListener listener) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeStrongBinder(listener != null ? listener.asBinder() : null);
                    this.mRemote.transact(65, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override
            public boolean isNetworkActive() throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    this.mRemote.transact(66, _data, _reply, 0);
                    _reply.readException();
                    boolean _result = _reply.readInt() != 0;
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override
            public void createPhysicalNetwork(int netId) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(netId);
                    this.mRemote.transact(67, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override
            public void createVirtualNetwork(int netId, boolean hasDNS, boolean secure) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(netId);
                    _data.writeInt(hasDNS ? 1 : 0);
                    _data.writeInt(secure ? 1 : 0);
                    this.mRemote.transact(68, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override
            public void removeNetwork(int netId) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(netId);
                    this.mRemote.transact(69, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override
            public void addInterfaceToNetwork(String iface, int netId) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeString(iface);
                    _data.writeInt(netId);
                    this.mRemote.transact(70, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override
            public void removeInterfaceFromNetwork(String iface, int netId) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeString(iface);
                    _data.writeInt(netId);
                    this.mRemote.transact(71, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override
            public void addLegacyRouteForNetId(int netId, RouteInfo routeInfo, int uid) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(netId);
                    if (routeInfo != null) {
                        _data.writeInt(1);
                        routeInfo.writeToParcel(_data, 0);
                    } else {
                        _data.writeInt(0);
                    }
                    _data.writeInt(uid);
                    this.mRemote.transact(72, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override
            public void setDefaultNetId(int netId) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(netId);
                    this.mRemote.transact(73, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override
            public void clearDefaultNetId() throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    this.mRemote.transact(74, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override
            public void setPermission(String permission, int[] uids) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeString(permission);
                    _data.writeIntArray(uids);
                    this.mRemote.transact(75, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override
            public void clearPermission(int[] uids) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeIntArray(uids);
                    this.mRemote.transact(76, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override
            public void allowProtect(int uid) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(uid);
                    this.mRemote.transact(77, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override
            public void denyProtect(int uid) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(uid);
                    this.mRemote.transact(78, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override
            public void addInterfaceToLocalNetwork(String iface, List<RouteInfo> routes) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeString(iface);
                    _data.writeTypedList(routes);
                    this.mRemote.transact(79, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override
            public void removeInterfaceFromLocalNetwork(String iface) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeString(iface);
                    this.mRemote.transact(80, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override
            public boolean getIPv6ForwardingEnabled() throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    this.mRemote.transact(81, _data, _reply, 0);
                    _reply.readException();
                    boolean _result = _reply.readInt() != 0;
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override
            public void setIPv6ForwardingEnabled(boolean enable) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(enable ? 1 : 0);
                    this.mRemote.transact(82, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override
            public void startIPv6Tethering(String downstreamIface, String upstreamIface, String address) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeString(downstreamIface);
                    _data.writeString(upstreamIface);
                    _data.writeString(address);
                    this.mRemote.transact(83, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override
            public void stopIPv6Tethering(String downstreamIface, String upstreamIface, String address) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeString(downstreamIface);
                    _data.writeString(upstreamIface);
                    _data.writeString(address);
                    this.mRemote.transact(84, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override
            public boolean isIPv6TetheringStarted() throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    this.mRemote.transact(85, _data, _reply, 0);
                    _reply.readException();
                    boolean _result = _reply.readInt() != 0;
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }
        }
    }
}
