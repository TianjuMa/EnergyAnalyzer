package thehabitslab.com.codebase;

import android.content.Context;
import android.database.Cursor;
import android.os.AsyncTask;
import android.provider.Settings;
import android.util.Log;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.*;
import java.net.*;
/**
 * Sends the energy data to the back end when requested.
 * The Replicator keeps track of when it is replicating and does not replicate multiple times.
 * The replicated data is deleted from the local database.
 * To replicate, this class should be instantiated and execute() should be called.
 * <p/>
 * Students are required to write the meat of the transmission of data to the back end.
 * <p/>
 * Created by William on 12/30/2016.
 */
public class Replicator extends AsyncTask<Void, Void, Object> {
    private static final String TAG = "Replicator";
    private static boolean isReplicating = false;
    private boolean isCanceled = false;

    private Context context;

    public Replicator(Context context) {
        this.context = context;
    }

    @Override
    /**
     * When execute() is called, this happens first
     */
    protected void onPreExecute() {
        isCanceled = isReplicating;
        isReplicating = true;
    }

    @Override
    /**
     * When execute() is called, this happens second
     */
    protected Void doInBackground(Void... params) {
        // Don't do anything if the execution is canceled
        if (!isCanceled) {
            // Query the database and package the data
            Cursor c = EnergyDBHelper.getFirst60Entries(context);
            int timeCol = c.getColumnIndex(EnergyDBHelper.EnergyEntry.COLUMN_NAME_TIME);
            int energyCol = c.getColumnIndex(EnergyDBHelper.EnergyEntry.COLUMN_NAME_ENERGY);
            String date = "";
            Double ene = 0.0;
            if (c != null) {
                c.moveToFirst();
                date = c.getString(1);
                ene = c.getDouble(0);
            }
            String id = Settings.Secure.getString(context.getContentResolver(),
                    Settings.Secure.ANDROID_ID);

            c.moveToFirst();
            OutputStreamWriter writer = null;
            BufferedReader reader;
            while (!c.isAfterLast()) {
                date = c.getString(timeCol);
                ene = c.getDouble(energyCol);
                try {
                    // TODO: make an HttpURLConnection and send data as parameters in a POST (one at a time)
                    //throw new UnsupportedOperationException("Not yet implemented");
                    URL url = new URL("http://murphy.wot.eecs.northwestern.edu/~tml5872/SQLGateway.py");
                    String data = "mac=" + id;
                    data += "&" + URLEncoder.encode("time", "UTF-8")
                            + "=" + URLEncoder.encode(date, "UTF-8");

                    data += "&" + URLEncoder.encode("energy", "UTF-8") + "="
                            + URLEncoder.encode(Double.toString(ene), "UTF-8");
                    URLConnection conn = url.openConnection();
                    conn.setDoOutput(true);
                    writer = new OutputStreamWriter(conn.getOutputStream());
                    writer.write(data);
                    Log.v(TAG, data);
                    writer.flush();
                    reader = new BufferedReader(new InputStreamReader(conn.getInputStream()));
                    StringBuilder sb = new StringBuilder();
                    String line = null;
                    String text;
                    // Read Server Response
                    while ((line = reader.readLine()) != null) {
                        // Append server response in string
                        sb.append(line + "\n");
                    }


                    text = sb.toString();
                } catch (Exception ex) {

                } finally {
                    try {
                        if (writer != null)
                            writer.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
                c.moveToNext();
            }
            EnergyDBHelper.deleteNEntries(context, 60);
            Log.v(TAG, "Deletion is successful.");
        }
        return null;
    }

    @Override
    /**
     * When execute is called, this happens third
     */
    protected void onPostExecute(Object result) {
        isReplicating = false;
    }

}