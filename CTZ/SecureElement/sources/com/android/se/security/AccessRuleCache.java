package com.android.se.security;

import android.os.Build;
import android.util.Log;
import com.android.se.security.ChannelAccess;
import com.android.se.security.gpac.AID_REF_DO;
import com.android.se.security.gpac.AR_DO;
import com.android.se.security.gpac.Hash_REF_DO;
import com.android.se.security.gpac.REF_DO;
import java.io.PrintWriter;
import java.security.AccessControlException;
import java.security.cert.Certificate;
import java.security.cert.CertificateEncodingException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

public class AccessRuleCache {
    private static final boolean DEBUG = Build.IS_DEBUGGABLE;
    private final String mTag = "SecureElement-AccessRuleCache";
    private byte[] mRefreshTag = null;
    private Map<REF_DO, ChannelAccess> mRuleCache = new HashMap();

    private static AID_REF_DO getAidRefDo(byte[] bArr) {
        byte[] bArr2 = {0, 0, 0, 0, 0};
        if (bArr == null || Arrays.equals(bArr, bArr2)) {
            return new AID_REF_DO(AID_REF_DO.TAG_DEFAULT_APPLICATION);
        }
        return new AID_REF_DO(79, bArr);
    }

    private static ChannelAccess mapArDo2ChannelAccess(AR_DO ar_do) {
        ChannelAccess.ACCESS access;
        ChannelAccess channelAccess = new ChannelAccess();
        if (ar_do.getApduArDo() != null) {
            channelAccess.setAccess(ChannelAccess.ACCESS.ALLOWED, "");
            channelAccess.setUseApduFilter(false);
            if (ar_do.getApduArDo().isApduAllowed()) {
                ArrayList<byte[]> apduHeaderList = ar_do.getApduArDo().getApduHeaderList();
                ArrayList<byte[]> filterMaskList = ar_do.getApduArDo().getFilterMaskList();
                if (apduHeaderList != null && filterMaskList != null && apduHeaderList.size() > 0 && apduHeaderList.size() == filterMaskList.size()) {
                    ApduFilter[] apduFilterArr = new ApduFilter[apduHeaderList.size()];
                    for (int i = 0; i < apduHeaderList.size(); i++) {
                        apduFilterArr[i] = new ApduFilter(apduHeaderList.get(i), filterMaskList.get(i));
                    }
                    channelAccess.setUseApduFilter(true);
                    channelAccess.setApduFilter(apduFilterArr);
                } else {
                    channelAccess.setApduAccess(ChannelAccess.ACCESS.ALLOWED);
                }
            } else {
                channelAccess.setApduAccess(ChannelAccess.ACCESS.DENIED);
            }
        } else {
            channelAccess.setAccess(ChannelAccess.ACCESS.DENIED, "No APDU access rule available.!");
        }
        if (ar_do.getNfcArDo() != null) {
            if (ar_do.getNfcArDo().isNfcAllowed()) {
                access = ChannelAccess.ACCESS.ALLOWED;
            } else {
                access = ChannelAccess.ACCESS.DENIED;
            }
            channelAccess.setNFCEventAccess(access);
        } else {
            channelAccess.setNFCEventAccess(channelAccess.getApduAccess());
        }
        return channelAccess;
    }

    public void reset() {
        this.mRefreshTag = null;
        this.mRuleCache.clear();
    }

    public void clearCache() {
        this.mRuleCache.clear();
    }

    public void putWithMerge(REF_DO ref_do, AR_DO ar_do) {
        putWithMerge(ref_do, mapArDo2ChannelAccess(ar_do));
    }

    public void putWithMerge(REF_DO ref_do, ChannelAccess channelAccess) {
        if (this.mRuleCache.containsKey(ref_do)) {
            ChannelAccess channelAccess2 = this.mRuleCache.get(ref_do);
            if (channelAccess.getAccess() == ChannelAccess.ACCESS.DENIED || channelAccess2.getAccess() == ChannelAccess.ACCESS.DENIED) {
                channelAccess2.setAccess(ChannelAccess.ACCESS.DENIED, channelAccess.getReason());
            } else if (channelAccess.getAccess() == ChannelAccess.ACCESS.UNDEFINED && channelAccess2.getAccess() != ChannelAccess.ACCESS.UNDEFINED) {
                channelAccess2.setAccess(channelAccess2.getAccess(), channelAccess2.getReason());
            } else if (channelAccess.getAccess() != ChannelAccess.ACCESS.UNDEFINED && channelAccess2.getAccess() == ChannelAccess.ACCESS.UNDEFINED) {
                channelAccess2.setAccess(channelAccess.getAccess(), channelAccess.getReason());
            } else {
                channelAccess2.setAccess(ChannelAccess.ACCESS.ALLOWED, channelAccess2.getReason());
            }
            if (channelAccess.getNFCEventAccess() == ChannelAccess.ACCESS.DENIED || channelAccess2.getNFCEventAccess() == ChannelAccess.ACCESS.DENIED) {
                channelAccess2.setNFCEventAccess(ChannelAccess.ACCESS.DENIED);
            } else if (channelAccess.getNFCEventAccess() == ChannelAccess.ACCESS.UNDEFINED && channelAccess2.getNFCEventAccess() != ChannelAccess.ACCESS.UNDEFINED) {
                channelAccess2.setNFCEventAccess(channelAccess2.getNFCEventAccess());
            } else if (channelAccess.getNFCEventAccess() != ChannelAccess.ACCESS.UNDEFINED && channelAccess2.getNFCEventAccess() == ChannelAccess.ACCESS.UNDEFINED) {
                channelAccess2.setNFCEventAccess(channelAccess.getNFCEventAccess());
            } else {
                channelAccess2.setNFCEventAccess(ChannelAccess.ACCESS.ALLOWED);
            }
            if (channelAccess.getApduAccess() == ChannelAccess.ACCESS.DENIED || channelAccess2.getApduAccess() == ChannelAccess.ACCESS.DENIED) {
                channelAccess2.setApduAccess(ChannelAccess.ACCESS.DENIED);
            } else if (channelAccess.getApduAccess() == ChannelAccess.ACCESS.UNDEFINED && channelAccess2.getApduAccess() != ChannelAccess.ACCESS.UNDEFINED) {
                channelAccess2.setApduAccess(channelAccess2.getApduAccess());
            } else if (channelAccess.getApduAccess() == ChannelAccess.ACCESS.UNDEFINED && channelAccess2.getApduAccess() == ChannelAccess.ACCESS.UNDEFINED && !channelAccess.isUseApduFilter()) {
                channelAccess2.setApduAccess(ChannelAccess.ACCESS.DENIED);
            } else if (channelAccess.getApduAccess() != ChannelAccess.ACCESS.UNDEFINED && channelAccess2.getApduAccess() == ChannelAccess.ACCESS.UNDEFINED) {
                channelAccess2.setApduAccess(channelAccess.getApduAccess());
            } else {
                channelAccess2.setApduAccess(ChannelAccess.ACCESS.ALLOWED);
            }
            int i = 0;
            if (channelAccess2.getApduAccess() == ChannelAccess.ACCESS.ALLOWED || channelAccess2.getApduAccess() == ChannelAccess.ACCESS.UNDEFINED) {
                Log.i("SecureElement-AccessRuleCache", "Merged Access Rule:  APDU filter together");
                if (channelAccess.isUseApduFilter()) {
                    channelAccess2.setUseApduFilter(true);
                    ApduFilter[] apduFilter = channelAccess2.getApduFilter();
                    ApduFilter[] apduFilter2 = channelAccess.getApduFilter();
                    if (apduFilter == null || apduFilter.length == 0) {
                        channelAccess2.setApduFilter(apduFilter2);
                    } else if (apduFilter2 == null || apduFilter2.length == 0) {
                        channelAccess2.setApduFilter(apduFilter);
                    } else {
                        ApduFilter[] apduFilterArr = new ApduFilter[apduFilter.length + apduFilter2.length];
                        int length = apduFilter.length;
                        int i2 = 0;
                        int i3 = 0;
                        while (i2 < length) {
                            apduFilterArr[i3] = apduFilter[i2];
                            i2++;
                            i3++;
                        }
                        int length2 = apduFilter2.length;
                        while (i < length2) {
                            apduFilterArr[i3] = apduFilter2[i];
                            i++;
                            i3++;
                        }
                        channelAccess2.setApduFilter(apduFilterArr);
                    }
                }
            } else {
                channelAccess2.setUseApduFilter(false);
                channelAccess2.setApduFilter(null);
            }
            if (DEBUG) {
                Log.i("SecureElement-AccessRuleCache", "Merged Access Rule: " + ref_do.toString() + ", " + channelAccess2.toString());
                return;
            }
            return;
        }
        if (DEBUG) {
            Log.i("SecureElement-AccessRuleCache", "Add Access Rule: " + ref_do.toString() + ", " + channelAccess.toString());
        }
        this.mRuleCache.put(ref_do, channelAccess);
    }

    public ChannelAccess findAccessRule(byte[] bArr, Certificate[] certificateArr) throws AccessControlException {
        AID_REF_DO aidRefDo = getAidRefDo(bArr);
        for (Certificate certificate : certificateArr) {
            try {
                REF_DO ref_do = new REF_DO(aidRefDo, new Hash_REF_DO(AccessControlEnforcer.getAppCertHash(certificate)));
                if (this.mRuleCache.containsKey(ref_do)) {
                    ChannelAccess channelAccess = this.mRuleCache.get(ref_do);
                    if (channelAccess.getApduAccess() == ChannelAccess.ACCESS.UNDEFINED) {
                        channelAccess.setApduAccess(ChannelAccess.ACCESS.DENIED);
                    }
                    if (channelAccess.getNFCEventAccess() == ChannelAccess.ACCESS.UNDEFINED && channelAccess.getApduAccess() != ChannelAccess.ACCESS.UNDEFINED) {
                        channelAccess.setNFCEventAccess(channelAccess.getApduAccess());
                    }
                    if (DEBUG) {
                        Log.i("SecureElement-AccessRuleCache", "findAccessRule() " + ref_do.toString() + ", " + this.mRuleCache.get(ref_do).toString());
                    }
                    return this.mRuleCache.get(ref_do);
                }
            } catch (CertificateEncodingException e) {
                throw new AccessControlException("Problem with Application Certificate.");
            }
        }
        if (searchForRulesWithSpecificAidButOtherHash(aidRefDo) != null) {
            if (DEBUG) {
                Log.i("SecureElement-AccessRuleCache", "Conflict Resolution Case A returning access rule 'NEVER'.");
            }
            ChannelAccess channelAccess2 = new ChannelAccess();
            channelAccess2.setApduAccess(ChannelAccess.ACCESS.DENIED);
            channelAccess2.setAccess(ChannelAccess.ACCESS.DENIED, "AID has a specific access rule with a different hash. (Case A)");
            channelAccess2.setNFCEventAccess(ChannelAccess.ACCESS.DENIED);
            return channelAccess2;
        }
        REF_DO ref_do2 = new REF_DO(getAidRefDo(bArr), new Hash_REF_DO());
        if (this.mRuleCache.containsKey(ref_do2)) {
            if (DEBUG) {
                Log.i("SecureElement-AccessRuleCache", "findAccessRule() " + ref_do2.toString() + ", " + this.mRuleCache.get(ref_do2).toString());
            }
            return this.mRuleCache.get(ref_do2);
        }
        AID_REF_DO aid_ref_do = new AID_REF_DO(79);
        for (Certificate certificate2 : certificateArr) {
            try {
                REF_DO ref_do3 = new REF_DO(aid_ref_do, new Hash_REF_DO(AccessControlEnforcer.getAppCertHash(certificate2)));
                if (this.mRuleCache.containsKey(ref_do3)) {
                    ChannelAccess channelAccess3 = this.mRuleCache.get(ref_do3);
                    if (channelAccess3.getApduAccess() == ChannelAccess.ACCESS.UNDEFINED) {
                        channelAccess3.setApduAccess(ChannelAccess.ACCESS.DENIED);
                    }
                    if (channelAccess3.getNFCEventAccess() == ChannelAccess.ACCESS.UNDEFINED && channelAccess3.getApduAccess() != ChannelAccess.ACCESS.UNDEFINED) {
                        channelAccess3.setNFCEventAccess(channelAccess3.getApduAccess());
                    }
                    if (DEBUG) {
                        Log.i("SecureElement-AccessRuleCache", "findAccessRule() " + ref_do3.toString() + ", " + this.mRuleCache.get(ref_do3).toString());
                    }
                    return this.mRuleCache.get(ref_do3);
                }
            } catch (CertificateEncodingException e2) {
                throw new AccessControlException("Problem with Application Certificate.");
            }
        }
        if (searchForRulesWithAllAidButOtherHash() != null) {
            if (DEBUG) {
                Log.i("SecureElement-AccessRuleCache", "Conflict Resolution Case C returning access rule 'NEVER'.");
            }
            ChannelAccess channelAccess4 = new ChannelAccess();
            channelAccess4.setApduAccess(ChannelAccess.ACCESS.DENIED);
            channelAccess4.setAccess(ChannelAccess.ACCESS.DENIED, "An access rule with a different hash and all AIDs was found. (Case C)");
            channelAccess4.setNFCEventAccess(ChannelAccess.ACCESS.DENIED);
            return channelAccess4;
        }
        REF_DO ref_do4 = new REF_DO(new AID_REF_DO(79), new Hash_REF_DO());
        if (this.mRuleCache.containsKey(ref_do4)) {
            if (DEBUG) {
                Log.i("SecureElement-AccessRuleCache", "findAccessRule() " + ref_do4.toString() + ", " + this.mRuleCache.get(ref_do4).toString());
            }
            return this.mRuleCache.get(ref_do4);
        }
        if (DEBUG) {
            Log.i("SecureElement-AccessRuleCache", "findAccessRule() not found");
            return null;
        }
        return null;
    }

    private REF_DO searchForRulesWithSpecificAidButOtherHash(AID_REF_DO aid_ref_do) {
        if (aid_ref_do == null) {
            return null;
        }
        if (aid_ref_do.getTag() == 79 && aid_ref_do.getAid().length == 0) {
            return null;
        }
        for (REF_DO ref_do : this.mRuleCache.keySet()) {
            if (aid_ref_do.equals(ref_do.getAidDo()) && ref_do.getHashDo() != null && ref_do.getHashDo().getHash().length > 0) {
                return ref_do;
            }
        }
        return null;
    }

    private Object searchForRulesWithAllAidButOtherHash() {
        AID_REF_DO aid_ref_do = new AID_REF_DO(79);
        for (REF_DO ref_do : this.mRuleCache.keySet()) {
            if (aid_ref_do.equals(ref_do.getAidDo()) && ref_do.getHashDo() != null && ref_do.getHashDo().getHash().length > 0) {
                return ref_do;
            }
        }
        return null;
    }

    public boolean isRefreshTagEqual(byte[] bArr) {
        if (bArr == null || this.mRefreshTag == null) {
            return false;
        }
        return Arrays.equals(bArr, this.mRefreshTag);
    }

    public byte[] getRefreshTag() {
        return this.mRefreshTag;
    }

    public void setRefreshTag(byte[] bArr) {
        this.mRefreshTag = bArr;
    }

    public void dump(PrintWriter printWriter) {
        printWriter.println("SecureElement-AccessRuleCache:");
        printWriter.print("Current refresh tag is: ");
        int i = 0;
        if (this.mRefreshTag == null) {
            printWriter.print("<null>");
        } else {
            for (byte b : this.mRefreshTag) {
                printWriter.printf("%02X:", Byte.valueOf(b));
            }
        }
        printWriter.println();
        printWriter.println("Rules:");
        for (Map.Entry<REF_DO, ChannelAccess> entry : this.mRuleCache.entrySet()) {
            i++;
            printWriter.print("rule " + i + ": ");
            printWriter.println(entry.getKey().toString() + " -> " + entry.getValue().toString());
        }
        printWriter.println();
    }
}
