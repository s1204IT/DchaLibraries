package android.view;

import android.graphics.GraphicBuffer;
import android.graphics.Rect;
import android.os.Parcel;
import android.os.Parcelable;

public class AppTransitionAnimationSpec implements Parcelable {
    public static final Parcelable.Creator<AppTransitionAnimationSpec> CREATOR = new Parcelable.Creator<AppTransitionAnimationSpec>() {
        @Override
        public AppTransitionAnimationSpec createFromParcel(Parcel parcel) {
            return new AppTransitionAnimationSpec(parcel);
        }

        @Override
        public AppTransitionAnimationSpec[] newArray(int i) {
            return new AppTransitionAnimationSpec[i];
        }
    };
    public final GraphicBuffer buffer;
    public final Rect rect;
    public final int taskId;

    public AppTransitionAnimationSpec(int i, GraphicBuffer graphicBuffer, Rect rect) {
        this.taskId = i;
        this.rect = rect;
        this.buffer = graphicBuffer;
    }

    public AppTransitionAnimationSpec(Parcel parcel) {
        this.taskId = parcel.readInt();
        this.rect = (Rect) parcel.readParcelable(null);
        this.buffer = (GraphicBuffer) parcel.readParcelable(null);
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel parcel, int i) {
        parcel.writeInt(this.taskId);
        parcel.writeParcelable(this.rect, 0);
        parcel.writeParcelable(this.buffer, 0);
    }

    public String toString() {
        return "{taskId: " + this.taskId + ", buffer: " + this.buffer + ", rect: " + this.rect + "}";
    }
}
