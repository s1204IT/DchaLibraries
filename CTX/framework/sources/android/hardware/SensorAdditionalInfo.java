package android.hardware;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

public class SensorAdditionalInfo {
    public static final int TYPE_CUSTOM_INFO = 268435456;
    public static final int TYPE_DEBUG_INFO = 1073741824;
    public static final int TYPE_DOCK_STATE = 196610;
    public static final int TYPE_FRAME_BEGIN = 0;
    public static final int TYPE_FRAME_END = 1;
    public static final int TYPE_HIGH_PERFORMANCE_MODE = 196611;
    public static final int TYPE_INTERNAL_TEMPERATURE = 65537;
    public static final int TYPE_LOCAL_GEOMAGNETIC_FIELD = 196608;
    public static final int TYPE_LOCAL_GRAVITY = 196609;
    public static final int TYPE_MAGNETIC_FIELD_CALIBRATION = 196612;
    public static final int TYPE_SAMPLING = 65540;
    public static final int TYPE_SENSOR_PLACEMENT = 65539;
    public static final int TYPE_UNTRACKED_DELAY = 65536;
    public static final int TYPE_VEC3_CALIBRATION = 65538;
    public final float[] floatValues;
    public final int[] intValues;
    public final Sensor sensor;
    public final int serial;
    public final int type;

    @Retention(RetentionPolicy.SOURCE)
    public @interface AdditionalInfoType {
    }

    SensorAdditionalInfo(Sensor sensor, int i, int i2, int[] iArr, float[] fArr) {
        this.sensor = sensor;
        this.type = i;
        this.serial = i2;
        this.intValues = iArr;
        this.floatValues = fArr;
    }

    public static SensorAdditionalInfo createLocalGeomagneticField(float f, float f2, float f3) {
        if (f >= 10.0f && f <= 100.0f && f2 >= 0.0f && f2 <= 3.141592653589793d) {
            double d = f3;
            if (d >= -1.5707963267948966d && d <= 1.5707963267948966d) {
                return new SensorAdditionalInfo(null, 196608, 0, null, new float[]{f, f2, f3});
            }
        }
        throw new IllegalArgumentException("Geomagnetic field info out of range");
    }

    public static SensorAdditionalInfo createCustomInfo(Sensor sensor, int i, float[] fArr) {
        if (i < 268435456 || i >= 1073741824 || sensor == null) {
            throw new IllegalArgumentException("invalid parameter(s): type: " + i + "; sensor: " + sensor);
        }
        return new SensorAdditionalInfo(sensor, i, 0, null, fArr);
    }
}
