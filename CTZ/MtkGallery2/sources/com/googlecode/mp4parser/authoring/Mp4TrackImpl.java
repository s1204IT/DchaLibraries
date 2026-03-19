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
import java.util.Iterator;
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
        boolean z;
        long j;
        Iterator it;
        long j2;
        Iterator it2;
        LinkedList linkedList;
        Iterator it3;
        SampleFlags defaultSampleFlags;
        long j3;
        LinkedList linkedList2;
        int i;
        boolean z2 = false;
        this.syncSamples = new long[0];
        long trackId = trackBox.getTrackHeaderBox().getTrackId();
        this.samples = new SampleList(trackBox);
        SampleTableBox sampleTableBox = trackBox.getMediaBox().getMediaInformationBox().getSampleTableBox();
        this.handler = trackBox.getMediaBox().getHandlerBox().getHandlerType();
        this.mihd = trackBox.getMediaBox().getMediaInformationBox().getMediaHeaderBox();
        this.decodingTimeEntries = new LinkedList();
        this.compositionTimeEntries = new LinkedList();
        this.sampleDependencies = new LinkedList();
        this.decodingTimeEntries.addAll(sampleTableBox.getTimeToSampleBox().getEntries());
        if (sampleTableBox.getCompositionTimeToSample() != null) {
            this.compositionTimeEntries.addAll(sampleTableBox.getCompositionTimeToSample().getEntries());
        }
        if (sampleTableBox.getSampleDependencyTypeBox() != null) {
            this.sampleDependencies.addAll(sampleTableBox.getSampleDependencyTypeBox().getEntries());
        }
        if (sampleTableBox.getSyncSampleBox() != null) {
            this.syncSamples = sampleTableBox.getSyncSampleBox().getSampleNumber();
        }
        this.sampleDescriptionBox = sampleTableBox.getSampleDescriptionBox();
        List boxes = trackBox.getParent().getBoxes(MovieExtendsBox.class);
        if (boxes.size() > 0) {
            Iterator it4 = boxes.iterator();
            while (it4.hasNext()) {
                for (TrackExtendsBox trackExtendsBox : ((MovieExtendsBox) it4.next()).getBoxes(TrackExtendsBox.class)) {
                    if (trackExtendsBox.getTrackId() == trackId) {
                        LinkedList linkedList3 = new LinkedList();
                        Iterator it5 = trackBox.getIsoFile().getBoxes(MovieFragmentBox.class).iterator();
                        long j4 = 1;
                        while (it5.hasNext()) {
                            for (TrackFragmentBox trackFragmentBox : ((MovieFragmentBox) it5.next()).getBoxes(TrackFragmentBox.class)) {
                                if (trackFragmentBox.getTrackFragmentHeaderBox().getTrackId() == trackId) {
                                    Iterator it6 = trackFragmentBox.getBoxes(TrackRunBox.class).iterator();
                                    while (it6.hasNext()) {
                                        TrackRunBox trackRunBox = (TrackRunBox) it6.next();
                                        TrackFragmentHeaderBox trackFragmentHeaderBox = ((TrackFragmentBox) trackRunBox.getParent()).getTrackFragmentHeaderBox();
                                        Iterator it7 = it6;
                                        long j5 = j4;
                                        boolean z3 = true;
                                        for (TrackRunBox.Entry entry : trackRunBox.getEntries()) {
                                            if (trackRunBox.isSampleDurationPresent()) {
                                                if (this.decodingTimeEntries.size() != 0) {
                                                    j2 = trackId;
                                                    if (this.decodingTimeEntries.get(this.decodingTimeEntries.size() - 1).getDelta() == entry.getSampleDuration()) {
                                                        TimeToSampleBox.Entry entry2 = this.decodingTimeEntries.get(this.decodingTimeEntries.size() - 1);
                                                        it2 = it4;
                                                        entry2.setCount(entry2.getCount() + 1);
                                                        linkedList = linkedList3;
                                                        it3 = it5;
                                                    }
                                                } else {
                                                    j2 = trackId;
                                                }
                                                it2 = it4;
                                                linkedList = linkedList3;
                                                it3 = it5;
                                                this.decodingTimeEntries.add(new TimeToSampleBox.Entry(1L, entry.getSampleDuration()));
                                            } else {
                                                j2 = trackId;
                                                it2 = it4;
                                                linkedList = linkedList3;
                                                it3 = it5;
                                                if (trackFragmentHeaderBox.hasDefaultSampleDuration()) {
                                                    this.decodingTimeEntries.add(new TimeToSampleBox.Entry(1L, trackFragmentHeaderBox.getDefaultSampleDuration()));
                                                } else {
                                                    this.decodingTimeEntries.add(new TimeToSampleBox.Entry(1L, trackExtendsBox.getDefaultSampleDuration()));
                                                }
                                            }
                                            if (trackRunBox.isSampleCompositionTimeOffsetPresent()) {
                                                if (this.compositionTimeEntries.size() != 0) {
                                                    i = 1;
                                                    if (this.compositionTimeEntries.get(this.compositionTimeEntries.size() - 1).getOffset() == entry.getSampleCompositionTimeOffset()) {
                                                        CompositionTimeToSample.Entry entry3 = this.compositionTimeEntries.get(this.compositionTimeEntries.size() - 1);
                                                        entry3.setCount(entry3.getCount() + 1);
                                                    }
                                                } else {
                                                    i = 1;
                                                }
                                                this.compositionTimeEntries.add(new CompositionTimeToSample.Entry(i, CastUtils.l2i(entry.getSampleCompositionTimeOffset())));
                                            }
                                            if (trackRunBox.isSampleFlagsPresent()) {
                                                defaultSampleFlags = entry.getSampleFlags();
                                            } else if (z3 && trackRunBox.isFirstSampleFlagsPresent()) {
                                                defaultSampleFlags = trackRunBox.getFirstSampleFlags();
                                            } else if (trackFragmentHeaderBox.hasDefaultSampleFlags()) {
                                                defaultSampleFlags = trackFragmentHeaderBox.getDefaultSampleFlags();
                                            } else {
                                                defaultSampleFlags = trackExtendsBox.getDefaultSampleFlags();
                                            }
                                            if (defaultSampleFlags != null && !defaultSampleFlags.isSampleIsDifferenceSample()) {
                                                j3 = j5;
                                                linkedList2 = linkedList;
                                                linkedList2.add(Long.valueOf(j3));
                                            } else {
                                                j3 = j5;
                                                linkedList2 = linkedList;
                                            }
                                            j5 = j3 + 1;
                                            linkedList3 = linkedList2;
                                            trackId = j2;
                                            it4 = it2;
                                            it5 = it3;
                                            z3 = false;
                                        }
                                        j4 = j5;
                                        it6 = it7;
                                        trackId = trackId;
                                        it4 = it4;
                                    }
                                }
                                linkedList3 = linkedList3;
                                trackId = trackId;
                                it4 = it4;
                                it5 = it5;
                            }
                            trackId = trackId;
                            it4 = it4;
                        }
                        j = trackId;
                        it = it4;
                        LinkedList linkedList4 = linkedList3;
                        long[] jArr = this.syncSamples;
                        this.syncSamples = new long[this.syncSamples.length + linkedList4.size()];
                        z = false;
                        System.arraycopy(jArr, 0, this.syncSamples, 0, jArr.length);
                        Iterator it8 = linkedList4.iterator();
                        int length = jArr.length;
                        while (it8.hasNext()) {
                            this.syncSamples[length] = ((Long) it8.next()).longValue();
                            length++;
                        }
                    } else {
                        z = z2;
                        j = trackId;
                        it = it4;
                    }
                    z2 = z;
                    trackId = j;
                    it4 = it;
                }
            }
        }
        MediaHeaderBox mediaHeaderBox = trackBox.getMediaBox().getMediaHeaderBox();
        TrackHeaderBox trackHeaderBox = trackBox.getTrackHeaderBox();
        setEnabled(trackHeaderBox.isEnabled());
        setInMovie(trackHeaderBox.isInMovie());
        setInPoster(trackHeaderBox.isInPoster());
        setInPreview(trackHeaderBox.isInPreview());
        this.trackMetaData.setTrackId(trackHeaderBox.getTrackId());
        this.trackMetaData.setCreationTime(DateHelper.convert(mediaHeaderBox.getCreationTime()));
        this.trackMetaData.setLanguage(mediaHeaderBox.getLanguage());
        this.trackMetaData.setModificationTime(DateHelper.convert(mediaHeaderBox.getModificationTime()));
        this.trackMetaData.setTimescale(mediaHeaderBox.getTimescale());
        this.trackMetaData.setHeight(trackHeaderBox.getHeight());
        this.trackMetaData.setWidth(trackHeaderBox.getWidth());
        this.trackMetaData.setLayer(trackHeaderBox.getLayer());
        this.trackMetaData.setMatrix(trackHeaderBox.getMatrix());
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
