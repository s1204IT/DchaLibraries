package com.android.systemui.volume;

import android.view.MotionEvent;
import android.view.View;

public class Interaction {

    public interface Callback {
        void onInteraction();
    }

    public static void register(View v, final Callback callback) {
        v.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v2, MotionEvent event) {
                callback.onInteraction();
                return false;
            }
        });
        v.setOnGenericMotionListener(new View.OnGenericMotionListener() {
            @Override
            public boolean onGenericMotion(View v2, MotionEvent event) {
                callback.onInteraction();
                return false;
            }
        });
    }
}
