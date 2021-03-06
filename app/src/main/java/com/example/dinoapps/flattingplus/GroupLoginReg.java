package com.example.dinoapps.flattingplus;

import android.content.Intent;
import android.database.Cursor;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.android.volley.AuthFailureError;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.VolleyLog;
import com.android.volley.toolbox.JsonArrayRequest;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;
import com.google.firebase.iid.FirebaseInstanceId;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import static android.R.attr.button;

public class GroupLoginReg extends AppCompatActivity {
    private String TAG = "GroupLogin";
    EditText groupname;
    EditText grouppass;
    TextView errorText;
    //    static DBHelper dbHelper;
    RequestQueue queue;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_group_login_reg);
        queue = Volley.newRequestQueue(this);
        groupname = (EditText) findViewById(R.id.groupNameText);
        grouppass = (EditText) findViewById(R.id.groupPassword);
        errorText = (TextView) findViewById(R.id.textView9);
        errorText.setVisibility(View.INVISIBLE);
        Button loginButton = (Button) findViewById(R.id.buttonLoginG);
        loginButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                if (groupname.getText().toString().equals("") || grouppass.getText().toString().equals("")) {
                    Log.v(TAG, "Password or group name is empty");
                    errorText.setText("Please make sure that both name and password are not empty and try again");
                    errorText.setVisibility(View.VISIBLE);
                } else {
                    String groupName = groupname.getText().toString();
                    String groupPassword = grouppass.getText().toString();
                    ArrayList<String> info = getAllUserInfo();
                    JSONRequestLogin("/get/flatgroup/add", groupName, groupPassword, info.get(2));
                }
            }
        });


        Button registerButton = (Button) findViewById(R.id.buttonRegG);
        registerButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                Log.v(TAG, "reg button clicked");
                if (groupname.getText().toString().equals("") || grouppass.getText().toString().equals("")) {
                    Log.v(TAG, "Password or group name is empty");
                    errorText.setText("Please make sure that both name and password are not empty and try again");
                    errorText.setVisibility(View.VISIBLE);
                } else {
                    String groupName = groupname.getText().toString();
                    String groupPassword = grouppass.getText().toString();
                    ArrayList<String> info = getAllUserInfo();
                    addGroupTONetDB("/add/group", groupName, groupPassword, info.get(2), FirebaseInstanceId.getInstance().getToken());
                }
            }
        });
    }

    public void JSONRequestLogin(String route, String gname, String password, String email) {
        String baseURL = "https://flattingplus.herokuapp.com";
        String url = baseURL + route + "?gname=" + gname + "&pass=" + password + "&email=" + email + "&token=" + FirebaseInstanceId.getInstance().getToken();

        JsonArrayRequest getRequest = new JsonArrayRequest(Request.Method.GET, url, null,
                new Response.Listener<JSONArray>() {
                    @Override
                    public void onResponse(JSONArray response) {
                        try {
                            Log.v("Response:group log reg ", response.toString(4));

                            if (response.length() < 1)//No group found
                            {
                                errorText.setText("Groupname and/or password incorrect");
                                Toast.makeText(getApplicationContext(), "Groupname and/or password incorrect",
                                        Toast.LENGTH_LONG).show();
                            } else {

                                ArrayList<String> info = getAllUserInfo();

                                String groupName = groupname.getText().toString();
                                MainActivity.dbHelper.addGroupToUser(groupName, Integer.parseInt(info.get(0)));

                                //Add group info to local db
                                JSONObject jObj = response.getJSONObject(0);
                                String id = jObj.getString("id");
                                groupName = jObj.getString("groupname");
                                String notes = jObj.getString("notes");
                                String shoppinglist = jObj.getString("shoppinglist");
                                String calendar = jObj.getString("calendar");
                                String money = jObj.getString("money");
                                String pass = jObj.getString("password");

                                MainActivity.dbHelper.insertGroup(groupName, shoppinglist, calendar, money, notes, pass);

                                Toast.makeText(getApplicationContext(), "Signin successful!",
                                        Toast.LENGTH_LONG).show();
                                MainActivity.dbHelper.updateGroup(groupName, shoppinglist, calendar, money, notes, pass);

                                Log.v(TAG, "Name: " + groupName + " id: " + id);
                                //Add group to online user
                                info = getAllUserInfo();
                                updateUser(info);
                            }
                        } catch (JSONException e) {
                            e.printStackTrace();
                        }
                    }
                }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                VolleyLog.e("Error: ", error.getMessage());
            }
        });

        // add the request object to the queue to be executed
        queue.add(getRequest);
    }


    public void addGroupTONetDB(String route, String gname, String password, String userEmail, String fireToken)//Register group
    {
        Log.v(TAG, gname + " " + password);
        String baseURL = "https://flattingplus.herokuapp.com";
        String url = baseURL + route;

         /*Post data*/
        Map<String, String> jsonParams = new HashMap<String, String>();

        jsonParams.put("group", gname);
        jsonParams.put("gpass", password);
        jsonParams.put("email", userEmail);
        jsonParams.put("token", fireToken);
        Log.v(TAG, "Email: " + userEmail);

        JsonObjectRequest postRequest = new JsonObjectRequest(Request.Method.PUT, url, new JSONObject(jsonParams),
                new Response.Listener<JSONObject>() {
                    @Override
                    public void onResponse(JSONObject response) {
//                        try
//                        {
                        //Add group to online user
                        ArrayList<String> info = getAllUserInfo();

                        String groupName = groupname.getText().toString();
                        String groupPass = grouppass.getText().toString();

                        Log.v(TAG, groupName + " User id: " + info.get(0));
                        MainActivity.dbHelper.addGroupToUser(groupName, Integer.parseInt(info.get(0)));

                        //Add group to local db
                        MainActivity.dbHelper.insertGroup(groupName, "Empty", "Empty", "Empty", "Empty", groupPass);

                        info = getAllUserInfo();
                        Log.v(TAG, info.toString());
                        updateUser(info);
//
                    }
                },
                new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {
                        //   Handle Error
                        Log.v(TAG, "Error: " + error);
                    }
                }) {
            @Override
            public Map<String, String> getHeaders() throws AuthFailureError {
                HashMap<String, String> headers = new HashMap<String, String>();
                headers.put("Content-Type", "application/json; charset=utf-8");
                headers.put("User-agent", System.getProperty("http.agent"));
                return headers;
            }
        };
        queue.add(postRequest);
    }

    public void updateUser(ArrayList<String> info) {
        String id = info.get(0);
        String name = info.get(1);
        String email = info.get(2);
        String flatg = info.get(4);
        String pic = info.get(3);

        Log.v(TAG, name + " " + email + " " + flatg + " " + pic);
        String baseURL = "https://flattingplus.herokuapp.com";
        String url = baseURL + "/update/user";

         /*Post data*/
        Map<String, String> jsonParams = new HashMap<String, String>();

        JSONObject user = new JSONObject();
        try {
            user.put("email", email);
            user.put("name", name);
            user.put("group", flatg);
            user.put("pic", pic);

        } catch (JSONException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }

        JsonObjectRequest postRequest = new JsonObjectRequest(Request.Method.POST, url, user,
                new Response.Listener<JSONObject>() {
                    @Override
                    public void onResponse(JSONObject response) {
                        try {
                            Log.v(TAG, "updated user: " + response.toString(4));

                            //Need to pull down all the notes after the last edited time in our local db
                            Intent i= new Intent(getApplicationContext(), getService.class);
                            String groupname = MainActivity.dbHelper.getGroup();
                            String date = MainActivity.dbHelper.getMostRecentDate();
                            Log.v(TAG, "Groupname: " + groupname + " Date: " + date);
                            i.putExtra("groupname", groupname);
                            i.putExtra("date", date);
                            startService(i);

                            Intent home = new Intent(getApplicationContext(), MainActivity.class);
                            startActivity(home);
                        } catch (JSONException e) {
                            e.printStackTrace();
                        }

                    }
                },
                new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {
                        //   Handle Error
                        Log.v(TAG, "Error: " + error);
                    }
                }) {
            @Override
            public Map<String, String> getHeaders() throws AuthFailureError {
                HashMap<String, String> headers = new HashMap<String, String>();
                headers.put("Content-Type", "application/json; charset=utf-8");
                headers.put("User-agent", System.getProperty("http.agent"));
                return headers;
            }
        };
        queue.add(postRequest);

    }

    public ArrayList<String> getAllUserInfo() {
        Cursor cursor = MainActivity.dbHelper.getUser();
        ArrayList<String> info = new ArrayList<String>();
        if (cursor.moveToFirst()) {
            do {
                for (int i = 0; i < cursor.getColumnCount(); i++) {
                    info.add(cursor.getString(i));
                }
            } while (cursor.moveToNext());
        }
        return info;
    }


}
