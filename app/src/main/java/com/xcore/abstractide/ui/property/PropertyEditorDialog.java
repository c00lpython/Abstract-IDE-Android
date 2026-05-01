package com.xcore.abstractide.ui.property;

import android.app.AlertDialog;
import android.content.Context;
import android.graphics.Color;
import android.text.InputType;
import android.view.View;
import android.widget.*;

import com.xcore.abstractide.core.model.BlockModel;
import com.xcore.abstractide.core.model.ProjectModel;

/**
 * Аналог: property_editor.py (упрощённая версия)
 * Диалог редактирования свойств блока
 */
public class PropertyEditorDialog {

    public interface OnPropertyChangedListener {
        void onChanged(BlockModel block);
    }

    /**
     * Показать диалог свойств блока
     */
    public static void show(Context context, BlockModel block, ProjectModel project,
                            OnPropertyChangedListener listener) {
        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder.setTitle("Properties: " + (block.getName() != null ? block.getName() : "Block"));

        // Создаём layout
        LinearLayout layout = new LinearLayout(context);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(40, 20, 40, 20);

        // ID (readonly)
        layout.addView(createRow(context, "ID", String.valueOf(block.getId()), true));
        layout.addView(createDivider(context));

        // Name
        EditText etName = new EditText(context);
        etName.setText(block.getName() != null ? block.getName() : "");
        etName.setTextColor(Color.WHITE);
        etName.setBackgroundColor(Color.parseColor("#2d2d2d"));
        layout.addView(createLabel(context, "Name"));
        layout.addView(etName);
        layout.addView(createDivider(context));

        // Type (readonly)
        String typeStr = block.getType() != null ?
                block.getType().getClassName() + "." + block.getType().getSubclassName() : "Unknown";
        layout.addView(createRow(context, "Type", typeStr, true));
        layout.addView(createDivider(context));

        // Position
        LinearLayout posLayout = new LinearLayout(context);
        posLayout.setOrientation(LinearLayout.HORIZONTAL);

        EditText etX = new EditText(context);
        etX.setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL);
        etX.setText(String.valueOf(block.getPosition().getOrDefault("x", 0.0)));
        etX.setTextColor(Color.WHITE);
        etX.setBackgroundColor(Color.parseColor("#2d2d2d"));
        etX.setWidth(120);

        EditText etY = new EditText(context);
        etY.setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL);
        etY.setText(String.valueOf(block.getPosition().getOrDefault("y", 0.0)));
        etY.setTextColor(Color.WHITE);
        etY.setBackgroundColor(Color.parseColor("#2d2d2d"));
        etY.setWidth(120);

        posLayout.addView(createLabel(context, "X:"));
        posLayout.addView(etX);
        posLayout.addView(createLabel(context, " Y:"));
        posLayout.addView(etY);

        layout.addView(createLabel(context, "Position"));
        layout.addView(posLayout);
        layout.addView(createDivider(context));

        // Size
        LinearLayout sizeLayout = new LinearLayout(context);
        sizeLayout.setOrientation(LinearLayout.HORIZONTAL);

        EditText etW = new EditText(context);
        etW.setInputType(InputType.TYPE_CLASS_NUMBER);
        etW.setText(String.valueOf(block.getSize().getOrDefault("width", 150.0).intValue()));
        etW.setTextColor(Color.WHITE);
        etW.setBackgroundColor(Color.parseColor("#2d2d2d"));
        etW.setWidth(100);

        EditText etH = new EditText(context);
        etH.setInputType(InputType.TYPE_CLASS_NUMBER);
        etH.setText(String.valueOf(block.getSize().getOrDefault("height", 80.0).intValue()));
        etH.setTextColor(Color.WHITE);
        etH.setBackgroundColor(Color.parseColor("#2d2d2d"));
        etH.setWidth(100);

        sizeLayout.addView(createLabel(context, "W:"));
        sizeLayout.addView(etW);
        sizeLayout.addView(createLabel(context, " H:"));
        sizeLayout.addView(etH);

        layout.addView(createLabel(context, "Size"));
        layout.addView(sizeLayout);
        layout.addView(createDivider(context));

        // Color
        EditText etColor = new EditText(context);
        etColor.setText(block.getColor() != null ? block.getColor() : "#3498db");
        etColor.setTextColor(Color.WHITE);
        etColor.setBackgroundColor(Color.parseColor("#2d2d2d"));
        layout.addView(createLabel(context, "Color"));
        layout.addView(etColor);
        layout.addView(createDivider(context));

        // Container info
        if (block.isContainerBlock()) {
            layout.addView(createRow(context, "Container Type",
                    block.getContainerType(), true));
            layout.addView(createRow(context, "Items",
                    String.valueOf(block.getContainerItems().size()), true));
            layout.addView(createDivider(context));
        }

        // Children count
        layout.addView(createRow(context, "Children",
                String.valueOf(block.getChildrenIds().size()), true));

        ScrollView scrollView = new ScrollView(context);
        scrollView.addView(layout);
        builder.setView(scrollView);

        // Кнопки
        builder.setPositiveButton("Save", (dialog, which) -> {
            try {
                block.setName(etName.getText().toString().trim());
                block.getPosition().put("x", Double.parseDouble(etX.getText().toString()));
                block.getPosition().put("y", Double.parseDouble(etY.getText().toString()));
                block.getSize().put("width", Double.parseDouble(etW.getText().toString()));
                block.getSize().put("height", Double.parseDouble(etH.getText().toString()));
                block.setColor(etColor.getText().toString().trim());
                block.touch();

                if (listener != null) listener.onChanged(block);

                Toast.makeText(context, "Saved!", Toast.LENGTH_SHORT).show();
            } catch (NumberFormatException e) {
                Toast.makeText(context, "Invalid number format", Toast.LENGTH_SHORT).show();
            }
        });
        builder.setNegativeButton("Cancel", null);
        builder.show();
    }

    private static TextView createLabel(Context context, String text) {
        TextView tv = new TextView(context);
        tv.setText(text);
        tv.setTextColor(Color.parseColor("#888888"));
        tv.setTextSize(11);
        tv.setPadding(0, 10, 0, 4);
        return tv;
    }

    private static LinearLayout createRow(Context context, String label, String value, boolean readOnly) {
        LinearLayout row = new LinearLayout(context);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setPadding(0, 8, 0, 8);

        TextView tvLabel = new TextView(context);
        tvLabel.setText(label);
        tvLabel.setTextColor(Color.parseColor("#cccccc"));
        tvLabel.setTextSize(12);
        tvLabel.setWidth(120);
        row.addView(tvLabel);

        TextView tvValue = new TextView(context);
        tvValue.setText(value);
        tvValue.setTextColor(readOnly ? Color.parseColor("#888888") : Color.WHITE);
        tvValue.setTextSize(12);
        row.addView(tvValue);

        return row;
    }

    private static View createDivider(Context context) {
        View divider = new View(context);
        divider.setLayoutParams(new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, 1));
        divider.setBackgroundColor(Color.parseColor("#333333"));
        return divider;
    }
}