package com.android.gallery3d.filtershow.pipeline;

public class SharedPreset {
    private volatile ImagePreset mProducerPreset = null;
    private volatile ImagePreset mConsumerPreset = null;
    private volatile ImagePreset mIntermediatePreset = null;
    private volatile boolean mHasNewContent = false;

    public synchronized void enqueuePreset(ImagePreset preset) {
        if (this.mProducerPreset == null || !this.mProducerPreset.same(preset)) {
            this.mProducerPreset = new ImagePreset(preset);
        } else {
            this.mProducerPreset.updateWith(preset);
        }
        ImagePreset temp = this.mIntermediatePreset;
        this.mIntermediatePreset = this.mProducerPreset;
        this.mProducerPreset = temp;
        this.mHasNewContent = true;
    }

    public synchronized ImagePreset dequeuePreset() {
        ImagePreset imagePreset;
        if (!this.mHasNewContent) {
            imagePreset = this.mConsumerPreset;
        } else {
            ImagePreset temp = this.mConsumerPreset;
            this.mConsumerPreset = this.mIntermediatePreset;
            this.mIntermediatePreset = temp;
            this.mHasNewContent = false;
            imagePreset = this.mConsumerPreset;
        }
        return imagePreset;
    }
}
