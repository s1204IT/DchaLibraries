package com.android.systemui.shared.system;

import android.graphics.Bitmap;
import android.graphics.GraphicBuffer;
import android.os.Parcel;
import android.os.Parcelable;

/* loaded from: classes.dex */
public class GraphicBufferCompat implements Parcelable {
    public static final Parcelable.Creator<GraphicBufferCompat> CREATOR = new Parcelable.Creator<GraphicBufferCompat>() { // from class: com.android.systemui.shared.system.GraphicBufferCompat.1
        AnonymousClass1() {
        }

        /* JADX DEBUG: Method merged with bridge method: createFromParcel(Landroid/os/Parcel;)Ljava/lang/Object; */
        @Override // android.os.Parcelable.Creator
        public GraphicBufferCompat createFromParcel(Parcel in) {
            return new GraphicBufferCompat(in);
        }

        /* JADX DEBUG: Method merged with bridge method: newArray(I)[Ljava/lang/Object; */
        @Override // android.os.Parcelable.Creator
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

    @Override // android.os.Parcelable
    public void writeToParcel(Parcel dest, int flags) {
        this.mBuffer.writeToParcel(dest, flags);
    }

    /* renamed from: com.android.systemui.shared.system.GraphicBufferCompat$1 */
    class AnonymousClass1 implements Parcelable.Creator<GraphicBufferCompat> {
        AnonymousClass1() {
        }

        /* JADX DEBUG: Method merged with bridge method: createFromParcel(Landroid/os/Parcel;)Ljava/lang/Object; */
        @Override // android.os.Parcelable.Creator
        public GraphicBufferCompat createFromParcel(Parcel in) {
            return new GraphicBufferCompat(in);
        }

        /* JADX DEBUG: Method merged with bridge method: newArray(I)[Ljava/lang/Object; */
        @Override // android.os.Parcelable.Creator
        public GraphicBufferCompat[] newArray(int size) {
            return new GraphicBufferCompat[size];
        }
    }

    @Override // android.os.Parcelable
    public int describeContents() {
        return 0;
    }
}
