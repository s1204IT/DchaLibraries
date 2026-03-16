package android.view;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Rect;
import android.net.ProxyInfo;
import android.os.Debug;
import android.os.Handler;
import android.os.Looper;
import android.os.RemoteException;
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

    interface ViewOperation<T> {
        void post(T... tArr);

        T[] pre();

        void run(T... tArr);
    }

    public static long getViewInstanceCount() {
        return Debug.countInstancesOfClass(View.class);
    }

    public static long getViewRootImplCount() {
        return Debug.countInstancesOfClass(ViewRootImpl.class);
    }

    @Deprecated
    public static void trace(View view, RecyclerTraceType type, int... parameters) {
    }

    @Deprecated
    public static void startRecyclerTracing(String prefix, View view) {
    }

    @Deprecated
    public static void stopRecyclerTracing() {
    }

    @Deprecated
    public static void trace(View view, HierarchyTraceType type) {
    }

    @Deprecated
    public static void startHierarchyTracing(String prefix, View view) {
    }

    @Deprecated
    public static void stopHierarchyTracing() {
    }

    static void dispatchCommand(View view, String command, String parameters, OutputStream clientStream) throws Throwable {
        View view2 = view.getRootView();
        if (REMOTE_COMMAND_DUMP.equalsIgnoreCase(command)) {
            dump(view2, false, true, clientStream);
            return;
        }
        if (REMOTE_COMMAND_DUMP_THEME.equalsIgnoreCase(command)) {
            dumpTheme(view2, clientStream);
            return;
        }
        if (REMOTE_COMMAND_CAPTURE_LAYERS.equalsIgnoreCase(command)) {
            captureLayers(view2, new DataOutputStream(clientStream));
            return;
        }
        String[] params = parameters.split(" ");
        if (REMOTE_COMMAND_CAPTURE.equalsIgnoreCase(command)) {
            capture(view2, clientStream, params[0]);
            return;
        }
        if (REMOTE_COMMAND_OUTPUT_DISPLAYLIST.equalsIgnoreCase(command)) {
            outputDisplayList(view2, params[0]);
            return;
        }
        if (REMOTE_COMMAND_INVALIDATE.equalsIgnoreCase(command)) {
            invalidate(view2, params[0]);
        } else if (REMOTE_COMMAND_REQUEST_LAYOUT.equalsIgnoreCase(command)) {
            requestLayout(view2, params[0]);
        } else if (REMOTE_PROFILE.equalsIgnoreCase(command)) {
            profile(view2, clientStream, params[0]);
        }
    }

    public static View findView(View root, String parameter) {
        if (parameter.indexOf(64) != -1) {
            String[] ids = parameter.split("@");
            String className = ids[0];
            int hashCode = (int) Long.parseLong(ids[1], 16);
            View view = root.getRootView();
            if (view instanceof ViewGroup) {
                return findView((ViewGroup) view, className, hashCode);
            }
            return null;
        }
        int id = root.getResources().getIdentifier(parameter, null, null);
        return root.getRootView().findViewById(id);
    }

    private static void invalidate(View root, String parameter) {
        View view = findView(root, parameter);
        if (view != null) {
            view.postInvalidate();
        }
    }

    private static void requestLayout(View root, String parameter) {
        final View view = findView(root, parameter);
        if (view != null) {
            root.post(new Runnable() {
                @Override
                public void run() {
                    view.requestLayout();
                }
            });
        }
    }

    private static void profile(View root, OutputStream clientStream, String parameter) throws Throwable {
        BufferedWriter out;
        View view = findView(root, parameter);
        BufferedWriter out2 = null;
        try {
            try {
                out = new BufferedWriter(new OutputStreamWriter(clientStream), 32768);
            } catch (Exception e) {
                e = e;
            }
        } catch (Throwable th) {
            th = th;
        }
        try {
            if (view != null) {
                profileViewAndChildren(view, out);
            } else {
                out.write("-1 -1 -1");
                out.newLine();
            }
            out.write("DONE.");
            out.newLine();
            if (out != null) {
                out.close();
            }
        } catch (Exception e2) {
            e = e2;
            out2 = out;
            Log.w("View", "Problem profiling the view:", e);
            if (out2 != null) {
                out2.close();
            }
        } catch (Throwable th2) {
            th = th2;
            out2 = out;
            if (out2 != null) {
                out2.close();
            }
            throw th;
        }
    }

    public static void profileViewAndChildren(View view, BufferedWriter out) throws IOException {
        profileViewAndChildren(view, out, true);
    }

    private static void profileViewAndChildren(final View view, BufferedWriter out, boolean root) throws IOException {
        long durationMeasure = (root || (view.mPrivateFlags & 2048) != 0) ? profileViewOperation(view, new ViewOperation<Void>() {
            @Override
            public Void[] pre() {
                forceLayout(view);
                return null;
            }

            private void forceLayout(View view2) {
                view2.forceLayout();
                if (view2 instanceof ViewGroup) {
                    ViewGroup group = (ViewGroup) view2;
                    int count = group.getChildCount();
                    for (int i = 0; i < count; i++) {
                        forceLayout(group.getChildAt(i));
                    }
                }
            }

            @Override
            public void run(Void... data) {
                view.measure(view.mOldWidthMeasureSpec, view.mOldHeightMeasureSpec);
            }

            @Override
            public void post(Void... data) {
            }
        }) : 0L;
        long durationLayout = (root || (view.mPrivateFlags & 8192) != 0) ? profileViewOperation(view, new ViewOperation<Void>() {
            @Override
            public Void[] pre() {
                return null;
            }

            @Override
            public void run(Void... data) {
                view.layout(view.mLeft, view.mTop, view.mRight, view.mBottom);
            }

            @Override
            public void post(Void... data) {
            }
        }) : 0L;
        long durationDraw = (!root && view.willNotDraw() && (view.mPrivateFlags & 32) == 0) ? 0L : profileViewOperation(view, new ViewOperation<Object>() {
            @Override
            public Object[] pre() {
                DisplayMetrics metrics = (view == null || view.getResources() == null) ? null : view.getResources().getDisplayMetrics();
                Bitmap bitmap = metrics != null ? Bitmap.createBitmap(metrics, metrics.widthPixels, metrics.heightPixels, Bitmap.Config.RGB_565) : null;
                Canvas canvas = bitmap != null ? new Canvas(bitmap) : null;
                return new Object[]{bitmap, canvas};
            }

            @Override
            public void run(Object... data) {
                if (data[1] != null) {
                    view.draw((Canvas) data[1]);
                }
            }

            @Override
            public void post(Object... data) {
                if (data[1] != null) {
                    ((Canvas) data[1]).setBitmap(null);
                }
                if (data[0] != null) {
                    ((Bitmap) data[0]).recycle();
                }
            }
        });
        out.write(String.valueOf(durationMeasure));
        out.write(32);
        out.write(String.valueOf(durationLayout));
        out.write(32);
        out.write(String.valueOf(durationDraw));
        out.newLine();
        if (view instanceof ViewGroup) {
            ViewGroup group = (ViewGroup) view;
            int count = group.getChildCount();
            for (int i = 0; i < count; i++) {
                profileViewAndChildren(group.getChildAt(i), out, false);
            }
        }
    }

    private static <T> long profileViewOperation(View view, final ViewOperation<T> operation) {
        final CountDownLatch latch = new CountDownLatch(1);
        final long[] duration = new long[1];
        view.post(new Runnable() {
            @Override
            public void run() {
                try {
                    Object[] objArrPre = operation.pre();
                    long start = Debug.threadCpuTimeNanos();
                    operation.run(objArrPre);
                    duration[0] = Debug.threadCpuTimeNanos() - start;
                    operation.post(objArrPre);
                } finally {
                    latch.countDown();
                }
            }
        });
        try {
            if (!latch.await(4000L, TimeUnit.MILLISECONDS)) {
                Log.w("View", "Could not complete the profiling of the view " + view);
                return -1L;
            }
            return duration[0];
        } catch (InterruptedException e) {
            Log.w("View", "Could not complete the profiling of the view " + view);
            Thread.currentThread().interrupt();
            return -1L;
        }
    }

    public static void captureLayers(View root, DataOutputStream clientStream) throws IOException {
        try {
            Rect outRect = new Rect();
            try {
                root.mAttachInfo.mSession.getDisplayFrame(root.mAttachInfo.mWindow, outRect);
            } catch (RemoteException e) {
            }
            clientStream.writeInt(outRect.width());
            clientStream.writeInt(outRect.height());
            captureViewLayer(root, clientStream, true);
            clientStream.write(2);
        } finally {
            clientStream.close();
        }
    }

    private static void captureViewLayer(View view, DataOutputStream clientStream, boolean visible) throws IOException {
        boolean localVisible = view.getVisibility() == 0 && visible;
        if ((view.mPrivateFlags & 128) != 128) {
            int id = view.getId();
            String name = view.getClass().getSimpleName();
            if (id != -1) {
                name = resolveId(view.getContext(), id).toString();
            }
            clientStream.write(1);
            clientStream.writeUTF(name);
            clientStream.writeByte(localVisible ? 1 : 0);
            int[] position = new int[2];
            view.getLocationInWindow(position);
            clientStream.writeInt(position[0]);
            clientStream.writeInt(position[1]);
            clientStream.flush();
            Bitmap b = performViewCapture(view, true);
            if (b != null) {
                ByteArrayOutputStream arrayOut = new ByteArrayOutputStream(b.getWidth() * b.getHeight() * 2);
                b.compress(Bitmap.CompressFormat.PNG, 100, arrayOut);
                clientStream.writeInt(arrayOut.size());
                arrayOut.writeTo(clientStream);
            }
            clientStream.flush();
        }
        if (view instanceof ViewGroup) {
            ViewGroup group = (ViewGroup) view;
            int count = group.getChildCount();
            for (int i = 0; i < count; i++) {
                captureViewLayer(group.getChildAt(i), clientStream, localVisible);
            }
        }
        if (view.mOverlay != null) {
            ViewGroup overlayContainer = view.getOverlay().mOverlayViewGroup;
            captureViewLayer(overlayContainer, clientStream, localVisible);
        }
    }

    private static void outputDisplayList(View root, String parameter) throws IOException {
        View view = findView(root, parameter);
        view.getViewRootImpl().outputDisplayList(view);
    }

    public static void outputDisplayList(View root, View target) {
        root.getViewRootImpl().outputDisplayList(target);
    }

    private static void capture(View root, OutputStream clientStream, String parameter) throws Throwable {
        View captureView = findView(root, parameter);
        capture(root, clientStream, captureView);
    }

    public static void capture(View root, OutputStream clientStream, View captureView) throws Throwable {
        Bitmap b = performViewCapture(captureView, false);
        if (b == null) {
            Log.w("View", "Failed to create capture bitmap!");
            b = Bitmap.createBitmap(root.getResources().getDisplayMetrics(), 1, 1, Bitmap.Config.ARGB_8888);
        }
        BufferedOutputStream out = null;
        try {
            BufferedOutputStream out2 = new BufferedOutputStream(clientStream, 32768);
            try {
                b.compress(Bitmap.CompressFormat.PNG, 100, out2);
                out2.flush();
                if (out2 != null) {
                    out2.close();
                }
                b.recycle();
            } catch (Throwable th) {
                th = th;
                out = out2;
                if (out != null) {
                    out.close();
                }
                b.recycle();
                throw th;
            }
        } catch (Throwable th2) {
            th = th2;
        }
    }

    private static Bitmap performViewCapture(final View captureView, final boolean skipChildren) {
        if (captureView != null) {
            final CountDownLatch latch = new CountDownLatch(1);
            final Bitmap[] cache = new Bitmap[1];
            captureView.post(new Runnable() {
                @Override
                public void run() {
                    try {
                        cache[0] = captureView.createSnapshot(Bitmap.Config.ARGB_8888, 0, skipChildren);
                    } catch (OutOfMemoryError e) {
                        Log.w("View", "Out of memory for bitmap");
                    } finally {
                        latch.countDown();
                    }
                }
            });
            try {
                latch.await(4000L, TimeUnit.MILLISECONDS);
                return cache[0];
            } catch (InterruptedException e) {
                Log.w("View", "Could not complete the capture of the view " + captureView);
                Thread.currentThread().interrupt();
            }
        }
        return null;
    }

    public static void dump(View root, boolean skipChildren, boolean includeProperties, OutputStream clientStream) throws Throwable {
        BufferedWriter out;
        try {
            out = new BufferedWriter(new OutputStreamWriter(clientStream, "utf-8"), 32768);
            try {
                try {
                    View view = root.getRootView();
                    if (view instanceof ViewGroup) {
                        ViewGroup group = (ViewGroup) view;
                        dumpViewHierarchy(group.getContext(), group, out, 0, skipChildren, includeProperties);
                    }
                    out.write("DONE.");
                    out.newLine();
                    if (out != null) {
                        out.close();
                    }
                } catch (Exception e) {
                    e = e;
                    Log.w("View", "Problem dumping the view:", e);
                    if (out != null) {
                        out.close();
                    }
                }
            } catch (Throwable th) {
                th = th;
                if (out != null) {
                    out.close();
                }
                throw th;
            }
        } catch (Exception e2) {
            e = e2;
            out = null;
        } catch (Throwable th2) {
            th = th2;
            out = null;
            if (out != null) {
            }
            throw th;
        }
    }

    public static void dumpTheme(View view, OutputStream clientStream) throws Throwable {
        BufferedWriter out;
        BufferedWriter out2 = null;
        try {
            try {
                out = new BufferedWriter(new OutputStreamWriter(clientStream, "utf-8"), 32768);
            } catch (Throwable th) {
                th = th;
            }
        } catch (Exception e) {
            e = e;
        }
        try {
            String[] attributes = getStyleAttributesDump(view.getContext().getResources(), view.getContext().getTheme());
            if (attributes != null) {
                for (int i = 0; i < attributes.length; i += 2) {
                    if (attributes[i] != null) {
                        out.write(attributes[i] + "\n");
                        out.write(attributes[i + 1] + "\n");
                    }
                }
            }
            out.write("DONE.");
            out.newLine();
            if (out != null) {
                out.close();
                out2 = out;
            } else {
                out2 = out;
            }
        } catch (Exception e2) {
            e = e2;
            out2 = out;
            Log.w("View", "Problem dumping View Theme:", e);
            if (out2 != null) {
                out2.close();
            }
        } catch (Throwable th2) {
            th = th2;
            out2 = out;
            if (out2 != null) {
                out2.close();
            }
            throw th;
        }
    }

    private static String[] getStyleAttributesDump(Resources resources, Resources.Theme theme) {
        TypedValue outValue = new TypedValue();
        int i = 0;
        int[] attributes = theme.getAllAttributes();
        String[] data = new String[attributes.length * 2];
        for (int attributeId : attributes) {
            try {
                data[i] = resources.getResourceName(attributeId);
                data[i + 1] = theme.resolveAttribute(attributeId, outValue, true) ? outValue.coerceToString().toString() : "null";
                i += 2;
                if (outValue.type == 1) {
                    data[i - 1] = resources.getResourceName(outValue.resourceId);
                }
            } catch (Resources.NotFoundException e) {
            }
        }
        return data;
    }

    private static View findView(ViewGroup group, String className, int hashCode) {
        View found;
        View found2;
        if (isRequestedView(group, className, hashCode)) {
            return group;
        }
        int count = group.getChildCount();
        for (int i = 0; i < count; i++) {
            View childAt = group.getChildAt(i);
            if (childAt instanceof ViewGroup) {
                View found3 = findView((ViewGroup) childAt, className, hashCode);
                if (found3 != null) {
                    return found3;
                }
            } else if (isRequestedView(childAt, className, hashCode)) {
                return childAt;
            }
            if (childAt.mOverlay == null || (found2 = findView(childAt.mOverlay.mOverlayViewGroup, className, hashCode)) == null) {
                if ((childAt instanceof HierarchyHandler) && (found = ((HierarchyHandler) childAt).findHierarchyView(className, hashCode)) != null) {
                    return found;
                }
            } else {
                return found2;
            }
        }
        return null;
    }

    private static boolean isRequestedView(View view, String className, int hashCode) {
        if (view.hashCode() == hashCode) {
            String viewClassName = view.getClass().getName();
            if (className.equals("ViewOverlay")) {
                return viewClassName.equals("android.view.ViewOverlay$OverlayViewGroup");
            }
            return className.equals(viewClassName);
        }
        return false;
    }

    private static void dumpViewHierarchy(Context context, ViewGroup viewGroup, BufferedWriter out, int level, boolean skipChildren, boolean includeProperties) throws Throwable {
        if (dumpView(context, viewGroup, out, level, includeProperties) && !skipChildren) {
            int count = viewGroup.getChildCount();
            for (int i = 0; i < count; i++) {
                View view = viewGroup.getChildAt(i);
                if (view instanceof ViewGroup) {
                    dumpViewHierarchy(context, (ViewGroup) view, out, level + 1, skipChildren, includeProperties);
                } else {
                    dumpView(context, view, out, level + 1, includeProperties);
                }
                if (view.mOverlay != null) {
                    ViewOverlay overlay = view.getOverlay();
                    ViewGroup overlayContainer = overlay.mOverlayViewGroup;
                    dumpViewHierarchy(context, overlayContainer, out, level + 2, skipChildren, includeProperties);
                }
            }
            if (viewGroup instanceof HierarchyHandler) {
                ((HierarchyHandler) viewGroup).dumpViewHierarchyWithProperties(out, level + 1);
            }
        }
    }

    private static boolean dumpView(Context context, View view, BufferedWriter out, int level, boolean includeProperties) throws Throwable {
        for (int i = 0; i < level; i++) {
            try {
                out.write(32);
            } catch (IOException e) {
                Log.w("View", "Error while dumping hierarchy tree");
                return false;
            }
        }
        String className = view.getClass().getName();
        if (className.equals("android.view.ViewOverlay$OverlayViewGroup")) {
            className = "ViewOverlay";
        }
        out.write(className);
        out.write(64);
        out.write(Integer.toHexString(view.hashCode()));
        out.write(32);
        if (includeProperties) {
            dumpViewProperties(context, view, out);
        }
        out.newLine();
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
        ArrayList arrayList = new ArrayList();
        cls.getDeclaredFieldsUnchecked(false, arrayList);
        ArrayList arrayList2 = new ArrayList();
        int size = arrayList.size();
        for (int i = 0; i < size; i++) {
            Field field = (Field) arrayList.get(i);
            try {
                field.getType();
                if (field.isAnnotationPresent(ExportedProperty.class)) {
                    field.setAccessible(true);
                    arrayList2.add(field);
                    sAnnotations.put(field, (ExportedProperty) field.getAnnotation(ExportedProperty.class));
                }
            } catch (NoClassDefFoundError e) {
            }
        }
        Field[] fieldArr2 = (Field[]) arrayList2.toArray(new Field[arrayList2.size()]);
        map.put(cls, fieldArr2);
        return fieldArr2;
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
        ArrayList arrayList = new ArrayList();
        cls.getDeclaredMethodsUnchecked(false, arrayList);
        ArrayList arrayList2 = new ArrayList();
        int size = arrayList.size();
        for (int i = 0; i < size; i++) {
            Method method = (Method) arrayList.get(i);
            try {
                method.getReturnType();
                method.getParameterTypes();
                if (method.getParameterTypes().length == 0 && method.isAnnotationPresent(ExportedProperty.class) && method.getReturnType() != Void.class) {
                    method.setAccessible(true);
                    arrayList2.add(method);
                    sAnnotations.put(method, (ExportedProperty) method.getAnnotation(ExportedProperty.class));
                }
            } catch (NoClassDefFoundError e) {
            }
        }
        Method[] methodArr2 = (Method[]) arrayList2.toArray(new Method[arrayList2.size()]);
        map.put(cls, methodArr2);
        return methodArr2;
    }

    private static void dumpViewProperties(Context context, Object view, BufferedWriter out) throws Throwable {
        dumpViewProperties(context, view, out, ProxyInfo.LOCAL_EXCL_LIST);
    }

    private static void dumpViewProperties(Context context, Object view, BufferedWriter out, String prefix) throws Throwable {
        if (view == null) {
            out.write(prefix + "=4,null ");
            return;
        }
        Class<?> klass = view.getClass();
        do {
            exportFields(context, view, out, klass, prefix);
            exportMethods(context, view, out, klass, prefix);
            klass = klass.getSuperclass();
        } while (klass != Object.class);
    }

    private static Object callMethodOnAppropriateTheadBlocking(final Method method, Object object) throws Throwable {
        if (!(object instanceof View)) {
            return method.invoke(object, (Object[]) null);
        }
        final View view = (View) object;
        Callable<Object> callable = new Callable<Object>() {
            @Override
            public Object call() throws IllegalAccessException, InvocationTargetException {
                return method.invoke(view, (Object[]) null);
            }
        };
        FutureTask<Object> future = new FutureTask<>(callable);
        Handler handler = view.getHandler();
        if (handler == null) {
            handler = new Handler(Looper.getMainLooper());
        }
        handler.post(future);
        while (true) {
            try {
                return future.get(4000L, TimeUnit.MILLISECONDS);
            } catch (InterruptedException e) {
            } catch (CancellationException e2) {
                throw new RuntimeException("Unexpected cancellation exception", e2);
            } catch (ExecutionException e3) {
                Throwable t = e3.getCause();
                if (t instanceof IllegalAccessException) {
                    throw ((IllegalAccessException) t);
                }
                if (t instanceof InvocationTargetException) {
                    throw ((InvocationTargetException) t);
                }
                throw new RuntimeException("Unexpected exception", t);
            }
        }
    }

    private static String formatIntToHexString(int value) {
        return "0x" + Integer.toHexString(value).toUpperCase();
    }

    private static void exportMethods(Context context, Object view, BufferedWriter out, Class<?> klass, String prefix) throws Throwable {
        Object methodValue;
        Class<?> returnType;
        ExportedProperty property;
        String categoryPrefix;
        Method[] methods = getExportedPropertyMethods(klass);
        for (Method method : methods) {
            try {
                methodValue = callMethodOnAppropriateTheadBlocking(method, view);
                returnType = method.getReturnType();
                property = sAnnotations.get(method);
                categoryPrefix = property.category().length() != 0 ? property.category() + ":" : ProxyInfo.LOCAL_EXCL_LIST;
            } catch (IllegalAccessException e) {
            } catch (InvocationTargetException e2) {
            } catch (TimeoutException e3) {
            }
            if (returnType == Integer.TYPE) {
                if (property.resolveId() && context != null) {
                    int id = ((Integer) methodValue).intValue();
                    methodValue = resolveId(context, id);
                } else {
                    FlagToString[] flagsMapping = property.flagMapping();
                    if (flagsMapping.length > 0) {
                        int intValue = ((Integer) methodValue).intValue();
                        String valuePrefix = categoryPrefix + prefix + method.getName() + '_';
                        exportUnrolledFlags(out, flagsMapping, intValue, valuePrefix);
                    }
                    IntToString[] mapping = property.mapping();
                    if (mapping.length > 0) {
                        int intValue2 = ((Integer) methodValue).intValue();
                        boolean mapped = false;
                        int mappingCount = mapping.length;
                        int j = 0;
                        while (true) {
                            if (j >= mappingCount) {
                                break;
                            }
                            IntToString mapper = mapping[j];
                            if (mapper.from() != intValue2) {
                                j++;
                            } else {
                                methodValue = mapper.to();
                                mapped = true;
                                break;
                            }
                        }
                        if (!mapped) {
                            methodValue = Integer.valueOf(intValue2);
                        }
                    }
                }
            } else {
                if (returnType == int[].class) {
                    String valuePrefix2 = categoryPrefix + prefix + method.getName() + '_';
                    exportUnrolledArray(context, out, property, (int[]) methodValue, valuePrefix2, "()");
                } else if (returnType == String[].class) {
                    String[] array = (String[]) methodValue;
                    if (property.hasAdjacentMapping() && array != null) {
                        for (int j2 = 0; j2 < array.length; j2 += 2) {
                            if (array[j2] != null) {
                                writeEntry(out, categoryPrefix + prefix, array[j2], "()", array[j2 + 1] == null ? "null" : array[j2 + 1]);
                            }
                        }
                    }
                } else if (!returnType.isPrimitive() && property.deepExport()) {
                    dumpViewProperties(context, methodValue, out, prefix + property.prefix());
                }
            }
            writeEntry(out, categoryPrefix + prefix, method.getName(), "()", methodValue);
        }
    }

    private static void exportFields(Context context, Object view, BufferedWriter out, Class<?> klass, String prefix) throws IOException {
        Class<?> type;
        ExportedProperty property;
        String categoryPrefix;
        Field[] fields = getExportedPropertyFields(klass);
        for (Field field : fields) {
            Object fieldValue = null;
            try {
                type = field.getType();
                property = sAnnotations.get(field);
                categoryPrefix = property.category().length() != 0 ? property.category() + ":" : ProxyInfo.LOCAL_EXCL_LIST;
            } catch (IllegalAccessException e) {
            }
            if (type == Integer.TYPE || type == Byte.TYPE) {
                if (property.resolveId() && context != null) {
                    int id = field.getInt(view);
                    fieldValue = resolveId(context, id);
                } else {
                    FlagToString[] flagsMapping = property.flagMapping();
                    if (flagsMapping.length > 0) {
                        int intValue = field.getInt(view);
                        String valuePrefix = categoryPrefix + prefix + field.getName() + '_';
                        exportUnrolledFlags(out, flagsMapping, intValue, valuePrefix);
                    }
                    IntToString[] mapping = property.mapping();
                    if (mapping.length > 0) {
                        int intValue2 = field.getInt(view);
                        int mappingCount = mapping.length;
                        int j = 0;
                        while (true) {
                            if (j >= mappingCount) {
                                break;
                            }
                            IntToString mapped = mapping[j];
                            if (mapped.from() != intValue2) {
                                j++;
                            } else {
                                fieldValue = mapped.to();
                                break;
                            }
                        }
                        if (fieldValue == null) {
                            fieldValue = Integer.valueOf(intValue2);
                        }
                    }
                    if (property.formatToHexString()) {
                        fieldValue = field.get(view);
                        if (type == Integer.TYPE) {
                            fieldValue = formatIntToHexString(((Integer) fieldValue).intValue());
                        } else if (type == Byte.TYPE) {
                            fieldValue = "0x" + Byte.toHexString(((Byte) fieldValue).byteValue(), true);
                        }
                    }
                }
            } else {
                if (type == int[].class) {
                    int[] array = (int[]) field.get(view);
                    String valuePrefix2 = categoryPrefix + prefix + field.getName() + '_';
                    exportUnrolledArray(context, out, property, array, valuePrefix2, ProxyInfo.LOCAL_EXCL_LIST);
                } else if (type == String[].class) {
                    String[] array2 = (String[]) field.get(view);
                    if (property.hasAdjacentMapping() && array2 != null) {
                        for (int j2 = 0; j2 < array2.length; j2 += 2) {
                            if (array2[j2] != null) {
                                writeEntry(out, categoryPrefix + prefix, array2[j2], ProxyInfo.LOCAL_EXCL_LIST, array2[j2 + 1] == null ? "null" : array2[j2 + 1]);
                            }
                        }
                    }
                } else if (!type.isPrimitive() && property.deepExport()) {
                    dumpViewProperties(context, field.get(view), out, prefix + property.prefix());
                }
            }
            if (fieldValue == null) {
                fieldValue = field.get(view);
            }
            writeEntry(out, categoryPrefix + prefix, field.getName(), ProxyInfo.LOCAL_EXCL_LIST, fieldValue);
        }
    }

    private static void writeEntry(BufferedWriter out, String prefix, String name, String suffix, Object value) throws IOException {
        out.write(prefix);
        out.write(name);
        out.write(suffix);
        out.write("=");
        writeValue(out, value);
        out.write(32);
    }

    private static void exportUnrolledFlags(BufferedWriter out, FlagToString[] mapping, int intValue, String prefix) throws IOException {
        for (FlagToString flagMapping : mapping) {
            boolean ifTrue = flagMapping.outputIf();
            int maskResult = intValue & flagMapping.mask();
            boolean test = maskResult == flagMapping.equals();
            if ((test && ifTrue) || (!test && !ifTrue)) {
                String name = flagMapping.name();
                String value = formatIntToHexString(maskResult);
                writeEntry(out, prefix, name, ProxyInfo.LOCAL_EXCL_LIST, value);
            }
        }
    }

    private static void exportUnrolledArray(Context context, BufferedWriter out, ExportedProperty property, int[] array, String prefix, String suffix) throws IOException {
        IntToString[] indexMapping = property.indexMapping();
        boolean hasIndexMapping = indexMapping.length > 0;
        IntToString[] mapping = property.mapping();
        boolean hasMapping = mapping.length > 0;
        boolean resolveId = property.resolveId() && context != null;
        int valuesCount = array.length;
        for (int j = 0; j < valuesCount; j++) {
            String value = null;
            int intValue = array[j];
            String name = String.valueOf(j);
            if (hasIndexMapping) {
                int mappingCount = indexMapping.length;
                int k = 0;
                while (true) {
                    if (k >= mappingCount) {
                        break;
                    }
                    IntToString mapped = indexMapping[k];
                    if (mapped.from() != j) {
                        k++;
                    } else {
                        name = mapped.to();
                        break;
                    }
                }
            }
            if (hasMapping) {
                int mappingCount2 = mapping.length;
                int k2 = 0;
                while (true) {
                    if (k2 >= mappingCount2) {
                        break;
                    }
                    IntToString mapped2 = mapping[k2];
                    if (mapped2.from() != intValue) {
                        k2++;
                    } else {
                        value = mapped2.to();
                        break;
                    }
                }
            }
            if (resolveId) {
                if (value == null) {
                    value = (String) resolveId(context, intValue);
                }
            } else {
                value = String.valueOf(intValue);
            }
            writeEntry(out, prefix, name, suffix, value);
        }
    }

    static Object resolveId(Context context, int id) {
        Resources resources = context.getResources();
        if (id >= 0) {
            try {
                String fieldValue = resources.getResourceTypeName(id) + '/' + resources.getResourceEntryName(id);
                return fieldValue;
            } catch (Resources.NotFoundException e) {
                String fieldValue2 = "id/" + formatIntToHexString(id);
                return fieldValue2;
            }
        }
        return "NO_ID";
    }

    private static void writeValue(BufferedWriter out, Object value) throws IOException {
        if (value != null) {
            String output = "[EXCEPTION]";
            try {
                output = value.toString().replace("\n", "\\n");
                return;
            } finally {
                out.write(String.valueOf(output.length()));
                out.write(",");
                out.write(output);
            }
        }
        out.write("4,null");
    }

    private static Field[] capturedViewGetPropertyFields(Class<?> klass) {
        if (mCapturedViewFieldsForClasses == null) {
            mCapturedViewFieldsForClasses = new HashMap<>();
        }
        HashMap<Class<?>, Field[]> map = mCapturedViewFieldsForClasses;
        Field[] fields = map.get(klass);
        if (fields != null) {
            return fields;
        }
        ArrayList<Field> foundFields = new ArrayList<>();
        Field[] fields2 = klass.getFields();
        for (Field field : fields2) {
            if (field.isAnnotationPresent(CapturedViewProperty.class)) {
                field.setAccessible(true);
                foundFields.add(field);
            }
        }
        Field[] fields3 = (Field[]) foundFields.toArray(new Field[foundFields.size()]);
        map.put(klass, fields3);
        return fields3;
    }

    private static Method[] capturedViewGetPropertyMethods(Class<?> klass) {
        if (mCapturedViewMethodsForClasses == null) {
            mCapturedViewMethodsForClasses = new HashMap<>();
        }
        HashMap<Class<?>, Method[]> map = mCapturedViewMethodsForClasses;
        Method[] methods = map.get(klass);
        if (methods != null) {
            return methods;
        }
        ArrayList<Method> foundMethods = new ArrayList<>();
        Method[] methods2 = klass.getMethods();
        for (Method method : methods2) {
            if (method.getParameterTypes().length == 0 && method.isAnnotationPresent(CapturedViewProperty.class) && method.getReturnType() != Void.class) {
                method.setAccessible(true);
                foundMethods.add(method);
            }
        }
        Method[] methods3 = (Method[]) foundMethods.toArray(new Method[foundMethods.size()]);
        map.put(klass, methods3);
        return methods3;
    }

    private static String capturedViewExportMethods(Object obj, Class<?> klass, String prefix) {
        if (obj == null) {
            return "null";
        }
        StringBuilder sb = new StringBuilder();
        Method[] methods = capturedViewGetPropertyMethods(klass);
        for (Method method : methods) {
            try {
                Object methodValue = method.invoke(obj, (Object[]) null);
                Class<?> returnType = method.getReturnType();
                CapturedViewProperty property = (CapturedViewProperty) method.getAnnotation(CapturedViewProperty.class);
                if (property.retrieveReturn()) {
                    sb.append(capturedViewExportMethods(methodValue, returnType, method.getName() + "#"));
                } else {
                    sb.append(prefix);
                    sb.append(method.getName());
                    sb.append("()=");
                    if (methodValue != null) {
                        String value = methodValue.toString().replace("\n", "\\n");
                        sb.append(value);
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

    private static String capturedViewExportFields(Object obj, Class<?> klass, String prefix) {
        if (obj == null) {
            return "null";
        }
        StringBuilder sb = new StringBuilder();
        Field[] fields = capturedViewGetPropertyFields(klass);
        for (Field field : fields) {
            try {
                Object fieldValue = field.get(obj);
                sb.append(prefix);
                sb.append(field.getName());
                sb.append("=");
                if (fieldValue != null) {
                    String value = fieldValue.toString().replace("\n", "\\n");
                    sb.append(value);
                } else {
                    sb.append("null");
                }
                sb.append(' ');
            } catch (IllegalAccessException e) {
            }
        }
        return sb.toString();
    }

    public static void dumpCapturedView(String tag, Object view) {
        Class<?> klass = view.getClass();
        Log.d(tag, (klass.getName() + ": ") + capturedViewExportFields(view, klass, ProxyInfo.LOCAL_EXCL_LIST) + capturedViewExportMethods(view, klass, ProxyInfo.LOCAL_EXCL_LIST));
    }

    public static Object invokeViewMethod(final View view, final Method method, final Object[] args) {
        final CountDownLatch latch = new CountDownLatch(1);
        final AtomicReference<Object> result = new AtomicReference<>();
        final AtomicReference<Throwable> exception = new AtomicReference<>();
        view.post(new Runnable() {
            @Override
            public void run() {
                try {
                    result.set(method.invoke(view, args));
                } catch (InvocationTargetException e) {
                    exception.set(e.getCause());
                } catch (Exception e2) {
                    exception.set(e2);
                }
                latch.countDown();
            }
        });
        try {
            latch.await();
            if (exception.get() != null) {
                throw new RuntimeException(exception.get());
            }
            return result.get();
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    public static void setLayoutParameter(final View view, String param, int value) throws IllegalAccessException, NoSuchFieldException {
        final ViewGroup.LayoutParams p = view.getLayoutParams();
        Field f = p.getClass().getField(param);
        if (f.getType() != Integer.TYPE) {
            throw new RuntimeException("Only integer layout parameters can be set. Field " + param + " is of type " + f.getType().getSimpleName());
        }
        f.set(p, Integer.valueOf(value));
        view.post(new Runnable() {
            @Override
            public void run() {
                view.setLayoutParams(p);
            }
        });
    }
}
