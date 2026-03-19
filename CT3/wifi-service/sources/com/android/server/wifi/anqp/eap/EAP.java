package com.android.server.wifi.anqp.eap;

import java.util.HashMap;
import java.util.Map;

public abstract class EAP {
    public static final int CredentialType = 5;
    public static final int EAP_3Com = 24;
    public static final int EAP_AKA = 23;
    public static final int EAP_AKAPrim = 50;
    public static final int EAP_ActiontecWireless = 35;
    public static final int EAP_EKE = 53;
    public static final int EAP_FAST = 43;
    public static final int EAP_GPSK = 51;
    public static final int EAP_HTTPDigest = 38;
    public static final int EAP_IKEv2 = 49;
    public static final int EAP_KEA = 11;
    public static final int EAP_KEA_VALIDATE = 12;
    public static final int EAP_LEAP = 17;
    public static final int EAP_Link = 45;
    public static final int EAP_MD5 = 4;
    public static final int EAP_MOBAC = 42;
    public static final int EAP_MSCHAPv2 = 26;
    public static final int EAP_OTP = 5;
    public static final int EAP_PAX = 46;
    public static final int EAP_PEAP = 29;
    public static final int EAP_POTP = 32;
    public static final int EAP_PSK = 47;
    public static final int EAP_PWD = 52;
    public static final int EAP_RSA = 9;
    public static final int EAP_SAKE = 48;
    public static final int EAP_SIM = 18;
    public static final int EAP_SPEKE = 41;
    public static final int EAP_TEAP = 55;
    public static final int EAP_TLS = 13;
    public static final int EAP_TTLS = 21;
    public static final int EAP_ZLXEAP = 44;
    public static final int ExpandedEAPMethod = 1;
    public static final int ExpandedInnerEAPMethod = 4;
    public static final int InnerAuthEAPMethodType = 3;
    public static final int NonEAPInnerAuthType = 2;
    public static final int TunneledEAPMethodCredType = 6;
    public static final int VendorSpecific = 221;
    private static final Map<Integer, EAPMethodID> sEapIds = new HashMap();
    private static final Map<EAPMethodID, Integer> sRevEapIds = new HashMap();
    private static final Map<Integer, AuthInfoID> sAuthIds = new HashMap();

    static {
        sEapIds.put(4, EAPMethodID.EAP_MD5);
        sEapIds.put(5, EAPMethodID.EAP_OTP);
        sEapIds.put(9, EAPMethodID.EAP_RSA);
        sEapIds.put(11, EAPMethodID.EAP_KEA);
        sEapIds.put(12, EAPMethodID.EAP_KEA_VALIDATE);
        sEapIds.put(13, EAPMethodID.EAP_TLS);
        sEapIds.put(17, EAPMethodID.EAP_LEAP);
        sEapIds.put(18, EAPMethodID.EAP_SIM);
        sEapIds.put(21, EAPMethodID.EAP_TTLS);
        sEapIds.put(23, EAPMethodID.EAP_AKA);
        sEapIds.put(24, EAPMethodID.EAP_3Com);
        sEapIds.put(26, EAPMethodID.EAP_MSCHAPv2);
        sEapIds.put(29, EAPMethodID.EAP_PEAP);
        sEapIds.put(32, EAPMethodID.EAP_POTP);
        sEapIds.put(35, EAPMethodID.EAP_ActiontecWireless);
        sEapIds.put(38, EAPMethodID.EAP_HTTPDigest);
        sEapIds.put(41, EAPMethodID.EAP_SPEKE);
        sEapIds.put(42, EAPMethodID.EAP_MOBAC);
        sEapIds.put(43, EAPMethodID.EAP_FAST);
        sEapIds.put(44, EAPMethodID.EAP_ZLXEAP);
        sEapIds.put(45, EAPMethodID.EAP_Link);
        sEapIds.put(46, EAPMethodID.EAP_PAX);
        sEapIds.put(47, EAPMethodID.EAP_PSK);
        sEapIds.put(48, EAPMethodID.EAP_SAKE);
        sEapIds.put(49, EAPMethodID.EAP_IKEv2);
        sEapIds.put(50, EAPMethodID.EAP_AKAPrim);
        sEapIds.put(51, EAPMethodID.EAP_GPSK);
        sEapIds.put(52, EAPMethodID.EAP_PWD);
        sEapIds.put(53, EAPMethodID.EAP_EKE);
        sEapIds.put(55, EAPMethodID.EAP_TEAP);
        for (Map.Entry<Integer, EAPMethodID> entry : sEapIds.entrySet()) {
            sRevEapIds.put(entry.getValue(), entry.getKey());
        }
        sAuthIds.put(1, AuthInfoID.ExpandedEAPMethod);
        sAuthIds.put(2, AuthInfoID.NonEAPInnerAuthType);
        sAuthIds.put(3, AuthInfoID.InnerAuthEAPMethodType);
        sAuthIds.put(4, AuthInfoID.ExpandedInnerEAPMethod);
        sAuthIds.put(5, AuthInfoID.CredentialType);
        sAuthIds.put(6, AuthInfoID.TunneledEAPMethodCredType);
        sAuthIds.put(Integer.valueOf(VendorSpecific), AuthInfoID.VendorSpecific);
    }

    public enum EAPMethodID {
        EAP_MD5,
        EAP_OTP,
        EAP_RSA,
        EAP_KEA,
        EAP_KEA_VALIDATE,
        EAP_TLS,
        EAP_LEAP,
        EAP_SIM,
        EAP_TTLS,
        EAP_AKA,
        EAP_3Com,
        EAP_MSCHAPv2,
        EAP_PEAP,
        EAP_POTP,
        EAP_ActiontecWireless,
        EAP_HTTPDigest,
        EAP_SPEKE,
        EAP_MOBAC,
        EAP_FAST,
        EAP_ZLXEAP,
        EAP_Link,
        EAP_PAX,
        EAP_PSK,
        EAP_SAKE,
        EAP_IKEv2,
        EAP_AKAPrim,
        EAP_GPSK,
        EAP_PWD,
        EAP_EKE,
        EAP_TEAP;

        public static EAPMethodID[] valuesCustom() {
            return values();
        }
    }

    public enum AuthInfoID {
        Undefined,
        ExpandedEAPMethod,
        NonEAPInnerAuthType,
        InnerAuthEAPMethodType,
        ExpandedInnerEAPMethod,
        CredentialType,
        TunneledEAPMethodCredType,
        VendorSpecific;

        public static AuthInfoID[] valuesCustom() {
            return values();
        }
    }

    public static EAPMethodID mapEAPMethod(int methodID) {
        return sEapIds.get(Integer.valueOf(methodID));
    }

    public static Integer mapEAPMethod(EAPMethodID methodID) {
        return sRevEapIds.get(methodID);
    }

    public static AuthInfoID mapAuthMethod(int methodID) {
        return sAuthIds.get(Integer.valueOf(methodID));
    }
}
