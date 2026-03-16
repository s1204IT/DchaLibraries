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
    static final boolean $assertionsDisabled;
    private int fromSample;
    Track origTrack;
    private long[] syncSampleArray;
    private int toSample;

    static {
        $assertionsDisabled = !CroppedTrack.class.desiredAssertionStatus();
    }

    public CroppedTrack(Track origTrack, long fromSample, long toSample) {
        this.origTrack = origTrack;
        if (!$assertionsDisabled && fromSample > 2147483647L) {
            throw new AssertionError();
        }
        if (!$assertionsDisabled && toSample > 2147483647L) {
            throw new AssertionError();
        }
        this.fromSample = (int) fromSample;
        this.toSample = (int) toSample;
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
            long[] decodingTimes = TimeToSampleBox.blowupTimeToSamples(this.origTrack.getDecodingTimeEntries());
            long[] nuDecodingTimes = new long[this.toSample - this.fromSample];
            System.arraycopy(decodingTimes, this.fromSample, nuDecodingTimes, 0, this.toSample - this.fromSample);
            LinkedList<TimeToSampleBox.Entry> returnDecodingEntries = new LinkedList<>();
            for (long nuDecodingTime : nuDecodingTimes) {
                if (returnDecodingEntries.isEmpty() || returnDecodingEntries.getLast().getDelta() != nuDecodingTime) {
                    TimeToSampleBox.Entry e = new TimeToSampleBox.Entry(1L, nuDecodingTime);
                    returnDecodingEntries.add(e);
                } else {
                    TimeToSampleBox.Entry e2 = returnDecodingEntries.getLast();
                    TimeToSampleBox.Entry e3 = e2;
                    e3.setCount(e3.getCount() + 1);
                }
            }
            return returnDecodingEntries;
        }
        return null;
    }

    @Override
    public List<CompositionTimeToSample.Entry> getCompositionTimeEntries() {
        if (this.origTrack.getCompositionTimeEntries() != null && !this.origTrack.getCompositionTimeEntries().isEmpty()) {
            int[] compositionTime = CompositionTimeToSample.blowupCompositionTimes(this.origTrack.getCompositionTimeEntries());
            int[] nuCompositionTimes = new int[this.toSample - this.fromSample];
            System.arraycopy(compositionTime, this.fromSample, nuCompositionTimes, 0, this.toSample - this.fromSample);
            LinkedList<CompositionTimeToSample.Entry> returnDecodingEntries = new LinkedList<>();
            for (int nuDecodingTime : nuCompositionTimes) {
                if (returnDecodingEntries.isEmpty() || returnDecodingEntries.getLast().getOffset() != nuDecodingTime) {
                    CompositionTimeToSample.Entry e = new CompositionTimeToSample.Entry(1, nuDecodingTime);
                    returnDecodingEntries.add(e);
                } else {
                    CompositionTimeToSample.Entry e2 = returnDecodingEntries.getLast();
                    CompositionTimeToSample.Entry e3 = e2;
                    e3.setCount(e3.getCount() + 1);
                }
            }
            return returnDecodingEntries;
        }
        return null;
    }

    @Override
    public synchronized long[] getSyncSamples() {
        long[] jArr;
        if (this.syncSampleArray == null) {
            if (this.origTrack.getSyncSamples() != null && this.origTrack.getSyncSamples().length > 0) {
                List<Long> syncSamples = new LinkedList<>();
                long[] arr$ = this.origTrack.getSyncSamples();
                for (long l : arr$) {
                    if (l >= this.fromSample && l < this.toSample) {
                        syncSamples.add(Long.valueOf(l - ((long) this.fromSample)));
                    }
                }
                this.syncSampleArray = new long[syncSamples.size()];
                for (int i = 0; i < this.syncSampleArray.length; i++) {
                    this.syncSampleArray[i] = syncSamples.get(i).longValue();
                }
                jArr = this.syncSampleArray;
            } else {
                jArr = null;
            }
        } else {
            jArr = this.syncSampleArray;
        }
        return jArr;
    }

    @Override
    public List<SampleDependencyTypeBox.Entry> getSampleDependencies() {
        if (this.origTrack.getSampleDependencies() == null || this.origTrack.getSampleDependencies().isEmpty()) {
            return null;
        }
        return this.origTrack.getSampleDependencies().subList(this.fromSample, this.toSample);
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
