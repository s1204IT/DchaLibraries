package com.android.calendar.alerts;

import android.app.ListActivity;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Toast;
import com.android.calendar.R;
import com.android.calendar.Utils;
import java.util.Arrays;

public class QuickResponseActivity extends ListActivity implements AdapterView.OnItemClickListener {
    static long mEventId;
    private String[] mResponses = null;

    @Override
    protected void onCreate(Bundle icicle) {
        super.onCreate(icicle);
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
        String[] responses = Utils.getQuickResponses(this);
        Arrays.sort(responses);
        this.mResponses = new String[responses.length + 1];
        int i = 0;
        while (i < responses.length) {
            this.mResponses[i] = responses[i];
            i++;
        }
        this.mResponses[i] = getResources().getString(R.string.quick_response_custom_msg);
        setListAdapter(new ArrayAdapter(this, R.layout.quick_response_item, this.mResponses));
    }

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        String body = null;
        if (this.mResponses != null && position < this.mResponses.length - 1) {
            body = this.mResponses[position];
        }
        new QueryThread(mEventId, body).start();
    }

    private class QueryThread extends Thread {
        String mBody;
        long mEventId;

        QueryThread(long eventId, String body) {
            this.mEventId = eventId;
            this.mBody = body;
        }

        @Override
        public void run() {
            Intent emailIntent = AlertReceiver.createEmailIntent(QuickResponseActivity.this, this.mEventId, this.mBody);
            if (emailIntent != null) {
                try {
                    QuickResponseActivity.this.startActivity(emailIntent);
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
