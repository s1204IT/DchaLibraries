package android.filterfw.core;

import java.util.Iterator;
import java.util.Map;
import java.util.SortedMap;
import java.util.TreeMap;

public class CachedFrameManager extends SimpleFrameManager {
    private int mStorageCapacity = 25165824;
    private int mStorageSize = 0;
    private int mTimeStamp = 0;
    private SortedMap<Integer, Frame> mAvailableFrames = new TreeMap();

    @Override
    public Frame newFrame(FrameFormat frameFormat) {
        Frame frameFindAvailableFrame = findAvailableFrame(frameFormat, 0, 0L);
        if (frameFindAvailableFrame == null) {
            frameFindAvailableFrame = super.newFrame(frameFormat);
        }
        frameFindAvailableFrame.setTimestamp(-2L);
        return frameFindAvailableFrame;
    }

    @Override
    public Frame newBoundFrame(FrameFormat frameFormat, int i, long j) {
        Frame frameFindAvailableFrame = findAvailableFrame(frameFormat, i, j);
        if (frameFindAvailableFrame == null) {
            frameFindAvailableFrame = super.newBoundFrame(frameFormat, i, j);
        }
        frameFindAvailableFrame.setTimestamp(-2L);
        return frameFindAvailableFrame;
    }

    @Override
    public Frame retainFrame(Frame frame) {
        return super.retainFrame(frame);
    }

    @Override
    public Frame releaseFrame(Frame frame) {
        if (frame.isReusable()) {
            int iDecRefCount = frame.decRefCount();
            if (iDecRefCount == 0 && frame.hasNativeAllocation()) {
                if (!storeFrame(frame)) {
                    frame.releaseNativeAllocation();
                    return null;
                }
                return null;
            }
            if (iDecRefCount < 0) {
                throw new RuntimeException("Frame reference count dropped below 0!");
            }
        } else {
            super.releaseFrame(frame);
        }
        return frame;
    }

    public void clearCache() {
        Iterator<Frame> it = this.mAvailableFrames.values().iterator();
        while (it.hasNext()) {
            it.next().releaseNativeAllocation();
        }
        this.mAvailableFrames.clear();
    }

    @Override
    public void tearDown() {
        clearCache();
    }

    private boolean storeFrame(Frame frame) {
        synchronized (this.mAvailableFrames) {
            int size = frame.getFormat().getSize();
            if (size > this.mStorageCapacity) {
                return false;
            }
            int i = this.mStorageSize + size;
            while (i > this.mStorageCapacity) {
                dropOldestFrame();
                i = this.mStorageSize + size;
            }
            frame.onFrameStore();
            this.mStorageSize = i;
            this.mAvailableFrames.put(Integer.valueOf(this.mTimeStamp), frame);
            this.mTimeStamp++;
            return true;
        }
    }

    private void dropOldestFrame() {
        int iIntValue = this.mAvailableFrames.firstKey().intValue();
        Frame frame = this.mAvailableFrames.get(Integer.valueOf(iIntValue));
        this.mStorageSize -= frame.getFormat().getSize();
        frame.releaseNativeAllocation();
        this.mAvailableFrames.remove(Integer.valueOf(iIntValue));
    }

    private Frame findAvailableFrame(FrameFormat frameFormat, int i, long j) {
        synchronized (this.mAvailableFrames) {
            for (Map.Entry<Integer, Frame> entry : this.mAvailableFrames.entrySet()) {
                Frame value = entry.getValue();
                if (value.getFormat().isReplaceableBy(frameFormat) && i == value.getBindingType() && (i == 0 || j == value.getBindingId())) {
                    super.retainFrame(value);
                    this.mAvailableFrames.remove(entry.getKey());
                    value.onFrameFetch();
                    value.reset(frameFormat);
                    this.mStorageSize -= frameFormat.getSize();
                    return value;
                }
            }
            return null;
        }
    }
}
