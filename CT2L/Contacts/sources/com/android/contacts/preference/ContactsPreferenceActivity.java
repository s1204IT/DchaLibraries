package com.android.contacts.preference;

import android.app.ActionBar;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.preference.PreferenceActivity;
import android.view.MenuItem;
import com.android.contacts.R;
import com.android.contacts.activities.PeopleActivity;
import java.util.List;

public final class ContactsPreferenceActivity extends PreferenceActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        ActionBar actionBar = getActionBar();
        if (actionBar != null) {
            actionBar.setDisplayOptions(4, 4);
        }
    }

    @Override
    public void onBuildHeaders(List<PreferenceActivity.Header> target) {
        loadHeadersFromResource(R.xml.preference_headers, target);
    }

    public static boolean isEmpty(Context context) {
        return (context.getResources().getBoolean(R.bool.config_sort_order_user_changeable) || context.getResources().getBoolean(R.bool.config_display_order_user_changeable)) ? false : true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                Intent intent = new Intent(this, (Class<?>) PeopleActivity.class);
                intent.setFlags(67108864);
                startActivity(intent);
                finish();
                return true;
            default:
                return false;
        }
    }

    @Override
    protected boolean isValidFragment(String fragmentName) {
        return true;
    }
}
