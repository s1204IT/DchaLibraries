package com.mediatek.services.telephony;

import android.os.Message;
import android.os.SystemProperties;
import android.telecom.PhoneAccountHandle;
import android.telephony.TelephonyManager;
import com.android.internal.telephony.Phone;
import com.android.internal.telephony.PhoneFactory;
import com.android.internal.telephony.TelephonyDevController;
import com.android.services.telephony.Log;
import com.mediatek.internal.telephony.MtkGsmCdmaPhone;
import com.mediatek.internal.telephony.MtkServiceStateTracker;
import com.mediatek.internal.telephony.RadioCapabilitySwitchUtil;
import com.mediatek.internal.telephony.ratconfiguration.RatConfiguration;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class EmergencyRuleHandler {
    private static final int PROJECT_SIM_NUM = TelephonyManager.getDefault().getPhoneCount();
    private Phone mDefaultEccPhone;
    private EmergencyNumberUtils mEccNumberUtils;
    private List<GCRuleHandler> mGCRuleList;
    private boolean mIsEccRetry;
    private Phone mMainPhone;
    private int mMainPhoneId;
    private TelephonyManager mTm;
    private Phone mGsmPhone = null;
    private Phone mCdmaPhone = null;
    private Phone mEccRetryPhone = null;
    TelephonyDevController mTelDevController = TelephonyDevController.getInstance();
    int mPrefRat = 0;

    public interface GCRuleHandler {
        Phone handleRequest();
    }

    private boolean hasC2kOverImsModem() {
        return (this.mTelDevController == null || this.mTelDevController.getModem(0) == null || !this.mTelDevController.getModem(0).hasC2kOverImsModem()) ? false : true;
    }

    public EmergencyRuleHandler(PhoneAccountHandle phoneAccountHandle, String str, boolean z, Phone phone) {
        String str2;
        this.mDefaultEccPhone = null;
        this.mMainPhone = null;
        this.mMainPhoneId = 0;
        for (Phone phone2 : PhoneFactory.getPhones()) {
            StringBuilder sb = new StringBuilder();
            sb.append("Phone");
            sb.append(phone2.getPhoneId());
            sb.append(":");
            if (phone2.getPhoneType() == 2) {
                str2 = "CDMA";
            } else {
                str2 = phone2.getPhoneType() == 1 ? "GSM" : "NONE";
            }
            sb.append(str2);
            sb.append(", service state:");
            sb.append(serviceStateToString(phone2.getServiceState().getState()));
            log(sb.toString());
        }
        this.mTm = TelephonyManager.getDefault();
        this.mEccNumberUtils = new EmergencyNumberUtils(str);
        this.mIsEccRetry = z;
        this.mDefaultEccPhone = phone;
        this.mMainPhoneId = RadioCapabilitySwitchUtil.getMainCapabilityPhoneId();
        this.mMainPhone = PhoneFactory.getPhone(this.mMainPhoneId);
        initPhones(phoneAccountHandle);
    }

    public static boolean isDualPhoneCdmaExist() {
        if (RatConfiguration.isC2kSupported() && PROJECT_SIM_NUM >= 2) {
            for (Phone phone : PhoneFactory.getPhones()) {
                if (phone.getPhoneType() == 2) {
                    return true;
                }
            }
        }
        return false;
    }

    private void initPhones(PhoneAccountHandle phoneAccountHandle) {
        this.mGsmPhone = getProperPhone(1);
        this.mCdmaPhone = getProperPhone(2);
        if (this.mGsmPhone != null) {
            log("GSM Network State == " + serviceStateToString(this.mGsmPhone.getServiceState().getState()));
        } else {
            log("No GSM Phone exist.");
        }
        if (this.mCdmaPhone != null) {
            log("CDMA Network State == " + serviceStateToString(this.mCdmaPhone.getServiceState().getState()));
        } else {
            log("No CDMA Phone exist.");
        }
        if (this.mIsEccRetry) {
            int i = Integer.parseInt(phoneAccountHandle.getId());
            this.mEccRetryPhone = PhoneFactory.getPhone(i);
            log("EccRetry phoneId:" + i);
        }
    }

    private Phone getProperPhone(int i) {
        int i2 = 0;
        if (i == 1) {
            if (this.mMainPhone != null && this.mMainPhone.getPhoneType() == 1 && this.mMainPhone.getServiceState().getState() == 0) {
                log("getProperPhone(G) : in service, main phone, phoneId:" + this.mMainPhoneId);
                return this.mMainPhone;
            }
            for (int i3 = 0; i3 < PROJECT_SIM_NUM; i3++) {
                if (i3 != this.mMainPhoneId) {
                    Phone phone = PhoneFactory.getPhone(i3);
                    if (phone.getPhoneType() == 1 && phone.getServiceState().getState() == 0) {
                        log("getProperPhone(G) : in service, non-main phone, slotid:" + i3);
                        return phone;
                    }
                }
            }
            if (this.mMainPhone != null && this.mMainPhone.getPhoneType() == 1 && 3 != this.mMainPhone.getServiceState().getState() && this.mTm.hasIccCard(this.mMainPhoneId)) {
                log("getProperPhone(G) : radio on, with SIM, main phone, phoneId:" + this.mMainPhoneId);
                return this.mMainPhone;
            }
            for (int i4 = 0; i4 < PROJECT_SIM_NUM; i4++) {
                if (i4 != this.mMainPhoneId) {
                    Phone phone2 = PhoneFactory.getPhone(i4);
                    if (phone2.getPhoneType() == 1 && 3 != phone2.getServiceState().getState() && this.mTm.hasIccCard(i4)) {
                        log("getProperPhone(G) : radio on + with SIM + non-main slot:" + i4);
                        return phone2;
                    }
                }
            }
            if (this.mMainPhone != null && this.mMainPhone.getPhoneType() == 1 && 3 != this.mMainPhone.getServiceState().getState()) {
                log("getProperPhone(G) : radio on + noSIM + main slot:" + this.mMainPhoneId);
                return this.mMainPhone;
            }
            for (int i5 = 0; i5 < PROJECT_SIM_NUM; i5++) {
                if (i5 != this.mMainPhoneId) {
                    Phone phone3 = PhoneFactory.getPhone(i5);
                    if (phone3.getPhoneType() == 1 && 3 != phone3.getServiceState().getState()) {
                        log("getProperPhone(G) : radio on + noSIM + non-main slot:" + i5);
                        return phone3;
                    }
                }
            }
            if (this.mMainPhone != null && this.mMainPhone.getPhoneType() == 1 && this.mTm.hasIccCard(this.mMainPhoneId)) {
                log("getProperPhone(G) : radio off + with SIM + main slot:" + this.mMainPhoneId);
                return this.mMainPhone;
            }
            for (int i6 = 0; i6 < PROJECT_SIM_NUM; i6++) {
                if (i6 != this.mMainPhoneId) {
                    Phone phone4 = PhoneFactory.getPhone(i6);
                    if (phone4.getPhoneType() == 1 && this.mTm.hasIccCard(i6)) {
                        log("getProperPhone(G) : radio off + with SIM + non-main slot:" + i6);
                        return phone4;
                    }
                }
            }
            if (this.mMainPhone != null && this.mMainPhone.getPhoneType() == 1) {
                log("getProperPhone(G) : radio off + noSIM + main slot:" + this.mMainPhoneId);
                return this.mMainPhone;
            }
            while (i2 < PROJECT_SIM_NUM) {
                if (i2 != this.mMainPhoneId) {
                    Phone phone5 = PhoneFactory.getPhone(i2);
                    if (phone5.getPhoneType() == 1) {
                        log("getProperPhone(G) : radio off + noSIM + non-main slot:" + i2);
                        return phone5;
                    }
                }
                i2++;
            }
            return null;
        }
        if (i == 2) {
            while (i2 < PROJECT_SIM_NUM) {
                Phone phone6 = PhoneFactory.getPhone(i2);
                if (phone6.getPhoneType() != 2) {
                    i2++;
                } else {
                    log("getProperPhone(C) : slot:" + i2);
                    return phone6;
                }
            }
            return null;
        }
        return null;
    }

    private boolean isGsmNetworkReady() {
        return this.mGsmPhone != null && this.mGsmPhone.getServiceState().getState() == 0;
    }

    private boolean isCdmaNetworkReady() {
        return this.mCdmaPhone != null && this.mCdmaPhone.getServiceState().getState() == 0;
    }

    String serviceStateToString(int i) {
        switch (i) {
            case 0:
                return "STATE_IN_SERVICE";
            case 1:
                return "STATE_OUT_OF_SERVICE";
            case 2:
                return "STATE_EMERGENCY_ONLY";
            case 3:
                return "STATE_POWER_OFF";
            default:
                log("serviceStateToString, invalid state:" + i);
                return "UNKNOWN_STATE";
        }
    }

    public Phone getPreferredPhone() {
        Phone gsmPhoneAndSwitchToCdmaIfNecessary;
        if (!RatConfiguration.isC2kSupported()) {
            if (this.mIsEccRetry) {
                log("for non-c2k project, return eccRetry phone:" + this.mEccRetryPhone);
                return this.mEccRetryPhone;
            }
            log("for non-c2k project, return default phone:" + this.mDefaultEccPhone);
            return this.mDefaultEccPhone;
        }
        boolean z = false;
        int i = 0;
        while (true) {
            if (i < PROJECT_SIM_NUM) {
                if (!this.mTm.hasIccCard(i)) {
                    break;
                }
                i++;
            } else {
                z = true;
                break;
            }
        }
        log("getPreferredPhone, allSimInserted:" + z);
        if (isDualPhoneCdmaExist()) {
            generateGCRuleList();
            gsmPhoneAndSwitchToCdmaIfNecessary = getPhoneFromGCRuleList();
            if (gsmPhoneAndSwitchToCdmaIfNecessary != null) {
                log("for G+C project with G+C phone, return " + gsmPhoneAndSwitchToCdmaIfNecessary + " rat:" + this.mPrefRat);
            } else {
                log("for G+C project with G+C phone, return default phone:" + this.mDefaultEccPhone);
                return this.mDefaultEccPhone;
            }
        } else if (this.mIsEccRetry) {
            log("for G+C project w/o G+C phone, return eccRetry phone:" + this.mEccRetryPhone);
            if (this.mEccRetryPhone != null && this.mEccRetryPhone.getPhoneType() == 1 && (this.mEccNumberUtils.isGsmOnlyNumber() || this.mEccNumberUtils.isGsmPreferredNumber())) {
                gsmPhoneAndSwitchToCdmaIfNecessary = this.mEccRetryPhone;
                this.mPrefRat = 3;
            } else {
                return this.mEccRetryPhone;
            }
        } else if (z) {
            if (this.mEccNumberUtils.isGsmOnlyNumber()) {
                gsmPhoneAndSwitchToCdmaIfNecessary = this.mDefaultEccPhone;
                this.mPrefRat = 1;
            } else if (this.mEccNumberUtils.isGsmPreferredNumber()) {
                gsmPhoneAndSwitchToCdmaIfNecessary = this.mDefaultEccPhone;
                this.mPrefRat = 3;
            } else if (hasC2kOverImsModem() && isSprintSupport() && this.mEccNumberUtils.isCdmaPreferredNumber()) {
                gsmPhoneAndSwitchToCdmaIfNecessary = this.mDefaultEccPhone;
                this.mPrefRat = 4;
            } else if (hasC2kOverImsModem() && ((this.mEccNumberUtils.isCdmaAlwaysNumber() || this.mEccNumberUtils.isCdmaPreferredNumber()) && this.mDefaultEccPhone != null && isSimLocked(this.mDefaultEccPhone))) {
                if (checkLocatedPlmnMcc(this.mDefaultEccPhone, "460")) {
                    gsmPhoneAndSwitchToCdmaIfNecessary = getFirstInServicePhone();
                    if (gsmPhoneAndSwitchToCdmaIfNecessary != null) {
                        log("for G+C project w/o G+C phone,allSimInserted,default phone locked,return found in-service phone: " + gsmPhoneAndSwitchToCdmaIfNecessary);
                    } else if (hasC2kRaf(this.mDefaultEccPhone)) {
                        gsmPhoneAndSwitchToCdmaIfNecessary = this.mDefaultEccPhone;
                        this.mPrefRat = 4;
                        log("for G+C project w/o G+C phone,allSimInserted,default phone locked with C2k RAF,return default phone: " + this.mDefaultEccPhone);
                    } else {
                        gsmPhoneAndSwitchToCdmaIfNecessary = getFirstCCapablePhone();
                        if (gsmPhoneAndSwitchToCdmaIfNecessary != null && isSimLocked(gsmPhoneAndSwitchToCdmaIfNecessary) && gsmPhoneAndSwitchToCdmaIfNecessary.getServiceState().getState() != 3) {
                            this.mPrefRat = 4;
                            log("for G+C project w/o G+C phone,allSimInserted,default phone locked w/o C2k RAF,c capable phone locked and not power off,return c capable phone:" + gsmPhoneAndSwitchToCdmaIfNecessary);
                        } else {
                            log("default phone locked w/o C2k RAF,cPhone null or not locked or power off");
                            gsmPhoneAndSwitchToCdmaIfNecessary = null;
                        }
                    }
                    if (gsmPhoneAndSwitchToCdmaIfNecessary == null) {
                        log("for G+C project w/o G+C phone,allSimInserted,return default phone:" + this.mDefaultEccPhone);
                        return this.mDefaultEccPhone;
                    }
                } else {
                    log("default phone locked, loc plmn not 460");
                    gsmPhoneAndSwitchToCdmaIfNecessary = null;
                    if (gsmPhoneAndSwitchToCdmaIfNecessary == null) {
                    }
                }
            } else {
                gsmPhoneAndSwitchToCdmaIfNecessary = null;
                if (gsmPhoneAndSwitchToCdmaIfNecessary == null && this.mPrefRat == 0) {
                    log("for G+C project w/o G+C phone,allSimInserted,return default phone:" + this.mDefaultEccPhone);
                    return this.mDefaultEccPhone;
                }
            }
        } else {
            if (!hasC2kOverImsModem()) {
                return this.mDefaultEccPhone;
            }
            if (this.mEccNumberUtils.isCdmaAlwaysNumber()) {
                gsmPhoneAndSwitchToCdmaIfNecessary = this.mCdmaPhone;
                this.mPrefRat = 2;
            } else if (this.mEccNumberUtils.isGsmAlwaysNumber() || this.mEccNumberUtils.isGsmOnlyNumber()) {
                gsmPhoneAndSwitchToCdmaIfNecessary = this.mGsmPhone;
                this.mPrefRat = 1;
            } else if (this.mEccNumberUtils.isCdmaPreferredNumber()) {
                gsmPhoneAndSwitchToCdmaIfNecessary = this.mCdmaPhone;
                this.mPrefRat = 4;
            } else if (this.mEccNumberUtils.isGsmPreferredNumber()) {
                gsmPhoneAndSwitchToCdmaIfNecessary = this.mGsmPhone;
                this.mPrefRat = 3;
            } else {
                log("for G+C project w/o G+C phone, in Service with SIM, return default phone:" + this.mDefaultEccPhone);
                return this.mDefaultEccPhone;
            }
            if (gsmPhoneAndSwitchToCdmaIfNecessary == null) {
                gsmPhoneAndSwitchToCdmaIfNecessary = getGsmPhoneAndSwitchToCdmaIfNecessary();
            }
        }
        if (hasC2kOverImsModem() && this.mPrefRat != 0 && gsmPhoneAndSwitchToCdmaIfNecessary != null) {
            ((MtkGsmCdmaPhone) gsmPhoneAndSwitchToCdmaIfNecessary).mMtkCi.setEccPreferredRat(this.mPrefRat, (Message) null);
        }
        return gsmPhoneAndSwitchToCdmaIfNecessary;
    }

    private Phone getGsmPhoneAndSwitchToCdmaIfNecessary() {
        if ((this.mPrefRat == 4 || this.mPrefRat == 2) && !this.mTm.hasIccCard(this.mDefaultEccPhone.getPhoneId()) && (this.mDefaultEccPhone.getRadioAccessFamily() & 12784) == 0) {
            log("defaulEccPhone is not c2k-enabled, trigger switch");
            this.mDefaultEccPhone.triggerModeSwitchByEcc(4, (Message) null);
        }
        return this.mDefaultEccPhone;
    }

    private boolean checkLocatedPlmnMcc(Phone phone, String str) {
        String locatedPlmn;
        if (phone != null && phone.getServiceStateTracker() != null && (phone.getServiceStateTracker() instanceof MtkServiceStateTracker)) {
            locatedPlmn = phone.getServiceStateTracker().getLocatedPlmn();
        } else {
            locatedPlmn = null;
        }
        if (locatedPlmn != null && str != null && locatedPlmn.startsWith(str)) {
            return true;
        }
        return false;
    }

    private Phone getFirstCCapablePhone() {
        if (RatConfiguration.isC2kSupported() && PROJECT_SIM_NUM >= 2) {
            for (Phone phone : PhoneFactory.getPhones()) {
                if ((phone.getRadioAccessFamily() & 12784) != 0) {
                    return phone;
                }
            }
            log("getFirstCCapablePhone no C phone found by RAF");
            return null;
        }
        return null;
    }

    private boolean hasC2kRaf(Phone phone) {
        if (phone != null && (phone.getRadioAccessFamily() & 12784) > 0) {
            return true;
        }
        return false;
    }

    private Phone getFirstInServicePhone() {
        for (Phone phone : PhoneFactory.getPhones()) {
            if (phone.getServiceState().getState() == 0) {
                return phone;
            }
        }
        log("getFirstInServicePhone no in-service phone found");
        return null;
    }

    private boolean isSimLocked(Phone phone) {
        if (phone == null) {
            return false;
        }
        int simState = this.mTm.getSimState(phone.getPhoneId());
        if (simState != 2 && simState != 3 && simState != 4 && simState != 7) {
            return false;
        }
        return true;
    }

    private void generateGCRuleList() {
        if (this.mGCRuleList != null) {
            this.mGCRuleList.clear();
        }
        this.mGCRuleList = new ArrayList();
        this.mGCRuleList.add(new MainPhoneNoSimGsmRule());
        this.mGCRuleList.add(new AlwaysNumberRule());
        this.mGCRuleList.add(new OnlyNumberRule());
        this.mGCRuleList.add(new EccRetryRule());
        this.mGCRuleList.add(new GCReadyRule());
        this.mGCRuleList.add(new GsmReadyOnlyRule());
        this.mGCRuleList.add(new CdmaReadyOnlyRule());
        this.mGCRuleList.add(new GCUnReadyRule());
    }

    private Phone getPhoneFromGCRuleList() {
        Iterator<GCRuleHandler> it = this.mGCRuleList.iterator();
        while (it.hasNext()) {
            Phone phoneHandleRequest = it.next().handleRequest();
            if (phoneHandleRequest != null) {
                return phoneHandleRequest;
            }
        }
        return null;
    }

    class MainPhoneNoSimGsmRule implements GCRuleHandler {
        MainPhoneNoSimGsmRule() {
        }

        @Override
        public Phone handleRequest() {
            EmergencyRuleHandler.this.log("MainPhoneNoSimGsmRule: handleRequest...");
            boolean z = false;
            int i = 0;
            while (true) {
                if (i < EmergencyRuleHandler.PROJECT_SIM_NUM) {
                    if (EmergencyRuleHandler.this.mTm.hasIccCard(i)) {
                        break;
                    }
                    i++;
                } else {
                    z = true;
                    break;
                }
            }
            if (!EmergencyRuleHandler.this.mIsEccRetry && z && EmergencyRuleHandler.this.mMainPhone != null) {
                if (!EmergencyRuleHandler.this.mEccNumberUtils.isGsmAlwaysNumber() && !EmergencyRuleHandler.this.mEccNumberUtils.isGsmOnlyNumber()) {
                    if (!EmergencyRuleHandler.this.mEccNumberUtils.isGsmPreferredNumber()) {
                        if (EmergencyRuleHandler.this.hasC2kOverImsModem() && (EmergencyRuleHandler.this.mEccNumberUtils.isCdmaAlwaysNumber() || EmergencyRuleHandler.this.mEccNumberUtils.isCdmaPreferredNumber())) {
                            EmergencyRuleHandler.this.mPrefRat = EmergencyRuleHandler.this.mEccNumberUtils.isCdmaAlwaysNumber() ? 2 : 4;
                            if ((EmergencyRuleHandler.this.mMainPhone.getRadioAccessFamily() & 12784) == 0) {
                                EmergencyRuleHandler.this.log("mMainPhone is not c2k-enabled, trigger switch");
                                EmergencyRuleHandler.this.mMainPhone.triggerModeSwitchByEcc(4, (Message) null);
                            }
                            return EmergencyRuleHandler.this.mMainPhone;
                        }
                    } else {
                        EmergencyRuleHandler.this.mPrefRat = 3;
                        return EmergencyRuleHandler.this.mMainPhone;
                    }
                } else {
                    EmergencyRuleHandler.this.mPrefRat = 1;
                    return EmergencyRuleHandler.this.mMainPhone;
                }
            }
            return null;
        }
    }

    class AlwaysNumberRule implements GCRuleHandler {
        AlwaysNumberRule() {
        }

        @Override
        public Phone handleRequest() {
            EmergencyRuleHandler.this.log("AlwaysNumberRule: handleRequest...");
            if (EmergencyRuleHandler.this.mEccNumberUtils.isGsmAlwaysNumber()) {
                EmergencyRuleHandler.this.mPrefRat = 1;
                return EmergencyRuleHandler.this.mGsmPhone;
            }
            if (SystemProperties.getInt("vendor.gsm.gcf.testmode", 0) != 2 && EmergencyRuleHandler.this.mEccNumberUtils.isCdmaAlwaysNumber()) {
                EmergencyRuleHandler.this.mPrefRat = 2;
                return EmergencyRuleHandler.this.mCdmaPhone;
            }
            return null;
        }
    }

    class OnlyNumberRule implements GCRuleHandler {
        OnlyNumberRule() {
        }

        @Override
        public Phone handleRequest() {
            EmergencyRuleHandler.this.log("OnlyNumberRule: handleRequest...");
            if (EmergencyRuleHandler.this.mEccNumberUtils.isGsmOnlyNumber()) {
                EmergencyRuleHandler.this.mPrefRat = 1;
                return EmergencyRuleHandler.this.mGsmPhone;
            }
            return null;
        }
    }

    class EccRetryRule implements GCRuleHandler {
        EccRetryRule() {
        }

        @Override
        public Phone handleRequest() {
            if (EmergencyRuleHandler.this.mIsEccRetry) {
                EmergencyRuleHandler.this.log("EccRetryRule: handleRequest...");
                if (EmergencyRuleHandler.this.mEccRetryPhone == null || EmergencyRuleHandler.this.mEccRetryPhone.getPhoneType() != 2 || !EmergencyRuleHandler.this.mEccNumberUtils.isCdmaPreferredNumber()) {
                    if (EmergencyRuleHandler.this.mEccRetryPhone != null && EmergencyRuleHandler.this.mEccRetryPhone.getPhoneType() == 1 && EmergencyRuleHandler.this.mEccNumberUtils.isGsmPreferredNumber()) {
                        EmergencyRuleHandler.this.mPrefRat = 3;
                    }
                } else {
                    EmergencyRuleHandler.this.mPrefRat = 4;
                }
                return EmergencyRuleHandler.this.mEccRetryPhone;
            }
            return null;
        }
    }

    class GsmReadyOnlyRule implements GCRuleHandler {
        GsmReadyOnlyRule() {
        }

        @Override
        public Phone handleRequest() {
            EmergencyRuleHandler.this.log("GsmReadyOnlyRule: handleRequest...");
            if (EmergencyRuleHandler.this.mEccNumberUtils.isGsmAlwaysNumber() || EmergencyRuleHandler.this.mEccNumberUtils.isCdmaAlwaysNumber() || EmergencyRuleHandler.this.mEccNumberUtils.isGsmOnlyNumber() || !EmergencyRuleHandler.this.isGsmNetworkReady() || EmergencyRuleHandler.this.isCdmaNetworkReady()) {
                return null;
            }
            return EmergencyRuleHandler.this.mGsmPhone;
        }
    }

    class CdmaReadyOnlyRule implements GCRuleHandler {
        CdmaReadyOnlyRule() {
        }

        @Override
        public Phone handleRequest() {
            EmergencyRuleHandler.this.log("CdmaReadyOnlyRule: handleRequest...");
            if (EmergencyRuleHandler.this.mEccNumberUtils.isGsmAlwaysNumber() || EmergencyRuleHandler.this.mEccNumberUtils.isCdmaAlwaysNumber() || EmergencyRuleHandler.this.mEccNumberUtils.isGsmOnlyNumber() || !EmergencyRuleHandler.this.isCdmaNetworkReady() || EmergencyRuleHandler.this.isGsmNetworkReady()) {
                return null;
            }
            return EmergencyRuleHandler.this.mCdmaPhone;
        }
    }

    class GCReadyRule implements GCRuleHandler {
        GCReadyRule() {
        }

        @Override
        public Phone handleRequest() {
            EmergencyRuleHandler.this.log("GCReadyRule: handleRequest...");
            if (!EmergencyRuleHandler.this.mEccNumberUtils.isGsmAlwaysNumber() && !EmergencyRuleHandler.this.mEccNumberUtils.isCdmaAlwaysNumber() && !EmergencyRuleHandler.this.mEccNumberUtils.isGsmOnlyNumber() && EmergencyRuleHandler.this.isCdmaNetworkReady() && EmergencyRuleHandler.this.isGsmNetworkReady()) {
                if (!EmergencyRuleHandler.this.mEccNumberUtils.isGsmPreferredNumber()) {
                    if (EmergencyRuleHandler.this.mEccNumberUtils.isCdmaPreferredNumber()) {
                        EmergencyRuleHandler.this.mPrefRat = 4;
                        return EmergencyRuleHandler.this.mCdmaPhone;
                    }
                } else {
                    EmergencyRuleHandler.this.mPrefRat = 3;
                    return EmergencyRuleHandler.this.mGsmPhone;
                }
            }
            return null;
        }
    }

    class GCUnReadyRule implements GCRuleHandler {
        GCUnReadyRule() {
        }

        @Override
        public Phone handleRequest() {
            EmergencyRuleHandler.this.log("GCUnReadyRule: handleRequest...");
            if (!EmergencyRuleHandler.this.mEccNumberUtils.isGsmAlwaysNumber() && !EmergencyRuleHandler.this.mEccNumberUtils.isCdmaAlwaysNumber() && !EmergencyRuleHandler.this.mEccNumberUtils.isGsmOnlyNumber() && !EmergencyRuleHandler.this.isCdmaNetworkReady() && !EmergencyRuleHandler.this.isGsmNetworkReady()) {
                if (!EmergencyRuleHandler.this.mEccNumberUtils.isGsmPreferredNumber()) {
                    if (EmergencyRuleHandler.this.mEccNumberUtils.isCdmaPreferredNumber()) {
                        EmergencyRuleHandler.this.mPrefRat = 4;
                        return EmergencyRuleHandler.this.mCdmaPhone;
                    }
                } else {
                    EmergencyRuleHandler.this.mPrefRat = 3;
                    return EmergencyRuleHandler.this.mGsmPhone;
                }
            }
            return null;
        }
    }

    private void log(String str) {
        Log.d("ECCRuleHandler", str, new Object[0]);
    }

    private boolean isSprintSupport() {
        if ("OP20".equals(SystemProperties.get("persist.vendor.operator.optr", ""))) {
            log("isSprintSupport: true");
            return true;
        }
        return false;
    }
}
