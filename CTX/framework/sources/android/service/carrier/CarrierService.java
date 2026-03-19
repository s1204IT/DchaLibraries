package android.service.carrier;

import android.app.Service;
import android.content.Intent;
import android.os.Bundle;
import android.os.IBinder;
import android.os.PersistableBundle;
import android.os.RemoteException;
import android.os.ResultReceiver;
import android.os.ServiceManager;
import android.service.carrier.ICarrierService;
import android.util.Log;
import com.android.internal.telephony.ITelephonyRegistry;

public abstract class CarrierService extends Service {
    public static final String CARRIER_SERVICE_INTERFACE = "android.service.carrier.CarrierService";
    private static final String LOG_TAG = "CarrierService";
    private static ITelephonyRegistry sRegistry;
    private final ICarrierService.Stub mStubWrapper = new ICarrierServiceWrapper();

    public abstract PersistableBundle onLoadConfig(CarrierIdentifier carrierIdentifier);

    public CarrierService() {
        if (sRegistry == null) {
            sRegistry = ITelephonyRegistry.Stub.asInterface(ServiceManager.getService("telephony.registry"));
        }
    }

    public final void notifyCarrierNetworkChange(boolean z) {
        try {
            if (sRegistry != null) {
                sRegistry.notifyCarrierNetworkChange(z);
            }
        } catch (RemoteException | NullPointerException e) {
        }
    }

    @Override
    public IBinder onBind(Intent intent) {
        return this.mStubWrapper;
    }

    public class ICarrierServiceWrapper extends ICarrierService.Stub {
        public static final String KEY_CONFIG_BUNDLE = "config_bundle";
        public static final int RESULT_ERROR = 1;
        public static final int RESULT_OK = 0;

        public ICarrierServiceWrapper() {
        }

        @Override
        public void getCarrierConfig(CarrierIdentifier carrierIdentifier, ResultReceiver resultReceiver) {
            try {
                Bundle bundle = new Bundle();
                bundle.putParcelable(KEY_CONFIG_BUNDLE, CarrierService.this.onLoadConfig(carrierIdentifier));
                resultReceiver.send(0, bundle);
            } catch (Exception e) {
                Log.e(CarrierService.LOG_TAG, "Error in onLoadConfig: " + e.getMessage(), e);
                resultReceiver.send(1, null);
            }
        }
    }
}
