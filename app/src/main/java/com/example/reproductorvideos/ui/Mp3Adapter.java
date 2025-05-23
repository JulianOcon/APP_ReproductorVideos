package com.example.reproductorvideos.ui;

import android.content.Context;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.OptIn;
import androidx.media3.common.util.UnstableApi;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.reproductorvideos.ExoPlayerActivity;
import com.example.reproductorvideos.Mp3Activity;
import com.example.reproductorvideos.R;
import com.example.reproductorvideos.model.Mp3File;

import java.util.ArrayList;
import java.util.List;

public class Mp3Adapter extends RecyclerView.Adapter<Mp3Adapter.Mp3ViewHolder> {

    private final Context context;
    private final List<Mp3File> mp3List = new ArrayList<>();

    public Mp3Adapter(Context context) {
        this.context = context;
    }

    /** Actualiza la lista mostrada en el RecyclerView */
    public void setMp3List(List<Mp3File> list) {
        mp3List.clear();
        mp3List.addAll(list);
        notifyDataSetChanged();
    }

    @NonNull @Override
    public Mp3ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(context)
                .inflate(R.layout.item_mp3, parent, false);
        return new Mp3ViewHolder(v);
    }

    @OptIn(markerClass = UnstableApi.class)
    @Override
    public void onBindViewHolder(@NonNull Mp3ViewHolder holder, int position) {
        Mp3File mp3 = mp3List.get(position);

        holder.tvTitulo.setText(mp3.getTitulo());
        holder.tvArtista.setText(mp3.getArtista());

        // Portada: URL o placeholder
        if (mp3.getCoverUrl() != null && !mp3.getCoverUrl().isEmpty()) {
            Glide.with(context)
                    .load(mp3.getCoverUrl())
                    .placeholder(R.drawable.default_cover)
                    .into(holder.imgCover);
        } else {
            holder.imgCover.setImageResource(R.drawable.default_cover);
        }

        holder.itemView.setOnClickListener(v -> {
            if (!(context instanceof Mp3Activity)) return;
            Mp3Activity act = (Mp3Activity) context;
            if (!act.isServiceBound()) return;
            Intent intent = new Intent(context, ExoPlayerActivity.class);
            intent.putExtra("mp3List", new ArrayList<>(mp3List));
            intent.putExtra("position", position);
            context.startActivity(intent);
        });

    }

    @Override
    public int getItemCount() {
        return mp3List.size();
    }

    static class Mp3ViewHolder extends RecyclerView.ViewHolder {
        final TextView tvTitulo, tvArtista;
        final ImageView imgCover;

        Mp3ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvTitulo  = itemView.findViewById(R.id.tvTituloMp3);
            tvArtista = itemView.findViewById(R.id.tvArtistaMp3);
            imgCover  = itemView.findViewById(R.id.coverImage);
        }
    }
}
