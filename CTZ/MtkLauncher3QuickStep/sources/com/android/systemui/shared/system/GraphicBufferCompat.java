package com.android.systemui.shared.system;

import android.graphics.Bitmap;
import android.graphics.GraphicBuffer;
import android.os.Parcel;
import android.os.Parcelable;

public class GraphicBufferCompat implements Parcelable {
    public static final Parcelable.Creator<GraphicBufferCompat> CREATOR = new Parcelable.Creator<GraphicBufferCompat>() {
        @Override
        public GraphicBufferCompat createFromParcel(Parcel in) {
            return new GraphicBufferCompat(in);
        }

        @Override
        public GraphicBufferCompat[] newArray(int size) {
            return new GraphicBufferCompat[size];
        }
    };
    private GraphicBuffer mBuffer;

    public GraphicBufferCompat(GraphicBuffer buffer) {
        this.mBuffer = buffer;
    }

    public GraphicBufferCompat(Parcel in) {
        this.mBuffer = (GraphicBuffer) GraphicBuffer.CREATOR.createFromParcel(in);
    }

    public Bitmap toBitmap() {
        if (this.mBuffer != null) {
            return Bitmap.createHardwareBitmap(this.mBuffer);
        }
        return null;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        this.mBuffer.writeToParcel(dest, flags);
    }

    @Override
    public int describeContents() {
        return 0;
    }
}
