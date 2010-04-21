package com.xomzom.androidstuff.timerapp;

import android.os.CountDownTimer;
import android.util.Log;

/**
 * A pause-able countdown timer, which can count seconds. the timer is accurate
 * to a single second, so if paused and resumed, will round down to the last
 * second ticked. E.g, if a timer 3 second timer is paused after 1.5 seconds,
 * it will still tick 2 seconds when resumed.
 * Because I'm lazy, the counter will work with a specific interval timer
 * activity, and not using a listener. This can be changed if I ever need this
 * class for anything else. 
 * 
 * @author dedi
 */
public class PausableTimer
{
    //
    // Constants.
    //
    
    /**
     * Milliseconds in a second.
     */
    private final static long MILLIS_IN_SECOND = 1000L;


    //
    // Members
    //
    
    /**
     * Our listener object.
     */
    private TimerMainActivity m_listener;
    
    /**
     * The actual countdown timer object. Will be null if the timer is currently
     * not running.
     */
    private CountDownTimer m_countdownTimer;
    
    /**
     * Seconds remaining for this timer.
     */
    private int m_secondsRemaining;
    
    
    //
    // Operations.
    //
    
    /**
     * Create a new pausable timer, associated with the given listener.
     */
    public PausableTimer(TimerMainActivity listener)
    {
        m_listener = listener;
    }
    
    /**
     * Start a timer that will tick for the given number of seconds. This will
     * cancel any currently running or paused timer. 
     */
    public void start(int seconds)
    {
        if (m_countdownTimer != null)
        {
            Log.d(this.getClass().toString(), 
                    "Warning: Timer started while running");
            m_countdownTimer.cancel();
            m_countdownTimer = null;
        }
        m_secondsRemaining = seconds;
        m_countdownTimer = 
            new CountDownTimer(seconds * MILLIS_IN_SECOND, MILLIS_IN_SECOND) {
            
            @Override
            public void onTick(long millisUntilFinished)
            {
                int secondsRemaining = 
                    (int)(millisUntilFinished / MILLIS_IN_SECOND);
                PausableTimer.this.onTick(secondsRemaining);
            }
            
            @Override
            public void onFinish()
            {
                PausableTimer.this.onFinish();                
            }
        };
        m_countdownTimer.start();
    }
    
    /**
     * Pause the timer.
     */
    public void pause()
    {
        if (m_countdownTimer == null)
            return;
        m_countdownTimer.cancel();
        m_countdownTimer = null;
    }

    /**
     * Resume the timer.
     */
    public void resume()
    {
        if (m_secondsRemaining != 0)
            start(m_secondsRemaining);
        else
            onFinish();
    }
    
    /**
     * Cancel a running timer.
     */
    public void stop()
    {
        pause();
        m_secondsRemaining = 0;
    }

    /**
     * A timer tick event.
     * 
     * @param secondsRemaining
     */
    protected void onTick(int secondsRemaining)
    {
        m_secondsRemaining = secondsRemaining;
        m_listener.onTimerTick(secondsRemaining);
    }

    /**
     * A 'timer expired' event.
     */
    protected void onFinish()
    {
        m_secondsRemaining = 0;
        m_countdownTimer = null;
        m_listener.onIntervalFinished();
    }
}
