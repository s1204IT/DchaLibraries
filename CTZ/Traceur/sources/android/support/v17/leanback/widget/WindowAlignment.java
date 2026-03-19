package android.support.v17.leanback.widget;

class WindowAlignment {
    private int mOrientation = 0;
    public final Axis vertical = new Axis("vertical");
    public final Axis horizontal = new Axis("horizontal");
    private Axis mMainAxis = this.horizontal;
    private Axis mSecondAxis = this.vertical;

    WindowAlignment() {
    }

    public static class Axis {
        private int mMaxEdge;
        private int mMaxScroll;
        private int mMinEdge;
        private int mMinScroll;
        private String mName;
        private int mPaddingMax;
        private int mPaddingMin;
        private boolean mReversedFlow;
        private int mSize;
        private int mPreferredKeyLine = 2;
        private int mWindowAlignment = 3;
        private int mWindowAlignmentOffset = 0;
        private float mWindowAlignmentOffsetPercent = 50.0f;

        public Axis(String name) {
            reset();
            this.mName = name;
        }

        public final void setWindowAlignment(int windowAlignment) {
            this.mWindowAlignment = windowAlignment;
        }

        final boolean isPreferKeylineOverHighEdge() {
            return (this.mPreferredKeyLine & 2) != 0;
        }

        final boolean isPreferKeylineOverLowEdge() {
            return (this.mPreferredKeyLine & 1) != 0;
        }

        public final int getMinScroll() {
            return this.mMinScroll;
        }

        public final void invalidateScrollMin() {
            this.mMinEdge = Integer.MIN_VALUE;
            this.mMinScroll = Integer.MIN_VALUE;
        }

        public final int getMaxScroll() {
            return this.mMaxScroll;
        }

        public final void invalidateScrollMax() {
            this.mMaxEdge = Integer.MAX_VALUE;
            this.mMaxScroll = Integer.MAX_VALUE;
        }

        void reset() {
            this.mMinEdge = Integer.MIN_VALUE;
            this.mMaxEdge = Integer.MAX_VALUE;
        }

        public final boolean isMinUnknown() {
            return this.mMinEdge == Integer.MIN_VALUE;
        }

        public final boolean isMaxUnknown() {
            return this.mMaxEdge == Integer.MAX_VALUE;
        }

        public final void setSize(int size) {
            this.mSize = size;
        }

        public final int getSize() {
            return this.mSize;
        }

        public final void setPadding(int paddingMin, int paddingMax) {
            this.mPaddingMin = paddingMin;
            this.mPaddingMax = paddingMax;
        }

        public final int getPaddingMin() {
            return this.mPaddingMin;
        }

        public final int getPaddingMax() {
            return this.mPaddingMax;
        }

        public final int getClientSize() {
            return (this.mSize - this.mPaddingMin) - this.mPaddingMax;
        }

        final int calculateKeyline() {
            int keyLine;
            int keyLine2;
            if (!this.mReversedFlow) {
                if (this.mWindowAlignmentOffset >= 0) {
                    keyLine2 = this.mWindowAlignmentOffset;
                } else {
                    int keyLine3 = this.mSize;
                    keyLine2 = keyLine3 + this.mWindowAlignmentOffset;
                }
                if (this.mWindowAlignmentOffsetPercent != -1.0f) {
                    return keyLine2 + ((int) ((this.mSize * this.mWindowAlignmentOffsetPercent) / 100.0f));
                }
                return keyLine2;
            }
            int keyLine4 = this.mWindowAlignmentOffset;
            if (keyLine4 >= 0) {
                keyLine = this.mSize - this.mWindowAlignmentOffset;
            } else {
                int keyLine5 = this.mWindowAlignmentOffset;
                keyLine = -keyLine5;
            }
            if (this.mWindowAlignmentOffsetPercent != -1.0f) {
                return keyLine - ((int) ((this.mSize * this.mWindowAlignmentOffsetPercent) / 100.0f));
            }
            return keyLine;
        }

        final int calculateScrollToKeyLine(int viewCenterPosition, int keyLine) {
            return viewCenterPosition - keyLine;
        }

        public final void updateMinMax(int minEdge, int maxEdge, int minChildViewCenter, int maxChildViewCenter) {
            this.mMinEdge = minEdge;
            this.mMaxEdge = maxEdge;
            int clientSize = getClientSize();
            int keyLine = calculateKeyline();
            boolean isMinUnknown = isMinUnknown();
            boolean isMaxUnknown = isMaxUnknown();
            if (!isMinUnknown) {
                if (!this.mReversedFlow) {
                    this.mMinScroll = calculateScrollToKeyLine(minChildViewCenter, keyLine);
                } else {
                    this.mMinScroll = calculateScrollToKeyLine(minChildViewCenter, keyLine);
                }
            }
            if (!isMaxUnknown) {
                if (!this.mReversedFlow) {
                    this.mMaxScroll = calculateScrollToKeyLine(maxChildViewCenter, keyLine);
                } else {
                    this.mMaxScroll = calculateScrollToKeyLine(maxChildViewCenter, keyLine);
                }
            }
            if (!isMaxUnknown && !isMinUnknown) {
                if (!this.mReversedFlow) {
                    if ((this.mWindowAlignment & 1) != 0) {
                        if (isPreferKeylineOverLowEdge()) {
                            this.mMinScroll = Math.min(this.mMinScroll, calculateScrollToKeyLine(maxChildViewCenter, keyLine));
                        }
                        this.mMaxScroll = Math.max(this.mMinScroll, this.mMaxScroll);
                        return;
                    } else {
                        if ((this.mWindowAlignment & 2) != 0) {
                            if (isPreferKeylineOverHighEdge()) {
                                this.mMaxScroll = Math.max(this.mMaxScroll, calculateScrollToKeyLine(minChildViewCenter, keyLine));
                            }
                            this.mMinScroll = Math.min(this.mMinScroll, this.mMaxScroll);
                            return;
                        }
                        return;
                    }
                }
                if ((this.mWindowAlignment & 1) != 0) {
                    if (isPreferKeylineOverLowEdge()) {
                        this.mMaxScroll = Math.max(this.mMaxScroll, calculateScrollToKeyLine(minChildViewCenter, keyLine));
                    }
                    this.mMinScroll = Math.min(this.mMinScroll, this.mMaxScroll);
                } else if ((this.mWindowAlignment & 2) != 0) {
                    if (isPreferKeylineOverHighEdge()) {
                        this.mMinScroll = Math.min(this.mMinScroll, calculateScrollToKeyLine(maxChildViewCenter, keyLine));
                    }
                    this.mMaxScroll = Math.max(this.mMinScroll, this.mMaxScroll);
                }
            }
        }

        public final int getScroll(int viewCenter) {
            int size = getSize();
            int keyLine = calculateKeyline();
            boolean isMinUnknown = isMinUnknown();
            boolean isMaxUnknown = isMaxUnknown();
            if (!isMinUnknown) {
                int keyLineToMinEdge = keyLine - this.mPaddingMin;
                if (this.mReversedFlow ? (this.mWindowAlignment & 2) != 0 : (this.mWindowAlignment & 1) != 0) {
                    if (viewCenter - this.mMinEdge <= keyLineToMinEdge) {
                        int alignToMin = this.mMinEdge - this.mPaddingMin;
                        if (!isMaxUnknown && alignToMin > this.mMaxScroll) {
                            return this.mMaxScroll;
                        }
                        return alignToMin;
                    }
                }
            }
            if (!isMaxUnknown) {
                int keyLineToMaxEdge = (size - keyLine) - this.mPaddingMax;
                if (this.mReversedFlow ? (this.mWindowAlignment & 1) != 0 : (this.mWindowAlignment & 2) != 0) {
                    if (this.mMaxEdge - viewCenter <= keyLineToMaxEdge) {
                        int alignToMax = this.mMaxEdge - (size - this.mPaddingMax);
                        if (!isMinUnknown && alignToMax < this.mMinScroll) {
                            return this.mMinScroll;
                        }
                        return alignToMax;
                    }
                }
            }
            int keyLineToMaxEdge2 = calculateScrollToKeyLine(viewCenter, keyLine);
            return keyLineToMaxEdge2;
        }

        public final void setReversedFlow(boolean reversedFlow) {
            this.mReversedFlow = reversedFlow;
        }

        public String toString() {
            return " min:" + this.mMinEdge + " " + this.mMinScroll + " max:" + this.mMaxEdge + " " + this.mMaxScroll;
        }
    }

    public final Axis mainAxis() {
        return this.mMainAxis;
    }

    public final Axis secondAxis() {
        return this.mSecondAxis;
    }

    public final void setOrientation(int orientation) {
        this.mOrientation = orientation;
        if (this.mOrientation == 0) {
            this.mMainAxis = this.horizontal;
            this.mSecondAxis = this.vertical;
        } else {
            this.mMainAxis = this.vertical;
            this.mSecondAxis = this.horizontal;
        }
    }

    public final void reset() {
        mainAxis().reset();
    }

    public String toString() {
        return "horizontal=" + this.horizontal + "; vertical=" + this.vertical;
    }
}
