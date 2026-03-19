package com.mediatek.internal.telephony;

import android.content.Context;
import android.content.Intent;
import android.os.Message;
import android.telephony.TelephonyManager;
import android.util.Log;
import com.android.ims.ImsException;
import com.android.internal.telephony.Phone;
import com.mediatek.internal.telephony.imsphone.MtkImsPhone;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.TimeZone;

public class MtkSuppServUtTest {
    private static final int ACTION_ACTIVATE = 1;
    private static final int ACTION_DEACTIVATE = 0;
    private static final int ACTION_INTERROGATE = 2;
    static final String ACTION_SUPPLEMENTARY_SERVICE_ROAMING_TEST = "android.intent.action.ACTION_SUPPLEMENTARY_SERVICE_ROAMING_TEST";
    static final String ACTION_SUPPLEMENTARY_SERVICE_UT_TEST = "android.intent.action.ACTION_SUPPLEMENTARY_SERVICE_UT_TEST";
    private static final String EXTRA_ACTION = "action";
    private static final String EXTRA_PHONE_ID = "phoneId";
    private static final String EXTRA_SERVICE_CODE = "serviceCode";
    private static final String EXTRA_SERVICE_INFO_A = "serviceInfoA";
    private static final String EXTRA_SERVICE_INFO_B = "serviceInfoB";
    private static final String EXTRA_SERVICE_INFO_C = "serviceInfoC";
    private static final String EXTRA_SERVICE_INFO_D = "serviceInfoD";
    static final String LOG_TAG = "MtkSuppServUtTest";
    private static final int NUM_PRESENTATION_ALLOWED = 0;
    private static final int NUM_PRESENTATION_RESTRICTED = 1;
    private static final String SC_BAIC = "35";
    private static final String SC_BAICr = "351";
    private static final String SC_CFB = "67";
    private static final String SC_CFNR = "62";
    private static final String SC_CFNRy = "61";
    private static final String SC_CFNotRegister = "68";
    private static final String SC_CFU = "21";
    private static final String SC_CFUR = "22";
    private static final String SC_CLIP = "30";
    private static final String SC_CLIR = "31";
    private static final String SC_COLP = "76";
    private static final String SC_COLR = "77";
    private static final String SC_WAIT = "43";
    private MtkImsPhone activeImsPhone;
    private MtkGsmCdmaPhone activePhone;
    private Context mContext;
    private int phoneId;
    private String serviceCode;
    private String serviceInfoA;
    private String serviceInfoB;
    private String serviceInfoC;
    private String serviceInfoD;
    private int ssAction;

    public MtkSuppServUtTest(Context context, Intent intent, Phone phone) {
        this.ssAction = intent.getIntExtra(EXTRA_ACTION, -1);
        this.serviceCode = intent.getStringExtra(EXTRA_SERVICE_CODE);
        this.serviceInfoA = intent.getStringExtra(EXTRA_SERVICE_INFO_A);
        this.serviceInfoB = intent.getStringExtra(EXTRA_SERVICE_INFO_B);
        this.serviceInfoC = intent.getStringExtra(EXTRA_SERVICE_INFO_C);
        this.serviceInfoD = intent.getStringExtra(EXTRA_SERVICE_INFO_D);
        this.phoneId = getValidPhoneId(intent.getIntExtra(EXTRA_PHONE_ID, -1));
        this.mContext = context;
        this.activePhone = (MtkGsmCdmaPhone) phone;
        this.activeImsPhone = this.activePhone.getImsPhone();
    }

    void run() {
        Log.d(LOG_TAG, "onReceive, ssAction = " + this.ssAction + ", serviceCode = " + this.serviceCode + ", serviceInfoA = " + this.serviceInfoA + ", serviceInfoB = " + this.serviceInfoB + ", serviceInfoC = " + this.serviceInfoC + ", serviceInfoD = " + this.serviceInfoD + ", phoneId = " + this.phoneId);
        try {
        } catch (RuntimeException e) {
            e.printStackTrace();
        }
        if (isServiceCodeCallForwarding(this.serviceCode)) {
            int iActionToCommandAction = actionToCommandAction(this.ssAction);
            int iScToCallForwardReason = scToCallForwardReason(this.serviceCode);
            String str = this.serviceInfoA;
            siToServiceClass(this.serviceInfoB);
            int iSiToTime = siToTime(this.serviceInfoC);
            long[] jArrConvertToLongTime = convertToLongTime(this.serviceInfoD);
            if (isInterrogate(this.ssAction)) {
                if (iScToCallForwardReason == 0) {
                    this.activePhone.getCallForwardInTimeSlot(iScToCallForwardReason, null);
                } else {
                    this.activePhone.getCallForwardingOption(iScToCallForwardReason, null);
                }
            } else if (isActivate(this.ssAction) || isDeactivate(this.ssAction)) {
                if (iScToCallForwardReason == 0 && jArrConvertToLongTime != null) {
                    Log.d(LOG_TAG, "onReceive: setCallForwardInTimeSlot");
                    this.activePhone.setCallForwardInTimeSlot(iActionToCommandAction, iScToCallForwardReason, str, iSiToTime, jArrConvertToLongTime, null);
                } else {
                    Log.d(LOG_TAG, "onReceive: setCallForwardingOption");
                    this.activePhone.setCallForwardingOption(iActionToCommandAction, iScToCallForwardReason, str, iSiToTime, null);
                }
            } else {
                Log.d(LOG_TAG, "onReceive: Not supported SS action");
            }
            return;
        }
        if (isServiceCodeCallBarring(this.serviceCode)) {
            boolean zIsActivate = isActivate(this.ssAction);
            String strScToBarringFacility = scToBarringFacility(this.serviceCode);
            siToServiceClass(this.serviceInfoB);
            if (isInterrogate(this.ssAction)) {
                this.activePhone.getCallBarring(strScToBarringFacility, "1234", null);
            } else if (isActivate(this.ssAction) || isDeactivate(this.ssAction)) {
                this.activePhone.setCallBarring(strScToBarringFacility, zIsActivate, "1234", null);
            } else {
                Log.d(LOG_TAG, "onReceive: Not supported SS action");
            }
            return;
        }
        if (this.serviceCode != null && this.serviceCode.equals(SC_WAIT)) {
            boolean zIsActivate2 = isActivate(this.ssAction);
            siToServiceClass(this.serviceInfoA);
            if (isInterrogate(this.ssAction)) {
                this.activePhone.getCallWaiting(null);
            } else if (isActivate(this.ssAction) || isDeactivate(this.ssAction)) {
                this.activePhone.setCallWaiting(zIsActivate2, null);
            } else {
                Log.d(LOG_TAG, "onReceive: Not supported SS action");
            }
            return;
        }
        if (this.serviceCode != null && this.serviceCode.equals(SC_CFUR)) {
            if (!isActivate(this.ssAction) && !isDeactivate(this.ssAction)) {
                Log.d(LOG_TAG, "onReceive: Not supported service code");
                return;
            }
            Intent intent = new Intent(ACTION_SUPPLEMENTARY_SERVICE_ROAMING_TEST);
            intent.putExtra(EXTRA_PHONE_ID, this.phoneId);
            this.mContext.sendBroadcast(intent);
            return;
        }
        int i = 1;
        if (this.serviceCode != null && this.serviceCode.equals(SC_CLIR)) {
            checkIMSStatus(SC_CLIR);
            if (!isActivate(this.ssAction)) {
                i = 2;
            }
            if (isInterrogate(this.ssAction)) {
                this.activePhone.getOutgoingCallerIdDisplay(null);
            } else if (isActivate(this.ssAction) || isDeactivate(this.ssAction)) {
                this.activePhone.setOutgoingCallerIdDisplay(i, null);
            } else {
                Log.d(LOG_TAG, "onReceive: Not supported SS action");
            }
            return;
        }
        if (this.serviceCode != null && this.serviceCode.equals(SC_CLIP)) {
            checkIMSStatus(SC_CLIP);
            boolean zIsActivate3 = isActivate(this.ssAction);
            if (isInterrogate(this.ssAction)) {
                try {
                    this.activeImsPhone.mCT.getUtInterface().queryCLIP((Message) null);
                } catch (ImsException e2) {
                    Log.d(LOG_TAG, "Could not get UT handle for queryCLIP.");
                }
            } else if (isActivate(this.ssAction) || isDeactivate(this.ssAction)) {
                try {
                    this.activeImsPhone.mCT.getUtInterface().updateCLIP(zIsActivate3, (Message) null);
                } catch (ImsException e3) {
                    Log.d(LOG_TAG, "Could not get UT handle for updateCLIP.");
                }
            } else {
                Log.d(LOG_TAG, "onReceive: Not supported SS action");
            }
            return;
        }
        if (this.serviceCode != null && this.serviceCode.equals(SC_COLR)) {
            checkIMSStatus(SC_COLR);
            if (!isActivate(this.ssAction)) {
                i = 0;
            }
            if (isInterrogate(this.ssAction)) {
                try {
                    this.activeImsPhone.mCT.getUtInterface().queryCOLR((Message) null);
                } catch (ImsException e4) {
                    Log.d(LOG_TAG, "processCode: Could not get UT handle for queryCOLR.");
                }
            } else if (isActivate(this.ssAction) || isDeactivate(this.ssAction)) {
                try {
                    this.activeImsPhone.mCT.getUtInterface().updateCOLR(i, (Message) null);
                } catch (ImsException e5) {
                    Log.d(LOG_TAG, "processCode: Could not get UT handle for updateCOLR.");
                }
            } else {
                Log.d(LOG_TAG, "onReceive: Not supported SS action");
            }
            return;
        }
        if (this.serviceCode != null && this.serviceCode.equals(SC_COLP)) {
            checkIMSStatus(SC_COLP);
            boolean zIsActivate4 = isActivate(this.ssAction);
            if (isInterrogate(this.ssAction)) {
                try {
                    this.activeImsPhone.mCT.getUtInterface().queryCOLP((Message) null);
                } catch (ImsException e6) {
                    Log.d(LOG_TAG, "processCode: Could not get UT handle for queryCOLP.");
                }
            } else if (isActivate(this.ssAction) || isDeactivate(this.ssAction)) {
                try {
                    this.activeImsPhone.mCT.getUtInterface().updateCOLP(zIsActivate4, (Message) null);
                } catch (ImsException e7) {
                    Log.d(LOG_TAG, "processCode: Could not get UT handle for updateCOLP.");
                }
            } else {
                Log.d(LOG_TAG, "onReceive: Not supported SS action");
            }
            return;
        }
        Log.d(LOG_TAG, "onReceive: Not supported service code");
        return;
        e.printStackTrace();
    }

    private static int getValidPhoneId(int i) {
        if (i >= 0 && i < TelephonyManager.getDefault().getPhoneCount()) {
            return i;
        }
        return 0;
    }

    private static boolean isServiceCodeCallForwarding(String str) {
        return str != null && (str.equals(SC_CFU) || str.equals(SC_CFB) || str.equals(SC_CFNRy) || str.equals(SC_CFNR) || str.equals(SC_CFNotRegister));
    }

    private static boolean isServiceCodeCallBarring(String str) {
        return str != null && (str.equals(SC_BAIC) || str.equals(SC_BAICr));
    }

    private static boolean isActivate(int i) {
        return i == 1;
    }

    private static boolean isDeactivate(int i) {
        return i == 0;
    }

    private static boolean isInterrogate(int i) {
        return i == 2;
    }

    private void checkIMSStatus(String str) {
        MtkImsPhone mtkImsPhone = this.activeImsPhone;
        if (mtkImsPhone != null && (mtkImsPhone.getServiceState().getState() == 0 || mtkImsPhone.isUtEnabled())) {
            Log.d(LOG_TAG, "checkIMSStatus: ready, code: " + str);
            return;
        }
        Log.d(LOG_TAG, "checkIMSStatus: IMS is not registered or not Ut enabled, code: " + str);
    }

    private static int actionToCommandAction(int i) {
        switch (i) {
            case 0:
                return 0;
            case 1:
                return 3;
            case 2:
                return 2;
            default:
                throw new RuntimeException("invalid action command");
        }
    }

    private static int scToCallForwardReason(String str) {
        if (str == null) {
            throw new RuntimeException("invalid call forward sc");
        }
        if (str.equals(SC_CFU)) {
            return 0;
        }
        if (str.equals(SC_CFB)) {
            return 1;
        }
        if (str.equals(SC_CFNR)) {
            return 3;
        }
        if (str.equals(SC_CFNRy)) {
            return 2;
        }
        if (str.equals(SC_CFNotRegister)) {
            return 6;
        }
        throw new RuntimeException("invalid call forward sc");
    }

    private static String scToBarringFacility(String str) {
        if (str == null) {
            throw new RuntimeException("invalid call barring sc");
        }
        if (str.equals(SC_BAIC)) {
            return "AI";
        }
        if (str.equals(SC_BAICr)) {
            return "IR";
        }
        throw new RuntimeException("invalid call barring sc");
    }

    private static int siToServiceClass(String str) {
        if (str == null || str.length() == 0) {
            return 0;
        }
        switch (Integer.parseInt(str, 10)) {
            case 1:
                return 1;
            case 2:
                return 512;
            default:
                throw new RuntimeException("unsupported service class " + str);
        }
    }

    private static int siToTime(String str) {
        if (str == null || str.length() == 0) {
            return 0;
        }
        return Integer.parseInt(str, 10);
    }

    private static long[] convertToLongTime(String str) {
        if (str == null) {
            return null;
        }
        String[] strArrSplit = str.split(",", 2);
        if (strArrSplit.length != 2) {
            return null;
        }
        long[] jArr = new long[2];
        for (int i = 0; i < 2; i++) {
            SimpleDateFormat simpleDateFormat = new SimpleDateFormat("HH:mm");
            simpleDateFormat.setTimeZone(TimeZone.getTimeZone("GMT+8"));
            try {
                jArr[i] = simpleDateFormat.parse(strArrSplit[i]).getTime();
            } catch (ParseException e) {
                e.printStackTrace();
                return null;
            }
        }
        return jArr;
    }
}
