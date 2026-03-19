package java.util;

public class Observable {
    private boolean changed = false;
    private Vector<Observer> obs = new Vector<>();

    public synchronized void addObserver(Observer observer) {
        if (observer == null) {
            throw new NullPointerException();
        }
        if (!this.obs.contains(observer)) {
            this.obs.addElement(observer);
        }
    }

    public synchronized void deleteObserver(Observer observer) {
        this.obs.removeElement(observer);
    }

    public void notifyObservers() {
        notifyObservers(null);
    }

    public void notifyObservers(Object obj) {
        synchronized (this) {
            if (hasChanged()) {
                Object[] array = this.obs.toArray();
                clearChanged();
                for (int length = array.length - 1; length >= 0; length--) {
                    ((Observer) array[length]).update(this, obj);
                }
            }
        }
    }

    public synchronized void deleteObservers() {
        this.obs.removeAllElements();
    }

    protected synchronized void setChanged() {
        this.changed = true;
    }

    protected synchronized void clearChanged() {
        this.changed = false;
    }

    public synchronized boolean hasChanged() {
        return this.changed;
    }

    public synchronized int countObservers() {
        return this.obs.size();
    }
}
