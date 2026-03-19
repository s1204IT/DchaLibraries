package com.mediatek.contacts.list;

import android.os.Bundle;
import android.support.v7.app.ActionBar;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import com.android.contacts.AppCompatContactsActivity;
import com.android.contacts.R;
import com.mediatek.contacts.util.Log;

public class ContactGroupListActivity extends AppCompatContactsActivity {
    @Override
    protected void onCreate(Bundle bundle) {
        super.onCreate(bundle);
        Log.i("ContactGroupListActivity", "[onCreate]");
        ActionBar supportActionBar = getSupportActionBar();
        if (supportActionBar != null) {
            supportActionBar.setDisplayOptions(12, 14);
            supportActionBar.setTitle(R.string.contact_group_list_title);
        }
        setContentView(R.layout.mtk_contact_group_list_activity);
        final ContactGroupBrowseListFragment contactGroupBrowseListFragment = (ContactGroupBrowseListFragment) getFragment(R.id.list_fragment);
        ((Button) findViewById(R.id.btn_ok)).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                contactGroupBrowseListFragment.onOkClick();
            }
        });
        ((Button) findViewById(R.id.btn_cancel)).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                ContactGroupListActivity.this.finish();
            }
        });
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem menuItem) {
        if (menuItem.getItemId() == 16908332) {
            finish();
            return true;
        }
        return super.onOptionsItemSelected(menuItem);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
    }
}
