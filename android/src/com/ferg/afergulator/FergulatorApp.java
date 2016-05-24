package com.ferg.afergulator;

import android.app.Application;

import timber.log.Timber;


public class FergulatorApp extends Application {

    @Override public void onCreate() {
        super.onCreate();

        Timber.uprootAll();
        if (BuildConfig.DEBUG) {
            Timber.plant(new Timber.DebugTree());
        }
    }
}
