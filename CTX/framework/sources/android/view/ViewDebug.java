package android.view;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Picture;
import android.graphics.Rect;
import android.net.wifi.WifiEnterpriseConfig;
import android.os.Debug;
import android.os.Handler;
import android.os.Looper;
import android.os.RemoteException;
import android.provider.SettingsStringUtil;
import android.util.DisplayMetrics;
import android.util.Log;
import android.util.TypedValue;
import android.view.ViewGroup;
import java.io.BufferedOutputStream;
import java.io.BufferedWriter;
import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.lang.reflect.AccessibleObject;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.concurrent.Callable;
import java.util.concurrent.CancellationException;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.FutureTask;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicReference;

public class ViewDebug {
    private static final int CAPTURE_TIMEOUT = 4000;
    public static final boolean DEBUG_DRAG = false;
    public static final boolean DEBUG_POSITIONING = false;
    private static final String REMOTE_COMMAND_CAPTURE = "CAPTURE";
    private static final String REMOTE_COMMAND_CAPTURE_LAYERS = "CAPTURE_LAYERS";
    private static final String REMOTE_COMMAND_DUMP = "DUMP";
    private static final String REMOTE_COMMAND_DUMP_THEME = "DUMP_THEME";
    private static final String REMOTE_COMMAND_INVALIDATE = "INVALIDATE";
    private static final String REMOTE_COMMAND_OUTPUT_DISPLAYLIST = "OUTPUT_DISPLAYLIST";
    private static final String REMOTE_COMMAND_REQUEST_LAYOUT = "REQUEST_LAYOUT";
    private static final String REMOTE_PROFILE = "PROFILE";

    @Deprecated
    public static final boolean TRACE_HIERARCHY = false;

    @Deprecated
    public static final boolean TRACE_RECYCLER = false;
    private static HashMap<AccessibleObject, ExportedProperty> sAnnotations;
    private static HashMap<Class<?>, Field[]> sFieldsForClasses;
    private static HashMap<Class<?>, Method[]> sMethodsForClasses;
    private static HashMap<Class<?>, Method[]> mCapturedViewMethodsForClasses = null;
    private static HashMap<Class<?>, Field[]> mCapturedViewFieldsForClasses = null;

    public interface CanvasProvider {
        Bitmap createBitmap();

        Canvas getCanvas(View view, int i, int i2);
    }

    @Target({ElementType.FIELD, ElementType.METHOD})
    @Retention(RetentionPolicy.RUNTIME)
    public @interface CapturedViewProperty {
        boolean retrieveReturn() default false;
    }

    @Target({ElementType.FIELD, ElementType.METHOD})
    @Retention(RetentionPolicy.RUNTIME)
    public @interface ExportedProperty {
        String category() default "";

        boolean deepExport() default false;

        FlagToString[] flagMapping() default {};

        boolean formatToHexString() default false;

        boolean hasAdjacentMapping() default false;

        IntToString[] indexMapping() default {};

        IntToString[] mapping() default {};

        String prefix() default "";

        boolean resolveId() default false;
    }

    @Target({ElementType.TYPE})
    @Retention(RetentionPolicy.RUNTIME)
    public @interface FlagToString {
        int equals();

        int mask();

        String name();

        boolean outputIf() default true;
    }

    public interface HierarchyHandler {
        void dumpViewHierarchyWithProperties(BufferedWriter bufferedWriter, int i);

        View findHierarchyView(String str, int i);
    }

    @Deprecated
    public enum HierarchyTraceType {
        INVALIDATE,
        INVALIDATE_CHILD,
        INVALIDATE_CHILD_IN_PARENT,
        REQUEST_LAYOUT,
        ON_LAYOUT,
        ON_MEASURE,
        DRAW,
        BUILD_CACHE
    }

    @Target({ElementType.TYPE})
    @Retention(RetentionPolicy.RUNTIME)
    public @interface IntToString {
        int from();

        String to();
    }

    @Deprecated
    public enum RecyclerTraceType {
        NEW_VIEW,
        BIND_VIEW,
        RECYCLE_FROM_ACTIVE_HEAP,
        RECYCLE_FROM_SCRAP_HEAP,
        MOVE_TO_SCRAP_HEAP,
        MOVE_FROM_ACTIVE_TO_SCRAP_HEAP
    }

    public static long getViewInstanceCount() {
        return Debug.countInstancesOfClass(View.class);
    }

    public static long getViewRootImplCount() {
        return Debug.countInstancesOfClass(ViewRootImpl.class);
    }

    @Deprecated
    public static void trace(View view, RecyclerTraceType recyclerTraceType, int... iArr) {
    }

    @Deprecated
    public static void startRecyclerTracing(String str, View view) {
    }

    @Deprecated
    public static void stopRecyclerTracing() {
    }

    @Deprecated
    public static void trace(View view, HierarchyTraceType hierarchyTraceType) {
    }

    @Deprecated
    public static void startHierarchyTracing(String str, View view) {
    }

    @Deprecated
    public static void stopHierarchyTracing() {
    }

    static void dispatchCommand(View view, String str, String str2, OutputStream outputStream) throws Throwable {
        View rootView = view.getRootView();
        if (REMOTE_COMMAND_DUMP.equalsIgnoreCase(str)) {
            dump(rootView, false, true, outputStream);
            return;
        }
        if (REMOTE_COMMAND_DUMP_THEME.equalsIgnoreCase(str)) {
            dumpTheme(rootView, outputStream);
            return;
        }
        if (REMOTE_COMMAND_CAPTURE_LAYERS.equalsIgnoreCase(str)) {
            captureLayers(rootView, new DataOutputStream(outputStream));
            return;
        }
        String[] strArrSplit = str2.split(WifiEnterpriseConfig.CA_CERT_ALIAS_DELIMITER);
        if (REMOTE_COMMAND_CAPTURE.equalsIgnoreCase(str)) {
            capture(rootView, outputStream, strArrSplit[0]);
            return;
        }
        if (REMOTE_COMMAND_OUTPUT_DISPLAYLIST.equalsIgnoreCase(str)) {
            outputDisplayList(rootView, strArrSplit[0]);
            return;
        }
        if (REMOTE_COMMAND_INVALIDATE.equalsIgnoreCase(str)) {
            invalidate(rootView, strArrSplit[0]);
        } else if (REMOTE_COMMAND_REQUEST_LAYOUT.equalsIgnoreCase(str)) {
            requestLayout(rootView, strArrSplit[0]);
        } else if (REMOTE_PROFILE.equalsIgnoreCase(str)) {
            profile(rootView, outputStream, strArrSplit[0]);
        }
    }

    public static View findView(View view, String str) {
        if (str.indexOf(64) != -1) {
            String[] strArrSplit = str.split("@");
            String str2 = strArrSplit[0];
            int i = (int) Long.parseLong(strArrSplit[1], 16);
            View rootView = view.getRootView();
            if (rootView instanceof ViewGroup) {
                return findView((ViewGroup) rootView, str2, i);
            }
            return null;
        }
        return view.getRootView().findViewById(view.getResources().getIdentifier(str, null, null));
    }

    private static void invalidate(View view, String str) {
        View viewFindView = findView(view, str);
        if (viewFindView != null) {
            viewFindView.postInvalidate();
        }
    }

    private static void requestLayout(View view, String str) {
        final View viewFindView = findView(view, str);
        if (viewFindView != null) {
            view.post(new Runnable() {
                @Override
                public void run() {
                    viewFindView.requestLayout();
                }
            });
        }
    }

    private static void profile(View view, OutputStream outputStream, String str) throws Throwable {
        BufferedWriter bufferedWriter;
        View viewFindView = findView(view, str);
        BufferedWriter bufferedWriter2 = null;
        try {
            try {
                bufferedWriter = new BufferedWriter(new OutputStreamWriter(outputStream), 32768);
            } catch (Throwable th) {
                th = th;
            }
        } catch (Exception e) {
            e = e;
        }
        try {
            if (viewFindView != null) {
                profileViewAndChildren(viewFindView, bufferedWriter);
            } else {
                bufferedWriter.write("-1 -1 -1");
                bufferedWriter.newLine();
            }
            bufferedWriter.write("DONE.");
            bufferedWriter.newLine();
            bufferedWriter.close();
        } catch (Exception e2) {
            e = e2;
            bufferedWriter2 = bufferedWriter;
            Log.w("View", "Problem profiling the view:", e);
            if (bufferedWriter2 != null) {
                bufferedWriter2.close();
            }
        } catch (Throwable th2) {
            th = th2;
            bufferedWriter2 = bufferedWriter;
            if (bufferedWriter2 != null) {
                bufferedWriter2.close();
            }
            throw th;
        }
    }

    public static void profileViewAndChildren(View view, BufferedWriter bufferedWriter) throws IOException {
        RenderNode renderNodeCreate = RenderNode.create("ViewDebug", null);
        profileViewAndChildren(view, renderNodeCreate, bufferedWriter, true);
        renderNodeCreate.destroy();
    }

    private static void profileViewAndChildren(View view, RenderNode renderNode, BufferedWriter bufferedWriter, boolean z) throws IOException {
        long jProfileViewMeasure;
        long jProfileViewLayout;
        if (z || (view.mPrivateFlags & 2048) != 0) {
            jProfileViewMeasure = profileViewMeasure(view);
        } else {
            jProfileViewMeasure = 0;
        }
        if (z || (view.mPrivateFlags & 8192) != 0) {
            jProfileViewLayout = profileViewLayout(view);
        } else {
            jProfileViewLayout = 0;
        }
        long jProfileViewDraw = (!z && view.willNotDraw() && (view.mPrivateFlags & 32) == 0) ? 0L : profileViewDraw(view, renderNode);
        bufferedWriter.write(String.valueOf(jProfileViewMeasure));
        bufferedWriter.write(32);
        bufferedWriter.write(String.valueOf(jProfileViewLayout));
        bufferedWriter.write(32);
        bufferedWriter.write(String.valueOf(jProfileViewDraw));
        bufferedWriter.newLine();
        if (view instanceof ViewGroup) {
            ViewGroup viewGroup = (ViewGroup) view;
            int childCount = viewGroup.getChildCount();
            for (int i = 0; i < childCount; i++) {
                profileViewAndChildren(viewGroup.getChildAt(i), renderNode, bufferedWriter, false);
            }
        }
    }

    private static long profileViewMeasure(final View view) {
        return profileViewOperation(view, new ViewOperation() {
            @Override
            public void pre() {
                forceLayout(view);
            }

            private void forceLayout(View view2) {
                view2.forceLayout();
                if (view2 instanceof ViewGroup) {
                    ViewGroup viewGroup = (ViewGroup) view2;
                    int childCount = viewGroup.getChildCount();
                    for (int i = 0; i < childCount; i++) {
                        forceLayout(viewGroup.getChildAt(i));
                    }
                }
            }

            @Override
            public void run() {
                view.measure(view.mOldWidthMeasureSpec, view.mOldHeightMeasureSpec);
            }
        });
    }

    private static long profileViewLayout(final View view) {
        return profileViewOperation(view, new ViewOperation() {
            @Override
            public final void run() {
                View view2 = view;
                view2.layout(view2.mLeft, view2.mTop, view2.mRight, view2.mBottom);
            }
        });
    }

    private static long profileViewDraw(final View view, RenderNode renderNode) {
        DisplayMetrics displayMetrics = view.getResources().getDisplayMetrics();
        if (displayMetrics == null) {
            return 0L;
        }
        if (view.isHardwareAccelerated()) {
            final DisplayListCanvas displayListCanvasStart = renderNode.start(displayMetrics.widthPixels, displayMetrics.heightPixels);
            try {
                return profileViewOperation(view, new ViewOperation() {
                    @Override
                    public final void run() {
                        view.draw(displayListCanvasStart);
                    }
                });
            } finally {
                renderNode.end(displayListCanvasStart);
            }
        }
        Bitmap bitmapCreateBitmap = Bitmap.createBitmap(displayMetrics, displayMetrics.widthPixels, displayMetrics.heightPixels, Bitmap.Config.RGB_565);
        final Canvas canvas = new Canvas(bitmapCreateBitmap);
        try {
            return profileViewOperation(view, new ViewOperation() {
                @Override
                public final void run() {
                    view.draw(canvas);
                }
            });
        } finally {
            canvas.setBitmap(null);
            bitmapCreateBitmap.recycle();
        }
    }

    interface ViewOperation {
        void run();

        default void pre() {
        }
    }

    private static long profileViewOperation(View view, final ViewOperation viewOperation) {
        final CountDownLatch countDownLatch = new CountDownLatch(1);
        final long[] jArr = new long[1];
        view.post(new Runnable() {
            @Override
            public final void run() {
                ViewDebug.lambda$profileViewOperation$3(viewOperation, jArr, countDownLatch);
            }
        });
        try {
            if (!countDownLatch.await(4000L, TimeUnit.MILLISECONDS)) {
                Log.w("View", "Could not complete the profiling of the view " + view);
                return -1L;
            }
            return jArr[0];
        } catch (InterruptedException e) {
            Log.w("View", "Could not complete the profiling of the view " + view);
            Thread.currentThread().interrupt();
            return -1L;
        }
    }

    static void lambda$profileViewOperation$3(ViewOperation viewOperation, long[] jArr, CountDownLatch countDownLatch) {
        try {
            viewOperation.pre();
            long jThreadCpuTimeNanos = Debug.threadCpuTimeNanos();
            viewOperation.run();
            jArr[0] = Debug.threadCpuTimeNanos() - jThreadCpuTimeNanos;
        } finally {
            countDownLatch.countDown();
        }
    }

    public static void captureLayers(View view, DataOutputStream dataOutputStream) throws IOException {
        try {
            Rect rect = new Rect();
            try {
                view.mAttachInfo.mSession.getDisplayFrame(view.mAttachInfo.mWindow, rect);
            } catch (RemoteException e) {
            }
            dataOutputStream.writeInt(rect.width());
            dataOutputStream.writeInt(rect.height());
            captureViewLayer(view, dataOutputStream, true);
            dataOutputStream.write(2);
        } finally {
            dataOutputStream.close();
        }
    }

    private static void captureViewLayer(View view, DataOutputStream dataOutputStream, boolean z) throws IOException {
        ?? r8 = (view.getVisibility() == 0 && z) ? 1 : 0;
        if ((view.mPrivateFlags & 128) != 128) {
            int id = view.getId();
            String simpleName = view.getClass().getSimpleName();
            if (id != -1) {
                simpleName = resolveId(view.getContext(), id).toString();
            }
            dataOutputStream.write(1);
            dataOutputStream.writeUTF(simpleName);
            dataOutputStream.writeByte(r8);
            int[] iArr = new int[2];
            view.getLocationInWindow(iArr);
            dataOutputStream.writeInt(iArr[0]);
            dataOutputStream.writeInt(iArr[1]);
            dataOutputStream.flush();
            Bitmap bitmapPerformViewCapture = performViewCapture(view, true);
            if (bitmapPerformViewCapture != null) {
                ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream(bitmapPerformViewCapture.getWidth() * bitmapPerformViewCapture.getHeight() * 2);
                bitmapPerformViewCapture.compress(Bitmap.CompressFormat.PNG, 100, byteArrayOutputStream);
                dataOutputStream.writeInt(byteArrayOutputStream.size());
                byteArrayOutputStream.writeTo(dataOutputStream);
            }
            dataOutputStream.flush();
        }
        if (view instanceof ViewGroup) {
            ViewGroup viewGroup = (ViewGroup) view;
            int childCount = viewGroup.getChildCount();
            for (int i = 0; i < childCount; i++) {
                captureViewLayer(viewGroup.getChildAt(i), dataOutputStream, r8);
            }
        }
        if (view.mOverlay != null) {
            captureViewLayer(view.getOverlay().mOverlayViewGroup, dataOutputStream, r8);
        }
    }

    private static void outputDisplayList(View view, String str) throws IOException {
        View viewFindView = findView(view, str);
        viewFindView.getViewRootImpl().outputDisplayList(viewFindView);
    }

    public static void outputDisplayList(View view, View view2) {
        view.getViewRootImpl().outputDisplayList(view2);
    }

    private static void capture(View view, OutputStream outputStream, String str) throws Throwable {
        capture(view, outputStream, findView(view, str));
    }

    public static void capture(View view, OutputStream outputStream, View view2) throws Throwable {
        BufferedOutputStream bufferedOutputStream;
        Throwable th;
        Bitmap bitmapPerformViewCapture = performViewCapture(view2, false);
        if (bitmapPerformViewCapture == null) {
            Log.w("View", "Failed to create capture bitmap!");
            bitmapPerformViewCapture = Bitmap.createBitmap(view.getResources().getDisplayMetrics(), 1, 1, Bitmap.Config.ARGB_8888);
        }
        try {
            bufferedOutputStream = new BufferedOutputStream(outputStream, 32768);
            try {
                bitmapPerformViewCapture.compress(Bitmap.CompressFormat.PNG, 100, bufferedOutputStream);
                bufferedOutputStream.flush();
                bufferedOutputStream.close();
                bitmapPerformViewCapture.recycle();
            } catch (Throwable th2) {
                th = th2;
                if (bufferedOutputStream != null) {
                    bufferedOutputStream.close();
                }
                bitmapPerformViewCapture.recycle();
                throw th;
            }
        } catch (Throwable th3) {
            bufferedOutputStream = null;
            th = th3;
        }
    }

    private static Bitmap performViewCapture(final View view, final boolean z) {
        if (view != null) {
            final CountDownLatch countDownLatch = new CountDownLatch(1);
            final Bitmap[] bitmapArr = new Bitmap[1];
            view.post(new Runnable() {
                @Override
                public final void run() {
                    ViewDebug.lambda$performViewCapture$4(view, bitmapArr, z, countDownLatch);
                }
            });
            try {
                countDownLatch.await(4000L, TimeUnit.MILLISECONDS);
                return bitmapArr[0];
            } catch (InterruptedException e) {
                Log.w("View", "Could not complete the capture of the view " + view);
                Thread.currentThread().interrupt();
                return null;
            }
        }
        return null;
    }

    static void lambda$performViewCapture$4(View view, Bitmap[] bitmapArr, boolean z, CountDownLatch countDownLatch) {
        try {
            try {
                bitmapArr[0] = view.createSnapshot(view.isHardwareAccelerated() ? new HardwareCanvasProvider() : new SoftwareCanvasProvider(), z);
            } catch (OutOfMemoryError e) {
                Log.w("View", "Out of memory for bitmap");
            }
        } finally {
            countDownLatch.countDown();
        }
    }

    @Deprecated
    public static void dump(View view, boolean z, boolean z2, OutputStream outputStream) throws Throwable {
        BufferedWriter bufferedWriter;
        BufferedWriter bufferedWriter2 = null;
        try {
            try {
                bufferedWriter = new BufferedWriter(new OutputStreamWriter(outputStream, "utf-8"), 32768);
            } catch (Exception e) {
                e = e;
            }
        } catch (Throwable th) {
            th = th;
        }
        try {
            View rootView = view.getRootView();
            if (rootView instanceof ViewGroup) {
                ViewGroup viewGroup = (ViewGroup) rootView;
                dumpViewHierarchy(viewGroup.getContext(), viewGroup, bufferedWriter, 0, z, z2);
            }
            bufferedWriter.write("DONE.");
            bufferedWriter.newLine();
            bufferedWriter.close();
        } catch (Exception e2) {
            e = e2;
            bufferedWriter2 = bufferedWriter;
            Log.w("View", "Problem dumping the view:", e);
            if (bufferedWriter2 != null) {
                bufferedWriter2.close();
            }
        } catch (Throwable th2) {
            th = th2;
            bufferedWriter2 = bufferedWriter;
            if (bufferedWriter2 != null) {
                bufferedWriter2.close();
            }
            throw th;
        }
    }

    public static void dumpv2(final View view, ByteArrayOutputStream byteArrayOutputStream) throws InterruptedException {
        final ViewHierarchyEncoder viewHierarchyEncoder = new ViewHierarchyEncoder(byteArrayOutputStream);
        final CountDownLatch countDownLatch = new CountDownLatch(1);
        view.post(new Runnable() {
            @Override
            public void run() {
                viewHierarchyEncoder.addProperty("window:left", view.mAttachInfo.mWindowLeft);
                viewHierarchyEncoder.addProperty("window:top", view.mAttachInfo.mWindowTop);
                view.encode(viewHierarchyEncoder);
                countDownLatch.countDown();
            }
        });
        countDownLatch.await(2L, TimeUnit.SECONDS);
        viewHierarchyEncoder.endStream();
    }

    public static void dumpTheme(View view, OutputStream outputStream) throws Throwable {
        ?? bufferedWriter;
        ?? r0 = 0;
        int length = 0;
        ?? r02 = 0;
        try {
            try {
                bufferedWriter = new BufferedWriter(new OutputStreamWriter(outputStream, "utf-8"), 32768);
            } catch (Throwable th) {
                th = th;
                bufferedWriter = r0;
            }
        } catch (Exception e) {
            e = e;
        }
        try {
            String[] styleAttributesDump = getStyleAttributesDump(view.getContext().getResources(), view.getContext().getTheme());
            if (styleAttributesDump != null) {
                int i = 0;
                while (true) {
                    length = styleAttributesDump.length;
                    if (i >= length) {
                        break;
                    }
                    if (styleAttributesDump[i] != null) {
                        bufferedWriter.write(styleAttributesDump[i] + "\n");
                        bufferedWriter.write(styleAttributesDump[i + 1] + "\n");
                    }
                    i += 2;
                }
            }
            bufferedWriter.write("DONE.");
            bufferedWriter.newLine();
            bufferedWriter.close();
            r0 = length;
        } catch (Exception e2) {
            e = e2;
            r02 = bufferedWriter;
            Log.w("View", "Problem dumping View Theme:", e);
            r0 = r02;
            if (r02 != 0) {
                r02.close();
                r0 = r02;
            }
        } catch (Throwable th2) {
            th = th2;
            if (bufferedWriter != 0) {
                bufferedWriter.close();
            }
            throw th;
        }
    }

    private static String[] getStyleAttributesDump(Resources resources, Resources.Theme theme) {
        String string;
        TypedValue typedValue = new TypedValue();
        int[] allAttributes = theme.getAllAttributes();
        String[] strArr = new String[allAttributes.length * 2];
        int i = 0;
        for (int i2 : allAttributes) {
            try {
                strArr[i] = resources.getResourceName(i2);
                int i3 = i + 1;
                if (!theme.resolveAttribute(i2, typedValue, true)) {
                    string = "null";
                } else {
                    string = typedValue.coerceToString().toString();
                }
                strArr[i3] = string;
                i += 2;
                if (typedValue.type == 1) {
                    strArr[i - 1] = resources.getResourceName(typedValue.resourceId);
                }
            } catch (Resources.NotFoundException e) {
            }
        }
        return strArr;
    }

    private static View findView(ViewGroup viewGroup, String str, int i) {
        View viewFindHierarchyView;
        View viewFindView;
        if (isRequestedView(viewGroup, str, i)) {
            return viewGroup;
        }
        int childCount = viewGroup.getChildCount();
        for (int i2 = 0; i2 < childCount; i2++) {
            View childAt = viewGroup.getChildAt(i2);
            if (childAt instanceof ViewGroup) {
                View viewFindView2 = findView((ViewGroup) childAt, str, i);
                if (viewFindView2 != null) {
                    return viewFindView2;
                }
            } else if (isRequestedView(childAt, str, i)) {
                return childAt;
            }
            if (childAt.mOverlay != null && (viewFindView = findView(childAt.mOverlay.mOverlayViewGroup, str, i)) != null) {
                return viewFindView;
            }
            if ((childAt instanceof HierarchyHandler) && (viewFindHierarchyView = ((HierarchyHandler) childAt).findHierarchyView(str, i)) != null) {
                return viewFindHierarchyView;
            }
        }
        return null;
    }

    private static boolean isRequestedView(View view, String str, int i) {
        if (view.hashCode() == i) {
            String name = view.getClass().getName();
            if (str.equals("ViewOverlay")) {
                return name.equals("android.view.ViewOverlay$OverlayViewGroup");
            }
            return str.equals(name);
        }
        return false;
    }

    private static void dumpViewHierarchy(Context context, ViewGroup viewGroup, BufferedWriter bufferedWriter, int i, boolean z, boolean z2) {
        if (!dumpView(context, viewGroup, bufferedWriter, i, z2) || z) {
            return;
        }
        int childCount = viewGroup.getChildCount();
        for (int i2 = 0; i2 < childCount; i2++) {
            View childAt = viewGroup.getChildAt(i2);
            if (childAt instanceof ViewGroup) {
                dumpViewHierarchy(context, (ViewGroup) childAt, bufferedWriter, i + 1, z, z2);
            } else {
                dumpView(context, childAt, bufferedWriter, i + 1, z2);
            }
            if (childAt.mOverlay != null) {
                dumpViewHierarchy(context, childAt.getOverlay().mOverlayViewGroup, bufferedWriter, i + 2, z, z2);
            }
        }
        if (viewGroup instanceof HierarchyHandler) {
            ((HierarchyHandler) viewGroup).dumpViewHierarchyWithProperties(bufferedWriter, i + 1);
        }
    }

    private static boolean dumpView(Context context, View view, BufferedWriter bufferedWriter, int i, boolean z) {
        for (int i2 = 0; i2 < i; i2++) {
            try {
                bufferedWriter.write(32);
            } catch (IOException e) {
                Log.w("View", "Error while dumping hierarchy tree");
                return false;
            }
        }
        String name = view.getClass().getName();
        if (name.equals("android.view.ViewOverlay$OverlayViewGroup")) {
            name = "ViewOverlay";
        }
        bufferedWriter.write(name);
        bufferedWriter.write(64);
        bufferedWriter.write(Integer.toHexString(view.hashCode()));
        bufferedWriter.write(32);
        if (z) {
            dumpViewProperties(context, view, bufferedWriter);
        }
        bufferedWriter.newLine();
        return true;
    }

    private static Field[] getExportedPropertyFields(Class<?> cls) {
        if (sFieldsForClasses == null) {
            sFieldsForClasses = new HashMap<>();
        }
        if (sAnnotations == null) {
            sAnnotations = new HashMap<>(512);
        }
        HashMap<Class<?>, Field[]> map = sFieldsForClasses;
        Field[] fieldArr = map.get(cls);
        if (fieldArr != null) {
            return fieldArr;
        }
        try {
            Field[] declaredFieldsUnchecked = cls.getDeclaredFieldsUnchecked(false);
            ArrayList arrayList = new ArrayList();
            for (Field field : declaredFieldsUnchecked) {
                if (field.getType() != null && field.isAnnotationPresent(ExportedProperty.class)) {
                    field.setAccessible(true);
                    arrayList.add(field);
                    sAnnotations.put(field, (ExportedProperty) field.getAnnotation(ExportedProperty.class));
                }
            }
            Field[] fieldArr2 = (Field[]) arrayList.toArray(new Field[arrayList.size()]);
            map.put(cls, fieldArr2);
            return fieldArr2;
        } catch (NoClassDefFoundError e) {
            throw new AssertionError(e);
        }
    }

    private static Method[] getExportedPropertyMethods(Class<?> cls) {
        if (sMethodsForClasses == null) {
            sMethodsForClasses = new HashMap<>(100);
        }
        if (sAnnotations == null) {
            sAnnotations = new HashMap<>(512);
        }
        HashMap<Class<?>, Method[]> map = sMethodsForClasses;
        Method[] methodArr = map.get(cls);
        if (methodArr != null) {
            return methodArr;
        }
        Method[] declaredMethodsUnchecked = cls.getDeclaredMethodsUnchecked(false);
        ArrayList arrayList = new ArrayList();
        for (Method method : declaredMethodsUnchecked) {
            try {
                method.getReturnType();
                method.getParameterTypes();
                if (method.getParameterTypes().length == 0 && method.isAnnotationPresent(ExportedProperty.class) && method.getReturnType() != Void.class) {
                    method.setAccessible(true);
                    arrayList.add(method);
                    sAnnotations.put(method, (ExportedProperty) method.getAnnotation(ExportedProperty.class));
                }
            } catch (NoClassDefFoundError e) {
            }
        }
        Method[] methodArr2 = (Method[]) arrayList.toArray(new Method[arrayList.size()]);
        map.put(cls, methodArr2);
        return methodArr2;
    }

    private static void dumpViewProperties(Context context, Object obj, BufferedWriter bufferedWriter) throws IOException {
        dumpViewProperties(context, obj, bufferedWriter, "");
    }

    private static void dumpViewProperties(Context context, Object obj, BufferedWriter bufferedWriter, String str) throws IOException {
        if (obj == null) {
            bufferedWriter.write(str + "=4,null ");
            return;
        }
        Class<?> superclass = obj.getClass();
        do {
            exportFields(context, obj, bufferedWriter, superclass, str);
            exportMethods(context, obj, bufferedWriter, superclass, str);
            superclass = superclass.getSuperclass();
        } while (superclass != Object.class);
    }

    private static Object callMethodOnAppropriateTheadBlocking(final Method method, Object obj) throws IllegalAccessException, TimeoutException, InvocationTargetException {
        if (!(obj instanceof View)) {
            return method.invoke(obj, (Object[]) null);
        }
        final View view = (View) obj;
        FutureTask futureTask = new FutureTask(new Callable<Object>() {
            @Override
            public Object call() throws IllegalAccessException, InvocationTargetException {
                return method.invoke(view, (Object[]) null);
            }
        });
        Handler handler = view.getHandler();
        if (handler == null) {
            handler = new Handler(Looper.getMainLooper());
        }
        handler.post(futureTask);
        while (true) {
            try {
                return futureTask.get(4000L, TimeUnit.MILLISECONDS);
            } catch (InterruptedException e) {
            } catch (CancellationException e2) {
                throw new RuntimeException("Unexpected cancellation exception", e2);
            } catch (ExecutionException e3) {
                Throwable cause = e3.getCause();
                if (cause instanceof IllegalAccessException) {
                    throw ((IllegalAccessException) cause);
                }
                if (cause instanceof InvocationTargetException) {
                    throw ((InvocationTargetException) cause);
                }
                throw new RuntimeException("Unexpected exception", cause);
            }
        }
    }

    private static String formatIntToHexString(int i) {
        return "0x" + Integer.toHexString(i).toUpperCase();
    }

    private static void exportMethods(Context context, Object obj, BufferedWriter bufferedWriter, Class<?> cls, String str) throws IOException {
        Object objCallMethodOnAppropriateTheadBlocking;
        Class<?> returnType;
        ExportedProperty exportedProperty;
        String str2;
        boolean z;
        for (Method method : getExportedPropertyMethods(cls)) {
            try {
                objCallMethodOnAppropriateTheadBlocking = callMethodOnAppropriateTheadBlocking(method, obj);
                returnType = method.getReturnType();
                exportedProperty = sAnnotations.get(method);
                str2 = exportedProperty.category().length() != 0 ? exportedProperty.category() + SettingsStringUtil.DELIMITER : "";
            } catch (IllegalAccessException e) {
            } catch (InvocationTargetException e2) {
            } catch (TimeoutException e3) {
            }
            if (returnType == Integer.TYPE) {
                if (exportedProperty.resolveId() && context != null) {
                    objCallMethodOnAppropriateTheadBlocking = resolveId(context, ((Integer) objCallMethodOnAppropriateTheadBlocking).intValue());
                } else {
                    FlagToString[] flagToStringArrFlagMapping = exportedProperty.flagMapping();
                    if (flagToStringArrFlagMapping.length > 0) {
                        exportUnrolledFlags(bufferedWriter, flagToStringArrFlagMapping, ((Integer) objCallMethodOnAppropriateTheadBlocking).intValue(), str2 + str + method.getName() + '_');
                    }
                    IntToString[] intToStringArrMapping = exportedProperty.mapping();
                    if (intToStringArrMapping.length > 0) {
                        int iIntValue = ((Integer) objCallMethodOnAppropriateTheadBlocking).intValue();
                        int length = intToStringArrMapping.length;
                        int i = 0;
                        while (true) {
                            if (i < length) {
                                IntToString intToString = intToStringArrMapping[i];
                                if (intToString.from() != iIntValue) {
                                    i++;
                                } else {
                                    objCallMethodOnAppropriateTheadBlocking = intToString.to();
                                    z = true;
                                    break;
                                }
                            } else {
                                z = false;
                                break;
                            }
                        }
                        if (!z) {
                            objCallMethodOnAppropriateTheadBlocking = Integer.valueOf(iIntValue);
                        }
                    }
                }
            } else {
                if (returnType == int[].class) {
                    exportUnrolledArray(context, bufferedWriter, exportedProperty, (int[]) objCallMethodOnAppropriateTheadBlocking, str2 + str + method.getName() + '_', "()");
                } else if (returnType == String[].class) {
                    String[] strArr = (String[]) objCallMethodOnAppropriateTheadBlocking;
                    if (exportedProperty.hasAdjacentMapping() && strArr != null) {
                        for (int i2 = 0; i2 < strArr.length; i2 += 2) {
                            if (strArr[i2] != null) {
                                int i3 = i2 + 1;
                                writeEntry(bufferedWriter, str2 + str, strArr[i2], "()", strArr[i3] == null ? "null" : strArr[i3]);
                            }
                        }
                    }
                } else if (!returnType.isPrimitive() && exportedProperty.deepExport()) {
                    dumpViewProperties(context, objCallMethodOnAppropriateTheadBlocking, bufferedWriter, str + exportedProperty.prefix());
                }
            }
            writeEntry(bufferedWriter, str2 + str, method.getName(), "()", objCallMethodOnAppropriateTheadBlocking);
        }
    }

    private static void exportFields(Context context, Object obj, BufferedWriter bufferedWriter, Class<?> cls, String str) throws IOException {
        Class<?> type;
        ExportedProperty exportedProperty;
        String str2;
        Object intToHexString;
        for (Field field : getExportedPropertyFields(cls)) {
            try {
                type = field.getType();
                exportedProperty = sAnnotations.get(field);
                str2 = exportedProperty.category().length() != 0 ? exportedProperty.category() + SettingsStringUtil.DELIMITER : "";
            } catch (IllegalAccessException e) {
            }
            if (type == Integer.TYPE || type == Byte.TYPE) {
                if (exportedProperty.resolveId() && context != null) {
                    intToHexString = resolveId(context, field.getInt(obj));
                } else {
                    FlagToString[] flagToStringArrFlagMapping = exportedProperty.flagMapping();
                    if (flagToStringArrFlagMapping.length > 0) {
                        exportUnrolledFlags(bufferedWriter, flagToStringArrFlagMapping, field.getInt(obj), str2 + str + field.getName() + '_');
                    }
                    IntToString[] intToStringArrMapping = exportedProperty.mapping();
                    if (intToStringArrMapping.length > 0) {
                        int i = field.getInt(obj);
                        int length = intToStringArrMapping.length;
                        int i2 = 0;
                        while (true) {
                            if (i2 < length) {
                                IntToString intToString = intToStringArrMapping[i2];
                                IntToString[] intToStringArr = intToStringArrMapping;
                                if (intToString.from() != i) {
                                    i2++;
                                    intToStringArrMapping = intToStringArr;
                                } else {
                                    intToHexString = intToString.to();
                                    break;
                                }
                            } else {
                                intToHexString = null;
                                break;
                            }
                        }
                        if (intToHexString == null) {
                            intToHexString = Integer.valueOf(i);
                        }
                    } else {
                        intToHexString = null;
                    }
                    if (exportedProperty.formatToHexString()) {
                        intToHexString = field.get(obj);
                        if (type == Integer.TYPE) {
                            intToHexString = formatIntToHexString(((Integer) intToHexString).intValue());
                        } else if (type == Byte.TYPE) {
                            intToHexString = "0x" + Byte.toHexString(((Byte) intToHexString).byteValue(), true);
                        }
                    }
                }
            } else {
                if (type == int[].class) {
                    exportUnrolledArray(context, bufferedWriter, exportedProperty, (int[]) field.get(obj), str2 + str + field.getName() + '_', "");
                } else if (type == String[].class) {
                    String[] strArr = (String[]) field.get(obj);
                    if (exportedProperty.hasAdjacentMapping() && strArr != null) {
                        for (int i3 = 0; i3 < strArr.length; i3 += 2) {
                            if (strArr[i3] != null) {
                                int i4 = i3 + 1;
                                writeEntry(bufferedWriter, str2 + str, strArr[i3], "", strArr[i4] == null ? "null" : strArr[i4]);
                            }
                        }
                    }
                } else if (!type.isPrimitive() && exportedProperty.deepExport()) {
                    dumpViewProperties(context, field.get(obj), bufferedWriter, str + exportedProperty.prefix());
                } else {
                    intToHexString = null;
                }
            }
            if (intToHexString == null) {
                intToHexString = field.get(obj);
            }
            writeEntry(bufferedWriter, str2 + str, field.getName(), "", intToHexString);
        }
    }

    private static void writeEntry(BufferedWriter bufferedWriter, String str, String str2, String str3, Object obj) throws IOException {
        bufferedWriter.write(str);
        bufferedWriter.write(str2);
        bufferedWriter.write(str3);
        bufferedWriter.write("=");
        writeValue(bufferedWriter, obj);
        bufferedWriter.write(32);
    }

    private static void exportUnrolledFlags(BufferedWriter bufferedWriter, FlagToString[] flagToStringArr, int i, String str) throws IOException {
        for (FlagToString flagToString : flagToStringArr) {
            boolean zOutputIf = flagToString.outputIf();
            int iMask = flagToString.mask() & i;
            boolean z = iMask == flagToString.equals();
            if ((z && zOutputIf) || (!z && !zOutputIf)) {
                writeEntry(bufferedWriter, str, flagToString.name(), "", formatIntToHexString(iMask));
            }
        }
    }

    public static String intToString(Class<?> cls, String str, int i) {
        IntToString[] mapping = getMapping(cls, str);
        if (mapping == null) {
            return Integer.toString(i);
        }
        for (IntToString intToString : mapping) {
            if (intToString.from() == i) {
                return intToString.to();
            }
        }
        return Integer.toString(i);
    }

    public static String flagsToString(Class<?> cls, String str, int i) {
        FlagToString[] flagMapping = getFlagMapping(cls, str);
        if (flagMapping == null) {
            return Integer.toHexString(i);
        }
        StringBuilder sb = new StringBuilder();
        int length = flagMapping.length;
        int i2 = 0;
        while (true) {
            if (i2 >= length) {
                break;
            }
            FlagToString flagToString = flagMapping[i2];
            boolean zOutputIf = flagToString.outputIf();
            if (((flagToString.mask() & i) == flagToString.equals()) && zOutputIf) {
                sb.append(flagToString.name());
                sb.append(' ');
            }
            i2++;
        }
        if (sb.length() > 0) {
            sb.deleteCharAt(sb.length() - 1);
        }
        return sb.toString();
    }

    private static FlagToString[] getFlagMapping(Class<?> cls, String str) {
        try {
            return ((ExportedProperty) cls.getDeclaredField(str).getAnnotation(ExportedProperty.class)).flagMapping();
        } catch (NoSuchFieldException e) {
            return null;
        }
    }

    private static IntToString[] getMapping(Class<?> cls, String str) {
        try {
            return ((ExportedProperty) cls.getDeclaredField(str).getAnnotation(ExportedProperty.class)).mapping();
        } catch (NoSuchFieldException e) {
            return null;
        }
    }

    private static void exportUnrolledArray(Context context, BufferedWriter bufferedWriter, ExportedProperty exportedProperty, int[] iArr, String str, String str2) throws IOException {
        IntToString[] intToStringArrIndexMapping = exportedProperty.indexMapping();
        boolean z = intToStringArrIndexMapping.length > 0;
        IntToString[] intToStringArrMapping = exportedProperty.mapping();
        boolean z2 = intToStringArrMapping.length > 0;
        boolean z3 = exportedProperty.resolveId() && context != null;
        int length = iArr.length;
        for (int i = 0; i < length; i++) {
            String strValueOf = null;
            int i2 = iArr[i];
            String strValueOf2 = String.valueOf(i);
            if (z) {
                int length2 = intToStringArrIndexMapping.length;
                int i3 = 0;
                while (true) {
                    if (i3 >= length2) {
                        break;
                    }
                    IntToString intToString = intToStringArrIndexMapping[i3];
                    if (intToString.from() != i) {
                        i3++;
                    } else {
                        strValueOf2 = intToString.to();
                        break;
                    }
                }
            }
            if (z2) {
                int length3 = intToStringArrMapping.length;
                int i4 = 0;
                while (true) {
                    if (i4 >= length3) {
                        break;
                    }
                    IntToString intToString2 = intToStringArrMapping[i4];
                    if (intToString2.from() != i2) {
                        i4++;
                    } else {
                        strValueOf = intToString2.to();
                        break;
                    }
                }
            }
            if (z3) {
                if (strValueOf == null) {
                    strValueOf = (String) resolveId(context, i2);
                }
            } else {
                strValueOf = String.valueOf(i2);
            }
            writeEntry(bufferedWriter, str, strValueOf2, str2, strValueOf);
        }
    }

    static Object resolveId(Context context, int i) {
        Resources resources = context.getResources();
        if (i >= 0) {
            try {
                return resources.getResourceTypeName(i) + '/' + resources.getResourceEntryName(i);
            } catch (Resources.NotFoundException e) {
                return "id/" + formatIntToHexString(i);
            }
        }
        return "NO_ID";
    }

    private static void writeValue(BufferedWriter bufferedWriter, Object obj) throws IOException {
        if (obj != null) {
            try {
                String strReplace = obj.toString().replace("\n", "\\n");
                bufferedWriter.write(String.valueOf(strReplace.length()));
                bufferedWriter.write(",");
                bufferedWriter.write(strReplace);
                return;
            } catch (Throwable th) {
                bufferedWriter.write(String.valueOf("[EXCEPTION]".length()));
                bufferedWriter.write(",");
                bufferedWriter.write("[EXCEPTION]");
                throw th;
            }
        }
        bufferedWriter.write("4,null");
    }

    private static Field[] capturedViewGetPropertyFields(Class<?> cls) {
        if (mCapturedViewFieldsForClasses == null) {
            mCapturedViewFieldsForClasses = new HashMap<>();
        }
        HashMap<Class<?>, Field[]> map = mCapturedViewFieldsForClasses;
        Field[] fieldArr = map.get(cls);
        if (fieldArr != null) {
            return fieldArr;
        }
        ArrayList arrayList = new ArrayList();
        for (Field field : cls.getFields()) {
            if (field.isAnnotationPresent(CapturedViewProperty.class)) {
                field.setAccessible(true);
                arrayList.add(field);
            }
        }
        Field[] fieldArr2 = (Field[]) arrayList.toArray(new Field[arrayList.size()]);
        map.put(cls, fieldArr2);
        return fieldArr2;
    }

    private static Method[] capturedViewGetPropertyMethods(Class<?> cls) {
        if (mCapturedViewMethodsForClasses == null) {
            mCapturedViewMethodsForClasses = new HashMap<>();
        }
        HashMap<Class<?>, Method[]> map = mCapturedViewMethodsForClasses;
        Method[] methodArr = map.get(cls);
        if (methodArr != null) {
            return methodArr;
        }
        ArrayList arrayList = new ArrayList();
        for (Method method : cls.getMethods()) {
            if (method.getParameterTypes().length == 0 && method.isAnnotationPresent(CapturedViewProperty.class) && method.getReturnType() != Void.class) {
                method.setAccessible(true);
                arrayList.add(method);
            }
        }
        Method[] methodArr2 = (Method[]) arrayList.toArray(new Method[arrayList.size()]);
        map.put(cls, methodArr2);
        return methodArr2;
    }

    private static String capturedViewExportMethods(Object obj, Class<?> cls, String str) {
        if (obj == null) {
            return "null";
        }
        StringBuilder sb = new StringBuilder();
        for (Method method : capturedViewGetPropertyMethods(cls)) {
            try {
                Object objInvoke = method.invoke(obj, (Object[]) null);
                Class<?> returnType = method.getReturnType();
                if (((CapturedViewProperty) method.getAnnotation(CapturedViewProperty.class)).retrieveReturn()) {
                    sb.append(capturedViewExportMethods(objInvoke, returnType, method.getName() + "#"));
                } else {
                    sb.append(str);
                    sb.append(method.getName());
                    sb.append("()=");
                    if (objInvoke != null) {
                        sb.append(objInvoke.toString().replace("\n", "\\n"));
                    } else {
                        sb.append("null");
                    }
                    sb.append("; ");
                }
            } catch (IllegalAccessException e) {
            } catch (InvocationTargetException e2) {
            }
        }
        return sb.toString();
    }

    private static String capturedViewExportFields(Object obj, Class<?> cls, String str) {
        if (obj == null) {
            return "null";
        }
        StringBuilder sb = new StringBuilder();
        for (Field field : capturedViewGetPropertyFields(cls)) {
            try {
                Object obj2 = field.get(obj);
                sb.append(str);
                sb.append(field.getName());
                sb.append("=");
                if (obj2 != null) {
                    sb.append(obj2.toString().replace("\n", "\\n"));
                } else {
                    sb.append("null");
                }
                sb.append(' ');
            } catch (IllegalAccessException e) {
            }
        }
        return sb.toString();
    }

    public static void dumpCapturedView(String str, Object obj) {
        Class<?> cls = obj.getClass();
        StringBuilder sb = new StringBuilder(cls.getName() + ": ");
        sb.append(capturedViewExportFields(obj, cls, ""));
        sb.append(capturedViewExportMethods(obj, cls, ""));
        Log.d(str, sb.toString());
    }

    public static Object invokeViewMethod(final View view, final Method method, final Object[] objArr) {
        final CountDownLatch countDownLatch = new CountDownLatch(1);
        final AtomicReference atomicReference = new AtomicReference();
        final AtomicReference atomicReference2 = new AtomicReference();
        view.post(new Runnable() {
            @Override
            public void run() {
                try {
                    atomicReference.set(method.invoke(view, objArr));
                } catch (InvocationTargetException e) {
                    atomicReference2.set(e.getCause());
                } catch (Exception e2) {
                    atomicReference2.set(e2);
                }
                countDownLatch.countDown();
            }
        });
        try {
            countDownLatch.await();
            if (atomicReference2.get() != null) {
                throw new RuntimeException((Throwable) atomicReference2.get());
            }
            return atomicReference.get();
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    public static void setLayoutParameter(final View view, String str, int i) throws IllegalAccessException, NoSuchFieldException {
        final ViewGroup.LayoutParams layoutParams = view.getLayoutParams();
        Field field = layoutParams.getClass().getField(str);
        if (field.getType() != Integer.TYPE) {
            throw new RuntimeException("Only integer layout parameters can be set. Field " + str + " is of type " + field.getType().getSimpleName());
        }
        field.set(layoutParams, Integer.valueOf(i));
        view.post(new Runnable() {
            @Override
            public void run() {
                view.setLayoutParams(layoutParams);
            }
        });
    }

    public static class SoftwareCanvasProvider implements CanvasProvider {
        private Bitmap mBitmap;
        private Canvas mCanvas;
        private boolean mEnabledHwBitmapsInSwMode;

        @Override
        public Canvas getCanvas(View view, int i, int i2) {
            this.mBitmap = Bitmap.createBitmap(view.getResources().getDisplayMetrics(), i, i2, Bitmap.Config.ARGB_8888);
            if (this.mBitmap == null) {
                throw new OutOfMemoryError();
            }
            this.mBitmap.setDensity(view.getResources().getDisplayMetrics().densityDpi);
            if (view.mAttachInfo != null) {
                this.mCanvas = view.mAttachInfo.mCanvas;
            }
            if (this.mCanvas == null) {
                this.mCanvas = new Canvas();
            }
            this.mEnabledHwBitmapsInSwMode = this.mCanvas.isHwBitmapsInSwModeEnabled();
            this.mCanvas.setBitmap(this.mBitmap);
            return this.mCanvas;
        }

        @Override
        public Bitmap createBitmap() {
            this.mCanvas.setBitmap(null);
            this.mCanvas.setHwBitmapsInSwModeEnabled(this.mEnabledHwBitmapsInSwMode);
            return this.mBitmap;
        }
    }

    public static class HardwareCanvasProvider implements CanvasProvider {
        private Picture mPicture;

        @Override
        public Canvas getCanvas(View view, int i, int i2) {
            this.mPicture = new Picture();
            return this.mPicture.beginRecording(i, i2);
        }

        @Override
        public Bitmap createBitmap() {
            this.mPicture.endRecording();
            return Bitmap.createBitmap(this.mPicture);
        }
    }
}
