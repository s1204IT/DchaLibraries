package com.mediatek.plugin.component;

import android.app.ActionBar;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import com.mediatek.plugin.utils.Log;

public class PluginBaseActivity extends Activity {
    private static final String TAG = "PluginManager/PluginBaseActivity";
    private boolean mLaunchAsPlugin = false;
    protected Activity mThis = this;

    public void addProxyActivity(Activity activity) {
        this.mLaunchAsPlugin = true;
        this.mThis = activity;
        initActivityInfo();
    }

    @Override
    public void setContentView(View view) {
        Log.d(TAG, "<setContentView> mLaunchAsPlugin444 = " + this.mLaunchAsPlugin);
        if (!this.mLaunchAsPlugin) {
            super.setContentView(view);
        } else {
            this.mThis.setContentView(view);
        }
    }

    @Override
    public void setContentView(View view, ViewGroup.LayoutParams layoutParams) {
        if (!this.mLaunchAsPlugin) {
            super.setContentView(view, layoutParams);
        } else {
            this.mThis.setContentView(view, layoutParams);
        }
    }

    @Override
    public void setContentView(int i) {
        if (!this.mLaunchAsPlugin) {
            super.setContentView(i);
        } else {
            this.mThis.setContentView(i);
        }
    }

    @Override
    public void addContentView(View view, ViewGroup.LayoutParams layoutParams) {
        if (!this.mLaunchAsPlugin) {
            super.addContentView(view, layoutParams);
        } else {
            this.mThis.addContentView(view, layoutParams);
        }
    }

    @Override
    public View findViewById(int i) {
        if (!this.mLaunchAsPlugin) {
            return super.findViewById(i);
        }
        return this.mThis.findViewById(i);
    }

    @Override
    public Intent getIntent() {
        if (!this.mLaunchAsPlugin) {
            return super.getIntent();
        }
        return this.mThis.getIntent();
    }

    @Override
    public ClassLoader getClassLoader() {
        if (!this.mLaunchAsPlugin) {
            return super.getClassLoader();
        }
        return this.mThis.getClassLoader();
    }

    @Override
    public Resources getResources() {
        if (!this.mLaunchAsPlugin) {
            return super.getResources();
        }
        return this.mThis.getResources();
    }

    @Override
    public String getPackageName() {
        if (!this.mLaunchAsPlugin) {
            return super.getPackageName();
        }
        return this.mThis.getPackageName();
    }

    @Override
    public LayoutInflater getLayoutInflater() {
        if (!this.mLaunchAsPlugin) {
            return super.getLayoutInflater();
        }
        return this.mThis.getLayoutInflater();
    }

    @Override
    public MenuInflater getMenuInflater() {
        if (!this.mLaunchAsPlugin) {
            return super.getMenuInflater();
        }
        return this.mThis.getMenuInflater();
    }

    @Override
    public SharedPreferences getSharedPreferences(String str, int i) {
        if (!this.mLaunchAsPlugin) {
            return super.getSharedPreferences(str, i);
        }
        return this.mThis.getSharedPreferences(str, i);
    }

    @Override
    public Context getApplicationContext() {
        if (!this.mLaunchAsPlugin) {
            return super.getApplicationContext();
        }
        return this.mThis.getApplicationContext();
    }

    @Override
    public Context getBaseContext() {
        if (!this.mLaunchAsPlugin) {
            return super.getBaseContext();
        }
        return this.mThis.getBaseContext();
    }

    @Override
    public WindowManager getWindowManager() {
        if (!this.mLaunchAsPlugin) {
            return super.getWindowManager();
        }
        return this.mThis.getWindowManager();
    }

    @Override
    public Window getWindow() {
        if (!this.mLaunchAsPlugin) {
            return super.getWindow();
        }
        return this.mThis.getWindow();
    }

    @Override
    public Object getSystemService(String str) {
        if (!this.mLaunchAsPlugin) {
            return super.getSystemService(str);
        }
        return this.mThis.getSystemService(str);
    }

    @Override
    public void finish() {
        if (!this.mLaunchAsPlugin) {
            super.finish();
        } else {
            this.mThis.finish();
        }
    }

    @Override
    public void onBackPressed() {
        if (!this.mLaunchAsPlugin) {
            super.onBackPressed();
        }
    }

    @Override
    protected void onActivityResult(int i, int i2, Intent intent) {
        if (!this.mLaunchAsPlugin) {
            super.onActivityResult(i, i2, intent);
        }
    }

    @Override
    protected void onStart() {
        if (!this.mLaunchAsPlugin) {
            super.onStart();
        }
    }

    @Override
    public void onRestart() {
        if (!this.mLaunchAsPlugin) {
            super.onRestart();
        }
    }

    @Override
    public void onRestoreInstanceState(Bundle bundle) {
        if (!this.mLaunchAsPlugin) {
            super.onRestoreInstanceState(bundle);
        }
    }

    @Override
    protected void onSaveInstanceState(Bundle bundle) {
        if (!this.mLaunchAsPlugin) {
            super.onSaveInstanceState(bundle);
        }
    }

    @Override
    public void onNewIntent(Intent intent) {
        if (!this.mLaunchAsPlugin) {
            super.onNewIntent(intent);
        }
    }

    @Override
    protected void onResume() {
        if (!this.mLaunchAsPlugin) {
            super.onResume();
        }
    }

    @Override
    protected void onPause() {
        if (!this.mLaunchAsPlugin) {
            super.onPause();
        }
    }

    @Override
    protected void onStop() {
        if (!this.mLaunchAsPlugin) {
            super.onStop();
        }
    }

    @Override
    protected void onDestroy() {
        if (!this.mLaunchAsPlugin) {
            super.onDestroy();
        }
    }

    @Override
    public boolean onTouchEvent(MotionEvent motionEvent) {
        if (!this.mLaunchAsPlugin) {
            return super.onTouchEvent(motionEvent);
        }
        return false;
    }

    @Override
    public boolean onKeyUp(int i, KeyEvent keyEvent) {
        if (!this.mLaunchAsPlugin) {
            return super.onKeyUp(i, keyEvent);
        }
        return false;
    }

    @Override
    public void onWindowAttributesChanged(WindowManager.LayoutParams layoutParams) {
        if (!this.mLaunchAsPlugin) {
            super.onWindowAttributesChanged(layoutParams);
        }
    }

    @Override
    public void onWindowFocusChanged(boolean z) {
        if (!this.mLaunchAsPlugin) {
            super.onWindowFocusChanged(z);
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        Log.d(TAG, "<onCreateOptionsMenu> mLaunchAsPlugin = " + this.mLaunchAsPlugin);
        if (!this.mLaunchAsPlugin) {
            return super.onCreateOptionsMenu(menu);
        }
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem menuItem) {
        if (!this.mLaunchAsPlugin) {
            return onOptionsItemSelected(menuItem);
        }
        return false;
    }

    private void initActivityInfo() {
    }

    public void onAttach(IProxy iProxy) {
    }

    @Override
    protected void onCreate(Bundle bundle) {
        if (!this.mLaunchAsPlugin) {
            super.onCreate(bundle);
        }
    }

    @Override
    public Resources.Theme getTheme() {
        if (!this.mLaunchAsPlugin) {
            return super.getTheme();
        }
        return this.mThis.getTheme();
    }

    @Override
    public ActionBar getActionBar() {
        if (!this.mLaunchAsPlugin) {
            return super.getActionBar();
        }
        return this.mThis.getActionBar();
    }
}
