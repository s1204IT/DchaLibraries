package android.gesture;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;
import android.os.Parcel;
import android.os.Parcelable;
import android.util.Log;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.concurrent.atomic.AtomicInteger;

public class Gesture implements Parcelable {
    private static final boolean BITMAP_RENDERING_ANTIALIAS = true;
    private static final boolean BITMAP_RENDERING_DITHER = true;
    private static final int BITMAP_RENDERING_WIDTH = 2;
    private static final long GESTURE_ID_BASE = System.currentTimeMillis();
    private static final AtomicInteger sGestureCount = new AtomicInteger(0);
    public static final Parcelable.Creator<Gesture> CREATOR = new Parcelable.Creator<Gesture>() {
        @Override
        public Gesture createFromParcel(Parcel parcel) {
            Gesture gestureDeserialize;
            long j = parcel.readLong();
            DataInputStream dataInputStream = new DataInputStream(new ByteArrayInputStream(parcel.createByteArray()));
            try {
                try {
                    gestureDeserialize = Gesture.deserialize(dataInputStream);
                } catch (IOException e) {
                    Log.e(GestureConstants.LOG_TAG, "Error reading Gesture from parcel:", e);
                    GestureUtils.closeStream(dataInputStream);
                    gestureDeserialize = null;
                }
                if (gestureDeserialize != null) {
                    gestureDeserialize.mGestureID = j;
                }
                return gestureDeserialize;
            } finally {
                GestureUtils.closeStream(dataInputStream);
            }
        }

        @Override
        public Gesture[] newArray(int i) {
            return new Gesture[i];
        }
    };
    private final RectF mBoundingBox = new RectF();
    private final ArrayList<GestureStroke> mStrokes = new ArrayList<>();
    private long mGestureID = GESTURE_ID_BASE + ((long) sGestureCount.incrementAndGet());

    public Object clone() {
        Gesture gesture = new Gesture();
        gesture.mBoundingBox.set(this.mBoundingBox.left, this.mBoundingBox.top, this.mBoundingBox.right, this.mBoundingBox.bottom);
        int size = this.mStrokes.size();
        for (int i = 0; i < size; i++) {
            gesture.mStrokes.add((GestureStroke) this.mStrokes.get(i).clone());
        }
        return gesture;
    }

    public ArrayList<GestureStroke> getStrokes() {
        return this.mStrokes;
    }

    public int getStrokesCount() {
        return this.mStrokes.size();
    }

    public void addStroke(GestureStroke gestureStroke) {
        this.mStrokes.add(gestureStroke);
        this.mBoundingBox.union(gestureStroke.boundingBox);
    }

    public float getLength() {
        ArrayList<GestureStroke> arrayList = this.mStrokes;
        int size = arrayList.size();
        int i = 0;
        for (int i2 = 0; i2 < size; i2++) {
            i = (int) (i + arrayList.get(i2).length);
        }
        return i;
    }

    public RectF getBoundingBox() {
        return this.mBoundingBox;
    }

    public Path toPath() {
        return toPath(null);
    }

    public Path toPath(Path path) {
        if (path == null) {
            path = new Path();
        }
        ArrayList<GestureStroke> arrayList = this.mStrokes;
        int size = arrayList.size();
        for (int i = 0; i < size; i++) {
            path.addPath(arrayList.get(i).getPath());
        }
        return path;
    }

    public Path toPath(int i, int i2, int i3, int i4) {
        return toPath(null, i, i2, i3, i4);
    }

    public Path toPath(Path path, int i, int i2, int i3, int i4) {
        if (path == null) {
            path = new Path();
        }
        ArrayList<GestureStroke> arrayList = this.mStrokes;
        int size = arrayList.size();
        for (int i5 = 0; i5 < size; i5++) {
            int i6 = 2 * i3;
            path.addPath(arrayList.get(i5).toPath(i - i6, i2 - i6, i4));
        }
        return path;
    }

    void setID(long j) {
        this.mGestureID = j;
    }

    public long getID() {
        return this.mGestureID;
    }

    public Bitmap toBitmap(int i, int i2, int i3, int i4, int i5) {
        Bitmap bitmapCreateBitmap = Bitmap.createBitmap(i, i2, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmapCreateBitmap);
        float f = i3;
        canvas.translate(f, f);
        Paint paint = new Paint();
        paint.setAntiAlias(true);
        paint.setDither(true);
        paint.setColor(i5);
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeJoin(Paint.Join.ROUND);
        paint.setStrokeCap(Paint.Cap.ROUND);
        paint.setStrokeWidth(2.0f);
        ArrayList<GestureStroke> arrayList = this.mStrokes;
        int size = arrayList.size();
        for (int i6 = 0; i6 < size; i6++) {
            int i7 = 2 * i3;
            canvas.drawPath(arrayList.get(i6).toPath(i - i7, i2 - i7, i4), paint);
        }
        return bitmapCreateBitmap;
    }

    public Bitmap toBitmap(int i, int i2, int i3, int i4) {
        Bitmap bitmapCreateBitmap = Bitmap.createBitmap(i, i2, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmapCreateBitmap);
        Paint paint = new Paint();
        paint.setAntiAlias(true);
        paint.setDither(true);
        paint.setColor(i4);
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeJoin(Paint.Join.ROUND);
        paint.setStrokeCap(Paint.Cap.ROUND);
        paint.setStrokeWidth(2.0f);
        Path path = toPath();
        RectF rectF = new RectF();
        path.computeBounds(rectF, true);
        int i5 = 2 * i3;
        float fWidth = (i - i5) / rectF.width();
        float fHeight = (i2 - i5) / rectF.height();
        if (fWidth <= fHeight) {
            fHeight = fWidth;
        }
        paint.setStrokeWidth(2.0f / fHeight);
        path.offset((-rectF.left) + ((i - (rectF.width() * fHeight)) / 2.0f), (-rectF.top) + ((i2 - (rectF.height() * fHeight)) / 2.0f));
        float f = i3;
        canvas.translate(f, f);
        canvas.scale(fHeight, fHeight);
        canvas.drawPath(path, paint);
        return bitmapCreateBitmap;
    }

    void serialize(DataOutputStream dataOutputStream) throws IOException {
        ArrayList<GestureStroke> arrayList = this.mStrokes;
        int size = arrayList.size();
        dataOutputStream.writeLong(this.mGestureID);
        dataOutputStream.writeInt(size);
        for (int i = 0; i < size; i++) {
            arrayList.get(i).serialize(dataOutputStream);
        }
    }

    static Gesture deserialize(DataInputStream dataInputStream) throws IOException {
        Gesture gesture = new Gesture();
        gesture.mGestureID = dataInputStream.readLong();
        int i = dataInputStream.readInt();
        for (int i2 = 0; i2 < i; i2++) {
            gesture.addStroke(GestureStroke.deserialize(dataInputStream));
        }
        return gesture;
    }

    @Override
    public void writeToParcel(Parcel parcel, int i) {
        boolean z;
        parcel.writeLong(this.mGestureID);
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream(32768);
        DataOutputStream dataOutputStream = new DataOutputStream(byteArrayOutputStream);
        try {
            try {
                serialize(dataOutputStream);
                z = true;
            } catch (IOException e) {
                Log.e(GestureConstants.LOG_TAG, "Error writing Gesture to parcel:", e);
                GestureUtils.closeStream(dataOutputStream);
                GestureUtils.closeStream(byteArrayOutputStream);
                z = false;
            }
            if (z) {
                parcel.writeByteArray(byteArrayOutputStream.toByteArray());
            }
        } finally {
            GestureUtils.closeStream(dataOutputStream);
            GestureUtils.closeStream(byteArrayOutputStream);
        }
    }

    @Override
    public int describeContents() {
        return 0;
    }
}
