package com.android.wallpaper.livepicker;

import android.app.ListActivity;
import android.app.WallpaperInfo;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ListView;
import com.android.wallpaper.livepicker.LiveWallpaperListAdapter;

public class LiveWallpaperActivity extends ListActivity {
    private LiveWallpaperListAdapter mAdapter;

    @Override
    protected void onCreate(Bundle bundle) {
        super.onCreate(bundle);
        setContentView(R.layout.live_wallpaper_base);
        this.mAdapter = new LiveWallpaperListAdapter(this);
        setListAdapter(this.mAdapter);
    }

    @Override
    protected void onActivityResult(int i, int i2, Intent intent) {
        super.onActivityResult(i, i2, intent);
        if (i == 100 && i2 == -1) {
            setResult(i2);
            finish();
        }
    }

    @Override
    protected void onListItemClick(ListView listView, View view, int i, long j) {
        WallpaperInfo wallpaperInfo = ((LiveWallpaperListAdapter.LiveWallpaperInfo) this.mAdapter.getItem(i)).info;
        if (wallpaperInfo != null) {
            Intent intent = new Intent(this, (Class<?>) LiveWallpaperPreview.class);
            intent.putExtra("android.live_wallpaper.info", wallpaperInfo);
            startActivityForResult(intent, 100);
        }
    }
}
