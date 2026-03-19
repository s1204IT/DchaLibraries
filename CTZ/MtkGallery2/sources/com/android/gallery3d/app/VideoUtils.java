package com.android.gallery3d.app;

import android.annotation.TargetApi;
import android.app.ProgressDialog;
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
    public static boolean startMute(String str, SaveVideoFileInfo saveVideoFileInfo, ProgressDialog progressDialog) throws IOException {
        if (ApiHelper.HAS_MEDIA_MUXER) {
            return genVideoUsingMuxer(str, saveVideoFileInfo.mFile.getPath(), -1, -1, false, true, progressDialog);
        }
        return startMuteUsingMp4Parser(str, saveVideoFileInfo);
    }

    public static boolean startTrim(File file, File file2, int i, int i2, TrimVideo trimVideo, ProgressDialog progressDialog) throws IOException {
        if (ApiHelper.HAS_MEDIA_MUXER) {
            return genVideoUsingMuxer(file.getPath(), file2.getPath(), i, i2, true, true, progressDialog);
        }
        return trimUsingMp4Parser(file, file2, i, i2, trimVideo);
    }

    private static boolean startMuteUsingMp4Parser(String str, SaveVideoFileInfo saveVideoFileInfo) throws IOException {
        File file = saveVideoFileInfo.mFile;
        RandomAccessFile randomAccessFile = new RandomAccessFile(new File(str), "r");
        Movie movieBuild = MovieCreator.build(randomAccessFile.getChannel());
        if (movieBuild == null) {
            com.mediatek.gallery3d.util.Log.d("Gallery2/VideoUtils", "MovieCreator fail:" + str);
            return false;
        }
        List<Track> tracks = movieBuild.getTracks();
        movieBuild.setTracks(new LinkedList());
        for (Track track : tracks) {
            if (track.getHandler().equals("vide")) {
                movieBuild.addTrack(track);
            }
        }
        writeMovieIntoFile(file, movieBuild);
        randomAccessFile.close();
        return true;
    }

    private static void writeMovieIntoFile(File file, Movie movie) throws IOException {
        if (!file.exists()) {
            file.createNewFile();
        }
        IsoFile isoFileBuild = new DefaultMp4Builder().build(movie);
        FileOutputStream fileOutputStream = new FileOutputStream(file);
        FileChannel channel = fileOutputStream.getChannel();
        isoFileBuild.getBox(channel);
        channel.close();
        fileOutputStream.close();
    }

    @TargetApi(18)
    private static boolean genVideoUsingMuxer(String str, String str2, int i, int i2, boolean z, boolean z2, ProgressDialog progressDialog) throws IOException {
        int i3;
        boolean z3;
        int i4;
        int iCorrectSeekTime;
        int i5;
        MediaMuxer mediaMuxer;
        int i6;
        int i7;
        boolean z4;
        MediaExtractor mediaExtractor = new MediaExtractor();
        com.mediatek.gallery3d.util.Log.d("Gallery2/VideoUtils", "setDataSource:" + str);
        mediaExtractor.setDataSource(str);
        int trackCount = mediaExtractor.getTrackCount();
        MediaMuxer mediaMuxer2 = new MediaMuxer(str2, 0);
        HashMap map = new HashMap(trackCount);
        String[] strArr = new String[18];
        int i8 = 0;
        int i9 = 0;
        int i10 = 0;
        int integer = -1;
        int i11 = -1;
        int i12 = -1;
        int i13 = 0;
        while (i13 < trackCount) {
            MediaFormat trackFormat = mediaExtractor.getTrackFormat(i13);
            String string = trackFormat.getString("mime");
            if (string == null) {
                mediaMuxer2.release();
                progressDialog.dismiss();
                com.mediatek.gallery3d.util.Log.d("Gallery2/VideoUtils", "mime is null:" + i13);
                return false;
            }
            int i14 = trackCount;
            if (i13 < 18) {
                strArr[i13] = string;
            }
            String[] strArr2 = strArr;
            com.mediatek.gallery3d.util.Log.d("Gallery2/VideoUtils", "genVideoUsingMuxer mime:" + string);
            if (trackFormat.containsKey("durationUs")) {
                i6 = integer;
                i7 = i10;
                long j = trackFormat.getLong("durationUs") / 1000;
                mediaMuxer = mediaMuxer2;
                if (j <= i) {
                    com.mediatek.gallery3d.util.Log.d("Gallery2/VideoUtils", "durationMs: " + j + " < startMs:" + i);
                    integer = i6;
                    i10 = i7;
                    mediaMuxer2 = mediaMuxer;
                }
                i13++;
                trackCount = i14;
                strArr = strArr2;
            } else {
                mediaMuxer = mediaMuxer2;
                i6 = integer;
                i7 = i10;
            }
            if ((string.equals("audio/3gpp") || string.equals("audio/amr-wb") || string.equals("audio/mp4a-latm")) && z && i9 == 0) {
                i12 = i13;
                i9 = 1;
            } else if ((string.equals("video/mp4v-es") || string.equals("video/3gpp") || string.equals("video/avc") || string.equals("video/hevc")) && z2 && i8 == 0) {
                i8 = 1;
            } else {
                z4 = false;
                if (z4) {
                    mediaMuxer2 = mediaMuxer;
                    integer = i6;
                } else {
                    com.mediatek.gallery3d.util.Log.d("Gallery2/VideoUtils", "Add Track mime:" + string);
                    mediaExtractor.selectTrack(i13);
                    mediaMuxer2 = mediaMuxer;
                    map.put(Integer.valueOf(i13), Integer.valueOf(mediaMuxer2.addTrack(trackFormat)));
                    if (trackFormat.containsKey("max-input-size")) {
                        integer = trackFormat.getInteger("max-input-size");
                        int i15 = i7 + 1;
                        com.mediatek.gallery3d.util.Log.d("Gallery2/VideoUtils", "KEY_MAX_INPUT_SIZE " + i15 + ":" + integer);
                        int i16 = i6;
                        if (integer <= i16) {
                            integer = i16;
                        }
                        i7 = i15;
                    } else {
                        integer = i6;
                    }
                    if (trackFormat.containsKey("width") && trackFormat.containsKey("height")) {
                        int integer2 = trackFormat.getInteger("width");
                        int integer3 = trackFormat.getInteger("height");
                        com.mediatek.gallery3d.util.Log.d("Gallery2/VideoUtils", "KEY_WIDTH : " + integer2 + " KEY_HEIGHT : " + integer3);
                        int i17 = ((integer2 * integer3) * 15) / 10;
                        StringBuilder sb = new StringBuilder();
                        sb.append("yuvBufferSize : ");
                        sb.append(i17);
                        com.mediatek.gallery3d.util.Log.d("Gallery2/VideoUtils", sb.toString());
                        i11 = i17;
                    }
                }
                i10 = i7;
                i13++;
                trackCount = i14;
                strArr = strArr2;
            }
            z4 = true;
            if (z4) {
            }
            i10 = i7;
            i13++;
            trackCount = i14;
            strArr = strArr2;
        }
        String[] strArr3 = strArr;
        int i18 = integer;
        int i19 = i10;
        if (i8 == 0 && i9 == 0) {
            mediaMuxer2.release();
            progressDialog.dismiss();
            com.mediatek.gallery3d.util.Log.d("Gallery2/VideoUtils", "No Track support");
            return false;
        }
        if (i18 < 0 || i19 < i8 + i9) {
            i3 = 1048576 > i11 ? 1048576 : i11;
            com.mediatek.gallery3d.util.Log.d("Gallery2/VideoUtils", "use DEFAULT_BUFFER_SIZE or yuvbuffersize : " + i3);
        } else {
            i3 = i18;
        }
        MediaMetadataRetriever mediaMetadataRetriever = new MediaMetadataRetriever();
        mediaMetadataRetriever.setDataSource(str);
        String strExtractMetadata = mediaMetadataRetriever.extractMetadata(24);
        if (strExtractMetadata != null && (i5 = Integer.parseInt(strExtractMetadata)) >= 0) {
            mediaMuxer2.setOrientationHint(i5);
        }
        String strExtractMetadata2 = mediaMetadataRetriever.extractMetadata(12);
        if (strExtractMetadata2.equals("video/mp2ts") || strExtractMetadata2.equals("video/mp2p")) {
            com.mediatek.gallery3d.util.Log.d("Gallery2/VideoUtils", "Fix key frame:" + strExtractMetadata2);
            z3 = true;
        } else {
            z3 = false;
        }
        if (i > 0) {
            if (i8 == 1 && i9 == 1 && (strExtractMetadata2.equals("video/mp4") || strExtractMetadata2.equals("video/3gpp") || strExtractMetadata2.equals("video/quicktime"))) {
                int i20 = i12;
                mediaExtractor.unselectTrack(i20);
                iCorrectSeekTime = correctSeekTime(mediaExtractor, i, i3);
                com.mediatek.gallery3d.util.Log.d("Gallery2/VideoUtils", "correct new StartMs: " + iCorrectSeekTime);
                if (iCorrectSeekTime == -10000000 || iCorrectSeekTime > (i4 = i2)) {
                    mediaMuxer2.release();
                    progressDialog.dismiss();
                    com.mediatek.gallery3d.util.Log.d("Gallery2/VideoUtils", "startMs can not be trimed");
                    return false;
                }
                mediaExtractor.selectTrack(i20);
            } else {
                i4 = i2;
                iCorrectSeekTime = i;
            }
            mediaExtractor.seekTo(((long) iCorrectSeekTime) * 1000, 2);
        } else {
            i4 = i2;
        }
        ByteBuffer byteBufferAllocate = ByteBuffer.allocate(i3);
        MediaCodec.BufferInfo bufferInfo = new MediaCodec.BufferInfo();
        try {
            try {
                mediaMuxer2.start();
                long j2 = -1;
                while (true) {
                    bufferInfo.offset = 0;
                    bufferInfo.size = mediaExtractor.readSampleData(byteBufferAllocate, 0);
                    if (bufferInfo.size < 0) {
                        com.mediatek.gallery3d.util.Log.d("Gallery2/VideoUtils", "Saw input EOS.");
                        bufferInfo.size = 0;
                        break;
                    }
                    bufferInfo.presentationTimeUs = mediaExtractor.getSampleTime();
                    if (j2 != -1 && bufferInfo.presentationTimeUs < j2) {
                        com.mediatek.gallery3d.util.Log.d("Gallery2/VideoUtils", "timeUs back!");
                        mediaMuxer2.release();
                        progressDialog.dismiss();
                        try {
                            mediaMuxer2.release();
                            return false;
                        } catch (IllegalStateException e) {
                            com.mediatek.gallery3d.util.Log.d("Gallery2/VideoUtils", "MediaMuxer release failed");
                            return false;
                        }
                    }
                    if (i4 > 0 && bufferInfo.presentationTimeUs > ((long) i4) * 1000) {
                        com.mediatek.gallery3d.util.Log.d("Gallery2/VideoUtils", "The current sample is over the trim end time.");
                        com.mediatek.gallery3d.util.Log.d("Gallery2/VideoUtils", "presentationTimeUs:" + bufferInfo.presentationTimeUs + "endMs:" + i4);
                        break;
                    }
                    bufferInfo.flags = mediaExtractor.getSampleFlags();
                    int sampleTrackIndex = mediaExtractor.getSampleTrackIndex();
                    if (z3 && checkKeyFrameIfPossible(byteBufferAllocate, strArr3[sampleTrackIndex])) {
                        bufferInfo.flags |= 1;
                    }
                    mediaMuxer2.writeSampleData(((Integer) map.get(Integer.valueOf(sampleTrackIndex))).intValue(), byteBufferAllocate, bufferInfo);
                    mediaExtractor.advance();
                    j2 = bufferInfo.presentationTimeUs;
                }
                mediaMuxer2.stop();
                try {
                    mediaMuxer2.release();
                    return true;
                } catch (IllegalStateException e2) {
                    com.mediatek.gallery3d.util.Log.d("Gallery2/VideoUtils", "MediaMuxer release failed");
                    return false;
                }
            } catch (Throwable th) {
                try {
                    mediaMuxer2.release();
                    throw th;
                } catch (IllegalStateException e3) {
                    com.mediatek.gallery3d.util.Log.d("Gallery2/VideoUtils", "MediaMuxer release failed");
                    return false;
                }
            }
        } catch (IllegalStateException e4) {
            progressDialog.dismiss();
            com.mediatek.gallery3d.util.Log.d("Gallery2/VideoUtils", "MediaMuxer.nativeStop failed");
            try {
                mediaMuxer2.release();
                return false;
            } catch (IllegalStateException e5) {
                com.mediatek.gallery3d.util.Log.d("Gallery2/VideoUtils", "MediaMuxer release failed");
                return false;
            }
        }
    }

    private static boolean trimUsingMp4Parser(File file, File file2, int i, int i2, TrimVideo trimVideo) throws IOException {
        RandomAccessFile randomAccessFile;
        Movie movie;
        if (file.exists() && file2.exists()) {
            com.mediatek.gallery3d.util.Log.v("Gallery2/VideoUtils", "startTrim() src is " + file.getAbsolutePath() + " and dst is " + file2.getAbsolutePath());
        }
        com.mediatek.gallery3d.util.Log.v("Gallery2/VideoUtils", "startTrim() startMs is " + i + " endMs is " + i2);
        RandomAccessFile randomAccessFile2 = new RandomAccessFile(file, "r");
        Movie movieBuild = MovieCreator.build(randomAccessFile2.getChannel());
        int i3 = 0;
        if (movieBuild == null) {
            return false;
        }
        List<Track> tracks = movieBuild.getTracks();
        movieBuild.setTracks(new LinkedList());
        double d = i2 / 1000;
        boolean z = false;
        double d2 = i / 1000;
        for (Track track : tracks) {
            if (track.getSyncSamples() != null && track.getSyncSamples().length > 0) {
                if (z) {
                    return false;
                }
                double[] dArr = new double[2];
                correctTimeToSyncSample2(track, d2, d, dArr);
                double d3 = dArr[0];
                d = dArr[1];
                z = true;
                d2 = d3;
            }
        }
        com.mediatek.gallery3d.util.Log.v("Gallery2/VideoUtils", "startTrim() startTime " + d2 + " endTime " + d);
        if (d2 == d) {
            return false;
        }
        for (Track track2 : tracks) {
            long j = 0;
            long j2 = -1;
            int i4 = i3;
            double delta = 0.0d;
            long j3 = -1;
            while (i4 < track2.getDecodingTimeEntries().size()) {
                TimeToSampleBox.Entry entry = track2.getDecodingTimeEntries().get(i4);
                long j4 = j;
                int i5 = i3;
                while (true) {
                    randomAccessFile = randomAccessFile2;
                    movie = movieBuild;
                    if (i5 >= entry.getCount()) {
                        break;
                    }
                    if (delta <= d2) {
                        j3 = j4;
                    }
                    if (delta <= d) {
                        delta += entry.getDelta() / track2.getTrackMetaData().getTimescale();
                        i5++;
                        j2 = j4;
                        entry = entry;
                        j4 = 1 + j4;
                        randomAccessFile2 = randomAccessFile;
                        movieBuild = movie;
                    }
                }
                i4++;
                j = j4;
                randomAccessFile2 = randomAccessFile;
                movieBuild = movie;
                i3 = 0;
            }
            Movie movie2 = movieBuild;
            movie2.addTrack(new CroppedTrack(track2, j3, j2));
            movieBuild = movie2;
            randomAccessFile2 = randomAccessFile2;
            i3 = 0;
        }
        writeMovieIntoFile(file2, movieBuild);
        randomAccessFile2.close();
        return true;
    }

    private static void correctTimeToSyncSample2(Track track, double d, double d2, double[] dArr) {
        double d3;
        double[] dArr2 = new double[track.getSyncSamples().length];
        com.mediatek.gallery3d.util.Log.v("Gallery2/VideoUtils", "correctTimeToSyncSample()SyncSample length:" + track.getSyncSamples().length + "DecodingTimeEntries: " + track.getDecodingTimeEntries().size());
        long j = 0L;
        double d4 = 0.0d;
        int i = 0;
        while (i < track.getDecodingTimeEntries().size()) {
            TimeToSampleBox.Entry entry = track.getDecodingTimeEntries().get(i);
            double delta = d4;
            long j2 = j;
            for (int i2 = 0; i2 < entry.getCount(); i2++) {
                j2++;
                int iBinarySearch = Arrays.binarySearch(track.getSyncSamples(), j2);
                if (iBinarySearch >= 0) {
                    dArr2[iBinarySearch] = delta;
                }
                delta += entry.getDelta() / track.getTrackMetaData().getTimescale();
            }
            i++;
            j = j2;
            d4 = delta;
        }
        int length = dArr2.length;
        int i3 = 0;
        double d5 = -1.0d;
        double d6 = 0.0d;
        while (true) {
            if (i3 >= length) {
                d3 = -1.0d;
                break;
            }
            d3 = dArr2[i3];
            if (d3 > d && d5 == -1.0d) {
                com.mediatek.gallery3d.util.Log.v("Gallery2/VideoUtils", "newCutStart " + d6);
                d5 = d6;
            }
            if (d3 <= d2) {
                i3++;
                d6 = d3;
            } else {
                com.mediatek.gallery3d.util.Log.v("Gallery2/VideoUtils", "newCutEnd " + d3);
                break;
            }
        }
        if (d5 == -1.0d) {
            d5 = dArr2[dArr2.length - 1];
        }
        if (d3 == -1.0d) {
            d3 = dArr2[dArr2.length - 1];
        }
        dArr[0] = d5;
        dArr[1] = d3;
    }

    private static int correctSeekTime(MediaExtractor mediaExtractor, int i, int i2) {
        mediaExtractor.seekTo(((long) i) * 1000, 2);
        ByteBuffer byteBufferAllocate = ByteBuffer.allocate(i2);
        MediaCodec.BufferInfo bufferInfo = new MediaCodec.BufferInfo();
        bufferInfo.offset = 0;
        bufferInfo.size = mediaExtractor.readSampleData(byteBufferAllocate, 0);
        if (bufferInfo.size < 0) {
            com.mediatek.gallery3d.util.Log.d("Gallery2/VideoUtils", "correctSeekTime again Saw input EOS.");
            return -10000000;
        }
        bufferInfo.presentationTimeUs = mediaExtractor.getSampleTime();
        return (int) (bufferInfo.presentationTimeUs / 1000);
    }

    private static boolean checkKeyFrameIfPossible(ByteBuffer byteBuffer, String str) {
        if (!str.equals("video/avc")) {
            if (str.equals("video/mp4v-es")) {
                int iPosition = byteBuffer.position();
                if (iPosition + 4 < byteBuffer.limit()) {
                    byteBuffer.position(iPosition + 3);
                    byte b = byteBuffer.get();
                    byteBuffer.position(iPosition);
                    if ((b & 255) == 179) {
                        com.mediatek.gallery3d.util.Log.d("Gallery2/VideoUtils", "key Frame:" + ((int) b));
                        return true;
                    }
                    return false;
                }
                return false;
            }
            return false;
        }
        int iPosition2 = byteBuffer.position();
        int i = iPosition2 + 4;
        if (i < byteBuffer.limit()) {
            byteBuffer.position(i);
            byte b2 = byteBuffer.get();
            byteBuffer.position(iPosition2);
            int i2 = b2 & 31;
            if (i2 == 5 || i2 == 7) {
                com.mediatek.gallery3d.util.Log.d("Gallery2/VideoUtils", "key Frame:" + i2);
                return true;
            }
            if (i2 == 9) {
                byteBuffer.position(iPosition2 + 10);
                byte b3 = byteBuffer.get();
                byteBuffer.position(iPosition2);
                int i3 = b3 & 31;
                if (i3 == 5 || i3 == 7) {
                    com.mediatek.gallery3d.util.Log.d("Gallery2/VideoUtils", "key Frame:" + i3);
                    return true;
                }
                return false;
            }
            return false;
        }
        return false;
    }
}
