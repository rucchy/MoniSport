package com.example.chema.monisport.utils;

import android.widget.BaseAdapter;
import android.content.Context;
import android.view.LayoutInflater;
import android.widget.TextView;
import android.app.Activity;
import java.util.List;

import android.view.View;
import android.view.ViewGroup;

import com.example.chema.monisport.R;

/**
 * Created by Chema on 16/10/2017.
 */

public class flechaAdapter extends BaseAdapter {

    private Activity activity;
    private List<String> data;
    private static LayoutInflater inflater=null;

    public flechaAdapter(Activity a,List<String> d) {
        activity=a;
        data=d;
        inflater = (LayoutInflater)activity.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
    }
    public int getCount() {
        return data.size();
    }

    public Object getItem(int position) {
        return position;
    }

    public long getItemId(int position) {
        return position;
    }
    public View getView(int position, View convertView, ViewGroup parent) {
        View vi=convertView;
        if(convertView==null)
            vi = inflater.inflate(R.layout.sesiones_name, null);

        TextView title = (TextView)vi.findViewById(R.id.nombre_sesion); // title

        String dato = data.get(position);
        // Setting all values in listview
        title.setText(dato);
        return vi;
    }
}
