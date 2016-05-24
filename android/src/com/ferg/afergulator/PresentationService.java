package com.ferg.afergulator;

import android.view.Display;
import android.view.WindowManager;

import com.google.android.gms.cast.CastRemoteDisplayLocalService;
import timber.log.Timber;

public class PresentationService extends CastRemoteDisplayLocalService {

    private GamePresentation mPresentation;

    @Override public void onCreatePresentation(Display display) {
        createPresentation(display);
    }

    @Override public void onDismissPresentation() {
        dismissPresentation();
    }

    private void createPresentation(Display display) {
        dismissPresentation();
        mPresentation = new GamePresentation(this, display);
        try {
            mPresentation.show();
        } catch (WindowManager.InvalidDisplayException ex) {
            Timber.e(ex, "Unable to show presentation, display was removed.");
            dismissPresentation();
        }
    }

    private void dismissPresentation() {
        if (mPresentation != null) {
            mPresentation.dismiss();
            mPresentation = null;
        }
    }

}