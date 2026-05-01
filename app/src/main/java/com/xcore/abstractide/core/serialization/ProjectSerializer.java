package com.xcore.abstractide.core.serialization;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.xcore.abstractide.core.model.ProjectModel;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.logging.Logger;

/**
 * Аналог: serialization.py — EnhancedProjectSerializer
 */
public class ProjectSerializer {

    private static final Logger LOGGER = Logger.getLogger(ProjectSerializer.class.getName());
    private final Gson gson = new GsonBuilder().setPrettyPrinting().serializeNulls().create();

    public String serializeProject(ProjectModel project) {
        project.prepareForSerialization();
        return gson.toJson(project);
    }

    public ProjectModel deserializeProject(String json) {
        ProjectModel project = gson.fromJson(json, ProjectModel.class);
        project.initTransients();
        return project;
    }

    public boolean saveProject(ProjectModel project, String filepath) {
        try {
            File file = new File(filepath);
            File parent = file.getParentFile();
            if (parent != null && !parent.exists()) parent.mkdirs();
            String json = serializeProject(project);
            try (FileWriter writer = new FileWriter(file, StandardCharsets.UTF_8)) {
                writer.write(json);
            }
            LOGGER.info("Project saved: " + filepath);
            return true;
        } catch (IOException e) {
            LOGGER.severe("Error saving: " + e.getMessage());
            return false;
        }
    }

    public ProjectModel loadProject(String filepath) {
        try {
            String json = new String(java.nio.file.Files.readAllBytes(
                    java.nio.file.Paths.get(filepath)), StandardCharsets.UTF_8);
            return deserializeProject(json);
        } catch (IOException e) {
            LOGGER.severe("Error loading: " + e.getMessage());
            return null;
        }
    }
}