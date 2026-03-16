package com.bumptech.glide.load.engine;

import com.bumptech.glide.Priority;
import com.bumptech.glide.load.Encoder;
import com.bumptech.glide.load.ResourceDecoder;
import com.bumptech.glide.load.ResourceEncoder;
import com.bumptech.glide.load.Transformation;
import com.bumptech.glide.load.data.DataFetcher;
import com.bumptech.glide.load.resource.transcode.ResourceTranscoder;
import java.io.InputStream;

interface ResourceRunnerFactory {
    <T, Z, R> ResourceRunner<Z, R> build(EngineKey engineKey, int i, int i2, ResourceDecoder<InputStream, Z> resourceDecoder, DataFetcher<T> dataFetcher, boolean z, Encoder<T> encoder, ResourceDecoder<T, Z> resourceDecoder2, Transformation<Z> transformation, ResourceEncoder<Z> resourceEncoder, ResourceTranscoder<Z, R> resourceTranscoder, Priority priority, boolean z2, EngineJobListener engineJobListener);
}
