package android.ddm;

import android.opengl.GLUtils;
import android.util.Log;
import android.view.View;
import android.view.ViewDebug;
import android.view.WindowManagerGlobal;
import java.io.BufferedWriter;
import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.lang.reflect.Method;
import java.nio.BufferUnderflowException;
import java.nio.ByteBuffer;
import org.apache.harmony.dalvik.ddmc.Chunk;
import org.apache.harmony.dalvik.ddmc.ChunkHandler;
import org.apache.harmony.dalvik.ddmc.DdmServer;

public class DdmHandleViewDebug extends ChunkHandler {
    private static final int ERR_EXCEPTION = -3;
    private static final int ERR_INVALID_OP = -1;
    private static final int ERR_INVALID_PARAM = -2;
    private static final String TAG = "DdmViewDebug";
    private static final int VUOP_CAPTURE_VIEW = 1;
    private static final int VUOP_DUMP_DISPLAYLIST = 2;
    private static final int VUOP_INVOKE_VIEW_METHOD = 4;
    private static final int VUOP_PROFILE_VIEW = 3;
    private static final int VUOP_SET_LAYOUT_PARAMETER = 5;
    private static final int VURT_CAPTURE_LAYERS = 2;
    private static final int VURT_DUMP_HIERARCHY = 1;
    private static final int VURT_DUMP_THEME = 3;
    public static final int CHUNK_VUGL = type("VUGL");
    private static final int CHUNK_VULW = type("VULW");
    private static final int CHUNK_VURT = type("VURT");
    private static final int CHUNK_VUOP = type("VUOP");
    private static final DdmHandleViewDebug sInstance = new DdmHandleViewDebug();

    private DdmHandleViewDebug() {
    }

    public static void register() {
        DdmServer.registerHandler(CHUNK_VUGL, sInstance);
        DdmServer.registerHandler(CHUNK_VULW, sInstance);
        DdmServer.registerHandler(CHUNK_VURT, sInstance);
        DdmServer.registerHandler(CHUNK_VUOP, sInstance);
    }

    public void connected() {
    }

    public void disconnected() {
    }

    public Chunk handleChunk(Chunk request) {
        int type = request.type;
        if (type == CHUNK_VUGL) {
            return handleOpenGlTrace(request);
        }
        if (type == CHUNK_VULW) {
            return listWindows();
        }
        ByteBuffer in = wrapChunk(request);
        int op = in.getInt();
        View rootView = getRootView(in);
        if (rootView == null) {
            return createFailChunk(-2, "Invalid View Root");
        }
        if (type == CHUNK_VURT) {
            if (op == 1) {
                return dumpHierarchy(rootView, in);
            }
            if (op == 2) {
                return captureLayers(rootView);
            }
            if (op == 3) {
                return dumpTheme(rootView);
            }
            return createFailChunk(-1, "Unknown view root operation: " + op);
        }
        View targetView = getTargetView(rootView, in);
        if (targetView == null) {
            return createFailChunk(-2, "Invalid target view");
        }
        if (type == CHUNK_VUOP) {
            switch (op) {
                case 1:
                    return captureView(rootView, targetView);
                case 2:
                    return dumpDisplayLists(rootView, targetView);
                case 3:
                    return profileView(rootView, targetView);
                case 4:
                    return invokeViewMethod(rootView, targetView, in);
                case 5:
                    return setLayoutParameter(rootView, targetView, in);
                default:
                    return createFailChunk(-1, "Unknown view operation: " + op);
            }
        }
        throw new RuntimeException("Unknown packet " + ChunkHandler.name(type));
    }

    private Chunk handleOpenGlTrace(Chunk request) {
        ByteBuffer in = wrapChunk(request);
        GLUtils.setTracingLevel(in.getInt());
        return null;
    }

    private Chunk listWindows() {
        String[] windowNames = WindowManagerGlobal.getInstance().getViewRootNames();
        int responseLength = 4;
        for (String str : windowNames) {
            responseLength = responseLength + 4 + (str.length() * 2);
        }
        ByteBuffer out = ByteBuffer.allocate(responseLength);
        out.order(ChunkHandler.CHUNK_ORDER);
        out.putInt(windowNames.length);
        for (String name : windowNames) {
            out.putInt(name.length());
            putString(out, name);
        }
        return new Chunk(CHUNK_VULW, out);
    }

    private View getRootView(ByteBuffer in) {
        try {
            int viewRootNameLength = in.getInt();
            String viewRootName = getString(in, viewRootNameLength);
            return WindowManagerGlobal.getInstance().getRootView(viewRootName);
        } catch (BufferUnderflowException e) {
            return null;
        }
    }

    private View getTargetView(View root, ByteBuffer in) {
        try {
            int viewLength = in.getInt();
            String viewName = getString(in, viewLength);
            return ViewDebug.findView(root, viewName);
        } catch (BufferUnderflowException e) {
            return null;
        }
    }

    private Chunk dumpHierarchy(View rootView, ByteBuffer in) throws Throwable {
        boolean skipChildren = in.getInt() > 0;
        boolean includeProperties = in.getInt() > 0;
        ByteArrayOutputStream b = new ByteArrayOutputStream(1024);
        try {
            ViewDebug.dump(rootView, skipChildren, includeProperties, b);
            byte[] data = b.toByteArray();
            return new Chunk(CHUNK_VURT, data, 0, data.length);
        } catch (IOException e) {
            return createFailChunk(1, "Unexpected error while obtaining view hierarchy: " + e.getMessage());
        }
    }

    private Chunk captureLayers(View rootView) {
        Chunk chunkCreateFailChunk;
        ByteArrayOutputStream b = new ByteArrayOutputStream(1024);
        DataOutputStream dos = new DataOutputStream(b);
        try {
            try {
                ViewDebug.captureLayers(rootView, dos);
                try {
                    dos.close();
                } catch (IOException e) {
                }
                byte[] data = b.toByteArray();
                chunkCreateFailChunk = new Chunk(CHUNK_VURT, data, 0, data.length);
            } finally {
                try {
                    dos.close();
                } catch (IOException e2) {
                }
            }
        } catch (IOException e3) {
            chunkCreateFailChunk = createFailChunk(1, "Unexpected error while obtaining view hierarchy: " + e3.getMessage());
        }
        return chunkCreateFailChunk;
    }

    private Chunk dumpTheme(View rootView) throws Throwable {
        ByteArrayOutputStream b = new ByteArrayOutputStream(1024);
        try {
            ViewDebug.dumpTheme(rootView, b);
            byte[] data = b.toByteArray();
            return new Chunk(CHUNK_VURT, data, 0, data.length);
        } catch (IOException e) {
            return createFailChunk(1, "Unexpected error while dumping the theme: " + e.getMessage());
        }
    }

    private Chunk captureView(View rootView, View targetView) throws Throwable {
        ByteArrayOutputStream b = new ByteArrayOutputStream(1024);
        try {
            ViewDebug.capture(rootView, b, targetView);
            byte[] data = b.toByteArray();
            return new Chunk(CHUNK_VUOP, data, 0, data.length);
        } catch (IOException e) {
            return createFailChunk(1, "Unexpected error while capturing view: " + e.getMessage());
        }
    }

    private Chunk dumpDisplayLists(final View rootView, final View targetView) {
        rootView.post(new Runnable() {
            @Override
            public void run() {
                ViewDebug.outputDisplayList(rootView, targetView);
            }
        });
        return null;
    }

    private Chunk invokeViewMethod(View rootView, View targetView, ByteBuffer in) {
        Class<?>[] argTypes;
        Object[] args;
        int l = in.getInt();
        String methodName = getString(in, l);
        if (!in.hasRemaining()) {
            argTypes = new Class[0];
            args = new Object[0];
        } else {
            int nArgs = in.getInt();
            argTypes = new Class[nArgs];
            args = new Object[nArgs];
            for (int i = 0; i < nArgs; i++) {
                char c = in.getChar();
                switch (c) {
                    case 'B':
                        argTypes[i] = Byte.TYPE;
                        args[i] = Byte.valueOf(in.get());
                        break;
                    case 'C':
                        argTypes[i] = Character.TYPE;
                        args[i] = Character.valueOf(in.getChar());
                        break;
                    case 'D':
                        argTypes[i] = Double.TYPE;
                        args[i] = Double.valueOf(in.getDouble());
                        break;
                    case 'F':
                        argTypes[i] = Float.TYPE;
                        args[i] = Float.valueOf(in.getFloat());
                        break;
                    case 'I':
                        argTypes[i] = Integer.TYPE;
                        args[i] = Integer.valueOf(in.getInt());
                        break;
                    case 'J':
                        argTypes[i] = Long.TYPE;
                        args[i] = Long.valueOf(in.getLong());
                        break;
                    case 'S':
                        argTypes[i] = Short.TYPE;
                        args[i] = Short.valueOf(in.getShort());
                        break;
                    case 'Z':
                        argTypes[i] = Boolean.TYPE;
                        args[i] = Boolean.valueOf(in.get() != 0);
                        break;
                    default:
                        Log.e(TAG, "arg " + i + ", unrecognized type: " + c);
                        return createFailChunk(-2, "Unsupported parameter type (" + c + ") to invoke view method.");
                }
            }
        }
        try {
            Method method = targetView.getClass().getMethod(methodName, argTypes);
            try {
                ViewDebug.invokeViewMethod(targetView, method, args);
                return null;
            } catch (Exception e) {
                Log.e(TAG, "Exception while invoking method: " + e.getCause().getMessage());
                String msg = e.getCause().getMessage();
                if (msg == null) {
                    msg = e.getCause().toString();
                }
                return createFailChunk(-3, msg);
            }
        } catch (NoSuchMethodException e2) {
            Log.e(TAG, "No such method: " + e2.getMessage());
            return createFailChunk(-2, "No such method: " + e2.getMessage());
        }
    }

    private Chunk setLayoutParameter(View rootView, View targetView, ByteBuffer in) {
        int l = in.getInt();
        String param = getString(in, l);
        int value = in.getInt();
        try {
            ViewDebug.setLayoutParameter(targetView, param, value);
            return null;
        } catch (Exception e) {
            Log.e(TAG, "Exception setting layout parameter: " + e);
            return createFailChunk(-3, "Error accessing field " + param + ":" + e.getMessage());
        }
    }

    private Chunk profileView(View rootView, View targetView) {
        ByteArrayOutputStream b = new ByteArrayOutputStream(32768);
        BufferedWriter bw = new BufferedWriter(new OutputStreamWriter(b), 32768);
        try {
            try {
                ViewDebug.profileViewAndChildren(targetView, bw);
                byte[] data = b.toByteArray();
                return new Chunk(CHUNK_VUOP, data, 0, data.length);
            } catch (IOException e) {
                Chunk chunkCreateFailChunk = createFailChunk(1, "Unexpected error while profiling view: " + e.getMessage());
                try {
                    bw.close();
                    return chunkCreateFailChunk;
                } catch (IOException e2) {
                    return chunkCreateFailChunk;
                }
            }
        } finally {
            try {
                bw.close();
            } catch (IOException e3) {
            }
        }
    }
}
