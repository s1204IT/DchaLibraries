package com.android.bluetooth.opp;

import android.app.Activity;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
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
    EditText mUpdateView;
    TestTcpServer server;
    public int mCurrentByte = 0;
    public View.OnClickListener insertRecordListener = new View.OnClickListener() {
        @Override
        public void onClick(View view) {
            String address = null;
            if (TestActivity.this.mAddressView.getText().length() != 0) {
                address = TestActivity.this.mAddressView.getText().toString();
                Log.v(Constants.TAG, "Send to address  " + address);
            }
            if (address == null) {
                address = "00:17:83:58:5D:CC";
            }
            Integer media = null;
            if (TestActivity.this.mMediaView.getText().length() != 0) {
                media = Integer.valueOf(Integer.parseInt(TestActivity.this.mMediaView.getText().toString().trim()));
                Log.v(Constants.TAG, "Send media no.  " + media);
            }
            if (media == null) {
                media = 1;
            }
            ContentValues values = new ContentValues();
            values.put("uri", "content://media/external/images/media/" + media);
            values.put(BluetoothShare.DESTINATION, address);
            values.put(BluetoothShare.DIRECTION, (Integer) 0);
            Long ts = Long.valueOf(System.currentTimeMillis());
            values.put("timestamp", ts);
            Integer records = null;
            if (TestActivity.this.mInsertView.getText().length() != 0) {
                records = Integer.valueOf(Integer.parseInt(TestActivity.this.mInsertView.getText().toString().trim()));
                Log.v(Constants.TAG, "parseInt  " + records);
            }
            if (records == null) {
                records = 1;
            }
            for (int i = 0; i < records.intValue(); i++) {
                Uri contentUri = TestActivity.this.getContentResolver().insert(BluetoothShare.CONTENT_URI, values);
                Log.v(Constants.TAG, "insert contentUri: " + contentUri);
                TestActivity.this.currentInsert = contentUri.getPathSegments().get(1);
                Log.v(Constants.TAG, "currentInsert = " + TestActivity.this.currentInsert);
            }
        }
    };
    public View.OnClickListener deleteRecordListener = new View.OnClickListener() {
        @Override
        public void onClick(View view) {
            Uri contentUri = Uri.parse(BluetoothShare.CONTENT_URI + "/" + TestActivity.this.mDeleteView.getText().toString());
            TestActivity.this.getContentResolver().delete(contentUri, null, null);
        }
    };
    public View.OnClickListener updateRecordListener = new View.OnClickListener() {
        @Override
        public void onClick(View view) {
            Uri contentUri = Uri.parse(BluetoothShare.CONTENT_URI + "/" + TestActivity.this.mUpdateView.getText().toString());
            ContentValues updateValues = new ContentValues();
            updateValues.put(BluetoothShare.USER_CONFIRMATION, (Integer) 1);
            TestActivity.this.getContentResolver().update(contentUri, updateValues, null, null);
        }
    };
    public View.OnClickListener ackRecordListener = new View.OnClickListener() {
        @Override
        public void onClick(View view) {
            Uri contentUri = Uri.parse(BluetoothShare.CONTENT_URI + "/" + TestActivity.this.mAckView.getText().toString());
            ContentValues updateValues = new ContentValues();
            updateValues.put(BluetoothShare.VISIBILITY, (Integer) 1);
            TestActivity.this.getContentResolver().update(contentUri, updateValues, null, null);
        }
    };
    public View.OnClickListener deleteAllRecordListener = new View.OnClickListener() {
        @Override
        public void onClick(View view) {
            Uri contentUri = Uri.parse(BluetoothShare.CONTENT_URI + "");
            TestActivity.this.getContentResolver().delete(contentUri, null, null);
        }
    };
    public View.OnClickListener startTcpServerListener = new View.OnClickListener() {
        @Override
        public void onClick(View view) {
            TestActivity.this.server = new TestTcpServer();
            Thread server_thread = new Thread(TestActivity.this.server);
            server_thread.start();
        }
    };
    public View.OnClickListener notifyTcpServerListener = new View.OnClickListener() {
        @Override
        public void onClick(View view) {
            Thread notifyThread = new Thread() {
                @Override
                public void run() {
                    synchronized (TestActivity.this.server) {
                        TestActivity.this.server.a = true;
                        TestActivity.this.server.notify();
                    }
                }
            };
            notifyThread.start();
        }
    };

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Intent intent = getIntent();
        String action = intent.getAction();
        Context c = getBaseContext();
        if ("android.intent.action.SEND".equals(action)) {
            String type = intent.getType();
            Uri stream = (Uri) intent.getParcelableExtra("android.intent.extra.STREAM");
            if (stream != null && type != null) {
                Log.v(Constants.TAG, " Get share intent with Uri " + stream + " mimetype is " + type);
                Cursor cursor = c.getContentResolver().query(stream, null, null, null, null);
                cursor.close();
            }
        }
        setContentView(R.layout.testactivity_main);
        Button mInsertRecord = (Button) findViewById(R.id.insert_record);
        Button mDeleteRecord = (Button) findViewById(R.id.delete_record);
        Button mUpdateRecord = (Button) findViewById(R.id.update_record);
        Button mAckRecord = (Button) findViewById(R.id.ack_record);
        Button mDeleteAllRecord = (Button) findViewById(R.id.deleteAll_record);
        this.mUpdateView = (EditText) findViewById(R.id.update_text);
        this.mAckView = (EditText) findViewById(R.id.ack_text);
        this.mDeleteView = (EditText) findViewById(R.id.delete_text);
        this.mInsertView = (EditText) findViewById(R.id.insert_text);
        this.mAddressView = (EditText) findViewById(R.id.address_text);
        this.mMediaView = (EditText) findViewById(R.id.media_text);
        mInsertRecord.setOnClickListener(this.insertRecordListener);
        mDeleteRecord.setOnClickListener(this.deleteRecordListener);
        mUpdateRecord.setOnClickListener(this.updateRecordListener);
        mAckRecord.setOnClickListener(this.ackRecordListener);
        mDeleteAllRecord.setOnClickListener(this.deleteAllRecordListener);
        Button mStartTcpServer = (Button) findViewById(R.id.start_server);
        mStartTcpServer.setOnClickListener(this.startTcpServerListener);
        Button mNotifyTcpServer = (Button) findViewById(R.id.notify_server);
        mNotifyTcpServer.setOnClickListener(this.notifyTcpServerListener);
    }
}
