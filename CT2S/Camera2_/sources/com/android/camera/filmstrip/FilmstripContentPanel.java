package com.android.camera.filmstrip;

import com.android.camera.filmstrip.FilmstripController;

public interface FilmstripContentPanel {

    public interface Listener extends FilmstripController.FilmstripListener {
        void onFilmstripHidden();

        void onFilmstripShown();

        void onSwipeOut();

        void onSwipeOutBegin();
    }

    boolean animateHide();

    void hide();

    boolean onBackPressed();

    void setFilmstripListener(Listener listener);

    void show();
}
