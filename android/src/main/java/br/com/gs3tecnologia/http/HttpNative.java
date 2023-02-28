package br.com.gs3tecnologia.http;

import android.util.Log;

public class HttpNative {

    public String echo(String value) {
        Log.i("Echo", value);
        return value;
    }
}
