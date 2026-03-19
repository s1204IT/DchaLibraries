package sun.net;

import java.net.URL;
import java.util.ArrayList;
import java.util.Iterator;

public class ProgressMonitor {
    private static ProgressMeteringPolicy meteringPolicy = new DefaultProgressMeteringPolicy();
    private static ProgressMonitor pm = new ProgressMonitor();
    private ArrayList<ProgressSource> progressSourceList = new ArrayList<>();
    private ArrayList<ProgressListener> progressListenerList = new ArrayList<>();

    public static synchronized ProgressMonitor getDefault() {
        return pm;
    }

    public static synchronized void setDefault(ProgressMonitor progressMonitor) {
        if (progressMonitor != null) {
            pm = progressMonitor;
        }
    }

    public static synchronized void setMeteringPolicy(ProgressMeteringPolicy progressMeteringPolicy) {
        if (progressMeteringPolicy != null) {
            meteringPolicy = progressMeteringPolicy;
        }
    }

    public ArrayList<ProgressSource> getProgressSources() {
        ArrayList<ProgressSource> arrayList = new ArrayList<>();
        try {
            synchronized (this.progressSourceList) {
                Iterator<ProgressSource> it = this.progressSourceList.iterator();
                while (it.hasNext()) {
                    arrayList.add((ProgressSource) it.next().clone());
                }
            }
        } catch (CloneNotSupportedException e) {
            e.printStackTrace();
        }
        return arrayList;
    }

    public synchronized int getProgressUpdateThreshold() {
        return meteringPolicy.getProgressUpdateThreshold();
    }

    public boolean shouldMeterInput(URL url, String str) {
        return meteringPolicy.shouldMeterInput(url, str);
    }

    public void registerSource(ProgressSource progressSource) {
        synchronized (this.progressSourceList) {
            if (this.progressSourceList.contains(progressSource)) {
                return;
            }
            this.progressSourceList.add(progressSource);
            if (this.progressListenerList.size() > 0) {
                ArrayList arrayList = new ArrayList();
                synchronized (this.progressListenerList) {
                    Iterator<ProgressListener> it = this.progressListenerList.iterator();
                    while (it.hasNext()) {
                        arrayList.add(it.next());
                    }
                }
                Iterator it2 = arrayList.iterator();
                while (it2.hasNext()) {
                    ((ProgressListener) it2.next()).progressStart(new ProgressEvent(progressSource, progressSource.getURL(), progressSource.getMethod(), progressSource.getContentType(), progressSource.getState(), progressSource.getProgress(), progressSource.getExpected()));
                }
            }
        }
    }

    public void unregisterSource(ProgressSource progressSource) {
        synchronized (this.progressSourceList) {
            if (this.progressSourceList.contains(progressSource)) {
                progressSource.close();
                this.progressSourceList.remove(progressSource);
                if (this.progressListenerList.size() > 0) {
                    ArrayList arrayList = new ArrayList();
                    synchronized (this.progressListenerList) {
                        Iterator<ProgressListener> it = this.progressListenerList.iterator();
                        while (it.hasNext()) {
                            arrayList.add(it.next());
                        }
                    }
                    Iterator it2 = arrayList.iterator();
                    while (it2.hasNext()) {
                        ((ProgressListener) it2.next()).progressFinish(new ProgressEvent(progressSource, progressSource.getURL(), progressSource.getMethod(), progressSource.getContentType(), progressSource.getState(), progressSource.getProgress(), progressSource.getExpected()));
                    }
                }
            }
        }
    }

    public void updateProgress(ProgressSource progressSource) {
        synchronized (this.progressSourceList) {
            if (this.progressSourceList.contains(progressSource)) {
                if (this.progressListenerList.size() > 0) {
                    ArrayList arrayList = new ArrayList();
                    synchronized (this.progressListenerList) {
                        Iterator<ProgressListener> it = this.progressListenerList.iterator();
                        while (it.hasNext()) {
                            arrayList.add(it.next());
                        }
                    }
                    Iterator it2 = arrayList.iterator();
                    while (it2.hasNext()) {
                        ((ProgressListener) it2.next()).progressUpdate(new ProgressEvent(progressSource, progressSource.getURL(), progressSource.getMethod(), progressSource.getContentType(), progressSource.getState(), progressSource.getProgress(), progressSource.getExpected()));
                    }
                }
            }
        }
    }

    public void addProgressListener(ProgressListener progressListener) {
        synchronized (this.progressListenerList) {
            this.progressListenerList.add(progressListener);
        }
    }

    public void removeProgressListener(ProgressListener progressListener) {
        synchronized (this.progressListenerList) {
            this.progressListenerList.remove(progressListener);
        }
    }
}
