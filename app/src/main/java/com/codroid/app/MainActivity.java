package com.codroid.app;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.content.Context;
import android.graphics.Color;
import android.graphics.Typeface;
import android.database.Cursor;
import android.widget.Button;
import android.widget.EditText;
import android.widget.HorizontalScrollView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;
import android.provider.OpenableColumns;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;

public class MainActivity extends Activity {
    private static final int REQUEST_OPEN = 1;
    private static final int REQUEST_SAVE = 2;
    private static final String DEFAULT_CODE =
            "# Welcome to Codroid\n" +
            "# Write code here, then use Open or Save to work with files.\n\n" +
            "print(\"Hello, world!\")\n";

    private EditText editor;
    private TextView fileLabel;
    private TextView cursorLabel;
    private Uri currentUri;
    private String currentName = "untitled.py";
    private String savedText = DEFAULT_CODE;
    private boolean loadingDocument;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().setStatusBarColor(Color.rgb(17, 24, 39));
        getWindow().setNavigationBarColor(Color.rgb(17, 24, 39));
        buildEditor();

        if (savedInstanceState != null) {
            currentName = savedInstanceState.getString("fileName", currentName);
            savedText = savedInstanceState.getString("savedText", savedText);
            String uri = savedInstanceState.getString("fileUri");
            currentUri = uri == null ? null : Uri.parse(uri);
            editor.setText(savedInstanceState.getString("editorText", DEFAULT_CODE));
            editor.setSelection(editor.length());
        } else {
            editor.setText(DEFAULT_CODE);
        }
        updateFileLabel();
        updateCursorLabel();
    }

    private void buildEditor() {
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(Color.rgb(15, 23, 42));
        root.setPadding(dp(16), dp(12), dp(16), dp(10));
        if (Build.VERSION.SDK_INT >= 35) {
            root.setOnApplyWindowInsetsListener((view, insets) -> {
                android.graphics.Insets bars = insets.getInsets(
                        android.view.WindowInsets.Type.systemBars());
                view.setPadding(dp(16), dp(12) + bars.top,
                        dp(16), dp(10) + bars.bottom);
                return insets;
            });
        }

        TextView title = new TextView(this);
        title.setText("CODROID");
        title.setTextColor(Color.rgb(24, 201, 154));
        title.setTextSize(13);
        title.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        title.setLetterSpacing(0.14f);
        root.addView(title, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, dp(24)));

        fileLabel = new TextView(this);
        fileLabel.setTextColor(Color.WHITE);
        fileLabel.setTextSize(20);
        fileLabel.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        fileLabel.setSingleLine(true);
        root.addView(fileLabel, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, dp(36)));

        HorizontalScrollView actionsScroll = new HorizontalScrollView(this);
        actionsScroll.setHorizontalScrollBarEnabled(false);
        LinearLayout actions = new LinearLayout(this);
        actions.setGravity(Gravity.CENTER_VERTICAL);
        actions.setOrientation(LinearLayout.HORIZONTAL);
        addAction(actions, "New", v -> createNewFile());
        addAction(actions, "Open", v -> openFile());
        addAction(actions, "Tab", v -> insertIndent());
        addAction(actions, "Save", v -> saveFile());
        actionsScroll.addView(actions);
        LinearLayout.LayoutParams actionsParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, dp(52));
        actionsParams.bottomMargin = dp(8);
        root.addView(actionsScroll, actionsParams);

        ScrollView editorScroll = new ScrollView(this);
        editorScroll.setFillViewport(true);
        editorScroll.setBackgroundColor(Color.rgb(24, 32, 48));
        editor = new CodeEditText(this);
        editor.setGravity(Gravity.TOP | Gravity.START);
        editor.setTextColor(Color.rgb(226, 232, 240));
        editor.setHintTextColor(Color.rgb(120, 132, 150));
        editor.setHint("Start writing code...");
        editor.setTextSize(15);
        editor.setTypeface(Typeface.MONOSPACE);
        editor.setPadding(dp(14), dp(14), dp(14), dp(14));
        editor.setBackgroundColor(Color.TRANSPARENT);
        editor.setInputType(android.text.InputType.TYPE_CLASS_TEXT
                | android.text.InputType.TYPE_TEXT_FLAG_MULTI_LINE
                | android.text.InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS);
        editor.setSingleLine(false);
        editor.setHorizontallyScrolling(true);
        editor.setVerticalScrollBarEnabled(true);
        editorScroll.addView(editor, new ScrollView.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
        root.addView(editorScroll, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, 0, 1));

        cursorLabel = new TextView(this);
        cursorLabel.setTextColor(Color.rgb(148, 163, 184));
        cursorLabel.setTextSize(12);
        cursorLabel.setGravity(Gravity.CENTER_VERTICAL);
        root.addView(cursorLabel, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, dp(32)));
        setContentView(root);

        editor.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) { }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                if (!loadingDocument) {
                    updateFileLabel();
                    updateCursorLabel();
                }
            }

            @Override
            public void afterTextChanged(Editable s) { }
        });
    }

    private void addAction(LinearLayout parent, String label, View.OnClickListener listener) {
        Button button = new Button(this);
        button.setText(label);
        button.setTextColor(Color.WHITE);
        button.setAllCaps(false);
        button.setTextSize(14);
        button.setMinHeight(dp(44));
        button.setPadding(dp(12), 0, dp(12), 0);
        button.setBackgroundTintList(android.content.res.ColorStateList.valueOf(
                label.equals("Save") ? Color.rgb(16, 145, 116) : Color.rgb(51, 65, 85)));
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, dp(44));
        params.rightMargin = dp(8);
        parent.addView(button, params);
        button.setOnClickListener(listener);
    }

    private void createNewFile() {
        if (!isDirty()) {
            resetEditor();
            return;
        }
        new AlertDialog.Builder(this)
                .setTitle("Discard unsaved changes?")
                .setMessage("Your changes to " + currentName + " have not been saved.")
                .setNegativeButton("Cancel", null)
                .setPositiveButton("Discard", (dialog, which) -> resetEditor())
                .show();
    }

    private void resetEditor() {
        loadingDocument = true;
        currentUri = null;
        currentName = "untitled.py";
        savedText = DEFAULT_CODE;
        editor.setText(DEFAULT_CODE);
        editor.setSelection(editor.length());
        loadingDocument = false;
        updateFileLabel();
        updateCursorLabel();
    }

    private void openFile() {
        if (isDirty()) {
            new AlertDialog.Builder(this)
                    .setTitle("Discard unsaved changes?")
                    .setMessage("Open another file and discard changes to " + currentName + "?")
                    .setNegativeButton("Cancel", null)
                    .setPositiveButton("Discard", (dialog, which) -> startOpenIntent())
                    .show();
        } else {
            startOpenIntent();
        }
    }

    private void startOpenIntent() {
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType("*/*");
        startActivityForResult(intent, REQUEST_OPEN);
    }

    private void saveFile() {
        if (currentUri == null) {
            Intent intent = new Intent(Intent.ACTION_CREATE_DOCUMENT);
            intent.addCategory(Intent.CATEGORY_OPENABLE);
            intent.setType("text/plain");
            intent.putExtra(Intent.EXTRA_TITLE, currentName);
            startActivityForResult(intent, REQUEST_SAVE);
        } else {
            writeFile(currentUri);
        }
    }

    private void insertIndent() {
        int start = Math.max(0, editor.getSelectionStart());
        int end = Math.max(start, editor.getSelectionEnd());
        editor.getText().replace(start, end, "    ");
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode != RESULT_OK || data == null || data.getData() == null) return;

        Uri uri = data.getData();
        if (requestCode == REQUEST_OPEN) {
            try {
                persistUriPermission(uri, data.getFlags());
                String content = readFile(uri);
                loadingDocument = true;
                currentUri = uri;
                currentName = displayName(uri);
                editor.setText(content);
                editor.setSelection(editor.length());
                savedText = content;
                loadingDocument = false;
                updateFileLabel();
                updateCursorLabel();
                hideKeyboard();
            } catch (Exception e) {
                loadingDocument = false;
                showMessage("Could not open file: " + e.getMessage());
            }
        } else if (requestCode == REQUEST_SAVE) {
            try {
                persistUriPermission(uri, data.getFlags());
                currentUri = uri;
                currentName = displayName(uri);
                writeFile(uri);
            } catch (Exception e) {
                showMessage("Could not keep access to file: " + e.getMessage());
            }
        }
    }

    private void persistUriPermission(Uri uri, int resultFlags) {
        int flags = resultFlags & (Intent.FLAG_GRANT_READ_URI_PERMISSION
                | Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
        getContentResolver().takePersistableUriPermission(uri, flags);
    }

    private String readFile(Uri uri) throws Exception {
        InputStream input = getContentResolver().openInputStream(uri);
        if (input == null) throw new IllegalStateException("File could not be read.");
        StringBuilder text = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(input, StandardCharsets.UTF_8))) {
            char[] buffer = new char[4096];
            int count;
            while ((count = reader.read(buffer)) != -1) text.append(buffer, 0, count);
        }
        return text.toString();
    }

    private void writeFile(Uri uri) {
        try {
            OutputStream output = getContentResolver().openOutputStream(uri, "wt");
            if (output == null) throw new IllegalStateException("File could not be written.");
            try (OutputStream stream = output) {
                stream.write(editor.getText().toString().getBytes(StandardCharsets.UTF_8));
                stream.flush();
            }
            savedText = editor.getText().toString();
            currentName = displayName(uri);
            updateFileLabel();
            showMessage("Saved " + currentName);
        } catch (Exception e) {
            showMessage("Could not save file: " + e.getMessage());
        }
    }

    private String displayName(Uri uri) throws Exception {
        try (Cursor cursor = getContentResolver().query(uri,
                new String[]{OpenableColumns.DISPLAY_NAME}, null, null, null)) {
            if (cursor != null && cursor.moveToFirst()) {
                int column = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME);
                if (column >= 0) {
                    String name = cursor.getString(column);
                    if (name != null && !name.trim().isEmpty()) return name;
                }
            }
        }
        String name = uri.getLastPathSegment();
        if (name == null || name.trim().isEmpty()) return "code.txt";
        int separator = name.lastIndexOf('/');
        return separator >= 0 ? name.substring(separator + 1) : name;
    }

    private boolean isDirty() {
        return !editor.getText().toString().equals(savedText);
    }

    private void updateFileLabel() {
        if (fileLabel != null) fileLabel.setText(currentName + (isDirty() ? " *" : ""));
    }

    private void updateCursorLabel() {
        if (editor == null || cursorLabel == null) return;
        int position = Math.max(0, editor.getSelectionStart());
        CharSequence content = editor.getText();
        int line = 1;
        int column = 1;
        for (int i = 0; i < Math.min(position, content.length()); i++) {
            if (content.charAt(i) == '\n') {
                line++;
                column = 1;
            } else {
                column++;
            }
        }
        cursorLabel.setText("Ln " + line + ", Col " + column
                + (isDirty() ? "   •   Unsaved changes" : "   •   Ready"));
    }

    private void hideKeyboard() {
        InputMethodManager manager = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
        if (manager != null) manager.hideSoftInputFromWindow(editor.getWindowToken(), 0);
    }

    private void showMessage(String message) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_S && event.isCtrlPressed()) {
            saveFile();
            return true;
        }
        if (keyCode == KeyEvent.KEYCODE_O && event.isCtrlPressed()) {
            openFile();
            return true;
        }
        return super.onKeyDown(keyCode, event);
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        outState.putString("editorText", editor.getText().toString());
        outState.putString("savedText", savedText);
        outState.putString("fileName", currentName);
        if (currentUri != null) outState.putString("fileUri", currentUri.toString());
        super.onSaveInstanceState(outState);
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }

    private final class CodeEditText extends EditText {
        CodeEditText(Context context) {
            super(context);
        }

        @Override
        protected void onSelectionChanged(int start, int end) {
            super.onSelectionChanged(start, end);
            updateCursorLabel();
        }
    }
}
