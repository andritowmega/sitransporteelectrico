package pe.gob.sitransporte.sitransporteconductor;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Context;
import android.content.pm.PackageManager;
import android.location.LocationListener;
import android.location.LocationManager;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.PowerManager;
import android.speech.tts.TextToSpeech;
import android.util.Log;
import android.widget.CompoundButton;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import com.here.android.mpa.common.GeoCoordinate;
import com.here.android.mpa.common.OnEngineInitListener;
import com.here.android.mpa.mapping.Map;
import com.here.android.mpa.mapping.MapMarker;
import com.here.android.mpa.mapping.MapObject;
import com.here.android.mpa.mapping.SupportMapFragment;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;

import static android.media.AudioManager.AUDIOFOCUS_GAIN_TRANSIENT;

public class HereActivity extends AppCompatActivity {

    TextToSpeech t1;
    private PowerManager.WakeLock wakelock;
    HashMap<String, String> paramstts;

    private Map map = null;
    GeoCoordinate coordinate;

    // map fragment embedded in this activity
    private SupportMapFragment mapFragment = null;
    private final static int REQUEST_CODE_ASK_PERMISSIONS = 1;
    private static final String[] REQUIRED_SDK_PERMISSIONS = new String[]{
            Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.WRITE_EXTERNAL_STORAGE};

    Double lat = 0.0;
    Double lng = 0.0;

    List<MapObject> StopsMarkersO = new ArrayList<MapObject>();
    List<GeoCoordinate> StopsMarkers = new ArrayList<GeoCoordinate>();
    List<String> StopsMarkersNames = new ArrayList<String>();

    com.here.android.mpa.common.Image myImage;
    com.here.android.mpa.common.Image paraderoImage;

    MapMarker mm;

    LocationManager milocManager;
    private LocationListener milocListener;

    int position=100;
    int position2=100;
    int position3=100;

    String readspech = "";

    String[] mensajes = {"Bienvenidos, esta unidad se encuentra acreditada en la etapa preoperativa del sit.", "En esta unidad, estamos felices de transportarlo todos los días, gracias por contar con nosotros para llevarlo a su destino.", "Por favor, avanzen a la parte posterior para dar mayor comodidad","Estimado usuario, solicite su parada con la debida anticipación.","Muchas gracias por su amabilidad, cedamos el asiento a quién lo necesita.","Cuidemos la limpieza y el orden de nuestros buses"};
    int countmsn=0;
    int countacomulatemsn=0;

    int result;

    AudioManager audioManager;

    MediaPlayer alarmas;

    Switch switchrute;
    TextView titletop;

    @SuppressLint("InvalidWakeLockTag")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        checkPermissions();

        t1 = new TextToSpeech(getApplicationContext(), new TextToSpeech.OnInitListener() {
            @Override
            public void onInit(int i) {
                t1.setLanguage(Locale.getDefault());
                paramstts = new HashMap<String, String>();
                paramstts.put(TextToSpeech.Engine.KEY_PARAM_VOLUME, "10");
            }
        });
        final PowerManager pm=(PowerManager)getSystemService(Context.POWER_SERVICE);
        this.wakelock=pm.newWakeLock(PowerManager.SCREEN_DIM_WAKE_LOCK, "etiqueta");
        wakelock.acquire();
        alarmas = MediaPlayer.create(this,R.raw.alarma);
        audioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
        audioManager.adjustStreamVolume(AudioManager.STREAM_MUSIC, AudioManager.ADJUST_LOWER, AudioManager.FLAG_SHOW_UI);
        audioManager.adjustStreamVolume(AudioManager.STREAM_MUSIC, AudioManager.ADJUST_LOWER, AudioManager.FLAG_SHOW_UI);
        audioManager.adjustStreamVolume(AudioManager.STREAM_MUSIC, AudioManager.ADJUST_LOWER, AudioManager.FLAG_SHOW_UI);
        audioManager.adjustStreamVolume(AudioManager.STREAM_MUSIC, AudioManager.ADJUST_LOWER, AudioManager.FLAG_SHOW_UI);
    }
    private SupportMapFragment getMapFragment() {
        return (SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.mapfragment);
    }
    private void initialize() {
        setContentView(R.layout.activity_here);
        mapFragment = getMapFragment();
        map = mapFragment.getMap();
        titletop = findViewById(R.id.titletop);
        switchrute = findViewById(R.id.switch1);



        // Set up disk cache path for the map service for this application
        boolean success = com.here.android.mpa.common.MapSettings.setIsolatedDiskCacheRootPath(
                getApplicationContext().getExternalFilesDir(null) + File.separator + ".here-maps",
                "com.here.android.tutorial.MapService");
        if (!success) {
            Toast.makeText(getApplicationContext(), "Unable to set isolated disk cache path.", Toast.LENGTH_LONG);
        } else {
            mapFragment.init(new OnEngineInitListener() {
                @Override
                public void onEngineInitializationCompleted(OnEngineInitListener.Error error) {
                    if (error == OnEngineInitListener.Error.NONE) {
                        // retrieve a reference of the map from the map fragment
                        map = mapFragment.getMap();
                        // Set the map center to the Vancouver region (no animation)
                        coordinate = new GeoCoordinate(-16.3988403, -71.5371017);
                        map.setCenter(coordinate,
                                com.here.android.mpa.mapping.Map.Animation.NONE);
                        // Set the zoom level to the average between min and max
                        map.setZoomLevel((map.getMaxZoomLevel() + map.getMinZoomLevel()) / 1.2);
                        Log.d("mapa"," ok");
                        //new StopsRute().execute(data);
                        //creando Cluster Layer
                        myImage = new com.here.android.mpa.common.Image();
                        paraderoImage = new com.here.android.mpa.common.Image();
                        try {
                            myImage.setImageResource(R.drawable.busicon);
                            paraderoImage.setImageResource(R.drawable.icono_paradero);
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                        mm = new MapMarker();
                        mm.setIcon(myImage);
                        ParaderosIda();
                        milocManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
                        milocListener = new MiLocationListener();
                        if (ActivityCompat.checkSelfPermission(HereActivity.this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(HereActivity.this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                            // TODO: Consider calling
                            //    ActivityCompat#requestPermissions
                            // here to request the missing permissions, and then overriding
                            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
                            //                                          int[] grantResults)
                            // to handle the case where the user grants the permission. See the documentation
                            // for ActivityCompat#requestPermissions for more details.
                        }
                        milocManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 5000, 0, milocListener);

                    } else {
                        System.out.println("ERROR: Cannot initialize Map Fragment");
                    }
                }

            });
        }
        switchrute.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if(isChecked){
                    ParaderosVuelta();
                    Toast.makeText(getApplicationContext(), "> Vuelta <", Toast.LENGTH_SHORT).show();
                    titletop.setText("Vuelta");
                }
                else{
                    ParaderosIda();
                    Toast.makeText(getApplicationContext(), "< Ida >", Toast.LENGTH_SHORT).show();
                    titletop.setText("Ida");
                }
            }
        });

    }
    protected void checkPermissions() {
        final List<String> missingPermissions = new ArrayList<String>();
        // check all required dynamic permissions
        for (final String permission : REQUIRED_SDK_PERMISSIONS) {
            final int result = ContextCompat.checkSelfPermission(this, permission);
            if (result != PackageManager.PERMISSION_GRANTED) {
                missingPermissions.add(permission);
            }
        }
        if (!missingPermissions.isEmpty()) {
            // request all missing permissions
            final String[] permissions = missingPermissions
                    .toArray(new String[missingPermissions.size()]);
            ActivityCompat.requestPermissions(this, permissions, REQUEST_CODE_ASK_PERMISSIONS);
        } else {
            final int[] grantResults = new int[REQUIRED_SDK_PERMISSIONS.length];
            Arrays.fill(grantResults, PackageManager.PERMISSION_GRANTED);
            onRequestPermissionsResult(REQUEST_CODE_ASK_PERMISSIONS, REQUIRED_SDK_PERMISSIONS,
                    grantResults);
        }
    }
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String permissions[],
                                           @NonNull int[] grantResults) {
        switch (requestCode) {
            case REQUEST_CODE_ASK_PERMISSIONS:
                for (int index = permissions.length - 1; index >= 0; --index) {
                    if (grantResults[index] != PackageManager.PERMISSION_GRANTED) {
                        // exit the app if one permission is not granted
                        Toast.makeText(this, "Required permission '" + permissions[index]
                                + "' not granted, exiting", Toast.LENGTH_LONG).show();
                        finish();
                        return;
                    }
                }
                // all permissions were granted
                initialize();
                break;
        }
    }
    private void ParaderosIda(){
        if(StopsMarkersO!=null) {
            map.removeMapObjects(StopsMarkersO);
            StopsMarkersO.clear();
        }
        if(StopsMarkers!=null){
            StopsMarkers.clear();
        }
        if(StopsMarkersNames!=null){
            StopsMarkersNames.clear();
        }
        StopsMarkers.add(new GeoCoordinate(-16.404107334787518,-71.527239690977126));
        StopsMarkers.add(new GeoCoordinate(-16.393374098025529,-71.535323415264671));
        StopsMarkers.add(new GeoCoordinate(-16.406000248991418,-71.539838321333903));
        StopsMarkers.add(new GeoCoordinate(-16.39831609307705,-71.5384597272555));
        StopsMarkers.add(new GeoCoordinate(-16.397882084999939,-71.523712001999968));
        StopsMarkers.add(new GeoCoordinate(-16.3939316,-71.521985983999969));
        StopsMarkers.add(new GeoCoordinate(-16.39009269547347,-71.527400036574448));
        StopsMarkers.add(new GeoCoordinate(-16.39214674699997,-71.54089251399995));
        StopsMarkers.add(new GeoCoordinate(-16.390404874999941,-71.545837800999948));
        StopsMarkers.add(new GeoCoordinate(-16.38879507199993,-71.550233980999963));
        StopsMarkers.add(new GeoCoordinate(-16.375526526001959,-71.557821765487631));
        StopsMarkers.add(new GeoCoordinate(-16.35406878799995,-71.567821436999964));
        StopsMarkers.add(new GeoCoordinate(-16.343253419319382,-71.577704919501741));
        StopsMarkers.add(new GeoCoordinate(-16.409827756768738,-71.534219140854461));

        for(int i=0;i<StopsMarkers.size();i++){
            MapMarker myMapMarker =
                    new MapMarker(StopsMarkers.get(i), paraderoImage);
            StopsMarkersO.add(myMapMarker);
        }

        StopsMarkersNames.add("Paucarpata");
        StopsMarkersNames.add("San Lazaro");
        StopsMarkersNames.add("La Merced");
        StopsMarkersNames.add("Bolognesi");
        StopsMarkersNames.add("Muñoz Najar");
        StopsMarkersNames.add("Plaza Mayta Cápac");
        StopsMarkersNames.add("Progreso");
        StopsMarkersNames.add("Recoleta");
        StopsMarkersNames.add("Claro");
        StopsMarkersNames.add("Real Plaza");
        StopsMarkersNames.add("Metro");
        StopsMarkersNames.add("Zamácola");
        StopsMarkersNames.add("Pesquero");
        StopsMarkersNames.add("Estadio");

        Log.d("Cantidad Markers",StopsMarkers.size()+"");
        Log.d("cantidad Nombres", StopsMarkersNames.size()+"");
        Log.d("cantidad MObjects", StopsMarkersO.size()+"");

        map.addMapObjects(StopsMarkersO);

    }

    private void ParaderosVuelta(){
        if(StopsMarkersO!=null) {
            map.removeMapObjects(StopsMarkersO);
            StopsMarkersO.clear();
        }
        if(StopsMarkers!=null){
            StopsMarkers.clear();
        }
        if(StopsMarkersNames!=null){
            StopsMarkersNames.clear();
        }
        StopsMarkers.add(new GeoCoordinate(-16.38885309852332,-71.549456793845835));
        StopsMarkers.add(new GeoCoordinate(-16.375043993086891,-71.557932235207645));
        StopsMarkers.add(new GeoCoordinate(-16.39018179565922,-71.54585950647693));
        StopsMarkers.add(new GeoCoordinate(-16.364994498247761,-71.560966524625172));
        StopsMarkers.add(new GeoCoordinate(-16.353628694968521,-71.567951619077718));
        StopsMarkers.add(new GeoCoordinate(-16.343250371308962,-71.577161576980075));


        for(int i=0;i<StopsMarkers.size();i++){
            MapMarker myMapMarker =
                    new MapMarker(StopsMarkers.get(i), paraderoImage);
            StopsMarkersO.add(myMapMarker);
        }

        StopsMarkersNames.add("Emmel");
        StopsMarkersNames.add("La Fonda");
        StopsMarkersNames.add("Mol Plaza");
        StopsMarkersNames.add("Primavera");
        StopsMarkersNames.add("Zamacola");
        StopsMarkersNames.add("Pesquero");


        Log.d("Cantidad Markers",StopsMarkers.size()+"");
        Log.d("cantidad Nombres", StopsMarkersNames.size()+"");
        Log.d("cantidad MObjects", StopsMarkersO.size()+"");

        map.addMapObjects(StopsMarkersO);

    }

    public class MiLocationListener implements LocationListener {
        public void onLocationChanged(final android.location.Location loc) {

            lat=loc.getLatitude();
            lng=loc.getLongitude();

            Log.i("GPS LOCATIONMANAGER", "GPS , " + loc.getLatitude() + "," + loc.getLongitude() + "");
            coordinate.setLatitude(lat);
            coordinate.setLongitude(lng);
            map.removeMapObject(mm);
            mm.setCoordinate(coordinate);
            map.setCenter(coordinate, com.here.android.mpa.mapping.Map.Animation.BOW);
            map.addMapObject(mm);
            for(int i=0;i<StopsMarkers.size();i++){
                double hipotenusa = distances(StopsMarkers.get(i).getLatitude(),StopsMarkers.get(i).getLongitude());
                if(hipotenusa>0 && hipotenusa<0.002){

                    if(position!=i && position2!=i && position3!=i) {
                        position3 = position2;
                        position2 = position;
                        position = i;
                        readspech =  "Siguente parada, "+ StopsMarkersNames.get(i);

                        Log.d("RANGOOK",StopsMarkersNames.get(i) + " - "+hipotenusa+" - "+readspech);

                        if(!t1.isSpeaking()){
                            if(countacomulatemsn%5==0){
                                readspech = readspech+". "+mensajes[countmsn];
                                countmsn++;
                            }
                            countacomulatemsn++;
                            Log.d("LEERPERMITIDO",readspech);
                            new CalculateDelay().execute(readspech);
                            if(countmsn>=mensajes.length){
                                countmsn=0;
                            }
                        }
                        else Log.d("LEENDOCANCELADO",readspech);
                    }
                    else{
                        Log.d("RANGONORP",StopsMarkersNames.get(i) + " - "+hipotenusa+" - "+readspech);
                    }
                }
                else{
                    Log.d("RANGO NO",StopsMarkersNames.get(i)+ " - "+hipotenusa);
                }
            }

            Log.d("LISTA MARKERS",StopsMarkers.size()+"");
        }
        public void onProviderDisabled(String provider) {
            Toast.makeText(getApplicationContext(), "Gps Desactivado", Toast.LENGTH_SHORT).show();
        }

        public void onProviderEnabled(String provider) {
            Toast.makeText(getApplicationContext(), "Gps Activo", Toast.LENGTH_SHORT).show();
        }

        public void onStatusChanged(String provider, int status, Bundle extras) {
        }
    }
    private double distances(double latcal, double lngcal){
        double cateto1 = lat - latcal;
        double cateto2 = lng - lngcal;
        return Math.sqrt(cateto1*cateto1 + cateto2*cateto2);
    }
    private class CalculateDelay extends AsyncTask<String, Integer, Boolean> {
        @RequiresApi(api = Build.VERSION_CODES.KITKAT)
        @Override
        protected Boolean doInBackground(String... params) {
            Log.d("PARAMS",params[0]);
            result = audioManager.requestAudioFocus(mOnAudioFocusChangeListener, AudioManager.STREAM_MUSIC, AudioManager.AUDIOFOCUS_GAIN_TRANSIENT);
            //result = audioManager.requestAudioFocus(mOnAudioFocusChangeListener,AudioManager.STREAM_MUSIC,AudioManager.AUDIOFOCUS_GAIN_TRANSIENT_EXCLUSIVE);
            int size = params[0].length();
            Log.d("Size", size + "");
            if (result == AudioManager.AUDIOFOCUS_REQUEST_GRANTED || result == AudioManager.AUDIOFOCUS_LOSS) {
                audioManager.adjustStreamVolume(AudioManager.STREAM_MUSIC, AudioManager.ADJUST_RAISE, AudioManager.FLAG_SHOW_UI);
                audioManager.adjustStreamVolume(AudioManager.STREAM_MUSIC, AudioManager.ADJUST_RAISE, AudioManager.FLAG_SHOW_UI);
                audioManager.adjustStreamVolume(AudioManager.STREAM_MUSIC, AudioManager.ADJUST_RAISE, AudioManager.FLAG_SHOW_UI);
                audioManager.adjustStreamVolume(AudioManager.STREAM_MUSIC, AudioManager.ADJUST_RAISE, AudioManager.FLAG_SHOW_UI);
                alarmas.start();
                try {
                    Thread.sleep(1500);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }

                Log.d("LEENDO",params[0]);
                t1.speak(params[0], TextToSpeech.QUEUE_FLUSH, paramstts);

                Log.d("VOICE ASYNTASK ", params[0]);
            } else if (result == AudioManager.AUDIOFOCUS_REQUEST_FAILED) {
                Log.d("VOICE: ", "SIN FOCUS");
            } else {
                Log.d("VOICE: ", "SIN FOCUS, ERROR DESCONOCIDO");
            }

            int delaytotal = (((size * 3) / 36) * 1000) + 2000;
            Log.d("DELAY", delaytotal + "");
            try {
                Thread.sleep(delaytotal - 1000);
            } catch (InterruptedException e) {
            }
            return true;

        }

        @Override
        protected void onPostExecute(Boolean result) {

            if(result){
                audioManager.adjustStreamVolume(AudioManager.STREAM_MUSIC, AudioManager.ADJUST_LOWER, AudioManager.FLAG_SHOW_UI);
                audioManager.adjustStreamVolume(AudioManager.STREAM_MUSIC, AudioManager.ADJUST_LOWER, AudioManager.FLAG_SHOW_UI);
                audioManager.adjustStreamVolume(AudioManager.STREAM_MUSIC, AudioManager.ADJUST_LOWER, AudioManager.FLAG_SHOW_UI);
                audioManager.adjustStreamVolume(AudioManager.STREAM_MUSIC, AudioManager.ADJUST_LOWER, AudioManager.FLAG_SHOW_UI);
                int result2 = audioManager.abandonAudioFocus(mOnAudioFocusChangeListener);
            }
            else{

            }
        }

        @Override
        protected void onCancelled() {

        }
    }
    private AudioManager.OnAudioFocusChangeListener mOnAudioFocusChangeListener = new AudioManager.OnAudioFocusChangeListener() {

        @Override
        public void onAudioFocusChange(int focusChange) {
            switch (focusChange) {
                case AudioManager.AUDIOFOCUS_GAIN:
                    Log.i("AUDIO", "AUDIOFOCUS_GAIN");
                    // Set volume level to desired levels
                    //play();
                    break;
                case AUDIOFOCUS_GAIN_TRANSIENT:
                    Log.i("AUDIO", "AUDIOFOCUS_GAIN_TRANSIENT");
                    // You have audio focus for a short time
                    //play();
                    break;
                case AudioManager.AUDIOFOCUS_GAIN_TRANSIENT_MAY_DUCK:
                    Log.i("AUDIO", "AUDIOFOCUS_GAIN_TRANSIENT_MAY_DUCK");
                    // Play over existing audio
                    //play();
                    break;
                case AudioManager.AUDIOFOCUS_LOSS:
                    Log.e("AUDIO", "AUDIOFOCUS_LOSS");
                    //stop();
                    break;
                case AudioManager.AUDIOFOCUS_LOSS_TRANSIENT:
                    Log.e("AUDIO", "AUDIOFOCUS_LOSS_TRANSIENT");
                    // Temporary loss of audio focus - expect to get it back - you can keep your resources around
                    //pause();
                    break;
                case AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK:
                    Log.e("AUDIO", "AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK");
                    // Lower the volume
                    break;
            }
        }
    };
}
