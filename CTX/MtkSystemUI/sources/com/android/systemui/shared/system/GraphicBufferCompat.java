package com.android.systemui.shared.system;

import android.graphics.GraphicBuffer;
import android.os.Parcel;
import android.os.Parcelable;

public class GraphicBufferCompat implements Parcelable {
    public static final Parcelable.Creator<GraphicBufferCompat> CREATOR = new Parcelable.Creator<GraphicBufferCompat>() {
        @Override
        public GraphicBufferCompat createFromParcel(Parcel parcel) {
            return new GraphicBufferCompat(parcel);
        }

        @Override
        public GraphicBufferCompat[] newArray(int i) {
            return new GraphicBufferCompat[i];
        }
    };
    private GraphicBuffer mBuffer;

    public GraphicBufferCompat(GraphicBuffer graphicBuffer) {
        this.mBuffer = graphicBuffer;
    }

    public GraphicBufferCompat(Parcel parcel) {
        this.mBuffer = (GraphicBuffer) GraphicBuffer.CREATOR.createFromParcel(parcel);
    }

    @Override
    public void writeToParcel(Parcel parcel, int i) {
        this.mBuffer.writeToParcel(parcel, i);
    }

    @Override
    public int describeContents() {
        return 0;
    }
}
