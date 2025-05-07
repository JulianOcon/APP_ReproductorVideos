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
import com.example.reproductorvideos.R;
import com.example.reproductorvideos.model.Video;
import com.example.reproductorvideos.ReproducirActivity;

import java.util.List;

public class VideoAdapter extends RecyclerView.Adapter<VideoAdapter.VideoViewHolder> {

    private Context context;
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

        // Cargar miniatura con Glide desde el servidor
        Glide.with(context)
                .load(video.getThumbnailUrl()) // ← CORREGIDO
                .placeholder(R.drawable.placeholder) // Opcional
                .into(holder.thumbnailImage);

        // Al hacer clic, abrir nueva actividad para reproducir el video
        holder.itemView.setOnClickListener(v -> {
            Intent intent = new Intent(context, ReproducirActivity.class);
            intent.putExtra("video_url", video.getUrl());
            context.startActivity(intent);
        });
    }

    public void updateData(List<Video> nuevosVideos) {
        videoList.clear();
        videoList.addAll(nuevosVideos);
        notifyDataSetChanged();
    }

    @Override
    public int getItemCount() {
        return videoList.size();
    }

    public static class VideoViewHolder extends RecyclerView.ViewHolder {
        TextView titleTextView;
        ImageView thumbnailImage;

        public VideoViewHolder(@NonNull View itemView) {
            super(itemView);
            titleTextView = itemView.findViewById(R.id.videoTitle);
            thumbnailImage = itemView.findViewById(R.id.thumbnailImage);
        }
    }
}
