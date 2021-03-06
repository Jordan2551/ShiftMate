package com.shiftbuddy.app;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.View;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Button;
import android.widget.TextView;

import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdView;
import com.google.android.gms.ads.MobileAds;
import com.shiftbuddy.app.Database.Connector.DBConnector;
import com.shiftbuddy.app.Database.DataPersister;
import com.shiftbuddy.app.Database.DataSource;
import com.shiftbuddy.app.Database.Tables.Shifts.Shift;
import com.shiftbuddy.app.Database.Tables.Shifts.Shifts;
import com.shiftbuddy.app.Shared.UniversalFunctions;
import com.shiftbuddy.app.Shared.UniversalVariables;
import com.shiftbuddy.app.ViewShifts.ViewShifts;

import org.joda.time.DateTime;

public class MainActivity extends DataPersister {

    private static Context appContext;

    public static DBConnector dbConnector;

    static final int END_SHIFT_REQUEST_CODE = 1;  // Request code for startActivityForResult for NewShift callback

    //region Variables & References

    Button nsButton;
    Button qsButton;
    Button vsButton;
    Button vgButton;
    Button settingsButton;
    TextView latestShiftTV;

    //endregion

    public static Context getContext() {
        return appContext;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        appContext = this;
        setContentView(R.layout.activity_main);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        setTitle("Shift Mate");

        //Set up the add banner
        MobileAds.initialize(getApplicationContext(), getString(R.string.ad_app_id));
        AdView mAdView = (AdView) findViewById(R.id.adView);
        AdRequest adRequest = new AdRequest.Builder().build();
        mAdView.loadAd(adRequest);

        //Calling this during onCreate() ensures that your application is properly initialized with default settings, which your application might need to read in order to determine some behaviors
        //(such as whether to download data while on a cellular network).
        PreferenceManager.setDefaultValues(this, R.xml.preferences, false);

        //Open the database connection
        dbConnector = new DBConnector(this);
        dbConnector.openConnection();

        //Acquire all shift records so we can use the shift list data globally, determine below if the latest shift has ended or not
        //And if we should or should not display the latestShiftTV
        DataSource.shifts.GetRecords(DataSource.shifts.tableName);

        latestShiftTV = (TextView) findViewById(R.id.latestShiftTV);

        //region New Shift Button

        nsButton = (Button) findViewById(R.id.nsButton);

        //Transfer the user to the NewShiftActivity
        nsButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                Intent intent = new Intent(MainActivity.this, NewShiftActivity.class);
                startActivity(intent);

            }
        });

        //endregion

        //region Quick Shift Button

        //This button opens a new quick shift (adds a new Shift entry into the DB) only if there is no
        //other open shift in the database already, otherwise, this button will offer to "end shift" which will
        //change the punchout time of that detected open shift which will close that open shift.
        qsButton = (Button) findViewById(R.id.qsButton);

        qsButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                int lastOpenShiftId = Shifts.getLastOpenShift();

                //End the shift if there is at least one shift without a punch out date time (an open shift)
                if (lastOpenShiftId != -1) {

                    //Transition to the NewShiftActivity with the data for the punchInDT
                    Intent intent = new Intent(MainActivity.this, NewShiftActivity.class);
                    intent.putExtra("endShiftRequest", true);
                    intent.putExtra("punchInDT", Shifts.shiftList.get(lastOpenShiftId).punchInDT);
                    intent.putExtra("shiftEndIndex", lastOpenShiftId);
                    startActivityForResult(intent, END_SHIFT_REQUEST_CODE);

                }

                //Otherwise, we add a new quick shift
                else {

                    Shift shift = new Shift();
                    shift.punchInDT = UniversalFunctions.dateToString(UniversalVariables.dateFormatDateTimeMilitaryString, DateTime.now(), null);
                    shift.punchOutDT = Shift.PUNCHOUT_NONE;
                    shift.breakTime = 0;
                    shift.totalPay = 0.0;

                    DataSource.shifts.CreateShift(DataSource.shifts.tableName, shift);

                    UpdateGUI();

                }

            }
        });


        //endregion

        //region View Shifts Button

        vsButton = (Button) findViewById(R.id.vsButton);

        //When the quick shift button is clicked we want to add a new database record that contains
        //an id and a start date and time(we leave the end date as null to indicate we have not finished a shift yet)
        vsButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                //Intent intent = new Intent(MainActivity.this, ViewShiftsActivity.class);
                Intent intent = new Intent(MainActivity.this, ViewShifts.class);
                startActivity(intent);

            }
        });

        //endregion

        //region Graphs Button

        vgButton = (Button) findViewById(R.id.vgButton);

        vgButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                Intent intent = new Intent(MainActivity.this, DataGraph.class);
                startActivity(intent);

            }
        });

        //endregion

        //region Settings Button

        settingsButton = (Button) findViewById(R.id.settingsButton);

        //Clicking on the settings button will open the settings activity
        settingsButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                Intent intent = new Intent(MainActivity.this, Settings.class);
                startActivity(intent);

            }
        });

        //endregion

        //Request to update the buttons displayed for the GUI based on the database's data
        UpdateGUI();

    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        return super.onOptionsItemSelected(item);
    }

    //Updates the GUI's selectable buttons depending on the records of the database
    public void UpdateGUI() {

        //If there is at least 1 shift with a punchOutDT of PUNCHOUT_NONE (an open shift) then we display "End Shift" and some helpful GUI elements
        //Otherwise, there is no open shift so we just display "Quick Shift" to let the user open a new quick shift
        if (Shifts.getLastOpenShift() != -1) {

            latestShiftTV.setText("Latest Quick Shift Started: " + UniversalFunctions.changeDateStringFormat(UniversalVariables.dateFormatDateTimeMilitary, UniversalVariables.dateFormatDateTimeDisplayString, DataSource.shifts.shiftList.get(DataSource.shifts.shiftList.size() - 1).punchInDT));
            latestShiftTV.setVisibility(View.VISIBLE);
            qsButton.setText("Punch Out");
            qsButton.setCompoundDrawablesWithIntrinsicBounds(R.drawable.ic_quick_punch_out, 0, 0, 0);

        } else {

            latestShiftTV.setText("Select 'Quick Shift' to punch in!");
            qsButton.setText("Quickly Punch In");
            qsButton.setCompoundDrawablesWithIntrinsicBounds(R.drawable.ic_quick_punch_in, 0, 0, 0);

        }

    }

    //Gets a response from NetShiftActivity regarding the ending of a quick shift
    //Once the NewShiftActivity finishes closing a quick shift we update the GUI
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        // Check which request we're responding to
        if (requestCode == END_SHIFT_REQUEST_CODE) {
            // Make sure the request was successful
            if (resultCode == RESULT_OK) {
                UpdateGUI();
            }
        }
    }

}
