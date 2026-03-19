package android.gesture;

import android.content.Context;
import android.util.Log;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.lang.ref.WeakReference;

public final class GestureLibraries {
    private GestureLibraries() {
    }

    public static GestureLibrary fromFile(String str) {
        return fromFile(new File(str));
    }

    public static GestureLibrary fromFile(File file) {
        return new FileGestureLibrary(file);
    }

    public static GestureLibrary fromPrivateFile(Context context, String str) {
        return fromFile(context.getFileStreamPath(str));
    }

    public static GestureLibrary fromRawResource(Context context, int i) {
        return new ResourceGestureLibrary(context, i);
    }

    private static class FileGestureLibrary extends GestureLibrary {
        private final File mPath;

        public FileGestureLibrary(File file) {
            this.mPath = file;
        }

        @Override
        public boolean isReadOnly() {
            return !this.mPath.canWrite();
        }

        @Override
        public boolean save() throws Throwable {
            if (!this.mStore.hasChanged()) {
                return true;
            }
            File file = this.mPath;
            File parentFile = file.getParentFile();
            if (!parentFile.exists() && !parentFile.mkdirs()) {
                return false;
            }
            try {
                file.createNewFile();
                this.mStore.save(new FileOutputStream(file), true);
                return true;
            } catch (FileNotFoundException e) {
                Log.d(GestureConstants.LOG_TAG, "Could not save the gesture library in " + this.mPath, e);
                return false;
            } catch (IOException e2) {
                Log.d(GestureConstants.LOG_TAG, "Could not save the gesture library in " + this.mPath, e2);
                return false;
            }
        }

        @Override
        public boolean load() throws Throwable {
            File file = this.mPath;
            if (file.exists() && file.canRead()) {
                try {
                    this.mStore.load(new FileInputStream(file), true);
                    return true;
                } catch (FileNotFoundException e) {
                    Log.d(GestureConstants.LOG_TAG, "Could not load the gesture library from " + this.mPath, e);
                } catch (IOException e2) {
                    Log.d(GestureConstants.LOG_TAG, "Could not load the gesture library from " + this.mPath, e2);
                }
            }
            return false;
        }
    }

    private static class ResourceGestureLibrary extends GestureLibrary {
        private final WeakReference<Context> mContext;
        private final int mResourceId;

        public ResourceGestureLibrary(Context context, int i) {
            this.mContext = new WeakReference<>(context);
            this.mResourceId = i;
        }

        @Override
        public boolean isReadOnly() {
            return true;
        }

        @Override
        public boolean save() {
            return false;
        }

        @Override
        public boolean load() throws Throwable {
            Context context = this.mContext.get();
            if (context != null) {
                try {
                    this.mStore.load(context.getResources().openRawResource(this.mResourceId), true);
                    return true;
                } catch (IOException e) {
                    Log.d(GestureConstants.LOG_TAG, "Could not load the gesture library from raw resource " + context.getResources().getResourceName(this.mResourceId), e);
                }
            }
            return false;
        }
    }
}
