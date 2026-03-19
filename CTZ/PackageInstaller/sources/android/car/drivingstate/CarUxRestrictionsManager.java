package android.car.drivingstate;

import android.car.CarNotConnectedException;

public final class CarUxRestrictionsManager {

    public interface OnUxRestrictionsChangedListener {
    }

    CarUxRestrictionsManager() {
        throw new RuntimeException("Stub!");
    }

    public synchronized void registerListener(OnUxRestrictionsChangedListener listener) throws CarNotConnectedException, IllegalArgumentException {
        throw new RuntimeException("Stub!");
    }

    public synchronized void unregisterListener() throws CarNotConnectedException {
        throw new RuntimeException("Stub!");
    }

    public CarUxRestrictions getCurrentCarUxRestrictions() throws CarNotConnectedException {
        throw new RuntimeException("Stub!");
    }
}
