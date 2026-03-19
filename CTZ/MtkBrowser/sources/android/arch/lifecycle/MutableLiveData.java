package android.arch.lifecycle;

public class MutableLiveData<T> extends LiveData<T> {
    @Override
    public void setValue(T value) {
        super.setValue(value);
    }
}
