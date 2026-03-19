package android.media;

import android.content.Context;
import android.media.SubtitleTrack;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.view.accessibility.CaptioningManager;
import java.util.Iterator;
import java.util.Locale;
import java.util.Vector;

public class SubtitleController {
    static final boolean $assertionsDisabled = false;
    private static final int WHAT_HIDE = 2;
    private static final int WHAT_SELECT_DEFAULT_TRACK = 4;
    private static final int WHAT_SELECT_TRACK = 3;
    private static final int WHAT_SHOW = 1;
    private Anchor mAnchor;
    private CaptioningManager mCaptioningManager;
    private Handler mHandler;
    private Listener mListener;
    private SubtitleTrack mSelectedTrack;
    private MediaTimeProvider mTimeProvider;
    private final Handler.Callback mCallback = new Handler.Callback() {
        @Override
        public boolean handleMessage(Message message) {
            switch (message.what) {
                case 1:
                    SubtitleController.this.doShow();
                    break;
                case 2:
                    SubtitleController.this.doHide();
                    break;
                case 3:
                    SubtitleController.this.doSelectTrack((SubtitleTrack) message.obj);
                    break;
                case 4:
                    SubtitleController.this.doSelectDefaultTrack();
                    break;
            }
            return true;
        }
    };
    private CaptioningManager.CaptioningChangeListener mCaptioningChangeListener = new CaptioningManager.CaptioningChangeListener() {
        @Override
        public void onEnabledChanged(boolean z) {
            SubtitleController.this.selectDefaultTrack();
        }

        @Override
        public void onLocaleChanged(Locale locale) {
            SubtitleController.this.selectDefaultTrack();
        }
    };
    private boolean mTrackIsExplicit = false;
    private boolean mVisibilityIsExplicit = false;
    private Vector<Renderer> mRenderers = new Vector<>();
    private boolean mShowing = false;
    private Vector<SubtitleTrack> mTracks = new Vector<>();

    public interface Anchor {
        Looper getSubtitleLooper();

        void setSubtitleWidget(SubtitleTrack.RenderingWidget renderingWidget);
    }

    public interface Listener {
        void onSubtitleTrackSelected(SubtitleTrack subtitleTrack);
    }

    public static abstract class Renderer {
        public abstract SubtitleTrack createTrack(MediaFormat mediaFormat);

        public abstract boolean supports(MediaFormat mediaFormat);
    }

    public SubtitleController(Context context, MediaTimeProvider mediaTimeProvider, Listener listener) {
        this.mTimeProvider = mediaTimeProvider;
        this.mListener = listener;
        this.mCaptioningManager = (CaptioningManager) context.getSystemService(Context.CAPTIONING_SERVICE);
    }

    protected void finalize() throws Throwable {
        this.mCaptioningManager.removeCaptioningChangeListener(this.mCaptioningChangeListener);
        super.finalize();
    }

    public SubtitleTrack[] getTracks() {
        SubtitleTrack[] subtitleTrackArr;
        synchronized (this.mTracks) {
            subtitleTrackArr = new SubtitleTrack[this.mTracks.size()];
            this.mTracks.toArray(subtitleTrackArr);
        }
        return subtitleTrackArr;
    }

    public SubtitleTrack getSelectedTrack() {
        return this.mSelectedTrack;
    }

    private SubtitleTrack.RenderingWidget getRenderingWidget() {
        if (this.mSelectedTrack == null) {
            return null;
        }
        return this.mSelectedTrack.getRenderingWidget();
    }

    public boolean selectTrack(SubtitleTrack subtitleTrack) {
        if (subtitleTrack != null && !this.mTracks.contains(subtitleTrack)) {
            return false;
        }
        processOnAnchor(this.mHandler.obtainMessage(3, subtitleTrack));
        return true;
    }

    private void doSelectTrack(SubtitleTrack subtitleTrack) {
        this.mTrackIsExplicit = true;
        if (this.mSelectedTrack == subtitleTrack) {
            return;
        }
        if (this.mSelectedTrack != null) {
            this.mSelectedTrack.hide();
            this.mSelectedTrack.setTimeProvider(null);
        }
        this.mSelectedTrack = subtitleTrack;
        if (this.mAnchor != null) {
            this.mAnchor.setSubtitleWidget(getRenderingWidget());
        }
        if (this.mSelectedTrack != null) {
            this.mSelectedTrack.setTimeProvider(this.mTimeProvider);
            this.mSelectedTrack.show();
        }
        if (this.mListener != null) {
            this.mListener.onSubtitleTrackSelected(subtitleTrack);
        }
    }

    public SubtitleTrack getDefaultTrack() {
        Locale locale;
        SubtitleTrack subtitleTrack;
        int i;
        Locale locale2 = this.mCaptioningManager.getLocale();
        if (locale2 == null) {
            locale = Locale.getDefault();
        } else {
            locale = locale2;
        }
        boolean z = !this.mCaptioningManager.isEnabled();
        synchronized (this.mTracks) {
            subtitleTrack = null;
            int i2 = -1;
            for (SubtitleTrack subtitleTrack2 : this.mTracks) {
                MediaFormat format = subtitleTrack2.getFormat();
                String string = format.getString("language");
                int i3 = 0;
                boolean z2 = format.getInteger(MediaFormat.KEY_IS_FORCED_SUBTITLE, 0) != 0;
                boolean z3 = format.getInteger(MediaFormat.KEY_IS_AUTOSELECT, 1) != 0;
                boolean z4 = format.getInteger(MediaFormat.KEY_IS_DEFAULT, 0) != 0;
                int i4 = (locale == null || locale.getLanguage().equals("") || locale.getISO3Language().equals(string) || locale.getLanguage().equals(string)) ? 1 : 0;
                if (!z2) {
                    i = 8;
                } else {
                    i = 0;
                }
                int i5 = i + ((locale2 == null && z4) ? 4 : 0);
                if (!z3) {
                    i3 = 2;
                }
                int i6 = i5 + i3 + i4;
                if (!z || z2) {
                    if (((locale2 == null && z4) || (i4 != 0 && (z3 || z2 || locale2 != null))) && i6 > i2) {
                        subtitleTrack = subtitleTrack2;
                        i2 = i6;
                    }
                }
            }
        }
        return subtitleTrack;
    }

    public void selectDefaultTrack() {
        processOnAnchor(this.mHandler.obtainMessage(4));
    }

    private void doSelectDefaultTrack() {
        if (this.mTrackIsExplicit) {
            if (!this.mVisibilityIsExplicit) {
                if (this.mCaptioningManager.isEnabled() || (this.mSelectedTrack != null && this.mSelectedTrack.getFormat().getInteger(MediaFormat.KEY_IS_FORCED_SUBTITLE, 0) != 0)) {
                    show();
                } else if (this.mSelectedTrack != null && this.mSelectedTrack.getTrackType() == 4) {
                    hide();
                }
                this.mVisibilityIsExplicit = false;
                return;
            }
            return;
        }
        SubtitleTrack defaultTrack = getDefaultTrack();
        if (defaultTrack != null) {
            selectTrack(defaultTrack);
            this.mTrackIsExplicit = false;
            if (!this.mVisibilityIsExplicit) {
                show();
                this.mVisibilityIsExplicit = false;
            }
        }
    }

    public void reset() {
        checkAnchorLooper();
        hide();
        selectTrack(null);
        this.mTracks.clear();
        this.mTrackIsExplicit = false;
        this.mVisibilityIsExplicit = false;
        this.mCaptioningManager.removeCaptioningChangeListener(this.mCaptioningChangeListener);
    }

    public SubtitleTrack addTrack(MediaFormat mediaFormat) {
        SubtitleTrack subtitleTrackCreateTrack;
        synchronized (this.mRenderers) {
            for (Renderer renderer : this.mRenderers) {
                if (renderer.supports(mediaFormat) && (subtitleTrackCreateTrack = renderer.createTrack(mediaFormat)) != null) {
                    synchronized (this.mTracks) {
                        if (this.mTracks.size() == 0) {
                            this.mCaptioningManager.addCaptioningChangeListener(this.mCaptioningChangeListener);
                        }
                        this.mTracks.add(subtitleTrackCreateTrack);
                    }
                    return subtitleTrackCreateTrack;
                }
            }
            return null;
        }
    }

    public void show() {
        processOnAnchor(this.mHandler.obtainMessage(1));
    }

    private void doShow() {
        this.mShowing = true;
        this.mVisibilityIsExplicit = true;
        if (this.mSelectedTrack != null) {
            this.mSelectedTrack.show();
        }
    }

    public void hide() {
        processOnAnchor(this.mHandler.obtainMessage(2));
    }

    private void doHide() {
        this.mVisibilityIsExplicit = true;
        if (this.mSelectedTrack != null) {
            this.mSelectedTrack.hide();
        }
        this.mShowing = false;
    }

    public void registerRenderer(Renderer renderer) {
        synchronized (this.mRenderers) {
            if (!this.mRenderers.contains(renderer)) {
                this.mRenderers.add(renderer);
            }
        }
    }

    public boolean hasRendererFor(MediaFormat mediaFormat) {
        synchronized (this.mRenderers) {
            Iterator<Renderer> it = this.mRenderers.iterator();
            while (it.hasNext()) {
                if (it.next().supports(mediaFormat)) {
                    return true;
                }
            }
            return false;
        }
    }

    public void setAnchor(Anchor anchor) {
        if (this.mAnchor == anchor) {
            return;
        }
        if (this.mAnchor != null) {
            checkAnchorLooper();
            this.mAnchor.setSubtitleWidget(null);
        }
        this.mAnchor = anchor;
        this.mHandler = null;
        if (this.mAnchor != null) {
            this.mHandler = new Handler(this.mAnchor.getSubtitleLooper(), this.mCallback);
            checkAnchorLooper();
            this.mAnchor.setSubtitleWidget(getRenderingWidget());
        }
    }

    private void checkAnchorLooper() {
    }

    private void processOnAnchor(Message message) {
        if (Looper.myLooper() == this.mHandler.getLooper()) {
            this.mHandler.dispatchMessage(message);
        } else {
            this.mHandler.sendMessage(message);
        }
    }
}
