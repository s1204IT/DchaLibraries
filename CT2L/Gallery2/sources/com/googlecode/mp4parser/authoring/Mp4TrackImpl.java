package com.googlecode.mp4parser.authoring;

import com.coremedia.iso.boxes.AbstractMediaHeaderBox;
import com.coremedia.iso.boxes.CompositionTimeToSample;
import com.coremedia.iso.boxes.MediaHeaderBox;
import com.coremedia.iso.boxes.SampleDependencyTypeBox;
import com.coremedia.iso.boxes.SampleDescriptionBox;
import com.coremedia.iso.boxes.SampleTableBox;
import com.coremedia.iso.boxes.TimeToSampleBox;
import com.coremedia.iso.boxes.TrackBox;
import com.coremedia.iso.boxes.TrackHeaderBox;
import com.coremedia.iso.boxes.fragment.MovieExtendsBox;
import com.coremedia.iso.boxes.fragment.MovieFragmentBox;
import com.coremedia.iso.boxes.fragment.SampleFlags;
import com.coremedia.iso.boxes.fragment.TrackExtendsBox;
import com.coremedia.iso.boxes.fragment.TrackFragmentBox;
import com.coremedia.iso.boxes.fragment.TrackFragmentHeaderBox;
import com.coremedia.iso.boxes.fragment.TrackRunBox;
import com.coremedia.iso.boxes.mdat.SampleList;
import com.googlecode.mp4parser.util.CastUtils;
import java.nio.ByteBuffer;
import java.util.LinkedList;
import java.util.List;

public class Mp4TrackImpl extends AbstractTrack {
    private List<CompositionTimeToSample.Entry> compositionTimeEntries;
    private List<TimeToSampleBox.Entry> decodingTimeEntries;
    private String handler;
    private AbstractMediaHeaderBox mihd;
    private List<SampleDependencyTypeBox.Entry> sampleDependencies;
    private SampleDescriptionBox sampleDescriptionBox;
    private List<ByteBuffer> samples;
    private long[] syncSamples;
    private TrackMetaData trackMetaData = new TrackMetaData();

    public Mp4TrackImpl(TrackBox trackBox) {
        SampleFlags sampleFlags;
        this.syncSamples = new long[0];
        long trackId = trackBox.getTrackHeaderBox().getTrackId();
        this.samples = new SampleList(trackBox);
        SampleTableBox stbl = trackBox.getMediaBox().getMediaInformationBox().getSampleTableBox();
        this.handler = trackBox.getMediaBox().getHandlerBox().getHandlerType();
        this.mihd = trackBox.getMediaBox().getMediaInformationBox().getMediaHeaderBox();
        this.decodingTimeEntries = new LinkedList();
        this.compositionTimeEntries = new LinkedList();
        this.sampleDependencies = new LinkedList();
        this.decodingTimeEntries.addAll(stbl.getTimeToSampleBox().getEntries());
        if (stbl.getCompositionTimeToSample() != null) {
            this.compositionTimeEntries.addAll(stbl.getCompositionTimeToSample().getEntries());
        }
        if (stbl.getSampleDependencyTypeBox() != null) {
            this.sampleDependencies.addAll(stbl.getSampleDependencyTypeBox().getEntries());
        }
        if (stbl.getSyncSampleBox() != null) {
            this.syncSamples = stbl.getSyncSampleBox().getSampleNumber();
        }
        this.sampleDescriptionBox = stbl.getSampleDescriptionBox();
        List<MovieExtendsBox> movieExtendsBoxes = trackBox.getParent().getBoxes(MovieExtendsBox.class);
        if (movieExtendsBoxes.size() > 0) {
            for (MovieExtendsBox mvex : movieExtendsBoxes) {
                List<TrackExtendsBox> trackExtendsBoxes = mvex.getBoxes(TrackExtendsBox.class);
                for (TrackExtendsBox trex : trackExtendsBoxes) {
                    if (trex.getTrackId() == trackId) {
                        List<Long> syncSampleList = new LinkedList<>();
                        long sampleNumber = 1;
                        for (MovieFragmentBox movieFragmentBox : trackBox.getIsoFile().getBoxes(MovieFragmentBox.class)) {
                            List<TrackFragmentBox> trafs = movieFragmentBox.getBoxes(TrackFragmentBox.class);
                            for (TrackFragmentBox traf : trafs) {
                                if (traf.getTrackFragmentHeaderBox().getTrackId() == trackId) {
                                    List<TrackRunBox> truns = traf.getBoxes(TrackRunBox.class);
                                    for (TrackRunBox trun : truns) {
                                        TrackFragmentHeaderBox tfhd = ((TrackFragmentBox) trun.getParent()).getTrackFragmentHeaderBox();
                                        boolean first = true;
                                        for (TrackRunBox.Entry entry : trun.getEntries()) {
                                            if (trun.isSampleDurationPresent()) {
                                                if (this.decodingTimeEntries.size() == 0 || this.decodingTimeEntries.get(this.decodingTimeEntries.size() - 1).getDelta() != entry.getSampleDuration()) {
                                                    this.decodingTimeEntries.add(new TimeToSampleBox.Entry(1L, entry.getSampleDuration()));
                                                } else {
                                                    TimeToSampleBox.Entry e = this.decodingTimeEntries.get(this.decodingTimeEntries.size() - 1);
                                                    e.setCount(e.getCount() + 1);
                                                }
                                            } else if (tfhd.hasDefaultSampleDuration()) {
                                                this.decodingTimeEntries.add(new TimeToSampleBox.Entry(1L, tfhd.getDefaultSampleDuration()));
                                            } else {
                                                this.decodingTimeEntries.add(new TimeToSampleBox.Entry(1L, trex.getDefaultSampleDuration()));
                                            }
                                            if (trun.isSampleCompositionTimeOffsetPresent()) {
                                                if (this.compositionTimeEntries.size() == 0 || this.compositionTimeEntries.get(this.compositionTimeEntries.size() - 1).getOffset() != entry.getSampleCompositionTimeOffset()) {
                                                    this.compositionTimeEntries.add(new CompositionTimeToSample.Entry(1, CastUtils.l2i(entry.getSampleCompositionTimeOffset())));
                                                } else {
                                                    CompositionTimeToSample.Entry e2 = this.compositionTimeEntries.get(this.compositionTimeEntries.size() - 1);
                                                    e2.setCount(e2.getCount() + 1);
                                                }
                                            }
                                            if (trun.isSampleFlagsPresent()) {
                                                sampleFlags = entry.getSampleFlags();
                                            } else if (first && trun.isFirstSampleFlagsPresent()) {
                                                sampleFlags = trun.getFirstSampleFlags();
                                            } else if (tfhd.hasDefaultSampleFlags()) {
                                                sampleFlags = tfhd.getDefaultSampleFlags();
                                            } else {
                                                sampleFlags = trex.getDefaultSampleFlags();
                                            }
                                            if (sampleFlags != null && !sampleFlags.isSampleIsDifferenceSample()) {
                                                syncSampleList.add(Long.valueOf(sampleNumber));
                                            }
                                            sampleNumber++;
                                            first = false;
                                        }
                                    }
                                }
                            }
                        }
                        long[] oldSS = this.syncSamples;
                        this.syncSamples = new long[this.syncSamples.length + syncSampleList.size()];
                        System.arraycopy(oldSS, 0, this.syncSamples, 0, oldSS.length);
                        int i = oldSS.length;
                        for (Long syncSampleNumber : syncSampleList) {
                            this.syncSamples[i] = syncSampleNumber.longValue();
                            i++;
                        }
                    }
                }
            }
        }
        MediaHeaderBox mdhd = trackBox.getMediaBox().getMediaHeaderBox();
        TrackHeaderBox tkhd = trackBox.getTrackHeaderBox();
        setEnabled(tkhd.isEnabled());
        setInMovie(tkhd.isInMovie());
        setInPoster(tkhd.isInPoster());
        setInPreview(tkhd.isInPreview());
        this.trackMetaData.setTrackId(tkhd.getTrackId());
        this.trackMetaData.setCreationTime(DateHelper.convert(mdhd.getCreationTime()));
        this.trackMetaData.setLanguage(mdhd.getLanguage());
        this.trackMetaData.setModificationTime(DateHelper.convert(mdhd.getModificationTime()));
        this.trackMetaData.setTimescale(mdhd.getTimescale());
        this.trackMetaData.setHeight(tkhd.getHeight());
        this.trackMetaData.setWidth(tkhd.getWidth());
        this.trackMetaData.setLayer(tkhd.getLayer());
        this.trackMetaData.setMatrix(tkhd.getMatrix());
    }

    @Override
    public List<ByteBuffer> getSamples() {
        return this.samples;
    }

    @Override
    public SampleDescriptionBox getSampleDescriptionBox() {
        return this.sampleDescriptionBox;
    }

    @Override
    public List<TimeToSampleBox.Entry> getDecodingTimeEntries() {
        return this.decodingTimeEntries;
    }

    @Override
    public List<CompositionTimeToSample.Entry> getCompositionTimeEntries() {
        return this.compositionTimeEntries;
    }

    @Override
    public long[] getSyncSamples() {
        return this.syncSamples;
    }

    @Override
    public List<SampleDependencyTypeBox.Entry> getSampleDependencies() {
        return this.sampleDependencies;
    }

    @Override
    public TrackMetaData getTrackMetaData() {
        return this.trackMetaData;
    }

    @Override
    public String getHandler() {
        return this.handler;
    }

    @Override
    public AbstractMediaHeaderBox getMediaHeaderBox() {
        return this.mihd;
    }

    public String toString() {
        return "Mp4TrackImpl{handler='" + this.handler + "'}";
    }
}
