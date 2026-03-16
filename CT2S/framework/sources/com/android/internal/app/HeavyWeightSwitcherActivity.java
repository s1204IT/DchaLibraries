package com.android.internal.app;

import android.app.Activity;
import android.app.ActivityManagerNative;
import android.content.IntentSender;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.graphics.drawable.Drawable;
import android.net.ProxyInfo;
import android.os.Bundle;
import android.os.RemoteException;
import android.util.Log;
import android.util.TypedValue;
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
        public void onClick(View v) {
            try {
                ActivityManagerNative.getDefault().moveTaskToFront(HeavyWeightSwitcherActivity.this.mCurTask, 0, null);
            } catch (RemoteException e) {
            }
            HeavyWeightSwitcherActivity.this.finish();
        }
    };
    private View.OnClickListener mSwitchNewListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            try {
                ActivityManagerNative.getDefault().finishHeavyWeightApp();
            } catch (RemoteException e) {
            }
            try {
                if (HeavyWeightSwitcherActivity.this.mHasResult) {
                    HeavyWeightSwitcherActivity.this.startIntentSenderForResult(HeavyWeightSwitcherActivity.this.mStartIntent, -1, null, 33554432, 33554432, 0);
                } else {
                    HeavyWeightSwitcherActivity.this.startIntentSenderForResult(HeavyWeightSwitcherActivity.this.mStartIntent, -1, null, 0, 0, 0);
                }
            } catch (IntentSender.SendIntentException ex) {
                Log.w("HeavyWeightSwitcherActivity", "Failure starting", ex);
            }
            HeavyWeightSwitcherActivity.this.finish();
        }
    };
    private View.OnClickListener mCancelListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            HeavyWeightSwitcherActivity.this.finish();
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(3);
        this.mStartIntent = (IntentSender) getIntent().getParcelableExtra("intent");
        this.mHasResult = getIntent().getBooleanExtra(KEY_HAS_RESULT, false);
        this.mCurApp = getIntent().getStringExtra(KEY_CUR_APP);
        this.mCurTask = getIntent().getIntExtra(KEY_CUR_TASK, 0);
        this.mNewApp = getIntent().getStringExtra(KEY_NEW_APP);
        setContentView(R.layout.heavy_weight_switcher);
        setIconAndText(R.id.old_app_icon, R.id.old_app_action, R.id.old_app_description, this.mCurApp, R.string.old_app_action, R.string.old_app_description);
        setIconAndText(R.id.new_app_icon, R.id.new_app_action, R.id.new_app_description, this.mNewApp, R.string.new_app_action, R.string.new_app_description);
        View button = findViewById(R.id.switch_old);
        button.setOnClickListener(this.mSwitchOldListener);
        View button2 = findViewById(R.id.switch_new);
        button2.setOnClickListener(this.mSwitchNewListener);
        View button3 = findViewById(R.id.cancel);
        button3.setOnClickListener(this.mCancelListener);
        TypedValue out = new TypedValue();
        getTheme().resolveAttribute(16843605, out, true);
        getWindow().setFeatureDrawableResource(3, out.resourceId);
    }

    void setText(int id, CharSequence text) {
        ((TextView) findViewById(id)).setText(text);
    }

    void setDrawable(int id, Drawable dr) {
        if (dr != null) {
            ((ImageView) findViewById(id)).setImageDrawable(dr);
        }
    }

    void setIconAndText(int iconId, int actionId, int descriptionId, String packageName, int actionStr, int descriptionStr) {
        CharSequence appName = ProxyInfo.LOCAL_EXCL_LIST;
        Drawable appIcon = null;
        if (this.mCurApp != null) {
            try {
                ApplicationInfo info = getPackageManager().getApplicationInfo(packageName, 0);
                appName = info.loadLabel(getPackageManager());
                appIcon = info.loadIcon(getPackageManager());
            } catch (PackageManager.NameNotFoundException e) {
            }
        }
        setDrawable(iconId, appIcon);
        setText(actionId, getString(actionStr, appName));
        setText(descriptionId, getText(descriptionStr));
    }
}
