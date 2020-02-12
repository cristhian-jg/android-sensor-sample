package com.crisgon.sport;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapView;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.model.LatLng;

/**
 * Created by @cristhian-jg on 11/02/2020.
 */
public class DialogMapView extends DialogFragment implements DialogInterface.OnClickListener, OnMapReadyCallback {

    private static final String MAP_VIEW_BUNDLE_KEY = "MapViewBundleKey";

    public static final String LATITUD = "latitud";
    public static final String LONGITUD = "longitud";

    private MapView mapView;
    private GoogleMap gmap;
    private double latitud;
    private double longitud;

    @NonNull
    @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
        Bundle arguments = getArguments();
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        LayoutInflater inflater = getActivity().getLayoutInflater();
        View layout = inflater.inflate(R.layout.dialog_map_view, null);
        builder.setView(layout);

        Bundle mapViewBundle = null;
        if (savedInstanceState != null) {
            mapViewBundle = savedInstanceState.getBundle(MAP_VIEW_BUNDLE_KEY);
        }

        mapView = layout.findViewById(R.id.mapView);
        mapView.onCreate(mapViewBundle);
        mapView.onResume();
        mapView.getMapAsync(this);

        latitud = 0;
        longitud = 0;

        if(arguments != null) {
            latitud = arguments.getDouble(LATITUD, 0);
            longitud = arguments.getDouble(LONGITUD, 0);
        }

        return builder.create();
    }

    @Override
    public void onClick(DialogInterface dialog, int which) {
        Log.i("Diálogos", "Aceptar");
        dialog.dismiss();
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        gmap = googleMap;
        gmap.setMinZoomPreference(12);
        LatLng ny = new LatLng(latitud, longitud);
        gmap.moveCamera(CameraUpdateFactory.newLatLng(ny));
    }

    @Override
    public void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);

        Bundle mapViewBundle = outState.getBundle(MAP_VIEW_BUNDLE_KEY);
        if (mapViewBundle == null) {
            mapViewBundle = new Bundle();
            outState.putBundle(MAP_VIEW_BUNDLE_KEY, mapViewBundle);
        }

        mapView.onSaveInstanceState(mapViewBundle);
    }
}

