/**
 * PeriodicTimer - a simple Interval Timer for Android
 * Copyright (c) 2010-2013, Dedi Hirschfeld
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *   * Redistributions of source code must retain the above copyright
 *     notice, this list of conditions and the following disclaimer.
 *   * Redistributions in binary form must reproduce the above copyright
 *     notice, this list of conditions and the following disclaimer in the
 *     documentation and/or other materials provided with the distribution.
 *   * Neither the name of the <organization> nor the
 *     names of its contributors may be used to endorse or promote products
 *     derived from this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
 * ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL DEDI HIRSCHFELD BE LIABLE FOR ANY
 * DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
 * (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 * ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

package com.xomzom.androidstuff.timerapp;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.media.AudioManager;
import android.media.Ringtone;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.TextView;

/**
 * The timer activity of the timer application.
 * TODO: Figure life-cycle, and make sure cleanup is done in the right place.
 *
 * @author dedi
 */
public class TimerMainActivity extends Activity
    implements OnSharedPreferenceChangeListener
{
    //
    // Constants.
    //

    /**
     * The various timer states.
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
     * The current interval number.
     */
    private int m_currentInterval;

    /**
     * A reference to the time view.
     */
    private TextView m_chronometer;

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
    private Ringtone m_ringtone;

    /**
     * The actual timer object.
     */
    private PausableTimer m_timer;

    /**
     * The main view.
     */
    private View m_mainView;

    //
    // Operations.
    //

    /**
     * An event raised when the activity is created - set the sequence view,
     * capture events, etc.
     */
    @Override
    public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        m_timer = new PausableTimer(this);

        setContentView(R.layout.timer_main_activity);
        m_mainView = findViewById(R.id.main_view);

        initWidgets();

        SharedPreferences prefs =
            PreferenceManager.getDefaultSharedPreferences(getBaseContext());
        prefs.registerOnSharedPreferenceChangeListener(this);

        updatePrefs();
        updateScreenForState();
        m_currentInterval = 0;
        setVolumeControlStream(AudioManager.STREAM_NOTIFICATION);
    }

    /**
     * An event raised when the activity needs to pause. Pause the timer.
     * Note that while pausing the activity pauses the timer, resuming it
     * does not automatically resume the timer. It seems to make more sense
     * for the user to resume the timer manually.
     * @see android.app.Activity#onPause()
     */
    @Override
    protected void onPause()
    {
        super.onPause();
        if (m_state == TimerState.RUNNING)
            onPauseRequest();
    }

    /**
     * Initialize the view widgets.
     */
    private void initWidgets()
    {
        m_chronometer = (TextView)findViewById(R.id.time_view);
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
        Uri notificationURI = Uri.parse(notificationAsString);
        m_ringtone = RingtoneManager.getRingtone(this, notificationURI);
        if (m_ringtone == null)
        {
            Log.e(this.getClass().toString(),
                  "Couldn't load ringtone. Loading something.");
            notificationURI = RingtoneManager.getValidRingtoneUri(this);
            m_ringtone = RingtoneManager.getRingtone(this, notificationURI);
        }

        m_ringtone.setStreamType(AudioManager.STREAM_NOTIFICATION);

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
            onStartRequest();
            return true;
        case R.id.menu_pause:
            onPauseRequest();
            return true;
        case R.id.menu_resume:
            onResumeRequest();
            return true;
        case R.id.menu_stop:
            onLastIntervalFinished();
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
            onStartRequest();
            return;
        case R.id.pause_button:
            onPauseRequest();
            return;
        case R.id.resume_button:
            onResumeRequest();
            return;
        case R.id.stop_button:
            onLastIntervalFinished();
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
            m_pauseMenuItem.setVisible(true);
            m_resumeMenuItem.setVisible(false);
        }
        m_startButton.setVisibility(Button.GONE);
        m_stopButton.setVisibility(Button.VISIBLE);
        m_pauseButton.setVisibility(Button.VISIBLE);
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
     * The 'start' button was pressed.
     */
    private void onStartRequest()
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
     * The 'pause' button was pressed.
     */
    private void onPauseRequest()
    {
        assert(m_state == TimerState.RUNNING);
        m_state = TimerState.PAUSED;
        m_timer.pause();
        setWidgetsForPauseState();
        updateScreenForState();
    }

    /**
     * The 'resume' button was pressed.
     */
    private void onResumeRequest()
    {
        assert(m_state == TimerState.PAUSED);
        setWidgetsForResumeState();
        m_state = TimerState.RUNNING;
        m_timer.resume();
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
     * A timer tick event.
     */
    public void onTimerTick(int secondsTillFinish)
    {
        // TODO: Format this.
        String secondsString = String.valueOf(secondsTillFinish);
        m_chronometer.setText(secondsString);
    }

    /**
     * An interval has finished.
     */
    protected void onIntervalFinished()
    {
        
        m_ringtone.play();
        m_timer.stop();
        m_currentInterval++;
        if (m_currentInterval <= m_numIntervals)
            startNextInterval();
        else
        {
            onLastIntervalFinished();
        }

    }

    /**
     * The timer finished or was stopped.
     */
    private void onLastIntervalFinished()
    {
        assert(m_state == TimerState.RUNNING || m_state == TimerState.PAUSED);
        m_timer.stop();
        m_state = TimerState.READY;
        setWidgetsForStopState();
        updateScreenForState();
    }

   /**
     * Start the next interval.
     */
    private void startNextInterval()
    {
        int thisIntervalLength =
            (m_currentInterval == 0 ? m_countdown : m_intervalLength);
        updateScreenForState();
        startTimer(thisIntervalLength);
    }

    /**
     * Start the timer, also displaying the number of seconds in the chronometer
     * view.
     * @param seconds
     */
    private void startTimer(int seconds)
    {
        // Fake a first tick to display the number of seconds left right now.
        onTimerTick(seconds);
        m_timer.start(seconds);
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
