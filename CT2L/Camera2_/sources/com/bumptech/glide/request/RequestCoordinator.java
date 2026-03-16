package com.bumptech.glide.request;

public interface RequestCoordinator {
    boolean canSetImage(Request request);

    boolean canSetPlaceholder(Request request);

    boolean isAnyRequestComplete();
}
