package com.xcore.abstractide.core.autosave;

import com.xcore.abstractide.core.model.ProjectModel;
import com.xcore.abstractide.core.serialization.ProjectSerializer;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;
import java.util.stream.Collectors;

/**
 * Аналог: auto_save.py — AutoSaveManager
 * Периодическое автосохранение проектов
 */
public class AutoSaveManager {

    private static final Logger LOGGER = Logger.getLogger(AutoSaveManager.class.getName());

    private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();
    private ScheduledFuture<?> autoSaveTask;

    private final ProjectSerializer serializer = new ProjectSerializer();
    private ProjectModel project;

    private int intervalSeconds = 60;
    private boolean enabled = true;
    private boolean isAutoSaving = false;
    private String lastSavePath;

    private final File autoSaveDir;

    // Callback-интерфейсы (аналог pyqtSignal)
    public interface AutoSaveListener {
        void onAutoSaved(String path);
        void onAutoSaveError(String error);
    }

    private AutoSaveListener listener;

    public AutoSaveManager() {
        String tempDir = System.getProperty("java.io.tmpdir");
        autoSaveDir = new File(tempDir, "abstract_ide_autosave");
        if (!autoSaveDir.exists()) {
            autoSaveDir.mkdirs();
        }
    }

    // ========== УПРАВЛЕНИЕ ==========

    public void setProject(ProjectModel project) {
        this.project = project;
    }

    public void setListener(AutoSaveListener listener) {
        this.listener = listener;
    }

    public void start() {
        if (enabled && autoSaveTask == null) {
            autoSaveTask = scheduler.scheduleAtFixedRate(
                    this::autoSave,
                    intervalSeconds,
                    intervalSeconds,
                    TimeUnit.SECONDS
            );
            LOGGER.info("Auto-save started (interval: " + intervalSeconds + "s)");
        }
    }

    public void stop() {
        if (autoSaveTask != null) {
            autoSaveTask.cancel(false);
            autoSaveTask = null;
        }
        LOGGER.info("Auto-save stopped");
    }

    public void setInterval(int seconds) {
        this.intervalSeconds = seconds;
        if (autoSaveTask != null) {
            // Перезапуск с новым интервалом
            stop();
            start();
        }
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
        if (enabled) {
            start();
        } else {
            stop();
        }
    }

    public void shutdown() {
        stop();
        scheduler.shutdown();
    }

    // ========== АВТОСОХРАНЕНИЕ ==========

    private synchronized void autoSave() {
        if (isAutoSaving || !enabled || project == null) return;
        isAutoSaving = true;

        try {
            String projectName = project.getName() != null ? project.getName() : "untitled";
            String safeName = projectName.replaceAll("[^a-zA-Z0-9 _-]", "").trim();
            if (safeName.isEmpty()) safeName = "untitled";

            String timestamp = LocalDateTime.now()
                    .format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
            String filename = safeName + "_autosave_" + timestamp + ".abstract";
            File file = new File(autoSaveDir, filename);

            boolean saved = serializer.saveProject(project, file.getAbsolutePath());
            if (saved) {
                lastSavePath = file.getAbsolutePath();
                if (listener != null) {
                    listener.onAutoSaved(file.getAbsolutePath());
                }
                LOGGER.info("Auto-save completed: " + file.getAbsolutePath());
                cleanupOldAutosaves(20);
            } else {
                if (listener != null) {
                    listener.onAutoSaveError("Failed to auto-save");
                }
            }

        } catch (Exception e) {
            LOGGER.severe("Auto-save error: " + e.getMessage());
            if (listener != null) {
                listener.onAutoSaveError("Auto-save error: " + e.getMessage());
            }
        } finally {
            isAutoSaving = false;
        }
    }

    // ========== ОЧИСТКА СТАРЫХ АВТОСОХРАНЕНИЙ ==========

    private void cleanupOldAutosaves(int keepLast) {
        try {
            if (project == null) return;

            String projectName = project.getName() != null ? project.getName() : "untitled";
            String safeName = projectName.replaceAll("[^a-zA-Z0-9 _-]", "").trim();

            File[] files = autoSaveDir.listFiles((dir, name) ->
                    name.startsWith(safeName) && name.endsWith(".abstract"));

            if (files == null || files.length <= keepLast) return;

            // Сортировать по времени изменения (новые сверху)
            Arrays.sort(files, (a, b) -> Long.compare(b.lastModified(), a.lastModified()));

            // Удалить старые
            for (int i = keepLast; i < files.length; i++) {
                try {
                    files[i].delete();
                } catch (Exception e) {
                    LOGGER.warning("Failed to delete old autosave: " + files[i].getName());
                }
            }
        } catch (Exception e) {
            LOGGER.warning("Cleanup error: " + e.getMessage());
        }
    }

    // ========== ВОССТАНОВЛЕНИЕ ==========

    public File restoreLastAutosave() {
        if (project == null) return null;

        String projectName = project.getName() != null ? project.getName() : "untitled";
        String safeName = projectName.replaceAll("[^a-zA-Z0-9 _-]", "").trim();

        File[] files = autoSaveDir.listFiles((dir, name) ->
                name.startsWith(safeName) && name.endsWith(".abstract"));

        if (files == null || files.length == 0) return null;

        // Найти самый новый
        File latest = files[0];
        for (File f : files) {
            if (f.lastModified() > latest.lastModified()) {
                latest = f;
            }
        }

        LOGGER.info("Found auto-save: " + latest.getAbsolutePath());
        return latest;
    }

    public List<File> getAutosaveFiles() {
        if (project == null) return new ArrayList<>();

        String projectName = project.getName() != null ? project.getName() : "untitled";
        String safeName = projectName.replaceAll("[^a-zA-Z0-9 _-]", "").trim();

        File[] files = autoSaveDir.listFiles((dir, name) ->
                name.startsWith(safeName) && name.endsWith(".abstract"));

        if (files == null) return new ArrayList<>();

        List<File> sortedFiles = Arrays.asList(files);
        sortedFiles.sort((a, b) -> Long.compare(b.lastModified(), a.lastModified()));
        return sortedFiles;
    }

    public void clearAutosaves() {
        List<File> files = getAutosaveFiles();
        for (File file : files) {
            file.delete();
        }
        LOGGER.info("Cleared " + files.size() + " auto-save files");
    }

    // ========== ГЕТТЕРЫ ==========

    public String getLastSavePath() { return lastSavePath; }
    public boolean isAutoSaving() { return isAutoSaving; }
    public boolean isEnabled() { return enabled; }
    public File getAutoSaveDir() { return autoSaveDir; }
}