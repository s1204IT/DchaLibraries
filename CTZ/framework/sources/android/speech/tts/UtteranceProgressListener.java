package android.speech.tts;

import android.speech.tts.TextToSpeech;

public abstract class UtteranceProgressListener {
    public abstract void onDone(String str);

    @Deprecated
    public abstract void onError(String str);

    public abstract void onStart(String str);

    public void onError(String str, int i) {
        onError(str);
    }

    public void onStop(String str, boolean z) {
    }

    public void onBeginSynthesis(String str, int i, int i2, int i3) {
    }

    public void onAudioAvailable(String str, byte[] bArr) {
    }

    public void onRangeStart(String str, int i, int i2, int i3) {
        onUtteranceRangeStart(str, i, i2);
    }

    @Deprecated
    public void onUtteranceRangeStart(String str, int i, int i2) {
    }

    static UtteranceProgressListener from(final TextToSpeech.OnUtteranceCompletedListener onUtteranceCompletedListener) {
        return new UtteranceProgressListener() {
            @Override
            public synchronized void onDone(String str) {
                onUtteranceCompletedListener.onUtteranceCompleted(str);
            }

            @Override
            public void onError(String str) {
                onUtteranceCompletedListener.onUtteranceCompleted(str);
            }

            @Override
            public void onStart(String str) {
            }

            @Override
            public void onStop(String str, boolean z) {
                onUtteranceCompletedListener.onUtteranceCompleted(str);
            }
        };
    }
}
