package com.android.server.usb;

import android.content.Context;
import android.media.midi.MidiDeviceInfo;
import android.media.midi.MidiDeviceServer;
import android.media.midi.MidiDeviceStatus;
import android.media.midi.MidiManager;
import android.media.midi.MidiReceiver;
import android.os.Bundle;
import android.system.ErrnoException;
import android.system.Os;
import android.system.OsConstants;
import android.system.StructPollfd;
import android.util.Log;
import com.android.internal.midi.MidiEventScheduler;
import com.android.internal.util.dump.DualDumpOutputStream;
import java.io.Closeable;
import java.io.FileDescriptor;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import libcore.io.IoUtils;

public final class UsbMidiDevice implements Closeable {
    private static final int BUFFER_SIZE = 512;
    private static final String TAG = "UsbMidiDevice";
    private final int mAlsaCard;
    private final int mAlsaDevice;
    private MidiEventScheduler[] mEventSchedulers;
    private FileDescriptor[] mFileDescriptors;
    private final InputReceiverProxy[] mInputPortReceivers;
    private FileInputStream[] mInputStreams;
    private boolean mIsOpen;
    private FileOutputStream[] mOutputStreams;
    private StructPollfd[] mPollFDs;
    private MidiDeviceServer mServer;
    private final int mSubdeviceCount;
    private final Object mLock = new Object();
    private int mPipeFD = -1;
    private final MidiDeviceServer.Callback mCallback = new MidiDeviceServer.Callback() {
        public void onDeviceStatusChanged(MidiDeviceServer midiDeviceServer, MidiDeviceStatus midiDeviceStatus) {
            boolean z;
            MidiDeviceInfo deviceInfo = midiDeviceStatus.getDeviceInfo();
            int inputPortCount = deviceInfo.getInputPortCount();
            int outputPortCount = deviceInfo.getOutputPortCount();
            int i = 0;
            int i2 = 0;
            while (true) {
                if (i2 < inputPortCount) {
                    if (!midiDeviceStatus.isInputPortOpen(i2)) {
                        i2++;
                    } else {
                        z = true;
                        break;
                    }
                } else {
                    z = false;
                    break;
                }
            }
            if (!z) {
                while (true) {
                    if (i >= outputPortCount) {
                        break;
                    }
                    if (midiDeviceStatus.getOutputPortOpenCount(i) <= 0) {
                        i++;
                    } else {
                        z = true;
                        break;
                    }
                }
            }
            synchronized (UsbMidiDevice.this.mLock) {
                if (z) {
                    try {
                        if (!UsbMidiDevice.this.mIsOpen) {
                            UsbMidiDevice.this.openLocked();
                        } else if (!z && UsbMidiDevice.this.mIsOpen) {
                            UsbMidiDevice.this.closeLocked();
                        }
                    } catch (Throwable th) {
                        throw th;
                    }
                }
            }
        }

        public void onClose() {
        }
    };

    private native void nativeClose(FileDescriptor[] fileDescriptorArr);

    private static native int nativeGetSubdeviceCount(int i, int i2);

    private native FileDescriptor[] nativeOpen(int i, int i2, int i3);

    private final class InputReceiverProxy extends MidiReceiver {
        private MidiReceiver mReceiver;

        private InputReceiverProxy() {
        }

        @Override
        public void onSend(byte[] bArr, int i, int i2, long j) throws IOException {
            MidiReceiver midiReceiver = this.mReceiver;
            if (midiReceiver != null) {
                midiReceiver.send(bArr, i, i2, j);
            }
        }

        public void setReceiver(MidiReceiver midiReceiver) {
            this.mReceiver = midiReceiver;
        }

        @Override
        public void onFlush() throws IOException {
            MidiReceiver midiReceiver = this.mReceiver;
            if (midiReceiver != null) {
                midiReceiver.flush();
            }
        }
    }

    public static UsbMidiDevice create(Context context, Bundle bundle, int i, int i2) {
        int iNativeGetSubdeviceCount = nativeGetSubdeviceCount(i, i2);
        if (iNativeGetSubdeviceCount <= 0) {
            Log.e(TAG, "nativeGetSubdeviceCount failed");
            return null;
        }
        UsbMidiDevice usbMidiDevice = new UsbMidiDevice(i, i2, iNativeGetSubdeviceCount);
        if (!usbMidiDevice.register(context, bundle)) {
            IoUtils.closeQuietly(usbMidiDevice);
            Log.e(TAG, "createDeviceServer failed");
            return null;
        }
        return usbMidiDevice;
    }

    private UsbMidiDevice(int i, int i2, int i3) {
        this.mAlsaCard = i;
        this.mAlsaDevice = i2;
        this.mSubdeviceCount = i3;
        this.mInputPortReceivers = new InputReceiverProxy[i3];
        for (int i4 = 0; i4 < i3; i4++) {
            this.mInputPortReceivers[i4] = new InputReceiverProxy();
        }
    }

    private boolean openLocked() {
        FileDescriptor[] fileDescriptorArrNativeOpen = nativeOpen(this.mAlsaCard, this.mAlsaDevice, this.mSubdeviceCount);
        if (fileDescriptorArrNativeOpen == null) {
            Log.e(TAG, "nativeOpen failed");
            return false;
        }
        this.mFileDescriptors = fileDescriptorArrNativeOpen;
        int length = fileDescriptorArrNativeOpen.length;
        int length2 = fileDescriptorArrNativeOpen.length - 1;
        this.mPollFDs = new StructPollfd[length];
        this.mInputStreams = new FileInputStream[length];
        for (int i = 0; i < length; i++) {
            FileDescriptor fileDescriptor = fileDescriptorArrNativeOpen[i];
            StructPollfd structPollfd = new StructPollfd();
            structPollfd.fd = fileDescriptor;
            structPollfd.events = (short) OsConstants.POLLIN;
            this.mPollFDs[i] = structPollfd;
            this.mInputStreams[i] = new FileInputStream(fileDescriptor);
        }
        this.mOutputStreams = new FileOutputStream[length2];
        this.mEventSchedulers = new MidiEventScheduler[length2];
        for (int i2 = 0; i2 < length2; i2++) {
            this.mOutputStreams[i2] = new FileOutputStream(fileDescriptorArrNativeOpen[i2]);
            MidiEventScheduler midiEventScheduler = new MidiEventScheduler();
            this.mEventSchedulers[i2] = midiEventScheduler;
            this.mInputPortReceivers[i2].setReceiver(midiEventScheduler.getReceiver());
        }
        final MidiReceiver[] outputPortReceivers = this.mServer.getOutputPortReceivers();
        new Thread("UsbMidiDevice input thread") {
            @Override
            public void run() {
                long jNanoTime;
                byte[] bArr = new byte[512];
                while (true) {
                    try {
                        jNanoTime = System.nanoTime();
                    } catch (ErrnoException e) {
                        Log.d(UsbMidiDevice.TAG, "reader thread exiting");
                    } catch (IOException e2) {
                        Log.d(UsbMidiDevice.TAG, "reader thread exiting");
                    }
                    synchronized (UsbMidiDevice.this.mLock) {
                        if (!UsbMidiDevice.this.mIsOpen) {
                            break;
                        }
                        for (int i3 = 0; i3 < UsbMidiDevice.this.mPollFDs.length; i3++) {
                            StructPollfd structPollfd2 = UsbMidiDevice.this.mPollFDs[i3];
                            if ((structPollfd2.revents & (OsConstants.POLLERR | OsConstants.POLLHUP)) != 0) {
                                break;
                            }
                            if ((structPollfd2.revents & OsConstants.POLLIN) != 0) {
                                structPollfd2.revents = (short) 0;
                                if (i3 == UsbMidiDevice.this.mInputStreams.length - 1) {
                                    break;
                                }
                                outputPortReceivers[i3].send(bArr, 0, UsbMidiDevice.this.mInputStreams[i3].read(bArr), jNanoTime);
                            }
                        }
                        Log.d(UsbMidiDevice.TAG, "reader thread exiting");
                        Log.d(UsbMidiDevice.TAG, "input thread exit");
                    }
                    Os.poll(UsbMidiDevice.this.mPollFDs, -1);
                }
                Log.d(UsbMidiDevice.TAG, "input thread exit");
            }
        }.start();
        for (int i3 = 0; i3 < length2; i3++) {
            final MidiEventScheduler midiEventScheduler2 = this.mEventSchedulers[i3];
            final FileOutputStream fileOutputStream = this.mOutputStreams[i3];
            final int i4 = i3;
            new Thread("UsbMidiDevice output thread " + i3) {
                @Override
                public void run() {
                    MidiEventScheduler.MidiEvent midiEventWaitNextEvent;
                    while (true) {
                        try {
                            midiEventWaitNextEvent = midiEventScheduler2.waitNextEvent();
                        } catch (InterruptedException e) {
                        }
                        if (midiEventWaitNextEvent != null) {
                            try {
                                fileOutputStream.write(midiEventWaitNextEvent.data, 0, midiEventWaitNextEvent.count);
                            } catch (IOException e2) {
                                Log.e(UsbMidiDevice.TAG, "write failed for port " + i4);
                            }
                            midiEventScheduler2.addEventToPool(midiEventWaitNextEvent);
                        } else {
                            Log.d(UsbMidiDevice.TAG, "output thread exit");
                            return;
                        }
                    }
                }
            }.start();
        }
        this.mIsOpen = true;
        return true;
    }

    private boolean register(Context context, Bundle bundle) {
        MidiManager midiManager = (MidiManager) context.getSystemService("midi");
        if (midiManager == null) {
            Log.e(TAG, "No MidiManager in UsbMidiDevice.create()");
            return false;
        }
        this.mServer = midiManager.createDeviceServer(this.mInputPortReceivers, this.mSubdeviceCount, null, null, bundle, 1, this.mCallback);
        if (this.mServer == null) {
            return false;
        }
        return true;
    }

    @Override
    public void close() throws IOException {
        synchronized (this.mLock) {
            if (this.mIsOpen) {
                closeLocked();
            }
        }
        if (this.mServer != null) {
            IoUtils.closeQuietly(this.mServer);
        }
    }

    private void closeLocked() {
        for (int i = 0; i < this.mEventSchedulers.length; i++) {
            this.mInputPortReceivers[i].setReceiver(null);
            this.mEventSchedulers[i].close();
        }
        this.mEventSchedulers = null;
        for (int i2 = 0; i2 < this.mInputStreams.length; i2++) {
            IoUtils.closeQuietly(this.mInputStreams[i2]);
        }
        this.mInputStreams = null;
        for (int i3 = 0; i3 < this.mOutputStreams.length; i3++) {
            IoUtils.closeQuietly(this.mOutputStreams[i3]);
        }
        this.mOutputStreams = null;
        nativeClose(this.mFileDescriptors);
        this.mFileDescriptors = null;
        this.mIsOpen = false;
    }

    public void dump(String str, DualDumpOutputStream dualDumpOutputStream, String str2, long j) {
        long jStart = dualDumpOutputStream.start(str2, j);
        dualDumpOutputStream.write("device_address", 1138166333443L, str);
        dualDumpOutputStream.write("card", 1120986464257L, this.mAlsaCard);
        dualDumpOutputStream.write("device", 1120986464258L, this.mAlsaDevice);
        dualDumpOutputStream.end(jStart);
    }
}
