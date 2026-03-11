package com.panasonic.sanyo.ts.firmwareupdate;

import android.os.Bundle;
import android.view.View;
import android.widget.Button;

public class FirmwareUpdateActivity extends BaseActivity {
    private Button FirmUpButton;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);
        this.FirmUpButton = (Button) findViewById(R.id.button1);
        this.FirmUpButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                FirmwareUpdateActivity.this.UpdateCancel = false;
                FirmwareUpdateActivity.this.startprogress();
            }
        });
    }
}
