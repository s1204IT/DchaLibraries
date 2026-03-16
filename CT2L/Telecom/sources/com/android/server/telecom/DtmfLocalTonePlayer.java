package com.android.server.telecom;

import android.content.Context;
import android.media.ToneGenerator;
import android.provider.Settings;

class DtmfLocalTonePlayer extends CallsManagerListenerBase {
    private Call mCall;
    private final Context mContext;
    private ToneGenerator mToneGenerator;

    public DtmfLocalTonePlayer(Context context) {
        this.mContext = context;
    }

    @Override
    public void onForegroundCallChanged(Call call, Call call2) {
        endDtmfSession(call);
        startDtmfSession(call2);
    }

    void playTone(Call call, char c) {
        if (this.mCall == call) {
            if (this.mToneGenerator == null) {
                Log.d(this, "playTone: mToneGenerator == null, %c.", Character.valueOf(c));
                return;
            }
            Log.d(this, "starting local tone: %c.", Character.valueOf(c));
            int mappedTone = getMappedTone(c);
            if (mappedTone != -1) {
                this.mToneGenerator.startTone(mappedTone, -1);
            }
        }
    }

    void stopTone(Call call) {
        if (this.mCall == call) {
            if (this.mToneGenerator == null) {
                Log.d(this, "stopTone: mToneGenerator == null.", new Object[0]);
            } else {
                Log.d(this, "stopping local tone.", new Object[0]);
                this.mToneGenerator.stopTone();
            }
        }
    }

    private void startDtmfSession(Call call) {
        boolean z = true;
        if (call != null) {
            Context context = call.getContext();
            if (!context.getResources().getBoolean(R.bool.allow_local_dtmf_tones) || Settings.System.getInt(context.getContentResolver(), "dtmf_tone", 1) != 1) {
                z = false;
            }
            this.mCall = call;
            if (z && this.mToneGenerator == null) {
                try {
                    this.mToneGenerator = new ToneGenerator(8, 80);
                } catch (RuntimeException e) {
                    Log.e(this, e, "Error creating local tone generator.", new Object[0]);
                    this.mToneGenerator = null;
                }
            }
        }
    }

    private void endDtmfSession(Call call) {
        if (call != null && this.mCall == call) {
            stopTone(call);
            this.mCall = null;
            if (this.mToneGenerator != null) {
                this.mToneGenerator.release();
                this.mToneGenerator = null;
            }
        }
    }

    private static final int getMappedTone(char c) {
        if (c >= '0' && c <= '9') {
            return (c + 0) - 48;
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
