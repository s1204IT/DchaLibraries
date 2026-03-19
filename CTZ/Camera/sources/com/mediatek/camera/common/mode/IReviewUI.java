package com.mediatek.camera.common.mode;

import android.graphics.Bitmap;
import android.view.View;

public interface IReviewUI {

    public static class ReviewSpec {
        public View.OnClickListener retakeListener = null;
        public View.OnClickListener playListener = null;
        public View.OnClickListener saveListener = null;
    }

    void hideReviewUI();

    void initReviewUI(ReviewSpec reviewSpec);

    void showReviewUI(Bitmap bitmap);

    void updateOrientation(int i);
}
