package com.android.server;

import android.content.Context;
import android.location.Country;
import android.location.CountryListener;
import android.location.ICountryDetector;
import android.location.ICountryListener;
import android.os.Handler;
import android.os.IBinder;
import android.os.RemoteException;
import android.util.Slog;
import com.android.internal.os.BackgroundThread;
import com.android.server.location.ComprehensiveCountryDetector;
import java.io.FileDescriptor;
import java.io.PrintWriter;
import java.util.HashMap;

public class CountryDetectorService extends ICountryDetector.Stub implements Runnable {
    private static final boolean DEBUG = false;
    private static final String TAG = "CountryDetector";
    private final Context mContext;
    private ComprehensiveCountryDetector mCountryDetector;
    private Handler mHandler;
    private CountryListener mLocationBasedDetectorListener;
    private final HashMap<IBinder, Receiver> mReceivers = new HashMap<>();
    private boolean mSystemReady;

    private final class Receiver implements IBinder.DeathRecipient {
        private final IBinder mKey;
        private final ICountryListener mListener;

        public Receiver(ICountryListener listener) {
            this.mListener = listener;
            this.mKey = listener.asBinder();
        }

        @Override
        public void binderDied() {
            CountryDetectorService.this.removeListener(this.mKey);
        }

        public boolean equals(Object otherObj) {
            return otherObj instanceof Receiver ? this.mKey.equals(((Receiver) otherObj).mKey) : CountryDetectorService.DEBUG;
        }

        public int hashCode() {
            return this.mKey.hashCode();
        }

        public ICountryListener getListener() {
            return this.mListener;
        }
    }

    public CountryDetectorService(Context context) {
        this.mContext = context;
    }

    public Country detectCountry() {
        if (this.mSystemReady) {
            return this.mCountryDetector.detectCountry();
        }
        return null;
    }

    public void addCountryListener(ICountryListener listener) throws RemoteException {
        if (!this.mSystemReady) {
            throw new RemoteException();
        }
        addListener(listener);
    }

    public void removeCountryListener(ICountryListener listener) throws RemoteException {
        if (!this.mSystemReady) {
            throw new RemoteException();
        }
        removeListener(listener.asBinder());
    }

    private void addListener(ICountryListener listener) {
        synchronized (this.mReceivers) {
            Receiver r = new Receiver(listener);
            try {
                listener.asBinder().linkToDeath(r, 0);
                this.mReceivers.put(listener.asBinder(), r);
            } catch (RemoteException e) {
                Slog.e(TAG, "linkToDeath failed:", e);
            }
            if (this.mReceivers.size() == 1) {
                Slog.d(TAG, "The first listener is added");
                setCountryListener(this.mLocationBasedDetectorListener);
            }
        }
    }

    private void removeListener(IBinder key) {
        synchronized (this.mReceivers) {
            this.mReceivers.remove(key);
            if (this.mReceivers.isEmpty()) {
                setCountryListener(null);
                Slog.d(TAG, "No listener is left");
            }
        }
    }

    protected void notifyReceivers(Country country) {
        synchronized (this.mReceivers) {
            for (Receiver receiver : this.mReceivers.values()) {
                try {
                    receiver.getListener().onCountryDetected(country);
                } catch (RemoteException e) {
                    Slog.e(TAG, "notifyReceivers failed:", e);
                }
            }
        }
    }

    void systemRunning() {
        BackgroundThread.getHandler().post(this);
    }

    private void initialize() {
        this.mCountryDetector = new ComprehensiveCountryDetector(this.mContext);
        this.mLocationBasedDetectorListener = new CountryListener() {
            public void onCountryDetected(final Country country) {
                CountryDetectorService.this.mHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        CountryDetectorService.this.notifyReceivers(country);
                    }
                });
            }
        };
    }

    @Override
    public void run() {
        this.mHandler = new Handler();
        initialize();
        this.mSystemReady = true;
    }

    protected void setCountryListener(final CountryListener listener) {
        this.mHandler.post(new Runnable() {
            @Override
            public void run() {
                CountryDetectorService.this.mCountryDetector.setCountryListener(listener);
            }
        });
    }

    boolean isSystemReady() {
        return this.mSystemReady;
    }

    protected void dump(FileDescriptor fd, PrintWriter fout, String[] args) {
        this.mContext.enforceCallingOrSelfPermission("android.permission.DUMP", TAG);
    }
}
