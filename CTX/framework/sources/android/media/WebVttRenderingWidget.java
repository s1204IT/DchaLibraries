package android.media;

import android.content.Context;
import android.media.SubtitleTrack;
import android.text.Layout;
import android.text.SpannableStringBuilder;
import android.util.ArrayMap;
import android.util.AttributeSet;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.accessibility.CaptioningManager;
import android.widget.LinearLayout;
import com.android.internal.widget.SubtitleView;
import java.util.ArrayList;
import java.util.Vector;

class WebVttRenderingWidget extends ViewGroup implements SubtitleTrack.RenderingWidget {
    private static final boolean DEBUG = false;
    private static final int DEBUG_CUE_BACKGROUND = -2130771968;
    private static final int DEBUG_REGION_BACKGROUND = -2147483393;
    private static final CaptioningManager.CaptionStyle DEFAULT_CAPTION_STYLE = CaptioningManager.CaptionStyle.DEFAULT;
    private static final float LINE_HEIGHT_RATIO = 0.0533f;
    private CaptioningManager.CaptionStyle mCaptionStyle;
    private final CaptioningManager.CaptioningChangeListener mCaptioningListener;
    private final ArrayMap<TextTrackCue, CueLayout> mCueBoxes;
    private float mFontSize;
    private boolean mHasChangeListener;
    private SubtitleTrack.RenderingWidget.OnChangedListener mListener;
    private final CaptioningManager mManager;
    private final ArrayMap<TextTrackRegion, RegionLayout> mRegionBoxes;

    public WebVttRenderingWidget(Context context) {
        this(context, null);
    }

    public WebVttRenderingWidget(Context context, AttributeSet attributeSet) {
        this(context, attributeSet, 0);
    }

    public WebVttRenderingWidget(Context context, AttributeSet attributeSet, int i) {
        this(context, attributeSet, i, 0);
    }

    public WebVttRenderingWidget(Context context, AttributeSet attributeSet, int i, int i2) {
        super(context, attributeSet, i, i2);
        this.mRegionBoxes = new ArrayMap<>();
        this.mCueBoxes = new ArrayMap<>();
        this.mCaptioningListener = new CaptioningManager.CaptioningChangeListener() {
            @Override
            public void onFontScaleChanged(float f) {
                WebVttRenderingWidget.this.setCaptionStyle(WebVttRenderingWidget.this.mCaptionStyle, f * WebVttRenderingWidget.this.getHeight() * WebVttRenderingWidget.LINE_HEIGHT_RATIO);
            }

            @Override
            public void onUserStyleChanged(CaptioningManager.CaptionStyle captionStyle) {
                WebVttRenderingWidget.this.setCaptionStyle(captionStyle, WebVttRenderingWidget.this.mFontSize);
            }
        };
        setLayerType(1, null);
        this.mManager = (CaptioningManager) context.getSystemService(Context.CAPTIONING_SERVICE);
        this.mCaptionStyle = this.mManager.getUserStyle();
        this.mFontSize = this.mManager.getFontScale() * getHeight() * LINE_HEIGHT_RATIO;
    }

    @Override
    public void setSize(int i, int i2) {
        measure(View.MeasureSpec.makeMeasureSpec(i, 1073741824), View.MeasureSpec.makeMeasureSpec(i2, 1073741824));
        layout(0, 0, i, i2);
    }

    @Override
    public void onAttachedToWindow() {
        super.onAttachedToWindow();
        manageChangeListener();
    }

    @Override
    public void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        manageChangeListener();
    }

    @Override
    public void setOnChangedListener(SubtitleTrack.RenderingWidget.OnChangedListener onChangedListener) {
        this.mListener = onChangedListener;
    }

    @Override
    public void setVisible(boolean z) {
        if (z) {
            setVisibility(0);
        } else {
            setVisibility(8);
        }
        manageChangeListener();
    }

    private void manageChangeListener() {
        boolean z = isAttachedToWindow() && getVisibility() == 0;
        if (this.mHasChangeListener != z) {
            this.mHasChangeListener = z;
            if (z) {
                this.mManager.addCaptioningChangeListener(this.mCaptioningListener);
                setCaptionStyle(this.mManager.getUserStyle(), this.mManager.getFontScale() * getHeight() * LINE_HEIGHT_RATIO);
            } else {
                this.mManager.removeCaptioningChangeListener(this.mCaptioningListener);
            }
        }
    }

    public void setActiveCues(Vector<SubtitleTrack.Cue> vector) {
        Context context = getContext();
        CaptioningManager.CaptionStyle captionStyle = this.mCaptionStyle;
        float f = this.mFontSize;
        prepForPrune();
        int size = vector.size();
        for (int i = 0; i < size; i++) {
            TextTrackCue textTrackCue = (TextTrackCue) vector.get(i);
            TextTrackRegion textTrackRegion = textTrackCue.mRegion;
            if (textTrackRegion != null) {
                RegionLayout regionLayout = this.mRegionBoxes.get(textTrackRegion);
                if (regionLayout == null) {
                    regionLayout = new RegionLayout(context, textTrackRegion, captionStyle, f);
                    this.mRegionBoxes.put(textTrackRegion, regionLayout);
                    addView(regionLayout, -2, -2);
                }
                regionLayout.put(textTrackCue);
            } else {
                CueLayout cueLayout = this.mCueBoxes.get(textTrackCue);
                if (cueLayout == null) {
                    cueLayout = new CueLayout(context, textTrackCue, captionStyle, f);
                    this.mCueBoxes.put(textTrackCue, cueLayout);
                    addView(cueLayout, -2, -2);
                }
                cueLayout.update();
                cueLayout.setOrder(i);
            }
        }
        prune();
        setSize(getWidth(), getHeight());
        if (this.mListener != null) {
            this.mListener.onChanged(this);
        }
    }

    private void setCaptionStyle(CaptioningManager.CaptionStyle captionStyle, float f) {
        CaptioningManager.CaptionStyle captionStyleApplyStyle = DEFAULT_CAPTION_STYLE.applyStyle(captionStyle);
        this.mCaptionStyle = captionStyleApplyStyle;
        this.mFontSize = f;
        int size = this.mCueBoxes.size();
        for (int i = 0; i < size; i++) {
            this.mCueBoxes.valueAt(i).setCaptionStyle(captionStyleApplyStyle, f);
        }
        int size2 = this.mRegionBoxes.size();
        for (int i2 = 0; i2 < size2; i2++) {
            this.mRegionBoxes.valueAt(i2).setCaptionStyle(captionStyleApplyStyle, f);
        }
    }

    private void prune() {
        int i = 0;
        int size = this.mRegionBoxes.size();
        int i2 = 0;
        while (i2 < size) {
            RegionLayout regionLayoutValueAt = this.mRegionBoxes.valueAt(i2);
            if (regionLayoutValueAt.prune()) {
                removeView(regionLayoutValueAt);
                this.mRegionBoxes.removeAt(i2);
                size--;
                i2--;
            }
            i2++;
        }
        int size2 = this.mCueBoxes.size();
        while (i < size2) {
            CueLayout cueLayoutValueAt = this.mCueBoxes.valueAt(i);
            if (!cueLayoutValueAt.isActive()) {
                removeView(cueLayoutValueAt);
                this.mCueBoxes.removeAt(i);
                size2--;
                i--;
            }
            i++;
        }
    }

    private void prepForPrune() {
        int size = this.mRegionBoxes.size();
        for (int i = 0; i < size; i++) {
            this.mRegionBoxes.valueAt(i).prepForPrune();
        }
        int size2 = this.mCueBoxes.size();
        for (int i2 = 0; i2 < size2; i2++) {
            this.mCueBoxes.valueAt(i2).prepForPrune();
        }
    }

    @Override
    protected void onMeasure(int i, int i2) {
        super.onMeasure(i, i2);
        int size = this.mRegionBoxes.size();
        for (int i3 = 0; i3 < size; i3++) {
            this.mRegionBoxes.valueAt(i3).measureForParent(i, i2);
        }
        int size2 = this.mCueBoxes.size();
        for (int i4 = 0; i4 < size2; i4++) {
            this.mCueBoxes.valueAt(i4).measureForParent(i, i2);
        }
    }

    @Override
    protected void onLayout(boolean z, int i, int i2, int i3, int i4) {
        int i5 = i3 - i;
        int i6 = i4 - i2;
        setCaptionStyle(this.mCaptionStyle, this.mManager.getFontScale() * LINE_HEIGHT_RATIO * i6);
        int size = this.mRegionBoxes.size();
        for (int i7 = 0; i7 < size; i7++) {
            layoutRegion(i5, i6, this.mRegionBoxes.valueAt(i7));
        }
        int size2 = this.mCueBoxes.size();
        for (int i8 = 0; i8 < size2; i8++) {
            layoutCue(i5, i6, this.mCueBoxes.valueAt(i8));
        }
    }

    private void layoutRegion(int i, int i2, RegionLayout regionLayout) {
        TextTrackRegion region = regionLayout.getRegion();
        int measuredHeight = regionLayout.getMeasuredHeight();
        int measuredWidth = regionLayout.getMeasuredWidth();
        int i3 = (int) ((region.mViewportAnchorPointX * (i - measuredWidth)) / 100.0f);
        int i4 = (int) ((region.mViewportAnchorPointY * (i2 - measuredHeight)) / 100.0f);
        regionLayout.layout(i3, i4, measuredWidth + i3, measuredHeight + i4);
    }

    private void layoutCue(int i, int i2, CueLayout cueLayout) {
        int i3;
        int i4;
        TextTrackCue cue = cueLayout.getCue();
        int layoutDirection = getLayoutDirection();
        int iResolveCueAlignment = resolveCueAlignment(layoutDirection, cue.mAlignment);
        boolean z = cue.mSnapToLines;
        int measuredWidth = (cueLayout.getMeasuredWidth() * 100) / i;
        switch (iResolveCueAlignment) {
            case 203:
                i3 = cue.mTextPosition;
                break;
            case 204:
                i3 = cue.mTextPosition - measuredWidth;
                break;
            default:
                i3 = cue.mTextPosition - (measuredWidth / 2);
                break;
        }
        if (layoutDirection == 1) {
            i3 = 100 - i3;
        }
        if (z) {
            int paddingLeft = (getPaddingLeft() * 100) / i;
            int paddingRight = (getPaddingRight() * 100) / i;
            if (i3 < paddingLeft && i3 + measuredWidth > paddingLeft) {
                i3 += paddingLeft;
                measuredWidth -= paddingLeft;
            }
            float f = 100 - paddingRight;
            if (i3 < f && i3 + measuredWidth > f) {
                measuredWidth -= paddingRight;
            }
        }
        int i5 = (i3 * i) / 100;
        int i6 = (measuredWidth * i) / 100;
        int iCalculateLinePosition = calculateLinePosition(cueLayout);
        int measuredHeight = cueLayout.getMeasuredHeight();
        if (iCalculateLinePosition < 0) {
            i4 = i2 + (iCalculateLinePosition * measuredHeight);
        } else {
            i4 = (iCalculateLinePosition * (i2 - measuredHeight)) / 100;
        }
        cueLayout.layout(i5, i4, i6 + i5, measuredHeight + i4);
    }

    private int calculateLinePosition(CueLayout cueLayout) {
        boolean z;
        TextTrackCue cue = cueLayout.getCue();
        Integer num = cue.mLinePosition;
        boolean z2 = cue.mSnapToLines;
        if (num != null) {
            z = false;
        } else {
            z = true;
        }
        if (!z2 && !z && (num.intValue() < 0 || num.intValue() > 100)) {
            return 100;
        }
        if (!z) {
            return num.intValue();
        }
        if (z2) {
            return -(cueLayout.mOrder + 1);
        }
        return 100;
    }

    private static int resolveCueAlignment(int i, int i2) {
        switch (i2) {
            case 201:
                return i == 0 ? 203 : 204;
            case 202:
                return i == 0 ? 204 : 203;
            default:
                return i2;
        }
    }

    private static class RegionLayout extends LinearLayout {
        private CaptioningManager.CaptionStyle mCaptionStyle;
        private float mFontSize;
        private final TextTrackRegion mRegion;
        private final ArrayList<CueLayout> mRegionCueBoxes;

        public RegionLayout(Context context, TextTrackRegion textTrackRegion, CaptioningManager.CaptionStyle captionStyle, float f) {
            super(context);
            this.mRegionCueBoxes = new ArrayList<>();
            this.mRegion = textTrackRegion;
            this.mCaptionStyle = captionStyle;
            this.mFontSize = f;
            setOrientation(1);
            setBackgroundColor(captionStyle.windowColor);
        }

        public void setCaptionStyle(CaptioningManager.CaptionStyle captionStyle, float f) {
            this.mCaptionStyle = captionStyle;
            this.mFontSize = f;
            int size = this.mRegionCueBoxes.size();
            for (int i = 0; i < size; i++) {
                this.mRegionCueBoxes.get(i).setCaptionStyle(captionStyle, f);
            }
            setBackgroundColor(captionStyle.windowColor);
        }

        public void measureForParent(int i, int i2) {
            TextTrackRegion textTrackRegion = this.mRegion;
            measure(View.MeasureSpec.makeMeasureSpec((((int) textTrackRegion.mWidth) * View.MeasureSpec.getSize(i)) / 100, Integer.MIN_VALUE), View.MeasureSpec.makeMeasureSpec(View.MeasureSpec.getSize(i2), Integer.MIN_VALUE));
        }

        public void prepForPrune() {
            int size = this.mRegionCueBoxes.size();
            for (int i = 0; i < size; i++) {
                this.mRegionCueBoxes.get(i).prepForPrune();
            }
        }

        public void put(TextTrackCue textTrackCue) {
            int size = this.mRegionCueBoxes.size();
            for (int i = 0; i < size; i++) {
                CueLayout cueLayout = this.mRegionCueBoxes.get(i);
                if (cueLayout.getCue() == textTrackCue) {
                    cueLayout.update();
                    return;
                }
            }
            CueLayout cueLayout2 = new CueLayout(getContext(), textTrackCue, this.mCaptionStyle, this.mFontSize);
            this.mRegionCueBoxes.add(cueLayout2);
            addView(cueLayout2, -2, -2);
            if (getChildCount() > this.mRegion.mLines) {
                removeViewAt(0);
            }
        }

        public boolean prune() {
            int size = this.mRegionCueBoxes.size();
            int i = 0;
            while (i < size) {
                CueLayout cueLayout = this.mRegionCueBoxes.get(i);
                if (!cueLayout.isActive()) {
                    this.mRegionCueBoxes.remove(i);
                    removeView(cueLayout);
                    size--;
                    i--;
                }
                i++;
            }
            return this.mRegionCueBoxes.isEmpty();
        }

        public TextTrackRegion getRegion() {
            return this.mRegion;
        }
    }

    private static class CueLayout extends LinearLayout {
        private boolean mActive;
        private CaptioningManager.CaptionStyle mCaptionStyle;
        public final TextTrackCue mCue;
        private float mFontSize;
        private int mOrder;

        public CueLayout(Context context, TextTrackCue textTrackCue, CaptioningManager.CaptionStyle captionStyle, float f) {
            int i;
            super(context);
            this.mCue = textTrackCue;
            this.mCaptionStyle = captionStyle;
            this.mFontSize = f;
            if (textTrackCue.mWritingDirection != 100) {
                i = 0;
            } else {
                i = 1;
            }
            setOrientation(i);
            switch (textTrackCue.mAlignment) {
                case 200:
                    setGravity(i == 0 ? 16 : 1);
                    break;
                case 201:
                    setGravity(Gravity.START);
                    break;
                case 202:
                    setGravity(Gravity.END);
                    break;
                case 203:
                    setGravity(3);
                    break;
                case 204:
                    setGravity(5);
                    break;
            }
            update();
        }

        public void setCaptionStyle(CaptioningManager.CaptionStyle captionStyle, float f) {
            this.mCaptionStyle = captionStyle;
            this.mFontSize = f;
            int childCount = getChildCount();
            for (int i = 0; i < childCount; i++) {
                View childAt = getChildAt(i);
                if (childAt instanceof SpanLayout) {
                    ((SpanLayout) childAt).setCaptionStyle(captionStyle, f);
                }
            }
        }

        public void prepForPrune() {
            this.mActive = false;
        }

        public void update() {
            Layout.Alignment alignment;
            this.mActive = true;
            removeAllViews();
            switch (WebVttRenderingWidget.resolveCueAlignment(getLayoutDirection(), this.mCue.mAlignment)) {
                case 203:
                    alignment = Layout.Alignment.ALIGN_LEFT;
                    break;
                case 204:
                    alignment = Layout.Alignment.ALIGN_RIGHT;
                    break;
                default:
                    alignment = Layout.Alignment.ALIGN_CENTER;
                    break;
            }
            CaptioningManager.CaptionStyle captionStyle = this.mCaptionStyle;
            float f = this.mFontSize;
            for (TextTrackCueSpan[] textTrackCueSpanArr : this.mCue.mLines) {
                SpanLayout spanLayout = new SpanLayout(getContext(), textTrackCueSpanArr);
                spanLayout.setAlignment(alignment);
                spanLayout.setCaptionStyle(captionStyle, f);
                addView(spanLayout, -2, -2);
            }
        }

        @Override
        protected void onMeasure(int i, int i2) {
            super.onMeasure(i, i2);
        }

        public void measureForParent(int i, int i2) {
            int i3;
            TextTrackCue textTrackCue = this.mCue;
            int size = View.MeasureSpec.getSize(i);
            int size2 = View.MeasureSpec.getSize(i2);
            int iResolveCueAlignment = WebVttRenderingWidget.resolveCueAlignment(getLayoutDirection(), textTrackCue.mAlignment);
            if (iResolveCueAlignment != 200) {
                switch (iResolveCueAlignment) {
                    case 203:
                        i3 = 100 - textTrackCue.mTextPosition;
                        break;
                    case 204:
                        i3 = textTrackCue.mTextPosition;
                        break;
                    default:
                        i3 = 0;
                        break;
                }
            } else if (textTrackCue.mTextPosition <= 50) {
                i3 = textTrackCue.mTextPosition * 2;
            } else {
                i3 = (100 - textTrackCue.mTextPosition) * 2;
            }
            measure(View.MeasureSpec.makeMeasureSpec((Math.min(textTrackCue.mSize, i3) * size) / 100, Integer.MIN_VALUE), View.MeasureSpec.makeMeasureSpec(size2, Integer.MIN_VALUE));
        }

        public void setOrder(int i) {
            this.mOrder = i;
        }

        public boolean isActive() {
            return this.mActive;
        }

        public TextTrackCue getCue() {
            return this.mCue;
        }
    }

    private static class SpanLayout extends SubtitleView {
        private final SpannableStringBuilder mBuilder;
        private final TextTrackCueSpan[] mSpans;

        public SpanLayout(Context context, TextTrackCueSpan[] textTrackCueSpanArr) {
            super(context);
            this.mBuilder = new SpannableStringBuilder();
            this.mSpans = textTrackCueSpanArr;
            update();
        }

        public void update() {
            SpannableStringBuilder spannableStringBuilder = this.mBuilder;
            TextTrackCueSpan[] textTrackCueSpanArr = this.mSpans;
            spannableStringBuilder.clear();
            spannableStringBuilder.clearSpans();
            int length = textTrackCueSpanArr.length;
            for (int i = 0; i < length; i++) {
                if (textTrackCueSpanArr[i].mEnabled) {
                    spannableStringBuilder.append((CharSequence) textTrackCueSpanArr[i].mText);
                }
            }
            setText(spannableStringBuilder);
        }

        public void setCaptionStyle(CaptioningManager.CaptionStyle captionStyle, float f) {
            setBackgroundColor(captionStyle.backgroundColor);
            setForegroundColor(captionStyle.foregroundColor);
            setEdgeColor(captionStyle.edgeColor);
            setEdgeType(captionStyle.edgeType);
            setTypeface(captionStyle.getTypeface());
            setTextSize(f);
        }
    }
}
