package com.example.reproductorvideos.utils;

import android.content.Context;
import android.content.SharedPreferences;

import androidx.media3.common.C;

import java.util.HashSet;
import java.util.Set;

public class FavoritosManager {
    private static final String PREFS_NAME = "FAVORITOS_PREFS";
    private static final String KEY_VIDEOS = "VIDEOS_FAVORITOS";

    private static final String KEY_MP3 = "MP3_FAVORITOS";

    public static void agregarFavorito(Context context, String url) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        Set<String> favs = new HashSet<>(prefs.getStringSet(KEY_VIDEOS, new HashSet<>()));
        favs.add(url);
        prefs.edit().putStringSet(KEY_VIDEOS, favs).apply();
    }

    public static void quitarFavorito(Context context, String url) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        Set<String> favs = new HashSet<>(prefs.getStringSet(KEY_VIDEOS, new HashSet<>()));
        favs.remove(url);
        prefs.edit().putStringSet(KEY_VIDEOS, favs).apply();
    }

    public static boolean esFavorito(Context context, String url) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        Set<String> favs = prefs.getStringSet(KEY_VIDEOS, new HashSet<>());
        return favs != null && favs.contains(url);
    }

    public static Set<String> obtenerFavoritos(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        return prefs.getStringSet(KEY_VIDEOS, new HashSet<>());
    }


    public static void agregarFavoritoMp3(Context context, String url) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        Set<String> favs = new HashSet<>(prefs.getStringSet(KEY_MP3, new HashSet<>()));
        favs.add(url);
        prefs.edit().putStringSet(KEY_MP3, favs).apply();
    }

    public static void quitarFavoritoMp3(Context context, String url) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        Set<String> favs = new HashSet<>(prefs.getStringSet(KEY_MP3, new HashSet<>()));
        favs.remove(url);
        prefs.edit().putStringSet(KEY_MP3, favs).apply();
    }

    public static boolean esFavoritoMp3(Context context, String url) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        Set<String> favs = prefs.getStringSet(KEY_MP3, new HashSet<>());
        return favs != null && favs.contains(url);
    }

    public static Set<String> obtenerFavoritosMp3(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        return prefs.getStringSet(KEY_MP3, new HashSet<>());
    }


}
