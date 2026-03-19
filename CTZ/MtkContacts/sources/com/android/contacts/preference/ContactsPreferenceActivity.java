package com.android.contacts.preference;

import android.R;
import android.content.Intent;
import android.content.res.Configuration;
import android.database.Cursor;
import android.os.Bundle;
import android.preference.PreferenceActivity;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatCallback;
import android.support.v7.app.AppCompatDelegate;
import android.text.TextUtils;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import com.android.contacts.activities.RequestPermissionsActivity;
import com.android.contacts.editor.SelectAccountDialogFragment;
import com.android.contacts.list.ProviderStatusWatcher;
import com.android.contacts.model.account.AccountWithDataSet;
import com.android.contacts.preference.DisplayOptionsPreferenceFragment;
import com.android.contacts.util.AccountSelectionUtil;
import com.mediatek.contacts.eventhandler.GeneralEventHandler;
import com.mediatek.contacts.util.Log;

public final class ContactsPreferenceActivity extends PreferenceActivity implements SelectAccountDialogFragment.Listener, DisplayOptionsPreferenceFragment.ProfileListener, GeneralEventHandler.Listener {
    private boolean mAreContactsAvailable;
    private AppCompatDelegate mCompatDelegate;
    private boolean mIsSafeToCommitTransactions;
    private String mNewLocalProfileExtra;
    private ProviderStatusWatcher mProviderStatusWatcher;

    @Override
    protected void onCreate(Bundle bundle) {
        this.mCompatDelegate = AppCompatDelegate.create(this, (AppCompatCallback) null);
        super.onCreate(bundle);
        this.mCompatDelegate.onCreate(bundle);
        this.mIsSafeToCommitTransactions = true;
        Log.d("ContactsPreferenceActivity", "[onCreate]");
        GeneralEventHandler.getInstance(this).register(this);
        RequestPermissionsActivity.startPermissionActivityIfNeeded(this);
        ActionBar supportActionBar = this.mCompatDelegate.getSupportActionBar();
        if (supportActionBar != null) {
            supportActionBar.setDisplayOptions(4, 4);
        }
        this.mProviderStatusWatcher = ProviderStatusWatcher.getInstance(this);
        this.mNewLocalProfileExtra = getIntent().getStringExtra("newLocalProfile");
        this.mAreContactsAvailable = this.mProviderStatusWatcher.getProviderStatus() == 0;
        if (bundle == null) {
            getFragmentManager().beginTransaction().replace(R.id.content, DisplayOptionsPreferenceFragment.newInstance(this.mNewLocalProfileExtra, this.mAreContactsAvailable), "display_options").commit();
            setActivityTitle(com.android.contacts.R.string.activity_title_settings);
        } else if (((AboutPreferenceFragment) getFragmentManager().findFragmentByTag("about_contacts")) != null) {
            setActivityTitle(com.android.contacts.R.string.setting_about);
        } else {
            setActivityTitle(com.android.contacts.R.string.activity_title_settings);
        }
    }

    @Override
    protected void onPostCreate(Bundle bundle) {
        super.onPostCreate(bundle);
        this.mCompatDelegate.onPostCreate(bundle);
    }

    @Override
    protected void onStart() {
        super.onStart();
        this.mIsSafeToCommitTransactions = true;
    }

    @Override
    protected void onResume() {
        super.onResume();
        this.mIsSafeToCommitTransactions = true;
    }

    @Override
    protected void onSaveInstanceState(Bundle bundle) {
        super.onSaveInstanceState(bundle);
        this.mIsSafeToCommitTransactions = false;
    }

    public boolean isSafeToCommitTransactions() {
        return this.mIsSafeToCommitTransactions;
    }

    @Override
    public MenuInflater getMenuInflater() {
        return this.mCompatDelegate.getMenuInflater();
    }

    @Override
    public void setContentView(int i) {
        this.mCompatDelegate.setContentView(i);
    }

    @Override
    public void setContentView(View view) {
        this.mCompatDelegate.setContentView(view);
    }

    @Override
    public void setContentView(View view, ViewGroup.LayoutParams layoutParams) {
        this.mCompatDelegate.setContentView(view, layoutParams);
    }

    @Override
    public void addContentView(View view, ViewGroup.LayoutParams layoutParams) {
        this.mCompatDelegate.addContentView(view, layoutParams);
    }

    @Override
    protected void onPostResume() {
        super.onPostResume();
        this.mCompatDelegate.onPostResume();
    }

    @Override
    protected void onTitleChanged(CharSequence charSequence, int i) {
        super.onTitleChanged(charSequence, i);
        this.mCompatDelegate.setTitle(charSequence);
    }

    @Override
    public void onConfigurationChanged(Configuration configuration) {
        super.onConfigurationChanged(configuration);
        this.mCompatDelegate.onConfigurationChanged(configuration);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        Log.d("ContactsPreferenceActivity", "[onDestroy] unregister!");
        GeneralEventHandler.getInstance(this).unRegister(this);
        this.mCompatDelegate.onDestroy();
    }

    @Override
    public void invalidateOptionsMenu() {
        this.mCompatDelegate.invalidateOptionsMenu();
    }

    protected void showAboutFragment() {
        if (!isSafeToCommitTransactions()) {
            Log.w("ContactsPreferenceActivity", "[showAboutFragment] Ignore due to onSavedInstance has called!");
        } else {
            getFragmentManager().beginTransaction().replace(R.id.content, AboutPreferenceFragment.newInstance(), "about_contacts").addToBackStack(null).commit();
            setActivityTitle(com.android.contacts.R.string.setting_about);
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem menuItem) {
        if (menuItem.getItemId() == 16908332) {
            onBackPressed();
            return true;
        }
        return false;
    }

    @Override
    public void onBackPressed() {
        if (!isSafeToCommitTransactions()) {
            Log.w("ContactsPreferenceActivity", "[onBackPressed] Ignore due to onSavedInstance has called!");
        } else if (getFragmentManager().getBackStackEntryCount() > 0) {
            setActivityTitle(com.android.contacts.R.string.activity_title_settings);
            getFragmentManager().popBackStack();
        } else {
            super.onBackPressed();
        }
    }

    private void setActivityTitle(int i) {
        ActionBar supportActionBar = this.mCompatDelegate.getSupportActionBar();
        if (supportActionBar != null) {
            supportActionBar.setTitle(i);
        }
    }

    @Override
    public void onProfileLoaded(Cursor cursor) {
        String string;
        long j;
        boolean z;
        int i;
        if (cursor == null || !cursor.moveToFirst()) {
            string = null;
            j = -1;
            z = false;
            i = 0;
        } else {
            boolean z2 = cursor.getInt(2) == 1;
            string = cursor.getString(1);
            j = cursor.getLong(0);
            i = cursor.getInt(3);
            z = z2;
        }
        ((DisplayOptionsPreferenceFragment) getFragmentManager().findFragmentByTag("display_options")).updateMyInfoPreference(z, (z && TextUtils.isEmpty(string)) ? getString(com.android.contacts.R.string.missing_name) : string, j, i);
    }

    @Override
    public void onAccountChosen(AccountWithDataSet accountWithDataSet, Bundle bundle) {
        AccountSelectionUtil.doImport(this, bundle.getInt("resourceId"), accountWithDataSet, bundle.getInt("subscriptionId"));
    }

    @Override
    public void onAccountSelectorCancelled() {
    }

    @Override
    public void onReceiveEvent(String str, Intent intent) {
        Log.d("ContactsPreferenceActivity", "[onReceiveEvent] eventType: " + str);
        if ("PhbChangeEvent".equals(str) && !isFinishing()) {
            Log.i("ContactsPreferenceActivity", "[onReceiveEvent] Phb state change ,finish");
            finish();
        }
    }
}
