package com.example.textmaker;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.os.Bundle;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;

import com.example.textmaker.databinding.RegisterActivityBinding;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.Objects;
import java.util.regex.Pattern;

public class RegisterActivity extends AppCompatActivity
{
    private RegisterActivityBinding binding;

    private FirebaseAuth firebaseAuth;
    private FirebaseFirestore firebaseFirestore;

    private String nameData, phoneData, emailData, passwordData;

    @SuppressLint("SourceLockedOrientationActivity")
    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        binding = RegisterActivityBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);

        firebaseAuth = FirebaseAuth.getInstance();
        firebaseFirestore = FirebaseFirestore.getInstance();
    }

    @Override
    protected void onStart()
    {
        super.onStart();

        binding.login.setOnClickListener(view -> onBackPressed());
        binding.register.setOnClickListener(view -> {
            try {
                nameData = binding.name.getText().toString();
                phoneData = binding.phone.getText().toString();
                emailData = binding.email.getText().toString().trim();
                passwordData = binding.password.getText().toString().trim();
                if(currentUserData(nameData, phoneData, emailData, passwordData)) {
                    firebaseAuth.createUserWithEmailAndPassword(emailData, passwordData).
                            addOnSuccessListener(authResult -> {
                                firebaseFirestore.collection("Users")
                                        .document(Objects.requireNonNull(FirebaseAuth.getInstance().getUid()))
                                        .set(new UsersData(nameData, phoneData, emailData, passwordData));
                                Intent intent = new Intent(this, CropperActivity.class);
                                startActivity(intent);
                            }).addOnFailureListener(e ->
                                    Toast.makeText(
                                            this,
                                            e.getMessage(),
                                            Toast.LENGTH_SHORT).show()
                            );
                }

            } catch (IllegalArgumentException ignored) {
            }
        });
    }

    private boolean currentUserData(String name, String phone, String email, String password)
    {
        if (!Pattern.compile(MainAppData.nameRegular).matcher(name).matches()) {
            Toast.makeText(this, R.string.invalid_name, Toast.LENGTH_SHORT).show();
            return false;
        }
        else if (!Pattern.compile(MainAppData.phoneRegular).matcher(phone).matches()) {
            Toast.makeText(this, R.string.invalid_phone, Toast.LENGTH_SHORT).show();
            return false;
        }
        else if (!Pattern.compile(MainAppData.emailRegular).matcher(email).matches()) {
            Toast.makeText(this, R.string.invalid_email, Toast.LENGTH_SHORT).show();
            return false;
        }
        else if (! Pattern.compile(MainAppData.passwordRegular).matcher(password).matches()) {
            Toast.makeText(this, R.string.invalid_password, Toast.LENGTH_SHORT).show();
            return false;
        }
        return true;
    }
}