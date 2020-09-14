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
package com.example.journals.app;

import android.content.Intent;
import android.view.Menu;
import android.view.MenuItem;

import com.example.journals.R;

import androidx.appcompat.app.AppCompatActivity;

/**
 * Activity providing default menu options.
 * <p/>
 * All activities that are not related to a specific menu item (ex. a "settings" activity) should
 * subclass this activity to get persistent default menu options throughout the app.
 */
public class DefaultMenuActivity extends AppCompatActivity {

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        super.onCreateOptionsMenu(menu);
        getMenuInflater().inflate(R.menu.menu_default, menu);
        // Get the search view and set the searchable configuration. TODO
        //        final SearchManager manager = (SearchManager) getSystemService(Context.SEARCH_SERVICE);
        //        final SearchView view = (SearchView) menu.findItem(R.id.action_search)
        //                .getActionView();
        //        view.setSearchableInfo(manager.getSearchableInfo(getComponentName()));
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_settings:
                // The settings menu option was selected.
                startActivity(new Intent(this, SettingsActivity.class));
                return true;
            case R.id.action_help:
                // The help menu option was selected.
                startActivity(new Intent(this, HelpActivity.class));
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

}