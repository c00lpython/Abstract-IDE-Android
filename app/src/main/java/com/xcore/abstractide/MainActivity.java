package com.xcore.abstractide;

import android.os.Bundle;
import android.widget.*;
import androidx.appcompat.app.AppCompatActivity;

import com.xcore.abstractide.core.builder.CodeBuilder;
import com.xcore.abstractide.core.model.BlockModel;
import com.xcore.abstractide.core.model.Connection;
import com.xcore.abstractide.core.model.ProjectModel;
import com.xcore.abstractide.core.project.ProjectManager;
import com.xcore.abstractide.ui.canvas.BlockCanvasView;
import com.xcore.abstractide.ui.explorer.ProjectExplorerFragment;
import com.xcore.abstractide.ui.palette.BlockPaletteFragment;
import com.xcore.abstractide.ui.property.PropertyEditorDialog;

import java.util.UUID;

public class MainActivity extends AppCompatActivity {

    private ProjectManager projectManager;
    private ProjectModel currentProject;
    private BlockCanvasView canvasView;
    private TextView tvTerminal;
    private TextView tvProperties;
    private ProjectExplorerFragment explorerFragment;
    private BlockPaletteFragment paletteFragment;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        projectManager = new ProjectManager();
        String projectId = getIntent().getStringExtra("project_id");
        currentProject = (projectId != null) ? projectManager.openProject(projectId) : projectManager.newProject("Untitled");
        if (currentProject == null) currentProject = projectManager.newProject("Untitled");

        tvTerminal = findViewById(R.id.tvTerminal);
        tvProperties = findViewById(R.id.tvProperties);
        canvasView = findViewById(R.id.canvasView);

        findViewById(R.id.btnBuild).setOnClickListener(v -> { CodeBuilder b = new CodeBuilder(); tvTerminal.setText("=== Build ===\n" + b.build(currentProject)); });
        findViewById(R.id.btnRun).setOnClickListener(v -> { CodeBuilder b = new CodeBuilder(); tvTerminal.setText("=== Run ===\n" + b.build(currentProject)); });
        findViewById(R.id.btnFullscreen).setOnClickListener(v -> tvTerminal.setTextSize(16));
        findViewById(R.id.btnEditProperties).setOnClickListener(v -> {
            for (BlockModel blk : currentProject.getAllBlocks()) {
                BlockCanvasView.DrawableBlock db = canvasView.getDrawableBlock(blk.getId());
                if (db != null && db.selected) { PropertyEditorDialog.show(MainActivity.this, blk, currentProject, ub -> { showProperties(ub); canvasView.invalidate(); explorerFragment.setProject(currentProject); }); break; }
            }
        });
        findViewById(R.id.menuFile).setOnClickListener(v -> showFileMenu());
        findViewById(R.id.menuEdit).setOnClickListener(v -> tvTerminal.append("\nEdit"));
        findViewById(R.id.menuView).setOnClickListener(v -> tvTerminal.append("\nView"));

        explorerFragment = (ProjectExplorerFragment) getSupportFragmentManager().findFragmentById(R.id.fragmentExplorer);
        if (explorerFragment != null) {
            explorerFragment.setProject(currentProject);
            explorerFragment.setOnBlockActionListener(new ProjectExplorerFragment.OnBlockActionListener() {
                @Override public void onBlockSelect(int blockId) {
                    canvasView.selectBlock(blockId);
                    canvasView.centerOnBlock(blockId);
                    BlockModel blk = currentProject.getBlock(blockId);
                    if (blk != null) showProperties(blk);
                }
                @Override public void onBlockRename(int blockId, String nn) { BlockModel blk = currentProject.getBlock(blockId); if (blk != null) { blk.setName(nn); blk.touch(); } canvasView.invalidate(); explorerFragment.setProject(currentProject); }
                @Override public void onBlockDelete(int blockId) { canvasView.removeBlock(blockId); explorerFragment.setProject(currentProject); }
            });
        }

        paletteFragment = (BlockPaletteFragment) getSupportFragmentManager().findFragmentById(R.id.fragmentPalette);
        if (paletteFragment != null) {
            paletteFragment.setOnBlockSelectedListener(def -> {
                BlockModel blk = new BlockModel();
                blk.setId(currentProject.getIdManager().getNextId());
                blk.setType(new BlockModel.BlockType("code", def.className, def.subclassName));
                blk.setName(def.subclassName);
                blk.setColor(def.color != null ? def.color : "#3498db");
                float[] c = canvasView.getViewCenter();
                blk.getPosition().put("x", (double)c[0]); blk.getPosition().put("y", (double)c[1]);

                // Установить контейнер если тип в списке
                if (BlockCanvasView.isContainerType(blk.getType())) {
                    blk.getProperties().put("_is_container", true);
                    blk.getProperties().put("_container_config", def.containerConfig != null ? def.containerConfig : new java.util.HashMap<>());
                    blk.getProperties().put("_container_items", new java.util.ArrayList<>());
                    blk.initTransients();
                }

                currentProject.addBlock(blk); canvasView.addBlock(blk);
                paletteFragment.autoAddToCreated(blk);
                tvTerminal.append("\n+ " + def.subclassName + " container=" + blk.isContainerBlock());
                explorerFragment.setProject(currentProject);
            });
        }

        SearchView sp = findViewById(R.id.searchPalette);
        if (sp != null && paletteFragment != null) sp.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override public boolean onQueryTextSubmit(String q) { return false; }
            @Override public boolean onQueryTextChange(String t) { paletteFragment.filter(t); return true; }
        });

        findViewById(R.id.tabNew).setOnClickListener(v -> paletteFragment.showCategory("code"));
        findViewById(R.id.tabCreated).setOnClickListener(v -> paletteFragment.showCategory("created"));

        canvasView.setOnBlockClickListener(blk -> { tvTerminal.append("\nSelected: " + blk.getName()); showProperties(blk); });
        canvasView.setOnCanvasChangeListener(new BlockCanvasView.OnCanvasChangeListener() {
            @Override public void onBlockMoved(int id, float x, float y) { BlockModel blk = currentProject.getBlock(id); if (blk != null) { blk.getPosition().put("x", (double)x); blk.getPosition().put("y", (double)y); } }
            @Override public void onConnectionCreated(int fromId, String fp, int toId, String tp) {
                Connection conn = new Connection(fromId, fp, toId, tp, UUID.randomUUID().toString(), "data", null, null);
                currentProject.addConnection(conn); canvasView.addConnection(conn);
                tvTerminal.append("\nConnected: " + fromId + " -> " + toId);
                canvasView.onConnectionToContainer(fromId, toId);
            }
            @Override public void onBlockNested(int cid, int pid) { tvTerminal.append("\nNested: " + cid + " in " + pid); explorerFragment.setProject(currentProject); }
        });

        addDemoBlock();
    }

    private void showProperties(BlockModel blk) {
        StringBuilder sb = new StringBuilder();
        sb.append("ID: ").append(blk.getId()).append("\nName: ").append(blk.getName()).append("\n");
        if (blk.getType() != null) sb.append("Type: ").append(blk.getType().getFullName()).append("\n");
        sb.append("Color: ").append(blk.getColor()).append("\nPos: (").append(blk.getPosition().get("x")).append(", ").append(blk.getPosition().get("y")).append(")\n");
        sb.append("Size: ").append(blk.getSize().get("width")).append("x").append(blk.getSize().get("height")).append("\n");
        if (blk.isContainerBlock()) sb.append("Container: ").append(blk.getContainerType()).append("\nItems: ").append(blk.getContainerItems().size()).append("\n");
        sb.append("Children: ").append(blk.getChildrenIds().size()).append("\n");
        tvProperties.setText(sb.toString());
    }

    private void addDemoBlock() {
        BlockModel container = new BlockModel();
        container.setId(currentProject.getIdManager().getNextId());
        container.setType(new BlockModel.BlockType("code", "ControlFlow", "If"));
        container.setName("If Block"); container.setColor("#8e44ad");
        container.getPosition().put("x", 100.0); container.getPosition().put("y", 100.0);
        container.getProperties().put("_is_container", true);
        container.getProperties().put("_container_config", new java.util.HashMap<>());
        container.getProperties().put("_container_items", new java.util.ArrayList<>());
        container.initTransients();
        currentProject.addBlock(container); canvasView.addBlock(container);

        BlockModel blk = new BlockModel();
        blk.setId(currentProject.getIdManager().getNextId());
        blk.setType(new BlockModel.BlockType("code", "Builtins", "Print"));
        blk.setName("Drag Me"); blk.setColor("#e74c3c");
        blk.getPosition().put("x", 300.0); blk.getPosition().put("y", 300.0);
        currentProject.addBlock(blk); canvasView.addBlock(blk);
        tvTerminal.setText("Ready.\nContainer: If Block\nDraggable: Drag Me");
    }

    private void showFileMenu() {
        PopupMenu popup = new PopupMenu(this, findViewById(R.id.menuFile));
        popup.getMenu().add("New Project"); popup.getMenu().add("Save"); popup.getMenu().add("Open...");
        popup.setOnMenuItemClickListener(item -> {
            if (item.getTitle().toString().contains("Save")) { projectManager.saveProject(currentProject); tvTerminal.append("\nSaved!"); }
            else if (item.getTitle().toString().contains("New")) { currentProject = projectManager.newProject("New Project"); canvasView.clearBlocks(); tvTerminal.setText("New project created"); }
            return true;
        });
        popup.show();
    }
}