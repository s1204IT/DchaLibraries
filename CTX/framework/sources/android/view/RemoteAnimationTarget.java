package android.view;

import android.app.WindowConfiguration;
import android.graphics.Point;
import android.graphics.Rect;
import android.os.Parcel;
import android.os.Parcelable;
import android.util.proto.ProtoOutputStream;
import java.io.PrintWriter;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

public class RemoteAnimationTarget implements Parcelable {
    public static final Parcelable.Creator<RemoteAnimationTarget> CREATOR = new Parcelable.Creator<RemoteAnimationTarget>() {
        @Override
        public RemoteAnimationTarget createFromParcel(Parcel parcel) {
            return new RemoteAnimationTarget(parcel);
        }

        @Override
        public RemoteAnimationTarget[] newArray(int i) {
            return new RemoteAnimationTarget[i];
        }
    };
    public static final int MODE_CLOSING = 1;
    public static final int MODE_OPENING = 0;
    public final Rect clipRect;
    public final Rect contentInsets;
    public boolean isNotInRecents;
    public final boolean isTranslucent;
    public final SurfaceControl leash;
    public final int mode;
    public final Point position;
    public final int prefixOrderIndex;
    public final Rect sourceContainerBounds;
    public final int taskId;
    public final WindowConfiguration windowConfiguration;

    @Retention(RetentionPolicy.SOURCE)
    public @interface Mode {
    }

    public RemoteAnimationTarget(int i, int i2, SurfaceControl surfaceControl, boolean z, Rect rect, Rect rect2, int i3, Point point, Rect rect3, WindowConfiguration windowConfiguration, boolean z2) {
        this.mode = i2;
        this.taskId = i;
        this.leash = surfaceControl;
        this.isTranslucent = z;
        this.clipRect = new Rect(rect);
        this.contentInsets = new Rect(rect2);
        this.prefixOrderIndex = i3;
        this.position = new Point(point);
        this.sourceContainerBounds = new Rect(rect3);
        this.windowConfiguration = windowConfiguration;
        this.isNotInRecents = z2;
    }

    public RemoteAnimationTarget(Parcel parcel) {
        this.taskId = parcel.readInt();
        this.mode = parcel.readInt();
        this.leash = (SurfaceControl) parcel.readParcelable(null);
        this.isTranslucent = parcel.readBoolean();
        this.clipRect = (Rect) parcel.readParcelable(null);
        this.contentInsets = (Rect) parcel.readParcelable(null);
        this.prefixOrderIndex = parcel.readInt();
        this.position = (Point) parcel.readParcelable(null);
        this.sourceContainerBounds = (Rect) parcel.readParcelable(null);
        this.windowConfiguration = (WindowConfiguration) parcel.readParcelable(null);
        this.isNotInRecents = parcel.readBoolean();
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel parcel, int i) {
        parcel.writeInt(this.taskId);
        parcel.writeInt(this.mode);
        parcel.writeParcelable(this.leash, 0);
        parcel.writeBoolean(this.isTranslucent);
        parcel.writeParcelable(this.clipRect, 0);
        parcel.writeParcelable(this.contentInsets, 0);
        parcel.writeInt(this.prefixOrderIndex);
        parcel.writeParcelable(this.position, 0);
        parcel.writeParcelable(this.sourceContainerBounds, 0);
        parcel.writeParcelable(this.windowConfiguration, 0);
        parcel.writeBoolean(this.isNotInRecents);
    }

    public void dump(PrintWriter printWriter, String str) {
        printWriter.print(str);
        printWriter.print("mode=");
        printWriter.print(this.mode);
        printWriter.print(" taskId=");
        printWriter.print(this.taskId);
        printWriter.print(" isTranslucent=");
        printWriter.print(this.isTranslucent);
        printWriter.print(" clipRect=");
        this.clipRect.printShortString(printWriter);
        printWriter.print(" contentInsets=");
        this.contentInsets.printShortString(printWriter);
        printWriter.print(" prefixOrderIndex=");
        printWriter.print(this.prefixOrderIndex);
        printWriter.print(" position=");
        this.position.printShortString(printWriter);
        printWriter.print(" sourceContainerBounds=");
        this.sourceContainerBounds.printShortString(printWriter);
        printWriter.println();
        printWriter.print(str);
        printWriter.print("windowConfiguration=");
        printWriter.println(this.windowConfiguration);
        printWriter.print(str);
        printWriter.print("leash=");
        printWriter.println(this.leash);
    }

    public void writeToProto(ProtoOutputStream protoOutputStream, long j) {
        long jStart = protoOutputStream.start(j);
        protoOutputStream.write(1120986464257L, this.taskId);
        protoOutputStream.write(1120986464258L, this.mode);
        this.leash.writeToProto(protoOutputStream, 1146756268035L);
        protoOutputStream.write(1133871366148L, this.isTranslucent);
        this.clipRect.writeToProto(protoOutputStream, 1146756268037L);
        this.contentInsets.writeToProto(protoOutputStream, 1146756268038L);
        protoOutputStream.write(1120986464263L, this.prefixOrderIndex);
        this.position.writeToProto(protoOutputStream, 1146756268040L);
        this.sourceContainerBounds.writeToProto(protoOutputStream, 1146756268041L);
        this.windowConfiguration.writeToProto(protoOutputStream, 1146756268042L);
        protoOutputStream.end(jStart);
    }
}
