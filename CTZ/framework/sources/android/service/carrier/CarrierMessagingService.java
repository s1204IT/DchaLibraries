package android.service.carrier;

import android.app.Service;
import android.content.Intent;
import android.net.Uri;
import android.os.IBinder;
import android.os.RemoteException;
import android.service.carrier.ICarrierMessagingService;
import java.util.List;

public abstract class CarrierMessagingService extends Service {
    public static final int DOWNLOAD_STATUS_ERROR = 2;
    public static final int DOWNLOAD_STATUS_OK = 0;
    public static final int DOWNLOAD_STATUS_RETRY_ON_CARRIER_NETWORK = 1;
    public static final int RECEIVE_OPTIONS_DEFAULT = 0;
    public static final int RECEIVE_OPTIONS_DROP = 1;
    public static final int RECEIVE_OPTIONS_SKIP_NOTIFY_WHEN_CREDENTIAL_PROTECTED_STORAGE_UNAVAILABLE = 2;
    public static final int SEND_FLAG_REQUEST_DELIVERY_STATUS = 1;
    public static final int SEND_STATUS_ERROR = 2;
    public static final int SEND_STATUS_OK = 0;
    public static final int SEND_STATUS_RETRY_ON_CARRIER_NETWORK = 1;
    public static final String SERVICE_INTERFACE = "android.service.carrier.CarrierMessagingService";
    private final ICarrierMessagingWrapper mWrapper = new ICarrierMessagingWrapper();

    public interface ResultCallback<T> {
        void onReceiveResult(T t) throws RemoteException;
    }

    @Deprecated
    public void onFilterSms(MessagePdu messagePdu, String str, int i, int i2, ResultCallback<Boolean> resultCallback) {
        try {
            resultCallback.onReceiveResult(true);
        } catch (RemoteException e) {
        }
    }

    public void onReceiveTextSms(MessagePdu messagePdu, String str, int i, int i2, final ResultCallback<Integer> resultCallback) {
        onFilterSms(messagePdu, str, i, i2, new ResultCallback<Boolean>() {
            @Override
            public void onReceiveResult(Boolean bool) throws RemoteException {
                resultCallback.onReceiveResult(Integer.valueOf(bool.booleanValue() ? 0 : 3));
            }
        });
    }

    @Deprecated
    public void onSendTextSms(String str, int i, String str2, ResultCallback<SendSmsResult> resultCallback) {
        try {
            resultCallback.onReceiveResult(new SendSmsResult(1, 0));
        } catch (RemoteException e) {
        }
    }

    public void onSendTextSms(String str, int i, String str2, int i2, ResultCallback<SendSmsResult> resultCallback) {
        onSendTextSms(str, i, str2, resultCallback);
    }

    @Deprecated
    public void onSendDataSms(byte[] bArr, int i, String str, int i2, ResultCallback<SendSmsResult> resultCallback) {
        try {
            resultCallback.onReceiveResult(new SendSmsResult(1, 0));
        } catch (RemoteException e) {
        }
    }

    public void onSendDataSms(byte[] bArr, int i, String str, int i2, int i3, ResultCallback<SendSmsResult> resultCallback) {
        onSendDataSms(bArr, i, str, i2, resultCallback);
    }

    @Deprecated
    public void onSendMultipartTextSms(List<String> list, int i, String str, ResultCallback<SendMultipartSmsResult> resultCallback) {
        try {
            resultCallback.onReceiveResult(new SendMultipartSmsResult(1, null));
        } catch (RemoteException e) {
        }
    }

    public void onSendMultipartTextSms(List<String> list, int i, String str, int i2, ResultCallback<SendMultipartSmsResult> resultCallback) {
        onSendMultipartTextSms(list, i, str, resultCallback);
    }

    public void onSendMms(Uri uri, int i, Uri uri2, ResultCallback<SendMmsResult> resultCallback) {
        try {
            resultCallback.onReceiveResult(new SendMmsResult(1, null));
        } catch (RemoteException e) {
        }
    }

    public void onDownloadMms(Uri uri, int i, Uri uri2, ResultCallback<Integer> resultCallback) {
        try {
            resultCallback.onReceiveResult(1);
        } catch (RemoteException e) {
        }
    }

    @Override
    public IBinder onBind(Intent intent) {
        if (!SERVICE_INTERFACE.equals(intent.getAction())) {
            return null;
        }
        return this.mWrapper;
    }

    public static final class SendMmsResult {
        private byte[] mSendConfPdu;
        private int mSendStatus;

        public SendMmsResult(int i, byte[] bArr) {
            this.mSendStatus = i;
            this.mSendConfPdu = bArr;
        }

        public int getSendStatus() {
            return this.mSendStatus;
        }

        public byte[] getSendConfPdu() {
            return this.mSendConfPdu;
        }
    }

    public static final class SendSmsResult {
        private final int mMessageRef;
        private final int mSendStatus;

        public SendSmsResult(int i, int i2) {
            this.mSendStatus = i;
            this.mMessageRef = i2;
        }

        public int getMessageRef() {
            return this.mMessageRef;
        }

        public int getSendStatus() {
            return this.mSendStatus;
        }
    }

    public static final class SendMultipartSmsResult {
        private final int[] mMessageRefs;
        private final int mSendStatus;

        public SendMultipartSmsResult(int i, int[] iArr) {
            this.mSendStatus = i;
            this.mMessageRefs = iArr;
        }

        public int[] getMessageRefs() {
            return this.mMessageRefs;
        }

        public int getSendStatus() {
            return this.mSendStatus;
        }
    }

    private class ICarrierMessagingWrapper extends ICarrierMessagingService.Stub {
        private ICarrierMessagingWrapper() {
        }

        @Override
        public void filterSms(MessagePdu messagePdu, String str, int i, int i2, final ICarrierMessagingCallback iCarrierMessagingCallback) {
            CarrierMessagingService.this.onReceiveTextSms(messagePdu, str, i, i2, new ResultCallback<Integer>() {
                @Override
                public void onReceiveResult(Integer num) throws RemoteException {
                    iCarrierMessagingCallback.onFilterComplete(num.intValue());
                }
            });
        }

        @Override
        public void sendTextSms(String str, int i, String str2, int i2, final ICarrierMessagingCallback iCarrierMessagingCallback) {
            CarrierMessagingService.this.onSendTextSms(str, i, str2, i2, new ResultCallback<SendSmsResult>() {
                @Override
                public void onReceiveResult(SendSmsResult sendSmsResult) throws RemoteException {
                    iCarrierMessagingCallback.onSendSmsComplete(sendSmsResult.getSendStatus(), sendSmsResult.getMessageRef());
                }
            });
        }

        @Override
        public void sendDataSms(byte[] bArr, int i, String str, int i2, int i3, final ICarrierMessagingCallback iCarrierMessagingCallback) {
            CarrierMessagingService.this.onSendDataSms(bArr, i, str, i2, i3, new ResultCallback<SendSmsResult>() {
                @Override
                public void onReceiveResult(SendSmsResult sendSmsResult) throws RemoteException {
                    iCarrierMessagingCallback.onSendSmsComplete(sendSmsResult.getSendStatus(), sendSmsResult.getMessageRef());
                }
            });
        }

        @Override
        public void sendMultipartTextSms(List<String> list, int i, String str, int i2, final ICarrierMessagingCallback iCarrierMessagingCallback) {
            CarrierMessagingService.this.onSendMultipartTextSms(list, i, str, i2, new ResultCallback<SendMultipartSmsResult>() {
                @Override
                public void onReceiveResult(SendMultipartSmsResult sendMultipartSmsResult) throws RemoteException {
                    iCarrierMessagingCallback.onSendMultipartSmsComplete(sendMultipartSmsResult.getSendStatus(), sendMultipartSmsResult.getMessageRefs());
                }
            });
        }

        @Override
        public void sendMms(Uri uri, int i, Uri uri2, final ICarrierMessagingCallback iCarrierMessagingCallback) {
            CarrierMessagingService.this.onSendMms(uri, i, uri2, new ResultCallback<SendMmsResult>() {
                @Override
                public void onReceiveResult(SendMmsResult sendMmsResult) throws RemoteException {
                    iCarrierMessagingCallback.onSendMmsComplete(sendMmsResult.getSendStatus(), sendMmsResult.getSendConfPdu());
                }
            });
        }

        @Override
        public void downloadMms(Uri uri, int i, Uri uri2, final ICarrierMessagingCallback iCarrierMessagingCallback) {
            CarrierMessagingService.this.onDownloadMms(uri, i, uri2, new ResultCallback<Integer>() {
                @Override
                public void onReceiveResult(Integer num) throws RemoteException {
                    iCarrierMessagingCallback.onDownloadMmsComplete(num.intValue());
                }
            });
        }
    }
}
