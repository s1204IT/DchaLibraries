package com.android.settings;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.ResolveInfo;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.UserManager;
import android.util.Log;
import java.util.Objects;

public class FallbackHome extends Activity {
    private BroadcastReceiver mReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            FallbackHome.this.maybeFinish();
        }
    };
    private Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            FallbackHome.this.maybeFinish();
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setTheme(android.R.style.Theme.Black.NoTitleBar.Fullscreen);
        registerReceiver(this.mReceiver, new IntentFilter("android.intent.action.USER_UNLOCKED"));
        maybeFinish();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        unregisterReceiver(this.mReceiver);
    }

    public void maybeFinish() {
        if (!((UserManager) getSystemService(UserManager.class)).isUserUnlocked()) {
            return;
        }
        Intent homeIntent = new Intent("android.intent.action.MAIN").addCategory("android.intent.category.HOME");
        ResolveInfo homeInfo = getPackageManager().resolveActivity(homeIntent, 0);
        if (Objects.equals(getPackageName(), homeInfo.activityInfo.packageName)) {
            Log.d("FallbackHome", "User unlocked but no home; let's hope someone enables one soon?");
            this.mHandler.sendEmptyMessageDelayed(0, 500L);
        } else {
            Log.d("FallbackHome", "User unlocked and real home found; let's go!");
            finish();
        }
    }
}
