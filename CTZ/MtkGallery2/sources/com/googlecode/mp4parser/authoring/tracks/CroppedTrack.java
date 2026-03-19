package com.googlecode.mp4parser.authoring.tracks;

import com.coremedia.iso.boxes.Box;
import com.coremedia.iso.boxes.CompositionTimeToSample;
import com.coremedia.iso.boxes.SampleDependencyTypeBox;
import com.coremedia.iso.boxes.SampleDescriptionBox;
import com.coremedia.iso.boxes.TimeToSampleBox;
import com.googlecode.mp4parser.authoring.AbstractTrack;
import com.googlecode.mp4parser.authoring.Track;
import com.googlecode.mp4parser.authoring.TrackMetaData;
import java.nio.ByteBuffer;
import java.util.LinkedList;
import java.util.List;

public class CroppedTrack extends AbstractTrack {
    static final boolean $assertionsDisabled = false;
    private int fromSample;
    Track origTrack;
    private long[] syncSampleArray;
    private int toSample;

    public CroppedTrack(Track track, long j, long j2) {
        this.origTrack = track;
        this.fromSample = (int) j;
        this.toSample = (int) j2;
    }

    @Override
    public List<ByteBuffer> getSamples() {
        return this.origTrack.getSamples().subList(this.fromSample, this.toSample);
    }

    @Override
    public SampleDescriptionBox getSampleDescriptionBox() {
        return this.origTrack.getSampleDescriptionBox();
    }

    @Override
    public List<TimeToSampleBox.Entry> getDecodingTimeEntries() {
        if (this.origTrack.getDecodingTimeEntries() != null && !this.origTrack.getDecodingTimeEntries().isEmpty()) {
            long[] jArrBlowupTimeToSamples = TimeToSampleBox.blowupTimeToSamples(this.origTrack.getDecodingTimeEntries());
            long[] jArr = new long[this.toSample - this.fromSample];
            System.arraycopy(jArrBlowupTimeToSamples, this.fromSample, jArr, 0, this.toSample - this.fromSample);
            LinkedList linkedList = new LinkedList();
            for (long j : jArr) {
                if (linkedList.isEmpty() || ((TimeToSampleBox.Entry) linkedList.getLast()).getDelta() != j) {
                    linkedList.add(new TimeToSampleBox.Entry(1L, j));
                } else {
                    TimeToSampleBox.Entry entry = (TimeToSampleBox.Entry) linkedList.getLast();
                    entry.setCount(entry.getCount() + 1);
                }
            }
            return linkedList;
        }
        return null;
    }

    @Override
    public List<CompositionTimeToSample.Entry> getCompositionTimeEntries() {
        if (this.origTrack.getCompositionTimeEntries() != null && !this.origTrack.getCompositionTimeEntries().isEmpty()) {
            int[] iArrBlowupCompositionTimes = CompositionTimeToSample.blowupCompositionTimes(this.origTrack.getCompositionTimeEntries());
            int[] iArr = new int[this.toSample - this.fromSample];
            System.arraycopy(iArrBlowupCompositionTimes, this.fromSample, iArr, 0, this.toSample - this.fromSample);
            LinkedList linkedList = new LinkedList();
            for (int i : iArr) {
                if (linkedList.isEmpty() || ((CompositionTimeToSample.Entry) linkedList.getLast()).getOffset() != i) {
                    linkedList.add(new CompositionTimeToSample.Entry(1, i));
                } else {
                    CompositionTimeToSample.Entry entry = (CompositionTimeToSample.Entry) linkedList.getLast();
                    entry.setCount(entry.getCount() + 1);
                }
            }
            return linkedList;
        }
        return null;
    }

    @Override
    public synchronized long[] getSyncSamples() {
        if (this.syncSampleArray == null) {
            if (this.origTrack.getSyncSamples() != null && this.origTrack.getSyncSamples().length > 0) {
                LinkedList linkedList = new LinkedList();
                for (long j : this.origTrack.getSyncSamples()) {
                    if (j >= this.fromSample && j < this.toSample) {
                        linkedList.add(Long.valueOf(j - ((long) this.fromSample)));
                    }
                }
                this.syncSampleArray = new long[linkedList.size()];
                for (int i = 0; i < this.syncSampleArray.length; i++) {
                    this.syncSampleArray[i] = ((Long) linkedList.get(i)).longValue();
                }
                return this.syncSampleArray;
            }
            return null;
        }
        return this.syncSampleArray;
    }

    @Override
    public List<SampleDependencyTypeBox.Entry> getSampleDependencies() {
        if (this.origTrack.getSampleDependencies() != null && !this.origTrack.getSampleDependencies().isEmpty()) {
            return this.origTrack.getSampleDependencies().subList(this.fromSample, this.toSample);
        }
        return null;
    }

    @Override
    public TrackMetaData getTrackMetaData() {
        return this.origTrack.getTrackMetaData();
    }

    @Override
    public String getHandler() {
        return this.origTrack.getHandler();
    }

    @Override
    public Box getMediaHeaderBox() {
        return this.origTrack.getMediaHeaderBox();
    }
}
