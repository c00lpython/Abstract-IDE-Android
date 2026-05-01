package com.xcore.abstractide.core.project;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.xcore.abstractide.core.model.ProjectModel;
import com.xcore.abstractide.core.serialization.ProjectSerializer;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;
import java.util.logging.Logger;

/**
 * Аналог: project_manager.py — AbstractProjectManager
 */
public class ProjectManager {

    private static final Logger LOGGER = Logger.getLogger(ProjectManager.class.getName());

    private ProjectModel currentProject;
    private final String projectsDir = "projects";
    private final String abstractDir = "results/projectarchs";
    private final ProjectSerializer serializer = new ProjectSerializer();
    private final Gson gson = new GsonBuilder().setPrettyPrinting().create();

    public ProjectManager() {
        new File(projectsDir).mkdirs();
        new File(abstractDir).mkdirs();
    }

    public ProjectModel newProject(String name) {
        currentProject = new ProjectModel(name);
        LOGGER.info("New project created: " + currentProject.getId());
        return currentProject;
    }

    public ProjectModel openProject(String projectId) {
        File abstractFile = new File(abstractDir, projectId + ".abstract");
        if (!abstractFile.exists()) {
            LOGGER.warning("Project file not found: " + abstractFile.getAbsolutePath());
            return null;
        }

        try {
            String json = new String(Files.readAllBytes(Paths.get(abstractFile.getAbsolutePath())),
                    StandardCharsets.UTF_8);
            ProjectModel loaded = serializer.deserializeProject(json);
            loaded.setId(projectId);
            currentProject = loaded;

            File jsonFile = new File(projectsDir, projectId + ".json");
            if (jsonFile.exists()) {
                String metaJson = new String(Files.readAllBytes(Paths.get(jsonFile.getAbsolutePath())),
                        StandardCharsets.UTF_8);
                ProjectModel meta = gson.fromJson(metaJson, ProjectModel.class);
                if (meta.getName() != null) {
                    currentProject.setName(meta.getName());
                }
            }

            LOGGER.info("Project opened: " + currentProject.getName());
            return currentProject;

        } catch (IOException e) {
            LOGGER.severe("Error opening project: " + e.getMessage());
            return null;
        }
    }

    public boolean saveProject() {
        return saveProject(currentProject);
    }

    public boolean saveProject(ProjectModel project) {
        if (project == null) project = currentProject;
        if (project == null) return false;

        try {
            String json = serializer.serializeProject(project);
            File abstractFile = new File(abstractDir, project.getId() + ".abstract");
            try (FileWriter writer = new FileWriter(abstractFile, StandardCharsets.UTF_8)) {
                writer.write(json);
            }

            File jsonFile = new File(projectsDir, project.getId() + ".json");
            String metaJson = gson.toJson(project);
            try (FileWriter writer = new FileWriter(jsonFile, StandardCharsets.UTF_8)) {
                writer.write(metaJson);
            }

            LOGGER.info("Project saved: " + project.getName());
            return true;

        } catch (IOException e) {
            LOGGER.severe("Error saving project: " + e.getMessage());
            return false;
        }
    }

    public List<Map<String, Object>> getAvailableProjects() {
        List<Map<String, Object>> projects = new ArrayList<>();
        File abstractPath = new File(abstractDir);

        if (abstractPath.exists() && abstractPath.isDirectory()) {
            File[] files = abstractPath.listFiles((dir, name) -> name.endsWith(".abstract"));
            if (files != null) {
                for (File file : files) {
                    String projectId = file.getName().replace(".abstract", "");
                    Map<String, Object> info = new LinkedHashMap<>();
                    info.put("id", projectId);
                    info.put("name", projectId.replace("project_", "Project "));
                    info.put("abstract_file", file.getAbsolutePath());

                    File jsonFile = new File(projectsDir, projectId + ".json");
                    if (jsonFile.exists()) {
                        try {
                            String json = new String(Files.readAllBytes(Paths.get(jsonFile.getAbsolutePath())),
                                    StandardCharsets.UTF_8);
                            ProjectModel meta = gson.fromJson(json, ProjectModel.class);
                            info.put("name", meta.getName());
                            info.put("blocks_count", meta.getBlockCount());
                        } catch (IOException ignored) {}
                    }

                    projects.add(info);
                }
            }
        }

        return projects;
    }

    public boolean deleteProject(String projectId) {
        File abstractFile = new File(abstractDir, projectId + ".abstract");
        File jsonFile = new File(projectsDir, projectId + ".json");

        if (abstractFile.exists()) abstractFile.delete();
        if (jsonFile.exists()) jsonFile.delete();

        if (currentProject != null && currentProject.getId().equals(projectId)) {
            currentProject = null;
        }

        LOGGER.info("Project deleted: " + projectId);
        return true;
    }

    public ProjectModel getCurrentProject() { return currentProject; }
    public void setCurrentProject(ProjectModel project) { this.currentProject = project; }
    public boolean hasOpenProject() { return currentProject != null; }
}