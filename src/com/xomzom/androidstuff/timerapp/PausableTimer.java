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

import android.os.CountDownTimer;
import android.util.Log;

/**
 * A pause-able countdown timer, which can count seconds. the timer is accurate
 * to a single second, so if paused and resumed, will round down to the last
 * second ticked. E.g, if a timer 3 second timer is paused after 1.5 seconds,
 * it will still tick 2 seconds when resumed.
 * Clock resolution is defined by TICKS_PER_SECOND, and the timer will send
 * roughly that number of ticks per second, all with the same seconds value.
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
    private final static int MILLIS_IN_SECOND = 1000;

    /**
     * Number of ticks per second. Higher value means more acuracy,
     * but higher CPU usage.
     */
    private final static int TICKS_PER_SECOND = 20;


    //
    // Members
    //

    /**
     * Our listener object.
     */
    private PausableTimerListener m_listener;

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
    public PausableTimer(PausableTimerListener listener)
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
        long countdownInterval = MILLIS_IN_SECOND / TICKS_PER_SECOND;
        m_countdownTimer =
            new CountDownTimer(seconds * MILLIS_IN_SECOND, countdownInterval) {

            @Override
            public void onTick(long millisUntilFinished)
            {
                PausableTimer.this.onTick(millisUntilFinished);
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
    protected void onTick(long millisUntilFinished)
    {
        Log.d(this.getClass().toString(), "onTick: " +  millisUntilFinished);
        // Round down if we're less than half a tick above the second,
        // round up otherwise.
        int halfInterval = (MILLIS_IN_SECOND / TICKS_PER_SECOND) / 2;
        m_secondsRemaining = (((int)millisUntilFinished + MILLIS_IN_SECOND -
                               halfInterval) / MILLIS_IN_SECOND);
        m_listener.onTimerTick(m_secondsRemaining);
    }

    /**
     * A 'timer expired' event.
     */
    protected void onFinish()
    {
        Log.d(this.getClass().toString(), "onFinish()");
        m_secondsRemaining = 0;
        m_countdownTimer = null;
        m_listener.onIntervalFinished();
    }
}
