package jp.co.benesse.dcha.setupwizard;

import android.content.BroadcastReceiver;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.text.TextUtils;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import jp.co.benesse.dcha.util.Logger;

public class TabletIntroductionSettingActivity extends ParentSettingActivity implements View.OnClickListener {
    private static final String ACTION_DATABOX_COMMAND = "jp.co.benesse.dcha.databox.intent.action.COMMAND";
    private static final String ACTION_DATABOX_IMPORTED_ENVIRONMENT_EVENT = "jp.co.benesse.dcha.databox.intent.action.IMPORTED_ENVIRONMENT_EVENT";
    private static final String CATEGORIES_DATABOX_IMPORT_ENVIRONMENT = "jp.co.benesse.dcha.databox.intent.category.IMPORT_ENVIRONMENT";
    private static final String COLUMN_KVS_SELECTION = "key=?";
    private static final String COLUMN_KVS_VALUE = "value";
    private static final String KEY_ENVIRONMENT = "environment";
    private static final String KEY_VERSION = "version";
    private static final String TAG = TabletIntroductionSettingActivity.class.getSimpleName();
    private static final Uri URI_TEST_ENVIRONMENT_INFO = Uri.parse("content://jp.co.benesse.dcha.databox.db.KvsProvider/kvs/test.environment.info");
    protected ImageView mStartBtn;
    protected Handler mHandler = new Handler();
    protected TextView mTestEnvironmentText = null;
    public BroadcastReceiver mImportedEnvironmentReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            Logger.d(TabletIntroductionSettingActivity.TAG, "onReceive 0001");
            if (TabletIntroductionSettingActivity.this.mTestEnvironmentText != null) {
                Logger.d(TabletIntroductionSettingActivity.TAG, "onReceive 0002");
                TabletIntroductionSettingActivity.this.mTestEnvironmentText.setText("");
                String environment = TabletIntroductionSettingActivity.this.getKvsValue(context, TabletIntroductionSettingActivity.URI_TEST_ENVIRONMENT_INFO, TabletIntroductionSettingActivity.KEY_ENVIRONMENT, null);
                String version = TabletIntroductionSettingActivity.this.getKvsValue(context, TabletIntroductionSettingActivity.URI_TEST_ENVIRONMENT_INFO, TabletIntroductionSettingActivity.KEY_VERSION, null);
                if (!TextUtils.isEmpty(environment) && !TextUtils.isEmpty(version)) {
                    Logger.d(TabletIntroductionSettingActivity.TAG, "onReceive 0003");
                    TabletIntroductionSettingActivity.this.mTestEnvironmentText.setText(TabletIntroductionSettingActivity.this.getString(R.string.test_environment_format, new Object[]{environment, version}));
                }
            }
            Logger.d(TabletIntroductionSettingActivity.TAG, "onReceive 0004");
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        Logger.d(TAG, "onCreate 0001");
        super.onCreate(savedInstanceState);
        setContentView(R.layout.act_tablet_introduction);
        this.mStartBtn = (ImageView) findViewById(R.id.start_btn);
        this.mTestEnvironmentText = (TextView) findViewById(R.id.test_environment_text);
        setFont(this.mTestEnvironmentText);
        this.mHandler.postDelayed(new Runnable() {
            @Override
            public void run() {
                if (TabletIntroductionSettingActivity.this.mStartBtn != null) {
                    TabletIntroductionSettingActivity.this.mStartBtn.setOnClickListener(TabletIntroductionSettingActivity.this);
                }
            }
        }, 750L);
        Logger.d(TAG, "onCreate 0002");
    }

    @Override
    protected void onStart() {
        Logger.d(TAG, "onStart 0001");
        super.onStart();
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(ACTION_DATABOX_IMPORTED_ENVIRONMENT_EVENT);
        registerReceiver(this.mImportedEnvironmentReceiver, intentFilter);
        try {
            ContentResolver cr = getContentResolver();
            cr.delete(URI_TEST_ENVIRONMENT_INFO, null, null);
        } catch (Exception e) {
            Logger.d(TAG, "onStart 0002", e);
        }
        Intent intent = new Intent();
        intent.setAction(ACTION_DATABOX_COMMAND);
        intent.addCategory(CATEGORIES_DATABOX_IMPORT_ENVIRONMENT);
        sendBroadcast(intent);
        Logger.d(TAG, "onStart 0003");
    }

    @Override
    protected void onStop() {
        Logger.d(TAG, "onStop 0001");
        super.onStop();
        if (this.mImportedEnvironmentReceiver != null) {
            Logger.d(TAG, "onStop 0002");
            unregisterReceiver(this.mImportedEnvironmentReceiver);
        }
        Logger.d(TAG, "onStop 0003");
    }

    @Override
    protected void onDestroy() {
        Logger.d(TAG, "onDestroy 0001");
        super.onDestroy();
        this.mStartBtn.setOnClickListener(null);
        this.mStartBtn = null;
        this.mTestEnvironmentText = null;
        this.mHandler = null;
        Logger.d(TAG, "onDestroy 0002");
    }

    @Override
    public void onClick(View v) {
        Logger.d(TAG, "onClick 0001");
        this.mStartBtn.setClickable(false);
        Intent intent = new Intent();
        intent.setClassName(ParentSettingActivity.PACKAGE_SYSTEM_SETTING, ParentSettingActivity.CLASS_SYSTEM_WIFI_SETTING);
        intent.putExtra(ParentSettingActivity.FIRST_FLG, true);
        startActivity(intent);
        finish();
        Logger.d(TAG, "onClick 0002");
    }

    protected String getKvsValue(Context context, Uri uri, String key, String defaultValue) {
        String value = defaultValue;
        if (context != null) {
            String[] projection = {COLUMN_KVS_VALUE};
            String[] selectionArgs = {key};
            Cursor cursor = null;
            try {
                ContentResolver cr = context.getContentResolver();
                cursor = cr.query(uri, projection, COLUMN_KVS_SELECTION, selectionArgs, null);
                if (cursor != null && cursor.moveToFirst()) {
                    value = cursor.getString(cursor.getColumnIndex(COLUMN_KVS_VALUE));
                }
                if (cursor != null) {
                    cursor.close();
                }
            } catch (Exception e) {
                if (cursor != null) {
                    cursor.close();
                }
            } catch (Throwable th) {
                if (cursor != null) {
                    cursor.close();
                }
                throw th;
            }
        }
        return value;
    }
}
