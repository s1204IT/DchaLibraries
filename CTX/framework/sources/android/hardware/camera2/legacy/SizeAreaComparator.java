package android.hardware.camera2.legacy;

import android.hardware.Camera;
import com.android.internal.util.Preconditions;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public class SizeAreaComparator implements Comparator<Camera.Size> {
    @Override
    public int compare(Camera.Size size, Camera.Size size2) {
        Preconditions.checkNotNull(size, "size must not be null");
        Preconditions.checkNotNull(size2, "size2 must not be null");
        if (size.equals(size2)) {
            return 0;
        }
        long j = size.width;
        long j2 = size2.width;
        long j3 = ((long) size.height) * j;
        long j4 = ((long) size2.height) * j2;
        return j3 == j4 ? j > j2 ? 1 : -1 : j3 > j4 ? 1 : -1;
    }

    public static Camera.Size findLargestByArea(List<Camera.Size> list) {
        Preconditions.checkNotNull(list, "sizes must not be null");
        return (Camera.Size) Collections.max(list, new SizeAreaComparator());
    }
}
