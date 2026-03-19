package com.mediatek.plugin.component;

import android.app.Activity;
import android.content.Intent;
import android.content.res.AssetManager;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.WindowManager;
import com.mediatek.plugin.Plugin;
import com.mediatek.plugin.PluginManager;
import com.mediatek.plugin.element.PluginDescriptor;
import com.mediatek.plugin.utils.Log;
import com.mediatek.plugin.utils.ReflectUtils;

public class PluginProxyActivity extends Activity implements IProxy {
    public static final String CLASS_PILGIN = "Class_plugin";
    public static final String PATH_APK = "Path_apk";
    private static final String TAG = "PluginManager/PluginProxyActivity";
    public static final String URI_IMG = "Uri_img";
    private AssetManager mAsset;
    private ClassLoader mClassLoader;
    private PluginBaseActivity mPluginActivity;
    private Resources mResources;

    @Override
    public void attach(IPlugin iPlugin) {
    }

    @Override
    protected void onCreate(Bundle bundle) {
        super.onCreate(bundle);
        Intent intent = getIntent();
        getActionBar();
        String stringExtra = intent.getStringExtra(ComponentSupport.KEY_TARGET_PLUGIN_ID);
        String stringExtra2 = intent.getStringExtra(ComponentSupport.KEY_TARGET_ACTVITY);
        Log.d(TAG, "<onCreate> bundle = " + stringExtra);
        if (stringExtra != null && stringExtra2 != null) {
            Plugin plugin = PluginManager.getInstance(this).getPlugin(stringExtra);
            if (plugin == null) {
                finish();
                Log.e(TAG, "<onCreate> plugin == null Please check!!!");
                return;
            }
            PluginDescriptor descriptor = plugin.getDescriptor();
            this.mResources = descriptor.getResources();
            this.mAsset = descriptor.getAssetManager();
            this.mPluginActivity = launchTargetActivity(stringExtra2, plugin.getClassLoader());
            if (this.mPluginActivity != null) {
                Log.d(TAG, "<onCreate> mPluginActivity = " + this.mPluginActivity);
                this.mPluginActivity.onCreate(bundle);
                return;
            }
            finish();
            Log.e(TAG, "<onCreate> mPluginActivity == null Please check!!!");
        }
    }

    @Override
    protected void onStart() {
        this.mPluginActivity.onStart();
        super.onStart();
    }

    @Override
    protected void onRestart() {
        this.mPluginActivity.onRestart();
        super.onRestart();
    }

    @Override
    protected void onResume() {
        this.mPluginActivity.onResume();
        super.onResume();
    }

    @Override
    protected void onPause() {
        this.mPluginActivity.onPause();
        super.onPause();
    }

    @Override
    protected void onStop() {
        this.mPluginActivity.onStop();
        super.onStop();
    }

    @Override
    protected void onDestroy() {
        this.mPluginActivity.onDestroy();
        this.mPluginActivity = null;
        this.mClassLoader = null;
        super.onDestroy();
    }

    @Override
    protected void onSaveInstanceState(Bundle bundle) {
        if (this.mPluginActivity != null) {
            this.mPluginActivity.onSaveInstanceState(bundle);
        }
        super.onSaveInstanceState(bundle);
    }

    @Override
    protected void onRestoreInstanceState(Bundle bundle) {
        if (this.mPluginActivity != null) {
            this.mPluginActivity.onRestoreInstanceState(bundle);
        }
        super.onRestoreInstanceState(bundle);
    }

    @Override
    protected void onNewIntent(Intent intent) {
        if (this.mPluginActivity != null) {
            this.mPluginActivity.onNewIntent(intent);
        }
        super.onNewIntent(intent);
    }

    @Override
    public void onBackPressed() {
        if (this.mPluginActivity != null) {
            this.mPluginActivity.onBackPressed();
        }
        super.onBackPressed();
    }

    @Override
    public boolean onTouchEvent(MotionEvent motionEvent) {
        super.onTouchEvent(motionEvent);
        return this.mPluginActivity.onTouchEvent(motionEvent);
    }

    @Override
    public boolean onKeyUp(int i, KeyEvent keyEvent) {
        super.onKeyUp(i, keyEvent);
        return this.mPluginActivity.onKeyUp(i, keyEvent);
    }

    @Override
    public void onWindowAttributesChanged(WindowManager.LayoutParams layoutParams) {
        if (this.mPluginActivity != null) {
            this.mPluginActivity.onWindowAttributesChanged(layoutParams);
        }
        super.onWindowAttributesChanged(layoutParams);
    }

    @Override
    public void onWindowFocusChanged(boolean z) {
        if (this.mPluginActivity != null) {
            this.mPluginActivity.onWindowFocusChanged(z);
        }
        super.onWindowFocusChanged(z);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        Log.d(TAG, " <onCreateOptionsMenu> ");
        if (this.mPluginActivity != null) {
            this.mPluginActivity.onCreateOptionsMenu(menu);
        }
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        if (this.mPluginActivity != null) {
            this.mPluginActivity.onPrepareOptionsMenu(menu);
        }
        return super.onPrepareOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem menuItem) {
        if (this.mPluginActivity != null) {
            this.mPluginActivity.onOptionsItemSelected(menuItem);
        }
        return super.onOptionsItemSelected(menuItem);
    }

    @Override
    public void onConfigurationChanged(Configuration configuration) {
        if (this.mPluginActivity != null) {
            this.mPluginActivity.onConfigurationChanged(configuration);
        }
        super.onConfigurationChanged(configuration);
    }

    @Override
    public Resources getResources() {
        if (this.mResources != null) {
            return this.mResources;
        }
        return super.getResources();
    }

    @Override
    public AssetManager getAssets() {
        if (this.mAsset != null) {
            return this.mAsset;
        }
        return super.getAssets();
    }

    @Override
    public Resources.Theme getTheme() {
        return super.getTheme();
    }

    @Override
    public ClassLoader getClassLoader() {
        if (this.mClassLoader != null) {
            return this.mClassLoader;
        }
        return super.getClassLoader();
    }

    @Override
    public MenuInflater getMenuInflater() {
        return new MenuInflater(this.mPluginActivity);
    }

    @Override
    public void setContentView(int i) {
        getWindow().setContentView(i);
    }

    @Override
    protected void onActivityResult(int i, int i2, Intent intent) {
        this.mPluginActivity.onActivityResult(i, i2, intent);
        super.onActivityResult(i, i2, intent);
    }

    private PluginBaseActivity launchTargetActivity(String str, ClassLoader classLoader) {
        ?? CreateInstance = ReflectUtils.createInstance(str, classLoader, new Object[0]);
        if (CreateInstance != 0 && (CreateInstance instanceof PluginBaseActivity)) {
            CreateInstance.addProxyActivity(this);
            attach(CreateInstance);
            return CreateInstance;
        }
        return null;
    }
}
