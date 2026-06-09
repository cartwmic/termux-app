package com.termux.app.terminal.io;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;

import androidx.annotation.NonNull;
import androidx.viewpager.widget.PagerAdapter;
import androidx.viewpager.widget.ViewPager;

import com.termux.R;
import com.termux.app.TermuxActivity;
import com.termux.shared.termux.extrakeys.ExtraKeysView;
import com.termux.terminal.TerminalSession;

public class TerminalToolbarViewPager {

    public static class PageAdapter extends PagerAdapter {

        final TermuxActivity mActivity;
        String mSavedTextInput;

        public PageAdapter(TermuxActivity activity, String savedTextInput) {
            this.mActivity = activity;
            this.mSavedTextInput = savedTextInput;
        }

        @Override
        public int getCount() {
            return 2;
        }

        @Override
        public boolean isViewFromObject(@NonNull View view, @NonNull Object object) {
            return view == object;
        }

        @NonNull
        @Override
        public Object instantiateItem(@NonNull ViewGroup collection, int position) {
            LayoutInflater inflater = LayoutInflater.from(mActivity);
            View layout;
            if (position == 0) {
                layout = inflater.inflate(R.layout.view_terminal_toolbar_extra_keys, collection, false);
                setupExtraKeysView(mActivity, (ExtraKeysView) layout);
            } else {
                layout = inflater.inflate(R.layout.view_terminal_toolbar_text_input, collection, false);
                final EditText editText = layout.findViewById(R.id.terminal_toolbar_text_input);
                setupTextInputView(mActivity, editText, mSavedTextInput);
                mSavedTextInput = null;
            }
            collection.addView(layout);
            return layout;
        }

        @Override
        public void destroyItem(@NonNull ViewGroup collection, int position, @NonNull Object view) {
            collection.removeView((View) view);
        }

    }

    /**
     * Wire up an {@link ExtraKeysView} (from either the ViewPager page or the stacked toolbar).
     * Sets the client, all-caps style, registers it with the activity and reloads the keys.
     */
    public static void setupExtraKeysView(TermuxActivity activity, ExtraKeysView extraKeysView) {
        extraKeysView.setExtraKeysViewClient(activity.getTermuxTerminalExtraKeys());
        extraKeysView.setButtonTextAllCaps(activity.getProperties().shouldExtraKeysTextBeAllCaps());
        activity.setExtraKeysView(extraKeysView);
        extraKeysView.reload(activity.getTermuxTerminalExtraKeys().getExtraKeysInfo(),
            activity.getTerminalToolbarDefaultHeight());

        // apply extra keys fix if enabled in prefs
        if (activity.getProperties().isUsingFullScreen() && activity.getProperties().isUsingFullScreenWorkAround()) {
            FullScreenWorkAround.apply(activity);
        }
    }

    /**
     * Wire up the text-input {@link EditText} (from either the ViewPager page or the stacked
     * toolbar). Restores any saved text and sends the text to the session on the editor action.
     */
    public static void setupTextInputView(TermuxActivity activity, final EditText editText, String savedTextInput) {
        if (savedTextInput != null) {
            editText.setText(savedTextInput);
        }

        editText.setOnEditorActionListener((v, actionId, event) -> {
            TerminalSession session = activity.getCurrentSession();
            if (session != null) {
                if (session.isRunning()) {
                    String textToSend = editText.getText().toString();
                    if (textToSend.length() == 0) textToSend = "\r";
                    session.write(textToSend);
                } else {
                    activity.getTermuxTerminalSessionClient().removeFinishedSession(session);
                }
                editText.setText("");
            }
            return true;
        });
    }



    public static class OnPageChangeListener extends ViewPager.SimpleOnPageChangeListener {

        final TermuxActivity mActivity;
        final ViewPager mTerminalToolbarViewPager;

        public OnPageChangeListener(TermuxActivity activity, ViewPager viewPager) {
            this.mActivity = activity;
            this.mTerminalToolbarViewPager = viewPager;
        }

        @Override
        public void onPageSelected(int position) {
            if (position == 0) {
                mActivity.getTerminalView().requestFocus();
            } else {
                final EditText editText = mTerminalToolbarViewPager.findViewById(R.id.terminal_toolbar_text_input);
                if (editText != null) editText.requestFocus();
            }
        }

    }

}
