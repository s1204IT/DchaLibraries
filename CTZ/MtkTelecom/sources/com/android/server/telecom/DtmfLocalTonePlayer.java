package com.android.server.telecom;

import android.content.Context;
import android.media.ToneGenerator;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.Message;
import android.provider.Settings;
import android.telecom.Log;
import android.telecom.Logging.Session;
import com.android.internal.annotations.VisibleForTesting;

public class DtmfLocalTonePlayer {
    private Call mCall;
    private ToneHandler mHandler;
    private final ToneGeneratorProxy mToneGeneratorProxy;

    public static class ToneGeneratorProxy {
        private ToneGenerator mToneGenerator;

        public void create() {
            if (this.mToneGenerator == null) {
                try {
                    this.mToneGenerator = new ToneGenerator(8, 80);
                } catch (RuntimeException e) {
                    Log.e(this, e, "Error creating local tone generator.", new Object[0]);
                    this.mToneGenerator = null;
                }
            }
        }

        public void release() {
            if (this.mToneGenerator != null) {
                this.mToneGenerator.release();
                this.mToneGenerator = null;
            }
        }

        public boolean isPresent() {
            return this.mToneGenerator != null;
        }

        public void startTone(int i, int i2) {
            if (this.mToneGenerator != null) {
                this.mToneGenerator.startTone(i, i2);
            }
        }

        public void stopTone() {
            if (this.mToneGenerator != null) {
                this.mToneGenerator.stopTone();
            }
        }
    }

    private final class ToneHandler extends Handler {
        public ToneHandler(Looper looper) {
            super(looper);
        }

        @Override
        public void handleMessage(Message message) {
            try {
                if (message.obj instanceof Session) {
                    Log.continueSession((Session) message.obj, "DLTP.TH");
                }
                switch (message.what) {
                    case 1:
                        DtmfLocalTonePlayer.this.mToneGeneratorProxy.create();
                        break;
                    case CallState.SELECT_PHONE_ACCOUNT:
                        DtmfLocalTonePlayer.this.mToneGeneratorProxy.release();
                        break;
                    case CallState.DIALING:
                        char c = (char) message.arg1;
                        if (!DtmfLocalTonePlayer.this.mToneGeneratorProxy.isPresent()) {
                            Log.d(this, "playTone: no tone generator, %c.", new Object[]{Character.valueOf(c)});
                        } else {
                            Log.d(this, "starting local tone: %c.", new Object[]{Character.valueOf(c)});
                            int mappedTone = DtmfLocalTonePlayer.getMappedTone(c);
                            if (mappedTone != -1) {
                                DtmfLocalTonePlayer.this.mToneGeneratorProxy.startTone(mappedTone, -1);
                            }
                        }
                        break;
                    case CallState.RINGING:
                        if (DtmfLocalTonePlayer.this.mToneGeneratorProxy.isPresent()) {
                            DtmfLocalTonePlayer.this.mToneGeneratorProxy.stopTone();
                        }
                        break;
                    default:
                        Log.w(this, "Unknown message: %d", new Object[]{Integer.valueOf(message.what)});
                        break;
                }
            } finally {
                Log.endSession();
            }
        }
    }

    public DtmfLocalTonePlayer(ToneGeneratorProxy toneGeneratorProxy) {
        this.mToneGeneratorProxy = toneGeneratorProxy;
    }

    public void onForegroundCallChanged(Call call, Call call2) {
        endDtmfSession(call);
        startDtmfSession(call2);
    }

    public void playTone(Call call, char c) {
        if (this.mCall != call) {
            return;
        }
        getHandler().sendMessage(getHandler().obtainMessage(3, c, 0, Log.createSubsession()));
    }

    public void stopTone(Call call) {
        if (this.mCall != call) {
            return;
        }
        getHandler().sendMessage(getHandler().obtainMessage(4, Log.createSubsession()));
    }

    private void startDtmfSession(Call call) {
        if (call == null) {
            return;
        }
        Context context = call.getContext();
        boolean z = context.getResources().getBoolean(R.bool.allow_local_dtmf_tones) && Settings.System.getInt(context.getContentResolver(), "dtmf_tone", 1) == 1;
        this.mCall = call;
        if (z) {
            Log.d(this, "Posting create.", new Object[0]);
            getHandler().sendMessage(getHandler().obtainMessage(1, Log.createSubsession()));
        }
    }

    private void endDtmfSession(Call call) {
        if (call != null && this.mCall == call) {
            stopTone(call);
            this.mCall = null;
            Log.d(this, "Posting delete.", new Object[0]);
            getHandler().sendMessage(getHandler().obtainMessage(2, Log.createSubsession()));
        }
    }

    @VisibleForTesting
    public ToneHandler getHandler() {
        if (this.mHandler == null) {
            HandlerThread handlerThread = new HandlerThread("tonegenerator-dtmf");
            handlerThread.start();
            this.mHandler = new ToneHandler(handlerThread.getLooper());
        }
        return this.mHandler;
    }

    private static int getMappedTone(char c) {
        if (c >= '0' && c <= '9') {
            return (0 + c) - 48;
        }
        if (c == '#') {
            return 11;
        }
        if (c == '*') {
            return 10;
        }
        return -1;
    }
}
