package android.hardware.wifi.supplicant.V1_0;

import android.hardware.wifi.supplicant.V1_0.ISupplicantNetwork;
import android.hardware.wifi.supplicant.V1_0.ISupplicantStaIfaceCallback;
import android.hidl.base.V1_0.DebugInfo;
import android.hidl.base.V1_0.IBase;
import android.os.HidlSupport;
import android.os.HwBinder;
import android.os.HwBlob;
import android.os.HwParcel;
import android.os.IHwBinder;
import android.os.IHwInterface;
import android.os.RemoteException;
import com.android.server.wifi.HalDeviceManager;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.Objects;

public interface ISupplicantStaNetwork extends ISupplicantNetwork {
    public static final String kInterfaceName = "android.hardware.wifi.supplicant@1.0::ISupplicantStaNetwork";

    @FunctionalInterface
    public interface getAuthAlgCallback {
        void onValues(SupplicantStatus supplicantStatus, int i);
    }

    @FunctionalInterface
    public interface getBssidCallback {
        void onValues(SupplicantStatus supplicantStatus, byte[] bArr);
    }

    @FunctionalInterface
    public interface getEapAltSubjectMatchCallback {
        void onValues(SupplicantStatus supplicantStatus, String str);
    }

    @FunctionalInterface
    public interface getEapAnonymousIdentityCallback {
        void onValues(SupplicantStatus supplicantStatus, ArrayList<Byte> arrayList);
    }

    @FunctionalInterface
    public interface getEapCACertCallback {
        void onValues(SupplicantStatus supplicantStatus, String str);
    }

    @FunctionalInterface
    public interface getEapCAPathCallback {
        void onValues(SupplicantStatus supplicantStatus, String str);
    }

    @FunctionalInterface
    public interface getEapClientCertCallback {
        void onValues(SupplicantStatus supplicantStatus, String str);
    }

    @FunctionalInterface
    public interface getEapDomainSuffixMatchCallback {
        void onValues(SupplicantStatus supplicantStatus, String str);
    }

    @FunctionalInterface
    public interface getEapEngineCallback {
        void onValues(SupplicantStatus supplicantStatus, boolean z);
    }

    @FunctionalInterface
    public interface getEapEngineIDCallback {
        void onValues(SupplicantStatus supplicantStatus, String str);
    }

    @FunctionalInterface
    public interface getEapIdentityCallback {
        void onValues(SupplicantStatus supplicantStatus, ArrayList<Byte> arrayList);
    }

    @FunctionalInterface
    public interface getEapMethodCallback {
        void onValues(SupplicantStatus supplicantStatus, int i);
    }

    @FunctionalInterface
    public interface getEapPasswordCallback {
        void onValues(SupplicantStatus supplicantStatus, ArrayList<Byte> arrayList);
    }

    @FunctionalInterface
    public interface getEapPhase2MethodCallback {
        void onValues(SupplicantStatus supplicantStatus, int i);
    }

    @FunctionalInterface
    public interface getEapPrivateKeyIdCallback {
        void onValues(SupplicantStatus supplicantStatus, String str);
    }

    @FunctionalInterface
    public interface getEapSubjectMatchCallback {
        void onValues(SupplicantStatus supplicantStatus, String str);
    }

    @FunctionalInterface
    public interface getGroupCipherCallback {
        void onValues(SupplicantStatus supplicantStatus, int i);
    }

    @FunctionalInterface
    public interface getIdStrCallback {
        void onValues(SupplicantStatus supplicantStatus, String str);
    }

    @FunctionalInterface
    public interface getKeyMgmtCallback {
        void onValues(SupplicantStatus supplicantStatus, int i);
    }

    @FunctionalInterface
    public interface getPairwiseCipherCallback {
        void onValues(SupplicantStatus supplicantStatus, int i);
    }

    @FunctionalInterface
    public interface getProtoCallback {
        void onValues(SupplicantStatus supplicantStatus, int i);
    }

    @FunctionalInterface
    public interface getPskCallback {
        void onValues(SupplicantStatus supplicantStatus, byte[] bArr);
    }

    @FunctionalInterface
    public interface getPskPassphraseCallback {
        void onValues(SupplicantStatus supplicantStatus, String str);
    }

    @FunctionalInterface
    public interface getRequirePmfCallback {
        void onValues(SupplicantStatus supplicantStatus, boolean z);
    }

    @FunctionalInterface
    public interface getScanSsidCallback {
        void onValues(SupplicantStatus supplicantStatus, boolean z);
    }

    @FunctionalInterface
    public interface getSsidCallback {
        void onValues(SupplicantStatus supplicantStatus, ArrayList<Byte> arrayList);
    }

    @FunctionalInterface
    public interface getWepKeyCallback {
        void onValues(SupplicantStatus supplicantStatus, ArrayList<Byte> arrayList);
    }

    @FunctionalInterface
    public interface getWepTxKeyIdxCallback {
        void onValues(SupplicantStatus supplicantStatus, int i);
    }

    @FunctionalInterface
    public interface getWpsNfcConfigurationTokenCallback {
        void onValues(SupplicantStatus supplicantStatus, ArrayList<Byte> arrayList);
    }

    @Override
    IHwBinder asBinder();

    SupplicantStatus disable() throws RemoteException;

    SupplicantStatus enable(boolean z) throws RemoteException;

    void getAuthAlg(getAuthAlgCallback getauthalgcallback) throws RemoteException;

    void getBssid(getBssidCallback getbssidcallback) throws RemoteException;

    @Override
    DebugInfo getDebugInfo() throws RemoteException;

    void getEapAltSubjectMatch(getEapAltSubjectMatchCallback geteapaltsubjectmatchcallback) throws RemoteException;

    void getEapAnonymousIdentity(getEapAnonymousIdentityCallback geteapanonymousidentitycallback) throws RemoteException;

    void getEapCACert(getEapCACertCallback geteapcacertcallback) throws RemoteException;

    void getEapCAPath(getEapCAPathCallback geteapcapathcallback) throws RemoteException;

    void getEapClientCert(getEapClientCertCallback geteapclientcertcallback) throws RemoteException;

    void getEapDomainSuffixMatch(getEapDomainSuffixMatchCallback geteapdomainsuffixmatchcallback) throws RemoteException;

    void getEapEngine(getEapEngineCallback geteapenginecallback) throws RemoteException;

    void getEapEngineID(getEapEngineIDCallback geteapengineidcallback) throws RemoteException;

    void getEapIdentity(getEapIdentityCallback geteapidentitycallback) throws RemoteException;

    void getEapMethod(getEapMethodCallback geteapmethodcallback) throws RemoteException;

    void getEapPassword(getEapPasswordCallback geteappasswordcallback) throws RemoteException;

    void getEapPhase2Method(getEapPhase2MethodCallback geteapphase2methodcallback) throws RemoteException;

    void getEapPrivateKeyId(getEapPrivateKeyIdCallback geteapprivatekeyidcallback) throws RemoteException;

    void getEapSubjectMatch(getEapSubjectMatchCallback geteapsubjectmatchcallback) throws RemoteException;

    void getGroupCipher(getGroupCipherCallback getgroupciphercallback) throws RemoteException;

    @Override
    ArrayList<byte[]> getHashChain() throws RemoteException;

    void getIdStr(getIdStrCallback getidstrcallback) throws RemoteException;

    void getKeyMgmt(getKeyMgmtCallback getkeymgmtcallback) throws RemoteException;

    void getPairwiseCipher(getPairwiseCipherCallback getpairwiseciphercallback) throws RemoteException;

    void getProto(getProtoCallback getprotocallback) throws RemoteException;

    void getPsk(getPskCallback getpskcallback) throws RemoteException;

    void getPskPassphrase(getPskPassphraseCallback getpskpassphrasecallback) throws RemoteException;

    void getRequirePmf(getRequirePmfCallback getrequirepmfcallback) throws RemoteException;

    void getScanSsid(getScanSsidCallback getscanssidcallback) throws RemoteException;

    void getSsid(getSsidCallback getssidcallback) throws RemoteException;

    void getWepKey(int i, getWepKeyCallback getwepkeycallback) throws RemoteException;

    void getWepTxKeyIdx(getWepTxKeyIdxCallback getweptxkeyidxcallback) throws RemoteException;

    void getWpsNfcConfigurationToken(getWpsNfcConfigurationTokenCallback getwpsnfcconfigurationtokencallback) throws RemoteException;

    @Override
    ArrayList<String> interfaceChain() throws RemoteException;

    @Override
    String interfaceDescriptor() throws RemoteException;

    @Override
    boolean linkToDeath(IHwBinder.DeathRecipient deathRecipient, long j) throws RemoteException;

    @Override
    void notifySyspropsChanged() throws RemoteException;

    @Override
    void ping() throws RemoteException;

    SupplicantStatus registerCallback(ISupplicantStaNetworkCallback iSupplicantStaNetworkCallback) throws RemoteException;

    SupplicantStatus select() throws RemoteException;

    SupplicantStatus sendNetworkEapIdentityResponse(ArrayList<Byte> arrayList) throws RemoteException;

    SupplicantStatus sendNetworkEapSimGsmAuthFailure() throws RemoteException;

    SupplicantStatus sendNetworkEapSimGsmAuthResponse(ArrayList<NetworkResponseEapSimGsmAuthParams> arrayList) throws RemoteException;

    SupplicantStatus sendNetworkEapSimUmtsAuthFailure() throws RemoteException;

    SupplicantStatus sendNetworkEapSimUmtsAuthResponse(NetworkResponseEapSimUmtsAuthParams networkResponseEapSimUmtsAuthParams) throws RemoteException;

    SupplicantStatus sendNetworkEapSimUmtsAutsResponse(byte[] bArr) throws RemoteException;

    SupplicantStatus setAuthAlg(int i) throws RemoteException;

    SupplicantStatus setBssid(byte[] bArr) throws RemoteException;

    SupplicantStatus setEapAltSubjectMatch(String str) throws RemoteException;

    SupplicantStatus setEapAnonymousIdentity(ArrayList<Byte> arrayList) throws RemoteException;

    SupplicantStatus setEapCACert(String str) throws RemoteException;

    SupplicantStatus setEapCAPath(String str) throws RemoteException;

    SupplicantStatus setEapClientCert(String str) throws RemoteException;

    SupplicantStatus setEapDomainSuffixMatch(String str) throws RemoteException;

    SupplicantStatus setEapEngine(boolean z) throws RemoteException;

    SupplicantStatus setEapEngineID(String str) throws RemoteException;

    SupplicantStatus setEapIdentity(ArrayList<Byte> arrayList) throws RemoteException;

    SupplicantStatus setEapMethod(int i) throws RemoteException;

    SupplicantStatus setEapPassword(ArrayList<Byte> arrayList) throws RemoteException;

    SupplicantStatus setEapPhase2Method(int i) throws RemoteException;

    SupplicantStatus setEapPrivateKeyId(String str) throws RemoteException;

    SupplicantStatus setEapSubjectMatch(String str) throws RemoteException;

    SupplicantStatus setGroupCipher(int i) throws RemoteException;

    @Override
    void setHALInstrumentation() throws RemoteException;

    SupplicantStatus setIdStr(String str) throws RemoteException;

    SupplicantStatus setKeyMgmt(int i) throws RemoteException;

    SupplicantStatus setPairwiseCipher(int i) throws RemoteException;

    SupplicantStatus setProactiveKeyCaching(boolean z) throws RemoteException;

    SupplicantStatus setProto(int i) throws RemoteException;

    SupplicantStatus setPsk(byte[] bArr) throws RemoteException;

    SupplicantStatus setPskPassphrase(String str) throws RemoteException;

    SupplicantStatus setRequirePmf(boolean z) throws RemoteException;

    SupplicantStatus setScanSsid(boolean z) throws RemoteException;

    SupplicantStatus setSsid(ArrayList<Byte> arrayList) throws RemoteException;

    SupplicantStatus setUpdateIdentifier(int i) throws RemoteException;

    SupplicantStatus setWepKey(int i, ArrayList<Byte> arrayList) throws RemoteException;

    SupplicantStatus setWepTxKeyIdx(int i) throws RemoteException;

    @Override
    boolean unlinkToDeath(IHwBinder.DeathRecipient deathRecipient) throws RemoteException;

    static ISupplicantStaNetwork asInterface(IHwBinder iHwBinder) {
        if (iHwBinder == null) {
            return null;
        }
        IHwInterface iHwInterfaceQueryLocalInterface = iHwBinder.queryLocalInterface(kInterfaceName);
        if (iHwInterfaceQueryLocalInterface != null && (iHwInterfaceQueryLocalInterface instanceof ISupplicantStaNetwork)) {
            return (ISupplicantStaNetwork) iHwInterfaceQueryLocalInterface;
        }
        Proxy proxy = new Proxy(iHwBinder);
        try {
            Iterator<String> it = proxy.interfaceChain().iterator();
            while (it.hasNext()) {
                if (it.next().equals(kInterfaceName)) {
                    return proxy;
                }
            }
        } catch (RemoteException e) {
        }
        return null;
    }

    static ISupplicantStaNetwork castFrom(IHwInterface iHwInterface) {
        if (iHwInterface == null) {
            return null;
        }
        return asInterface(iHwInterface.asBinder());
    }

    static ISupplicantStaNetwork getService(String str, boolean z) throws RemoteException {
        return asInterface(HwBinder.getService(kInterfaceName, str, z));
    }

    static ISupplicantStaNetwork getService(boolean z) throws RemoteException {
        return getService(HalDeviceManager.HAL_INSTANCE_NAME, z);
    }

    static ISupplicantStaNetwork getService(String str) throws RemoteException {
        return asInterface(HwBinder.getService(kInterfaceName, str));
    }

    static ISupplicantStaNetwork getService() throws RemoteException {
        return getService(HalDeviceManager.HAL_INSTANCE_NAME);
    }

    public static final class ParamSizeLimits {
        public static final int PSK_PASSPHRASE_MAX_LEN_IN_BYTES = 63;
        public static final int PSK_PASSPHRASE_MIN_LEN_IN_BYTES = 8;
        public static final int SSID_MAX_LEN_IN_BYTES = 32;
        public static final int WEP104_KEY_LEN_IN_BYTES = 13;
        public static final int WEP40_KEY_LEN_IN_BYTES = 5;
        public static final int WEP_KEYS_MAX_NUM = 4;

        public static final String toString(int i) {
            if (i == 32) {
                return "SSID_MAX_LEN_IN_BYTES";
            }
            if (i == 8) {
                return "PSK_PASSPHRASE_MIN_LEN_IN_BYTES";
            }
            if (i == 63) {
                return "PSK_PASSPHRASE_MAX_LEN_IN_BYTES";
            }
            if (i == 4) {
                return "WEP_KEYS_MAX_NUM";
            }
            if (i == 5) {
                return "WEP40_KEY_LEN_IN_BYTES";
            }
            if (i == 13) {
                return "WEP104_KEY_LEN_IN_BYTES";
            }
            return "0x" + Integer.toHexString(i);
        }

        public static final String dumpBitfield(int i) {
            ArrayList arrayList = new ArrayList();
            int i2 = 32;
            if ((i & 32) == 32) {
                arrayList.add("SSID_MAX_LEN_IN_BYTES");
            } else {
                i2 = 0;
            }
            if ((i & 8) == 8) {
                arrayList.add("PSK_PASSPHRASE_MIN_LEN_IN_BYTES");
                i2 |= 8;
            }
            if ((i & 63) == 63) {
                arrayList.add("PSK_PASSPHRASE_MAX_LEN_IN_BYTES");
                i2 |= 63;
            }
            if ((i & 4) == 4) {
                arrayList.add("WEP_KEYS_MAX_NUM");
                i2 |= 4;
            }
            if ((i & 5) == 5) {
                arrayList.add("WEP40_KEY_LEN_IN_BYTES");
                i2 |= 5;
            }
            if ((i & 13) == 13) {
                arrayList.add("WEP104_KEY_LEN_IN_BYTES");
                i2 |= 13;
            }
            if (i != i2) {
                arrayList.add("0x" + Integer.toHexString(i & (~i2)));
            }
            return String.join(" | ", arrayList);
        }
    }

    public static final class KeyMgmtMask {
        public static final int FT_EAP = 32;
        public static final int FT_PSK = 64;
        public static final int IEEE8021X = 8;
        public static final int NONE = 4;
        public static final int OSEN = 32768;
        public static final int WPA_EAP = 1;
        public static final int WPA_PSK = 2;

        public static final String toString(int i) {
            if (i == 1) {
                return "WPA_EAP";
            }
            if (i == 2) {
                return "WPA_PSK";
            }
            if (i == 4) {
                return "NONE";
            }
            if (i == 8) {
                return "IEEE8021X";
            }
            if (i == 32) {
                return "FT_EAP";
            }
            if (i == 64) {
                return "FT_PSK";
            }
            if (i == 32768) {
                return "OSEN";
            }
            return "0x" + Integer.toHexString(i);
        }

        public static final String dumpBitfield(int i) {
            ArrayList arrayList = new ArrayList();
            int i2 = 1;
            if ((i & 1) == 1) {
                arrayList.add("WPA_EAP");
            } else {
                i2 = 0;
            }
            if ((i & 2) == 2) {
                arrayList.add("WPA_PSK");
                i2 |= 2;
            }
            if ((i & 4) == 4) {
                arrayList.add("NONE");
                i2 |= 4;
            }
            if ((i & 8) == 8) {
                arrayList.add("IEEE8021X");
                i2 |= 8;
            }
            if ((i & 32) == 32) {
                arrayList.add("FT_EAP");
                i2 |= 32;
            }
            if ((i & 64) == 64) {
                arrayList.add("FT_PSK");
                i2 |= 64;
            }
            if ((i & OSEN) == 32768) {
                arrayList.add("OSEN");
                i2 |= OSEN;
            }
            if (i != i2) {
                arrayList.add("0x" + Integer.toHexString(i & (~i2)));
            }
            return String.join(" | ", arrayList);
        }
    }

    public static final class ProtoMask {
        public static final int OSEN = 8;
        public static final int RSN = 2;
        public static final int WPA = 1;

        public static final String toString(int i) {
            if (i == 1) {
                return "WPA";
            }
            if (i == 2) {
                return "RSN";
            }
            if (i == 8) {
                return "OSEN";
            }
            return "0x" + Integer.toHexString(i);
        }

        public static final String dumpBitfield(int i) {
            ArrayList arrayList = new ArrayList();
            int i2 = 1;
            if ((i & 1) == 1) {
                arrayList.add("WPA");
            } else {
                i2 = 0;
            }
            if ((i & 2) == 2) {
                arrayList.add("RSN");
                i2 |= 2;
            }
            if ((i & 8) == 8) {
                arrayList.add("OSEN");
                i2 |= 8;
            }
            if (i != i2) {
                arrayList.add("0x" + Integer.toHexString(i & (~i2)));
            }
            return String.join(" | ", arrayList);
        }
    }

    public static final class AuthAlgMask {
        public static final int LEAP = 4;
        public static final int OPEN = 1;
        public static final int SHARED = 2;

        public static final String toString(int i) {
            if (i == 1) {
                return "OPEN";
            }
            if (i == 2) {
                return "SHARED";
            }
            if (i == 4) {
                return "LEAP";
            }
            return "0x" + Integer.toHexString(i);
        }

        public static final String dumpBitfield(int i) {
            ArrayList arrayList = new ArrayList();
            int i2 = 1;
            if ((i & 1) == 1) {
                arrayList.add("OPEN");
            } else {
                i2 = 0;
            }
            if ((i & 2) == 2) {
                arrayList.add("SHARED");
                i2 |= 2;
            }
            if ((i & 4) == 4) {
                arrayList.add("LEAP");
                i2 |= 4;
            }
            if (i != i2) {
                arrayList.add("0x" + Integer.toHexString(i & (~i2)));
            }
            return String.join(" | ", arrayList);
        }
    }

    public static final class GroupCipherMask {
        public static final int CCMP = 16;
        public static final int GTK_NOT_USED = 16384;
        public static final int TKIP = 8;
        public static final int WEP104 = 4;
        public static final int WEP40 = 2;

        public static final String toString(int i) {
            if (i == 2) {
                return "WEP40";
            }
            if (i == 4) {
                return "WEP104";
            }
            if (i == 8) {
                return "TKIP";
            }
            if (i == 16) {
                return "CCMP";
            }
            if (i == 16384) {
                return "GTK_NOT_USED";
            }
            return "0x" + Integer.toHexString(i);
        }

        public static final String dumpBitfield(int i) {
            ArrayList arrayList = new ArrayList();
            int i2 = 2;
            if ((i & 2) == 2) {
                arrayList.add("WEP40");
            } else {
                i2 = 0;
            }
            if ((i & 4) == 4) {
                arrayList.add("WEP104");
                i2 |= 4;
            }
            if ((i & 8) == 8) {
                arrayList.add("TKIP");
                i2 |= 8;
            }
            if ((i & 16) == 16) {
                arrayList.add("CCMP");
                i2 |= 16;
            }
            if ((i & 16384) == 16384) {
                arrayList.add("GTK_NOT_USED");
                i2 |= 16384;
            }
            if (i != i2) {
                arrayList.add("0x" + Integer.toHexString(i & (~i2)));
            }
            return String.join(" | ", arrayList);
        }
    }

    public static final class PairwiseCipherMask {
        public static final int CCMP = 16;
        public static final int NONE = 1;
        public static final int TKIP = 8;

        public static final String toString(int i) {
            if (i == 1) {
                return "NONE";
            }
            if (i == 8) {
                return "TKIP";
            }
            if (i == 16) {
                return "CCMP";
            }
            return "0x" + Integer.toHexString(i);
        }

        public static final String dumpBitfield(int i) {
            ArrayList arrayList = new ArrayList();
            int i2 = 1;
            if ((i & 1) == 1) {
                arrayList.add("NONE");
            } else {
                i2 = 0;
            }
            if ((i & 8) == 8) {
                arrayList.add("TKIP");
                i2 |= 8;
            }
            if ((i & 16) == 16) {
                arrayList.add("CCMP");
                i2 |= 16;
            }
            if (i != i2) {
                arrayList.add("0x" + Integer.toHexString(i & (~i2)));
            }
            return String.join(" | ", arrayList);
        }
    }

    public static final class EapMethod {
        public static final int AKA = 5;
        public static final int AKA_PRIME = 6;
        public static final int PEAP = 0;
        public static final int PWD = 3;
        public static final int SIM = 4;
        public static final int TLS = 1;
        public static final int TTLS = 2;
        public static final int WFA_UNAUTH_TLS = 7;

        public static final String toString(int i) {
            if (i == 0) {
                return "PEAP";
            }
            if (i == 1) {
                return "TLS";
            }
            if (i == 2) {
                return "TTLS";
            }
            if (i == 3) {
                return "PWD";
            }
            if (i == 4) {
                return "SIM";
            }
            if (i == 5) {
                return "AKA";
            }
            if (i == 6) {
                return "AKA_PRIME";
            }
            if (i == 7) {
                return "WFA_UNAUTH_TLS";
            }
            return "0x" + Integer.toHexString(i);
        }

        public static final String dumpBitfield(int i) {
            ArrayList arrayList = new ArrayList();
            arrayList.add("PEAP");
            int i2 = 1;
            if ((i & 1) == 1) {
                arrayList.add("TLS");
            } else {
                i2 = 0;
            }
            if ((i & 2) == 2) {
                arrayList.add("TTLS");
                i2 |= 2;
            }
            if ((i & 3) == 3) {
                arrayList.add("PWD");
                i2 |= 3;
            }
            if ((i & 4) == 4) {
                arrayList.add("SIM");
                i2 |= 4;
            }
            if ((i & 5) == 5) {
                arrayList.add("AKA");
                i2 |= 5;
            }
            if ((i & 6) == 6) {
                arrayList.add("AKA_PRIME");
                i2 |= 6;
            }
            if ((i & 7) == 7) {
                arrayList.add("WFA_UNAUTH_TLS");
                i2 |= 7;
            }
            if (i != i2) {
                arrayList.add("0x" + Integer.toHexString(i & (~i2)));
            }
            return String.join(" | ", arrayList);
        }
    }

    public static final class EapPhase2Method {
        public static final int AKA = 6;
        public static final int AKA_PRIME = 7;
        public static final int GTC = 4;
        public static final int MSPAP = 2;
        public static final int MSPAPV2 = 3;
        public static final int NONE = 0;
        public static final int PAP = 1;
        public static final int SIM = 5;

        public static final String toString(int i) {
            if (i == 0) {
                return "NONE";
            }
            if (i == 1) {
                return "PAP";
            }
            if (i == 2) {
                return "MSPAP";
            }
            if (i == 3) {
                return "MSPAPV2";
            }
            if (i == 4) {
                return "GTC";
            }
            if (i == 5) {
                return "SIM";
            }
            if (i == 6) {
                return "AKA";
            }
            if (i == 7) {
                return "AKA_PRIME";
            }
            return "0x" + Integer.toHexString(i);
        }

        public static final String dumpBitfield(int i) {
            ArrayList arrayList = new ArrayList();
            arrayList.add("NONE");
            int i2 = 1;
            if ((i & 1) == 1) {
                arrayList.add("PAP");
            } else {
                i2 = 0;
            }
            if ((i & 2) == 2) {
                arrayList.add("MSPAP");
                i2 |= 2;
            }
            if ((i & 3) == 3) {
                arrayList.add("MSPAPV2");
                i2 |= 3;
            }
            if ((i & 4) == 4) {
                arrayList.add("GTC");
                i2 |= 4;
            }
            if ((i & 5) == 5) {
                arrayList.add("SIM");
                i2 |= 5;
            }
            if ((i & 6) == 6) {
                arrayList.add("AKA");
                i2 |= 6;
            }
            if ((i & 7) == 7) {
                arrayList.add("AKA_PRIME");
                i2 |= 7;
            }
            if (i != i2) {
                arrayList.add("0x" + Integer.toHexString(i & (~i2)));
            }
            return String.join(" | ", arrayList);
        }
    }

    public static final class NetworkResponseEapSimGsmAuthParams {
        public final byte[] kc = new byte[8];
        public final byte[] sres = new byte[4];

        public final boolean equals(Object obj) {
            if (this == obj) {
                return true;
            }
            if (obj == null || obj.getClass() != NetworkResponseEapSimGsmAuthParams.class) {
                return false;
            }
            NetworkResponseEapSimGsmAuthParams networkResponseEapSimGsmAuthParams = (NetworkResponseEapSimGsmAuthParams) obj;
            if (HidlSupport.deepEquals(this.kc, networkResponseEapSimGsmAuthParams.kc) && HidlSupport.deepEquals(this.sres, networkResponseEapSimGsmAuthParams.sres)) {
                return true;
            }
            return false;
        }

        public final int hashCode() {
            return Objects.hash(Integer.valueOf(HidlSupport.deepHashCode(this.kc)), Integer.valueOf(HidlSupport.deepHashCode(this.sres)));
        }

        public final String toString() {
            return "{.kc = " + Arrays.toString(this.kc) + ", .sres = " + Arrays.toString(this.sres) + "}";
        }

        public final void readFromParcel(HwParcel hwParcel) {
            readEmbeddedFromParcel(hwParcel, hwParcel.readBuffer(12L), 0L);
        }

        public static final ArrayList<NetworkResponseEapSimGsmAuthParams> readVectorFromParcel(HwParcel hwParcel) {
            ArrayList<NetworkResponseEapSimGsmAuthParams> arrayList = new ArrayList<>();
            HwBlob buffer = hwParcel.readBuffer(16L);
            int int32 = buffer.getInt32(8L);
            HwBlob embeddedBuffer = hwParcel.readEmbeddedBuffer(int32 * 12, buffer.handle(), 0L, true);
            arrayList.clear();
            for (int i = 0; i < int32; i++) {
                NetworkResponseEapSimGsmAuthParams networkResponseEapSimGsmAuthParams = new NetworkResponseEapSimGsmAuthParams();
                networkResponseEapSimGsmAuthParams.readEmbeddedFromParcel(hwParcel, embeddedBuffer, i * 12);
                arrayList.add(networkResponseEapSimGsmAuthParams);
            }
            return arrayList;
        }

        public final void readEmbeddedFromParcel(HwParcel hwParcel, HwBlob hwBlob, long j) {
            hwBlob.copyToInt8Array(0 + j, this.kc, 8);
            hwBlob.copyToInt8Array(j + 8, this.sres, 4);
        }

        public final void writeToParcel(HwParcel hwParcel) {
            HwBlob hwBlob = new HwBlob(12);
            writeEmbeddedToBlob(hwBlob, 0L);
            hwParcel.writeBuffer(hwBlob);
        }

        public static final void writeVectorToParcel(HwParcel hwParcel, ArrayList<NetworkResponseEapSimGsmAuthParams> arrayList) {
            HwBlob hwBlob = new HwBlob(16);
            int size = arrayList.size();
            hwBlob.putInt32(8L, size);
            hwBlob.putBool(12L, false);
            HwBlob hwBlob2 = new HwBlob(size * 12);
            for (int i = 0; i < size; i++) {
                arrayList.get(i).writeEmbeddedToBlob(hwBlob2, i * 12);
            }
            hwBlob.putBlob(0L, hwBlob2);
            hwParcel.writeBuffer(hwBlob);
        }

        public final void writeEmbeddedToBlob(HwBlob hwBlob, long j) {
            hwBlob.putInt8Array(0 + j, this.kc);
            hwBlob.putInt8Array(j + 8, this.sres);
        }
    }

    public static final class NetworkResponseEapSimUmtsAuthParams {
        public final ArrayList<Byte> res = new ArrayList<>();
        public final byte[] ik = new byte[16];
        public final byte[] ck = new byte[16];

        public final boolean equals(Object obj) {
            if (this == obj) {
                return true;
            }
            if (obj == null || obj.getClass() != NetworkResponseEapSimUmtsAuthParams.class) {
                return false;
            }
            NetworkResponseEapSimUmtsAuthParams networkResponseEapSimUmtsAuthParams = (NetworkResponseEapSimUmtsAuthParams) obj;
            if (HidlSupport.deepEquals(this.res, networkResponseEapSimUmtsAuthParams.res) && HidlSupport.deepEquals(this.ik, networkResponseEapSimUmtsAuthParams.ik) && HidlSupport.deepEquals(this.ck, networkResponseEapSimUmtsAuthParams.ck)) {
                return true;
            }
            return false;
        }

        public final int hashCode() {
            return Objects.hash(Integer.valueOf(HidlSupport.deepHashCode(this.res)), Integer.valueOf(HidlSupport.deepHashCode(this.ik)), Integer.valueOf(HidlSupport.deepHashCode(this.ck)));
        }

        public final String toString() {
            return "{.res = " + this.res + ", .ik = " + Arrays.toString(this.ik) + ", .ck = " + Arrays.toString(this.ck) + "}";
        }

        public final void readFromParcel(HwParcel hwParcel) {
            readEmbeddedFromParcel(hwParcel, hwParcel.readBuffer(48L), 0L);
        }

        public static final ArrayList<NetworkResponseEapSimUmtsAuthParams> readVectorFromParcel(HwParcel hwParcel) {
            ArrayList<NetworkResponseEapSimUmtsAuthParams> arrayList = new ArrayList<>();
            HwBlob buffer = hwParcel.readBuffer(16L);
            int int32 = buffer.getInt32(8L);
            HwBlob embeddedBuffer = hwParcel.readEmbeddedBuffer(int32 * 48, buffer.handle(), 0L, true);
            arrayList.clear();
            for (int i = 0; i < int32; i++) {
                NetworkResponseEapSimUmtsAuthParams networkResponseEapSimUmtsAuthParams = new NetworkResponseEapSimUmtsAuthParams();
                networkResponseEapSimUmtsAuthParams.readEmbeddedFromParcel(hwParcel, embeddedBuffer, i * 48);
                arrayList.add(networkResponseEapSimUmtsAuthParams);
            }
            return arrayList;
        }

        public final void readEmbeddedFromParcel(HwParcel hwParcel, HwBlob hwBlob, long j) {
            long j2 = j + 0;
            int int32 = hwBlob.getInt32(8 + j2);
            HwBlob embeddedBuffer = hwParcel.readEmbeddedBuffer(int32 * 1, hwBlob.handle(), j2 + 0, true);
            this.res.clear();
            for (int i = 0; i < int32; i++) {
                this.res.add(Byte.valueOf(embeddedBuffer.getInt8(i * 1)));
            }
            hwBlob.copyToInt8Array(j + 16, this.ik, 16);
            hwBlob.copyToInt8Array(j + 32, this.ck, 16);
        }

        public final void writeToParcel(HwParcel hwParcel) {
            HwBlob hwBlob = new HwBlob(48);
            writeEmbeddedToBlob(hwBlob, 0L);
            hwParcel.writeBuffer(hwBlob);
        }

        public static final void writeVectorToParcel(HwParcel hwParcel, ArrayList<NetworkResponseEapSimUmtsAuthParams> arrayList) {
            HwBlob hwBlob = new HwBlob(16);
            int size = arrayList.size();
            hwBlob.putInt32(8L, size);
            hwBlob.putBool(12L, false);
            HwBlob hwBlob2 = new HwBlob(size * 48);
            for (int i = 0; i < size; i++) {
                arrayList.get(i).writeEmbeddedToBlob(hwBlob2, i * 48);
            }
            hwBlob.putBlob(0L, hwBlob2);
            hwParcel.writeBuffer(hwBlob);
        }

        public final void writeEmbeddedToBlob(HwBlob hwBlob, long j) {
            int size = this.res.size();
            long j2 = j + 0;
            hwBlob.putInt32(8 + j2, size);
            hwBlob.putBool(12 + j2, false);
            HwBlob hwBlob2 = new HwBlob(size * 1);
            for (int i = 0; i < size; i++) {
                hwBlob2.putInt8(i * 1, this.res.get(i).byteValue());
            }
            hwBlob.putBlob(j2 + 0, hwBlob2);
            hwBlob.putInt8Array(16 + j, this.ik);
            hwBlob.putInt8Array(j + 32, this.ck);
        }
    }

    public static final class Proxy implements ISupplicantStaNetwork {
        private IHwBinder mRemote;

        public Proxy(IHwBinder iHwBinder) {
            this.mRemote = (IHwBinder) Objects.requireNonNull(iHwBinder);
        }

        @Override
        public IHwBinder asBinder() {
            return this.mRemote;
        }

        public String toString() {
            try {
                return interfaceDescriptor() + "@Proxy";
            } catch (RemoteException e) {
                return "[class or subclass of android.hardware.wifi.supplicant@1.0::ISupplicantStaNetwork]@Proxy";
            }
        }

        public final boolean equals(Object obj) {
            return HidlSupport.interfacesEqual(this, obj);
        }

        public final int hashCode() {
            return asBinder().hashCode();
        }

        @Override
        public void getId(ISupplicantNetwork.getIdCallback getidcallback) throws RemoteException {
            HwParcel hwParcel = new HwParcel();
            hwParcel.writeInterfaceToken(ISupplicantNetwork.kInterfaceName);
            HwParcel hwParcel2 = new HwParcel();
            try {
                this.mRemote.transact(1, hwParcel, hwParcel2, 0);
                hwParcel2.verifySuccess();
                hwParcel.releaseTemporaryStorage();
                SupplicantStatus supplicantStatus = new SupplicantStatus();
                supplicantStatus.readFromParcel(hwParcel2);
                getidcallback.onValues(supplicantStatus, hwParcel2.readInt32());
            } finally {
                hwParcel2.release();
            }
        }

        @Override
        public void getInterfaceName(ISupplicantNetwork.getInterfaceNameCallback getinterfacenamecallback) throws RemoteException {
            HwParcel hwParcel = new HwParcel();
            hwParcel.writeInterfaceToken(ISupplicantNetwork.kInterfaceName);
            HwParcel hwParcel2 = new HwParcel();
            try {
                this.mRemote.transact(2, hwParcel, hwParcel2, 0);
                hwParcel2.verifySuccess();
                hwParcel.releaseTemporaryStorage();
                SupplicantStatus supplicantStatus = new SupplicantStatus();
                supplicantStatus.readFromParcel(hwParcel2);
                getinterfacenamecallback.onValues(supplicantStatus, hwParcel2.readString());
            } finally {
                hwParcel2.release();
            }
        }

        @Override
        public void getType(ISupplicantNetwork.getTypeCallback gettypecallback) throws RemoteException {
            HwParcel hwParcel = new HwParcel();
            hwParcel.writeInterfaceToken(ISupplicantNetwork.kInterfaceName);
            HwParcel hwParcel2 = new HwParcel();
            try {
                this.mRemote.transact(3, hwParcel, hwParcel2, 0);
                hwParcel2.verifySuccess();
                hwParcel.releaseTemporaryStorage();
                SupplicantStatus supplicantStatus = new SupplicantStatus();
                supplicantStatus.readFromParcel(hwParcel2);
                gettypecallback.onValues(supplicantStatus, hwParcel2.readInt32());
            } finally {
                hwParcel2.release();
            }
        }

        @Override
        public SupplicantStatus registerCallback(ISupplicantStaNetworkCallback iSupplicantStaNetworkCallback) throws RemoteException {
            HwParcel hwParcel = new HwParcel();
            hwParcel.writeInterfaceToken(ISupplicantStaNetwork.kInterfaceName);
            hwParcel.writeStrongBinder(iSupplicantStaNetworkCallback == null ? null : iSupplicantStaNetworkCallback.asBinder());
            HwParcel hwParcel2 = new HwParcel();
            try {
                this.mRemote.transact(4, hwParcel, hwParcel2, 0);
                hwParcel2.verifySuccess();
                hwParcel.releaseTemporaryStorage();
                SupplicantStatus supplicantStatus = new SupplicantStatus();
                supplicantStatus.readFromParcel(hwParcel2);
                return supplicantStatus;
            } finally {
                hwParcel2.release();
            }
        }

        @Override
        public SupplicantStatus setSsid(ArrayList<Byte> arrayList) throws RemoteException {
            HwParcel hwParcel = new HwParcel();
            hwParcel.writeInterfaceToken(ISupplicantStaNetwork.kInterfaceName);
            hwParcel.writeInt8Vector(arrayList);
            HwParcel hwParcel2 = new HwParcel();
            try {
                this.mRemote.transact(5, hwParcel, hwParcel2, 0);
                hwParcel2.verifySuccess();
                hwParcel.releaseTemporaryStorage();
                SupplicantStatus supplicantStatus = new SupplicantStatus();
                supplicantStatus.readFromParcel(hwParcel2);
                return supplicantStatus;
            } finally {
                hwParcel2.release();
            }
        }

        @Override
        public SupplicantStatus setBssid(byte[] bArr) throws RemoteException {
            HwParcel hwParcel = new HwParcel();
            hwParcel.writeInterfaceToken(ISupplicantStaNetwork.kInterfaceName);
            HwBlob hwBlob = new HwBlob(6);
            hwBlob.putInt8Array(0L, bArr);
            hwParcel.writeBuffer(hwBlob);
            HwParcel hwParcel2 = new HwParcel();
            try {
                this.mRemote.transact(6, hwParcel, hwParcel2, 0);
                hwParcel2.verifySuccess();
                hwParcel.releaseTemporaryStorage();
                SupplicantStatus supplicantStatus = new SupplicantStatus();
                supplicantStatus.readFromParcel(hwParcel2);
                return supplicantStatus;
            } finally {
                hwParcel2.release();
            }
        }

        @Override
        public SupplicantStatus setScanSsid(boolean z) throws RemoteException {
            HwParcel hwParcel = new HwParcel();
            hwParcel.writeInterfaceToken(ISupplicantStaNetwork.kInterfaceName);
            hwParcel.writeBool(z);
            HwParcel hwParcel2 = new HwParcel();
            try {
                this.mRemote.transact(7, hwParcel, hwParcel2, 0);
                hwParcel2.verifySuccess();
                hwParcel.releaseTemporaryStorage();
                SupplicantStatus supplicantStatus = new SupplicantStatus();
                supplicantStatus.readFromParcel(hwParcel2);
                return supplicantStatus;
            } finally {
                hwParcel2.release();
            }
        }

        @Override
        public SupplicantStatus setKeyMgmt(int i) throws RemoteException {
            HwParcel hwParcel = new HwParcel();
            hwParcel.writeInterfaceToken(ISupplicantStaNetwork.kInterfaceName);
            hwParcel.writeInt32(i);
            HwParcel hwParcel2 = new HwParcel();
            try {
                this.mRemote.transact(8, hwParcel, hwParcel2, 0);
                hwParcel2.verifySuccess();
                hwParcel.releaseTemporaryStorage();
                SupplicantStatus supplicantStatus = new SupplicantStatus();
                supplicantStatus.readFromParcel(hwParcel2);
                return supplicantStatus;
            } finally {
                hwParcel2.release();
            }
        }

        @Override
        public SupplicantStatus setProto(int i) throws RemoteException {
            HwParcel hwParcel = new HwParcel();
            hwParcel.writeInterfaceToken(ISupplicantStaNetwork.kInterfaceName);
            hwParcel.writeInt32(i);
            HwParcel hwParcel2 = new HwParcel();
            try {
                this.mRemote.transact(9, hwParcel, hwParcel2, 0);
                hwParcel2.verifySuccess();
                hwParcel.releaseTemporaryStorage();
                SupplicantStatus supplicantStatus = new SupplicantStatus();
                supplicantStatus.readFromParcel(hwParcel2);
                return supplicantStatus;
            } finally {
                hwParcel2.release();
            }
        }

        @Override
        public SupplicantStatus setAuthAlg(int i) throws RemoteException {
            HwParcel hwParcel = new HwParcel();
            hwParcel.writeInterfaceToken(ISupplicantStaNetwork.kInterfaceName);
            hwParcel.writeInt32(i);
            HwParcel hwParcel2 = new HwParcel();
            try {
                this.mRemote.transact(10, hwParcel, hwParcel2, 0);
                hwParcel2.verifySuccess();
                hwParcel.releaseTemporaryStorage();
                SupplicantStatus supplicantStatus = new SupplicantStatus();
                supplicantStatus.readFromParcel(hwParcel2);
                return supplicantStatus;
            } finally {
                hwParcel2.release();
            }
        }

        @Override
        public SupplicantStatus setGroupCipher(int i) throws RemoteException {
            HwParcel hwParcel = new HwParcel();
            hwParcel.writeInterfaceToken(ISupplicantStaNetwork.kInterfaceName);
            hwParcel.writeInt32(i);
            HwParcel hwParcel2 = new HwParcel();
            try {
                this.mRemote.transact(11, hwParcel, hwParcel2, 0);
                hwParcel2.verifySuccess();
                hwParcel.releaseTemporaryStorage();
                SupplicantStatus supplicantStatus = new SupplicantStatus();
                supplicantStatus.readFromParcel(hwParcel2);
                return supplicantStatus;
            } finally {
                hwParcel2.release();
            }
        }

        @Override
        public SupplicantStatus setPairwiseCipher(int i) throws RemoteException {
            HwParcel hwParcel = new HwParcel();
            hwParcel.writeInterfaceToken(ISupplicantStaNetwork.kInterfaceName);
            hwParcel.writeInt32(i);
            HwParcel hwParcel2 = new HwParcel();
            try {
                this.mRemote.transact(12, hwParcel, hwParcel2, 0);
                hwParcel2.verifySuccess();
                hwParcel.releaseTemporaryStorage();
                SupplicantStatus supplicantStatus = new SupplicantStatus();
                supplicantStatus.readFromParcel(hwParcel2);
                return supplicantStatus;
            } finally {
                hwParcel2.release();
            }
        }

        @Override
        public SupplicantStatus setPskPassphrase(String str) throws RemoteException {
            HwParcel hwParcel = new HwParcel();
            hwParcel.writeInterfaceToken(ISupplicantStaNetwork.kInterfaceName);
            hwParcel.writeString(str);
            HwParcel hwParcel2 = new HwParcel();
            try {
                this.mRemote.transact(13, hwParcel, hwParcel2, 0);
                hwParcel2.verifySuccess();
                hwParcel.releaseTemporaryStorage();
                SupplicantStatus supplicantStatus = new SupplicantStatus();
                supplicantStatus.readFromParcel(hwParcel2);
                return supplicantStatus;
            } finally {
                hwParcel2.release();
            }
        }

        @Override
        public SupplicantStatus setPsk(byte[] bArr) throws RemoteException {
            HwParcel hwParcel = new HwParcel();
            hwParcel.writeInterfaceToken(ISupplicantStaNetwork.kInterfaceName);
            HwBlob hwBlob = new HwBlob(32);
            hwBlob.putInt8Array(0L, bArr);
            hwParcel.writeBuffer(hwBlob);
            HwParcel hwParcel2 = new HwParcel();
            try {
                this.mRemote.transact(14, hwParcel, hwParcel2, 0);
                hwParcel2.verifySuccess();
                hwParcel.releaseTemporaryStorage();
                SupplicantStatus supplicantStatus = new SupplicantStatus();
                supplicantStatus.readFromParcel(hwParcel2);
                return supplicantStatus;
            } finally {
                hwParcel2.release();
            }
        }

        @Override
        public SupplicantStatus setWepKey(int i, ArrayList<Byte> arrayList) throws RemoteException {
            HwParcel hwParcel = new HwParcel();
            hwParcel.writeInterfaceToken(ISupplicantStaNetwork.kInterfaceName);
            hwParcel.writeInt32(i);
            hwParcel.writeInt8Vector(arrayList);
            HwParcel hwParcel2 = new HwParcel();
            try {
                this.mRemote.transact(15, hwParcel, hwParcel2, 0);
                hwParcel2.verifySuccess();
                hwParcel.releaseTemporaryStorage();
                SupplicantStatus supplicantStatus = new SupplicantStatus();
                supplicantStatus.readFromParcel(hwParcel2);
                return supplicantStatus;
            } finally {
                hwParcel2.release();
            }
        }

        @Override
        public SupplicantStatus setWepTxKeyIdx(int i) throws RemoteException {
            HwParcel hwParcel = new HwParcel();
            hwParcel.writeInterfaceToken(ISupplicantStaNetwork.kInterfaceName);
            hwParcel.writeInt32(i);
            HwParcel hwParcel2 = new HwParcel();
            try {
                this.mRemote.transact(16, hwParcel, hwParcel2, 0);
                hwParcel2.verifySuccess();
                hwParcel.releaseTemporaryStorage();
                SupplicantStatus supplicantStatus = new SupplicantStatus();
                supplicantStatus.readFromParcel(hwParcel2);
                return supplicantStatus;
            } finally {
                hwParcel2.release();
            }
        }

        @Override
        public SupplicantStatus setRequirePmf(boolean z) throws RemoteException {
            HwParcel hwParcel = new HwParcel();
            hwParcel.writeInterfaceToken(ISupplicantStaNetwork.kInterfaceName);
            hwParcel.writeBool(z);
            HwParcel hwParcel2 = new HwParcel();
            try {
                this.mRemote.transact(17, hwParcel, hwParcel2, 0);
                hwParcel2.verifySuccess();
                hwParcel.releaseTemporaryStorage();
                SupplicantStatus supplicantStatus = new SupplicantStatus();
                supplicantStatus.readFromParcel(hwParcel2);
                return supplicantStatus;
            } finally {
                hwParcel2.release();
            }
        }

        @Override
        public SupplicantStatus setEapMethod(int i) throws RemoteException {
            HwParcel hwParcel = new HwParcel();
            hwParcel.writeInterfaceToken(ISupplicantStaNetwork.kInterfaceName);
            hwParcel.writeInt32(i);
            HwParcel hwParcel2 = new HwParcel();
            try {
                this.mRemote.transact(18, hwParcel, hwParcel2, 0);
                hwParcel2.verifySuccess();
                hwParcel.releaseTemporaryStorage();
                SupplicantStatus supplicantStatus = new SupplicantStatus();
                supplicantStatus.readFromParcel(hwParcel2);
                return supplicantStatus;
            } finally {
                hwParcel2.release();
            }
        }

        @Override
        public SupplicantStatus setEapPhase2Method(int i) throws RemoteException {
            HwParcel hwParcel = new HwParcel();
            hwParcel.writeInterfaceToken(ISupplicantStaNetwork.kInterfaceName);
            hwParcel.writeInt32(i);
            HwParcel hwParcel2 = new HwParcel();
            try {
                this.mRemote.transact(19, hwParcel, hwParcel2, 0);
                hwParcel2.verifySuccess();
                hwParcel.releaseTemporaryStorage();
                SupplicantStatus supplicantStatus = new SupplicantStatus();
                supplicantStatus.readFromParcel(hwParcel2);
                return supplicantStatus;
            } finally {
                hwParcel2.release();
            }
        }

        @Override
        public SupplicantStatus setEapIdentity(ArrayList<Byte> arrayList) throws RemoteException {
            HwParcel hwParcel = new HwParcel();
            hwParcel.writeInterfaceToken(ISupplicantStaNetwork.kInterfaceName);
            hwParcel.writeInt8Vector(arrayList);
            HwParcel hwParcel2 = new HwParcel();
            try {
                this.mRemote.transact(20, hwParcel, hwParcel2, 0);
                hwParcel2.verifySuccess();
                hwParcel.releaseTemporaryStorage();
                SupplicantStatus supplicantStatus = new SupplicantStatus();
                supplicantStatus.readFromParcel(hwParcel2);
                return supplicantStatus;
            } finally {
                hwParcel2.release();
            }
        }

        @Override
        public SupplicantStatus setEapAnonymousIdentity(ArrayList<Byte> arrayList) throws RemoteException {
            HwParcel hwParcel = new HwParcel();
            hwParcel.writeInterfaceToken(ISupplicantStaNetwork.kInterfaceName);
            hwParcel.writeInt8Vector(arrayList);
            HwParcel hwParcel2 = new HwParcel();
            try {
                this.mRemote.transact(21, hwParcel, hwParcel2, 0);
                hwParcel2.verifySuccess();
                hwParcel.releaseTemporaryStorage();
                SupplicantStatus supplicantStatus = new SupplicantStatus();
                supplicantStatus.readFromParcel(hwParcel2);
                return supplicantStatus;
            } finally {
                hwParcel2.release();
            }
        }

        @Override
        public SupplicantStatus setEapPassword(ArrayList<Byte> arrayList) throws RemoteException {
            HwParcel hwParcel = new HwParcel();
            hwParcel.writeInterfaceToken(ISupplicantStaNetwork.kInterfaceName);
            hwParcel.writeInt8Vector(arrayList);
            HwParcel hwParcel2 = new HwParcel();
            try {
                this.mRemote.transact(22, hwParcel, hwParcel2, 0);
                hwParcel2.verifySuccess();
                hwParcel.releaseTemporaryStorage();
                SupplicantStatus supplicantStatus = new SupplicantStatus();
                supplicantStatus.readFromParcel(hwParcel2);
                return supplicantStatus;
            } finally {
                hwParcel2.release();
            }
        }

        @Override
        public SupplicantStatus setEapCACert(String str) throws RemoteException {
            HwParcel hwParcel = new HwParcel();
            hwParcel.writeInterfaceToken(ISupplicantStaNetwork.kInterfaceName);
            hwParcel.writeString(str);
            HwParcel hwParcel2 = new HwParcel();
            try {
                this.mRemote.transact(23, hwParcel, hwParcel2, 0);
                hwParcel2.verifySuccess();
                hwParcel.releaseTemporaryStorage();
                SupplicantStatus supplicantStatus = new SupplicantStatus();
                supplicantStatus.readFromParcel(hwParcel2);
                return supplicantStatus;
            } finally {
                hwParcel2.release();
            }
        }

        @Override
        public SupplicantStatus setEapCAPath(String str) throws RemoteException {
            HwParcel hwParcel = new HwParcel();
            hwParcel.writeInterfaceToken(ISupplicantStaNetwork.kInterfaceName);
            hwParcel.writeString(str);
            HwParcel hwParcel2 = new HwParcel();
            try {
                this.mRemote.transact(24, hwParcel, hwParcel2, 0);
                hwParcel2.verifySuccess();
                hwParcel.releaseTemporaryStorage();
                SupplicantStatus supplicantStatus = new SupplicantStatus();
                supplicantStatus.readFromParcel(hwParcel2);
                return supplicantStatus;
            } finally {
                hwParcel2.release();
            }
        }

        @Override
        public SupplicantStatus setEapClientCert(String str) throws RemoteException {
            HwParcel hwParcel = new HwParcel();
            hwParcel.writeInterfaceToken(ISupplicantStaNetwork.kInterfaceName);
            hwParcel.writeString(str);
            HwParcel hwParcel2 = new HwParcel();
            try {
                this.mRemote.transact(25, hwParcel, hwParcel2, 0);
                hwParcel2.verifySuccess();
                hwParcel.releaseTemporaryStorage();
                SupplicantStatus supplicantStatus = new SupplicantStatus();
                supplicantStatus.readFromParcel(hwParcel2);
                return supplicantStatus;
            } finally {
                hwParcel2.release();
            }
        }

        @Override
        public SupplicantStatus setEapPrivateKeyId(String str) throws RemoteException {
            HwParcel hwParcel = new HwParcel();
            hwParcel.writeInterfaceToken(ISupplicantStaNetwork.kInterfaceName);
            hwParcel.writeString(str);
            HwParcel hwParcel2 = new HwParcel();
            try {
                this.mRemote.transact(26, hwParcel, hwParcel2, 0);
                hwParcel2.verifySuccess();
                hwParcel.releaseTemporaryStorage();
                SupplicantStatus supplicantStatus = new SupplicantStatus();
                supplicantStatus.readFromParcel(hwParcel2);
                return supplicantStatus;
            } finally {
                hwParcel2.release();
            }
        }

        @Override
        public SupplicantStatus setEapSubjectMatch(String str) throws RemoteException {
            HwParcel hwParcel = new HwParcel();
            hwParcel.writeInterfaceToken(ISupplicantStaNetwork.kInterfaceName);
            hwParcel.writeString(str);
            HwParcel hwParcel2 = new HwParcel();
            try {
                this.mRemote.transact(27, hwParcel, hwParcel2, 0);
                hwParcel2.verifySuccess();
                hwParcel.releaseTemporaryStorage();
                SupplicantStatus supplicantStatus = new SupplicantStatus();
                supplicantStatus.readFromParcel(hwParcel2);
                return supplicantStatus;
            } finally {
                hwParcel2.release();
            }
        }

        @Override
        public SupplicantStatus setEapAltSubjectMatch(String str) throws RemoteException {
            HwParcel hwParcel = new HwParcel();
            hwParcel.writeInterfaceToken(ISupplicantStaNetwork.kInterfaceName);
            hwParcel.writeString(str);
            HwParcel hwParcel2 = new HwParcel();
            try {
                this.mRemote.transact(28, hwParcel, hwParcel2, 0);
                hwParcel2.verifySuccess();
                hwParcel.releaseTemporaryStorage();
                SupplicantStatus supplicantStatus = new SupplicantStatus();
                supplicantStatus.readFromParcel(hwParcel2);
                return supplicantStatus;
            } finally {
                hwParcel2.release();
            }
        }

        @Override
        public SupplicantStatus setEapEngine(boolean z) throws RemoteException {
            HwParcel hwParcel = new HwParcel();
            hwParcel.writeInterfaceToken(ISupplicantStaNetwork.kInterfaceName);
            hwParcel.writeBool(z);
            HwParcel hwParcel2 = new HwParcel();
            try {
                this.mRemote.transact(29, hwParcel, hwParcel2, 0);
                hwParcel2.verifySuccess();
                hwParcel.releaseTemporaryStorage();
                SupplicantStatus supplicantStatus = new SupplicantStatus();
                supplicantStatus.readFromParcel(hwParcel2);
                return supplicantStatus;
            } finally {
                hwParcel2.release();
            }
        }

        @Override
        public SupplicantStatus setEapEngineID(String str) throws RemoteException {
            HwParcel hwParcel = new HwParcel();
            hwParcel.writeInterfaceToken(ISupplicantStaNetwork.kInterfaceName);
            hwParcel.writeString(str);
            HwParcel hwParcel2 = new HwParcel();
            try {
                this.mRemote.transact(30, hwParcel, hwParcel2, 0);
                hwParcel2.verifySuccess();
                hwParcel.releaseTemporaryStorage();
                SupplicantStatus supplicantStatus = new SupplicantStatus();
                supplicantStatus.readFromParcel(hwParcel2);
                return supplicantStatus;
            } finally {
                hwParcel2.release();
            }
        }

        @Override
        public SupplicantStatus setEapDomainSuffixMatch(String str) throws RemoteException {
            HwParcel hwParcel = new HwParcel();
            hwParcel.writeInterfaceToken(ISupplicantStaNetwork.kInterfaceName);
            hwParcel.writeString(str);
            HwParcel hwParcel2 = new HwParcel();
            try {
                this.mRemote.transact(31, hwParcel, hwParcel2, 0);
                hwParcel2.verifySuccess();
                hwParcel.releaseTemporaryStorage();
                SupplicantStatus supplicantStatus = new SupplicantStatus();
                supplicantStatus.readFromParcel(hwParcel2);
                return supplicantStatus;
            } finally {
                hwParcel2.release();
            }
        }

        @Override
        public SupplicantStatus setProactiveKeyCaching(boolean z) throws RemoteException {
            HwParcel hwParcel = new HwParcel();
            hwParcel.writeInterfaceToken(ISupplicantStaNetwork.kInterfaceName);
            hwParcel.writeBool(z);
            HwParcel hwParcel2 = new HwParcel();
            try {
                this.mRemote.transact(32, hwParcel, hwParcel2, 0);
                hwParcel2.verifySuccess();
                hwParcel.releaseTemporaryStorage();
                SupplicantStatus supplicantStatus = new SupplicantStatus();
                supplicantStatus.readFromParcel(hwParcel2);
                return supplicantStatus;
            } finally {
                hwParcel2.release();
            }
        }

        @Override
        public SupplicantStatus setIdStr(String str) throws RemoteException {
            HwParcel hwParcel = new HwParcel();
            hwParcel.writeInterfaceToken(ISupplicantStaNetwork.kInterfaceName);
            hwParcel.writeString(str);
            HwParcel hwParcel2 = new HwParcel();
            try {
                this.mRemote.transact(33, hwParcel, hwParcel2, 0);
                hwParcel2.verifySuccess();
                hwParcel.releaseTemporaryStorage();
                SupplicantStatus supplicantStatus = new SupplicantStatus();
                supplicantStatus.readFromParcel(hwParcel2);
                return supplicantStatus;
            } finally {
                hwParcel2.release();
            }
        }

        @Override
        public SupplicantStatus setUpdateIdentifier(int i) throws RemoteException {
            HwParcel hwParcel = new HwParcel();
            hwParcel.writeInterfaceToken(ISupplicantStaNetwork.kInterfaceName);
            hwParcel.writeInt32(i);
            HwParcel hwParcel2 = new HwParcel();
            try {
                this.mRemote.transact(34, hwParcel, hwParcel2, 0);
                hwParcel2.verifySuccess();
                hwParcel.releaseTemporaryStorage();
                SupplicantStatus supplicantStatus = new SupplicantStatus();
                supplicantStatus.readFromParcel(hwParcel2);
                return supplicantStatus;
            } finally {
                hwParcel2.release();
            }
        }

        @Override
        public void getSsid(getSsidCallback getssidcallback) throws RemoteException {
            HwParcel hwParcel = new HwParcel();
            hwParcel.writeInterfaceToken(ISupplicantStaNetwork.kInterfaceName);
            HwParcel hwParcel2 = new HwParcel();
            try {
                this.mRemote.transact(35, hwParcel, hwParcel2, 0);
                hwParcel2.verifySuccess();
                hwParcel.releaseTemporaryStorage();
                SupplicantStatus supplicantStatus = new SupplicantStatus();
                supplicantStatus.readFromParcel(hwParcel2);
                getssidcallback.onValues(supplicantStatus, hwParcel2.readInt8Vector());
            } finally {
                hwParcel2.release();
            }
        }

        @Override
        public void getBssid(getBssidCallback getbssidcallback) throws RemoteException {
            HwParcel hwParcel = new HwParcel();
            hwParcel.writeInterfaceToken(ISupplicantStaNetwork.kInterfaceName);
            HwParcel hwParcel2 = new HwParcel();
            try {
                this.mRemote.transact(36, hwParcel, hwParcel2, 0);
                hwParcel2.verifySuccess();
                hwParcel.releaseTemporaryStorage();
                SupplicantStatus supplicantStatus = new SupplicantStatus();
                supplicantStatus.readFromParcel(hwParcel2);
                byte[] bArr = new byte[6];
                hwParcel2.readBuffer(6L).copyToInt8Array(0L, bArr, 6);
                getbssidcallback.onValues(supplicantStatus, bArr);
            } finally {
                hwParcel2.release();
            }
        }

        @Override
        public void getScanSsid(getScanSsidCallback getscanssidcallback) throws RemoteException {
            HwParcel hwParcel = new HwParcel();
            hwParcel.writeInterfaceToken(ISupplicantStaNetwork.kInterfaceName);
            HwParcel hwParcel2 = new HwParcel();
            try {
                this.mRemote.transact(37, hwParcel, hwParcel2, 0);
                hwParcel2.verifySuccess();
                hwParcel.releaseTemporaryStorage();
                SupplicantStatus supplicantStatus = new SupplicantStatus();
                supplicantStatus.readFromParcel(hwParcel2);
                getscanssidcallback.onValues(supplicantStatus, hwParcel2.readBool());
            } finally {
                hwParcel2.release();
            }
        }

        @Override
        public void getKeyMgmt(getKeyMgmtCallback getkeymgmtcallback) throws RemoteException {
            HwParcel hwParcel = new HwParcel();
            hwParcel.writeInterfaceToken(ISupplicantStaNetwork.kInterfaceName);
            HwParcel hwParcel2 = new HwParcel();
            try {
                this.mRemote.transact(38, hwParcel, hwParcel2, 0);
                hwParcel2.verifySuccess();
                hwParcel.releaseTemporaryStorage();
                SupplicantStatus supplicantStatus = new SupplicantStatus();
                supplicantStatus.readFromParcel(hwParcel2);
                getkeymgmtcallback.onValues(supplicantStatus, hwParcel2.readInt32());
            } finally {
                hwParcel2.release();
            }
        }

        @Override
        public void getProto(getProtoCallback getprotocallback) throws RemoteException {
            HwParcel hwParcel = new HwParcel();
            hwParcel.writeInterfaceToken(ISupplicantStaNetwork.kInterfaceName);
            HwParcel hwParcel2 = new HwParcel();
            try {
                this.mRemote.transact(39, hwParcel, hwParcel2, 0);
                hwParcel2.verifySuccess();
                hwParcel.releaseTemporaryStorage();
                SupplicantStatus supplicantStatus = new SupplicantStatus();
                supplicantStatus.readFromParcel(hwParcel2);
                getprotocallback.onValues(supplicantStatus, hwParcel2.readInt32());
            } finally {
                hwParcel2.release();
            }
        }

        @Override
        public void getAuthAlg(getAuthAlgCallback getauthalgcallback) throws RemoteException {
            HwParcel hwParcel = new HwParcel();
            hwParcel.writeInterfaceToken(ISupplicantStaNetwork.kInterfaceName);
            HwParcel hwParcel2 = new HwParcel();
            try {
                this.mRemote.transact(40, hwParcel, hwParcel2, 0);
                hwParcel2.verifySuccess();
                hwParcel.releaseTemporaryStorage();
                SupplicantStatus supplicantStatus = new SupplicantStatus();
                supplicantStatus.readFromParcel(hwParcel2);
                getauthalgcallback.onValues(supplicantStatus, hwParcel2.readInt32());
            } finally {
                hwParcel2.release();
            }
        }

        @Override
        public void getGroupCipher(getGroupCipherCallback getgroupciphercallback) throws RemoteException {
            HwParcel hwParcel = new HwParcel();
            hwParcel.writeInterfaceToken(ISupplicantStaNetwork.kInterfaceName);
            HwParcel hwParcel2 = new HwParcel();
            try {
                this.mRemote.transact(41, hwParcel, hwParcel2, 0);
                hwParcel2.verifySuccess();
                hwParcel.releaseTemporaryStorage();
                SupplicantStatus supplicantStatus = new SupplicantStatus();
                supplicantStatus.readFromParcel(hwParcel2);
                getgroupciphercallback.onValues(supplicantStatus, hwParcel2.readInt32());
            } finally {
                hwParcel2.release();
            }
        }

        @Override
        public void getPairwiseCipher(getPairwiseCipherCallback getpairwiseciphercallback) throws RemoteException {
            HwParcel hwParcel = new HwParcel();
            hwParcel.writeInterfaceToken(ISupplicantStaNetwork.kInterfaceName);
            HwParcel hwParcel2 = new HwParcel();
            try {
                this.mRemote.transact(42, hwParcel, hwParcel2, 0);
                hwParcel2.verifySuccess();
                hwParcel.releaseTemporaryStorage();
                SupplicantStatus supplicantStatus = new SupplicantStatus();
                supplicantStatus.readFromParcel(hwParcel2);
                getpairwiseciphercallback.onValues(supplicantStatus, hwParcel2.readInt32());
            } finally {
                hwParcel2.release();
            }
        }

        @Override
        public void getPskPassphrase(getPskPassphraseCallback getpskpassphrasecallback) throws RemoteException {
            HwParcel hwParcel = new HwParcel();
            hwParcel.writeInterfaceToken(ISupplicantStaNetwork.kInterfaceName);
            HwParcel hwParcel2 = new HwParcel();
            try {
                this.mRemote.transact(43, hwParcel, hwParcel2, 0);
                hwParcel2.verifySuccess();
                hwParcel.releaseTemporaryStorage();
                SupplicantStatus supplicantStatus = new SupplicantStatus();
                supplicantStatus.readFromParcel(hwParcel2);
                getpskpassphrasecallback.onValues(supplicantStatus, hwParcel2.readString());
            } finally {
                hwParcel2.release();
            }
        }

        @Override
        public void getPsk(getPskCallback getpskcallback) throws RemoteException {
            HwParcel hwParcel = new HwParcel();
            hwParcel.writeInterfaceToken(ISupplicantStaNetwork.kInterfaceName);
            HwParcel hwParcel2 = new HwParcel();
            try {
                this.mRemote.transact(44, hwParcel, hwParcel2, 0);
                hwParcel2.verifySuccess();
                hwParcel.releaseTemporaryStorage();
                SupplicantStatus supplicantStatus = new SupplicantStatus();
                supplicantStatus.readFromParcel(hwParcel2);
                byte[] bArr = new byte[32];
                hwParcel2.readBuffer(32L).copyToInt8Array(0L, bArr, 32);
                getpskcallback.onValues(supplicantStatus, bArr);
            } finally {
                hwParcel2.release();
            }
        }

        @Override
        public void getWepKey(int i, getWepKeyCallback getwepkeycallback) throws RemoteException {
            HwParcel hwParcel = new HwParcel();
            hwParcel.writeInterfaceToken(ISupplicantStaNetwork.kInterfaceName);
            hwParcel.writeInt32(i);
            HwParcel hwParcel2 = new HwParcel();
            try {
                this.mRemote.transact(45, hwParcel, hwParcel2, 0);
                hwParcel2.verifySuccess();
                hwParcel.releaseTemporaryStorage();
                SupplicantStatus supplicantStatus = new SupplicantStatus();
                supplicantStatus.readFromParcel(hwParcel2);
                getwepkeycallback.onValues(supplicantStatus, hwParcel2.readInt8Vector());
            } finally {
                hwParcel2.release();
            }
        }

        @Override
        public void getWepTxKeyIdx(getWepTxKeyIdxCallback getweptxkeyidxcallback) throws RemoteException {
            HwParcel hwParcel = new HwParcel();
            hwParcel.writeInterfaceToken(ISupplicantStaNetwork.kInterfaceName);
            HwParcel hwParcel2 = new HwParcel();
            try {
                this.mRemote.transact(46, hwParcel, hwParcel2, 0);
                hwParcel2.verifySuccess();
                hwParcel.releaseTemporaryStorage();
                SupplicantStatus supplicantStatus = new SupplicantStatus();
                supplicantStatus.readFromParcel(hwParcel2);
                getweptxkeyidxcallback.onValues(supplicantStatus, hwParcel2.readInt32());
            } finally {
                hwParcel2.release();
            }
        }

        @Override
        public void getRequirePmf(getRequirePmfCallback getrequirepmfcallback) throws RemoteException {
            HwParcel hwParcel = new HwParcel();
            hwParcel.writeInterfaceToken(ISupplicantStaNetwork.kInterfaceName);
            HwParcel hwParcel2 = new HwParcel();
            try {
                this.mRemote.transact(47, hwParcel, hwParcel2, 0);
                hwParcel2.verifySuccess();
                hwParcel.releaseTemporaryStorage();
                SupplicantStatus supplicantStatus = new SupplicantStatus();
                supplicantStatus.readFromParcel(hwParcel2);
                getrequirepmfcallback.onValues(supplicantStatus, hwParcel2.readBool());
            } finally {
                hwParcel2.release();
            }
        }

        @Override
        public void getEapMethod(getEapMethodCallback geteapmethodcallback) throws RemoteException {
            HwParcel hwParcel = new HwParcel();
            hwParcel.writeInterfaceToken(ISupplicantStaNetwork.kInterfaceName);
            HwParcel hwParcel2 = new HwParcel();
            try {
                this.mRemote.transact(48, hwParcel, hwParcel2, 0);
                hwParcel2.verifySuccess();
                hwParcel.releaseTemporaryStorage();
                SupplicantStatus supplicantStatus = new SupplicantStatus();
                supplicantStatus.readFromParcel(hwParcel2);
                geteapmethodcallback.onValues(supplicantStatus, hwParcel2.readInt32());
            } finally {
                hwParcel2.release();
            }
        }

        @Override
        public void getEapPhase2Method(getEapPhase2MethodCallback geteapphase2methodcallback) throws RemoteException {
            HwParcel hwParcel = new HwParcel();
            hwParcel.writeInterfaceToken(ISupplicantStaNetwork.kInterfaceName);
            HwParcel hwParcel2 = new HwParcel();
            try {
                this.mRemote.transact(49, hwParcel, hwParcel2, 0);
                hwParcel2.verifySuccess();
                hwParcel.releaseTemporaryStorage();
                SupplicantStatus supplicantStatus = new SupplicantStatus();
                supplicantStatus.readFromParcel(hwParcel2);
                geteapphase2methodcallback.onValues(supplicantStatus, hwParcel2.readInt32());
            } finally {
                hwParcel2.release();
            }
        }

        @Override
        public void getEapIdentity(getEapIdentityCallback geteapidentitycallback) throws RemoteException {
            HwParcel hwParcel = new HwParcel();
            hwParcel.writeInterfaceToken(ISupplicantStaNetwork.kInterfaceName);
            HwParcel hwParcel2 = new HwParcel();
            try {
                this.mRemote.transact(50, hwParcel, hwParcel2, 0);
                hwParcel2.verifySuccess();
                hwParcel.releaseTemporaryStorage();
                SupplicantStatus supplicantStatus = new SupplicantStatus();
                supplicantStatus.readFromParcel(hwParcel2);
                geteapidentitycallback.onValues(supplicantStatus, hwParcel2.readInt8Vector());
            } finally {
                hwParcel2.release();
            }
        }

        @Override
        public void getEapAnonymousIdentity(getEapAnonymousIdentityCallback geteapanonymousidentitycallback) throws RemoteException {
            HwParcel hwParcel = new HwParcel();
            hwParcel.writeInterfaceToken(ISupplicantStaNetwork.kInterfaceName);
            HwParcel hwParcel2 = new HwParcel();
            try {
                this.mRemote.transact(51, hwParcel, hwParcel2, 0);
                hwParcel2.verifySuccess();
                hwParcel.releaseTemporaryStorage();
                SupplicantStatus supplicantStatus = new SupplicantStatus();
                supplicantStatus.readFromParcel(hwParcel2);
                geteapanonymousidentitycallback.onValues(supplicantStatus, hwParcel2.readInt8Vector());
            } finally {
                hwParcel2.release();
            }
        }

        @Override
        public void getEapPassword(getEapPasswordCallback geteappasswordcallback) throws RemoteException {
            HwParcel hwParcel = new HwParcel();
            hwParcel.writeInterfaceToken(ISupplicantStaNetwork.kInterfaceName);
            HwParcel hwParcel2 = new HwParcel();
            try {
                this.mRemote.transact(52, hwParcel, hwParcel2, 0);
                hwParcel2.verifySuccess();
                hwParcel.releaseTemporaryStorage();
                SupplicantStatus supplicantStatus = new SupplicantStatus();
                supplicantStatus.readFromParcel(hwParcel2);
                geteappasswordcallback.onValues(supplicantStatus, hwParcel2.readInt8Vector());
            } finally {
                hwParcel2.release();
            }
        }

        @Override
        public void getEapCACert(getEapCACertCallback geteapcacertcallback) throws RemoteException {
            HwParcel hwParcel = new HwParcel();
            hwParcel.writeInterfaceToken(ISupplicantStaNetwork.kInterfaceName);
            HwParcel hwParcel2 = new HwParcel();
            try {
                this.mRemote.transact(53, hwParcel, hwParcel2, 0);
                hwParcel2.verifySuccess();
                hwParcel.releaseTemporaryStorage();
                SupplicantStatus supplicantStatus = new SupplicantStatus();
                supplicantStatus.readFromParcel(hwParcel2);
                geteapcacertcallback.onValues(supplicantStatus, hwParcel2.readString());
            } finally {
                hwParcel2.release();
            }
        }

        @Override
        public void getEapCAPath(getEapCAPathCallback geteapcapathcallback) throws RemoteException {
            HwParcel hwParcel = new HwParcel();
            hwParcel.writeInterfaceToken(ISupplicantStaNetwork.kInterfaceName);
            HwParcel hwParcel2 = new HwParcel();
            try {
                this.mRemote.transact(54, hwParcel, hwParcel2, 0);
                hwParcel2.verifySuccess();
                hwParcel.releaseTemporaryStorage();
                SupplicantStatus supplicantStatus = new SupplicantStatus();
                supplicantStatus.readFromParcel(hwParcel2);
                geteapcapathcallback.onValues(supplicantStatus, hwParcel2.readString());
            } finally {
                hwParcel2.release();
            }
        }

        @Override
        public void getEapClientCert(getEapClientCertCallback geteapclientcertcallback) throws RemoteException {
            HwParcel hwParcel = new HwParcel();
            hwParcel.writeInterfaceToken(ISupplicantStaNetwork.kInterfaceName);
            HwParcel hwParcel2 = new HwParcel();
            try {
                this.mRemote.transact(55, hwParcel, hwParcel2, 0);
                hwParcel2.verifySuccess();
                hwParcel.releaseTemporaryStorage();
                SupplicantStatus supplicantStatus = new SupplicantStatus();
                supplicantStatus.readFromParcel(hwParcel2);
                geteapclientcertcallback.onValues(supplicantStatus, hwParcel2.readString());
            } finally {
                hwParcel2.release();
            }
        }

        @Override
        public void getEapPrivateKeyId(getEapPrivateKeyIdCallback geteapprivatekeyidcallback) throws RemoteException {
            HwParcel hwParcel = new HwParcel();
            hwParcel.writeInterfaceToken(ISupplicantStaNetwork.kInterfaceName);
            HwParcel hwParcel2 = new HwParcel();
            try {
                this.mRemote.transact(56, hwParcel, hwParcel2, 0);
                hwParcel2.verifySuccess();
                hwParcel.releaseTemporaryStorage();
                SupplicantStatus supplicantStatus = new SupplicantStatus();
                supplicantStatus.readFromParcel(hwParcel2);
                geteapprivatekeyidcallback.onValues(supplicantStatus, hwParcel2.readString());
            } finally {
                hwParcel2.release();
            }
        }

        @Override
        public void getEapSubjectMatch(getEapSubjectMatchCallback geteapsubjectmatchcallback) throws RemoteException {
            HwParcel hwParcel = new HwParcel();
            hwParcel.writeInterfaceToken(ISupplicantStaNetwork.kInterfaceName);
            HwParcel hwParcel2 = new HwParcel();
            try {
                this.mRemote.transact(57, hwParcel, hwParcel2, 0);
                hwParcel2.verifySuccess();
                hwParcel.releaseTemporaryStorage();
                SupplicantStatus supplicantStatus = new SupplicantStatus();
                supplicantStatus.readFromParcel(hwParcel2);
                geteapsubjectmatchcallback.onValues(supplicantStatus, hwParcel2.readString());
            } finally {
                hwParcel2.release();
            }
        }

        @Override
        public void getEapAltSubjectMatch(getEapAltSubjectMatchCallback geteapaltsubjectmatchcallback) throws RemoteException {
            HwParcel hwParcel = new HwParcel();
            hwParcel.writeInterfaceToken(ISupplicantStaNetwork.kInterfaceName);
            HwParcel hwParcel2 = new HwParcel();
            try {
                this.mRemote.transact(58, hwParcel, hwParcel2, 0);
                hwParcel2.verifySuccess();
                hwParcel.releaseTemporaryStorage();
                SupplicantStatus supplicantStatus = new SupplicantStatus();
                supplicantStatus.readFromParcel(hwParcel2);
                geteapaltsubjectmatchcallback.onValues(supplicantStatus, hwParcel2.readString());
            } finally {
                hwParcel2.release();
            }
        }

        @Override
        public void getEapEngine(getEapEngineCallback geteapenginecallback) throws RemoteException {
            HwParcel hwParcel = new HwParcel();
            hwParcel.writeInterfaceToken(ISupplicantStaNetwork.kInterfaceName);
            HwParcel hwParcel2 = new HwParcel();
            try {
                this.mRemote.transact(59, hwParcel, hwParcel2, 0);
                hwParcel2.verifySuccess();
                hwParcel.releaseTemporaryStorage();
                SupplicantStatus supplicantStatus = new SupplicantStatus();
                supplicantStatus.readFromParcel(hwParcel2);
                geteapenginecallback.onValues(supplicantStatus, hwParcel2.readBool());
            } finally {
                hwParcel2.release();
            }
        }

        @Override
        public void getEapEngineID(getEapEngineIDCallback geteapengineidcallback) throws RemoteException {
            HwParcel hwParcel = new HwParcel();
            hwParcel.writeInterfaceToken(ISupplicantStaNetwork.kInterfaceName);
            HwParcel hwParcel2 = new HwParcel();
            try {
                this.mRemote.transact(60, hwParcel, hwParcel2, 0);
                hwParcel2.verifySuccess();
                hwParcel.releaseTemporaryStorage();
                SupplicantStatus supplicantStatus = new SupplicantStatus();
                supplicantStatus.readFromParcel(hwParcel2);
                geteapengineidcallback.onValues(supplicantStatus, hwParcel2.readString());
            } finally {
                hwParcel2.release();
            }
        }

        @Override
        public void getEapDomainSuffixMatch(getEapDomainSuffixMatchCallback geteapdomainsuffixmatchcallback) throws RemoteException {
            HwParcel hwParcel = new HwParcel();
            hwParcel.writeInterfaceToken(ISupplicantStaNetwork.kInterfaceName);
            HwParcel hwParcel2 = new HwParcel();
            try {
                this.mRemote.transact(61, hwParcel, hwParcel2, 0);
                hwParcel2.verifySuccess();
                hwParcel.releaseTemporaryStorage();
                SupplicantStatus supplicantStatus = new SupplicantStatus();
                supplicantStatus.readFromParcel(hwParcel2);
                geteapdomainsuffixmatchcallback.onValues(supplicantStatus, hwParcel2.readString());
            } finally {
                hwParcel2.release();
            }
        }

        @Override
        public void getIdStr(getIdStrCallback getidstrcallback) throws RemoteException {
            HwParcel hwParcel = new HwParcel();
            hwParcel.writeInterfaceToken(ISupplicantStaNetwork.kInterfaceName);
            HwParcel hwParcel2 = new HwParcel();
            try {
                this.mRemote.transact(62, hwParcel, hwParcel2, 0);
                hwParcel2.verifySuccess();
                hwParcel.releaseTemporaryStorage();
                SupplicantStatus supplicantStatus = new SupplicantStatus();
                supplicantStatus.readFromParcel(hwParcel2);
                getidstrcallback.onValues(supplicantStatus, hwParcel2.readString());
            } finally {
                hwParcel2.release();
            }
        }

        @Override
        public void getWpsNfcConfigurationToken(getWpsNfcConfigurationTokenCallback getwpsnfcconfigurationtokencallback) throws RemoteException {
            HwParcel hwParcel = new HwParcel();
            hwParcel.writeInterfaceToken(ISupplicantStaNetwork.kInterfaceName);
            HwParcel hwParcel2 = new HwParcel();
            try {
                this.mRemote.transact(63, hwParcel, hwParcel2, 0);
                hwParcel2.verifySuccess();
                hwParcel.releaseTemporaryStorage();
                SupplicantStatus supplicantStatus = new SupplicantStatus();
                supplicantStatus.readFromParcel(hwParcel2);
                getwpsnfcconfigurationtokencallback.onValues(supplicantStatus, hwParcel2.readInt8Vector());
            } finally {
                hwParcel2.release();
            }
        }

        @Override
        public SupplicantStatus enable(boolean z) throws RemoteException {
            HwParcel hwParcel = new HwParcel();
            hwParcel.writeInterfaceToken(ISupplicantStaNetwork.kInterfaceName);
            hwParcel.writeBool(z);
            HwParcel hwParcel2 = new HwParcel();
            try {
                this.mRemote.transact(64, hwParcel, hwParcel2, 0);
                hwParcel2.verifySuccess();
                hwParcel.releaseTemporaryStorage();
                SupplicantStatus supplicantStatus = new SupplicantStatus();
                supplicantStatus.readFromParcel(hwParcel2);
                return supplicantStatus;
            } finally {
                hwParcel2.release();
            }
        }

        @Override
        public SupplicantStatus disable() throws RemoteException {
            HwParcel hwParcel = new HwParcel();
            hwParcel.writeInterfaceToken(ISupplicantStaNetwork.kInterfaceName);
            HwParcel hwParcel2 = new HwParcel();
            try {
                this.mRemote.transact(65, hwParcel, hwParcel2, 0);
                hwParcel2.verifySuccess();
                hwParcel.releaseTemporaryStorage();
                SupplicantStatus supplicantStatus = new SupplicantStatus();
                supplicantStatus.readFromParcel(hwParcel2);
                return supplicantStatus;
            } finally {
                hwParcel2.release();
            }
        }

        @Override
        public SupplicantStatus select() throws RemoteException {
            HwParcel hwParcel = new HwParcel();
            hwParcel.writeInterfaceToken(ISupplicantStaNetwork.kInterfaceName);
            HwParcel hwParcel2 = new HwParcel();
            try {
                this.mRemote.transact(66, hwParcel, hwParcel2, 0);
                hwParcel2.verifySuccess();
                hwParcel.releaseTemporaryStorage();
                SupplicantStatus supplicantStatus = new SupplicantStatus();
                supplicantStatus.readFromParcel(hwParcel2);
                return supplicantStatus;
            } finally {
                hwParcel2.release();
            }
        }

        @Override
        public SupplicantStatus sendNetworkEapSimGsmAuthResponse(ArrayList<NetworkResponseEapSimGsmAuthParams> arrayList) throws RemoteException {
            HwParcel hwParcel = new HwParcel();
            hwParcel.writeInterfaceToken(ISupplicantStaNetwork.kInterfaceName);
            NetworkResponseEapSimGsmAuthParams.writeVectorToParcel(hwParcel, arrayList);
            HwParcel hwParcel2 = new HwParcel();
            try {
                this.mRemote.transact(67, hwParcel, hwParcel2, 0);
                hwParcel2.verifySuccess();
                hwParcel.releaseTemporaryStorage();
                SupplicantStatus supplicantStatus = new SupplicantStatus();
                supplicantStatus.readFromParcel(hwParcel2);
                return supplicantStatus;
            } finally {
                hwParcel2.release();
            }
        }

        @Override
        public SupplicantStatus sendNetworkEapSimGsmAuthFailure() throws RemoteException {
            HwParcel hwParcel = new HwParcel();
            hwParcel.writeInterfaceToken(ISupplicantStaNetwork.kInterfaceName);
            HwParcel hwParcel2 = new HwParcel();
            try {
                this.mRemote.transact(68, hwParcel, hwParcel2, 0);
                hwParcel2.verifySuccess();
                hwParcel.releaseTemporaryStorage();
                SupplicantStatus supplicantStatus = new SupplicantStatus();
                supplicantStatus.readFromParcel(hwParcel2);
                return supplicantStatus;
            } finally {
                hwParcel2.release();
            }
        }

        @Override
        public SupplicantStatus sendNetworkEapSimUmtsAuthResponse(NetworkResponseEapSimUmtsAuthParams networkResponseEapSimUmtsAuthParams) throws RemoteException {
            HwParcel hwParcel = new HwParcel();
            hwParcel.writeInterfaceToken(ISupplicantStaNetwork.kInterfaceName);
            networkResponseEapSimUmtsAuthParams.writeToParcel(hwParcel);
            HwParcel hwParcel2 = new HwParcel();
            try {
                this.mRemote.transact(69, hwParcel, hwParcel2, 0);
                hwParcel2.verifySuccess();
                hwParcel.releaseTemporaryStorage();
                SupplicantStatus supplicantStatus = new SupplicantStatus();
                supplicantStatus.readFromParcel(hwParcel2);
                return supplicantStatus;
            } finally {
                hwParcel2.release();
            }
        }

        @Override
        public SupplicantStatus sendNetworkEapSimUmtsAutsResponse(byte[] bArr) throws RemoteException {
            HwParcel hwParcel = new HwParcel();
            hwParcel.writeInterfaceToken(ISupplicantStaNetwork.kInterfaceName);
            HwBlob hwBlob = new HwBlob(14);
            hwBlob.putInt8Array(0L, bArr);
            hwParcel.writeBuffer(hwBlob);
            HwParcel hwParcel2 = new HwParcel();
            try {
                this.mRemote.transact(70, hwParcel, hwParcel2, 0);
                hwParcel2.verifySuccess();
                hwParcel.releaseTemporaryStorage();
                SupplicantStatus supplicantStatus = new SupplicantStatus();
                supplicantStatus.readFromParcel(hwParcel2);
                return supplicantStatus;
            } finally {
                hwParcel2.release();
            }
        }

        @Override
        public SupplicantStatus sendNetworkEapSimUmtsAuthFailure() throws RemoteException {
            HwParcel hwParcel = new HwParcel();
            hwParcel.writeInterfaceToken(ISupplicantStaNetwork.kInterfaceName);
            HwParcel hwParcel2 = new HwParcel();
            try {
                this.mRemote.transact(71, hwParcel, hwParcel2, 0);
                hwParcel2.verifySuccess();
                hwParcel.releaseTemporaryStorage();
                SupplicantStatus supplicantStatus = new SupplicantStatus();
                supplicantStatus.readFromParcel(hwParcel2);
                return supplicantStatus;
            } finally {
                hwParcel2.release();
            }
        }

        @Override
        public SupplicantStatus sendNetworkEapIdentityResponse(ArrayList<Byte> arrayList) throws RemoteException {
            HwParcel hwParcel = new HwParcel();
            hwParcel.writeInterfaceToken(ISupplicantStaNetwork.kInterfaceName);
            hwParcel.writeInt8Vector(arrayList);
            HwParcel hwParcel2 = new HwParcel();
            try {
                this.mRemote.transact(72, hwParcel, hwParcel2, 0);
                hwParcel2.verifySuccess();
                hwParcel.releaseTemporaryStorage();
                SupplicantStatus supplicantStatus = new SupplicantStatus();
                supplicantStatus.readFromParcel(hwParcel2);
                return supplicantStatus;
            } finally {
                hwParcel2.release();
            }
        }

        @Override
        public ArrayList<String> interfaceChain() throws RemoteException {
            HwParcel hwParcel = new HwParcel();
            hwParcel.writeInterfaceToken(IBase.kInterfaceName);
            HwParcel hwParcel2 = new HwParcel();
            try {
                this.mRemote.transact(256067662, hwParcel, hwParcel2, 0);
                hwParcel2.verifySuccess();
                hwParcel.releaseTemporaryStorage();
                return hwParcel2.readStringVector();
            } finally {
                hwParcel2.release();
            }
        }

        @Override
        public String interfaceDescriptor() throws RemoteException {
            HwParcel hwParcel = new HwParcel();
            hwParcel.writeInterfaceToken(IBase.kInterfaceName);
            HwParcel hwParcel2 = new HwParcel();
            try {
                this.mRemote.transact(256136003, hwParcel, hwParcel2, 0);
                hwParcel2.verifySuccess();
                hwParcel.releaseTemporaryStorage();
                return hwParcel2.readString();
            } finally {
                hwParcel2.release();
            }
        }

        @Override
        public ArrayList<byte[]> getHashChain() throws RemoteException {
            HwParcel hwParcel = new HwParcel();
            hwParcel.writeInterfaceToken(IBase.kInterfaceName);
            HwParcel hwParcel2 = new HwParcel();
            try {
                this.mRemote.transact(256398152, hwParcel, hwParcel2, 0);
                hwParcel2.verifySuccess();
                hwParcel.releaseTemporaryStorage();
                ArrayList<byte[]> arrayList = new ArrayList<>();
                HwBlob buffer = hwParcel2.readBuffer(16L);
                int int32 = buffer.getInt32(8L);
                HwBlob embeddedBuffer = hwParcel2.readEmbeddedBuffer(int32 * 32, buffer.handle(), 0L, true);
                arrayList.clear();
                for (int i = 0; i < int32; i++) {
                    byte[] bArr = new byte[32];
                    embeddedBuffer.copyToInt8Array(i * 32, bArr, 32);
                    arrayList.add(bArr);
                }
                return arrayList;
            } finally {
                hwParcel2.release();
            }
        }

        @Override
        public void setHALInstrumentation() throws RemoteException {
            HwParcel hwParcel = new HwParcel();
            hwParcel.writeInterfaceToken(IBase.kInterfaceName);
            HwParcel hwParcel2 = new HwParcel();
            try {
                this.mRemote.transact(256462420, hwParcel, hwParcel2, 1);
                hwParcel.releaseTemporaryStorage();
            } finally {
                hwParcel2.release();
            }
        }

        @Override
        public boolean linkToDeath(IHwBinder.DeathRecipient deathRecipient, long j) throws RemoteException {
            return this.mRemote.linkToDeath(deathRecipient, j);
        }

        @Override
        public void ping() throws RemoteException {
            HwParcel hwParcel = new HwParcel();
            hwParcel.writeInterfaceToken(IBase.kInterfaceName);
            HwParcel hwParcel2 = new HwParcel();
            try {
                this.mRemote.transact(256921159, hwParcel, hwParcel2, 0);
                hwParcel2.verifySuccess();
                hwParcel.releaseTemporaryStorage();
            } finally {
                hwParcel2.release();
            }
        }

        @Override
        public DebugInfo getDebugInfo() throws RemoteException {
            HwParcel hwParcel = new HwParcel();
            hwParcel.writeInterfaceToken(IBase.kInterfaceName);
            HwParcel hwParcel2 = new HwParcel();
            try {
                this.mRemote.transact(257049926, hwParcel, hwParcel2, 0);
                hwParcel2.verifySuccess();
                hwParcel.releaseTemporaryStorage();
                DebugInfo debugInfo = new DebugInfo();
                debugInfo.readFromParcel(hwParcel2);
                return debugInfo;
            } finally {
                hwParcel2.release();
            }
        }

        @Override
        public void notifySyspropsChanged() throws RemoteException {
            HwParcel hwParcel = new HwParcel();
            hwParcel.writeInterfaceToken(IBase.kInterfaceName);
            HwParcel hwParcel2 = new HwParcel();
            try {
                this.mRemote.transact(257120595, hwParcel, hwParcel2, 1);
                hwParcel.releaseTemporaryStorage();
            } finally {
                hwParcel2.release();
            }
        }

        @Override
        public boolean unlinkToDeath(IHwBinder.DeathRecipient deathRecipient) throws RemoteException {
            return this.mRemote.unlinkToDeath(deathRecipient);
        }
    }

    public static abstract class Stub extends HwBinder implements ISupplicantStaNetwork {
        @Override
        public IHwBinder asBinder() {
            return this;
        }

        @Override
        public final ArrayList<String> interfaceChain() {
            return new ArrayList<>(Arrays.asList(ISupplicantStaNetwork.kInterfaceName, ISupplicantNetwork.kInterfaceName, IBase.kInterfaceName));
        }

        @Override
        public final String interfaceDescriptor() {
            return ISupplicantStaNetwork.kInterfaceName;
        }

        @Override
        public final ArrayList<byte[]> getHashChain() {
            return new ArrayList<>(Arrays.asList(new byte[]{-79, 46, -16, -67, -40, -92, -46, 71, -88, -90, -23, 96, -78, 39, -19, 50, 56, 63, 43, 2, 65, -11, 93, 103, -4, -22, 110, -1, 106, 103, 55, -6}, new byte[]{-51, -96, 16, 8, -64, 105, 34, -6, 55, -63, 33, 62, -101, -72, 49, -95, 9, -77, 23, 69, 50, -128, 86, 22, -5, 113, 97, -19, -60, 3, -122, 111}, new byte[]{-67, -38, -74, 24, 77, 122, 52, 109, -90, -96, 125, -64, -126, -116, -15, -102, 105, 111, 76, -86, 54, 17, -59, 31, 46, 20, 86, 90, 20, -76, 15, -39}));
        }

        @Override
        public final void setHALInstrumentation() {
        }

        @Override
        public final boolean linkToDeath(IHwBinder.DeathRecipient deathRecipient, long j) {
            return true;
        }

        @Override
        public final void ping() {
        }

        @Override
        public final DebugInfo getDebugInfo() {
            DebugInfo debugInfo = new DebugInfo();
            debugInfo.pid = HidlSupport.getPidIfSharable();
            debugInfo.ptr = 0L;
            debugInfo.arch = 0;
            return debugInfo;
        }

        @Override
        public final void notifySyspropsChanged() {
            HwBinder.enableInstrumentation();
        }

        @Override
        public final boolean unlinkToDeath(IHwBinder.DeathRecipient deathRecipient) {
            return true;
        }

        @Override
        public IHwInterface queryLocalInterface(String str) {
            if (ISupplicantStaNetwork.kInterfaceName.equals(str)) {
                return this;
            }
            return null;
        }

        public void registerAsService(String str) throws RemoteException {
            registerService(str);
        }

        public String toString() {
            return interfaceDescriptor() + "@Stub";
        }

        @Override
        public void onTransact(int i, HwParcel hwParcel, final HwParcel hwParcel2, int i2) throws RemoteException {
            switch (i) {
                case 1:
                    if (((i2 & 1) != 0 ? 1 : 0) != 0) {
                        hwParcel2.writeStatus(Integer.MIN_VALUE);
                        hwParcel2.send();
                    } else {
                        hwParcel.enforceInterface(ISupplicantNetwork.kInterfaceName);
                        getId(new ISupplicantNetwork.getIdCallback() {
                            @Override
                            public void onValues(SupplicantStatus supplicantStatus, int i3) {
                                hwParcel2.writeStatus(0);
                                supplicantStatus.writeToParcel(hwParcel2);
                                hwParcel2.writeInt32(i3);
                                hwParcel2.send();
                            }
                        });
                    }
                    break;
                case 2:
                    if (((i2 & 1) != 0 ? 1 : 0) != 0) {
                        hwParcel2.writeStatus(Integer.MIN_VALUE);
                        hwParcel2.send();
                    } else {
                        hwParcel.enforceInterface(ISupplicantNetwork.kInterfaceName);
                        getInterfaceName(new ISupplicantNetwork.getInterfaceNameCallback() {
                            @Override
                            public void onValues(SupplicantStatus supplicantStatus, String str) {
                                hwParcel2.writeStatus(0);
                                supplicantStatus.writeToParcel(hwParcel2);
                                hwParcel2.writeString(str);
                                hwParcel2.send();
                            }
                        });
                    }
                    break;
                case 3:
                    if (((i2 & 1) != 0 ? 1 : 0) != 0) {
                        hwParcel2.writeStatus(Integer.MIN_VALUE);
                        hwParcel2.send();
                    } else {
                        hwParcel.enforceInterface(ISupplicantNetwork.kInterfaceName);
                        getType(new ISupplicantNetwork.getTypeCallback() {
                            @Override
                            public void onValues(SupplicantStatus supplicantStatus, int i3) {
                                hwParcel2.writeStatus(0);
                                supplicantStatus.writeToParcel(hwParcel2);
                                hwParcel2.writeInt32(i3);
                                hwParcel2.send();
                            }
                        });
                    }
                    break;
                case 4:
                    if ((i2 & 1) != 0) {
                        hwParcel2.writeStatus(Integer.MIN_VALUE);
                        hwParcel2.send();
                    } else {
                        hwParcel.enforceInterface(ISupplicantStaNetwork.kInterfaceName);
                        SupplicantStatus supplicantStatusRegisterCallback = registerCallback(ISupplicantStaNetworkCallback.asInterface(hwParcel.readStrongBinder()));
                        hwParcel2.writeStatus(0);
                        supplicantStatusRegisterCallback.writeToParcel(hwParcel2);
                        hwParcel2.send();
                    }
                    break;
                case 5:
                    if ((i2 & 1) != 0) {
                        hwParcel2.writeStatus(Integer.MIN_VALUE);
                        hwParcel2.send();
                    } else {
                        hwParcel.enforceInterface(ISupplicantStaNetwork.kInterfaceName);
                        SupplicantStatus ssid = setSsid(hwParcel.readInt8Vector());
                        hwParcel2.writeStatus(0);
                        ssid.writeToParcel(hwParcel2);
                        hwParcel2.send();
                    }
                    break;
                case 6:
                    if ((i2 & 1) != 0) {
                        hwParcel2.writeStatus(Integer.MIN_VALUE);
                        hwParcel2.send();
                    } else {
                        hwParcel.enforceInterface(ISupplicantStaNetwork.kInterfaceName);
                        byte[] bArr = new byte[6];
                        hwParcel.readBuffer(6L).copyToInt8Array(0L, bArr, 6);
                        SupplicantStatus bssid = setBssid(bArr);
                        hwParcel2.writeStatus(0);
                        bssid.writeToParcel(hwParcel2);
                        hwParcel2.send();
                    }
                    break;
                case 7:
                    if ((i2 & 1) != 0) {
                        hwParcel2.writeStatus(Integer.MIN_VALUE);
                        hwParcel2.send();
                    } else {
                        hwParcel.enforceInterface(ISupplicantStaNetwork.kInterfaceName);
                        SupplicantStatus scanSsid = setScanSsid(hwParcel.readBool());
                        hwParcel2.writeStatus(0);
                        scanSsid.writeToParcel(hwParcel2);
                        hwParcel2.send();
                    }
                    break;
                case 8:
                    if ((i2 & 1) != 0) {
                        hwParcel2.writeStatus(Integer.MIN_VALUE);
                        hwParcel2.send();
                    } else {
                        hwParcel.enforceInterface(ISupplicantStaNetwork.kInterfaceName);
                        SupplicantStatus keyMgmt = setKeyMgmt(hwParcel.readInt32());
                        hwParcel2.writeStatus(0);
                        keyMgmt.writeToParcel(hwParcel2);
                        hwParcel2.send();
                    }
                    break;
                case 9:
                    if ((i2 & 1) != 0) {
                        hwParcel2.writeStatus(Integer.MIN_VALUE);
                        hwParcel2.send();
                    } else {
                        hwParcel.enforceInterface(ISupplicantStaNetwork.kInterfaceName);
                        SupplicantStatus proto = setProto(hwParcel.readInt32());
                        hwParcel2.writeStatus(0);
                        proto.writeToParcel(hwParcel2);
                        hwParcel2.send();
                    }
                    break;
                case 10:
                    if ((i2 & 1) != 0) {
                        hwParcel2.writeStatus(Integer.MIN_VALUE);
                        hwParcel2.send();
                    } else {
                        hwParcel.enforceInterface(ISupplicantStaNetwork.kInterfaceName);
                        SupplicantStatus authAlg = setAuthAlg(hwParcel.readInt32());
                        hwParcel2.writeStatus(0);
                        authAlg.writeToParcel(hwParcel2);
                        hwParcel2.send();
                    }
                    break;
                case 11:
                    if ((i2 & 1) != 0) {
                        hwParcel2.writeStatus(Integer.MIN_VALUE);
                        hwParcel2.send();
                    } else {
                        hwParcel.enforceInterface(ISupplicantStaNetwork.kInterfaceName);
                        SupplicantStatus groupCipher = setGroupCipher(hwParcel.readInt32());
                        hwParcel2.writeStatus(0);
                        groupCipher.writeToParcel(hwParcel2);
                        hwParcel2.send();
                    }
                    break;
                case 12:
                    if ((i2 & 1) != 0) {
                        hwParcel2.writeStatus(Integer.MIN_VALUE);
                        hwParcel2.send();
                    } else {
                        hwParcel.enforceInterface(ISupplicantStaNetwork.kInterfaceName);
                        SupplicantStatus pairwiseCipher = setPairwiseCipher(hwParcel.readInt32());
                        hwParcel2.writeStatus(0);
                        pairwiseCipher.writeToParcel(hwParcel2);
                        hwParcel2.send();
                    }
                    break;
                case 13:
                    if ((i2 & 1) != 0) {
                        hwParcel2.writeStatus(Integer.MIN_VALUE);
                        hwParcel2.send();
                    } else {
                        hwParcel.enforceInterface(ISupplicantStaNetwork.kInterfaceName);
                        SupplicantStatus pskPassphrase = setPskPassphrase(hwParcel.readString());
                        hwParcel2.writeStatus(0);
                        pskPassphrase.writeToParcel(hwParcel2);
                        hwParcel2.send();
                    }
                    break;
                case 14:
                    if ((i2 & 1) != 0) {
                        hwParcel2.writeStatus(Integer.MIN_VALUE);
                        hwParcel2.send();
                    } else {
                        hwParcel.enforceInterface(ISupplicantStaNetwork.kInterfaceName);
                        byte[] bArr2 = new byte[32];
                        hwParcel.readBuffer(32L).copyToInt8Array(0L, bArr2, 32);
                        SupplicantStatus psk = setPsk(bArr2);
                        hwParcel2.writeStatus(0);
                        psk.writeToParcel(hwParcel2);
                        hwParcel2.send();
                    }
                    break;
                case 15:
                    if ((i2 & 1) != 0) {
                        hwParcel2.writeStatus(Integer.MIN_VALUE);
                        hwParcel2.send();
                    } else {
                        hwParcel.enforceInterface(ISupplicantStaNetwork.kInterfaceName);
                        SupplicantStatus wepKey = setWepKey(hwParcel.readInt32(), hwParcel.readInt8Vector());
                        hwParcel2.writeStatus(0);
                        wepKey.writeToParcel(hwParcel2);
                        hwParcel2.send();
                    }
                    break;
                case 16:
                    if ((i2 & 1) != 0) {
                        hwParcel2.writeStatus(Integer.MIN_VALUE);
                        hwParcel2.send();
                    } else {
                        hwParcel.enforceInterface(ISupplicantStaNetwork.kInterfaceName);
                        SupplicantStatus wepTxKeyIdx = setWepTxKeyIdx(hwParcel.readInt32());
                        hwParcel2.writeStatus(0);
                        wepTxKeyIdx.writeToParcel(hwParcel2);
                        hwParcel2.send();
                    }
                    break;
                case 17:
                    if ((i2 & 1) != 0) {
                        hwParcel2.writeStatus(Integer.MIN_VALUE);
                        hwParcel2.send();
                    } else {
                        hwParcel.enforceInterface(ISupplicantStaNetwork.kInterfaceName);
                        SupplicantStatus requirePmf = setRequirePmf(hwParcel.readBool());
                        hwParcel2.writeStatus(0);
                        requirePmf.writeToParcel(hwParcel2);
                        hwParcel2.send();
                    }
                    break;
                case 18:
                    if ((i2 & 1) != 0) {
                        hwParcel2.writeStatus(Integer.MIN_VALUE);
                        hwParcel2.send();
                    } else {
                        hwParcel.enforceInterface(ISupplicantStaNetwork.kInterfaceName);
                        SupplicantStatus eapMethod = setEapMethod(hwParcel.readInt32());
                        hwParcel2.writeStatus(0);
                        eapMethod.writeToParcel(hwParcel2);
                        hwParcel2.send();
                    }
                    break;
                case 19:
                    if ((i2 & 1) != 0) {
                        hwParcel2.writeStatus(Integer.MIN_VALUE);
                        hwParcel2.send();
                    } else {
                        hwParcel.enforceInterface(ISupplicantStaNetwork.kInterfaceName);
                        SupplicantStatus eapPhase2Method = setEapPhase2Method(hwParcel.readInt32());
                        hwParcel2.writeStatus(0);
                        eapPhase2Method.writeToParcel(hwParcel2);
                        hwParcel2.send();
                    }
                    break;
                case 20:
                    if ((i2 & 1) != 0) {
                        hwParcel2.writeStatus(Integer.MIN_VALUE);
                        hwParcel2.send();
                    } else {
                        hwParcel.enforceInterface(ISupplicantStaNetwork.kInterfaceName);
                        SupplicantStatus eapIdentity = setEapIdentity(hwParcel.readInt8Vector());
                        hwParcel2.writeStatus(0);
                        eapIdentity.writeToParcel(hwParcel2);
                        hwParcel2.send();
                    }
                    break;
                case ISupplicantStaIfaceCallback.ReasonCode.UNSUPPORTED_RSN_IE_VERSION:
                    if ((i2 & 1) != 0) {
                        hwParcel2.writeStatus(Integer.MIN_VALUE);
                        hwParcel2.send();
                    } else {
                        hwParcel.enforceInterface(ISupplicantStaNetwork.kInterfaceName);
                        SupplicantStatus eapAnonymousIdentity = setEapAnonymousIdentity(hwParcel.readInt8Vector());
                        hwParcel2.writeStatus(0);
                        eapAnonymousIdentity.writeToParcel(hwParcel2);
                        hwParcel2.send();
                    }
                    break;
                case 22:
                    if ((i2 & 1) != 0) {
                        hwParcel2.writeStatus(Integer.MIN_VALUE);
                        hwParcel2.send();
                    } else {
                        hwParcel.enforceInterface(ISupplicantStaNetwork.kInterfaceName);
                        SupplicantStatus eapPassword = setEapPassword(hwParcel.readInt8Vector());
                        hwParcel2.writeStatus(0);
                        eapPassword.writeToParcel(hwParcel2);
                        hwParcel2.send();
                    }
                    break;
                case 23:
                    if ((i2 & 1) != 0) {
                        hwParcel2.writeStatus(Integer.MIN_VALUE);
                        hwParcel2.send();
                    } else {
                        hwParcel.enforceInterface(ISupplicantStaNetwork.kInterfaceName);
                        SupplicantStatus eapCACert = setEapCACert(hwParcel.readString());
                        hwParcel2.writeStatus(0);
                        eapCACert.writeToParcel(hwParcel2);
                        hwParcel2.send();
                    }
                    break;
                case 24:
                    if ((i2 & 1) != 0) {
                        hwParcel2.writeStatus(Integer.MIN_VALUE);
                        hwParcel2.send();
                    } else {
                        hwParcel.enforceInterface(ISupplicantStaNetwork.kInterfaceName);
                        SupplicantStatus eapCAPath = setEapCAPath(hwParcel.readString());
                        hwParcel2.writeStatus(0);
                        eapCAPath.writeToParcel(hwParcel2);
                        hwParcel2.send();
                    }
                    break;
                case 25:
                    if ((i2 & 1) != 0) {
                        hwParcel2.writeStatus(Integer.MIN_VALUE);
                        hwParcel2.send();
                    } else {
                        hwParcel.enforceInterface(ISupplicantStaNetwork.kInterfaceName);
                        SupplicantStatus eapClientCert = setEapClientCert(hwParcel.readString());
                        hwParcel2.writeStatus(0);
                        eapClientCert.writeToParcel(hwParcel2);
                        hwParcel2.send();
                    }
                    break;
                case ISupplicantStaIfaceCallback.ReasonCode.TDLS_TEARDOWN_UNSPECIFIED:
                    if ((i2 & 1) != 0) {
                        hwParcel2.writeStatus(Integer.MIN_VALUE);
                        hwParcel2.send();
                    } else {
                        hwParcel.enforceInterface(ISupplicantStaNetwork.kInterfaceName);
                        SupplicantStatus eapPrivateKeyId = setEapPrivateKeyId(hwParcel.readString());
                        hwParcel2.writeStatus(0);
                        eapPrivateKeyId.writeToParcel(hwParcel2);
                        hwParcel2.send();
                    }
                    break;
                case 27:
                    if ((i2 & 1) != 0) {
                        hwParcel2.writeStatus(Integer.MIN_VALUE);
                        hwParcel2.send();
                    } else {
                        hwParcel.enforceInterface(ISupplicantStaNetwork.kInterfaceName);
                        SupplicantStatus eapSubjectMatch = setEapSubjectMatch(hwParcel.readString());
                        hwParcel2.writeStatus(0);
                        eapSubjectMatch.writeToParcel(hwParcel2);
                        hwParcel2.send();
                    }
                    break;
                case 28:
                    if ((i2 & 1) != 0) {
                        hwParcel2.writeStatus(Integer.MIN_VALUE);
                        hwParcel2.send();
                    } else {
                        hwParcel.enforceInterface(ISupplicantStaNetwork.kInterfaceName);
                        SupplicantStatus eapAltSubjectMatch = setEapAltSubjectMatch(hwParcel.readString());
                        hwParcel2.writeStatus(0);
                        eapAltSubjectMatch.writeToParcel(hwParcel2);
                        hwParcel2.send();
                    }
                    break;
                case 29:
                    if ((i2 & 1) != 0) {
                        hwParcel2.writeStatus(Integer.MIN_VALUE);
                        hwParcel2.send();
                    } else {
                        hwParcel.enforceInterface(ISupplicantStaNetwork.kInterfaceName);
                        SupplicantStatus eapEngine = setEapEngine(hwParcel.readBool());
                        hwParcel2.writeStatus(0);
                        eapEngine.writeToParcel(hwParcel2);
                        hwParcel2.send();
                    }
                    break;
                case 30:
                    if ((i2 & 1) != 0) {
                        hwParcel2.writeStatus(Integer.MIN_VALUE);
                        hwParcel2.send();
                    } else {
                        hwParcel.enforceInterface(ISupplicantStaNetwork.kInterfaceName);
                        SupplicantStatus eapEngineID = setEapEngineID(hwParcel.readString());
                        hwParcel2.writeStatus(0);
                        eapEngineID.writeToParcel(hwParcel2);
                        hwParcel2.send();
                    }
                    break;
                case 31:
                    if ((i2 & 1) != 0) {
                        hwParcel2.writeStatus(Integer.MIN_VALUE);
                        hwParcel2.send();
                    } else {
                        hwParcel.enforceInterface(ISupplicantStaNetwork.kInterfaceName);
                        SupplicantStatus eapDomainSuffixMatch = setEapDomainSuffixMatch(hwParcel.readString());
                        hwParcel2.writeStatus(0);
                        eapDomainSuffixMatch.writeToParcel(hwParcel2);
                        hwParcel2.send();
                    }
                    break;
                case 32:
                    if ((i2 & 1) != 0) {
                        hwParcel2.writeStatus(Integer.MIN_VALUE);
                        hwParcel2.send();
                    } else {
                        hwParcel.enforceInterface(ISupplicantStaNetwork.kInterfaceName);
                        SupplicantStatus proactiveKeyCaching = setProactiveKeyCaching(hwParcel.readBool());
                        hwParcel2.writeStatus(0);
                        proactiveKeyCaching.writeToParcel(hwParcel2);
                        hwParcel2.send();
                    }
                    break;
                case 33:
                    if ((i2 & 1) != 0) {
                        hwParcel2.writeStatus(Integer.MIN_VALUE);
                        hwParcel2.send();
                    } else {
                        hwParcel.enforceInterface(ISupplicantStaNetwork.kInterfaceName);
                        SupplicantStatus idStr = setIdStr(hwParcel.readString());
                        hwParcel2.writeStatus(0);
                        idStr.writeToParcel(hwParcel2);
                        hwParcel2.send();
                    }
                    break;
                case 34:
                    if ((i2 & 1) != 0) {
                        hwParcel2.writeStatus(Integer.MIN_VALUE);
                        hwParcel2.send();
                    } else {
                        hwParcel.enforceInterface(ISupplicantStaNetwork.kInterfaceName);
                        SupplicantStatus updateIdentifier = setUpdateIdentifier(hwParcel.readInt32());
                        hwParcel2.writeStatus(0);
                        updateIdentifier.writeToParcel(hwParcel2);
                        hwParcel2.send();
                    }
                    break;
                case 35:
                    if (((i2 & 1) != 0 ? 1 : 0) != 0) {
                        hwParcel2.writeStatus(Integer.MIN_VALUE);
                        hwParcel2.send();
                    } else {
                        hwParcel.enforceInterface(ISupplicantStaNetwork.kInterfaceName);
                        getSsid(new getSsidCallback() {
                            @Override
                            public void onValues(SupplicantStatus supplicantStatus, ArrayList<Byte> arrayList) {
                                hwParcel2.writeStatus(0);
                                supplicantStatus.writeToParcel(hwParcel2);
                                hwParcel2.writeInt8Vector(arrayList);
                                hwParcel2.send();
                            }
                        });
                    }
                    break;
                case ISupplicantStaIfaceCallback.ReasonCode.STA_LEAVING:
                    if (((i2 & 1) != 0 ? 1 : 0) != 0) {
                        hwParcel2.writeStatus(Integer.MIN_VALUE);
                        hwParcel2.send();
                    } else {
                        hwParcel.enforceInterface(ISupplicantStaNetwork.kInterfaceName);
                        getBssid(new getBssidCallback() {
                            @Override
                            public void onValues(SupplicantStatus supplicantStatus, byte[] bArr3) {
                                hwParcel2.writeStatus(0);
                                supplicantStatus.writeToParcel(hwParcel2);
                                HwBlob hwBlob = new HwBlob(6);
                                hwBlob.putInt8Array(0L, bArr3);
                                hwParcel2.writeBuffer(hwBlob);
                                hwParcel2.send();
                            }
                        });
                    }
                    break;
                case 37:
                    if (((i2 & 1) != 0 ? 1 : 0) != 0) {
                        hwParcel2.writeStatus(Integer.MIN_VALUE);
                        hwParcel2.send();
                    } else {
                        hwParcel.enforceInterface(ISupplicantStaNetwork.kInterfaceName);
                        getScanSsid(new getScanSsidCallback() {
                            @Override
                            public void onValues(SupplicantStatus supplicantStatus, boolean z) {
                                hwParcel2.writeStatus(0);
                                supplicantStatus.writeToParcel(hwParcel2);
                                hwParcel2.writeBool(z);
                                hwParcel2.send();
                            }
                        });
                    }
                    break;
                case 38:
                    if (((i2 & 1) != 0 ? 1 : 0) != 0) {
                        hwParcel2.writeStatus(Integer.MIN_VALUE);
                        hwParcel2.send();
                    } else {
                        hwParcel.enforceInterface(ISupplicantStaNetwork.kInterfaceName);
                        getKeyMgmt(new getKeyMgmtCallback() {
                            @Override
                            public void onValues(SupplicantStatus supplicantStatus, int i3) {
                                hwParcel2.writeStatus(0);
                                supplicantStatus.writeToParcel(hwParcel2);
                                hwParcel2.writeInt32(i3);
                                hwParcel2.send();
                            }
                        });
                    }
                    break;
                case 39:
                    if (((i2 & 1) != 0 ? 1 : 0) != 0) {
                        hwParcel2.writeStatus(Integer.MIN_VALUE);
                        hwParcel2.send();
                    } else {
                        hwParcel.enforceInterface(ISupplicantStaNetwork.kInterfaceName);
                        getProto(new getProtoCallback() {
                            @Override
                            public void onValues(SupplicantStatus supplicantStatus, int i3) {
                                hwParcel2.writeStatus(0);
                                supplicantStatus.writeToParcel(hwParcel2);
                                hwParcel2.writeInt32(i3);
                                hwParcel2.send();
                            }
                        });
                    }
                    break;
                case ISupplicantStaIfaceCallback.StatusCode.INVALID_IE:
                    if (((i2 & 1) != 0 ? 1 : 0) != 0) {
                        hwParcel2.writeStatus(Integer.MIN_VALUE);
                        hwParcel2.send();
                    } else {
                        hwParcel.enforceInterface(ISupplicantStaNetwork.kInterfaceName);
                        getAuthAlg(new getAuthAlgCallback() {
                            @Override
                            public void onValues(SupplicantStatus supplicantStatus, int i3) {
                                hwParcel2.writeStatus(0);
                                supplicantStatus.writeToParcel(hwParcel2);
                                hwParcel2.writeInt32(i3);
                                hwParcel2.send();
                            }
                        });
                    }
                    break;
                case ISupplicantStaIfaceCallback.StatusCode.GROUP_CIPHER_NOT_VALID:
                    if (((i2 & 1) != 0 ? 1 : 0) != 0) {
                        hwParcel2.writeStatus(Integer.MIN_VALUE);
                        hwParcel2.send();
                    } else {
                        hwParcel.enforceInterface(ISupplicantStaNetwork.kInterfaceName);
                        getGroupCipher(new getGroupCipherCallback() {
                            @Override
                            public void onValues(SupplicantStatus supplicantStatus, int i3) {
                                hwParcel2.writeStatus(0);
                                supplicantStatus.writeToParcel(hwParcel2);
                                hwParcel2.writeInt32(i3);
                                hwParcel2.send();
                            }
                        });
                    }
                    break;
                case 42:
                    if (((i2 & 1) != 0 ? 1 : 0) != 0) {
                        hwParcel2.writeStatus(Integer.MIN_VALUE);
                        hwParcel2.send();
                    } else {
                        hwParcel.enforceInterface(ISupplicantStaNetwork.kInterfaceName);
                        getPairwiseCipher(new getPairwiseCipherCallback() {
                            @Override
                            public void onValues(SupplicantStatus supplicantStatus, int i3) {
                                hwParcel2.writeStatus(0);
                                supplicantStatus.writeToParcel(hwParcel2);
                                hwParcel2.writeInt32(i3);
                                hwParcel2.send();
                            }
                        });
                    }
                    break;
                case 43:
                    if (((i2 & 1) != 0 ? 1 : 0) != 0) {
                        hwParcel2.writeStatus(Integer.MIN_VALUE);
                        hwParcel2.send();
                    } else {
                        hwParcel.enforceInterface(ISupplicantStaNetwork.kInterfaceName);
                        getPskPassphrase(new getPskPassphraseCallback() {
                            @Override
                            public void onValues(SupplicantStatus supplicantStatus, String str) {
                                hwParcel2.writeStatus(0);
                                supplicantStatus.writeToParcel(hwParcel2);
                                hwParcel2.writeString(str);
                                hwParcel2.send();
                            }
                        });
                    }
                    break;
                case ISupplicantStaIfaceCallback.StatusCode.UNSUPPORTED_RSN_IE_VERSION:
                    if (((i2 & 1) != 0 ? 1 : 0) != 0) {
                        hwParcel2.writeStatus(Integer.MIN_VALUE);
                        hwParcel2.send();
                    } else {
                        hwParcel.enforceInterface(ISupplicantStaNetwork.kInterfaceName);
                        getPsk(new getPskCallback() {
                            @Override
                            public void onValues(SupplicantStatus supplicantStatus, byte[] bArr3) {
                                hwParcel2.writeStatus(0);
                                supplicantStatus.writeToParcel(hwParcel2);
                                HwBlob hwBlob = new HwBlob(32);
                                hwBlob.putInt8Array(0L, bArr3);
                                hwParcel2.writeBuffer(hwBlob);
                                hwParcel2.send();
                            }
                        });
                    }
                    break;
                case 45:
                    if (((i2 & 1) != 0 ? 1 : 0) != 0) {
                        hwParcel2.writeStatus(Integer.MIN_VALUE);
                        hwParcel2.send();
                    } else {
                        hwParcel.enforceInterface(ISupplicantStaNetwork.kInterfaceName);
                        getWepKey(hwParcel.readInt32(), new getWepKeyCallback() {
                            @Override
                            public void onValues(SupplicantStatus supplicantStatus, ArrayList<Byte> arrayList) {
                                hwParcel2.writeStatus(0);
                                supplicantStatus.writeToParcel(hwParcel2);
                                hwParcel2.writeInt8Vector(arrayList);
                                hwParcel2.send();
                            }
                        });
                    }
                    break;
                case 46:
                    if (((i2 & 1) != 0 ? 1 : 0) != 0) {
                        hwParcel2.writeStatus(Integer.MIN_VALUE);
                        hwParcel2.send();
                    } else {
                        hwParcel.enforceInterface(ISupplicantStaNetwork.kInterfaceName);
                        getWepTxKeyIdx(new getWepTxKeyIdxCallback() {
                            @Override
                            public void onValues(SupplicantStatus supplicantStatus, int i3) {
                                hwParcel2.writeStatus(0);
                                supplicantStatus.writeToParcel(hwParcel2);
                                hwParcel2.writeInt32(i3);
                                hwParcel2.send();
                            }
                        });
                    }
                    break;
                case 47:
                    if (((i2 & 1) != 0 ? 1 : 0) != 0) {
                        hwParcel2.writeStatus(Integer.MIN_VALUE);
                        hwParcel2.send();
                    } else {
                        hwParcel.enforceInterface(ISupplicantStaNetwork.kInterfaceName);
                        getRequirePmf(new getRequirePmfCallback() {
                            @Override
                            public void onValues(SupplicantStatus supplicantStatus, boolean z) {
                                hwParcel2.writeStatus(0);
                                supplicantStatus.writeToParcel(hwParcel2);
                                hwParcel2.writeBool(z);
                                hwParcel2.send();
                            }
                        });
                    }
                    break;
                case 48:
                    if (((i2 & 1) != 0 ? 1 : 0) != 0) {
                        hwParcel2.writeStatus(Integer.MIN_VALUE);
                        hwParcel2.send();
                    } else {
                        hwParcel.enforceInterface(ISupplicantStaNetwork.kInterfaceName);
                        getEapMethod(new getEapMethodCallback() {
                            @Override
                            public void onValues(SupplicantStatus supplicantStatus, int i3) {
                                hwParcel2.writeStatus(0);
                                supplicantStatus.writeToParcel(hwParcel2);
                                hwParcel2.writeInt32(i3);
                                hwParcel2.send();
                            }
                        });
                    }
                    break;
                case 49:
                    if (((i2 & 1) != 0 ? 1 : 0) != 0) {
                        hwParcel2.writeStatus(Integer.MIN_VALUE);
                        hwParcel2.send();
                    } else {
                        hwParcel.enforceInterface(ISupplicantStaNetwork.kInterfaceName);
                        getEapPhase2Method(new getEapPhase2MethodCallback() {
                            @Override
                            public void onValues(SupplicantStatus supplicantStatus, int i3) {
                                hwParcel2.writeStatus(0);
                                supplicantStatus.writeToParcel(hwParcel2);
                                hwParcel2.writeInt32(i3);
                                hwParcel2.send();
                            }
                        });
                    }
                    break;
                case 50:
                    if (((i2 & 1) != 0 ? 1 : 0) != 0) {
                        hwParcel2.writeStatus(Integer.MIN_VALUE);
                        hwParcel2.send();
                    } else {
                        hwParcel.enforceInterface(ISupplicantStaNetwork.kInterfaceName);
                        getEapIdentity(new getEapIdentityCallback() {
                            @Override
                            public void onValues(SupplicantStatus supplicantStatus, ArrayList<Byte> arrayList) {
                                hwParcel2.writeStatus(0);
                                supplicantStatus.writeToParcel(hwParcel2);
                                hwParcel2.writeInt8Vector(arrayList);
                                hwParcel2.send();
                            }
                        });
                    }
                    break;
                case 51:
                    if (((i2 & 1) != 0 ? 1 : 0) != 0) {
                        hwParcel2.writeStatus(Integer.MIN_VALUE);
                        hwParcel2.send();
                    } else {
                        hwParcel.enforceInterface(ISupplicantStaNetwork.kInterfaceName);
                        getEapAnonymousIdentity(new getEapAnonymousIdentityCallback() {
                            @Override
                            public void onValues(SupplicantStatus supplicantStatus, ArrayList<Byte> arrayList) {
                                hwParcel2.writeStatus(0);
                                supplicantStatus.writeToParcel(hwParcel2);
                                hwParcel2.writeInt8Vector(arrayList);
                                hwParcel2.send();
                            }
                        });
                    }
                    break;
                case 52:
                    if (((i2 & 1) != 0 ? 1 : 0) != 0) {
                        hwParcel2.writeStatus(Integer.MIN_VALUE);
                        hwParcel2.send();
                    } else {
                        hwParcel.enforceInterface(ISupplicantStaNetwork.kInterfaceName);
                        getEapPassword(new getEapPasswordCallback() {
                            @Override
                            public void onValues(SupplicantStatus supplicantStatus, ArrayList<Byte> arrayList) {
                                hwParcel2.writeStatus(0);
                                supplicantStatus.writeToParcel(hwParcel2);
                                hwParcel2.writeInt8Vector(arrayList);
                                hwParcel2.send();
                            }
                        });
                    }
                    break;
                case 53:
                    if (((i2 & 1) != 0 ? 1 : 0) != 0) {
                        hwParcel2.writeStatus(Integer.MIN_VALUE);
                        hwParcel2.send();
                    } else {
                        hwParcel.enforceInterface(ISupplicantStaNetwork.kInterfaceName);
                        getEapCACert(new getEapCACertCallback() {
                            @Override
                            public void onValues(SupplicantStatus supplicantStatus, String str) {
                                hwParcel2.writeStatus(0);
                                supplicantStatus.writeToParcel(hwParcel2);
                                hwParcel2.writeString(str);
                                hwParcel2.send();
                            }
                        });
                    }
                    break;
                case 54:
                    if (((i2 & 1) != 0 ? 1 : 0) != 0) {
                        hwParcel2.writeStatus(Integer.MIN_VALUE);
                        hwParcel2.send();
                    } else {
                        hwParcel.enforceInterface(ISupplicantStaNetwork.kInterfaceName);
                        getEapCAPath(new getEapCAPathCallback() {
                            @Override
                            public void onValues(SupplicantStatus supplicantStatus, String str) {
                                hwParcel2.writeStatus(0);
                                supplicantStatus.writeToParcel(hwParcel2);
                                hwParcel2.writeString(str);
                                hwParcel2.send();
                            }
                        });
                    }
                    break;
                case 55:
                    if (((i2 & 1) != 0 ? 1 : 0) != 0) {
                        hwParcel2.writeStatus(Integer.MIN_VALUE);
                        hwParcel2.send();
                    } else {
                        hwParcel.enforceInterface(ISupplicantStaNetwork.kInterfaceName);
                        getEapClientCert(new getEapClientCertCallback() {
                            @Override
                            public void onValues(SupplicantStatus supplicantStatus, String str) {
                                hwParcel2.writeStatus(0);
                                supplicantStatus.writeToParcel(hwParcel2);
                                hwParcel2.writeString(str);
                                hwParcel2.send();
                            }
                        });
                    }
                    break;
                case 56:
                    if (((i2 & 1) != 0 ? 1 : 0) != 0) {
                        hwParcel2.writeStatus(Integer.MIN_VALUE);
                        hwParcel2.send();
                    } else {
                        hwParcel.enforceInterface(ISupplicantStaNetwork.kInterfaceName);
                        getEapPrivateKeyId(new getEapPrivateKeyIdCallback() {
                            @Override
                            public void onValues(SupplicantStatus supplicantStatus, String str) {
                                hwParcel2.writeStatus(0);
                                supplicantStatus.writeToParcel(hwParcel2);
                                hwParcel2.writeString(str);
                                hwParcel2.send();
                            }
                        });
                    }
                    break;
                case 57:
                    if (((i2 & 1) != 0 ? 1 : 0) != 0) {
                        hwParcel2.writeStatus(Integer.MIN_VALUE);
                        hwParcel2.send();
                    } else {
                        hwParcel.enforceInterface(ISupplicantStaNetwork.kInterfaceName);
                        getEapSubjectMatch(new getEapSubjectMatchCallback() {
                            @Override
                            public void onValues(SupplicantStatus supplicantStatus, String str) {
                                hwParcel2.writeStatus(0);
                                supplicantStatus.writeToParcel(hwParcel2);
                                hwParcel2.writeString(str);
                                hwParcel2.send();
                            }
                        });
                    }
                    break;
                case 58:
                    if (((i2 & 1) != 0 ? 1 : 0) != 0) {
                        hwParcel2.writeStatus(Integer.MIN_VALUE);
                        hwParcel2.send();
                    } else {
                        hwParcel.enforceInterface(ISupplicantStaNetwork.kInterfaceName);
                        getEapAltSubjectMatch(new getEapAltSubjectMatchCallback() {
                            @Override
                            public void onValues(SupplicantStatus supplicantStatus, String str) {
                                hwParcel2.writeStatus(0);
                                supplicantStatus.writeToParcel(hwParcel2);
                                hwParcel2.writeString(str);
                                hwParcel2.send();
                            }
                        });
                    }
                    break;
                case 59:
                    if (((i2 & 1) != 0 ? 1 : 0) != 0) {
                        hwParcel2.writeStatus(Integer.MIN_VALUE);
                        hwParcel2.send();
                    } else {
                        hwParcel.enforceInterface(ISupplicantStaNetwork.kInterfaceName);
                        getEapEngine(new getEapEngineCallback() {
                            @Override
                            public void onValues(SupplicantStatus supplicantStatus, boolean z) {
                                hwParcel2.writeStatus(0);
                                supplicantStatus.writeToParcel(hwParcel2);
                                hwParcel2.writeBool(z);
                                hwParcel2.send();
                            }
                        });
                    }
                    break;
                case 60:
                    if (((i2 & 1) != 0 ? 1 : 0) != 0) {
                        hwParcel2.writeStatus(Integer.MIN_VALUE);
                        hwParcel2.send();
                    } else {
                        hwParcel.enforceInterface(ISupplicantStaNetwork.kInterfaceName);
                        getEapEngineID(new getEapEngineIDCallback() {
                            @Override
                            public void onValues(SupplicantStatus supplicantStatus, String str) {
                                hwParcel2.writeStatus(0);
                                supplicantStatus.writeToParcel(hwParcel2);
                                hwParcel2.writeString(str);
                                hwParcel2.send();
                            }
                        });
                    }
                    break;
                case 61:
                    if (((i2 & 1) != 0 ? 1 : 0) != 0) {
                        hwParcel2.writeStatus(Integer.MIN_VALUE);
                        hwParcel2.send();
                    } else {
                        hwParcel.enforceInterface(ISupplicantStaNetwork.kInterfaceName);
                        getEapDomainSuffixMatch(new getEapDomainSuffixMatchCallback() {
                            @Override
                            public void onValues(SupplicantStatus supplicantStatus, String str) {
                                hwParcel2.writeStatus(0);
                                supplicantStatus.writeToParcel(hwParcel2);
                                hwParcel2.writeString(str);
                                hwParcel2.send();
                            }
                        });
                    }
                    break;
                case 62:
                    if (((i2 & 1) != 0 ? 1 : 0) != 0) {
                        hwParcel2.writeStatus(Integer.MIN_VALUE);
                        hwParcel2.send();
                    } else {
                        hwParcel.enforceInterface(ISupplicantStaNetwork.kInterfaceName);
                        getIdStr(new getIdStrCallback() {
                            @Override
                            public void onValues(SupplicantStatus supplicantStatus, String str) {
                                hwParcel2.writeStatus(0);
                                supplicantStatus.writeToParcel(hwParcel2);
                                hwParcel2.writeString(str);
                                hwParcel2.send();
                            }
                        });
                    }
                    break;
                case 63:
                    if (((i2 & 1) != 0 ? 1 : 0) != 0) {
                        hwParcel2.writeStatus(Integer.MIN_VALUE);
                        hwParcel2.send();
                    } else {
                        hwParcel.enforceInterface(ISupplicantStaNetwork.kInterfaceName);
                        getWpsNfcConfigurationToken(new getWpsNfcConfigurationTokenCallback() {
                            @Override
                            public void onValues(SupplicantStatus supplicantStatus, ArrayList<Byte> arrayList) {
                                hwParcel2.writeStatus(0);
                                supplicantStatus.writeToParcel(hwParcel2);
                                hwParcel2.writeInt8Vector(arrayList);
                                hwParcel2.send();
                            }
                        });
                    }
                    break;
                case 64:
                    if ((i2 & 1) != 0) {
                        hwParcel2.writeStatus(Integer.MIN_VALUE);
                        hwParcel2.send();
                    } else {
                        hwParcel.enforceInterface(ISupplicantStaNetwork.kInterfaceName);
                        SupplicantStatus supplicantStatusEnable = enable(hwParcel.readBool());
                        hwParcel2.writeStatus(0);
                        supplicantStatusEnable.writeToParcel(hwParcel2);
                        hwParcel2.send();
                    }
                    break;
                case 65:
                    if ((i2 & 1) != 0) {
                        hwParcel2.writeStatus(Integer.MIN_VALUE);
                        hwParcel2.send();
                    } else {
                        hwParcel.enforceInterface(ISupplicantStaNetwork.kInterfaceName);
                        SupplicantStatus supplicantStatusDisable = disable();
                        hwParcel2.writeStatus(0);
                        supplicantStatusDisable.writeToParcel(hwParcel2);
                        hwParcel2.send();
                    }
                    break;
                case ISupplicantStaIfaceCallback.ReasonCode.MESH_CHANNEL_SWITCH_UNSPECIFIED:
                    if ((i2 & 1) != 0) {
                        hwParcel2.writeStatus(Integer.MIN_VALUE);
                        hwParcel2.send();
                    } else {
                        hwParcel.enforceInterface(ISupplicantStaNetwork.kInterfaceName);
                        SupplicantStatus supplicantStatusSelect = select();
                        hwParcel2.writeStatus(0);
                        supplicantStatusSelect.writeToParcel(hwParcel2);
                        hwParcel2.send();
                    }
                    break;
                case ISupplicantStaIfaceCallback.StatusCode.REQ_REFUSED_SSPN:
                    if ((i2 & 1) != 0) {
                        hwParcel2.writeStatus(Integer.MIN_VALUE);
                        hwParcel2.send();
                    } else {
                        hwParcel.enforceInterface(ISupplicantStaNetwork.kInterfaceName);
                        SupplicantStatus supplicantStatusSendNetworkEapSimGsmAuthResponse = sendNetworkEapSimGsmAuthResponse(NetworkResponseEapSimGsmAuthParams.readVectorFromParcel(hwParcel));
                        hwParcel2.writeStatus(0);
                        supplicantStatusSendNetworkEapSimGsmAuthResponse.writeToParcel(hwParcel2);
                        hwParcel2.send();
                    }
                    break;
                case 68:
                    if ((i2 & 1) != 0) {
                        hwParcel2.writeStatus(Integer.MIN_VALUE);
                        hwParcel2.send();
                    } else {
                        hwParcel.enforceInterface(ISupplicantStaNetwork.kInterfaceName);
                        SupplicantStatus supplicantStatusSendNetworkEapSimGsmAuthFailure = sendNetworkEapSimGsmAuthFailure();
                        hwParcel2.writeStatus(0);
                        supplicantStatusSendNetworkEapSimGsmAuthFailure.writeToParcel(hwParcel2);
                        hwParcel2.send();
                    }
                    break;
                case 69:
                    if ((i2 & 1) != 0) {
                        hwParcel2.writeStatus(Integer.MIN_VALUE);
                        hwParcel2.send();
                    } else {
                        hwParcel.enforceInterface(ISupplicantStaNetwork.kInterfaceName);
                        NetworkResponseEapSimUmtsAuthParams networkResponseEapSimUmtsAuthParams = new NetworkResponseEapSimUmtsAuthParams();
                        networkResponseEapSimUmtsAuthParams.readFromParcel(hwParcel);
                        SupplicantStatus supplicantStatusSendNetworkEapSimUmtsAuthResponse = sendNetworkEapSimUmtsAuthResponse(networkResponseEapSimUmtsAuthParams);
                        hwParcel2.writeStatus(0);
                        supplicantStatusSendNetworkEapSimUmtsAuthResponse.writeToParcel(hwParcel2);
                        hwParcel2.send();
                    }
                    break;
                case 70:
                    if ((i2 & 1) != 0) {
                        hwParcel2.writeStatus(Integer.MIN_VALUE);
                        hwParcel2.send();
                    } else {
                        hwParcel.enforceInterface(ISupplicantStaNetwork.kInterfaceName);
                        byte[] bArr3 = new byte[14];
                        hwParcel.readBuffer(14L).copyToInt8Array(0L, bArr3, 14);
                        SupplicantStatus supplicantStatusSendNetworkEapSimUmtsAutsResponse = sendNetworkEapSimUmtsAutsResponse(bArr3);
                        hwParcel2.writeStatus(0);
                        supplicantStatusSendNetworkEapSimUmtsAutsResponse.writeToParcel(hwParcel2);
                        hwParcel2.send();
                    }
                    break;
                case 71:
                    if ((i2 & 1) != 0) {
                        hwParcel2.writeStatus(Integer.MIN_VALUE);
                        hwParcel2.send();
                    } else {
                        hwParcel.enforceInterface(ISupplicantStaNetwork.kInterfaceName);
                        SupplicantStatus supplicantStatusSendNetworkEapSimUmtsAuthFailure = sendNetworkEapSimUmtsAuthFailure();
                        hwParcel2.writeStatus(0);
                        supplicantStatusSendNetworkEapSimUmtsAuthFailure.writeToParcel(hwParcel2);
                        hwParcel2.send();
                    }
                    break;
                case ISupplicantStaIfaceCallback.StatusCode.INVALID_RSNIE:
                    if ((i2 & 1) != 0) {
                        hwParcel2.writeStatus(Integer.MIN_VALUE);
                        hwParcel2.send();
                    } else {
                        hwParcel.enforceInterface(ISupplicantStaNetwork.kInterfaceName);
                        SupplicantStatus supplicantStatusSendNetworkEapIdentityResponse = sendNetworkEapIdentityResponse(hwParcel.readInt8Vector());
                        hwParcel2.writeStatus(0);
                        supplicantStatusSendNetworkEapIdentityResponse.writeToParcel(hwParcel2);
                        hwParcel2.send();
                    }
                    break;
                default:
                    switch (i) {
                        case 256067662:
                            if ((i2 & 1) != 0) {
                                hwParcel2.writeStatus(Integer.MIN_VALUE);
                                hwParcel2.send();
                            } else {
                                hwParcel.enforceInterface(IBase.kInterfaceName);
                                ArrayList<String> arrayListInterfaceChain = interfaceChain();
                                hwParcel2.writeStatus(0);
                                hwParcel2.writeStringVector(arrayListInterfaceChain);
                                hwParcel2.send();
                            }
                            break;
                        case 256131655:
                            if ((i2 & 1) != 0) {
                                hwParcel2.writeStatus(Integer.MIN_VALUE);
                                hwParcel2.send();
                            } else {
                                hwParcel.enforceInterface(IBase.kInterfaceName);
                                hwParcel2.writeStatus(0);
                                hwParcel2.send();
                            }
                            break;
                        case 256136003:
                            if ((i2 & 1) != 0) {
                                hwParcel2.writeStatus(Integer.MIN_VALUE);
                                hwParcel2.send();
                            } else {
                                hwParcel.enforceInterface(IBase.kInterfaceName);
                                String strInterfaceDescriptor = interfaceDescriptor();
                                hwParcel2.writeStatus(0);
                                hwParcel2.writeString(strInterfaceDescriptor);
                                hwParcel2.send();
                            }
                            break;
                        case 256398152:
                            if ((i2 & 1) != 0) {
                                hwParcel2.writeStatus(Integer.MIN_VALUE);
                                hwParcel2.send();
                            } else {
                                hwParcel.enforceInterface(IBase.kInterfaceName);
                                ArrayList<byte[]> hashChain = getHashChain();
                                hwParcel2.writeStatus(0);
                                HwBlob hwBlob = new HwBlob(16);
                                int size = hashChain.size();
                                hwBlob.putInt32(8L, size);
                                hwBlob.putBool(12L, false);
                                HwBlob hwBlob2 = new HwBlob(size * 32);
                                while (i < size) {
                                    hwBlob2.putInt8Array(i * 32, hashChain.get(i));
                                    i++;
                                }
                                hwBlob.putBlob(0L, hwBlob2);
                                hwParcel2.writeBuffer(hwBlob);
                                hwParcel2.send();
                            }
                            break;
                        case 256462420:
                            if (((i2 & 1) != 0 ? 1 : 0) != 1) {
                                hwParcel2.writeStatus(Integer.MIN_VALUE);
                                hwParcel2.send();
                            } else {
                                hwParcel.enforceInterface(IBase.kInterfaceName);
                                setHALInstrumentation();
                            }
                            break;
                        case 256660548:
                            if (((i2 & 1) != 0 ? 1 : 0) != 0) {
                                hwParcel2.writeStatus(Integer.MIN_VALUE);
                                hwParcel2.send();
                            }
                            break;
                        case 256921159:
                            if ((i2 & 1) != 0) {
                                hwParcel2.writeStatus(Integer.MIN_VALUE);
                                hwParcel2.send();
                            } else {
                                hwParcel.enforceInterface(IBase.kInterfaceName);
                                ping();
                                hwParcel2.writeStatus(0);
                                hwParcel2.send();
                            }
                            break;
                        case 257049926:
                            if ((i2 & 1) != 0) {
                                hwParcel2.writeStatus(Integer.MIN_VALUE);
                                hwParcel2.send();
                            } else {
                                hwParcel.enforceInterface(IBase.kInterfaceName);
                                DebugInfo debugInfo = getDebugInfo();
                                hwParcel2.writeStatus(0);
                                debugInfo.writeToParcel(hwParcel2);
                                hwParcel2.send();
                            }
                            break;
                        case 257120595:
                            if (((i2 & 1) != 0 ? 1 : 0) != 1) {
                                hwParcel2.writeStatus(Integer.MIN_VALUE);
                                hwParcel2.send();
                            } else {
                                hwParcel.enforceInterface(IBase.kInterfaceName);
                                notifySyspropsChanged();
                            }
                            break;
                        case 257250372:
                            if (((i2 & 1) != 0 ? 1 : 0) != 0) {
                                hwParcel2.writeStatus(Integer.MIN_VALUE);
                                hwParcel2.send();
                            }
                            break;
                    }
                    break;
            }
        }
    }
}
