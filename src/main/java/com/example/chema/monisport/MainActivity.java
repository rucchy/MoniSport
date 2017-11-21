package com.example.chema.monisport;

import android.app.Activity;
import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.view.Window;
import android.widget.Button;
import android.util.Log;

public class MainActivity extends AppCompatActivity{

    /**
     * Tag for Log
     */
    private static final String TAG = "ActividadPrincipal";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);


        Button btn1 = (Button) findViewById(R.id.button_ir_bt);
        btn1.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) {
                Intent intent = new Intent (v.getContext(), ConexionBluetooth.class);
                startActivityForResult(intent, 0);
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
