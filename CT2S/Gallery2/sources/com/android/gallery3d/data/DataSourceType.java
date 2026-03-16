package com.android.gallery3d.data;

import com.android.gallery3d.util.MediaSetUtils;

public final class DataSourceType {
    private static final Path PICASA_ROOT = Path.fromString("/picasa");
    private static final Path LOCAL_ROOT = Path.fromString("/local");

    public static int identifySourceType(MediaSet set) {
        if (set == null) {
            return 0;
        }
        Path path = set.getPath();
        if (MediaSetUtils.isCameraSource(path)) {
            return 3;
        }
        Path prefix = path.getPrefixPath();
        if (prefix == PICASA_ROOT) {
            return 2;
        }
        return prefix == LOCAL_ROOT ? 1 : 0;
    }
}
