package android.view;

import android.content.Context;
import android.hardware.SensorListener;

@Deprecated
public abstract class OrientationListener implements SensorListener {
    public static final int ORIENTATION_UNKNOWN = -1;
    private OrientationEventListener mOrientationEventLis;

    public abstract void onOrientationChanged(int i);

    public OrientationListener(Context context) {
        this.mOrientationEventLis = new OrientationEventListenerInternal(context);
    }

    public OrientationListener(Context context, int i) {
        this.mOrientationEventLis = new OrientationEventListenerInternal(context, i);
    }

    class OrientationEventListenerInternal extends OrientationEventListener {
        OrientationEventListenerInternal(Context context) {
            super(context);
        }

        OrientationEventListenerInternal(Context context, int i) {
            super(context, i);
            registerListener(OrientationListener.this);
        }

        @Override
        public void onOrientationChanged(int i) {
            OrientationListener.this.onOrientationChanged(i);
        }
    }

    public void enable() {
        this.mOrientationEventLis.enable();
    }

    public void disable() {
        this.mOrientationEventLis.disable();
    }

    @Override
    public void onAccuracyChanged(int i, int i2) {
    }

    @Override
    public void onSensorChanged(int i, float[] fArr) {
    }
}
