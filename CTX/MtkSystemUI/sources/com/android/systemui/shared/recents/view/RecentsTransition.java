package com.android.systemui.shared.recents.view;

import android.app.ActivityOptions;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.os.Bundle;
import android.os.Handler;
import android.os.IRemoteCallback;
import android.os.RemoteException;
import android.view.DisplayListCanvas;
import android.view.RenderNode;
import android.view.ThreadedRenderer;
import android.view.View;
import java.util.function.Consumer;

public class RecentsTransition {
    public static ActivityOptions createAspectScaleAnimation(Context context, Handler handler, boolean z, AppTransitionAnimationSpecsFuture appTransitionAnimationSpecsFuture, final Runnable runnable) {
        return ActivityOptions.makeMultiThumbFutureAspectScaleAnimation(context, handler, appTransitionAnimationSpecsFuture != null ? appTransitionAnimationSpecsFuture.getFuture() : null, new ActivityOptions.OnAnimationStartedListener() {
            private boolean mHandled;

            public void onAnimationStarted() {
                if (this.mHandled) {
                    return;
                }
                this.mHandled = true;
                if (runnable != null) {
                    runnable.run();
                }
            }
        }, z);
    }

    public static IRemoteCallback wrapStartedListener(final Handler handler, final Runnable runnable) {
        if (runnable == null) {
            return null;
        }
        return new IRemoteCallback.Stub() {
            public void sendResult(Bundle bundle) throws RemoteException {
                handler.post(runnable);
            }
        };
    }

    public static Bitmap drawViewIntoHardwareBitmap(int i, int i2, final View view, final float f, final int i3) {
        return createHardwareBitmap(i, i2, new Consumer<Canvas>() {
            @Override
            public void accept(Canvas canvas) {
                canvas.scale(f, f);
                if (i3 != 0) {
                    canvas.drawColor(i3);
                }
                if (view != null) {
                    view.draw(canvas);
                }
            }
        });
    }

    public static Bitmap createHardwareBitmap(int i, int i2, Consumer<Canvas> consumer) {
        RenderNode renderNodeCreate = RenderNode.create("RecentsTransition", (View) null);
        renderNodeCreate.setLeftTopRightBottom(0, 0, i, i2);
        renderNodeCreate.setClipToBounds(false);
        DisplayListCanvas displayListCanvasStart = renderNodeCreate.start(i, i2);
        consumer.accept(displayListCanvasStart);
        renderNodeCreate.end(displayListCanvasStart);
        return ThreadedRenderer.createHardwareBitmap(renderNodeCreate, i, i2);
    }
}
