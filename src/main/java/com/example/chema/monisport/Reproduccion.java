package com.example.chema.monisport;

import android.content.Intent;
import android.graphics.Color;
import android.graphics.Point;
import android.net.Uri;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.Display;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.Toast;

import com.example.chema.monisport.utils.customExoPlayerListener;
import com.google.android.exoplayer2.ExoPlayerFactory;
import com.google.android.exoplayer2.SimpleExoPlayer;
import com.google.android.exoplayer2.extractor.DefaultExtractorsFactory;
import com.google.android.exoplayer2.extractor.ExtractorsFactory;
import com.google.android.exoplayer2.source.ExtractorMediaSource;
import com.google.android.exoplayer2.source.MediaSource;
import com.google.android.exoplayer2.trackselection.AdaptiveTrackSelection;
import com.google.android.exoplayer2.trackselection.DefaultTrackSelector;
import com.google.android.exoplayer2.trackselection.TrackSelection;
import com.google.android.exoplayer2.trackselection.TrackSelector;
import com.google.android.exoplayer2.ui.SimpleExoPlayerView;
import com.google.android.exoplayer2.upstream.BandwidthMeter;
import com.google.android.exoplayer2.upstream.DataSource;
import com.google.android.exoplayer2.upstream.DefaultBandwidthMeter;
import com.google.android.exoplayer2.upstream.DefaultDataSourceFactory;
import com.google.android.exoplayer2.util.Util;
import com.jjoe64.graphview.GraphView;

import java.io.File;
import java.io.FileReader;
import java.text.DecimalFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;

import com.jjoe64.graphview.LegendRenderer;
import com.jjoe64.graphview.Viewport;
import com.jjoe64.graphview.series.DataPoint;
import com.jjoe64.graphview.series.DataPointInterface;
import com.jjoe64.graphview.series.LineGraphSeries;
import com.jjoe64.graphview.series.OnDataPointTapListener;
import com.jjoe64.graphview.series.Series;
import com.opencsv.CSVReader;



/**
 * Created by Chema on 16/10/2017.
 */

public class Reproduccion  extends AppCompatActivity{

    /**
     * Tag for Log
     */
    private static final String TAG = "ActividadReproduccion";

    private static HashMap graficas=new HashMap<String,GraphView>();
    private static LinearLayout scrollContainer;
    private ScrollView scrollGraficas;
    private static String nombreSesion;
    private SimpleExoPlayer mMediaPlayer;
    private SimpleExoPlayerView simpleExoPlayerView;
    private Button btnSincronizar;
    private int width_pantalla;
    private int ancho_deseado;
    private int alto_deseado;
    private DecimalFormat decimal = new DecimalFormat("+0.00;-0.00");

    private static double tiempo_actual =0;
    private boolean reproduciendo=false;
    private static boolean sincronizar=true;

    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.reproductor);
        Intent intent = getIntent();

        Display display =  getWindowManager().getDefaultDisplay();
        Point size = new Point();
        display.getSize(size);
        width_pantalla = size.x;
        ancho_deseado=(int)(width_pantalla/1.5);
        alto_deseado=(int)(ancho_deseado/1.5);

        scrollContainer=(LinearLayout) findViewById(R.id.scroll_container_reproductor);

        File f=(File)intent.getExtras().get("Directorio");
        File[] filesCsv=f.listFiles();

        String nombre=f.getName();
        SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyyMMdd_HHmmss");
        try{
            Date date = simpleDateFormat.parse(nombre);
            nombreSesion = "Sesión del "+new SimpleDateFormat("dd/MM/yyyy' a las 'HH:mm").format(date);
        }catch (ParseException ex)
        {
            Log.e(TAG,"Exception "+ex);
        }
        for (int o = 0; o < filesCsv.length; o++) {
            File dato = filesCsv[o];
            String nombreArchivo=dato.getName();
            String[] arrayArchivo=nombreArchivo.split("\\.");
            String[] arrayDatos=arrayArchivo[0].split("_");

            if(arrayDatos[0].equals("DATOS")){
                try{
                    CSVReader reader = new CSVReader(new FileReader(dato));
                    List<String[]> myEntries = reader.readAll();
                    reader.close();
                    Iterator it=myEntries.iterator();
                    String nombreDatos;

                    String[] d=(String[])it.next();
                    nombreDatos=d[0];
                    it.next();
                    String[] d2=(String[])it.next();
                    int cantidad=2;
                    ArrayList<DataPoint> dataPointsX=new ArrayList<DataPoint>();
                    ArrayList<DataPoint> dataPointsY=new ArrayList<DataPoint>();
                    ArrayList<DataPoint> dataPointsZ=new ArrayList<DataPoint>();
                    if(d2.length==3){
                        cantidad=3;
                    }else if(d2.length==4){
                        cantidad=4;
                    }

                    while(it.hasNext()){
                        String[] dd=(String[])it.next();

                        double tiempo=Double.parseDouble(dd[0]);
                        double x=Double.parseDouble(dd[1]);
                        dataPointsX.add(new DataPoint(tiempo,x));
                        if(cantidad==3){
                            double y=Double.parseDouble(dd[2]);
                            dataPointsY.add(new DataPoint(tiempo,y));
                        }else if(cantidad==4){
                            double y=Double.parseDouble(dd[2]);
                            dataPointsY.add(new DataPoint(tiempo,y));
                            double z=Double.parseDouble(dd[3]);
                            dataPointsZ.add(new DataPoint(tiempo,z));
                        }
                    }
                    GraphView nGraph = new GraphView(this);
                    nGraph.getViewport().setXAxisBoundsManual(true);
                    nGraph.getViewport().setMinX(-4);
                    nGraph.getViewport().setMaxX(50);
                    nGraph.getViewport().setScrollable(true);
                    nGraph.setLayerType(View.LAYER_TYPE_HARDWARE,null);
                    nGraph.setTitle(nombreDatos);
                    nGraph.setBackgroundColor(Color.WHITE);
                    nGraph.setLayoutParams(new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,200));
                    nGraph.getGridLabelRenderer().setNumHorizontalLabels(10);
                    graficas.put(nombreDatos,nGraph);

                    LineGraphSeries<DataPoint> series = new LineGraphSeries<>(dataPointsX.toArray(new DataPoint[dataPointsX.size()]));
                    series.setOnDataPointTapListener(new OnDataPointTapListener() {
                        @Override
                        public void onTap(Series series, DataPointInterface dataPoint) {
                            int segundoExtra=0;
                            if(dataPoint.getX()%1>0.5){
                                segundoExtra=1;
                            }
                            int segundo=(int)dataPoint.getX()+segundoExtra;
                            Toast.makeText(getApplication(), "Segundo: "+segundo, Toast.LENGTH_SHORT).show();
                            if(sincronizar){
                                mMediaPlayer.seekTo((int)(dataPoint.getX()*1000));
                                mMediaPlayer.setPlayWhenReady(false);
                            }else{
                                actualizarGraficas(dataPoint.getX());
                            }

                        }
                    });
                    nGraph.addSeries(series);


                    if(cantidad==3){
                        series.setTitle("x");
                        LineGraphSeries<DataPoint> seriesy=new LineGraphSeries<>(dataPointsY.toArray(new DataPoint[dataPointsY.size()]));
                        seriesy.setTitle("y");
                        seriesy.setColor(Color.GREEN);
                        nGraph.addSeries(seriesy);
                        seriesy.setOnDataPointTapListener(new OnDataPointTapListener() {
                            @Override
                            public void onTap(Series series, DataPointInterface dataPoint) {
                                int segundoExtra=0;
                                if(dataPoint.getX()%1>0.5){
                                    segundoExtra=1;
                                }
                                int segundo=(int)dataPoint.getX()+segundoExtra;
                                Toast.makeText(getApplication(), "Segundo: "+segundo, Toast.LENGTH_SHORT).show();
                                if(sincronizar){
                                    mMediaPlayer.seekTo((int)(dataPoint.getX()*1000));
                                    mMediaPlayer.setPlayWhenReady(false);
                                }else{
                                    actualizarGraficas(dataPoint.getX());
                                }
                            }
                        });
                    }else if(cantidad==4){
                        series.setTitle("x");
                        LineGraphSeries<DataPoint> seriesy=new LineGraphSeries<>(dataPointsY.toArray(new DataPoint[dataPointsY.size()]));
                        seriesy.setTitle("y");
                        seriesy.setColor(Color.GREEN);
                        nGraph.addSeries(seriesy);
                        seriesy.setOnDataPointTapListener(new OnDataPointTapListener() {
                            @Override
                            public void onTap(Series series, DataPointInterface dataPoint) {
                                int segundoExtra=0;
                                if(dataPoint.getX()%1>0.5){
                                    segundoExtra=1;
                                }
                                int segundo=(int)dataPoint.getX()+segundoExtra;
                                Toast.makeText(getApplication(), "Segundo: "+segundo, Toast.LENGTH_SHORT).show();
                                if(sincronizar){
                                    mMediaPlayer.seekTo((int)(dataPoint.getX()*1000));
                                    mMediaPlayer.setPlayWhenReady(false);
                                }else{
                                    actualizarGraficas(dataPoint.getX());
                                }
                            }
                        });
                        LineGraphSeries<DataPoint> seriesz=new LineGraphSeries<>(dataPointsZ.toArray(new DataPoint[dataPointsZ.size()]));
                        seriesz.setTitle("z");
                        seriesz.setOnDataPointTapListener(new OnDataPointTapListener() {
                            @Override
                            public void onTap(Series series, DataPointInterface dataPoint) {
                                int segundoExtra=0;
                                if(dataPoint.getX()%1>0.5){
                                    segundoExtra=1;
                                }
                                int segundo=(int)dataPoint.getX()+segundoExtra;
                                Toast.makeText(getApplication(), "Segundo: "+segundo, Toast.LENGTH_SHORT).show();
                                if(sincronizar){
                                    mMediaPlayer.seekTo((int)(dataPoint.getX()*1000));
                                    mMediaPlayer.setPlayWhenReady(false);
                                }else{
                                    actualizarGraficas(dataPoint.getX());
                                }
                            }
                        });
                        seriesz.setColor(Color.RED);
                        nGraph.addSeries(seriesz);
                    }
                    if(cantidad>2){
                        nGraph.getLegendRenderer().setVisible(true);
                        nGraph.getLegendRenderer().setAlign(LegendRenderer.LegendAlign.BOTTOM);
                    }
                    DataPoint[] points = new DataPoint[2];
                    points[0] = new DataPoint(0, nGraph.getViewport().getMinY(false));
                    points[1] = new DataPoint(0,nGraph.getViewport().getMaxY(false));
                    LineGraphSeries<DataPoint> seriesp = new LineGraphSeries<>(points);
                    seriesp.setColor(Color.BLACK);
                    nGraph.addSeries(seriesp);

                    scrollGraficas=(ScrollView) findViewById(R.id.scroll_graficas_reproductor);
                    ViewGroup.LayoutParams p=scrollGraficas.getLayoutParams();
                    p.width=width_pantalla-ancho_deseado;
                    p.height=ancho_deseado;
                    scrollGraficas.setLayoutParams(p);
                    scrollContainer.addView(nGraph);

                }catch (Exception e){
                    Log.e(TAG,"Exception "+e);
                }

            }else if(arrayDatos[0].equals("VID")){
                BandwidthMeter bandwidthMeter = new DefaultBandwidthMeter();
                TrackSelection.Factory videoTrackSelectionFactory =
                        new AdaptiveTrackSelection.Factory(bandwidthMeter);
                TrackSelector trackSelector =
                        new DefaultTrackSelector(videoTrackSelectionFactory);
                // 2. Create the player
                mMediaPlayer =
                        ExoPlayerFactory.newSimpleInstance(this, trackSelector);
                simpleExoPlayerView= (SimpleExoPlayerView) findViewById(R.id.camera_preview_reproductor);
                simpleExoPlayerView.setPlayer(mMediaPlayer);
                simpleExoPlayerView.getLayoutParams().width=ancho_deseado;
                simpleExoPlayerView.getLayoutParams().height=alto_deseado;
                // Measures bandwidth during playback. Can be null if not required.

                // Produces DataSource instances through which media data is loaded.
                DataSource.Factory dataSourceFactory = new DefaultDataSourceFactory(this,
                        Util.getUserAgent(this, "MoniSport"));
                // Produces Extractor instances for parsing the media data.
                ExtractorsFactory extractorsFactory = new DefaultExtractorsFactory();
                // This is the MediaSource representing the media to be played.

                MediaSource videoSource = new ExtractorMediaSource(Uri.fromFile(dato),
                        dataSourceFactory, extractorsFactory, null, null);
                // Prepare the player with the source.
                mMediaPlayer.prepare(videoSource);

                mMediaPlayer.addListener(
                        new customExoPlayerListener(this)
                );

            }
        }
        btnSincronizar = (Button) findViewById(R.id.button_sincronizar);
        btnSincronizar.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (sincronizar) {
                    btnSincronizar.setText(getResources().getString(R.string.desincronizar));
                    sincronizar=false;
                }else{
                    btnSincronizar.setText(getResources().getString(R.string.sincronizar));
                    sincronizar=true;
                }
            }
        });
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (mMediaPlayer != null) mMediaPlayer.release();
    }

    public SimpleExoPlayer getPlayer(){
        return mMediaPlayer;
    }
    public void setTiempoActual(double i){
        tiempo_actual=i;
    }
    public void actualizarGraficas(){
        if(sincronizar){
            Iterator it=graficas.entrySet().iterator();
            while (it.hasNext()) {
                HashMap.Entry pair = (HashMap.Entry) it.next();
                GraphView g = (GraphView) pair.getValue();
                List<Series> todasSeries=g.getSeries();
                Viewport viewport=g.getViewport();
                LineGraphSeries<DataPoint> series = (LineGraphSeries<DataPoint>) todasSeries.get(g.getSeries().size() - 1);
                LineGraphSeries<DataPoint> seriesPrimera=(LineGraphSeries<DataPoint>)todasSeries.get(0);
                if(seriesPrimera.getHighestValueX() > 50){
                    if(tiempo_actual < 25){
                        if(viewport.getMinX(false)!=-4){
                            viewport.setMinX(-4);
                            viewport.setMaxX(50);
                        }
                    }else if (tiempo_actual >= 25){
                        if(tiempo_actual+27<=seriesPrimera.getHighestValueX()+4) {
                            viewport.setMinX(tiempo_actual-29);
                            viewport.setMaxX(tiempo_actual+25);
                        }else if(viewport.getMaxX(false)!=seriesPrimera.getHighestValueX()+4){
                            viewport.setMinX(seriesPrimera.getHighestValueX()-50);
                            viewport.setMaxX(seriesPrimera.getHighestValueX()+4);
                        }
                    }
                }

                DataPoint[] data=new DataPoint[2];
                data[0]= new DataPoint(tiempo_actual,viewport.getMinY(false));
                data[1]= new DataPoint(tiempo_actual,viewport.getMaxY(false));
                series.resetData(data);

                int tamano= todasSeries.size();
                String valor="";
                for(int i=0; i<tamano-1;i++){
                    if(tamano==2) {
                        valor += getValorInterpolado((LineGraphSeries<DataPoint>) todasSeries.get(i), tiempo_actual);
                    }else{
                        switch (i){
                            case 0:
                                valor += "x: "+ getValorInterpolado((LineGraphSeries<DataPoint>) todasSeries.get(i), tiempo_actual);
                                break;
                            case 1:
                                valor += " y: "+ getValorInterpolado((LineGraphSeries<DataPoint>) todasSeries.get(i), tiempo_actual);
                                break;
                            case 2:
                                valor += " z: "+ getValorInterpolado((LineGraphSeries<DataPoint>) todasSeries.get(i), tiempo_actual);
                                break;
                        }
                    }
                }
                g.setTitle((String)pair.getKey()+" actual: "+valor);
            }
        }

    }
    public void actualizarGraficas(double clickado){
        if(!sincronizar){
            Iterator it=graficas.entrySet().iterator();
            while (it.hasNext()) {
                HashMap.Entry pair = (HashMap.Entry) it.next();
                GraphView g = (GraphView) pair.getValue();
                List<Series> todasSeries=g.getSeries();
                Viewport viewport=g.getViewport();
                LineGraphSeries<DataPoint> series = (LineGraphSeries<DataPoint>) todasSeries.get(g.getSeries().size() - 1);
                LineGraphSeries<DataPoint> seriesPrimera=(LineGraphSeries<DataPoint>)todasSeries.get(0);
                if(seriesPrimera.getHighestValueX() > 50){
                    if(clickado < 25){
                        if(viewport.getMinX(false)!=-4){
                            viewport.setMinX(-4);
                            viewport.setMaxX(50);
                        }
                    }else if (clickado >= 25){
                        if(clickado+27<=seriesPrimera.getHighestValueX()+4) {
                            viewport.setMinX(clickado-29);
                            viewport.setMaxX(clickado+25);
                        }else if(viewport.getMaxX(false)!=seriesPrimera.getHighestValueX()+4){
                            viewport.setMinX(seriesPrimera.getHighestValueX()-50);
                            viewport.setMaxX(seriesPrimera.getHighestValueX()+4);
                        }
                    }
                }

                DataPoint[] data=new DataPoint[2];
                data[0]= new DataPoint(clickado,viewport.getMinY(false));
                data[1]= new DataPoint(clickado,viewport.getMaxY(false));
                series.resetData(data);

                int tamano= todasSeries.size();
                String valor="";
                for(int i=0; i<tamano-1;i++){
                    if(tamano==2) {
                        valor += getValorInterpolado((LineGraphSeries<DataPoint>) todasSeries.get(i), clickado);
                    }else{
                        switch (i){
                            case 0:
                                valor += "x: "+ getValorInterpolado((LineGraphSeries<DataPoint>) todasSeries.get(i), clickado);
                                break;
                            case 1:
                                valor += " y: "+ getValorInterpolado((LineGraphSeries<DataPoint>) todasSeries.get(i), clickado);
                                break;
                            case 2:
                                valor += " z: "+ getValorInterpolado((LineGraphSeries<DataPoint>) todasSeries.get(i), clickado);
                                break;
                        }
                    }
                }
                g.setTitle((String)pair.getKey()+" actual: "+valor);
            }
        }

    }
    public void setReproduciendo(boolean b){
        reproduciendo=b;

    }
    public boolean getReproduciendo(){
        return reproduciendo;
    }

    private String getValorInterpolado(LineGraphSeries serie, double tiempo){ // CONTROLAR ANTES DE LLAMAR
        Iterator valores=serie.getValues(serie.getLowestValueX(),serie.getHighestValueX());
        double valor=0;
        double menorTiempo=0;
        double menorValor=0;
        while(valores.hasNext()){
            DataPoint p=(DataPoint)valores.next();
            if(p.getX()==tiempo){
                valor=p.getY();
                break;
            }else {
                if(p.getX()<tiempo){
                    menorValor=p.getY();
                    menorTiempo=p.getX();
                }else{
                    //ecuacion de la recta
                    valor=(((p.getY()-menorValor)/(p.getX()-menorTiempo))*(tiempo-menorTiempo))+menorValor;
                    break;
                }
            }
        }
        return decimal.format(valor);
    }
}
