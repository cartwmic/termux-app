package com.termux.app.terminal.io;

import android.content.Context;
import android.graphics.Typeface;
import android.text.Editable;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.TextWatcher;
import android.text.format.DateUtils;
import android.text.style.StyleSpan;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.view.inputmethod.InputMethodManager;
import android.widget.BaseAdapter;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;

import com.google.android.material.bottomsheet.BottomSheetBehavior;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.termux.R;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/**
 * Bottom-sheet history picker for the terminal toolbar text-input box
 * (specs terminal-toolbar.history-picker-search-and-selection,
 * terminal-toolbar.history-entry-deletion-and-clearing,
 * terminal-toolbar.history-picker-affordance).
 *
 * The sheet contains a search field (seeded from the box, owns IME focus while
 * open) filtering the history list live via {@link TextInputHistoryMatcher},
 * with matched characters bolded, dim relative timestamps, long-press-to-delete
 * rows and a clear-all footer. Tapping an entry replaces the box contents with
 * that entry (cursor at end), dismisses the sheet and returns focus + IME to
 * the box. Dismissing without picking leaves the box untouched.
 */
public final class TextInputHistorySheet {

    /** A filtered row: the history entry plus its match result for highlighting. */
    private static final class Row {
        final TextInputHistory.Entry entry;
        final TextInputHistoryMatcher.Result match;

        Row(TextInputHistory.Entry entry, TextInputHistoryMatcher.Result match) {
            this.entry = entry;
            this.match = match;
        }
    }

    private TextInputHistorySheet() {
    }

    /** Open the history picker for the given text-input box. */
    public static void show(final Context context, final EditText targetBox) {
        final BottomSheetDialog dialog = new BottomSheetDialog(context);
        final View content = LayoutInflater.from(context)
            .inflate(R.layout.view_text_input_history_sheet, null, false);
        dialog.setContentView(content);
        dialog.getBehavior().setState(BottomSheetBehavior.STATE_EXPANDED);
        dialog.getBehavior().setSkipCollapsed(true);
        // Keep the list visible above the soft keyboard and give the search
        // field the IME while the sheet is open.
        if (dialog.getWindow() != null) {
            dialog.getWindow().setSoftInputMode(
                WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE
                    | WindowManager.LayoutParams.SOFT_INPUT_STATE_VISIBLE);
        }

        final EditText searchField = content.findViewById(R.id.text_input_history_search);
        final ListView listView = content.findViewById(R.id.text_input_history_list);
        final TextView placeholder = content.findViewById(R.id.text_input_history_placeholder);
        final TextView clearAll = content.findViewById(R.id.text_input_history_clear_all);

        final List<Row> rows = new ArrayList<>();
        final BaseAdapter adapter = new BaseAdapter() {
            @Override
            public int getCount() {
                return rows.size();
            }

            @Override
            public Object getItem(int position) {
                return rows.get(position);
            }

            @Override
            public long getItemId(int position) {
                return position;
            }

            @Override
            public View getView(int position, View convertView, ViewGroup parent) {
                View view = convertView != null ? convertView : LayoutInflater.from(context)
                    .inflate(R.layout.item_text_input_history_entry, parent, false);
                Row row = rows.get(position);
                TextView text = view.findViewById(R.id.text_input_history_entry_text);
                TextView time = view.findViewById(R.id.text_input_history_entry_time);
                text.setText(highlight(row.entry.text, row.match.matchedIndices));
                time.setText(DateUtils.getRelativeTimeSpanString(row.entry.timestamp,
                    System.currentTimeMillis(), DateUtils.MINUTE_IN_MILLIS,
                    DateUtils.FORMAT_ABBREV_RELATIVE));
                return view;
            }
        };
        listView.setAdapter(adapter);

        final Runnable refresh = () -> {
            String query = searchField.getText().toString();
            rows.clear();
            for (TextInputHistory.Entry entry : TextInputHistory.getInstance().snapshot()) {
                TextInputHistoryMatcher.Result match = TextInputHistoryMatcher.match(query, entry.text);
                if (match != null) rows.add(new Row(entry, match));
            }
            // Rank by match score, ties broken by recency (snapshot is already
            // most-recent-first; the timestamp comparison keeps that explicit).
            Collections.sort(rows, (a, b) -> {
                if (a.match.score != b.match.score) return b.match.score - a.match.score;
                return Long.compare(b.entry.timestamp, a.entry.timestamp);
            });
            adapter.notifyDataSetChanged();

            if (TextInputHistory.getInstance().isEmpty()) {
                placeholder.setText(R.string.text_input_history_empty);
                placeholder.setVisibility(View.VISIBLE);
            } else if (rows.isEmpty()) {
                placeholder.setText(R.string.text_input_history_no_matches);
                placeholder.setVisibility(View.VISIBLE);
            } else {
                placeholder.setVisibility(View.GONE);
            }
        };

        // Seed the search field from the box; the box itself is never mutated
        // unless an entry is picked.
        String seed = targetBox.getText().toString();
        searchField.setText(seed);
        searchField.setSelection(seed.length());
        searchField.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
            }

            @Override
            public void afterTextChanged(Editable s) {
                refresh.run();
            }
        });

        listView.setOnItemClickListener((parent, view, position, id) -> {
            String picked = rows.get(position).entry.text;
            targetBox.setText(picked);
            targetBox.setSelection(picked.length());
            dialog.dismiss();
        });

        listView.setOnItemLongClickListener((parent, view, position, id) -> {
            TextInputHistory.getInstance().delete(rows.get(position).entry);
            refresh.run();
            return true;
        });

        clearAll.setOnClickListener(v -> {
            TextInputHistory.getInstance().clear();
            refresh.run();
        });

        // Focus and the soft keyboard return to the box on any dismissal;
        // without a pick the box contents are untouched.
        dialog.setOnDismissListener(d -> {
            targetBox.requestFocus();
            InputMethodManager imm =
                (InputMethodManager) context.getSystemService(Context.INPUT_METHOD_SERVICE);
            if (imm != null) imm.showSoftInput(targetBox, InputMethodManager.SHOW_IMPLICIT);
        });

        refresh.run();
        dialog.show();
        searchField.requestFocus();
    }

    /** Bold the matched character indices for fuzzy-match highlighting. */
    private static CharSequence highlight(String text, int[] matchedIndices) {
        if (matchedIndices.length == 0) return text;
        SpannableString spannable = new SpannableString(text);
        for (int index : matchedIndices) {
            if (index < 0 || index >= text.length()) continue;
            spannable.setSpan(new StyleSpan(Typeface.BOLD), index, index + 1,
                Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
        }
        return spannable;
    }
}
