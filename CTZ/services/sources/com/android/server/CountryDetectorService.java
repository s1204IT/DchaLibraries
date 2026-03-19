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
import com.android.internal.util.DumpUtils;
import com.android.server.location.ComprehensiveCountryDetector;
import java.io.FileDescriptor;
import java.io.PrintWriter;
import java.util.HashMap;
import java.util.Iterator;

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

        public Receiver(ICountryListener iCountryListener) {
            this.mListener = iCountryListener;
            this.mKey = iCountryListener.asBinder();
        }

        @Override
        public void binderDied() {
            CountryDetectorService.this.removeListener(this.mKey);
        }

        public boolean equals(Object obj) {
            if (obj instanceof Receiver) {
                return this.mKey.equals(((Receiver) obj).mKey);
            }
            return false;
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
        if (!this.mSystemReady) {
            return null;
        }
        return this.mCountryDetector.detectCountry();
    }

    public void addCountryListener(ICountryListener iCountryListener) throws RemoteException {
        if (!this.mSystemReady) {
            throw new RemoteException();
        }
        addListener(iCountryListener);
    }

    public void removeCountryListener(ICountryListener iCountryListener) throws RemoteException {
        if (!this.mSystemReady) {
            throw new RemoteException();
        }
        removeListener(iCountryListener.asBinder());
    }

    private void addListener(ICountryListener iCountryListener) {
        synchronized (this.mReceivers) {
            Receiver receiver = new Receiver(iCountryListener);
            try {
                iCountryListener.asBinder().linkToDeath(receiver, 0);
                this.mReceivers.put(iCountryListener.asBinder(), receiver);
            } catch (RemoteException e) {
                Slog.e(TAG, "linkToDeath failed:", e);
            }
            if (this.mReceivers.size() == 1) {
                Slog.d(TAG, "The first listener is added");
                setCountryListener(this.mLocationBasedDetectorListener);
            }
        }
    }

    private void removeListener(IBinder iBinder) {
        synchronized (this.mReceivers) {
            this.mReceivers.remove(iBinder);
            if (this.mReceivers.isEmpty()) {
                setCountryListener(null);
                Slog.d(TAG, "No listener is left");
            }
        }
    }

    protected void notifyReceivers(Country country) {
        synchronized (this.mReceivers) {
            Iterator<Receiver> it = this.mReceivers.values().iterator();
            while (it.hasNext()) {
                try {
                    it.next().getListener().onCountryDetected(country);
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

    protected void setCountryListener(final CountryListener countryListener) {
        this.mHandler.post(new Runnable() {
            @Override
            public void run() {
                CountryDetectorService.this.mCountryDetector.setCountryListener(countryListener);
            }
        });
    }

    boolean isSystemReady() {
        return this.mSystemReady;
    }

    protected void dump(FileDescriptor fileDescriptor, PrintWriter printWriter, String[] strArr) {
        if (DumpUtils.checkDumpPermission(this.mContext, TAG, printWriter)) {
        }
    }
}
