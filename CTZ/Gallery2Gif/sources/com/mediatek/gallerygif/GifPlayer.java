package com.mediatek.gallerygif;

import android.content.Context;
import android.graphics.Bitmap;
import android.os.ParcelFileDescriptor;
import com.mediatek.gallerybasic.base.MediaData;
import com.mediatek.gallerybasic.base.Player;
import com.mediatek.gallerybasic.base.ThumbType;
import com.mediatek.gallerybasic.base.Work;
import com.mediatek.gallerybasic.gl.GLIdleExecuter;
import com.mediatek.gallerybasic.gl.MBitmapTexture;
import com.mediatek.gallerybasic.gl.MGLCanvas;
import com.mediatek.gallerybasic.gl.MTexture;
import com.mediatek.gallerybasic.platform.PlatformHelper;
import com.mediatek.gallerybasic.util.BitmapUtils;
import com.mediatek.gallerybasic.util.Log;
import com.mediatek.gallerybasic.util.Utils;

public class GifPlayer extends Player {
    public static final int FRAME_COUNT_MAX = 20;
    public static final float RESIZE_RATIO = 3.0f;
    private static final String TAG = "MtkGallery2/GifPlayer";
    private int mCurrentFrameDuration;
    private int mCurrentFrameIndex;
    private DecodeJob mCurrentJob;
    private byte[] mEncryptBuffer;
    private ParcelFileDescriptor mFD;
    private int mFrameCount;
    private GifDecoderWrapper mGifDecoderWrapper;
    private GLIdleExecuter mGlIdleExecuter;
    private int mHeight;
    private boolean mIsCancelled;
    private boolean mIsPlaying;
    private Object mLock;
    private Bitmap mNextBitmap;
    private int mTargetSize;
    private Bitmap mTempBitmap;
    private MBitmapTexture mTexture;
    private ThumbType mThumbType;
    private int mWidth;
    private Work<Void> recycleJob;

    public GifPlayer(Context context, MediaData mediaData, Player.OutputType outputType, ThumbType thumbType, GLIdleExecuter gLIdleExecuter) {
        super(context, mediaData, outputType);
        this.mLock = new Object();
        this.mIsPlaying = false;
        this.mEncryptBuffer = null;
        this.recycleJob = new Work<Void>() {
            @Override
            public boolean isCanceled() {
                return false;
            }

            @Override
            public Void run() {
                synchronized (GifPlayer.this.mLock) {
                    if (GifPlayer.this.mGifDecoderWrapper != null) {
                        GifPlayer.this.mGifDecoderWrapper.close();
                        GifPlayer.this.mGifDecoderWrapper = null;
                    }
                }
                return null;
            }
        };
        this.mWidth = this.mMediaData.width;
        this.mHeight = this.mMediaData.height;
        this.mThumbType = thumbType;
        this.mGlIdleExecuter = gLIdleExecuter;
    }

    @Override
    protected boolean onPrepare() {
        return true;
    }

    @Override
    protected synchronized void onRelease() {
        Log.d(TAG, "<onRelease> caption = " + this.mMediaData.caption);
        removeAllMessages();
        if (this.mGlIdleExecuter != null) {
            this.mGlIdleExecuter.addOnGLIdleCmd(new GLIdleExecuter.GLIdleCmd() {
                @Override
                public boolean onGLIdle(MGLCanvas mGLCanvas) {
                    if (GifPlayer.this.mTexture != null) {
                        GifPlayer.this.mTexture.recycle();
                        GifPlayer.this.mTexture = null;
                        return false;
                    }
                    return false;
                }
            });
        } else if (this.mTexture != null) {
            this.mTexture.recycle();
            this.mTexture = null;
        }
    }

    @Override
    protected boolean onStart() {
        Log.d(TAG, "<onStart> caption = " + this.mMediaData.caption);
        if (this.mEncryptBuffer != null) {
            this.mGifDecoderWrapper = GifDecoderWrapper.createGifDecoderWrapper(this.mEncryptBuffer, 0, this.mEncryptBuffer.length);
        } else if (this.mMediaData.filePath != null && !this.mMediaData.filePath.equals("")) {
            this.mGifDecoderWrapper = GifDecoderWrapper.createGifDecoderWrapper(this.mMediaData.filePath);
        } else if (this.mMediaData.uri != null) {
            try {
                this.mFD = this.mContext.getContentResolver().openFileDescriptor(this.mMediaData.uri, "r");
                this.mGifDecoderWrapper = GifDecoderWrapper.createGifDecoderWrapper(this.mFD.getFileDescriptor());
            } catch (Exception e) {
                Log.w(TAG, "<onStart> Exception", e);
                Utils.closeSilently(this.mFD);
                this.mFD = null;
                return false;
            }
        }
        if (this.mGifDecoderWrapper == null) {
            return false;
        }
        if (this.mIsCancelled) {
            recycleDecoderWrapper();
            return false;
        }
        this.mWidth = this.mGifDecoderWrapper.getWidth();
        this.mHeight = this.mGifDecoderWrapper.getHeight();
        this.mFrameCount = getGifTotalFrameCount();
        if (this.mFrameCount <= 0 || this.mWidth <= 0 || this.mHeight <= 0) {
            Log.d(TAG, "<onStart> broken gif, path: " + this.mMediaData.filePath + ", mFrameCount " + this.mFrameCount + ", mWidth " + this.mWidth + ", mHeight " + this.mHeight);
            return false;
        }
        this.mTargetSize = this.mThumbType.getTargetSize();
        Log.d(TAG, " The image width and height = " + this.mWidth + " " + this.mHeight + " mThumbType.getTargetSize() = " + this.mTargetSize + " mFrameCount = " + this.mFrameCount);
        PlatformHelper.submitJob(new DecodeJob(0));
        this.mIsPlaying = true;
        this.mCurrentFrameIndex = 0;
        this.mCurrentFrameDuration = getGifFrameDuration(this.mCurrentFrameIndex);
        sendFrameAvailable();
        sendPlayFrameDelayed(0);
        return true;
    }

    @Override
    protected synchronized boolean onPause() {
        Log.d(TAG, "<onPause> caption = " + this.mMediaData.caption);
        this.mIsPlaying = false;
        removeAllMessages();
        if (this.mCurrentJob != null) {
            this.mCurrentJob.cancel();
            this.mCurrentJob = null;
        }
        recycleDecoderWrapper();
        if (this.mNextBitmap != null) {
            this.mNextBitmap.recycle();
            this.mNextBitmap = null;
        }
        if (this.mTempBitmap != null) {
            this.mTempBitmap.recycle();
            this.mTempBitmap = null;
        }
        if (this.mFD != null) {
            Utils.closeSilently(this.mFD);
            this.mFD = null;
        }
        return true;
    }

    @Override
    protected boolean onStop() {
        Log.d(TAG, "<onStop> caption = " + this.mMediaData.caption);
        removeAllMessages();
        this.mCurrentFrameIndex = 0;
        this.mCurrentFrameDuration = getGifFrameDuration(this.mCurrentFrameIndex);
        return true;
    }

    @Override
    public void setBuffer(byte[] bArr) {
        this.mEncryptBuffer = bArr;
    }

    @Override
    public int getOutputWidth() {
        if (this.mTexture != null) {
            return this.mWidth;
        }
        return 0;
    }

    @Override
    public int getOutputHeight() {
        if (this.mTexture != null) {
            return this.mHeight;
        }
        return 0;
    }

    @Override
    protected synchronized void onPlayFrame() {
        if (this.mIsPlaying) {
            if (this.mNextBitmap != null) {
                this.mNextBitmap.recycle();
                this.mNextBitmap = null;
            }
            this.mNextBitmap = this.mTempBitmap;
            this.mTempBitmap = null;
            sendFrameAvailable();
            sendPlayFrameDelayed(this.mCurrentFrameDuration);
            if (this.mCurrentJob != null) {
                return;
            }
            this.mCurrentFrameIndex++;
            if (this.mCurrentFrameIndex >= this.mFrameCount) {
                this.mCurrentFrameIndex = 0;
            }
            this.mCurrentFrameDuration = getGifFrameDuration(this.mCurrentFrameIndex);
            if (this.mCurrentFrameDuration != -1) {
                this.mCurrentJob = new DecodeJob(this.mCurrentFrameIndex);
                PlatformHelper.submitJob(this.mCurrentJob);
            }
        }
    }

    @Override
    public synchronized MTexture getTexture(MGLCanvas mGLCanvas) {
        if (this.mNextBitmap != null) {
            if (this.mTexture != null) {
                this.mTexture.recycle();
            }
            this.mTexture = new MBitmapTexture(this.mNextBitmap);
            this.mNextBitmap = null;
        }
        return this.mTexture;
    }

    @Override
    public void onCancel() {
        this.mIsCancelled = true;
    }

    private int getGifTotalFrameCount() {
        if (this.mGifDecoderWrapper == null) {
            return 0;
        }
        return this.mGifDecoderWrapper.getTotalFrameCount();
    }

    private int getGifFrameDuration(int i) {
        int frameDuration;
        synchronized (this.mLock) {
            if (this.mGifDecoderWrapper != null) {
                frameDuration = this.mGifDecoderWrapper.getFrameDuration(i);
            } else {
                frameDuration = -1;
            }
        }
        return frameDuration;
    }

    class DecodeJob implements Work<Bitmap> {
        private boolean mCanceled = false;
        private int mIndex;

        public DecodeJob(int i) {
            this.mIndex = i;
        }

        @Override
        public Bitmap run() {
            Bitmap bitmapResizeBitmap = null;
            if (!isCanceled() && GifPlayer.this.mGifDecoderWrapper != null) {
                synchronized (GifPlayer.this.mLock) {
                    if (GifPlayer.this.mGifDecoderWrapper != null) {
                        bitmapResizeBitmap = GifPlayer.this.resizeBitmap(GifPlayer.this.mGifDecoderWrapper.getFrameBitmap(this.mIndex));
                    }
                }
                onDoFinished(bitmapResizeBitmap);
                return bitmapResizeBitmap;
            }
            Log.d(GifPlayer.TAG, "<DecodeJob.onDo> isCanceled() & mGifDecoderWrapper:" + isCanceled() + " " + GifPlayer.this.mGifDecoderWrapper);
            onDoFinished(null);
            return null;
        }

        private void onDoFinished(Bitmap bitmap) {
            synchronized (GifPlayer.this) {
                if (GifPlayer.this.mTempBitmap != null) {
                    GifPlayer.this.mTempBitmap.recycle();
                    GifPlayer.this.mTempBitmap = null;
                }
                if (bitmap != null) {
                    if (!isCanceled()) {
                        GifPlayer.this.mTempBitmap = bitmap;
                    } else {
                        bitmap.recycle();
                    }
                }
                GifPlayer.this.mCurrentJob = null;
            }
        }

        @Override
        public boolean isCanceled() {
            return this.mCanceled;
        }

        public void cancel() {
            this.mCanceled = true;
        }
    }

    private Bitmap resizeBitmap(Bitmap bitmap) {
        Bitmap bitmapResizeDownBySideLength;
        if (this.mWidth == 0) {
            return BitmapUtils.replaceBackgroundColor(bitmap, true);
        }
        float f = this.mHeight / this.mWidth;
        if (this.mThumbType == ThumbType.MIDDLE && f > 3.0f) {
            bitmapResizeDownBySideLength = BitmapUtils.resizeDownBySideLength(bitmap, (int) (this.mTargetSize * 3.0f), true);
        } else {
            bitmapResizeDownBySideLength = BitmapUtils.resizeDownBySideLength(bitmap, this.mTargetSize, true);
        }
        return BitmapUtils.replaceBackgroundColor(bitmapResizeDownBySideLength, true);
    }

    private void resetFrameCount() {
        if (this.mFrameCount > 20) {
            this.mFrameCount = 20;
            Log.d(TAG, "reset frame count " + this.mFrameCount);
        }
    }

    private void recycleDecoderWrapper() {
        PlatformHelper.submitJob(this.recycleJob);
    }
}
