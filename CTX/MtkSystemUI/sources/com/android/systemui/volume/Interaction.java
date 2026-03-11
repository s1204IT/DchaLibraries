package com.android.systemui.volume;

import android.view.MotionEvent;
import android.view.View;

public class Interaction {

    public interface Callback {
        void onInteraction();
    }

    public static void register(View view, final Callback callback) {
        view.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View view2, MotionEvent motionEvent) {
                callback.onInteraction();
                return false;
            }
        });
        view.setOnGenericMotionListener(new View.OnGenericMotionListener() {
            @Override
            public boolean onGenericMotion(View view2, MotionEvent motionEvent) {
                callback.onInteraction();
                return false;
            }
        });
    }
}
