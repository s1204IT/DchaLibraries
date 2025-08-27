package jp.co.benesse.dcha.systemsettings;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import jp.co.benesse.dcha.util.Logger;

/* loaded from: classes.dex */
public class AbandonSettingActivity extends ParentSettingActivity implements View.OnClickListener {
    private ImageView mNoBtn;
    private ImageView mYesBtn;

    @Override // jp.co.benesse.dcha.systemsettings.ParentSettingActivity, android.app.Activity
    protected void onCreate(Bundle bundle) {
        Logger.d("AbandonSettingActivity", "onCreate 0001");
        super.onCreate(bundle);
        setContentView(R.layout.act_abandon);
        this.mYesBtn = (ImageView) findViewById(R.id.yes);
        this.mNoBtn = (ImageView) findViewById(R.id.no);
        this.mYesBtn.setOnClickListener(this);
        this.mNoBtn.setOnClickListener(this);
        Logger.d("AbandonSettingActivity", "onCreate 0002");
    }

    @Override // jp.co.benesse.dcha.systemsettings.ParentSettingActivity, android.app.Activity
    protected void onDestroy() {
        Logger.d("AbandonSettingActivity", "onDestroy 0001");
        super.onDestroy();
        this.mYesBtn.setOnClickListener(null);
        this.mNoBtn.setOnClickListener(null);
        this.mYesBtn = null;
        this.mNoBtn = null;
        Logger.d("AbandonSettingActivity", "onDestroy 0002");
    }

    @Override // android.view.View.OnClickListener
    public void onClick(View view) {
        Logger.d("AbandonSettingActivity", "onClick 0001");
        this.mNoBtn.setClickable(false);
        this.mYesBtn.setClickable(false);
        if (view.getId() == this.mNoBtn.getId()) {
            Logger.d("AbandonSettingActivity", "onClick 0002");
            finish();
        } else if (view.getId() == this.mYesBtn.getId()) {
            Logger.d("AbandonSettingActivity", "onClick 0003");
            Intent intent = new Intent("android.intent.action.MASTER_CLEAR");
            intent.putExtra("Terminate", "sY4r50Og");
            sendBroadcast(intent);
            finish();
        }
        Logger.d("AbandonSettingActivity", "onClick 0004");
    }
}
