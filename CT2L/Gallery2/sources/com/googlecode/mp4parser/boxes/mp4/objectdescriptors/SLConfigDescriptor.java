package com.googlecode.mp4parser.boxes.mp4.objectdescriptors;

import android.support.v4.app.FragmentManagerImpl;
import com.coremedia.iso.IsoTypeReader;
import java.io.IOException;
import java.nio.ByteBuffer;

@Descriptor(tags = {FragmentManagerImpl.ANIM_STYLE_FADE_EXIT})
public class SLConfigDescriptor extends BaseDescriptor {
    int predefined;

    @Override
    public void parseDetail(ByteBuffer bb) throws IOException {
        this.predefined = IsoTypeReader.readUInt8(bb);
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("SLConfigDescriptor");
        sb.append("{predefined=").append(this.predefined);
        sb.append('}');
        return sb.toString();
    }

    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        SLConfigDescriptor that = (SLConfigDescriptor) o;
        return this.predefined == that.predefined;
    }

    public int hashCode() {
        return this.predefined;
    }
}
