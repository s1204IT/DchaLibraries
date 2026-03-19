package android.filterfw.core;

public class NativeProgram extends Program {
    private boolean mHasGetValueFunction;
    private boolean mHasInitFunction;
    private boolean mHasResetFunction;
    private boolean mHasSetValueFunction;
    private boolean mHasTeardownFunction;
    private boolean mTornDown = false;
    private int nativeProgramId;

    private native boolean allocate();

    private native boolean bindGetValueFunction(String str);

    private native boolean bindInitFunction(String str);

    private native boolean bindProcessFunction(String str);

    private native boolean bindResetFunction(String str);

    private native boolean bindSetValueFunction(String str);

    private native boolean bindTeardownFunction(String str);

    private native String callNativeGetValue(String str);

    private native boolean callNativeInit();

    private native boolean callNativeProcess(NativeFrame[] nativeFrameArr, NativeFrame nativeFrame);

    private native boolean callNativeReset();

    private native boolean callNativeSetValue(String str, String str2);

    private native boolean callNativeTeardown();

    private native boolean deallocate();

    private native boolean nativeInit();

    private native boolean openNativeLibrary(String str);

    public NativeProgram(String nativeLibName, String nativeFunctionPrefix) {
        this.mHasInitFunction = false;
        this.mHasTeardownFunction = false;
        this.mHasSetValueFunction = false;
        this.mHasGetValueFunction = false;
        this.mHasResetFunction = false;
        allocate();
        String fullLibName = "lib" + nativeLibName + ".so";
        if (!openNativeLibrary(fullLibName)) {
            throw new RuntimeException("Could not find native library named '" + fullLibName + "' required for native program!");
        }
        String processFuncName = nativeFunctionPrefix + "_process";
        if (!bindProcessFunction(processFuncName)) {
            throw new RuntimeException("Could not find native program function name " + processFuncName + " in library " + fullLibName + "! This function is required!");
        }
        String initFuncName = nativeFunctionPrefix + "_init";
        this.mHasInitFunction = bindInitFunction(initFuncName);
        String teardownFuncName = nativeFunctionPrefix + "_teardown";
        this.mHasTeardownFunction = bindTeardownFunction(teardownFuncName);
        String setValueFuncName = nativeFunctionPrefix + "_setvalue";
        this.mHasSetValueFunction = bindSetValueFunction(setValueFuncName);
        String getValueFuncName = nativeFunctionPrefix + "_getvalue";
        this.mHasGetValueFunction = bindGetValueFunction(getValueFuncName);
        String resetFuncName = nativeFunctionPrefix + "_reset";
        this.mHasResetFunction = bindResetFunction(resetFuncName);
        if (this.mHasInitFunction && !callNativeInit()) {
            throw new RuntimeException("Could not initialize NativeProgram!");
        }
    }

    public void tearDown() {
        if (this.mTornDown) {
            return;
        }
        if (this.mHasTeardownFunction && !callNativeTeardown()) {
            throw new RuntimeException("Could not tear down NativeProgram!");
        }
        deallocate();
        this.mTornDown = true;
    }

    @Override
    public void reset() {
        if (!this.mHasResetFunction || callNativeReset()) {
        } else {
            throw new RuntimeException("Could not reset NativeProgram!");
        }
    }

    protected void finalize() throws Throwable {
        tearDown();
    }

    @Override
    public void process(Frame[] inputs, Frame output) {
        if (this.mTornDown) {
            throw new RuntimeException("NativeProgram already torn down!");
        }
        NativeFrame[] nativeInputs = new NativeFrame[inputs.length];
        for (int i = 0; i < inputs.length; i++) {
            if (inputs[i] == null || (inputs[i] instanceof NativeFrame)) {
                nativeInputs[i] = (NativeFrame) inputs[i];
            } else {
                throw new RuntimeException("NativeProgram got non-native frame as input " + i + "!");
            }
        }
        if (output == null || (output instanceof NativeFrame)) {
            NativeFrame nativeOutput = (NativeFrame) output;
            if (callNativeProcess(nativeInputs, nativeOutput)) {
                return;
            } else {
                throw new RuntimeException("Calling native process() caused error!");
            }
        }
        throw new RuntimeException("NativeProgram got non-native output frame!");
    }

    @Override
    public void setHostValue(String variableName, Object value) {
        if (this.mTornDown) {
            throw new RuntimeException("NativeProgram already torn down!");
        }
        if (!this.mHasSetValueFunction) {
            throw new RuntimeException("Attempting to set native variable, but native code does not define native setvalue function!");
        }
        if (callNativeSetValue(variableName, value.toString())) {
        } else {
            throw new RuntimeException("Error setting native value for variable '" + variableName + "'!");
        }
    }

    @Override
    public Object getHostValue(String variableName) {
        if (this.mTornDown) {
            throw new RuntimeException("NativeProgram already torn down!");
        }
        if (!this.mHasGetValueFunction) {
            throw new RuntimeException("Attempting to get native variable, but native code does not define native getvalue function!");
        }
        return callNativeGetValue(variableName);
    }

    static {
        System.loadLibrary("filterfw");
    }
}
