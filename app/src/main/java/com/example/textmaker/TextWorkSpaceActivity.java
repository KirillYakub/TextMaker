package com.example.textmaker;

import android.annotation.SuppressLint;
import android.app.Dialog;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.textmaker.databinding.TextWorkspaceActivityBinding;
import com.google.gson.JsonElement;

public class TextWorkSpaceActivity extends AppCompatActivity
{
    private final MainAppData data = new MainAppData(this);
    private TextWorkspaceActivityBinding binding;
    private ImageView close;

    private Dialog dialog;
    private String text;

    @SuppressLint("SourceLockedOrientationActivity")
    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        binding = TextWorkspaceActivityBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);

        text = getIntent().getStringExtra(MainAppData.textToEditTag);
        if(text == null) {
            initDialog();
            printPhotoText();
        }
        else
            binding.imageText.setText(text);
    }

    @Override
    protected void onStart()
    {
        super.onStart();

        binding.back.setOnClickListener(view -> onBackPressed());
        binding.copy.setOnClickListener(view -> saveDataToClipboard());
        binding.save.setOnClickListener(view -> {
            MainAppData.dataList.add(binding.imageText.getText().toString());
            Toast.makeText(this,
                    R.string.text_saved,
                    Toast.LENGTH_SHORT).show();
        });
    }

    @Override
    protected void onPause()
    {
        super.onPause();
        data.saveListData();
    }

    private void printPhotoText()
    {
        StringBuilder imageText = new StringBuilder();
        if(MainAppData.documentTypeForRecognizing.equals(MainAppData.documentsOrBooksDetections)) {
            for (JsonElement page : MainAppData.annotation.get("pages").getAsJsonArray()) {
                for (JsonElement block : page.getAsJsonObject().get("blocks").getAsJsonArray()) {
                    StringBuilder blockText = new StringBuilder();
                    for (JsonElement para : block.getAsJsonObject().get("paragraphs").getAsJsonArray()) {
                        for (JsonElement word : para.getAsJsonObject().get("words").getAsJsonArray()) {
                            StringBuilder wordText = new StringBuilder();
                            for (JsonElement symbol : word.getAsJsonObject().get("symbols").getAsJsonArray())
                                wordText.append(symbol.getAsJsonObject().get("text").getAsString());
                            blockText.append(wordText).append(" ");
                        }
                    }
                    imageText.append(blockText).append("\n\n");
                }
            }
        }
        else
            imageText.append(MainAppData.annotation.get("text").getAsString());

        text = imageText.toString();
        if(!MainAppData.lang.equals(MainAppData.langArr[0]))
            text = text.replaceAll("[a-zA-Z]", "");
        else
            text = text.replaceAll("[^\\p{ASCII}]", "");

        if(!isSymbolsMoreThanLetters()) {
            text = text.trim();
            text = text.replaceAll("\\s+", " ");
            binding.imageText.setText(text);
        }
        else
            openDialog();

        Toast.makeText(
                this,
                getString(R.string.language) + MainAppData.lang,
                Toast.LENGTH_LONG).show();
    }

    private void saveDataToClipboard()
    {
        String textToCopy = binding.imageText.getText().toString();
        ClipboardManager clipboard = (ClipboardManager) getSystemService(CLIPBOARD_SERVICE);
        ClipData clip = ClipData.newPlainText("Text from image", textToCopy);
        clipboard.setPrimaryClip(clip);

        Toast.makeText(this, R.string.text_copied, Toast.LENGTH_SHORT).show();
    }

    private boolean isSymbolsMoreThanLetters()
    {
        int letterCount = 0, digitCount = 0, punctuationCount = 0;
        for (int i = 0; i < text.length(); i++) {
            char strChar = text.charAt(i);
            if (Character.isLetter(strChar))
                letterCount++;
            else if (Character.isDigit(strChar))
                digitCount++;
            else {
                if(!Character.isWhitespace(strChar))
                    punctuationCount++;
            }
        }
        return punctuationCount + digitCount >= letterCount;
    }

    private void initDialog()
    {
        dialog = new Dialog(this);
        dialog.setContentView(R.layout.text_error_dialog);
        dialog.setCancelable(false);
        dialog.setCanceledOnTouchOutside(false);
        dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));

        close = dialog.findViewById(R.id.close);
    }

    private void openDialog()
    {
        close.setOnClickListener(view -> {
            dialog.cancel();
            onBackPressed();
        });
        dialog.show();
    }
}