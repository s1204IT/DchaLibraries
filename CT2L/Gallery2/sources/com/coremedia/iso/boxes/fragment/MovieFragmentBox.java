package com.coremedia.iso.boxes.fragment;

import com.coremedia.iso.boxes.Box;
import com.googlecode.mp4parser.AbstractContainerBox;
import java.util.Iterator;

public class MovieFragmentBox extends AbstractContainerBox {
    public MovieFragmentBox() {
        super("moof");
    }

    public long getOffset() {
        Box box;
        long offset = 0;
        for (Box b = this; b.getParent() != null; b = b.getParent()) {
            Iterator<Box> it = b.getParent().getBoxes().iterator();
            while (it.hasNext() && b != (box = it.next())) {
                offset += box.getSize();
            }
        }
        return offset;
    }
}
