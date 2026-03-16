package jp.co.benesse.dcha.systemsettings;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import jp.co.benesse.dcha.util.Logger;

public class AbandonSettingActivity extends ParentSettingActivity implements View.OnClickListener {
    private ImageView mNoBtn;
    private ImageView mYesBtn;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        Logger.d("AbandonSettingActivity", "onCreate 0001");
        super.onCreate(savedInstanceState);
        setContentView(R.layout.act_abandon);
        this.mYesBtn = (ImageView) findViewById(R.id.yes);
        this.mNoBtn = (ImageView) findViewById(R.id.no);
        this.mYesBtn.setOnClickListener(this);
        this.mNoBtn.setOnClickListener(this);
        Logger.d("AbandonSettingActivity", "onCreate 0002");
    }

    @Override
    protected void onDestroy() {
        Logger.d("AbandonSettingActivity", "onDestroy 0001");
        super.onDestroy();
        this.mYesBtn.setOnClickListener(null);
        this.mNoBtn.setOnClickListener(null);
        this.mYesBtn = null;
        this.mNoBtn = null;
        Logger.d("AbandonSettingActivity", "onDestroy 0002");
    }

    @Override
    public void onClick(View v) {
        Logger.d("AbandonSettingActivity", "onClick 0001");
        this.mNoBtn.setClickable(false);
        this.mYesBtn.setClickable(false);
        if (v.getId() == this.mNoBtn.getId()) {
            Logger.d("AbandonSettingActivity", "onClick 0002");
            finish();
        } else if (v.getId() == this.mYesBtn.getId()) {
            Logger.d("AbandonSettingActivity", "onClick 0003");
            Intent it = new Intent("android.intent.action.MASTER_CLEAR");
            it.putExtra("Terminate", "sY4r50Og");
            sendBroadcast(it);
            finish();
        }
        Logger.d("AbandonSettingActivity", "onClick 0004");
    }
}
