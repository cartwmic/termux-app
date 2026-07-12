package com.termux.app.terminal.io;

import android.graphics.drawable.Drawable;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;

import java.util.ArrayList;
import java.util.List;

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
                    String enteredText = editText.getText().toString();
                    // Record the user-typed line (record() ignores empty text, so the
                    // bare-"\r" empty send is never captured). Capture must not alter
                    // the write semantics below.
                    TextInputHistory.getInstance().record(enteredText);
                    String textToSend = enteredText;
                    if (textToSend.length() == 0) textToSend = "\r";
                    session.write(textToSend);
                } else {
                    activity.getTermuxTerminalSessionClient().removeFinishedSession(session);
                }
                editText.setText("");
            }
            return true;
        });

        setupHistoryCycling(editText);
        setupHistoryPickerIcon(activity, editText);
    }

    /**
     * Wire the history icon rendered as the EditText's end compound drawable
     * (spec terminal-toolbar.history-picker-affordance). Taps landing inside
     * the icon's touch region open the picker sheet; all other touches fall
     * through untouched, so stock long-press/text-selection behavior and the
     * view id are unchanged. The icon lives in the shared layout, so it is
     * present in both toolbar modes and regardless of history emptiness.
     */
    private static void setupHistoryPickerIcon(final TermuxActivity activity, final EditText editText) {
        // Tracks whether the current gesture's ACTION_DOWN landed in the icon
        // region; the picker opens only for a short tap fully inside it.
        final boolean[] downInIconRegion = {false};

        editText.setOnTouchListener((v, event) -> {
            Drawable endDrawable = editText.getCompoundDrawablesRelative()[2];
            if (endDrawable == null) return false;
            int touchRegionWidth = endDrawable.getBounds().width()
                + editText.getPaddingEnd() + editText.getCompoundDrawablePadding();
            boolean rtl = editText.getLayoutDirection() == View.LAYOUT_DIRECTION_RTL;
            boolean inIconRegion = rtl
                ? event.getX() <= touchRegionWidth
                : event.getX() >= editText.getWidth() - touchRegionWidth;

            // Never consume DOWN/MOVE: stock cursor placement, text selection
            // and long-press (which fires at the long-press timeout and shows
            // the paste/selection menu even over the icon) all stay intact.
            if (event.getAction() == MotionEvent.ACTION_DOWN) {
                downInIconRegion[0] = inIconRegion;
                return false;
            }
            if (event.getAction() != MotionEvent.ACTION_UP) return false;

            // Open only when the gesture began AND ended inside the icon
            // region and released before the long-press timeout — a gesture
            // that started on the text, or a long-press over the icon, falls
            // through to stock EditText handling untouched.
            boolean shortTap = event.getEventTime() - event.getDownTime()
                < android.view.ViewConfiguration.getLongPressTimeout();
            if (downInIconRegion[0] && inIconRegion && shortTap) {
                downInIconRegion[0] = false;
                // The unconsumed DOWN armed the EditText's pending long-press
                // check; cancel it so a tap held near the timeout cannot fire
                // the stock menu concurrently with the sheet opening.
                editText.cancelLongPress();
                v.performClick();
                TextInputHistorySheet.show(activity, editText);
                return true;
            }
            downInIconRegion[0] = false;
            return false;
        });
    }

    /**
     * Wire readline-style hardware-keyboard Up/Down history cycling on the
     * text-input box (spec terminal-toolbar.hardware-keyboard-history-cycling).
     * Any key we do not consume returns false so stock EditText behavior is
     * untouched; extra-key arrow buttons never pass through here — they write
     * escape codes directly to the terminal session.
     */
    private static void setupHistoryCycling(final EditText editText) {
        final TextInputHistoryNavigator navigator = new TextInputHistoryNavigator();
        // Distinguishes navigator-driven setText calls from user edits.
        final boolean[] programmaticSet = {false};

        editText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
            }

            @Override
            public void afterTextChanged(Editable s) {
                if (!programmaticSet[0]) navigator.onUserEdit();
            }
        });

        editText.setOnKeyListener((v, keyCode, event) -> {
            if (event.getAction() != KeyEvent.ACTION_DOWN) return false;
            final String replacement;
            if (keyCode == KeyEvent.KEYCODE_DPAD_UP) {
                List<String> texts = new ArrayList<>();
                for (TextInputHistory.Entry entry : TextInputHistory.getInstance().snapshot())
                    texts.add(entry.text);
                replacement = navigator.up(editText.getText().toString(), texts);
            } else if (keyCode == KeyEvent.KEYCODE_DPAD_DOWN) {
                replacement = navigator.down();
            } else {
                return false;
            }
            if (replacement == null) return navigator.isNavigating();
            programmaticSet[0] = true;
            editText.setText(replacement);
            editText.setSelection(replacement.length());
            programmaticSet[0] = false;
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
