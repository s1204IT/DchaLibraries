package com.mediatek.calendarimporter;

import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.os.Bundle;
import android.provider.CalendarContract;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;
import com.mediatek.calendarimporter.utils.LogUtils;
import com.mediatek.calendarimporter.utils.Utils;

public class ShowHandleResultActivity extends Activity {
    private static final String EXTRA_BEGIN_TIME = "beginTime";
    private static final String KEY_VIEW_TYPE = "VIEW";
    private static final String MONTH_VIEW = "MONTH";
    private static final String TAG = "ShowHandleResultActivity";
    private long mEventDtStart;

    @Override
    public void onCreate(Bundle bundle) {
        super.onCreate(bundle);
        LogUtils.d(TAG, "onCreate.");
        requestWindowFeature(1);
        setContentView(R.layout.import_dialog);
        int themeMainColor = Utils.getThemeMainColor(this, 17170450);
        if (themeMainColor != 17170450) {
            ((TextView) findViewById(R.id.result_title)).setTextColor(themeMainColor);
            findViewById(R.id.result_divide_line).setBackgroundColor(themeMainColor);
        }
        ((ProgressBar) findViewById(R.id.import_progress)).setProgress(100);
        TextView textView = (TextView) findViewById(R.id.import_tip);
        Intent intent = getIntent();
        this.mEventDtStart = intent.getLongExtra("eventStartTime", System.currentTimeMillis());
        int intExtra = intent.getIntExtra("SucceedCnt", 0);
        int intExtra2 = intent.getIntExtra("totalCnt", 0);
        Button button = (Button) findViewById(R.id.button_open);
        if (intExtra <= 0 || intExtra < intExtra2) {
            textView.setText(R.string.import_vcs_failed);
            button.setEnabled(false);
        } else {
            textView.setText(R.string.import_complete);
            button.setEnabled(true);
        }
        button.setVisibility(0);
        findViewById(R.id.button_done).setVisibility(0);
        findViewById(R.id.button_open).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent2 = new Intent("android.intent.action.VIEW");
                intent2.setDataAndType(CalendarContract.CONTENT_URI, "time/epoch");
                intent2.setFlags(268435456);
                Bundle bundle2 = new Bundle();
                bundle2.putString(ShowHandleResultActivity.KEY_VIEW_TYPE, ShowHandleResultActivity.MONTH_VIEW);
                intent2.putExtra(ShowHandleResultActivity.EXTRA_BEGIN_TIME, ShowHandleResultActivity.this.mEventDtStart);
                intent2.putExtras(bundle2);
                try {
                    ShowHandleResultActivity.this.startActivity(intent2);
                } catch (ActivityNotFoundException e) {
                    Toast.makeText(ShowHandleResultActivity.this, R.string.open_calendar_failed, 1).show();
                    LogUtils.e(ShowHandleResultActivity.TAG, "Start Activity failed! Maybe the Calendar App is closed.Exception:" + e);
                }
                ShowHandleResultActivity.this.finish();
            }
        });
        findViewById(R.id.button_done).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                ShowHandleResultActivity.this.finish();
            }
        });
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        this.mEventDtStart = intent.getLongExtra("eventStartTime", System.currentTimeMillis());
    }
}
