package com.android.internal.telephony.gsm;

import android.content.Context;
import android.os.AsyncResult;
import android.os.Message;
import android.telephony.CellLocation;
import android.telephony.SmsCbLocation;
import android.telephony.SmsCbMessage;
import android.telephony.TelephonyManager;
import android.telephony.gsm.GsmCellLocation;
import com.android.internal.telephony.CellBroadcastHandler;
import com.android.internal.telephony.Phone;
import java.util.HashMap;
import java.util.Iterator;

public class GsmCellBroadcastHandler extends CellBroadcastHandler {
    private static final boolean VDBG = false;
    protected final HashMap<SmsCbConcatInfo, byte[][]> mSmsCbPageMap;

    protected GsmCellBroadcastHandler(Context context, Phone phone) {
        super("GsmCellBroadcastHandler", context, phone);
        this.mSmsCbPageMap = new HashMap<>(4);
        phone.mCi.setOnNewGsmBroadcastSms(getHandler(), 1, null);
    }

    protected GsmCellBroadcastHandler(String str, Context context, Phone phone, Object obj) {
        super(str, context, phone, obj);
        this.mSmsCbPageMap = new HashMap<>(4);
        phone.mCi.setOnNewGsmBroadcastSms(getHandler(), 1, null);
    }

    @Override
    protected void onQuitting() {
        this.mPhone.mCi.unSetOnNewGsmBroadcastSms(getHandler());
        super.onQuitting();
    }

    public static GsmCellBroadcastHandler makeGsmCellBroadcastHandler(Context context, Phone phone) {
        GsmCellBroadcastHandler gsmCellBroadcastHandler = new GsmCellBroadcastHandler(context, phone);
        gsmCellBroadcastHandler.start();
        return gsmCellBroadcastHandler;
    }

    @Override
    protected boolean handleSmsMessage(Message message) {
        SmsCbMessage smsCbMessageHandleGsmBroadcastSms;
        if ((message.obj instanceof AsyncResult) && (smsCbMessageHandleGsmBroadcastSms = handleGsmBroadcastSms((AsyncResult) message.obj)) != null) {
            handleBroadcastSms(smsCbMessageHandleGsmBroadcastSms);
            return true;
        }
        return super.handleSmsMessage(message);
    }

    protected SmsCbMessage handleGsmBroadcastSms(AsyncResult asyncResult) {
        int cid;
        int lac;
        SmsCbLocation smsCbLocation;
        byte[][] bArr;
        try {
            byte[] bArr2 = (byte[]) asyncResult.result;
            SmsCbHeader smsCbHeader = new SmsCbHeader(bArr2);
            String networkOperatorForPhone = TelephonyManager.from(this.mContext).getNetworkOperatorForPhone(this.mPhone.getPhoneId());
            CellLocation cellLocation = this.mPhone.getCellLocation();
            if (cellLocation instanceof GsmCellLocation) {
                GsmCellLocation gsmCellLocation = (GsmCellLocation) cellLocation;
                lac = gsmCellLocation.getLac();
                cid = gsmCellLocation.getCid();
            } else {
                cid = -1;
                lac = -1;
            }
            int geographicalScope = smsCbHeader.getGeographicalScope();
            if (geographicalScope != 0) {
                switch (geographicalScope) {
                    case 2:
                        smsCbLocation = new SmsCbLocation(networkOperatorForPhone, lac, -1);
                        break;
                    case 3:
                        smsCbLocation = new SmsCbLocation(networkOperatorForPhone, lac, cid);
                        break;
                    default:
                        smsCbLocation = new SmsCbLocation(networkOperatorForPhone);
                        break;
                }
            }
            int numberOfPages = smsCbHeader.getNumberOfPages();
            if (numberOfPages > 1) {
                SmsCbConcatInfo smsCbConcatInfo = new SmsCbConcatInfo(smsCbHeader, smsCbLocation);
                bArr = this.mSmsCbPageMap.get(smsCbConcatInfo);
                if (bArr == null) {
                    bArr = new byte[numberOfPages][];
                    this.mSmsCbPageMap.put(smsCbConcatInfo, bArr);
                }
                bArr[smsCbHeader.getPageIndex() - 1] = bArr2;
                for (byte[] bArr3 : bArr) {
                    if (bArr3 == null) {
                        log("still missing pdu");
                        return null;
                    }
                }
                this.mSmsCbPageMap.remove(smsCbConcatInfo);
            } else {
                bArr = new byte[][]{bArr2};
            }
            Iterator<SmsCbConcatInfo> it = this.mSmsCbPageMap.keySet().iterator();
            while (it.hasNext()) {
                if (!it.next().matchesLocation(networkOperatorForPhone, lac, cid)) {
                    it.remove();
                }
            }
            return GsmSmsCbMessage.createSmsCbMessage(this.mContext, smsCbHeader, smsCbLocation, bArr);
        } catch (RuntimeException e) {
            loge("Error in decoding SMS CB pdu", e);
            return null;
        }
    }

    protected static final class SmsCbConcatInfo {
        private final SmsCbHeader mHeader;
        private final SmsCbLocation mLocation;

        public SmsCbConcatInfo(SmsCbHeader smsCbHeader, SmsCbLocation smsCbLocation) {
            this.mHeader = smsCbHeader;
            this.mLocation = smsCbLocation;
        }

        public int hashCode() {
            return (this.mHeader.getSerialNumber() * 31) + this.mLocation.hashCode();
        }

        public boolean equals(Object obj) {
            if (!(obj instanceof SmsCbConcatInfo)) {
                return false;
            }
            SmsCbConcatInfo smsCbConcatInfo = (SmsCbConcatInfo) obj;
            return this.mHeader.getSerialNumber() == smsCbConcatInfo.mHeader.getSerialNumber() && this.mLocation.equals(smsCbConcatInfo.mLocation) && this.mHeader.getServiceCategory() == smsCbConcatInfo.mHeader.getServiceCategory();
        }

        public boolean matchesLocation(String str, int i, int i2) {
            return this.mLocation.isInLocationArea(str, i, i2);
        }
    }
}
