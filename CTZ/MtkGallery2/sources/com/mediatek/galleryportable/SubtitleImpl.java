package com.mediatek.galleryportable;

import android.content.Context;
import android.graphics.Canvas;
import android.media.Cea708CaptionRenderer;
import android.media.ClosedCaptionRenderer;
import android.media.MediaPlayer;
import android.media.SubtitleController;
import android.media.SubtitleTrack;
import android.media.TtmlRenderer;
import android.media.WebVttRenderer;
import android.os.Looper;
import android.os.SystemClock;

public class SubtitleImpl {
    private static final String[] CLASS_NAMES = {"android.media.Cea708CaptionRenderer", "android.media.ClosedCaptionRenderer", "android.media.SubtitleController", "android.media.SubtitleTrack", "android.media.TtmlRenderer", "android.media.WebVttRenderer"};
    private boolean mIsSubtitleSupported;
    private SubtitleTrack.RenderingWidget mSubtitleWidget;
    private SubtitleTrack.RenderingWidget.OnChangedListener mSubtitlesChangedListener;
    private MovieViewAdapter mViewAdapter;

    public SubtitleImpl(MovieViewAdapter MovieViewAdapter) {
        this.mViewAdapter = MovieViewAdapter;
        checkSubtitleSupport();
    }

    private void checkSubtitleSupport() {
        SystemClock.elapsedRealtime();
        String[] strArr = CLASS_NAMES;
        int length = strArr.length;
        int i = 0;
        while (true) {
            if (i >= length) {
                break;
            }
            String className = strArr[i];
            try {
                Class<?> clazz = SubtitleImpl.class.getClassLoader().loadClass(className);
                this.mIsSubtitleSupported = clazz != null;
                if (!this.mIsSubtitleSupported) {
                    break;
                } else {
                    i++;
                }
            } catch (ClassNotFoundException e) {
                this.mIsSubtitleSupported = false;
                Log.d("VP_SubtitleImpl", className + " not support, catch ClassNotFoundException");
            }
        }
        Log.d("VP_SubtitleImpl", "checkSubtitleSupport, mIsSubtitleSupported = " + this.mIsSubtitleSupported);
    }

    public void onLayout() {
        if (this.mIsSubtitleSupported && this.mSubtitleWidget != null) {
            measureAndLayoutSubtitleWidget();
        }
    }

    public void draw(Canvas canvas) {
        if (this.mIsSubtitleSupported && this.mSubtitleWidget != null) {
            int saveCount = canvas.save();
            canvas.translate(this.mViewAdapter.getPaddingLeft(), this.mViewAdapter.getPaddingTop());
            this.mSubtitleWidget.draw(canvas);
            canvas.restoreToCount(saveCount);
        }
    }

    public void onAttachedToWindow() {
        if (this.mIsSubtitleSupported && this.mSubtitleWidget != null) {
            this.mSubtitleWidget.onAttachedToWindow();
        }
    }

    public void onDetachedFromWindow() {
        if (this.mIsSubtitleSupported && this.mSubtitleWidget != null) {
            this.mSubtitleWidget.onDetachedFromWindow();
        }
    }

    private void measureAndLayoutSubtitleWidget() {
        if (this.mIsSubtitleSupported && this.mSubtitleWidget != null) {
            int width = (this.mViewAdapter.getWidth() - this.mViewAdapter.getPaddingLeft()) - this.mViewAdapter.getPaddingRight();
            int height = (this.mViewAdapter.getHeight() - this.mViewAdapter.getPaddingTop()) - this.mViewAdapter.getPaddingBottom();
            this.mSubtitleWidget.setSize(width, height);
        }
    }

    public class SubtitleAnchor implements SubtitleController.Anchor {
        public SubtitleAnchor() {
        }

        public void setSubtitleWidget(SubtitleTrack.RenderingWidget subtitleWidget) {
            if (SubtitleImpl.this.mIsSubtitleSupported && SubtitleImpl.this.mSubtitleWidget != subtitleWidget) {
                boolean attachedToWindow = SubtitleImpl.this.mViewAdapter.isAttachedToWindow();
                if (SubtitleImpl.this.mSubtitleWidget != null) {
                    if (attachedToWindow) {
                        SubtitleImpl.this.mSubtitleWidget.onDetachedFromWindow();
                    }
                    SubtitleImpl.this.mSubtitleWidget.setOnChangedListener((SubtitleTrack.RenderingWidget.OnChangedListener) null);
                }
                SubtitleImpl.this.mSubtitleWidget = subtitleWidget;
                if (subtitleWidget != null) {
                    if (SubtitleImpl.this.mSubtitlesChangedListener == null) {
                        SubtitleImpl.this.mSubtitlesChangedListener = new SubtitleTrack.RenderingWidget.OnChangedListener() {
                            public void onChanged(SubtitleTrack.RenderingWidget renderingWidget) {
                                SubtitleImpl.this.mViewAdapter.invalidate();
                            }
                        };
                    }
                    SubtitleImpl.this.mViewAdapter.setWillNotDraw(false);
                    subtitleWidget.setOnChangedListener(SubtitleImpl.this.mSubtitlesChangedListener);
                    if (attachedToWindow) {
                        subtitleWidget.onAttachedToWindow();
                        SubtitleImpl.this.mViewAdapter.requestLayout();
                    }
                } else {
                    SubtitleImpl.this.mViewAdapter.setWillNotDraw(true);
                }
                SubtitleImpl.this.mViewAdapter.invalidate();
            }
        }

        public Looper getSubtitleLooper() {
            return Looper.getMainLooper();
        }
    }

    public void register(Context context, MediaPlayer mp) {
        if (!this.mIsSubtitleSupported) {
            return;
        }
        SubtitleController controller = new SubtitleController(context, mp.getMediaTimeProvider(), mp);
        controller.registerRenderer(new WebVttRenderer(context));
        controller.registerRenderer(new TtmlRenderer(context));
        controller.registerRenderer(new Cea708CaptionRenderer(context));
        controller.registerRenderer(new ClosedCaptionRenderer(context));
        mp.setSubtitleAnchor(controller, new SubtitleAnchor());
    }
}
