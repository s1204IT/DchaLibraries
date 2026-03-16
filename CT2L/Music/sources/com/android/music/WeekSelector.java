package com.android.music;

import android.app.Activity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;

public class WeekSelector extends Activity {
    private View.OnClickListener mListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            int numweeks = WeekSelector.this.mWeeks.getCurrentSelectedPos() + 1;
            MusicUtils.setIntPref(WeekSelector.this, "numweeks", numweeks);
            WeekSelector.this.setResult(-1);
            WeekSelector.this.finish();
        }
    };
    VerticalTextSpinner mWeeks;

    @Override
    public void onCreate(Bundle icicle) {
        super.onCreate(icicle);
        setVolumeControlStream(3);
        requestWindowFeature(1);
        setContentView(R.layout.weekpicker);
        getWindow().setLayout(-1, -2);
        this.mWeeks = (VerticalTextSpinner) findViewById(R.id.weeks);
        this.mWeeks.setItems(getResources().getStringArray(R.array.weeklist));
        this.mWeeks.setWrapAround(false);
        this.mWeeks.setScrollInterval(200L);
        int def = MusicUtils.getIntPref(this, "numweeks", 2);
        int pos = icicle != null ? icicle.getInt("numweeks", def - 1) : def - 1;
        this.mWeeks.setSelectedPos(pos);
        ((Button) findViewById(R.id.set)).setOnClickListener(this.mListener);
        ((Button) findViewById(R.id.cancel)).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                WeekSelector.this.setResult(0);
                WeekSelector.this.finish();
            }
        });
    }

    @Override
    public void onSaveInstanceState(Bundle outcicle) {
        outcicle.putInt("numweeks", this.mWeeks.getCurrentSelectedPos());
    }

    @Override
    public void onResume() {
        super.onResume();
    }
}
