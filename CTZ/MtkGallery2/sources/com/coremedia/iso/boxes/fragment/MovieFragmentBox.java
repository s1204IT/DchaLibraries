package com.coremedia.iso.boxes.fragment;

import com.coremedia.iso.boxes.Box;
import com.coremedia.iso.boxes.ContainerBox;
import com.googlecode.mp4parser.AbstractContainerBox;
import java.util.Iterator;

public class MovieFragmentBox extends AbstractContainerBox {
    public MovieFragmentBox() {
        super("moof");
    }

    public long getOffset() {
        Box next;
        long size = 0;
        for (ContainerBox parent = this; parent.getParent() != null; parent = parent.getParent()) {
            Iterator<Box> it = parent.getParent().getBoxes().iterator();
            while (it.hasNext() && parent != (next = it.next())) {
                size += next.getSize();
            }
        }
        return size;
    }
}
