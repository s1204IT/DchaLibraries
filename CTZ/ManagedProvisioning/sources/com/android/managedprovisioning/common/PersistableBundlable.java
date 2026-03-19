package com.android.managedprovisioning.common;

import android.os.Parcel;
import android.os.Parcelable;
import android.os.PersistableBundle;
import android.text.TextUtils;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Objects;

public abstract class PersistableBundlable implements Parcelable {
    public abstract PersistableBundle toPersistableBundle();

    public static PersistableBundle getPersistableBundleFromParcel(Parcel parcel) {
        return (PersistableBundle) parcel.readParcelable(PersistableBundle.class.getClassLoader());
    }

    public boolean equals(Object obj) {
        return isPersistableBundlableEquals(this, obj);
    }

    public int hashCode() {
        ArrayList arrayList = new ArrayList(toPersistableBundle().keySet());
        Collections.sort(arrayList);
        return TextUtils.join(",", arrayList).hashCode();
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel parcel, int i) {
        parcel.writeParcelable(toPersistableBundle(), i);
    }

    private static boolean isPersistableBundlableEquals(PersistableBundlable persistableBundlable, Object obj) {
        if (persistableBundlable == obj) {
            return true;
        }
        if (obj == null || persistableBundlable.getClass() != obj.getClass()) {
            return false;
        }
        return isPersistableBundleEquals(persistableBundlable.toPersistableBundle(), ((PersistableBundlable) obj).toPersistableBundle());
    }

    private static boolean isPersistableBundleEquals(PersistableBundle persistableBundle, PersistableBundle persistableBundle2) {
        if (persistableBundle == persistableBundle2) {
            return true;
        }
        if (persistableBundle == null || persistableBundle2 == null || persistableBundle.size() != persistableBundle2.size()) {
            return false;
        }
        for (String str : persistableBundle.keySet()) {
            if (!isPersistableBundleSupportedValueEquals(persistableBundle.get(str), persistableBundle2.get(str))) {
                return false;
            }
        }
        return true;
    }

    private static boolean isPersistableBundleSupportedValueEquals(Object obj, Object obj2) {
        if (obj == obj2) {
            return true;
        }
        if (obj == null || obj2 == null || !obj.getClass().equals(obj2.getClass())) {
            return false;
        }
        if (obj instanceof PersistableBundle) {
            return isPersistableBundleEquals((PersistableBundle) obj, (PersistableBundle) obj2);
        }
        if (obj instanceof int[]) {
            return Arrays.equals((int[]) obj, (int[]) obj2);
        }
        if (obj instanceof long[]) {
            return Arrays.equals((long[]) obj, (long[]) obj2);
        }
        if (obj instanceof double[]) {
            return Arrays.equals((double[]) obj, (double[]) obj2);
        }
        if (obj instanceof boolean[]) {
            return Arrays.equals((boolean[]) obj, (boolean[]) obj2);
        }
        if (obj instanceof String[]) {
            return Arrays.equals((String[]) obj, (String[]) obj2);
        }
        return Objects.equals(obj, obj2);
    }
}
