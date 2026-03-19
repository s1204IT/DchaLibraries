package com.mediatek.calendar.nfc;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.nfc.NdefMessage;
import android.nfc.NdefRecord;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.provider.CalendarContract;
import android.util.Log;
import android.widget.Toast;
import com.android.calendar.R;
import com.mediatek.vcalendar.VCalParser;
import com.mediatek.vcalendar.VCalStatusChangeOperator;

public class NfcImportVCalActivity extends Activity implements VCalStatusChangeOperator {
    private static final String[] CALENDAR_PERMISSION = {"android.permission.READ_CALENDAR", "android.permission.WRITE_CALENDAR"};
    private Handler mImportHandler = null;
    private NdefRecord mRecord;

    private Handler getsaveContactHandler() {
        if (this.mImportHandler == null) {
            HandlerThread handlerThread = new HandlerThread("importVCalendar");
            handlerThread.start();
            this.mImportHandler = new Handler(handlerThread.getLooper());
        }
        return this.mImportHandler;
    }

    @Override
    protected void onCreate(Bundle bundle) {
        super.onCreate(bundle);
        Log.i("NfcImportVCalActivity", "NfcImportVCalActivity, onCreate.");
        Intent intent = getIntent();
        if (!"android.nfc.action.NDEF_DISCOVERED".equals(intent.getAction())) {
            Log.w("NfcImportVCalActivity", "Unknowon intent " + intent);
            finish();
            return;
        }
        if (!hasRequiredPermission(CALENDAR_PERMISSION)) {
            Toast.makeText(getApplicationContext(), getResources().getString(R.string.denied_required_permission), 1).show();
            finish();
            return;
        }
        String type = intent.getType();
        if (type == null || (!"text/x-vcalendar".equalsIgnoreCase(type) && !"text/calendar".equalsIgnoreCase(type))) {
            Log.w("NfcImportVCalActivity", "Not a vcalendar!");
            finish();
        } else {
            if (intent.getParcelableArrayExtra("android.nfc.extra.NDEF_MESSAGES") != null) {
                this.mRecord = ((NdefMessage) intent.getParcelableArrayExtra("android.nfc.extra.NDEF_MESSAGES")[0]).getRecords()[0];
            }
            doImportAction(this);
        }
    }

    protected boolean hasRequiredPermission(String[] strArr) {
        for (String str : strArr) {
            if (checkSelfPermission(str) != 0) {
                return false;
            }
        }
        return true;
    }

    private void doImportAction(VCalStatusChangeOperator vCalStatusChangeOperator) {
        Log.i("NfcImportVCalActivity", "In doImportAction ");
        String str = new String(this.mRecord.getPayload());
        Handler handler = getsaveContactHandler();
        if (handler != null) {
            handler.post(new ImporterThread(getApplicationContext(), str, vCalStatusChangeOperator));
        }
    }

    private static class ImporterThread extends Thread {
        private Context mContext;
        private VCalStatusChangeOperator mListener;
        private VCalParser mParser;
        private String mVcsContent;

        public ImporterThread(Context context, String str, VCalStatusChangeOperator vCalStatusChangeOperator) {
            this.mContext = context;
            this.mVcsContent = str;
            this.mListener = vCalStatusChangeOperator;
        }

        @Override
        public void run() {
            this.mParser = new VCalParser(this.mVcsContent, this.mContext, this.mListener);
            this.mParser.startParseVcsContent();
        }
    }

    @Override
    public void vCalOperationExceptionOccured(int i, int i2, int i3) {
        Log.v("NfcImportVCalActivity", "vCalOperationExceptionOccured");
    }

    @Override
    public void vCalOperationFinished(int i, int i2, Object obj) {
        long j;
        long j2;
        Uri uriWithAppendedPath;
        Log.v("NfcImportVCalActivity", "vCalOperationFinished, obj=" + obj);
        long jCurrentTimeMillis = System.currentTimeMillis();
        if (obj != null) {
            Bundle bundle = (Bundle) obj;
            long j3 = bundle.getLong("key_event_id");
            Log.d("NfcImportVCalActivity", "vCalOperationFinished, eventId=" + j3);
            long j4 = bundle.getLong("key_start_millis");
            j2 = bundle.getLong("key_end_millis");
            j = j4;
            uriWithAppendedPath = Uri.withAppendedPath(CalendarContract.Events.CONTENT_URI, String.valueOf(j3));
        } else {
            j = jCurrentTimeMillis;
            j2 = j;
            uriWithAppendedPath = null;
        }
        Log.v("NfcImportVCalActivity", "vCalOperationFinished, timeMillis=" + j);
        Log.v("NfcImportVCalActivity", "vCalOperationFinished, endMills=" + j2);
        startActivity(createViewEventIntent(uriWithAppendedPath, j, j2));
        finish();
    }

    public Intent createViewEventIntent(Uri uri, long j, long j2) {
        Intent intent = new Intent("android.intent.action.VIEW");
        intent.setDataAndType(uri, "vnd.android.cursor.item/event");
        intent.putExtra("beginTime", j);
        intent.putExtra("endTime", j2);
        return intent;
    }
}
