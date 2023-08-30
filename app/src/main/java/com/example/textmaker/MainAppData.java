package com.example.textmaker;

import android.content.Context;
import android.content.SharedPreferences;

import com.google.gson.JsonObject;
import org.json.JSONArray;
import org.json.JSONException;

import java.util.ArrayList;
import java.util.List;

public class MainAppData
{
    public static final int REQUEST_CAMERA_PERMISSION_CODE = 100;
    public static final int REQUEST_GALLERY_PERMISSION_CODE = 101;

    //Use it for current user log in/sign up
    public static final String emailRegular =
            "^[_A-Za-z0-9]+@[A-Za-z0-9]+(\\.[A-Za-z0-9]+)*(\\.[A-Za-z]{2,})$";
    public static final String passwordRegular =
            "^(?=.*[0-9])(?=.*[a-z])(?=.*[A-Z])(?=.*[@#$%^&+=!_])(?=\\S+$).{8,}$";
    public static final String phoneRegular =
            "^(\\+[0-9]{1,3})?\\s*([0-9]{2,3}\\s*){4}$";
    public static final String nameRegular = "^[a-zA-Z]{3,12}$";

    //Use it for set current OCR config
    public static final String documentsOrBooksDetections = "DOCUMENT_TEXT_DETECTION";
    public static final String notesOrLabelsDetection = "TEXT_DETECTION";
    public static final String[] langArr = {"En", "Ru", "Uk"};
    public static String documentTypeForRecognizing = documentsOrBooksDetections;
    public static String lang;

    //OCR text object
    public static JsonObject annotation;

    //Text results list data
    public static List<String> dataList;
    public static String firstStringSymbolsOutput(String str)
    {
        final int size = 50;
        int symbolId;
        String subStr;

        if(str.length() > size){
            subStr = str.substring(0, size);
            symbolId = subStr.lastIndexOf(' ');
            if(symbolId != -1)
                subStr = subStr.substring(0, symbolId);
            return subStr + " ...";
        }
        else
            return str;
    }
    public static final String textToEditTag = "TEXT_TO_EDIT";

    //Save list data to SharedPreferences
    private SharedPreferences sharedPreferences;
    private final Context context;

    private final String textListTag = "LIST_SHARED_PREFS";
    private final String sharedPrefsTag = "SHARED_PREFS";

    public void saveListData()
    {
        sharedPreferences = context.getSharedPreferences(sharedPrefsTag, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        JSONArray jsonArray = new JSONArray(dataList);
        String jsonString = jsonArray.toString();
        editor.putString(textListTag, jsonString);
        editor.apply();
    }
    public void loadListData() throws JSONException {
        sharedPreferences = context.getSharedPreferences(sharedPrefsTag, Context.MODE_PRIVATE);
        String jsonString = sharedPreferences.getString(textListTag, "");
        JSONArray jsonArray = new JSONArray(jsonString);
        dataList = new ArrayList<>();
        for (int i = 0; i < jsonArray.length(); i++)
            dataList.add(jsonArray.getString(i));
    }

    public MainAppData(Context context) {
        this.context = context;
    }
}