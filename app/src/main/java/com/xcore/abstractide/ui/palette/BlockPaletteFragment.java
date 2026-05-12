package com.xcore.abstractide.ui.palette;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseExpandableListAdapter;
import android.widget.ExpandableListView;
import android.widget.TextView;
import android.widget.Toast;
import androidx.fragment.app.Fragment;

import com.xcore.abstractide.core.model.BlockModel;
import com.xcore.abstractide.core.parser.BlockDefinitionParser;
import com.xcore.abstractide.core.parser.BlockDefinitionParser.BlockDefinition;

import java.io.InputStream;
import java.util.*;

public class BlockPaletteFragment extends Fragment {

    private static final String TAG = "Palette";

    private ExpandableListView expandableListView;
    private BlockDefinitionParser parser;
    private final List<CategoryGroup> categories = new ArrayList<>();
    private final List<CategoryGroup> allCategories = new ArrayList<>();
    private final List<BlockDefinition> createdBlocks = new ArrayList<>();
    private PaletteAdapter paletteAdapter;
    private String currentFilter = "";
    private String currentTab = "new";

    private static final Set<String> CALLABLE_TYPES = new HashSet<>(Arrays.asList(
            "Definitions.Function", "Definitions.Class", "Definitions.Variable",
            "DataTypes.Variable", "DataTypes.String", "DataTypes.Integer",
            "DataTypes.Float", "DataTypes.Boolean", "DataTypes.List",
            "DataTypes.Tuple", "DataTypes.Dict", "DataTypes.Set",
            "DataTypes.Bytes", "DataTypes.None",
            "Definitions.List", "Definitions.Dict", "Definitions.Tuple"
    ));

    public interface OnBlockSelectedListener {
        void onBlockSelected(BlockDefinition definition);
    }

    private OnBlockSelectedListener listener;

    public void setOnBlockSelectedListener(OnBlockSelectedListener listener) {
        this.listener = listener;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        expandableListView = new ExpandableListView(getContext());
        expandableListView.setBackgroundColor(0xFF252525);
        expandableListView.setDividerHeight(0);
        expandableListView.setGroupIndicator(null);

        parser = new BlockDefinitionParser();
        loadBlocks();
        paletteAdapter = new PaletteAdapter();
        expandableListView.setAdapter(paletteAdapter);

        expandableListView.setOnChildClickListener((parent, v, groupPos, childPos, id) -> {
            if (groupPos < categories.size()) {
                CategoryGroup group = categories.get(groupPos);
                if (childPos < group.blocks.size()) {
                    BlockDefinition def = group.blocks.get(childPos);
                    if (listener != null) listener.onBlockSelected(def);
                    Toast.makeText(getContext(), "Selected: " + def.subclassName, Toast.LENGTH_SHORT).show();
                }
            }
            return true;
        });

        for (int i = 0; i < paletteAdapter.getGroupCount(); i++) {
            expandableListView.expandGroup(i);
        }

        return expandableListView;
    }

    private void loadBlocks() {
        categories.clear();
        allCategories.clear();

        try {
            InputStream is = getContext().getAssets().open("blocks/CodeBlocks.json");
            byte[] buffer = new byte[is.available()];
            is.read(buffer);
            String jsonContent = new String(buffer);
            is.close();

            parser.parseJson(jsonContent, "code");

        } catch (Exception e) {
            Toast.makeText(getContext(), "Error: " + e.getMessage(), Toast.LENGTH_LONG).show();
            e.printStackTrace();
            return;
        }

        List<BlockDefinition> defs = parser.getAllDefinitions();
        Toast.makeText(getContext(), "Loaded: " + defs.size() + " blocks", Toast.LENGTH_LONG).show();

        Map<String, List<BlockDefinition>> grouped = new LinkedHashMap<>();
        for (BlockDefinition def : defs) {
            grouped.computeIfAbsent(def.className, k -> new ArrayList<>()).add(def);
        }

        for (Map.Entry<String, List<BlockDefinition>> e : grouped.entrySet()) {
            CategoryGroup cg = new CategoryGroup(e.getKey(), e.getValue());
            categories.add(cg);
            allCategories.add(cg);
        }
    }

    public void addCreatedBlock(BlockDefinition def) {
        if (!createdBlocks.contains(def)) {
            createdBlocks.add(def);
            if ("created".equals(currentTab)) {
                showCategory("created");
            }
        }
    }

    public void autoAddToCreated(BlockModel block) {
        if (block.getType() == null) return;
        String fullType = block.getType().getFullName();
        if (CALLABLE_TYPES.contains(fullType)) {
            BlockDefinition def = new BlockDefinition();
            def.fullName = fullType;
            def.className = block.getType().getClassName();
            def.subclassName = block.getType().getSubclassName();
            def.color = block.getColor();
            addCreatedBlock(def);
        }
    }

    public void filter(String text) {
        currentFilter = text != null ? text.toLowerCase().trim() : "";
        categories.clear();
        if (currentFilter.isEmpty()) {
            if ("created".equals(currentTab)) {
                if (createdBlocks.isEmpty()) {
                    categories.add(new CategoryGroup("Created Blocks", new ArrayList<>()));
                } else {
                    categories.add(new CategoryGroup("Created", new ArrayList<>(createdBlocks)));
                }
            } else {
                categories.addAll(allCategories);
            }
        } else {
            List<BlockDefinition> source = "created".equals(currentTab) ? createdBlocks : getAllDefinitions();
            List<BlockDefinition> filtered = new ArrayList<>();
            for (BlockDefinition d : source) {
                if (d.subclassName.toLowerCase().contains(currentFilter) ||
                        d.className.toLowerCase().contains(currentFilter)) {
                    filtered.add(d);
                }
            }
            if (!filtered.isEmpty()) {
                categories.add(new CategoryGroup("Results", filtered));
            }
        }
        paletteAdapter.notifyDataSetChanged();
        for (int i = 0; i < paletteAdapter.getGroupCount(); i++)
            expandableListView.expandGroup(i);
    }

    private List<BlockDefinition> getAllDefinitions() {
        List<BlockDefinition> all = new ArrayList<>();
        for (CategoryGroup g : allCategories) all.addAll(g.blocks);
        return all;
    }

    public void showCategory(String category) {
        currentTab = category;
        if ("created".equals(category)) {
            categories.clear();
            if (createdBlocks.isEmpty()) {
                categories.add(new CategoryGroup("Created Blocks", new ArrayList<>()));
                Toast.makeText(getContext(), "No created blocks yet", Toast.LENGTH_SHORT).show();
            } else {
                categories.add(new CategoryGroup("Created", new ArrayList<>(createdBlocks)));
            }
        } else {
            currentTab = "new";
            categories.clear();
            categories.addAll(allCategories);
        }
        paletteAdapter.notifyDataSetChanged();
        for (int i = 0; i < paletteAdapter.getGroupCount(); i++)
            expandableListView.expandGroup(i);
    }

    public BlockModel createBlockFromDefinition(BlockDefinition def) {
        return parser.createBlock(def.fullName, "code");
    }

    private class PaletteAdapter extends BaseExpandableListAdapter {
        @Override public int getGroupCount() { return categories.size(); }
        @Override public int getChildrenCount(int gp) { return categories.get(gp).blocks.size(); }
        @Override public Object getGroup(int gp) { return categories.get(gp); }
        @Override public Object getChild(int gp, int cp) { return categories.get(gp).blocks.get(cp); }
        @Override public long getGroupId(int gp) { return gp; }
        @Override public long getChildId(int gp, int cp) { return cp; }
        @Override public boolean hasStableIds() { return false; }
        @Override public View getGroupView(int gp, boolean exp, View cv, ViewGroup p) {
            TextView tv;
            if (cv == null) {
                tv = new TextView(getContext());
                tv.setPadding(30, 14, 14, 14);
                tv.setTextSize(12);
                tv.setTextColor(0xFFFFFFFF);
            } else tv = (TextView) cv;
            CategoryGroup g = categories.get(gp);
            tv.setText((exp ? "▼ " : "▶ ") + g.className + " (" + g.blocks.size() + ")");
            tv.setBackgroundColor(exp ? 0xFF3a3a3a : 0xFF2d2d2d);
            return tv;
        }
        @Override public View getChildView(int gp, int cp, boolean last, View cv, ViewGroup p) {
            TextView tv;
            if (cv == null) {
                tv = new TextView(getContext());
                tv.setPadding(55, 12, 12, 12);
                tv.setTextSize(11);
                tv.setTextColor(0xFFcccccc);
            } else tv = (TextView) cv;
            BlockDefinition def = categories.get(gp).blocks.get(cp);
            String suffix = def.hasDroplist ? " ▼" : "";
            tv.setText("⬛ " + def.subclassName + suffix);
            tv.setBackgroundColor(0xFF252525);
            return tv;
        }
        @Override public boolean isChildSelectable(int gp, int cp) { return true; }
    }

    private static class CategoryGroup {
        String className;
        List<BlockDefinition> blocks;
        CategoryGroup(String c, List<BlockDefinition> b) { className = c; blocks = b; }
    }
}