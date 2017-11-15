package com.example.chema.monisport;

import android.content.Context;
import android.content.pm.ActivityInfo;
import android.graphics.Color;
import android.graphics.Point;
import android.media.CamcorderProfile;
import android.media.MediaRecorder;
import android.os.Handler;
import android.os.Message;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.hardware.Camera;
import android.util.Log;
import android.view.Display;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.Button;
import android.widget.LinearLayout;
import android.view.ViewGroup.LayoutParams;
import android.widget.RelativeLayout;
import android.widget.ScrollView;

import com.example.chema.monisport.SensorTag.BarometerCalibrationCoefficients;
import com.example.chema.monisport.SensorTag.SensorTag;
import com.example.chema.monisport.utils.Point3D;

import com.jjoe64.graphview.GraphView;
import com.jjoe64.graphview.LegendRenderer;
import com.jjoe64.graphview.series.DataPoint;
import com.jjoe64.graphview.series.LineGraphSeries;
import com.opencsv.CSVWriter;


import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;


public class Grabacion extends AppCompatActivity {

    private static final String TAG = "ActividadCamara";

    private Camera mCamera;
    private CameraPreview mPreview;
    private MediaRecorder mediaRecorder;
    private static boolean isRecording = false;
    private int ancho_deseado;
    private int alto_deseado;
    private int width_pantalla;
    private int height_pantalla;
    private Button btn;
    private LinearLayout preview;
    private int orientacionAnt;
    private int orientacion;

    private final Handler mHandler = new Handler();
    private Runnable mTimer2;
    private static double graph2LastXValue;

    private DecimalFormat decimal = new DecimalFormat("+0.00;-0.00");
    private static final double PA_PER_METER = 12.0;
    private static HashMap graficas=new HashMap<String,GraphView>();
    private static LinearLayout scrollContainer;
    private static boolean viendose=false;
    private static Context c;
    private ScrollView scrollGraficas;
    private static String nombreSesion;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        c=this;
        setContentView(R.layout.camera);
        setRequestedOrientation (ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
        orientacionAnt=1;
        // Create an instance of Camera
        mCamera = getCameraInstance();

        Display display =  getWindowManager().getDefaultDisplay();
        Point size = new Point();
        display.getSize(size);
        width_pantalla = size.x;
        height_pantalla =size.y;
        ancho_deseado=(int)(width_pantalla/1.5);
        alto_deseado=(int)(ancho_deseado/1.5);
        preview = (LinearLayout) findViewById(R.id.camera_preview);

        scrollContainer=(LinearLayout) findViewById(R.id.scroll_container);

        setup();

        scrollGraficas=(ScrollView) findViewById(R.id.scroll_graficas);
        LayoutParams p=scrollGraficas.getLayoutParams();
        p.width=width_pantalla-ancho_deseado;
        scrollGraficas.setLayoutParams(p);

        final Animation rotateAnim0a270 = AnimationUtils.loadAnimation(this,R.anim.rotation0a270);
        final Animation rotateAnim180a270 = AnimationUtils.loadAnimation(this,R.anim.rotation180a270);
        final Animation rotateAnim90a180 = AnimationUtils.loadAnimation(this,R.anim.rotation90a180);
        final Animation rotateAnim270a180 = AnimationUtils.loadAnimation(this,R.anim.rotation270a180);
        final Animation rotateAnim270a0 = AnimationUtils.loadAnimation(this,R.anim.rotation270a0);
        final Animation rotateAnim90a0 = AnimationUtils.loadAnimation(this,R.anim.rotation90a0);
        final Animation rotateAnim0a90 = AnimationUtils.loadAnimation(this,R.anim.rotation0a90);
        final Animation rotateAnim180a90 = AnimationUtils.loadAnimation(this,R.anim.rotation180a90);

        btn = (Button) findViewById(R.id.button_capture);
        btn.setLayerType(View.LAYER_TYPE_HARDWARE,null);


        SimpleOrientationListener mOrientationListener = new SimpleOrientationListener(
             this   ) {
            @Override
            public void onSimpleOrientationChanged(int orientation) {

               switch (orientation){
                   case 1:
                       if(mCamera!=null&&isRecording==false) {
                          orientacion=0;
                       }
                       if(orientacionAnt==2){
                           btn.startAnimation(rotateAnim270a0);

                       }else if(orientacionAnt==4){
                           btn.startAnimation(rotateAnim90a0);

                       }
                       break;
                   case 2:
                       if(mCamera!=null&&isRecording==false){
                           orientacion=90;
                       }
                       if(orientacionAnt==1){
                           btn.startAnimation(rotateAnim0a270);

                       }else if(orientacionAnt==3){
                           btn.startAnimation(rotateAnim180a270);

                       }
                       break;
                   case 3:
                       if(mCamera!=null&&isRecording==false) {
                          orientacion=180;
                       }
                       if(orientacionAnt==2){
                           btn.startAnimation(rotateAnim270a180);

                       }else if(orientacionAnt==4){
                           btn.startAnimation(rotateAnim90a180);

                       }
                       break;
                   case 4:
                       if(mCamera!=null&&isRecording==false) {
                           orientacion=270;
                       }
                       if(orientacionAnt==1){
                           btn.startAnimation(rotateAnim0a90);


                       }else if(orientacionAnt==3){
                           btn.startAnimation(rotateAnim180a90);

                       }
                       break;
                }
                moveGraph(orientation);
                orientacionAnt=orientation;
            }
        };

        mOrientationListener.enable();

        btn.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) {
                if (isRecording) {
                    mediaRecorder.stop();
                    releaseMediaRecorder();
                    stopGraph();
                    mCamera.lock();
                    btn.setText("Grabar");
                    isRecording = false;

                }else{
                    if(prepareVideoRecorder()){
                        mediaRecorder.start();
                        startGraph();
                        btn.setText("Parar");
                        isRecording = true;
                    }else{
                        releaseMediaRecorder();
                    }
                }
            }
        });


    }


    private boolean prepareVideoRecorder() {
        mediaRecorder= new MediaRecorder();
        Camera.Size optimalPreviewSize = getOptimalPreviewSize(mCamera.getParameters().getSupportedVideoSizes(), ancho_deseado);
        mCamera.unlock();

        mediaRecorder.setCamera(mCamera);
        mediaRecorder.setAudioSource(MediaRecorder.AudioSource.CAMCORDER);
        mediaRecorder.setVideoSource(MediaRecorder.VideoSource.CAMERA);
        CamcorderProfile profile=CamcorderProfile.get(CamcorderProfile.QUALITY_480P);

        mediaRecorder.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4);
        mediaRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.DEFAULT);
        mediaRecorder.setVideoEncodingBitRate(profile.videoBitRate);
        mediaRecorder.setVideoFrameRate(profile.videoFrameRate);
        mediaRecorder.setVideoEncoder(profile.videoCodec);

        mediaRecorder.setOrientationHint(orientacion);

        mediaRecorder.setVideoSize(optimalPreviewSize.width,optimalPreviewSize.height);

        mediaRecorder.setOutputFile(getOutputMediaFile(MEDIA_TYPE_VIDEO).toString());
        mediaRecorder.setPreviewDisplay(mPreview.getmHolder().getSurface());
        try {
            mediaRecorder.prepare();
        } catch (IllegalStateException e) {
            Log.d("ERROR CAMARA", "IllegalStateException preparing MediaRecorder: " + e.getMessage());
            releaseMediaRecorder();
            return false;

        } catch (IOException e) {
            Log.d("ERROR CAMARA", "IOException preparing MediaRecorder: " + e.getMessage());
            releaseMediaRecorder();
            return false;
        }
        return true;
    }

    public static final int MEDIA_TYPE_VIDEO = 2;

    /** Create a File for saving an image or video */
    private static File getOutputMediaFile(int type){
        // To be safe, you should check that the SDCard is mounted
        // using Environment.getExternalStorageState() before doing this.
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
        nombreSesion="/storage/emulated/0/MoniSport/Session/"+timeStamp;
        File mediaStorageDir = new File(nombreSesion);
        // This location works best if you want the created images to be shared
        // between applications and persist after your app has been uninstalled.

        // Create the storage directory if it does not exist
        if (! mediaStorageDir.exists()){
            if (! mediaStorageDir.mkdirs()){
                Log.d("MyCameraApp", "failed to create directory");
                return null;
            }
        }

        // Create a media file name

        File mediaFile;
        if(type == MEDIA_TYPE_VIDEO) {
            mediaFile = new File(mediaStorageDir.getPath() + File.separator +
                    "VID_"+ timeStamp + ".mp4");
        } else {
            return null;
        }

        return mediaFile;
    }

    private void releaseMediaRecorder(){
        if (mediaRecorder != null) {
            mediaRecorder.reset();   // clear recorder configuration
            mediaRecorder.release(); // release the recorder object
            mediaRecorder = null;
            mCamera.lock();           // lock camera for later use
        }
    }
        /** A safe way to get an instance of the Camera object. */
    public static Camera getCameraInstance(){
        Camera c = null;
        try {
            c = Camera.open(); // attempt to get a Camera instance
        }
        catch (Exception e){
            // Camera is not available (in use or does not exist)
        }
        return c; // returns null if camera is unavailable
    }

    @Override
    public void onDestroy(){
        super.onDestroy();
        setdown();
    }

    @Override
    protected void onPause() {
        super.onPause();
        setdown();
    }

    @Override
    protected void onResume() {
        super.onResume();

        preview.removeAllViews();
        if(mCamera==null){
            mCamera=getCameraInstance();
        }
        setup();
        viendose=true;

    }



    private void setup(){
        // Create our Preview view and set it as the content of our activity.
        mPreview = new CameraPreview(this, mCamera);
        Camera.Size optimalPreviewSize = getOptimalPreviewSize(mCamera.getParameters().getSupportedPreviewSizes(), ancho_deseado);
        mPreview.setSize_preview(optimalPreviewSize);

        preview.getLayoutParams().width=ancho_deseado;
        preview.getLayoutParams().height=alto_deseado;
        preview.addView(mPreview);


    }
    private void setdown(){
        if(isRecording){
            mediaRecorder.stop();
            stopGraph();
        }
        releaseMediaRecorder();
        btn.setText("Grabar");
        isRecording = false;
        mPreview.getHolder().removeCallback(mPreview);
        if(mCamera!=null){
            mCamera.release();
            mCamera=null;
        }

    }
    private void startGraph(){
        graph2LastXValue=0;
        mTimer2 = new Runnable() {
            @Override
            public void run() {
                mHandler.postDelayed(this, 200);
                graph2LastXValue += 0.2d;
            }
        };
        //mHandler.postDelayed(mTimer2, 1000);
        mHandler.postDelayed(mTimer2, 0);
    }
    private void stopGraph(){
        Iterator itgrafica = graficas.entrySet().iterator();
        while(itgrafica.hasNext()){
            List<String[]> data = new ArrayList<String[]>();
            HashMap.Entry pair = (HashMap.Entry)itgrafica.next();
            String nombre=(String)pair.getKey();
            data.add(new String[]{nombre});
            GraphView g=(GraphView) pair.getValue();
            int tamano= g.getSeries().size();

            Iterator valores=g.getSeries().get(0).getValues(g.getSeries().get(0).getLowestValueX(),g.getSeries().get(0).getHighestValueX());
            if(tamano==1){
                data.add(new String[]{"tiempo:","valor:"});
                while(valores.hasNext()){
                    DataPoint p=(DataPoint)valores.next();
                    data.add(new String[]{String.valueOf((double) Math.round(p.getX()*1000)/1000),String.valueOf(p.getY())});
                }
            }else if(tamano==2){
                Iterator valores2=g.getSeries().get(1).getValues(0,g.getSeries().get(1).getHighestValueX());
                while(valores.hasNext()) {
                    data.add(new String[]{"tiempo:","x:","y:"});
                    DataPoint x = (DataPoint) valores.next();
                    DataPoint y = (DataPoint) valores2.next();
                    data.add(new String[]{String.valueOf((double) Math.round(x.getX()*1000)/1000),String.valueOf(x.getY()),String.valueOf(y.getY())});
                }
            }else if(tamano==3){
                data.add(new String[]{"tiempo:","x:","y:","z:"});
                Iterator valores2=g.getSeries().get(1).getValues(0,g.getSeries().get(1).getHighestValueX());
                Iterator valores3=g.getSeries().get(2).getValues(0,g.getSeries().get(2).getHighestValueX());
                while(valores.hasNext()) {
                    DataPoint x = (DataPoint) valores.next();
                    DataPoint y = (DataPoint) valores2.next();
                    DataPoint z = (DataPoint) valores3.next();
                    data.add(new String[]{String.valueOf((double) Math.round(x.getX()*1000)/1000),String.valueOf(x.getY()),String.valueOf(y.getY()),String.valueOf(z.getY())});
                }
            }

            //Limpiamos las graficas
            g.removeAllSeries();
            g.getViewport().setMinX(-4);
            g.getViewport().setMaxX(50);
            //Guardamos los datos en un csv
            File mediaStorageDir = new File(nombreSesion);
            String filePath = mediaStorageDir.getPath() + File.separator +"DATOS_"+nombre+".csv";
            CSVWriter writer;
            try{
                writer = new CSVWriter(new FileWriter(filePath));
                writer.writeAll(data,true);
                writer.close();
            }catch (Exception e){
            }
        }
        /*Iterator itdatos = datosArray.entrySet().iterator();
        while(itdatos.hasNext()) {
            List<String[]> data = new ArrayList<String[]>();
            HashMap.Entry pair = (HashMap.Entry) itdatos.next();
            String nombre=(String)pair.getKey();
            data.add(new String[]{nombre});
            ArrayList<Pair<Double,String>> g=(ArrayList<Pair<Double,String>>) pair.getValue();
            Iterator valores=g.iterator();
            while(valores.hasNext()){
                Pair<Double,String> p=(Pair<Double,String>)valores.next();
                data.add(new String[]{String.valueOf((double) Math.round(p.first*1000)/1000),String.valueOf(p.second.replace(",",".").replace("\n"," "))});
            }
            g.clear();
            //Guardamos los datos en un csv
            File mediaStorageDir = new File(nombreSesion);
            String filePath = mediaStorageDir.getPath() + File.separator +"DATOS_"+nombre+".csv";
            CSVWriter writer;
            try{
                writer = new CSVWriter(new FileWriter(filePath));
                writer.writeAll(data,true);
                writer.close();
            }catch (Exception e){
            }
        }*/

        mHandler.removeCallbacks(mTimer2);
    }
    private void moveGraph(int orientacion){
        RelativeLayout.LayoutParams p=(RelativeLayout.LayoutParams)scrollGraficas.getLayoutParams();
        Camera.Size optimalPreviewSize = getOptimalPreviewSize(mCamera.getParameters().getSupportedPreviewSizes(), ancho_deseado);
        int height;
        int width;
        switch (orientacion){
            case 1:
                height=520;
                width=width_pantalla-preview.getWidth();
                scrollGraficas.setRotation(0);
                if(orientacionAnt==2||orientacionAnt==4) {
                    scrollGraficas.setX(preview.getWidth()-273);
                }
                scrollGraficas.setY(0);

                p.width=width;
                p.height=height;
                scrollGraficas.setLayoutParams(p);
                resizeWidthChildren(scrollContainer,width);
                break;
            case 2:
                height = 200;
                width = height_pantalla - 100;
                scrollGraficas.setX(optimalPreviewSize.width+160);
                scrollGraficas.setY(height_pantalla / 2 - 140);
                scrollGraficas.setRotation(-90);
                p.width=width;
                p.height=height;
                scrollGraficas.setLayoutParams(p);
                resizeWidthChildren(scrollContainer,width);
                break;
            case 3:
                if(orientacionAnt==2||orientacionAnt==4) {
                    scrollGraficas.setX(preview.getWidth()-273);
                }
                scrollGraficas.setY(0);
                scrollGraficas.setRotation(180);
                height=520;
                width=width_pantalla-preview.getWidth();
                p.width=width;
                p.height=height;
                scrollGraficas.setLayoutParams(p);
                resizeWidthChildren(scrollContainer,width);
                break;
            case 4:
                scrollGraficas.setX(optimalPreviewSize.width+160);
                scrollGraficas.setY(height_pantalla / 2 - 140);
                scrollGraficas.setRotation(90);
                height=200;
                width=height_pantalla-100;
                p.width=width;
                p.height=height;
                scrollGraficas.setLayoutParams(p);
                resizeWidthChildren(scrollContainer,width);
                break;
        }
    }

    private Camera.Size getOptimalPreviewSize(List<Camera.Size> sizes, int w) {
        final double ASPECT_TOLERANCE = 0.1;
        double targetRatio = 1.5;
        if (sizes == null)
            return null;

        Camera.Size optimalSize = null;
        double minDiff = Double.MAX_VALUE;

        int targetWidth = w;

        // Try to find an size match aspect ratio and size
        for (Camera.Size size : sizes) {
            double ratio = (double) size.width / size.height;
            if (Math.abs(ratio - targetRatio) > ASPECT_TOLERANCE)
                continue;
            if (Math.abs(size.width - targetWidth) < minDiff) {
                optimalSize = size;
                minDiff = Math.abs(size.width - targetWidth);
            }
        }

        // Cannot find the one match the aspect ratio, ignore the
        // requirement
        if (optimalSize == null) {
            minDiff = Double.MAX_VALUE;
            for (Camera.Size size : sizes) {
                if (Math.abs(size.width - targetWidth) < minDiff) {
                    optimalSize = size;
                    minDiff = Math.abs(size.width - targetWidth);
                }
            }
        }
        return optimalSize;
    }

    public Handler getHandler(){
        return handlerDatos;
    }

    /**
     * The Handler that gets information back from the BluetoothChatService
     */
    private final Handler handlerDatos = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            if (viendose) {
                byte[] rawValue;
                Point3D v;
                String mensaje;
                switch (msg.what) {
                    case Constantes.HEART_RATE_ZEPHYR:
                        if (!graficas.containsKey("Pulso")) {
                            crearGrafica("Pulso");
                        }
                        if(isRecording){
                            String HeartRatetext = msg.getData().getString("HeartRate");
                            setDatosGrafica("Pulso",HeartRatetext);
                        }
                        //Log.d(TAG, "Heart Rate Info is " + HeartRatetext);
                        break;
                    case Constantes.RESPIRATION_RATE_ZEPHYR:
                        if (!graficas.containsKey("Respiracion")) {
                            crearGrafica("Respiracion");
                        }
                        if(isRecording){
                            String PeakAccText = msg.getData().getString("RespirationRate");
                            setDatosGrafica("Respiracion",PeakAccText);
                        }
                        //Log.d(TAG, "Respiration Rate Info is " + RespirationRatetext);
                        break;
                    case Constantes.ACCELERATION_COMPONENT:
                        if (!graficas.containsKey("Aceleracion")) {
                            crearGrafica("Aceleracion");
                        }
                        if(isRecording){
                            double PeakAccText[] = msg.getData().getDoubleArray("Aceleracion");
                            setDatosGrafica("Aceleracion",PeakAccText);
                        }
                        //Log.d(TAG, "Respiration Rate Info is " + RespirationRatetext);
                        break;
                    /*case Constantes.SKIN_TEMPERATURE_ZEPHYR:
                        String SkinTemperaturetext = msg.getData().getString("SkinTemperature");
                        Log.d(TAG, "SkinTemperature Rate Info is " + SkinTemperaturetext);
                        break;*/
                    case Constantes.POSTURE_ZEPHYR:
                        String PostureText = msg.getData().getString("Posture");
                        if(!graficas.containsKey("Postura")){
                            crearGrafica("Postura");
                        }
                        if(isRecording){
                            setDatosGrafica("Postura",PostureText);
                        }
                        //Log.d(TAG, "Posture Rate Info is " + PostureText);
                        break;
                    /*case Constantes.PEAK_ACCLERATION_ZEPHYR:
                        if (!graficas.containsKey("PicoAceleracion")) {
                            crearGrafica("PicoAceleracion");
                        }
                        if(isRecording){
                            String PeakAccText = msg.getData().getString("PeakAcceleration");
                            setDatosGrafica("PicoAceleracion",PeakAccText);
                        }
                        //Log.d(TAG, "Aceleration Rate Info is " + PeakAccText);
                        break;*/
                    /*case Constantes.ACCLERATION_SENSORTAG:
                        if(!datos.containsKey("Aceleracion")){
                            crearTabla("Aceleracion",3);
                        }
                        if(isRecording){
                            rawValue = msg.getData().getByteArray("Acelerometro");
                            v = SensorTag.ACCELEROMETER.convert(rawValue);
                            mensaje = "x: "+decimal.format(v.x) + "\ny: " + decimal.format(v.y) + "\nz: "
                                    + decimal.format(v.z) + "\n";
                            setDatos("Aceleracion",mensaje);
                        }

                        //Log.d(TAG, "Acelerometro: " + mensaje);
                        break;*/
                    case Constantes.MAGNETOMETER_SENSORTAG:
                        if(!graficas.containsKey("Magnetometro")){
                            crearGrafica("Magnetometro");
                        }
                        if(isRecording) {
                            rawValue = msg.getData().getByteArray("MAGNETOMETER");
                            v = SensorTag.MAGNETOMETER.convert(rawValue);
                            double x= Double.parseDouble(decimal.format(v.x).replace(",", ".").replace("+",""));
                            double y= Double.parseDouble(decimal.format(v.y).replace(",", ".").replace("+",""));
                            double z= Double.parseDouble(decimal.format(v.z).replace(",", ".").replace("+",""));
                            double datos[]={x,y,z};
                            setDatosGrafica("Magnetometro",datos);

                        }//Log.d(TAG, "MAGNETOMETER: " + mensaje);
                        break;
                    case Constantes.GYROSCOPE_SENSORTAG:
                        if(!graficas.containsKey("Giroscopio")){
                            crearGrafica("Giroscopio");
                        }
                        if(isRecording) {
                            rawValue = msg.getData().getByteArray("GYROSCOPE");
                            v = SensorTag.GYROSCOPE.convert(rawValue);
                            double x= Double.parseDouble(decimal.format(v.x).replace(",", ".").replace("+",""));
                            double y= Double.parseDouble(decimal.format(v.y).replace(",", ".").replace("+",""));
                            double z= Double.parseDouble(decimal.format(v.z).replace(",", ".").replace("+",""));
                            double datos[]={x,y,z};
                            setDatosGrafica("Giroscopio",datos);
                        }
                        //Log.d(TAG, "GYROSCOPE: " + mensaje);
                        break;
                    case Constantes.IR_TEMPERATURE_SENSORTAG:
                        if (!graficas.containsKey("TempAmbiente")) {
                            crearGrafica("TempAmbiente");
                        }
                        if (!graficas.containsKey("TempObjeto")) {
                            crearGrafica("TempObjeto");
                        }
                        if(isRecording) {
                            rawValue = msg.getData().getByteArray("IR_TEMPERATURE");
                            v = SensorTag.IR_TEMPERATURE.convert(rawValue);
                            mensaje = decimal.format(v.x) + "\n";
                            setDatosGrafica("TempAmbiente",mensaje);
                            //Log.d(TAG, "Temperatura ambiente: " + mensaje);
                            mensaje = decimal.format(v.y) + "\n";
                            setDatosGrafica("TempObjeto",mensaje);
                            //Log.d(TAG, "Temperatura objeto: " + mensaje);
                        }
                        break;
                    case Constantes.HUMIDITY_SENSORTAG:
                        if (!graficas.containsKey("Humedad")) {
                            crearGrafica("Humedad");
                        }
                        if(isRecording){
                            rawValue = msg.getData().getByteArray("HUMIDITY");
                            v = SensorTag.HUMIDITY.convert(rawValue);
                            mensaje = decimal.format(v.x) + "\n";
                            setDatosGrafica("Humedad",mensaje);
                        }
                        //Log.d(TAG, "HUMEDAD: " + humedad);
                        break;
                    case Constantes.BAROMETER_SENSORTAG:
                        if (!graficas.containsKey("Barometro")) {
                            crearGrafica("Barometro");
                        }
                        if(isRecording) {
                            rawValue = msg.getData().getByteArray("BAROMETER");
                            v = SensorTag.BAROMETER.convert(rawValue);

                            double h = (v.x - BarometerCalibrationCoefficients.INSTANCE.heightCalibration)
                                    / PA_PER_METER;
                            h = (double) Math.round(-h * 10.0) / 10.0;
                            mensaje = decimal.format(v.x / 100.0f);
                            setDatosGrafica("Barometro",mensaje);
                        }
                       // Log.d(TAG, "Barometro: " + mensaje);
                        break;
                }
            }
        }

    };

    private void crearGrafica(String nombre){
        GraphView nGraph = new GraphView(c);
        nGraph.getViewport().setXAxisBoundsManual(true);
        nGraph.getViewport().setMinX(-4);
        nGraph.getViewport().setMaxX(50);
        nGraph.setLayerType(View.LAYER_TYPE_HARDWARE,null);
        nGraph.setTitle(nombre);
        nGraph.setBackgroundColor(Color.WHITE);
        nGraph.setLayoutParams(new LayoutParams(LayoutParams.MATCH_PARENT,200));
        nGraph.getGridLabelRenderer().setNumHorizontalLabels(10);
        graficas.put(nombre,nGraph);

        scrollContainer.addView(nGraph);
    }

    private void setDatosGrafica(String nombre, String dato){
        boolean scroll;
        double res = Double.parseDouble(dato.replace(",", ".").replace("+",""));
        if (graph2LastXValue >= 46) {
            scroll = true;
        } else {
            scroll = false;
        }
        GraphView g = (GraphView) graficas.get(nombre);
        if(g.getSeries().isEmpty()){
            LineGraphSeries<DataPoint> series = new LineGraphSeries<>();
            g.addSeries(series);
        }
        LineGraphSeries<DataPoint> s = (LineGraphSeries) g.getSeries().get(0);
        s.appendData(new DataPoint(graph2LastXValue, res), scroll, Integer.MAX_VALUE);
        g.setTitle(nombre+" actual:" +res);
    }
    private void setDatosGrafica(String nombre, double[] dato){
        boolean scroll;
        double x=dato[0];
        double y=dato[1];
        double z=0;
        if(dato.length==3){
            z=dato[2];
        }

        if (graph2LastXValue >= 46) {
            scroll = true;
        } else {
            scroll = false;
        }
        GraphView g = (GraphView) graficas.get(nombre);

        if(g.getSeries().isEmpty()){
            for(int i=0;i<dato.length;i++){
                LineGraphSeries<DataPoint> series = new LineGraphSeries<>();
                series.setTitle("x");
                if(i==1){
                    series.setColor(Color.GREEN);
                    series.setTitle("y");
                }else if(i==2){
                    series.setColor(Color.RED);
                    series.setTitle("z");
                }
                g.addSeries(series);
            }
        }
        LineGraphSeries<DataPoint> seriesx = (LineGraphSeries) g.getSeries().get(0);
        LineGraphSeries<DataPoint> seriesy = (LineGraphSeries) g.getSeries().get(1);
        seriesx.appendData(new DataPoint(graph2LastXValue, x), scroll, Integer.MAX_VALUE);
        seriesy.appendData(new DataPoint(graph2LastXValue, y), scroll, Integer.MAX_VALUE);
        String titulo=nombre+" actual: x:"+x+" y:"+y;
        if(dato.length==3){
            LineGraphSeries<DataPoint> seriesz = (LineGraphSeries) g.getSeries().get(2);
            seriesz.appendData(new DataPoint(graph2LastXValue, z), scroll, Integer.MAX_VALUE);
            titulo+=" z:"+z;
        }
        g.getLegendRenderer().setVisible(true);
        g.getLegendRenderer().setAlign(LegendRenderer.LegendAlign.BOTTOM);
        g.setTitle(titulo);
    }
    private void resizeWidthChildren(LinearLayout parent, int newWidth) {
        // size is in pixels so make sure you have taken device display density into account
        int childCount = parent.getChildCount();
        for(int i = 0; i < childCount; i++) {
            View v = parent.getChildAt(i);
            if (v instanceof GraphView) {
                LayoutParams params=((GraphView)v).getLayoutParams();
                params.width=newWidth;
                ((GraphView)v).setLayoutParams(params);
            }
        }
    }
}
