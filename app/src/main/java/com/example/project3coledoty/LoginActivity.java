package com.example.project3coledoty;

import androidx.activity.result.ActivityResult;
import androidx.activity.result.ActivityResultCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.developer.gbuttons.GoogleSignInButton;
import com.example.project3coledoty.database.UserDao;
import com.example.project3coledoty.database.WeightTrackerDatabase;
import com.example.project3coledoty.model.User;
import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.tasks.Task;

import java.util.List;

public class LoginActivity extends AppCompatActivity {

    private WeightTrackerDatabase mWeightTrackerDb;
    private UserDao mUserDao;
    private User mUser;
    private TextView mFeedback;
    GoogleSignInButton googleBtn;
    GoogleSignInOptions gOptions;
    GoogleSignInClient gClient;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        // get singleton instance of database
        mWeightTrackerDb = WeightTrackerDatabase.getInstance(getApplicationContext());
        mUserDao = mWeightTrackerDb.userDao();
        mFeedback = (TextView) findViewById(R.id.feedbackTextView);
        googleBtn = findViewById(R.id.googleBtn); //Google sign in button instance

        //Creates a new Google sign in request
        gOptions = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN).requestEmail().build(); //Creates a new Google sign in request
        gClient = GoogleSignIn.getClient(this, gOptions); //

        //Checks if the user is already signed in
        GoogleSignInAccount acct = GoogleSignIn.getLastSignedInAccount(this);
        if(acct!=null){
            changeToWeightActivity();
        }

        // Listens for when the Google login button is pressed
        googleBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                googleLogin();
            }
        });

    }

    // If logged in will set the code to 1000
    void googleLogin(){
        Intent signInIntent = gClient.getSignInIntent();
        startActivityForResult(signInIntent,1000);
    }

    // Checks if the user is logged in and changes the activity over to the Weight Activity while checking for exceptions
    @Override
    protected void onActivityResult(int requestCode, int resultCode,Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if(requestCode == 1000){
            Task<GoogleSignInAccount> task = GoogleSignIn.getSignedInAccountFromIntent(data);

            try {
                task.getResult(ApiException.class);
                changeToWeightActivity();
            } catch (ApiException e) {
                Toast.makeText(getApplicationContext(), "Something went wrong", Toast.LENGTH_SHORT).show();
            }
        }

    }



    /**
     * Login button callback
     * Searches User table for credentials input
     * Authenticates user and starts WeightActivity
     */
    public void onLoginClick(View view) {
        try {
            // get user input from EditText fields
            String username = ((EditText) findViewById(R.id.editTextUsername)).getText().toString();
            String password = ((EditText) findViewById(R.id.editTextPassword)).getText().toString();

            // search SQLite database for matching username and password
            boolean isAuthenticated = login(username, password);
            if (isAuthenticated) {
                mUser = new User(username, password);
                changeToWeightActivity();
                mFeedback.setText(R.string.login_textView_2);
                mFeedback.setTextColor(Color.parseColor("#767676"));
            } else {
                mFeedback.setText(R.string.login_failed);
                mFeedback.setTextColor(Color.RED);
            }
        }catch(Exception e) {
            String error = "";
            for(StackTraceElement elem: e.getStackTrace()) {
                error += elem.toString();
            }
            Log.e("LoginActivity", error);
        }
    }


    /**
     * Callback for account creation button
     * Queries User table to see if username chosen is already taken,
     * then inserts new user record into User table
     */
    public void onCreateAccountClick(View view) {

        // get user input from EditText fields
        String username = ((EditText) findViewById(R.id.editTextUsername)).getText().toString();
        String password = ((EditText) findViewById(R.id.editTextPassword)).getText().toString();

        try {
            // if user input is not blank
            if (!username.isEmpty() && !password.isEmpty()) {
                // get all users from database
                List<User> userList = mUserDao.getUsers();
                boolean found = false;
                // see if username is already taken
                if (userList.size() > 0) {
                    for (int i = 0; i < userList.size(); i++) {
                        if (userList.get(i).getUsername().equals(username)) {
                            found = true;
                        }
                    }
                }
                // if username is not in the database already
                if (!found) {
                    mUserDao.insertUser(new User(username, password));
                    mFeedback.setText(R.string.user_create_success);
                    mFeedback.setTextColor(Color.parseColor("#0e6b0e"));
                }
                // else if username is already taken, notify user
                else {
                    mFeedback.setText(R.string.username_found);
                    mFeedback.setTextColor(Color.RED);
                }
            }
            // else if user input is blank notify user
            else {
                mFeedback.setText(R.string.user_create_fail);
                mFeedback.setTextColor(Color.RED);
            }

        }catch(Exception e) {
            String error = "";
            for(StackTraceElement elem: e.getStackTrace()) {
                error += elem.toString();
            }
            Log.e("LoginActivity - ", error);
        }

    }

    /**
     * Query User table in SQLite database for matching username and password
     */
    private boolean login(String username, String password) {
        List<User> userList = mUserDao.getUsers();
        for (int i = 0; i < userList.size(); i++) {
            if (userList.get(i).getUsername().equals(username) &&
                    userList.get(i).getPassword().equals(password)) {
                return true;
            }
        }
        return false;
    }


    /**
     * Starts the WeightActivity
     */
    public void changeToWeightActivity() {
        finish();
        Intent intent = new Intent(this, WeightActivity.class);
        intent.putExtra("user", mUser);
        startActivity(intent);
    }
}