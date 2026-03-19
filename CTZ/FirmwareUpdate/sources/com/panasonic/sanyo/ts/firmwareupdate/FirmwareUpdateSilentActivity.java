package com.panasonic.sanyo.ts.firmwareupdate;

public class FirmwareUpdateSilentActivity extends BaseActivity {
    @Override
    protected void onResume() {
        super.onResume();
        this.UpdateCancel = false;
        this.SDPath = getIntent().getData().getPath();
        startprogress();
    }

    @Override
    protected void onPause() {
        super.onPause();
        finish();
    }
}
