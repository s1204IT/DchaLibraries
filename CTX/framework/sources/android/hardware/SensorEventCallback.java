package android.hardware;

public abstract class SensorEventCallback implements SensorEventListener2 {
    @Override
    public void onSensorChanged(SensorEvent sensorEvent) {
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int i) {
    }

    @Override
    public void onFlushCompleted(Sensor sensor) {
    }

    public void onSensorAdditionalInfo(SensorAdditionalInfo sensorAdditionalInfo) {
    }
}
