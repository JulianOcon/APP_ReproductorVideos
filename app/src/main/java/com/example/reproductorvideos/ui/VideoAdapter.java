package com.example.reproductorvideos.ui;

import android.content.Context;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.reproductorvideos.utils.FavoritosManager;
import com.example.reproductorvideos.R;
import com.example.reproductorvideos.model.Video;
import com.example.reproductorvideos.ReproducirActivity;

import java.util.List;

public class VideoAdapter extends RecyclerView.Adapter<VideoAdapter.VideoViewHolder> {

    private final Context context;
    private List<Video> videoList;

    public VideoAdapter(Context context, List<Video> videoList) {
        this.context = context;
        this.videoList = videoList;
    }

    @NonNull
    @Override
    public VideoViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.video_item, parent, false);
        return new VideoViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull VideoViewHolder holder, int position) {
        Video video = videoList.get(position);
        holder.titleTextView.setText(video.getTitle());

        Glide.with(context)
                .load(video.getThumbnailUrl())
                .placeholder(R.drawable.placeholder)
                .into(holder.thumbnailImage);

        boolean esFavorito = FavoritosManager.esFavorito(context, video.getUrl());
        video.setFavorito(esFavorito);
        holder.btnFavorito.setImageResource(
                esFavorito ? R.drawable.ic_fav_on : R.drawable.ic_fav_off);

        holder.btnFavorito.setOnClickListener(v-> {
            boolean nuevoEstado = !video.isFavorito();
            video.setFavorito(nuevoEstado);
            if (nuevoEstado) {
                FavoritosManager.agregarFavorito(context, video.getUrl());
                holder.btnFavorito.setImageResource(R.drawable.ic_fav_on);
            } else {
                FavoritosManager.quitarFavorito(context, video.getUrl());
                holder.btnFavorito.setImageResource(R.drawable.ic_fav_off);
            }
        });

        holder.itemView.setOnClickListener(v -> {
            if (context instanceof ReproducirActivity) {
                ((ReproducirActivity) context).reproducirNuevoVideo(video.getUrl(), video.getTitle());
            } else {
                Intent intent = new Intent(context, ReproducirActivity.class);
                intent.putExtra("video_url", video.getUrl());
                intent.putExtra("video_titulo", video.getTitle());
                context.startActivity(intent);
            }
        });
    }

    public void updateData(List<Video> nuevosVideos) {
        videoList.clear();
        videoList.addAll(nuevosVideos);
        notifyDataSetChanged();
    }

    @Override
    public int getItemCount() {
        return videoList != null ? videoList.size() : 0;
    }

    public static class VideoViewHolder extends RecyclerView.ViewHolder {
        TextView titleTextView;
        ImageView thumbnailImage;
        ImageView btnFavorito;

        public VideoViewHolder(@NonNull View itemView) {
            super(itemView);
            titleTextView = itemView.findViewById(R.id.videoTitle);
            thumbnailImage = itemView.findViewById(R.id.thumbnailImage);
            btnFavorito = itemView.findViewById(R.id.btnFavorito);
        }
    }

    public void actualizarLista(List<Video> nuevaLista) {
        this.videoList = nuevaLista;
        notifyDataSetChanged();
    }
}
