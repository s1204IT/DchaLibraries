package com.android.launcher2;

import android.app.Activity;
import android.app.DialogFragment;
import android.app.Fragment;
import android.os.Bundle;
import com.android.launcher.R;

public class WallpaperChooser extends Activity {
    @Override
    public void onCreate(Bundle icicle) {
        super.onCreate(icicle);
        setContentView(R.layout.wallpaper_chooser_base);
        Fragment fragmentView = getFragmentManager().findFragmentById(R.id.wallpaper_chooser_fragment);
        if (fragmentView == null) {
            DialogFragment fragment = WallpaperChooserDialogFragment.newInstance();
            fragment.show(getFragmentManager(), "dialog");
        }
    }
}
