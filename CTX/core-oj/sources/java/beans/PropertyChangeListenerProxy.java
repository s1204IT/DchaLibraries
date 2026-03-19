package java.beans;

import java.util.EventListenerProxy;

public class PropertyChangeListenerProxy extends EventListenerProxy<PropertyChangeListener> implements PropertyChangeListener {
    private final String propertyName;

    public PropertyChangeListenerProxy(String str, PropertyChangeListener propertyChangeListener) {
        super(propertyChangeListener);
        this.propertyName = str;
    }

    @Override
    public void propertyChange(PropertyChangeEvent propertyChangeEvent) {
        getListener().propertyChange(propertyChangeEvent);
    }

    public String getPropertyName() {
        return this.propertyName;
    }
}
