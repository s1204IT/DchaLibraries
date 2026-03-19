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
        if (obj instanceof Face) {
            return this.mPersonId.equals(obj.mPersonId);
        }
        return false;
    }

    @Override
    public int compareTo(Face face) {
        return this.mName.compareTo(face.mName);
    }
}
