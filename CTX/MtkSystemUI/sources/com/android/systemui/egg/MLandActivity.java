package com.android.systemui.egg;

import android.app.Activity;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import com.android.systemui.R;

public class MLandActivity extends Activity {
    MLand mLand;

    @Override
    public void onCreate(Bundle bundle) {
        super.onCreate(bundle);
        setContentView(R.layout.mland);
        this.mLand = (MLand) findViewById(R.id.world);
        this.mLand.setScoreFieldHolder((ViewGroup) findViewById(R.id.scores));
        this.mLand.setSplash(findViewById(R.id.welcome));
        int size = this.mLand.getGameControllers().size();
        if (size > 0) {
            this.mLand.setupPlayers(size);
        }
    }

    public void updateSplashPlayers() {
        int numPlayers = this.mLand.getNumPlayers();
        View viewFindViewById = findViewById(R.id.player_minus_button);
        View viewFindViewById2 = findViewById(R.id.player_plus_button);
        if (numPlayers == 1) {
            viewFindViewById.setVisibility(4);
            viewFindViewById2.setVisibility(0);
            viewFindViewById2.requestFocus();
            return;
        }
        MLand mLand = this.mLand;
        if (numPlayers == 6) {
            viewFindViewById.setVisibility(0);
            viewFindViewById2.setVisibility(4);
            viewFindViewById.requestFocus();
        } else {
            viewFindViewById.setVisibility(0);
            viewFindViewById2.setVisibility(0);
        }
    }

    @Override
    public void onPause() {
        this.mLand.stop();
        super.onPause();
    }

    @Override
    public void onResume() {
        super.onResume();
        this.mLand.onAttachedToWindow();
        updateSplashPlayers();
        this.mLand.showSplash();
    }

    public void playerMinus(View view) {
        this.mLand.removePlayer();
        updateSplashPlayers();
    }

    public void playerPlus(View view) {
        this.mLand.addPlayer();
        updateSplashPlayers();
    }

    public void startButtonPressed(View view) {
        findViewById(R.id.player_minus_button).setVisibility(4);
        findViewById(R.id.player_plus_button).setVisibility(4);
        this.mLand.start(true);
    }
}
