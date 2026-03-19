package android.media;

class TextTrackRegion {
    static final int SCROLL_VALUE_NONE = 300;
    static final int SCROLL_VALUE_SCROLL_UP = 301;
    String mId = "";
    float mWidth = 100.0f;
    int mLines = 3;
    float mViewportAnchorPointX = 0.0f;
    float mAnchorPointX = 0.0f;
    float mViewportAnchorPointY = 100.0f;
    float mAnchorPointY = 100.0f;
    int mScrollValue = 300;

    TextTrackRegion() {
    }

    public String toString() {
        String str;
        StringBuilder sb = new StringBuilder(" {id:\"");
        sb.append(this.mId);
        sb.append("\", width:");
        sb.append(this.mWidth);
        sb.append(", lines:");
        sb.append(this.mLines);
        sb.append(", anchorPoint:(");
        sb.append(this.mAnchorPointX);
        sb.append(", ");
        sb.append(this.mAnchorPointY);
        sb.append("), viewportAnchorPoints:");
        sb.append(this.mViewportAnchorPointX);
        sb.append(", ");
        sb.append(this.mViewportAnchorPointY);
        sb.append("), scrollValue:");
        if (this.mScrollValue == 300) {
            str = "none";
        } else {
            str = this.mScrollValue == 301 ? "scroll_up" : "INVALID";
        }
        sb.append(str);
        sb.append("}");
        return sb.toString();
    }
}
