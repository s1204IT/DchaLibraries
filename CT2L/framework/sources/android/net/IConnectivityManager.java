package android.net;

import android.app.PendingIntent;
import android.os.Binder;
import android.os.IBinder;
import android.os.IInterface;
import android.os.Messenger;
import android.os.Parcel;
import android.os.ParcelFileDescriptor;
import android.os.RemoteException;
import com.android.internal.net.LegacyVpnInfo;
import com.android.internal.net.VpnConfig;
import com.android.internal.net.VpnProfile;

public interface IConnectivityManager extends IInterface {
    boolean addVpnAddress(String str, int i) throws RemoteException;

    void captivePortalCheckCompleted(NetworkInfo networkInfo, boolean z) throws RemoteException;

    int checkMobileProvisioning(int i) throws RemoteException;

    ParcelFileDescriptor establishVpn(VpnConfig vpnConfig) throws RemoteException;

    int findConnectionTypeForIface(String str) throws RemoteException;

    LinkProperties getActiveLinkProperties() throws RemoteException;

    NetworkInfo getActiveNetworkInfo() throws RemoteException;

    NetworkInfo getActiveNetworkInfoForUid(int i) throws RemoteException;

    NetworkQuotaInfo getActiveNetworkQuotaInfo() throws RemoteException;

    NetworkInfo[] getAllNetworkInfo() throws RemoteException;

    NetworkState[] getAllNetworkState() throws RemoteException;

    Network[] getAllNetworks() throws RemoteException;

    NetworkCapabilities[] getDefaultNetworkCapabilitiesForUser(int i) throws RemoteException;

    ProxyInfo getDefaultProxy() throws RemoteException;

    ProxyInfo getGlobalProxy() throws RemoteException;

    int getLastTetherError(String str) throws RemoteException;

    LegacyVpnInfo getLegacyVpnInfo() throws RemoteException;

    LinkProperties getLinkProperties(Network network) throws RemoteException;

    LinkProperties getLinkPropertiesForType(int i) throws RemoteException;

    String getMobileProvisioningUrl() throws RemoteException;

    String getMobileRedirectedProvisioningUrl() throws RemoteException;

    NetworkCapabilities getNetworkCapabilities(Network network) throws RemoteException;

    Network getNetworkForType(int i) throws RemoteException;

    NetworkInfo getNetworkInfo(int i) throws RemoteException;

    NetworkInfo getNetworkInfoForNetwork(Network network) throws RemoteException;

    NetworkInfo getProvisioningOrActiveNetworkInfo() throws RemoteException;

    int getRestoreDefaultNetworkDelay(int i) throws RemoteException;

    String[] getTetherableBluetoothRegexs() throws RemoteException;

    String[] getTetherableIfaces() throws RemoteException;

    String[] getTetherableUsbRegexs() throws RemoteException;

    String[] getTetherableWifiRegexs() throws RemoteException;

    String[] getTetheredDhcpRanges() throws RemoteException;

    String[] getTetheredIfaces() throws RemoteException;

    String[] getTetheringErroredIfaces() throws RemoteException;

    VpnConfig getVpnConfig() throws RemoteException;

    boolean isActiveNetworkMetered() throws RemoteException;

    boolean isNetworkSupported(int i) throws RemoteException;

    boolean isTetheringSupported() throws RemoteException;

    NetworkRequest listenForNetwork(NetworkCapabilities networkCapabilities, Messenger messenger, IBinder iBinder) throws RemoteException;

    void pendingListenForNetwork(NetworkCapabilities networkCapabilities, PendingIntent pendingIntent) throws RemoteException;

    NetworkRequest pendingRequestForNetwork(NetworkCapabilities networkCapabilities, PendingIntent pendingIntent) throws RemoteException;

    boolean prepareVpn(String str, String str2) throws RemoteException;

    void registerNetworkAgent(Messenger messenger, NetworkInfo networkInfo, LinkProperties linkProperties, NetworkCapabilities networkCapabilities, int i, NetworkMisc networkMisc) throws RemoteException;

    void registerNetworkFactory(Messenger messenger, String str) throws RemoteException;

    void releaseNetworkRequest(NetworkRequest networkRequest) throws RemoteException;

    void releasePendingNetworkRequest(PendingIntent pendingIntent) throws RemoteException;

    boolean removeVpnAddress(String str, int i) throws RemoteException;

    void reportBadNetwork(Network network) throws RemoteException;

    void reportInetCondition(int i, int i2) throws RemoteException;

    NetworkRequest requestNetwork(NetworkCapabilities networkCapabilities, Messenger messenger, int i, IBinder iBinder, int i2) throws RemoteException;

    boolean requestRouteToHostAddress(int i, byte[] bArr) throws RemoteException;

    void setAirplaneMode(boolean z) throws RemoteException;

    void setDataDependency(int i, boolean z) throws RemoteException;

    void setGlobalProxy(ProxyInfo proxyInfo) throws RemoteException;

    void setProvisioningNotificationVisible(boolean z, int i, String str) throws RemoteException;

    boolean setUnderlyingNetworksForVpn(Network[] networkArr) throws RemoteException;

    int setUsbTethering(boolean z) throws RemoteException;

    void setVpnPackageAuthorization(boolean z) throws RemoteException;

    void startLegacyVpn(VpnProfile vpnProfile) throws RemoteException;

    void supplyMessenger(int i, Messenger messenger) throws RemoteException;

    int tether(String str) throws RemoteException;

    void unregisterNetworkFactory(Messenger messenger) throws RemoteException;

    int untether(String str) throws RemoteException;

    boolean updateLockdownVpn() throws RemoteException;

    public static abstract class Stub extends Binder implements IConnectivityManager {
        private static final String DESCRIPTOR = "android.net.IConnectivityManager";
        static final int TRANSACTION_addVpnAddress = 62;
        static final int TRANSACTION_captivePortalCheckCompleted = 44;
        static final int TRANSACTION_checkMobileProvisioning = 47;
        static final int TRANSACTION_establishVpn = 39;
        static final int TRANSACTION_findConnectionTypeForIface = 46;
        static final int TRANSACTION_getActiveLinkProperties = 11;
        static final int TRANSACTION_getActiveNetworkInfo = 1;
        static final int TRANSACTION_getActiveNetworkInfoForUid = 2;
        static final int TRANSACTION_getActiveNetworkQuotaInfo = 16;
        static final int TRANSACTION_getAllNetworkInfo = 5;
        static final int TRANSACTION_getAllNetworkState = 15;
        static final int TRANSACTION_getAllNetworks = 7;
        static final int TRANSACTION_getDefaultNetworkCapabilitiesForUser = 8;
        static final int TRANSACTION_getDefaultProxy = 35;
        static final int TRANSACTION_getGlobalProxy = 33;
        static final int TRANSACTION_getLastTetherError = 21;
        static final int TRANSACTION_getLegacyVpnInfo = 42;
        static final int TRANSACTION_getLinkProperties = 13;
        static final int TRANSACTION_getLinkPropertiesForType = 12;
        static final int TRANSACTION_getMobileProvisioningUrl = 48;
        static final int TRANSACTION_getMobileRedirectedProvisioningUrl = 49;
        static final int TRANSACTION_getNetworkCapabilities = 14;
        static final int TRANSACTION_getNetworkForType = 6;
        static final int TRANSACTION_getNetworkInfo = 3;
        static final int TRANSACTION_getNetworkInfoForNetwork = 4;
        static final int TRANSACTION_getProvisioningOrActiveNetworkInfo = 9;
        static final int TRANSACTION_getRestoreDefaultNetworkDelay = 61;
        static final int TRANSACTION_getTetherableBluetoothRegexs = 29;
        static final int TRANSACTION_getTetherableIfaces = 23;
        static final int TRANSACTION_getTetherableUsbRegexs = 27;
        static final int TRANSACTION_getTetherableWifiRegexs = 28;
        static final int TRANSACTION_getTetheredDhcpRanges = 26;
        static final int TRANSACTION_getTetheredIfaces = 24;
        static final int TRANSACTION_getTetheringErroredIfaces = 25;
        static final int TRANSACTION_getVpnConfig = 40;
        static final int TRANSACTION_isActiveNetworkMetered = 17;
        static final int TRANSACTION_isNetworkSupported = 10;
        static final int TRANSACTION_isTetheringSupported = 22;
        static final int TRANSACTION_listenForNetwork = 58;
        static final int TRANSACTION_pendingListenForNetwork = 59;
        static final int TRANSACTION_pendingRequestForNetwork = 56;
        static final int TRANSACTION_prepareVpn = 37;
        static final int TRANSACTION_registerNetworkAgent = 54;
        static final int TRANSACTION_registerNetworkFactory = 52;
        static final int TRANSACTION_releaseNetworkRequest = 60;
        static final int TRANSACTION_releasePendingNetworkRequest = 57;
        static final int TRANSACTION_removeVpnAddress = 63;
        static final int TRANSACTION_reportBadNetwork = 32;
        static final int TRANSACTION_reportInetCondition = 31;
        static final int TRANSACTION_requestNetwork = 55;
        static final int TRANSACTION_requestRouteToHostAddress = 18;
        static final int TRANSACTION_setAirplaneMode = 51;
        static final int TRANSACTION_setDataDependency = 36;
        static final int TRANSACTION_setGlobalProxy = 34;
        static final int TRANSACTION_setProvisioningNotificationVisible = 50;
        static final int TRANSACTION_setUnderlyingNetworksForVpn = 64;
        static final int TRANSACTION_setUsbTethering = 30;
        static final int TRANSACTION_setVpnPackageAuthorization = 38;
        static final int TRANSACTION_startLegacyVpn = 41;
        static final int TRANSACTION_supplyMessenger = 45;
        static final int TRANSACTION_tether = 19;
        static final int TRANSACTION_unregisterNetworkFactory = 53;
        static final int TRANSACTION_untether = 20;
        static final int TRANSACTION_updateLockdownVpn = 43;

        public Stub() {
            attachInterface(this, DESCRIPTOR);
        }

        public static IConnectivityManager asInterface(IBinder obj) {
            if (obj == null) {
                return null;
            }
            IInterface iin = obj.queryLocalInterface(DESCRIPTOR);
            if (iin != null && (iin instanceof IConnectivityManager)) {
                return (IConnectivityManager) iin;
            }
            return new Proxy(obj);
        }

        @Override
        public IBinder asBinder() {
            return this;
        }

        @Override
        public boolean onTransact(int code, Parcel data, Parcel reply, int flags) throws RemoteException {
            NetworkRequest _arg0;
            NetworkCapabilities _arg02;
            PendingIntent _arg1;
            NetworkCapabilities _arg03;
            Messenger _arg12;
            PendingIntent _arg04;
            NetworkCapabilities _arg05;
            PendingIntent _arg13;
            NetworkCapabilities _arg06;
            Messenger _arg14;
            Messenger _arg07;
            NetworkInfo _arg15;
            LinkProperties _arg2;
            NetworkCapabilities _arg3;
            NetworkMisc _arg5;
            Messenger _arg08;
            Messenger _arg09;
            Messenger _arg16;
            NetworkInfo _arg010;
            VpnProfile _arg011;
            VpnConfig _arg012;
            ProxyInfo _arg013;
            Network _arg014;
            Network _arg015;
            Network _arg016;
            Network _arg017;
            switch (code) {
                case 1:
                    data.enforceInterface(DESCRIPTOR);
                    NetworkInfo _result = getActiveNetworkInfo();
                    reply.writeNoException();
                    if (_result != null) {
                        reply.writeInt(1);
                        _result.writeToParcel(reply, 1);
                        return true;
                    }
                    reply.writeInt(0);
                    return true;
                case 2:
                    data.enforceInterface(DESCRIPTOR);
                    int _arg018 = data.readInt();
                    NetworkInfo _result2 = getActiveNetworkInfoForUid(_arg018);
                    reply.writeNoException();
                    if (_result2 != null) {
                        reply.writeInt(1);
                        _result2.writeToParcel(reply, 1);
                        return true;
                    }
                    reply.writeInt(0);
                    return true;
                case 3:
                    data.enforceInterface(DESCRIPTOR);
                    int _arg019 = data.readInt();
                    NetworkInfo _result3 = getNetworkInfo(_arg019);
                    reply.writeNoException();
                    if (_result3 != null) {
                        reply.writeInt(1);
                        _result3.writeToParcel(reply, 1);
                        return true;
                    }
                    reply.writeInt(0);
                    return true;
                case 4:
                    data.enforceInterface(DESCRIPTOR);
                    if (data.readInt() != 0) {
                        _arg017 = Network.CREATOR.createFromParcel(data);
                    } else {
                        _arg017 = null;
                    }
                    NetworkInfo _result4 = getNetworkInfoForNetwork(_arg017);
                    reply.writeNoException();
                    if (_result4 != null) {
                        reply.writeInt(1);
                        _result4.writeToParcel(reply, 1);
                        return true;
                    }
                    reply.writeInt(0);
                    return true;
                case 5:
                    data.enforceInterface(DESCRIPTOR);
                    NetworkInfo[] _result5 = getAllNetworkInfo();
                    reply.writeNoException();
                    reply.writeTypedArray(_result5, 1);
                    return true;
                case 6:
                    data.enforceInterface(DESCRIPTOR);
                    int _arg020 = data.readInt();
                    Network _result6 = getNetworkForType(_arg020);
                    reply.writeNoException();
                    if (_result6 != null) {
                        reply.writeInt(1);
                        _result6.writeToParcel(reply, 1);
                        return true;
                    }
                    reply.writeInt(0);
                    return true;
                case 7:
                    data.enforceInterface(DESCRIPTOR);
                    Network[] _result7 = getAllNetworks();
                    reply.writeNoException();
                    reply.writeTypedArray(_result7, 1);
                    return true;
                case 8:
                    data.enforceInterface(DESCRIPTOR);
                    int _arg021 = data.readInt();
                    NetworkCapabilities[] _result8 = getDefaultNetworkCapabilitiesForUser(_arg021);
                    reply.writeNoException();
                    reply.writeTypedArray(_result8, 1);
                    return true;
                case 9:
                    data.enforceInterface(DESCRIPTOR);
                    NetworkInfo _result9 = getProvisioningOrActiveNetworkInfo();
                    reply.writeNoException();
                    if (_result9 != null) {
                        reply.writeInt(1);
                        _result9.writeToParcel(reply, 1);
                        return true;
                    }
                    reply.writeInt(0);
                    return true;
                case 10:
                    data.enforceInterface(DESCRIPTOR);
                    int _arg022 = data.readInt();
                    boolean _result10 = isNetworkSupported(_arg022);
                    reply.writeNoException();
                    reply.writeInt(_result10 ? 1 : 0);
                    return true;
                case 11:
                    data.enforceInterface(DESCRIPTOR);
                    LinkProperties _result11 = getActiveLinkProperties();
                    reply.writeNoException();
                    if (_result11 != null) {
                        reply.writeInt(1);
                        _result11.writeToParcel(reply, 1);
                        return true;
                    }
                    reply.writeInt(0);
                    return true;
                case 12:
                    data.enforceInterface(DESCRIPTOR);
                    int _arg023 = data.readInt();
                    LinkProperties _result12 = getLinkPropertiesForType(_arg023);
                    reply.writeNoException();
                    if (_result12 != null) {
                        reply.writeInt(1);
                        _result12.writeToParcel(reply, 1);
                        return true;
                    }
                    reply.writeInt(0);
                    return true;
                case 13:
                    data.enforceInterface(DESCRIPTOR);
                    if (data.readInt() != 0) {
                        _arg016 = Network.CREATOR.createFromParcel(data);
                    } else {
                        _arg016 = null;
                    }
                    LinkProperties _result13 = getLinkProperties(_arg016);
                    reply.writeNoException();
                    if (_result13 != null) {
                        reply.writeInt(1);
                        _result13.writeToParcel(reply, 1);
                        return true;
                    }
                    reply.writeInt(0);
                    return true;
                case 14:
                    data.enforceInterface(DESCRIPTOR);
                    if (data.readInt() != 0) {
                        _arg015 = Network.CREATOR.createFromParcel(data);
                    } else {
                        _arg015 = null;
                    }
                    NetworkCapabilities _result14 = getNetworkCapabilities(_arg015);
                    reply.writeNoException();
                    if (_result14 != null) {
                        reply.writeInt(1);
                        _result14.writeToParcel(reply, 1);
                        return true;
                    }
                    reply.writeInt(0);
                    return true;
                case 15:
                    data.enforceInterface(DESCRIPTOR);
                    NetworkState[] _result15 = getAllNetworkState();
                    reply.writeNoException();
                    reply.writeTypedArray(_result15, 1);
                    return true;
                case 16:
                    data.enforceInterface(DESCRIPTOR);
                    NetworkQuotaInfo _result16 = getActiveNetworkQuotaInfo();
                    reply.writeNoException();
                    if (_result16 != null) {
                        reply.writeInt(1);
                        _result16.writeToParcel(reply, 1);
                        return true;
                    }
                    reply.writeInt(0);
                    return true;
                case 17:
                    data.enforceInterface(DESCRIPTOR);
                    boolean _result17 = isActiveNetworkMetered();
                    reply.writeNoException();
                    reply.writeInt(_result17 ? 1 : 0);
                    return true;
                case 18:
                    data.enforceInterface(DESCRIPTOR);
                    int _arg024 = data.readInt();
                    byte[] _arg17 = data.createByteArray();
                    boolean _result18 = requestRouteToHostAddress(_arg024, _arg17);
                    reply.writeNoException();
                    reply.writeInt(_result18 ? 1 : 0);
                    return true;
                case 19:
                    data.enforceInterface(DESCRIPTOR);
                    String _arg025 = data.readString();
                    int _result19 = tether(_arg025);
                    reply.writeNoException();
                    reply.writeInt(_result19);
                    return true;
                case 20:
                    data.enforceInterface(DESCRIPTOR);
                    String _arg026 = data.readString();
                    int _result20 = untether(_arg026);
                    reply.writeNoException();
                    reply.writeInt(_result20);
                    return true;
                case 21:
                    data.enforceInterface(DESCRIPTOR);
                    String _arg027 = data.readString();
                    int _result21 = getLastTetherError(_arg027);
                    reply.writeNoException();
                    reply.writeInt(_result21);
                    return true;
                case 22:
                    data.enforceInterface(DESCRIPTOR);
                    boolean _result22 = isTetheringSupported();
                    reply.writeNoException();
                    reply.writeInt(_result22 ? 1 : 0);
                    return true;
                case 23:
                    data.enforceInterface(DESCRIPTOR);
                    String[] _result23 = getTetherableIfaces();
                    reply.writeNoException();
                    reply.writeStringArray(_result23);
                    return true;
                case 24:
                    data.enforceInterface(DESCRIPTOR);
                    String[] _result24 = getTetheredIfaces();
                    reply.writeNoException();
                    reply.writeStringArray(_result24);
                    return true;
                case 25:
                    data.enforceInterface(DESCRIPTOR);
                    String[] _result25 = getTetheringErroredIfaces();
                    reply.writeNoException();
                    reply.writeStringArray(_result25);
                    return true;
                case 26:
                    data.enforceInterface(DESCRIPTOR);
                    String[] _result26 = getTetheredDhcpRanges();
                    reply.writeNoException();
                    reply.writeStringArray(_result26);
                    return true;
                case 27:
                    data.enforceInterface(DESCRIPTOR);
                    String[] _result27 = getTetherableUsbRegexs();
                    reply.writeNoException();
                    reply.writeStringArray(_result27);
                    return true;
                case 28:
                    data.enforceInterface(DESCRIPTOR);
                    String[] _result28 = getTetherableWifiRegexs();
                    reply.writeNoException();
                    reply.writeStringArray(_result28);
                    return true;
                case 29:
                    data.enforceInterface(DESCRIPTOR);
                    String[] _result29 = getTetherableBluetoothRegexs();
                    reply.writeNoException();
                    reply.writeStringArray(_result29);
                    return true;
                case 30:
                    data.enforceInterface(DESCRIPTOR);
                    boolean _arg028 = data.readInt() != 0;
                    int _result30 = setUsbTethering(_arg028);
                    reply.writeNoException();
                    reply.writeInt(_result30);
                    return true;
                case 31:
                    data.enforceInterface(DESCRIPTOR);
                    int _arg029 = data.readInt();
                    int _arg18 = data.readInt();
                    reportInetCondition(_arg029, _arg18);
                    reply.writeNoException();
                    return true;
                case 32:
                    data.enforceInterface(DESCRIPTOR);
                    if (data.readInt() != 0) {
                        _arg014 = Network.CREATOR.createFromParcel(data);
                    } else {
                        _arg014 = null;
                    }
                    reportBadNetwork(_arg014);
                    reply.writeNoException();
                    return true;
                case 33:
                    data.enforceInterface(DESCRIPTOR);
                    ProxyInfo _result31 = getGlobalProxy();
                    reply.writeNoException();
                    if (_result31 != null) {
                        reply.writeInt(1);
                        _result31.writeToParcel(reply, 1);
                        return true;
                    }
                    reply.writeInt(0);
                    return true;
                case 34:
                    data.enforceInterface(DESCRIPTOR);
                    if (data.readInt() != 0) {
                        _arg013 = ProxyInfo.CREATOR.createFromParcel(data);
                    } else {
                        _arg013 = null;
                    }
                    setGlobalProxy(_arg013);
                    reply.writeNoException();
                    return true;
                case 35:
                    data.enforceInterface(DESCRIPTOR);
                    ProxyInfo _result32 = getDefaultProxy();
                    reply.writeNoException();
                    if (_result32 != null) {
                        reply.writeInt(1);
                        _result32.writeToParcel(reply, 1);
                        return true;
                    }
                    reply.writeInt(0);
                    return true;
                case 36:
                    data.enforceInterface(DESCRIPTOR);
                    int _arg030 = data.readInt();
                    boolean _arg19 = data.readInt() != 0;
                    setDataDependency(_arg030, _arg19);
                    reply.writeNoException();
                    return true;
                case 37:
                    data.enforceInterface(DESCRIPTOR);
                    String _arg031 = data.readString();
                    String _arg110 = data.readString();
                    boolean _result33 = prepareVpn(_arg031, _arg110);
                    reply.writeNoException();
                    reply.writeInt(_result33 ? 1 : 0);
                    return true;
                case 38:
                    data.enforceInterface(DESCRIPTOR);
                    boolean _arg032 = data.readInt() != 0;
                    setVpnPackageAuthorization(_arg032);
                    reply.writeNoException();
                    return true;
                case 39:
                    data.enforceInterface(DESCRIPTOR);
                    if (data.readInt() != 0) {
                        _arg012 = VpnConfig.CREATOR.createFromParcel(data);
                    } else {
                        _arg012 = null;
                    }
                    ParcelFileDescriptor _result34 = establishVpn(_arg012);
                    reply.writeNoException();
                    if (_result34 != null) {
                        reply.writeInt(1);
                        _result34.writeToParcel(reply, 1);
                        return true;
                    }
                    reply.writeInt(0);
                    return true;
                case 40:
                    data.enforceInterface(DESCRIPTOR);
                    VpnConfig _result35 = getVpnConfig();
                    reply.writeNoException();
                    if (_result35 != null) {
                        reply.writeInt(1);
                        _result35.writeToParcel(reply, 1);
                        return true;
                    }
                    reply.writeInt(0);
                    return true;
                case 41:
                    data.enforceInterface(DESCRIPTOR);
                    if (data.readInt() != 0) {
                        _arg011 = VpnProfile.CREATOR.createFromParcel(data);
                    } else {
                        _arg011 = null;
                    }
                    startLegacyVpn(_arg011);
                    reply.writeNoException();
                    return true;
                case 42:
                    data.enforceInterface(DESCRIPTOR);
                    LegacyVpnInfo _result36 = getLegacyVpnInfo();
                    reply.writeNoException();
                    if (_result36 != null) {
                        reply.writeInt(1);
                        _result36.writeToParcel(reply, 1);
                        return true;
                    }
                    reply.writeInt(0);
                    return true;
                case 43:
                    data.enforceInterface(DESCRIPTOR);
                    boolean _result37 = updateLockdownVpn();
                    reply.writeNoException();
                    reply.writeInt(_result37 ? 1 : 0);
                    return true;
                case 44:
                    data.enforceInterface(DESCRIPTOR);
                    if (data.readInt() != 0) {
                        _arg010 = NetworkInfo.CREATOR.createFromParcel(data);
                    } else {
                        _arg010 = null;
                    }
                    boolean _arg111 = data.readInt() != 0;
                    captivePortalCheckCompleted(_arg010, _arg111);
                    reply.writeNoException();
                    return true;
                case 45:
                    data.enforceInterface(DESCRIPTOR);
                    int _arg033 = data.readInt();
                    if (data.readInt() != 0) {
                        _arg16 = Messenger.CREATOR.createFromParcel(data);
                    } else {
                        _arg16 = null;
                    }
                    supplyMessenger(_arg033, _arg16);
                    reply.writeNoException();
                    return true;
                case 46:
                    data.enforceInterface(DESCRIPTOR);
                    String _arg034 = data.readString();
                    int _result38 = findConnectionTypeForIface(_arg034);
                    reply.writeNoException();
                    reply.writeInt(_result38);
                    return true;
                case 47:
                    data.enforceInterface(DESCRIPTOR);
                    int _arg035 = data.readInt();
                    int _result39 = checkMobileProvisioning(_arg035);
                    reply.writeNoException();
                    reply.writeInt(_result39);
                    return true;
                case 48:
                    data.enforceInterface(DESCRIPTOR);
                    String _result40 = getMobileProvisioningUrl();
                    reply.writeNoException();
                    reply.writeString(_result40);
                    return true;
                case 49:
                    data.enforceInterface(DESCRIPTOR);
                    String _result41 = getMobileRedirectedProvisioningUrl();
                    reply.writeNoException();
                    reply.writeString(_result41);
                    return true;
                case 50:
                    data.enforceInterface(DESCRIPTOR);
                    boolean _arg036 = data.readInt() != 0;
                    int _arg112 = data.readInt();
                    String _arg22 = data.readString();
                    setProvisioningNotificationVisible(_arg036, _arg112, _arg22);
                    reply.writeNoException();
                    return true;
                case 51:
                    data.enforceInterface(DESCRIPTOR);
                    boolean _arg037 = data.readInt() != 0;
                    setAirplaneMode(_arg037);
                    reply.writeNoException();
                    return true;
                case 52:
                    data.enforceInterface(DESCRIPTOR);
                    if (data.readInt() != 0) {
                        _arg09 = Messenger.CREATOR.createFromParcel(data);
                    } else {
                        _arg09 = null;
                    }
                    String _arg113 = data.readString();
                    registerNetworkFactory(_arg09, _arg113);
                    reply.writeNoException();
                    return true;
                case 53:
                    data.enforceInterface(DESCRIPTOR);
                    if (data.readInt() != 0) {
                        _arg08 = Messenger.CREATOR.createFromParcel(data);
                    } else {
                        _arg08 = null;
                    }
                    unregisterNetworkFactory(_arg08);
                    reply.writeNoException();
                    return true;
                case 54:
                    data.enforceInterface(DESCRIPTOR);
                    if (data.readInt() != 0) {
                        _arg07 = Messenger.CREATOR.createFromParcel(data);
                    } else {
                        _arg07 = null;
                    }
                    if (data.readInt() != 0) {
                        _arg15 = NetworkInfo.CREATOR.createFromParcel(data);
                    } else {
                        _arg15 = null;
                    }
                    if (data.readInt() != 0) {
                        _arg2 = LinkProperties.CREATOR.createFromParcel(data);
                    } else {
                        _arg2 = null;
                    }
                    if (data.readInt() != 0) {
                        _arg3 = NetworkCapabilities.CREATOR.createFromParcel(data);
                    } else {
                        _arg3 = null;
                    }
                    int _arg4 = data.readInt();
                    if (data.readInt() != 0) {
                        _arg5 = NetworkMisc.CREATOR.createFromParcel(data);
                    } else {
                        _arg5 = null;
                    }
                    registerNetworkAgent(_arg07, _arg15, _arg2, _arg3, _arg4, _arg5);
                    reply.writeNoException();
                    return true;
                case 55:
                    data.enforceInterface(DESCRIPTOR);
                    if (data.readInt() != 0) {
                        _arg06 = NetworkCapabilities.CREATOR.createFromParcel(data);
                    } else {
                        _arg06 = null;
                    }
                    if (data.readInt() != 0) {
                        _arg14 = Messenger.CREATOR.createFromParcel(data);
                    } else {
                        _arg14 = null;
                    }
                    int _arg23 = data.readInt();
                    IBinder _arg32 = data.readStrongBinder();
                    int _arg42 = data.readInt();
                    NetworkRequest _result42 = requestNetwork(_arg06, _arg14, _arg23, _arg32, _arg42);
                    reply.writeNoException();
                    if (_result42 != null) {
                        reply.writeInt(1);
                        _result42.writeToParcel(reply, 1);
                        return true;
                    }
                    reply.writeInt(0);
                    return true;
                case 56:
                    data.enforceInterface(DESCRIPTOR);
                    if (data.readInt() != 0) {
                        _arg05 = NetworkCapabilities.CREATOR.createFromParcel(data);
                    } else {
                        _arg05 = null;
                    }
                    if (data.readInt() != 0) {
                        _arg13 = PendingIntent.CREATOR.createFromParcel(data);
                    } else {
                        _arg13 = null;
                    }
                    NetworkRequest _result43 = pendingRequestForNetwork(_arg05, _arg13);
                    reply.writeNoException();
                    if (_result43 != null) {
                        reply.writeInt(1);
                        _result43.writeToParcel(reply, 1);
                        return true;
                    }
                    reply.writeInt(0);
                    return true;
                case 57:
                    data.enforceInterface(DESCRIPTOR);
                    if (data.readInt() != 0) {
                        _arg04 = PendingIntent.CREATOR.createFromParcel(data);
                    } else {
                        _arg04 = null;
                    }
                    releasePendingNetworkRequest(_arg04);
                    reply.writeNoException();
                    return true;
                case 58:
                    data.enforceInterface(DESCRIPTOR);
                    if (data.readInt() != 0) {
                        _arg03 = NetworkCapabilities.CREATOR.createFromParcel(data);
                    } else {
                        _arg03 = null;
                    }
                    if (data.readInt() != 0) {
                        _arg12 = Messenger.CREATOR.createFromParcel(data);
                    } else {
                        _arg12 = null;
                    }
                    IBinder _arg24 = data.readStrongBinder();
                    NetworkRequest _result44 = listenForNetwork(_arg03, _arg12, _arg24);
                    reply.writeNoException();
                    if (_result44 != null) {
                        reply.writeInt(1);
                        _result44.writeToParcel(reply, 1);
                        return true;
                    }
                    reply.writeInt(0);
                    return true;
                case 59:
                    data.enforceInterface(DESCRIPTOR);
                    if (data.readInt() != 0) {
                        _arg02 = NetworkCapabilities.CREATOR.createFromParcel(data);
                    } else {
                        _arg02 = null;
                    }
                    if (data.readInt() != 0) {
                        _arg1 = PendingIntent.CREATOR.createFromParcel(data);
                    } else {
                        _arg1 = null;
                    }
                    pendingListenForNetwork(_arg02, _arg1);
                    reply.writeNoException();
                    return true;
                case 60:
                    data.enforceInterface(DESCRIPTOR);
                    if (data.readInt() != 0) {
                        _arg0 = NetworkRequest.CREATOR.createFromParcel(data);
                    } else {
                        _arg0 = null;
                    }
                    releaseNetworkRequest(_arg0);
                    reply.writeNoException();
                    return true;
                case 61:
                    data.enforceInterface(DESCRIPTOR);
                    int _arg038 = data.readInt();
                    int _result45 = getRestoreDefaultNetworkDelay(_arg038);
                    reply.writeNoException();
                    reply.writeInt(_result45);
                    return true;
                case 62:
                    data.enforceInterface(DESCRIPTOR);
                    String _arg039 = data.readString();
                    int _arg114 = data.readInt();
                    boolean _result46 = addVpnAddress(_arg039, _arg114);
                    reply.writeNoException();
                    reply.writeInt(_result46 ? 1 : 0);
                    return true;
                case 63:
                    data.enforceInterface(DESCRIPTOR);
                    String _arg040 = data.readString();
                    int _arg115 = data.readInt();
                    boolean _result47 = removeVpnAddress(_arg040, _arg115);
                    reply.writeNoException();
                    reply.writeInt(_result47 ? 1 : 0);
                    return true;
                case 64:
                    data.enforceInterface(DESCRIPTOR);
                    Network[] _arg041 = (Network[]) data.createTypedArray(Network.CREATOR);
                    boolean _result48 = setUnderlyingNetworksForVpn(_arg041);
                    reply.writeNoException();
                    reply.writeInt(_result48 ? 1 : 0);
                    return true;
                case IBinder.INTERFACE_TRANSACTION:
                    reply.writeString(DESCRIPTOR);
                    return true;
                default:
                    return super.onTransact(code, data, reply, flags);
            }
        }

        private static class Proxy implements IConnectivityManager {
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
            public NetworkInfo getActiveNetworkInfo() throws RemoteException {
                NetworkInfo _result;
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    this.mRemote.transact(1, _data, _reply, 0);
                    _reply.readException();
                    if (_reply.readInt() != 0) {
                        _result = NetworkInfo.CREATOR.createFromParcel(_reply);
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
            public NetworkInfo getActiveNetworkInfoForUid(int uid) throws RemoteException {
                NetworkInfo _result;
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(uid);
                    this.mRemote.transact(2, _data, _reply, 0);
                    _reply.readException();
                    if (_reply.readInt() != 0) {
                        _result = NetworkInfo.CREATOR.createFromParcel(_reply);
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
            public NetworkInfo getNetworkInfo(int networkType) throws RemoteException {
                NetworkInfo _result;
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(networkType);
                    this.mRemote.transact(3, _data, _reply, 0);
                    _reply.readException();
                    if (_reply.readInt() != 0) {
                        _result = NetworkInfo.CREATOR.createFromParcel(_reply);
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
            public NetworkInfo getNetworkInfoForNetwork(Network network) throws RemoteException {
                NetworkInfo _result;
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
                    this.mRemote.transact(4, _data, _reply, 0);
                    _reply.readException();
                    if (_reply.readInt() != 0) {
                        _result = NetworkInfo.CREATOR.createFromParcel(_reply);
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
            public NetworkInfo[] getAllNetworkInfo() throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    this.mRemote.transact(5, _data, _reply, 0);
                    _reply.readException();
                    NetworkInfo[] _result = (NetworkInfo[]) _reply.createTypedArray(NetworkInfo.CREATOR);
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override
            public Network getNetworkForType(int networkType) throws RemoteException {
                Network _result;
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(networkType);
                    this.mRemote.transact(6, _data, _reply, 0);
                    _reply.readException();
                    if (_reply.readInt() != 0) {
                        _result = Network.CREATOR.createFromParcel(_reply);
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
            public Network[] getAllNetworks() throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    this.mRemote.transact(7, _data, _reply, 0);
                    _reply.readException();
                    Network[] _result = (Network[]) _reply.createTypedArray(Network.CREATOR);
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override
            public NetworkCapabilities[] getDefaultNetworkCapabilitiesForUser(int userId) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(userId);
                    this.mRemote.transact(8, _data, _reply, 0);
                    _reply.readException();
                    NetworkCapabilities[] _result = (NetworkCapabilities[]) _reply.createTypedArray(NetworkCapabilities.CREATOR);
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override
            public NetworkInfo getProvisioningOrActiveNetworkInfo() throws RemoteException {
                NetworkInfo _result;
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    this.mRemote.transact(9, _data, _reply, 0);
                    _reply.readException();
                    if (_reply.readInt() != 0) {
                        _result = NetworkInfo.CREATOR.createFromParcel(_reply);
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
            public boolean isNetworkSupported(int networkType) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(networkType);
                    this.mRemote.transact(10, _data, _reply, 0);
                    _reply.readException();
                    boolean _result = _reply.readInt() != 0;
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override
            public LinkProperties getActiveLinkProperties() throws RemoteException {
                LinkProperties _result;
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    this.mRemote.transact(11, _data, _reply, 0);
                    _reply.readException();
                    if (_reply.readInt() != 0) {
                        _result = LinkProperties.CREATOR.createFromParcel(_reply);
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
            public LinkProperties getLinkPropertiesForType(int networkType) throws RemoteException {
                LinkProperties _result;
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(networkType);
                    this.mRemote.transact(12, _data, _reply, 0);
                    _reply.readException();
                    if (_reply.readInt() != 0) {
                        _result = LinkProperties.CREATOR.createFromParcel(_reply);
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
            public LinkProperties getLinkProperties(Network network) throws RemoteException {
                LinkProperties _result;
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
                    this.mRemote.transact(13, _data, _reply, 0);
                    _reply.readException();
                    if (_reply.readInt() != 0) {
                        _result = LinkProperties.CREATOR.createFromParcel(_reply);
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
            public NetworkCapabilities getNetworkCapabilities(Network network) throws RemoteException {
                NetworkCapabilities _result;
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
                    this.mRemote.transact(14, _data, _reply, 0);
                    _reply.readException();
                    if (_reply.readInt() != 0) {
                        _result = NetworkCapabilities.CREATOR.createFromParcel(_reply);
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
            public NetworkState[] getAllNetworkState() throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    this.mRemote.transact(15, _data, _reply, 0);
                    _reply.readException();
                    NetworkState[] _result = (NetworkState[]) _reply.createTypedArray(NetworkState.CREATOR);
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override
            public NetworkQuotaInfo getActiveNetworkQuotaInfo() throws RemoteException {
                NetworkQuotaInfo _result;
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    this.mRemote.transact(16, _data, _reply, 0);
                    _reply.readException();
                    if (_reply.readInt() != 0) {
                        _result = NetworkQuotaInfo.CREATOR.createFromParcel(_reply);
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
            public boolean isActiveNetworkMetered() throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    this.mRemote.transact(17, _data, _reply, 0);
                    _reply.readException();
                    boolean _result = _reply.readInt() != 0;
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override
            public boolean requestRouteToHostAddress(int networkType, byte[] hostAddress) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(networkType);
                    _data.writeByteArray(hostAddress);
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
            public int tether(String iface) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeString(iface);
                    this.mRemote.transact(19, _data, _reply, 0);
                    _reply.readException();
                    int _result = _reply.readInt();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override
            public int untether(String iface) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeString(iface);
                    this.mRemote.transact(20, _data, _reply, 0);
                    _reply.readException();
                    int _result = _reply.readInt();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override
            public int getLastTetherError(String iface) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeString(iface);
                    this.mRemote.transact(21, _data, _reply, 0);
                    _reply.readException();
                    int _result = _reply.readInt();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override
            public boolean isTetheringSupported() throws RemoteException {
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
            public String[] getTetherableIfaces() throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    this.mRemote.transact(23, _data, _reply, 0);
                    _reply.readException();
                    String[] _result = _reply.createStringArray();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override
            public String[] getTetheredIfaces() throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    this.mRemote.transact(24, _data, _reply, 0);
                    _reply.readException();
                    String[] _result = _reply.createStringArray();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override
            public String[] getTetheringErroredIfaces() throws RemoteException {
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
            public String[] getTetheredDhcpRanges() throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    this.mRemote.transact(26, _data, _reply, 0);
                    _reply.readException();
                    String[] _result = _reply.createStringArray();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override
            public String[] getTetherableUsbRegexs() throws RemoteException {
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
            public String[] getTetherableWifiRegexs() throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    this.mRemote.transact(28, _data, _reply, 0);
                    _reply.readException();
                    String[] _result = _reply.createStringArray();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override
            public String[] getTetherableBluetoothRegexs() throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    this.mRemote.transact(29, _data, _reply, 0);
                    _reply.readException();
                    String[] _result = _reply.createStringArray();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override
            public int setUsbTethering(boolean enable) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(enable ? 1 : 0);
                    this.mRemote.transact(30, _data, _reply, 0);
                    _reply.readException();
                    int _result = _reply.readInt();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override
            public void reportInetCondition(int networkType, int percentage) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(networkType);
                    _data.writeInt(percentage);
                    this.mRemote.transact(31, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override
            public void reportBadNetwork(Network network) throws RemoteException {
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
                    this.mRemote.transact(32, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override
            public ProxyInfo getGlobalProxy() throws RemoteException {
                ProxyInfo _result;
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    this.mRemote.transact(33, _data, _reply, 0);
                    _reply.readException();
                    if (_reply.readInt() != 0) {
                        _result = ProxyInfo.CREATOR.createFromParcel(_reply);
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
            public void setGlobalProxy(ProxyInfo p) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    if (p != null) {
                        _data.writeInt(1);
                        p.writeToParcel(_data, 0);
                    } else {
                        _data.writeInt(0);
                    }
                    this.mRemote.transact(34, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override
            public ProxyInfo getDefaultProxy() throws RemoteException {
                ProxyInfo _result;
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    this.mRemote.transact(35, _data, _reply, 0);
                    _reply.readException();
                    if (_reply.readInt() != 0) {
                        _result = ProxyInfo.CREATOR.createFromParcel(_reply);
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
            public void setDataDependency(int networkType, boolean met) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(networkType);
                    _data.writeInt(met ? 1 : 0);
                    this.mRemote.transact(36, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override
            public boolean prepareVpn(String oldPackage, String newPackage) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeString(oldPackage);
                    _data.writeString(newPackage);
                    this.mRemote.transact(37, _data, _reply, 0);
                    _reply.readException();
                    boolean _result = _reply.readInt() != 0;
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override
            public void setVpnPackageAuthorization(boolean authorized) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(authorized ? 1 : 0);
                    this.mRemote.transact(38, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override
            public ParcelFileDescriptor establishVpn(VpnConfig config) throws RemoteException {
                ParcelFileDescriptor _result;
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    if (config != null) {
                        _data.writeInt(1);
                        config.writeToParcel(_data, 0);
                    } else {
                        _data.writeInt(0);
                    }
                    this.mRemote.transact(39, _data, _reply, 0);
                    _reply.readException();
                    if (_reply.readInt() != 0) {
                        _result = ParcelFileDescriptor.CREATOR.createFromParcel(_reply);
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
            public VpnConfig getVpnConfig() throws RemoteException {
                VpnConfig _result;
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    this.mRemote.transact(40, _data, _reply, 0);
                    _reply.readException();
                    if (_reply.readInt() != 0) {
                        _result = VpnConfig.CREATOR.createFromParcel(_reply);
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
            public void startLegacyVpn(VpnProfile profile) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    if (profile != null) {
                        _data.writeInt(1);
                        profile.writeToParcel(_data, 0);
                    } else {
                        _data.writeInt(0);
                    }
                    this.mRemote.transact(41, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override
            public LegacyVpnInfo getLegacyVpnInfo() throws RemoteException {
                LegacyVpnInfo _result;
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    this.mRemote.transact(42, _data, _reply, 0);
                    _reply.readException();
                    if (_reply.readInt() != 0) {
                        _result = LegacyVpnInfo.CREATOR.createFromParcel(_reply);
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
            public boolean updateLockdownVpn() throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    this.mRemote.transact(43, _data, _reply, 0);
                    _reply.readException();
                    boolean _result = _reply.readInt() != 0;
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override
            public void captivePortalCheckCompleted(NetworkInfo info, boolean isCaptivePortal) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    if (info != null) {
                        _data.writeInt(1);
                        info.writeToParcel(_data, 0);
                    } else {
                        _data.writeInt(0);
                    }
                    _data.writeInt(isCaptivePortal ? 1 : 0);
                    this.mRemote.transact(44, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override
            public void supplyMessenger(int networkType, Messenger messenger) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(networkType);
                    if (messenger != null) {
                        _data.writeInt(1);
                        messenger.writeToParcel(_data, 0);
                    } else {
                        _data.writeInt(0);
                    }
                    this.mRemote.transact(45, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override
            public int findConnectionTypeForIface(String iface) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeString(iface);
                    this.mRemote.transact(46, _data, _reply, 0);
                    _reply.readException();
                    int _result = _reply.readInt();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override
            public int checkMobileProvisioning(int suggestedTimeOutMs) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(suggestedTimeOutMs);
                    this.mRemote.transact(47, _data, _reply, 0);
                    _reply.readException();
                    int _result = _reply.readInt();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override
            public String getMobileProvisioningUrl() throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    this.mRemote.transact(48, _data, _reply, 0);
                    _reply.readException();
                    String _result = _reply.readString();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override
            public String getMobileRedirectedProvisioningUrl() throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    this.mRemote.transact(49, _data, _reply, 0);
                    _reply.readException();
                    String _result = _reply.readString();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override
            public void setProvisioningNotificationVisible(boolean visible, int networkType, String action) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(visible ? 1 : 0);
                    _data.writeInt(networkType);
                    _data.writeString(action);
                    this.mRemote.transact(50, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override
            public void setAirplaneMode(boolean enable) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(enable ? 1 : 0);
                    this.mRemote.transact(51, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override
            public void registerNetworkFactory(Messenger messenger, String name) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    if (messenger != null) {
                        _data.writeInt(1);
                        messenger.writeToParcel(_data, 0);
                    } else {
                        _data.writeInt(0);
                    }
                    _data.writeString(name);
                    this.mRemote.transact(52, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override
            public void unregisterNetworkFactory(Messenger messenger) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    if (messenger != null) {
                        _data.writeInt(1);
                        messenger.writeToParcel(_data, 0);
                    } else {
                        _data.writeInt(0);
                    }
                    this.mRemote.transact(53, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override
            public void registerNetworkAgent(Messenger messenger, NetworkInfo ni, LinkProperties lp, NetworkCapabilities nc, int score, NetworkMisc misc) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    if (messenger != null) {
                        _data.writeInt(1);
                        messenger.writeToParcel(_data, 0);
                    } else {
                        _data.writeInt(0);
                    }
                    if (ni != null) {
                        _data.writeInt(1);
                        ni.writeToParcel(_data, 0);
                    } else {
                        _data.writeInt(0);
                    }
                    if (lp != null) {
                        _data.writeInt(1);
                        lp.writeToParcel(_data, 0);
                    } else {
                        _data.writeInt(0);
                    }
                    if (nc != null) {
                        _data.writeInt(1);
                        nc.writeToParcel(_data, 0);
                    } else {
                        _data.writeInt(0);
                    }
                    _data.writeInt(score);
                    if (misc != null) {
                        _data.writeInt(1);
                        misc.writeToParcel(_data, 0);
                    } else {
                        _data.writeInt(0);
                    }
                    this.mRemote.transact(54, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override
            public NetworkRequest requestNetwork(NetworkCapabilities networkCapabilities, Messenger messenger, int timeoutSec, IBinder binder, int legacy) throws RemoteException {
                NetworkRequest _result;
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    if (networkCapabilities != null) {
                        _data.writeInt(1);
                        networkCapabilities.writeToParcel(_data, 0);
                    } else {
                        _data.writeInt(0);
                    }
                    if (messenger != null) {
                        _data.writeInt(1);
                        messenger.writeToParcel(_data, 0);
                    } else {
                        _data.writeInt(0);
                    }
                    _data.writeInt(timeoutSec);
                    _data.writeStrongBinder(binder);
                    _data.writeInt(legacy);
                    this.mRemote.transact(55, _data, _reply, 0);
                    _reply.readException();
                    if (_reply.readInt() != 0) {
                        _result = NetworkRequest.CREATOR.createFromParcel(_reply);
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
            public NetworkRequest pendingRequestForNetwork(NetworkCapabilities networkCapabilities, PendingIntent operation) throws RemoteException {
                NetworkRequest _result;
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    if (networkCapabilities != null) {
                        _data.writeInt(1);
                        networkCapabilities.writeToParcel(_data, 0);
                    } else {
                        _data.writeInt(0);
                    }
                    if (operation != null) {
                        _data.writeInt(1);
                        operation.writeToParcel(_data, 0);
                    } else {
                        _data.writeInt(0);
                    }
                    this.mRemote.transact(56, _data, _reply, 0);
                    _reply.readException();
                    if (_reply.readInt() != 0) {
                        _result = NetworkRequest.CREATOR.createFromParcel(_reply);
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
            public void releasePendingNetworkRequest(PendingIntent operation) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    if (operation != null) {
                        _data.writeInt(1);
                        operation.writeToParcel(_data, 0);
                    } else {
                        _data.writeInt(0);
                    }
                    this.mRemote.transact(57, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override
            public NetworkRequest listenForNetwork(NetworkCapabilities networkCapabilities, Messenger messenger, IBinder binder) throws RemoteException {
                NetworkRequest _result;
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    if (networkCapabilities != null) {
                        _data.writeInt(1);
                        networkCapabilities.writeToParcel(_data, 0);
                    } else {
                        _data.writeInt(0);
                    }
                    if (messenger != null) {
                        _data.writeInt(1);
                        messenger.writeToParcel(_data, 0);
                    } else {
                        _data.writeInt(0);
                    }
                    _data.writeStrongBinder(binder);
                    this.mRemote.transact(58, _data, _reply, 0);
                    _reply.readException();
                    if (_reply.readInt() != 0) {
                        _result = NetworkRequest.CREATOR.createFromParcel(_reply);
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
            public void pendingListenForNetwork(NetworkCapabilities networkCapabilities, PendingIntent operation) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    if (networkCapabilities != null) {
                        _data.writeInt(1);
                        networkCapabilities.writeToParcel(_data, 0);
                    } else {
                        _data.writeInt(0);
                    }
                    if (operation != null) {
                        _data.writeInt(1);
                        operation.writeToParcel(_data, 0);
                    } else {
                        _data.writeInt(0);
                    }
                    this.mRemote.transact(59, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override
            public void releaseNetworkRequest(NetworkRequest networkRequest) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    if (networkRequest != null) {
                        _data.writeInt(1);
                        networkRequest.writeToParcel(_data, 0);
                    } else {
                        _data.writeInt(0);
                    }
                    this.mRemote.transact(60, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override
            public int getRestoreDefaultNetworkDelay(int networkType) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(networkType);
                    this.mRemote.transact(61, _data, _reply, 0);
                    _reply.readException();
                    int _result = _reply.readInt();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override
            public boolean addVpnAddress(String address, int prefixLength) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeString(address);
                    _data.writeInt(prefixLength);
                    this.mRemote.transact(62, _data, _reply, 0);
                    _reply.readException();
                    boolean _result = _reply.readInt() != 0;
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override
            public boolean removeVpnAddress(String address, int prefixLength) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeString(address);
                    _data.writeInt(prefixLength);
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
            public boolean setUnderlyingNetworksForVpn(Network[] networks) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeTypedArray(networks, 0);
                    this.mRemote.transact(64, _data, _reply, 0);
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
