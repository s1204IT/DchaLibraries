package com.bumptech.glide;

import com.bumptech.glide.load.Encoder;
import com.bumptech.glide.load.ResourceDecoder;
import com.bumptech.glide.load.ResourceEncoder;
import java.io.InputStream;

public interface DataLoadProvider<T, Z> {
    ResourceDecoder<InputStream, Z> getCacheDecoder();

    ResourceEncoder<Z> getEncoder();

    ResourceDecoder<T, Z> getSourceDecoder();

    Encoder<T> getSourceEncoder();
}
