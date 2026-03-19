package android.service.carrier;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import android.os.PersistableBundle;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.service.carrier.ICarrierService;
import com.android.internal.telephony.ITelephonyRegistry;

public abstract class CarrierService extends Service {
    public static final String CARRIER_SERVICE_INTERFACE = "android.service.carrier.CarrierService";
    private static ITelephonyRegistry sRegistry;
    private final ICarrierService.Stub mStubWrapper = new ICarrierServiceWrapper(this, null);

    public abstract PersistableBundle onLoadConfig(CarrierIdentifier carrierIdentifier);

    public CarrierService() {
        if (sRegistry != null) {
            return;
        }
        sRegistry = ITelephonyRegistry.Stub.asInterface(ServiceManager.getService("telephony.registry"));
    }

    public final void notifyCarrierNetworkChange(boolean active) {
        try {
            if (sRegistry != null) {
                sRegistry.notifyCarrierNetworkChange(active);
            }
        } catch (RemoteException | NullPointerException e) {
        }
    }

    @Override
    public IBinder onBind(Intent intent) {
        return this.mStubWrapper;
    }

    private class ICarrierServiceWrapper extends ICarrierService.Stub {
        ICarrierServiceWrapper(CarrierService this$0, ICarrierServiceWrapper iCarrierServiceWrapper) {
            this();
        }

        private ICarrierServiceWrapper() {
        }

        @Override
        public PersistableBundle getCarrierConfig(CarrierIdentifier id) {
            return CarrierService.this.onLoadConfig(id);
        }
    }
}
