package android.speech.tts;

import android.speech.tts.TextToSpeech;

public abstract class UtteranceProgressListener {
    public abstract void onDone(String str);

    @Deprecated
    public abstract void onError(String str);

    public abstract void onStart(String str);

    public void onError(String utteranceId, int errorCode) {
        onError(utteranceId);
    }

    static UtteranceProgressListener from(final TextToSpeech.OnUtteranceCompletedListener listener) {
        return new UtteranceProgressListener() {
            @Override
            public synchronized void onDone(String utteranceId) {
                listener.onUtteranceCompleted(utteranceId);
            }

            @Override
            public void onError(String utteranceId) {
                listener.onUtteranceCompleted(utteranceId);
            }

            @Override
            public void onStart(String utteranceId) {
            }
        };
    }
}
