package com.example.to_do_list;

import android.app.AlarmManager;
import android.app.DatePickerDialog;
import android.app.PendingIntent;
import android.app.TimePickerDialog;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.Button;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.TimePicker;
import android.widget.Toast;

import com.example.to_do_list.db.Task;

import java.util.Calendar;
import java.util.Date;

import io.realm.Realm;
import io.realm.RealmResults;
import io.realm.Sort;

public class AddTaskActivity extends AppCompatActivity implements TimePickerDialog.OnTimeSetListener, DatePickerDialog.OnDateSetListener {

    Switch switchNotify;
    Button addTaskBtn;
    Button timePickBtn;
    Button datePickBtn;
    TextView timeTextView;
    TextView dateTextView;
    Calendar calendar;
    int cYear, cMonth, cDayOfMonth, cHourOfDay, cMinute;
    private Toast toast = null;
    static int reqCode = 0;
    private Realm realm;
    EditText taskEditTxt;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_task);
        realm = Realm.getDefaultInstance();

        taskEditTxt = findViewById(R.id.task_description);
        timeTextView = findViewById(R.id.pickTimeTextView);
        dateTextView = findViewById(R.id.pickDateTextView);
        if (!getIntent().getStringExtra("taskText").equals(""))
            taskEditTxt.setText(getIntent().getStringExtra("taskText"));

        timePickBtn = findViewById(R.id.btnAddPickTime);
        timePickBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                DialogFragment timePicker = new TimePickerFragment();
                timePicker.show(getSupportFragmentManager(), "time picker");
            }
        });

        datePickBtn = findViewById(R.id.btnAddPickDate);
        datePickBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                DialogFragment datePicker = new DatePickerFragment();
                datePicker.show(getSupportFragmentManager(), "date picker");
            }
        });

        addTaskBtn = findViewById(R.id.btnAddTask);
        addTaskBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (String.valueOf(taskEditTxt.getText().toString()).trim().equalsIgnoreCase("")) {
                    taskEditTxt.setError("Input field cannot be empty.");
                    taskEditTxt.requestFocus();
                } else if (String.valueOf(taskEditTxt.getText().toString()).length() > 25) {
                    taskEditTxt.setError("The task input field cannot be over 25 characters long.");
                } else if (Utils.isExpiredDate(cYear, cMonth, cDayOfMonth, cHourOfDay, cMinute) && !timeTextView.getText().toString().equals("Time not set")) {
                    if (toast != null)
                        toast.cancel();
                    toast = Toast.makeText(getApplicationContext(), "The date has expired. Please pick another date.", Toast.LENGTH_SHORT);
                    toast.show();
                } else {
                    addOrUpdateTask(v);
                    finish();
                }
            }
        });

        final LinearLayout linearLayout = findViewById(R.id.linearLayoutSetTime);
        linearLayout.setVisibility(View.GONE);

        switchNotify = findViewById(R.id.switchNotify);
        switchNotify.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (switchNotify.isChecked()) {
                    linearLayout.setVisibility(View.VISIBLE);

                    Date currDate = new Date();
                    Calendar calendar = Calendar.getInstance();
                    calendar.setTime(currDate);
                    cYear = calendar.get(Calendar.YEAR);
                    cMonth = calendar.get(Calendar.MONTH);
                    cDayOfMonth = calendar.get(Calendar.DAY_OF_MONTH);
                    String date = String.valueOf(cYear + "-" + (cMonth + 1) + "-" + cDayOfMonth);
                    dateTextView.setText(date);
                    cHourOfDay = calendar.get(Calendar.HOUR_OF_DAY) + 1;
                    cMinute = 0;
                    String time = "";
                    if (cHourOfDay != 24) {
                        time = String.valueOf(cHourOfDay + ":" + "00");
                    } else {
                        time = "00" + ":" + "00";
                    }
                    timeTextView.setText(time);
                } else {
                    timeTextView.setText("Time not set");
                    dateTextView.setText("Date not set");
                    linearLayout.setVisibility(View.GONE);
                }
            }
        });
    }

    public void setNotification(String task) {
        calendar = Calendar.getInstance();
        calendar.set(Calendar.YEAR, cYear);
        calendar.set(Calendar.MONTH, cMonth);
        calendar.set(Calendar.DAY_OF_MONTH, cDayOfMonth);

        calendar.set(Calendar.HOUR_OF_DAY, cHourOfDay);
        calendar.set(Calendar.MINUTE, cMinute);
        calendar.set(Calendar.SECOND, 0);
        AlarmManager alarmManager = (AlarmManager) getSystemService(Context.ALARM_SERVICE);
        Intent intent = new Intent(getApplicationContext(), TaskNotification.class);
        intent.putExtra("task", task);
        intent.putExtra("timeInMillis", calendar.getTimeInMillis());
        intent.putExtra("reqCode", reqCode);
        Task dbTask = realm.where(Task.class).equalTo("title", task).findFirst();
        reqCode = dbTask.getId();
        PendingIntent pendingIntent = PendingIntent.getBroadcast(getApplicationContext(), reqCode, intent, 0);
        alarmManager.set(alarmManager.RTC_WAKEUP, calendar.getTimeInMillis(), pendingIntent);
    }

    @Override
    public void onTimeSet(TimePicker view, int hourOfDay, int minute) {
        String min = String.valueOf(minute);
        if (minute < 10) {
            min = "0" + min;
        }

        if (switchNotify.isChecked()) {
            timeTextView.setText(hourOfDay + ": " + min);
            cHourOfDay = hourOfDay;
            cMinute = minute;
        }
    }

    @Override
    public void onDateSet(DatePicker view, int year, int month, int dayOfMonth) {
        if (switchNotify.isChecked()) {
            dateTextView.setText(year + "-" + (month + 1) + "-" + dayOfMonth);
            cYear = year;
            cMonth = month;
            cDayOfMonth = dayOfMonth;
        }
    }

    public void addOrUpdateTask(View view) {
        String taskToInsertOrUpdate = String.valueOf(taskEditTxt.getText());
        if (!getIntent().getStringExtra("taskText").equals("")) {
            RealmResults<Task> results = realm.where(Task.class).findAll().sort("id", Sort.ASCENDING);
            int position = getIntent().getIntExtra("position", 0);
            Task dbTask = results.get(position);
            realm.beginTransaction();
            dbTask.setTitle(taskToInsertOrUpdate);
            realm.commitTransaction();
        } else {

            realm.beginTransaction();
            Number currentIdNum = realm.where(Task.class).max("id");
            Task task = new Task();
            if (currentIdNum == null) {
                task.setId(1);
            } else {
                task.setId(currentIdNum.intValue() + 1);
            }
            task.setTitle(taskToInsertOrUpdate);
            realm.copyToRealm(task);
            realm.commitTransaction();
        }
        if (!timeTextView.getText().toString().equals("Time not set")) {
            setNotification(taskToInsertOrUpdate);
        }
    }
}
