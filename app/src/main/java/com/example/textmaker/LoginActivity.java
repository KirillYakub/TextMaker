package com.example.textmaker;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.Dialog;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.IntentSenderRequest;

import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;

import com.example.textmaker.databinding.LoginActivityBinding;
import com.google.android.gms.auth.api.identity.BeginSignInRequest;
import com.google.android.gms.auth.api.identity.Identity;
import com.google.android.gms.auth.api.identity.SignInClient;
import com.google.android.gms.auth.api.identity.SignInCredential;
import com.google.android.gms.common.api.ApiException;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.GoogleAuthProvider;

import java.util.regex.Pattern;

public class LoginActivity extends AppCompatActivity
{
    private LoginActivityBinding binding;

    private FirebaseAuth mAuth;
    private SignInClient oneTapClient;
    private BeginSignInRequest signUpRequest;
    private Dialog dialog;

    private String emailData, passwordData;

    @SuppressLint("SourceLockedOrientationActivity")
    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        binding = LoginActivityBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);

        if(isInternetOn()) {
            mAuth = FirebaseAuth.getInstance();
            FirebaseUser user = mAuth.getCurrentUser();
            if (user != null)
                signInSuccessfully();
            googleInputIdentify();
        }
        else
            showErrorDialog();
    }

    @Override
    protected void onStart()
    {
        super.onStart();

        ActivityResultLauncher<IntentSenderRequest>
                activityResultLauncher = registerForActivityResult(new ActivityResultContracts.StartIntentSenderForResult(),
                result -> {
                    if(result.getResultCode() == Activity.RESULT_OK) {
                        try {
                            SignInCredential credential =
                                    oneTapClient.getSignInCredentialFromIntent(result.getData());
                            String idToken = credential.getGoogleIdToken();
                            if (idToken != null) {
                                AuthCredential firebaseCredential =
                                        GoogleAuthProvider.getCredential(idToken, null);
                                mAuth.signInWithCredential(firebaseCredential)
                                        .addOnCompleteListener(this, task -> {
                                            if (task.isSuccessful()) {
                                                FirebaseUser user = mAuth.getCurrentUser();
                                                if (user != null)
                                                    signInSuccessfully();
                                                else
                                                    Toast.makeText(this,
                                                            R.string.user_not_found,
                                                            Toast.LENGTH_SHORT).show();
                                            }
                                            else
                                                Toast.makeText(
                                                        this,
                                                        R.string.firebase_error,
                                                        Toast.LENGTH_SHORT).show();
                                        });
                            }
                        } catch (ApiException ignored) {
                            Toast.makeText(
                                    this,
                                    R.string.google_error,
                                    Toast.LENGTH_SHORT).show();
                        }
                    }
                });
        binding.google.setOnClickListener(
                view -> oneTapClient.beginSignIn(signUpRequest)
                        .addOnSuccessListener(this, result -> {
                            IntentSenderRequest intentSenderRequest =
                                    new IntentSenderRequest.Builder(
                                            result.getPendingIntent().getIntentSender()).build();
                            activityResultLauncher.launch(intentSenderRequest);
                        })
                        .addOnFailureListener(this, e ->
                                Toast.makeText(
                                        this,
                                        e.getLocalizedMessage(),
                                        Toast.LENGTH_SHORT).show()
                        )
        );
        binding.login.setOnClickListener(view -> {
            try {
                emailData = binding.email.getText().toString().trim();
                passwordData = binding.password.getText().toString().trim();
                if (currentUserData(emailData, passwordData)) {
                    mAuth.signInWithEmailAndPassword(emailData, passwordData)
                            .addOnSuccessListener(authResult -> {
                                FirebaseUser user = mAuth.getCurrentUser();
                                if (user != null)
                                    signInSuccessfully();
                                else
                                    Toast.makeText(
                                            this,
                                            R.string.user_not_found,
                                            Toast.LENGTH_SHORT).show();
                            })
                            .addOnFailureListener(e ->
                                    Toast.makeText(
                                            this,
                                            e.getMessage(),
                                            Toast.LENGTH_SHORT).show()
                            );
                }

            } catch (IllegalArgumentException ignored){
            }
        });
        binding.register.setOnClickListener(view -> {
            Intent intent = new Intent(this, RegisterActivity.class);
            startActivity(intent);
        });
        binding.resetPassword.setOnClickListener(view -> {
            try {
                emailData = binding.email.getText().toString().trim();
                if(Pattern.compile(MainAppData.emailRegular).matcher(emailData).matches()) {
                    mAuth.sendPasswordResetEmail(emailData)
                            .addOnSuccessListener(e ->
                                    Toast.makeText(
                                            this,
                                            R.string.email_sent,
                                            Toast.LENGTH_SHORT).show())
                            .addOnFailureListener(e ->
                                    Toast.makeText(
                                            this,
                                            e.getMessage(),
                                            Toast.LENGTH_SHORT).show()
                            );
                }
                else
                    Toast.makeText(
                            this,
                            R.string.invalid_email,
                            Toast.LENGTH_SHORT).show();

            }catch (IllegalArgumentException ignored) {
            }
        });
    }

    private boolean isInternetOn()
    {
        ConnectivityManager connectivityManager =
                (ConnectivityManager) getSystemService(CONNECTIVITY_SERVICE);
        NetworkInfo activeNetworkInfo = connectivityManager.getActiveNetworkInfo();
        return activeNetworkInfo != null && activeNetworkInfo.isConnectedOrConnecting();
    }

    private boolean currentUserData(String email, String password)
    {
        if (!Pattern.compile(MainAppData.emailRegular).matcher(email).matches()) {
            Toast.makeText(this, R.string.invalid_email, Toast.LENGTH_SHORT).show();
            return false;
        }
        else if (!Pattern.compile(MainAppData.passwordRegular).matcher(password).matches()) {
            Toast.makeText(this, R.string.invalid_password, Toast.LENGTH_SHORT).show();
            return false;
        }
        return true;
    }

    private void signInSuccessfully()
    {
        Intent intent = new Intent(this, CropperActivity.class);
        startActivity(intent);
    }

    private void googleInputIdentify()
    {
        oneTapClient = Identity.getSignInClient(this);
        signUpRequest = BeginSignInRequest.builder()
                .setGoogleIdTokenRequestOptions(BeginSignInRequest.GoogleIdTokenRequestOptions.builder()
                        .setSupported(true)
                        .setServerClientId(getString(R.string.web_api_key))
                        .setFilterByAuthorizedAccounts(false)
                        .build())
                .build();
    }

    private void showErrorDialog()
    {
        dialog = new Dialog(this);
        dialog.setContentView(R.layout.error_dialog);
        dialog.setCancelable(false);
        dialog.setCanceledOnTouchOutside(false);
        dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));

        TextView close_app = dialog.findViewById(R.id.close_app);
        close_app.setOnClickListener(view -> {
            dialog.dismiss();
            finish();
        });
        dialog.show();
    }
}