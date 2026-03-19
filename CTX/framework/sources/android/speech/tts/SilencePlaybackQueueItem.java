package android.speech.tts;

import android.os.ConditionVariable;
import android.speech.tts.TextToSpeechService;

class SilencePlaybackQueueItem extends PlaybackQueueItem {
    private final ConditionVariable mCondVar;
    private final long mSilenceDurationMs;

    SilencePlaybackQueueItem(TextToSpeechService.UtteranceProgressDispatcher utteranceProgressDispatcher, Object obj, long j) {
        super(utteranceProgressDispatcher, obj);
        this.mCondVar = new ConditionVariable();
        this.mSilenceDurationMs = j;
    }

    @Override
    public void run() {
        boolean zBlock;
        getDispatcher().dispatchOnStart();
        if (this.mSilenceDurationMs > 0) {
            zBlock = this.mCondVar.block(this.mSilenceDurationMs);
        } else {
            zBlock = false;
        }
        if (zBlock) {
            getDispatcher().dispatchOnStop();
        } else {
            getDispatcher().dispatchOnSuccess();
        }
    }

    @Override
    void stop(int i) {
        this.mCondVar.open();
    }
}
