package com.ferg.afergulator;

import android.app.PendingIntent;
import android.content.*;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.os.PowerManager;
import android.support.v4.view.MenuItemCompat;
import android.support.v7.app.*;
import android.support.v7.media.MediaRouteSelector;
import android.support.v7.media.MediaRouter;
import android.view.*;
import android.widget.*;

import java.io.*;
import java.util.Set;

import butterknife.*;
import com.ferg.afergulator.widget.ButtonNES;
import com.ferg.afergulator.widget.ButtonNES.Key;
import com.google.android.gms.cast.*;
import com.google.android.gms.common.api.Status;
import go.nesdroid.Nesdroid;
import timber.log.Timber;

public class MainActivity extends AppCompatActivity implements ActionBar.OnNavigationListener {

    private static final int FILE_SELECT_CODE      = 0xc001;
    private static final String REMOTE_DISPLAY_APP_ID = "27FA9440";

    @InjectView(R.id.gameView) GameView mGameView;

    protected PowerManager.WakeLock mWakeLock;

    private SharedPreferences  mRecentPrefs;
    private MediaRouter        mMediaRouter;
    private RomAdapter         mRomAdapter;
    private MediaRouteSelector mMediaRouteSelector;

    private CastDevice mSelectedDevice;

    private MediaRouter.Callback mMediaRouterCallback = new MediaRouter.Callback() {

        @Override
        public void onRouteSelected(MediaRouter router, MediaRouter.RouteInfo info) {
            mSelectedDevice = CastDevice.getFromBundle(info.getExtras());
            String routeId = info.getId();

            Timber.d("Route ID: %s", routeId);
            Intent intent = new Intent(MainActivity.this, MainActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
            PendingIntent notificationPendingIntent = PendingIntent.getActivity(
                    MainActivity.this, 0, intent, 0);

            CastRemoteDisplayLocalService.NotificationSettings settings =
                    new CastRemoteDisplayLocalService.NotificationSettings.Builder()
                            .setNotificationPendingIntent(notificationPendingIntent).build();

            CastRemoteDisplayLocalService.startService(
                    getApplicationContext(),
                    PresentationService.class, REMOTE_DISPLAY_APP_ID,
                    mSelectedDevice, settings,
                    new CastRemoteDisplayLocalService.Callbacks() {
                        @Override
                        public void onServiceCreated(CastRemoteDisplayLocalService castRemoteDisplayLocalService) {

                        }

                        @Override
                        public void onRemoteDisplaySessionStarted(
                                CastRemoteDisplayLocalService service) {
                            // initialize sender UI
                        }

                        @Override
                        public void onRemoteDisplaySessionError(
                                Status errorReason){
//                            initError();
                        }
                    });
        }

        @Override
        public void onRouteUnselected(MediaRouter router, MediaRouter.RouteInfo info) {
//            teardown();
            mSelectedDevice = null;
            CastRemoteDisplayLocalService.stopService();
        }
    };

    static {
        Timber.uprootAll();
        if (BuildConfig.DEBUG) {
            Timber.plant(new Timber.DebugTree());
        }
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        requestWindowFeature(Window.FEATURE_ACTION_BAR_OVERLAY);

        setContentView(R.layout.main);
        ButterKnife.inject(this);

        mRecentPrefs = getSharedPreferences("recent", MODE_PRIVATE);

        getSupportActionBar().setNavigationMode(ActionBar.NAVIGATION_MODE_LIST);
        getSupportActionBar().setDisplayShowTitleEnabled(false);
        setSpinnerAdapter();

        final PowerManager pm = (PowerManager) getSystemService(Context.POWER_SERVICE);

        mWakeLock = pm.newWakeLock(PowerManager.SCREEN_BRIGHT_WAKE_LOCK, "Fergulator");
        mWakeLock.acquire();

        mMediaRouter = MediaRouter.getInstance(getApplicationContext());
        mMediaRouteSelector = new MediaRouteSelector.Builder()
                .addControlCategory(CastMediaControlIntent.categoryForCast("27FA9440"))
//                .addControlCategory(CastMediaControlIntent.categoryForCast(BuildConfig.APPLICATION_ID))
                .build();

    }

    public void setSpinnerAdapter() {
        mRomAdapter = new RomAdapter();
        getSupportActionBar().setListNavigationCallbacks(mRomAdapter, this);
    }

    @Override protected void onStart() {
        super.onStart();
        mMediaRouter.addCallback(mMediaRouteSelector, mMediaRouterCallback,
                                 MediaRouter.CALLBACK_FLAG_REQUEST_DISCOVERY);
    }

    @Override protected void onStop() {
        super.onStop();
        mMediaRouter.removeCallback(mMediaRouterCallback);
    }

    @Override
    protected void onResume() {
        super.onResume();

        mGameView.onResume();
    }

    @Override
    protected void onPause() {
        super.onPause();

        mGameView.onPause();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        super.onCreateOptionsMenu(menu);
        getMenuInflater().inflate(R.menu.cast_menu, menu);

        MenuItem mediaRouteMenuItem = menu.findItem(R.id.media_route_menu_item);
        MediaRouteActionProvider mediaRouteActionProvider =
                (MediaRouteActionProvider) MenuItemCompat.getActionProvider(mediaRouteMenuItem);
        mediaRouteActionProvider.setRouteSelector(mMediaRouteSelector);

        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.menu_nes_load:
                Nesdroid.LoadState();
                return true;
            case R.id.menu_nes_save:
                Nesdroid.SaveState();
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
        if (getSupportActionBar().isShowing()) {
            getSupportActionBar().hide();
            mGameView.setSystemUiVisibility(View.SYSTEM_UI_FLAG_LOW_PROFILE);
        } else {
            mGameView.setSystemUiVisibility(View.SYSTEM_UI_FLAG_VISIBLE);
            getSupportActionBar().show();
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

            int i = getSupportActionBar().getSelectedNavigationIndex();
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

            getSupportActionBar().setSelectedNavigationItem(0);
            return true;
        }

        Nesdroid.PauseEmulator();

        String rom = mRomAdapter.getItem(itemPosition);
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
            mRomAdapter.remove(name);
            getSupportActionBar().setSelectedNavigationItem(0);
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

                mRomAdapter.remove(name);
                mRomAdapter.insert(name, 2);
                getSupportActionBar().setSelectedNavigationItem(2);
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
