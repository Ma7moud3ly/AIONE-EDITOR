package com.ma7moud3ly.aione_editor;

import android.Manifest;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.Rect;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.text.Editable;
import android.text.InputType;
import android.text.TextWatcher;
import android.text.method.ScrollingMovementMethod;
import android.util.DisplayMetrics;
import android.util.Log;
import android.util.TypedValue;
import android.view.View;
import android.view.ViewTreeObserver;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.FileProvider;

public class EditorActivity extends AppCompatActivity {
    private TextView title, lines;
    private EditText editor;
    public String name = "", path = "";
    private File script;
    private int fontSize;
    private boolean isDark = false;
    private boolean show_lines = true;
    public static final String scripts_path = Environment.getExternalStorageDirectory() + "/AIONE-EDITOR";

    private final int ZOOMVALUE = 2;
    private final int SHR = 0;
    private final int LIS = 1;
    private final int NEW = 2;
    private final int SAV = 3;
    private int AFTER_SAVE = -1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        getEditorSettings();
        super.onCreate(savedInstanceState);
        setContentView(R.layout.editor);
        isStoragePermissionGranted();
        LinearLayout layout = findViewById(R.id.editor_layout);
        if (isDark) layout.setBackgroundColor(getResources().getColor(android.R.color.black));
        else layout.setBackgroundColor(getResources().getColor(android.R.color.white));
        lines = findViewById(R.id.lines);
        lines.setTextSize(TypedValue.COMPLEX_UNIT_DIP, fontSize);
        lines.setVerticalScrollBarEnabled(true);
        lines.setMovementMethod(new ScrollingMovementMethod());
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            lines.setOnScrollChangeListener(new View.OnScrollChangeListener() {
                @Override
                public void onScrollChange(View view, int i, int i1, int i2, int i3) {
                    editor.setScrollY(i1);
                }
            });
        }
        title = findViewById(R.id.script_title);
        init_editor();
    }

    private void init_editor() {
        final View header = findViewById(R.id.header);
        final View about = findViewById(R.id.about_btn);
        final View insert_btns = findViewById(R.id.insert_btns);
        insert_btns.setVisibility(View.GONE);
        editor = findViewById(R.id.editor);
        editor.setTextSize(TypedValue.COMPLEX_UNIT_DIP, fontSize);
        editor.getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
            @Override
            public void onGlobalLayout() {
                if (keyboardShown(editor.getRootView())) {
                    // insert_btns.setVisibility(View.VISIBLE);
                    header.setVisibility(View.GONE);
                    about.setVisibility(View.GONE);
                } else {
                    header.setVisibility(View.VISIBLE);
                    about.setVisibility(View.VISIBLE);
                    //insert_btns.setVisibility(View.GONE);
                }
            }
        });
        editor.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {

            }

            @Override
            public void onTextChanged(CharSequence charSequence, int j, int i1, int i2) {
            }

            @Override
            public void afterTextChanged(Editable editable) {
                if (!show_lines) return;
                lines.setText("");
                int n = editor.getLineCount();
                for (int i = 1; i <= n; i++) lines.append(((i < 10) ? "0" : "") + i + "\n");
            }
        });
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            editor.setOnScrollChangeListener(new View.OnScrollChangeListener() {
                @Override
                public void onScrollChange(View view, int i, int i1, int i2, int i3) {
                    lines.setScrollY(i1);
                }
            });
        }
        show_lines(show_lines);
        count_lines();
        getScript();
    }

    private boolean keyboardShown(View rootView) {
        final int softKeyboardHeight = 100;
        Rect r = new Rect();
        rootView.getWindowVisibleDisplayFrame(r);
        DisplayMetrics dm = rootView.getResources().getDisplayMetrics();
        int heightDiff = rootView.getBottom() - r.bottom;
        return heightDiff > softKeyboardHeight * dm.density;
    }

    private void count_lines() {
        if (!show_lines) return;
        lines.postDelayed(new Runnable() {
            @Override
            public void run() {
                lines.setText("");
                int n = editor.getLineCount();
                String format = (n > 99) ? "%03d" : "%02d";
                for (int i = 1; i <= n; i++) {
                    String num = String.format(format, i);
                    lines.append(num + "\n");
                }
            }
        }, 500);
    }

    private void show_lines(boolean b) {
        if (b) lines.setVisibility(View.VISIBLE);
        else lines.setVisibility(View.GONE);
    }

    private void getScript() {
        final Intent intent = getIntent();
        final String action = intent.getAction();
        final Bundle bundle = intent.getExtras();
        /*if (bundle != null)
            for (String key : bundle.keySet())
                Log.e("HINT", key + " : " + (bundle.get(key) != null ? bundle.get(key) : "NULL"));*/
        if (Intent.ACTION_VIEW.equals(action)) {
            //Log.i("HINT", "DATA");
            if (intent.getData() != null) return;
            path = intent.getData().getPath();
            path = path.replace("/root", "");
            File f = new File(path);
            name = f.getName();
        } else if (Intent.ACTION_SEND.equals(action)) {
            //Log.i("HINT", "SEND");
            if (intent.hasExtra(Intent.EXTRA_TEXT)) {
                String txt = bundle.getString(Intent.EXTRA_TEXT);
                if (txt != null) editor.setText(txt);
                path = "";
                name = "";
            } else if (intent.hasExtra(Intent.EXTRA_STREAM)) {
                Uri uri = (Uri) bundle.get(Intent.EXTRA_STREAM);
                path = getRealPathFromURI(this, uri);
                script = new File(path);
                name = script.getName();
            }
        } else if (intent.hasExtra("path") && intent.hasExtra("name")) {
            path = intent.getStringExtra("path");
            name = intent.getStringExtra("name");
        } else {
            //Log.i("HINT", "NONE");
        }
        if (!path.equals("")) {
            String txt = Scripts.read(path);
            editor.setText(txt);
            title.setText(name);
            script = new File(path);
        }

    }

    private String getRealPathFromURI(Context context, Uri contentUri) {
        if (contentUri != null && contentUri.getPath().startsWith("/storage"))
            return contentUri.getPath();
        Cursor cursor = null;
        try {
            String[] proj = {MediaStore.Images.Media.DATA};
            cursor = context.getContentResolver().query(contentUri, proj, null, null, null);
            int column_index = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATA);
            cursor.moveToFirst();
            return cursor.getString(column_index);
        } catch (Exception e) {
            e.printStackTrace();
            return "";
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
    }

    private void zoomIn(boolean in) {
        if (in && fontSize < 100) fontSize += ZOOMVALUE;
        else if (!in && fontSize > 8) fontSize -= ZOOMVALUE;
        editor.setTextSize(TypedValue.COMPLEX_UNIT_DIP, fontSize);
        lines.setTextSize(TypedValue.COMPLEX_UNIT_DIP, fontSize);
        count_lines();
    }

    public void insert(View v) {
        editor.getText().insert(editor.getSelectionEnd(), ((TextView) v).getText());
    }

    public void about(View v) {
        startActivity(new Intent(this, AboutActivity.class));
    }

    public void edButtons(View v) {
        switch (v.getId()) {
            case R.id.edNew://save && open new
                AFTER_SAVE = NEW;
                check_save();
                break;
            case R.id.script_title://show script list
                AFTER_SAVE = LIS;
                check_save();
                break;
            case R.id.edShare://save and share file
                AFTER_SAVE = SHR;
                check_save();
                break;
            case R.id.edSave://save only
                AFTER_SAVE = SAV;
                if (script != null) save();
                else save_as_dialog();
                break;
            case R.id.edClear:
                editor.setText("");
                break;
            case R.id.edZoomIn:
                zoomIn(true);
                break;
            case R.id.edZoomOut:
                zoomIn(false);
                break;
            case R.id.edDarkMode:
                isDark = !isDark;
                super.recreate();
                break;
            case R.id.edLines:
                show_lines = !show_lines;
                show_lines(show_lines);
                if (show_lines) count_lines();
                break;
        }

    }

    private void check_save() {
        final String txt = editor.getText().toString();
        boolean empty_file = (txt.isEmpty() && script == null);
        boolean no_changes = (script != null && txt.equals(Scripts.read(path)));
        if (empty_file || no_changes) after_save();
        else save_dialog();
    }

    private void save() {
        Scripts.write(path, editor.getText().toString());
        Toast.makeText(getApplicationContext(), name + " saved", Toast.LENGTH_SHORT).show();
        setEditorSettings();
        after_save();
    }

    private void after_save() {
        int after_save = AFTER_SAVE;
        AFTER_SAVE = -1;
        switch (after_save) {
            case SHR:
                shareScript(this, path);
                break;
            case LIS:
                scriptList();
                break;
            case NEW:
                newScript();
                break;
            case SAV:
                break;
            default:
                finish();
                break;
        }
    }


    private void save_as_dialog() {
        AlertDialog.Builder alert = new AlertDialog.Builder(this, isDark ? R.style.AlertThemeDark : R.style.AlertThemeLight);
        final EditText fname = new EditText(this);
        fname.setText("untitled");
        fname.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_CLASS_TEXT);
        fname.setHint("title");
        fname.setSelectAllOnFocus(true);
        final EditText ext = new EditText(this);
        ext.setText(".txt");
        ext.setHint("extension");
        ext.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_CLASS_TEXT);

        final LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.addView(fname);
        layout.addView(ext);

        alert.setView(layout);
        alert.setMessage("write file name");
        alert.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.dismiss();
                after_save();
            }
        });
        alert.setPositiveButton("Save", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                try {
                    name = fname.getText().toString().trim() + ext.getText().toString().trim();
                    title.setText(name);
                    path = scripts_path + "/" + name;
                    script = new File(path);
                    save();
                } catch (Exception ee) {
                    Toast.makeText(getApplicationContext(), "invalid file name", Toast.LENGTH_LONG).show();
                    ee.printStackTrace();
                    save_as_dialog();
                }
            }
        });
        alert.show();
    }

    private void save_dialog() {
        AlertDialog.Builder alert = new AlertDialog.Builder(this, isDark ? R.style.AlertThemeDark : R.style.AlertThemeLight);
        alert.setTitle("Wait !");
        alert.setIcon(R.drawable.ic_baseline_error_outline_24);
        final boolean is_file_new = (script == null && path.equals(""));
        alert.setMessage(is_file_new ? "Do you want to save this file ?" : "Do you want to save changes ?");
        alert.setPositiveButton("Yes", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                if (is_file_new) save_as_dialog();
                else save();
            }
        });
        alert.setNegativeButton("No", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                after_save();
            }
        });
        alert.show();
    }

    public static void shareScript(Context context, final String mpath) {
        if (mpath == "") return;
        Intent shareIntent = new Intent();
        shareIntent.setAction(Intent.ACTION_SEND);
        Uri uri = Uri.fromFile(new File(mpath));
        shareIntent.putExtra(Intent.EXTRA_STREAM, uri);
        shareIntent.setType("text/*");
        context.startActivity(Intent.createChooser(shareIntent, context.getResources().getString(R.string.app_name)));
    }

    private void newScript() {
        editor.setText("");
        title.setText("untitled");
        script = null;
        name = "";
        path = "";
    }

    private void scriptList() {
        startActivity(new Intent(this, ScriptsActivity.class));
    }


    private void getEditorSettings() {
        SharedPreferences sharedPref = getPreferences(Context.MODE_PRIVATE);
        fontSize = sharedPref.getInt("font_size", 12);
        isDark = sharedPref.getBoolean("dark_mode", false);
        show_lines = sharedPref.getBoolean("show_lines", true);
        path = sharedPref.getString("path", "");
        name = sharedPref.getString("name", "");
        if (isDark) super.setTheme(R.style.AppThemeDark);
        else super.setTheme(R.style.AppThemeLight);
    }

    private void setEditorSettings() {
        SharedPreferences.Editor sharedPrefEditor = this.getPreferences(Context.MODE_PRIVATE).edit();
        sharedPrefEditor.putInt("font_size", fontSize);
        sharedPrefEditor.putBoolean("dark_mode", isDark);
        sharedPrefEditor.putBoolean("show_lines", show_lines);
        if (!path.equals("") && script.exists()) {
            sharedPrefEditor.putString("path", path);
            sharedPrefEditor.putString("name", name);
        } else {
            sharedPrefEditor.putString("path", "");
            sharedPrefEditor.putString("name", "");
        }
        sharedPrefEditor.commit();
    }

    @Override
    public void finish() {
        super.finish();
    }

    @Override
    protected void onDestroy() {
        setEditorSettings();
        super.onDestroy();
    }

    @Override
    public void onBackPressed() {
        check_save();
    }

    private boolean isStoragePermissionGranted() {
        if (Build.VERSION.SDK_INT >= 23) {
            if (checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE)
                    == PackageManager.PERMISSION_GRANTED && checkSelfPermission(Manifest.permission.READ_EXTERNAL_STORAGE)
                    == PackageManager.PERMISSION_GRANTED) {
                return true;
            } else {
                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.
                        WRITE_EXTERNAL_STORAGE, Manifest.permission.READ_EXTERNAL_STORAGE}, 1);
                return false;
            }
        } else {
            return true;
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions,
                                           int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            Toast.makeText(getApplicationContext(), "You can store and retrieve files now :)", Toast.LENGTH_LONG).show();
            File dir = new File(scripts_path);
            if (!dir.exists()) dir.mkdirs();
        } else {
            Toast.makeText(getApplicationContext(), "You must enable the external storage permission" +
                    " to store and retrieve files", Toast.LENGTH_LONG).show();
            finish();
        }
    }


}
