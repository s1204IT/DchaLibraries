package com.bumptech.glide.request;

public class ThumbnailRequestCoordinator implements RequestCoordinator, Request {
    private RequestCoordinator coordinator;
    private Request full;
    private Request thumb;

    public ThumbnailRequestCoordinator() {
        this(null);
    }

    public ThumbnailRequestCoordinator(RequestCoordinator coordinator) {
        this.coordinator = coordinator;
    }

    public void setRequests(Request full, Request thumb) {
        this.full = full;
        this.thumb = thumb;
    }

    @Override
    public boolean canSetImage(Request request) {
        return parentCanSetImage() && (request == this.full || !this.full.isComplete());
    }

    private boolean parentCanSetImage() {
        return this.coordinator == null || this.coordinator.canSetImage(this);
    }

    @Override
    public boolean canSetPlaceholder(Request request) {
        return parentCanSetPlaceholder() && request == this.full && !isAnyRequestComplete();
    }

    private boolean parentCanSetPlaceholder() {
        return this.coordinator == null || this.coordinator.canSetPlaceholder(this);
    }

    @Override
    public boolean isAnyRequestComplete() {
        return parentIsAnyRequestComplete() || this.full.isComplete() || this.thumb.isComplete();
    }

    private boolean parentIsAnyRequestComplete() {
        return this.coordinator != null && this.coordinator.isAnyRequestComplete();
    }

    @Override
    public void run() {
        if (!this.thumb.isRunning()) {
            this.thumb.run();
        }
        if (!this.full.isRunning()) {
            this.full.run();
        }
    }

    @Override
    public void clear() {
        this.full.clear();
        this.thumb.clear();
    }

    @Override
    public boolean isRunning() {
        return this.full.isRunning();
    }

    @Override
    public boolean isComplete() {
        return this.full.isComplete() || this.thumb.isComplete();
    }

    @Override
    public boolean isFailed() {
        return this.full.isFailed();
    }

    @Override
    public void recycle() {
        this.full.recycle();
        this.thumb.recycle();
    }
}
