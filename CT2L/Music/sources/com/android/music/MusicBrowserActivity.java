package com.android.music;

import android.app.Activity;
import android.content.ComponentName;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import android.os.RemoteException;
import com.android.music.IMediaPlaybackService;
import com.android.music.MusicUtils;

public class MusicBrowserActivity extends Activity {
    private ServiceConnection autoshuffle = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName classname, IBinder obj) {
            try {
                MusicBrowserActivity.this.unbindService(this);
            } catch (IllegalArgumentException e) {
            }
            IMediaPlaybackService serv = IMediaPlaybackService.Stub.asInterface(obj);
            if (serv != null) {
                try {
                    serv.setShuffleMode(2);
                } catch (RemoteException e2) {
                }
            }
        }

        @Override
        public void onServiceDisconnected(ComponentName classname) {
        }
    };
    private MusicUtils.ServiceToken mToken;

    @Override
    public void onCreate(Bundle icicle) {
        super.onCreate(icicle);
        int activeTab = MusicUtils.getIntPref(this, "activetab", R.id.artisttab);
        if (activeTab != R.id.artisttab && activeTab != R.id.albumtab && activeTab != R.id.songtab && activeTab != R.id.playlisttab) {
            activeTab = R.id.artisttab;
        }
        MusicUtils.activateTab(this, activeTab);
        String shuf = getIntent().getStringExtra("autoshuffle");
        if ("true".equals(shuf)) {
            this.mToken = MusicUtils.bindToService(this, this.autoshuffle);
        }
    }

    @Override
    public void onDestroy() {
        if (this.mToken != null) {
            MusicUtils.unbindFromService(this.mToken);
        }
        super.onDestroy();
    }
}
