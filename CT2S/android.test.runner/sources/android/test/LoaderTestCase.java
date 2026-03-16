package android.test;

import android.content.Loader;
import android.os.AsyncTask;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import java.util.concurrent.ArrayBlockingQueue;

public class LoaderTestCase extends AndroidTestCase {
    static {
        new AsyncTask<Void, Void, Void>() {
            @Override
            protected Void doInBackground(Void... args) {
                return null;
            }

            @Override
            protected void onPostExecute(Void result) {
            }
        };
    }

    public <T> T getLoaderResultSynchronously(final Loader<T> loader) {
        final ArrayBlockingQueue<T> queue = new ArrayBlockingQueue<>(1);
        final Loader.OnLoadCompleteListener<T> listener = new Loader.OnLoadCompleteListener<T>() {
            @Override
            public void onLoadComplete(Loader<T> completedLoader, T data) {
                completedLoader.unregisterListener(this);
                completedLoader.stopLoading();
                completedLoader.reset();
                queue.add(data);
            }
        };
        Handler mainThreadHandler = new Handler(Looper.getMainLooper()) {
            @Override
            public void handleMessage(Message msg) {
                loader.registerListener(0, listener);
                loader.startLoading();
            }
        };
        mainThreadHandler.sendEmptyMessage(0);
        try {
            T result = queue.take();
            return result;
        } catch (InterruptedException e) {
            throw new RuntimeException("waiting thread interrupted", e);
        }
    }
}
