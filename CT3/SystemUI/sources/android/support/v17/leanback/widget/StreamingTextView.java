package android.support.v17.leanback.widget;

import android.animation.ObjectAnimator;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.os.Build;
import android.support.v17.leanback.R$drawable;
import android.text.SpannableStringBuilder;
import android.text.SpannedString;
import android.text.style.ReplacementSpan;
import android.util.AttributeSet;
import android.util.Property;
import android.view.View;
import android.view.accessibility.AccessibilityNodeInfo;
import android.widget.EditText;
import java.util.Random;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

class StreamingTextView extends EditText {
    private static final Pattern SPLIT_PATTERN = Pattern.compile("\\S+");
    private static final Property<StreamingTextView, Integer> STREAM_POSITION_PROPERTY = new Property<StreamingTextView, Integer>(Integer.class, "streamPosition") {
        @Override
        public Integer get(StreamingTextView view) {
            return Integer.valueOf(view.getStreamPosition());
        }

        @Override
        public void set(StreamingTextView view, Integer value) {
            view.setStreamPosition(value.intValue());
        }
    };
    private Bitmap mOneDot;
    private final Random mRandom;
    private int mStreamPosition;
    private ObjectAnimator mStreamingAnimation;
    private Bitmap mTwoDot;

    public StreamingTextView(Context context, AttributeSet attrs) {
        super(context, attrs);
        this.mRandom = new Random();
    }

    public StreamingTextView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        this.mRandom = new Random();
    }

    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();
        this.mOneDot = getScaledBitmap(R$drawable.lb_text_dot_one, 1.3f);
        this.mTwoDot = getScaledBitmap(R$drawable.lb_text_dot_two, 1.3f);
        reset();
    }

    private Bitmap getScaledBitmap(int resourceId, float scaled) {
        Bitmap bitmap = BitmapFactory.decodeResource(getResources(), resourceId);
        return Bitmap.createScaledBitmap(bitmap, (int) (bitmap.getWidth() * scaled), (int) (bitmap.getHeight() * scaled), false);
    }

    public void reset() {
        this.mStreamPosition = -1;
        cancelStreamAnimation();
        setText("");
    }

    public void updateRecognizedText(String stableText, String pendingText) {
        if (stableText == null) {
            stableText = "";
        }
        SpannableStringBuilder displayText = new SpannableStringBuilder(stableText);
        if (pendingText != null) {
            int pendingTextStart = displayText.length();
            displayText.append((CharSequence) pendingText);
            addDottySpans(displayText, pendingText, pendingTextStart);
        }
        this.mStreamPosition = Math.max(stableText.length(), this.mStreamPosition);
        updateText(new SpannedString(displayText));
        startStreamAnimation();
    }

    public int getStreamPosition() {
        return this.mStreamPosition;
    }

    public void setStreamPosition(int streamPosition) {
        this.mStreamPosition = streamPosition;
        invalidate();
    }

    private void startStreamAnimation() {
        cancelStreamAnimation();
        int pos = getStreamPosition();
        int totalLen = length();
        int animLen = totalLen - pos;
        if (animLen <= 0) {
            return;
        }
        if (this.mStreamingAnimation == null) {
            this.mStreamingAnimation = new ObjectAnimator();
            this.mStreamingAnimation.setTarget(this);
            this.mStreamingAnimation.setProperty(STREAM_POSITION_PROPERTY);
        }
        this.mStreamingAnimation.setIntValues(pos, totalLen);
        this.mStreamingAnimation.setDuration(((long) animLen) * 50);
        this.mStreamingAnimation.start();
    }

    private void cancelStreamAnimation() {
        if (this.mStreamingAnimation == null) {
            return;
        }
        this.mStreamingAnimation.cancel();
    }

    private void addDottySpans(SpannableStringBuilder displayText, String text, int textStart) {
        Matcher m = SPLIT_PATTERN.matcher(text);
        while (m.find()) {
            int wordStart = textStart + m.start();
            int wordEnd = textStart + m.end();
            DottySpan span = new DottySpan(text.charAt(m.start()), wordStart);
            displayText.setSpan(span, wordStart, wordEnd, 33);
        }
    }

    private void updateText(CharSequence displayText) {
        setText(displayText);
        bringPointIntoView(length());
    }

    @Override
    public void onInitializeAccessibilityNodeInfo(AccessibilityNodeInfo info) {
        super.onInitializeAccessibilityNodeInfo(info);
        info.setClassName(StreamingTextView.class.getCanonicalName());
    }

    private class DottySpan extends ReplacementSpan {
        private final int mPosition;
        private final int mSeed;

        public DottySpan(int seed, int pos) {
            this.mSeed = seed;
            this.mPosition = pos;
        }

        @Override
        public void draw(Canvas canvas, CharSequence text, int start, int end, float x, int top, int y, int bottom, Paint paint) {
            int width = (int) paint.measureText(text, start, end);
            int dotWidth = StreamingTextView.this.mOneDot.getWidth();
            int sliceWidth = dotWidth * 2;
            int sliceCount = width / sliceWidth;
            int excess = width % sliceWidth;
            int prop = excess / 2;
            boolean rtl = StreamingTextView.isLayoutRtl(StreamingTextView.this);
            StreamingTextView.this.mRandom.setSeed(this.mSeed);
            int oldAlpha = paint.getAlpha();
            for (int i = 0; i < sliceCount && this.mPosition + i < StreamingTextView.this.mStreamPosition; i++) {
                float left = (i * sliceWidth) + prop + (dotWidth / 2);
                float dotLeft = rtl ? ((width + x) - left) - dotWidth : x + left;
                paint.setAlpha((StreamingTextView.this.mRandom.nextInt(4) + 1) * 63);
                if (StreamingTextView.this.mRandom.nextBoolean()) {
                    canvas.drawBitmap(StreamingTextView.this.mTwoDot, dotLeft, y - StreamingTextView.this.mTwoDot.getHeight(), paint);
                } else {
                    canvas.drawBitmap(StreamingTextView.this.mOneDot, dotLeft, y - StreamingTextView.this.mOneDot.getHeight(), paint);
                }
            }
            paint.setAlpha(oldAlpha);
        }

        @Override
        public int getSize(Paint paint, CharSequence text, int start, int end, Paint.FontMetricsInt fontMetricsInt) {
            return (int) paint.measureText(text, start, end);
        }
    }

    public static boolean isLayoutRtl(View view) {
        return Build.VERSION.SDK_INT >= 17 && 1 == view.getLayoutDirection();
    }
}
