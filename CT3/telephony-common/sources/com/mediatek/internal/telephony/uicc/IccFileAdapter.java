package com.mediatek.internal.telephony.uicc;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.telephony.Rlog;
import com.android.internal.telephony.Phone;
import java.util.HashMap;

public class IccFileAdapter {
    private static final String TAG = "IccFileAdapter";
    private static IccFileAdapter sInstance;
    private Context mContext;
    private MmsIccFileFetcher mMmsIccFileFetcher;
    private OmhIccFileFetcher mOmhIccFileFetcher;
    private Phone mPhone;
    private int mPhoneId;
    private final BroadcastReceiver mReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            IccFileAdapter.this.mMmsIccFileFetcher.onHandleIntent(intent);
            IccFileAdapter.this.mOmhIccFileFetcher.onHandleIntent(intent);
            IccFileAdapter.this.mSsIccFileFetcher.onHandleIntent(intent);
            IccFileAdapter.this.mSmsbcIccFileFetcher.onHandleIntent(intent);
            IccFileAdapter.this.mSmsIccFileFetcher.onHandleIntent(intent);
        }
    };
    private SmsIccFileFetcher mSmsIccFileFetcher;
    private SmsbcIccFileFetcher mSmsbcIccFileFetcher;
    private SsIccFileFetcher mSsIccFileFetcher;

    public IccFileAdapter(Context c, Phone phone) {
        log("IccFileAdapter Creating!");
        this.mContext = c;
        this.mPhone = phone;
        this.mPhoneId = this.mPhone.getPhoneId();
        this.mMmsIccFileFetcher = new MmsIccFileFetcher(this.mContext, phone);
        this.mOmhIccFileFetcher = new OmhIccFileFetcher(this.mContext, phone);
        this.mSsIccFileFetcher = new SsIccFileFetcher(this.mContext, phone);
        this.mSmsbcIccFileFetcher = new SmsbcIccFileFetcher(this.mContext, phone);
        this.mSmsIccFileFetcher = new SmsIccFileFetcher(this.mContext, phone);
        IntentFilter filter = new IntentFilter();
        filter.addAction("android.intent.action.SIM_STATE_CHANGED");
        filter.addAction("android.intent.action.RADIO_TECHNOLOGY");
        this.mContext.registerReceiver(this.mReceiver, filter);
    }

    protected void log(String msg) {
        Rlog.d(TAG, msg + " (phoneId " + this.mPhoneId + ")");
    }

    protected void loge(String msg) {
        Rlog.e(TAG, msg + " (phoneId " + this.mPhoneId + ")");
    }

    public boolean isOmhCard() {
        return this.mOmhIccFileFetcher.isOmhCard();
    }

    public Object getMmsConfigInfo() {
        return this.mMmsIccFileFetcher.getMmsConfigInfo();
    }

    public Object getMmsIcpInfo() {
        return this.mMmsIccFileFetcher.getMmsIcpInfo();
    }

    public HashMap<String, Object> getSsFeatureCode(int subId) {
        return this.mSsIccFileFetcher.mData;
    }

    public int[] getFcsForApp(int start, int end, int subId) {
        return this.mSsIccFileFetcher.getFcsForApp(start, end, subId);
    }

    public SmsbcIccFileFetcher getSmsbcIccFileFetcher() {
        return this.mSmsbcIccFileFetcher;
    }

    public SmsIccFileFetcher getSmsIccFileFetcher() {
        return this.mSmsIccFileFetcher;
    }

    public int getBcsmsCfgFromRuim(int userCategory, int userPriority) {
        return this.mSmsbcIccFileFetcher.getBcsmsCfgFromRuim(userCategory, userPriority);
    }

    public int getNextMessageId() {
        return this.mSmsIccFileFetcher.getNextMessageId();
    }

    public int getWapMsgId() {
        return this.mSmsIccFileFetcher.getWapMsgId();
    }
}
