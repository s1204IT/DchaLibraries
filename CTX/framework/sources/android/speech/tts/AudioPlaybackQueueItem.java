package android.speech.tts;

import android.content.Context;
import android.media.AudioAttributes;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.ConditionVariable;
import android.speech.tts.TextToSpeechService;
import android.util.Log;

class AudioPlaybackQueueItem extends PlaybackQueueItem {
    private static final String TAG = "TTS.AudioQueueItem";
    private final TextToSpeechService.AudioOutputParams mAudioParams;
    private final Context mContext;
    private final ConditionVariable mDone;
    private volatile boolean mFinished;
    private MediaPlayer mPlayer;
    private final Uri mUri;

    AudioPlaybackQueueItem(TextToSpeechService.UtteranceProgressDispatcher utteranceProgressDispatcher, Object obj, Context context, Uri uri, TextToSpeechService.AudioOutputParams audioOutputParams) {
        super(utteranceProgressDispatcher, obj);
        this.mContext = context;
        this.mUri = uri;
        this.mAudioParams = audioOutputParams;
        this.mDone = new ConditionVariable();
        this.mPlayer = null;
        this.mFinished = false;
    }

    @Override
    public void run() {
        TextToSpeechService.UtteranceProgressDispatcher dispatcher = getDispatcher();
        dispatcher.dispatchOnStart();
        int i = this.mAudioParams.mSessionId;
        Context context = this.mContext;
        Uri uri = this.mUri;
        AudioAttributes audioAttributes = this.mAudioParams.mAudioAttributes;
        if (i <= 0) {
            i = 0;
        }
        this.mPlayer = MediaPlayer.create(context, uri, null, audioAttributes, i);
        if (this.mPlayer == null) {
            dispatcher.dispatchOnError(-5);
            return;
        }
        try {
            this.mPlayer.setOnErrorListener(new MediaPlayer.OnErrorListener() {
                @Override
                public boolean onError(MediaPlayer mediaPlayer, int i2, int i3) {
                    Log.w(AudioPlaybackQueueItem.TAG, "Audio playback error: " + i2 + ", " + i3);
                    AudioPlaybackQueueItem.this.mDone.open();
                    return true;
                }
            });
            this.mPlayer.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
                @Override
                public void onCompletion(MediaPlayer mediaPlayer) {
                    AudioPlaybackQueueItem.this.mFinished = true;
                    AudioPlaybackQueueItem.this.mDone.open();
                }
            });
            setupVolume(this.mPlayer, this.mAudioParams.mVolume, this.mAudioParams.mPan);
            this.mPlayer.start();
            this.mDone.block();
            finish();
        } catch (IllegalArgumentException e) {
            Log.w(TAG, "MediaPlayer failed", e);
            this.mDone.open();
        }
        if (this.mFinished) {
            dispatcher.dispatchOnSuccess();
        } else {
            dispatcher.dispatchOnStop();
        }
    }

    private static void setupVolume(MediaPlayer mediaPlayer, float f, float f2) {
        float f3;
        float fClip = clip(f, 0.0f, 1.0f);
        float fClip2 = clip(f2, -1.0f, 1.0f);
        if (fClip2 > 0.0f) {
            float f4 = fClip * (1.0f - fClip2);
            f3 = fClip;
            fClip = f4;
        } else if (fClip2 < 0.0f) {
            f3 = fClip * (1.0f + fClip2);
        } else {
            f3 = fClip;
        }
        mediaPlayer.setVolume(fClip, f3);
    }

    private static final float clip(float f, float f2, float f3) {
        return f < f2 ? f2 : f < f3 ? f : f3;
    }

    private void finish() {
        try {
            this.mPlayer.stop();
        } catch (IllegalStateException e) {
        }
        this.mPlayer.release();
    }

    @Override
    void stop(int i) {
        this.mDone.open();
    }
}
