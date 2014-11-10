package com.ferg.afergulator;

import android.app.*;
import android.content.Intent;
import android.os.Bundle;
import android.view.*;
import android.widget.*;

import com.google.android.glass.app.Card;
import com.google.android.glass.widget.CardScrollAdapter;
import com.google.android.glass.widget.CardScrollView;

import java.io.IOException;
import java.util.ArrayList;

public class MainActivity extends Activity {

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        RomAdapter adapter = new RomAdapter();

        CardScrollView scrollView = new CardScrollView(this);
        scrollView.setAdapter(adapter);
        scrollView.setOnItemClickListener(adapter);
        scrollView.activate();

        setContentView(scrollView);
    }

    private class RomAdapter extends CardScrollAdapter implements AdapterView.OnItemClickListener {

        ArrayList<Card> mRoms;
        ArrayList<String> mFiles = new ArrayList<String>();

        public RomAdapter() {
            mRoms = new ArrayList<Card>();

            try {
                String[] roms = getRoms();

                Card card;
                for (String rom : roms) {
                    card = new Card(MainActivity.this);
                    card.setText(displayRomName(rom));
                    mRoms.add(card);
                    mFiles.add(rom);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        @Override
        public int findIdPosition(Object aId) {
            return -1;
        }

        @Override
        public int findItemPosition(Object aItem) {
            return mRoms.indexOf(aItem);
        }

        @Override
        public int getCount() {
            return mRoms.size();
        }

        @Override
        public Object getItem(int aPosition) {
            return mRoms.get(aPosition);
        }

        @Override
        public View getView(int aPosition, View aConvertView, ViewGroup aParent) {
            return mRoms.get(aPosition).toView();
        }

        private String[] getRoms() throws IOException {
            return getAssets().list("roms");
        }

        private String displayRomName(String rom) {
            if (rom.endsWith(".nes")) {
                return rom.substring(0, rom.length() - 4);
            }
            return rom;
        }

        public void onItemClick(AdapterView<?> aPparent, View aView, int aPosition, long aId) {
            String rom = mFiles.get(aPosition);
            Intent i = new Intent(MainActivity.this, GameActivity.class);
            i.putExtra("rom", rom);
            startActivity(i);
        }
    }
}
