package android.os.health;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.lang.reflect.Field;
import java.util.Arrays;

public class HealthKeys {
    public static final int BASE_PACKAGE = 40000;
    public static final int BASE_PID = 20000;
    public static final int BASE_PROCESS = 30000;
    public static final int BASE_SERVICE = 50000;
    public static final int BASE_UID = 10000;
    public static final int TYPE_COUNT = 5;
    public static final int TYPE_MEASUREMENT = 1;
    public static final int TYPE_MEASUREMENTS = 4;
    public static final int TYPE_STATS = 2;
    public static final int TYPE_TIMER = 0;
    public static final int TYPE_TIMERS = 3;
    public static final int UNKNOWN_KEY = 0;

    @Target({ElementType.FIELD})
    @Retention(RetentionPolicy.RUNTIME)
    public @interface Constant {
        int type();
    }

    public static class Constants {
        private final String mDataType;
        private final int[][] mKeys = new int[5][];

        public Constants(Class cls) {
            this.mDataType = cls.getSimpleName();
            Field[] declaredFields = cls.getDeclaredFields();
            int length = declaredFields.length;
            SortedIntArray[] sortedIntArrayArr = new SortedIntArray[this.mKeys.length];
            for (int i = 0; i < sortedIntArrayArr.length; i++) {
                sortedIntArrayArr[i] = new SortedIntArray(length);
            }
            for (Field field : declaredFields) {
                Constant constant = (Constant) field.getAnnotation(Constant.class);
                if (constant != null) {
                    int iType = constant.type();
                    if (iType >= sortedIntArrayArr.length) {
                        throw new RuntimeException("Unknown Constant type " + iType + " on " + field);
                    }
                    try {
                        sortedIntArrayArr[iType].addValue(field.getInt(null));
                    } catch (IllegalAccessException e) {
                        throw new RuntimeException("Can't read constant value type=" + iType + " field=" + field, e);
                    }
                }
            }
            for (int i2 = 0; i2 < sortedIntArrayArr.length; i2++) {
                this.mKeys[i2] = sortedIntArrayArr[i2].getArray();
            }
        }

        public String getDataType() {
            return this.mDataType;
        }

        public int getSize(int i) {
            return this.mKeys[i].length;
        }

        public int getIndex(int i, int i2) {
            int iBinarySearch = Arrays.binarySearch(this.mKeys[i], i2);
            if (iBinarySearch >= 0) {
                return iBinarySearch;
            }
            throw new RuntimeException("Unknown Constant " + i2 + " (of type " + i + " )");
        }

        public int[] getKeys(int i) {
            return this.mKeys[i];
        }
    }

    private static class SortedIntArray {
        int[] mArray;
        int mCount;

        SortedIntArray(int i) {
            this.mArray = new int[i];
        }

        void addValue(int i) {
            int[] iArr = this.mArray;
            int i2 = this.mCount;
            this.mCount = i2 + 1;
            iArr[i2] = i;
        }

        int[] getArray() {
            if (this.mCount == this.mArray.length) {
                Arrays.sort(this.mArray);
                return this.mArray;
            }
            int[] iArr = new int[this.mCount];
            System.arraycopy(this.mArray, 0, iArr, 0, this.mCount);
            Arrays.sort(iArr);
            return iArr;
        }
    }
}
