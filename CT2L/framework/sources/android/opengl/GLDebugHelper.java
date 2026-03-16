package android.opengl;

import java.io.Writer;
import javax.microedition.khronos.egl.EGL;
import javax.microedition.khronos.opengles.GL;

public class GLDebugHelper {
    public static final int CONFIG_CHECK_GL_ERROR = 1;
    public static final int CONFIG_CHECK_THREAD = 2;
    public static final int CONFIG_LOG_ARGUMENT_NAMES = 4;
    public static final int ERROR_WRONG_THREAD = 28672;

    public static GL wrap(GL gl, int configFlags, Writer log) {
        GL gl2 = configFlags != 0 ? new GLErrorWrapper(gl, configFlags) : gl;
        if (log == null) {
            return gl2;
        }
        boolean logArgumentNames = (configFlags & 4) != 0;
        return new GLLogWrapper(gl2, log, logArgumentNames);
    }

    public static EGL wrap(EGL egl, int configFlags, Writer log) {
        if (log != null) {
            return new EGLLogWrapper(egl, configFlags, log);
        }
        return egl;
    }
}
