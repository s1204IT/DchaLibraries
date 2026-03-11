package android.arch.lifecycle;

public class MutableLiveData<T> extends LiveData<T> {
    @Override
    public void setValue(T t) {
        super.setValue(t);
    }
}
