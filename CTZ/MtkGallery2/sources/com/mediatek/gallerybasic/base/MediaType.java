package com.mediatek.gallerybasic.base;

import java.util.Comparator;
import java.util.Iterator;
import java.util.Map;
import java.util.TreeMap;

public class MediaType {
    public static final int TYPE_INVALID = -1;
    private TreeMap<Integer, Integer> mTypes = new TreeMap<>(new Comparator<Integer>() {
        @Override
        public int compare(Integer num, Integer num2) {
            return num2.compareTo(num);
        }
    });

    public boolean isValid() {
        return !this.mTypes.isEmpty();
    }

    public void addType(int i, int i2) {
        this.mTypes.put(Integer.valueOf(i), Integer.valueOf(i2));
    }

    public int getMainType() {
        if (this.mTypes.isEmpty()) {
            return -1;
        }
        return this.mTypes.firstEntry().getValue().intValue();
    }

    public int[] getAllTypes() {
        int i = 0;
        if (this.mTypes.isEmpty()) {
            return new int[]{-1};
        }
        int[] iArr = new int[this.mTypes.size()];
        Iterator<Map.Entry<Integer, Integer>> it = this.mTypes.entrySet().iterator();
        while (it.hasNext()) {
            iArr[i] = it.next().getValue().intValue();
            i++;
        }
        return iArr;
    }

    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("main type = " + getMainType() + ", ");
        sb.append("all type = [");
        boolean z = true;
        for (int i : getAllTypes()) {
            if (!z) {
                sb.append(", ");
            } else {
                z = false;
            }
            sb.append(i);
        }
        sb.append("]");
        return sb.toString();
    }

    public boolean equals(Object obj) {
        if (obj == 0 || !(obj instanceof MediaType)) {
            return false;
        }
        return toString().equals(obj.toString());
    }
}
