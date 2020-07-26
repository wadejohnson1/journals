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

import android.content.Intent;
import android.os.Bundle;

import com.example.journals.R;
import com.example.journals.provider.JournalContract.Activities;
import com.example.journals.provider.JournalContract.Contacts;
import com.example.journals.widget.AccountsUpdateListenerActivity;

import androidx.fragment.app.Fragment;

/**
 * Base activity for viewing or editing journal entry details and associated data. It is an empty
 * activity that simply starts a fragment.
 * <p/>
 * Intent must contain an action of either {@link Intent#ACTION_VIEW} or {@link Intent#ACTION_EDIT}
 * and a type of either {@link Activities#CONTENT_TYPE_ITEM} or {@link Contacts#CONTENT_TYPE_ITEM}.
 */
public class EntryActivity extends AccountsUpdateListenerActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.empty_content);
        if (savedInstanceState == null) {
            final Fragment fragment;
            // Determine the action to perform on the data.
            switch (getIntent().getAction()) {
                case Intent.ACTION_VIEW:
                    switch (getIntent().getType()) {
                        case Activities.CONTENT_TYPE_ITEM:
                            fragment = new SpotDetailFragment();
                            break;
                        case Contacts.CONTENT_TYPE_ITEM:
                            fragment = new ContactDetailFragment();
                            break;
                        default:
                            throw new IllegalArgumentException();
                    }
                    break;
                case Intent.ACTION_EDIT:
                    switch (getIntent().getType()) {
                        case Activities.CONTENT_TYPE_ITEM:
                            fragment = new SpotEditorFragment();
                            break;
                        case Contacts.CONTENT_TYPE_ITEM:
                            fragment = new ContactEditorFragment();
                            break;
                        default:
                            throw new IllegalArgumentException();
                    }
                    break;
                default:
                    throw new IllegalArgumentException();
            }
            final Bundle args = new Bundle();
            args.putAll(getIntent().getExtras());
            fragment.setArguments(args);
            getSupportFragmentManager().beginTransaction().replace(R.id.content_container, fragment)
                    .commit();
        }
    }

}
