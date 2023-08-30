package com.example.textmaker;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.Dialog;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.util.Base64;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.ImageView;
import android.widget.Toast;

import com.example.textmaker.databinding.CropperActivityBinding;
import com.google.firebase.auth.FirebaseAuth;

import androidx.activity.result.ActivityResult;
import androidx.activity.result.ActivityResultCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;

import com.google.android.gms.tasks.Task;
import com.google.firebase.functions.FirebaseFunctions;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonPrimitive;

import org.json.JSONException;

import java.io.*;
import java.util.ArrayList;

public class CropperActivity extends AppCompatActivity
{
    private CropperActivityBinding binding;
    private FirebaseFunctions mFunctions;
    private Dialog dialog, instructionDialog;
    private Animation increase, decrease;
    private ImageView close;

    private Uri imageUri;
    private final MainAppData data = new MainAppData(this);

    @SuppressLint({"SourceLockedOrientationActivity", "SetTextI18n"})
    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        binding = CropperActivityBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);

        loadData();
        createWaitDialog();
        initViews();
    }

    @Override
    protected void onStart()
    {
        super.onStart();

        binding.exit.setOnClickListener(view -> onBackPressed());
        binding.camera.setOnClickListener(view -> {
            try {
                dispatchCaptureImageIntent();
            } catch (IOException ignored) {
            }
        });
        binding.gallery.setOnClickListener(view -> dispatchSelectImageIntent());
        binding.documents.setOnClickListener(view -> {
            if(!MainAppData.documentTypeForRecognizing.equals(MainAppData.documentsOrBooksDetections)) {
                MainAppData.documentTypeForRecognizing = MainAppData.documentsOrBooksDetections;
                binding.documents.startAnimation(decrease);
                binding.notes.startAnimation(increase);
                Toast.makeText(
                        this,
                        R.string.document_mode,
                        Toast.LENGTH_SHORT).show();
            }
        });
        binding.notes.setOnClickListener(view -> {
            if(!MainAppData.documentTypeForRecognizing.equals(MainAppData.notesOrLabelsDetection)){
                MainAppData.documentTypeForRecognizing = MainAppData.notesOrLabelsDetection;
                binding.documents.startAnimation(increase);
                binding.notes.startAnimation(decrease);
                Toast.makeText(
                        this,
                        R.string.notes_mode,
                        Toast.LENGTH_SHORT).show();
            }
        });
        binding.instruction.setOnClickListener(view -> openInfoDialog());
        binding.list.setOnClickListener(view -> {
            Intent intent = new Intent(this, ListActivity.class);
            startActivity(intent);
        });
    }

    @Override
    protected void onPause()
    {
        super.onPause();
        data.saveListData();
    }

    @Override
    public void onBackPressed()
    {
        super.onBackPressed();
        FirebaseAuth.getInstance().signOut();
    }

    private void dispatchCaptureImageIntent() throws IOException
    {
        if (checkSelfPermission(Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
            File storageDirectory = getExternalFilesDir(Environment.DIRECTORY_PICTURES);
            File imageFile = File.createTempFile("image", ".jpg", storageDirectory);
            imageUri = FileProvider.getUriForFile(this,
                    getPackageName() + ".file-provider", imageFile);

            Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
            intent.putExtra(MediaStore.EXTRA_OUTPUT, imageUri);
            cameraActivityLauncher.launch(intent);
        }
        else
            requestPermissions(new String[]{
                    Manifest.permission.CAMERA},
                    MainAppData.REQUEST_CAMERA_PERMISSION_CODE
            );
    }

    private boolean getCurrentVersionPermission()
    {
        String permission;
        if (Build.VERSION.SDK_INT >= 33)
            permission = Manifest.permission.READ_MEDIA_IMAGES;
        else
            permission = Manifest.permission.READ_EXTERNAL_STORAGE;

        if (ContextCompat.checkSelfPermission(
                this, permission) == PackageManager.PERMISSION_GRANTED)
            return true;
        else {
            requestPermissions(new String[]{permission},
                    MainAppData.REQUEST_GALLERY_PERMISSION_CODE);
            return false;
        }
    }

    private void dispatchSelectImageIntent()
    {
        if (getCurrentVersionPermission()) {
            Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
            galleryActivityLauncher.launch(intent);
        }
    }

    private final ActivityResultLauncher<Intent> cameraActivityLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(), result -> {
                if(result.getResultCode() == Activity.RESULT_OK)
                    createBitmapFromUri();
            }
    );

    private final ActivityResultLauncher<Intent> galleryActivityLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(), new ActivityResultCallback<ActivityResult>()
            {
                @Override
                public void onActivityResult(ActivityResult result)
                {
                    if(result.getResultCode() == Activity.RESULT_OK)
                    {
                        Intent data = result.getData();
                        if(data != null) {
                            imageUri = data.getData();
                            createBitmapFromUri();
                        }
                    }
                }
            }
    );

    private void createBitmapFromUri()
    {
        if(imageUri != null)
        {
            try {
                Bitmap bitmap = MediaStore.Images.Media.getBitmap(getContentResolver(), imageUri);
                getTextFromImage(bitmap);
            } catch (IOException ignored) {
            }
        }
        else
            Toast.makeText(
                    this,
                    R.string.work_with_image,
                    Toast.LENGTH_LONG).show();
    }

    private String bitmapToString(Bitmap bitmap)
    {
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.JPEG, 100, byteArrayOutputStream);
        byte[] imageBytes = byteArrayOutputStream.toByteArray();
        return Base64.encodeToString(imageBytes, Base64.NO_WRAP);
    }

    private void getTextFromImage(Bitmap bitmap)
    {
        dialog.show();

        String base64encoded = bitmapToString(bitmap);
        mFunctions = FirebaseFunctions.getInstance();

        JsonObject request = new JsonObject();
        JsonObject image = new JsonObject();
        image.add("content", new JsonPrimitive(base64encoded));
        request.add("image", image);
        JsonObject feature = new JsonObject();
        feature.add("type", new JsonPrimitive(MainAppData.documentTypeForRecognizing));
        JsonArray features = new JsonArray();
        features.add(feature);
        request.add("features", features);

        JsonObject imageContext = new JsonObject();
        JsonArray languageHints = new JsonArray();
        languageHints.add(getRequestLanguage());
        imageContext.add("languageHints", languageHints);
        request.add("imageContext", imageContext);

        annotateImage(request.toString())
                .addOnCompleteListener(task -> {
                    if (!task.isSuccessful())
                        Toast.makeText(
                                this,
                                R.string.went_wrong,
                                Toast.LENGTH_SHORT).show();
                    else {
                        try {
                            MainAppData.annotation =
                                    task.getResult()
                                            .getAsJsonArray()
                                            .get(0)
                                            .getAsJsonObject()
                                            .get("fullTextAnnotation")
                                            .getAsJsonObject();
                            Intent intent = new Intent(this, TextWorkSpaceActivity.class);
                            startActivity(intent);
                        } catch (NullPointerException ignored) {
                            Toast.makeText(
                                    this,
                                    R.string.no_text,
                                    Toast.LENGTH_SHORT).show();
                        }
                    }
                    dialog.cancel();
                });
    }

    private Task<JsonElement> annotateImage(String requestJson) {
        return mFunctions
                .getHttpsCallable("annotateImage")
                .call(requestJson)
                .continueWith(task -> JsonParser.parseString(new Gson().toJson(task.getResult().getData())));
    }

    private void openInfoDialog()
    {
        instructionDialog.show();
        close.setOnClickListener(view -> instructionDialog.cancel());
    }

    private String getRequestLanguage()
    {
        MainAppData.lang = MainAppData.langArr[binding.langPicker.getValue()];
        return MainAppData.lang.toLowerCase();
    }

    private void initViews()
    {
        increase = AnimationUtils.loadAnimation(this, R.anim.block_size_increase);
        decrease = AnimationUtils.loadAnimation(this, R.anim.block_size_decrease);

        binding.langPicker.setMinValue(0);
        binding.langPicker.setMaxValue(MainAppData.langArr.length - 1);
        binding.langPicker.setDisplayedValues(MainAppData.langArr);
    }

    private void createWaitDialog()
    {
        dialog = new Dialog(this);
        dialog.setContentView(R.layout.wait_dialog);
        dialog.setCancelable(false);
        dialog.setCanceledOnTouchOutside(false);
        dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));

        instructionDialog = new Dialog(this);
        instructionDialog.setContentView(R.layout.instruction_dialog);
        instructionDialog.setCancelable(false);
        instructionDialog.setCanceledOnTouchOutside(false);
        instructionDialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));

        close = instructionDialog.findViewById(R.id.close);
    }

    private void loadData()
    {
        try {
            data.loadListData();
        } catch (JSONException ignored) {
            MainAppData.dataList = new ArrayList<>();
        }
    }
}