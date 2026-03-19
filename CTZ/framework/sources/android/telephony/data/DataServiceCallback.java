package android.telephony.data;

import android.os.RemoteException;
import android.telephony.Rlog;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.ref.WeakReference;
import java.util.List;

public class DataServiceCallback {
    public static final int RESULT_ERROR_BUSY = 3;
    public static final int RESULT_ERROR_ILLEGAL_STATE = 4;
    public static final int RESULT_ERROR_INVALID_ARG = 2;
    public static final int RESULT_ERROR_UNSUPPORTED = 1;
    public static final int RESULT_SUCCESS = 0;
    private static final String TAG = DataServiceCallback.class.getSimpleName();
    private final WeakReference<IDataServiceCallback> mCallback;

    @Retention(RetentionPolicy.SOURCE)
    public @interface ResultCode {
    }

    public DataServiceCallback(IDataServiceCallback iDataServiceCallback) {
        this.mCallback = new WeakReference<>(iDataServiceCallback);
    }

    public void onSetupDataCallComplete(int i, DataCallResponse dataCallResponse) {
        IDataServiceCallback iDataServiceCallback = this.mCallback.get();
        if (iDataServiceCallback != null) {
            try {
                iDataServiceCallback.onSetupDataCallComplete(i, dataCallResponse);
            } catch (RemoteException e) {
                Rlog.e(TAG, "Failed to onSetupDataCallComplete on the remote");
            }
        }
    }

    public void onDeactivateDataCallComplete(int i) {
        IDataServiceCallback iDataServiceCallback = this.mCallback.get();
        if (iDataServiceCallback != null) {
            try {
                iDataServiceCallback.onDeactivateDataCallComplete(i);
            } catch (RemoteException e) {
                Rlog.e(TAG, "Failed to onDeactivateDataCallComplete on the remote");
            }
        }
    }

    public void onSetInitialAttachApnComplete(int i) {
        IDataServiceCallback iDataServiceCallback = this.mCallback.get();
        if (iDataServiceCallback != null) {
            try {
                iDataServiceCallback.onSetInitialAttachApnComplete(i);
            } catch (RemoteException e) {
                Rlog.e(TAG, "Failed to onSetInitialAttachApnComplete on the remote");
            }
        }
    }

    public void onSetDataProfileComplete(int i) {
        IDataServiceCallback iDataServiceCallback = this.mCallback.get();
        if (iDataServiceCallback != null) {
            try {
                iDataServiceCallback.onSetDataProfileComplete(i);
            } catch (RemoteException e) {
                Rlog.e(TAG, "Failed to onSetDataProfileComplete on the remote");
            }
        }
    }

    public void onGetDataCallListComplete(int i, List<DataCallResponse> list) {
        IDataServiceCallback iDataServiceCallback = this.mCallback.get();
        if (iDataServiceCallback != null) {
            try {
                iDataServiceCallback.onGetDataCallListComplete(i, list);
            } catch (RemoteException e) {
                Rlog.e(TAG, "Failed to onGetDataCallListComplete on the remote");
            }
        }
    }

    public void onDataCallListChanged(List<DataCallResponse> list) {
        IDataServiceCallback iDataServiceCallback = this.mCallback.get();
        if (iDataServiceCallback != null) {
            try {
                iDataServiceCallback.onDataCallListChanged(list);
            } catch (RemoteException e) {
                Rlog.e(TAG, "Failed to onDataCallListChanged on the remote");
            }
        }
    }
}
