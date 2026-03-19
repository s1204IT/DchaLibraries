package com.android.internal.telephony.gsm;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.AsyncResult;
import android.os.Message;
import android.os.SystemClock;
import android.provider.Telephony;
import android.telephony.CellLocation;
import android.telephony.SmsCbLocation;
import android.telephony.SmsCbMessage;
import android.telephony.SubscriptionManager;
import android.telephony.TelephonyManager;
import android.telephony.gsm.GsmCellLocation;
import android.text.TextUtils;
import com.android.internal.telephony.CellBroadcastHandler;
import com.android.internal.telephony.Phone;
import com.android.internal.telephony.ServiceStateTracker;
import com.mediatek.internal.telephony.CellBroadcastFwkExt;
import com.mediatek.internal.telephony.EtwsNotification;
import java.util.HashMap;
import java.util.Iterator;

public class GsmCellBroadcastHandler extends CellBroadcastHandler {
    private static final String INTENT_ETWS_ALARM = "com.android.internal.telephony.etws";
    private static final boolean VDBG = false;
    private static boolean mIsCellAreaInTw = false;
    private CellBroadcastFwkExt mCellBroadcastFwkExt;
    private PendingIntent mEtwsAlarmIntent;
    private final BroadcastReceiver mEtwsPrimaryBroadcastReceiver;
    private final BroadcastReceiver mPlmnChangedBroadcastReceiver;
    private final HashMap<SmsCbConcatInfo, byte[][]> mSmsCbPageMap;

    protected GsmCellBroadcastHandler(Context context, Phone phone) {
        super("GsmCellBroadcastHandler", context, phone);
        this.mSmsCbPageMap = new HashMap<>(4);
        this.mEtwsAlarmIntent = null;
        this.mCellBroadcastFwkExt = null;
        this.mEtwsPrimaryBroadcastReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context2, Intent intent) {
                if (!intent.getAction().equals(GsmCellBroadcastHandler.INTENT_ETWS_ALARM)) {
                    return;
                }
                long etws_sub = intent.getLongExtra("subscription", -1L);
                GsmCellBroadcastHandler.this.log("receive EVENT_ETWS_ALARM " + etws_sub);
                if (etws_sub != GsmCellBroadcastHandler.this.mPhone.getSubId()) {
                    return;
                }
                GsmCellBroadcastHandler.this.mCellBroadcastFwkExt.closeEtwsChannel(new EtwsNotification());
                GsmCellBroadcastHandler.this.stopEtwsAlarm();
            }
        };
        this.mPlmnChangedBroadcastReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context2, Intent intent) {
                if (!intent.getAction().equals("mediatek.intent.action.LOCATED_PLMN_CHANGED")) {
                    return;
                }
                String plmn = intent.getStringExtra(Telephony.CellBroadcasts.PLMN);
                if (TextUtils.isEmpty(plmn)) {
                    return;
                }
                String iso = intent.getStringExtra("iso");
                String mcc = plmn.substring(0, 3);
                GsmCellBroadcastHandler.this.log("receive Plmn Changed " + plmn + ", mcc " + mcc + ", iso " + iso);
                if (TextUtils.isEmpty(iso)) {
                    GsmCellBroadcastHandler.this.log("empty iso! It maybe test in lab so ignore this change");
                } else if (mcc.equals("466")) {
                    boolean unused = GsmCellBroadcastHandler.mIsCellAreaInTw = true;
                } else {
                    boolean unused2 = GsmCellBroadcastHandler.mIsCellAreaInTw = false;
                }
            }
        };
        phone.mCi.setOnNewGsmBroadcastSms(getHandler(), 1, null);
        phone.mCi.setOnEtwsNotification(getHandler(), ServiceStateTracker.NITZ_UPDATE_DIFF_DEFAULT, null);
        this.mCellBroadcastFwkExt = new CellBroadcastFwkExt(phone);
        IntentFilter filter = new IntentFilter();
        filter.addAction(INTENT_ETWS_ALARM);
        context.registerReceiver(this.mEtwsPrimaryBroadcastReceiver, filter);
        IntentFilter plmnFliter = new IntentFilter();
        plmnFliter.addAction("mediatek.intent.action.LOCATED_PLMN_CHANGED");
        context.registerReceiver(this.mPlmnChangedBroadcastReceiver, plmnFliter);
    }

    @Override
    protected void onQuitting() {
        this.mPhone.mCi.unSetOnNewGsmBroadcastSms(getHandler());
        this.mPhone.mCi.unSetOnEtwsNotification(getHandler());
        super.onQuitting();
    }

    public static GsmCellBroadcastHandler makeGsmCellBroadcastHandler(Context context, Phone phone) {
        GsmCellBroadcastHandler handler = new GsmCellBroadcastHandler(context, phone);
        handler.start();
        return handler;
    }

    @Override
    protected boolean handleSmsMessage(Message message) {
        SmsCbMessage cbMessage;
        if ((message.obj instanceof AsyncResult) && (cbMessage = handleGsmBroadcastSms((AsyncResult) message.obj)) != null) {
            handleBroadcastSms(cbMessage);
            return true;
        }
        return super.handleSmsMessage(message);
    }

    private SmsCbMessage handleGsmBroadcastSms(AsyncResult ar) {
        SmsCbLocation location;
        byte[][] pdus;
        try {
            byte[] receivedPdu = (byte[]) ar.result;
            SmsCbHeader header = new SmsCbHeader(receivedPdu, false);
            String plmn = TelephonyManager.from(this.mContext).getNetworkOperatorForPhone(this.mPhone.getPhoneId());
            int lac = -1;
            int cid = -1;
            CellLocation cl = this.mPhone.getCellLocation();
            if (cl instanceof GsmCellLocation) {
                GsmCellLocation cellLocation = (GsmCellLocation) cl;
                lac = cellLocation.getLac();
                cid = cellLocation.getCid();
            }
            switch (header.getGeographicalScope()) {
                case 0:
                case 3:
                    location = new SmsCbLocation(plmn, lac, cid);
                    break;
                case 1:
                default:
                    location = new SmsCbLocation(plmn);
                    break;
                case 2:
                    location = new SmsCbLocation(plmn, lac, -1);
                    break;
            }
            int pageCount = header.getNumberOfPages();
            if (pageCount > 1) {
                SmsCbConcatInfo concatInfo = new SmsCbConcatInfo(header, location);
                pdus = this.mSmsCbPageMap.get(concatInfo);
                if (pdus == null) {
                    pdus = new byte[pageCount][];
                    this.mSmsCbPageMap.put(concatInfo, pdus);
                }
                pdus[header.getPageIndex() - 1] = receivedPdu;
                for (byte[] pdu : pdus) {
                    if (pdu == null) {
                        return null;
                    }
                }
                this.mSmsCbPageMap.remove(concatInfo);
            } else {
                pdus = new byte[][]{receivedPdu};
            }
            Iterator<SmsCbConcatInfo> iter = this.mSmsCbPageMap.keySet().iterator();
            while (iter.hasNext()) {
                SmsCbConcatInfo info = iter.next();
                if (!info.matchesLocation(plmn, lac, cid)) {
                    iter.remove();
                }
            }
            if (header.getServiceCategory() == 4352 || header.getServiceCategory() == 4353 || header.getServiceCategory() == 4354 || header.getServiceCategory() == 4355 || header.getServiceCategory() == 4356) {
                stopEtwsAlarm();
                startEtwsAlarm();
            }
            return GsmSmsCbMessage.createSmsCbMessage(header, location, pdus);
        } catch (RuntimeException e) {
            loge("Error in decoding SMS CB pdu", e);
            return null;
        }
    }

    private static final class SmsCbConcatInfo {
        private final SmsCbHeader mHeader;
        private final SmsCbLocation mLocation;

        SmsCbConcatInfo(SmsCbHeader header, SmsCbLocation location) {
            this.mHeader = header;
            this.mLocation = location;
        }

        public int hashCode() {
            return (this.mHeader.getSerialNumber() * 31) + this.mLocation.hashCode();
        }

        public boolean equals(Object obj) {
            return (obj instanceof SmsCbConcatInfo) && this.mHeader.getSerialNumber() == obj.mHeader.getSerialNumber() && this.mLocation.equals(obj.mLocation) && this.mHeader.getServiceCategory() == obj.mHeader.getServiceCategory();
        }

        public boolean matchesLocation(String plmn, int lac, int cid) {
            return this.mLocation.isInLocationArea(plmn, lac, cid);
        }
    }

    @Override
    protected boolean handleEtwsPrimaryNotification(Message message) {
        if (message.obj instanceof AsyncResult) {
            AsyncResult ar = (AsyncResult) message.obj;
            EtwsNotification noti = (EtwsNotification) ar.result;
            log(noti.toString());
            boolean isDuplicated = this.mCellBroadcastFwkExt.containDuplicatedEtwsNotification(noti);
            if (!isDuplicated) {
                this.mCellBroadcastFwkExt.openEtwsChannel(noti);
                SmsCbMessage etwsPrimary = handleEtwsPdu(noti.getEtwsPdu(), noti.plmnId);
                if (etwsPrimary != null) {
                    log("ETWS Primary dispatch to App, open necessary channels and start timer");
                    handleBroadcastSms(etwsPrimary, true);
                    stopEtwsAlarm();
                    startEtwsAlarm();
                    return true;
                }
            } else {
                log("find duplicated ETWS notifiction");
                return false;
            }
        }
        return super.handleEtwsPrimaryNotification(message);
    }

    private SmsCbMessage handleEtwsPdu(byte[] pdu, String plmn) {
        SmsCbLocation location;
        if (pdu == null || pdu.length != 56) {
            log("invalid ETWS PDU");
            return null;
        }
        SmsCbHeader header = new SmsCbHeader(pdu, true);
        GsmCellLocation cellLocation = (GsmCellLocation) this.mPhone.getCellLocation();
        int lac = cellLocation.getLac();
        int cid = cellLocation.getCid();
        switch (header.getGeographicalScope()) {
            case 0:
            case 3:
                location = new SmsCbLocation(plmn, lac, cid);
                break;
            case 1:
            default:
                location = new SmsCbLocation(plmn);
                break;
            case 2:
                location = new SmsCbLocation(plmn, lac, -1);
                break;
        }
        byte[][] pdus = {pdu};
        return GsmSmsCbMessage.createSmsCbMessage(header, location, pdus);
    }

    protected void startEtwsAlarm() {
        AlarmManager am = (AlarmManager) this.mPhone.getContext().getSystemService("alarm");
        log("startEtwsAlarm");
        Intent intent = new Intent(INTENT_ETWS_ALARM);
        SubscriptionManager.putPhoneIdAndSubIdExtra(intent, this.mPhone.getPhoneId());
        this.mEtwsAlarmIntent = PendingIntent.getBroadcast(this.mPhone.getContext(), 0, intent, 134217728);
        am.set(2, SystemClock.elapsedRealtime() + 1800000, this.mEtwsAlarmIntent);
    }

    protected void stopEtwsAlarm() {
        AlarmManager am = (AlarmManager) this.mPhone.getContext().getSystemService("alarm");
        log("stopEtwsAlarm");
        if (this.mEtwsAlarmIntent == null) {
            return;
        }
        am.cancel(this.mEtwsAlarmIntent);
        this.mEtwsAlarmIntent = null;
    }

    public static boolean isCellAreaInTw() {
        return mIsCellAreaInTw;
    }
}
