package com.fgtit.reader.service;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.util.Log;

import androidx.preference.PreferenceManager;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.StringJoiner;

public class ApiService extends AsyncTask<String,Void,String> {

    final static String TAG = "ApiService";
    final static String BASE_URL = "http://clocking.edu-portal.live/api/";

    public final static String GET_USER_DATA = "get_user_data";
    public final static String POST_USER_AUTH = "post_user_auth";

    public AsyncResponse delegate = null;

    private String result;

    private Context context;

    public ApiService(Context applicationContext, AsyncResponse delegate) {
        this.context = applicationContext;
        this.delegate = delegate;
    }

    @Override
    protected String doInBackground(String... params) {
        switch (params[0]) {
            case GET_USER_DATA:
                result = retrieveUserData(params[1]);
                break;
            case POST_USER_AUTH:
                result = postUserAuth(params[1], params[2]);
                break;
            default:
                break;
        }
        return result;
    }

    @Override
    protected void onPostExecute(String result) {
        this.delegate.processFinish(result);
        super.onPostExecute(result);
    }

    protected HttpURLConnection getHttpURLConnection(String url, String method) throws IOException {
        URL urlObj = new URL(url);
        HttpURLConnection httpURLConnection = (HttpURLConnection) urlObj.openConnection();
        httpURLConnection.setRequestMethod(method);
        httpURLConnection.setDoOutput(true);
        httpURLConnection.setRequestProperty("Accept", "application/json");
        httpURLConnection.setRequestProperty("Content-Type", "application/json; utf-8");
        return httpURLConnection;
    }

    protected String getResponse(HttpURLConnection httpURLConnection) throws IOException, JSONException {
        InputStream inputStream = httpURLConnection.getInputStream();
        BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(inputStream));
        StringBuilder response = new StringBuilder();
        String responseLine = null;
        while ((responseLine = bufferedReader.readLine()) != null) {
            response.append(responseLine.trim());
        }
        Log.d(TAG, "RESPONSE: " + response.toString());

        return response.toString();
    }

    protected String retrieveUserData(String username){
        Log.e(TAG, "retrieveUserData: " + username);
        try {
            SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(this.context);
            HttpURLConnection httpURLConnection = getHttpURLConnection(preferences.getString("base_url", BASE_URL) + "register", "POST");

            String jsonInputString = "{\"id\": \""+username+"\"}";

            OutputStream os = httpURLConnection.getOutputStream();
            byte[] input = jsonInputString.getBytes("utf-8");
            os.write(input, 0, input.length);

            String response = getResponse(httpURLConnection);

            final JSONObject jsonObject = new JSONObject(response);
            final JSONObject data = jsonObject.getJSONObject("data");
            Log.d(TAG, "RESPONSE DATA: " + data.toString());
            return data.toString();

            //return response.toString();
        } catch (IOException | JSONException e) {
            e.printStackTrace();
            Log.e(TAG, "Error - RetrieveUserData: " + e.getMessage());
        }
        return null;
    }

    protected String postUserAuth(String username, String mode){
        Log.e(TAG, "postUserAuth: " + username);
        try {
            HttpURLConnection httpURLConnection = getHttpURLConnection(BASE_URL + "clockin", "POST");

            String jsonInputString = mode == null ? "{\"id\": \""+username+"\"}" : "{\"id\": \""+username+"\", \"mode\": \""+mode+"\"}";
            Log.d(TAG, "JSON VALUES: " + jsonInputString);
            OutputStream os = httpURLConnection.getOutputStream();
            byte[] input = jsonInputString.getBytes("utf-8");
            os.write(input, 0, input.length);

            return getResponse(httpURLConnection);

//            final JSONObject jsonObject = new JSONObject(response);
//            final JSONObject data = jsonObject.getJSONObject("data");
//            Log.d(TAG, "RESPONSE DATA: " + data.toString());
//            return data.toString();

            //return response.toString();
        } catch (IOException | JSONException e) {
            e.printStackTrace();
            Log.e(TAG, "Error - RetrieveUserData: " + e.getMessage());
        }
        return null;
    }

    public static String parseJSON(String data, String key) throws JSONException {
        final JSONObject jsonObject = new JSONObject(data);
        return jsonObject.getString(key);
    }
}
