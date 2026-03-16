package java.util;

public class Observable {
    List<Observer> observers = new ArrayList();
    boolean changed = false;

    public void addObserver(Observer observer) {
        if (observer == null) {
            throw new NullPointerException("observer == null");
        }
        synchronized (this) {
            if (!this.observers.contains(observer)) {
                this.observers.add(observer);
            }
        }
    }

    protected void clearChanged() {
        this.changed = false;
    }

    public int countObservers() {
        return this.observers.size();
    }

    public synchronized void deleteObserver(Observer observer) {
        this.observers.remove(observer);
    }

    public synchronized void deleteObservers() {
        this.observers.clear();
    }

    public boolean hasChanged() {
        return this.changed;
    }

    public void notifyObservers() {
        notifyObservers(null);
    }

    public void notifyObservers(Object data) {
        Observer[] arrays = null;
        synchronized (this) {
            if (hasChanged()) {
                clearChanged();
                int size = this.observers.size();
                arrays = new Observer[size];
                this.observers.toArray(arrays);
            }
        }
        if (arrays != null) {
            Observer[] arr$ = arrays;
            for (Observer observer : arr$) {
                observer.update(this, data);
            }
        }
    }

    protected void setChanged() {
        this.changed = true;
    }
}
