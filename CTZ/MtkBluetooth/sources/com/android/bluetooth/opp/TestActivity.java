package com.android.bluetooth.opp;

import android.app.Activity;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import com.android.bluetooth.R;

public class TestActivity extends Activity {
    public String currentInsert;
    EditText mAckView;
    EditText mAddressView;
    EditText mDeleteView;
    EditText mInsertView;
    EditText mMediaView;
    TestTcpServer mServer;
    EditText mUpdateView;
    public int mCurrentByte = 0;
    public View.OnClickListener insertRecordListener = new View.OnClickListener() {
        @Override
        public void onClick(View view) {
            String string;
            Integer numValueOf;
            Integer numValueOf2 = null;
            if (TestActivity.this.mAddressView.getText().length() != 0) {
                string = TestActivity.this.mAddressView.getText().toString();
                Log.v(Constants.TAG, "Send to address  " + string);
            } else {
                string = null;
            }
            if (string == null) {
                string = "00:17:83:58:5D:CC";
            }
            if (TestActivity.this.mMediaView.getText().length() != 0) {
                numValueOf = Integer.valueOf(Integer.parseInt(TestActivity.this.mMediaView.getText().toString().trim()));
                Log.v(Constants.TAG, "Send media no.  " + numValueOf);
            } else {
                numValueOf = null;
            }
            if (numValueOf == null) {
                numValueOf = 1;
            }
            ContentValues contentValues = new ContentValues();
            contentValues.put("uri", "content://media/external/images/media/" + numValueOf);
            contentValues.put(BluetoothShare.DESTINATION, string);
            contentValues.put(BluetoothShare.DIRECTION, (Integer) 0);
            contentValues.put("timestamp", Long.valueOf(System.currentTimeMillis()));
            if (TestActivity.this.mInsertView.getText().length() != 0) {
                numValueOf2 = Integer.valueOf(Integer.parseInt(TestActivity.this.mInsertView.getText().toString().trim()));
                Log.v(Constants.TAG, "parseInt  " + numValueOf2);
            }
            if (numValueOf2 == null) {
                numValueOf2 = 1;
            }
            for (int i = 0; i < numValueOf2.intValue(); i++) {
                Uri uriInsert = TestActivity.this.getContentResolver().insert(BluetoothShare.CONTENT_URI, contentValues);
                Log.v(Constants.TAG, "insert contentUri: " + uriInsert);
                TestActivity.this.currentInsert = uriInsert.getPathSegments().get(1);
                Log.v(Constants.TAG, "currentInsert = " + TestActivity.this.currentInsert);
            }
        }
    };
    public View.OnClickListener deleteRecordListener = new View.OnClickListener() {
        @Override
        public void onClick(View view) {
            TestActivity.this.getContentResolver().delete(Uri.parse(BluetoothShare.CONTENT_URI + "/" + TestActivity.this.mDeleteView.getText().toString()), null, null);
        }
    };
    public View.OnClickListener updateRecordListener = new View.OnClickListener() {
        @Override
        public void onClick(View view) {
            Uri uri = Uri.parse(BluetoothShare.CONTENT_URI + "/" + TestActivity.this.mUpdateView.getText().toString());
            ContentValues contentValues = new ContentValues();
            contentValues.put("confirm", (Integer) 1);
            TestActivity.this.getContentResolver().update(uri, contentValues, null, null);
        }
    };
    public View.OnClickListener ackRecordListener = new View.OnClickListener() {
        @Override
        public void onClick(View view) {
            Uri uri = Uri.parse(BluetoothShare.CONTENT_URI + "/" + TestActivity.this.mAckView.getText().toString());
            ContentValues contentValues = new ContentValues();
            contentValues.put(BluetoothShare.VISIBILITY, (Integer) 1);
            TestActivity.this.getContentResolver().update(uri, contentValues, null, null);
        }
    };
    public View.OnClickListener deleteAllRecordListener = new View.OnClickListener() {
        @Override
        public void onClick(View view) {
            TestActivity.this.getContentResolver().delete(Uri.parse(String.valueOf(BluetoothShare.CONTENT_URI)), null, null);
        }
    };
    public View.OnClickListener startTcpServerListener = new View.OnClickListener() {
        @Override
        public void onClick(View view) {
            TestActivity.this.mServer = new TestTcpServer();
            new Thread(TestActivity.this.mServer).start();
        }
    };
    public View.OnClickListener notifyTcpServerListener = new View.OnClickListener() {
        @Override
        public void onClick(View view) {
            new Thread() {
                @Override
                public void run() {
                    synchronized (TestActivity.this.mServer) {
                        TestActivity.this.mServer.a = true;
                        TestActivity.this.mServer.notify();
                    }
                }
            }.start();
        }
    };

    @Override
    public void onCreate(Bundle bundle) {
        super.onCreate(bundle);
        Intent intent = getIntent();
        String action = intent.getAction();
        Context baseContext = getBaseContext();
        if ("android.intent.action.SEND".equals(action)) {
            String type = intent.getType();
            Uri uri = (Uri) intent.getParcelableExtra("android.intent.extra.STREAM");
            if (uri != null && type != null) {
                Log.v(Constants.TAG, " Get share intent with Uri " + uri + " mimetype is " + type);
                baseContext.getContentResolver().query(uri, null, null, null, null).close();
            }
        }
        setContentView(R.layout.testactivity_main);
        Button button = (Button) findViewById(R.id.insert_record);
        Button button2 = (Button) findViewById(R.id.delete_record);
        Button button3 = (Button) findViewById(R.id.update_record);
        Button button4 = (Button) findViewById(R.id.ack_record);
        Button button5 = (Button) findViewById(R.id.deleteAll_record);
        this.mUpdateView = (EditText) findViewById(R.id.update_text);
        this.mAckView = (EditText) findViewById(R.id.ack_text);
        this.mDeleteView = (EditText) findViewById(R.id.delete_text);
        this.mInsertView = (EditText) findViewById(R.id.insert_text);
        this.mAddressView = (EditText) findViewById(R.id.address_text);
        this.mMediaView = (EditText) findViewById(R.id.media_text);
        button.setOnClickListener(this.insertRecordListener);
        button2.setOnClickListener(this.deleteRecordListener);
        button3.setOnClickListener(this.updateRecordListener);
        button4.setOnClickListener(this.ackRecordListener);
        button5.setOnClickListener(this.deleteAllRecordListener);
        ((Button) findViewById(R.id.start_server)).setOnClickListener(this.startTcpServerListener);
        ((Button) findViewById(R.id.notify_server)).setOnClickListener(this.notifyTcpServerListener);
    }
}
