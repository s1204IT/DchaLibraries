package com.android.bips.ui;

import android.R;
import android.app.ActionBar;
import android.app.Activity;
import android.content.ComponentCallbacks2;
import android.os.Bundle;
import android.view.MenuItem;

public class AddPrintersActivity extends Activity {

    interface OnPermissionChangeListener {
        void onPermissionChange();
    }

    @Override
    protected void onCreate(Bundle bundle) {
        super.onCreate(bundle);
        getFragmentManager().beginTransaction().replace(R.id.content, new AddPrintersFragment()).commit();
        ActionBar actionBar = getActionBar();
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true);
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem menuItem) {
        if (menuItem.getItemId() == 16908332) {
            onBackPressed();
            return true;
        }
        return super.onOptionsItemSelected(menuItem);
    }

    @Override
    public void onRequestPermissionsResult(int i, String[] strArr, int[] iArr) {
        ComponentCallbacks2 componentCallbacks2FindFragmentById = getFragmentManager().findFragmentById(R.id.content);
        if (componentCallbacks2FindFragmentById != null && (componentCallbacks2FindFragmentById instanceof OnPermissionChangeListener)) {
            ((OnPermissionChangeListener) componentCallbacks2FindFragmentById).onPermissionChange();
        }
    }
}
