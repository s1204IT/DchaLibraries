package android.telephony.ims.stub;

import android.annotation.SystemApi;
import android.os.RemoteException;
import android.telephony.SmsMessage;
import android.telephony.ims.aidl.IImsSmsListener;
import android.util.Log;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

@SystemApi
public class ImsSmsImplBase {
    public static final int DELIVER_STATUS_ERROR_GENERIC = 2;
    public static final int DELIVER_STATUS_ERROR_NO_MEMORY = 3;
    public static final int DELIVER_STATUS_ERROR_REQUEST_NOT_SUPPORTED = 4;
    public static final int DELIVER_STATUS_OK = 1;
    private static final String LOG_TAG = "SmsImplBase";
    public static final int SEND_STATUS_ERROR = 2;
    public static final int SEND_STATUS_ERROR_FALLBACK = 4;
    public static final int SEND_STATUS_ERROR_RETRY = 3;
    public static final int SEND_STATUS_OK = 1;
    public static final int STATUS_REPORT_STATUS_ERROR = 2;
    public static final int STATUS_REPORT_STATUS_OK = 1;
    private IImsSmsListener mListener;
    private final Object mLock = new Object();

    @Retention(RetentionPolicy.SOURCE)
    public @interface DeliverStatusResult {
    }

    @Retention(RetentionPolicy.SOURCE)
    public @interface SendStatusResult {
    }

    @Retention(RetentionPolicy.SOURCE)
    public @interface StatusReportResult {
    }

    public void registerSmsListener(IImsSmsListener iImsSmsListener) {
        synchronized (this.mLock) {
            this.mListener = iImsSmsListener;
        }
    }

    public void sendSms(int i, int i2, String str, String str2, boolean z, byte[] bArr) {
        try {
            onSendSmsResult(i, i2, 2, 1);
        } catch (RuntimeException e) {
            Log.e(LOG_TAG, "Can not send sms: " + e.getMessage());
        }
    }

    public void acknowledgeSms(int i, int i2, int i3) {
        Log.e(LOG_TAG, "acknowledgeSms() not implemented.");
    }

    public void acknowledgeSmsReport(int i, int i2, int i3) {
        Log.e(LOG_TAG, "acknowledgeSmsReport() not implemented.");
    }

    public final void onSmsReceived(int i, String str, byte[] bArr) throws RuntimeException {
        synchronized (this.mLock) {
            if (this.mListener == null) {
                throw new RuntimeException("Feature not ready.");
            }
            try {
                this.mListener.onSmsReceived(i, str, bArr);
            } catch (RemoteException e) {
                Log.e(LOG_TAG, "Can not deliver sms: " + e.getMessage());
                SmsMessage smsMessageCreateFromPdu = SmsMessage.createFromPdu(bArr, str);
                if (smsMessageCreateFromPdu != null && smsMessageCreateFromPdu.mWrappedSmsMessage != null) {
                    acknowledgeSms(i, smsMessageCreateFromPdu.mWrappedSmsMessage.mMessageRef, 2);
                } else {
                    Log.w(LOG_TAG, "onSmsReceived: Invalid pdu entered.");
                    acknowledgeSms(i, 0, 2);
                }
            }
        }
    }

    public final void onSendSmsResult(int i, int i2, int i3, int i4) throws RuntimeException {
        synchronized (this.mLock) {
            if (this.mListener == null) {
                throw new RuntimeException("Feature not ready.");
            }
            try {
                this.mListener.onSendSmsResult(i, i2, i3, i4);
            } catch (RemoteException e) {
                e.rethrowFromSystemServer();
            }
        }
    }

    public final void onSmsStatusReportReceived(int i, int i2, String str, byte[] bArr) throws RuntimeException {
        synchronized (this.mLock) {
            if (this.mListener == null) {
                throw new RuntimeException("Feature not ready.");
            }
            try {
                this.mListener.onSmsStatusReportReceived(i, i2, str, bArr);
            } catch (RemoteException e) {
                Log.e(LOG_TAG, "Can not process sms status report: " + e.getMessage());
                acknowledgeSmsReport(i, i2, 2);
            }
        }
    }

    public String getSmsFormat() {
        return "3gpp";
    }

    public void onReady() {
    }
}
