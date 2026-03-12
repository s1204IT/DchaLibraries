package com.android.settings.intelligence.search;

import android.app.Activity;
import android.app.FragmentManager;
import android.os.Bundle;
import com.android.settings.intelligence.R;

public class SearchActivity extends Activity {
    @Override
    public void onCreate(Bundle bundle) {
        super.onCreate(bundle);
        setContentView(R.layout.search_main);
        getWindow().setSoftInputMode(32);
        FragmentManager fragmentManager = getFragmentManager();
        if (fragmentManager.findFragmentById(R.id.main_content) == null) {
            fragmentManager.beginTransaction().add(R.id.main_content, new SearchFragment()).commit();
        }
    }

    @Override
    public boolean onNavigateUp() {
        finish();
        return true;
    }
}
