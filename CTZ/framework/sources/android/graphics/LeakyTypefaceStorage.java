package android.graphics;

import android.os.Parcel;
import android.os.Process;
import android.util.ArrayMap;
import com.android.internal.annotations.GuardedBy;
import java.util.ArrayList;

public class LeakyTypefaceStorage {
    private static final Object sLock = new Object();

    @GuardedBy("sLock")
    private static final ArrayList<Typeface> sStorage = new ArrayList<>();

    @GuardedBy("sLock")
    private static final ArrayMap<Typeface, Integer> sTypefaceMap = new ArrayMap<>();

    public static void writeTypefaceToParcel(Typeface typeface, Parcel parcel) {
        int iIntValue;
        parcel.writeInt(Process.myPid());
        synchronized (sLock) {
            Integer num = sTypefaceMap.get(typeface);
            if (num != null) {
                iIntValue = num.intValue();
            } else {
                int size = sStorage.size();
                sStorage.add(typeface);
                sTypefaceMap.put(typeface, Integer.valueOf(size));
                iIntValue = size;
            }
            parcel.writeInt(iIntValue);
        }
    }

    public static Typeface readTypefaceFromParcel(Parcel parcel) {
        Typeface typeface;
        int i = parcel.readInt();
        int i2 = parcel.readInt();
        if (i != Process.myPid()) {
            return null;
        }
        synchronized (sLock) {
            typeface = sStorage.get(i2);
        }
        return typeface;
    }
}
