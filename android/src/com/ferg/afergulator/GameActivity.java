package com.ferg.afergulator;

import android.content.Context;
import android.os.Bundle;
import android.os.PowerManager;
import android.support.v7.app.AppCompatActivity;
import android.view.KeyEvent;

import java.io.IOException;
import java.io.InputStream;

import com.ferg.afergulator.widget.ButtonNES;
import com.ferg.afergulator.widget.ButtonNES.Key;
import go.nesdroid.Nesdroid;
import timber.log.Timber;

public class GameActivity extends AppCompatActivity {

    private static final String TAG = GameActivity.class.getSimpleName();

    private boolean audioEnabled = true;

    private GameView mGameView;

    protected PowerManager.WakeLock mWakeLock;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.game);

        final PowerManager pm = (PowerManager) getSystemService(Context.POWER_SERVICE);

        mWakeLock = pm.newWakeLock(PowerManager.SCREEN_BRIGHT_WAKE_LOCK, "Fergulator");
        mWakeLock.acquire();

        mGameView = (GameView) findViewById(R.id.gameView);
    }

    @Override
    protected void onResume() {
        super.onResume();
        mGameView.onResume();

        String rom = getIntent().getStringExtra("rom");
        Timber.i(TAG, "Loading: " + rom);
        
        InputStream is = null;
        try {
            is = getAssets().open("roms/" + rom);
            mGameView.loadGame(is, rom);
        } catch (IOException e) {
            Timber.i(e, "Error loading game!");
            e.printStackTrace();
        } finally {
            if (is != null) {
                try {
                    is.close();
                } catch (IOException e) {
                    Timber.i(TAG, e.toString());
                    e.printStackTrace();
                }
            }
        }
    }

    @Override
    protected void onPause() {
        super.onPause();

        Nesdroid.PauseEmulator();
        mGameView.onPause();
    }

    @Override
    public void onDestroy() {
        mWakeLock.release();
        super.onDestroy();
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
}
