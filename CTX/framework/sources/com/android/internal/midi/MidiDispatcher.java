package com.android.internal.midi;

import android.media.midi.MidiReceiver;
import android.media.midi.MidiSender;
import java.io.IOException;
import java.util.concurrent.CopyOnWriteArrayList;

public final class MidiDispatcher extends MidiReceiver {
    private final MidiReceiverFailureHandler mFailureHandler;
    private final CopyOnWriteArrayList<MidiReceiver> mReceivers;
    private final MidiSender mSender;

    public interface MidiReceiverFailureHandler {
        void onReceiverFailure(MidiReceiver midiReceiver, IOException iOException);
    }

    public MidiDispatcher() {
        this(null);
    }

    public MidiDispatcher(MidiReceiverFailureHandler midiReceiverFailureHandler) {
        this.mReceivers = new CopyOnWriteArrayList<>();
        this.mSender = new MidiSender() {
            @Override
            public void onConnect(MidiReceiver midiReceiver) {
                MidiDispatcher.this.mReceivers.add(midiReceiver);
            }

            @Override
            public void onDisconnect(MidiReceiver midiReceiver) {
                MidiDispatcher.this.mReceivers.remove(midiReceiver);
            }
        };
        this.mFailureHandler = midiReceiverFailureHandler;
    }

    public int getReceiverCount() {
        return this.mReceivers.size();
    }

    public MidiSender getSender() {
        return this.mSender;
    }

    @Override
    public void onSend(byte[] bArr, int i, int i2, long j) throws IOException {
        for (MidiReceiver midiReceiver : this.mReceivers) {
            try {
                midiReceiver.send(bArr, i, i2, j);
            } catch (IOException e) {
                this.mReceivers.remove(midiReceiver);
                if (this.mFailureHandler != null) {
                    this.mFailureHandler.onReceiverFailure(midiReceiver, e);
                }
            }
        }
    }

    @Override
    public void onFlush() throws IOException {
        for (MidiReceiver midiReceiver : this.mReceivers) {
            try {
                midiReceiver.flush();
            } catch (IOException e) {
                this.mReceivers.remove(midiReceiver);
                if (this.mFailureHandler != null) {
                    this.mFailureHandler.onReceiverFailure(midiReceiver, e);
                }
            }
        }
    }
}
