package java.lang;

import java.io.FileDescriptor;
import java.net.InetAddress;
import java.security.Permission;

public class SecurityManager {

    @Deprecated
    protected boolean inCheck;

    public void checkAccept(String host, int port) {
    }

    public void checkAccess(Thread thread) {
    }

    public void checkAccess(ThreadGroup group) {
    }

    public void checkConnect(String host, int port) {
    }

    public void checkConnect(String host, int port, Object context) {
    }

    public void checkCreateClassLoader() {
    }

    public void checkDelete(String file) {
    }

    public void checkExec(String cmd) {
    }

    public void checkExit(int status) {
    }

    public void checkLink(String libName) {
    }

    public void checkListen(int port) {
    }

    public void checkMemberAccess(Class<?> cls, int type) {
    }

    public void checkMulticast(InetAddress maddr) {
    }

    @Deprecated
    public void checkMulticast(InetAddress maddr, byte ttl) {
    }

    public void checkPackageAccess(String packageName) {
    }

    public void checkPackageDefinition(String packageName) {
    }

    public void checkPropertiesAccess() {
    }

    public void checkPropertyAccess(String key) {
    }

    public void checkRead(FileDescriptor fd) {
    }

    public void checkRead(String file) {
    }

    public void checkRead(String file, Object context) {
    }

    public void checkSecurityAccess(String target) {
    }

    public void checkSetFactory() {
    }

    public boolean checkTopLevelWindow(Object window) {
        return true;
    }

    public void checkSystemClipboardAccess() {
    }

    public void checkAwtEventQueueAccess() {
    }

    public void checkPrintJobAccess() {
    }

    public void checkWrite(FileDescriptor fd) {
    }

    public void checkWrite(String file) {
    }

    @Deprecated
    public boolean getInCheck() {
        return this.inCheck;
    }

    protected Class[] getClassContext() {
        return null;
    }

    @Deprecated
    protected ClassLoader currentClassLoader() {
        return null;
    }

    @Deprecated
    protected int classLoaderDepth() {
        return -1;
    }

    @Deprecated
    protected Class<?> currentLoadedClass() {
        return null;
    }

    @Deprecated
    protected int classDepth(String name) {
        return -1;
    }

    @Deprecated
    protected boolean inClass(String name) {
        return false;
    }

    @Deprecated
    protected boolean inClassLoader() {
        return false;
    }

    public ThreadGroup getThreadGroup() {
        return Thread.currentThread().getThreadGroup();
    }

    public Object getSecurityContext() {
        return null;
    }

    public void checkPermission(Permission permission) {
    }

    public void checkPermission(Permission permission, Object context) {
    }
}
