package android.graphics;

import android.os.Parcel;
import android.os.Parcelable;
import android.text.TextUtils;
import android.util.proto.ProtoOutputStream;
import java.io.PrintWriter;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class Rect implements Parcelable {
    public static final Parcelable.Creator<Rect> CREATOR = new Parcelable.Creator<Rect>() {
        @Override
        public Rect createFromParcel(Parcel parcel) {
            Rect rect = new Rect();
            rect.readFromParcel(parcel);
            return rect;
        }

        @Override
        public Rect[] newArray(int i) {
            return new Rect[i];
        }
    };
    public int bottom;
    public int left;
    public int right;
    public int top;

    private static final class UnflattenHelper {
        private static final Pattern FLATTENED_PATTERN = Pattern.compile("(-?\\d+) (-?\\d+) (-?\\d+) (-?\\d+)");

        private UnflattenHelper() {
        }

        static Matcher getMatcher(String str) {
            return FLATTENED_PATTERN.matcher(str);
        }
    }

    public Rect() {
    }

    public Rect(int i, int i2, int i3, int i4) {
        this.left = i;
        this.top = i2;
        this.right = i3;
        this.bottom = i4;
    }

    public Rect(Rect rect) {
        if (rect == null) {
            this.bottom = 0;
            this.right = 0;
            this.top = 0;
            this.left = 0;
            return;
        }
        this.left = rect.left;
        this.top = rect.top;
        this.right = rect.right;
        this.bottom = rect.bottom;
    }

    public static Rect copyOrNull(Rect rect) {
        if (rect == null) {
            return null;
        }
        return new Rect(rect);
    }

    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null || getClass() != obj.getClass()) {
            return false;
        }
        Rect rect = (Rect) obj;
        if (this.left == rect.left && this.top == rect.top && this.right == rect.right && this.bottom == rect.bottom) {
            return true;
        }
        return false;
    }

    public int hashCode() {
        return (31 * ((((this.left * 31) + this.top) * 31) + this.right)) + this.bottom;
    }

    public String toString() {
        StringBuilder sb = new StringBuilder(32);
        sb.append("Rect(");
        sb.append(this.left);
        sb.append(", ");
        sb.append(this.top);
        sb.append(" - ");
        sb.append(this.right);
        sb.append(", ");
        sb.append(this.bottom);
        sb.append(")");
        return sb.toString();
    }

    public String toShortString() {
        return toShortString(new StringBuilder(32));
    }

    public String toShortString(StringBuilder sb) {
        sb.setLength(0);
        sb.append('[');
        sb.append(this.left);
        sb.append(',');
        sb.append(this.top);
        sb.append("][");
        sb.append(this.right);
        sb.append(',');
        sb.append(this.bottom);
        sb.append(']');
        return sb.toString();
    }

    public String flattenToString() {
        StringBuilder sb = new StringBuilder(32);
        sb.append(this.left);
        sb.append(' ');
        sb.append(this.top);
        sb.append(' ');
        sb.append(this.right);
        sb.append(' ');
        sb.append(this.bottom);
        return sb.toString();
    }

    public static Rect unflattenFromString(String str) {
        if (TextUtils.isEmpty(str)) {
            return null;
        }
        Matcher matcher = UnflattenHelper.getMatcher(str);
        if (matcher.matches()) {
            return new Rect(Integer.parseInt(matcher.group(1)), Integer.parseInt(matcher.group(2)), Integer.parseInt(matcher.group(3)), Integer.parseInt(matcher.group(4)));
        }
        return null;
    }

    public void printShortString(PrintWriter printWriter) {
        printWriter.print('[');
        printWriter.print(this.left);
        printWriter.print(',');
        printWriter.print(this.top);
        printWriter.print("][");
        printWriter.print(this.right);
        printWriter.print(',');
        printWriter.print(this.bottom);
        printWriter.print(']');
    }

    public void writeToProto(ProtoOutputStream protoOutputStream, long j) {
        long jStart = protoOutputStream.start(j);
        protoOutputStream.write(1120986464257L, this.left);
        protoOutputStream.write(1120986464258L, this.top);
        protoOutputStream.write(1120986464259L, this.right);
        protoOutputStream.write(1120986464260L, this.bottom);
        protoOutputStream.end(jStart);
    }

    public final boolean isEmpty() {
        return this.left >= this.right || this.top >= this.bottom;
    }

    public final int width() {
        return this.right - this.left;
    }

    public final int height() {
        return this.bottom - this.top;
    }

    public final int centerX() {
        return (this.left + this.right) >> 1;
    }

    public final int centerY() {
        return (this.top + this.bottom) >> 1;
    }

    public final float exactCenterX() {
        return (this.left + this.right) * 0.5f;
    }

    public final float exactCenterY() {
        return (this.top + this.bottom) * 0.5f;
    }

    public void setEmpty() {
        this.bottom = 0;
        this.top = 0;
        this.right = 0;
        this.left = 0;
    }

    public void set(int i, int i2, int i3, int i4) {
        this.left = i;
        this.top = i2;
        this.right = i3;
        this.bottom = i4;
    }

    public void set(Rect rect) {
        this.left = rect.left;
        this.top = rect.top;
        this.right = rect.right;
        this.bottom = rect.bottom;
    }

    public void offset(int i, int i2) {
        this.left += i;
        this.top += i2;
        this.right += i;
        this.bottom += i2;
    }

    public void offsetTo(int i, int i2) {
        this.right += i - this.left;
        this.bottom += i2 - this.top;
        this.left = i;
        this.top = i2;
    }

    public void inset(int i, int i2) {
        this.left += i;
        this.top += i2;
        this.right -= i;
        this.bottom -= i2;
    }

    public void inset(Rect rect) {
        this.left += rect.left;
        this.top += rect.top;
        this.right -= rect.right;
        this.bottom -= rect.bottom;
    }

    public void inset(int i, int i2, int i3, int i4) {
        this.left += i;
        this.top += i2;
        this.right -= i3;
        this.bottom -= i4;
    }

    public boolean contains(int i, int i2) {
        return this.left < this.right && this.top < this.bottom && i >= this.left && i < this.right && i2 >= this.top && i2 < this.bottom;
    }

    public boolean contains(int i, int i2, int i3, int i4) {
        return this.left < this.right && this.top < this.bottom && this.left <= i && this.top <= i2 && this.right >= i3 && this.bottom >= i4;
    }

    public boolean contains(Rect rect) {
        return this.left < this.right && this.top < this.bottom && this.left <= rect.left && this.top <= rect.top && this.right >= rect.right && this.bottom >= rect.bottom;
    }

    public boolean intersect(int i, int i2, int i3, int i4) {
        if (this.left < i3 && i < this.right && this.top < i4 && i2 < this.bottom) {
            if (this.left < i) {
                this.left = i;
            }
            if (this.top < i2) {
                this.top = i2;
            }
            if (this.right > i3) {
                this.right = i3;
            }
            if (this.bottom > i4) {
                this.bottom = i4;
                return true;
            }
            return true;
        }
        return false;
    }

    public boolean intersect(Rect rect) {
        return intersect(rect.left, rect.top, rect.right, rect.bottom);
    }

    public void intersectUnchecked(Rect rect) {
        this.left = Math.max(this.left, rect.left);
        this.top = Math.max(this.top, rect.top);
        this.right = Math.min(this.right, rect.right);
        this.bottom = Math.min(this.bottom, rect.bottom);
    }

    public boolean setIntersect(Rect rect, Rect rect2) {
        if (rect.left < rect2.right && rect2.left < rect.right && rect.top < rect2.bottom && rect2.top < rect.bottom) {
            this.left = Math.max(rect.left, rect2.left);
            this.top = Math.max(rect.top, rect2.top);
            this.right = Math.min(rect.right, rect2.right);
            this.bottom = Math.min(rect.bottom, rect2.bottom);
            return true;
        }
        return false;
    }

    public boolean intersects(int i, int i2, int i3, int i4) {
        return this.left < i3 && i < this.right && this.top < i4 && i2 < this.bottom;
    }

    public static boolean intersects(Rect rect, Rect rect2) {
        return rect.left < rect2.right && rect2.left < rect.right && rect.top < rect2.bottom && rect2.top < rect.bottom;
    }

    public void union(int i, int i2, int i3, int i4) {
        if (i < i3 && i2 < i4) {
            if (this.left < this.right && this.top < this.bottom) {
                if (this.left > i) {
                    this.left = i;
                }
                if (this.top > i2) {
                    this.top = i2;
                }
                if (this.right < i3) {
                    this.right = i3;
                }
                if (this.bottom < i4) {
                    this.bottom = i4;
                    return;
                }
                return;
            }
            this.left = i;
            this.top = i2;
            this.right = i3;
            this.bottom = i4;
        }
    }

    public void union(Rect rect) {
        union(rect.left, rect.top, rect.right, rect.bottom);
    }

    public void union(int i, int i2) {
        if (i < this.left) {
            this.left = i;
        } else if (i > this.right) {
            this.right = i;
        }
        if (i2 < this.top) {
            this.top = i2;
        } else if (i2 > this.bottom) {
            this.bottom = i2;
        }
    }

    public void sort() {
        if (this.left > this.right) {
            int i = this.left;
            this.left = this.right;
            this.right = i;
        }
        if (this.top > this.bottom) {
            int i2 = this.top;
            this.top = this.bottom;
            this.bottom = i2;
        }
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel parcel, int i) {
        parcel.writeInt(this.left);
        parcel.writeInt(this.top);
        parcel.writeInt(this.right);
        parcel.writeInt(this.bottom);
    }

    public void readFromParcel(Parcel parcel) {
        this.left = parcel.readInt();
        this.top = parcel.readInt();
        this.right = parcel.readInt();
        this.bottom = parcel.readInt();
    }

    public void scale(float f) {
        if (f != 1.0f) {
            this.left = (int) ((this.left * f) + 0.5f);
            this.top = (int) ((this.top * f) + 0.5f);
            this.right = (int) ((this.right * f) + 0.5f);
            this.bottom = (int) ((this.bottom * f) + 0.5f);
        }
    }
}
