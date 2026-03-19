package com.android.server.wifi.hotspot2.pps;

import android.net.wifi.WifiEnterpriseConfig;
import android.security.KeyStore;
import android.text.TextUtils;
import android.util.Base64;
import android.util.Log;
import com.android.server.wifi.IMSIParameter;
import com.android.server.wifi.anqp.eap.EAP;
import com.android.server.wifi.anqp.eap.EAPMethod;
import com.android.server.wifi.anqp.eap.NonEAPInnerAuth;
import com.android.server.wifi.hotspot2.Utils;
import com.android.server.wifi.hotspot2.omadm.OMAException;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.util.Arrays;

public class Credential {
    public static final String CertTypeIEEE = "802.1ar";
    public static final String CertTypeX509 = "x509v3";
    private final CertType mCertType;
    private final boolean mCheckAAACert;
    private final long mCtime;
    private final boolean mDisregardPassword;
    private final EAPMethod mEAPMethod;
    private final long mExpTime;
    private final byte[] mFingerPrint;
    private final IMSIParameter mImsi;
    private final boolean mMachineManaged;
    private final String mPassword;
    private final String mRealm;
    private final String mSTokenApp;
    private final boolean mShare;
    private final String mUserName;

    public enum CertType {
        IEEE,
        x509v3;

        public static CertType[] valuesCustom() {
            return values();
        }
    }

    public Credential(long ctime, long expTime, String realm, boolean checkAAACert, EAPMethod eapMethod, String userName, String password, boolean machineManaged, String stApp, boolean share) {
        this.mCtime = ctime;
        this.mExpTime = expTime;
        this.mRealm = realm;
        this.mCheckAAACert = checkAAACert;
        this.mEAPMethod = eapMethod;
        this.mUserName = userName;
        if (!TextUtils.isEmpty(password)) {
            byte[] pwOctets = Base64.decode(password, 0);
            this.mPassword = new String(pwOctets, StandardCharsets.UTF_8);
        } else {
            this.mPassword = null;
        }
        this.mDisregardPassword = false;
        this.mMachineManaged = machineManaged;
        this.mSTokenApp = stApp;
        this.mShare = share;
        this.mCertType = null;
        this.mFingerPrint = null;
        this.mImsi = null;
    }

    public Credential(long ctime, long expTime, String realm, boolean checkAAACert, EAPMethod eapMethod, CertType certType, byte[] fingerPrint) {
        this.mCtime = ctime;
        this.mExpTime = expTime;
        this.mRealm = realm;
        this.mCheckAAACert = checkAAACert;
        this.mEAPMethod = eapMethod;
        this.mCertType = certType;
        this.mFingerPrint = fingerPrint;
        this.mUserName = null;
        this.mPassword = null;
        this.mDisregardPassword = false;
        this.mMachineManaged = false;
        this.mSTokenApp = null;
        this.mShare = false;
        this.mImsi = null;
    }

    public Credential(long ctime, long expTime, String realm, boolean checkAAACert, EAPMethod eapMethod, IMSIParameter imsi) {
        this.mCtime = ctime;
        this.mExpTime = expTime;
        this.mRealm = realm;
        this.mCheckAAACert = checkAAACert;
        this.mEAPMethod = eapMethod;
        this.mImsi = imsi;
        this.mCertType = null;
        this.mFingerPrint = null;
        this.mUserName = null;
        this.mPassword = null;
        this.mDisregardPassword = false;
        this.mMachineManaged = false;
        this.mSTokenApp = null;
        this.mShare = false;
    }

    public Credential(Credential other, String password) {
        this.mCtime = other.mCtime;
        this.mExpTime = other.mExpTime;
        this.mRealm = other.mRealm;
        this.mCheckAAACert = other.mCheckAAACert;
        this.mUserName = other.mUserName;
        this.mPassword = password;
        this.mDisregardPassword = other.mDisregardPassword;
        this.mMachineManaged = other.mMachineManaged;
        this.mSTokenApp = other.mSTokenApp;
        this.mShare = other.mShare;
        this.mEAPMethod = other.mEAPMethod;
        this.mCertType = other.mCertType;
        this.mFingerPrint = other.mFingerPrint;
        this.mImsi = other.mImsi;
    }

    public Credential(WifiEnterpriseConfig enterpriseConfig, KeyStore keyStore, boolean update) throws IOException {
        byte[] fingerPrint;
        this.mCtime = -1L;
        this.mExpTime = -1L;
        this.mRealm = enterpriseConfig.getRealm();
        this.mCheckAAACert = false;
        this.mEAPMethod = mapEapMethod(enterpriseConfig.getEapMethod(), enterpriseConfig.getPhase2Method());
        this.mCertType = this.mEAPMethod.getEAPMethodID() == EAP.EAPMethodID.EAP_TLS ? CertType.x509v3 : null;
        if (enterpriseConfig.getClientCertificate() != null) {
            try {
                MessageDigest digester = MessageDigest.getInstance("SHA-256");
                fingerPrint = digester.digest(enterpriseConfig.getClientCertificate().getEncoded());
            } catch (GeneralSecurityException gse) {
                Log.e(Utils.hs2LogTag(getClass()), "Failed to generate certificate fingerprint: " + gse);
                fingerPrint = null;
            }
        } else if (enterpriseConfig.getClientCertificateAlias() != null) {
            String alias = enterpriseConfig.getClientCertificateAlias();
            byte[] octets = keyStore.get("USRCERT_" + alias);
            if (octets != null) {
                try {
                    MessageDigest digester2 = MessageDigest.getInstance("SHA-256");
                    fingerPrint = digester2.digest(octets);
                } catch (GeneralSecurityException gse2) {
                    Log.e(Utils.hs2LogTag(getClass()), "Failed to construct digest: " + gse2);
                    fingerPrint = null;
                }
            } else {
                try {
                    fingerPrint = Base64.decode(enterpriseConfig.getClientCertificateAlias(), 0);
                } catch (IllegalArgumentException e) {
                    Log.e(Utils.hs2LogTag(getClass()), "Bad base 64 alias");
                    fingerPrint = null;
                }
            }
        } else {
            fingerPrint = null;
        }
        this.mFingerPrint = fingerPrint;
        String imsi = enterpriseConfig.getPlmn();
        this.mImsi = (imsi == null || imsi.length() == 0) ? null : new IMSIParameter(imsi);
        this.mUserName = enterpriseConfig.getIdentity();
        this.mPassword = enterpriseConfig.getPassword();
        this.mDisregardPassword = update && this.mPassword.length() < 2;
        this.mMachineManaged = false;
        this.mSTokenApp = null;
        this.mShare = false;
    }

    public static CertType mapCertType(String certType) throws OMAException {
        if (certType.equalsIgnoreCase(CertTypeX509)) {
            return CertType.x509v3;
        }
        if (certType.equalsIgnoreCase(CertTypeIEEE)) {
            return CertType.IEEE;
        }
        throw new OMAException("Invalid cert type: '" + certType + "'");
    }

    private static EAPMethod mapEapMethod(int eapMethod, int phase2Method) throws IOException {
        NonEAPInnerAuth inner;
        String methodName;
        switch (eapMethod) {
            case 1:
                return new EAPMethod(EAP.EAPMethodID.EAP_TLS, null);
            case 2:
                switch (phase2Method) {
                    case 1:
                        inner = new NonEAPInnerAuth(NonEAPInnerAuth.NonEAPType.PAP);
                        break;
                    case 2:
                        inner = new NonEAPInnerAuth(NonEAPInnerAuth.NonEAPType.MSCHAP);
                        break;
                    case 3:
                        inner = new NonEAPInnerAuth(NonEAPInnerAuth.NonEAPType.MSCHAPv2);
                        break;
                    default:
                        throw new IOException("TTLS phase2 method " + phase2Method + " not valid for Passpoint");
                }
                return new EAPMethod(EAP.EAPMethodID.EAP_TTLS, inner);
            case 3:
            default:
                if (eapMethod >= 0 && eapMethod < WifiEnterpriseConfig.Eap.strings.length) {
                    methodName = WifiEnterpriseConfig.Eap.strings[eapMethod];
                } else {
                    methodName = Integer.toString(eapMethod);
                }
                throw new IOException("EAP method id " + methodName + " is not valid for Passpoint");
            case 4:
                return new EAPMethod(EAP.EAPMethodID.EAP_SIM, null);
            case 5:
                return new EAPMethod(EAP.EAPMethodID.EAP_AKA, null);
            case 6:
                return new EAPMethod(EAP.EAPMethodID.EAP_AKAPrim, null);
        }
    }

    public EAPMethod getEAPMethod() {
        return this.mEAPMethod;
    }

    public String getRealm() {
        return this.mRealm;
    }

    public IMSIParameter getImsi() {
        return this.mImsi;
    }

    public String getUserName() {
        return this.mUserName;
    }

    public String getPassword() {
        return this.mPassword;
    }

    public boolean hasDisregardPassword() {
        return this.mDisregardPassword;
    }

    public CertType getCertType() {
        return this.mCertType;
    }

    public byte[] getFingerPrint() {
        return this.mFingerPrint;
    }

    public long getCtime() {
        return this.mCtime;
    }

    public long getExpTime() {
        return this.mExpTime;
    }

    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        Credential that = (Credential) o;
        if (this.mCheckAAACert == that.mCheckAAACert && this.mCtime == that.mCtime && this.mExpTime == that.mExpTime && this.mMachineManaged == that.mMachineManaged && this.mShare == that.mShare && this.mCertType == that.mCertType && this.mEAPMethod.equals(that.mEAPMethod) && Arrays.equals(this.mFingerPrint, that.mFingerPrint) && safeEquals(this.mImsi, that.mImsi)) {
            return (this.mDisregardPassword || safeEquals(this.mPassword, that.mPassword)) && this.mRealm.equals(that.mRealm) && safeEquals(this.mSTokenApp, that.mSTokenApp) && safeEquals(this.mUserName, that.mUserName);
        }
        return false;
    }

    private static boolean safeEquals(Object s1, Object s2) {
        if (s1 == null) {
            return s2 == null;
        }
        if (s2 != null) {
            return s1.equals(s2);
        }
        return false;
    }

    public int hashCode() {
        int result = (int) (this.mCtime ^ (this.mCtime >>> 32));
        return (((((((((((((((((((((((result * 31) + ((int) (this.mExpTime ^ (this.mExpTime >>> 32)))) * 31) + this.mRealm.hashCode()) * 31) + (this.mCheckAAACert ? 1 : 0)) * 31) + (this.mUserName != null ? this.mUserName.hashCode() : 0)) * 31) + (this.mPassword != null ? this.mPassword.hashCode() : 0)) * 31) + (this.mMachineManaged ? 1 : 0)) * 31) + (this.mSTokenApp != null ? this.mSTokenApp.hashCode() : 0)) * 31) + (this.mShare ? 1 : 0)) * 31) + this.mEAPMethod.hashCode()) * 31) + (this.mCertType != null ? this.mCertType.hashCode() : 0)) * 31) + (this.mFingerPrint != null ? Arrays.hashCode(this.mFingerPrint) : 0)) * 31) + (this.mImsi != null ? this.mImsi.hashCode() : 0);
    }

    public String toString() {
        return "Credential{mCtime=" + Utils.toUTCString(this.mCtime) + ", mExpTime=" + Utils.toUTCString(this.mExpTime) + ", mRealm='" + this.mRealm + "', mCheckAAACert=" + this.mCheckAAACert + ", mUserName='" + this.mUserName + "', mPassword='" + this.mPassword + "', mDisregardPassword=" + this.mDisregardPassword + ", mMachineManaged=" + this.mMachineManaged + ", mSTokenApp='" + this.mSTokenApp + "', mShare=" + this.mShare + ", mEAPMethod=" + this.mEAPMethod + ", mCertType=" + this.mCertType + ", mFingerPrint=" + Utils.toHexString(this.mFingerPrint) + ", mImsi='" + this.mImsi + "'}";
    }
}
