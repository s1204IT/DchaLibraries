package com.panasonic.sanyo.ts.touchpanelfwupdate;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.Cursor;
import android.os.Bundle;
import android.os.PowerManager;
import android.provider.Settings;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

/* loaded from: classes.dex */
public class TouchpanelFwUpdateActivity extends Activity {
    Button mButton = null;
    ProgressDialog mProgressDialog = null;
    boolean bResumed = false;
    String mPendingPath = null;
    private BroadcastReceiver mReceiver = new BroadcastReceiver() { // from class: com.panasonic.sanyo.ts.touchpanelfwupdate.TouchpanelFwUpdateActivity.2
        @Override // android.content.BroadcastReceiver
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            Log.d("TouchpanelFwUpdateActivity", "----- onReceive() : action[" + action + "] -----");
            if ("com.panasonic.sanyo.ts.intent.action.TOUCHPANEL_FIRMWARE_UPDATED".equals(action)) {
                if (TouchpanelFwUpdateActivity.this.mProgressDialog != null) {
                    TouchpanelFwUpdateActivity.this.mProgressDialog.dismiss();
                    TouchpanelFwUpdateActivity.this.mProgressDialog = null;
                }
                if (intent.getIntExtra("result", -1) == 0) {
                    ((PowerManager) TouchpanelFwUpdateActivity.this.getSystemService("power")).reboot(null);
                } else {
                    Toast.makeText(TouchpanelFwUpdateActivity.this, "アップデートに失敗しました", 1).show();
                }
            }
        }
    };

    @Override // android.app.Activity
    public void onCreate(Bundle bundle) {
        super.onCreate(bundle);
        setContentView(R.layout.main);
        this.mButton = (Button) findViewById(R.id.button1);
        this.mButton.setOnClickListener(new View.OnClickListener() { // from class: com.panasonic.sanyo.ts.touchpanelfwupdate.TouchpanelFwUpdateActivity.1
            @Override // android.view.View.OnClickListener
            public void onClick(View view) {
                Intent intent = new Intent("android.intent.action.OPEN_DOCUMENT");
                intent.setType("*/*");
                TouchpanelFwUpdateActivity.this.startActivityForResult(intent, 8634);
            }
        });
    }

    @Override // android.app.Activity
    protected void onResume() {
        super.onResume();
        registerReceiver(this.mReceiver, new IntentFilter("com.panasonic.sanyo.ts.intent.action.TOUCHPANEL_FIRMWARE_UPDATED"));
        this.bResumed = true;
        if (this.mPendingPath != null) {
            String str = this.mPendingPath;
            this.mPendingPath = null;
            updateTouchpanelFw(str);
        }
    }

    @Override // android.app.Activity
    protected void onPause() {
        super.onPause();
        this.bResumed = false;
        unregisterReceiver(this.mReceiver);
    }

    /* JADX DEBUG: Don't trust debug lines info. Repeating lines: [101=4] */
    @Override // android.app.Activity
    protected void onActivityResult(int i, int i2, Intent intent) {
        if (i != 8634 || i2 != -1 || intent == null) {
            Log.e("TouchpanelFwUpdateActivity", "----- Failed! -----");
            return;
        }
        Cursor cursorQuery = getContentResolver().query(intent.getData(), new String[]{"_data"}, null, null);
        if (cursorQuery == null) {
            Toast.makeText(this, "ファイルが見つかりません", 0).show();
            return;
        }
        try {
            if (cursorQuery.getCount() != 1) {
                Toast.makeText(this, "ファイルが見つかりません", 0).show();
                return;
            }
            cursorQuery.moveToFirst();
            int columnIndex = cursorQuery.getColumnIndex("_data");
            String string = columnIndex >= 0 ? cursorQuery.getString(columnIndex) : null;
            if (string == null) {
                Toast.makeText(this, "ファイルが見つかりません", 0).show();
                return;
            }
            Log.d("TouchpanelFwUpdateActivity", "----- path : " + string + " -----");
            if (this.bResumed) {
                updateTouchpanelFw(string);
            } else {
                this.mPendingPath = string;
            }
        } finally {
            cursorQuery.close();
        }
    }

    private void updateTouchpanelFw(String str) {
        this.mPendingPath = null;
        if (!Settings.System.putString(getContentResolver(), "bc:touchpanel:nvt:fw_update", str)) {
            Toast.makeText(this, "アップデートできません", 0).show();
            return;
        }
        if (this.mProgressDialog != null) {
            this.mProgressDialog.dismiss();
            this.mProgressDialog = null;
        }
        this.mProgressDialog = new ProgressDialog(this);
        this.mProgressDialog.setProgressStyle(0);
        this.mProgressDialog.setTitle("アップデート実行中");
        this.mProgressDialog.setMessage("端末に触れないでください");
        this.mProgressDialog.setCancelable(false);
        this.mProgressDialog.show();
    }
}
