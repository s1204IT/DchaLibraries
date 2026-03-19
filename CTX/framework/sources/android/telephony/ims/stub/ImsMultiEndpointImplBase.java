package android.telephony.ims.stub;

import android.annotation.SystemApi;
import android.os.RemoteException;
import android.telephony.ims.ImsExternalCallState;
import android.util.Log;
import com.android.ims.internal.IImsExternalCallStateListener;
import com.android.ims.internal.IImsMultiEndpoint;
import java.util.List;

@SystemApi
public class ImsMultiEndpointImplBase {
    private static final String TAG = "MultiEndpointImplBase";
    private IImsMultiEndpoint mImsMultiEndpoint = new IImsMultiEndpoint.Stub() {
        @Override
        public void setListener(IImsExternalCallStateListener iImsExternalCallStateListener) throws RemoteException {
            ImsMultiEndpointImplBase.this.mListener = iImsExternalCallStateListener;
        }

        @Override
        public void requestImsExternalCallStateInfo() throws RemoteException {
            ImsMultiEndpointImplBase.this.requestImsExternalCallStateInfo();
        }
    };
    private IImsExternalCallStateListener mListener;

    public IImsMultiEndpoint getIImsMultiEndpoint() {
        return this.mImsMultiEndpoint;
    }

    public final void onImsExternalCallStateUpdate(List<ImsExternalCallState> list) {
        Log.d(TAG, "ims external call state update triggered.");
        if (this.mListener != null) {
            try {
                this.mListener.onImsExternalCallStateUpdate(list);
            } catch (RemoteException e) {
                throw new RuntimeException(e);
            }
        }
    }

    public void requestImsExternalCallStateInfo() {
        Log.d(TAG, "requestImsExternalCallStateInfo() not implemented");
    }
}
