package com.android.systemui.shared.system;

import android.graphics.GraphicBuffer;
import android.os.Parcel;
import android.os.Parcelable;

/* loaded from: classes.dex */
public class GraphicBufferCompat implements Parcelable {
    public static final Parcelable.Creator<GraphicBufferCompat> CREATOR = new Parcelable.Creator<GraphicBufferCompat>() { // from class: com.android.systemui.shared.system.GraphicBufferCompat.1
        /* JADX DEBUG: Method merged with bridge method: createFromParcel(Landroid/os/Parcel;)Ljava/lang/Object; */
        /* JADX WARN: Can't rename method to resolve collision */
        @Override // android.os.Parcelable.Creator
        public GraphicBufferCompat createFromParcel(Parcel parcel) {
            return new GraphicBufferCompat(parcel);
        }

        /* JADX DEBUG: Method merged with bridge method: newArray(I)[Ljava/lang/Object; */
        /* JADX WARN: Can't rename method to resolve collision */
        @Override // android.os.Parcelable.Creator
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

    @Override // android.os.Parcelable
    public void writeToParcel(Parcel parcel, int i) {
        this.mBuffer.writeToParcel(parcel, i);
    }

    @Override // android.os.Parcelable
    public int describeContents() {
        return 0;
    }
}
