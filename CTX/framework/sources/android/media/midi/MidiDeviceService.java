package android.media.midi;

import android.app.Service;
import android.content.Intent;
import android.media.midi.IMidiManager;
import android.media.midi.MidiDeviceServer;
import android.os.IBinder;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.util.Log;

public abstract class MidiDeviceService extends Service {
    public static final String SERVICE_INTERFACE = "android.media.midi.MidiDeviceService";
    private static final String TAG = "MidiDeviceService";
    private final MidiDeviceServer.Callback mCallback = new MidiDeviceServer.Callback() {
        @Override
        public void onDeviceStatusChanged(MidiDeviceServer midiDeviceServer, MidiDeviceStatus midiDeviceStatus) {
            MidiDeviceService.this.onDeviceStatusChanged(midiDeviceStatus);
        }

        @Override
        public void onClose() {
            MidiDeviceService.this.onClose();
        }
    };
    private MidiDeviceInfo mDeviceInfo;
    private IMidiManager mMidiManager;
    private MidiDeviceServer mServer;

    public abstract MidiReceiver[] onGetInputPortReceivers();

    @Override
    public void onCreate() {
        MidiDeviceServer midiDeviceServer;
        MidiDeviceInfo serviceDeviceInfo;
        this.mMidiManager = IMidiManager.Stub.asInterface(ServiceManager.getService("midi"));
        try {
            serviceDeviceInfo = this.mMidiManager.getServiceDeviceInfo(getPackageName(), getClass().getName());
        } catch (RemoteException e) {
            Log.e(TAG, "RemoteException in IMidiManager.getServiceDeviceInfo");
            midiDeviceServer = null;
        }
        if (serviceDeviceInfo == null) {
            Log.e(TAG, "Could not find MidiDeviceInfo for MidiDeviceService " + this);
            return;
        }
        this.mDeviceInfo = serviceDeviceInfo;
        MidiReceiver[] midiReceiverArrOnGetInputPortReceivers = onGetInputPortReceivers();
        if (midiReceiverArrOnGetInputPortReceivers == null) {
            midiReceiverArrOnGetInputPortReceivers = new MidiReceiver[0];
        }
        midiDeviceServer = new MidiDeviceServer(this.mMidiManager, midiReceiverArrOnGetInputPortReceivers, serviceDeviceInfo, this.mCallback);
        this.mServer = midiDeviceServer;
    }

    public final MidiReceiver[] getOutputPortReceivers() {
        if (this.mServer == null) {
            return null;
        }
        return this.mServer.getOutputPortReceivers();
    }

    public final MidiDeviceInfo getDeviceInfo() {
        return this.mDeviceInfo;
    }

    public void onDeviceStatusChanged(MidiDeviceStatus midiDeviceStatus) {
    }

    public void onClose() {
    }

    @Override
    public IBinder onBind(Intent intent) {
        if (SERVICE_INTERFACE.equals(intent.getAction()) && this.mServer != null) {
            return this.mServer.getBinderInterface().asBinder();
        }
        return null;
    }
}
