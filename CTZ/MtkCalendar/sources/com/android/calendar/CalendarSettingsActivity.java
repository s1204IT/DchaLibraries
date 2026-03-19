package com.android.calendar;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.content.ContentResolver;
import android.content.Intent;
import android.os.BenesseExtension;
import android.os.Bundle;
import android.os.Handler;
import android.preference.PreferenceActivity;
import android.view.Menu;
import android.view.MenuItem;
import java.util.List;

public class CalendarSettingsActivity extends PreferenceActivity {
    private Account[] mAccounts;
    private Handler mHandler = new Handler();
    private boolean mHideMenuButtons = false;
    Runnable mCheckAccounts = new Runnable() {
        @Override
        public void run() {
            Account[] accounts = AccountManager.get(CalendarSettingsActivity.this).getAccounts();
            if (accounts != null && !accounts.equals(CalendarSettingsActivity.this.mAccounts)) {
                CalendarSettingsActivity.this.invalidateHeaders();
            }
        }
    };

    @Override
    public void onBuildHeaders(List<PreferenceActivity.Header> list) {
        loadHeadersFromResource(R.xml.calendar_settings_headers, list);
        Account[] accounts = AccountManager.get(getApplicationContext()).getAccounts();
        if (accounts != null) {
            for (Account account : accounts) {
                if (ContentResolver.getIsSyncable(account, "com.android.calendar") > 0) {
                    PreferenceActivity.Header header = new PreferenceActivity.Header();
                    header.title = account.name;
                    header.fragment = "com.android.calendar.selectcalendars.SelectCalendarsSyncFragment";
                    Bundle bundle = new Bundle();
                    bundle.putString("account_name", account.name);
                    bundle.putString("account_type", account.type);
                    header.fragmentArguments = bundle;
                    list.add(1, header);
                }
            }
        }
        this.mAccounts = accounts;
        if (Utils.getTardis() + 60000 > System.currentTimeMillis()) {
            PreferenceActivity.Header header2 = new PreferenceActivity.Header();
            header2.title = getString(R.string.preferences_experimental_category);
            header2.fragment = "com.android.calendar.OtherPreferences";
            list.add(header2);
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem menuItem) {
        if (menuItem.getItemId() == 16908332) {
            finish();
            return true;
        }
        if (menuItem.getItemId() == R.id.action_add_account) {
            if (BenesseExtension.getDchaState() != 0) {
                return true;
            }
            Intent intent = new Intent("android.settings.ADD_ACCOUNT_SETTINGS");
            intent.putExtra("authorities", new String[]{"com.android.calendar"});
            intent.addFlags(67108864);
            startActivity(intent);
            return true;
        }
        return super.onOptionsItemSelected(menuItem);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        if (!this.mHideMenuButtons) {
            getMenuInflater().inflate(R.menu.settings_title_bar, menu);
        }
        getActionBar().setDisplayOptions(4, 4);
        return true;
    }

    @Override
    public void onResume() {
        if (this.mHandler != null) {
            this.mHandler.postDelayed(this.mCheckAccounts, 3000L);
        }
        super.onResume();
    }

    @Override
    public void onPause() {
        if (this.mHandler != null) {
            this.mHandler.removeCallbacks(this.mCheckAccounts);
        }
        super.onPause();
    }

    @Override
    protected boolean isValidFragment(String str) {
        return true;
    }

    public void hideMenuButtons() {
        this.mHideMenuButtons = true;
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        CalendarController.removeInstance(this);
    }
}
