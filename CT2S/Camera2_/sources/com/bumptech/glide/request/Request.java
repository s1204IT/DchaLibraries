package com.bumptech.glide.request;

public interface Request {
    void clear();

    boolean isComplete();

    boolean isFailed();

    boolean isRunning();

    void recycle();

    void run();
}
