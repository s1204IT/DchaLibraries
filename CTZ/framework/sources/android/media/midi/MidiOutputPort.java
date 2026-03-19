package android.media.midi;

import android.os.IBinder;
import android.os.ParcelFileDescriptor;
import android.os.RemoteException;
import android.util.Log;
import com.android.internal.midi.MidiDispatcher;
import dalvik.system.CloseGuard;
import java.io.Closeable;
import java.io.FileDescriptor;
import java.io.FileInputStream;
import java.io.IOException;
import libcore.io.IoUtils;

public final class MidiOutputPort extends MidiSender implements Closeable {
    private static final String TAG = "MidiOutputPort";
    private IMidiDeviceServer mDeviceServer;
    private final MidiDispatcher mDispatcher;
    private final CloseGuard mGuard;
    private final FileInputStream mInputStream;
    private boolean mIsClosed;
    private final int mPortNumber;
    private final Thread mThread;
    private final IBinder mToken;

    MidiOutputPort(IMidiDeviceServer iMidiDeviceServer, IBinder iBinder, FileDescriptor fileDescriptor, int i) {
        this.mDispatcher = new MidiDispatcher();
        this.mGuard = CloseGuard.get();
        this.mThread = new Thread() {
            @Override
            public void run() {
                int i2;
                byte[] bArr = new byte[1024];
                while (true) {
                    try {
                        try {
                            i2 = MidiOutputPort.this.mInputStream.read(bArr);
                        } catch (IOException e) {
                            Log.e(MidiOutputPort.TAG, "read failed", e);
                        }
                        if (i2 >= 0) {
                            int packetType = MidiPortImpl.getPacketType(bArr, i2);
                            switch (packetType) {
                                case 1:
                                    MidiOutputPort.this.mDispatcher.send(bArr, MidiPortImpl.getDataOffset(bArr, i2), MidiPortImpl.getDataSize(bArr, i2), MidiPortImpl.getPacketTimestamp(bArr, i2));
                                    break;
                                case 2:
                                    MidiOutputPort.this.mDispatcher.flush();
                                    break;
                                default:
                                    Log.e(MidiOutputPort.TAG, "Unknown packet type " + packetType);
                                    break;
                            }
                        } else {
                            return;
                        }
                    } finally {
                        IoUtils.closeQuietly(MidiOutputPort.this.mInputStream);
                    }
                }
            }
        };
        this.mDeviceServer = iMidiDeviceServer;
        this.mToken = iBinder;
        this.mPortNumber = i;
        this.mInputStream = new ParcelFileDescriptor.AutoCloseInputStream(new ParcelFileDescriptor(fileDescriptor));
        this.mThread.start();
        this.mGuard.open("close");
    }

    MidiOutputPort(FileDescriptor fileDescriptor, int i) {
        this(null, null, fileDescriptor, i);
    }

    public final int getPortNumber() {
        return this.mPortNumber;
    }

    @Override
    public void onConnect(MidiReceiver midiReceiver) {
        this.mDispatcher.getSender().connect(midiReceiver);
    }

    @Override
    public void onDisconnect(MidiReceiver midiReceiver) {
        this.mDispatcher.getSender().disconnect(midiReceiver);
    }

    @Override
    public void close() throws IOException {
        synchronized (this.mGuard) {
            if (this.mIsClosed) {
                return;
            }
            this.mGuard.close();
            this.mInputStream.close();
            if (this.mDeviceServer != null) {
                try {
                    this.mDeviceServer.closePort(this.mToken);
                } catch (RemoteException e) {
                    Log.e(TAG, "RemoteException in MidiOutputPort.close()");
                }
            }
            this.mIsClosed = true;
        }
    }

    protected void finalize() throws Throwable {
        try {
            if (this.mGuard != null) {
                this.mGuard.warnIfOpen();
            }
            this.mDeviceServer = null;
            close();
        } finally {
            super.finalize();
        }
    }
}
