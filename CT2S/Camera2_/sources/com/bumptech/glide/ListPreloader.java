package com.bumptech.glide;

import android.annotation.TargetApi;
import android.os.Build;
import android.widget.AbsListView;
import com.bumptech.glide.request.GlideAnimation;
import com.bumptech.glide.request.target.BaseTarget;
import com.bumptech.glide.request.target.Target;
import java.util.ArrayDeque;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;

public abstract class ListPreloader<T> implements AbsListView.OnScrollListener {
    private boolean isIncreasing = true;
    private int lastEnd;
    private int lastFirstVisible;
    private int lastStart;
    private final int maxPreload;
    private final PreloadTargetQueue preloadTargetQueue;
    private int totalItemCount;

    protected abstract int[] getDimensions(T t);

    protected abstract List<T> getItems(int i, int i2);

    protected abstract GenericRequestBuilder getRequestBuilder(T t);

    public ListPreloader(int maxPreload) {
        this.maxPreload = maxPreload;
        this.preloadTargetQueue = new PreloadTargetQueue(maxPreload + 1);
    }

    @Override
    public void onScrollStateChanged(AbsListView absListView, int i) {
    }

    @Override
    public void onScroll(AbsListView absListView, int firstVisible, int visibleCount, int totalCount) {
        this.totalItemCount = totalCount;
        if (firstVisible > this.lastFirstVisible) {
            preload(firstVisible + visibleCount, true);
        } else if (firstVisible < this.lastFirstVisible) {
            preload(firstVisible, false);
        }
        this.lastFirstVisible = firstVisible;
    }

    private void preload(int start, boolean increasing) {
        if (this.isIncreasing != increasing) {
            this.isIncreasing = increasing;
            cancelAll();
        }
        preload(start, (increasing ? this.maxPreload : -this.maxPreload) + start);
    }

    private void preload(int from, int to) {
        int start;
        int end;
        if (from < to) {
            start = Math.max(this.lastEnd, from);
            end = to;
        } else {
            start = to;
            end = Math.min(this.lastStart, from);
        }
        int end2 = Math.min(this.totalItemCount, end);
        int start2 = Math.min(this.totalItemCount, Math.max(0, start));
        List<T> items = getItems(start2, end2);
        if (from < to) {
            int numItems = items.size();
            for (int i = 0; i < numItems; i++) {
                preloadItem(items, i);
            }
        } else {
            for (int i2 = items.size() - 1; i2 >= 0; i2--) {
                preloadItem(items, i2);
            }
        }
        this.lastStart = start2;
        this.lastEnd = end2;
    }

    private void preloadItem(List<T> items, int position) {
        T item = items.get(position);
        int[] dimensions = getDimensions(item);
        if (dimensions != null) {
            getRequestBuilder(item).into(this.preloadTargetQueue.next(dimensions[0], dimensions[1]));
        }
    }

    private void cancelAll() {
        for (int i = 0; i < this.maxPreload; i++) {
            Glide.clear(this.preloadTargetQueue.next(0, 0));
        }
    }

    private static class PreloadTargetQueue {
        private final Queue<PreloadTarget> queue;

        @TargetApi(9)
        private PreloadTargetQueue(int size) {
            if (Build.VERSION.SDK_INT >= 9) {
                this.queue = new ArrayDeque(size);
            } else {
                this.queue = new LinkedList();
            }
            for (int i = 0; i < size; i++) {
                this.queue.offer(new PreloadTarget());
            }
        }

        public PreloadTarget next(int width, int height) {
            PreloadTarget result = this.queue.poll();
            this.queue.offer(result);
            result.photoWidth = width;
            result.photoHeight = height;
            return result;
        }
    }

    private static class PreloadTarget extends BaseTarget {
        private int photoHeight;
        private int photoWidth;

        private PreloadTarget() {
        }

        @Override
        public void onResourceReady(Object resource, GlideAnimation glideAnimation) {
        }

        @Override
        public void getSize(Target.SizeReadyCallback cb) {
            cb.onSizeReady(this.photoWidth, this.photoHeight);
        }
    }
}
