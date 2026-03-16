package com.android.photos;

import android.R;
import android.app.Activity;
import android.os.Bundle;
import com.android.photos.MultiChoiceManager;

public class AlbumActivity extends Activity implements MultiChoiceManager.Provider {
    private MultiChoiceManager mMultiChoiceManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Bundle intentExtras = getIntent().getExtras();
        this.mMultiChoiceManager = new MultiChoiceManager(this);
        if (savedInstanceState == null) {
            AlbumFragment albumFragment = new AlbumFragment();
            this.mMultiChoiceManager.setDelegate(albumFragment);
            albumFragment.setArguments(intentExtras);
            getFragmentManager().beginTransaction().add(R.id.content, albumFragment).commit();
        }
        getActionBar().setTitle(intentExtras.getString("AlbumTitle"));
    }

    @Override
    public MultiChoiceManager getMultiChoiceManager() {
        return this.mMultiChoiceManager;
    }
}
