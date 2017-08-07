package engineering.reverse.ludumcomments;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.media.RingtoneManager;
import android.support.v4.app.NotificationCompat;
import android.util.Log;

import com.android.volley.DefaultRetryPolicy;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;

/**
 * Created by cogito on 8/1/17.
 */

public class NotificationTimer extends BroadcastReceiver {
    @Override
    public void onReceive(final Context context, Intent intent) {
        RequestQueue queue;
        queue = Volley.newRequestQueue(context.getApplicationContext());
        final ArrayList<CommentData> commentDatas = new ArrayList<CommentData>();
        final SharedPreferences prefs = context.getSharedPreferences("data", Context.MODE_PRIVATE);

        final String vx = prefs.getString("vx", null);
        final String note = prefs.getString("note", null);
        final String node = prefs.getString("node", null);
        final int gameno = prefs.getInt("gameno", -1);
        if (vx == null || note == null || node == null || gameno == -1) {
            Log.d("PREFS", "Values not loaded from prefs. Fatal error");
            return;
        }
        Log.d("PREFS", node + " " + note + " " + vx + " Game #" + gameno);

        final String noteUrl = "https://api.ldjam.com/" + vx + "/" + note + "/get/" + gameno;
        final String nodeUrl = "https://api.ldjam.com/" + vx + "/" + node + "/get/" + gameno;

        Log.d("LOAD", noteUrl);
        Log.d("LOAD", nodeUrl);
        final Data data = new Data();

        JsonObjectRequest noteRequest =
                new JsonObjectRequest(
                        noteUrl,
                        null,
                        new Response.Listener<JSONObject>() {
                            @Override
                            public void onResponse(JSONObject response) {
                                try {
                                    boolean newRating = false;
                                    JSONArray jsonArray = response.getJSONArray(note);
                                    int comments = prefs.getInt("comments", -1);
                                    int newComments = jsonArray.length();
                                    if (newComments > comments)
                                        data.addedComments = newComments - comments;
                                } catch (JSONException e) {
                                    e.printStackTrace();
                                }
                            }
                        }, new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {
                        error.printStackTrace();
                    }
                });
        JsonObjectRequest nodeRequest =
                new JsonObjectRequest(
                        nodeUrl,
                        null,
                        new Response.Listener<JSONObject>() {
                            @Override
                            public void onResponse(JSONObject response) {
                                try {
                                    JSONArray jsonArray = response.getJSONArray("node");
                                    JSONObject jsonObjecter = jsonArray.getJSONObject(0);
                                    jsonObjecter = jsonObjecter.getJSONObject("magic");
                                    double grade = jsonObjecter.getDouble("grade");
                                    if (grade > prefs.getFloat("grade", 0)) {
                                        data.grade = grade;
                                    }
                                    addNotification(context.getApplicationContext(), data);
                                } catch (JSONException e) {
                                    e.printStackTrace();
                                }
                            }
                        }, new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {
                        error.printStackTrace();
                    }
                });

        noteRequest.setRetryPolicy(VolleyUtils.getRetryPolicy());
        nodeRequest.setRetryPolicy(VolleyUtils.getRetryPolicy());

        queue.add(noteRequest);
        queue.add(nodeRequest);
    }

    private void addNotification(Context context, Data data) {
        String commentText = "", gradeText = "";
        String notifText = "";
        if (data.addedComments > 0)
            commentText = data.addedComments + " new comments and ";
        if (data.grade > 0)
            gradeText = data.grade + " ratings";
        if (commentText == null && gradeText == null)
            return;
        notifText = commentText + gradeText + " received!";
        NotificationCompat.Builder notificationBuilder = new NotificationCompat.Builder(context)
                .setContentTitle("Updates on your LD entry")
                .setContentText(notifText)
                .setSmallIcon(R.mipmap.ic_launcher_round)
                .setPriority(Notification.PRIORITY_HIGH)
                .setAutoCancel(true)
                .setCategory(Notification.CATEGORY_SOCIAL)
                .setSound(
                        RingtoneManager.getActualDefaultRingtoneUri(
                                context, RingtoneManager.TYPE_NOTIFICATION));
        Intent intent = new Intent(context.getApplicationContext(), MainActivity.class);
        PendingIntent resultPendingIntent = PendingIntent.
                getActivity(
                        context,
                        12,
                        intent,
                        PendingIntent.FLAG_ONE_SHOT);
        notificationBuilder.setContentIntent(resultPendingIntent);
        NotificationManager manager = (NotificationManager) context.
                getSystemService(
                        Context.NOTIFICATION_SERVICE);
        manager.notify(234,notificationBuilder.build());

    }

    class Data {
        int addedComments = -1;
        double grade = -1;
    }
}