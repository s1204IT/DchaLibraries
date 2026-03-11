package com.android.launcher3;

import android.annotation.TargetApi;
import android.content.Context;
import android.graphics.Point;
import android.util.DisplayMetrics;
import android.view.Display;
import android.view.WindowManager;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;

public class InvariantDeviceProfile {
    int defaultLayoutId;
    public int fillResIconDpi;
    public int hotseatAllAppsRank;
    float hotseatIconSize;
    public int iconBitmapSize;
    public float iconSize;
    public float iconTextSize;
    DeviceProfile landscapeProfile;
    int minAllAppsPredictionColumns;
    float minHeightDps;
    float minWidthDps;
    String name;
    public int numColumns;
    public int numFolderColumns;
    public int numFolderRows;
    public int numHotseatIcons;
    public int numRows;
    DeviceProfile portraitProfile;
    private static float DEFAULT_ICON_SIZE_DP = 60.0f;
    private static float KNEARESTNEIGHBOR = 3.0f;
    private static float WEIGHT_POWER = 5.0f;
    private static float WEIGHT_EFFICIENT = 100000.0f;

    public InvariantDeviceProfile() {
    }

    public InvariantDeviceProfile(InvariantDeviceProfile p) {
        this(p.name, p.minWidthDps, p.minHeightDps, p.numRows, p.numColumns, p.numFolderRows, p.numFolderColumns, p.minAllAppsPredictionColumns, p.iconSize, p.iconTextSize, p.numHotseatIcons, p.hotseatIconSize, p.defaultLayoutId);
    }

    InvariantDeviceProfile(String n, float w, float h, int r, int c, int fr, int fc, int maapc, float is, float its, int hs, float his, int dlId) {
        if (hs % 2 == 0) {
            throw new RuntimeException("All Device Profiles must have an odd number of hotseat spaces");
        }
        this.name = n;
        this.minWidthDps = w;
        this.minHeightDps = h;
        this.numRows = r;
        this.numColumns = c;
        this.numFolderRows = fr;
        this.numFolderColumns = fc;
        this.minAllAppsPredictionColumns = maapc;
        this.iconSize = is;
        this.iconTextSize = its;
        this.numHotseatIcons = hs;
        this.hotseatIconSize = his;
        this.defaultLayoutId = dlId;
    }

    @TargetApi(23)
    InvariantDeviceProfile(Context context) {
        WindowManager wm = (WindowManager) context.getSystemService("window");
        Display display = wm.getDefaultDisplay();
        DisplayMetrics dm = new DisplayMetrics();
        display.getMetrics(dm);
        Point smallestSize = new Point();
        Point largestSize = new Point();
        display.getCurrentSizeRange(smallestSize, largestSize);
        this.minWidthDps = Utilities.dpiFromPx(Math.min(smallestSize.x, smallestSize.y), dm);
        this.minHeightDps = Utilities.dpiFromPx(Math.min(largestSize.x, largestSize.y), dm);
        ArrayList<InvariantDeviceProfile> closestProfiles = findClosestDeviceProfiles(this.minWidthDps, this.minHeightDps, getPredefinedDeviceProfiles());
        InvariantDeviceProfile interpolatedDeviceProfileOut = invDistWeightedInterpolate(this.minWidthDps, this.minHeightDps, closestProfiles);
        InvariantDeviceProfile closestProfile = closestProfiles.get(0);
        this.numRows = closestProfile.numRows;
        this.numColumns = closestProfile.numColumns;
        this.numHotseatIcons = closestProfile.numHotseatIcons;
        this.hotseatAllAppsRank = this.numHotseatIcons / 2;
        this.defaultLayoutId = closestProfile.defaultLayoutId;
        this.numFolderRows = closestProfile.numFolderRows;
        this.numFolderColumns = closestProfile.numFolderColumns;
        this.minAllAppsPredictionColumns = closestProfile.minAllAppsPredictionColumns;
        this.iconSize = interpolatedDeviceProfileOut.iconSize;
        this.iconBitmapSize = Utilities.pxFromDp(this.iconSize, dm);
        this.iconTextSize = interpolatedDeviceProfileOut.iconTextSize;
        this.hotseatIconSize = interpolatedDeviceProfileOut.hotseatIconSize;
        this.fillResIconDpi = getLauncherIconDensity(this.iconBitmapSize);
        applyPartnerDeviceProfileOverrides(context, dm);
        Point realSize = new Point();
        display.getRealSize(realSize);
        int smallSide = Math.min(realSize.x, realSize.y);
        int largeSide = Math.max(realSize.x, realSize.y);
        this.landscapeProfile = new DeviceProfile(context, this, smallestSize, largestSize, largeSide, smallSide, true);
        this.portraitProfile = new DeviceProfile(context, this, smallestSize, largestSize, smallSide, largeSide, false);
    }

    ArrayList<InvariantDeviceProfile> getPredefinedDeviceProfiles() {
        ArrayList<InvariantDeviceProfile> predefinedDeviceProfiles = new ArrayList<>();
        predefinedDeviceProfiles.add(new InvariantDeviceProfile("Super Short Stubby", 255.0f, 300.0f, 2, 3, 2, 3, 3, 48.0f, 13.0f, 3, 48.0f, R.xml.default_workspace_3x3));
        predefinedDeviceProfiles.add(new InvariantDeviceProfile("Shorter Stubby", 255.0f, 400.0f, 3, 3, 3, 3, 3, 48.0f, 13.0f, 3, 48.0f, R.xml.default_workspace_3x3));
        predefinedDeviceProfiles.add(new InvariantDeviceProfile("Short Stubby", 275.0f, 420.0f, 3, 4, 3, 4, 4, 48.0f, 13.0f, 5, 48.0f, R.xml.default_workspace_4x4));
        predefinedDeviceProfiles.add(new InvariantDeviceProfile("Stubby", 255.0f, 450.0f, 3, 4, 3, 4, 4, 48.0f, 13.0f, 5, 48.0f, R.xml.default_workspace_4x4));
        predefinedDeviceProfiles.add(new InvariantDeviceProfile("Nexus S", 296.0f, 491.33f, 4, 4, 4, 4, 4, 48.0f, 13.0f, 5, 48.0f, R.xml.default_workspace_4x4));
        predefinedDeviceProfiles.add(new InvariantDeviceProfile("Nexus 4", 359.0f, 567.0f, 4, 4, 4, 4, 4, DEFAULT_ICON_SIZE_DP, 13.0f, 5, 56.0f, R.xml.default_workspace_4x4));
        predefinedDeviceProfiles.add(new InvariantDeviceProfile("Nexus 5", 335.0f, 567.0f, 4, 4, 4, 4, 4, DEFAULT_ICON_SIZE_DP, 13.0f, 5, 56.0f, R.xml.default_workspace_4x4));
        predefinedDeviceProfiles.add(new InvariantDeviceProfile("Large Phone", 406.0f, 694.0f, 5, 5, 4, 4, 4, 64.0f, 14.4f, 5, 56.0f, R.xml.default_workspace_5x5));
        predefinedDeviceProfiles.add(new InvariantDeviceProfile("Nexus 7", 575.0f, 904.0f, 5, 6, 4, 5, 4, 72.0f, 14.4f, 7, 60.0f, R.xml.default_workspace_5x6));
        predefinedDeviceProfiles.add(new InvariantDeviceProfile("Nexus 10", 727.0f, 1207.0f, 5, 6, 4, 5, 4, 76.0f, 14.4f, 7, 76.0f, R.xml.default_workspace_5x6));
        predefinedDeviceProfiles.add(new InvariantDeviceProfile("20-inch Tablet", 1527.0f, 2527.0f, 7, 7, 6, 6, 4, 100.0f, 20.0f, 7, 72.0f, R.xml.default_workspace_5x6));
        return predefinedDeviceProfiles;
    }

    private int getLauncherIconDensity(int requiredSize) {
        int[] densityBuckets = {120, 160, 213, 240, 320, 480, 640};
        int density = 640;
        for (int i = densityBuckets.length - 1; i >= 0; i--) {
            float expectedSize = (densityBuckets[i] * 48.0f) / 160.0f;
            if (expectedSize >= requiredSize) {
                density = densityBuckets[i];
            }
        }
        return density;
    }

    private void applyPartnerDeviceProfileOverrides(Context context, DisplayMetrics dm) {
        Partner p = Partner.get(context.getPackageManager());
        if (p == null) {
            return;
        }
        p.applyInvariantDeviceProfileOverrides(this, dm);
    }

    float dist(float x0, float y0, float x1, float y1) {
        return (float) Math.hypot(x1 - x0, y1 - y0);
    }

    ArrayList<InvariantDeviceProfile> findClosestDeviceProfiles(final float width, final float height, ArrayList<InvariantDeviceProfile> points) {
        Collections.sort(points, new Comparator<InvariantDeviceProfile>() {
            @Override
            public int compare(InvariantDeviceProfile a, InvariantDeviceProfile b) {
                return Float.compare(InvariantDeviceProfile.this.dist(width, height, a.minWidthDps, a.minHeightDps), InvariantDeviceProfile.this.dist(width, height, b.minWidthDps, b.minHeightDps));
            }
        });
        return points;
    }

    InvariantDeviceProfile invDistWeightedInterpolate(float width, float height, ArrayList<InvariantDeviceProfile> points) {
        float weights = 0.0f;
        InvariantDeviceProfile p = points.get(0);
        if (dist(width, height, p.minWidthDps, p.minHeightDps) == 0.0f) {
            return p;
        }
        InvariantDeviceProfile out = new InvariantDeviceProfile();
        for (int i = 0; i < points.size() && i < KNEARESTNEIGHBOR; i++) {
            InvariantDeviceProfile p2 = new InvariantDeviceProfile(points.get(i));
            float w = weight(width, height, p2.minWidthDps, p2.minHeightDps, WEIGHT_POWER);
            weights += w;
            out.add(p2.multiply(w));
        }
        return out.multiply(1.0f / weights);
    }

    private void add(InvariantDeviceProfile p) {
        this.iconSize += p.iconSize;
        this.iconTextSize += p.iconTextSize;
        this.hotseatIconSize += p.hotseatIconSize;
    }

    private InvariantDeviceProfile multiply(float w) {
        this.iconSize *= w;
        this.iconTextSize *= w;
        this.hotseatIconSize *= w;
        return this;
    }

    private float weight(float x0, float y0, float x1, float y1, float pow) {
        float d = dist(x0, y0, x1, y1);
        if (Float.compare(d, 0.0f) == 0) {
            return Float.POSITIVE_INFINITY;
        }
        return (float) (((double) WEIGHT_EFFICIENT) / Math.pow(d, pow));
    }
}
