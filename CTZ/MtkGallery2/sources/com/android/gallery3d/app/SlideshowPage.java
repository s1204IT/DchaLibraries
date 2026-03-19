package com.android.gallery3d.app;

import android.content.Intent;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.view.MotionEvent;
import com.android.gallery3d.R;
import com.android.gallery3d.app.SlideshowDataAdapter;
import com.android.gallery3d.common.Utils;
import com.android.gallery3d.data.ContentListener;
import com.android.gallery3d.data.MediaItem;
import com.android.gallery3d.data.MediaSet;
import com.android.gallery3d.data.Path;
import com.android.gallery3d.glrenderer.GLCanvas;
import com.android.gallery3d.ui.GLView;
import com.android.gallery3d.ui.SlideshowView;
import com.android.gallery3d.ui.SynchronizedHandler;
import com.android.gallery3d.util.Future;
import com.android.gallery3d.util.FutureListener;
import java.util.ArrayList;
import java.util.Random;

public class SlideshowPage extends ActivityState {
    private Handler mHandler;
    private Model mModel;
    private SlideshowView mSlideshowView;
    private Slide mPendingSlide = null;
    private boolean mIsActive = false;
    private final Intent mResultIntent = new Intent();
    private final GLView mRootPane = new GLView() {
        @Override
        protected void onLayout(boolean z, int i, int i2, int i3, int i4) {
            SlideshowPage.this.mSlideshowView.layout(0, 0, i3 - i, i4 - i2);
        }

        @Override
        protected boolean onTouch(MotionEvent motionEvent) {
            if (motionEvent.getAction() == 1) {
                SlideshowPage.this.onBackPressed();
            }
            return true;
        }

        @Override
        protected void renderBackground(GLCanvas gLCanvas) {
            gLCanvas.clearBuffer(getBackgroundColor());
        }
    };

    public interface Model {
        Future<Slide> nextSlide(FutureListener<Slide> futureListener);

        void pause();

        void resume();
    }

    public static class Slide {
        public Bitmap bitmap;
        public int index;
        public MediaItem item;

        public Slide(MediaItem mediaItem, int i, Bitmap bitmap) {
            this.bitmap = bitmap;
            this.item = mediaItem;
            this.index = i;
        }
    }

    @Override
    protected int getBackgroundColorId() {
        return R.color.slideshow_background;
    }

    @Override
    public void onCreate(Bundle bundle, Bundle bundle2) {
        super.onCreate(bundle, bundle2);
        this.mFlags |= 3;
        if (bundle.getBoolean("dream")) {
            this.mFlags |= 36;
        } else {
            this.mFlags |= 8;
        }
        this.mHandler = new SynchronizedHandler(this.mActivity.getGLRoot()) {
            @Override
            public void handleMessage(Message message) {
                switch (message.what) {
                    case 1:
                        SlideshowPage.this.loadNextBitmap();
                        return;
                    case 2:
                        SlideshowPage.this.showPendingBitmap();
                        return;
                    default:
                        throw new AssertionError();
                }
            }
        };
        initializeViews();
        initializeData(bundle);
    }

    private void loadNextBitmap() {
        this.mModel.nextSlide(new FutureListener<Slide>() {
            @Override
            public void onFutureDone(Future<Slide> future) {
                SlideshowPage.this.mPendingSlide = future.get();
                SlideshowPage.this.mHandler.sendEmptyMessage(2);
            }
        });
    }

    private void showPendingBitmap() {
        Slide slide = this.mPendingSlide;
        if (slide == null || (slide.bitmap == null && slide.index == -1)) {
            if (this.mIsActive) {
                this.mActivity.getStateManager().finishState(this);
            }
        } else {
            this.mSlideshowView.next(slide.bitmap, slide.item.getRotation(), slide.item);
            setStateResult(-1, this.mResultIntent.putExtra("media-item-path", slide.item.getPath().toString()).putExtra("photo-index", slide.index));
            this.mHandler.sendEmptyMessageDelayed(1, 3000L);
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        this.mIsActive = false;
        this.mModel.pause();
        this.mSlideshowView.release();
        this.mHandler.removeMessages(1);
        this.mHandler.removeMessages(2);
    }

    @Override
    public void onResume() {
        super.onResume();
        this.mIsActive = true;
        this.mModel.resume();
        if (this.mPendingSlide != null) {
            showPendingBitmap();
        } else {
            loadNextBitmap();
        }
    }

    private void initializeData(Bundle bundle) {
        String strReplace;
        boolean z = bundle.getBoolean("random-order", false);
        String string = bundle.getString("media-set-path");
        if (string != null) {
            strReplace = string.replace("/filter/camera_shortcut,", "");
        } else {
            strReplace = "";
        }
        MediaSet mediaSet = this.mActivity.getDataManager().getMediaSet(strReplace);
        if (z) {
            this.mModel = new SlideshowDataAdapter(this.mActivity, new ShuffleSource(mediaSet, bundle.getBoolean("repeat")), 0, null);
            setStateResult(-1, this.mResultIntent.putExtra("photo-index", 0));
            return;
        }
        int i = bundle.getInt("photo-index");
        String string2 = bundle.getString("media-item-path");
        this.mModel = new SlideshowDataAdapter(this.mActivity, new SequentialSource(mediaSet, bundle.getBoolean("repeat")), i, string2 != null ? Path.fromString(string2) : null);
        setStateResult(-1, this.mResultIntent.putExtra("photo-index", i));
    }

    private void initializeViews() {
        this.mSlideshowView = new SlideshowView(this.mActivity);
        this.mRootPane.addComponent(this.mSlideshowView);
        setContentPane(this.mRootPane);
    }

    private static MediaItem findMediaItem(MediaSet mediaSet, int i) {
        int subMediaSetCount = mediaSet.getSubMediaSetCount();
        int i2 = i;
        for (int i3 = 0; i3 < subMediaSetCount; i3++) {
            MediaSet subMediaSet = mediaSet.getSubMediaSet(i3);
            int totalMediaItemCount = subMediaSet.getTotalMediaItemCount();
            if (i2 < totalMediaItemCount) {
                return findMediaItem(subMediaSet, i2);
            }
            i2 -= totalMediaItemCount;
        }
        ArrayList<MediaItem> mediaItem = mediaSet.getMediaItem(i2, 1);
        if (mediaItem.isEmpty()) {
            return null;
        }
        return mediaItem.get(0);
    }

    private static class ShuffleSource implements SlideshowDataAdapter.SlideshowSource {
        private final MediaSet mMediaSet;
        private final boolean mRepeat;
        private final Random mRandom = new Random();
        private int[] mOrder = new int[0];
        private long mSourceVersion = -1;
        private int mLastIndex = -1;

        public ShuffleSource(MediaSet mediaSet, boolean z) {
            this.mMediaSet = (MediaSet) Utils.checkNotNull(mediaSet);
            this.mRepeat = z;
        }

        @Override
        public int findItemIndex(Path path, int i) {
            return i;
        }

        @Override
        public MediaItem getMediaItem(int i) {
            if ((!this.mRepeat && i >= this.mOrder.length) || this.mOrder.length == 0) {
                return null;
            }
            this.mLastIndex = this.mOrder[i % this.mOrder.length];
            MediaItem mediaItemFindMediaItem = SlideshowPage.findMediaItem(this.mMediaSet, this.mLastIndex);
            for (int i2 = 0; i2 < 5 && mediaItemFindMediaItem == null; i2++) {
                Log.w("Gallery2/SlideshowPage", "fail to find image: " + this.mLastIndex);
                this.mLastIndex = this.mRandom.nextInt(this.mOrder.length);
                mediaItemFindMediaItem = SlideshowPage.findMediaItem(this.mMediaSet, this.mLastIndex);
            }
            return mediaItemFindMediaItem;
        }

        @Override
        public long reload() {
            long jReloadForSlideShow = this.mMediaSet.reloadForSlideShow();
            if (jReloadForSlideShow != this.mSourceVersion) {
                this.mSourceVersion = jReloadForSlideShow;
                int totalMediaItemCount = this.mMediaSet.getTotalMediaItemCount();
                if (totalMediaItemCount != this.mOrder.length) {
                    generateOrderArray(totalMediaItemCount);
                }
            }
            return jReloadForSlideShow;
        }

        private void generateOrderArray(int i) {
            if (this.mOrder.length != i) {
                this.mOrder = new int[i];
                for (int i2 = 0; i2 < i; i2++) {
                    this.mOrder[i2] = i2;
                }
            }
            int i3 = i - 1;
            for (int i4 = i3; i4 > 0; i4--) {
                Utils.swap(this.mOrder, i4, this.mRandom.nextInt(i4 + 1));
            }
            if (this.mOrder[0] == this.mLastIndex && i > 1) {
                Utils.swap(this.mOrder, 0, this.mRandom.nextInt(i3) + 1);
            }
        }

        @Override
        public void addContentListener(ContentListener contentListener) {
            this.mMediaSet.addContentListener(contentListener);
        }

        @Override
        public void removeContentListener(ContentListener contentListener) {
            this.mMediaSet.removeContentListener(contentListener);
        }

        @Override
        public void stopReload() {
            if (this.mMediaSet != null) {
                this.mMediaSet.stopReload();
            }
        }
    }

    private static class SequentialSource implements SlideshowDataAdapter.SlideshowSource {
        private ArrayList<MediaItem> mData = new ArrayList<>();
        private int mDataStart = 0;
        private long mDataVersion = -1;
        private final MediaSet mMediaSet;
        private final boolean mRepeat;

        public SequentialSource(MediaSet mediaSet, boolean z) {
            this.mMediaSet = mediaSet;
            this.mRepeat = z;
        }

        @Override
        public int findItemIndex(Path path, int i) {
            return this.mMediaSet.getIndexOfItem(path, i);
        }

        @Override
        public MediaItem getMediaItem(int i) {
            int size = this.mDataStart + this.mData.size();
            if (this.mRepeat) {
                int mediaItemCount = this.mMediaSet.getMediaItemCount();
                if (mediaItemCount == 0) {
                    return null;
                }
                i %= mediaItemCount;
            }
            if (i < this.mDataStart || i >= size) {
                this.mData = this.mMediaSet.getMediaItem(i, 32);
                this.mDataStart = i;
                size = this.mData.size() + i;
            }
            if (i < this.mDataStart || i >= size) {
                return null;
            }
            return this.mData.get(i - this.mDataStart);
        }

        @Override
        public long reload() {
            long jReloadForSlideShow = this.mMediaSet.reloadForSlideShow();
            if (jReloadForSlideShow != this.mDataVersion) {
                this.mDataVersion = jReloadForSlideShow;
                this.mData.clear();
            }
            return this.mDataVersion;
        }

        @Override
        public void addContentListener(ContentListener contentListener) {
            this.mMediaSet.addContentListener(contentListener);
        }

        @Override
        public void removeContentListener(ContentListener contentListener) {
            this.mMediaSet.removeContentListener(contentListener);
        }

        @Override
        public void stopReload() {
            if (this.mMediaSet != null) {
                this.mMediaSet.stopReload();
            }
        }
    }
}
