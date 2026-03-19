package com.mediatek.camera.ui.shutter;

import android.graphics.drawable.Drawable;
import android.os.Build;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.RelativeLayout;
import com.mediatek.camera.R;
import com.mediatek.camera.common.IAppUiListener;
import com.mediatek.camera.common.app.IApp;
import com.mediatek.camera.common.debug.LogHelper;
import com.mediatek.camera.common.debug.LogUtil;
import com.mediatek.camera.common.utils.PriorityConcurrentSkipListMap;
import com.mediatek.camera.ui.AbstractViewManager;
import com.mediatek.camera.ui.shutter.ShutterButton;
import com.mediatek.camera.ui.shutter.ShutterRootLayout;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.ConcurrentSkipListMap;
import java.util.concurrent.atomic.AtomicInteger;

public class ShutterButtonManager extends AbstractViewManager implements ShutterRootLayout.OnShutterChangeListener {
    private static final LogUtil.Tag TAG = new LogUtil.Tag(ShutterButtonManager.class.getSimpleName());
    private static final AtomicInteger sNextGenerateId = new AtomicInteger(1);
    private LayoutInflater mInflater;
    private OnShutterChangeListener mListener;
    private boolean mShutterButton;
    private ShutterButton.OnShutterButtonListener mShutterButtonListener;
    private PriorityConcurrentSkipListMap<String, IAppUiListener.OnShutterButtonListener> mShutterButtonListeners;
    private ConcurrentSkipListMap<Integer, ShutterItem> mShutterButtons;
    private ShutterRootLayout mShutterLayout;

    public interface OnShutterChangeListener {
        void onShutterTypeChanged(String str);
    }

    private static class ShutterItem {
        public Drawable mShutterDrawable;
        public String mShutterName;
        public String mShutterType;
        public ShutterView mShutterView;

        private ShutterItem() {
        }
    }

    public ShutterButtonManager(IApp iApp, ViewGroup viewGroup) {
        super(iApp, viewGroup);
        this.mShutterButtonListeners = new PriorityConcurrentSkipListMap<>(true);
        this.mShutterButtons = new ConcurrentSkipListMap<>();
        this.mShutterButton = false;
        this.mShutterButtonListener = new ShutterButtonListenerImpl();
        this.mInflater = (LayoutInflater) iApp.getActivity().getSystemService("layout_inflater");
    }

    @Override
    protected View getView() {
        this.mShutterLayout = (ShutterRootLayout) this.mApp.getActivity().findViewById(R.id.shutter_root);
        this.mShutterLayout.setOnShutterChangedListener(this);
        this.mApp.getAppUi().registerGestureListener(this.mShutterLayout.getGestureListener(), 20);
        this.mApp.registerKeyEventListener(this.mShutterLayout.getKeyEventListener(), Integer.MAX_VALUE);
        return this.mShutterLayout;
    }

    public void setOnShutterChangedListener(OnShutterChangeListener onShutterChangeListener) {
        this.mListener = onShutterChangeListener;
    }

    @Override
    public void onShutterChangedStart(String str) {
        if (this.mListener != null) {
            this.mListener.onShutterTypeChanged(str);
        }
    }

    @Override
    public void onShutterChangedEnd() {
    }

    @Override
    public void setEnabled(boolean z) {
        if (this.mShutterLayout != null) {
            this.mShutterLayout.setEnabled(z);
            int childCount = this.mShutterLayout.getChildCount();
            for (int i = 0; i < childCount; i++) {
                this.mShutterLayout.getChildAt(i).setEnabled(z);
            }
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        if (this.mShutterLayout != null) {
            this.mShutterLayout.onResume();
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        if (this.mShutterLayout != null) {
            this.mShutterLayout.onPause();
        }
    }

    public void setTextEnabled(boolean z) {
        if (this.mShutterLayout != null) {
            int childCount = this.mShutterLayout.getChildCount();
            for (int i = 0; i < childCount; i++) {
                ((ShutterView) this.mShutterLayout.getChildAt(i)).setTextEnabled(z);
            }
        }
    }

    public void registerOnShutterButtonListener(IAppUiListener.OnShutterButtonListener onShutterButtonListener, int i) {
        if (onShutterButtonListener == null) {
            LogHelper.e(TAG, "registerOnShutterButtonListener error [why null]");
        }
        PriorityConcurrentSkipListMap<String, IAppUiListener.OnShutterButtonListener> priorityConcurrentSkipListMap = this.mShutterButtonListeners;
        PriorityConcurrentSkipListMap<String, IAppUiListener.OnShutterButtonListener> priorityConcurrentSkipListMap2 = this.mShutterButtonListeners;
        priorityConcurrentSkipListMap.put(PriorityConcurrentSkipListMap.getPriorityKey(i, onShutterButtonListener), onShutterButtonListener);
    }

    public void unregisterOnShutterButtonListener(IAppUiListener.OnShutterButtonListener onShutterButtonListener) {
        if (onShutterButtonListener == null) {
            LogHelper.e(TAG, "unregisterOnShutterButtonListener error [why null]");
        }
        if (this.mShutterButtonListeners.containsValue(onShutterButtonListener)) {
            this.mShutterButtonListeners.remove(this.mShutterButtonListeners.findKey(onShutterButtonListener));
        }
    }

    public void registerShutterButton(Drawable drawable, String str, int i) {
        if (this.mShutterButtons.containsKey(Integer.valueOf(i))) {
            return;
        }
        ShutterItem shutterItem = new ShutterItem();
        shutterItem.mShutterDrawable = drawable;
        shutterItem.mShutterType = str;
        if ("Picture".equals(str)) {
            shutterItem.mShutterName = (String) this.mApp.getActivity().getResources().getText(R.string.shutter_type_photo);
        } else if ("Video".equals(str)) {
            shutterItem.mShutterName = (String) this.mApp.getActivity().getResources().getText(R.string.shutter_type_video);
        }
        this.mShutterButtons.put(Integer.valueOf(i), shutterItem);
    }

    public void registerDone() {
        if (this.mShutterLayout.getChildCount() != 0) {
            return;
        }
        this.mShutterLayout.removeAllViews();
        Iterator<Integer> it = this.mShutterButtons.keySet().iterator();
        ShutterItem shutterItem = null;
        int i = 0;
        while (it.hasNext()) {
            ShutterItem shutterItem2 = this.mShutterButtons.get(it.next());
            ShutterView shutterView = (ShutterView) this.mInflater.inflate(R.layout.shutter_item, (ViewGroup) this.mShutterLayout, false);
            shutterView.setType(shutterItem2.mShutterType);
            shutterView.setName(shutterItem2.mShutterName);
            shutterView.setDrawable(shutterItem2.mShutterDrawable);
            shutterView.setId(generateViewId());
            shutterView.setOnShutterTextClickedListener(this.mShutterLayout);
            shutterView.setTag(Integer.valueOf(i));
            this.mShutterLayout.addView(shutterView);
            shutterView.setOnShutterButtonListener(this.mShutterButtonListener);
            shutterItem2.mShutterView = shutterView;
            RelativeLayout.LayoutParams layoutParams = (RelativeLayout.LayoutParams) shutterView.getLayoutParams();
            if (shutterItem == null) {
                layoutParams.addRule(13);
            } else {
                layoutParams.addRule(1, shutterItem.mShutterView.getId());
            }
            i++;
            shutterItem = shutterItem2;
        }
    }

    public void triggerShutterButtonClicked(int i) {
        if (isEnabled()) {
            for (Map.Entry<String, IAppUiListener.OnShutterButtonListener> entry : this.mShutterButtonListeners.entrySet()) {
                IAppUiListener.OnShutterButtonListener value = entry.getValue();
                PriorityConcurrentSkipListMap<String, IAppUiListener.OnShutterButtonListener> priorityConcurrentSkipListMap = this.mShutterButtonListeners;
                if (PriorityConcurrentSkipListMap.getPriorityByKey(entry.getKey()) > i && !this.mShutterButton && value != null && value.onShutterButtonClick()) {
                    return;
                }
            }
        }
    }

    public void updateModeSupportType(String str, String[] strArr) {
        String str2;
        for (int i = 0; i < this.mShutterLayout.getChildCount(); i++) {
            ShutterView shutterView = (ShutterView) this.mShutterLayout.getChildAt(i);
            boolean z = false;
            for (String str3 : strArr) {
                if (str3.equals(shutterView.getType())) {
                    z = true;
                }
            }
            if (z) {
                shutterView.setVisibility(0);
            } else {
                shutterView.setVisibility(4);
            }
        }
        if (strArr.length == 1) {
            str2 = strArr[0];
        } else {
            str2 = str;
        }
        LogHelper.d(TAG, "currentType = " + str + " targetType = " + str2);
        for (int i2 = 0; i2 < this.mShutterLayout.getChildCount(); i2++) {
            if (str2.equals(((ShutterView) this.mShutterLayout.getChildAt(i2)).getType())) {
                this.mShutterLayout.updateCurrentShutterIndex(i2);
            }
        }
    }

    public void updateCurrentModeShutter(String str, Drawable drawable) {
        int i = 0;
        if (drawable != null) {
            while (i < this.mShutterLayout.getChildCount()) {
                ShutterView shutterView = (ShutterView) this.mShutterLayout.getChildAt(i);
                if (shutterView.getType().equals(str)) {
                    shutterView.setDrawable(drawable);
                }
                i++;
            }
            return;
        }
        while (i < this.mShutterLayout.getChildCount()) {
            ShutterView shutterView2 = (ShutterView) this.mShutterLayout.getChildAt(i);
            Iterator<Integer> it = this.mShutterButtons.keySet().iterator();
            while (it.hasNext()) {
                ShutterItem shutterItem = this.mShutterButtons.get(it.next());
                if (shutterView2.getType().equals(shutterItem.mShutterType)) {
                    shutterView2.setDrawable(shutterItem.mShutterDrawable);
                }
            }
            i++;
        }
    }

    public View getShutterRootView() {
        return this.mShutterLayout;
    }

    private class ShutterButtonListenerImpl implements ShutterButton.OnShutterButtonListener {
        private ShutterButtonListenerImpl() {
        }

        @Override
        public void onShutterButtonFocused(boolean z) {
            Iterator it = ShutterButtonManager.this.mShutterButtonListeners.entrySet().iterator();
            ShutterButtonManager.this.mShutterButton = z;
            while (it.hasNext()) {
                IAppUiListener.OnShutterButtonListener onShutterButtonListener = (IAppUiListener.OnShutterButtonListener) ((Map.Entry) it.next()).getValue();
                if (onShutterButtonListener != null && onShutterButtonListener.onShutterButtonFocus(z)) {
                    return;
                }
            }
        }

        @Override
        public void onShutterButtonClicked() {
            Iterator it = ShutterButtonManager.this.mShutterButtonListeners.entrySet().iterator();
            while (it.hasNext()) {
                IAppUiListener.OnShutterButtonListener onShutterButtonListener = (IAppUiListener.OnShutterButtonListener) ((Map.Entry) it.next()).getValue();
                if (onShutterButtonListener != null && onShutterButtonListener.onShutterButtonClick()) {
                    return;
                }
            }
        }

        @Override
        public void onShutterButtonLongPressed() {
            Iterator it = ShutterButtonManager.this.mShutterButtonListeners.entrySet().iterator();
            while (it.hasNext()) {
                IAppUiListener.OnShutterButtonListener onShutterButtonListener = (IAppUiListener.OnShutterButtonListener) ((Map.Entry) it.next()).getValue();
                if (onShutterButtonListener != null && onShutterButtonListener.onShutterButtonLongPressed()) {
                    return;
                }
            }
        }
    }

    private static int generateViewId() {
        int i;
        int i2;
        if (Build.VERSION.SDK_INT < 17) {
            do {
                i = sNextGenerateId.get();
                i2 = i + 1;
                if (i2 > 16777215) {
                    i2 = 1;
                }
            } while (!sNextGenerateId.compareAndSet(i, i2));
            return i;
        }
        return View.generateViewId();
    }
}
