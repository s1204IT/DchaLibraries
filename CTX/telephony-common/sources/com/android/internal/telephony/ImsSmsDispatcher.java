package com.android.internal.telephony;

import android.os.RemoteException;
import android.telephony.Rlog;
import android.telephony.SmsMessage;
import android.telephony.ims.ImsReasonInfo;
import android.telephony.ims.aidl.IImsSmsListener;
import android.telephony.ims.feature.ImsFeature;
import android.telephony.ims.stub.ImsRegistrationImplBase;
import android.util.Pair;
import com.android.ims.ImsException;
import com.android.ims.ImsManager;
import com.android.internal.annotations.VisibleForTesting;
import com.android.internal.telephony.GsmAlphabet;
import com.android.internal.telephony.ImsSmsDispatcher;
import com.android.internal.telephony.SMSDispatcher;
import com.android.internal.telephony.SmsDispatchersController;
import com.android.internal.telephony.SmsMessageBase;
import com.android.internal.telephony.util.SMSDispatcherUtil;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

public class ImsSmsDispatcher extends SMSDispatcher {
    private static final String TAG = "ImsSmsDispacher";
    private ImsFeature.CapabilityCallback mCapabilityCallback;
    private final ImsManager.Connector mImsManagerConnector;
    private final IImsSmsListener mImsSmsListener;
    private volatile boolean mIsImsServiceUp;
    private volatile boolean mIsRegistered;
    private volatile boolean mIsSmsCapable;
    private final Object mLock;

    @VisibleForTesting
    public AtomicInteger mNextToken;
    private ImsRegistrationImplBase.Callback mRegistrationCallback;

    @VisibleForTesting
    public Map<Integer, SMSDispatcher.SmsTracker> mTrackers;

    class AnonymousClass3 extends IImsSmsListener.Stub {
        AnonymousClass3() {
        }

        public void onSendSmsResult(int i, int i2, int i3, int i4) throws RemoteException {
            Rlog.d(ImsSmsDispatcher.TAG, "onSendSmsResult token=" + i + " messageRef=" + i2 + " status=" + i3 + " reason=" + i4);
            SMSDispatcher.SmsTracker smsTracker = ImsSmsDispatcher.this.mTrackers.get(Integer.valueOf(i));
            if (smsTracker == null) {
                throw new IllegalArgumentException("Invalid token.");
            }
            smsTracker.mMessageRef = i2;
            ImsSmsDispatcher.this.handleImsSmsSendResult(smsTracker, ImsSmsDispatcher.this.mTrackers, i, i3);
            switch (i3) {
                case 1:
                    smsTracker.onSent(ImsSmsDispatcher.this.mContext);
                    return;
                case 2:
                    smsTracker.onFailed(ImsSmsDispatcher.this.mContext, i4, 0);
                    ImsSmsDispatcher.this.mTrackers.remove(Integer.valueOf(i));
                    return;
                case 3:
                    smsTracker.mRetryCount++;
                    ImsSmsDispatcher.this.sendSms(smsTracker);
                    return;
                case 4:
                    ImsSmsDispatcher.this.fallbackToPstn(i, smsTracker);
                    return;
                default:
                    return;
            }
        }

        public void onSmsStatusReportReceived(int i, int i2, String str, byte[] bArr) throws RemoteException {
            Rlog.d(ImsSmsDispatcher.TAG, "Status report received.");
            SMSDispatcher.SmsTracker smsTracker = ImsSmsDispatcher.this.mTrackers.get(Integer.valueOf(i));
            if (smsTracker == null) {
                throw new RemoteException("Invalid token.");
            }
            Pair<Boolean, Boolean> pairHandleSmsStatusReport = ImsSmsDispatcher.this.mSmsDispatchersController.handleSmsStatusReport(smsTracker, str, bArr);
            Rlog.d(ImsSmsDispatcher.TAG, "Status report handle result, success: " + pairHandleSmsStatusReport.first + "complete: " + pairHandleSmsStatusReport.second);
            try {
                ImsSmsDispatcher.this.getImsManager().acknowledgeSmsReport(i, i2, ((Boolean) pairHandleSmsStatusReport.first).booleanValue() ? 1 : 2);
            } catch (ImsException e) {
                Rlog.e(ImsSmsDispatcher.TAG, "Failed to acknowledgeSmsReport(). Error: " + e.getMessage());
            }
            if (((Boolean) pairHandleSmsStatusReport.second).booleanValue()) {
                ImsSmsDispatcher.this.mTrackers.remove(Integer.valueOf(i));
            }
        }

        public void onSmsReceived(final int i, String str, byte[] bArr) throws RemoteException {
            Rlog.d(ImsSmsDispatcher.TAG, "SMS received.");
            final SmsMessage smsMessageCreateFromPdu = SmsMessage.createFromPdu(bArr, str);
            ImsSmsDispatcher.this.mSmsDispatchersController.injectSmsPdu(smsMessageCreateFromPdu, str, new SmsDispatchersController.SmsInjectionCallback() {
                @Override
                public final void onSmsInjectedResult(int i2) {
                    ImsSmsDispatcher.AnonymousClass3.lambda$onSmsReceived$0(this.f$0, smsMessageCreateFromPdu, i, i2);
                }
            }, true);
        }

        public static void lambda$onSmsReceived$0(AnonymousClass3 anonymousClass3, SmsMessage smsMessage, int i, int i2) {
            Rlog.d(ImsSmsDispatcher.TAG, "SMS handled result: " + i2);
            int i3 = 1;
            if (i2 != 1) {
                switch (i2) {
                    case 3:
                        i3 = 3;
                        break;
                    case 4:
                        i3 = 4;
                        break;
                    default:
                        i3 = 2;
                        break;
                }
            }
            if (smsMessage != null) {
                try {
                    if (smsMessage.mWrappedSmsMessage != null) {
                        ImsSmsDispatcher.this.getImsManager().acknowledgeSms(i, smsMessage.mWrappedSmsMessage.mMessageRef, i3);
                    } else {
                        Rlog.w(ImsSmsDispatcher.TAG, "SMS Received with a PDU that could not be parsed.");
                        ImsSmsDispatcher.this.getImsManager().acknowledgeSms(i, 0, i3);
                    }
                } catch (ImsException e) {
                    Rlog.e(ImsSmsDispatcher.TAG, "Failed to acknowledgeSms(). Error: " + e.getMessage());
                }
            }
        }
    }

    public ImsSmsDispatcher(Phone phone, SmsDispatchersController smsDispatchersController) {
        super(phone, smsDispatchersController);
        this.mTrackers = new ConcurrentHashMap();
        this.mNextToken = new AtomicInteger();
        this.mLock = new Object();
        this.mRegistrationCallback = new ImsRegistrationImplBase.Callback() {
            public void onRegistered(int i) {
                Rlog.d(ImsSmsDispatcher.TAG, "onImsConnected imsRadioTech=" + i);
                synchronized (ImsSmsDispatcher.this.mLock) {
                    ImsSmsDispatcher.this.mIsRegistered = true;
                }
            }

            public void onRegistering(int i) {
                Rlog.d(ImsSmsDispatcher.TAG, "onImsProgressing imsRadioTech=" + i);
                synchronized (ImsSmsDispatcher.this.mLock) {
                    ImsSmsDispatcher.this.mIsRegistered = false;
                }
            }

            public void onDeregistered(ImsReasonInfo imsReasonInfo) {
                Rlog.d(ImsSmsDispatcher.TAG, "onImsDisconnected imsReasonInfo=" + imsReasonInfo);
                synchronized (ImsSmsDispatcher.this.mLock) {
                    ImsSmsDispatcher.this.mIsRegistered = false;
                }
            }
        };
        this.mCapabilityCallback = new ImsFeature.CapabilityCallback() {
            public void onCapabilitiesStatusChanged(ImsFeature.Capabilities capabilities) {
                synchronized (ImsSmsDispatcher.this.mLock) {
                    ImsSmsDispatcher.this.mIsSmsCapable = capabilities.isCapable(8);
                }
            }
        };
        this.mImsSmsListener = new AnonymousClass3();
        this.mImsManagerConnector = new ImsManager.Connector(this.mContext, this.mPhone.getPhoneId(), new ImsManager.Connector.Listener() {
            public void connectionReady(ImsManager imsManager) throws ImsException {
                Rlog.d(ImsSmsDispatcher.TAG, "ImsManager: connection ready.");
                synchronized (ImsSmsDispatcher.this.mLock) {
                    ImsSmsDispatcher.this.setListeners();
                    ImsSmsDispatcher.this.mIsImsServiceUp = true;
                }
            }

            public void connectionUnavailable() {
                Rlog.d(ImsSmsDispatcher.TAG, "ImsManager: connection unavailable.");
                synchronized (ImsSmsDispatcher.this.mLock) {
                    ImsSmsDispatcher.this.mIsImsServiceUp = false;
                }
            }
        });
        this.mImsManagerConnector.connect();
    }

    private void setListeners() throws ImsException {
        getImsManager().addRegistrationCallback(this.mRegistrationCallback);
        getImsManager().addCapabilitiesCallback(this.mCapabilityCallback);
        getImsManager().setSmsListener(this.mImsSmsListener);
        getImsManager().onSmsReady();
    }

    public boolean isAvailable() {
        boolean z;
        synchronized (this.mLock) {
            Rlog.d(TAG, "isAvailable: up=" + this.mIsImsServiceUp + ", reg= " + this.mIsRegistered + ", cap= " + this.mIsSmsCapable);
            z = this.mIsImsServiceUp && this.mIsRegistered && this.mIsSmsCapable;
        }
        return z;
    }

    @Override
    public String getFormat() {
        try {
            return getImsManager().getSmsFormat();
        } catch (ImsException e) {
            Rlog.e(TAG, "Failed to get sms format. Error: " + e.getMessage());
            return "unknown";
        }
    }

    @Override
    protected boolean shouldBlockSmsForEcbm() {
        return false;
    }

    @Override
    protected SmsMessageBase.SubmitPduBase getSubmitPdu(String str, String str2, String str3, boolean z, SmsHeader smsHeader, int i, int i2) {
        return SMSDispatcherUtil.getSubmitPdu(isCdmaMo(), str, str2, str3, z, smsHeader, i, i2);
    }

    @Override
    protected SmsMessageBase.SubmitPduBase getSubmitPdu(String str, String str2, int i, byte[] bArr, boolean z) {
        return SMSDispatcherUtil.getSubmitPdu(isCdmaMo(), str, str2, i, bArr, z);
    }

    @Override
    protected GsmAlphabet.TextEncodingDetails calculateLength(CharSequence charSequence, boolean z) {
        return SMSDispatcherUtil.calculateLength(isCdmaMo(), charSequence, z);
    }

    @Override
    public void sendSms(SMSDispatcher.SmsTracker smsTracker) {
        Rlog.d(TAG, "sendSms:  mRetryCount=" + smsTracker.mRetryCount + " mMessageRef=" + smsTracker.mMessageRef + " SS=" + this.mPhone.getServiceState().getState());
        smsTracker.mUsesImsServiceForIms = true;
        HashMap<String, Object> data = smsTracker.getData();
        byte[] bArr = (byte[]) data.get("pdu");
        byte[] bArr2 = (byte[]) data.get("smsc");
        boolean z = smsTracker.mRetryCount > 0;
        if ("3gpp".equals(getFormat()) && smsTracker.mRetryCount > 0 && (bArr[0] & 1) == 1) {
            bArr[0] = (byte) (bArr[0] | 4);
            bArr[1] = (byte) smsTracker.mMessageRef;
        }
        int iIncrementAndGet = this.mNextToken.incrementAndGet();
        this.mTrackers.put(Integer.valueOf(iIncrementAndGet), smsTracker);
        try {
            getImsManager().sendSms(iIncrementAndGet, smsTracker.mMessageRef, getFormat(), bArr2 != null ? new String(bArr2) : null, z, bArr);
        } catch (ImsException e) {
            Rlog.e(TAG, "sendSms failed. Falling back to PSTN. Error: " + e.getMessage());
            fallbackToPstn(iIncrementAndGet, smsTracker);
        }
    }

    private ImsManager getImsManager() {
        return ImsManager.getInstance(this.mContext, this.mPhone.getPhoneId());
    }

    @VisibleForTesting
    public void fallbackToPstn(int i, SMSDispatcher.SmsTracker smsTracker) {
        this.mSmsDispatchersController.sendRetrySms(smsTracker);
        this.mTrackers.remove(Integer.valueOf(i));
    }

    @Override
    protected boolean isCdmaMo() {
        return this.mSmsDispatchersController.isCdmaFormat(getFormat());
    }

    protected void handleImsSmsSendResult(SMSDispatcher.SmsTracker smsTracker, Map<Integer, SMSDispatcher.SmsTracker> map, int i, int i2) {
        Rlog.d(TAG, "handleImsSmsSendResult do nothing");
    }
}
