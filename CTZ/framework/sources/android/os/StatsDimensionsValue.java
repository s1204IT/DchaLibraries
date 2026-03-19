package android.os;

import android.annotation.SystemApi;
import android.os.Parcelable;
import android.provider.SettingsStringUtil;
import android.util.Slog;
import java.util.ArrayList;
import java.util.List;

@SystemApi
public final class StatsDimensionsValue implements Parcelable {
    public static final int BOOLEAN_VALUE_TYPE = 5;
    public static final Parcelable.Creator<StatsDimensionsValue> CREATOR = new Parcelable.Creator<StatsDimensionsValue>() {
        @Override
        public StatsDimensionsValue createFromParcel(Parcel parcel) {
            return new StatsDimensionsValue(parcel);
        }

        @Override
        public StatsDimensionsValue[] newArray(int i) {
            return new StatsDimensionsValue[i];
        }
    };
    public static final int FLOAT_VALUE_TYPE = 6;
    public static final int INT_VALUE_TYPE = 3;
    public static final int LONG_VALUE_TYPE = 4;
    public static final int STRING_VALUE_TYPE = 2;
    private static final String TAG = "StatsDimensionsValue";
    public static final int TUPLE_VALUE_TYPE = 7;
    private final int mField;
    private final Object mValue;
    private final int mValueType;

    public StatsDimensionsValue(Parcel parcel) {
        this.mField = parcel.readInt();
        this.mValueType = parcel.readInt();
        this.mValue = readValueFromParcel(this.mValueType, parcel);
    }

    public int getField() {
        return this.mField;
    }

    public String getStringValue() {
        try {
            if (this.mValueType == 2) {
                return (String) this.mValue;
            }
            return null;
        } catch (ClassCastException e) {
            Slog.w(TAG, "Failed to successfully get value", e);
            return null;
        }
    }

    public int getIntValue() {
        try {
            if (this.mValueType == 3) {
                return ((Integer) this.mValue).intValue();
            }
            return 0;
        } catch (ClassCastException e) {
            Slog.w(TAG, "Failed to successfully get value", e);
            return 0;
        }
    }

    public long getLongValue() {
        try {
            if (this.mValueType == 4) {
                return ((Long) this.mValue).longValue();
            }
            return 0L;
        } catch (ClassCastException e) {
            Slog.w(TAG, "Failed to successfully get value", e);
            return 0L;
        }
    }

    public boolean getBooleanValue() {
        try {
            if (this.mValueType == 5) {
                return ((Boolean) this.mValue).booleanValue();
            }
            return false;
        } catch (ClassCastException e) {
            Slog.w(TAG, "Failed to successfully get value", e);
            return false;
        }
    }

    public float getFloatValue() {
        try {
            if (this.mValueType == 6) {
                return ((Float) this.mValue).floatValue();
            }
            return 0.0f;
        } catch (ClassCastException e) {
            Slog.w(TAG, "Failed to successfully get value", e);
            return 0.0f;
        }
    }

    public List<StatsDimensionsValue> getTupleValueList() {
        if (this.mValueType != 7) {
            return null;
        }
        try {
            StatsDimensionsValue[] statsDimensionsValueArr = (StatsDimensionsValue[]) this.mValue;
            ArrayList arrayList = new ArrayList(statsDimensionsValueArr.length);
            for (StatsDimensionsValue statsDimensionsValue : statsDimensionsValueArr) {
                arrayList.add(statsDimensionsValue);
            }
            return arrayList;
        } catch (ClassCastException e) {
            Slog.w(TAG, "Failed to successfully get value", e);
            return null;
        }
    }

    public int getValueType() {
        return this.mValueType;
    }

    public boolean isValueType(int i) {
        return this.mValueType == i;
    }

    public String toString() {
        try {
            StringBuilder sb = new StringBuilder();
            sb.append(this.mField);
            sb.append(SettingsStringUtil.DELIMITER);
            if (this.mValueType == 7) {
                sb.append("{");
                for (StatsDimensionsValue statsDimensionsValue : (StatsDimensionsValue[]) this.mValue) {
                    sb.append(statsDimensionsValue.toString());
                    sb.append("|");
                }
                sb.append("}");
            } else {
                sb.append(this.mValue.toString());
            }
            return sb.toString();
        } catch (ClassCastException e) {
            Slog.w(TAG, "Failed to successfully get value", e);
            return "";
        }
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel parcel, int i) {
        parcel.writeInt(this.mField);
        parcel.writeInt(this.mValueType);
        writeValueToParcel(this.mValueType, this.mValue, parcel, i);
    }

    private static boolean writeValueToParcel(int i, Object obj, Parcel parcel, int i2) {
        try {
            switch (i) {
                case 2:
                    parcel.writeString((String) obj);
                    break;
                case 3:
                    parcel.writeInt(((Integer) obj).intValue());
                    break;
                case 4:
                    parcel.writeLong(((Long) obj).longValue());
                    break;
                case 5:
                    parcel.writeBoolean(((Boolean) obj).booleanValue());
                    break;
                case 6:
                    parcel.writeFloat(((Float) obj).floatValue());
                    break;
                case 7:
                    StatsDimensionsValue[] statsDimensionsValueArr = (StatsDimensionsValue[]) obj;
                    parcel.writeInt(statsDimensionsValueArr.length);
                    for (StatsDimensionsValue statsDimensionsValue : statsDimensionsValueArr) {
                        statsDimensionsValue.writeToParcel(parcel, i2);
                    }
                    break;
                default:
                    Slog.w(TAG, "readValue of an impossible type " + i);
                    break;
            }
            return true;
        } catch (ClassCastException e) {
            Slog.w(TAG, "writeValue cast failed", e);
            return false;
        }
    }

    private static Object readValueFromParcel(int i, Parcel parcel) {
        switch (i) {
            case 2:
                return parcel.readString();
            case 3:
                return Integer.valueOf(parcel.readInt());
            case 4:
                return Long.valueOf(parcel.readLong());
            case 5:
                return Boolean.valueOf(parcel.readBoolean());
            case 6:
                return Float.valueOf(parcel.readFloat());
            case 7:
                int i2 = parcel.readInt();
                StatsDimensionsValue[] statsDimensionsValueArr = new StatsDimensionsValue[i2];
                for (int i3 = 0; i3 < i2; i3++) {
                    statsDimensionsValueArr[i3] = new StatsDimensionsValue(parcel);
                }
                return statsDimensionsValueArr;
            default:
                Slog.w(TAG, "readValue of an impossible type " + i);
                return null;
        }
    }
}
