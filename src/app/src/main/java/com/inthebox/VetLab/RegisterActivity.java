package com.inthebox.VetLab;

import android.app.ProgressDialog;
import android.content.Intent;
import android.net.Uri;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ProgressBar;
import android.widget.Toast;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.soundcloud.android.crop.Crop;

import java.io.File;

public class RegisterActivity extends AppCompatActivity {

    private final int GALLERY_REQUEST = 1;

    private Uri mImageUri = null;

    // Interface references
    private ImageButton mProfilePic;
    private EditText mFirstNameField;
    private EditText mLastNameField;
    private EditText mEmailField;
    private EditText mPasswordField;
    private EditText mPhoneField;
    private Button mRegisterBtn;

    // Firebase references
    private FirebaseAuth mAuth;
    private DatabaseReference mDatabase;

    private ProgressDialog mProgress;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);

        mProgress = new ProgressDialog(this);

        // Setting up the inteface
        mProfilePic = (ImageButton) findViewById(R.id.registerProfilePic);
        mFirstNameField = (EditText) findViewById(R.id.registerFirstName);
        mLastNameField  = (EditText) findViewById(R.id.registerLastName);
        mEmailField     = (EditText) findViewById(R.id.registerEmail);
        mPasswordField  = (EditText) findViewById(R.id.registerPassword);
        mPhoneField     = (EditText) findViewById(R.id.registerPhoneNumber);
        mRegisterBtn    = (Button) findViewById(R.id.registerBtn);

        // Setting up firebase references
        mAuth = FirebaseAuth.getInstance();
        mDatabase = FirebaseDatabase.getInstance().getReference().child("Users");

        mProfilePic.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent galleryIntent = new Intent(Intent.ACTION_GET_CONTENT);
                galleryIntent.setType("image/*");
                startActivityForResult(galleryIntent, GALLERY_REQUEST);
            }
        });

        mRegisterBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                startRegister();
            }
        });
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == GALLERY_REQUEST && resultCode == RESULT_OK) {
            Uri imageUri = data.getData();
            beginCrop(imageUri);
        }

        if (requestCode == Crop.REQUEST_CROP) {
            handleCrop(resultCode, data);
        }
    }

    private void startRegister() {
        final String first_name = mFirstNameField.getText().toString().trim();
        final String last_name = mLastNameField.getText().toString().trim();
        final String email    = mEmailField.getText().toString().trim();
        final String password = mPasswordField.getText().toString().trim();
        final String phone_number = mPhoneField.getText().toString().trim();

        if (!first_name.isEmpty() && !last_name.isEmpty() && !email.isEmpty() && !password.isEmpty() && !phone_number.isEmpty() && mImageUri != null) {
            if (password.length() >= 6) {
                mProgress.setMessage("Registering...");
                mProgress.show();

                mAuth.createUserWithEmailAndPassword(email, password).addOnCompleteListener(new OnCompleteListener<AuthResult>() {
                    @Override
                    public void onComplete(@NonNull Task<AuthResult> task) {
                        if (task.isSuccessful()) {
                            String user_id = mAuth.getCurrentUser().getUid();

                            DatabaseReference current_user_db = mDatabase.child(user_id);

                            current_user_db.child("first_name").setValue(first_name);
                            current_user_db.child("last_name").setValue(last_name);
                            current_user_db.child("email").setValue(email);
                            current_user_db.child("phone_number").setValue(phone_number);
                            current_user_db.child("profile_pic").setValue(mImageUri.toString());

                            Intent mainIntent = new Intent(RegisterActivity.this, MainActivity.class);
                            mainIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                            startActivity(mainIntent);

                            mProgress.dismiss();
                        }
                    }
                });
            } else {
                Toast.makeText(RegisterActivity.this, "Password needs to be at least 6 characters long", Toast.LENGTH_SHORT).show();
            }
        } else {
            Toast.makeText(this, "You need to fill in all the fields", Toast.LENGTH_SHORT).show();
            mProgress.dismiss();
        }
    }

    private void beginCrop(Uri source) {
        mImageUri = Uri.fromFile(new File(getCacheDir(), "cropped"));
        Crop.of(source, mImageUri).asSquare().start(this);
    }

    private void handleCrop(int resultCode, Intent result) {
        if (resultCode == RESULT_OK) {
            mProfilePic.setImageURI(Crop.getOutput(result));
        } else if (resultCode == Crop.RESULT_ERROR) {
            Toast.makeText(this, Crop.getError(result).getMessage(), Toast.LENGTH_SHORT).show();
        }
    }
}
