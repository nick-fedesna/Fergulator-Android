package com.ferg.afergulator;

import android.content.Context;
import android.os.Bundle;
import android.view.Display;

import com.google.android.gms.cast.CastPresentation;

class GamePresentation extends CastPresentation {

    public GamePresentation(Context context, Display display) {
        super(context, display);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.cast_layout);
    }
}