package com.android.uiautomator.testrunner;

import android.os.Bundle;
import android.os.IBinder;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.os.SystemClock;
import android.view.inputmethod.InputMethodInfo;
import com.android.internal.view.IInputMethodManager;
import com.android.uiautomator.core.UiDevice;
import junit.framework.TestCase;

@Deprecated
public class UiAutomatorTestCase extends TestCase {
    private static final String DISABLE_IME = "disable_ime";
    private static final String DUMMY_IME_PACKAGE = "com.android.testing.dummyime";
    private IAutomationSupport mAutomationSupport;
    private Bundle mParams;
    private boolean mShouldDisableIme = false;
    private UiDevice mUiDevice;

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        this.mShouldDisableIme = "true".equals(this.mParams.getString(DISABLE_IME));
        if (this.mShouldDisableIme) {
            setDummyIme();
        }
    }

    @Override
    protected void tearDown() throws Exception {
        if (this.mShouldDisableIme) {
            restoreActiveIme();
        }
        super.tearDown();
    }

    public UiDevice getUiDevice() {
        return this.mUiDevice;
    }

    public Bundle getParams() {
        return this.mParams;
    }

    public IAutomationSupport getAutomationSupport() {
        return this.mAutomationSupport;
    }

    void setUiDevice(UiDevice uiDevice) {
        this.mUiDevice = uiDevice;
    }

    void setParams(Bundle bundle) {
        this.mParams = bundle;
    }

    void setAutomationSupport(IAutomationSupport iAutomationSupport) {
        this.mAutomationSupport = iAutomationSupport;
    }

    public void sleep(long j) {
        SystemClock.sleep(j);
    }

    private void setDummyIme() throws RemoteException {
        IInputMethodManager iInputMethodManagerAsInterface = IInputMethodManager.Stub.asInterface(ServiceManager.getService("input_method"));
        String id = null;
        for (InputMethodInfo inputMethodInfo : iInputMethodManagerAsInterface.getInputMethodList()) {
            if (DUMMY_IME_PACKAGE.equals(inputMethodInfo.getComponent().getPackageName())) {
                id = inputMethodInfo.getId();
            }
        }
        if (id == null) {
            throw new RuntimeException(String.format("Required testing fixture missing: IME package (%s)", DUMMY_IME_PACKAGE));
        }
        iInputMethodManagerAsInterface.setInputMethod((IBinder) null, id);
    }

    private void restoreActiveIme() throws RemoteException {
    }
}
