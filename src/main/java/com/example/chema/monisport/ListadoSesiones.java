package com.example.chema.monisport;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListView;

import java.io.File;
import com.example.chema.monisport.utils.flechaAdapter;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Date;
import java.util.List;

/**
 * Created by Chema on 09/10/2017.
 */

public class ListadoSesiones extends AppCompatActivity {

    /**
     * Tag for Log
     */
    private static final String TAG = "ActividadListado";
    private File[] files;

    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.listado_sesiones);

        File f = new File("/storage/emulated/0/MoniSport/Session/");
        files = f.listFiles();
        // Array TEXTO donde guardaremos los nombres de los ficheros
        List<String> item = new ArrayList<String>();
        //Hacemos un Loop por cada fichero para extraer el nombre de cada uno
        Arrays.sort( files, new Comparator()
        {
            public int compare(Object o1, Object o2) {

                if (((File)o1).lastModified() > ((File)o2).lastModified()) {
                    return -1;
                } else if (((File)o1).lastModified() < ((File)o2).lastModified()) {
                    return +1;
                } else {
                    return 0;
                }
            }
        });

        for (int i = 0; i < files.length; i++){
            //Sacamos del array files un fichero
            File file = files[i];

            //Si es directorio...
            if (file.isDirectory()){
                String nombre=file.getName();

                SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyyMMdd_HHmmss");
                try{
                    Date date = simpleDateFormat.parse(nombre);

                    String nuevoDateFormat = new SimpleDateFormat("dd/MM/yyyy' a las 'HH:mm").format(date);
                    File[] filesCsv=file.listFiles();
                    String contenido = "\nDatos:";
                    for (int o = 0; o < filesCsv.length; o++){
                        File dato=filesCsv[o];
                        String nombreArchivo=dato.getName();
                        String[] arrayArchivo=nombreArchivo.split("\\.");
                        String[] arrayDatos=arrayArchivo[0].split("_");

                        if(arrayDatos[0].equals("DATOS")){
                            contenido+=" "+arrayDatos[1]+",";
                        }else if(arrayDatos[0].equals("VID")){
                            contenido+=" Video,";
                        }


                    }
                    if(contenido.endsWith(",")){
                        contenido=contenido.substring(0,contenido.length() - 1);
                    }
                    String ruta="\nAlmacenada en: "+f.getPath()+"/"+nombre+"/";
                    String titulo="Sesión del "+nuevoDateFormat+contenido+ruta;
                    item.add(titulo );
                }catch (ParseException ex)
                {
                    Log.e(TAG,"Exception "+ex);
                }


            }

        }

        flechaAdapter sesiones = new flechaAdapter(this,item);
        ListView sesionesListView = (ListView) findViewById(R.id.listSesiones);
        sesionesListView.setAdapter(sesiones);
        sesionesListView.setOnItemClickListener(sesionClickListener);

    }
    private AdapterView.OnItemClickListener sesionClickListener
            = new AdapterView.OnItemClickListener() {
        public void onItemClick(AdapterView<?> av, View v, int arg2, long arg3) {
            Intent intent = new Intent (v.getContext(), Reproduccion.class);
            intent.putExtra("Directorio", files[arg2]);
            startActivity(intent);
        }
    };

}
