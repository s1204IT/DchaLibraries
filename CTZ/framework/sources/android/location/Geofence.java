package android.location;

import android.os.Parcel;
import android.os.Parcelable;

public final class Geofence implements Parcelable {
    public static final Parcelable.Creator<Geofence> CREATOR = new Parcelable.Creator<Geofence>() {
        @Override
        public Geofence createFromParcel(Parcel parcel) {
            int i = parcel.readInt();
            double d = parcel.readDouble();
            double d2 = parcel.readDouble();
            float f = parcel.readFloat();
            Geofence.checkType(i);
            return Geofence.createCircle(d, d2, f);
        }

        @Override
        public Geofence[] newArray(int i) {
            return new Geofence[i];
        }
    };
    public static final int TYPE_HORIZONTAL_CIRCLE = 1;
    private final double mLatitude;
    private final double mLongitude;
    private final float mRadius;
    private final int mType;

    public static Geofence createCircle(double d, double d2, float f) {
        return new Geofence(d, d2, f);
    }

    private Geofence(double d, double d2, float f) {
        checkRadius(f);
        checkLatLong(d, d2);
        this.mType = 1;
        this.mLatitude = d;
        this.mLongitude = d2;
        this.mRadius = f;
    }

    public int getType() {
        return this.mType;
    }

    public double getLatitude() {
        return this.mLatitude;
    }

    public double getLongitude() {
        return this.mLongitude;
    }

    public float getRadius() {
        return this.mRadius;
    }

    private static void checkRadius(float f) {
        if (f <= 0.0f) {
            throw new IllegalArgumentException("invalid radius: " + f);
        }
    }

    private static void checkLatLong(double d, double d2) {
        if (d > 90.0d || d < -90.0d) {
            throw new IllegalArgumentException("invalid latitude: " + d);
        }
        if (d2 > 180.0d || d2 < -180.0d) {
            throw new IllegalArgumentException("invalid longitude: " + d2);
        }
    }

    private static void checkType(int i) {
        if (i != 1) {
            throw new IllegalArgumentException("invalid type: " + i);
        }
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel parcel, int i) {
        parcel.writeInt(this.mType);
        parcel.writeDouble(this.mLatitude);
        parcel.writeDouble(this.mLongitude);
        parcel.writeFloat(this.mRadius);
    }

    private static String typeToString(int i) {
        if (i == 1) {
            return "CIRCLE";
        }
        checkType(i);
        return null;
    }

    public String toString() {
        return String.format("Geofence[%s %.6f, %.6f %.0fm]", typeToString(this.mType), Double.valueOf(this.mLatitude), Double.valueOf(this.mLongitude), Float.valueOf(this.mRadius));
    }

    public int hashCode() {
        long jDoubleToLongBits = Double.doubleToLongBits(this.mLatitude);
        int i = ((int) (jDoubleToLongBits ^ (jDoubleToLongBits >>> 32))) + 31;
        long jDoubleToLongBits2 = Double.doubleToLongBits(this.mLongitude);
        return (31 * ((((i * 31) + ((int) (jDoubleToLongBits2 ^ (jDoubleToLongBits2 >>> 32)))) * 31) + Float.floatToIntBits(this.mRadius))) + this.mType;
    }

    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null || !(obj instanceof Geofence)) {
            return false;
        }
        Geofence geofence = (Geofence) obj;
        if (this.mRadius == geofence.mRadius && this.mLatitude == geofence.mLatitude && this.mLongitude == geofence.mLongitude && this.mType == geofence.mType) {
            return true;
        }
        return false;
    }
}
