package com.android.server;

import android.R;
import android.content.Context;
import android.os.Binder;
import android.system.ErrnoException;
import android.system.Os;
import android.system.OsConstants;
import android.system.StructStat;
import android.util.Slog;
import java.io.FileDescriptor;
import java.io.PrintWriter;
import java.util.ArrayList;

public final class PinnerService extends SystemService {
    private static final boolean DEBUG = false;
    private static final String TAG = "PinnerService";
    private BinderService mBinderService;
    private final Context mContext;
    private final ArrayList<String> mPinnedFiles;

    public PinnerService(Context context) {
        super(context);
        this.mPinnedFiles = new ArrayList<>();
        this.mContext = context;
    }

    @Override
    public void onStart() {
        Slog.e(TAG, "Starting PinnerService");
        this.mBinderService = new BinderService(this, null);
        publishBinderService("pinner", this.mBinderService);
        String[] filesToPin = this.mContext.getResources().getStringArray(R.array.config_deviceTabletopRotations);
        for (int i = 0; i < filesToPin.length; i++) {
            boolean success = pinFile(filesToPin[i], 0L, 0L);
            if (success) {
                this.mPinnedFiles.add(filesToPin[i]);
                Slog.i(TAG, "Pinned file = " + filesToPin[i]);
            } else {
                Slog.e(TAG, "Failed to pin file = " + filesToPin[i]);
            }
        }
    }

    private boolean pinFile(String fileToPin, long offset, long length) {
        FileDescriptor fd = new FileDescriptor();
        try {
            FileDescriptor fd2 = Os.open(fileToPin, OsConstants.O_RDONLY | OsConstants.O_CLOEXEC | OsConstants.O_NOFOLLOW, OsConstants.O_RDONLY);
            StructStat sb = Os.fstat(fd2);
            if (offset + length > sb.st_size) {
                Os.close(fd2);
                return false;
            }
            if (length == 0) {
                length = sb.st_size - offset;
            }
            long address = Os.mmap(0L, length, OsConstants.PROT_READ, OsConstants.MAP_PRIVATE, fd2, offset);
            Os.close(fd2);
            Os.mlock(address, length);
            return true;
        } catch (ErrnoException e) {
            Slog.e(TAG, "Failed to pin file " + fileToPin + " with error " + e.getMessage());
            if (fd.valid()) {
                try {
                    Os.close(fd);
                    return false;
                } catch (ErrnoException eClose) {
                    Slog.e(TAG, "Failed to close fd, error = " + eClose.getMessage());
                    return false;
                }
            }
            return false;
        }
    }

    private final class BinderService extends Binder {
        BinderService(PinnerService this$0, BinderService binderService) {
            this();
        }

        private BinderService() {
        }

        @Override
        protected void dump(FileDescriptor fd, PrintWriter pw, String[] args) {
            PinnerService.this.mContext.enforceCallingOrSelfPermission("android.permission.DUMP", PinnerService.TAG);
            pw.println("Pinned Files:");
            for (int i = 0; i < PinnerService.this.mPinnedFiles.size(); i++) {
                pw.println((String) PinnerService.this.mPinnedFiles.get(i));
            }
        }
    }
}
