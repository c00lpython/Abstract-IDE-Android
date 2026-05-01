package com.xcore.abstractide.ui.project;

import android.content.Intent;
import android.os.Bundle;
import android.widget.*;
import androidx.appcompat.app.AppCompatActivity;

import com.xcore.abstractide.MainActivity;
import com.xcore.abstractide.R;
import com.xcore.abstractide.core.project.ProjectManager;

import org.json.JSONArray;
import org.json.JSONObject;
import java.util.*;

public class StartActivity extends AppCompatActivity {

    private static final String PREFS = "AbstractIDE";
    private ProjectManager projectManager;
    private List<Map<String, String>> recent = new ArrayList<>();
    private ArrayAdapter<String> adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_start);

        projectManager = new ProjectManager();
        loadRecent();

        Button btnNew = findViewById(R.id.btnNewProject);
        Button btnOpen = findViewById(R.id.btnOpenProject);
        ListView list = findViewById(R.id.listRecentProjects);

        adapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1);
        list.setAdapter(adapter);
        updateList();

        list.setOnItemClickListener((p, v, pos, id) -> {
            if (pos < recent.size()) {
                String projectId = recent.get(pos).get("id");
                openMainActivity(projectId);
            }
        });

        btnNew.setOnClickListener(v -> {
            NewProjectDialog.show(StartActivity.this, projectManager, projectId -> {
                addRecent(projectManager.getCurrentProject().getName(), projectId);
                openMainActivity(projectId);
            });
        });

        btnOpen.setOnClickListener(v ->
                Toast.makeText(this, "Load .abstract file coming soon", Toast.LENGTH_SHORT).show());
    }

    private void openMainActivity(String projectId) {
        Intent intent = new Intent(this, MainActivity.class);
        intent.putExtra("project_id", projectId);
        startActivity(intent);
    }

    private void updateList() {
        adapter.clear();
        if (recent.isEmpty()) adapter.add("No recent projects");
        else for (Map<String, String> p : recent) adapter.add(p.get("name"));
        adapter.notifyDataSetChanged();
    }

    private void loadRecent() {
        try {
            JSONArray arr = new JSONArray(getSharedPreferences(PREFS, MODE_PRIVATE).getString("recent", "[]"));
            for (int i = 0; i < arr.length(); i++) {
                JSONObject o = arr.getJSONObject(i);
                Map<String, String> m = new LinkedHashMap<>();
                m.put("name", o.getString("name"));
                m.put("id", o.getString("id"));
                m.put("modified", o.optString("modified", ""));
                recent.add(m);
            }
        } catch (Exception e) {
            recent = new ArrayList<>();
        }
    }

    private void saveRecent() {
        JSONArray arr = new JSONArray();
        for (Map<String, String> p : recent) {
            try {
                arr.put(new JSONObject().put("name", p.get("name")).put("id", p.get("id")).put("modified", p.get("modified")));
            } catch (Exception ignored) {}
        }
        getSharedPreferences(PREFS, MODE_PRIVATE).edit().putString("recent", arr.toString()).apply();
    }

    private void addRecent(String name, String id) {
        recent.removeIf(p -> id.equals(p.get("id")));
        Map<String, String> entry = new LinkedHashMap<>();
        entry.put("name", name);
        entry.put("id", id);
        entry.put("modified", java.time.LocalDateTime.now().toString());
        recent.add(0, entry);
        if (recent.size() > 10) recent = recent.subList(0, 10);
        saveRecent();
        updateList();
    }
}