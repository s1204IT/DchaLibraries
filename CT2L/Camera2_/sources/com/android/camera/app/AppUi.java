package com.android.camera.app;

import android.view.View;
import android.widget.FrameLayout;
import com.android.camera.filmstrip.FilmstripController;

public interface AppUi {
    FilmstripController getFilmstripController();

    FrameLayout getModuleLayoutRoot();

    void init(View view, boolean z, boolean z2);
}
