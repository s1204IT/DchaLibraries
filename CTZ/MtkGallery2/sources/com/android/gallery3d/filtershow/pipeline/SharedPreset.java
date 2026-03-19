package com.android.gallery3d.filtershow.pipeline;

public class SharedPreset {
    private volatile ImagePreset mProducerPreset = null;
    private volatile ImagePreset mConsumerPreset = null;
    private volatile ImagePreset mIntermediatePreset = null;
    private volatile boolean mHasNewContent = false;

    public synchronized void enqueuePreset(ImagePreset imagePreset) {
        if (this.mProducerPreset == null || !this.mProducerPreset.same(imagePreset)) {
            this.mProducerPreset = new ImagePreset(imagePreset);
        } else {
            this.mProducerPreset.updateWith(imagePreset);
        }
        ImagePreset imagePreset2 = this.mIntermediatePreset;
        this.mIntermediatePreset = this.mProducerPreset;
        this.mProducerPreset = imagePreset2;
        this.mHasNewContent = true;
    }

    public synchronized ImagePreset dequeuePreset() {
        if (!this.mHasNewContent) {
            return this.mConsumerPreset;
        }
        ImagePreset imagePreset = this.mConsumerPreset;
        this.mConsumerPreset = this.mIntermediatePreset;
        this.mIntermediatePreset = imagePreset;
        this.mHasNewContent = false;
        return this.mConsumerPreset;
    }
}
