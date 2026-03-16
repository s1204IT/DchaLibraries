package com.android.phone.settings;

import android.os.Bundle;
import android.preference.PreferenceActivity;
import android.view.MenuItem;
import com.android.phone.R;

public class VoicemailSettingsActivity extends PreferenceActivity {
    @Override
    protected void onCreate(Bundle icicle) {
        super.onCreate(icicle);
        getActionBar().setTitle(R.string.voicemail);
        getFragmentManager().beginTransaction().replace(android.R.id.content, new VoicemailSettingsFragment()).commit();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() != 16908332) {
            return super.onOptionsItemSelected(item);
        }
        onBackPressed();
        return true;
    }
}
