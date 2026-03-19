package com.googlecode.mp4parser.authoring.builder;

import com.coremedia.iso.BoxParser;
import com.coremedia.iso.IsoFile;
import com.coremedia.iso.IsoTypeWriter;
import com.coremedia.iso.boxes.Box;
import com.coremedia.iso.boxes.CompositionTimeToSample;
import com.coremedia.iso.boxes.ContainerBox;
import com.coremedia.iso.boxes.DataEntryUrlBox;
import com.coremedia.iso.boxes.DataInformationBox;
import com.coremedia.iso.boxes.DataReferenceBox;
import com.coremedia.iso.boxes.FileTypeBox;
import com.coremedia.iso.boxes.HandlerBox;
import com.coremedia.iso.boxes.MediaBox;
import com.coremedia.iso.boxes.MediaHeaderBox;
import com.coremedia.iso.boxes.MediaInformationBox;
import com.coremedia.iso.boxes.MovieBox;
import com.coremedia.iso.boxes.MovieHeaderBox;
import com.coremedia.iso.boxes.SampleDependencyTypeBox;
import com.coremedia.iso.boxes.SampleSizeBox;
import com.coremedia.iso.boxes.SampleTableBox;
import com.coremedia.iso.boxes.SampleToChunkBox;
import com.coremedia.iso.boxes.StaticChunkOffsetBox;
import com.coremedia.iso.boxes.SyncSampleBox;
import com.coremedia.iso.boxes.TimeToSampleBox;
import com.coremedia.iso.boxes.TrackBox;
import com.coremedia.iso.boxes.TrackHeaderBox;
import com.googlecode.mp4parser.authoring.DateHelper;
import com.googlecode.mp4parser.authoring.Movie;
import com.googlecode.mp4parser.authoring.Track;
import com.googlecode.mp4parser.util.CastUtils;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.MappedByteBuffer;
import java.nio.channels.GatheringByteChannel;
import java.nio.channels.ReadableByteChannel;
import java.nio.channels.WritableByteChannel;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

public class DefaultMp4Builder {
    static final boolean $assertionsDisabled = false;
    private static Logger LOG = Logger.getLogger(DefaultMp4Builder.class.getName());
    public int STEPSIZE = 64;
    Set<StaticChunkOffsetBox> chunkOffsetBoxes = new HashSet();
    HashMap<Track, List<ByteBuffer>> track2Sample = new HashMap<>();
    HashMap<Track, long[]> track2SampleSizes = new HashMap<>();
    private FragmentIntersectionFinder intersectionFinder = new TwoSecondIntersectionFinder();

    public IsoFile build(Movie movie) {
        LOG.fine("Creating movie " + movie);
        Iterator<Track> it = movie.getTracks().iterator();
        while (true) {
            if (!it.hasNext()) {
                break;
            }
            Track next = it.next();
            List<ByteBuffer> samples = next.getSamples();
            putSamples(next, samples);
            long[] jArr = new long[samples.size()];
            for (int i = 0; i < jArr.length; i++) {
                jArr[i] = samples.get(i).limit();
            }
            putSampleSizes(next, jArr);
        }
        IsoFile isoFile = new IsoFile();
        LinkedList linkedList = new LinkedList();
        linkedList.add("isom");
        linkedList.add("iso2");
        linkedList.add("avc1");
        isoFile.addBox(new FileTypeBox("isom", 0L, linkedList));
        isoFile.addBox(createMovieBox(movie));
        InterleaveChunkMdat interleaveChunkMdat = new InterleaveChunkMdat(movie);
        isoFile.addBox(interleaveChunkMdat);
        long dataOffset = interleaveChunkMdat.getDataOffset();
        Iterator<StaticChunkOffsetBox> it2 = this.chunkOffsetBoxes.iterator();
        while (it2.hasNext()) {
            long[] chunkOffsets = it2.next().getChunkOffsets();
            for (int i2 = 0; i2 < chunkOffsets.length; i2++) {
                chunkOffsets[i2] = chunkOffsets[i2] + dataOffset;
            }
        }
        return isoFile;
    }

    protected long[] putSampleSizes(Track track, long[] jArr) {
        return this.track2SampleSizes.put(track, jArr);
    }

    protected List<ByteBuffer> putSamples(Track track, List<ByteBuffer> list) {
        return this.track2Sample.put(track, list);
    }

    private MovieBox createMovieBox(Movie movie) {
        MovieBox movieBox = new MovieBox();
        MovieHeaderBox movieHeaderBox = new MovieHeaderBox();
        movieHeaderBox.setCreationTime(DateHelper.convert(new Date()));
        movieHeaderBox.setModificationTime(DateHelper.convert(new Date()));
        long timescale = getTimescale(movie);
        long trackId = 0;
        long j = 0;
        for (Track track : movie.getTracks()) {
            long duration = (getDuration(track) * timescale) / track.getTrackMetaData().getTimescale();
            if (duration > j) {
                j = duration;
            }
        }
        movieHeaderBox.setDuration(j);
        movieHeaderBox.setTimescale(timescale);
        for (Track track2 : movie.getTracks()) {
            if (trackId < track2.getTrackMetaData().getTrackId()) {
                trackId = track2.getTrackMetaData().getTrackId();
            }
        }
        movieHeaderBox.setNextTrackId(trackId + 1);
        if (movieHeaderBox.getCreationTime() >= 4294967296L || movieHeaderBox.getModificationTime() >= 4294967296L || movieHeaderBox.getDuration() >= 4294967296L) {
            movieHeaderBox.setVersion(1);
        }
        movieBox.addBox(movieHeaderBox);
        Iterator<Track> it = movie.getTracks().iterator();
        while (it.hasNext()) {
            movieBox.addBox(createTrackBox(it.next(), movie));
        }
        Box boxCreateUdta = createUdta(movie);
        if (boxCreateUdta != null) {
            movieBox.addBox(boxCreateUdta);
        }
        return movieBox;
    }

    protected Box createUdta(Movie movie) {
        return null;
    }

    private TrackBox createTrackBox(Track track, Movie movie) {
        SampleTableBox sampleTableBox;
        TrackBox trackBox;
        MediaBox mediaBox;
        ?? r23;
        SampleTableBox sampleTableBox2;
        Track track2 = track;
        LOG.info("Creating Mp4TrackImpl " + track2);
        TrackBox trackBox2 = new TrackBox();
        TrackHeaderBox trackHeaderBox = new TrackHeaderBox();
        int i = track.isEnabled() ? 1 : 0;
        if (track.isInMovie()) {
            i += 2;
        }
        if (track.isInPreview()) {
            i += 4;
        }
        if (track.isInPoster()) {
            i += 8;
        }
        trackHeaderBox.setFlags(i);
        trackHeaderBox.setAlternateGroup(track.getTrackMetaData().getGroup());
        trackHeaderBox.setCreationTime(DateHelper.convert(track.getTrackMetaData().getCreationTime()));
        trackHeaderBox.setDuration((getDuration(track) * getTimescale(movie)) / track.getTrackMetaData().getTimescale());
        trackHeaderBox.setHeight(track.getTrackMetaData().getHeight());
        trackHeaderBox.setWidth(track.getTrackMetaData().getWidth());
        trackHeaderBox.setLayer(track.getTrackMetaData().getLayer());
        trackHeaderBox.setModificationTime(DateHelper.convert(new Date()));
        trackHeaderBox.setTrackId(track.getTrackMetaData().getTrackId());
        trackHeaderBox.setVolume(track.getTrackMetaData().getVolume());
        trackHeaderBox.setMatrix(track.getTrackMetaData().getMatrix());
        if (trackHeaderBox.getCreationTime() >= 4294967296L || trackHeaderBox.getModificationTime() >= 4294967296L || trackHeaderBox.getDuration() >= 4294967296L) {
            trackHeaderBox.setVersion(1);
        }
        trackBox2.addBox(trackHeaderBox);
        MediaBox mediaBox2 = new MediaBox();
        trackBox2.addBox(mediaBox2);
        MediaHeaderBox mediaHeaderBox = new MediaHeaderBox();
        mediaHeaderBox.setCreationTime(DateHelper.convert(track.getTrackMetaData().getCreationTime()));
        mediaHeaderBox.setDuration(getDuration(track));
        mediaHeaderBox.setTimescale(track.getTrackMetaData().getTimescale());
        mediaHeaderBox.setLanguage(track.getTrackMetaData().getLanguage());
        mediaBox2.addBox(mediaHeaderBox);
        HandlerBox handlerBox = new HandlerBox();
        mediaBox2.addBox(handlerBox);
        handlerBox.setHandlerType(track.getHandler());
        ?? mediaInformationBox = new MediaInformationBox();
        mediaInformationBox.addBox(track.getMediaHeaderBox());
        ?? dataInformationBox = new DataInformationBox();
        DataReferenceBox dataReferenceBox = new DataReferenceBox();
        dataInformationBox.addBox(dataReferenceBox);
        DataEntryUrlBox dataEntryUrlBox = new DataEntryUrlBox();
        dataEntryUrlBox.setFlags(1);
        dataReferenceBox.addBox(dataEntryUrlBox);
        mediaInformationBox.addBox(dataInformationBox);
        SampleTableBox sampleTableBox3 = new SampleTableBox();
        sampleTableBox3.addBox(track.getSampleDescriptionBox());
        if (track.getDecodingTimeEntries() != null && !track.getDecodingTimeEntries().isEmpty()) {
            TimeToSampleBox timeToSampleBox = new TimeToSampleBox();
            timeToSampleBox.setEntries(track.getDecodingTimeEntries());
            sampleTableBox3.addBox(timeToSampleBox);
        }
        List<CompositionTimeToSample.Entry> compositionTimeEntries = track.getCompositionTimeEntries();
        if (compositionTimeEntries != null && !compositionTimeEntries.isEmpty()) {
            CompositionTimeToSample compositionTimeToSample = new CompositionTimeToSample();
            compositionTimeToSample.setEntries(compositionTimeEntries);
            sampleTableBox3.addBox(compositionTimeToSample);
        }
        long[] syncSamples = track.getSyncSamples();
        if (syncSamples != null && syncSamples.length > 0) {
            SyncSampleBox syncSampleBox = new SyncSampleBox();
            syncSampleBox.setSampleNumber(syncSamples);
            sampleTableBox3.addBox(syncSampleBox);
        }
        if (track.getSampleDependencies() != null && !track.getSampleDependencies().isEmpty()) {
            SampleDependencyTypeBox sampleDependencyTypeBox = new SampleDependencyTypeBox();
            sampleDependencyTypeBox.setEntries(track.getSampleDependencies());
            sampleTableBox3.addBox(sampleDependencyTypeBox);
        }
        HashMap map = new HashMap();
        for (Track track3 : movie.getTracks()) {
            map.put(track3, getChunkSizes(track3, movie));
        }
        int[] iArr = (int[]) map.get(track2);
        SampleToChunkBox sampleToChunkBox = new SampleToChunkBox();
        sampleToChunkBox.setEntries(new LinkedList());
        long j = -2147483648L;
        int i2 = 0;
        ?? r5 = mediaInformationBox;
        while (i2 < iArr.length) {
            if (j != iArr[i2]) {
                sampleTableBox2 = sampleTableBox3;
                trackBox = trackBox2;
                mediaBox = mediaBox2;
                r23 = r5;
                sampleToChunkBox.getEntries().add(new SampleToChunkBox.Entry(i2 + 1, iArr[i2], 1L));
                j = iArr[i2];
            } else {
                trackBox = trackBox2;
                mediaBox = mediaBox2;
                r23 = r5;
                sampleTableBox2 = sampleTableBox3;
            }
            i2++;
            sampleTableBox3 = sampleTableBox2;
            trackBox2 = trackBox;
            mediaBox2 = mediaBox;
            r5 = r23;
        }
        TrackBox trackBox3 = trackBox2;
        ?? r22 = mediaBox2;
        ?? r232 = r5;
        SampleTableBox sampleTableBox4 = sampleTableBox3;
        sampleTableBox4.addBox(sampleToChunkBox);
        SampleSizeBox sampleSizeBox = new SampleSizeBox();
        sampleSizeBox.setSampleSizes(this.track2SampleSizes.get(track2));
        sampleTableBox4.addBox(sampleSizeBox);
        StaticChunkOffsetBox staticChunkOffsetBox = new StaticChunkOffsetBox();
        this.chunkOffsetBoxes.add(staticChunkOffsetBox);
        long[] jArr = new long[iArr.length];
        if (LOG.isLoggable(Level.FINE)) {
            LOG.fine("Calculating chunk offsets for track_" + track.getTrackMetaData().getTrackId());
        }
        int i3 = 0;
        long j2 = 0;
        while (i3 < iArr.length) {
            if (LOG.isLoggable(Level.FINER)) {
                LOG.finer("Calculating chunk offsets for track_" + track.getTrackMetaData().getTrackId() + " chunk " + i3);
            }
            for (Track track4 : movie.getTracks()) {
                if (LOG.isLoggable(Level.FINEST)) {
                    Logger logger = LOG;
                    StringBuilder sb = new StringBuilder();
                    sb.append("Adding offsets of track_");
                    sampleTableBox = sampleTableBox4;
                    sb.append(track4.getTrackMetaData().getTrackId());
                    logger.finest(sb.toString());
                } else {
                    sampleTableBox = sampleTableBox4;
                }
                int[] iArr2 = (int[]) map.get(track4);
                int i4 = 0;
                long j3 = 0;
                while (i4 < i3) {
                    j3 += (long) iArr2[i4];
                    i4++;
                    map = map;
                    iArr = iArr;
                }
                HashMap map2 = map;
                int[] iArr3 = iArr;
                if (track4 == track2) {
                    jArr[i3] = j2;
                }
                int iL2i = CastUtils.l2i(j3);
                while (true) {
                    int[] iArr4 = iArr2;
                    if (iL2i < ((long) iArr2[i3]) + j3) {
                        j2 += this.track2SampleSizes.get(track4)[iL2i];
                        iL2i++;
                        iArr2 = iArr4;
                    }
                }
                sampleTableBox4 = sampleTableBox;
                map = map2;
                iArr = iArr3;
                track2 = track;
            }
            i3++;
            track2 = track;
        }
        SampleTableBox sampleTableBox5 = sampleTableBox4;
        staticChunkOffsetBox.setChunkOffsets(jArr);
        sampleTableBox5.addBox(staticChunkOffsetBox);
        r232.addBox(sampleTableBox5);
        r22.addBox(r232);
        return trackBox3;
    }

    private class InterleaveChunkMdat implements Box {
        long contentSize;
        ContainerBox parent;
        List<ByteBuffer> samples;
        List<Track> tracks;

        @Override
        public ContainerBox getParent() {
            return this.parent;
        }

        @Override
        public void setParent(ContainerBox containerBox) {
            this.parent = containerBox;
        }

        @Override
        public void parse(ReadableByteChannel readableByteChannel, ByteBuffer byteBuffer, long j, BoxParser boxParser) throws IOException {
        }

        private InterleaveChunkMdat(Movie movie) {
            this.samples = new ArrayList();
            long j = 0;
            this.contentSize = 0L;
            this.tracks = movie.getTracks();
            HashMap map = new HashMap();
            for (Track track : movie.getTracks()) {
                map.put(track, DefaultMp4Builder.this.getChunkSizes(track, movie));
            }
            int i = 0;
            while (i < ((int[]) map.values().iterator().next()).length) {
                for (Track track2 : this.tracks) {
                    int[] iArr = (int[]) map.get(track2);
                    long j2 = j;
                    for (int i2 = 0; i2 < i; i2++) {
                        j2 += (long) iArr[i2];
                    }
                    int iL2i = CastUtils.l2i(j2);
                    while (iL2i < ((long) iArr[i]) + j2) {
                        ByteBuffer byteBuffer = DefaultMp4Builder.this.track2Sample.get(track2).get(iL2i);
                        this.contentSize += (long) byteBuffer.limit();
                        this.samples.add((ByteBuffer) byteBuffer.rewind());
                        iL2i++;
                        i = i;
                    }
                    j = 0;
                }
                i++;
                j = 0;
            }
        }

        public long getDataOffset() {
            Box next;
            long size = 16;
            for (Box parent = this; parent.getParent() != null; parent = parent.getParent()) {
                Iterator<Box> it = parent.getParent().getBoxes().iterator();
                while (it.hasNext() && parent != (next = it.next())) {
                    size += next.getSize();
                }
            }
            return size;
        }

        @Override
        public String getType() {
            return "mdat";
        }

        @Override
        public long getSize() {
            return 16 + this.contentSize;
        }

        private boolean isSmallBox(long j) {
            return j + 8 < 4294967296L;
        }

        @Override
        public void getBox(WritableByteChannel writableByteChannel) throws IOException {
            ByteBuffer byteBufferAllocate = ByteBuffer.allocate(16);
            long size = getSize();
            if (isSmallBox(size)) {
                IsoTypeWriter.writeUInt32(byteBufferAllocate, size);
            } else {
                IsoTypeWriter.writeUInt32(byteBufferAllocate, 1L);
            }
            byteBufferAllocate.put(IsoFile.fourCCtoBytes("mdat"));
            if (isSmallBox(size)) {
                byteBufferAllocate.put(new byte[8]);
            } else {
                IsoTypeWriter.writeUInt64(byteBufferAllocate, size);
            }
            byteBufferAllocate.rewind();
            writableByteChannel.write(byteBufferAllocate);
            if (writableByteChannel instanceof GatheringByteChannel) {
                List<ByteBuffer> listUnifyAdjacentBuffers = DefaultMp4Builder.this.unifyAdjacentBuffers(this.samples);
                int i = 0;
                while (i < Math.ceil(((double) listUnifyAdjacentBuffers.size()) / ((double) DefaultMp4Builder.this.STEPSIZE))) {
                    int i2 = DefaultMp4Builder.this.STEPSIZE * i;
                    i++;
                    List<ByteBuffer> listSubList = listUnifyAdjacentBuffers.subList(i2, DefaultMp4Builder.this.STEPSIZE * i < listUnifyAdjacentBuffers.size() ? DefaultMp4Builder.this.STEPSIZE * i : listUnifyAdjacentBuffers.size());
                    ByteBuffer[] byteBufferArr = (ByteBuffer[]) listSubList.toArray(new ByteBuffer[listSubList.size()]);
                    do {
                        ((GatheringByteChannel) writableByteChannel).write(byteBufferArr);
                    } while (byteBufferArr[byteBufferArr.length - 1].remaining() > 0);
                }
                return;
            }
            for (ByteBuffer byteBuffer : this.samples) {
                byteBuffer.rewind();
                writableByteChannel.write(byteBuffer);
            }
        }
    }

    int[] getChunkSizes(Track track, Movie movie) {
        long size;
        long[] jArrSampleNumbers = this.intersectionFinder.sampleNumbers(track, movie);
        int[] iArr = new int[jArrSampleNumbers.length];
        int i = 0;
        while (i < jArrSampleNumbers.length) {
            long j = jArrSampleNumbers[i] - 1;
            int i2 = i + 1;
            if (jArrSampleNumbers.length == i2) {
                size = track.getSamples().size();
            } else {
                size = jArrSampleNumbers[i2] - 1;
            }
            iArr[i] = CastUtils.l2i(size - j);
            i = i2;
        }
        return iArr;
    }

    private static long sum(int[] iArr) {
        long j = 0;
        for (int i : iArr) {
            j += (long) i;
        }
        return j;
    }

    protected static long getDuration(Track track) {
        long count = 0;
        for (TimeToSampleBox.Entry entry : track.getDecodingTimeEntries()) {
            count += entry.getCount() * entry.getDelta();
        }
        return count;
    }

    public long getTimescale(Movie movie) {
        long timescale = movie.getTracks().iterator().next().getTrackMetaData().getTimescale();
        Iterator<Track> it = movie.getTracks().iterator();
        while (it.hasNext()) {
            timescale = gcd(it.next().getTrackMetaData().getTimescale(), timescale);
        }
        return timescale;
    }

    public static long gcd(long j, long j2) {
        if (j2 == 0) {
            return j;
        }
        return gcd(j2, j % j2);
    }

    public List<ByteBuffer> unifyAdjacentBuffers(List<ByteBuffer> list) {
        ArrayList arrayList = new ArrayList(list.size());
        for (ByteBuffer byteBuffer : list) {
            int size = arrayList.size() - 1;
            if (size >= 0 && byteBuffer.hasArray() && ((ByteBuffer) arrayList.get(size)).hasArray() && byteBuffer.array() == ((ByteBuffer) arrayList.get(size)).array() && ((ByteBuffer) arrayList.get(size)).arrayOffset() + ((ByteBuffer) arrayList.get(size)).limit() == byteBuffer.arrayOffset()) {
                ByteBuffer byteBuffer2 = (ByteBuffer) arrayList.remove(size);
                arrayList.add(ByteBuffer.wrap(byteBuffer.array(), byteBuffer2.arrayOffset(), byteBuffer2.limit() + byteBuffer.limit()).slice());
            } else if (size >= 0 && (byteBuffer instanceof MappedByteBuffer) && (arrayList.get(size) instanceof MappedByteBuffer) && ((ByteBuffer) arrayList.get(size)).limit() == ((ByteBuffer) arrayList.get(size)).capacity() - byteBuffer.capacity()) {
                ByteBuffer byteBuffer3 = (ByteBuffer) arrayList.get(size);
                byteBuffer3.limit(byteBuffer.limit() + byteBuffer3.limit());
            } else {
                arrayList.add(byteBuffer);
            }
        }
        return arrayList;
    }
}
