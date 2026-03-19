package com.android.internal.util;

import android.os.Message;

public class State implements IState {
    protected State() {
    }

    @Override
    public void enter() {
    }

    @Override
    public void exit() {
    }

    @Override
    public boolean processMessage(Message message) {
        return false;
    }

    @Override
    public String getName() {
        String name = getClass().getName();
        return name.substring(name.lastIndexOf(36) + 1);
    }
}
