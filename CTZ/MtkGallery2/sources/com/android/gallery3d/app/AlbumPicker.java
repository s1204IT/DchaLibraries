package com.android.gallery3d.app;

import android.os.Bundle;
import com.android.gallery3d.R;

public class AlbumPicker extends PickerActivity {
    @Override
    public void onCreate(Bundle bundle) {
        super.onCreate(bundle);
        setTitle(R.string.select_album);
        Bundle extras = getIntent().getExtras();
        Bundle bundle2 = extras == null ? new Bundle() : new Bundle(extras);
        bundle2.putBoolean("get-album", true);
        bundle2.putString("media-path", getDataManager().getTopSetPath(1));
        getStateManager().startState(AlbumSetPage.class, bundle2);
    }
}
