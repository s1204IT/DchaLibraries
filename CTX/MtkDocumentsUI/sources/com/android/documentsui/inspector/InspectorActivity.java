package com.android.documentsui.inspector;

import android.app.Activity;
import android.app.FragmentManager;
import android.os.Bundle;
import android.view.MenuItem;
import android.widget.Toolbar;
import com.android.documentsui.R;

public class InspectorActivity extends Activity {
    private InspectorFragment mFragment;

    @Override
    public void onCreate(Bundle bundle) {
        super.onCreate(bundle);
        requestWindowFeature(8);
        setContentView(R.layout.inspector_activity);
        setActionBar((Toolbar) findViewById(R.id.toolbar));
        getActionBar().setDisplayHomeAsUpEnabled(true);
        FragmentManager fragmentManager = getFragmentManager();
        this.mFragment = (InspectorFragment) fragmentManager.findFragmentById(R.id.fragment_container);
        if (this.mFragment == null) {
            this.mFragment = InspectorFragment.newInstance(getIntent());
            fragmentManager.beginTransaction().add(R.id.fragment_container, this.mFragment).commit();
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem menuItem) {
        if (menuItem.getItemId() == 16908332) {
            finish();
            return true;
        }
        return super.onOptionsItemSelected(menuItem);
    }
}
