package org.gsma.joyn;

import org.gsma.joyn.IJoynServiceRegistrationListener;

public abstract class JoynServiceRegistrationListener extends IJoynServiceRegistrationListener.Stub {
    @Override
    public abstract void onServiceRegistered();

    @Override
    public abstract void onServiceUnregistered();
}
