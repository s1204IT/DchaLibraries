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
        protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
            SlideshowPage.this.mSlideshowView.layout(0, 0, right - left, bottom - top);
        }

        @Override
        protected boolean onTouch(MotionEvent event) {
            if (event.getAction() == 1) {
                SlideshowPage.this.onBackPressed();
            }
            return true;
        }

        @Override
        protected void renderBackground(GLCanvas canvas) {
            canvas.clearBuffer(getBackgroundColor());
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

        public Slide(MediaItem item, int index, Bitmap bitmap) {
            this.bitmap = bitmap;
            this.item = item;
            this.index = index;
        }
    }

    @Override
    protected int getBackgroundColorId() {
        return R.color.slideshow_background;
    }

    @Override
    public void onCreate(Bundle data, Bundle restoreState) {
        super.onCreate(data, restoreState);
        this.mFlags |= 3;
        if (data.getBoolean("dream")) {
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
        initializeData(data);
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
        if (slide == null) {
            if (this.mIsActive) {
                this.mActivity.getStateManager().finishState(this);
            }
        } else {
            this.mSlideshowView.next(slide.bitmap, slide.item.getRotation());
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

    private void initializeData(Bundle data) {
        boolean random = data.getBoolean("random-order", false);
        String mediaPath = data.getString("media-set-path");
        MediaSet mediaSet = this.mActivity.getDataManager().getMediaSet(FilterUtils.newFilterPath(mediaPath, 1));
        if (random) {
            boolean repeat = data.getBoolean("repeat");
            this.mModel = new SlideshowDataAdapter(this.mActivity, new ShuffleSource(mediaSet, repeat), 0, null);
            setStateResult(-1, this.mResultIntent.putExtra("photo-index", 0));
        } else {
            int index = data.getInt("photo-index");
            String itemPath = data.getString("media-item-path");
            Path path = itemPath != null ? Path.fromString(itemPath) : null;
            boolean repeat2 = data.getBoolean("repeat");
            this.mModel = new SlideshowDataAdapter(this.mActivity, new SequentialSource(mediaSet, repeat2), index, path);
            setStateResult(-1, this.mResultIntent.putExtra("photo-index", index));
        }
    }

    private void initializeViews() {
        this.mSlideshowView = new SlideshowView();
        this.mRootPane.addComponent(this.mSlideshowView);
        setContentPane(this.mRootPane);
    }

    private static MediaItem findMediaItem(MediaSet mediaSet, int index) {
        int n = mediaSet.getSubMediaSetCount();
        for (int i = 0; i < n; i++) {
            MediaSet subset = mediaSet.getSubMediaSet(i);
            int count = subset.getTotalMediaItemCount();
            if (index < count) {
                return findMediaItem(subset, index);
            }
            index -= count;
        }
        ArrayList<MediaItem> list = mediaSet.getMediaItem(index, 1);
        if (list.isEmpty()) {
            return null;
        }
        return list.get(0);
    }

    private static class ShuffleSource implements SlideshowDataAdapter.SlideshowSource {
        private final MediaSet mMediaSet;
        private final boolean mRepeat;
        private final Random mRandom = new Random();
        private int[] mOrder = new int[0];
        private long mSourceVersion = -1;
        private int mLastIndex = -1;

        public ShuffleSource(MediaSet mediaSet, boolean repeat) {
            this.mMediaSet = (MediaSet) Utils.checkNotNull(mediaSet);
            this.mRepeat = repeat;
        }

        @Override
        public int findItemIndex(Path path, int hint) {
            return hint;
        }

        @Override
        public MediaItem getMediaItem(int index) {
            MediaItem item = null;
            if ((this.mRepeat || index < this.mOrder.length) && this.mOrder.length != 0) {
                this.mLastIndex = this.mOrder[index % this.mOrder.length];
                item = SlideshowPage.findMediaItem(this.mMediaSet, this.mLastIndex);
                for (int i = 0; i < 5 && item == null; i++) {
                    Log.w("SlideshowPage", "fail to find image: " + this.mLastIndex);
                    this.mLastIndex = this.mRandom.nextInt(this.mOrder.length);
                    item = SlideshowPage.findMediaItem(this.mMediaSet, this.mLastIndex);
                }
            }
            return item;
        }

        @Override
        public long reload() {
            long version = this.mMediaSet.reload();
            if (version != this.mSourceVersion) {
                this.mSourceVersion = version;
                int count = this.mMediaSet.getTotalMediaItemCount();
                if (count != this.mOrder.length) {
                    generateOrderArray(count);
                }
            }
            return version;
        }

        private void generateOrderArray(int totalCount) {
            if (this.mOrder.length != totalCount) {
                this.mOrder = new int[totalCount];
                for (int i = 0; i < totalCount; i++) {
                    this.mOrder[i] = i;
                }
            }
            for (int i2 = totalCount - 1; i2 > 0; i2--) {
                Utils.swap(this.mOrder, i2, this.mRandom.nextInt(i2 + 1));
            }
            if (this.mOrder[0] == this.mLastIndex && totalCount > 1) {
                Utils.swap(this.mOrder, 0, this.mRandom.nextInt(totalCount - 1) + 1);
            }
        }

        @Override
        public void addContentListener(ContentListener listener) {
            this.mMediaSet.addContentListener(listener);
        }

        @Override
        public void removeContentListener(ContentListener listener) {
            this.mMediaSet.removeContentListener(listener);
        }
    }

    private static class SequentialSource implements SlideshowDataAdapter.SlideshowSource {
        private ArrayList<MediaItem> mData = new ArrayList<>();
        private int mDataStart = 0;
        private long mDataVersion = -1;
        private final MediaSet mMediaSet;
        private final boolean mRepeat;

        public SequentialSource(MediaSet mediaSet, boolean repeat) {
            this.mMediaSet = mediaSet;
            this.mRepeat = repeat;
        }

        @Override
        public int findItemIndex(Path path, int hint) {
            return this.mMediaSet.getIndexOfItem(path, hint);
        }

        @Override
        public MediaItem getMediaItem(int index) {
            int dataEnd = this.mDataStart + this.mData.size();
            if (this.mRepeat) {
                int count = this.mMediaSet.getMediaItemCount();
                if (count == 0) {
                    return null;
                }
                index %= count;
            }
            if (index < this.mDataStart || index >= dataEnd) {
                this.mData = this.mMediaSet.getMediaItem(index, 32);
                this.mDataStart = index;
                dataEnd = index + this.mData.size();
            }
            if (index < this.mDataStart || index >= dataEnd) {
                return null;
            }
            return this.mData.get(index - this.mDataStart);
        }

        @Override
        public long reload() {
            long version = this.mMediaSet.reload();
            if (version != this.mDataVersion) {
                this.mDataVersion = version;
                this.mData.clear();
            }
            return this.mDataVersion;
        }

        @Override
        public void addContentListener(ContentListener listener) {
            this.mMediaSet.addContentListener(listener);
        }

        @Override
        public void removeContentListener(ContentListener listener) {
            this.mMediaSet.removeContentListener(listener);
        }
    }
}
