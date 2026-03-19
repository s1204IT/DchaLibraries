package com.android.gallery3d.app;

import com.android.gallery3d.R;
import com.android.gallery3d.data.Path;
import mf.org.apache.xerces.impl.xs.SchemaSymbols;

public class FilterUtils {
    public static void setupMenuItems(GalleryActionBar galleryActionBar, Path path, boolean z) {
        boolean z2;
        boolean z3;
        boolean z4;
        boolean z5;
        boolean z6;
        boolean z7;
        boolean z8;
        boolean z9;
        boolean z10;
        boolean z11;
        boolean z12;
        boolean z13;
        boolean z14;
        boolean z15;
        boolean z16;
        boolean z17;
        boolean z18;
        boolean z19;
        boolean z20;
        int[] iArr = new int[6];
        getAppliedFilters(path, iArr);
        boolean z21 = false;
        int i = iArr[0];
        int i2 = iArr[1];
        int i3 = iArr[3];
        int i4 = iArr[4];
        int i5 = iArr[5];
        if ((i & 2) == 0) {
            z2 = false;
        } else {
            z2 = true;
        }
        if ((i4 & 2) == 0) {
            z3 = false;
        } else {
            z3 = true;
        }
        setMenuItemApplied(galleryActionBar, 2, z2, z3);
        if ((i & 4) == 0) {
            z4 = false;
        } else {
            z4 = true;
        }
        if ((i4 & 4) == 0) {
            z5 = false;
        } else {
            z5 = true;
        }
        setMenuItemApplied(galleryActionBar, 4, z4, z5);
        if ((i & 8) == 0) {
            z6 = false;
        } else {
            z6 = true;
        }
        if ((i4 & 8) == 0) {
            z7 = false;
        } else {
            z7 = true;
        }
        setMenuItemApplied(galleryActionBar, 8, z6, z7);
        if ((i & 32) == 0) {
            z8 = false;
        } else {
            z8 = true;
        }
        if ((i4 & 32) == 0) {
            z9 = false;
        } else {
            z9 = true;
        }
        setMenuItemApplied(galleryActionBar, 32, z8, z9);
        if (z && i != 0) {
            z10 = false;
        } else {
            z10 = true;
        }
        galleryActionBar.setClusterItemVisibility(1, z10);
        if (i != 0) {
            z11 = false;
        } else {
            z11 = true;
        }
        if (i4 != 0) {
            z12 = false;
        } else {
            z12 = true;
        }
        setMenuItemApplied(galleryActionBar, R.id.action_cluster_album, z11, z12);
        int i6 = i2 & 1;
        if (i6 == 0) {
            z13 = false;
        } else {
            z13 = true;
        }
        if (i6 != 0 || i3 != 0) {
            z14 = false;
        } else {
            z14 = true;
        }
        if ((i5 & 1) == 0) {
            z15 = false;
        } else {
            z15 = true;
        }
        setMenuItemAppliedEnabled(galleryActionBar, R.string.show_images_only, z13, z14, z15);
        int i7 = i2 & 2;
        if (i7 == 0) {
            z16 = false;
        } else {
            z16 = true;
        }
        if (i7 != 0 || i3 != 0) {
            z17 = false;
        } else {
            z17 = true;
        }
        if ((i5 & 2) == 0) {
            z18 = false;
        } else {
            z18 = true;
        }
        setMenuItemAppliedEnabled(galleryActionBar, R.string.show_videos_only, z16, z17, z18);
        if (i2 != 0) {
            z19 = false;
        } else {
            z19 = true;
        }
        if (i2 == 0 || i3 != 0) {
            z20 = false;
        } else {
            z20 = true;
        }
        if (i5 == 0) {
            z21 = true;
        }
        setMenuItemAppliedEnabled(galleryActionBar, R.string.show_all, z19, z20, z21);
    }

    private static void getAppliedFilters(Path path, int[] iArr) {
        getAppliedFilters(path, iArr, false);
    }

    private static void getAppliedFilters(Path path, int[] iArr, boolean z) {
        String[] strArrSplit = path.split();
        for (int i = 0; i < strArrSplit.length; i++) {
            if (strArrSplit[i].startsWith("{")) {
                for (String str : Path.splitSequence(strArrSplit[i])) {
                    getAppliedFilters(Path.fromString(str), iArr, z);
                }
            }
        }
        if (strArrSplit[0].equals("cluster")) {
            if (strArrSplit.length == 4) {
                z = true;
            }
            int clusterType = toClusterType(strArrSplit[2]);
            iArr[0] = iArr[0] | clusterType;
            iArr[4] = clusterType;
            if (z) {
                iArr[2] = clusterType | iArr[2];
            }
        }
    }

    private static int toClusterType(String str) {
        if (str.equals(SchemaSymbols.ATTVAL_TIME)) {
            return 2;
        }
        if (str.equals("location")) {
            return 4;
        }
        if (str.equals("tag")) {
            return 8;
        }
        if (str.equals("size")) {
            return 16;
        }
        if (str.equals("face")) {
            return 32;
        }
        return 0;
    }

    private static void setMenuItemApplied(GalleryActionBar galleryActionBar, int i, boolean z, boolean z2) {
        galleryActionBar.setClusterItemEnabled(i, !z);
    }

    private static void setMenuItemAppliedEnabled(GalleryActionBar galleryActionBar, int i, boolean z, boolean z2, boolean z3) {
        galleryActionBar.setClusterItemEnabled(i, z2);
    }

    public static String newClusterPath(String str, int i) {
        String str2;
        if (i == 2) {
            str2 = SchemaSymbols.ATTVAL_TIME;
        } else if (i == 4) {
            str2 = "location";
        } else if (i == 8) {
            str2 = "tag";
        } else if (i == 16) {
            str2 = "size";
        } else if (i == 32) {
            str2 = "face";
        } else {
            return str;
        }
        return "/cluster/{" + str + "}/" + str2;
    }

    public static String switchClusterPath(String str, int i) {
        return newClusterPath(removeOneClusterFromPath(str), i);
    }

    private static String removeOneClusterFromPath(String str) {
        return removeOneClusterFromPath(str, new boolean[1]);
    }

    private static String removeOneClusterFromPath(String str, boolean[] zArr) {
        if (zArr[0]) {
            return str;
        }
        String[] strArrSplit = Path.split(str);
        if (strArrSplit[0].equals("cluster")) {
            zArr[0] = true;
            return Path.splitSequence(strArrSplit[1])[0];
        }
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < strArrSplit.length; i++) {
            sb.append("/");
            if (strArrSplit[i].startsWith("{")) {
                sb.append("{");
                String[] strArrSplitSequence = Path.splitSequence(strArrSplit[i]);
                for (int i2 = 0; i2 < strArrSplitSequence.length; i2++) {
                    if (i2 > 0) {
                        sb.append(",");
                    }
                    sb.append(removeOneClusterFromPath(strArrSplitSequence[i2], zArr));
                }
                sb.append("}");
            } else {
                sb.append(strArrSplit[i]);
            }
        }
        return sb.toString();
    }
}
