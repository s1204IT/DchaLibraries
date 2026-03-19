package android.car;

import android.content.Context;
import android.content.ServiceConnection;

public final class Car {
    public static Car createCar(Context context, ServiceConnection serviceConnectionListener) {
        throw new RuntimeException("Stub!");
    }

    public void connect() throws IllegalStateException {
        throw new RuntimeException("Stub!");
    }

    public void disconnect() {
        throw new RuntimeException("Stub!");
    }

    public boolean isConnected() {
        throw new RuntimeException("Stub!");
    }

    public Object getCarManager(String serviceName) throws CarNotConnectedException {
        throw new RuntimeException("Stub!");
    }
}
