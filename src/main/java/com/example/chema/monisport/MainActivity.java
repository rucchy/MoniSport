package com.example.chema.monisport;

import android.app.Activity;
import android.app.Dialog;
import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.view.Window;
import android.widget.Button;
import android.util.Log;
import android.widget.EditText;
import android.widget.Toast;

public class MainActivity extends AppCompatActivity{

    /**
     * Tag for Log
     */
    private static final String TAG = "ActividadPrincipal";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        final Dialog myDialog = new Dialog(this);
        myDialog.setContentView(R.layout.popup_user);
        myDialog.setCancelable(true);
        myDialog.setTitle(R.string.msg_datos);
        Button guardar = (Button) myDialog.findViewById(R.id.btn_guardar);
        Button volver = (Button) myDialog.findViewById(R.id.btn_volver);

        final EditText nombre = (EditText) myDialog.findViewById(R.id.nombre_intro);;
        final EditText edad = (EditText) myDialog.findViewById(R.id.edad_intro);
        final EditText altura = (EditText) myDialog.findViewById(R.id.altura_intro);
        final EditText peso = (EditText) myDialog.findViewById(R.id.peso_intro);

        guardar.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View v)
            {
                String nombre_intro=nombre.getText().toString().trim();
                String edad_intro=edad.getText().toString().trim();
                String altura_intro=altura.getText().toString().trim();
                String peso_intro=peso.getText().toString().trim();
                if(!nombre_intro.equals("")&&!edad_intro.equals("")&&!altura_intro.equals("")&&!peso_intro.equals("")) {
                    Bundle b=new Bundle();
                    b.putStringArray("datos_deportista", new String[]{nombre_intro, edad_intro,altura_intro,peso_intro});
                    Intent intent = new Intent(v.getContext(), ConexionBluetooth.class);
                    intent.putExtras(b);
                    startActivityForResult(intent, 0);
                    myDialog.hide();
                }else{
                    Toast.makeText(getApplicationContext(), "Debes introducir los datos del deportista", Toast.LENGTH_LONG).show();
                }
            }
        });

        volver.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View v)
            {
                myDialog.hide();
            }
        });

        Button btn1 = (Button) findViewById(R.id.button_ir_bt);
        btn1.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) {
                nombre.setText("");
                edad.setText("");
                altura.setText("");
                peso.setText("");
                myDialog.show();
            }
        });

        Button btn2 = (Button) findViewById(R.id.button_ir_sesiones);
        btn2.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) {
                Intent intent = new Intent (v.getContext(), ListadoSesiones.class);
                startActivityForResult(intent, 0);
            }
        });

    }

    protected void onActivityResult(int requestCode, int resultCode,
                                    Intent data) {
    }
    @Override
    public void onDestroy(){
        super.onDestroy();
    }


}
