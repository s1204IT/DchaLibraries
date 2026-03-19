package com.android.phone;

import android.app.Activity;
import android.app.FragmentManager;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.MenuItem;

public class NetworkSelectSettingActivity extends Activity {
    public static final String KEY_PHONE_ID = "phone_id";
    private static final String TAG = "NetworkSelectSettingActivity";

    public static Intent getIntent(Context context, int i) {
        Intent intent = new Intent(context, (Class<?>) NetworkSelectSettingActivity.class);
        intent.putExtra(KEY_PHONE_ID, i);
        return intent;
    }

    @Override
    public void onCreate(Bundle bundle) {
        Log.d(TAG, "onCreate()");
        super.onCreate(bundle);
        int i = getIntent().getExtras().getInt(KEY_PHONE_ID);
        setContentView(R.layout.choose_network);
        FragmentManager fragmentManager = getFragmentManager();
        if (fragmentManager.findFragmentById(R.id.choose_network_content) == null) {
            fragmentManager.beginTransaction().add(R.id.choose_network_content, NetworkSelectSetting.newInstance(i), TAG).commit();
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem menuItem) {
        if (menuItem.getItemId() == 16908332) {
            onBackPressed();
            return true;
        }
        return super.onOptionsItemSelected(menuItem);
    }
}
