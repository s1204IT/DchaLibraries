package com.android.server.wifi;

import android.R;
import android.content.Context;
import android.hardware.wifi.supplicant.V1_0.ISupplicantNetwork;
import android.hardware.wifi.supplicant.V1_0.ISupplicantStaNetwork;
import android.hardware.wifi.supplicant.V1_0.ISupplicantStaNetworkCallback;
import android.hardware.wifi.supplicant.V1_0.SupplicantStatus;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiEnterpriseConfig;
import android.os.HidlSupport;
import android.os.IHwInterface;
import android.os.RemoteException;
import android.text.TextUtils;
import android.util.Log;
import android.util.MutableBoolean;
import android.util.MutableInt;
import com.android.internal.annotations.VisibleForTesting;
import com.android.internal.util.ArrayUtils;
import com.android.server.wifi.WifiBackupRestore;
import com.android.server.wifi.util.NativeUtil;
import com.mediatek.server.wifi.MtkWapi;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.BitSet;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.annotation.concurrent.ThreadSafe;
import org.json.JSONException;
import org.json.JSONObject;

@ThreadSafe
public class SupplicantStaNetworkHal {

    @VisibleForTesting
    public static final String ID_STRING_KEY_CONFIG_KEY = "configKey";

    @VisibleForTesting
    public static final String ID_STRING_KEY_CREATOR_UID = "creatorUid";

    @VisibleForTesting
    public static final String ID_STRING_KEY_FQDN = "fqdn";
    private static final String TAG = "SupplicantStaNetworkHal";
    private int mAuthAlgMask;
    private byte[] mBssid;
    private String mEapAltSubjectMatch;
    private ArrayList<Byte> mEapAnonymousIdentity;
    private String mEapCACert;
    private String mEapCAPath;
    private String mEapClientCert;
    private String mEapDomainSuffixMatch;
    private boolean mEapEngine;
    private String mEapEngineID;
    private ArrayList<Byte> mEapIdentity;
    private int mEapMethod;
    private ArrayList<Byte> mEapPassword;
    private int mEapPhase2Method;
    private String mEapPrivateKeyId;
    private String mEapSubjectMatch;
    private int mGroupCipherMask;
    private ISupplicantStaNetwork mISupplicantStaNetwork;
    private ISupplicantStaNetworkCallback mISupplicantStaNetworkCallback;
    private String mIdStr;
    private final String mIfaceName;
    private int mKeyMgmtMask;
    private int mNetworkId;
    private int mPairwiseCipherMask;
    private int mProtoMask;
    private byte[] mPsk;
    private String mPskPassphrase;
    private boolean mRequirePmf;
    private boolean mScanSsid;
    private ArrayList<Byte> mSsid;
    private boolean mSystemSupportsFastBssTransition;
    private ArrayList<Byte> mWepKey;
    private int mWepTxKeyIdx;
    private final WifiMonitor mWifiMonitor;
    private static final Pattern GSM_AUTH_RESPONSE_PARAMS_PATTERN = Pattern.compile(":([0-9a-fA-F]+):([0-9a-fA-F]+)");
    private static final Pattern UMTS_AUTH_RESPONSE_PARAMS_PATTERN = Pattern.compile("^:([0-9a-fA-F]+):([0-9a-fA-F]+):([0-9a-fA-F]+)$");
    private static final Pattern UMTS_AUTS_RESPONSE_PARAMS_PATTERN = Pattern.compile("^:([0-9a-fA-F]+)$");
    private final Object mLock = new Object();
    private boolean mVerboseLoggingEnabled = false;

    SupplicantStaNetworkHal(ISupplicantStaNetwork iSupplicantStaNetwork, String str, Context context, WifiMonitor wifiMonitor) {
        this.mSystemSupportsFastBssTransition = false;
        this.mISupplicantStaNetwork = iSupplicantStaNetwork;
        this.mIfaceName = str;
        this.mWifiMonitor = wifiMonitor;
        this.mSystemSupportsFastBssTransition = context.getResources().getBoolean(R.^attr-private.preserveIconSpacing);
    }

    void enableVerboseLogging(boolean z) {
        synchronized (this.mLock) {
            this.mVerboseLoggingEnabled = z;
        }
    }

    public boolean loadWifiConfiguration(WifiConfiguration wifiConfiguration, Map<String, String> map) {
        synchronized (this.mLock) {
            try {
                if (wifiConfiguration == null) {
                    return false;
                }
                wifiConfiguration.SSID = null;
                if (getSsid() && !ArrayUtils.isEmpty(this.mSsid)) {
                    wifiConfiguration.SSID = NativeUtil.encodeSsid(this.mSsid);
                    wifiConfiguration.networkId = -1;
                    if (getId()) {
                        wifiConfiguration.networkId = this.mNetworkId;
                        wifiConfiguration.getNetworkSelectionStatus().setNetworkSelectionBSSID((String) null);
                        if (getBssid() && !ArrayUtils.isEmpty(this.mBssid)) {
                            wifiConfiguration.getNetworkSelectionStatus().setNetworkSelectionBSSID(NativeUtil.macAddressFromByteArray(this.mBssid));
                        }
                        wifiConfiguration.hiddenSSID = false;
                        if (getScanSsid()) {
                            wifiConfiguration.hiddenSSID = this.mScanSsid;
                        }
                        wifiConfiguration.requirePMF = false;
                        if (getRequirePmf()) {
                            wifiConfiguration.requirePMF = this.mRequirePmf;
                        }
                        wifiConfiguration.wepTxKeyIndex = -1;
                        if (getWepTxKeyIdx()) {
                            wifiConfiguration.wepTxKeyIndex = this.mWepTxKeyIdx;
                        }
                        for (int i = 0; i < 4; i++) {
                            wifiConfiguration.wepKeys[i] = null;
                            if (getWepKey(i) && !ArrayUtils.isEmpty(this.mWepKey)) {
                                wifiConfiguration.wepKeys[i] = NativeUtil.bytesToHexOrQuotedString(this.mWepKey);
                            }
                        }
                        wifiConfiguration.preSharedKey = null;
                        if (getPskPassphrase() && !TextUtils.isEmpty(this.mPskPassphrase)) {
                            wifiConfiguration.preSharedKey = NativeUtil.addEnclosingQuotes(this.mPskPassphrase);
                        } else if (getPsk() && !ArrayUtils.isEmpty(this.mPsk)) {
                            wifiConfiguration.preSharedKey = NativeUtil.hexStringFromByteArray(this.mPsk);
                        }
                        if (getKeyMgmt()) {
                            wifiConfiguration.allowedKeyManagement = removeFastTransitionFlags(supplicantToWifiConfigurationKeyMgmtMask(this.mKeyMgmtMask));
                        }
                        if (getProto()) {
                            wifiConfiguration.allowedProtocols = supplicantToWifiConfigurationProtoMask(this.mProtoMask);
                        }
                        if (getAuthAlg()) {
                            wifiConfiguration.allowedAuthAlgorithms = supplicantToWifiConfigurationAuthAlgMask(this.mAuthAlgMask);
                        }
                        if (getGroupCipher()) {
                            wifiConfiguration.allowedGroupCiphers = supplicantToWifiConfigurationGroupCipherMask(this.mGroupCipherMask);
                        }
                        if (getPairwiseCipher()) {
                            wifiConfiguration.allowedPairwiseCiphers = supplicantToWifiConfigurationPairwiseCipherMask(this.mPairwiseCipherMask);
                        }
                        if (getIdStr() && !TextUtils.isEmpty(this.mIdStr)) {
                            map.putAll(parseNetworkExtra(this.mIdStr));
                        } else {
                            Log.w(TAG, "getIdStr failed or empty");
                        }
                        return loadWifiEnterpriseConfig(wifiConfiguration.SSID, wifiConfiguration.enterpriseConfig);
                    }
                    Log.e(TAG, "getId failed");
                    return false;
                }
                Log.e(TAG, "failed to read ssid");
                return false;
            } catch (Throwable th) {
                throw th;
            }
        }
    }

    public boolean saveWifiConfiguration(WifiConfiguration wifiConfiguration) {
        boolean z;
        synchronized (this.mLock) {
            try {
                if (wifiConfiguration == null) {
                    return false;
                }
                if (wifiConfiguration.SSID != null && !setSsid(NativeUtil.decodeSsid(wifiConfiguration.SSID))) {
                    Log.e(TAG, "failed to set SSID: " + wifiConfiguration.SSID);
                    return false;
                }
                String networkSelectionBSSID = wifiConfiguration.getNetworkSelectionStatus().getNetworkSelectionBSSID();
                if (networkSelectionBSSID != null && !setBssid(NativeUtil.macAddressToByteArray(networkSelectionBSSID))) {
                    Log.e(TAG, "failed to set BSSID: " + networkSelectionBSSID);
                    return false;
                }
                if (wifiConfiguration.preSharedKey != null) {
                    if (wifiConfiguration.preSharedKey.startsWith("\"")) {
                        if (!setPskPassphrase(NativeUtil.removeEnclosingQuotes(wifiConfiguration.preSharedKey))) {
                            Log.e(TAG, "failed to set psk passphrase");
                            return false;
                        }
                    } else if (MtkWapi.isWapiPskConfiguration(wifiConfiguration)) {
                        if (!setPskPassphrase(NativeUtil.addEnclosingQuotes(wifiConfiguration.preSharedKey))) {
                            Log.e(TAG, "failed to set wapi psk passphrase");
                            return false;
                        }
                    } else if (!setPsk(NativeUtil.hexStringToByteArray(wifiConfiguration.preSharedKey))) {
                        Log.e(TAG, "failed to set psk");
                        return false;
                    }
                }
                if (MtkWapi.isWapiCertConfiguration(wifiConfiguration) && wifiConfiguration.wapiCertSel == null && !MtkWapi.updateWapiCertSelList(wifiConfiguration)) {
                    Log.e(TAG, "failed to set wapi certificate selection list");
                    return false;
                }
                if (MtkWapi.isWapiCertConfiguration(wifiConfiguration) && !MtkWapi.setWapiCertAlias(this, getSupplicantNetworkId(), wifiConfiguration.wapiCertSel)) {
                    Log.e(TAG, "failed to set alias: " + wifiConfiguration.wapiCertSel);
                    return false;
                }
                if (wifiConfiguration.wepKeys != null) {
                    z = false;
                    for (int i = 0; i < wifiConfiguration.wepKeys.length; i++) {
                        if (wifiConfiguration.wepKeys[i] != null) {
                            if (setWepKey(i, NativeUtil.hexOrQuotedStringToBytes(wifiConfiguration.wepKeys[i]))) {
                                z = true;
                            } else {
                                Log.e(TAG, "failed to set wep_key " + i);
                                return false;
                            }
                        }
                    }
                } else {
                    z = false;
                }
                if (z && !setWepTxKeyIdx(wifiConfiguration.wepTxKeyIndex)) {
                    Log.e(TAG, "failed to set wep_tx_keyidx: " + wifiConfiguration.wepTxKeyIndex);
                    return false;
                }
                if (!setScanSsid(wifiConfiguration.hiddenSSID)) {
                    Log.e(TAG, wifiConfiguration.SSID + ": failed to set hiddenSSID: " + wifiConfiguration.hiddenSSID);
                    return false;
                }
                if (!setRequirePmf(wifiConfiguration.requirePMF)) {
                    Log.e(TAG, wifiConfiguration.SSID + ": failed to set requirePMF: " + wifiConfiguration.requirePMF);
                    return false;
                }
                if (wifiConfiguration.allowedKeyManagement.cardinality() != 0 && !setKeyMgmt(wifiConfigurationToSupplicantKeyMgmtMask(addFastTransitionFlags(wifiConfiguration.allowedKeyManagement)))) {
                    Log.e(TAG, "failed to set Key Management");
                    return false;
                }
                if (wifiConfiguration.allowedProtocols.cardinality() != 0 && !setProto(wifiConfigurationToSupplicantProtoMask(wifiConfiguration.allowedProtocols))) {
                    Log.e(TAG, "failed to set Security Protocol");
                    return false;
                }
                if (wifiConfiguration.allowedAuthAlgorithms.cardinality() != 0 && !setAuthAlg(wifiConfigurationToSupplicantAuthAlgMask(wifiConfiguration.allowedAuthAlgorithms))) {
                    Log.e(TAG, "failed to set AuthAlgorithm");
                    return false;
                }
                if (wifiConfiguration.allowedGroupCiphers.cardinality() != 0 && !setGroupCipher(wifiConfigurationToSupplicantGroupCipherMask(wifiConfiguration.allowedGroupCiphers))) {
                    Log.e(TAG, "failed to set Group Cipher");
                    return false;
                }
                if (wifiConfiguration.allowedPairwiseCiphers.cardinality() != 0 && !setPairwiseCipher(wifiConfigurationToSupplicantPairwiseCipherMask(wifiConfiguration.allowedPairwiseCiphers))) {
                    Log.e(TAG, "failed to set PairwiseCipher");
                    return false;
                }
                HashMap map = new HashMap();
                if (wifiConfiguration.isPasspoint()) {
                    map.put(ID_STRING_KEY_FQDN, wifiConfiguration.FQDN);
                }
                map.put(ID_STRING_KEY_CONFIG_KEY, wifiConfiguration.configKey());
                map.put(ID_STRING_KEY_CREATOR_UID, Integer.toString(wifiConfiguration.creatorUid));
                if (!setIdStr(createNetworkExtra(map))) {
                    Log.e(TAG, "failed to set id string");
                    return false;
                }
                if (wifiConfiguration.updateIdentifier != null && !setUpdateIdentifier(Integer.parseInt(wifiConfiguration.updateIdentifier))) {
                    Log.e(TAG, "failed to set update identifier");
                    return false;
                }
                if (wifiConfiguration.enterpriseConfig != null && wifiConfiguration.enterpriseConfig.getEapMethod() != -1 && !saveWifiEnterpriseConfig(wifiConfiguration.SSID, wifiConfiguration.enterpriseConfig)) {
                    return false;
                }
                this.mISupplicantStaNetworkCallback = new SupplicantStaNetworkHalCallback(wifiConfiguration.networkId, wifiConfiguration.SSID);
                if (registerCallback(this.mISupplicantStaNetworkCallback)) {
                    return true;
                }
                Log.e(TAG, "Failed to register callback");
                return false;
            } catch (Throwable th) {
                throw th;
            }
        }
    }

    private int getSupplicantNetworkId() {
        synchronized (this.mLock) {
            if (!checkISupplicantStaNetworkAndLogFailure("getId")) {
                return -1;
            }
            final MutableInt mutableInt = new MutableInt(-1);
            try {
                final MutableBoolean mutableBoolean = new MutableBoolean(false);
                this.mISupplicantStaNetwork.getId(new ISupplicantNetwork.getIdCallback() {
                    @Override
                    public final void onValues(SupplicantStatus supplicantStatus, int i) {
                        SupplicantStaNetworkHal.lambda$getSupplicantNetworkId$0(this.f$0, mutableBoolean, mutableInt, supplicantStatus, i);
                    }
                });
                return mutableInt.value;
            } catch (RemoteException e) {
                handleRemoteException(e, "getId");
                return -1;
            }
        }
    }

    public static void lambda$getSupplicantNetworkId$0(SupplicantStaNetworkHal supplicantStaNetworkHal, MutableBoolean mutableBoolean, MutableInt mutableInt, SupplicantStatus supplicantStatus, int i) {
        mutableBoolean.value = supplicantStatus.code == 0;
        if (mutableBoolean.value) {
            mutableInt.value = i;
        } else {
            supplicantStaNetworkHal.checkStatusAndLogFailure(supplicantStatus, "getId");
        }
    }

    private boolean loadWifiEnterpriseConfig(String str, WifiEnterpriseConfig wifiEnterpriseConfig) {
        String str2;
        synchronized (this.mLock) {
            try {
                if (wifiEnterpriseConfig == null) {
                    return false;
                }
                if (getEapMethod()) {
                    wifiEnterpriseConfig.setEapMethod(supplicantToWifiConfigurationEapMethod(this.mEapMethod));
                    if (getEapPhase2Method()) {
                        wifiEnterpriseConfig.setPhase2Method(supplicantToWifiConfigurationEapPhase2Method(this.mEapPhase2Method));
                        if (getEapIdentity() && !ArrayUtils.isEmpty(this.mEapIdentity)) {
                            wifiEnterpriseConfig.setFieldValue("identity", NativeUtil.stringFromByteArrayList(this.mEapIdentity));
                        }
                        if (getEapAnonymousIdentity() && !ArrayUtils.isEmpty(this.mEapAnonymousIdentity)) {
                            wifiEnterpriseConfig.setFieldValue("anonymous_identity", NativeUtil.stringFromByteArrayList(this.mEapAnonymousIdentity));
                        }
                        if (getEapPassword() && !ArrayUtils.isEmpty(this.mEapPassword)) {
                            wifiEnterpriseConfig.setFieldValue("password", NativeUtil.stringFromByteArrayList(this.mEapPassword));
                        }
                        if (getEapClientCert() && !TextUtils.isEmpty(this.mEapClientCert)) {
                            wifiEnterpriseConfig.setFieldValue(WifiBackupRestore.SupplicantBackupMigration.SUPPLICANT_KEY_CLIENT_CERT, this.mEapClientCert);
                        }
                        if (getEapCACert() && !TextUtils.isEmpty(this.mEapCACert)) {
                            wifiEnterpriseConfig.setFieldValue(WifiBackupRestore.SupplicantBackupMigration.SUPPLICANT_KEY_CA_CERT, this.mEapCACert);
                        }
                        if (getEapSubjectMatch() && !TextUtils.isEmpty(this.mEapSubjectMatch)) {
                            wifiEnterpriseConfig.setFieldValue("subject_match", this.mEapSubjectMatch);
                        }
                        if (getEapEngineID() && !TextUtils.isEmpty(this.mEapEngineID)) {
                            wifiEnterpriseConfig.setFieldValue("engine_id", this.mEapEngineID);
                        }
                        if (getEapEngine() && !TextUtils.isEmpty(this.mEapEngineID)) {
                            if (this.mEapEngine) {
                                str2 = "1";
                            } else {
                                str2 = "0";
                            }
                            wifiEnterpriseConfig.setFieldValue("engine", str2);
                        }
                        if (getEapPrivateKeyId() && !TextUtils.isEmpty(this.mEapPrivateKeyId)) {
                            wifiEnterpriseConfig.setFieldValue("key_id", this.mEapPrivateKeyId);
                        }
                        if (getEapAltSubjectMatch() && !TextUtils.isEmpty(this.mEapAltSubjectMatch)) {
                            wifiEnterpriseConfig.setFieldValue("altsubject_match", this.mEapAltSubjectMatch);
                        }
                        if (getEapDomainSuffixMatch() && !TextUtils.isEmpty(this.mEapDomainSuffixMatch)) {
                            wifiEnterpriseConfig.setFieldValue("domain_suffix_match", this.mEapDomainSuffixMatch);
                        }
                        if (getEapCAPath() && !TextUtils.isEmpty(this.mEapCAPath)) {
                            wifiEnterpriseConfig.setFieldValue(WifiBackupRestore.SupplicantBackupMigration.SUPPLICANT_KEY_CA_PATH, this.mEapCAPath);
                        }
                        return true;
                    }
                    Log.e(TAG, "failed to get eap phase2 method");
                    return false;
                }
                Log.e(TAG, "failed to get eap method. Assumimg not an enterprise network");
                return true;
            } catch (Throwable th) {
                throw th;
            }
        }
    }

    private boolean saveWifiEnterpriseConfig(String str, WifiEnterpriseConfig wifiEnterpriseConfig) {
        synchronized (this.mLock) {
            try {
                if (wifiEnterpriseConfig == null) {
                    return false;
                }
                if (!setEapMethod(wifiConfigurationToSupplicantEapMethod(wifiEnterpriseConfig.getEapMethod()))) {
                    Log.e(TAG, str + ": failed to set eap method: " + wifiEnterpriseConfig.getEapMethod());
                    return false;
                }
                if (!setEapPhase2Method(wifiConfigurationToSupplicantEapPhase2Method(wifiEnterpriseConfig.getPhase2Method()))) {
                    Log.e(TAG, str + ": failed to set eap phase 2 method: " + wifiEnterpriseConfig.getPhase2Method());
                    return false;
                }
                String fieldValue = wifiEnterpriseConfig.getFieldValue("identity");
                if (!TextUtils.isEmpty(fieldValue) && !setEapIdentity(NativeUtil.stringToByteArrayList(fieldValue))) {
                    Log.e(TAG, str + ": failed to set eap identity: " + fieldValue);
                    return false;
                }
                String fieldValue2 = wifiEnterpriseConfig.getFieldValue("anonymous_identity");
                if (!TextUtils.isEmpty(fieldValue2) && !setEapAnonymousIdentity(NativeUtil.stringToByteArrayList(fieldValue2))) {
                    Log.e(TAG, str + ": failed to set eap anonymous identity: " + fieldValue2);
                    return false;
                }
                String fieldValue3 = wifiEnterpriseConfig.getFieldValue("password");
                if (!TextUtils.isEmpty(fieldValue3) && !setEapPassword(NativeUtil.stringToByteArrayList(fieldValue3))) {
                    Log.e(TAG, str + ": failed to set eap password");
                    return false;
                }
                String fieldValue4 = wifiEnterpriseConfig.getFieldValue(WifiBackupRestore.SupplicantBackupMigration.SUPPLICANT_KEY_CLIENT_CERT);
                if (!TextUtils.isEmpty(fieldValue4) && !setEapClientCert(fieldValue4)) {
                    Log.e(TAG, str + ": failed to set eap client cert: " + fieldValue4);
                    return false;
                }
                String fieldValue5 = wifiEnterpriseConfig.getFieldValue(WifiBackupRestore.SupplicantBackupMigration.SUPPLICANT_KEY_CA_CERT);
                if (!TextUtils.isEmpty(fieldValue5) && !setEapCACert(fieldValue5)) {
                    Log.e(TAG, str + ": failed to set eap ca cert: " + fieldValue5);
                    return false;
                }
                String fieldValue6 = wifiEnterpriseConfig.getFieldValue("subject_match");
                if (!TextUtils.isEmpty(fieldValue6) && !setEapSubjectMatch(fieldValue6)) {
                    Log.e(TAG, str + ": failed to set eap subject match: " + fieldValue6);
                    return false;
                }
                String fieldValue7 = wifiEnterpriseConfig.getFieldValue("engine_id");
                if (!TextUtils.isEmpty(fieldValue7) && !setEapEngineID(fieldValue7)) {
                    Log.e(TAG, str + ": failed to set eap engine id: " + fieldValue7);
                    return false;
                }
                String fieldValue8 = wifiEnterpriseConfig.getFieldValue("engine");
                if (!TextUtils.isEmpty(fieldValue8) && !setEapEngine(fieldValue8.equals("1"))) {
                    Log.e(TAG, str + ": failed to set eap engine: " + fieldValue8);
                    return false;
                }
                String fieldValue9 = wifiEnterpriseConfig.getFieldValue("key_id");
                if (!TextUtils.isEmpty(fieldValue9) && !setEapPrivateKeyId(fieldValue9)) {
                    Log.e(TAG, str + ": failed to set eap private key: " + fieldValue9);
                    return false;
                }
                String fieldValue10 = wifiEnterpriseConfig.getFieldValue("altsubject_match");
                if (!TextUtils.isEmpty(fieldValue10) && !setEapAltSubjectMatch(fieldValue10)) {
                    Log.e(TAG, str + ": failed to set eap alt subject match: " + fieldValue10);
                    return false;
                }
                String fieldValue11 = wifiEnterpriseConfig.getFieldValue("domain_suffix_match");
                if (!TextUtils.isEmpty(fieldValue11) && !setEapDomainSuffixMatch(fieldValue11)) {
                    Log.e(TAG, str + ": failed to set eap domain suffix match: " + fieldValue11);
                    return false;
                }
                String fieldValue12 = wifiEnterpriseConfig.getFieldValue(WifiBackupRestore.SupplicantBackupMigration.SUPPLICANT_KEY_CA_PATH);
                if (!TextUtils.isEmpty(fieldValue12) && !setEapCAPath(fieldValue12)) {
                    Log.e(TAG, str + ": failed to set eap ca path: " + fieldValue12);
                    return false;
                }
                String fieldValue13 = wifiEnterpriseConfig.getFieldValue("proactive_key_caching");
                if (!TextUtils.isEmpty(fieldValue13) && !setEapProactiveKeyCaching(fieldValue13.equals("1"))) {
                    Log.e(TAG, str + ": failed to set proactive key caching: " + fieldValue13);
                    return false;
                }
                return true;
            } catch (Throwable th) {
                throw th;
            }
        }
    }

    private static int wifiConfigurationToSupplicantKeyMgmtMask(BitSet bitSet) {
        int i = 0;
        int iNextSetBit = bitSet.nextSetBit(0);
        while (iNextSetBit != -1) {
            switch (iNextSetBit) {
                case 0:
                    i |= 4;
                    break;
                case 1:
                    i |= 2;
                    break;
                case 2:
                    i |= 1;
                    break;
                case 3:
                    i |= 8;
                    break;
                case 4:
                default:
                    throw new IllegalArgumentException("Invalid protoMask bit in keyMgmt: " + iNextSetBit);
                case 5:
                    i |= ISupplicantStaNetwork.KeyMgmtMask.OSEN;
                    break;
                case 6:
                    i |= 64;
                    break;
                case 7:
                    i |= 32;
                    break;
                case 8:
                    i |= 4096;
                    break;
                case 9:
                    i |= 8192;
                    break;
            }
            iNextSetBit = bitSet.nextSetBit(iNextSetBit + 1);
        }
        return i;
    }

    private static int wifiConfigurationToSupplicantProtoMask(BitSet bitSet) {
        int i = 0;
        int iNextSetBit = bitSet.nextSetBit(0);
        while (iNextSetBit != -1) {
            switch (iNextSetBit) {
                case 0:
                    i |= 1;
                    break;
                case 1:
                    i |= 2;
                    break;
                case 2:
                    i |= 8;
                    break;
                case 3:
                    i |= 4;
                    break;
                default:
                    throw new IllegalArgumentException("Invalid protoMask bit in wificonfig: " + iNextSetBit);
            }
            iNextSetBit = bitSet.nextSetBit(iNextSetBit + 1);
        }
        return i;
    }

    private static int wifiConfigurationToSupplicantAuthAlgMask(BitSet bitSet) {
        int i = 0;
        int iNextSetBit = bitSet.nextSetBit(0);
        while (iNextSetBit != -1) {
            switch (iNextSetBit) {
                case 0:
                    i |= 1;
                    break;
                case 1:
                    i |= 2;
                    break;
                case 2:
                    i |= 4;
                    break;
                default:
                    throw new IllegalArgumentException("Invalid authAlgMask bit in wificonfig: " + iNextSetBit);
            }
            iNextSetBit = bitSet.nextSetBit(iNextSetBit + 1);
        }
        return i;
    }

    private static int wifiConfigurationToSupplicantGroupCipherMask(BitSet bitSet) {
        int i = 0;
        int iNextSetBit = bitSet.nextSetBit(0);
        while (iNextSetBit != -1) {
            switch (iNextSetBit) {
                case 0:
                    i |= 2;
                    break;
                case 1:
                    i |= 4;
                    break;
                case 2:
                    i |= 8;
                    break;
                case 3:
                    i |= 16;
                    break;
                case 4:
                    i |= 16384;
                    break;
                default:
                    throw new IllegalArgumentException("Invalid GroupCipherMask bit in wificonfig: " + iNextSetBit);
            }
            iNextSetBit = bitSet.nextSetBit(iNextSetBit + 1);
        }
        return i;
    }

    private static int wifiConfigurationToSupplicantPairwiseCipherMask(BitSet bitSet) {
        int i = 0;
        int iNextSetBit = bitSet.nextSetBit(0);
        while (iNextSetBit != -1) {
            switch (iNextSetBit) {
                case 0:
                    i |= 1;
                    break;
                case 1:
                    i |= 8;
                    break;
                case 2:
                    i |= 16;
                    break;
                default:
                    throw new IllegalArgumentException("Invalid pairwiseCipherMask bit in wificonfig: " + iNextSetBit);
            }
            iNextSetBit = bitSet.nextSetBit(iNextSetBit + 1);
        }
        return i;
    }

    private static int supplicantToWifiConfigurationEapMethod(int i) {
        switch (i) {
            case 0:
                return 0;
            case 1:
                return 1;
            case 2:
                return 2;
            case 3:
                return 3;
            case 4:
                return 4;
            case 5:
                return 5;
            case 6:
                return 6;
            case 7:
                return 7;
            default:
                Log.e(TAG, "invalid eap method value from supplicant: " + i);
                return -1;
        }
    }

    private static int supplicantToWifiConfigurationEapPhase2Method(int i) {
        switch (i) {
            case 0:
                return 0;
            case 1:
                return 1;
            case 2:
                return 2;
            case 3:
                return 3;
            case 4:
                return 4;
            case 5:
                return 5;
            case 6:
                return 6;
            case 7:
                return 7;
            default:
                Log.e(TAG, "invalid eap phase2 method value from supplicant: " + i);
                return -1;
        }
    }

    private static int supplicantMaskValueToWifiConfigurationBitSet(int i, int i2, BitSet bitSet, int i3) {
        bitSet.set(i3, (i & i2) == i2);
        return i & (~i2);
    }

    private static BitSet supplicantToWifiConfigurationKeyMgmtMask(int i) {
        BitSet bitSet = new BitSet();
        int iSupplicantMaskValueToWifiConfigurationBitSet = supplicantMaskValueToWifiConfigurationBitSet(supplicantMaskValueToWifiConfigurationBitSet(supplicantMaskValueToWifiConfigurationBitSet(supplicantMaskValueToWifiConfigurationBitSet(supplicantMaskValueToWifiConfigurationBitSet(supplicantMaskValueToWifiConfigurationBitSet(supplicantMaskValueToWifiConfigurationBitSet(supplicantMaskValueToWifiConfigurationBitSet(supplicantMaskValueToWifiConfigurationBitSet(i, 4, bitSet, 0), 2, bitSet, 1), 1, bitSet, 2), 8, bitSet, 3), ISupplicantStaNetwork.KeyMgmtMask.OSEN, bitSet, 5), 64, bitSet, 6), 32, bitSet, 7), 4096, bitSet, 8), 8192, bitSet, 9);
        if (iSupplicantMaskValueToWifiConfigurationBitSet != 0) {
            throw new IllegalArgumentException("invalid key mgmt mask from supplicant: " + iSupplicantMaskValueToWifiConfigurationBitSet);
        }
        return bitSet;
    }

    private static BitSet supplicantToWifiConfigurationProtoMask(int i) {
        BitSet bitSet = new BitSet();
        int iSupplicantMaskValueToWifiConfigurationBitSet = supplicantMaskValueToWifiConfigurationBitSet(supplicantMaskValueToWifiConfigurationBitSet(supplicantMaskValueToWifiConfigurationBitSet(supplicantMaskValueToWifiConfigurationBitSet(i, 1, bitSet, 0), 2, bitSet, 1), 8, bitSet, 2), 4, bitSet, 3);
        if (iSupplicantMaskValueToWifiConfigurationBitSet != 0) {
            throw new IllegalArgumentException("invalid proto mask from supplicant: " + iSupplicantMaskValueToWifiConfigurationBitSet);
        }
        return bitSet;
    }

    private static BitSet supplicantToWifiConfigurationAuthAlgMask(int i) {
        BitSet bitSet = new BitSet();
        int iSupplicantMaskValueToWifiConfigurationBitSet = supplicantMaskValueToWifiConfigurationBitSet(supplicantMaskValueToWifiConfigurationBitSet(supplicantMaskValueToWifiConfigurationBitSet(i, 1, bitSet, 0), 2, bitSet, 1), 4, bitSet, 2);
        if (iSupplicantMaskValueToWifiConfigurationBitSet != 0) {
            throw new IllegalArgumentException("invalid auth alg mask from supplicant: " + iSupplicantMaskValueToWifiConfigurationBitSet);
        }
        return bitSet;
    }

    private static BitSet supplicantToWifiConfigurationGroupCipherMask(int i) {
        BitSet bitSet = new BitSet();
        int iSupplicantMaskValueToWifiConfigurationBitSet = supplicantMaskValueToWifiConfigurationBitSet(supplicantMaskValueToWifiConfigurationBitSet(supplicantMaskValueToWifiConfigurationBitSet(supplicantMaskValueToWifiConfigurationBitSet(supplicantMaskValueToWifiConfigurationBitSet(i, 2, bitSet, 0), 4, bitSet, 1), 8, bitSet, 2), 16, bitSet, 3), 16384, bitSet, 4);
        if (iSupplicantMaskValueToWifiConfigurationBitSet != 0) {
            throw new IllegalArgumentException("invalid group cipher mask from supplicant: " + iSupplicantMaskValueToWifiConfigurationBitSet);
        }
        return bitSet;
    }

    private static BitSet supplicantToWifiConfigurationPairwiseCipherMask(int i) {
        BitSet bitSet = new BitSet();
        int iSupplicantMaskValueToWifiConfigurationBitSet = supplicantMaskValueToWifiConfigurationBitSet(supplicantMaskValueToWifiConfigurationBitSet(supplicantMaskValueToWifiConfigurationBitSet(i, 1, bitSet, 0), 8, bitSet, 1), 16, bitSet, 2);
        if (iSupplicantMaskValueToWifiConfigurationBitSet != 0) {
            throw new IllegalArgumentException("invalid pairwise cipher mask from supplicant: " + iSupplicantMaskValueToWifiConfigurationBitSet);
        }
        return bitSet;
    }

    private static int wifiConfigurationToSupplicantEapMethod(int i) {
        switch (i) {
            case 0:
                return 0;
            case 1:
                return 1;
            case 2:
                return 2;
            case 3:
                return 3;
            case 4:
                return 4;
            case 5:
                return 5;
            case 6:
                return 6;
            case 7:
                return 7;
            default:
                Log.e(TAG, "invalid eap method value from WifiConfiguration: " + i);
                return -1;
        }
    }

    private static int wifiConfigurationToSupplicantEapPhase2Method(int i) {
        switch (i) {
            case 0:
                return 0;
            case 1:
                return 1;
            case 2:
                return 2;
            case 3:
                return 3;
            case 4:
                return 4;
            case 5:
                return 5;
            case 6:
                return 6;
            case 7:
                return 7;
            default:
                Log.e(TAG, "invalid eap phase2 method value from WifiConfiguration: " + i);
                return -1;
        }
    }

    private boolean getId() {
        synchronized (this.mLock) {
            if (!checkISupplicantStaNetworkAndLogFailure("getId")) {
                return false;
            }
            try {
                final MutableBoolean mutableBoolean = new MutableBoolean(false);
                this.mISupplicantStaNetwork.getId(new ISupplicantNetwork.getIdCallback() {
                    @Override
                    public final void onValues(SupplicantStatus supplicantStatus, int i) {
                        SupplicantStaNetworkHal.lambda$getId$1(this.f$0, mutableBoolean, supplicantStatus, i);
                    }
                });
                return mutableBoolean.value;
            } catch (RemoteException e) {
                handleRemoteException(e, "getId");
                return false;
            }
        }
    }

    public static void lambda$getId$1(SupplicantStaNetworkHal supplicantStaNetworkHal, MutableBoolean mutableBoolean, SupplicantStatus supplicantStatus, int i) {
        mutableBoolean.value = supplicantStatus.code == 0;
        if (mutableBoolean.value) {
            supplicantStaNetworkHal.mNetworkId = i;
        } else {
            supplicantStaNetworkHal.checkStatusAndLogFailure(supplicantStatus, "getId");
        }
    }

    private boolean registerCallback(ISupplicantStaNetworkCallback iSupplicantStaNetworkCallback) {
        synchronized (this.mLock) {
            if (!checkISupplicantStaNetworkAndLogFailure("registerCallback")) {
                return false;
            }
            try {
                return checkStatusAndLogFailure(this.mISupplicantStaNetwork.registerCallback(iSupplicantStaNetworkCallback), "registerCallback");
            } catch (RemoteException e) {
                handleRemoteException(e, "registerCallback");
                return false;
            }
        }
    }

    private boolean setSsid(ArrayList<Byte> arrayList) {
        synchronized (this.mLock) {
            if (!checkISupplicantStaNetworkAndLogFailure("setSsid")) {
                return false;
            }
            try {
                return checkStatusAndLogFailure(this.mISupplicantStaNetwork.setSsid(arrayList), "setSsid");
            } catch (RemoteException e) {
                handleRemoteException(e, "setSsid");
                return false;
            }
        }
    }

    public boolean setBssid(String str) {
        boolean bssid;
        synchronized (this.mLock) {
            try {
                try {
                    bssid = setBssid(NativeUtil.macAddressToByteArray(str));
                } catch (IllegalArgumentException e) {
                    Log.e(TAG, "Illegal argument " + str, e);
                    return false;
                }
            } catch (Throwable th) {
                throw th;
            }
        }
        return bssid;
    }

    private boolean setBssid(byte[] bArr) {
        synchronized (this.mLock) {
            if (!checkISupplicantStaNetworkAndLogFailure("setBssid")) {
                return false;
            }
            try {
                return checkStatusAndLogFailure(this.mISupplicantStaNetwork.setBssid(bArr), "setBssid");
            } catch (RemoteException e) {
                handleRemoteException(e, "setBssid");
                return false;
            }
        }
    }

    private boolean setScanSsid(boolean z) {
        synchronized (this.mLock) {
            if (!checkISupplicantStaNetworkAndLogFailure("setScanSsid")) {
                return false;
            }
            try {
                return checkStatusAndLogFailure(this.mISupplicantStaNetwork.setScanSsid(z), "setScanSsid");
            } catch (RemoteException e) {
                handleRemoteException(e, "setScanSsid");
                return false;
            }
        }
    }

    private boolean setKeyMgmt(int i) {
        synchronized (this.mLock) {
            if (!checkISupplicantStaNetworkAndLogFailure("setKeyMgmt")) {
                return false;
            }
            try {
                return checkStatusAndLogFailure(this.mISupplicantStaNetwork.setKeyMgmt(i), "setKeyMgmt");
            } catch (RemoteException e) {
                handleRemoteException(e, "setKeyMgmt");
                return false;
            }
        }
    }

    private boolean setProto(int i) {
        synchronized (this.mLock) {
            if (!checkISupplicantStaNetworkAndLogFailure("setProto")) {
                return false;
            }
            try {
                return checkStatusAndLogFailure(this.mISupplicantStaNetwork.setProto(i), "setProto");
            } catch (RemoteException e) {
                handleRemoteException(e, "setProto");
                return false;
            }
        }
    }

    private boolean setAuthAlg(int i) {
        synchronized (this.mLock) {
            if (!checkISupplicantStaNetworkAndLogFailure("setAuthAlg")) {
                return false;
            }
            try {
                return checkStatusAndLogFailure(this.mISupplicantStaNetwork.setAuthAlg(i), "setAuthAlg");
            } catch (RemoteException e) {
                handleRemoteException(e, "setAuthAlg");
                return false;
            }
        }
    }

    private boolean setGroupCipher(int i) {
        synchronized (this.mLock) {
            if (!checkISupplicantStaNetworkAndLogFailure("setGroupCipher")) {
                return false;
            }
            try {
                return checkStatusAndLogFailure(this.mISupplicantStaNetwork.setGroupCipher(i), "setGroupCipher");
            } catch (RemoteException e) {
                handleRemoteException(e, "setGroupCipher");
                return false;
            }
        }
    }

    private boolean setPairwiseCipher(int i) {
        synchronized (this.mLock) {
            if (!checkISupplicantStaNetworkAndLogFailure("setPairwiseCipher")) {
                return false;
            }
            try {
                return checkStatusAndLogFailure(this.mISupplicantStaNetwork.setPairwiseCipher(i), "setPairwiseCipher");
            } catch (RemoteException e) {
                handleRemoteException(e, "setPairwiseCipher");
                return false;
            }
        }
    }

    private boolean setPskPassphrase(String str) {
        synchronized (this.mLock) {
            if (!checkISupplicantStaNetworkAndLogFailure("setPskPassphrase")) {
                return false;
            }
            try {
                return checkStatusAndLogFailure(this.mISupplicantStaNetwork.setPskPassphrase(str), "setPskPassphrase");
            } catch (RemoteException e) {
                handleRemoteException(e, "setPskPassphrase");
                return false;
            }
        }
    }

    private boolean setPsk(byte[] bArr) {
        synchronized (this.mLock) {
            if (!checkISupplicantStaNetworkAndLogFailure("setPsk")) {
                return false;
            }
            try {
                return checkStatusAndLogFailure(this.mISupplicantStaNetwork.setPsk(bArr), "setPsk");
            } catch (RemoteException e) {
                handleRemoteException(e, "setPsk");
                return false;
            }
        }
    }

    private boolean setWepKey(int i, ArrayList<Byte> arrayList) {
        synchronized (this.mLock) {
            if (!checkISupplicantStaNetworkAndLogFailure("setWepKey")) {
                return false;
            }
            try {
                return checkStatusAndLogFailure(this.mISupplicantStaNetwork.setWepKey(i, arrayList), "setWepKey");
            } catch (RemoteException e) {
                handleRemoteException(e, "setWepKey");
                return false;
            }
        }
    }

    private boolean setWepTxKeyIdx(int i) {
        synchronized (this.mLock) {
            if (!checkISupplicantStaNetworkAndLogFailure("setWepTxKeyIdx")) {
                return false;
            }
            try {
                return checkStatusAndLogFailure(this.mISupplicantStaNetwork.setWepTxKeyIdx(i), "setWepTxKeyIdx");
            } catch (RemoteException e) {
                handleRemoteException(e, "setWepTxKeyIdx");
                return false;
            }
        }
    }

    private boolean setRequirePmf(boolean z) {
        synchronized (this.mLock) {
            if (!checkISupplicantStaNetworkAndLogFailure("setRequirePmf")) {
                return false;
            }
            try {
                return checkStatusAndLogFailure(this.mISupplicantStaNetwork.setRequirePmf(z), "setRequirePmf");
            } catch (RemoteException e) {
                handleRemoteException(e, "setRequirePmf");
                return false;
            }
        }
    }

    private boolean setUpdateIdentifier(int i) {
        synchronized (this.mLock) {
            if (!checkISupplicantStaNetworkAndLogFailure("setUpdateIdentifier")) {
                return false;
            }
            try {
                return checkStatusAndLogFailure(this.mISupplicantStaNetwork.setUpdateIdentifier(i), "setUpdateIdentifier");
            } catch (RemoteException e) {
                handleRemoteException(e, "setUpdateIdentifier");
                return false;
            }
        }
    }

    private boolean setEapMethod(int i) {
        synchronized (this.mLock) {
            if (!checkISupplicantStaNetworkAndLogFailure("setEapMethod")) {
                return false;
            }
            try {
                return checkStatusAndLogFailure(this.mISupplicantStaNetwork.setEapMethod(i), "setEapMethod");
            } catch (RemoteException e) {
                handleRemoteException(e, "setEapMethod");
                return false;
            }
        }
    }

    private boolean setEapPhase2Method(int i) {
        synchronized (this.mLock) {
            if (!checkISupplicantStaNetworkAndLogFailure("setEapPhase2Method")) {
                return false;
            }
            try {
                return checkStatusAndLogFailure(this.mISupplicantStaNetwork.setEapPhase2Method(i), "setEapPhase2Method");
            } catch (RemoteException e) {
                handleRemoteException(e, "setEapPhase2Method");
                return false;
            }
        }
    }

    private boolean setEapIdentity(ArrayList<Byte> arrayList) {
        synchronized (this.mLock) {
            if (!checkISupplicantStaNetworkAndLogFailure("setEapIdentity")) {
                return false;
            }
            try {
                return checkStatusAndLogFailure(this.mISupplicantStaNetwork.setEapIdentity(arrayList), "setEapIdentity");
            } catch (RemoteException e) {
                handleRemoteException(e, "setEapIdentity");
                return false;
            }
        }
    }

    private boolean setEapAnonymousIdentity(ArrayList<Byte> arrayList) {
        synchronized (this.mLock) {
            if (!checkISupplicantStaNetworkAndLogFailure("setEapAnonymousIdentity")) {
                return false;
            }
            try {
                return checkStatusAndLogFailure(this.mISupplicantStaNetwork.setEapAnonymousIdentity(arrayList), "setEapAnonymousIdentity");
            } catch (RemoteException e) {
                handleRemoteException(e, "setEapAnonymousIdentity");
                return false;
            }
        }
    }

    private boolean setEapPassword(ArrayList<Byte> arrayList) {
        synchronized (this.mLock) {
            if (!checkISupplicantStaNetworkAndLogFailure("setEapPassword")) {
                return false;
            }
            try {
                return checkStatusAndLogFailure(this.mISupplicantStaNetwork.setEapPassword(arrayList), "setEapPassword");
            } catch (RemoteException e) {
                handleRemoteException(e, "setEapPassword");
                return false;
            }
        }
    }

    private boolean setEapCACert(String str) {
        synchronized (this.mLock) {
            if (!checkISupplicantStaNetworkAndLogFailure("setEapCACert")) {
                return false;
            }
            try {
                return checkStatusAndLogFailure(this.mISupplicantStaNetwork.setEapCACert(str), "setEapCACert");
            } catch (RemoteException e) {
                handleRemoteException(e, "setEapCACert");
                return false;
            }
        }
    }

    private boolean setEapCAPath(String str) {
        synchronized (this.mLock) {
            if (!checkISupplicantStaNetworkAndLogFailure("setEapCAPath")) {
                return false;
            }
            try {
                return checkStatusAndLogFailure(this.mISupplicantStaNetwork.setEapCAPath(str), "setEapCAPath");
            } catch (RemoteException e) {
                handleRemoteException(e, "setEapCAPath");
                return false;
            }
        }
    }

    private boolean setEapClientCert(String str) {
        synchronized (this.mLock) {
            if (!checkISupplicantStaNetworkAndLogFailure("setEapClientCert")) {
                return false;
            }
            try {
                return checkStatusAndLogFailure(this.mISupplicantStaNetwork.setEapClientCert(str), "setEapClientCert");
            } catch (RemoteException e) {
                handleRemoteException(e, "setEapClientCert");
                return false;
            }
        }
    }

    private boolean setEapPrivateKeyId(String str) {
        synchronized (this.mLock) {
            if (!checkISupplicantStaNetworkAndLogFailure("setEapPrivateKeyId")) {
                return false;
            }
            try {
                return checkStatusAndLogFailure(this.mISupplicantStaNetwork.setEapPrivateKeyId(str), "setEapPrivateKeyId");
            } catch (RemoteException e) {
                handleRemoteException(e, "setEapPrivateKeyId");
                return false;
            }
        }
    }

    private boolean setEapSubjectMatch(String str) {
        synchronized (this.mLock) {
            if (!checkISupplicantStaNetworkAndLogFailure("setEapSubjectMatch")) {
                return false;
            }
            try {
                return checkStatusAndLogFailure(this.mISupplicantStaNetwork.setEapSubjectMatch(str), "setEapSubjectMatch");
            } catch (RemoteException e) {
                handleRemoteException(e, "setEapSubjectMatch");
                return false;
            }
        }
    }

    private boolean setEapAltSubjectMatch(String str) {
        synchronized (this.mLock) {
            if (!checkISupplicantStaNetworkAndLogFailure("setEapAltSubjectMatch")) {
                return false;
            }
            try {
                return checkStatusAndLogFailure(this.mISupplicantStaNetwork.setEapAltSubjectMatch(str), "setEapAltSubjectMatch");
            } catch (RemoteException e) {
                handleRemoteException(e, "setEapAltSubjectMatch");
                return false;
            }
        }
    }

    private boolean setEapEngine(boolean z) {
        synchronized (this.mLock) {
            if (!checkISupplicantStaNetworkAndLogFailure("setEapEngine")) {
                return false;
            }
            try {
                return checkStatusAndLogFailure(this.mISupplicantStaNetwork.setEapEngine(z), "setEapEngine");
            } catch (RemoteException e) {
                handleRemoteException(e, "setEapEngine");
                return false;
            }
        }
    }

    private boolean setEapEngineID(String str) {
        synchronized (this.mLock) {
            if (!checkISupplicantStaNetworkAndLogFailure("setEapEngineID")) {
                return false;
            }
            try {
                return checkStatusAndLogFailure(this.mISupplicantStaNetwork.setEapEngineID(str), "setEapEngineID");
            } catch (RemoteException e) {
                handleRemoteException(e, "setEapEngineID");
                return false;
            }
        }
    }

    private boolean setEapDomainSuffixMatch(String str) {
        synchronized (this.mLock) {
            if (!checkISupplicantStaNetworkAndLogFailure("setEapDomainSuffixMatch")) {
                return false;
            }
            try {
                return checkStatusAndLogFailure(this.mISupplicantStaNetwork.setEapDomainSuffixMatch(str), "setEapDomainSuffixMatch");
            } catch (RemoteException e) {
                handleRemoteException(e, "setEapDomainSuffixMatch");
                return false;
            }
        }
    }

    private boolean setEapProactiveKeyCaching(boolean z) {
        synchronized (this.mLock) {
            if (!checkISupplicantStaNetworkAndLogFailure("setEapProactiveKeyCaching")) {
                return false;
            }
            try {
                return checkStatusAndLogFailure(this.mISupplicantStaNetwork.setProactiveKeyCaching(z), "setEapProactiveKeyCaching");
            } catch (RemoteException e) {
                handleRemoteException(e, "setEapProactiveKeyCaching");
                return false;
            }
        }
    }

    private boolean setIdStr(String str) {
        synchronized (this.mLock) {
            if (!checkISupplicantStaNetworkAndLogFailure("setIdStr")) {
                return false;
            }
            try {
                return checkStatusAndLogFailure(this.mISupplicantStaNetwork.setIdStr(str), "setIdStr");
            } catch (RemoteException e) {
                handleRemoteException(e, "setIdStr");
                return false;
            }
        }
    }

    private boolean getSsid() {
        synchronized (this.mLock) {
            if (!checkISupplicantStaNetworkAndLogFailure("getSsid")) {
                return false;
            }
            try {
                final MutableBoolean mutableBoolean = new MutableBoolean(false);
                this.mISupplicantStaNetwork.getSsid(new ISupplicantStaNetwork.getSsidCallback() {
                    @Override
                    public final void onValues(SupplicantStatus supplicantStatus, ArrayList arrayList) {
                        SupplicantStaNetworkHal.lambda$getSsid$2(this.f$0, mutableBoolean, supplicantStatus, arrayList);
                    }
                });
                return mutableBoolean.value;
            } catch (RemoteException e) {
                handleRemoteException(e, "getSsid");
                return false;
            }
        }
    }

    public static void lambda$getSsid$2(SupplicantStaNetworkHal supplicantStaNetworkHal, MutableBoolean mutableBoolean, SupplicantStatus supplicantStatus, ArrayList arrayList) {
        mutableBoolean.value = supplicantStatus.code == 0;
        if (mutableBoolean.value) {
            supplicantStaNetworkHal.mSsid = arrayList;
        } else {
            supplicantStaNetworkHal.checkStatusAndLogFailure(supplicantStatus, "getSsid");
        }
    }

    private boolean getBssid() {
        synchronized (this.mLock) {
            if (!checkISupplicantStaNetworkAndLogFailure("getBssid")) {
                return false;
            }
            try {
                final MutableBoolean mutableBoolean = new MutableBoolean(false);
                this.mISupplicantStaNetwork.getBssid(new ISupplicantStaNetwork.getBssidCallback() {
                    @Override
                    public final void onValues(SupplicantStatus supplicantStatus, byte[] bArr) {
                        SupplicantStaNetworkHal.lambda$getBssid$3(this.f$0, mutableBoolean, supplicantStatus, bArr);
                    }
                });
                return mutableBoolean.value;
            } catch (RemoteException e) {
                handleRemoteException(e, "getBssid");
                return false;
            }
        }
    }

    public static void lambda$getBssid$3(SupplicantStaNetworkHal supplicantStaNetworkHal, MutableBoolean mutableBoolean, SupplicantStatus supplicantStatus, byte[] bArr) {
        mutableBoolean.value = supplicantStatus.code == 0;
        if (mutableBoolean.value) {
            supplicantStaNetworkHal.mBssid = bArr;
        } else {
            supplicantStaNetworkHal.checkStatusAndLogFailure(supplicantStatus, "getBssid");
        }
    }

    private boolean getScanSsid() {
        synchronized (this.mLock) {
            if (!checkISupplicantStaNetworkAndLogFailure("getScanSsid")) {
                return false;
            }
            try {
                final MutableBoolean mutableBoolean = new MutableBoolean(false);
                this.mISupplicantStaNetwork.getScanSsid(new ISupplicantStaNetwork.getScanSsidCallback() {
                    @Override
                    public final void onValues(SupplicantStatus supplicantStatus, boolean z) {
                        SupplicantStaNetworkHal.lambda$getScanSsid$4(this.f$0, mutableBoolean, supplicantStatus, z);
                    }
                });
                return mutableBoolean.value;
            } catch (RemoteException e) {
                handleRemoteException(e, "getScanSsid");
                return false;
            }
        }
    }

    public static void lambda$getScanSsid$4(SupplicantStaNetworkHal supplicantStaNetworkHal, MutableBoolean mutableBoolean, SupplicantStatus supplicantStatus, boolean z) {
        mutableBoolean.value = supplicantStatus.code == 0;
        if (mutableBoolean.value) {
            supplicantStaNetworkHal.mScanSsid = z;
        } else {
            supplicantStaNetworkHal.checkStatusAndLogFailure(supplicantStatus, "getScanSsid");
        }
    }

    private boolean getKeyMgmt() {
        synchronized (this.mLock) {
            if (!checkISupplicantStaNetworkAndLogFailure("getKeyMgmt")) {
                return false;
            }
            try {
                final MutableBoolean mutableBoolean = new MutableBoolean(false);
                this.mISupplicantStaNetwork.getKeyMgmt(new ISupplicantStaNetwork.getKeyMgmtCallback() {
                    @Override
                    public final void onValues(SupplicantStatus supplicantStatus, int i) {
                        SupplicantStaNetworkHal.lambda$getKeyMgmt$5(this.f$0, mutableBoolean, supplicantStatus, i);
                    }
                });
                return mutableBoolean.value;
            } catch (RemoteException e) {
                handleRemoteException(e, "getKeyMgmt");
                return false;
            }
        }
    }

    public static void lambda$getKeyMgmt$5(SupplicantStaNetworkHal supplicantStaNetworkHal, MutableBoolean mutableBoolean, SupplicantStatus supplicantStatus, int i) {
        mutableBoolean.value = supplicantStatus.code == 0;
        if (mutableBoolean.value) {
            supplicantStaNetworkHal.mKeyMgmtMask = i;
        } else {
            supplicantStaNetworkHal.checkStatusAndLogFailure(supplicantStatus, "getKeyMgmt");
        }
    }

    private boolean getProto() {
        synchronized (this.mLock) {
            if (!checkISupplicantStaNetworkAndLogFailure("getProto")) {
                return false;
            }
            try {
                final MutableBoolean mutableBoolean = new MutableBoolean(false);
                this.mISupplicantStaNetwork.getProto(new ISupplicantStaNetwork.getProtoCallback() {
                    @Override
                    public final void onValues(SupplicantStatus supplicantStatus, int i) {
                        SupplicantStaNetworkHal.lambda$getProto$6(this.f$0, mutableBoolean, supplicantStatus, i);
                    }
                });
                return mutableBoolean.value;
            } catch (RemoteException e) {
                handleRemoteException(e, "getProto");
                return false;
            }
        }
    }

    public static void lambda$getProto$6(SupplicantStaNetworkHal supplicantStaNetworkHal, MutableBoolean mutableBoolean, SupplicantStatus supplicantStatus, int i) {
        mutableBoolean.value = supplicantStatus.code == 0;
        if (mutableBoolean.value) {
            supplicantStaNetworkHal.mProtoMask = i;
        } else {
            supplicantStaNetworkHal.checkStatusAndLogFailure(supplicantStatus, "getProto");
        }
    }

    private boolean getAuthAlg() {
        synchronized (this.mLock) {
            if (!checkISupplicantStaNetworkAndLogFailure("getAuthAlg")) {
                return false;
            }
            try {
                final MutableBoolean mutableBoolean = new MutableBoolean(false);
                this.mISupplicantStaNetwork.getAuthAlg(new ISupplicantStaNetwork.getAuthAlgCallback() {
                    @Override
                    public final void onValues(SupplicantStatus supplicantStatus, int i) {
                        SupplicantStaNetworkHal.lambda$getAuthAlg$7(this.f$0, mutableBoolean, supplicantStatus, i);
                    }
                });
                return mutableBoolean.value;
            } catch (RemoteException e) {
                handleRemoteException(e, "getAuthAlg");
                return false;
            }
        }
    }

    public static void lambda$getAuthAlg$7(SupplicantStaNetworkHal supplicantStaNetworkHal, MutableBoolean mutableBoolean, SupplicantStatus supplicantStatus, int i) {
        mutableBoolean.value = supplicantStatus.code == 0;
        if (mutableBoolean.value) {
            supplicantStaNetworkHal.mAuthAlgMask = i;
        } else {
            supplicantStaNetworkHal.checkStatusAndLogFailure(supplicantStatus, "getAuthAlg");
        }
    }

    private boolean getGroupCipher() {
        synchronized (this.mLock) {
            if (!checkISupplicantStaNetworkAndLogFailure("getGroupCipher")) {
                return false;
            }
            try {
                final MutableBoolean mutableBoolean = new MutableBoolean(false);
                this.mISupplicantStaNetwork.getGroupCipher(new ISupplicantStaNetwork.getGroupCipherCallback() {
                    @Override
                    public final void onValues(SupplicantStatus supplicantStatus, int i) {
                        SupplicantStaNetworkHal.lambda$getGroupCipher$8(this.f$0, mutableBoolean, supplicantStatus, i);
                    }
                });
                return mutableBoolean.value;
            } catch (RemoteException e) {
                handleRemoteException(e, "getGroupCipher");
                return false;
            }
        }
    }

    public static void lambda$getGroupCipher$8(SupplicantStaNetworkHal supplicantStaNetworkHal, MutableBoolean mutableBoolean, SupplicantStatus supplicantStatus, int i) {
        mutableBoolean.value = supplicantStatus.code == 0;
        if (mutableBoolean.value) {
            supplicantStaNetworkHal.mGroupCipherMask = i;
        } else {
            supplicantStaNetworkHal.checkStatusAndLogFailure(supplicantStatus, "getGroupCipher");
        }
    }

    private boolean getPairwiseCipher() {
        synchronized (this.mLock) {
            if (!checkISupplicantStaNetworkAndLogFailure("getPairwiseCipher")) {
                return false;
            }
            try {
                final MutableBoolean mutableBoolean = new MutableBoolean(false);
                this.mISupplicantStaNetwork.getPairwiseCipher(new ISupplicantStaNetwork.getPairwiseCipherCallback() {
                    @Override
                    public final void onValues(SupplicantStatus supplicantStatus, int i) {
                        SupplicantStaNetworkHal.lambda$getPairwiseCipher$9(this.f$0, mutableBoolean, supplicantStatus, i);
                    }
                });
                return mutableBoolean.value;
            } catch (RemoteException e) {
                handleRemoteException(e, "getPairwiseCipher");
                return false;
            }
        }
    }

    public static void lambda$getPairwiseCipher$9(SupplicantStaNetworkHal supplicantStaNetworkHal, MutableBoolean mutableBoolean, SupplicantStatus supplicantStatus, int i) {
        mutableBoolean.value = supplicantStatus.code == 0;
        if (mutableBoolean.value) {
            supplicantStaNetworkHal.mPairwiseCipherMask = i;
        } else {
            supplicantStaNetworkHal.checkStatusAndLogFailure(supplicantStatus, "getPairwiseCipher");
        }
    }

    private boolean getPskPassphrase() {
        synchronized (this.mLock) {
            if (!checkISupplicantStaNetworkAndLogFailure("getPskPassphrase")) {
                return false;
            }
            try {
                final MutableBoolean mutableBoolean = new MutableBoolean(false);
                this.mISupplicantStaNetwork.getPskPassphrase(new ISupplicantStaNetwork.getPskPassphraseCallback() {
                    @Override
                    public final void onValues(SupplicantStatus supplicantStatus, String str) {
                        SupplicantStaNetworkHal.lambda$getPskPassphrase$10(this.f$0, mutableBoolean, supplicantStatus, str);
                    }
                });
                return mutableBoolean.value;
            } catch (RemoteException e) {
                handleRemoteException(e, "getPskPassphrase");
                return false;
            }
        }
    }

    public static void lambda$getPskPassphrase$10(SupplicantStaNetworkHal supplicantStaNetworkHal, MutableBoolean mutableBoolean, SupplicantStatus supplicantStatus, String str) {
        mutableBoolean.value = supplicantStatus.code == 0;
        if (mutableBoolean.value) {
            supplicantStaNetworkHal.mPskPassphrase = str;
        } else {
            supplicantStaNetworkHal.checkStatusAndLogFailure(supplicantStatus, "getPskPassphrase");
        }
    }

    private boolean getPsk() {
        synchronized (this.mLock) {
            if (!checkISupplicantStaNetworkAndLogFailure("getPsk")) {
                return false;
            }
            try {
                final MutableBoolean mutableBoolean = new MutableBoolean(false);
                this.mISupplicantStaNetwork.getPsk(new ISupplicantStaNetwork.getPskCallback() {
                    @Override
                    public final void onValues(SupplicantStatus supplicantStatus, byte[] bArr) {
                        SupplicantStaNetworkHal.lambda$getPsk$11(this.f$0, mutableBoolean, supplicantStatus, bArr);
                    }
                });
                return mutableBoolean.value;
            } catch (RemoteException e) {
                handleRemoteException(e, "getPsk");
                return false;
            }
        }
    }

    public static void lambda$getPsk$11(SupplicantStaNetworkHal supplicantStaNetworkHal, MutableBoolean mutableBoolean, SupplicantStatus supplicantStatus, byte[] bArr) {
        mutableBoolean.value = supplicantStatus.code == 0;
        if (mutableBoolean.value) {
            supplicantStaNetworkHal.mPsk = bArr;
        } else {
            supplicantStaNetworkHal.checkStatusAndLogFailure(supplicantStatus, "getPsk");
        }
    }

    private boolean getWepKey(int i) {
        synchronized (this.mLock) {
            if (!checkISupplicantStaNetworkAndLogFailure("keyIdx")) {
                return false;
            }
            try {
                final MutableBoolean mutableBoolean = new MutableBoolean(false);
                this.mISupplicantStaNetwork.getWepKey(i, new ISupplicantStaNetwork.getWepKeyCallback() {
                    @Override
                    public final void onValues(SupplicantStatus supplicantStatus, ArrayList arrayList) {
                        SupplicantStaNetworkHal.lambda$getWepKey$12(this.f$0, mutableBoolean, supplicantStatus, arrayList);
                    }
                });
                return mutableBoolean.value;
            } catch (RemoteException e) {
                handleRemoteException(e, "keyIdx");
                return false;
            }
        }
    }

    public static void lambda$getWepKey$12(SupplicantStaNetworkHal supplicantStaNetworkHal, MutableBoolean mutableBoolean, SupplicantStatus supplicantStatus, ArrayList arrayList) {
        mutableBoolean.value = supplicantStatus.code == 0;
        if (mutableBoolean.value) {
            supplicantStaNetworkHal.mWepKey = arrayList;
            return;
        }
        Log.e(TAG, "keyIdx,  failed: " + supplicantStatus.debugMessage);
    }

    private boolean getWepTxKeyIdx() {
        synchronized (this.mLock) {
            if (!checkISupplicantStaNetworkAndLogFailure("getWepTxKeyIdx")) {
                return false;
            }
            try {
                final MutableBoolean mutableBoolean = new MutableBoolean(false);
                this.mISupplicantStaNetwork.getWepTxKeyIdx(new ISupplicantStaNetwork.getWepTxKeyIdxCallback() {
                    @Override
                    public final void onValues(SupplicantStatus supplicantStatus, int i) {
                        SupplicantStaNetworkHal.lambda$getWepTxKeyIdx$13(this.f$0, mutableBoolean, supplicantStatus, i);
                    }
                });
                return mutableBoolean.value;
            } catch (RemoteException e) {
                handleRemoteException(e, "getWepTxKeyIdx");
                return false;
            }
        }
    }

    public static void lambda$getWepTxKeyIdx$13(SupplicantStaNetworkHal supplicantStaNetworkHal, MutableBoolean mutableBoolean, SupplicantStatus supplicantStatus, int i) {
        mutableBoolean.value = supplicantStatus.code == 0;
        if (mutableBoolean.value) {
            supplicantStaNetworkHal.mWepTxKeyIdx = i;
        } else {
            supplicantStaNetworkHal.checkStatusAndLogFailure(supplicantStatus, "getWepTxKeyIdx");
        }
    }

    private boolean getRequirePmf() {
        synchronized (this.mLock) {
            if (!checkISupplicantStaNetworkAndLogFailure("getRequirePmf")) {
                return false;
            }
            try {
                final MutableBoolean mutableBoolean = new MutableBoolean(false);
                this.mISupplicantStaNetwork.getRequirePmf(new ISupplicantStaNetwork.getRequirePmfCallback() {
                    @Override
                    public final void onValues(SupplicantStatus supplicantStatus, boolean z) {
                        SupplicantStaNetworkHal.lambda$getRequirePmf$14(this.f$0, mutableBoolean, supplicantStatus, z);
                    }
                });
                return mutableBoolean.value;
            } catch (RemoteException e) {
                handleRemoteException(e, "getRequirePmf");
                return false;
            }
        }
    }

    public static void lambda$getRequirePmf$14(SupplicantStaNetworkHal supplicantStaNetworkHal, MutableBoolean mutableBoolean, SupplicantStatus supplicantStatus, boolean z) {
        mutableBoolean.value = supplicantStatus.code == 0;
        if (mutableBoolean.value) {
            supplicantStaNetworkHal.mRequirePmf = z;
        } else {
            supplicantStaNetworkHal.checkStatusAndLogFailure(supplicantStatus, "getRequirePmf");
        }
    }

    private boolean getEapMethod() {
        synchronized (this.mLock) {
            if (!checkISupplicantStaNetworkAndLogFailure("getEapMethod")) {
                return false;
            }
            try {
                final MutableBoolean mutableBoolean = new MutableBoolean(false);
                this.mISupplicantStaNetwork.getEapMethod(new ISupplicantStaNetwork.getEapMethodCallback() {
                    @Override
                    public final void onValues(SupplicantStatus supplicantStatus, int i) {
                        SupplicantStaNetworkHal.lambda$getEapMethod$15(this.f$0, mutableBoolean, supplicantStatus, i);
                    }
                });
                return mutableBoolean.value;
            } catch (RemoteException e) {
                handleRemoteException(e, "getEapMethod");
                return false;
            }
        }
    }

    public static void lambda$getEapMethod$15(SupplicantStaNetworkHal supplicantStaNetworkHal, MutableBoolean mutableBoolean, SupplicantStatus supplicantStatus, int i) {
        mutableBoolean.value = supplicantStatus.code == 0;
        if (mutableBoolean.value) {
            supplicantStaNetworkHal.mEapMethod = i;
        } else {
            supplicantStaNetworkHal.checkStatusAndLogFailure(supplicantStatus, "getEapMethod");
        }
    }

    private boolean getEapPhase2Method() {
        synchronized (this.mLock) {
            if (!checkISupplicantStaNetworkAndLogFailure("getEapPhase2Method")) {
                return false;
            }
            try {
                final MutableBoolean mutableBoolean = new MutableBoolean(false);
                this.mISupplicantStaNetwork.getEapPhase2Method(new ISupplicantStaNetwork.getEapPhase2MethodCallback() {
                    @Override
                    public final void onValues(SupplicantStatus supplicantStatus, int i) {
                        SupplicantStaNetworkHal.lambda$getEapPhase2Method$16(this.f$0, mutableBoolean, supplicantStatus, i);
                    }
                });
                return mutableBoolean.value;
            } catch (RemoteException e) {
                handleRemoteException(e, "getEapPhase2Method");
                return false;
            }
        }
    }

    public static void lambda$getEapPhase2Method$16(SupplicantStaNetworkHal supplicantStaNetworkHal, MutableBoolean mutableBoolean, SupplicantStatus supplicantStatus, int i) {
        mutableBoolean.value = supplicantStatus.code == 0;
        if (mutableBoolean.value) {
            supplicantStaNetworkHal.mEapPhase2Method = i;
        } else {
            supplicantStaNetworkHal.checkStatusAndLogFailure(supplicantStatus, "getEapPhase2Method");
        }
    }

    private boolean getEapIdentity() {
        synchronized (this.mLock) {
            if (!checkISupplicantStaNetworkAndLogFailure("getEapIdentity")) {
                return false;
            }
            try {
                final MutableBoolean mutableBoolean = new MutableBoolean(false);
                this.mISupplicantStaNetwork.getEapIdentity(new ISupplicantStaNetwork.getEapIdentityCallback() {
                    @Override
                    public final void onValues(SupplicantStatus supplicantStatus, ArrayList arrayList) {
                        SupplicantStaNetworkHal.lambda$getEapIdentity$17(this.f$0, mutableBoolean, supplicantStatus, arrayList);
                    }
                });
                return mutableBoolean.value;
            } catch (RemoteException e) {
                handleRemoteException(e, "getEapIdentity");
                return false;
            }
        }
    }

    public static void lambda$getEapIdentity$17(SupplicantStaNetworkHal supplicantStaNetworkHal, MutableBoolean mutableBoolean, SupplicantStatus supplicantStatus, ArrayList arrayList) {
        mutableBoolean.value = supplicantStatus.code == 0;
        if (mutableBoolean.value) {
            supplicantStaNetworkHal.mEapIdentity = arrayList;
        } else {
            supplicantStaNetworkHal.checkStatusAndLogFailure(supplicantStatus, "getEapIdentity");
        }
    }

    private boolean getEapAnonymousIdentity() {
        synchronized (this.mLock) {
            if (!checkISupplicantStaNetworkAndLogFailure("getEapAnonymousIdentity")) {
                return false;
            }
            try {
                final MutableBoolean mutableBoolean = new MutableBoolean(false);
                this.mISupplicantStaNetwork.getEapAnonymousIdentity(new ISupplicantStaNetwork.getEapAnonymousIdentityCallback() {
                    @Override
                    public final void onValues(SupplicantStatus supplicantStatus, ArrayList arrayList) {
                        SupplicantStaNetworkHal.lambda$getEapAnonymousIdentity$18(this.f$0, mutableBoolean, supplicantStatus, arrayList);
                    }
                });
                return mutableBoolean.value;
            } catch (RemoteException e) {
                handleRemoteException(e, "getEapAnonymousIdentity");
                return false;
            }
        }
    }

    public static void lambda$getEapAnonymousIdentity$18(SupplicantStaNetworkHal supplicantStaNetworkHal, MutableBoolean mutableBoolean, SupplicantStatus supplicantStatus, ArrayList arrayList) {
        mutableBoolean.value = supplicantStatus.code == 0;
        if (mutableBoolean.value) {
            supplicantStaNetworkHal.mEapAnonymousIdentity = arrayList;
        } else {
            supplicantStaNetworkHal.checkStatusAndLogFailure(supplicantStatus, "getEapAnonymousIdentity");
        }
    }

    public String fetchEapAnonymousIdentity() {
        synchronized (this.mLock) {
            if (!getEapAnonymousIdentity()) {
                return null;
            }
            return NativeUtil.stringFromByteArrayList(this.mEapAnonymousIdentity);
        }
    }

    private boolean getEapPassword() {
        synchronized (this.mLock) {
            if (!checkISupplicantStaNetworkAndLogFailure("getEapPassword")) {
                return false;
            }
            try {
                final MutableBoolean mutableBoolean = new MutableBoolean(false);
                this.mISupplicantStaNetwork.getEapPassword(new ISupplicantStaNetwork.getEapPasswordCallback() {
                    @Override
                    public final void onValues(SupplicantStatus supplicantStatus, ArrayList arrayList) {
                        SupplicantStaNetworkHal.lambda$getEapPassword$19(this.f$0, mutableBoolean, supplicantStatus, arrayList);
                    }
                });
                return mutableBoolean.value;
            } catch (RemoteException e) {
                handleRemoteException(e, "getEapPassword");
                return false;
            }
        }
    }

    public static void lambda$getEapPassword$19(SupplicantStaNetworkHal supplicantStaNetworkHal, MutableBoolean mutableBoolean, SupplicantStatus supplicantStatus, ArrayList arrayList) {
        mutableBoolean.value = supplicantStatus.code == 0;
        if (mutableBoolean.value) {
            supplicantStaNetworkHal.mEapPassword = arrayList;
        } else {
            supplicantStaNetworkHal.checkStatusAndLogFailure(supplicantStatus, "getEapPassword");
        }
    }

    private boolean getEapCACert() {
        synchronized (this.mLock) {
            if (!checkISupplicantStaNetworkAndLogFailure("getEapCACert")) {
                return false;
            }
            try {
                final MutableBoolean mutableBoolean = new MutableBoolean(false);
                this.mISupplicantStaNetwork.getEapCACert(new ISupplicantStaNetwork.getEapCACertCallback() {
                    @Override
                    public final void onValues(SupplicantStatus supplicantStatus, String str) {
                        SupplicantStaNetworkHal.lambda$getEapCACert$20(this.f$0, mutableBoolean, supplicantStatus, str);
                    }
                });
                return mutableBoolean.value;
            } catch (RemoteException e) {
                handleRemoteException(e, "getEapCACert");
                return false;
            }
        }
    }

    public static void lambda$getEapCACert$20(SupplicantStaNetworkHal supplicantStaNetworkHal, MutableBoolean mutableBoolean, SupplicantStatus supplicantStatus, String str) {
        mutableBoolean.value = supplicantStatus.code == 0;
        if (mutableBoolean.value) {
            supplicantStaNetworkHal.mEapCACert = str;
        } else {
            supplicantStaNetworkHal.checkStatusAndLogFailure(supplicantStatus, "getEapCACert");
        }
    }

    private boolean getEapCAPath() {
        synchronized (this.mLock) {
            if (!checkISupplicantStaNetworkAndLogFailure("getEapCAPath")) {
                return false;
            }
            try {
                final MutableBoolean mutableBoolean = new MutableBoolean(false);
                this.mISupplicantStaNetwork.getEapCAPath(new ISupplicantStaNetwork.getEapCAPathCallback() {
                    @Override
                    public final void onValues(SupplicantStatus supplicantStatus, String str) {
                        SupplicantStaNetworkHal.lambda$getEapCAPath$21(this.f$0, mutableBoolean, supplicantStatus, str);
                    }
                });
                return mutableBoolean.value;
            } catch (RemoteException e) {
                handleRemoteException(e, "getEapCAPath");
                return false;
            }
        }
    }

    public static void lambda$getEapCAPath$21(SupplicantStaNetworkHal supplicantStaNetworkHal, MutableBoolean mutableBoolean, SupplicantStatus supplicantStatus, String str) {
        mutableBoolean.value = supplicantStatus.code == 0;
        if (mutableBoolean.value) {
            supplicantStaNetworkHal.mEapCAPath = str;
        } else {
            supplicantStaNetworkHal.checkStatusAndLogFailure(supplicantStatus, "getEapCAPath");
        }
    }

    private boolean getEapClientCert() {
        synchronized (this.mLock) {
            if (!checkISupplicantStaNetworkAndLogFailure("getEapClientCert")) {
                return false;
            }
            try {
                final MutableBoolean mutableBoolean = new MutableBoolean(false);
                this.mISupplicantStaNetwork.getEapClientCert(new ISupplicantStaNetwork.getEapClientCertCallback() {
                    @Override
                    public final void onValues(SupplicantStatus supplicantStatus, String str) {
                        SupplicantStaNetworkHal.lambda$getEapClientCert$22(this.f$0, mutableBoolean, supplicantStatus, str);
                    }
                });
                return mutableBoolean.value;
            } catch (RemoteException e) {
                handleRemoteException(e, "getEapClientCert");
                return false;
            }
        }
    }

    public static void lambda$getEapClientCert$22(SupplicantStaNetworkHal supplicantStaNetworkHal, MutableBoolean mutableBoolean, SupplicantStatus supplicantStatus, String str) {
        mutableBoolean.value = supplicantStatus.code == 0;
        if (mutableBoolean.value) {
            supplicantStaNetworkHal.mEapClientCert = str;
        } else {
            supplicantStaNetworkHal.checkStatusAndLogFailure(supplicantStatus, "getEapClientCert");
        }
    }

    private boolean getEapPrivateKeyId() {
        synchronized (this.mLock) {
            if (!checkISupplicantStaNetworkAndLogFailure("getEapPrivateKeyId")) {
                return false;
            }
            try {
                final MutableBoolean mutableBoolean = new MutableBoolean(false);
                this.mISupplicantStaNetwork.getEapPrivateKeyId(new ISupplicantStaNetwork.getEapPrivateKeyIdCallback() {
                    @Override
                    public final void onValues(SupplicantStatus supplicantStatus, String str) {
                        SupplicantStaNetworkHal.lambda$getEapPrivateKeyId$23(this.f$0, mutableBoolean, supplicantStatus, str);
                    }
                });
                return mutableBoolean.value;
            } catch (RemoteException e) {
                handleRemoteException(e, "getEapPrivateKeyId");
                return false;
            }
        }
    }

    public static void lambda$getEapPrivateKeyId$23(SupplicantStaNetworkHal supplicantStaNetworkHal, MutableBoolean mutableBoolean, SupplicantStatus supplicantStatus, String str) {
        mutableBoolean.value = supplicantStatus.code == 0;
        if (mutableBoolean.value) {
            supplicantStaNetworkHal.mEapPrivateKeyId = str;
        } else {
            supplicantStaNetworkHal.checkStatusAndLogFailure(supplicantStatus, "getEapPrivateKeyId");
        }
    }

    private boolean getEapSubjectMatch() {
        synchronized (this.mLock) {
            if (!checkISupplicantStaNetworkAndLogFailure("getEapSubjectMatch")) {
                return false;
            }
            try {
                final MutableBoolean mutableBoolean = new MutableBoolean(false);
                this.mISupplicantStaNetwork.getEapSubjectMatch(new ISupplicantStaNetwork.getEapSubjectMatchCallback() {
                    @Override
                    public final void onValues(SupplicantStatus supplicantStatus, String str) {
                        SupplicantStaNetworkHal.lambda$getEapSubjectMatch$24(this.f$0, mutableBoolean, supplicantStatus, str);
                    }
                });
                return mutableBoolean.value;
            } catch (RemoteException e) {
                handleRemoteException(e, "getEapSubjectMatch");
                return false;
            }
        }
    }

    public static void lambda$getEapSubjectMatch$24(SupplicantStaNetworkHal supplicantStaNetworkHal, MutableBoolean mutableBoolean, SupplicantStatus supplicantStatus, String str) {
        mutableBoolean.value = supplicantStatus.code == 0;
        if (mutableBoolean.value) {
            supplicantStaNetworkHal.mEapSubjectMatch = str;
        } else {
            supplicantStaNetworkHal.checkStatusAndLogFailure(supplicantStatus, "getEapSubjectMatch");
        }
    }

    private boolean getEapAltSubjectMatch() {
        synchronized (this.mLock) {
            if (!checkISupplicantStaNetworkAndLogFailure("getEapAltSubjectMatch")) {
                return false;
            }
            try {
                final MutableBoolean mutableBoolean = new MutableBoolean(false);
                this.mISupplicantStaNetwork.getEapAltSubjectMatch(new ISupplicantStaNetwork.getEapAltSubjectMatchCallback() {
                    @Override
                    public final void onValues(SupplicantStatus supplicantStatus, String str) {
                        SupplicantStaNetworkHal.lambda$getEapAltSubjectMatch$25(this.f$0, mutableBoolean, supplicantStatus, str);
                    }
                });
                return mutableBoolean.value;
            } catch (RemoteException e) {
                handleRemoteException(e, "getEapAltSubjectMatch");
                return false;
            }
        }
    }

    public static void lambda$getEapAltSubjectMatch$25(SupplicantStaNetworkHal supplicantStaNetworkHal, MutableBoolean mutableBoolean, SupplicantStatus supplicantStatus, String str) {
        mutableBoolean.value = supplicantStatus.code == 0;
        if (mutableBoolean.value) {
            supplicantStaNetworkHal.mEapAltSubjectMatch = str;
        } else {
            supplicantStaNetworkHal.checkStatusAndLogFailure(supplicantStatus, "getEapAltSubjectMatch");
        }
    }

    private boolean getEapEngine() {
        synchronized (this.mLock) {
            if (!checkISupplicantStaNetworkAndLogFailure("getEapEngine")) {
                return false;
            }
            try {
                final MutableBoolean mutableBoolean = new MutableBoolean(false);
                this.mISupplicantStaNetwork.getEapEngine(new ISupplicantStaNetwork.getEapEngineCallback() {
                    @Override
                    public final void onValues(SupplicantStatus supplicantStatus, boolean z) {
                        SupplicantStaNetworkHal.lambda$getEapEngine$26(this.f$0, mutableBoolean, supplicantStatus, z);
                    }
                });
                return mutableBoolean.value;
            } catch (RemoteException e) {
                handleRemoteException(e, "getEapEngine");
                return false;
            }
        }
    }

    public static void lambda$getEapEngine$26(SupplicantStaNetworkHal supplicantStaNetworkHal, MutableBoolean mutableBoolean, SupplicantStatus supplicantStatus, boolean z) {
        mutableBoolean.value = supplicantStatus.code == 0;
        if (mutableBoolean.value) {
            supplicantStaNetworkHal.mEapEngine = z;
        } else {
            supplicantStaNetworkHal.checkStatusAndLogFailure(supplicantStatus, "getEapEngine");
        }
    }

    private boolean getEapEngineID() {
        synchronized (this.mLock) {
            if (!checkISupplicantStaNetworkAndLogFailure("getEapEngineID")) {
                return false;
            }
            try {
                final MutableBoolean mutableBoolean = new MutableBoolean(false);
                this.mISupplicantStaNetwork.getEapEngineID(new ISupplicantStaNetwork.getEapEngineIDCallback() {
                    @Override
                    public final void onValues(SupplicantStatus supplicantStatus, String str) {
                        SupplicantStaNetworkHal.lambda$getEapEngineID$27(this.f$0, mutableBoolean, supplicantStatus, str);
                    }
                });
                return mutableBoolean.value;
            } catch (RemoteException e) {
                handleRemoteException(e, "getEapEngineID");
                return false;
            }
        }
    }

    public static void lambda$getEapEngineID$27(SupplicantStaNetworkHal supplicantStaNetworkHal, MutableBoolean mutableBoolean, SupplicantStatus supplicantStatus, String str) {
        mutableBoolean.value = supplicantStatus.code == 0;
        if (mutableBoolean.value) {
            supplicantStaNetworkHal.mEapEngineID = str;
        } else {
            supplicantStaNetworkHal.checkStatusAndLogFailure(supplicantStatus, "getEapEngineID");
        }
    }

    private boolean getEapDomainSuffixMatch() {
        synchronized (this.mLock) {
            if (!checkISupplicantStaNetworkAndLogFailure("getEapDomainSuffixMatch")) {
                return false;
            }
            try {
                final MutableBoolean mutableBoolean = new MutableBoolean(false);
                this.mISupplicantStaNetwork.getEapDomainSuffixMatch(new ISupplicantStaNetwork.getEapDomainSuffixMatchCallback() {
                    @Override
                    public final void onValues(SupplicantStatus supplicantStatus, String str) {
                        SupplicantStaNetworkHal.lambda$getEapDomainSuffixMatch$28(this.f$0, mutableBoolean, supplicantStatus, str);
                    }
                });
                return mutableBoolean.value;
            } catch (RemoteException e) {
                handleRemoteException(e, "getEapDomainSuffixMatch");
                return false;
            }
        }
    }

    public static void lambda$getEapDomainSuffixMatch$28(SupplicantStaNetworkHal supplicantStaNetworkHal, MutableBoolean mutableBoolean, SupplicantStatus supplicantStatus, String str) {
        mutableBoolean.value = supplicantStatus.code == 0;
        if (mutableBoolean.value) {
            supplicantStaNetworkHal.mEapDomainSuffixMatch = str;
        } else {
            supplicantStaNetworkHal.checkStatusAndLogFailure(supplicantStatus, "getEapDomainSuffixMatch");
        }
    }

    private boolean getIdStr() {
        synchronized (this.mLock) {
            if (!checkISupplicantStaNetworkAndLogFailure("getIdStr")) {
                return false;
            }
            try {
                final MutableBoolean mutableBoolean = new MutableBoolean(false);
                this.mISupplicantStaNetwork.getIdStr(new ISupplicantStaNetwork.getIdStrCallback() {
                    @Override
                    public final void onValues(SupplicantStatus supplicantStatus, String str) {
                        SupplicantStaNetworkHal.lambda$getIdStr$29(this.f$0, mutableBoolean, supplicantStatus, str);
                    }
                });
                return mutableBoolean.value;
            } catch (RemoteException e) {
                handleRemoteException(e, "getIdStr");
                return false;
            }
        }
    }

    public static void lambda$getIdStr$29(SupplicantStaNetworkHal supplicantStaNetworkHal, MutableBoolean mutableBoolean, SupplicantStatus supplicantStatus, String str) {
        mutableBoolean.value = supplicantStatus.code == 0;
        if (mutableBoolean.value) {
            supplicantStaNetworkHal.mIdStr = str;
        } else {
            supplicantStaNetworkHal.checkStatusAndLogFailure(supplicantStatus, "getIdStr");
        }
    }

    private boolean enable(boolean z) {
        synchronized (this.mLock) {
            if (!checkISupplicantStaNetworkAndLogFailure("enable")) {
                return false;
            }
            try {
                return checkStatusAndLogFailure(this.mISupplicantStaNetwork.enable(z), "enable");
            } catch (RemoteException e) {
                handleRemoteException(e, "enable");
                return false;
            }
        }
    }

    private boolean disable() {
        synchronized (this.mLock) {
            if (!checkISupplicantStaNetworkAndLogFailure("disable")) {
                return false;
            }
            try {
                return checkStatusAndLogFailure(this.mISupplicantStaNetwork.disable(), "disable");
            } catch (RemoteException e) {
                handleRemoteException(e, "disable");
                return false;
            }
        }
    }

    public boolean select() {
        synchronized (this.mLock) {
            if (!checkISupplicantStaNetworkAndLogFailure("select")) {
                return false;
            }
            try {
                return checkStatusAndLogFailure(this.mISupplicantStaNetwork.select(), "select");
            } catch (RemoteException e) {
                handleRemoteException(e, "select");
                return false;
            }
        }
    }

    public boolean sendNetworkEapSimGsmAuthResponse(String str) {
        synchronized (this.mLock) {
            try {
                try {
                    Matcher matcher = GSM_AUTH_RESPONSE_PARAMS_PATTERN.matcher(str);
                    ArrayList<ISupplicantStaNetwork.NetworkResponseEapSimGsmAuthParams> arrayList = new ArrayList<>();
                    while (matcher.find()) {
                        if (matcher.groupCount() != 2) {
                            Log.e(TAG, "Malformed gsm auth response params: " + str);
                            return false;
                        }
                        ISupplicantStaNetwork.NetworkResponseEapSimGsmAuthParams networkResponseEapSimGsmAuthParams = new ISupplicantStaNetwork.NetworkResponseEapSimGsmAuthParams();
                        byte[] bArrHexStringToByteArray = NativeUtil.hexStringToByteArray(matcher.group(1));
                        if (bArrHexStringToByteArray != null && bArrHexStringToByteArray.length == networkResponseEapSimGsmAuthParams.kc.length) {
                            byte[] bArrHexStringToByteArray2 = NativeUtil.hexStringToByteArray(matcher.group(2));
                            if (bArrHexStringToByteArray2 != null && bArrHexStringToByteArray2.length == networkResponseEapSimGsmAuthParams.sres.length) {
                                System.arraycopy(bArrHexStringToByteArray, 0, networkResponseEapSimGsmAuthParams.kc, 0, networkResponseEapSimGsmAuthParams.kc.length);
                                System.arraycopy(bArrHexStringToByteArray2, 0, networkResponseEapSimGsmAuthParams.sres, 0, networkResponseEapSimGsmAuthParams.sres.length);
                                arrayList.add(networkResponseEapSimGsmAuthParams);
                            }
                            Log.e(TAG, "Invalid sres value: " + matcher.group(2));
                            return false;
                        }
                        Log.e(TAG, "Invalid kc value: " + matcher.group(1));
                        return false;
                    }
                    if (arrayList.size() <= 3 && arrayList.size() >= 2) {
                        return sendNetworkEapSimGsmAuthResponse(arrayList);
                    }
                    Log.e(TAG, "Malformed gsm auth response params: " + str);
                    return false;
                } catch (IllegalArgumentException e) {
                    Log.e(TAG, "Illegal argument " + str, e);
                    return false;
                }
            } catch (Throwable th) {
                throw th;
            }
        }
    }

    private boolean sendNetworkEapSimGsmAuthResponse(ArrayList<ISupplicantStaNetwork.NetworkResponseEapSimGsmAuthParams> arrayList) {
        synchronized (this.mLock) {
            if (!checkISupplicantStaNetworkAndLogFailure("sendNetworkEapSimGsmAuthResponse")) {
                return false;
            }
            try {
                return checkStatusAndLogFailure(this.mISupplicantStaNetwork.sendNetworkEapSimGsmAuthResponse(arrayList), "sendNetworkEapSimGsmAuthResponse");
            } catch (RemoteException e) {
                handleRemoteException(e, "sendNetworkEapSimGsmAuthResponse");
                return false;
            }
        }
    }

    public boolean sendNetworkEapSimGsmAuthFailure() {
        synchronized (this.mLock) {
            if (!checkISupplicantStaNetworkAndLogFailure("sendNetworkEapSimGsmAuthFailure")) {
                return false;
            }
            try {
                return checkStatusAndLogFailure(this.mISupplicantStaNetwork.sendNetworkEapSimGsmAuthFailure(), "sendNetworkEapSimGsmAuthFailure");
            } catch (RemoteException e) {
                handleRemoteException(e, "sendNetworkEapSimGsmAuthFailure");
                return false;
            }
        }
    }

    public boolean sendNetworkEapSimUmtsAuthResponse(String str) {
        synchronized (this.mLock) {
            try {
                try {
                    Matcher matcher = UMTS_AUTH_RESPONSE_PARAMS_PATTERN.matcher(str);
                    if (matcher.find() && matcher.groupCount() == 3) {
                        ISupplicantStaNetwork.NetworkResponseEapSimUmtsAuthParams networkResponseEapSimUmtsAuthParams = new ISupplicantStaNetwork.NetworkResponseEapSimUmtsAuthParams();
                        byte[] bArrHexStringToByteArray = NativeUtil.hexStringToByteArray(matcher.group(1));
                        if (bArrHexStringToByteArray != null && bArrHexStringToByteArray.length == networkResponseEapSimUmtsAuthParams.ik.length) {
                            byte[] bArrHexStringToByteArray2 = NativeUtil.hexStringToByteArray(matcher.group(2));
                            if (bArrHexStringToByteArray2 != null && bArrHexStringToByteArray2.length == networkResponseEapSimUmtsAuthParams.ck.length) {
                                byte[] bArrHexStringToByteArray3 = NativeUtil.hexStringToByteArray(matcher.group(3));
                                if (bArrHexStringToByteArray3 != null && bArrHexStringToByteArray3.length != 0) {
                                    System.arraycopy(bArrHexStringToByteArray, 0, networkResponseEapSimUmtsAuthParams.ik, 0, networkResponseEapSimUmtsAuthParams.ik.length);
                                    System.arraycopy(bArrHexStringToByteArray2, 0, networkResponseEapSimUmtsAuthParams.ck, 0, networkResponseEapSimUmtsAuthParams.ck.length);
                                    for (byte b : bArrHexStringToByteArray3) {
                                        networkResponseEapSimUmtsAuthParams.res.add(Byte.valueOf(b));
                                    }
                                    return sendNetworkEapSimUmtsAuthResponse(networkResponseEapSimUmtsAuthParams);
                                }
                                Log.e(TAG, "Invalid res value: " + matcher.group(3));
                                return false;
                            }
                            Log.e(TAG, "Invalid ck value: " + matcher.group(2));
                            return false;
                        }
                        Log.e(TAG, "Invalid ik value: " + matcher.group(1));
                        return false;
                    }
                    Log.e(TAG, "Malformed umts auth response params: " + str);
                    return false;
                } catch (IllegalArgumentException e) {
                    Log.e(TAG, "Illegal argument " + str, e);
                    return false;
                }
            } catch (Throwable th) {
                throw th;
            }
        }
    }

    private boolean sendNetworkEapSimUmtsAuthResponse(ISupplicantStaNetwork.NetworkResponseEapSimUmtsAuthParams networkResponseEapSimUmtsAuthParams) {
        synchronized (this.mLock) {
            if (!checkISupplicantStaNetworkAndLogFailure("sendNetworkEapSimUmtsAuthResponse")) {
                return false;
            }
            try {
                return checkStatusAndLogFailure(this.mISupplicantStaNetwork.sendNetworkEapSimUmtsAuthResponse(networkResponseEapSimUmtsAuthParams), "sendNetworkEapSimUmtsAuthResponse");
            } catch (RemoteException e) {
                handleRemoteException(e, "sendNetworkEapSimUmtsAuthResponse");
                return false;
            }
        }
    }

    public boolean sendNetworkEapSimUmtsAutsResponse(String str) {
        synchronized (this.mLock) {
            try {
                try {
                    Matcher matcher = UMTS_AUTS_RESPONSE_PARAMS_PATTERN.matcher(str);
                    if (matcher.find() && matcher.groupCount() == 1) {
                        byte[] bArrHexStringToByteArray = NativeUtil.hexStringToByteArray(matcher.group(1));
                        if (bArrHexStringToByteArray != null && bArrHexStringToByteArray.length == 14) {
                            return sendNetworkEapSimUmtsAutsResponse(bArrHexStringToByteArray);
                        }
                        Log.e(TAG, "Invalid auts value: " + matcher.group(1));
                        return false;
                    }
                    Log.e(TAG, "Malformed umts auts response params: " + str);
                    return false;
                } catch (IllegalArgumentException e) {
                    Log.e(TAG, "Illegal argument " + str, e);
                    return false;
                }
            } catch (Throwable th) {
                throw th;
            }
        }
    }

    private boolean sendNetworkEapSimUmtsAutsResponse(byte[] bArr) {
        synchronized (this.mLock) {
            if (!checkISupplicantStaNetworkAndLogFailure("sendNetworkEapSimUmtsAutsResponse")) {
                return false;
            }
            try {
                return checkStatusAndLogFailure(this.mISupplicantStaNetwork.sendNetworkEapSimUmtsAutsResponse(bArr), "sendNetworkEapSimUmtsAutsResponse");
            } catch (RemoteException e) {
                handleRemoteException(e, "sendNetworkEapSimUmtsAutsResponse");
                return false;
            }
        }
    }

    public boolean sendNetworkEapSimUmtsAuthFailure() {
        synchronized (this.mLock) {
            if (!checkISupplicantStaNetworkAndLogFailure("sendNetworkEapSimUmtsAuthFailure")) {
                return false;
            }
            try {
                return checkStatusAndLogFailure(this.mISupplicantStaNetwork.sendNetworkEapSimUmtsAuthFailure(), "sendNetworkEapSimUmtsAuthFailure");
            } catch (RemoteException e) {
                handleRemoteException(e, "sendNetworkEapSimUmtsAuthFailure");
                return false;
            }
        }
    }

    protected android.hardware.wifi.supplicant.V1_1.ISupplicantStaNetwork getSupplicantStaNetworkForV1_1Mockable() {
        if (this.mISupplicantStaNetwork == null) {
            return null;
        }
        return android.hardware.wifi.supplicant.V1_1.ISupplicantStaNetwork.castFrom((IHwInterface) this.mISupplicantStaNetwork);
    }

    public boolean sendNetworkEapIdentityResponse(String str, String str2) {
        boolean zSendNetworkEapIdentityResponse;
        synchronized (this.mLock) {
            try {
                try {
                    ArrayList<Byte> arrayListStringToByteArrayList = NativeUtil.stringToByteArrayList(str);
                    ArrayList<Byte> arrayListStringToByteArrayList2 = null;
                    if (!TextUtils.isEmpty(str2)) {
                        arrayListStringToByteArrayList2 = NativeUtil.stringToByteArrayList(str2);
                    }
                    zSendNetworkEapIdentityResponse = sendNetworkEapIdentityResponse(arrayListStringToByteArrayList, arrayListStringToByteArrayList2);
                } catch (IllegalArgumentException e) {
                    Log.e(TAG, "Illegal argument " + str + "," + str2, e);
                    return false;
                }
            } catch (Throwable th) {
                throw th;
            }
        }
        return zSendNetworkEapIdentityResponse;
    }

    private boolean sendNetworkEapIdentityResponse(ArrayList<Byte> arrayList, ArrayList<Byte> arrayList2) {
        SupplicantStatus supplicantStatusSendNetworkEapIdentityResponse;
        synchronized (this.mLock) {
            if (!checkISupplicantStaNetworkAndLogFailure("sendNetworkEapIdentityResponse")) {
                return false;
            }
            try {
                android.hardware.wifi.supplicant.V1_1.ISupplicantStaNetwork supplicantStaNetworkForV1_1Mockable = getSupplicantStaNetworkForV1_1Mockable();
                if (supplicantStaNetworkForV1_1Mockable != null && arrayList2 != null) {
                    supplicantStatusSendNetworkEapIdentityResponse = supplicantStaNetworkForV1_1Mockable.sendNetworkEapIdentityResponse_1_1(arrayList, arrayList2);
                } else {
                    supplicantStatusSendNetworkEapIdentityResponse = this.mISupplicantStaNetwork.sendNetworkEapIdentityResponse(arrayList);
                }
                return checkStatusAndLogFailure(supplicantStatusSendNetworkEapIdentityResponse, "sendNetworkEapIdentityResponse");
            } catch (RemoteException e) {
                handleRemoteException(e, "sendNetworkEapIdentityResponse");
                return false;
            }
        }
    }

    public String getWpsNfcConfigurationToken() {
        synchronized (this.mLock) {
            ArrayList<Byte> wpsNfcConfigurationTokenInternal = getWpsNfcConfigurationTokenInternal();
            if (wpsNfcConfigurationTokenInternal == null) {
                return null;
            }
            return NativeUtil.hexStringFromByteArray(NativeUtil.byteArrayFromArrayList(wpsNfcConfigurationTokenInternal));
        }
    }

    private ArrayList<Byte> getWpsNfcConfigurationTokenInternal() {
        synchronized (this.mLock) {
            if (!checkISupplicantStaNetworkAndLogFailure("getWpsNfcConfigurationToken")) {
                return null;
            }
            final HidlSupport.Mutable mutable = new HidlSupport.Mutable();
            try {
                this.mISupplicantStaNetwork.getWpsNfcConfigurationToken(new ISupplicantStaNetwork.getWpsNfcConfigurationTokenCallback() {
                    @Override
                    public final void onValues(SupplicantStatus supplicantStatus, ArrayList arrayList) {
                        SupplicantStaNetworkHal.lambda$getWpsNfcConfigurationTokenInternal$30(this.f$0, mutable, supplicantStatus, arrayList);
                    }
                });
            } catch (RemoteException e) {
                handleRemoteException(e, "getWpsNfcConfigurationToken");
            }
            return (ArrayList) mutable.value;
        }
    }

    public static void lambda$getWpsNfcConfigurationTokenInternal$30(SupplicantStaNetworkHal supplicantStaNetworkHal, HidlSupport.Mutable mutable, SupplicantStatus supplicantStatus, ArrayList arrayList) {
        if (supplicantStaNetworkHal.checkStatusAndLogFailure(supplicantStatus, "getWpsNfcConfigurationToken")) {
            mutable.value = arrayList;
        }
    }

    private boolean checkStatusAndLogFailure(SupplicantStatus supplicantStatus, String str) {
        synchronized (this.mLock) {
            if (supplicantStatus.code != 0) {
                Log.e(TAG, "ISupplicantStaNetwork." + str + " failed: " + supplicantStatus);
                return false;
            }
            if (this.mVerboseLoggingEnabled) {
                Log.d(TAG, "ISupplicantStaNetwork." + str + " succeeded");
            }
            return true;
        }
    }

    private void logCallback(String str) {
        synchronized (this.mLock) {
            if (this.mVerboseLoggingEnabled) {
                Log.d(TAG, "ISupplicantStaNetworkCallback." + str + " received");
            }
        }
    }

    private boolean checkISupplicantStaNetworkAndLogFailure(String str) {
        synchronized (this.mLock) {
            if (this.mISupplicantStaNetwork == null) {
                Log.e(TAG, "Can't call " + str + ", ISupplicantStaNetwork is null");
                return false;
            }
            if (this.mVerboseLoggingEnabled) {
                Log.d(TAG, "Do ISupplicantStaNetwork." + str);
            }
            return true;
        }
    }

    private void handleRemoteException(RemoteException remoteException, String str) {
        synchronized (this.mLock) {
            this.mISupplicantStaNetwork = null;
            Log.e(TAG, "ISupplicantStaNetwork." + str + " failed with exception", remoteException);
        }
    }

    private BitSet addFastTransitionFlags(BitSet bitSet) {
        synchronized (this.mLock) {
            if (!this.mSystemSupportsFastBssTransition) {
                return bitSet;
            }
            BitSet bitSet2 = (BitSet) bitSet.clone();
            if (bitSet.get(1)) {
                bitSet2.set(6);
            }
            if (bitSet.get(2)) {
                bitSet2.set(7);
            }
            return bitSet2;
        }
    }

    private BitSet removeFastTransitionFlags(BitSet bitSet) {
        BitSet bitSet2;
        synchronized (this.mLock) {
            bitSet2 = (BitSet) bitSet.clone();
            bitSet2.clear(6);
            bitSet2.clear(7);
        }
        return bitSet2;
    }

    public static String createNetworkExtra(Map<String, String> map) {
        try {
            return URLEncoder.encode(new JSONObject(map).toString(), "UTF-8");
        } catch (UnsupportedEncodingException e) {
            Log.e(TAG, "Unable to serialize networkExtra: " + e.toString());
            return null;
        } catch (NullPointerException e2) {
            Log.e(TAG, "Unable to serialize networkExtra: " + e2.toString());
            return null;
        }
    }

    public static Map<String, String> parseNetworkExtra(String str) {
        if (TextUtils.isEmpty(str)) {
            return null;
        }
        try {
            JSONObject jSONObject = new JSONObject(URLDecoder.decode(str, "UTF-8"));
            HashMap map = new HashMap();
            Iterator<String> itKeys = jSONObject.keys();
            while (itKeys.hasNext()) {
                String next = itKeys.next();
                Object obj = jSONObject.get(next);
                if (obj instanceof String) {
                    map.put(next, (String) obj);
                }
            }
            return map;
        } catch (UnsupportedEncodingException e) {
            Log.e(TAG, "Unable to deserialize networkExtra: " + e.toString());
            return null;
        } catch (JSONException e2) {
            return null;
        }
    }

    private class SupplicantStaNetworkHalCallback extends ISupplicantStaNetworkCallback.Stub {
        private final int mFramewokNetworkId;
        private final String mSsid;

        SupplicantStaNetworkHalCallback(int i, String str) {
            this.mFramewokNetworkId = i;
            this.mSsid = str;
        }

        @Override
        public void onNetworkEapSimGsmAuthRequest(ISupplicantStaNetworkCallback.NetworkRequestEapSimGsmAuthParams networkRequestEapSimGsmAuthParams) {
            synchronized (SupplicantStaNetworkHal.this.mLock) {
                SupplicantStaNetworkHal.this.logCallback("onNetworkEapSimGsmAuthRequest");
                String[] strArr = new String[networkRequestEapSimGsmAuthParams.rands.size()];
                int i = 0;
                Iterator<byte[]> it = networkRequestEapSimGsmAuthParams.rands.iterator();
                while (it.hasNext()) {
                    strArr[i] = NativeUtil.hexStringFromByteArray(it.next());
                    i++;
                }
                SupplicantStaNetworkHal.this.mWifiMonitor.broadcastNetworkGsmAuthRequestEvent(SupplicantStaNetworkHal.this.mIfaceName, this.mFramewokNetworkId, this.mSsid, strArr);
            }
        }

        @Override
        public void onNetworkEapSimUmtsAuthRequest(ISupplicantStaNetworkCallback.NetworkRequestEapSimUmtsAuthParams networkRequestEapSimUmtsAuthParams) {
            synchronized (SupplicantStaNetworkHal.this.mLock) {
                SupplicantStaNetworkHal.this.logCallback("onNetworkEapSimUmtsAuthRequest");
                SupplicantStaNetworkHal.this.mWifiMonitor.broadcastNetworkUmtsAuthRequestEvent(SupplicantStaNetworkHal.this.mIfaceName, this.mFramewokNetworkId, this.mSsid, new String[]{NativeUtil.hexStringFromByteArray(networkRequestEapSimUmtsAuthParams.rand), NativeUtil.hexStringFromByteArray(networkRequestEapSimUmtsAuthParams.autn)});
            }
        }

        @Override
        public void onNetworkEapIdentityRequest() {
            synchronized (SupplicantStaNetworkHal.this.mLock) {
                SupplicantStaNetworkHal.this.logCallback("onNetworkEapIdentityRequest");
                SupplicantStaNetworkHal.this.mWifiMonitor.broadcastNetworkIdentityRequestEvent(SupplicantStaNetworkHal.this.mIfaceName, this.mFramewokNetworkId, this.mSsid);
            }
        }
    }
}
