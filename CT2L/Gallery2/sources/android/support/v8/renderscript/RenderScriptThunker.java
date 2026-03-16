package android.support.v8.renderscript;

import android.content.Context;
import android.renderscript.RenderScript;
import android.support.v8.renderscript.RenderScript;
import java.lang.reflect.Method;

class RenderScriptThunker extends RenderScript {
    android.renderscript.RenderScript mN;

    @Override
    void validate() {
        if (this.mN == null) {
            throw new RSInvalidStateException("Calling RS with no Context active.");
        }
    }

    @Override
    public void setPriority(RenderScript.Priority p) {
        try {
            if (p == RenderScript.Priority.LOW) {
                this.mN.setPriority(RenderScript.Priority.LOW);
            }
            if (p == RenderScript.Priority.NORMAL) {
                this.mN.setPriority(RenderScript.Priority.NORMAL);
            }
        } catch (android.renderscript.RSRuntimeException e) {
            throw ExceptionThunker.convertException(e);
        }
    }

    RenderScriptThunker(Context ctx) {
        super(ctx);
        isNative = true;
    }

    public static RenderScript create(Context ctx, int sdkVersion) {
        try {
            RenderScriptThunker rs = new RenderScriptThunker(ctx);
            Class<?> javaRS = Class.forName("android.renderscript.RenderScript");
            Class<?>[] clsArr = {Context.class, Integer.TYPE};
            Object[] args = {ctx, new Integer(sdkVersion)};
            Method create = javaRS.getDeclaredMethod("create", clsArr);
            rs.mN = (android.renderscript.RenderScript) create.invoke(null, args);
            return rs;
        } catch (android.renderscript.RSRuntimeException e) {
            throw ExceptionThunker.convertException(e);
        } catch (Exception e2) {
            throw new RSRuntimeException("Failure to create platform RenderScript context");
        }
    }

    @Override
    public void contextDump() {
        try {
            this.mN.contextDump();
        } catch (android.renderscript.RSRuntimeException e) {
            throw ExceptionThunker.convertException(e);
        }
    }

    @Override
    public void finish() {
        try {
            this.mN.finish();
        } catch (android.renderscript.RSRuntimeException e) {
            throw ExceptionThunker.convertException(e);
        }
    }

    @Override
    public void destroy() {
        try {
            this.mN.destroy();
            this.mN = null;
        } catch (android.renderscript.RSRuntimeException e) {
            throw ExceptionThunker.convertException(e);
        }
    }

    @Override
    public void setMessageHandler(RenderScript.RSMessageHandler msg) {
        this.mMessageCallback = msg;
        try {
            RenderScript.RSMessageHandler handler = new RenderScript.RSMessageHandler() {
                @Override
                public void run() {
                    RenderScriptThunker.this.mMessageCallback.mData = this.mData;
                    RenderScriptThunker.this.mMessageCallback.mID = this.mID;
                    RenderScriptThunker.this.mMessageCallback.mLength = this.mLength;
                    RenderScriptThunker.this.mMessageCallback.run();
                }
            };
            this.mN.setMessageHandler(handler);
        } catch (android.renderscript.RSRuntimeException e) {
            throw ExceptionThunker.convertException(e);
        }
    }

    @Override
    public void setErrorHandler(RenderScript.RSErrorHandler msg) {
        this.mErrorCallback = msg;
        try {
            RenderScript.RSErrorHandler handler = new RenderScript.RSErrorHandler() {
                @Override
                public void run() {
                    RenderScriptThunker.this.mErrorCallback.mErrorMessage = this.mErrorMessage;
                    RenderScriptThunker.this.mErrorCallback.mErrorNum = this.mErrorNum;
                    RenderScriptThunker.this.mErrorCallback.run();
                }
            };
            this.mN.setErrorHandler(handler);
        } catch (android.renderscript.RSRuntimeException e) {
            throw ExceptionThunker.convertException(e);
        }
    }

    boolean equals(Object obj1, Object obj2) {
        if (obj2 instanceof BaseObj) {
            return ((android.renderscript.BaseObj) obj1).equals(((BaseObj) obj2).getNObj());
        }
        return false;
    }
}
