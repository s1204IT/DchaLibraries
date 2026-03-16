package com.bumptech.glide.provider;

import com.bumptech.glide.load.Encoder;
import com.bumptech.glide.load.ResourceDecoder;
import com.bumptech.glide.load.ResourceEncoder;
import com.bumptech.glide.load.model.ModelLoader;
import com.bumptech.glide.load.resource.transcode.ResourceTranscoder;
import java.io.InputStream;

public class ChildLoadProvider<A, T, Z, R> implements LoadProvider<A, T, Z, R> {
    private ResourceDecoder<InputStream, Z> cacheDecoder;
    private ResourceEncoder<Z> encoder;
    private LoadProvider<A, T, Z, R> parent;
    private ResourceDecoder<T, Z> sourceDecoder;
    private Encoder<T> sourceEncoder;
    private ResourceTranscoder<Z, R> transcoder;

    public ChildLoadProvider(LoadProvider<A, T, Z, R> parent) {
        this.parent = parent;
    }

    @Override
    public ModelLoader<A, T> getModelLoader() {
        return this.parent.getModelLoader();
    }

    public void setCacheDecoder(ResourceDecoder<InputStream, Z> cacheDecoder) {
        this.cacheDecoder = cacheDecoder;
    }

    public void setSourceDecoder(ResourceDecoder<T, Z> sourceDecoder) {
        this.sourceDecoder = sourceDecoder;
    }

    public void setEncoder(ResourceEncoder<Z> encoder) {
        this.encoder = encoder;
    }

    public void setTranscoder(ResourceTranscoder<Z, R> transcoder) {
        this.transcoder = transcoder;
    }

    public void setSourceEncoder(Encoder<T> sourceEncoder) {
        this.sourceEncoder = sourceEncoder;
    }

    @Override
    public ResourceDecoder<InputStream, Z> getCacheDecoder() {
        return this.cacheDecoder != null ? this.cacheDecoder : this.parent.getCacheDecoder();
    }

    @Override
    public ResourceDecoder<T, Z> getSourceDecoder() {
        return this.sourceDecoder != null ? this.sourceDecoder : this.parent.getSourceDecoder();
    }

    @Override
    public Encoder<T> getSourceEncoder() {
        return this.sourceEncoder != null ? this.sourceEncoder : this.parent.getSourceEncoder();
    }

    @Override
    public ResourceEncoder<Z> getEncoder() {
        return this.encoder != null ? this.encoder : this.parent.getEncoder();
    }

    @Override
    public ResourceTranscoder<Z, R> getTranscoder() {
        return this.transcoder != null ? this.transcoder : this.parent.getTranscoder();
    }
}
