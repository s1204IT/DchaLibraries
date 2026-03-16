package android.content;

import android.content.IClipboard;
import android.content.IOnPrimaryClipChangedListener;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.RemoteException;
import android.os.ServiceManager;
import java.util.ArrayList;

public class ClipboardManager extends android.text.ClipboardManager {
    static final int MSG_REPORT_PRIMARY_CLIP_CHANGED = 1;
    private static IClipboard sService;
    private static final Object sStaticLock = new Object();
    private final Context mContext;
    private final ArrayList<OnPrimaryClipChangedListener> mPrimaryClipChangedListeners = new ArrayList<>();
    private final IOnPrimaryClipChangedListener.Stub mPrimaryClipChangedServiceListener = new IOnPrimaryClipChangedListener.Stub() {
        @Override
        public void dispatchPrimaryClipChanged() {
            ClipboardManager.this.mHandler.sendEmptyMessage(1);
        }
    };
    private final Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case 1:
                    ClipboardManager.this.reportPrimaryClipChanged();
                    break;
            }
        }
    };

    public interface OnPrimaryClipChangedListener {
        void onPrimaryClipChanged();
    }

    private static IClipboard getService() {
        IClipboard iClipboard;
        synchronized (sStaticLock) {
            if (sService != null) {
                iClipboard = sService;
            } else {
                IBinder b = ServiceManager.getService(Context.CLIPBOARD_SERVICE);
                sService = IClipboard.Stub.asInterface(b);
                iClipboard = sService;
            }
        }
        return iClipboard;
    }

    public ClipboardManager(Context context, Handler handler) {
        this.mContext = context;
    }

    public void setPrimaryClip(ClipData clip) {
        if (clip != null) {
            try {
                clip.prepareToLeaveProcess();
            } catch (RemoteException e) {
                return;
            }
        }
        getService().setPrimaryClip(clip, this.mContext.getOpPackageName());
    }

    public ClipData getPrimaryClip() {
        try {
            return getService().getPrimaryClip(this.mContext.getOpPackageName());
        } catch (RemoteException e) {
            return null;
        }
    }

    public ClipDescription getPrimaryClipDescription() {
        try {
            return getService().getPrimaryClipDescription(this.mContext.getOpPackageName());
        } catch (RemoteException e) {
            return null;
        }
    }

    public boolean hasPrimaryClip() {
        try {
            return getService().hasPrimaryClip(this.mContext.getOpPackageName());
        } catch (RemoteException e) {
            return false;
        }
    }

    public void addPrimaryClipChangedListener(OnPrimaryClipChangedListener what) {
        synchronized (this.mPrimaryClipChangedListeners) {
            if (this.mPrimaryClipChangedListeners.size() == 0) {
                try {
                    getService().addPrimaryClipChangedListener(this.mPrimaryClipChangedServiceListener, this.mContext.getOpPackageName());
                } catch (RemoteException e) {
                }
            }
            this.mPrimaryClipChangedListeners.add(what);
        }
    }

    public void removePrimaryClipChangedListener(OnPrimaryClipChangedListener what) {
        synchronized (this.mPrimaryClipChangedListeners) {
            this.mPrimaryClipChangedListeners.remove(what);
            if (this.mPrimaryClipChangedListeners.size() == 0) {
                try {
                    getService().removePrimaryClipChangedListener(this.mPrimaryClipChangedServiceListener);
                } catch (RemoteException e) {
                }
            }
        }
    }

    @Override
    public CharSequence getText() {
        ClipData clip = getPrimaryClip();
        if (clip == null || clip.getItemCount() <= 0) {
            return null;
        }
        return clip.getItemAt(0).coerceToText(this.mContext);
    }

    @Override
    public void setText(CharSequence text) {
        setPrimaryClip(ClipData.newPlainText(null, text));
    }

    @Override
    public boolean hasText() {
        try {
            return getService().hasClipboardText(this.mContext.getOpPackageName());
        } catch (RemoteException e) {
            return false;
        }
    }

    void reportPrimaryClipChanged() {
        synchronized (this.mPrimaryClipChangedListeners) {
            int N = this.mPrimaryClipChangedListeners.size();
            if (N > 0) {
                Object[] listeners = this.mPrimaryClipChangedListeners.toArray();
                for (Object obj : listeners) {
                    ((OnPrimaryClipChangedListener) obj).onPrimaryClipChanged();
                }
            }
        }
    }
}
