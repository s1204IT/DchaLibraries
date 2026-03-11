package androidx.slice.widget;

import android.support.v4.util.ArraySet;
import androidx.slice.SliceSpec;
import androidx.slice.SliceSpecs;
import java.util.Arrays;
import java.util.Set;

public final class SliceLiveData {
    public static final SliceSpec OLD_BASIC = new SliceSpec("androidx.app.slice.BASIC", 1);
    public static final SliceSpec OLD_LIST = new SliceSpec("androidx.app.slice.LIST", 1);
    public static final Set<SliceSpec> SUPPORTED_SPECS = new ArraySet(Arrays.asList(SliceSpecs.BASIC, SliceSpecs.LIST, OLD_BASIC, OLD_LIST));
}
