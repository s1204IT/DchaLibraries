package com.android.location.provider;

import android.hardware.location.IFusedLocationHardware;
import android.hardware.location.IFusedLocationHardwareSink;
import android.location.Location;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.os.RemoteException;
import android.util.Log;
import java.util.HashMap;
import java.util.Map;

public final class FusedLocationHardware {
    private IFusedLocationHardware mLocationHardware;
    private final String TAG = "FusedLocationHardware";
    HashMap<FusedLocationHardwareSink, DispatcherHandler> mSinkList = new HashMap<>();
    private IFusedLocationHardwareSink mInternalSink = new IFusedLocationHardwareSink.Stub() {
        public void onLocationAvailable(Location[] locations) {
            FusedLocationHardware.this.dispatchLocations(locations);
        }

        public void onDiagnosticDataAvailable(String data) {
            FusedLocationHardware.this.dispatchDiagnosticData(data);
        }
    };

    public FusedLocationHardware(IFusedLocationHardware locationHardware) {
        this.mLocationHardware = locationHardware;
    }

    public void registerSink(FusedLocationHardwareSink sink, Looper looper) {
        if (sink == null || looper == null) {
            throw new IllegalArgumentException("Parameter sink and looper cannot be null.");
        }
        synchronized (this.mSinkList) {
            boolean registerSink = this.mSinkList.size() == 0;
            if (!this.mSinkList.containsKey(sink)) {
                HashMap<FusedLocationHardwareSink, DispatcherHandler> newSinkList = new HashMap<>(this.mSinkList);
                newSinkList.put(sink, new DispatcherHandler(looper));
                this.mSinkList = newSinkList;
                if (registerSink) {
                    try {
                        this.mLocationHardware.registerSink(this.mInternalSink);
                    } catch (RemoteException e) {
                        Log.e("FusedLocationHardware", "RemoteException at registerSink");
                    }
                }
            }
        }
    }

    public void unregisterSink(FusedLocationHardwareSink sink) {
        if (sink == null) {
            throw new IllegalArgumentException("Parameter sink cannot be null.");
        }
        synchronized (this.mSinkList) {
            if (this.mSinkList.containsKey(sink)) {
                HashMap<FusedLocationHardwareSink, DispatcherHandler> newSinkList = new HashMap<>(this.mSinkList);
                newSinkList.remove(sink);
                boolean unregisterSink = newSinkList.size() == 0;
                this.mSinkList = newSinkList;
                if (unregisterSink) {
                    try {
                        this.mLocationHardware.unregisterSink(this.mInternalSink);
                    } catch (RemoteException e) {
                        Log.e("FusedLocationHardware", "RemoteException at unregisterSink");
                    }
                }
            }
        }
    }

    public int getSupportedBatchSize() {
        try {
            return this.mLocationHardware.getSupportedBatchSize();
        } catch (RemoteException e) {
            Log.e("FusedLocationHardware", "RemoteException at getSupportedBatchSize");
            return 0;
        }
    }

    public void startBatching(int id, GmsFusedBatchOptions batchOptions) {
        try {
            this.mLocationHardware.startBatching(id, batchOptions.getParcelableOptions());
        } catch (RemoteException e) {
            Log.e("FusedLocationHardware", "RemoteException at startBatching");
        }
    }

    public void stopBatching(int id) {
        try {
            this.mLocationHardware.stopBatching(id);
        } catch (RemoteException e) {
            Log.e("FusedLocationHardware", "RemoteException at stopBatching");
        }
    }

    public void updateBatchingOptions(int id, GmsFusedBatchOptions batchOptions) {
        try {
            this.mLocationHardware.updateBatchingOptions(id, batchOptions.getParcelableOptions());
        } catch (RemoteException e) {
            Log.e("FusedLocationHardware", "RemoteException at updateBatchingOptions");
        }
    }

    public void requestBatchOfLocations(int batchSizeRequest) {
        try {
            this.mLocationHardware.requestBatchOfLocations(batchSizeRequest);
        } catch (RemoteException e) {
            Log.e("FusedLocationHardware", "RemoteException at requestBatchOfLocations");
        }
    }

    public boolean supportsDiagnosticDataInjection() {
        try {
            return this.mLocationHardware.supportsDiagnosticDataInjection();
        } catch (RemoteException e) {
            Log.e("FusedLocationHardware", "RemoteException at supportsDiagnisticDataInjection");
            return false;
        }
    }

    public void injectDiagnosticData(String data) {
        try {
            this.mLocationHardware.injectDiagnosticData(data);
        } catch (RemoteException e) {
            Log.e("FusedLocationHardware", "RemoteException at injectDiagnosticData");
        }
    }

    public boolean supportsDeviceContextInjection() {
        try {
            return this.mLocationHardware.supportsDeviceContextInjection();
        } catch (RemoteException e) {
            Log.e("FusedLocationHardware", "RemoteException at supportsDeviceContextInjection");
            return false;
        }
    }

    public void injectDeviceContext(int deviceEnabledContext) {
        try {
            this.mLocationHardware.injectDeviceContext(deviceEnabledContext);
        } catch (RemoteException e) {
            Log.e("FusedLocationHardware", "RemoteException at injectDeviceContext");
        }
    }

    private class DispatcherHandler extends Handler {
        public static final int DISPATCH_DIAGNOSTIC_DATA = 2;
        public static final int DISPATCH_LOCATION = 1;

        public DispatcherHandler(Looper looper) {
            super(looper, null, true);
        }

        @Override
        public void handleMessage(Message message) {
            MessageCommand command = (MessageCommand) message.obj;
            switch (message.what) {
                case 1:
                    command.dispatchLocation();
                    return;
                case 2:
                    command.dispatchDiagnosticData();
                    break;
            }
            Log.e("FusedLocationHardware", "Invalid dispatch message");
        }
    }

    private class MessageCommand {
        private final String mData;
        private final Location[] mLocations;
        private final FusedLocationHardwareSink mSink;

        public MessageCommand(FusedLocationHardwareSink sink, Location[] locations, String data) {
            this.mSink = sink;
            this.mLocations = locations;
            this.mData = data;
        }

        public void dispatchLocation() {
            this.mSink.onLocationAvailable(this.mLocations);
        }

        public void dispatchDiagnosticData() {
            this.mSink.onDiagnosticDataAvailable(this.mData);
        }
    }

    private void dispatchLocations(Location[] locations) {
        HashMap<FusedLocationHardwareSink, DispatcherHandler> sinks;
        synchronized (this.mSinkList) {
            sinks = this.mSinkList;
        }
        for (Map.Entry<FusedLocationHardwareSink, DispatcherHandler> entry : sinks.entrySet()) {
            Message message = Message.obtain(entry.getValue(), 1, new MessageCommand(entry.getKey(), locations, null));
            message.sendToTarget();
        }
    }

    private void dispatchDiagnosticData(String data) {
        HashMap<FusedLocationHardwareSink, DispatcherHandler> sinks;
        synchronized (this.mSinkList) {
            sinks = this.mSinkList;
        }
        for (Map.Entry<FusedLocationHardwareSink, DispatcherHandler> entry : sinks.entrySet()) {
            Message message = Message.obtain(entry.getValue(), 2, new MessageCommand(entry.getKey(), null, data));
            message.sendToTarget();
        }
    }
}
