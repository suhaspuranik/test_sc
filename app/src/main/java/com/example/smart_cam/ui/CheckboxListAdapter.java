package com.example.smart_cam.ui;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.smart_cam.R;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

public class CheckboxListAdapter extends RecyclerView.Adapter<CheckboxListAdapter.ViewHolder> {
    public static class Item {
        public final int id;
        public final String label;

        public Item(int id, String label) {
            this.id = id;
            this.label = label;
        }
    }

    public interface OnSelectionChangedListener {
        void onSelectionChanged(Set<Integer> selectedIds);
    }

    private final LayoutInflater inflater;
    private final ArrayList<Item> items;
    private final LinkedHashSet<Integer> selectedIds;
    private OnSelectionChangedListener selectionChangedListener;

    public CheckboxListAdapter(Context context, List<Item> initialItems) {
        this.inflater = LayoutInflater.from(context);
        this.items = new ArrayList<>(initialItems != null ? initialItems : new ArrayList<>());
        this.selectedIds = new LinkedHashSet<>();
        setHasStableIds(true);
    }

    public void setOnSelectionChangedListener(OnSelectionChangedListener listener) {
        this.selectionChangedListener = listener;
    }

    public void setItems(List<Item> newItems) {
        this.items.clear();
        if (newItems != null) {
            this.items.addAll(newItems);
        }
        notifyDataSetChanged();
    }

    public void setSelectedIds(Collection<Integer> ids) {
        this.selectedIds.clear();
        if (ids != null) {
            this.selectedIds.addAll(ids);
        }
        notifyDataSetChanged();
        if (selectionChangedListener != null) {
            selectionChangedListener.onSelectionChanged(new LinkedHashSet<>(selectedIds));
        }
    }

    public ArrayList<Integer> getSelectedIds() {
        return new ArrayList<>(selectedIds);
    }

    public ArrayList<String> getSelectedLabels() {
        ArrayList<String> labels = new ArrayList<>();
        for (Item item : items) {
            if (selectedIds.contains(item.id)) {
                labels.add(item.label);
            }
        }
        return labels;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = inflater.inflate(R.layout.list_item_village, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Item item = items.get(position);
        // Detach listener before state change to avoid triggering on recycle
        holder.checkBox.setOnCheckedChangeListener(null);
        holder.checkBox.setText(item.label);
        holder.checkBox.setChecked(selectedIds.contains(item.id));
        holder.checkBox.setTag(item.id);

        holder.checkBox.setOnCheckedChangeListener((buttonView, isChecked) -> {
            Integer id = (Integer) buttonView.getTag();
            if (id != null) {
                if (isChecked) {
                    selectedIds.add(id);
                } else {
                    selectedIds.remove(id);
                }
                if (selectionChangedListener != null) {
                    selectionChangedListener.onSelectionChanged(new LinkedHashSet<>(selectedIds));
                }
            }
        });
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    @Override
    public long getItemId(int position) {
        return items.get(position).id;
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        final CheckBox checkBox;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            this.checkBox = itemView.findViewById(R.id.cb_village);
        }
    }
}

