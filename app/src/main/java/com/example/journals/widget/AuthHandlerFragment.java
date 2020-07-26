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
package com.example.journals.widget;

import com.android.volley.Request;
import com.example.journals.network.AuthFailureRetryPolicy;
import com.example.journals.network.VolleySingleton;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.RecyclerView;

/**
 * A fragment that manages custom views around a {@link RecyclerView}.  When no adapter is set on
 * the recycler view, an indeterminant progress indicator is shown.  When the recycler view's
 * adapter is empty, a message is displayed instead of the recycler view.
 */
public class AuthHandlerFragment extends Fragment {

    /**
     * Set whether the fragment is currently started ({@code true} from the end of {@link
     * #onStart()} to the end of {@link #onStop()}.  Used to prevent asynchronous tasks from
     * updating the fragment when it is stopped.
     */
    private boolean mIsStarted = false;

    /**
     * Get whether the fragment is currently started.
     *
     * @return {@code true} if the fragment is started (from the end of {@link #onStart()} to just
     * prior to cancelling all Volley requests in {@link #onStop()}), {@code false} otherwise
     */
    public boolean isStarted() {
        return mIsStarted;
    }

    @Override
    public void onStart() {
        super.onStart();
        mIsStarted = true;
    }

    @Override
    public void onStop() {
        super.onStop();
        mIsStarted = false;
        // Cancel any running network tasks.
        VolleySingleton.getInstance(getContext()).getRequestQueue().cancelAll(this);
    }

    /**
     * Start a request with an {@link AuthFailureRetryPolicy} that will be cancelled automatically
     * if not complete prior to {@link #onStop()}.
     *
     * @param request the request to start
     */
    protected void startRequest(@NonNull Request request) {
        // Prevent request from retrying in the event of an auth failure.
        request.setRetryPolicy(new AuthFailureRetryPolicy());
        // Make request cancelable if this fragment is destroyed before completing.
        request.setTag(this);
        // Add the request to the network queue.
        VolleySingleton.getInstance(getContext()).addToRequestQueue(request);
    }

}
