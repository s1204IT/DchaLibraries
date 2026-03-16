package com.android.internal.telephony.cdma;

import android.R;
import android.os.AsyncResult;
import android.os.Build;
import android.os.Message;
import android.os.SystemClock;
import android.os.SystemProperties;
import android.telephony.CellIdentityLte;
import android.telephony.CellInfo;
import android.telephony.CellInfoLte;
import android.telephony.CellSignalStrengthLte;
import android.telephony.Rlog;
import android.telephony.ServiceState;
import android.telephony.SubscriptionManager;
import android.telephony.TelephonyManager;
import android.telephony.cdma.CdmaCellLocation;
import android.text.TextUtils;
import android.util.EventLog;
import com.android.internal.telephony.EventLogTags;
import com.android.internal.telephony.MccTable;
import com.android.internal.telephony.Phone;
import com.android.internal.telephony.ProxyController;
import com.android.internal.telephony.dataconnection.DcTrackerBase;
import com.android.internal.telephony.uicc.IccCardApplicationStatus;
import com.android.internal.telephony.uicc.RuimRecords;
import com.android.internal.telephony.uicc.UiccCardApplication;
import java.io.FileDescriptor;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;

public class CdmaLteServiceStateTracker extends CdmaServiceStateTracker {
    private static final int EVENT_ALL_DATA_DISCONNECTED = 1001;
    private CDMALTEPhone mCdmaLtePhone;
    private final CellInfoLte mCellInfoLte;
    private CellIdentityLte mLasteCellIdentityLte;
    private CellIdentityLte mNewCellIdentityLte;

    public CdmaLteServiceStateTracker(CDMALTEPhone phone) {
        super(phone, new CellInfoLte());
        this.mNewCellIdentityLte = new CellIdentityLte();
        this.mLasteCellIdentityLte = new CellIdentityLte();
        this.mCdmaLtePhone = phone;
        this.mCdmaLtePhone.registerForSimRecordsLoaded(this, 16, null);
        this.mCellInfoLte = (CellInfoLte) this.mCellInfo;
        ((CellInfoLte) this.mCellInfo).setCellSignalStrength(new CellSignalStrengthLte());
        ((CellInfoLte) this.mCellInfo).setCellIdentity(new CellIdentityLte());
        log("CdmaLteServiceStateTracker Constructors");
    }

    @Override
    public void dispose() {
        this.mPhone.unregisterForSimRecordsLoaded(this);
        super.dispose();
    }

    @Override
    public void handleMessage(Message msg) {
        if (!this.mPhone.mIsTheCurrentActivePhone) {
            loge("Received message " + msg + "[" + msg.what + "] while being destroyed. Ignoring.");
            return;
        }
        log("handleMessage: " + msg.what);
        switch (msg.what) {
            case 5:
                log("handleMessage EVENT_POLL_STATE_GPRS");
                AsyncResult ar = (AsyncResult) msg.obj;
                handlePollStateResult(msg.what, ar);
                return;
            case 16:
                updatePhoneObject();
                return;
            case 27:
                updatePhoneObject();
                RuimRecords ruim = (RuimRecords) this.mIccRecords;
                if (ruim != null) {
                    if (ruim.isProvisioned()) {
                        this.mMdn = ruim.getMdn();
                        this.mMin = ruim.getMin();
                        parseSidNid(ruim.getSid(), ruim.getNid());
                        this.mPrlVersion = ruim.getPrlVersion();
                        this.mIsMinInfoReady = true;
                    }
                    updateOtaspState();
                }
                this.mPhone.prepareEri();
                pollState();
                return;
            case 1001:
                int dds = SubscriptionManager.getDefaultDataSubId();
                ProxyController.getInstance().unregisterForAllDataDisconnected(dds, this);
                synchronized (this) {
                    if (this.mPendingRadioPowerOffAfterDataOff) {
                        log("EVENT_ALL_DATA_DISCONNECTED, turn radio off now.");
                        hangupAndPowerOff();
                        this.mPendingRadioPowerOffAfterDataOff = false;
                    } else {
                        log("EVENT_ALL_DATA_DISCONNECTED is stale");
                    }
                    break;
                }
                return;
            default:
                super.handleMessage(msg);
                return;
        }
    }

    @Override
    protected void handlePollStateResultMessage(int what, AsyncResult ar) {
        int mcc;
        int mnc;
        int tac;
        int pci;
        int eci;
        if (what == 5) {
            String[] states = (String[]) ar.result;
            log("handlePollStateResultMessage: EVENT_POLL_STATE_GPRS states.length=" + states.length + " states=" + states);
            int type = 0;
            int regState = -1;
            if (states.length > 0) {
                try {
                    regState = Integer.parseInt(states[0]);
                    if (states.length >= 4 && states[3] != null) {
                        type = Integer.parseInt(states[3]);
                    }
                } catch (NumberFormatException ex) {
                    loge("handlePollStateResultMessage: error parsing GprsRegistrationState: " + ex);
                }
                if (states.length >= 10) {
                    String operatorNumeric = null;
                    try {
                        operatorNumeric = this.mNewSS.getOperatorNumeric();
                        mcc = Integer.parseInt(operatorNumeric.substring(0, 3));
                    } catch (Exception e) {
                        try {
                            operatorNumeric = this.mSS.getOperatorNumeric();
                            mcc = Integer.parseInt(operatorNumeric.substring(0, 3));
                        } catch (Exception ex2) {
                            loge("handlePollStateResultMessage: bad mcc operatorNumeric=" + operatorNumeric + " ex=" + ex2);
                            operatorNumeric = "";
                            mcc = Integer.MAX_VALUE;
                        }
                    }
                    try {
                        mnc = Integer.parseInt(operatorNumeric.substring(3));
                    } catch (Exception e2) {
                        loge("handlePollStateResultMessage: bad mnc operatorNumeric=" + operatorNumeric + " e=" + e2);
                        mnc = Integer.MAX_VALUE;
                    }
                    try {
                        tac = Integer.decode(states[6]).intValue();
                    } catch (Exception e3) {
                        loge("handlePollStateResultMessage: bad tac states[6]=" + states[6] + " e=" + e3);
                        tac = Integer.MAX_VALUE;
                    }
                    try {
                        pci = Integer.decode(states[7]).intValue();
                    } catch (Exception e4) {
                        loge("handlePollStateResultMessage: bad pci states[7]=" + states[7] + " e=" + e4);
                        pci = Integer.MAX_VALUE;
                    }
                    try {
                        eci = Integer.decode(states[8]).intValue();
                    } catch (Exception e5) {
                        loge("handlePollStateResultMessage: bad eci states[8]=" + states[8] + " e=" + e5);
                        eci = Integer.MAX_VALUE;
                    }
                    try {
                        Integer.decode(states[9]).intValue();
                    } catch (Exception e6) {
                    }
                    this.mNewCellIdentityLte = new CellIdentityLte(mcc, mnc, eci, pci, tac);
                    log("handlePollStateResultMessage: mNewLteCellIdentity=" + this.mNewCellIdentityLte);
                }
            }
            this.mNewSS.setRilDataRadioTechnology(type);
            int dataRegState = regCodeToServiceState(regState);
            this.mNewSS.setDataRegState(dataRegState);
            this.mNewSS.setDataRoaming(regCodeIsRoaming(regState));
            log("handlPollStateResultMessage: CdmaLteSST setDataRegState=" + dataRegState + " regState=" + regState + " dataRadioTechnology=" + type);
            return;
        }
        super.handlePollStateResultMessage(what, ar);
    }

    @Override
    public void pollState() {
        this.mPollingContext = new int[1];
        this.mPollingContext[0] = 0;
        switch (this.mCi.getRadioState()) {
            case RADIO_UNAVAILABLE:
                this.mNewSS.setStateOutOfService();
                this.mNewCellLoc.setStateInvalid();
                setSignalStrengthDefaultValues();
                this.mGotCountryCode = false;
                pollStateDone();
                break;
            case RADIO_OFF:
                this.mNewSS.setStateOff();
                this.mNewCellLoc.setStateInvalid();
                setSignalStrengthDefaultValues();
                this.mGotCountryCode = false;
                pollStateDone();
                break;
            default:
                int[] iArr = this.mPollingContext;
                iArr[0] = iArr[0] + 1;
                this.mCi.getOperator(obtainMessage(25, this.mPollingContext));
                int[] iArr2 = this.mPollingContext;
                iArr2[0] = iArr2[0] + 1;
                this.mCi.getVoiceRegistrationState(obtainMessage(24, this.mPollingContext));
                int[] iArr3 = this.mPollingContext;
                iArr3[0] = iArr3[0] + 1;
                this.mCi.getDataRegistrationState(obtainMessage(5, this.mPollingContext));
                break;
        }
    }

    @Override
    protected void pollStateDone() {
        log("pollStateDone: lte 1 ss=[" + this.mSS + "] newSS=[" + this.mNewSS + "]");
        if (this.mPhone.isMccMncMarkedAsNonRoaming(this.mNewSS.getOperatorNumeric()) || this.mPhone.isSidMarkedAsNonRoaming(this.mNewSS.getSystemId())) {
            log("pollStateDone: override - marked as non-roaming.");
            this.mNewSS.setVoiceRoaming(false);
            this.mNewSS.setDataRoaming(false);
            this.mNewSS.setCdmaEriIconIndex(1);
        } else if (this.mPhone.isMccMncMarkedAsRoaming(this.mNewSS.getOperatorNumeric()) || this.mPhone.isSidMarkedAsRoaming(this.mNewSS.getSystemId())) {
            log("pollStateDone: override - marked as roaming.");
            this.mNewSS.setVoiceRoaming(true);
            this.mNewSS.setDataRoaming(true);
            this.mNewSS.setCdmaEriIconIndex(0);
            this.mNewSS.setCdmaEriIconMode(0);
        }
        if (Build.IS_DEBUGGABLE && SystemProperties.getBoolean("telephony.test.forceRoaming", false)) {
            this.mNewSS.setVoiceRoaming(true);
            this.mNewSS.setDataRoaming(true);
        }
        useDataRegStateForDataOnlyDevices();
        boolean hasRegistered = this.mSS.getVoiceRegState() != 0 && this.mNewSS.getVoiceRegState() == 0;
        boolean hasDeregistered = this.mSS.getVoiceRegState() == 0 && this.mNewSS.getVoiceRegState() != 0;
        boolean hasCdmaDataConnectionAttached = this.mSS.getDataRegState() != 0 && this.mNewSS.getDataRegState() == 0;
        boolean hasCdmaDataConnectionDetached = this.mSS.getDataRegState() == 0 && this.mNewSS.getDataRegState() != 0;
        boolean hasCdmaDataConnectionChanged = this.mSS.getDataRegState() != this.mNewSS.getDataRegState();
        boolean hasVoiceRadioTechnologyChanged = this.mSS.getRilVoiceRadioTechnology() != this.mNewSS.getRilVoiceRadioTechnology();
        boolean hasDataRadioTechnologyChanged = this.mSS.getRilDataRadioTechnology() != this.mNewSS.getRilDataRadioTechnology();
        boolean hasChanged = !this.mNewSS.equals(this.mSS);
        boolean hasVoiceRoamingOn = !this.mSS.getVoiceRoaming() && this.mNewSS.getVoiceRoaming();
        boolean hasVoiceRoamingOff = this.mSS.getVoiceRoaming() && !this.mNewSS.getVoiceRoaming();
        boolean hasDataRoamingOn = !this.mSS.getDataRoaming() && this.mNewSS.getDataRoaming();
        boolean hasDataRoamingOff = this.mSS.getDataRoaming() && !this.mNewSS.getDataRoaming();
        boolean hasLocationChanged = !this.mNewCellLoc.equals(this.mCellLoc);
        boolean has4gHandoff = this.mNewSS.getDataRegState() == 0 && ((this.mSS.getRilDataRadioTechnology() == 14 && this.mNewSS.getRilDataRadioTechnology() == 13) || (this.mSS.getRilDataRadioTechnology() == 13 && this.mNewSS.getRilDataRadioTechnology() == 14));
        boolean hasMultiApnSupport = ((this.mNewSS.getRilDataRadioTechnology() != 14 && this.mNewSS.getRilDataRadioTechnology() != 13) || this.mSS.getRilDataRadioTechnology() == 14 || this.mSS.getRilDataRadioTechnology() == 13) ? false : true;
        boolean hasLostMultiApnSupport = this.mNewSS.getRilDataRadioTechnology() >= 4 && this.mNewSS.getRilDataRadioTechnology() <= 8;
        TelephonyManager tm = (TelephonyManager) this.mPhone.getContext().getSystemService("phone");
        log("pollStateDone: hasRegistered=" + hasRegistered + " hasDeegistered=" + hasDeregistered + " hasCdmaDataConnectionAttached=" + hasCdmaDataConnectionAttached + " hasCdmaDataConnectionDetached=" + hasCdmaDataConnectionDetached + " hasCdmaDataConnectionChanged=" + hasCdmaDataConnectionChanged + " hasVoiceRadioTechnologyChanged= " + hasVoiceRadioTechnologyChanged + " hasDataRadioTechnologyChanged=" + hasDataRadioTechnologyChanged + " hasChanged=" + hasChanged + " hasVoiceRoamingOn=" + hasVoiceRoamingOn + " hasVoiceRoamingOff=" + hasVoiceRoamingOff + " hasDataRoamingOn=" + hasDataRoamingOn + " hasDataRoamingOff=" + hasDataRoamingOff + " hasLocationChanged=" + hasLocationChanged + " has4gHandoff = " + has4gHandoff + " hasMultiApnSupport=" + hasMultiApnSupport + " hasLostMultiApnSupport=" + hasLostMultiApnSupport);
        if (this.mSS.getVoiceRegState() != this.mNewSS.getVoiceRegState() || this.mSS.getDataRegState() != this.mNewSS.getDataRegState()) {
            EventLog.writeEvent(EventLogTags.CDMA_SERVICE_STATE_CHANGE, Integer.valueOf(this.mSS.getVoiceRegState()), Integer.valueOf(this.mSS.getDataRegState()), Integer.valueOf(this.mNewSS.getVoiceRegState()), Integer.valueOf(this.mNewSS.getDataRegState()));
        }
        ServiceState tss = this.mSS;
        this.mSS = this.mNewSS;
        this.mNewSS = tss;
        this.mNewSS.setStateOutOfService();
        CdmaCellLocation tcl = this.mCellLoc;
        this.mCellLoc = this.mNewCellLoc;
        this.mNewCellLoc = tcl;
        this.mNewSS.setStateOutOfService();
        if (hasVoiceRadioTechnologyChanged) {
            updatePhoneObject();
        }
        if (hasDataRadioTechnologyChanged) {
            tm.setDataNetworkTypeForPhone(this.mPhone.getPhoneId(), this.mSS.getRilDataRadioTechnology());
        }
        if (hasRegistered) {
            this.mNetworkAttachedRegistrants.notifyRegistrants();
        }
        if (hasChanged) {
            boolean hasBrandOverride = (this.mUiccController.getUiccCard(getPhoneId()) == null || this.mUiccController.getUiccCard(getPhoneId()).getOperatorBrandOverride() == null) ? false : true;
            if (!hasBrandOverride && this.mCi.getRadioState().isOn() && this.mPhone.isEriFileLoaded() && (this.mSS.getRilVoiceRadioTechnology() != 14 || this.mPhone.getContext().getResources().getBoolean(R.^attr-private.magnifierZoom))) {
                String eriText = this.mSS.getOperatorAlphaLong();
                if (this.mSS.getVoiceRegState() == 0) {
                    eriText = this.mPhone.getCdmaEriText();
                } else if (this.mSS.getVoiceRegState() == 3) {
                    eriText = this.mIccRecords != null ? this.mIccRecords.getServiceProviderName() : null;
                    if (TextUtils.isEmpty(eriText)) {
                        eriText = SystemProperties.get("ro.cdma.home.operator.alpha");
                    }
                } else if (this.mSS.getDataRegState() != 0) {
                    eriText = this.mPhone.getContext().getText(R.string.PERSOSUBSTATE_SIM_SP_EHPLMN_SUCCESS).toString();
                }
                this.mSS.setOperatorAlphaLong(eriText);
            }
            if (this.mUiccApplcation != null && this.mUiccApplcation.getState() == IccCardApplicationStatus.AppState.APPSTATE_READY && this.mIccRecords != null && this.mSS.getVoiceRegState() == 0) {
                boolean showSpn = ((RuimRecords) this.mIccRecords).getCsimSpnDisplayCondition();
                int iconIndex = this.mSS.getCdmaEriIconIndex();
                if (showSpn && iconIndex == 1 && isInHomeSidNid(this.mSS.getSystemId(), this.mSS.getNetworkId()) && this.mIccRecords != null) {
                    this.mSS.setOperatorAlphaLong(this.mIccRecords.getServiceProviderName());
                }
            }
            tm.setNetworkOperatorNameForPhone(this.mPhone.getPhoneId(), this.mSS.getOperatorAlphaLong());
            String prevOperatorNumeric = tm.getNetworkOperatorForPhone(this.mPhone.getPhoneId());
            String operatorNumeric = this.mSS.getOperatorNumeric();
            if (isInvalidOperatorNumeric(operatorNumeric)) {
                int sid = this.mSS.getSystemId();
                operatorNumeric = fixUnknownMcc(operatorNumeric, sid);
            }
            tm.setNetworkOperatorNumericForPhone(this.mPhone.getPhoneId(), operatorNumeric);
            updateCarrierMccMncConfiguration(operatorNumeric, prevOperatorNumeric, this.mPhone.getContext());
            if (isInvalidOperatorNumeric(operatorNumeric)) {
                log("operatorNumeric is null");
                tm.setNetworkCountryIsoForPhone(this.mPhone.getPhoneId(), "");
                this.mGotCountryCode = false;
            } else {
                String isoCountryCode = "";
                operatorNumeric.substring(0, 3);
                try {
                    isoCountryCode = MccTable.countryCodeForMcc(Integer.parseInt(operatorNumeric.substring(0, 3)));
                } catch (NumberFormatException ex) {
                    loge("countryCodeForMcc error" + ex);
                } catch (StringIndexOutOfBoundsException ex2) {
                    loge("countryCodeForMcc error" + ex2);
                }
                tm.setNetworkCountryIsoForPhone(this.mPhone.getPhoneId(), isoCountryCode);
                this.mGotCountryCode = true;
                setOperatorIdd(operatorNumeric);
                if (shouldFixTimeZoneNow(this.mPhone, operatorNumeric, prevOperatorNumeric, this.mNeedFixZone)) {
                    fixTimeZone(isoCountryCode);
                }
            }
            tm.setNetworkRoamingForPhone(this.mPhone.getPhoneId(), this.mSS.getVoiceRoaming() || this.mSS.getDataRoaming());
            updateSpnDisplay();
            setRoamingType(this.mSS);
            log("Broadcasting ServiceState : " + this.mSS);
            this.mPhone.notifyServiceStateChanged(this.mSS);
        }
        if (hasCdmaDataConnectionAttached || has4gHandoff) {
            this.mAttachedRegistrants.notifyRegistrants();
        }
        if (hasCdmaDataConnectionDetached) {
            this.mDetachedRegistrants.notifyRegistrants();
        }
        if (hasCdmaDataConnectionChanged || hasDataRadioTechnologyChanged) {
            notifyDataRegStateRilRadioTechnologyChanged();
            this.mPhone.notifyDataConnection(null);
        }
        if (hasVoiceRoamingOn) {
            this.mVoiceRoamingOnRegistrants.notifyRegistrants();
        }
        if (hasVoiceRoamingOff) {
            this.mVoiceRoamingOffRegistrants.notifyRegistrants();
        }
        if (hasDataRoamingOn) {
            this.mDataRoamingOnRegistrants.notifyRegistrants();
        }
        if (hasDataRoamingOff) {
            this.mDataRoamingOffRegistrants.notifyRegistrants();
        }
        if (hasLocationChanged) {
            this.mPhone.notifyLocationChanged();
        }
        ArrayList<CellInfo> arrayCi = new ArrayList<>();
        synchronized (this.mCellInfo) {
            CellInfoLte cil = (CellInfoLte) this.mCellInfo;
            boolean cidChanged = !this.mNewCellIdentityLte.equals(this.mLasteCellIdentityLte);
            if (hasRegistered || hasDeregistered || cidChanged) {
                long jElapsedRealtime = SystemClock.elapsedRealtime() * 1000;
                boolean registered = this.mSS.getVoiceRegState() == 0;
                this.mLasteCellIdentityLte = this.mNewCellIdentityLte;
                cil.setRegistered(registered);
                cil.setCellIdentity(this.mLasteCellIdentityLte);
                log("pollStateDone: hasRegistered=" + hasRegistered + " hasDeregistered=" + hasDeregistered + " cidChanged=" + cidChanged + " mCellInfo=" + this.mCellInfo);
                arrayCi.add(this.mCellInfo);
            }
            this.mPhoneBase.notifyCellInfo(arrayCi);
        }
    }

    @Override
    protected boolean onSignalStrengthResult(AsyncResult ar, boolean isGsm) {
        if (this.mSS.getRilDataRadioTechnology() == 14) {
            isGsm = true;
        }
        boolean ssChanged = super.onSignalStrengthResult(ar, isGsm);
        synchronized (this.mCellInfo) {
            if (this.mSS.getRilDataRadioTechnology() == 14) {
                this.mCellInfoLte.setTimeStamp(SystemClock.elapsedRealtime() * 1000);
                this.mCellInfoLte.setTimeStampType(4);
                this.mCellInfoLte.getCellSignalStrength().initialize(this.mSignalStrength, Integer.MAX_VALUE);
            }
            if (this.mCellInfoLte.getCellIdentity() != null) {
                ArrayList<CellInfo> arrayCi = new ArrayList<>();
                arrayCi.add(this.mCellInfoLte);
                this.mPhoneBase.notifyCellInfo(arrayCi);
            }
        }
        return ssChanged;
    }

    @Override
    public boolean isConcurrentVoiceAndDataAllowed() {
        return this.mSS.getCssIndicator() == 1;
    }

    private boolean isInHomeSidNid(int sid, int nid) {
        if (isSidsAllZeros() || this.mHomeSystemId.length != this.mHomeNetworkId.length || sid == 0) {
            return true;
        }
        for (int i = 0; i < this.mHomeSystemId.length; i++) {
            if (this.mHomeSystemId[i] == sid && (this.mHomeNetworkId[i] == 0 || this.mHomeNetworkId[i] == 65535 || nid == 0 || nid == 65535 || this.mHomeNetworkId[i] == nid)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public List<CellInfo> getAllCellInfo() {
        if (this.mCi.getRilVersion() >= 8) {
            return super.getAllCellInfo();
        }
        ArrayList<CellInfo> arrayList = new ArrayList<>();
        synchronized (this.mCellInfo) {
            arrayList.add(this.mCellInfoLte);
        }
        log("getAllCellInfo: arrayList=" + arrayList);
        return arrayList;
    }

    @Override
    protected UiccCardApplication getUiccCardApplication() {
        return this.mUiccController.getUiccCardApplication(((CDMALTEPhone) this.mPhone).getPhoneId(), 2);
    }

    protected void updateCdmaSubscription() {
        this.mCi.getCDMASubscription(obtainMessage(34));
    }

    @Override
    public void powerOffRadioSafely(DcTrackerBase dcTracker) {
        synchronized (this) {
            if (!this.mPendingRadioPowerOffAfterDataOff) {
                int dds = SubscriptionManager.getDefaultDataSubId();
                if (dcTracker.isDisconnected() && (dds == this.mPhone.getSubId() || (dds != this.mPhone.getSubId() && ProxyController.getInstance().isDataDisconnected(dds)))) {
                    dcTracker.cleanUpAllConnections(Phone.REASON_RADIO_TURNED_OFF);
                    log("Data disconnected, turn off radio right away.");
                    hangupAndPowerOff();
                } else {
                    dcTracker.cleanUpAllConnections(Phone.REASON_RADIO_TURNED_OFF);
                    if (dds != this.mPhone.getSubId() && !ProxyController.getInstance().isDataDisconnected(dds)) {
                        log("Data is active on DDS.  Wait for all data disconnect");
                        ProxyController.getInstance().registerForAllDataDisconnected(dds, this, 1001, null);
                        this.mPendingRadioPowerOffAfterDataOff = true;
                    }
                    Message msg = Message.obtain(this);
                    msg.what = 38;
                    int i = this.mPendingRadioPowerOffAfterDataOffTag + 1;
                    this.mPendingRadioPowerOffAfterDataOffTag = i;
                    msg.arg1 = i;
                    if (sendMessageDelayed(msg, 30000L)) {
                        log("Wait upto 30s for data to disconnect, then turn off radio.");
                        this.mPendingRadioPowerOffAfterDataOff = true;
                    } else {
                        log("Cannot send delayed Msg, turn off radio right away.");
                        hangupAndPowerOff();
                        this.mPendingRadioPowerOffAfterDataOff = false;
                    }
                }
            }
        }
    }

    @Override
    protected void log(String s) {
        Rlog.d("CdmaSST", "[CdmaLteSST] " + s);
    }

    @Override
    protected void loge(String s) {
        Rlog.e("CdmaSST", "[CdmaLteSST] " + s);
    }

    @Override
    public void dump(FileDescriptor fd, PrintWriter pw, String[] args) {
        pw.println("CdmaLteServiceStateTracker extends:");
        super.dump(fd, pw, args);
        pw.println(" mCdmaLtePhone=" + this.mCdmaLtePhone);
    }
}
