package com.crisgon.sport;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.content.pm.PackageManager;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.location.Location;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MainActivity extends AppCompatActivity implements OnMapReadyCallback {

    public static final String TAG = "MainActivity";

    public static final int REQUEST_ACCESS_COURSE_LOCATION = 1;
    public static final int REQUEST_ACCESS_FINE_LOCATION = 2;

    private GoogleMap mMap;
    private FusedLocationProviderClient fusedLocationClient;
    private Location ubicacionActual;
    private MarkerOptions markerOptions;
    private boolean hayLocalizacion = false;
    private boolean hayPermiso = true;

    private FirebaseFirestore db = FirebaseFirestore.getInstance();

    private List<Sensor> sensores;
    private SensorEventListener sensorEventListener;
    private SensorManager sensorManager;
    private Sensor sensorRitmo;
    private Sensor sensorPasos;

    private Button btnIniciar;
    private TextView tvFrecuenciaCardiaca;
    private TextView tvPasosDados;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        btnIniciar = (Button) findViewById(R.id.btnIniciar);
        tvFrecuenciaCardiaca = (TextView) findViewById(R.id.tvFrecuenciaCardiaca);
        tvPasosDados = (TextView) findViewById(R.id.tvPasosDados);

        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);

        sensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);

        /** Obtengo un objecto FusedLocaltionProviderClient mediante LocationServices y compruebo los permisos */
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(MainActivity.this);
        if (ContextCompat.checkSelfPermission(MainActivity.this,
                android.Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            hayPermiso = false;
            ActivityCompat.requestPermissions(MainActivity.this,
                    new String[]{android.Manifest.permission.ACCESS_COARSE_LOCATION},
                    REQUEST_ACCESS_COURSE_LOCATION);
        }
        if (ContextCompat.checkSelfPermission(MainActivity.this,
                Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            hayPermiso = false;
            ActivityCompat.requestPermissions(MainActivity.this,
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                    REQUEST_ACCESS_FINE_LOCATION);
        }

        if (hayPermiso) getLastLocation();
        else Log.d(getClass().getSimpleName(), "Sin permisos para obtener la ubicación");

        btnIniciar.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                /** Los sensores empiezan a escuchar una vez se le da al botón iniciar */

                btnIniciar.setVisibility(View.INVISIBLE);
                Toast.makeText(MainActivity.this,
                        "Sensors enabled",
                        Toast.LENGTH_SHORT).show();

                sensorEventListener = new SensorEventListener() {
                    @Override
                    public void onSensorChanged(SensorEvent event) {
                        synchronized (this) {
                            switch (event.sensor.getType()) {
                                case Sensor.TYPE_STEP_COUNTER:
                                    Log.i(TAG, "StepCounter: " + event.values[0]);
                                    //Toast.makeText(MainActivity.this, "StepCounter: " + event.values[0], Toast.LENGTH_SHORT).show();
                                    if (event.values[0] <= 9)
                                        tvPasosDados.setText("0" + event.values[0]);
                                    else tvPasosDados.setText(String.valueOf(event.values[0]));
                                    break;
                                case Sensor.TYPE_HEART_RATE:
                                    Log.i(TAG, "HeartRate: " + event.values[0]);
                                    //Toast.makeText(MainActivity.this, "HeartRate: " + event.values[0], Toast.LENGTH_SHORT).show();
                                    if (event.values[0] <= 9)
                                        tvFrecuenciaCardiaca.setText("0" + event.values[0]);
                                    else tvFrecuenciaCardiaca.setText(String.valueOf(event.values[0]));
                                    break;
                            }
                        }
                    }

                    @Override
                    public void onAccuracyChanged(Sensor sensor, int accuracy) {

                    }
                };
                //bundle = new Bundle();
                //if (ubicacionActual != null) {
                //    bundle.putDouble(DialogMapView.LONGITUD, ubicacionActual.getLongitude());
                //    bundle.putDouble(DialogMapView.LATITUD, ubicacionActual.getLatitude());
                //    DialogMapView dialogoMapView = new DialogMapView();
                //    dialogoMapView.setArguments(bundle);
                //    dialogoMapView.show(getSupportFragmentManager(), "error_dialog_mapview");
                //}
            }
        });

        /** Creo el nuevo marcador que mostrará la posicion en el mapa */
        markerOptions = new MarkerOptions();

    }

    @Override
    protected void onResume() {
        super.onResume();
        /** Obtengo la lista de sensores el sensor que cuenta los pasos */
        sensores = sensorManager.getSensorList(Sensor.TYPE_STEP_COUNTER);
        if (!sensores.isEmpty()) {
            sensorPasos = sensores.get(0);
            sensorManager.registerListener(sensorEventListener, sensorPasos, SensorManager.SENSOR_DELAY_NORMAL);
        }

        /** Obtengo la lista de sensores el sensor que cuenta la frecuencia cardiaca */
        sensores = sensorManager.getSensorList(Sensor.TYPE_HEART_RATE);
        if (!sensores.isEmpty()) {
            sensorRitmo = sensores.get(0);
            sensorManager.registerListener(sensorEventListener, sensorRitmo, SensorManager.SENSOR_DELAY_NORMAL);
        }
    }

    /** Quito los sensores del sensorManager para controlar la batería */
    @Override
    protected void onPause() {
        super.onPause();
        sensorManager.unregisterListener(sensorEventListener);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        switch (requestCode) {
            case REQUEST_ACCESS_COURSE_LOCATION:
            case REQUEST_ACCESS_FINE_LOCATION:
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    getLastLocation();
                }
                break;
        }
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
    }

    public void getLastLocation() {
        fusedLocationClient.getLastLocation().addOnSuccessListener(this, new OnSuccessListener<Location>() {
            @Override
            public void onSuccess(Location location) {
                // Comprueba que la ubicación no sea nula, puede pasar en algunas ocaciones
                ubicacionActual = location;
                if (location != null) {
                    Log.d("UBICACIÓN", "Latitud: " + location.getLatitude()
                            + ", Longitud: " + location.getLongitude());
                    Toast.makeText(MainActivity.this, "Latitud: " + location.getLatitude() +
                            ", Longitud: " + location.getLongitude(), Toast.LENGTH_SHORT).show();

                    Map<String, Object> mapLocation = new HashMap<>();
                    mapLocation.put("latitud", location.getLatitude());
                    mapLocation.put("longitud", location.getLongitude());

                    /** Compruebo si ya existe una ubicación en la base de datos */
                    DocumentReference docIdRef = db.collection("ubicaciones").document("1");
                    docIdRef.get().addOnCompleteListener(new OnCompleteListener<DocumentSnapshot>() {
                        @Override
                        public void onComplete(@NonNull Task<DocumentSnapshot> task) {
                            if (task.isSuccessful()) {
                                DocumentSnapshot document = task.getResult();
                                if (document.exists()) {
                                    Log.d(TAG, "Document exists!");
                                    hayLocalizacion = true;
                                } else {
                                    Log.d(TAG, "Document does not exist!");
                                    hayLocalizacion = false;
                                }
                            } else {
                                Log.d(TAG, "Failed with: ", task.getException());
                                hayLocalizacion = true;
                            }
                        }
                    });

                    /** Si no hay se creará una nueva con la ubicación actual */
                    if (!hayLocalizacion) {
                        db.collection("ubicaciones").document("1").set(mapLocation);
                    }

                    /** Si hay se actualizará con la ubicación actual */
                    if (hayLocalizacion) {
                        db.collection("ubicaciones").document().update(mapLocation).addOnSuccessListener(new OnSuccessListener<Void>() {
                            @Override
                            public void onSuccess(Void aVoid) {
                                Log.d(TAG, "DocumentSnapshot was updated");
                            }
                        }).addOnFailureListener(new OnFailureListener() {
                            @Override
                            public void onFailure(@NonNull Exception e) {
                                Log.w(TAG, "Error editting document", e);
                            }
                        });
                    }
                }
            }
        });
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;

        /** Limpio el mapa para eliminar el marker anterior */
        mMap.clear();

        /** Obtengo la ultima longitud y latitud de la ubicación que se encuentra en la base de datos*/
        db.collection("ubicaciones")
                .get()
                .addOnSuccessListener(new OnSuccessListener<QuerySnapshot>() {
                    @Override
                    public void onSuccess(QuerySnapshot queryDocumentSnapshots) {

                        for (QueryDocumentSnapshot queryDocumentSnapshot : queryDocumentSnapshots) {

                            double latitud = queryDocumentSnapshot.getDouble("latitud");
                            double longitud = queryDocumentSnapshot.getDouble("longitud");
                            Log.d(TAG, "{User id: }" + queryDocumentSnapshot.getId() +
                                    ", latitud: " + latitud + ", longitud: " + longitud);

                            /** Creo un nuevo marcador y le indico la posición donde aparecerá */
                            markerOptions.position(new LatLng(latitud, longitud));
                            //if (!hayMarcador) {
                            mMap.addMarker(markerOptions);
                            //hayMarcador = true;
                            //}
                            /** Muevo la camara a la ubicación actual */
                            mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(markerOptions.getPosition(), 8));
                        }

                        /** Invoco este metodo para que vuelva a contar 30 segundos hasta que vuelva a iniciar onMapReady*/
                        countDownTimer();
                    }
                }).addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception e) {
                Log.w(TAG, "Error al leer las ubicaciones.");
            }
        });
    }

    /** CountDownTime que cuenta 30 segundos para volver a ejecutar los metodo getLastLocation y on MapReady*/
    private void countDownTimer() {
        new CountDownTimer(30000, 1000) {

            @Override
            public void onTick(long millisUntilFinished) {
                Log.i(TAG, "onTick: " + millisUntilFinished / 1000);
            }

            /** Pasados 30 segundos ejecuta onMapReady y vuelve a consultar la longitud y latitud*/
            @Override
            public void onFinish() {
                /** Vuelvo a obtener la localización, por lo que si me he movido deberá haber cambiado
                 * la longitud y latitud*/
                getLastLocation();
                /** Vuelvo a llamar a el metodo onMapReady para que vuelva a obtener la ubicación de la base de datos */
                onMapReady(mMap);
            }
        }.start();
    }
}
