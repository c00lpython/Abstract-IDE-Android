package com.xcore.abstractide.ui.project;

import android.app.AlertDialog;
import android.content.Context;
import android.graphics.Color;
import android.text.InputType;
import android.view.View;
import android.widget.*;

public class NewProjectDialog {

    public interface OnProjectCreatedListener {
        void onCreated(String projectId);
    }

    public static void show(Context context, com.xcore.abstractide.core.project.ProjectManager projectManager,
                            OnProjectCreatedListener listener) {
        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder.setTitle("New Project");

        LinearLayout layout = new LinearLayout(context);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(40, 20, 40, 20);

        // Name
        TextView lblName = new TextView(context);
        lblName.setText("Name");
        lblName.setTextColor(Color.parseColor("#cccccc"));
        lblName.setTextSize(12);
        layout.addView(lblName);

        EditText etName = new EditText(context);
        etName.setText("Untitled");
        etName.setTextColor(Color.WHITE);
        etName.setBackgroundColor(Color.parseColor("#2d2d2d"));
        etName.setPadding(20, 12, 20, 12);
        layout.addView(etName);
        layout.addView(space(context, 12));

        // Language
        TextView lblLang = new TextView(context);
        lblLang.setText("Language");
        lblLang.setTextColor(Color.parseColor("#cccccc"));
        lblLang.setTextSize(12);
        layout.addView(lblLang);

        Spinner spLanguage = new Spinner(context);
        String[] langs = {"Python", "JavaScript", "Java", "C++", "Kotlin", "TypeScript"};
        ArrayAdapter<String> langAdapter = new ArrayAdapter<>(context,
                android.R.layout.simple_spinner_item, langs);
        langAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spLanguage.setAdapter(langAdapter);
        spLanguage.setBackgroundColor(Color.parseColor("#2d2d2d"));
        layout.addView(spLanguage);
        layout.addView(space(context, 12));

        // Git support
        CheckBox cbGit = new CheckBox(context);
        cbGit.setText("Git support (need login)");
        cbGit.setTextColor(Color.parseColor("#cccccc"));
        layout.addView(cbGit);
        layout.addView(space(context, 12));

        // Path of builds
        TextView lblPath = new TextView(context);
        lblPath.setText("Path of builds");
        lblPath.setTextColor(Color.parseColor("#cccccc"));
        lblPath.setTextSize(12);
        layout.addView(lblPath);

        LinearLayout pathRow = new LinearLayout(context);
        pathRow.setOrientation(LinearLayout.HORIZONTAL);

        EditText etPath = new EditText(context);
        etPath.setText("/storage/emulated/0/AbstractIDE/builds");
        etPath.setTextColor(Color.WHITE);
        etPath.setBackgroundColor(Color.parseColor("#2d2d2d"));
        etPath.setPadding(20, 12, 20, 12);
        LinearLayout.LayoutParams pathParams = new LinearLayout.LayoutParams(0,
                LinearLayout.LayoutParams.WRAP_CONTENT, 1);
        etPath.setLayoutParams(pathParams);
        pathRow.addView(etPath);

        Button btnBrowse = new Button(context);
        btnBrowse.setText("📁");
        btnBrowse.setTextColor(Color.WHITE);
        btnBrowse.setBackgroundColor(Color.parseColor("#3a3a3a"));
        btnBrowse.setOnClickListener(v ->
                Toast.makeText(context, "Browse coming soon", Toast.LENGTH_SHORT).show());
        pathRow.addView(btnBrowse);

        layout.addView(pathRow);
        layout.addView(space(context, 12));

        // COOP section
        CheckBox cbCoop = new CheckBox(context);
        cbCoop.setText("COOP (need to login)");
        cbCoop.setTextColor(Color.parseColor("#cccccc"));
        layout.addView(cbCoop);

        LinearLayout coopFields = new LinearLayout(context);
        coopFields.setOrientation(LinearLayout.VERTICAL);
        coopFields.setVisibility(View.GONE);
        coopFields.setPadding(20, 0, 0, 0);

        EditText etIP = new EditText(context);
        etIP.setHint("IP");
        etIP.setHintTextColor(Color.parseColor("#888888"));
        etIP.setTextColor(Color.WHITE);
        etIP.setBackgroundColor(Color.parseColor("#2d2d2d"));
        etIP.setPadding(20, 12, 20, 12);
        coopFields.addView(etIP);

        EditText etLogin = new EditText(context);
        etLogin.setHint("Login");
        etLogin.setHintTextColor(Color.parseColor("#888888"));
        etLogin.setTextColor(Color.WHITE);
        etLogin.setBackgroundColor(Color.parseColor("#2d2d2d"));
        etLogin.setPadding(20, 12, 20, 12);
        coopFields.addView(etLogin);

        EditText etPassword = new EditText(context);
        etPassword.setHint("Password");
        etPassword.setHintTextColor(Color.parseColor("#888888"));
        etPassword.setTextColor(Color.WHITE);
        etPassword.setBackgroundColor(Color.parseColor("#2d2d2d"));
        etPassword.setPadding(20, 12, 20, 12);
        etPassword.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
        coopFields.addView(etPassword);

        CheckBox cbRemember = new CheckBox(context);
        cbRemember.setText("Remember");
        cbRemember.setTextColor(Color.parseColor("#cccccc"));
        coopFields.addView(cbRemember);

        CheckBox cbAssociation = new CheckBox(context);
        cbAssociation.setText("Make an association?");
        cbAssociation.setTextColor(Color.parseColor("#cccccc"));
        coopFields.addView(cbAssociation);

        layout.addView(coopFields);

        cbCoop.setOnCheckedChangeListener((button, checked) -> {
            coopFields.setVisibility(checked ? View.VISIBLE : View.GONE);
        });

        layout.addView(space(context, 20));

        ScrollView scroll = new ScrollView(context);
        scroll.addView(layout);
        builder.setView(scroll);

        builder.setPositiveButton("Create", (dialog, which) -> {
            String name = etName.getText().toString().trim();
            if (name.isEmpty()) name = "Untitled";

            var project = projectManager.newProject(name);
            projectManager.saveProject();

            if (listener != null) {
                listener.onCreated(project.getId());
            }
        });

        builder.setNegativeButton("Cancel", null);
        builder.show();
    }

    private static View space(Context context, int dp) {
        View v = new View(context);
        v.setLayoutParams(new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, dp));
        return v;
    }
}