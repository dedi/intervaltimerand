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
import android.content.Context;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.widget.TextView;

/**
 * The chronometer view. This is basically a view that maximizes the text in
 * it depending on it's size.
 *
 * @author dedi
 */
public class ChronoTextView extends TextView
{

    //
    // Members.
    //



    //
    // Operations.
    //

    
    public ChronoTextView(Context context)
    {
        super(context);
    }

    public ChronoTextView(Context context, AttributeSet attrs)
    {
        super(context, attrs);
    }

    /*
     * Set the text size so that the current text is maximized.
     */
    private void setTextSize() 
    {
        String text = getText().toString();
        int textChars = text.length();
        if (textChars < 2)
        {
            textChars = 2;
        }
        int displayWidth = getWidth() - getPaddingLeft() - getPaddingRight();
        int displayHeight =
            (int)((getHeight() - getPaddingTop() - getPaddingBottom()) * 0.8);
        int fontMaxWidth = displayWidth / textChars;
        int fontMaxSize = Math.min(fontMaxWidth, displayHeight);
        setTextSize(TypedValue.COMPLEX_UNIT_PX, fontMaxSize);
    }

    @Override
    protected void onTextChanged(CharSequence text, int start,
                                 int before, int after)
    {
        setTextSize();
    }

    @Override
    protected void onSizeChanged (int w, int h, int oldw, int oldh)
    {
        super.onSizeChanged(w, h, oldw, oldh);
        setTextSize();
    }
}