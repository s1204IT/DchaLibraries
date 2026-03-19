package com.android.traceur;

import android.graphics.drawable.Icon;
import android.preference.PreferenceManager;
import android.service.quicksettings.TileService;

public class QsService extends TileService {
    private static QsService sListeningInstance;

    public static void updateTile() {
        if (sListeningInstance != null) {
            sListeningInstance.update();
        }
    }

    @Override
    public void onStartListening() {
        sListeningInstance = this;
        update();
    }

    @Override
    public void onStopListening() {
        if (sListeningInstance == this) {
            sListeningInstance = null;
        }
    }

    private void update() {
        boolean z = PreferenceManager.getDefaultSharedPreferences(this).getBoolean(getString(R.string.pref_key_tracing_on), false);
        String string = getString(z ? R.string.stop_tracing : R.string.record_trace);
        getQsTile().setIcon(Icon.createWithResource(this, R.drawable.stat_sys_adb));
        getQsTile().setState(z ? 2 : 1);
        getQsTile().setLabel(string);
        getQsTile().updateTile();
    }

    @Override
    public void onClick() {
        PreferenceManager.getDefaultSharedPreferences(this).edit().putBoolean(getString(R.string.pref_key_tracing_on), !r0.getBoolean(getString(R.string.pref_key_tracing_on), false)).apply();
        Receiver.updateTracing(this);
    }
}
