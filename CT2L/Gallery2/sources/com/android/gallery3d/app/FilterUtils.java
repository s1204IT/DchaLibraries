package com.android.gallery3d.app;

import android.support.v4.app.NotificationCompat;
import com.android.gallery3d.R;
import com.android.gallery3d.data.Path;

public class FilterUtils {
    public static void setupMenuItems(GalleryActionBar actionBar, Path path, boolean inAlbum) {
        int[] result = new int[6];
        getAppliedFilters(path, result);
        int ctype = result[0];
        int ftype = result[1];
        int ftypef = result[3];
        int ccurrent = result[4];
        int fcurrent = result[5];
        setMenuItemApplied(actionBar, 2, (ctype & 2) != 0, (ccurrent & 2) != 0);
        setMenuItemApplied(actionBar, 4, (ctype & 4) != 0, (ccurrent & 4) != 0);
        setMenuItemApplied(actionBar, 8, (ctype & 8) != 0, (ccurrent & 8) != 0);
        setMenuItemApplied(actionBar, 32, (ctype & 32) != 0, (ccurrent & 32) != 0);
        actionBar.setClusterItemVisibility(1, !inAlbum || ctype == 0);
        setMenuItemApplied(actionBar, R.id.action_cluster_album, ctype == 0, ccurrent == 0);
        setMenuItemAppliedEnabled(actionBar, R.string.show_images_only, (ftype & 1) != 0, (ftype & 1) == 0 && ftypef == 0, (fcurrent & 1) != 0);
        setMenuItemAppliedEnabled(actionBar, R.string.show_videos_only, (ftype & 2) != 0, (ftype & 2) == 0 && ftypef == 0, (fcurrent & 2) != 0);
        setMenuItemAppliedEnabled(actionBar, R.string.show_all, ftype == 0, ftype != 0 && ftypef == 0, fcurrent == 0);
    }

    private static void getAppliedFilters(Path path, int[] result) {
        getAppliedFilters(path, result, false);
    }

    private static void getAppliedFilters(Path path, int[] result, boolean underCluster) {
        String[] segments = path.split();
        for (int i = 0; i < segments.length; i++) {
            if (segments[i].startsWith("{")) {
                String[] sets = Path.splitSequence(segments[i]);
                for (String str : sets) {
                    Path sub = Path.fromString(str);
                    getAppliedFilters(sub, result, underCluster);
                }
            }
        }
        if (segments[0].equals("cluster")) {
            if (segments.length == 4) {
                underCluster = true;
            }
            int ctype = toClusterType(segments[2]);
            result[0] = result[0] | ctype;
            result[4] = ctype;
            if (underCluster) {
                result[2] = result[2] | ctype;
            }
        }
    }

    private static int toClusterType(String s) {
        if (s.equals("time")) {
            return 2;
        }
        if (s.equals("location")) {
            return 4;
        }
        if (s.equals("tag")) {
            return 8;
        }
        if (s.equals("size")) {
            return 16;
        }
        if (s.equals("face")) {
            return 32;
        }
        return 0;
    }

    private static void setMenuItemApplied(GalleryActionBar model, int id, boolean applied, boolean updateTitle) {
        model.setClusterItemEnabled(id, !applied);
    }

    private static void setMenuItemAppliedEnabled(GalleryActionBar model, int id, boolean applied, boolean enabled, boolean updateTitle) {
        model.setClusterItemEnabled(id, enabled);
    }

    public static String newFilterPath(String base, int filterType) {
        int mediaType;
        switch (filterType) {
            case 1:
                mediaType = 2;
                break;
            case 2:
                mediaType = 4;
                break;
            default:
                return base;
        }
        return "/filter/mediatype/" + mediaType + "/{" + base + "}";
    }

    public static String newClusterPath(String base, int clusterType) {
        String kind;
        switch (clusterType) {
            case 2:
                kind = "time";
                break;
            case 4:
                kind = "location";
                break;
            case NotificationCompat.FLAG_ONLY_ALERT_ONCE:
                kind = "tag";
                break;
            case NotificationCompat.FLAG_AUTO_CANCEL:
                kind = "size";
                break;
            case 32:
                kind = "face";
                break;
            default:
                return base;
        }
        return "/cluster/{" + base + "}/" + kind;
    }

    public static String switchClusterPath(String base, int clusterType) {
        return newClusterPath(removeOneClusterFromPath(base), clusterType);
    }

    private static String removeOneClusterFromPath(String base) {
        boolean[] done = new boolean[1];
        return removeOneClusterFromPath(base, done);
    }

    private static String removeOneClusterFromPath(String base, boolean[] done) {
        if (!done[0]) {
            String[] segments = Path.split(base);
            if (segments[0].equals("cluster")) {
                done[0] = true;
                return Path.splitSequence(segments[1])[0];
            }
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < segments.length; i++) {
                sb.append("/");
                if (segments[i].startsWith("{")) {
                    sb.append("{");
                    String[] sets = Path.splitSequence(segments[i]);
                    for (int j = 0; j < sets.length; j++) {
                        if (j > 0) {
                            sb.append(",");
                        }
                        sb.append(removeOneClusterFromPath(sets[j], done));
                    }
                    sb.append("}");
                } else {
                    sb.append(segments[i]);
                }
            }
            return sb.toString();
        }
        return base;
    }
}
