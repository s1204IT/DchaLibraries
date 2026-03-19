package com.mediatek.camera.gesture;

import android.content.Context;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import android.view.View;
import com.mediatek.camera.common.IAppUiListener;
import com.mediatek.camera.common.debug.LogHelper;
import com.mediatek.camera.common.debug.LogUtil;
import com.mediatek.camera.common.utils.PriorityConcurrentSkipListMap;
import com.mediatek.camera.gesture.GestureRecognizer;
import java.util.Iterator;
import java.util.Map;

public class GestureManager {
    private static final LogUtil.Tag TAG = new LogUtil.Tag(GestureManager.class.getSimpleName());
    private GestureNotifier mGestureNotifier;
    private GestureRecognizer mGestureRecognizer;
    private PriorityConcurrentSkipListMap<Integer, IAppUiListener.OnGestureListener> mGestureListeners = new PriorityConcurrentSkipListMap<>(true);
    private View.OnTouchListener mTouchListener = new View.OnTouchListener() {
        @Override
        public boolean onTouch(View view, MotionEvent motionEvent) {
            GestureManager.this.mGestureRecognizer.onTouchEvent(motionEvent);
            return true;
        }
    };

    public GestureManager(Context context) {
        this.mGestureRecognizer = new GestureRecognizer(context, new GestureListenerImpl());
        this.mGestureNotifier = new GestureNotifier();
    }

    public void registerGestureListener(IAppUiListener.OnGestureListener onGestureListener, int i) {
        if (onGestureListener == null) {
            LogHelper.e(TAG, "registerGestureListener error [why null]");
        }
        PriorityConcurrentSkipListMap<Integer, IAppUiListener.OnGestureListener> priorityConcurrentSkipListMap = this.mGestureListeners;
        PriorityConcurrentSkipListMap<Integer, IAppUiListener.OnGestureListener> priorityConcurrentSkipListMap2 = this.mGestureListeners;
        priorityConcurrentSkipListMap.put((Integer) PriorityConcurrentSkipListMap.getPriorityKey(i, onGestureListener), onGestureListener);
    }

    public void unregisterGestureListener(IAppUiListener.OnGestureListener onGestureListener) {
        if (onGestureListener == null) {
            LogHelper.e(TAG, "unregisterGestureListener error [why null]");
        }
        if (this.mGestureListeners.containsValue(onGestureListener)) {
            this.mGestureListeners.remove(this.mGestureListeners.findKey(onGestureListener));
        }
    }

    public View.OnTouchListener getOnTouchListener() {
        return this.mTouchListener;
    }

    private class GestureNotifier implements IAppUiListener.OnGestureListener {
        private GestureNotifier() {
        }

        @Override
        public boolean onDown(MotionEvent motionEvent) {
            Iterator it = GestureManager.this.mGestureListeners.entrySet().iterator();
            while (it.hasNext()) {
                IAppUiListener.OnGestureListener onGestureListener = (IAppUiListener.OnGestureListener) ((Map.Entry) it.next()).getValue();
                if (onGestureListener != null && onGestureListener.onDown(motionEvent)) {
                    return true;
                }
            }
            return false;
        }

        @Override
        public boolean onUp(MotionEvent motionEvent) {
            Iterator it = GestureManager.this.mGestureListeners.entrySet().iterator();
            while (it.hasNext()) {
                IAppUiListener.OnGestureListener onGestureListener = (IAppUiListener.OnGestureListener) ((Map.Entry) it.next()).getValue();
                if (onGestureListener != null && onGestureListener.onUp(motionEvent)) {
                    return true;
                }
            }
            return false;
        }

        @Override
        public boolean onFling(MotionEvent motionEvent, MotionEvent motionEvent2, float f, float f2) {
            Iterator it = GestureManager.this.mGestureListeners.entrySet().iterator();
            while (it.hasNext()) {
                IAppUiListener.OnGestureListener onGestureListener = (IAppUiListener.OnGestureListener) ((Map.Entry) it.next()).getValue();
                if (onGestureListener != null && onGestureListener.onFling(motionEvent, motionEvent2, f, f2)) {
                    return true;
                }
            }
            return false;
        }

        @Override
        public boolean onScroll(MotionEvent motionEvent, MotionEvent motionEvent2, float f, float f2) {
            Iterator it = GestureManager.this.mGestureListeners.entrySet().iterator();
            while (it.hasNext()) {
                IAppUiListener.OnGestureListener onGestureListener = (IAppUiListener.OnGestureListener) ((Map.Entry) it.next()).getValue();
                if (onGestureListener != null && onGestureListener.onScroll(motionEvent, motionEvent2, f, f2)) {
                    return true;
                }
            }
            return false;
        }

        @Override
        public boolean onSingleTapUp(float f, float f2) {
            Iterator it = GestureManager.this.mGestureListeners.entrySet().iterator();
            while (it.hasNext()) {
                IAppUiListener.OnGestureListener onGestureListener = (IAppUiListener.OnGestureListener) ((Map.Entry) it.next()).getValue();
                if (onGestureListener != null && onGestureListener.onSingleTapUp(f, f2)) {
                    return true;
                }
            }
            return false;
        }

        @Override
        public boolean onSingleTapConfirmed(float f, float f2) {
            Iterator it = GestureManager.this.mGestureListeners.entrySet().iterator();
            while (it.hasNext()) {
                IAppUiListener.OnGestureListener onGestureListener = (IAppUiListener.OnGestureListener) ((Map.Entry) it.next()).getValue();
                if (onGestureListener != null && onGestureListener.onSingleTapConfirmed(f, f2)) {
                    return true;
                }
            }
            return false;
        }

        @Override
        public boolean onDoubleTap(float f, float f2) {
            Iterator it = GestureManager.this.mGestureListeners.entrySet().iterator();
            while (it.hasNext()) {
                IAppUiListener.OnGestureListener onGestureListener = (IAppUiListener.OnGestureListener) ((Map.Entry) it.next()).getValue();
                if (onGestureListener != null && onGestureListener.onDoubleTap(f, f2)) {
                    return true;
                }
            }
            return false;
        }

        @Override
        public boolean onScale(ScaleGestureDetector scaleGestureDetector) {
            Iterator it = GestureManager.this.mGestureListeners.entrySet().iterator();
            while (it.hasNext()) {
                IAppUiListener.OnGestureListener onGestureListener = (IAppUiListener.OnGestureListener) ((Map.Entry) it.next()).getValue();
                if (onGestureListener != null && onGestureListener.onScale(scaleGestureDetector)) {
                    return true;
                }
            }
            return true;
        }

        @Override
        public boolean onScaleBegin(ScaleGestureDetector scaleGestureDetector) {
            Iterator it = GestureManager.this.mGestureListeners.entrySet().iterator();
            while (it.hasNext()) {
                IAppUiListener.OnGestureListener onGestureListener = (IAppUiListener.OnGestureListener) ((Map.Entry) it.next()).getValue();
                if (onGestureListener != null && onGestureListener.onScaleBegin(scaleGestureDetector)) {
                    return true;
                }
            }
            return true;
        }

        @Override
        public boolean onScaleEnd(ScaleGestureDetector scaleGestureDetector) {
            Iterator it = GestureManager.this.mGestureListeners.entrySet().iterator();
            while (it.hasNext()) {
                IAppUiListener.OnGestureListener onGestureListener = (IAppUiListener.OnGestureListener) ((Map.Entry) it.next()).getValue();
                if (onGestureListener != null && onGestureListener.onScaleEnd(scaleGestureDetector)) {
                    return true;
                }
            }
            return true;
        }

        @Override
        public boolean onLongPress(float f, float f2) {
            Iterator it = GestureManager.this.mGestureListeners.entrySet().iterator();
            while (it.hasNext()) {
                IAppUiListener.OnGestureListener onGestureListener = (IAppUiListener.OnGestureListener) ((Map.Entry) it.next()).getValue();
                if (onGestureListener != null && onGestureListener.onLongPress(f, f2)) {
                    return true;
                }
            }
            return false;
        }
    }

    private class GestureListenerImpl implements GestureRecognizer.Listener {
        private GestureListenerImpl() {
        }

        @Override
        public boolean onSingleTapConfirmed(MotionEvent motionEvent) {
            return GestureManager.this.mGestureNotifier.onSingleTapConfirmed(motionEvent.getX(), motionEvent.getY());
        }

        @Override
        public boolean onDoubleTap(MotionEvent motionEvent) {
            return GestureManager.this.mGestureNotifier.onDoubleTap(motionEvent.getX(), motionEvent.getY());
        }

        @Override
        public boolean onDoubleTapEvent(MotionEvent motionEvent) {
            return GestureManager.this.mGestureNotifier.onDoubleTap(motionEvent.getX(), motionEvent.getY());
        }

        @Override
        public boolean onDown(MotionEvent motionEvent) {
            return GestureManager.this.mGestureNotifier.onDown(motionEvent);
        }

        @Override
        public void onShowPress(MotionEvent motionEvent) {
        }

        @Override
        public boolean onSingleTapUp(MotionEvent motionEvent) {
            return GestureManager.this.mGestureNotifier.onSingleTapUp(motionEvent.getX(), motionEvent.getY());
        }

        @Override
        public boolean onScroll(MotionEvent motionEvent, MotionEvent motionEvent2, float f, float f2) {
            return GestureManager.this.mGestureNotifier.onScroll(motionEvent, motionEvent2, f, f2);
        }

        @Override
        public void onLongPress(MotionEvent motionEvent) {
            GestureManager.this.mGestureNotifier.onLongPress(motionEvent.getX(), motionEvent.getY());
        }

        @Override
        public boolean onFling(MotionEvent motionEvent, MotionEvent motionEvent2, float f, float f2) {
            return GestureManager.this.mGestureNotifier.onFling(motionEvent, motionEvent2, f, f2);
        }

        @Override
        public boolean onScale(ScaleGestureDetector scaleGestureDetector) {
            return GestureManager.this.mGestureNotifier.onScale(scaleGestureDetector);
        }

        @Override
        public boolean onScaleBegin(ScaleGestureDetector scaleGestureDetector) {
            return GestureManager.this.mGestureNotifier.onScaleBegin(scaleGestureDetector);
        }

        @Override
        public void onScaleEnd(ScaleGestureDetector scaleGestureDetector) {
            GestureManager.this.mGestureNotifier.onScaleEnd(scaleGestureDetector);
        }

        @Override
        public void onUpEvent(MotionEvent motionEvent) {
            GestureManager.this.mGestureNotifier.onUp(motionEvent);
        }

        @Override
        public void onDownEvent(MotionEvent motionEvent) {
            GestureManager.this.mGestureNotifier.onDown(motionEvent);
        }
    }
}
