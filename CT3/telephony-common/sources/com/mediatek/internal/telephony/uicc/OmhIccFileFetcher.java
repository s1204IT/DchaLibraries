package com.mediatek.internal.telephony.uicc;

import android.content.Context;
import android.content.Intent;
import android.os.Message;
import android.os.SystemProperties;
import com.android.internal.telephony.Phone;
import java.util.ArrayList;

public final class OmhIccFileFetcher extends IccFileFetcherBase {
    private static final String OMH_CARD_NO = "0";
    private static final int OMH_CARD_QUERY = 1001;
    private static final int OMH_CARD_RETRY_COUNT = 5;
    private static final int OMH_CARD_RETRY_INTERVAL = 1000;
    private static final String OMH_CARD_UNKNOWN = "-1";
    private static final String OMH_CARD_YES = "1";
    private static final String OMH_INFO_READY = "omh_info_ready";
    private static final String TAG = "OmhIccFileFetcher";
    ArrayList<String> mFileList;
    private int mRetryTimes;

    public OmhIccFileFetcher(Context c, Phone phone) {
        super(c, phone);
        this.mRetryTimes = 0;
        this.mFileList = new ArrayList<>();
        this.mFileList.add(OMH_INFO_READY);
    }

    @Override
    public ArrayList<String> onGetKeys() {
        return this.mFileList;
    }

    @Override
    public IccFileRequest onGetFilePara(String key) {
        return null;
    }

    @Override
    public void onParseResult(String key, byte[] transparent, ArrayList<byte[]> linearfixed) {
    }

    void retryCheckOmhCard() {
        String omh = SystemProperties.get("ril.cdma.card.omh", OMH_CARD_UNKNOWN);
        log("retryCheckOmhCard with omh = " + omh + " mRetryTimes = " + this.mRetryTimes);
        if (OMH_CARD_UNKNOWN.equals(omh) && this.mRetryTimes < 5) {
            this.mRetryTimes++;
            sendEmptyMessageDelayed(1001, 1000L);
            log("retryCheckOmhCard, retry again.");
        } else {
            if (OMH_CARD_UNKNOWN.equals(omh)) {
                this.mData.put(OMH_INFO_READY, "0");
            } else {
                this.mData.put(OMH_INFO_READY, omh);
            }
            notifyOmhCardDone(true);
            log("retryCheckOmhCard, notify app the check is ready.");
        }
    }

    private void notifyOmhCardDone(boolean state) {
        log("notifyOmhCardDone, check omh card is done with state = " + state);
        Intent intent = new Intent("com.mediatek.internal.omh.cardcheck");
        intent.putExtra("subid", this.mPhone.getSubId());
        intent.putExtra("is_ready", state ? "yes" : "no");
        this.mContext.sendBroadcast(intent);
    }

    @Override
    protected void exchangeSimInfo() {
        log("exchangeSimInfo, just check the property.");
        String omh = SystemProperties.get("ril.cdma.card.omh", OMH_CARD_UNKNOWN);
        log("exchangeSimInfo, ril.cdma.card.omh = " + omh);
        if (OMH_CARD_UNKNOWN.equals(omh)) {
            retryCheckOmhCard();
            this.mRetryTimes = 0;
        } else {
            this.mData.put(OMH_INFO_READY, omh);
            notifyOmhCardDone(true);
        }
    }

    @Override
    public void handleMessage(Message msg) {
        switch (msg.what) {
            case 1001:
                retryCheckOmhCard();
                break;
            default:
                super.handleMessage(msg);
                break;
        }
    }

    protected boolean isOmhCard() {
        if (this.mData.containsKey(OMH_INFO_READY)) {
            String omhState = (String) this.mData.get(OMH_INFO_READY);
            return "1".equals(omhState);
        }
        String omhCard = SystemProperties.get("ril.cdma.card.omh", OMH_CARD_UNKNOWN);
        if (!OMH_CARD_UNKNOWN.equals(omhCard)) {
            log("isOmhCard(), omh info maybe not ready, but the card check is done!!!!!!");
            return "1".equals(omhCard);
        }
        return false;
    }
}
