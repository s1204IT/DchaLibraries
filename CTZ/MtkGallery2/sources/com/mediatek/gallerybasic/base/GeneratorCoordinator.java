package com.mediatek.gallerybasic.base;

import com.mediatek.gallerybasic.util.Log;
import java.io.File;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

public final class GeneratorCoordinator {
    private static final String TAG = "MtkGallery2/GeneratorCoordinator";
    private static volatile Secretary sSecretary = null;
    private static volatile OnGeneratedListener sOnGeneratedListener = null;

    public interface OnGeneratedListener {
        void onGeneratedListen();
    }

    private GeneratorCoordinator() {
    }

    private static class MediaGenerator {
        public final Generator generator;
        public final MediaData media;

        public MediaGenerator(Generator generator, MediaData mediaData) {
            this.media = mediaData;
            this.generator = generator;
        }
    }

    private static class Secretary extends Thread {
        private MediaGenerator mCurrentMediaGenerator;
        private final BlockingQueue<MediaGenerator> mMediaGeneratorQueue;

        public Secretary(String str) {
            super("GeneratorCoordinator - Secretary" + str);
            this.mMediaGeneratorQueue = new LinkedBlockingQueue();
        }

        @Override
        public void run() {
            while (!Thread.currentThread().isInterrupted()) {
                try {
                    MediaGenerator mediaGeneratorTake = this.mMediaGeneratorQueue.take();
                    synchronized (this) {
                        this.mCurrentMediaGenerator = mediaGeneratorTake;
                    }
                    if (mediaGeneratorTake.generator.needGenerating(mediaGeneratorTake.media, 0)) {
                        Log.v(GeneratorCoordinator.TAG, "begin handling transcoding request for " + mediaGeneratorTake.media.filePath);
                        int iGenerateAndWait = mediaGeneratorTake.generator.generateAndWait(mediaGeneratorTake.media, 0);
                        Log.v(GeneratorCoordinator.TAG, "end handling transcoding request for " + mediaGeneratorTake.media.filePath + " with result " + iGenerateAndWait);
                        if (iGenerateAndWait == 0 && GeneratorCoordinator.sOnGeneratedListener != null) {
                            GeneratorCoordinator.sOnGeneratedListener.onGeneratedListen();
                        }
                    }
                } catch (InterruptedException e) {
                    Log.e(GeneratorCoordinator.TAG, "Terminating " + getName());
                    interrupt();
                    return;
                }
            }
        }

        private void submit(MediaGenerator mediaGenerator) {
            if (isAlive()) {
                Log.v(GeneratorCoordinator.TAG, "submit transcoding request for " + mediaGenerator.media.filePath);
                this.mMediaGeneratorQueue.add(mediaGenerator);
                return;
            }
            Log.e(GeneratorCoordinator.TAG, getName() + " should be started before submitting tasks.");
        }

        private void cancelCurrentTranscode() {
            MediaGenerator mediaGenerator;
            synchronized (this) {
                mediaGenerator = this.mCurrentMediaGenerator;
            }
            if (mediaGenerator != null) {
                mediaGenerator.generator.onCancelRequested(mediaGenerator.media, 0);
            }
        }

        private void cancelTranscodingForLostFile() {
            MediaGenerator mediaGenerator;
            synchronized (this) {
                mediaGenerator = this.mCurrentMediaGenerator;
            }
            if (mediaGenerator != null && !new File(mediaGenerator.media.filePath).exists()) {
                Log.v(GeneratorCoordinator.TAG, "cancelTranscodingForLostFile " + mediaGenerator.media.filePath);
                mediaGenerator.generator.onCancelRequested(mediaGenerator.media, 0);
            }
        }

        private void cancelPendingTranscode() {
            this.mMediaGeneratorQueue.clear();
        }

        private void cancelAllTranscode() {
            cancelCurrentTranscode();
            cancelPendingTranscode();
        }
    }

    public static void setOnGeneratedListener(OnGeneratedListener onGeneratedListener) {
        sOnGeneratedListener = onGeneratedListener;
    }

    public static void requestThumbnail(Generator generator, MediaData mediaData) {
        Secretary secretary = sSecretary;
        if (secretary != null) {
            secretary.submit(new MediaGenerator(generator, mediaData));
        }
    }

    public static void pause() {
        if (sSecretary != null) {
            sSecretary.cancelAllTranscode();
        }
    }

    public static void cancelTranscodingForLostFile() {
        if (sSecretary != null) {
            sSecretary.cancelTranscodingForLostFile();
        }
    }

    public static void cancelPendingTranscode() {
        if (sSecretary != null) {
            sSecretary.cancelPendingTranscode();
        }
    }

    public static void start() {
        if (sSecretary == null) {
            sSecretary = new Secretary("transcoding proxy");
            sSecretary.start();
        }
    }
}
