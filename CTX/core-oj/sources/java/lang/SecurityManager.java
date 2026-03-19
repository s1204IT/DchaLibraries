package java.lang;

import java.io.FileDescriptor;
import java.net.InetAddress;
import java.security.Permission;

public class SecurityManager {

    @Deprecated
    protected boolean inCheck;

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
    protected Class<?> currentLoadedClass() {
        return null;
    }

    @Deprecated
    protected int classDepth(String str) {
        return -1;
    }

    @Deprecated
    protected int classLoaderDepth() {
        return -1;
    }

    @Deprecated
    protected boolean inClass(String str) {
        return false;
    }

    @Deprecated
    protected boolean inClassLoader() {
        return false;
    }

    public Object getSecurityContext() {
        return null;
    }

    public void checkPermission(Permission permission) {
    }

    public void checkPermission(Permission permission, Object obj) {
    }

    public void checkCreateClassLoader() {
    }

    public void checkAccess(Thread thread) {
    }

    public void checkAccess(ThreadGroup threadGroup) {
    }

    public void checkExit(int i) {
    }

    public void checkExec(String str) {
    }

    public void checkLink(String str) {
    }

    public void checkRead(FileDescriptor fileDescriptor) {
    }

    public void checkRead(String str) {
    }

    public void checkRead(String str, Object obj) {
    }

    public void checkWrite(FileDescriptor fileDescriptor) {
    }

    public void checkWrite(String str) {
    }

    public void checkDelete(String str) {
    }

    public void checkConnect(String str, int i) {
    }

    public void checkConnect(String str, int i, Object obj) {
    }

    public void checkListen(int i) {
    }

    public void checkAccept(String str, int i) {
    }

    public void checkMulticast(InetAddress inetAddress) {
    }

    @Deprecated
    public void checkMulticast(InetAddress inetAddress, byte b) {
    }

    public void checkPropertiesAccess() {
    }

    public void checkPropertyAccess(String str) {
    }

    public boolean checkTopLevelWindow(Object obj) {
        return true;
    }

    public void checkPrintJobAccess() {
    }

    public void checkSystemClipboardAccess() {
    }

    public void checkAwtEventQueueAccess() {
    }

    public void checkPackageAccess(String str) {
    }

    public void checkPackageDefinition(String str) {
    }

    public void checkSetFactory() {
    }

    public void checkMemberAccess(Class<?> cls, int i) {
    }

    public void checkSecurityAccess(String str) {
    }

    public ThreadGroup getThreadGroup() {
        return Thread.currentThread().getThreadGroup();
    }
}
