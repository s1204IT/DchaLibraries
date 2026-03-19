package com.mediatek.internal.telephony.gsm;

import android.content.Context;
import android.os.AsyncResult;
import android.os.Message;
import android.telephony.CellLocation;
import android.telephony.SmsCbLocation;
import android.telephony.SmsCbMessage;
import android.telephony.TelephonyManager;
import android.telephony.gsm.GsmCellLocation;
import com.android.internal.telephony.Phone;
import com.android.internal.telephony.WakeLockStateMachine;
import com.android.internal.telephony.gsm.GsmCellBroadcastHandler;
import com.android.internal.telephony.gsm.GsmSmsCbMessage;
import com.mediatek.internal.telephony.MtkEtwsNotification;
import java.util.Iterator;

public class MtkGsmCellBroadcastHandler extends GsmCellBroadcastHandler {
    protected static final int EVENT_NEW_ETWS_NOTIFICATION = 2000;
    private static final boolean VDBG = false;

    public MtkGsmCellBroadcastHandler(Context context, Phone phone) {
        super("MtkGsmCellBroadcastHandler", context, phone, (Object) null);
        this.mDefaultState = new DefaultStateEx();
        this.mIdleState = new IdleStateEx();
        this.mWaitingState = new WaitingStateEx();
        addState(this.mDefaultState);
        addState(this.mIdleState, this.mDefaultState);
        addState(this.mWaitingState, this.mDefaultState);
        setInitialState(this.mIdleState);
        phone.mCi.setOnEtwsNotification(getHandler(), 2000, null);
    }

    protected void onQuitting() {
        this.mPhone.mCi.unSetOnEtwsNotification(getHandler());
        super.onQuitting();
    }

    class DefaultStateEx extends WakeLockStateMachine.DefaultState {
        DefaultStateEx() {
            super(MtkGsmCellBroadcastHandler.this);
        }

        public boolean processMessage(Message message) {
            int i = message.what;
            return super.processMessage(message);
        }
    }

    class IdleStateEx extends WakeLockStateMachine.IdleState {
        IdleStateEx() {
            super(MtkGsmCellBroadcastHandler.this);
        }

        public boolean processMessage(Message message) {
            if (message.what == 2000) {
                MtkGsmCellBroadcastHandler.this.log("receive ETWS notification");
                if (MtkGsmCellBroadcastHandler.this.handleEtwsPrimaryNotification(message)) {
                    MtkGsmCellBroadcastHandler.this.transitionTo(MtkGsmCellBroadcastHandler.this.mWaitingState);
                    return true;
                }
                return true;
            }
            return super.processMessage(message);
        }
    }

    class WaitingStateEx extends WakeLockStateMachine.WaitingState {
        WaitingStateEx() {
            super(MtkGsmCellBroadcastHandler.this);
        }

        public boolean processMessage(Message message) {
            if (message.what == 2000) {
                MtkGsmCellBroadcastHandler.this.log("deferring message until return to idle");
                MtkGsmCellBroadcastHandler.this.deferMessage(message);
                return true;
            }
            return super.processMessage(message);
        }
    }

    protected SmsCbMessage handleGsmBroadcastSms(AsyncResult asyncResult) {
        int cid;
        int lac;
        SmsCbLocation smsCbLocation;
        byte[][] bArr;
        try {
            byte[] bArr2 = (byte[]) asyncResult.result;
            String networkOperatorForPhone = TelephonyManager.from(this.mContext).getNetworkOperatorForPhone(this.mPhone.getPhoneId());
            MtkSmsCbHeader mtkSmsCbHeader = new MtkSmsCbHeader(bArr2, networkOperatorForPhone, false);
            CellLocation cellLocation = this.mPhone.getCellLocation();
            if (cellLocation instanceof GsmCellLocation) {
                GsmCellLocation gsmCellLocation = (GsmCellLocation) cellLocation;
                lac = gsmCellLocation.getLac();
                cid = gsmCellLocation.getCid();
            } else {
                cid = -1;
                lac = -1;
            }
            int geographicalScope = mtkSmsCbHeader.getGeographicalScope();
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
            int numberOfPages = mtkSmsCbHeader.getNumberOfPages();
            if (numberOfPages > 1) {
                GsmCellBroadcastHandler.SmsCbConcatInfo smsCbConcatInfo = new GsmCellBroadcastHandler.SmsCbConcatInfo(mtkSmsCbHeader, smsCbLocation);
                bArr = (byte[][]) this.mSmsCbPageMap.get(smsCbConcatInfo);
                if (bArr == null) {
                    bArr = new byte[numberOfPages][];
                    this.mSmsCbPageMap.put(smsCbConcatInfo, bArr);
                }
                bArr[mtkSmsCbHeader.getPageIndex() - 1] = bArr2;
                for (byte[] bArr3 : bArr) {
                    if (bArr3 == null) {
                        return null;
                    }
                }
                this.mSmsCbPageMap.remove(smsCbConcatInfo);
            } else {
                bArr = new byte[][]{bArr2};
            }
            Iterator it = this.mSmsCbPageMap.keySet().iterator();
            while (it.hasNext()) {
                if (!((GsmCellBroadcastHandler.SmsCbConcatInfo) it.next()).matchesLocation(networkOperatorForPhone, lac, cid)) {
                    it.remove();
                }
            }
            return GsmSmsCbMessage.createSmsCbMessage(this.mContext, mtkSmsCbHeader, smsCbLocation, bArr);
        } catch (RuntimeException e) {
            loge("Error in decoding SMS CB pdu", e);
            return null;
        }
    }

    private boolean handleEtwsPrimaryNotification(Message message) {
        if (message.obj instanceof AsyncResult) {
            MtkEtwsNotification mtkEtwsNotification = (MtkEtwsNotification) ((AsyncResult) message.obj).result;
            log(mtkEtwsNotification.toString());
            SmsCbMessage smsCbMessageHandleEtwsPdu = handleEtwsPdu(mtkEtwsNotification.getEtwsPdu(), mtkEtwsNotification.plmnId);
            if (smsCbMessageHandleEtwsPdu != null) {
                log("ETWS Primary dispatch to App");
                handleBroadcastSms(smsCbMessageHandleEtwsPdu);
                return true;
            }
            return false;
        }
        return false;
    }

    private SmsCbMessage handleEtwsPdu(byte[] bArr, String str) {
        int cid;
        int lac;
        SmsCbLocation smsCbLocation;
        if (bArr == null || bArr.length != 56) {
            log("invalid ETWS PDU");
            return null;
        }
        MtkSmsCbHeader mtkSmsCbHeader = new MtkSmsCbHeader(bArr, str, true);
        CellLocation cellLocation = this.mPhone.getCellLocation();
        if (cellLocation instanceof GsmCellLocation) {
            GsmCellLocation gsmCellLocation = (GsmCellLocation) cellLocation;
            lac = gsmCellLocation.getLac();
            cid = gsmCellLocation.getCid();
        } else {
            cid = -1;
            lac = -1;
        }
        int geographicalScope = mtkSmsCbHeader.getGeographicalScope();
        if (geographicalScope != 0) {
            switch (geographicalScope) {
                case 2:
                    smsCbLocation = new SmsCbLocation(str, lac, -1);
                    break;
                case 3:
                    smsCbLocation = new SmsCbLocation(str, lac, cid);
                    break;
                default:
                    smsCbLocation = new SmsCbLocation(str);
                    break;
            }
        }
        return GsmSmsCbMessage.createSmsCbMessage(this.mContext, mtkSmsCbHeader, smsCbLocation, new byte[][]{bArr});
    }

    public static MtkGsmCellBroadcastHandler makeGsmCellBroadcastHandler(Context context, Phone phone) {
        MtkGsmCellBroadcastHandler mtkGsmCellBroadcastHandler = new MtkGsmCellBroadcastHandler(context, phone);
        mtkGsmCellBroadcastHandler.start();
        return mtkGsmCellBroadcastHandler;
    }
}
