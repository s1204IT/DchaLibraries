package com.coremedia.iso.boxes.mdat;

import com.coremedia.iso.IsoFile;
import com.coremedia.iso.boxes.Box;
import com.coremedia.iso.boxes.ChunkOffsetBox;
import com.coremedia.iso.boxes.SampleSizeBox;
import com.coremedia.iso.boxes.SampleToChunkBox;
import com.coremedia.iso.boxes.TrackBox;
import com.coremedia.iso.boxes.fragment.MovieExtendsBox;
import com.coremedia.iso.boxes.fragment.MovieFragmentBox;
import com.coremedia.iso.boxes.fragment.TrackExtendsBox;
import com.coremedia.iso.boxes.fragment.TrackFragmentBox;
import com.coremedia.iso.boxes.fragment.TrackFragmentHeaderBox;
import com.coremedia.iso.boxes.fragment.TrackRunBox;
import com.googlecode.mp4parser.util.CastUtils;
import java.nio.ByteBuffer;
import java.util.AbstractList;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

public class SampleList extends AbstractList<ByteBuffer> {
    IsoFile isoFile;
    MediaDataBox[] mdats;
    long[] offsets;
    long[] sizes;
    HashMap<MediaDataBox, Long> mdatStartCache = new HashMap<>();
    HashMap<MediaDataBox, Long> mdatEndCache = new HashMap<>();

    public SampleList(TrackBox trackBox) {
        initIsoFile(trackBox.getIsoFile());
        SampleSizeBox sampleSizeBox = trackBox.getSampleTableBox().getSampleSizeBox();
        ChunkOffsetBox chunkOffsetBox = trackBox.getSampleTableBox().getChunkOffsetBox();
        SampleToChunkBox sampleToChunkBox = trackBox.getSampleTableBox().getSampleToChunkBox();
        long[] chunkOffsets = chunkOffsetBox != null ? chunkOffsetBox.getChunkOffsets() : new long[0];
        if (sampleToChunkBox != null && sampleToChunkBox.getEntries().size() > 0 && chunkOffsets.length > 0 && sampleSizeBox != null && sampleSizeBox.getSampleCount() > 0) {
            long[] jArrBlowup = sampleToChunkBox.blowup(chunkOffsets.length);
            if (sampleSizeBox.getSampleSize() > 0) {
                this.sizes = new long[CastUtils.l2i(sampleSizeBox.getSampleCount())];
                Arrays.fill(this.sizes, sampleSizeBox.getSampleSize());
            } else {
                this.sizes = sampleSizeBox.getSampleSizes();
            }
            this.offsets = new long[this.sizes.length];
            int i = 0;
            int i2 = 0;
            while (i < jArrBlowup.length) {
                long j = jArrBlowup[i];
                long j2 = chunkOffsets[i];
                int i3 = i2;
                for (int i4 = 0; i4 < j; i4++) {
                    long j3 = this.sizes[i3];
                    this.offsets[i3] = j2;
                    j2 += j3;
                    i3++;
                }
                i++;
                i2 = i3;
            }
        }
        List boxes = trackBox.getParent().getBoxes(MovieExtendsBox.class);
        if (boxes.size() > 0) {
            HashMap map = new HashMap();
            for (TrackExtendsBox trackExtendsBox : ((MovieExtendsBox) boxes.get(0)).getBoxes(TrackExtendsBox.class)) {
                if (trackExtendsBox.getTrackId() == trackBox.getTrackHeaderBox().getTrackId()) {
                    Iterator it = trackBox.getIsoFile().getBoxes(MovieFragmentBox.class).iterator();
                    while (it.hasNext()) {
                        map.putAll(getOffsets((MovieFragmentBox) it.next(), trackBox.getTrackHeaderBox().getTrackId(), trackExtendsBox));
                    }
                }
            }
            if (this.sizes == null || this.offsets == null) {
                this.sizes = new long[0];
                this.offsets = new long[0];
            }
            splitToArrays(map);
        }
    }

    private void splitToArrays(Map<Long, Long> map) {
        ArrayList arrayList = new ArrayList(map.keySet());
        Collections.sort(arrayList);
        long[] jArr = new long[this.sizes.length + arrayList.size()];
        System.arraycopy(this.sizes, 0, jArr, 0, this.sizes.length);
        long[] jArr2 = new long[this.offsets.length + arrayList.size()];
        System.arraycopy(this.offsets, 0, jArr2, 0, this.offsets.length);
        for (int i = 0; i < arrayList.size(); i++) {
            jArr2[this.offsets.length + i] = ((Long) arrayList.get(i)).longValue();
            jArr[this.sizes.length + i] = map.get(arrayList.get(i)).longValue();
        }
        this.sizes = jArr;
        this.offsets = jArr2;
    }

    private void initIsoFile(IsoFile isoFile) {
        this.isoFile = isoFile;
        LinkedList linkedList = new LinkedList();
        long j = 0;
        for (Box box : this.isoFile.getBoxes()) {
            long size = box.getSize();
            if ("mdat".equals(box.getType())) {
                if (box instanceof MediaDataBox) {
                    long jLimit = ((long) box.getHeader().limit()) + j;
                    this.mdatStartCache.put((MediaDataBox) box, Long.valueOf(jLimit));
                    this.mdatEndCache.put((MediaDataBox) box, Long.valueOf(jLimit + size));
                    linkedList.add(box);
                } else {
                    throw new RuntimeException("Sample need to be in mdats and mdats need to be instanceof MediaDataBox");
                }
            }
            j += size;
        }
        this.mdats = (MediaDataBox[]) linkedList.toArray(new MediaDataBox[linkedList.size()]);
    }

    @Override
    public int size() {
        return this.sizes.length;
    }

    @Override
    public ByteBuffer get(int i) {
        long j = this.offsets[i];
        int iL2i = CastUtils.l2i(this.sizes[i]);
        for (MediaDataBox mediaDataBox : this.mdats) {
            long jLongValue = this.mdatStartCache.get(mediaDataBox).longValue();
            long jLongValue2 = this.mdatEndCache.get(mediaDataBox).longValue();
            if (jLongValue <= j && ((long) iL2i) + j <= jLongValue2) {
                return mediaDataBox.getContent(j - jLongValue, iL2i);
            }
        }
        throw new RuntimeException("The sample with offset " + j + " and size " + iL2i + " is NOT located within an mdat");
    }

    Map<Long, Long> getOffsets(MovieFragmentBox movieFragmentBox, long j, TrackExtendsBox trackExtendsBox) {
        long offset;
        Iterator it;
        Iterator it2;
        long j2;
        HashMap map = new HashMap();
        Iterator it3 = movieFragmentBox.getBoxes(TrackFragmentBox.class).iterator();
        while (it3.hasNext()) {
            TrackFragmentBox trackFragmentBox = (TrackFragmentBox) it3.next();
            if (trackFragmentBox.getTrackFragmentHeaderBox().getTrackId() == j) {
                if (trackFragmentBox.getTrackFragmentHeaderBox().hasBaseDataOffset()) {
                    offset = trackFragmentBox.getTrackFragmentHeaderBox().getBaseDataOffset();
                } else {
                    offset = movieFragmentBox.getOffset();
                }
                Iterator it4 = trackFragmentBox.getBoxes(TrackRunBox.class).iterator();
                while (it4.hasNext()) {
                    TrackRunBox trackRunBox = (TrackRunBox) it4.next();
                    long dataOffset = ((long) trackRunBox.getDataOffset()) + offset;
                    TrackFragmentHeaderBox trackFragmentHeaderBox = ((TrackFragmentBox) trackRunBox.getParent()).getTrackFragmentHeaderBox();
                    long j3 = 0;
                    for (TrackRunBox.Entry entry : trackRunBox.getEntries()) {
                        if (trackRunBox.isSampleSizePresent()) {
                            it = it3;
                            long sampleSize = entry.getSampleSize();
                            it2 = it4;
                            j2 = offset;
                            map.put(Long.valueOf(j3 + dataOffset), Long.valueOf(sampleSize));
                            j3 += sampleSize;
                        } else {
                            it = it3;
                            it2 = it4;
                            j2 = offset;
                            if (trackFragmentHeaderBox.hasDefaultSampleSize()) {
                                long defaultSampleSize = trackFragmentHeaderBox.getDefaultSampleSize();
                                map.put(Long.valueOf(j3 + dataOffset), Long.valueOf(defaultSampleSize));
                                j3 += defaultSampleSize;
                            } else {
                                if (trackExtendsBox == null) {
                                    throw new RuntimeException("File doesn't contain trex box but track fragments aren't fully self contained. Cannot determine sample size.");
                                }
                                long defaultSampleSize2 = trackExtendsBox.getDefaultSampleSize();
                                map.put(Long.valueOf(j3 + dataOffset), Long.valueOf(defaultSampleSize2));
                                j3 += defaultSampleSize2;
                            }
                        }
                        it3 = it;
                        offset = j2;
                        it4 = it2;
                    }
                }
            }
            it3 = it3;
        }
        return map;
    }
}
