package com.ferg.afergulator;

import android.app.Application;

import java.io.Closeable;
import java.io.IOException;

import timber.log.Timber;

public class App extends Application {

    private static final Timber.Tree LOG = new Timber.DebugTree();

    @Override
    public void onCreate() {
        super.onCreate();

        Timber.uprootAll();
        Timber.plant(LOG);
    }

    static void closeSilently(Closeable aCloseable) {
        if (aCloseable != null) {
            try {
                aCloseable.close();
            } catch (IOException ignored) { }
        }
    }

}
