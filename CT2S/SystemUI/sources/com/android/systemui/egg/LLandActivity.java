package com.android.systemui.egg;

import android.app.Activity;
import android.os.Bundle;
import android.widget.TextView;
import com.android.systemui.R;

public class LLandActivity extends Activity {
    LLand mLand;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.lland);
        this.mLand = (LLand) findViewById(R.id.world);
        this.mLand.setScoreField((TextView) findViewById(R.id.score));
        this.mLand.setSplash(findViewById(R.id.welcome));
    }

    @Override
    public void onPause() {
        this.mLand.stop();
        super.onPause();
    }
}
