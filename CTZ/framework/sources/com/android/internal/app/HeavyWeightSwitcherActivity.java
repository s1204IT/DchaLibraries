package com.android.internal.app;

import android.app.Activity;
import android.app.ActivityManager;
import android.content.IntentSender;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.RemoteException;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import com.android.internal.R;

public class HeavyWeightSwitcherActivity extends Activity {
    public static final String KEY_CUR_APP = "cur_app";
    public static final String KEY_CUR_TASK = "cur_task";
    public static final String KEY_HAS_RESULT = "has_result";
    public static final String KEY_INTENT = "intent";
    public static final String KEY_NEW_APP = "new_app";
    String mCurApp;
    int mCurTask;
    boolean mHasResult;
    String mNewApp;
    IntentSender mStartIntent;
    private View.OnClickListener mSwitchOldListener = new View.OnClickListener() {
        @Override
        public void onClick(View view) {
            try {
                ActivityManager.getService().moveTaskToFront(HeavyWeightSwitcherActivity.this.mCurTask, 0, null);
            } catch (RemoteException e) {
            }
            HeavyWeightSwitcherActivity.this.finish();
        }
    };
    private View.OnClickListener mSwitchNewListener = new View.OnClickListener() {
        @Override
        public void onClick(View view) {
            try {
                ActivityManager.getService().finishHeavyWeightApp();
            } catch (RemoteException e) {
            }
            try {
                if (HeavyWeightSwitcherActivity.this.mHasResult) {
                    HeavyWeightSwitcherActivity.this.startIntentSenderForResult(HeavyWeightSwitcherActivity.this.mStartIntent, -1, null, 33554432, 33554432, 0);
                } else {
                    HeavyWeightSwitcherActivity.this.startIntentSenderForResult(HeavyWeightSwitcherActivity.this.mStartIntent, -1, null, 0, 0, 0);
                }
            } catch (IntentSender.SendIntentException e2) {
                Log.w("HeavyWeightSwitcherActivity", "Failure starting", e2);
            }
            HeavyWeightSwitcherActivity.this.finish();
        }
    };

    @Override
    protected void onCreate(Bundle bundle) {
        super.onCreate(bundle);
        requestWindowFeature(1);
        this.mStartIntent = (IntentSender) getIntent().getParcelableExtra("intent");
        this.mHasResult = getIntent().getBooleanExtra(KEY_HAS_RESULT, false);
        this.mCurApp = getIntent().getStringExtra(KEY_CUR_APP);
        this.mCurTask = getIntent().getIntExtra(KEY_CUR_TASK, 0);
        this.mNewApp = getIntent().getStringExtra(KEY_NEW_APP);
        setContentView(R.layout.heavy_weight_switcher);
        setIconAndText(R.id.old_app_icon, R.id.old_app_action, 0, this.mCurApp, this.mNewApp, R.string.old_app_action, 0);
        setIconAndText(R.id.new_app_icon, R.id.new_app_action, R.id.new_app_description, this.mNewApp, this.mCurApp, R.string.new_app_action, R.string.new_app_description);
        findViewById(R.id.switch_old).setOnClickListener(this.mSwitchOldListener);
        findViewById(R.id.switch_new).setOnClickListener(this.mSwitchNewListener);
    }

    void setText(int i, CharSequence charSequence) {
        ((TextView) findViewById(i)).setText(charSequence);
    }

    void setDrawable(int i, Drawable drawable) {
        if (drawable != null) {
            ((ImageView) findViewById(i)).setImageDrawable(drawable);
        }
    }

    void setIconAndText(int i, int i2, int i3, String str, String str2, int i4, int i5) {
        CharSequence charSequenceLoadLabel;
        Drawable drawableLoadIcon = null;
        if (str != 0) {
            try {
                ApplicationInfo applicationInfo = getPackageManager().getApplicationInfo(str, 0);
                CharSequence charSequenceLoadLabel2 = applicationInfo.loadLabel(getPackageManager());
                try {
                    drawableLoadIcon = applicationInfo.loadIcon(getPackageManager());
                    str = charSequenceLoadLabel2;
                } catch (PackageManager.NameNotFoundException e) {
                    str = charSequenceLoadLabel2;
                }
            } catch (PackageManager.NameNotFoundException e2) {
            }
        }
        setDrawable(i, drawableLoadIcon);
        setText(i2, getString(i4, new Object[]{str}));
        if (i3 != 0) {
            if (str2 != null) {
                try {
                    charSequenceLoadLabel = getPackageManager().getApplicationInfo(str2, 0).loadLabel(getPackageManager());
                } catch (PackageManager.NameNotFoundException e3) {
                    charSequenceLoadLabel = str2;
                }
            } else {
                charSequenceLoadLabel = str2;
            }
            setText(i3, getString(i5, charSequenceLoadLabel));
        }
    }
}
