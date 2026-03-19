package android.graphics;

import android.graphics.Shader;

public class BitmapShader extends Shader {
    public final Bitmap mBitmap;
    private Shader.TileMode mTileX;
    private Shader.TileMode mTileY;

    private static native long nativeCreate(Bitmap bitmap, int i, int i2);

    public BitmapShader(Bitmap bitmap, Shader.TileMode tileX, Shader.TileMode tileY) {
        this.mBitmap = bitmap;
        this.mTileX = tileX;
        this.mTileY = tileY;
        init(nativeCreate(bitmap, tileX.nativeInt, tileY.nativeInt));
    }

    @Override
    protected Shader copy() {
        BitmapShader copy = new BitmapShader(this.mBitmap, this.mTileX, this.mTileY);
        copyLocalMatrix(copy);
        return copy;
    }
}
