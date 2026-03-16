package com.bumptech.glide.load.engine;

import com.bumptech.glide.load.Encoder;
import com.bumptech.glide.load.Key;
import com.bumptech.glide.load.ResourceDecoder;
import com.bumptech.glide.load.ResourceEncoder;
import com.bumptech.glide.load.Transformation;
import com.bumptech.glide.load.resource.transcode.ResourceTranscoder;
import java.io.UnsupportedEncodingException;
import java.nio.ByteBuffer;
import java.security.MessageDigest;

public class EngineKey implements Key {
    private static final String FORMAT = "UTF-8";
    private final ResourceDecoder cacheDecoder;
    private final ResourceDecoder decoder;
    private final ResourceEncoder encoder;
    private int hashCode;
    private final int height;
    private final String id;
    private OriginalEngineKey originalKey;
    private Encoder sourceEncoder;
    private String stringKey;
    private ResourceTranscoder transcoder;
    private final Transformation transformation;
    private final int width;

    public EngineKey(String id, int width, int height, ResourceDecoder cacheDecoder, ResourceDecoder decoder, Transformation transformation, ResourceEncoder encoder, ResourceTranscoder transcoder, Encoder sourceEncoder) {
        this.id = id;
        this.width = width;
        this.height = height;
        this.cacheDecoder = cacheDecoder;
        this.decoder = decoder;
        this.transformation = transformation;
        this.encoder = encoder;
        this.transcoder = transcoder;
        this.sourceEncoder = sourceEncoder;
    }

    public Key getOriginalKey() {
        if (this.originalKey == null) {
            this.originalKey = new OriginalEngineKey(this.id);
        }
        return this.originalKey;
    }

    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        EngineKey engineKey = (EngineKey) o;
        return this.id.equals(engineKey.id) && this.height == engineKey.height && this.width == engineKey.width && this.transformation.getId().equals(engineKey.transformation.getId()) && this.decoder.getId().equals(engineKey.decoder.getId()) && this.cacheDecoder.getId().equals(engineKey.cacheDecoder.getId()) && this.encoder.getId().equals(engineKey.encoder.getId()) && this.transcoder.getId().equals(engineKey.transcoder.getId()) && this.sourceEncoder.getId().equals(engineKey.sourceEncoder.getId());
    }

    public int hashCode() {
        if (this.hashCode == 0) {
            this.hashCode = this.id.hashCode();
            this.hashCode = (this.hashCode * 31) + this.width;
            this.hashCode = (this.hashCode * 31) + this.height;
            this.hashCode = (this.hashCode * 31) + this.cacheDecoder.getId().hashCode();
            this.hashCode = (this.hashCode * 31) + this.decoder.getId().hashCode();
            this.hashCode = (this.hashCode * 31) + this.transformation.getId().hashCode();
            this.hashCode = (this.hashCode * 31) + this.encoder.getId().hashCode();
            this.hashCode = (this.hashCode * 31) + this.transcoder.getId().hashCode();
            this.hashCode = (this.hashCode * 31) + this.sourceEncoder.getId().hashCode();
        }
        return this.hashCode;
    }

    public String toString() {
        if (this.stringKey == null) {
            this.stringKey = this.id + this.width + this.height + this.cacheDecoder.getId() + this.decoder.getId() + this.transformation.getId() + this.encoder.getId() + this.transcoder.getId() + this.sourceEncoder;
        }
        return this.stringKey;
    }

    @Override
    public void updateDiskCacheKey(MessageDigest messageDigest) throws UnsupportedEncodingException {
        byte[] dimensions = ByteBuffer.allocate(8).putInt(this.width).putInt(this.height).array();
        messageDigest.update(this.id.getBytes(FORMAT));
        messageDigest.update(dimensions);
        messageDigest.update(this.cacheDecoder.getId().getBytes(FORMAT));
        messageDigest.update(this.decoder.getId().getBytes(FORMAT));
        messageDigest.update(this.transformation.getId().getBytes(FORMAT));
        messageDigest.update(this.encoder.getId().getBytes(FORMAT));
        messageDigest.update(this.sourceEncoder.getId().getBytes(FORMAT));
    }
}
