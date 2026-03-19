package android.hardware;

import android.annotation.SystemApi;
import android.os.Handler;
import android.os.MemoryFile;
import android.util.Log;
import android.util.SparseArray;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public abstract class SensorManager {
    public static final int AXIS_MINUS_X = 129;
    public static final int AXIS_MINUS_Y = 130;
    public static final int AXIS_MINUS_Z = 131;
    public static final int AXIS_X = 1;
    public static final int AXIS_Y = 2;
    public static final int AXIS_Z = 3;

    @Deprecated
    public static final int DATA_X = 0;

    @Deprecated
    public static final int DATA_Y = 1;

    @Deprecated
    public static final int DATA_Z = 2;
    public static final float GRAVITY_DEATH_STAR_I = 3.5303614E-7f;
    public static final float GRAVITY_EARTH = 9.80665f;
    public static final float GRAVITY_JUPITER = 23.12f;
    public static final float GRAVITY_MARS = 3.71f;
    public static final float GRAVITY_MERCURY = 3.7f;
    public static final float GRAVITY_MOON = 1.6f;
    public static final float GRAVITY_NEPTUNE = 11.0f;
    public static final float GRAVITY_PLUTO = 0.6f;
    public static final float GRAVITY_SATURN = 8.96f;
    public static final float GRAVITY_SUN = 275.0f;
    public static final float GRAVITY_THE_ISLAND = 4.815162f;
    public static final float GRAVITY_URANUS = 8.69f;
    public static final float GRAVITY_VENUS = 8.87f;
    public static final float LIGHT_CLOUDY = 100.0f;
    public static final float LIGHT_FULLMOON = 0.25f;
    public static final float LIGHT_NO_MOON = 0.001f;
    public static final float LIGHT_OVERCAST = 10000.0f;
    public static final float LIGHT_SHADE = 20000.0f;
    public static final float LIGHT_SUNLIGHT = 110000.0f;
    public static final float LIGHT_SUNLIGHT_MAX = 120000.0f;
    public static final float LIGHT_SUNRISE = 400.0f;
    public static final float MAGNETIC_FIELD_EARTH_MAX = 60.0f;
    public static final float MAGNETIC_FIELD_EARTH_MIN = 30.0f;
    public static final float PRESSURE_STANDARD_ATMOSPHERE = 1013.25f;

    @Deprecated
    public static final int RAW_DATA_INDEX = 3;

    @Deprecated
    public static final int RAW_DATA_X = 3;

    @Deprecated
    public static final int RAW_DATA_Y = 4;

    @Deprecated
    public static final int RAW_DATA_Z = 5;

    @Deprecated
    public static final int SENSOR_ACCELEROMETER = 2;

    @Deprecated
    public static final int SENSOR_ALL = 127;
    public static final int SENSOR_DELAY_FASTEST = 0;
    public static final int SENSOR_DELAY_GAME = 1;
    public static final int SENSOR_DELAY_NORMAL = 3;
    public static final int SENSOR_DELAY_UI = 2;

    @Deprecated
    public static final int SENSOR_LIGHT = 16;

    @Deprecated
    public static final int SENSOR_MAGNETIC_FIELD = 8;

    @Deprecated
    public static final int SENSOR_MAX = 64;

    @Deprecated
    public static final int SENSOR_MIN = 1;

    @Deprecated
    public static final int SENSOR_ORIENTATION = 1;

    @Deprecated
    public static final int SENSOR_ORIENTATION_RAW = 128;

    @Deprecated
    public static final int SENSOR_PROXIMITY = 32;
    public static final int SENSOR_STATUS_ACCURACY_HIGH = 3;
    public static final int SENSOR_STATUS_ACCURACY_LOW = 1;
    public static final int SENSOR_STATUS_ACCURACY_MEDIUM = 2;
    public static final int SENSOR_STATUS_NO_CONTACT = -1;
    public static final int SENSOR_STATUS_UNRELIABLE = 0;

    @Deprecated
    public static final int SENSOR_TEMPERATURE = 4;

    @Deprecated
    public static final int SENSOR_TRICORDER = 64;
    public static final float STANDARD_GRAVITY = 9.80665f;
    protected static final String TAG = "SensorManager";
    private static final float[] sTempMatrix = new float[16];
    private LegacySensorManager mLegacySensorManager;
    private final SparseArray<List<Sensor>> mSensorListByType = new SparseArray<>();

    protected abstract boolean cancelTriggerSensorImpl(TriggerEventListener triggerEventListener, Sensor sensor, boolean z);

    protected abstract int configureDirectChannelImpl(SensorDirectChannel sensorDirectChannel, Sensor sensor, int i);

    protected abstract SensorDirectChannel createDirectChannelImpl(MemoryFile memoryFile, HardwareBuffer hardwareBuffer);

    protected abstract void destroyDirectChannelImpl(SensorDirectChannel sensorDirectChannel);

    protected abstract boolean flushImpl(SensorEventListener sensorEventListener);

    protected abstract List<Sensor> getFullDynamicSensorList();

    protected abstract List<Sensor> getFullSensorList();

    protected abstract boolean initDataInjectionImpl(boolean z);

    protected abstract boolean injectSensorDataImpl(Sensor sensor, float[] fArr, int i, long j);

    protected abstract void registerDynamicSensorCallbackImpl(DynamicSensorCallback dynamicSensorCallback, Handler handler);

    protected abstract boolean registerListenerImpl(SensorEventListener sensorEventListener, Sensor sensor, int i, Handler handler, int i2, int i3);

    protected abstract boolean requestTriggerSensorImpl(TriggerEventListener triggerEventListener, Sensor sensor);

    protected abstract boolean setOperationParameterImpl(SensorAdditionalInfo sensorAdditionalInfo);

    protected abstract void unregisterDynamicSensorCallbackImpl(DynamicSensorCallback dynamicSensorCallback);

    protected abstract void unregisterListenerImpl(SensorEventListener sensorEventListener, Sensor sensor);

    @Deprecated
    public int getSensors() {
        return getLegacySensorManager().getSensors();
    }

    public List<Sensor> getSensorList(int i) {
        List<Sensor> listUnmodifiableList;
        List<Sensor> fullSensorList = getFullSensorList();
        synchronized (this.mSensorListByType) {
            listUnmodifiableList = this.mSensorListByType.get(i);
            if (listUnmodifiableList == null) {
                if (i != -1) {
                    ArrayList arrayList = new ArrayList();
                    for (Sensor sensor : fullSensorList) {
                        if (sensor.getType() == i) {
                            arrayList.add(sensor);
                        }
                    }
                    fullSensorList = arrayList;
                }
                listUnmodifiableList = Collections.unmodifiableList(fullSensorList);
                this.mSensorListByType.append(i, listUnmodifiableList);
            }
        }
        return listUnmodifiableList;
    }

    public List<Sensor> getDynamicSensorList(int i) {
        List<Sensor> fullDynamicSensorList = getFullDynamicSensorList();
        if (i == -1) {
            return Collections.unmodifiableList(fullDynamicSensorList);
        }
        ArrayList arrayList = new ArrayList();
        for (Sensor sensor : fullDynamicSensorList) {
            if (sensor.getType() == i) {
                arrayList.add(sensor);
            }
        }
        return Collections.unmodifiableList(arrayList);
    }

    public Sensor getDefaultSensor(int i) {
        boolean z;
        List<Sensor> sensorList = getSensorList(i);
        if (i == 8 || i == 17 || i == 22 || i == 23 || i == 24 || i == 25 || i == 26 || i == 32) {
            z = true;
        } else {
            z = false;
        }
        for (Sensor sensor : sensorList) {
            if (sensor.isWakeUpSensor() == z) {
                return sensor;
            }
        }
        return null;
    }

    public Sensor getDefaultSensor(int i, boolean z) {
        for (Sensor sensor : getSensorList(i)) {
            if (sensor.isWakeUpSensor() == z) {
                return sensor;
            }
        }
        return null;
    }

    @Deprecated
    public boolean registerListener(SensorListener sensorListener, int i) {
        return registerListener(sensorListener, i, 3);
    }

    @Deprecated
    public boolean registerListener(SensorListener sensorListener, int i, int i2) {
        return getLegacySensorManager().registerListener(sensorListener, i, i2);
    }

    @Deprecated
    public void unregisterListener(SensorListener sensorListener) {
        unregisterListener(sensorListener, 255);
    }

    @Deprecated
    public void unregisterListener(SensorListener sensorListener, int i) {
        getLegacySensorManager().unregisterListener(sensorListener, i);
    }

    public void unregisterListener(SensorEventListener sensorEventListener, Sensor sensor) {
        if (sensorEventListener == null || sensor == null) {
            return;
        }
        unregisterListenerImpl(sensorEventListener, sensor);
    }

    public void unregisterListener(SensorEventListener sensorEventListener) {
        if (sensorEventListener == null) {
            return;
        }
        unregisterListenerImpl(sensorEventListener, null);
    }

    public boolean registerListener(SensorEventListener sensorEventListener, Sensor sensor, int i) {
        return registerListener(sensorEventListener, sensor, i, (Handler) null);
    }

    public boolean registerListener(SensorEventListener sensorEventListener, Sensor sensor, int i, int i2) {
        return registerListenerImpl(sensorEventListener, sensor, getDelay(i), null, i2, 0);
    }

    public boolean registerListener(SensorEventListener sensorEventListener, Sensor sensor, int i, Handler handler) {
        return registerListenerImpl(sensorEventListener, sensor, getDelay(i), handler, 0, 0);
    }

    public boolean registerListener(SensorEventListener sensorEventListener, Sensor sensor, int i, int i2, Handler handler) {
        return registerListenerImpl(sensorEventListener, sensor, getDelay(i), handler, i2, 0);
    }

    public boolean flush(SensorEventListener sensorEventListener) {
        return flushImpl(sensorEventListener);
    }

    public SensorDirectChannel createDirectChannel(MemoryFile memoryFile) {
        return createDirectChannelImpl(memoryFile, null);
    }

    public SensorDirectChannel createDirectChannel(HardwareBuffer hardwareBuffer) {
        return createDirectChannelImpl(null, hardwareBuffer);
    }

    void destroyDirectChannel(SensorDirectChannel sensorDirectChannel) {
        destroyDirectChannelImpl(sensorDirectChannel);
    }

    @Deprecated
    public int configureDirectChannel(SensorDirectChannel sensorDirectChannel, Sensor sensor, int i) {
        return configureDirectChannelImpl(sensorDirectChannel, sensor, i);
    }

    public static abstract class DynamicSensorCallback {
        public void onDynamicSensorConnected(Sensor sensor) {
        }

        public void onDynamicSensorDisconnected(Sensor sensor) {
        }
    }

    public void registerDynamicSensorCallback(DynamicSensorCallback dynamicSensorCallback) {
        registerDynamicSensorCallback(dynamicSensorCallback, null);
    }

    public void registerDynamicSensorCallback(DynamicSensorCallback dynamicSensorCallback, Handler handler) {
        registerDynamicSensorCallbackImpl(dynamicSensorCallback, handler);
    }

    public void unregisterDynamicSensorCallback(DynamicSensorCallback dynamicSensorCallback) {
        unregisterDynamicSensorCallbackImpl(dynamicSensorCallback);
    }

    public boolean isDynamicSensorDiscoverySupported() {
        return getSensorList(32).size() > 0;
    }

    public static boolean getRotationMatrix(float[] fArr, float[] fArr2, float[] fArr3, float[] fArr4) {
        float f = fArr3[0];
        float f2 = fArr3[1];
        float f3 = fArr3[2];
        float f4 = (f * f) + (f2 * f2) + (f3 * f3);
        if (f4 < 0.96236104f) {
            return false;
        }
        float f5 = fArr4[0];
        float f6 = fArr4[1];
        float f7 = fArr4[2];
        float f8 = (f6 * f3) - (f7 * f2);
        float f9 = (f7 * f) - (f5 * f3);
        float f10 = (f5 * f2) - (f6 * f);
        float fSqrt = (float) Math.sqrt((f8 * f8) + (f9 * f9) + (f10 * f10));
        if (fSqrt < 0.1f) {
            return false;
        }
        float f11 = 1.0f / fSqrt;
        float f12 = f8 * f11;
        float f13 = f9 * f11;
        float f14 = f10 * f11;
        float fSqrt2 = 1.0f / ((float) Math.sqrt(f4));
        float f15 = f * fSqrt2;
        float f16 = f2 * fSqrt2;
        float f17 = f3 * fSqrt2;
        float f18 = (f16 * f14) - (f17 * f13);
        float f19 = (f17 * f12) - (f15 * f14);
        float f20 = (f15 * f13) - (f16 * f12);
        if (fArr != null) {
            if (fArr.length == 9) {
                fArr[0] = f12;
                fArr[1] = f13;
                fArr[2] = f14;
                fArr[3] = f18;
                fArr[4] = f19;
                fArr[5] = f20;
                fArr[6] = f15;
                fArr[7] = f16;
                fArr[8] = f17;
            } else if (fArr.length == 16) {
                fArr[0] = f12;
                fArr[1] = f13;
                fArr[2] = f14;
                fArr[3] = 0.0f;
                fArr[4] = f18;
                fArr[5] = f19;
                fArr[6] = f20;
                fArr[7] = 0.0f;
                fArr[8] = f15;
                fArr[9] = f16;
                fArr[10] = f17;
                fArr[11] = 0.0f;
                fArr[12] = 0.0f;
                fArr[13] = 0.0f;
                fArr[14] = 0.0f;
                fArr[15] = 1.0f;
            }
        }
        if (fArr2 != null) {
            float fSqrt3 = 1.0f / ((float) Math.sqrt(((f5 * f5) + (f6 * f6)) + (f7 * f7)));
            float f21 = ((f18 * f5) + (f19 * f6) + (f20 * f7)) * fSqrt3;
            float f22 = ((f5 * f15) + (f6 * f16) + (f7 * f17)) * fSqrt3;
            if (fArr2.length == 9) {
                fArr2[0] = 1.0f;
                fArr2[1] = 0.0f;
                fArr2[2] = 0.0f;
                fArr2[3] = 0.0f;
                fArr2[4] = f21;
                fArr2[5] = f22;
                fArr2[6] = 0.0f;
                fArr2[7] = -f22;
                fArr2[8] = f21;
                return true;
            }
            if (fArr2.length == 16) {
                fArr2[0] = 1.0f;
                fArr2[1] = 0.0f;
                fArr2[2] = 0.0f;
                fArr2[4] = 0.0f;
                fArr2[5] = f21;
                fArr2[6] = f22;
                fArr2[8] = 0.0f;
                fArr2[9] = -f22;
                fArr2[10] = f21;
                fArr2[14] = 0.0f;
                fArr2[13] = 0.0f;
                fArr2[12] = 0.0f;
                fArr2[11] = 0.0f;
                fArr2[7] = 0.0f;
                fArr2[3] = 0.0f;
                fArr2[15] = 1.0f;
                return true;
            }
            return true;
        }
        return true;
    }

    public static float getInclination(float[] fArr) {
        if (fArr.length == 9) {
            return (float) Math.atan2(fArr[5], fArr[4]);
        }
        return (float) Math.atan2(fArr[6], fArr[5]);
    }

    public static boolean remapCoordinateSystem(float[] fArr, int i, int i2, float[] fArr2) {
        if (fArr == fArr2) {
            float[] fArr3 = sTempMatrix;
            synchronized (fArr3) {
                if (remapCoordinateSystemImpl(fArr, i, i2, fArr3)) {
                    int length = fArr2.length;
                    for (int i3 = 0; i3 < length; i3++) {
                        fArr2[i3] = fArr3[i3];
                    }
                    return true;
                }
            }
        }
        return remapCoordinateSystemImpl(fArr, i, i2, fArr2);
    }

    private static boolean remapCoordinateSystemImpl(float[] fArr, int i, int i2, float[] fArr2) {
        int i3;
        int length = fArr2.length;
        int i4 = 0;
        if (fArr.length != length) {
            return false;
        }
        if ((i & 124) != 0 || (i2 & 124) != 0) {
            return false;
        }
        int i5 = i & 3;
        if (i5 == 0 || (i3 = i2 & 3) == 0 || i5 == i3) {
            return false;
        }
        int i6 = i ^ i2;
        int i7 = i5 - 1;
        int i8 = i3 - 1;
        int i9 = (i6 & 3) - 1;
        int i10 = 3;
        if (((((i9 + 1) % 3) ^ i7) | (((i9 + 2) % 3) ^ i8)) != 0) {
            i6 ^= 128;
        }
        boolean z = i >= 128;
        boolean z2 = i2 >= 128;
        boolean z3 = i6 >= 128;
        int i11 = length == 16 ? 4 : 3;
        int i12 = 0;
        while (i12 < i10) {
            int i13 = i12 * i11;
            while (i4 < i10) {
                if (i7 == i4) {
                    fArr2[i13 + i4] = z ? -fArr[i13 + 0] : fArr[i13 + 0];
                }
                if (i8 == i4) {
                    fArr2[i13 + i4] = z2 ? -fArr[i13 + 1] : fArr[i13 + 1];
                }
                if (i9 == i4) {
                    fArr2[i13 + i4] = z3 ? -fArr[i13 + 2] : fArr[i13 + 2];
                }
                i4++;
                i10 = 3;
            }
            i12++;
            i4 = 0;
            i10 = 3;
        }
        if (length == 16) {
            fArr2[14] = 0.0f;
            fArr2[13] = 0.0f;
            fArr2[12] = 0.0f;
            fArr2[11] = 0.0f;
            fArr2[7] = 0.0f;
            fArr2[3] = 0.0f;
            fArr2[15] = 1.0f;
            return true;
        }
        return true;
    }

    public static float[] getOrientation(float[] fArr, float[] fArr2) {
        if (fArr.length == 9) {
            fArr2[0] = (float) Math.atan2(fArr[1], fArr[4]);
            fArr2[1] = (float) Math.asin(-fArr[7]);
            fArr2[2] = (float) Math.atan2(-fArr[6], fArr[8]);
        } else {
            fArr2[0] = (float) Math.atan2(fArr[1], fArr[5]);
            fArr2[1] = (float) Math.asin(-fArr[9]);
            fArr2[2] = (float) Math.atan2(-fArr[8], fArr[10]);
        }
        return fArr2;
    }

    public static float getAltitude(float f, float f2) {
        return 44330.0f * (1.0f - ((float) Math.pow(f2 / f, 0.19029495120048523d)));
    }

    public static void getAngleChange(float[] fArr, float[] fArr2, float[] fArr3) {
        float f;
        float f2;
        float f3;
        float f4;
        float f5;
        float f6;
        float f7;
        float f8;
        float f9;
        float f10;
        float f11;
        float f12;
        float f13;
        float f14;
        float f15;
        float f16;
        float f17;
        float f18 = 0.0f;
        if (fArr2.length == 9) {
            f2 = fArr2[0];
            f3 = fArr2[1];
            f4 = fArr2[2];
            f5 = fArr2[3];
            f6 = fArr2[4];
            f7 = fArr2[5];
            f8 = fArr2[6];
            f9 = fArr2[7];
            f = fArr2[8];
        } else if (fArr2.length == 16) {
            f2 = fArr2[0];
            f3 = fArr2[1];
            f4 = fArr2[2];
            f5 = fArr2[4];
            f6 = fArr2[5];
            f7 = fArr2[6];
            f8 = fArr2[8];
            f9 = fArr2[9];
            f = fArr2[10];
        } else {
            f = 0.0f;
            f2 = 0.0f;
            f3 = 0.0f;
            f4 = 0.0f;
            f5 = 0.0f;
            f6 = 0.0f;
            f7 = 0.0f;
            f8 = 0.0f;
            f9 = 0.0f;
        }
        if (fArr3.length == 9) {
            f18 = fArr3[0];
            f11 = fArr3[1];
            f12 = fArr3[2];
            float f19 = fArr3[3];
            f17 = fArr3[4];
            float f20 = fArr3[5];
            float f21 = fArr3[6];
            float f22 = fArr3[7];
            f10 = fArr3[8];
            f14 = f21;
            f15 = f20;
            f16 = f22;
            f13 = f19;
        } else if (fArr3.length == 16) {
            f18 = fArr3[0];
            f11 = fArr3[1];
            f12 = fArr3[2];
            f13 = fArr3[4];
            float f23 = fArr3[5];
            f15 = fArr3[6];
            f14 = fArr3[8];
            f16 = fArr3[9];
            f10 = fArr3[10];
            f17 = f23;
        } else {
            f10 = 0.0f;
            f11 = 0.0f;
            f12 = 0.0f;
            f13 = 0.0f;
            f14 = 0.0f;
            f15 = 0.0f;
            f16 = 0.0f;
            f17 = 0.0f;
        }
        float f24 = (f3 * f12) + (f6 * f15) + (f9 * f10);
        fArr[0] = (float) Math.atan2((f18 * f3) + (f13 * f6) + (f14 * f9), (f11 * f3) + (f17 * f6) + (f16 * f9));
        fArr[1] = (float) Math.asin(-f24);
        fArr[2] = (float) Math.atan2(-((f2 * f12) + (f5 * f15) + (f8 * f10)), (f12 * f4) + (f15 * f7) + (f10 * f));
    }

    public static void getRotationMatrixFromVector(float[] fArr, float[] fArr2) {
        float fSqrt;
        float f = fArr2[0];
        float f2 = fArr2[1];
        float f3 = fArr2[2];
        if (fArr2.length >= 4) {
            fSqrt = fArr2[3];
        } else {
            float f4 = ((1.0f - (f * f)) - (f2 * f2)) - (f3 * f3);
            fSqrt = f4 > 0.0f ? (float) Math.sqrt(f4) : 0.0f;
        }
        float f5 = 2.0f * f;
        float f6 = f * f5;
        float f7 = 2.0f * f2;
        float f8 = f7 * f2;
        float f9 = 2.0f * f3;
        float f10 = f9 * f3;
        float f11 = f2 * f5;
        float f12 = f9 * fSqrt;
        float f13 = f5 * f3;
        float f14 = f7 * fSqrt;
        float f15 = f7 * f3;
        float f16 = f5 * fSqrt;
        if (fArr.length == 9) {
            fArr[0] = (1.0f - f8) - f10;
            fArr[1] = f11 - f12;
            fArr[2] = f13 + f14;
            fArr[3] = f11 + f12;
            float f17 = 1.0f - f6;
            fArr[4] = f17 - f10;
            fArr[5] = f15 - f16;
            fArr[6] = f13 - f14;
            fArr[7] = f15 + f16;
            fArr[8] = f17 - f8;
            return;
        }
        if (fArr.length == 16) {
            fArr[0] = (1.0f - f8) - f10;
            fArr[1] = f11 - f12;
            fArr[2] = f13 + f14;
            fArr[3] = 0.0f;
            fArr[4] = f11 + f12;
            float f18 = 1.0f - f6;
            fArr[5] = f18 - f10;
            fArr[6] = f15 - f16;
            fArr[7] = 0.0f;
            fArr[8] = f13 - f14;
            fArr[9] = f15 + f16;
            fArr[10] = f18 - f8;
            fArr[11] = 0.0f;
            fArr[14] = 0.0f;
            fArr[13] = 0.0f;
            fArr[12] = 0.0f;
            fArr[15] = 1.0f;
        }
    }

    public static void getQuaternionFromVector(float[] fArr, float[] fArr2) {
        if (fArr2.length >= 4) {
            fArr[0] = fArr2[3];
        } else {
            fArr[0] = ((1.0f - (fArr2[0] * fArr2[0])) - (fArr2[1] * fArr2[1])) - (fArr2[2] * fArr2[2]);
            fArr[0] = fArr[0] > 0.0f ? (float) Math.sqrt(fArr[0]) : 0.0f;
        }
        fArr[1] = fArr2[0];
        fArr[2] = fArr2[1];
        fArr[3] = fArr2[2];
    }

    public boolean requestTriggerSensor(TriggerEventListener triggerEventListener, Sensor sensor) {
        return requestTriggerSensorImpl(triggerEventListener, sensor);
    }

    public boolean cancelTriggerSensor(TriggerEventListener triggerEventListener, Sensor sensor) {
        return cancelTriggerSensorImpl(triggerEventListener, sensor, true);
    }

    @SystemApi
    public boolean initDataInjection(boolean z) {
        return initDataInjectionImpl(z);
    }

    @SystemApi
    public boolean injectSensorData(Sensor sensor, float[] fArr, int i, long j) {
        if (sensor == null) {
            throw new IllegalArgumentException("sensor cannot be null");
        }
        if (!sensor.isDataInjectionSupported()) {
            throw new IllegalArgumentException("sensor does not support data injection");
        }
        if (fArr == null) {
            throw new IllegalArgumentException("sensor data cannot be null");
        }
        int maxLengthValuesArray = Sensor.getMaxLengthValuesArray(sensor, 23);
        if (fArr.length != maxLengthValuesArray) {
            throw new IllegalArgumentException("Wrong number of values for sensor " + sensor.getName() + " actual=" + fArr.length + " expected=" + maxLengthValuesArray);
        }
        if (i < -1 || i > 3) {
            throw new IllegalArgumentException("Invalid sensor accuracy");
        }
        if (j <= 0) {
            throw new IllegalArgumentException("Negative or zero sensor timestamp");
        }
        return injectSensorDataImpl(sensor, fArr, i, j);
    }

    private LegacySensorManager getLegacySensorManager() {
        LegacySensorManager legacySensorManager;
        synchronized (this.mSensorListByType) {
            if (this.mLegacySensorManager == null) {
                Log.i(TAG, "This application is using deprecated SensorManager API which will be removed someday.  Please consider switching to the new API.");
                this.mLegacySensorManager = new LegacySensorManager(this);
            }
            legacySensorManager = this.mLegacySensorManager;
        }
        return legacySensorManager;
    }

    private static int getDelay(int i) {
        switch (i) {
            case 0:
                return 0;
            case 1:
                return 20000;
            case 2:
                return 66667;
            case 3:
                return 200000;
            default:
                return i;
        }
    }

    public boolean setOperationParameter(SensorAdditionalInfo sensorAdditionalInfo) {
        return setOperationParameterImpl(sensorAdditionalInfo);
    }
}
