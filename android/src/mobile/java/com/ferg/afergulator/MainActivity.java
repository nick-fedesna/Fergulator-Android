package com.ferg.afergulator;

import android.app.ActionBar;
import android.app.Activity;
import android.content.*;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.os.PowerManager;
import android.view.*;
import android.widget.*;

import java.io.*;
import java.util.Set;

import butterknife.*;
import timber.log.Timber;

import com.ferg.afergulator.widget.ButtonNES;
import com.ferg.afergulator.widget.ButtonNES.Key;

public class MainActivity extends Activity implements ActionBar.OnNavigationListener {

    private static final int FILE_SELECT_CODE = 0xc001;

    static {
        if (BuildConfig.DEBUG)
            Timber.plant(new Timber.DebugTree());
    }

    @InjectView(R.id.gameView) GameView mGameView;

    private RomAdapter        romAdapter;
    private SharedPreferences mRecentPrefs;

    protected PowerManager.WakeLock mWakeLock;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        requestWindowFeature(Window.FEATURE_ACTION_BAR_OVERLAY);

        setContentView(R.layout.main);
        ButterKnife.inject(this);

        mRecentPrefs = getSharedPreferences("recent", MODE_PRIVATE);

        getActionBar().setNavigationMode(ActionBar.NAVIGATION_MODE_LIST);
        getActionBar().setDisplayShowTitleEnabled(false);
        setSpinnerAdapter();

        final PowerManager pm = (PowerManager) getSystemService(Context.POWER_SERVICE);

        mWakeLock = pm.newWakeLock(PowerManager.SCREEN_BRIGHT_WAKE_LOCK, "Fergulator");
        mWakeLock.acquire();
    }

    public void setSpinnerAdapter() {
        romAdapter = new RomAdapter();
        getActionBar().setListNavigationCallbacks(romAdapter, this);
    }

    @Override
    protected void onResume() {
        super.onResume();

        mGameView.onResume();
    }

    @Override
    protected void onPause() {
        super.onPause();

        Engine.pauseEmulator();
        Engine.saveBatteryRam();
        mGameView.onPause();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // TODO: Leave this out until save states work
        // getMenuInflater().inflate(R.menu.main_nes, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.menu_nes_load:
                Engine.loadState();
                return true;
            case R.id.menu_nes_save:
                Engine.saveState();
                return true;
            case R.id.menu_nes_shutdown:
                Toast.makeText(this, "power down not implemented yet", Toast.LENGTH_SHORT).show();
                return true;
        }
        return super.onOptionsItemSelected(item);
    }


    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        Key nesKey = ButtonNES.keyFromKeyCode(keyCode);
        if (nesKey != null) {
            Engine.buttonDown(nesKey);
            return true;
        }

        return false;
    }
     
     @Override
     public boolean onKeyUp(int keyCode, KeyEvent event) {
         Key nesKey = ButtonNES.keyFromKeyCode(keyCode);
         if (nesKey != null) {
             Engine.buttonUp(nesKey);
             return true;
         }

         return false;
     }

    @OnClick(R.id.frameLayout)
    public void toggleActionBar() {
        if (getActionBar().isShowing()) {
            getActionBar().hide();
            mGameView.setSystemUiVisibility(View.SYSTEM_UI_FLAG_LOW_PROFILE);
        } else {
            mGameView.setSystemUiVisibility(View.SYSTEM_UI_FLAG_VISIBLE);
            getActionBar().show();
        }
    }

    private class RomAdapter extends ArrayAdapter<String> {

        int mHighlightColor;

        public RomAdapter() {
            super(MainActivity.this, R.layout.rom_spinner_item);

            mHighlightColor = getResources().getColor(android.R.color.holo_blue_light);

            add("Select ROM:");
            add("Browse...");

            Set<String> recent = (mRecentPrefs.getAll() == null) ?
                                 null : mRecentPrefs.getAll().keySet();

            if (recent != null) {
                addAll(recent);
            }
        }

        @Override
        public View getDropDownView(int position, View convertView, ViewGroup parent) {
            TextView v = (TextView) super.getDropDownView(position, convertView, parent);

            int i = getActionBar().getSelectedNavigationIndex();
            v.setTextColor(position == i ? Color.WHITE : mHighlightColor);

            return v;
        }
    }

    @Override
    public boolean onNavigationItemSelected(int itemPosition, long itemId) {
        if (itemPosition == 0) return false;

        if (itemPosition == 1) {
            Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
            intent.setType("*/*");
            intent.addCategory(Intent.CATEGORY_OPENABLE);

            try {
                startActivityForResult(Intent.createChooser(intent, "Select a NES rom:"), FILE_SELECT_CODE);
            } catch (ActivityNotFoundException ex) {
                Toast.makeText(this, "No file browser. =(", Toast.LENGTH_SHORT).show();
            }

            getActionBar().setSelectedNavigationItem(0);
            return true;
        }

        Engine.pauseEmulator();

        String rom = romAdapter.getItem(itemPosition);
        String romUriString = mRecentPrefs.getString(rom, null);
        if (romUriString != null) {
            loadRom(Uri.parse(romUriString));
        }

        return true;
    }

    private void loadRom(Uri uri) {
        Timber.d("Loading ROM: %s", uri);

        String name = uri.getLastPathSegment();

        if (name != null && name.endsWith(".zip")) {
            Timber.d("ZIP File: TODO");
        }

        InputStream is = null;
        try {
            is = getContentResolver().openInputStream(uri);
            mGameView.loadGame(is, name);
            toggleActionBar();
        } catch (IOException e) {
            Toast.makeText(this, "Invalid NES rom!", Toast.LENGTH_SHORT).show();
            Timber.w(e, "Invalid NES rom!");
            mRecentPrefs.edit().remove(name).apply();
            romAdapter.remove(name);
            getActionBar().setSelectedNavigationItem(0);
        } finally {
            closeSilently(is);
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == FILE_SELECT_CODE && resultCode == RESULT_OK) {
            Uri uri = data.getData();
            if (uri != null) {
                String name = uri.getLastPathSegment();
                name = displayRomName(name);

                mRecentPrefs.edit().putString(name, uri.toString()).apply();

                romAdapter.remove(name);
                romAdapter.insert(name, 2);
                getActionBar().setSelectedNavigationItem(2);
            }
        }
    }

    private String displayRomName(String rom) {
        if (rom.endsWith(".nes")) {
            return rom.substring(0, rom.length() - 4);
        }
        return rom;
    }

    static void closeSilently(Closeable aCloseable) {
        if (aCloseable != null) {
            try {
                aCloseable.close();
            } catch (IOException ignored) { }
        }
    }

}
