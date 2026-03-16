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
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

public class DefaultMp4Builder {
    static final boolean $assertionsDisabled;
    private static Logger LOG;
    public int STEPSIZE = 64;
    Set<StaticChunkOffsetBox> chunkOffsetBoxes = new HashSet();
    HashMap<Track, List<ByteBuffer>> track2Sample = new HashMap<>();
    HashMap<Track, long[]> track2SampleSizes = new HashMap<>();
    private FragmentIntersectionFinder intersectionFinder = new TwoSecondIntersectionFinder();

    static {
        $assertionsDisabled = !DefaultMp4Builder.class.desiredAssertionStatus();
        LOG = Logger.getLogger(DefaultMp4Builder.class.getName());
    }

    public IsoFile build(Movie movie) {
        LOG.fine("Creating movie " + movie);
        for (Track track : movie.getTracks()) {
            List<ByteBuffer> samples = track.getSamples();
            putSamples(track, samples);
            long[] sizes = new long[samples.size()];
            for (int i = 0; i < sizes.length; i++) {
                sizes[i] = samples.get(i).limit();
            }
            putSampleSizes(track, sizes);
        }
        IsoFile isoFile = new IsoFile();
        List<String> minorBrands = new LinkedList<>();
        minorBrands.add("isom");
        minorBrands.add("iso2");
        minorBrands.add("avc1");
        isoFile.addBox(new FileTypeBox("isom", 0L, minorBrands));
        isoFile.addBox(createMovieBox(movie));
        InterleaveChunkMdat mdat = new InterleaveChunkMdat(movie);
        isoFile.addBox(mdat);
        long dataOffset = mdat.getDataOffset();
        for (StaticChunkOffsetBox chunkOffsetBox : this.chunkOffsetBoxes) {
            long[] offsets = chunkOffsetBox.getChunkOffsets();
            for (int i2 = 0; i2 < offsets.length; i2++) {
                offsets[i2] = offsets[i2] + dataOffset;
            }
        }
        return isoFile;
    }

    protected long[] putSampleSizes(Track track, long[] sizes) {
        return this.track2SampleSizes.put(track, sizes);
    }

    protected List<ByteBuffer> putSamples(Track track, List<ByteBuffer> samples) {
        return this.track2Sample.put(track, samples);
    }

    private MovieBox createMovieBox(Movie movie) {
        MovieBox movieBox = new MovieBox();
        MovieHeaderBox mvhd = new MovieHeaderBox();
        mvhd.setCreationTime(DateHelper.convert(new Date()));
        mvhd.setModificationTime(DateHelper.convert(new Date()));
        long movieTimeScale = getTimescale(movie);
        long duration = 0;
        for (Track track : movie.getTracks()) {
            long tracksDuration = (getDuration(track) * movieTimeScale) / track.getTrackMetaData().getTimescale();
            if (tracksDuration > duration) {
                duration = tracksDuration;
            }
        }
        mvhd.setDuration(duration);
        mvhd.setTimescale(movieTimeScale);
        long nextTrackId = 0;
        for (Track track2 : movie.getTracks()) {
            if (nextTrackId < track2.getTrackMetaData().getTrackId()) {
                nextTrackId = track2.getTrackMetaData().getTrackId();
            }
        }
        mvhd.setNextTrackId(nextTrackId + 1);
        if (mvhd.getCreationTime() >= 4294967296L || mvhd.getModificationTime() >= 4294967296L || mvhd.getDuration() >= 4294967296L) {
            mvhd.setVersion(1);
        }
        movieBox.addBox(mvhd);
        Iterator<Track> it = movie.getTracks().iterator();
        while (it.hasNext()) {
            movieBox.addBox(createTrackBox(it.next(), movie));
        }
        Box udta = createUdta(movie);
        if (udta != null) {
            movieBox.addBox(udta);
        }
        return movieBox;
    }

    protected Box createUdta(Movie movie) {
        return null;
    }

    private TrackBox createTrackBox(Track track, Movie movie) {
        LOG.info("Creating Mp4TrackImpl " + track);
        TrackBox trackBox = new TrackBox();
        TrackHeaderBox tkhd = new TrackHeaderBox();
        int flags = 0;
        if (track.isEnabled()) {
            flags = 0 + 1;
        }
        if (track.isInMovie()) {
            flags += 2;
        }
        if (track.isInPreview()) {
            flags += 4;
        }
        if (track.isInPoster()) {
            flags += 8;
        }
        tkhd.setFlags(flags);
        tkhd.setAlternateGroup(track.getTrackMetaData().getGroup());
        tkhd.setCreationTime(DateHelper.convert(track.getTrackMetaData().getCreationTime()));
        tkhd.setDuration((getDuration(track) * getTimescale(movie)) / track.getTrackMetaData().getTimescale());
        tkhd.setHeight(track.getTrackMetaData().getHeight());
        tkhd.setWidth(track.getTrackMetaData().getWidth());
        tkhd.setLayer(track.getTrackMetaData().getLayer());
        tkhd.setModificationTime(DateHelper.convert(new Date()));
        tkhd.setTrackId(track.getTrackMetaData().getTrackId());
        tkhd.setVolume(track.getTrackMetaData().getVolume());
        tkhd.setMatrix(track.getTrackMetaData().getMatrix());
        if (tkhd.getCreationTime() >= 4294967296L || tkhd.getModificationTime() >= 4294967296L || tkhd.getDuration() >= 4294967296L) {
            tkhd.setVersion(1);
        }
        trackBox.addBox(tkhd);
        MediaBox mediaBox = new MediaBox();
        trackBox.addBox(mediaBox);
        MediaHeaderBox mdhd = new MediaHeaderBox();
        mdhd.setCreationTime(DateHelper.convert(track.getTrackMetaData().getCreationTime()));
        mdhd.setDuration(getDuration(track));
        mdhd.setTimescale(track.getTrackMetaData().getTimescale());
        mdhd.setLanguage(track.getTrackMetaData().getLanguage());
        mediaBox.addBox(mdhd);
        HandlerBox hdlr = new HandlerBox();
        mediaBox.addBox(hdlr);
        hdlr.setHandlerType(track.getHandler());
        MediaInformationBox mediaInformationBox = new MediaInformationBox();
        mediaInformationBox.addBox(track.getMediaHeaderBox());
        DataInformationBox dataInformationBox = new DataInformationBox();
        DataReferenceBox dref = new DataReferenceBox();
        dataInformationBox.addBox(dref);
        DataEntryUrlBox url = new DataEntryUrlBox();
        url.setFlags(1);
        dref.addBox(url);
        mediaInformationBox.addBox(dataInformationBox);
        SampleTableBox stbl = new SampleTableBox();
        stbl.addBox(track.getSampleDescriptionBox());
        List<TimeToSampleBox.Entry> decodingTimeToSampleEntries = track.getDecodingTimeEntries();
        if (decodingTimeToSampleEntries != null && !track.getDecodingTimeEntries().isEmpty()) {
            TimeToSampleBox stts = new TimeToSampleBox();
            stts.setEntries(track.getDecodingTimeEntries());
            stbl.addBox(stts);
        }
        List<CompositionTimeToSample.Entry> compositionTimeToSampleEntries = track.getCompositionTimeEntries();
        if (compositionTimeToSampleEntries != null && !compositionTimeToSampleEntries.isEmpty()) {
            CompositionTimeToSample ctts = new CompositionTimeToSample();
            ctts.setEntries(compositionTimeToSampleEntries);
            stbl.addBox(ctts);
        }
        long[] syncSamples = track.getSyncSamples();
        if (syncSamples != null && syncSamples.length > 0) {
            SyncSampleBox stss = new SyncSampleBox();
            stss.setSampleNumber(syncSamples);
            stbl.addBox(stss);
        }
        if (track.getSampleDependencies() != null && !track.getSampleDependencies().isEmpty()) {
            SampleDependencyTypeBox sdtp = new SampleDependencyTypeBox();
            sdtp.setEntries(track.getSampleDependencies());
            stbl.addBox(sdtp);
        }
        HashMap<Track, int[]> track2ChunkSizes = new HashMap<>();
        for (Track current : movie.getTracks()) {
            track2ChunkSizes.put(current, getChunkSizes(current, movie));
        }
        int[] tracksChunkSizes = track2ChunkSizes.get(track);
        SampleToChunkBox stsc = new SampleToChunkBox();
        stsc.setEntries(new LinkedList());
        long lastChunkSize = -2147483648L;
        for (int i = 0; i < tracksChunkSizes.length; i++) {
            if (lastChunkSize != tracksChunkSizes[i]) {
                stsc.getEntries().add(new SampleToChunkBox.Entry(i + 1, tracksChunkSizes[i], 1L));
                lastChunkSize = tracksChunkSizes[i];
            }
        }
        stbl.addBox(stsc);
        SampleSizeBox stsz = new SampleSizeBox();
        stsz.setSampleSizes(this.track2SampleSizes.get(track));
        stbl.addBox(stsz);
        StaticChunkOffsetBox stco = new StaticChunkOffsetBox();
        this.chunkOffsetBoxes.add(stco);
        long offset = 0;
        long[] chunkOffset = new long[tracksChunkSizes.length];
        if (LOG.isLoggable(Level.FINE)) {
            LOG.fine("Calculating chunk offsets for track_" + track.getTrackMetaData().getTrackId());
        }
        for (int i2 = 0; i2 < tracksChunkSizes.length; i2++) {
            if (LOG.isLoggable(Level.FINER)) {
                LOG.finer("Calculating chunk offsets for track_" + track.getTrackMetaData().getTrackId() + " chunk " + i2);
            }
            for (Track current2 : movie.getTracks()) {
                if (LOG.isLoggable(Level.FINEST)) {
                    LOG.finest("Adding offsets of track_" + current2.getTrackMetaData().getTrackId());
                }
                int[] chunkSizes = track2ChunkSizes.get(current2);
                long firstSampleOfChunk = 0;
                for (int j = 0; j < i2; j++) {
                    firstSampleOfChunk += (long) chunkSizes[j];
                }
                if (current2 == track) {
                    chunkOffset[i2] = offset;
                }
                for (int j2 = CastUtils.l2i(firstSampleOfChunk); j2 < ((long) chunkSizes[i2]) + firstSampleOfChunk; j2++) {
                    offset += this.track2SampleSizes.get(current2)[j2];
                }
            }
        }
        stco.setChunkOffsets(chunkOffset);
        stbl.addBox(stco);
        mediaInformationBox.addBox(stbl);
        mediaBox.addBox(mediaInformationBox);
        return trackBox;
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
        public void setParent(ContainerBox parent) {
            this.parent = parent;
        }

        @Override
        public void parse(ReadableByteChannel readableByteChannel, ByteBuffer header, long contentSize, BoxParser boxParser) throws IOException {
        }

        private InterleaveChunkMdat(Movie movie) {
            this.samples = new ArrayList();
            this.contentSize = 0L;
            this.tracks = movie.getTracks();
            Map<Track, int[]> chunks = new HashMap<>();
            for (Track track : movie.getTracks()) {
                chunks.put(track, DefaultMp4Builder.this.getChunkSizes(track, movie));
            }
            for (int i = 0; i < chunks.values().iterator().next().length; i++) {
                for (Track track2 : this.tracks) {
                    int[] chunkSizes = chunks.get(track2);
                    long firstSampleOfChunk = 0;
                    for (int j = 0; j < i; j++) {
                        firstSampleOfChunk += (long) chunkSizes[j];
                    }
                    for (int j2 = CastUtils.l2i(firstSampleOfChunk); j2 < ((long) chunkSizes[i]) + firstSampleOfChunk; j2++) {
                        ByteBuffer s = DefaultMp4Builder.this.track2Sample.get(track2).get(j2);
                        this.contentSize += (long) s.limit();
                        this.samples.add((ByteBuffer) s.rewind());
                    }
                }
            }
        }

        public long getDataOffset() {
            Box box;
            long offset = 16;
            for (Box b = this; b.getParent() != null; b = b.getParent()) {
                Iterator<Box> it = b.getParent().getBoxes().iterator();
                while (it.hasNext() && b != (box = it.next())) {
                    offset += box.getSize();
                }
            }
            return offset;
        }

        @Override
        public String getType() {
            return "mdat";
        }

        @Override
        public long getSize() {
            return 16 + this.contentSize;
        }

        private boolean isSmallBox(long contentSize) {
            return 8 + contentSize < 4294967296L;
        }

        @Override
        public void getBox(WritableByteChannel writableByteChannel) throws IOException {
            ByteBuffer bb = ByteBuffer.allocate(16);
            long size = getSize();
            if (isSmallBox(size)) {
                IsoTypeWriter.writeUInt32(bb, size);
            } else {
                IsoTypeWriter.writeUInt32(bb, 1L);
            }
            bb.put(IsoFile.fourCCtoBytes("mdat"));
            if (isSmallBox(size)) {
                bb.put(new byte[8]);
            } else {
                IsoTypeWriter.writeUInt64(bb, size);
            }
            bb.rewind();
            writableByteChannel.write(bb);
            if (writableByteChannel instanceof GatheringByteChannel) {
                List<ByteBuffer> nuSamples = DefaultMp4Builder.this.unifyAdjacentBuffers(this.samples);
                for (int i = 0; i < Math.ceil(((double) nuSamples.size()) / ((double) DefaultMp4Builder.this.STEPSIZE)); i++) {
                    List<ByteBuffer> sublist = nuSamples.subList(i * DefaultMp4Builder.this.STEPSIZE, (i + 1) * DefaultMp4Builder.this.STEPSIZE < nuSamples.size() ? (i + 1) * DefaultMp4Builder.this.STEPSIZE : nuSamples.size());
                    ByteBuffer[] sampleArray = (ByteBuffer[]) sublist.toArray(new ByteBuffer[sublist.size()]);
                    do {
                        ((GatheringByteChannel) writableByteChannel).write(sampleArray);
                    } while (sampleArray[sampleArray.length - 1].remaining() > 0);
                }
                return;
            }
            for (ByteBuffer sample : this.samples) {
                sample.rewind();
                writableByteChannel.write(sample);
            }
        }
    }

    int[] getChunkSizes(Track track, Movie movie) {
        long end;
        long[] referenceChunkStarts = this.intersectionFinder.sampleNumbers(track, movie);
        int[] chunkSizes = new int[referenceChunkStarts.length];
        for (int i = 0; i < referenceChunkStarts.length; i++) {
            long start = referenceChunkStarts[i] - 1;
            if (referenceChunkStarts.length == i + 1) {
                end = track.getSamples().size();
            } else {
                end = referenceChunkStarts[i + 1] - 1;
            }
            chunkSizes[i] = CastUtils.l2i(end - start);
        }
        if ($assertionsDisabled || this.track2Sample.get(track).size() == sum(chunkSizes)) {
            return chunkSizes;
        }
        throw new AssertionError("The number of samples and the sum of all chunk lengths must be equal");
    }

    private static long sum(int[] ls) {
        long rc = 0;
        for (long l : ls) {
            rc += l;
        }
        return rc;
    }

    protected static long getDuration(Track track) {
        long duration = 0;
        for (TimeToSampleBox.Entry entry : track.getDecodingTimeEntries()) {
            duration += entry.getCount() * entry.getDelta();
        }
        return duration;
    }

    public long getTimescale(Movie movie) {
        long timescale = movie.getTracks().iterator().next().getTrackMetaData().getTimescale();
        for (Track track : movie.getTracks()) {
            timescale = gcd(track.getTrackMetaData().getTimescale(), timescale);
        }
        return timescale;
    }

    public static long gcd(long a, long b) {
        return b == 0 ? a : gcd(b, a % b);
    }

    public List<ByteBuffer> unifyAdjacentBuffers(List<ByteBuffer> samples) {
        ArrayList<ByteBuffer> nuSamples = new ArrayList<>(samples.size());
        for (ByteBuffer buffer : samples) {
            int lastIndex = nuSamples.size() - 1;
            if (lastIndex >= 0 && buffer.hasArray() && nuSamples.get(lastIndex).hasArray() && buffer.array() == nuSamples.get(lastIndex).array()) {
                if (nuSamples.get(lastIndex).limit() + nuSamples.get(lastIndex).arrayOffset() == buffer.arrayOffset()) {
                    ByteBuffer oldBuffer = nuSamples.remove(lastIndex);
                    ByteBuffer nu = ByteBuffer.wrap(buffer.array(), oldBuffer.arrayOffset(), oldBuffer.limit() + buffer.limit()).slice();
                    nuSamples.add(nu);
                }
            }
            if (lastIndex >= 0 && (buffer instanceof MappedByteBuffer) && (nuSamples.get(lastIndex) instanceof MappedByteBuffer) && nuSamples.get(lastIndex).limit() == nuSamples.get(lastIndex).capacity() - buffer.capacity()) {
                ByteBuffer oldBuffer2 = nuSamples.get(lastIndex);
                oldBuffer2.limit(buffer.limit() + oldBuffer2.limit());
            } else {
                nuSamples.add(buffer);
            }
        }
        return nuSamples;
    }
}
