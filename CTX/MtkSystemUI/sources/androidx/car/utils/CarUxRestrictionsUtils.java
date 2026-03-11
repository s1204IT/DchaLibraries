package androidx.car.utils;

import android.car.drivingstate.CarUxRestrictions;
import android.content.Context;
import android.text.InputFilter;
import android.widget.TextView;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class CarUxRestrictionsUtils {
    private static InputFilter sStringLengthFilter;

    public static void apply(Context context, CarUxRestrictions carUxRestrictions, TextView tv) {
        if (sStringLengthFilter == null) {
            int lengthLimit = carUxRestrictions.getMaxRestrictedStringLength();
            sStringLengthFilter = new InputFilter.LengthFilter(lengthLimit);
        }
        int activeUxr = carUxRestrictions.getActiveRestrictions();
        List<InputFilter> filters = Arrays.asList(tv.getFilters());
        if ((activeUxr & 4) != 0) {
            if (!filters.contains(sStringLengthFilter)) {
                ArrayList<InputFilter> updatedFilters = new ArrayList<>(filters);
                updatedFilters.add(sStringLengthFilter);
                tv.setFilters((InputFilter[]) updatedFilters.toArray(new InputFilter[updatedFilters.size()]));
                return;
            }
            return;
        }
        if (filters.contains(sStringLengthFilter)) {
            ArrayList<InputFilter> updatedFilters2 = new ArrayList<>(filters);
            updatedFilters2.remove(sStringLengthFilter);
            tv.setFilters((InputFilter[]) updatedFilters2.toArray(new InputFilter[updatedFilters2.size()]));
        }
    }
}
