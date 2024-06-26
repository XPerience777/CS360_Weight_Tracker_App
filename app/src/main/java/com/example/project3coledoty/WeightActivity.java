package com.example.project3coledoty;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.telephony.SmsManager;
import android.telephony.TelephonyManager;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.RelativeLayout;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;
import android.widget.Toast;

import com.example.project3coledoty.database.DailyWeightDao;
import com.example.project3coledoty.database.GoalWeightDao;
import com.example.project3coledoty.database.WeightTrackerDatabase;
import com.example.project3coledoty.model.DailyWeight;
import com.example.project3coledoty.model.GoalWeight;
import com.example.project3coledoty.model.User;
import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.List;
import java.util.Locale;

import static android.widget.Toast.makeText;

public class WeightActivity extends AppCompatActivity {

    private static final int MY_PERMISSIONS_REQUEST_SEND_SMS = 1;
    private static final int PERMISSION_READ_STATE = 2;
    private final int LAUNCH_ADD_RECORD_ACTIVITY = 1;
    private final int LAUNCH_CHANGE_RECORD_ACTIVITY = 2;
    private final int LAUNCH_DELETE_RECORD_ACTIVITY = 3;
    private final int LAUNCH_CHANGE_TARGET_ACTIVITY = 4;
    private WeightTrackerDatabase mWeightTrackerDb;
    private DailyWeightDao mDailyWeightDao;
    private GoalWeightDao mGoalWeightDao;
    private User mUser;
    DailyWeight mNewDailyWeight;
    TableLayout mTableLayout;
    TextView mTargetWeight;
    TableRow mNoRecordsRow;
    TextView userName;
    Button logout;
    GoogleSignInClient gClient;
    GoogleSignInOptions gOptions;


    @Override
    protected void onCreate(Bundle savedInstanceState) {

        try {
            super.onCreate(savedInstanceState);
            this.getSupportActionBar().hide();
            gOptions = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN).requestEmail().build();
            gClient = GoogleSignIn.getClient(this, gOptions);
            setContentView(R.layout.activity_weight);

            mTableLayout = (TableLayout)findViewById(R.id.dailyWeightTable);
            mTargetWeight = (TextView)findViewById(R.id.goalWeightText);

            // get singleton instance of database
            mWeightTrackerDb = WeightTrackerDatabase.getInstance(getApplicationContext());
            mDailyWeightDao = mWeightTrackerDb.dailyWeightDao();


            // get User object from login screen
            Intent intent = getIntent();
            mUser = (User) getIntent().getSerializableExtra("user");

            // refresh the table and target weight textview
            refreshTable();
            refreshTargetWeight();

            // check for granted permissions, ask user for permissions if need be
            checkForAllPermissions();

        }
        catch (Exception e) {
            e.printStackTrace();
        }


    }

    //Changes back to the login screen
    public void changeToLoginActivity() {
        finish();
        Intent intent = new Intent(this, LoginActivity.class);
        intent.putExtra("user", mUser);
        startActivity(intent);
    }


    /**
     * Refreshes target weight figure on screen
     */
    public void refreshTargetWeight() {
        try {
            mGoalWeightDao = mWeightTrackerDb.goalWeightDao();
            int count = mGoalWeightDao.countGoalEntries(mUser.getUsername());
            // if there is no goal weight, insert default goal weight of 150lbs
            if (count == 0) {
                GoalWeight defaultGoal = new GoalWeight(150.0, mUser.getUsername());
                mGoalWeightDao.insertGoalWeight(defaultGoal);
                return;
            }
            else if (count < 0 || count > 1) {
                System.out.println("Something went wrong, counting entries of GoalWeight DB" +
                        "returns number other than 0 or 1");
                return;
            }

            count = mGoalWeightDao.countGoalEntries(mUser.getUsername());

            GoalWeight currentGoal = mGoalWeightDao.getSingleGoalWeight(mUser.getUsername());
            mTargetWeight.setText(currentGoal.getGoal() + " lbs");

        }catch (Exception e) {
            e.printStackTrace();
        }
    }


    public void refreshTable() {
        try {
            // remove all but the header from the TableLayout
            cleanTable(mTableLayout);
            // get all DailyWeight records of this user
            List<DailyWeight> userDailyWeights = mDailyWeightDao.getDailyWeightsOfUser(mUser.getUsername());

            // if there are no DailyWeights for this user
            if (userDailyWeights.size() == 0) {
                System.out.println("addNoRecordsRow executed.;asldkfhas;dlkjfhas;dessssssssssssssssssssssssssssssss");
                addNoRecordsRow(mTableLayout);
            }
            // if there are DailyWeights for this user
            else {

                DateFormat formatter = new SimpleDateFormat("MM/dd/yyyy", Locale.US);
                TableRow header = (TableRow) findViewById(R.id.headerRow);
                TextView headerDate = (TextView) findViewById(R.id.headerDate);
                TextView headerWeight = (TextView) findViewById(R.id.headerWeight);

                // layout parameters for row:
                TableLayout.LayoutParams layoutParamsTable = (TableLayout.LayoutParams) header.getLayoutParams();
                // layout parameters for textViews:
                TableRow.LayoutParams layoutParamsRow = (TableRow.LayoutParams) headerDate.getLayoutParams();

                for (int i = 0; i < userDailyWeights.size(); i++) {
                    TableRow row = new TableRow(this);
                    TextView dateTextView = new TextView(this);
                    TextView weightTextView = new TextView(this);

                    // activate the layout parameters
                    row.setLayoutParams(layoutParamsTable);
                    dateTextView.setLayoutParams(layoutParamsRow);
                    weightTextView.setLayoutParams(layoutParamsRow);
                    // set additional view properties
                    dateTextView.setWidth(0);
                    dateTextView.setGravity(Gravity.CENTER);
                    dateTextView.setPadding(20, 20, 20, 20);
                    weightTextView.setWidth(0);
                    weightTextView.setGravity(Gravity.CENTER);
                    weightTextView.setPadding(20, 20, 20, 20);

                    // set the value of the date TextView
                    dateTextView.setText(formatter.format(userDailyWeights.get(i).getDate()));
                    // set the value of the weight TextView
                    weightTextView.setText(Double.toString(userDailyWeights.get(i).getWeight()));

                    // add the 2 TextViews to the current row
                    row.addView(dateTextView);
                    row.addView(weightTextView);

                    // add row to TableLayout
                    mTableLayout.addView(row);
                }
            }

        }catch (Exception e) {
            e.printStackTrace();
        }
    }

    // Remove all but the header from the TableLayout
    // Also leave "no records" row if there are no DailyWeights
    private void cleanTable(TableLayout table) {
        try {
            int childCount = table.getChildCount();

            // Remove all rows except the first one
            if (childCount > 1) {
                table.removeViews(1, childCount - 1);
            }

        }catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void addNoRecordsRow(TableLayout table) {
        mTableLayout.addView(mNoRecordsRow);
        System.out.println("addNoRecordsRow() EXECUTED....................................................");
    }

    // Check if user has reached target weight
    private void reachedGoalCheck() {
        try {
            // get all DailyWeight records of this user
            List<DailyWeight> userDailyWeights = mDailyWeightDao.getDailyWeightsOfUser(mUser.getUsername());

            // get most recent weight. Latest date in the list
            if (userDailyWeights.size() != 0) {
                double currentWeight = userDailyWeights.get(userDailyWeights.size() - 1).getWeight();

                // get target weight for this user
                double targetWeight = mGoalWeightDao.getSingleGoalWeight(mUser.getUsername()).getGoal();

                // if current weight is lower than goal, send congratulating text to user
                if (currentWeight <= targetWeight) {
                    sendTextToUser();
                }
            }

        }catch (Exception e) {
            e.printStackTrace();
        }
    }


    public void sendTextToUser() {
        try {
            // if SEND_SMS and READ_PHONE_STATE permissions granted, send the text
            checkForAllPermissions();
            if (ActivityCompat.checkSelfPermission(this,
                    Manifest.permission.SEND_SMS) ==
                    PackageManager.PERMISSION_GRANTED &&
                    ContextCompat.checkSelfPermission(this,
                    Manifest.permission.READ_PHONE_STATE)
                    == PackageManager.PERMISSION_GRANTED) {
                System.out.println("Sending a text congragulating user...");
                SmsManager smsManager = SmsManager.getDefault();
                TelephonyManager telephonyManager = (TelephonyManager) this.getSystemService(this.TELEPHONY_SERVICE);
                String phoneNum = telephonyManager.getLine1Number();
                if (phoneNum != null) {
                    smsManager.sendTextMessage(phoneNum, null,
                            "Congratulations, you reached your target weight!",
                            null, null);
                }
            }
            else {
                // show toast
                Toast toast = makeText(WeightActivity.this, "Congratulations, you reached your target weight!",
                        Toast.LENGTH_LONG);
                toast.setGravity(Gravity.TOP, 0, 0);
                toast.getView().setBackgroundColor(0xFFCC99FF);
                toast.show();
            }

        }catch (Exception e) {
            e.printStackTrace();
        }
    }


    public void checkForPhoneStatePermissions() {
        try {
            // if we do not have this permission, ask the user
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_PHONE_STATE)
                    != PackageManager.PERMISSION_GRANTED) {
                System.out.println("Asking for READ_PHONE_STATE permission...");
                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.READ_PHONE_STATE}, PERMISSION_READ_STATE);
            }
            // else the app already has the permission
            else {
                System.out.println("App already has READ_PHONE_STATE permission");
            }
        }catch (Exception e) {
            e.printStackTrace();
        }
    }


    public void checkForAllPermissions() {
        try {
            // if we do not have one or both of necessary permissions
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_PHONE_STATE)
                    != PackageManager.PERMISSION_GRANTED ||
                ContextCompat.checkSelfPermission(this, Manifest.permission.SEND_SMS)
                        != PackageManager.PERMISSION_GRANTED)
            {
                System.out.println("Asking for READ_PHONE_STATE permission...");
                ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.READ_PHONE_STATE,
                                Manifest.permission.SEND_SMS},
                        PERMISSION_READ_STATE);
            }
            // else the app already has the permission
            else {
                System.out.println("App already has READ_PHONE_STATE permission");
            }
        }catch (Exception e) {
            e.printStackTrace();
        }
    }



    // Callback for add record button
    public void addRecordOnClick(View view) {
        try {
            // call AddRecordActivity to get new date and weight input from user
            Intent intent = new Intent(this, AddRecordActivity.class);
            startActivityForResult(intent, LAUNCH_ADD_RECORD_ACTIVITY);

        }catch (Exception e) {
            e.printStackTrace();
        }
    }

    // Callback for delete record button
    public void deleteRecordOnClick(View view) {
        try {
            // call DeleteRecordActivity
            Intent intent = new Intent(this, DeleteRecordActivity.class);
            intent.putExtra("user", mUser);
            startActivityForResult(intent, LAUNCH_DELETE_RECORD_ACTIVITY);

        }catch (Exception e) {
            e.printStackTrace();
        }
    }

    // Callback for edit record button
    public void editRecordOnClick(View view) {
            try {
                // call ChangeRecordActivity
                Intent intent = new Intent(this, ChangeRecordActivity.class);
                intent.putExtra("user", mUser);
                startActivityForResult(intent, LAUNCH_CHANGE_RECORD_ACTIVITY);

            }catch (Exception e) {
                e.printStackTrace();
            }
    }


    // Callback for edit target weight button
    public void editTargetOnClick(View view) {
        try {
            // call ChangeRecordActivity
            Intent intent = new Intent(this, ChangeTargetActivity.class);
            intent.putExtra("user", mUser);
            startActivityForResult(intent, LAUNCH_CHANGE_TARGET_ACTIVITY);

        }catch (Exception e) {
            e.printStackTrace();
        }

    }

    //When clicked the google client will be logged out and will swap to login screen
    public void logoutOnClick(View view) {
        try {
            gClient.signOut().addOnCompleteListener(new OnCompleteListener<Void>() {
                @Override
                public void onComplete(@NonNull Task<Void> task) {
                    changeToLoginActivity();
                }
            });

        }catch (Exception e) {
            e.printStackTrace();
        }

    }


    /**
     * Check for SEND_SMS permission and ask user for permission if not granted
     */
    private void checkForSmsPermission() {
        // if app doesn't have SEND_SMS permission yet
        if (ActivityCompat.checkSelfPermission(this,
                Manifest.permission.SEND_SMS) !=
                PackageManager.PERMISSION_GRANTED) {

            System.out.println("System didn't have permission for texting yet. " +
                    "Asking permission now...");

            // ask user for permission
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.SEND_SMS},
                    MY_PERMISSIONS_REQUEST_SEND_SMS);
        }
        // else permission already granted
        else {
            System.out.println("SEND_SMS permission already granted");
        }
    }


    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if(resultCode == Activity.RESULT_OK) {

            // for adding a record
            if (requestCode == LAUNCH_ADD_RECORD_ACTIVITY) {
                try {
                    mNewDailyWeight = (DailyWeight) data.getSerializableExtra("newDailyWeight");
                    mNewDailyWeight.setUsername(mUser.getUsername());

                    mDailyWeightDao.insertDailyWeight(mNewDailyWeight);
                    refreshTable();
                    refreshTargetWeight();
                    reachedGoalCheck();

                }catch (Exception e) {
                    e.printStackTrace();
                }
            }

            // for changing a record
            if (requestCode == LAUNCH_CHANGE_RECORD_ACTIVITY) {
                refreshTable();
                refreshTargetWeight();
                reachedGoalCheck();

                // show toast
                Toast toast = makeText(WeightActivity.this, "Weight record successfully changed",
                        Toast.LENGTH_LONG);
                toast.getView().setBackgroundColor(0xFFCC99FF);
                toast.show();
            }

            // for deleting a record
            if (requestCode == LAUNCH_DELETE_RECORD_ACTIVITY) {
                cleanTable(mTableLayout);
                refreshTable();
                refreshTargetWeight();

                // show toast
                Toast toast = makeText(WeightActivity.this, "Weight record successfully deleted",
                        Toast.LENGTH_LONG);
                toast.getView().setBackgroundColor(0xFFCC99FF);
                toast.show();
            }

            // for changing target weight
            if (requestCode == LAUNCH_CHANGE_TARGET_ACTIVITY) {
                refreshTable();
                refreshTargetWeight();
                reachedGoalCheck();

                // show toast
                Toast toast = makeText(WeightActivity.this, "Target weight successfully updated",
                        Toast.LENGTH_LONG);
                toast.getView().setBackgroundColor(0xFFCC99FF);
                toast.show();
            }
        }
    }

}