package jp.co.omronsoft.android.emoji;

import android.graphics.Canvas;
import android.graphics.Paint;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;

public class Movie {
    private final int mNativeMovie;

    public static native Movie decodeByteArray(byte[] bArr, int i, int i2);

    public static native Movie decodeStream(InputStream inputStream);

    private native void deleteMovie(int i);

    public native void draw(Canvas canvas, float f, float f2, Paint paint);

    public native int duration();

    public native int height();

    public native boolean isOpaque();

    public native boolean setTime(int i);

    public native int width();

    private Movie(int nativeMovie) {
        if (nativeMovie == 0) {
            throw new RuntimeException("native movie creation failed");
        }
        this.mNativeMovie = nativeMovie;
    }

    public void draw(Canvas canvas, float x, float y) {
        draw(canvas, x, y, null);
    }

    public static Movie decodeFile(String pathName) {
        try {
            InputStream is = new FileInputStream(pathName);
            return decodeTempStream(is);
        } catch (FileNotFoundException e) {
            return null;
        }
    }

    private static Movie decodeTempStream(InputStream is) {
        Movie moov = null;
        try {
            moov = decodeStream(is);
            is.close();
            return moov;
        } catch (IOException e) {
            return moov;
        }
    }

    protected void finalize() throws Throwable {
        deleteMovie(this.mNativeMovie);
        super.finalize();
    }
}
