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
package com.example.journals;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.WebView;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentTransaction;

/**
 * An activity providing general application help.
 */
public class HelpActivity extends AppCompatActivity {

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        super.onCreateOptionsMenu(menu);
        getMenuInflater().inflate(R.menu.menu_help, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) { // TODO
        switch (item.getItemId()) {
            case android.R.id.home:
                finish();
                return true;
            case R.id.action_termsAndConditions:
                // The "terms and conditions" menu option was selected.
                Bundle args = new Bundle();
                args.putInt(WebViewFragment.ARG_RESOURCE, R.string.policy_termsAndConditions);
                showFragment(new WebViewFragment(), args);
                return true;
            case R.id.action_privacyPolicy:
                // The "privacy policy" menu option was selected.
                args = new Bundle();
                args.putInt(WebViewFragment.ARG_RESOURCE, R.string.policy_privacyPolicy);
                showFragment(new WebViewFragment(), args);
                return true;
            case R.id.action_communityGuidelines:
                // the "about" menu option was selected.
                args = new Bundle();
                args.putInt(WebViewFragment.ARG_RESOURCE, R.string.policy_communityGuidelines);
                showFragment(new WebViewFragment(), args);
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.toolbar_content);
        // Set the layout toolbar as the action bar.
        final Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        // Start the first fragment.
        final Bundle args = new Bundle();
        args.putInt(WebViewFragment.ARG_RESOURCE, R.string.policy_termsAndConditions);
        showFragment(new WebViewFragment(), args);
    }

    /**
     * Show the specified fragment in the content view of this activity.
     * <p/>
     * This fragment will replace any existing fragments.
     *
     * @param fragment the fragment to show
     * @param args     arguments for this fragment
     */
    private void showFragment(Fragment fragment, Bundle args) {
        fragment.setArguments(args);
        final FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
        transaction.replace(R.id.content_container, fragment);
        transaction.commit();
    }

    /**
     * Fragment used to show "Terms and Conditions".
     */
    public static class WebViewFragment extends Fragment {

        /**
         * Argument designating the string resource display in the fragment.
         * <p/>
         * Type: int
         */
        public static final String ARG_RESOURCE = "ARG_RESOURCE";
        /**
         * Argument designating the URL display in the fragment.
         * <p/>
         * Type: String
         */
        public static final String ARG_URL = "ARG_URL";

        @Override
        public void onActivityCreated(Bundle savedInstanceState) {
            super.onActivityCreated(savedInstanceState);
            final WebView view = getView().findViewById(R.id.webview);
            final int resId = getArguments().getInt(ARG_RESOURCE);
            if (resId > 0) {
                final String text = getString(resId);
                view.loadData(text, "text/html", "utf-8");
            } else {
                view.loadUrl(getArguments().getString(ARG_URL));
            }
        }

        @Override
        public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container,
                                 Bundle savedInstanceState) {
            return inflater.inflate(R.layout.webview, container, false);
        }

    }

}