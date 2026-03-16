package com.android.ex.camera2.portability;

import android.graphics.Point;
import android.hardware.Camera;
import android.text.TextUtils;
import java.util.ArrayList;
import java.util.List;

public class Size {
    public static final String DELIMITER = ",";
    private final Point val;

    public static List<Size> buildListFromCameraSizes(List<Camera.Size> cameraSizes) {
        ArrayList<Size> list = new ArrayList<>(cameraSizes.size());
        for (Camera.Size cameraSize : cameraSizes) {
            list.add(new Size(cameraSize));
        }
        return list;
    }

    public static List<Size> buildListFromAndroidSizes(List<android.util.Size> androidSizes) {
        ArrayList<Size> list = new ArrayList<>(androidSizes.size());
        for (android.util.Size androidSize : androidSizes) {
            list.add(new Size(androidSize));
        }
        return list;
    }

    public static String listToString(List<Size> sizes) {
        ArrayList<Integer> flatSizes = new ArrayList<>();
        for (Size s : sizes) {
            flatSizes.add(Integer.valueOf(s.width()));
            flatSizes.add(Integer.valueOf(s.height()));
        }
        return TextUtils.join(DELIMITER, flatSizes);
    }

    public static List<Size> stringToList(String encodedSizes) {
        String[] flatSizes = TextUtils.split(encodedSizes, DELIMITER);
        ArrayList<Size> list = new ArrayList<>();
        for (int i = 0; i < flatSizes.length; i += 2) {
            int width = Integer.parseInt(flatSizes[i]);
            int height = Integer.parseInt(flatSizes[i + 1]);
            list.add(new Size(width, height));
        }
        return list;
    }

    public Size(int width, int height) {
        this.val = new Point(width, height);
    }

    public Size(Size other) {
        if (other == null) {
            this.val = new Point(0, 0);
        } else {
            this.val = new Point(other.width(), other.height());
        }
    }

    public Size(Camera.Size other) {
        if (other == null) {
            this.val = new Point(0, 0);
        } else {
            this.val = new Point(other.width, other.height);
        }
    }

    public Size(android.util.Size other) {
        if (other == null) {
            this.val = new Point(0, 0);
        } else {
            this.val = new Point(other.getWidth(), other.getHeight());
        }
    }

    public Size(Point p) {
        if (p == null) {
            this.val = new Point(0, 0);
        } else {
            this.val = new Point(p);
        }
    }

    public int width() {
        return this.val.x;
    }

    public int height() {
        return this.val.y;
    }

    public boolean equals(Object o) {
        if (!(o instanceof Size)) {
            return false;
        }
        Size other = (Size) o;
        return this.val.equals(other.val);
    }

    public int hashCode() {
        return this.val.hashCode();
    }

    public String toString() {
        return "Size: (" + width() + " x " + height() + ")";
    }
}
