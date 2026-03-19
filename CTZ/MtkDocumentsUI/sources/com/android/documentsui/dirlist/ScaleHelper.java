package com.android.documentsui.dirlist;

import android.content.Context;
import android.os.Build;
import android.support.v4.util.Preconditions;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import com.android.documentsui.base.Features;
import com.android.documentsui.base.SharedMinimal;
import java.util.function.Consumer;

final class ScaleHelper {
    private final Context mContext;
    private final Features mFeatures;
    private final Consumer<Float> mScaleCallback;
    private ScaleGestureDetector mScaleDetector;

    public ScaleHelper(Context context, Features features, Consumer<Float> consumer) {
        this.mContext = context;
        this.mFeatures = features;
        this.mScaleCallback = consumer;
    }

    private boolean onInterceptTouchEvent(RecyclerView recyclerView, MotionEvent motionEvent) {
        if (this.mFeatures.isGestureScaleEnabled() && this.mScaleDetector != null) {
            this.mScaleDetector.onTouchEvent(motionEvent);
            return false;
        }
        return false;
    }

    void attach(RecyclerView recyclerView) {
        Preconditions.checkState(Build.IS_DEBUGGABLE);
        Preconditions.checkState(this.mScaleDetector == null);
        this.mScaleDetector = new ScaleGestureDetector(this.mContext, new ScaleGestureDetector.SimpleOnScaleGestureListener() {
            @Override
            public boolean onScale(ScaleGestureDetector scaleGestureDetector) {
                if (SharedMinimal.VERBOSE) {
                    Log.v("ScaleHelper", "Received scale event: " + scaleGestureDetector.getScaleFactor());
                }
                ScaleHelper.this.mScaleCallback.accept(Float.valueOf(scaleGestureDetector.getScaleFactor()));
                return true;
            }
        });
        recyclerView.addOnItemTouchListener(new RecyclerView.OnItemTouchListener() {
            @Override
            public boolean onInterceptTouchEvent(RecyclerView recyclerView2, MotionEvent motionEvent) {
                return ScaleHelper.this.onInterceptTouchEvent(recyclerView2, motionEvent);
            }

            @Override
            public void onTouchEvent(RecyclerView recyclerView2, MotionEvent motionEvent) {
            }

            @Override
            public void onRequestDisallowInterceptTouchEvent(boolean z) {
            }
        });
    }
}
