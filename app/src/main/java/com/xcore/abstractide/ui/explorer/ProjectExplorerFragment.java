package com.xcore.abstractide.ui.explorer;

import android.os.Bundle;
import android.view.*;
import android.widget.*;
import androidx.fragment.app.Fragment;

import com.xcore.abstractide.core.model.BlockModel;
import com.xcore.abstractide.core.model.ProjectModel;

import java.util.*;

/**
 * Аналог: explorer_widget.py — ExplorerWidget
 * Дерево блоков проекта
 */
public class ProjectExplorerFragment extends Fragment {

    private ExpandableListView expandableListView;
    private ProjectModel project;
    private ExplorerAdapter adapter;
    private List<ExplorerItem> rootItems = new ArrayList<>();

    private OnBlockActionListener listener;

    public interface OnBlockActionListener {
        void onBlockRename(int blockId, String newName);
        void onBlockDelete(int blockId);
        void onBlockSelect(int blockId);
    }

    public void setOnBlockActionListener(OnBlockActionListener listener) {
        this.listener = listener;
    }

    public void setProject(ProjectModel project) {
        this.project = project;
        if (adapter != null) {
            buildTree();
            adapter.notifyDataSetChanged();
            expandAll();
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        expandableListView = new ExpandableListView(getContext());
        expandableListView.setBackgroundColor(0xFF1e1e1e);
        expandableListView.setDividerHeight(0);
        expandableListView.setGroupIndicator(null);
        expandableListView.setPadding(8, 8, 8, 8);

        buildTree();
        adapter = new ExplorerAdapter();
        expandableListView.setAdapter(adapter);
        expandAll();

        // Клик по корневому блоку (группе)
        expandableListView.setOnGroupClickListener((parent, v, groupPos, id) -> {
            ExplorerItem item = rootItems.get(groupPos);
            if (listener != null && item.blockId > 0) {
                listener.onBlockSelect(item.blockId);
            }
            return false; // false = разрешить разворачивать/сворачивать группу
        });

        // Клик по дочернему блоку
        expandableListView.setOnChildClickListener((parent, v, groupPos, childPos, id) -> {
            ExplorerItem child = rootItems.get(groupPos).children.get(childPos);
            if (listener != null && child.blockId > 0) {
                listener.onBlockSelect(child.blockId);
            }
            return true;
        });

        // Долгое нажатие — контекстное меню
        expandableListView.setOnItemLongClickListener((parent, view, position, id) -> {
            int groupPos = ExpandableListView.getPackedPositionGroup(position);
            int childPos = ExpandableListView.getPackedPositionChild(position);

            if (childPos >= 0) {
                ExplorerItem item = rootItems.get(groupPos).children.get(childPos);
                showContextMenu(view, item);
            } else {
                ExplorerItem item = rootItems.get(groupPos);
                if (item.blockId > 0) showContextMenu(view, item);
            }
            return true;
        });

        return expandableListView;
    }

    private void buildTree() {
        rootItems.clear();
        if (project == null || project.getAllBlocks().isEmpty()) {
            ExplorerItem empty = new ExplorerItem("No blocks", 0, "📄");
            rootItems.add(empty);
            return;
        }

        // Группировка: корневые блоки (без parent_id)
        List<BlockModel> roots = new ArrayList<>();
        for (BlockModel block : project.getAllBlocks()) {
            if (block.getParentId() == null) {
                roots.add(block);
            }
        }

        // Сортировка по имени
        roots.sort(Comparator.comparing(b -> b.getName() != null ? b.getName() : ""));

        for (BlockModel root : roots) {
            ExplorerItem item = blockToItem(root);
            rootItems.add(item);
        }
    }

    private ExplorerItem blockToItem(BlockModel block) {
        String icon = getIcon(block);
        String name = block.getName() != null ? block.getName() : "Block " + block.getId();
        ExplorerItem item = new ExplorerItem(name, block.getId(), icon);

        // Добавить детей
        if (block.getChildrenIds() != null) {
            for (int childId : block.getChildrenIds()) {
                BlockModel child = project.getBlock(childId);
                if (child != null) {
                    item.children.add(blockToItem(child));
                }
            }
        }

        return item;
    }

    private String getIcon(BlockModel block) {
        if (block == null || block.getType() == null) return "📄";
        if (block.isReferenceBlock()) return "🔗";
        if (block.isCallBlock()) return "📞";

        String sub = block.getType().getSubclassName();
        if (sub == null) return "📄";
        String s = sub.toLowerCase();

        if (s.contains("function")) return "ƒ";
        if (s.contains("variable")) return "📊";
        if (s.contains("class")) return "📦";
        if (s.contains("if") || s.contains("else") || s.contains("switch")) return "🔀";
        if (s.contains("for") || s.contains("while") || s.contains("loop")) return "↻";
        if (s.contains("print")) return "🖨";
        if (s.contains("input")) return "⌨";
        if (s.contains("list")) return "📋";
        if (s.contains("dict")) return "📚";
        return "📄";
    }

    private void expandAll() {
        for (int i = 0; i < adapter.getGroupCount(); i++) {
            expandableListView.expandGroup(i);
        }
    }

    private void showContextMenu(View anchor, ExplorerItem item) {
        PopupMenu popup = new PopupMenu(getContext(), anchor);
        popup.getMenu().add("✏️ Rename");
        popup.getMenu().add("🗑️ Delete");

        popup.setOnMenuItemClickListener(menuItem -> {
            if (menuItem.getTitle().toString().contains("Rename")) {
                showRenameDialog(item);
            } else if (menuItem.getTitle().toString().contains("Delete")) {
                if (listener != null) listener.onBlockDelete(item.blockId);
            }
            return true;
        });

        popup.show();
    }

    private void showRenameDialog(ExplorerItem item) {
        android.app.AlertDialog.Builder builder = new android.app.AlertDialog.Builder(getContext());
        builder.setTitle("Rename Block");

        final EditText input = new EditText(getContext());
        input.setText(item.name);
        input.setTextColor(0xFFFFFFFF);
        input.setBackgroundColor(0xFF3a3a3a);
        builder.setView(input);

        builder.setPositiveButton("OK", (dialog, which) -> {
            String newName = input.getText().toString().trim();
            if (!newName.isEmpty() && listener != null) {
                listener.onBlockRename(item.blockId, newName);
            }
        });
        builder.setNegativeButton("Cancel", null);
        builder.show();
    }

    // ========== ADAPTER ==========

    private class ExplorerAdapter extends BaseExpandableListAdapter {

        @Override
        public int getGroupCount() {
            return rootItems.size();
        }

        @Override
        public int getChildrenCount(int groupPosition) {
            return rootItems.get(groupPosition).children.size();
        }

        @Override
        public Object getGroup(int groupPosition) {
            return rootItems.get(groupPosition);
        }

        @Override
        public Object getChild(int groupPosition, int childPosition) {
            return rootItems.get(groupPosition).children.get(childPosition);
        }

        @Override
        public long getGroupId(int groupPosition) { return groupPosition; }

        @Override
        public long getChildId(int groupPosition, int childPosition) { return childPosition; }

        @Override
        public boolean hasStableIds() { return false; }

        @Override
        public View getGroupView(int groupPosition, boolean isExpanded, View convertView, ViewGroup parent) {
            TextView tv = new TextView(getContext());
            tv.setPadding(24, 16, 16, 16);
            tv.setTextSize(13);
            tv.setTextColor(0xFFdddddd);
            tv.setBackgroundColor(0xFF2a2a2a);

            ExplorerItem item = rootItems.get(groupPosition);
            tv.setText(item.icon + " " + item.name);

            return tv;
        }

        @Override
        public View getChildView(int groupPosition, int childPosition, boolean isLastChild, View convertView, ViewGroup parent) {
            TextView tv = new TextView(getContext());
            tv.setPadding(48, 12, 12, 12);
            tv.setTextSize(12);
            tv.setTextColor(0xFFcccccc);
            tv.setBackgroundColor(0xFF252525);

            ExplorerItem item = rootItems.get(groupPosition).children.get(childPosition);
            tv.setText(item.icon + " " + item.name + " [" + item.blockId + "]");

            return tv;
        }

        @Override
        public boolean isChildSelectable(int groupPosition, int childPosition) {
            return true;
        }
    }

    // ========== DATA CLASS ==========

    private static class ExplorerItem {
        String name;
        int blockId;
        String icon;
        List<ExplorerItem> children = new ArrayList<>();

        ExplorerItem(String name, int blockId, String icon) {
            this.name = name;
            this.blockId = blockId;
            this.icon = icon;
        }
    }
}