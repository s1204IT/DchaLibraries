package com.panasonic.sanyo.ts.firmwareupdate;

/* loaded from: classes.dex */
public class FirmwareUpdateSilentActivity extends BaseActivity {
    @Override // com.panasonic.sanyo.ts.firmwareupdate.BaseActivity, android.app.Activity
    protected void onResume() {
        super.onResume();
        this.UpdateCancel = false;
        this.SDPath = getIntent().getData().getPath();
        startprogress();
    }

    @Override // com.panasonic.sanyo.ts.firmwareupdate.BaseActivity, android.app.Activity
    protected void onPause() {
        super.onPause();
        finish();
    }
}
