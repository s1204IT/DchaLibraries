package mf.org.apache.xerces.impl.dtd.models;

import mf.org.apache.xerces.xni.QName;

public class MixedContentModel implements ContentModelValidator {
    private final QName[] fChildren;
    private final int[] fChildrenType;
    private final int fCount;
    private final boolean fOrdered;

    public MixedContentModel(QName[] children, int[] type, int offset, int length, boolean ordered) {
        this.fCount = length;
        this.fChildren = new QName[this.fCount];
        this.fChildrenType = new int[this.fCount];
        for (int i = 0; i < this.fCount; i++) {
            this.fChildren[i] = new QName(children[offset + i]);
            this.fChildrenType[i] = type[offset + i];
        }
        this.fOrdered = ordered;
    }

    @Override
    public int validate(QName[] children, int offset, int length) {
        if (this.fOrdered) {
            int inIndex = 0;
            for (int outIndex = 0; outIndex < length; outIndex++) {
                if (children[offset + outIndex].localpart != null) {
                    int type = this.fChildrenType[inIndex];
                    if (type == 0) {
                        if (this.fChildren[inIndex].rawname != children[offset + outIndex].rawname) {
                            return outIndex;
                        }
                    } else if (type == 6) {
                        String uri = this.fChildren[inIndex].uri;
                        if (uri != null && uri != children[outIndex].uri) {
                            return outIndex;
                        }
                    } else if (type == 8) {
                        if (children[outIndex].uri != null) {
                            return outIndex;
                        }
                    } else if (type == 7 && this.fChildren[inIndex].uri == children[outIndex].uri) {
                        return outIndex;
                    }
                    inIndex++;
                }
            }
            return -1;
        }
        for (int outIndex2 = 0; outIndex2 < length; outIndex2++) {
            QName curChild = children[offset + outIndex2];
            if (curChild.localpart != null) {
                int inIndex2 = 0;
                while (inIndex2 < this.fCount) {
                    int type2 = this.fChildrenType[inIndex2];
                    if (type2 == 0) {
                        if (curChild.rawname == this.fChildren[inIndex2].rawname) {
                            break;
                        }
                        inIndex2++;
                    } else if (type2 == 6) {
                        String uri2 = this.fChildren[inIndex2].uri;
                        if (uri2 == null || uri2 == children[outIndex2].uri) {
                            break;
                        }
                        inIndex2++;
                    } else if (type2 == 8) {
                        if (children[outIndex2].uri == null) {
                            break;
                        }
                        inIndex2++;
                    } else {
                        if (type2 == 7 && this.fChildren[inIndex2].uri != children[outIndex2].uri) {
                            break;
                        }
                        inIndex2++;
                    }
                }
                if (inIndex2 == this.fCount) {
                    return outIndex2;
                }
            }
        }
        return -1;
    }
}
