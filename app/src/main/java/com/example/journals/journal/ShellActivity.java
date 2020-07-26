/*
 * MIT License
 *
 * Copyright (c) 2020 Wade Johnson
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and
 * associated documentation files (the "Software"), to deal in the Software without restriction,
 * including without limitation the rights to use, copy, modify, merge, publish, distribute,
 * sublicense, and/or sell copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or
 * substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT
 * NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND
 * NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM,
 * DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */
package com.example.journals.journal;

import android.os.Bundle;
import android.util.Log;

import com.example.journals.R;

import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;

/**
 * An empty activity that simply starts a fragment.
 * <p/>
 * Intent must contain a parcelable extra of a class that extends {@link Fragment} using the key
 * {@link #EXTRA_FRAGMENT_CLASS}, and may optionally contain a {@link Bundle} extra of the fragment
 * arguments using the key {@link #EXTRA_FRAGMENT_ARGS}.  The fragment class passed in must have an
 * empty constructor in order to be properly instantiated.
 */
public class ShellActivity extends AppCompatActivity {

    /**
     * Argument designating the fragment class to instantiate.
     * <p/>
     * Type: Parcelable (Class, of a type that extends {@link Fragment})
     */
    public static final String EXTRA_FRAGMENT_CLASS = "EXTRA_FRAGMENT_CLASS";
    /**
     * Extra designating the fragment arguments.
     * <p/>
     * Type: {@link Bundle}
     */
    public static final String EXTRA_FRAGMENT_ARGS = "EXTRA_FRAGMENT_ARGS";

    /**
     * Tag to display with debug messages.
     */
    private static final String DEBUG_TAG = ShellActivity.class.getSimpleName();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.empty_content);
        // Instantiate the fragment.
        if (savedInstanceState != null) {
            Fragment fragment = null;
            final Class clazz = getIntent().getParcelableExtra(EXTRA_FRAGMENT_CLASS);
            try {
                fragment = (Fragment) clazz.newInstance();
            } catch (InstantiationException e) {
                Log.w(DEBUG_TAG, "Could not access fragment constructor.");
            } catch (IllegalAccessException e) {
                Log.w(DEBUG_TAG, "Could not access fragment method or field.");
            }
            // Add the fragment arguments.
            if (fragment != null) {
                fragment.setArguments(getIntent().getBundleExtra(EXTRA_FRAGMENT_ARGS));
                getSupportFragmentManager().beginTransaction()
                        .add(R.id.content_container, fragment).commit();
            }
        }
    }

}
