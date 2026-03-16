package com.android.gallery3d.data;

import android.graphics.Rect;

public class Face implements Comparable<Face> {
    private String mName;
    private String mPersonId;
    private Rect mPosition;

    public Rect getPosition() {
        return this.mPosition;
    }

    public String getName() {
        return this.mName;
    }

    public boolean equals(Object obj) {
        if (!(obj instanceof Face)) {
            return false;
        }
        Face face = (Face) obj;
        return this.mPersonId.equals(face.mPersonId);
    }

    @Override
    public int compareTo(Face another) {
        return this.mName.compareTo(another.mName);
    }
}
