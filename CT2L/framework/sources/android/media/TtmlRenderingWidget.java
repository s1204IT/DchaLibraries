package android.media;

import android.content.Context;
import android.media.SubtitleTrack;
import android.net.ProxyInfo;
import android.util.AttributeSet;
import android.view.View;
import android.view.accessibility.CaptioningManager;
import android.widget.LinearLayout;
import android.widget.TextView;
import java.util.Vector;

class TtmlRenderingWidget extends LinearLayout implements SubtitleTrack.RenderingWidget {
    private SubtitleTrack.RenderingWidget.OnChangedListener mListener;
    private final TextView mTextView;

    public TtmlRenderingWidget(Context context) {
        this(context, null);
    }

    public TtmlRenderingWidget(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public TtmlRenderingWidget(Context context, AttributeSet attrs, int defStyleAttr) {
        this(context, attrs, defStyleAttr, 0);
    }

    public TtmlRenderingWidget(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        setLayerType(1, null);
        CaptioningManager captionManager = (CaptioningManager) context.getSystemService(Context.CAPTIONING_SERVICE);
        this.mTextView = new TextView(context);
        this.mTextView.setTextColor(captionManager.getUserStyle().foregroundColor);
        addView(this.mTextView, -1, -1);
        this.mTextView.setGravity(81);
    }

    @Override
    public void setOnChangedListener(SubtitleTrack.RenderingWidget.OnChangedListener listener) {
        this.mListener = listener;
    }

    @Override
    public void setSize(int width, int height) {
        int widthSpec = View.MeasureSpec.makeMeasureSpec(width, 1073741824);
        int heightSpec = View.MeasureSpec.makeMeasureSpec(height, 1073741824);
        measure(widthSpec, heightSpec);
        layout(0, 0, width, height);
    }

    @Override
    public void setVisible(boolean visible) {
        if (visible) {
            setVisibility(0);
        } else {
            setVisibility(8);
        }
    }

    @Override
    public void onAttachedToWindow() {
        super.onAttachedToWindow();
    }

    @Override
    public void onDetachedFromWindow() {
        super.onDetachedFromWindow();
    }

    public void setActiveCues(Vector<SubtitleTrack.Cue> activeCues) {
        int count = activeCues.size();
        String subtitleText = ProxyInfo.LOCAL_EXCL_LIST;
        for (int i = 0; i < count; i++) {
            TtmlCue cue = (TtmlCue) activeCues.get(i);
            subtitleText = subtitleText + cue.mText + "\n";
        }
        this.mTextView.setText(subtitleText);
        if (this.mListener != null) {
            this.mListener.onChanged(this);
        }
    }
}
