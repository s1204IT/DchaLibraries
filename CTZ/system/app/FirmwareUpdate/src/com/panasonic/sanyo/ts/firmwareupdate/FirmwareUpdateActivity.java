package com.panasonic.sanyo.ts.firmwareupdate;

import android.os.Bundle;
import android.view.View;
import android.widget.Button;

/* loaded from: classes.dex */
public class FirmwareUpdateActivity extends BaseActivity {
    private Button FirmUpButton;

    @Override // com.panasonic.sanyo.ts.firmwareupdate.BaseActivity, android.app.Activity
    public void onCreate(Bundle bundle) {
        super.onCreate(bundle);
        setContentView(R.layout.main);
        this.FirmUpButton = (Button) findViewById(R.id.button1);
        this.FirmUpButton.setOnClickListener(new View.OnClickListener() { // from class: com.panasonic.sanyo.ts.firmwareupdate.FirmwareUpdateActivity.1
            @Override // android.view.View.OnClickListener
            public void onClick(View view) {
                FirmwareUpdateActivity.this.UpdateCancel = false;
                FirmwareUpdateActivity.this.startprogress();
            }
        });
    }
}
