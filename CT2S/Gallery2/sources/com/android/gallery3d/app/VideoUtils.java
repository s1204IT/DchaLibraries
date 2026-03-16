package com.android.gallery3d.app;

import android.media.MediaCodec;
import android.media.MediaExtractor;
import android.media.MediaFormat;
import android.media.MediaMetadataRetriever;
import android.media.MediaMuxer;
import com.android.gallery3d.common.ApiHelper;
import com.android.gallery3d.util.SaveVideoFileInfo;
import com.coremedia.iso.IsoFile;
import com.coremedia.iso.boxes.TimeToSampleBox;
import com.googlecode.mp4parser.authoring.Movie;
import com.googlecode.mp4parser.authoring.Track;
import com.googlecode.mp4parser.authoring.builder.DefaultMp4Builder;
import com.googlecode.mp4parser.authoring.container.mp4.MovieCreator;
import com.googlecode.mp4parser.authoring.tracks.CroppedTrack;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;

public class VideoUtils {
    public static void startMute(String filePath, SaveVideoFileInfo dstFileInfo) throws IOException {
        if (ApiHelper.HAS_MEDIA_MUXER) {
            genVideoUsingMuxer(filePath, dstFileInfo.mFile.getPath(), -1, -1, false, true);
        } else {
            startMuteUsingMp4Parser(filePath, dstFileInfo);
        }
    }

    public static void startTrim(File src, File dst, int startMs, int endMs) throws IOException {
        if (ApiHelper.HAS_MEDIA_MUXER) {
            genVideoUsingMuxer(src.getPath(), dst.getPath(), startMs, endMs, true, true);
        } else {
            trimUsingMp4Parser(src, dst, startMs, endMs);
        }
    }

    private static void startMuteUsingMp4Parser(String filePath, SaveVideoFileInfo dstFileInfo) throws IOException {
        File dst = dstFileInfo.mFile;
        File src = new File(filePath);
        RandomAccessFile randomAccessFile = new RandomAccessFile(src, "r");
        Movie movie = MovieCreator.build(randomAccessFile.getChannel());
        List<Track> tracks = movie.getTracks();
        movie.setTracks(new LinkedList());
        for (Track track : tracks) {
            if (track.getHandler().equals("vide")) {
                movie.addTrack(track);
            }
        }
        writeMovieIntoFile(dst, movie);
        randomAccessFile.close();
    }

    private static void writeMovieIntoFile(File dst, Movie movie) throws IOException {
        if (!dst.exists()) {
            dst.createNewFile();
        }
        IsoFile out = new DefaultMp4Builder().build(movie);
        FileOutputStream fos = new FileOutputStream(dst);
        FileChannel fc = fos.getChannel();
        out.getBox(fc);
        fc.close();
        fos.close();
    }

    private static void genVideoUsingMuxer(String srcPath, String dstPath, int startMs, int endMs, boolean useAudio, boolean useVideo) throws IOException {
        int degrees;
        MediaExtractor extractor = new MediaExtractor();
        extractor.setDataSource(srcPath);
        int trackCount = extractor.getTrackCount();
        MediaMuxer muxer = new MediaMuxer(dstPath, 0);
        HashMap<Integer, Integer> indexMap = new HashMap<>(trackCount);
        int bufferSize = -1;
        for (int i = 0; i < trackCount; i++) {
            MediaFormat format = extractor.getTrackFormat(i);
            String mime = format.getString("mime");
            boolean selectCurrentTrack = false;
            if (mime.startsWith("audio/") && useAudio) {
                selectCurrentTrack = true;
            } else if (mime.startsWith("video/") && useVideo) {
                selectCurrentTrack = true;
            }
            if (selectCurrentTrack) {
                extractor.selectTrack(i);
                try {
                    int dstIndex = muxer.addTrack(format);
                    if (dstIndex < 0) {
                        extractor.release();
                        muxer.release();
                        File file = new File(dstPath);
                        if (file != null && file.exists() && file.isFile()) {
                            file.delete();
                        }
                        throw new IOException("Can't add track in format: " + mime);
                    }
                    indexMap.put(Integer.valueOf(i), Integer.valueOf(dstIndex));
                    if (format.containsKey("max-input-size")) {
                        int newSize = format.getInteger("max-input-size");
                        if (newSize > bufferSize) {
                            bufferSize = newSize;
                        }
                    }
                } catch (Exception e) {
                    extractor.release();
                    muxer.release();
                    File file2 = new File(dstPath);
                    if (file2 != null && file2.exists() && file2.isFile()) {
                        file2.delete();
                    }
                    throw new IOException("Can't add track in format: " + mime);
                }
            }
        }
        if (bufferSize < 0) {
            bufferSize = 1048576;
        }
        MediaMetadataRetriever retrieverSrc = new MediaMetadataRetriever();
        retrieverSrc.setDataSource(srcPath);
        String degreesString = retrieverSrc.extractMetadata(24);
        if (degreesString != null && (degrees = Integer.parseInt(degreesString)) >= 0) {
            muxer.setOrientationHint(degrees);
        }
        if (startMs > 0) {
            extractor.seekTo(((long) startMs) * 1000, 2);
        }
        ByteBuffer dstBuf = ByteBuffer.allocate(bufferSize);
        MediaCodec.BufferInfo bufferInfo = new MediaCodec.BufferInfo();
        try {
            muxer.start();
            while (true) {
                bufferInfo.offset = 0;
                bufferInfo.size = extractor.readSampleData(dstBuf, 0);
                if (bufferInfo.size < 0) {
                    android.util.Log.d("VideoUtils", "Saw input EOS.");
                    bufferInfo.size = 0;
                    break;
                }
                bufferInfo.presentationTimeUs = extractor.getSampleTime();
                if (endMs > 0 && bufferInfo.presentationTimeUs > ((long) endMs) * 1000) {
                    android.util.Log.d("VideoUtils", "The current sample is over the trim end time.");
                    break;
                }
                bufferInfo.flags = extractor.getSampleFlags();
                int trackIndex = extractor.getSampleTrackIndex();
                muxer.writeSampleData(indexMap.get(Integer.valueOf(trackIndex)).intValue(), dstBuf, bufferInfo);
                extractor.advance();
            }
            muxer.stop();
        } catch (IllegalStateException e2) {
            android.util.Log.w("VideoUtils", "The source video file is malformed");
        } finally {
            muxer.release();
        }
    }

    private static void trimUsingMp4Parser(File src, File dst, int startMs, int endMs) throws IOException {
        RandomAccessFile randomAccessFile = new RandomAccessFile(src, "r");
        Movie movie = MovieCreator.build(randomAccessFile.getChannel());
        List<Track> tracks = movie.getTracks();
        movie.setTracks(new LinkedList());
        double startTime = startMs / 1000;
        double endTime = endMs / 1000;
        boolean timeCorrected = false;
        for (Track track : tracks) {
            if (track.getSyncSamples() != null && track.getSyncSamples().length > 0) {
                if (timeCorrected) {
                    throw new RuntimeException("The startTime has already been corrected by another track with SyncSample. Not Supported.");
                }
                startTime = correctTimeToSyncSample(track, startTime, false);
                endTime = correctTimeToSyncSample(track, endTime, true);
                timeCorrected = true;
            }
        }
        for (Track track2 : tracks) {
            long currentSample = 0;
            double currentTime = 0.0d;
            long startSample = -1;
            long endSample = -1;
            for (int i = 0; i < track2.getDecodingTimeEntries().size(); i++) {
                TimeToSampleBox.Entry entry = track2.getDecodingTimeEntries().get(i);
                for (int j = 0; j < entry.getCount(); j++) {
                    if (currentTime <= startTime) {
                        startSample = currentSample;
                    }
                    if (currentTime <= endTime) {
                        endSample = currentSample;
                        currentTime += entry.getDelta() / track2.getTrackMetaData().getTimescale();
                        currentSample++;
                    }
                }
            }
            movie.addTrack(new CroppedTrack(track2, startSample, endSample));
        }
        writeMovieIntoFile(dst, movie);
        randomAccessFile.close();
    }

    private static double correctTimeToSyncSample(Track track, double cutHere, boolean next) {
        double[] timeOfSyncSamples = new double[track.getSyncSamples().length];
        long currentSample = 0;
        double currentTime = 0.0d;
        for (int i = 0; i < track.getDecodingTimeEntries().size(); i++) {
            TimeToSampleBox.Entry entry = track.getDecodingTimeEntries().get(i);
            for (int j = 0; j < entry.getCount(); j++) {
                if (Arrays.binarySearch(track.getSyncSamples(), 1 + currentSample) >= 0) {
                    timeOfSyncSamples[Arrays.binarySearch(track.getSyncSamples(), 1 + currentSample)] = currentTime;
                }
                currentTime += entry.getDelta() / track.getTrackMetaData().getTimescale();
                currentSample++;
            }
        }
        double previous = 0.0d;
        for (double timeOfSyncSample : timeOfSyncSamples) {
            if (timeOfSyncSample > cutHere) {
                if (next) {
                    return timeOfSyncSample;
                }
                return previous;
            }
            previous = timeOfSyncSample;
        }
        return timeOfSyncSamples[timeOfSyncSamples.length - 1];
    }
}
