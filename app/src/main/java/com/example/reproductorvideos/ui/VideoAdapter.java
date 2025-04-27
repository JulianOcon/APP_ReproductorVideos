package com.example.reproductorvideos.ui;

import android.content.Context;
import android.net.Uri;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.VideoView;
import com.example.reproductorvideos.R;


import androidx.recyclerview.widget.RecyclerView;

import com.example.reproductorvideos.model.Video;

import java.util.List;

public class VideoAdapter extends RecyclerView.Adapter<VideoAdapter.VideoViewHolder> {

    private Context context;
    private List<Video> videoList;

    public VideoAdapter(Context context, List<Video> videoList) {
        this.context = context;
        this.videoList = videoList;
    }

    @Override
    public VideoViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        // Infla el layout personalizado para cada item
        View view = LayoutInflater.from(context).inflate(R.layout.video_item, parent, false);
        return new VideoViewHolder(view);
    }

    @Override
    public void onBindViewHolder(VideoViewHolder holder, int position) {
        // Obtiene el video actual de la lista
        Video video = videoList.get(position);

        // Asigna el título del video al TextView
        holder.textView.setText(video.getTitle());

        // Asigna la URL del video al VideoView y comienza la reproducción
        Uri videoUri = Uri.parse(video.getUrl());  // Se obtiene la URL del video
        holder.videoView.setVideoURI(videoUri);
        holder.videoView.start();  // Comienza a reproducir el video
    }

    @Override
    public int getItemCount() {
        return videoList.size();
    }

    // ViewHolder para cada item del RecyclerView
    public static class VideoViewHolder extends RecyclerView.ViewHolder {
        TextView textView;
        VideoView videoView;

        public VideoViewHolder(View itemView) {
            super(itemView);
            textView = itemView.findViewById(R.id.videoTitle);  // Asigna el TextView
            videoView = itemView.findViewById(R.id.videoView);  // Asigna el VideoView
        }
    }
}
