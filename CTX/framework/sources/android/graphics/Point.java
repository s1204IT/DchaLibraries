package android.graphics;

import android.os.Parcel;
import android.os.Parcelable;
import android.util.proto.ProtoOutputStream;
import java.io.PrintWriter;

public class Point implements Parcelable {
    public static final Parcelable.Creator<Point> CREATOR = new Parcelable.Creator<Point>() {
        @Override
        public Point createFromParcel(Parcel parcel) {
            Point point = new Point();
            point.readFromParcel(parcel);
            return point;
        }

        @Override
        public Point[] newArray(int i) {
            return new Point[i];
        }
    };
    public int x;
    public int y;

    public Point() {
    }

    public Point(int i, int i2) {
        this.x = i;
        this.y = i2;
    }

    public Point(Point point) {
        this.x = point.x;
        this.y = point.y;
    }

    public void set(int i, int i2) {
        this.x = i;
        this.y = i2;
    }

    public final void negate() {
        this.x = -this.x;
        this.y = -this.y;
    }

    public final void offset(int i, int i2) {
        this.x += i;
        this.y += i2;
    }

    public final boolean equals(int i, int i2) {
        return this.x == i && this.y == i2;
    }

    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null || getClass() != obj.getClass()) {
            return false;
        }
        Point point = (Point) obj;
        if (this.x == point.x && this.y == point.y) {
            return true;
        }
        return false;
    }

    public int hashCode() {
        return (31 * this.x) + this.y;
    }

    public String toString() {
        return "Point(" + this.x + ", " + this.y + ")";
    }

    public void printShortString(PrintWriter printWriter) {
        printWriter.print("[");
        printWriter.print(this.x);
        printWriter.print(",");
        printWriter.print(this.y);
        printWriter.print("]");
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel parcel, int i) {
        parcel.writeInt(this.x);
        parcel.writeInt(this.y);
    }

    public void writeToProto(ProtoOutputStream protoOutputStream, long j) {
        long jStart = protoOutputStream.start(j);
        protoOutputStream.write(1120986464257L, this.x);
        protoOutputStream.write(1120986464258L, this.y);
        protoOutputStream.end(jStart);
    }

    public void readFromParcel(Parcel parcel) {
        this.x = parcel.readInt();
        this.y = parcel.readInt();
    }
}
