package com.example.shiftmate;

import android.app.Activity;
import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
import android.content.Intent;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.Button;
import android.widget.DatePicker;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.TimePicker;
import android.widget.Toast;

import com.example.shiftmate.Database.DataSource;
import com.example.shiftmate.Database.Tables.Shifts.Shift;
import com.example.shiftmate.Shared.UniversalFunctions;
import com.example.shiftmate.Shared.UniversalVariables;

import org.joda.time.DateTime;
import org.joda.time.Period;
import org.w3c.dom.Text;

import java.util.Calendar;

//Indicates what data and Text Views should be updated according to the logic of the activity
enum UpdateRequest {

    UPDATE_BEGIN_DATE,
    UPDATE_END_DATE,
    UPDATE_BEGIN_TIME,
    UPDATE_END_TIME,

}

//Indicates how this activity was created for dealing with shift saving/updating logic
enum ActivityRequester {

    MAIN_ACTIVITY,
    END_SHIFT,
    VIEW_SHIFTS_ACTIVITY

}

//DDOS COMPLETE THE BEHAVIOUR OF CALCULATING TOTAL PAY ACCORDING TO THE NEW SETTINGS AND SET THE NEW DB VALUES UP

public class NewShiftActivity extends AppCompatActivity {

    //DDOS#1 see if you can find a more elegant way to implement the DatePickerDialog
    //Currently, we must have a DatePickerDialog.OnDateSetListener object for each
    //TextView that is associated with a DatePickerDialog. This is because the onDataSet callback function does not return the view that initially called
    //The DatePickerDialog, so we can't dynamically know for which date TextView to set the returned date for

    //region Variables & References

    //region Quick shift end related

    static final int UPDATE_OR_END_SHIFT_REQUEST_CODE = 1;  // Request code for startActivityForResult for NewShift callback

    //The Shift requested to be ended
    private Shift shiftToEnd;


    //Indicates how this activity started
    private ActivityRequester activityRequester = ActivityRequester.MAIN_ACTIVITY;

    //endregion

    Button ssButton;

    //These strings hold the shift begin and end dates in a MM/dd/yyyy format
    //These strings get initialized with the current date by default
    String shiftBeginDate = UniversalFunctions.dateToString(UniversalVariables.dateFormatDateString, DateTime.now(), null);
    String shiftEndDate = UniversalFunctions.dateToString(UniversalVariables.dateFormatDateString, DateTime.now(), null);

    //These strings hold the shift begin and end dates in a MM/dd/yyyy format
    //These strings get initialized with the current time by default
    String shiftBeginTime = UniversalFunctions.dateToString(UniversalVariables.dateFormatTimeString, DateTime.now(), null);
    String shiftEndTime = UniversalFunctions.dateToString(UniversalVariables.dateFormatTimeString, DateTime.now(), null);

    //A period object to determine the hour and minute difference between two dates
    Period datePeriod;
    int hourDifference, minuteDifference;

    int breakTime = 0;

    //Holds the total paid hours and minutes according to the hourDifference and minuteDifference passed along with the break time (if applicable, in minutes)
    int[] totalPaidHrsAndMins;

    double payPerHour = 0;

    TextView salesText;
    double totalSales = 0;

    TextView salesPercentageText;
    double salesPercentage = 0;

    TextView tipsText;
    double tips = 0;

    TextView totalPayText;
    double totalPay = 0;

    TextView breakTimeText;

    TextView shiftBeginDateText;
    TextView shiftEndDateText;

    TextView shiftBeginTimeText;
    TextView shiftEndTimeText;

    TextView payPerHourText;
    TextView notesText;

    TextView totalHoursText;
    TextView currencySymbol;

    //region Linear Layouts

    LinearLayout pphLL;
    LinearLayout tipsLL;
    LinearLayout salesLL;
    LinearLayout notesLL;

    //endreiogn

    Calendar myCalendar = Calendar.getInstance();

    //region shiftBeginDate

    //Initialize an event that gets called once a DatePickerDialog returns with the date data
    DatePickerDialog.OnDateSetListener shiftBeginDateSetListener = new DatePickerDialog.OnDateSetListener() {

        @Override
        public void onDateSet(DatePicker view, int year, int monthOfYear,
                              int dayOfMonth) {
            // TODO Auto-generated method stub
            myCalendar.set(Calendar.YEAR, year);
            myCalendar.set(Calendar.MONTH, monthOfYear);
            myCalendar.set(Calendar.DAY_OF_MONTH, dayOfMonth);

            updateGUIAndData(UpdateRequest.UPDATE_BEGIN_DATE);

        }

    };

    //endregion

    //region shiftEndDate

    //DDOS#1
    //Initialize an event that gets called once a DatePickerDialog returns with the date data
    DatePickerDialog.OnDateSetListener shiftEndDateSetListener = new DatePickerDialog.OnDateSetListener() {

        @Override
        public void onDateSet(DatePicker view, int year, int monthOfYear,
                              int dayOfMonth) {
            // TODO Auto-generated method stub
            myCalendar.set(Calendar.YEAR, year);
            myCalendar.set(Calendar.MONTH, monthOfYear);
            myCalendar.set(Calendar.DAY_OF_MONTH, dayOfMonth);

            updateGUIAndData(UpdateRequest.UPDATE_END_DATE);

        }

    };

    //endregion

    //region shiftBeginTime

    //Initialize an event that gets called once a DatePickerDialog returns with the date data
    TimePickerDialog.OnTimeSetListener shiftBeginTimeSetListener = new TimePickerDialog.OnTimeSetListener() {

        @Override
        public void onTimeSet(TimePicker view, int hourOfDay, int minute) {

            myCalendar.set(Calendar.HOUR_OF_DAY, hourOfDay);
            myCalendar.set(Calendar.MINUTE, minute);
            updateGUIAndData(UpdateRequest.UPDATE_BEGIN_TIME);

        }

    };

    //endregion

    //region shiftEndTime

    //Initialize an event that gets called once a DatePickerDialog returns with the date data
    TimePickerDialog.OnTimeSetListener shiftEndTimeSetListener = new TimePickerDialog.OnTimeSetListener() {

        @Override
        public void onTimeSet(TimePicker view, int hourOfDay, int minute) {

            myCalendar.set(Calendar.HOUR_OF_DAY, hourOfDay);
            myCalendar.set(Calendar.MINUTE, minute);
            updateGUIAndData(UpdateRequest.UPDATE_END_TIME);

        }

    };

    //endregion

    //region Update Functions

    //Updates the strings representing the dates and the corresponding TextViews with the selected dates from the DatePickerDialogs
    private void updateGUIAndData(UpdateRequest updateRequest) {

        //Update the requested data and the Text Views according to the request type
        switch (updateRequest) {

            case UPDATE_BEGIN_DATE:
                shiftBeginDate = UniversalFunctions.dateToString(UniversalVariables.dateFormatDateString, null, myCalendar.getTime());
                shiftBeginDateText.setText(UniversalFunctions.changeDateStringFormat(UniversalVariables.dateFormatDate, UniversalVariables.dateFormatDateDisplayString, shiftBeginDate));
                break;

            case UPDATE_END_DATE:
                shiftEndDate = UniversalFunctions.dateToString(UniversalVariables.dateFormatDateString, null, myCalendar.getTime());
                shiftEndDateText.setText(UniversalFunctions.changeDateStringFormat(UniversalVariables.dateFormatDate, UniversalVariables.dateFormatDateDisplayString, shiftEndDate));
                break;

            case UPDATE_BEGIN_TIME:
                shiftBeginTime = UniversalFunctions.dateToString(UniversalVariables.dateFormatTimeString, null, myCalendar.getTime());
                shiftBeginTimeText.setText(UniversalFunctions.changeDateStringFormat(UniversalVariables.dateFormatTime, UniversalVariables.dateFormatTimeString, shiftBeginTime));
                break;

            case UPDATE_END_TIME:
                shiftEndTime = UniversalFunctions.dateToString(UniversalVariables.dateFormatTimeString, null, myCalendar.getTime());
                shiftEndTimeText.setText(UniversalFunctions.changeDateStringFormat(UniversalVariables.dateFormatTime, UniversalVariables.dateFormatTimeString, shiftEndTime));
                break;

        }

        //Update the period according to the data updated above
        updatePeriod();

        //Update Total Hours and Total Paid Hours
        updateHoursAndPayment();

    }

    //Updates the period and gets the hour and minute difference for the currently selected shift being and end dates
    private void updatePeriod() {

        datePeriod = new Period(UniversalFunctions.stringToDateTime(UniversalVariables.dateFormatDateTime, shiftBeginDate + " " + shiftBeginTime),
                UniversalFunctions.stringToDateTime(UniversalVariables.dateFormatDateTime, shiftEndDate + " " + shiftEndTime));

        hourDifference = datePeriod.getHours() + datePeriod.getDays() * 24 + ((datePeriod.getWeeks() * 7) * 24) + (((datePeriod.getMonths() * 4) * 7) * 24) + ((((datePeriod.getYears() * 12) * 4) * 7) * 24);
        hourDifference = hourDifference < 0 ? 0 : hourDifference;

        //Get the minute difference between two dates
        minuteDifference = datePeriod.getMinutes();
        minuteDifference = minuteDifference < 0 ? 0 : minuteDifference;

    }

    //Updates the total hours, paid hours and total payment for the shift based on the date range and pay per hour
    private void updateHoursAndPayment() {

        //If the two selected dates and times actually have a difference between them (the shift begin is before shift end)
        if (hourDifference * 60 + minuteDifference > 0) {

            //If the total shift time in minutes is larger than the break time then set the Total Hours and Paid Hours Text Views accordingly
            if (hourDifference * 60 + minuteDifference > breakTime) {

                totalHoursText.setText(hourDifference + ":" + String.format("%02d", minuteDifference));
                totalPaidHrsAndMins = UniversalFunctions.getHoursAndMinutes((hourDifference * 60 + minuteDifference) - breakTime);

                //Reset the total pay
                totalPay = 0;

                //The total pay gets calculated according to the initiating activity
                if (activityRequester == ActivityRequester.MAIN_ACTIVITY || activityRequester == ActivityRequester.END_SHIFT) {

                    //If it was either the main activity or end shift function that initiated this activity, then we want to calculate the total pay
                    //according to the settings the user has chosen.
                    //For example: the user enabled commision based pay only so we check if that settings was enabled and calculate accordingly

                    if (PreferenceManager.getDefaultSharedPreferences(getBaseContext()).getBoolean(Settings.KEY_PAY_PER_HOUR_SWITCH, true))
                        totalPay += Math.round(((totalPaidHrsAndMins[0] + ((Double.valueOf(totalPaidHrsAndMins[1])) / 60)) * payPerHour) * 100) / 100;

                    if (PreferenceManager.getDefaultSharedPreferences(getBaseContext()).getBoolean(Settings.KEY_SALES_SWITCH, true))
                        totalPay += (Math.round(totalSales * (salesPercentage / 100)) * 100) / 100;


                } else if (activityRequester == ActivityRequester.VIEW_SHIFTS_ACTIVITY) {

                    //If it was ViewShiftActivity that initiated this activity, then we want to calculate the total pay
                    //according to the data set to the chosen shift to be updated.
                    //For example: the user enabled wage based pay only so we check the requested shift's wageEnabled boolean value and calculate accordingly

                    if (shiftToEnd.wageEnabled)
                        totalPay += Math.round(((totalPaidHrsAndMins[0] + ((Double.valueOf(totalPaidHrsAndMins[1])) / 60)) * payPerHour) * 100) / 100;

                    if (shiftToEnd.commisionEnabled)
                        totalPay += (Math.round(totalSales * (salesPercentage / 100)) * 100) / 100;

                }

                //Add the tips
                totalPay += tips;
                totalPayText.setText(String.valueOf(totalPay));

            }

            //Otherwise, the break time (in minutes) is larger than the total shift time (in minutes) then set the Total Hours and Paid Hours Text Views to 0
            else {
                totalHoursText.setText("0");
                totalPayText.setText("0");
            }

        }

        //The shift being date is after the shift end date or the shift begin and end date are equal, which means we set the Total Hours and Paid Hours Text Views to 0
        else {
            totalHoursText.setText("0");
            totalPayText.setText("0");
        }

    }

    //Sets the activity up in a way that reflects the settings the user has selected or according to the specific shift's settings
    private void setUserSettings() {

        //When the main activity/or end shift called for this activity, then set up the settings according to the app's preferences
        if (activityRequester == ActivityRequester.MAIN_ACTIVITY || activityRequester == ActivityRequester.END_SHIFT) {

            if (PreferenceManager.getDefaultSharedPreferences(this).getBoolean(Settings.KEY_TIPS_SWITCH, true) == false)
                tipsLL.setVisibility(View.GONE);

            if (PreferenceManager.getDefaultSharedPreferences(this).getBoolean(Settings.KEY_PAY_PER_HOUR_SWITCH, true) == false)
                pphLL.setVisibility(View.GONE);

            if (PreferenceManager.getDefaultSharedPreferences(this).getBoolean(Settings.KEY_SALES_SWITCH, true) == false)
                salesLL.setVisibility(View.GONE);

        } else if (activityRequester == ActivityRequester.VIEW_SHIFTS_ACTIVITY) {

            if (!shiftToEnd.wageEnabled)
                pphLL.setVisibility(View.GONE);


            if (!shiftToEnd.commisionEnabled)
                salesLL.setVisibility(View.GONE);

        }

    }

    //endregion

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_new_shift);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        setTitle("New Shift");

        ssButton = (Button) findViewById(R.id.ssButton);

        ssButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                //If the start of the shift is before the end of the shift date
                if (UniversalFunctions.stringToDateTime(UniversalVariables.dateFormatDate, shiftBeginDate).isBefore(UniversalFunctions.stringToDateTime(UniversalVariables.dateFormatDate, shiftEndDate))
                        || UniversalFunctions.stringToDateTime(UniversalVariables.dateFormatDate, shiftBeginDate).isEqual(UniversalFunctions.stringToDateTime(UniversalVariables.dateFormatDate, shiftEndDate))) {

                    Shift shift = new Shift();
                    shift.punchInDT = shiftBeginDate + " " + shiftBeginTime;
                    shift.punchOutDT = shiftEndDate + " " + shiftEndTime;
                    shift.totalPay = totalPay;
                    shift.totalMinutes = hourDifference * 60 + minuteDifference;
                    shift.tips = tips;
                    shift.sales = totalSales;
                    shift.salesPercentage = salesPercentage;
                    shift.wageEnabled = PreferenceManager.getDefaultSharedPreferences(getBaseContext()).getBoolean(SettingsFragment.KEY_PAY_PER_HOUR_SWITCH, true);
                    shift.commisionEnabled = PreferenceManager.getDefaultSharedPreferences(getBaseContext()).getBoolean(SettingsFragment.KEY_SALES_SWITCH, true);

                    shift.notes = notesText.getText().toString();

                    if (notesText.length() > 0)
                        shift.notes = notesText.getText().toString();

                    else
                        shift.notes = "";

                    shift.payPerHour = payPerHour;


                    //If the total time of the shift (in minutes) is bigger or equal to the break time then allow the user to save the shift
                    if (hourDifference * 60 + minuteDifference >= breakTime)
                        shift.breakTime = breakTime;

                        //Otherwise, the break time is greater than the shift time, which would yield a negative total paid time (doesn't make sense)
                        //Prompt the user with a toast and exit the function
                    else {
                        Toast.makeText(getApplicationContext(), R.string.Break_Time_Less_Than_Shift_Time, Toast.LENGTH_LONG).show();
                        return;
                    }

                    //Save/update the Shift (depending on who called for this activity)
                    switch (activityRequester) {

                        case MAIN_ACTIVITY:
                            DataSource.shifts.CreateShift(DataSource.shifts.tableName, shift);
                            break;

                        case END_SHIFT:
                        case VIEW_SHIFTS_ACTIVITY:

                            shift.Id = shiftToEnd.Id;//Supply the Id of the shift to end

                            DataSource.shifts.UpdateOrEndShift(DataSource.shifts.tableName, shift);

                            //Set up the return intent for the MainActivity containing an OK code
                            Intent returnIntent = new Intent();
                            setResult(Activity.RESULT_OK, returnIntent);

                            break;


                    }

                    Toast.makeText(getApplicationContext(), String.format("Your shift of %d:%02d hours has been saved!", hourDifference, minuteDifference), Toast.LENGTH_LONG).show();

                    //After the new shift was created and the confirmation toast was displayed, finish with this activity and go backk to the main activity
                    finish();

                }

                //Otherwise, prompt the user with a toast
                else
                    Toast.makeText(getApplicationContext(), R.string.Shift_Begin_Before_End, Toast.LENGTH_LONG).show();

            }
        });

        breakTimeText = (TextView) findViewById(R.id.breakTimeText);

        breakTimeText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {

            }

            //Get the value for the break time. If there is no value then set to NO_BREAKK (0)
            @Override
            public void afterTextChanged(Editable s) {

                if (breakTimeText.length() > 0)
                    breakTime = Integer.parseInt(breakTimeText.getText().toString());

                else
                    breakTime = 0;

                //Update the total and paid hours data, because break time affects this data
                updateHoursAndPayment();

            }

        });

        shiftBeginDateText = (TextView) findViewById(R.id.shiftBeginDateText);
        shiftBeginDateText.setText(UniversalFunctions.changeDateStringFormat(UniversalVariables.dateFormatDate, UniversalVariables.dateFormatDateDisplayString, shiftBeginDate));

        shiftBeginDateText.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {

                //Initialize a new instance of the DatePickerDialog class with the current date & display the DatePickerDialog itself
                //In the arguments we have:
                //The reference to the context class (this)
                //The over-ridden callback function defined in "date"
                //The object of type Calendar which we use to get the current year, month and day for the DatePickerDialog to be initialized with
                new DatePickerDialog(NewShiftActivity.this, shiftBeginDateSetListener, myCalendar
                        .get(Calendar.YEAR), myCalendar.get(Calendar.MONTH),
                        myCalendar.get(Calendar.DAY_OF_MONTH)).show();

            }
        });

        shiftEndDateText = (TextView) findViewById(R.id.shiftEndDateText);
        shiftEndDateText.setText(UniversalFunctions.changeDateStringFormat(UniversalVariables.dateFormatDate, UniversalVariables.dateFormatDateDisplayString, shiftEndDate));

        //DDOS#1
        shiftEndDateText.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {

                //Initialize a new instance of the DatePickerDialog class with the current date & display the DatePickerDialog itself
                //In the arguments we have:
                //The reference to the context class (this)
                //The over-ridden callback function defined in "date"
                //The object of type Calendar which we use to get the current year, month and day for the DatePickerDialog to be initialized with
                new DatePickerDialog(NewShiftActivity.this, shiftEndDateSetListener, myCalendar
                        .get(Calendar.YEAR), myCalendar.get(Calendar.MONTH),
                        myCalendar.get(Calendar.DAY_OF_MONTH)).show();

            }
        });

        shiftBeginTimeText = (TextView) findViewById(R.id.shiftBeginTimeText);
        shiftBeginTimeText.setText(UniversalFunctions.changeDateStringFormat(UniversalVariables.dateFormatTime, UniversalVariables.dateFormatTimeString, shiftBeginTime));

        shiftBeginTimeText.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {

                //Initialize a new instance of the DatePickerDialog class with the current date & display the DatePickerDialog itself
                //In the arguments we have:
                //The reference to the context class (this)
                //The over-ridden callback function defined in "date"
                //The object of type Calendar which we use to get the current year, month and day for the DatePickerDialog to be initialized with
                new TimePickerDialog(NewShiftActivity.this, shiftBeginTimeSetListener, myCalendar
                        .get(Calendar.HOUR), myCalendar.get(Calendar.MINUTE),
                        false).show();

            }
        });

        shiftEndTimeText = (TextView) findViewById(R.id.shiftEndTimeText);
        shiftEndTimeText.setText(UniversalFunctions.changeDateStringFormat(UniversalVariables.dateFormatTime, UniversalVariables.dateFormatTimeString, shiftEndTime));

        //DDOS#1
        shiftEndTimeText.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {

                //Initialize a new instance of the DatePickerDialog class with the current date & display the DatePickerDialog itself
                //In the arguments we have:
                //The reference to the context class (this)
                //The over-ridden callback function defined in "date"
                //The object of type Calendar which we use to get the current year, month and day for the DatePickerDialog to be initialized with
                new TimePickerDialog(NewShiftActivity.this, shiftEndTimeSetListener, myCalendar
                        .get(Calendar.HOUR), myCalendar.get(Calendar.MINUTE),
                        false).show();

            }
        });

        payPerHour = Float.valueOf(PreferenceManager.getDefaultSharedPreferences(this).getString(Settings.KEY_PAY_PER_HOUR, "0"));

        payPerHourText = (TextView) findViewById(R.id.payPerHourText);
        payPerHourText.setText(PreferenceManager.getDefaultSharedPreferences(this).getString(Settings.KEY_PAY_PER_HOUR, "0"));

        salesPercentage = Double.valueOf(PreferenceManager.getDefaultSharedPreferences(this).getString(Settings.KEY_SALES_PERCENTAGE, "0"));

        salesPercentageText = (TextView) findViewById(R.id.salesPercentageText);
        salesPercentageText.setText(PreferenceManager.getDefaultSharedPreferences(this).getString(Settings.KEY_SALES_PERCENTAGE, "0"));

        tipsText = (TextView) findViewById(R.id.tipsText);
        tipsText.setText("");

        salesText = (TextView) findViewById(R.id.salesText);
        salesText.setText("0");

        notesText = (TextView) findViewById(R.id.notesText);
        notesText.setText("");

        totalHoursText = (TextView) findViewById(R.id.totalHoursText);
        totalHoursText.setText("0");

        currencySymbol = (TextView) findViewById(R.id.currencySymbol);
        currencySymbol.setText(DataSource.currencies.currencyList.get(Settings.getCurrencyListSelectedIndex()).currencySymbol);

        totalPayText = (TextView) findViewById(R.id.totalPayText);
        totalPayText.setText("0");

        //region Linear Layouts

        //Set the visibility and usability of components according to the settings

        pphLL = (LinearLayout) findViewById(R.id.pphLL);
        salesLL = (LinearLayout) findViewById(R.id.salesLL);
        tipsLL = (LinearLayout) findViewById(R.id.tipsLL);
        notesLL = (LinearLayout) findViewById(R.id.notesLL);

        //Total pay calculation TextViews event registration
        payPerHourText.addTextChangedListener(totalPayCalculationTextWatcher);
        salesPercentageText.addTextChangedListener(totalPayCalculationTextWatcher);
        salesPercentageText.addTextChangedListener(totalPayCalculationTextWatcher);
        salesText.addTextChangedListener(totalPayCalculationTextWatcher);
        tipsText.addTextChangedListener(totalPayCalculationTextWatcher);

        //endregion

        //Acquire data from MainActivity (for when a quick shift was requested to be ended)
        //If this activity was started from selecting the New Shift button then the data below will be null!
        if (getIntent().getBooleanExtra("endShiftRequest", false)) {

            setTitle("End Shift");

            activityRequester = ActivityRequester.END_SHIFT;

            //Get the Id of the shift we are requested to the EndShift function on
            shiftToEnd = DataSource.shifts.shiftList.get(getIntent().getIntExtra("shiftEndIndex", 0));

            DateTime punchInDT = UniversalFunctions.stringToDateTime(UniversalVariables.dateFormatDateTime, shiftToEnd.punchInDT);
            DateTime punchOutDT = DateTime.now();

            shiftBeginDate = UniversalFunctions.dateToString(UniversalVariables.dateFormatDateString, punchInDT, null);
            shiftBeginDateText.setText(UniversalFunctions.changeDateStringFormat(UniversalVariables.dateFormatDate, UniversalVariables.dateFormatDateDisplayString, shiftBeginDate));

            shiftEndDate = UniversalFunctions.dateToString(UniversalVariables.dateFormatDateString, punchOutDT, null);
            shiftEndDateText.setText(UniversalFunctions.changeDateStringFormat(UniversalVariables.dateFormatDate, UniversalVariables.dateFormatDateDisplayString, shiftEndDate));

            shiftBeginTime = UniversalFunctions.dateToString(UniversalVariables.dateFormatTimeString, punchInDT, null);
            shiftBeginTimeText.setText(shiftBeginTime);

            shiftEndTime = UniversalFunctions.dateToString(UniversalVariables.dateFormatTimeString, punchOutDT, null);
            shiftEndTimeText.setText(shiftEndTime);

            //Update the period according to the data updated above
            updatePeriod();

            //Update Total Hours and Total Paid Hours
            updateHoursAndPayment();

        }

        //If this activity was started from selecting to update a shift from ViewShiftsActivity then fill in in that particular shifts' details
        else if (getIntent().getBooleanExtra("updateShiftRequest", false)) {

            setTitle("Update Shift");

            activityRequester = ActivityRequester.VIEW_SHIFTS_ACTIVITY;

            //Change the save shift button text to update shift
            ssButton.setText("Update Shift");

            //Get tall the data for the requested Shift to update:
            shiftToEnd = DataSource.shifts.shiftList.get(getIntent().getIntExtra("shiftEndIndex", 0));

            shiftBeginDate = UniversalFunctions.changeDateStringFormat(UniversalVariables.dateFormatDateTime, UniversalVariables.dateFormatDateString, shiftToEnd.punchInDT);
            shiftBeginDateText.setText(UniversalFunctions.changeDateStringFormat(UniversalVariables.dateFormatDate, UniversalVariables.dateFormatDateDisplayString, shiftBeginDate));

            shiftEndDate = UniversalFunctions.changeDateStringFormat(UniversalVariables.dateFormatDateTime, UniversalVariables.dateFormatDateString, shiftToEnd.punchOutDT);
            shiftEndDateText.setText(UniversalFunctions.changeDateStringFormat(UniversalVariables.dateFormatDate, UniversalVariables.dateFormatDateDisplayString, shiftEndDate));

            shiftBeginTime = UniversalFunctions.changeDateStringFormat(UniversalVariables.dateFormatDateTime, UniversalVariables.dateFormatTimeString, shiftToEnd.punchInDT);
            shiftBeginTimeText.setText(shiftBeginTime);

            shiftEndTime = UniversalFunctions.changeDateStringFormat(UniversalVariables.dateFormatDateTime, UniversalVariables.dateFormatTimeString, shiftToEnd.punchOutDT);
            shiftEndTimeText.setText(shiftEndTime);

            breakTime = shiftToEnd.breakTime;
            breakTimeText.setText(String.valueOf(breakTime != 0 ? breakTime : ""));

            payPerHourText.setText(PreferenceManager.getDefaultSharedPreferences(this).getString(Settings.KEY_PAY_PER_HOUR, "0"));
            notesText.setText(shiftToEnd.notes);
            tipsText.setText(String.valueOf(shiftToEnd.tips != 0 ? shiftToEnd.tips : ""));
            salesText.setText(String.valueOf(shiftToEnd.sales != 0 ? shiftToEnd.sales : ""));

            //Update the period according to the data updated above
            updatePeriod();

            //Update Total Hours and Total Paid Hours
            updateHoursAndPayment();

        }

        setUserSettings();

    }

    //The text watcher that sets total pay calculation related data, as well as calls for total pay re-calculation according to certain text view data changes
    TextWatcher totalPayCalculationTextWatcher = new TextWatcher() {
        @Override
        public void beforeTextChanged(CharSequence s, int start, int count, int after) {

        }

        /*
        Each EditText has its own Spannable. TextWatcher's events has this Spannable as s parameter. I check their hashCode (unique Id of each object).
        myEditText1.getText() returns that Spannable. So if the myEditText1.getText().hashCode() equals with s.hashCode() it means that s belongs to myEditText1
         */
        @Override
        public void onTextChanged(CharSequence s, int start, int before, int count) {

            if (s.hashCode() == payPerHourText.getText().hashCode()) {

                if (s.length() > 0)
                    payPerHour = Double.valueOf(Double.valueOf(s.toString()));
                else
                    payPerHour = 0;

            } else if (s.hashCode() == salesText.getText().hashCode()) {

                if (s.length() > 0)
                    totalSales = Double.valueOf(Double.valueOf(s.toString()));
                else
                    totalSales = 0;

            } else if (s.hashCode() == salesPercentageText.getText().hashCode()) {

                if (s.length() > 0)
                    salesPercentage = Double.valueOf(Double.valueOf(s.toString()));
                else
                    salesPercentage = 0;

            } else if (s.hashCode() == tipsText.getText().hashCode()) {

                if (s.length() > 0)
                    tips = Double.valueOf(Double.valueOf(s.toString()));
                else
                    tips = 0;

            }

            //Update the total pay calculation
            updateHoursAndPayment();

        }

        @Override
        public void afterTextChanged(Editable s) {

        }
    };


}
