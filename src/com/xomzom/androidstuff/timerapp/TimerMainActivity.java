package com.xomzom.androidstuff.timerapp;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.media.Ringtone;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.SystemClock;
import android.preference.PreferenceManager;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.Chronometer;
import android.widget.TextView;
import android.widget.Chronometer.OnChronometerTickListener;

/**
 * The timer activity of the timer application.
 * TODO: Figure life-cycle, and make sure cleanup is done in the right place.
 * 
 * @author dedi
 */
public class TimerMainActivity extends Activity implements
        OnChronometerTickListener, OnSharedPreferenceChangeListener
{
    //
    // Constants.
    //
    
    /**
     * The various timer states. Pause is not supported yet, though.
     */
    enum TimerState {
        READY,   // Ready to run. Valid next state: Running.
        RUNNING, // Running. Valid next steps: Ready (if stopped), paused
        PAUSED,  // Paused. Valid next steps: Running, Ready (if stopped).
    }

    //
    // Members.
    //
    
    /**
     * The current timer state.
     */
    private TimerState m_state = TimerState.READY;

    /**
     * The number of intervals.
     */
    private int m_numIntervals;

    /**
     * The interval length.
     */
    private int m_intervalLength;

    /**
     * The countdown before the first interval.
     */
    private int m_countdown;
    
    /**
     * The notification tone URI.
     */
    private Uri m_notificationURI;

    /**
     * The current interval number.
     */
    private int m_currentInterval;

    /**
     * A reference to the chronometer object.
     */
    private Chronometer m_chronometer;
    
    /**
     * The 'start timer' menu item.
     */
    private MenuItem m_startMenuItem;

    /**
     * The 'pause timer' menu item.
     */
    private MenuItem m_pauseMenuItem;

    /**
     * The 'resume timer' menu item.
     */
    private MenuItem m_resumeMenuItem;

    /**
     * The 'stop timer' menu item.
     */
    private MenuItem m_stopMenuItem;
    
    /**
     * The 'start timer' button.
     */
    private Button m_startButton;

    /**
     * The 'pause timer' button.
     */
    private Button m_pauseButton;

    /**
     * The 'resume timer' button.
     */
    private Button m_resumeButton;

    /**
     * The 'stop timer' button.
     */
    private Button m_stopButton;
    
    /**
     * The title textview.
     */
    private TextView m_titleView;

    /**
     * The state textview.
     */
    private TextView m_stateView;
    
    /**
     * The 'prevent screen locking' flag.
     */
    private boolean m_preventLocking;

    /**
     * The ringtone to use.
     */
    Ringtone m_ringtone;
    
    /**
     * The main view.
     */
    View m_mainView;

    //
    // Operations.
    //

    /**
     * Notification called when the activity is created - set the sequence view,
     * capture events, etc.
     */
    @Override
    public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.timer_main_activity);
        m_mainView = findViewById(R.id.main_view);
        
        initWidgets();
        
        SharedPreferences prefs = 
            PreferenceManager.getDefaultSharedPreferences(getBaseContext());
        prefs.registerOnSharedPreferenceChangeListener(this);

        updatePrefs();
        updateScreenForState();
        m_currentInterval = 0;
    }

    /**
     * Initialize the view widgets.
     */
    private void initWidgets()
    {
        m_chronometer = (Chronometer)findViewById(R.id.chronometer);
        m_chronometer.setOnChronometerTickListener(this);
        m_titleView = (TextView)findViewById(R.id.interval_timer_title);
        m_stateView = (TextView)findViewById(R.id.interval_timer_state);

        m_startButton = (Button)findViewById(R.id.start_button);
        m_stopButton = (Button)findViewById(R.id.stop_button);
        m_pauseButton = (Button)findViewById(R.id.pause_button);
        m_resumeButton = (Button)findViewById(R.id.resume_button);

        OnClickListener buttonListener = new OnClickListener() {
            @Override
            public void onClick(View view)
            {
                TimerMainActivity.this.onButtonClicked((Button)view);
            }
        };
        m_startButton.setOnClickListener(buttonListener);
        m_stopButton.setOnClickListener(buttonListener);
        m_pauseButton.setOnClickListener(buttonListener);
        m_resumeButton.setOnClickListener(buttonListener);
    }

    /**
     * Update user-modifiable preferences.
     */
    private void updatePrefs()
    {
        m_numIntervals = getIntPrefByKeyID(R.string.pref_num_intervals_key,
                R.string.pref_num_intervals_default);
        assert(m_numIntervals > 0);
        m_intervalLength = getIntPrefByKeyID(R.string.pref_interval_length_key,
                R.string.pref_interval_length_default);
        assert(m_intervalLength > 0);
        m_countdown = getIntPrefByKeyID(R.string.pref_countdown_key,
                R.string.pref_countdown_default);
        assert(m_intervalLength >= 0);
        
        SharedPreferences prefs = 
            PreferenceManager.getDefaultSharedPreferences(getBaseContext());

        String notificationAsString = 
            prefs.getString(getString(R.string.pref_ringtone_key),
                            "DEFAULT_NOTIFICATION_URI");
        m_notificationURI = Uri.parse(notificationAsString);
        m_ringtone = RingtoneManager.getRingtone(this, m_notificationURI);

        m_preventLocking = 
            prefs.getBoolean(getString(R.string.pref_nolock_key), false);
        if (m_state == TimerState.RUNNING)
            m_mainView.setKeepScreenOn(m_preventLocking);
    }

    /**
     * Update the title, stateview and screen locking according to the current 
     * state and preferences.
     */
    private void updateScreenForState()
    {
        String title = getString(R.string.timer_title_message,
                m_numIntervals, m_intervalLength, m_countdown);
        m_titleView.setText(title);
        String stateMsg = "";
        m_mainView.setKeepScreenOn(false);
        if (m_state == TimerState.READY)
            stateMsg = getString(R.string.state_ready);
        else if (m_state == TimerState.PAUSED)
            stateMsg = getString(R.string.state_paused);
        else if (m_state == TimerState.RUNNING)
        {
            m_mainView.setKeepScreenOn(m_preventLocking);
            if (m_currentInterval == 0)
                stateMsg = getString(R.string.state_running_countdown);
            else
            {
                stateMsg = 
                    getString(R.string.state_running_interval, 
                        m_currentInterval);
            }
        }
        m_stateView.setText(stateMsg);
    }

    /**
     * Preferences were changed. Re-read all values.
     */
    @Override
    public void onSharedPreferenceChanged(SharedPreferences a_arg0,
            String a_arg1)
    {
        // Guess it's OK to do this even while we're running... Although of
        // course we might have already passed the interval length, or the
        // number of intervals.
        updatePrefs();
        updateScreenForState();
        
    }

    /**
     * Create the options menu.
     * 
     * @see android.app.Activity#onCreateOptionsMenu(android.view.Menu)
     */
    @Override
    public boolean onCreateOptionsMenu(Menu menu)
    {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.options, menu);
        m_startMenuItem = menu.findItem(R.id.menu_start);
        m_stopMenuItem = menu.findItem(R.id.menu_stop);
        m_pauseMenuItem = menu.findItem(R.id.menu_pause);
        m_resumeMenuItem = menu.findItem(R.id.menu_resume);

        setWidgetsForState();

        return true;
    }

    /**
     * An options menu item was selected.
     * @see android.app.Activity#onOptionsItemSelected(android.view.MenuItem)
     */
    @Override
    public boolean onOptionsItemSelected(MenuItem item)
    {
        switch (item.getItemId()) {
        case R.id.menu_start:
            startTimer();
            return true;
        case R.id.menu_pause:
            pauseTimer();
            return true;
        case R.id.menu_resume:
            resumeTimer();
            return true;
        case R.id.menu_stop:
            stopTimer();
            return true;
        case R.id.menu_settings:
            startSettingsActivity();
            return true;
        case R.id.menu_exit:
            finish();
            return true;
        }
        return false;
    }

    /**
     * One of the action buttons was selected.
     * @param a_view
     */
    protected void onButtonClicked(Button button)
    {
        switch (button.getId()) {
        case R.id.start_button:
            startTimer();
            return;
        case R.id.pause_button:
            pauseTimer();
            return;
        case R.id.resume_button:
            resumeTimer();
            return;
        case R.id.stop_button:
            stopTimer();
            return;
        }
    }
    
    /**
     * Set the menu items and widgets for start state.
     */
    private void setWidgetsForStartState()
    {
        // Menu items are only initialized the first time they are used.
        if (m_startMenuItem != null)
        {
            m_startMenuItem.setVisible(false);
            m_stopMenuItem.setVisible(true);
            m_pauseMenuItem.setVisible(false); // Because it's not working yet.
            m_resumeMenuItem.setVisible(false);
        }
        m_startButton.setVisibility(Button.GONE);
        m_stopButton.setVisibility(Button.VISIBLE);
        m_pauseButton.setVisibility(Button.GONE);
        m_resumeButton.setVisibility(Button.GONE);
        
    }

    /**
     * Set the menu items and widgets for pause state.
     */
    private void setWidgetsForPauseState()
    {
        // Menu items are only initialized the first time they are used.
        if (m_startMenuItem != null)
        {
            m_startMenuItem.setVisible(false);
            m_stopMenuItem.setVisible(true);
            m_pauseMenuItem.setVisible(false);
            m_resumeMenuItem.setVisible(true);
        }
        m_startButton.setVisibility(Button.GONE);
        m_stopButton.setVisibility(Button.VISIBLE);
        m_pauseButton.setVisibility(Button.GONE);
        m_resumeButton.setVisibility(Button.VISIBLE);
    }

    /**
     * Set the menu items and widgets for resume state.
     */
    private void setWidgetsForResumeState()
    {
        // Menu items are only initialized the first time they are used.
        if (m_startMenuItem != null)
        {
            m_startMenuItem.setVisible(false);
            m_stopMenuItem.setVisible(true);
            m_pauseMenuItem.setVisible(true);
            m_resumeMenuItem.setVisible(false);
        }
 
        m_startButton.setVisibility(Button.GONE);
        m_stopButton.setVisibility(Button.VISIBLE);
        m_pauseButton.setVisibility(Button.VISIBLE);
        m_resumeButton.setVisibility(Button.GONE);
     }


    /**
     * Set the menu items and widgets for stop state.
     */
    private void setWidgetsForStopState()
    {
        // Menu items are only initialized the first time they are used.
        if (m_startMenuItem != null)
        {
            m_startMenuItem.setVisible(true);
            m_stopMenuItem.setVisible(false);
            m_pauseMenuItem.setVisible(false); // Because it's not working yet.
            m_resumeMenuItem.setVisible(false);
        }
        m_startButton.setVisibility(Button.VISIBLE);
        m_stopButton.setVisibility(Button.GONE);
        m_pauseButton.setVisibility(Button.GONE);
        m_resumeButton.setVisibility(Button.GONE);
     }
    
    /**
     * Set the menu items and widgets according to the current state.
     */
    private void setWidgetsForState()
    {
        switch (m_state)
        {
            case READY:
                setWidgetsForStopState();
                break;
            case PAUSED:
                setWidgetsForPauseState();
                break;
            case RUNNING:
                setWidgetsForStartState();
                break;
        }
    }

    /**
     * Start the timer.
     */
    private void startTimer()
    {
        assert(m_state == TimerState.READY);
        setWidgetsForStartState();
        m_state = TimerState.RUNNING;

        // Skip 0 length countdown.
        m_currentInterval = 0;
        if (m_countdown == 0)
            m_currentInterval++;

        startNextInterval();
    }

    /**
     * Pause the timer (not implemented yet).
     */
    private void pauseTimer()
    {
        assert(m_state == TimerState.RUNNING);
        m_state = TimerState.PAUSED;
        setWidgetsForPauseState();
        updateScreenForState();

        // TODO: actually pause.
    }

    /**
     * Resume a paused timer (not implemented yet).
     */
    private void resumeTimer()
    {
        assert(m_state == TimerState.PAUSED);
        setWidgetsForResumeState();
        m_state = TimerState.RUNNING;
        updateScreenForState();
        
        // TODO: actually resume.
    }

    /**
     * Stop a running timer.
     */
    private void stopTimer()
    {
        assert(m_state == TimerState.RUNNING || m_state == TimerState.PAUSED);
        m_state = TimerState.READY;
        setWidgetsForStopState();
        m_chronometer.stop();
        updateScreenForState();
    }

    /**
     * Start the 'settings' activity.
     */
    private void startSettingsActivity()
    {
        Intent intent = new Intent(this, SettingsActivity.class);
        startActivity(intent);
    }

    /**
     * A chronometer tick event.
     */
    @Override
    public void onChronometerTick(Chronometer a_chronometer)
    {
        long currentTimeMillis = SystemClock.elapsedRealtime();
        long elapsedTime = (currentTimeMillis - m_chronometer.getBase()) / 1000L;
        int targetTime = (m_currentInterval == 0) ? m_countdown
                : m_intervalLength;
        if (elapsedTime < targetTime)
        {
            return;
        }

        m_chronometer.stop();
        m_currentInterval++;
        if (m_currentInterval <= m_numIntervals)
            startNextInterval();
        else
        {
            stopTimer();
        }

        m_ringtone.play();
    }

    /**
     * Start the next interval.
     */
    private void startNextInterval()
    {
        updateScreenForState();

        long currentTimeMillis = SystemClock.elapsedRealtime();
        m_chronometer.setBase(currentTimeMillis);
        m_chronometer.start();
    }

    /**
     * Helper method: get a string preference, identified by it's key string ID.
     * 
     * @param keyStrId The resource ID of the string identifying the key.
     * @param defValueResId The resource ID of the string specifying the 
     * default value.
     */
    private String getStrPrefByKeyID(int keyStrId, int defValueResId)
    {
        SharedPreferences prefs = PreferenceManager
                .getDefaultSharedPreferences(getBaseContext());

        String key = getString(keyStrId);
        if (key == null || prefs == null)
            return null;
        String defaultValue = getString(defValueResId);

        return prefs.getString(key, defaultValue);
    }

    /**
     * Helper method: get an int preference (cast from a string), identified by
     * it's key string ID. If the preference was not set, return the default
     * value string, cast to integer, instead.
     * 
     * @param keyStrId The resource ID of the string identifying the preference
     * key.
     * @param defValueResId The resource ID of the string specifying the 
     * default value (and NOT the default value itself!!!).
     */
    private int getIntPrefByKeyID(int keyStrId, int defValueResId)
    {
        String valueAsString = getStrPrefByKeyID(keyStrId, defValueResId);

        // Make sure this is really an int or you'll get a
        // NumberFormatException!
        int value = Integer.parseInt(valueAsString);
        return value;
    }
}
