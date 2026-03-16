package com.android.music;

import android.app.Activity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

public class DeleteItems extends Activity {
    private Button mButton;
    private View.OnClickListener mButtonClicked = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            MusicUtils.deleteTracks(DeleteItems.this, DeleteItems.this.mItemList);
            DeleteItems.this.finish();
        }
    };
    private long[] mItemList;
    private TextView mPrompt;

    @Override
    public void onCreate(Bundle icicle) {
        super.onCreate(icicle);
        setVolumeControlStream(3);
        requestWindowFeature(1);
        setContentView(R.layout.confirm_delete);
        getWindow().setLayout(-1, -2);
        this.mPrompt = (TextView) findViewById(R.id.prompt);
        this.mButton = (Button) findViewById(R.id.delete);
        this.mButton.setOnClickListener(this.mButtonClicked);
        ((Button) findViewById(R.id.cancel)).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                DeleteItems.this.finish();
            }
        });
        Bundle b = getIntent().getExtras();
        String desc = b.getString("description");
        this.mItemList = b.getLongArray("items");
        this.mPrompt.setText(desc);
    }
}
