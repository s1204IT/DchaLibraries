package com.android.deskclock;

import android.app.Activity;
import android.app.ListActivity;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Parcelable;
import android.view.View;
import android.widget.Button;
import android.widget.ListView;
import com.android.deskclock.provider.Alarm;
import com.android.deskclock.widget.selector.AlarmSelection;
import com.android.deskclock.widget.selector.AlarmSelectionAdapter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class AlarmSelectionActivity extends ListActivity {
    public static final int ACTION_DISMISS = 0;
    private static final int ACTION_INVALID = -1;
    public static final String EXTRA_ACTION = "com.android.deskclock.EXTRA_ACTION";
    public static final String EXTRA_ALARMS = "com.android.deskclock.EXTRA_ALARMS";
    private int mAction;
    private final List<AlarmSelection> mSelections = new ArrayList();

    @Override
    protected void onCreate(Bundle bundle) {
        super.onCreate(bundle);
        setContentView(R.layout.selection_layout);
        ((Button) findViewById(R.id.cancel_button)).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                AlarmSelectionActivity.this.finish();
            }
        });
        Intent intent = getIntent();
        Parcelable[] parcelableArrayExtra = intent.getParcelableArrayExtra(EXTRA_ALARMS);
        this.mAction = intent.getIntExtra(EXTRA_ACTION, -1);
        for (Parcelable parcelable : parcelableArrayExtra) {
            Alarm alarm = (Alarm) parcelable;
            this.mSelections.add(new AlarmSelection(String.format(Locale.US, "%d %02d", Integer.valueOf(alarm.hour), Integer.valueOf(alarm.minutes)), alarm));
        }
        setListAdapter(new AlarmSelectionAdapter(this, R.layout.alarm_row, this.mSelections));
    }

    @Override
    public void onListItemClick(ListView listView, View view, int i, long j) {
        super.onListItemClick(listView, view, i, j);
        Alarm alarm = this.mSelections.get((int) j).getAlarm();
        if (alarm != null) {
            new ProcessAlarmActionAsync(alarm, this, this.mAction).execute(new Void[0]);
        }
        finish();
    }

    private static class ProcessAlarmActionAsync extends AsyncTask<Void, Void, Void> {
        private final int mAction;
        private final Activity mActivity;
        private final Alarm mAlarm;

        public ProcessAlarmActionAsync(Alarm alarm, Activity activity, int i) {
            this.mAlarm = alarm;
            this.mActivity = activity;
            this.mAction = i;
        }

        @Override
        protected Void doInBackground(Void... voidArr) {
            switch (this.mAction) {
                case -1:
                    LogUtils.i("Invalid action", new Object[0]);
                    break;
                case 0:
                    HandleApiCalls.dismissAlarm(this.mAlarm, this.mActivity);
                    break;
            }
            return null;
        }
    }
}
