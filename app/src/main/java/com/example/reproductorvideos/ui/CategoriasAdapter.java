package com.example.reproductorvideos.ui;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import java.util.List;

public class CategoriasAdapter
        extends RecyclerView.Adapter<CategoriasAdapter.VH> {

    public interface OnClick {
        void onClick(String categoria);
    }

    private final List<String> items;
    private final OnClick listener;

    public CategoriasAdapter(List<String> items, OnClick listener) {
        this.items = items;
        this.listener = listener;
    }

    @NonNull @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext())
                .inflate(android.R.layout.simple_list_item_1, parent, false);
        return new VH(v);
    }

    @Override
    public void onBindViewHolder(@NonNull VH holder, int pos) {
        String cat = items.get(pos);
        holder.text.setText(cat);
        holder.itemView.setOnClickListener(v -> listener.onClick(cat));
    }

    @Override public int getItemCount() { return items.size(); }

    static class VH extends RecyclerView.ViewHolder {
        TextView text;
        VH(@NonNull View itemView) {
            super(itemView);
            text = itemView.findViewById(android.R.id.text1);
        }
    }
}
