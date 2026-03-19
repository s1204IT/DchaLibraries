package com.mediatek.gallery3d.video;

import android.app.Activity;
import android.net.Uri;
import android.nfc.NfcAdapter;
import android.nfc.NfcEvent;
import android.os.Bundle;
import android.os.Handler;
import com.android.gallery3d.app.MoviePlayer;
import com.mediatek.gallery3d.util.Log;

public class NfcHooker extends MovieHooker implements NfcAdapter.CreateBeamUrisCallback {
    private static final String TAG = "VP_NfcHooker";
    private IMovieItem mMovieItem;
    private NfcAdapter mNfcAdapter;
    private MoviePlayer mPlayer;
    private boolean mBeamVideoIsPlaying = false;
    private final Handler mHandler = new Handler();
    private final Runnable mPlayVideoRunnable = new Runnable() {
        @Override
        public void run() {
            if (NfcHooker.this.mPlayer != null && NfcHooker.this.mBeamVideoIsPlaying) {
                Log.v(NfcHooker.TAG, "NFC call play video");
                NfcHooker.this.mPlayer.onPlayPause();
            }
        }
    };
    private final Runnable mPauseVideoRunnable = new Runnable() {
        @Override
        public void run() {
            if (NfcHooker.this.mPlayer == null || !NfcHooker.this.mPlayer.isPlaying()) {
                NfcHooker.this.mBeamVideoIsPlaying = false;
                return;
            }
            Log.v(NfcHooker.TAG, "NFC call pause video");
            NfcHooker.this.mBeamVideoIsPlaying = true;
            NfcHooker.this.mPlayer.onPlayPause();
        }
    };

    @Override
    public void setParameter(String str, Object obj) {
        super.setParameter(str, obj);
        if (obj instanceof IMovieItem) {
            this.mMovieItem = (IMovieItem) obj;
        }
        if (obj instanceof MoviePlayer) {
            this.mPlayer = obj;
        }
    }

    @Override
    public void onCreate(Bundle bundle) {
        init();
    }

    private void init() {
        this.mNfcAdapter = NfcAdapter.getDefaultAdapter(getContext().getApplicationContext());
        if (this.mNfcAdapter == null) {
            Log.w(TAG, "NFC not available!");
            return;
        }
        this.mNfcAdapter.setBeamPushUrisCallback(this, getContext());
        this.mNfcAdapter.setOnNdefPushCompleteCallback(new NfcAdapter.OnNdefPushCompleteCallback() {
            @Override
            public void onNdefPushComplete(NfcEvent nfcEvent) {
                NfcHooker.this.mHandler.removeCallbacks(NfcHooker.this.mPlayVideoRunnable);
                NfcHooker.this.mHandler.post(NfcHooker.this.mPlayVideoRunnable);
            }
        }, getContext(), new Activity[]{getContext()});
    }

    @Override
    public Uri[] createBeamUris(NfcEvent nfcEvent) {
        if (this.mMovieItem == null) {
            Log.v(TAG, "createBeamUris, mMovieItem == null, return");
            return null;
        }
        this.mHandler.removeCallbacks(this.mPauseVideoRunnable);
        this.mHandler.postDelayed(this.mPauseVideoRunnable, 500L);
        Uri uri = this.mMovieItem.getUri();
        Log.v(TAG, "NFC call for uri " + uri);
        return new Uri[]{uri};
    }
}
