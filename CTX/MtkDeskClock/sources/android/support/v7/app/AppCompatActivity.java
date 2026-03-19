package android.support.v7.app;

import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.CallSuper;
import android.support.annotation.IdRes;
import android.support.annotation.LayoutRes;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.StyleRes;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.NavUtils;
import android.support.v4.app.TaskStackBuilder;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.view.ActionMode;
import android.support.v7.widget.Toolbar;
import android.support.v7.widget.VectorEnabledTintResources;
import android.util.DisplayMetrics;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;

public class AppCompatActivity extends FragmentActivity implements AppCompatCallback, TaskStackBuilder.SupportParentable, ActionBarDrawerToggle.DelegateProvider {
    private AppCompatDelegate mDelegate;
    private Resources mResources;
    private int mThemeId = 0;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        AppCompatDelegate delegate = getDelegate();
        delegate.installViewFactory();
        delegate.onCreate(savedInstanceState);
        if (delegate.applyDayNight() && this.mThemeId != 0) {
            if (Build.VERSION.SDK_INT >= 23) {
                onApplyThemeResource(getTheme(), this.mThemeId, false);
            } else {
                setTheme(this.mThemeId);
            }
        }
        super.onCreate(savedInstanceState);
    }

    @Override
    public void setTheme(@StyleRes int resid) {
        super.setTheme(resid);
        this.mThemeId = resid;
    }

    @Override
    protected void onPostCreate(@Nullable Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);
        getDelegate().onPostCreate(savedInstanceState);
    }

    @Nullable
    public ActionBar getSupportActionBar() {
        return getDelegate().getSupportActionBar();
    }

    public void setSupportActionBar(@Nullable Toolbar toolbar) {
        getDelegate().setSupportActionBar(toolbar);
    }

    @Override
    public MenuInflater getMenuInflater() {
        return getDelegate().getMenuInflater();
    }

    @Override
    public void setContentView(@LayoutRes int layoutResID) {
        getDelegate().setContentView(layoutResID);
    }

    @Override
    public void setContentView(View view) {
        getDelegate().setContentView(view);
    }

    @Override
    public void setContentView(View view, ViewGroup.LayoutParams params) {
        getDelegate().setContentView(view, params);
    }

    @Override
    public void addContentView(View view, ViewGroup.LayoutParams params) {
        getDelegate().addContentView(view, params);
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        getDelegate().onConfigurationChanged(newConfig);
        if (this.mResources != null) {
            DisplayMetrics newMetrics = super.getResources().getDisplayMetrics();
            this.mResources.updateConfiguration(newConfig, newMetrics);
        }
    }

    @Override
    protected void onPostResume() {
        super.onPostResume();
        getDelegate().onPostResume();
    }

    @Override
    protected void onStart() {
        super.onStart();
        getDelegate().onStart();
    }

    @Override
    protected void onStop() {
        super.onStop();
        getDelegate().onStop();
    }

    @Override
    public <T extends View> T findViewById(@IdRes int i) {
        return (T) getDelegate().findViewById(i);
    }

    @Override
    public final boolean onMenuItemSelected(int featureId, MenuItem item) {
        if (super.onMenuItemSelected(featureId, item)) {
            return true;
        }
        ActionBar ab = getSupportActionBar();
        if (item.getItemId() == 16908332 && ab != null && (ab.getDisplayOptions() & 4) != 0) {
            return onSupportNavigateUp();
        }
        return false;
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        getDelegate().onDestroy();
    }

    @Override
    protected void onTitleChanged(CharSequence title, int color) {
        super.onTitleChanged(title, color);
        getDelegate().setTitle(title);
    }

    public boolean supportRequestWindowFeature(int featureId) {
        return getDelegate().requestWindowFeature(featureId);
    }

    @Override
    public void supportInvalidateOptionsMenu() {
        getDelegate().invalidateOptionsMenu();
    }

    @Override
    public void invalidateOptionsMenu() {
        getDelegate().invalidateOptionsMenu();
    }

    @Override
    @CallSuper
    public void onSupportActionModeStarted(@NonNull ActionMode mode) {
    }

    @Override
    @CallSuper
    public void onSupportActionModeFinished(@NonNull ActionMode mode) {
    }

    @Override
    @Nullable
    public ActionMode onWindowStartingSupportActionMode(@NonNull ActionMode.Callback callback) {
        return null;
    }

    @Nullable
    public ActionMode startSupportActionMode(@NonNull ActionMode.Callback callback) {
        return getDelegate().startSupportActionMode(callback);
    }

    @Deprecated
    public void setSupportProgressBarVisibility(boolean visible) {
    }

    @Deprecated
    public void setSupportProgressBarIndeterminateVisibility(boolean visible) {
    }

    @Deprecated
    public void setSupportProgressBarIndeterminate(boolean indeterminate) {
    }

    @Deprecated
    public void setSupportProgress(int progress) {
    }

    public void onCreateSupportNavigateUpTaskStack(@NonNull TaskStackBuilder builder) throws PackageManager.NameNotFoundException {
        builder.addParentStack(this);
    }

    public void onPrepareSupportNavigateUpTaskStack(@NonNull TaskStackBuilder builder) {
    }

    public boolean onSupportNavigateUp() throws PackageManager.NameNotFoundException {
        Intent upIntent = getSupportParentActivityIntent();
        if (upIntent != null) {
            if (supportShouldUpRecreateTask(upIntent)) {
                TaskStackBuilder b = TaskStackBuilder.create(this);
                onCreateSupportNavigateUpTaskStack(b);
                onPrepareSupportNavigateUpTaskStack(b);
                b.startActivities();
                try {
                    ActivityCompat.finishAffinity(this);
                    return true;
                } catch (IllegalStateException e) {
                    finish();
                    return true;
                }
            }
            supportNavigateUpTo(upIntent);
            return true;
        }
        return false;
    }

    @Override
    @Nullable
    public Intent getSupportParentActivityIntent() {
        return NavUtils.getParentActivityIntent(this);
    }

    public boolean supportShouldUpRecreateTask(@NonNull Intent targetIntent) {
        return NavUtils.shouldUpRecreateTask(this, targetIntent);
    }

    public void supportNavigateUpTo(@NonNull Intent upIntent) {
        NavUtils.navigateUpTo(this, upIntent);
    }

    @Override
    public void onContentChanged() {
        onSupportContentChanged();
    }

    @Deprecated
    public void onSupportContentChanged() {
    }

    @Override
    @Nullable
    public ActionBarDrawerToggle.Delegate getDrawerToggleDelegate() {
        return getDelegate().getDrawerToggleDelegate();
    }

    @Override
    public boolean onMenuOpened(int featureId, Menu menu) {
        return super.onMenuOpened(featureId, menu);
    }

    @Override
    public void onPanelClosed(int featureId, Menu menu) {
        super.onPanelClosed(featureId, menu);
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        getDelegate().onSaveInstanceState(outState);
    }

    @NonNull
    public AppCompatDelegate getDelegate() {
        if (this.mDelegate == null) {
            this.mDelegate = AppCompatDelegate.create(this, this);
        }
        return this.mDelegate;
    }

    @Override
    public boolean dispatchKeyEvent(KeyEvent event) {
        int keyCode = event.getKeyCode();
        ActionBar actionBar = getSupportActionBar();
        if (keyCode == 82 && actionBar != null && actionBar.onMenuKeyEvent(event)) {
            return true;
        }
        return super.dispatchKeyEvent(event);
    }

    @Override
    public Resources getResources() {
        if (this.mResources == null && VectorEnabledTintResources.shouldBeUsed()) {
            this.mResources = new VectorEnabledTintResources(this, super.getResources());
        }
        return this.mResources == null ? super.getResources() : this.mResources;
    }

    private boolean performMenuItemShortcut(int keycode, KeyEvent event) {
        Window currentWindow;
        if (Build.VERSION.SDK_INT < 26 && !event.isCtrlPressed() && !KeyEvent.metaStateHasNoModifiers(event.getMetaState()) && event.getRepeatCount() == 0 && !KeyEvent.isModifierKey(event.getKeyCode()) && (currentWindow = getWindow()) != null && currentWindow.getDecorView() != null) {
            View decorView = currentWindow.getDecorView();
            if (decorView.dispatchKeyShortcutEvent(event)) {
                return true;
            }
            return false;
        }
        return false;
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (performMenuItemShortcut(keyCode, event)) {
            return true;
        }
        return super.onKeyDown(keyCode, event);
    }

    @Override
    public void openOptionsMenu() {
        ActionBar actionBar = getSupportActionBar();
        if (getWindow().hasFeature(0)) {
            if (actionBar == null || !actionBar.openOptionsMenu()) {
                super.openOptionsMenu();
            }
        }
    }

    @Override
    public void closeOptionsMenu() {
        ActionBar actionBar = getSupportActionBar();
        if (getWindow().hasFeature(0)) {
            if (actionBar == null || !actionBar.closeOptionsMenu()) {
                super.closeOptionsMenu();
            }
        }
    }
}
