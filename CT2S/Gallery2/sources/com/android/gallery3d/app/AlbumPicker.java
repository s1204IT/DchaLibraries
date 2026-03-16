package com.android.gallery3d.app;

import android.content.Intent;
import android.os.Bundle;
import com.android.gallery3d.R;

public class AlbumPicker extends PickerActivity {
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setTitle(R.string.select_album);
        Intent intent = getIntent();
        Bundle extras = intent.getExtras();
        Bundle data = extras == null ? new Bundle() : new Bundle(extras);
        data.putBoolean("get-album", true);
        data.putString("media-path", getDataManager().getTopSetPath(1));
        getStateManager().startState(AlbumSetPage.class, data);
    }
}
