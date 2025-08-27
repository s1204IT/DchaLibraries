package com.android.settings;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.PowerManager;
import android.os.SystemClock;
import android.os.UserHandle;
import android.os.UserManager;
import android.provider.Settings;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AnimationUtils;
import java.util.Objects;

/* loaded from: classes.dex */
public class FallbackHome extends Activity {
    private boolean mProvisioned;
    private final Runnable mProgressTimeoutRunnable = new Runnable() { // from class: com.android.settings.-$$Lambda$FallbackHome$t1fq3k7x_PY-DiX5Fz-YbaIlCdg
        @Override // java.lang.Runnable
        public final void run() {
            FallbackHome.lambda$new$0(this.f$0);
        }
    };
    private BroadcastReceiver mReceiver = new BroadcastReceiver() { // from class: com.android.settings.FallbackHome.1
        @Override // android.content.BroadcastReceiver
        public void onReceive(Context context, Intent intent) {
            FallbackHome.this.maybeFinish();
        }
    };
    private Handler mHandler = new Handler() { // from class: com.android.settings.FallbackHome.2
        @Override // android.os.Handler
        public void handleMessage(Message message) {
            FallbackHome.this.maybeFinish();
        }
    };

    public static /* synthetic */ void lambda$new$0(FallbackHome fallbackHome) {
        View viewInflate = fallbackHome.getLayoutInflater().inflate(R.layout.fallback_home_finishing_boot, (ViewGroup) null);
        fallbackHome.setContentView(viewInflate);
        viewInflate.setAlpha(0.0f);
        viewInflate.animate().alpha(1.0f).setDuration(500L).setInterpolator(AnimationUtils.loadInterpolator(fallbackHome, android.R.interpolator.fast_out_slow_in)).start();
        fallbackHome.getWindow().addFlags(128);
    }

    @Override // android.app.Activity
    protected void onCreate(Bundle bundle) {
        super.onCreate(bundle);
        this.mProvisioned = Settings.Global.getInt(getContentResolver(), "device_provisioned", 0) != 0;
        if (!this.mProvisioned) {
            setTheme(R.style.FallbackHome_SetupWizard);
            getWindow().getDecorView().setSystemUiVisibility(4102);
        } else {
            getWindow().getDecorView().setSystemUiVisibility(1536);
        }
        registerReceiver(this.mReceiver, new IntentFilter("android.intent.action.USER_UNLOCKED"));
        maybeFinish();
    }

    @Override // android.app.Activity
    protected void onResume() {
        super.onResume();
        if (this.mProvisioned) {
            this.mHandler.postDelayed(this.mProgressTimeoutRunnable, 2000L);
        }
    }

    @Override // android.app.Activity
    protected void onPause() {
        super.onPause();
        this.mHandler.removeCallbacks(this.mProgressTimeoutRunnable);
    }

    @Override // android.app.Activity
    protected void onDestroy() {
        super.onDestroy();
        unregisterReceiver(this.mReceiver);
    }

    private void maybeFinish() {
        if (((UserManager) getSystemService(UserManager.class)).isUserUnlocked()) {
            if (Objects.equals(getPackageName(), getPackageManager().resolveActivity(new Intent("android.intent.action.MAIN").addCategory("android.intent.category.HOME"), 0).activityInfo.packageName)) {
                if (UserManager.isSplitSystemUser() && UserHandle.myUserId() == 0) {
                    return;
                }
                Log.d("FallbackHome", "User unlocked but no home; let's hope someone enables one soon?");
                this.mHandler.sendEmptyMessageDelayed(0, 500L);
                return;
            }
            Log.d("FallbackHome", "User unlocked and real home found; let's go!");
            ((PowerManager) getSystemService(PowerManager.class)).userActivity(SystemClock.uptimeMillis(), false);
            finish();
        }
    }
}
