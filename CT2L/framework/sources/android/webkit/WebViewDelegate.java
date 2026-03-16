package android.webkit;

import android.app.ActivityThread;
import android.app.Application;
import android.content.Context;
import android.content.res.Resources;
import android.graphics.Canvas;
import android.net.http.ErrorStrings;
import android.os.SystemProperties;
import android.os.Trace;
import android.util.SparseArray;
import android.view.HardwareCanvas;
import android.view.View;
import android.view.ViewRootImpl;

public final class WebViewDelegate {

    public interface OnTraceEnabledChangeListener {
        void onTraceEnabledChange(boolean z);
    }

    WebViewDelegate() {
    }

    public void setOnTraceEnabledChangeListener(final OnTraceEnabledChangeListener listener) {
        SystemProperties.addChangeCallback(new Runnable() {
            @Override
            public void run() {
                listener.onTraceEnabledChange(WebViewDelegate.this.isTraceTagEnabled());
            }
        });
    }

    public boolean isTraceTagEnabled() {
        return Trace.isTagEnabled(16L);
    }

    public boolean canInvokeDrawGlFunctor(View containerView) {
        ViewRootImpl viewRootImpl = containerView.getViewRootImpl();
        return viewRootImpl != null;
    }

    public void invokeDrawGlFunctor(View containerView, long nativeDrawGLFunctor, boolean waitForCompletion) {
        ViewRootImpl viewRootImpl = containerView.getViewRootImpl();
        viewRootImpl.invokeFunctor(nativeDrawGLFunctor, waitForCompletion);
    }

    public void callDrawGlFunction(Canvas canvas, long nativeDrawGLFunctor) {
        if (!(canvas instanceof HardwareCanvas)) {
            throw new IllegalArgumentException(canvas.getClass().getName() + " is not hardware accelerated");
        }
        ((HardwareCanvas) canvas).callDrawGLFunction2(nativeDrawGLFunctor);
    }

    public void detachDrawGlFunctor(View containerView, long nativeDrawGLFunctor) {
        ViewRootImpl viewRootImpl = containerView.getViewRootImpl();
        if (nativeDrawGLFunctor != 0 && viewRootImpl != null) {
            viewRootImpl.detachFunctor(nativeDrawGLFunctor);
        }
    }

    public int getPackageId(Resources resources, String packageName) {
        SparseArray<String> packageIdentifiers = resources.getAssets().getAssignedPackageIdentifiers();
        for (int i = 0; i < packageIdentifiers.size(); i++) {
            String name = packageIdentifiers.valueAt(i);
            if (packageName.equals(name)) {
                return packageIdentifiers.keyAt(i);
            }
        }
        throw new RuntimeException("Package not found: " + packageName);
    }

    public Application getApplication() {
        return ActivityThread.currentApplication();
    }

    public String getErrorString(Context context, int errorCode) {
        return ErrorStrings.getString(errorCode, context);
    }

    public void addWebViewAssetPath(Context context) {
        context.getAssets().addAssetPath(WebViewFactory.getLoadedPackageInfo().applicationInfo.sourceDir);
    }
}
