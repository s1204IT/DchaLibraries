package com.android.calendar.alerts;

import android.app.ListActivity;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Toast;
import com.android.calendar.CalendarController;
import com.android.calendar.R;
import com.android.calendar.Utils;
import java.util.Arrays;

public class QuickResponseActivity extends ListActivity implements AdapterView.OnItemClickListener {
    static long mEventId;
    private String[] mResponses = null;

    @Override
    protected void onCreate(Bundle bundle) {
        super.onCreate(bundle);
        Intent intent = getIntent();
        if (intent == null) {
            finish();
            return;
        }
        mEventId = intent.getLongExtra("eventId", -1L);
        if (mEventId == -1) {
            finish();
            return;
        }
        getListView().setOnItemClickListener(this);
        String[] quickResponses = Utils.getQuickResponses(this);
        Arrays.sort(quickResponses);
        this.mResponses = new String[quickResponses.length + 1];
        int i = 0;
        while (i < quickResponses.length) {
            this.mResponses[i] = quickResponses[i];
            i++;
        }
        this.mResponses[i] = getResources().getString(R.string.quick_response_custom_msg);
        setListAdapter(new ArrayAdapter(this, R.layout.quick_response_item, this.mResponses));
    }

    @Override
    public void onItemClick(AdapterView<?> adapterView, View view, int i, long j) {
        String str;
        if (this.mResponses != null && i < this.mResponses.length - 1) {
            str = this.mResponses[i];
        } else {
            str = null;
        }
        new QueryThread(mEventId, str).start();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        CalendarController.removeInstance(this);
    }

    private class QueryThread extends Thread {
        String mBody;
        long mEventId;

        QueryThread(long j, String str) {
            this.mEventId = j;
            this.mBody = str;
        }

        @Override
        public void run() {
            Intent intentCreateEmailIntent = AlertReceiver.createEmailIntent(QuickResponseActivity.this, this.mEventId, this.mBody);
            if (intentCreateEmailIntent != null) {
                try {
                    QuickResponseActivity.this.startActivity(intentCreateEmailIntent);
                    QuickResponseActivity.this.finish();
                } catch (ActivityNotFoundException e) {
                    QuickResponseActivity.this.getListView().post(new Runnable() {
                        @Override
                        public void run() {
                            Toast.makeText(QuickResponseActivity.this, R.string.quick_response_email_failed, 1);
                            QuickResponseActivity.this.finish();
                        }
                    });
                }
            }
        }
    }
}
