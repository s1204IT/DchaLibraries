package com.android.documentsui.roots;

import android.util.Log;
import com.android.documentsui.base.MimeTypes;
import com.android.documentsui.base.RootInfo;
import com.android.documentsui.base.SharedMinimal;
import com.android.documentsui.base.State;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

public interface ProvidersAccess {
    RootInfo getDefaultRootBlocking(State state);

    Collection<RootInfo> getMatchingRootsBlocking(State state);

    String getPackageName(String str);

    RootInfo getRecentsRoot();

    RootInfo getRootOneshot(String str, String str2);

    Collection<RootInfo> getRootsBlocking();

    static List<RootInfo> getMatchingRoots(Collection<RootInfo> collection, State state) {
        ArrayList arrayList = new ArrayList();
        for (RootInfo rootInfo : collection) {
            if (SharedMinimal.VERBOSE) {
                Log.v("ProvidersAccess", "Evaluationg root: " + rootInfo);
            }
            if (state.action == 4 && !rootInfo.supportsCreate()) {
                if (SharedMinimal.VERBOSE) {
                    Log.v("ProvidersAccess", "Excluding read-only root because: ACTION_CREATE.");
                }
            } else if (state.action == 2 && !rootInfo.supportsCreate()) {
                if (SharedMinimal.VERBOSE) {
                    Log.v("ProvidersAccess", "Excluding read-only root because: ACTION_PICK_COPY_DESTINATION.");
                }
            } else if (state.action == 6 && !rootInfo.supportsChildren()) {
                if (SharedMinimal.VERBOSE) {
                    Log.v("ProvidersAccess", "Excluding root !supportsChildren because: ACTION_OPEN_TREE.");
                }
            } else if (!state.showAdvanced && rootInfo.isAdvanced()) {
                if (SharedMinimal.VERBOSE) {
                    Log.v("ProvidersAccess", "Excluding root because: unwanted advanced device.");
                }
            } else if (state.localOnly && !rootInfo.isLocalOnly()) {
                if (SharedMinimal.VERBOSE) {
                    Log.v("ProvidersAccess", "Excluding root because: unwanted non-local device.");
                }
            } else if (state.directoryCopy && rootInfo.isDownloads()) {
                if (SharedMinimal.VERBOSE) {
                    Log.v("ProvidersAccess", "Excluding downloads root because: unsupported directory copy.");
                }
            } else if (state.action == 3 && rootInfo.isEmpty()) {
                if (SharedMinimal.VERBOSE) {
                    Log.v("ProvidersAccess", "Excluding empty root because: ACTION_OPEN.");
                }
            } else if (state.action == 5 && rootInfo.isEmpty()) {
                if (SharedMinimal.VERBOSE) {
                    Log.v("ProvidersAccess", "Excluding empty root because: ACTION_GET_CONTENT.");
                }
            } else if (!(MimeTypes.mimeMatches(rootInfo.derivedMimeTypes, state.acceptMimes) || MimeTypes.mimeMatches(state.acceptMimes, rootInfo.derivedMimeTypes))) {
                if (SharedMinimal.VERBOSE) {
                    Log.v("ProvidersAccess", "Excluding root because: unsupported content types > " + Arrays.toString(state.acceptMimes));
                }
            } else if (state.excludedAuthorities.contains(rootInfo.authority)) {
                if (SharedMinimal.VERBOSE) {
                    Log.v("ProvidersAccess", "Excluding root because: owned by calling package.");
                }
            } else {
                arrayList.add(rootInfo);
            }
        }
        if (SharedMinimal.DEBUG) {
            Log.d("ProvidersAccess", "Matched roots: " + arrayList);
        }
        return arrayList;
    }
}
