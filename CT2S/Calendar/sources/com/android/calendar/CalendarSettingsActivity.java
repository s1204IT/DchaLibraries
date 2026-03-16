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
    public void onBuildHeaders(List<PreferenceActivity.Header> target) {
        loadHeadersFromResource(R.xml.calendar_settings_headers, target);
        Account[] accounts = AccountManager.get(this).getAccounts();
        if (accounts != null) {
            for (Account acct : accounts) {
                if (ContentResolver.getIsSyncable(acct, "com.android.calendar") > 0) {
                    PreferenceActivity.Header accountHeader = new PreferenceActivity.Header();
                    accountHeader.title = acct.name;
                    accountHeader.fragment = "com.android.calendar.selectcalendars.SelectCalendarsSyncFragment";
                    Bundle args = new Bundle();
                    args.putString("account_name", acct.name);
                    args.putString("account_type", acct.type);
                    accountHeader.fragmentArguments = args;
                    target.add(1, accountHeader);
                }
            }
        }
        this.mAccounts = accounts;
        if (Utils.getTardis() + 60000 > System.currentTimeMillis()) {
            PreferenceActivity.Header tardisHeader = new PreferenceActivity.Header();
            tardisHeader.title = getString(R.string.preferences_experimental_category);
            tardisHeader.fragment = "com.android.calendar.OtherPreferences";
            target.add(tardisHeader);
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == 16908332) {
            finish();
            return true;
        }
        if (item.getItemId() == R.id.action_add_account) {
            if (BenesseExtension.getDchaState() != 0) {
                return true;
            }
            Intent nextIntent = new Intent("android.settings.ADD_ACCOUNT_SETTINGS");
            String[] array = {"com.android.calendar"};
            nextIntent.putExtra("authorities", array);
            nextIntent.addFlags(67108864);
            startActivity(nextIntent);
            return true;
        }
        return super.onOptionsItemSelected(item);
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
    protected boolean isValidFragment(String fragmentName) {
        return true;
    }

    public void hideMenuButtons() {
        this.mHideMenuButtons = true;
    }
}
