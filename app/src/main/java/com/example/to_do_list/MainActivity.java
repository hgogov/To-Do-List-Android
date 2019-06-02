package com.example.to_do_list;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Intent;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;

import com.example.to_do_list.db.Task;

import java.util.ArrayList;

import io.realm.Realm;
import io.realm.RealmResults;
import io.realm.Sort;

public class MainActivity extends AppCompatActivity {

    private ListView mTaskListView;
    private ArrayAdapter<String> mAdapter;
    private ArrayList<String> taskList = new ArrayList<>();
    private Realm realm;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        realm = Realm.getDefaultInstance();
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        mTaskListView = findViewById(android.R.id.list);

        FloatingActionButton fab = findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(getApplicationContext(), AddTaskActivity.class);
                intent.putExtra("taskText", "");
                startActivity(intent);
            }
        });

        mTaskListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                Intent intent = new Intent(getApplicationContext(), AddTaskActivity.class);
                intent.putExtra("taskText", String.valueOf(taskList.get(position)));
                intent.putExtra("position", position);
                startActivity(intent);
            }
        });
        //updateUI();
    }

    @Override
    protected void onResume() {
        super.onResume();
        updateUI();
    }

    public void deleteTask(View view) {
        View parent = (View) view.getParent();
        TextView taskTextView = parent.findViewById(R.id.task_title);
        Intent intent = new Intent(getApplicationContext(), TaskNotification.class);
        RealmResults<Task> results = realm.where(Task.class).findAll().sort("id", Sort.ASCENDING);
        int position = mTaskListView.getPositionForView(taskTextView);
        Task dbTask = results.get(position);
        int reqCode = dbTask.getId();
        PendingIntent pendingIntent = PendingIntent.getBroadcast(getApplicationContext(), reqCode, intent, PendingIntent.FLAG_UPDATE_CURRENT);
        if (AddTaskActivity.ALARM_SERVICE != null) {
            String as = AddTaskActivity.ALARM_SERVICE;
            AlarmManager alarmManager = (AlarmManager) getApplicationContext().getSystemService(as);
            alarmManager.cancel(pendingIntent);
        }

        realm.beginTransaction();
        dbTask.deleteFromRealm();
        realm.commitTransaction();
        updateUI();
    }

    public void updateUI() {
        taskList = new ArrayList<>();

        RealmResults<Task> result = realm.where(Task.class).findAll().sort("id", Sort.ASCENDING);
        for (Task task : result) {
            taskList.add(task.getTitle());
        }

        if (mAdapter == null) {
            mAdapter = new ArrayAdapter<>(this, //populate the mTaskListView with data
                    R.layout.item_todo, // the view to use for the items
                    R.id.task_title, // where to put the String of data
                    taskList); // where to get all the data
            mTaskListView.setAdapter(mAdapter); //set mAdapter as the adapter of mTaskListView
        } else { //If the adapter is already created repaint the view on the screen with the new data
            mAdapter.clear();
            mAdapter.addAll(taskList);
            mAdapter.notifyDataSetChanged();
        }
    }
}
