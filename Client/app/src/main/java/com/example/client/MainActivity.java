package com.example.client;

import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;

import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.net.Socket;
import java.net.UnknownHostException;

public class MainActivity extends AppCompatActivity{


    private static final int REQUEST_EXTERNAL_STORAGE = 1;
    private static String[] PERMISSIONS_STORAGE = {Manifest.permission.READ_EXTERNAL_STORAGE,Manifest.permission.WRITE_EXTERNAL_STORAGE};
    private static final String TAG = "Message!";


    static final int SocketServerPORT = 8000;

    LinearLayout loginPanel,chatPanel;

    EditText editTextUserName,editTextAddress;
    Button buttonConnect;
    TextView chatMsg,chatMsg2,textPort;

    EditText editTextSay;
    Button buttonSend;
    String msgLog = "";
    ChatClientThread chatClientThread = null;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        verifyStoragePermissions();
        setContentView(R.layout.activity_main);



        loginPanel = findViewById(R.id.loginpanel);
        chatPanel = findViewById(R.id.chatpanel);

        editTextUserName =  findViewById(R.id.username);
        editTextAddress =  findViewById(R.id.address);
        textPort =  findViewById(R.id.port);
        textPort.setText("port: " + SocketServerPORT);
        buttonConnect =  findViewById(R.id.connect);
        chatMsg =  findViewById(R.id.chatmsg);
        chatMsg2 =  findViewById(R.id.chatmsg2);

        buttonConnect.setOnClickListener(buttonConnectOnClickListener);

        editTextSay = findViewById(R.id.say);
        buttonSend = findViewById(R.id.send);

        buttonSend.setOnClickListener(buttonSendOnClickListener);

        ActionBar actionBar = getSupportActionBar();
        if (chatPanel.getVisibility() == View.GONE){
            assert actionBar != null;
            actionBar.hide();
        }


    }

    View.OnClickListener buttonConnectOnClickListener = new View.OnClickListener() {

        @Override
        public void onClick(View v) {
            String textUserName = editTextUserName.getText().toString();

            if (textUserName.equals("")) {
                Toast.makeText(MainActivity.this, "Enter User Name",
                        Toast.LENGTH_LONG).show();
                return;
            }

            String serverAddress = editTextAddress.getText().toString();
            if (serverAddress.equals("")) {
                Toast.makeText(MainActivity.this, "Enter Address",
                        Toast.LENGTH_LONG).show();
                return;
            }

            msgLog = "";
//            chatMsg2.setText(msgLog);
            loginPanel.setVisibility(View.GONE);
            chatPanel.setVisibility(View.VISIBLE);

            chatClientThread = new ChatClientThread(
                    textUserName, serverAddress, SocketServerPORT);
            chatClientThread.start();
            ActionBar actionBar = getSupportActionBar();
            assert actionBar != null;
            actionBar.show();
        }

    };
    View.OnClickListener buttonSendOnClickListener = new View.OnClickListener() {

        @Override
        public void onClick(View v) {
            if (editTextSay.getText().toString().equals("")) {
                Toast.makeText(MainActivity.this,"Please write something",Toast.LENGTH_SHORT).show();
                return;
            }

            if(chatClientThread==null){
                return;
            }

            chatClientThread.sendMsg(editTextSay.getText().toString() + "\n");
        }

    };

    private class ChatClientThread extends Thread {

        String name;
        String dstAddress;
        int dstPort;

        String msgToSend = "";
        boolean goOut = false;

        ChatClientThread(String name, String address, int port) {
            this.name = name;
            dstAddress = address;
            dstPort = port;
        }


        public void run() {
            Socket socket = null;
            DataOutputStream dataOutputStream = null;
            DataInputStream dataInputStream = null;

            try {
                socket = new Socket(dstAddress, dstPort);
                dataOutputStream = new DataOutputStream(
                        socket.getOutputStream());
                dataInputStream = new DataInputStream(socket.getInputStream());
                dataOutputStream.writeUTF(name);
                dataOutputStream.flush();

                while (!goOut) {
                    if (dataInputStream.available() > 0) {
                        msgLog += dataInputStream.readUTF();

                        MainActivity.this.runOnUiThread(() -> {
                            chatMsg2.setText(msgLog);
                        });
                    }

                    if(!msgToSend.equals("")){
                        dataOutputStream.writeUTF(msgToSend);
                        dataOutputStream.flush();
                        msgToSend = "";

                    }
                }

            } catch (UnknownHostException e) {
                e.printStackTrace();
                final String eString = e.toString();
                MainActivity.this.runOnUiThread(() -> Toast.makeText(MainActivity.this, "Please Check your InternetConnection", Toast.LENGTH_LONG).show());
            } catch (IOException e) {
                e.printStackTrace();
                final String eString = e.toString();
                MainActivity.this.runOnUiThread(new Runnable() {

                    @Override
                    public void run() {
                        Toast.makeText(MainActivity.this, "please Load server application  first or check your Wifi Connection", Toast.LENGTH_LONG).show();
                    }

                });
            } finally {
                if (socket != null) {
                    try {
                        socket.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }

                if (dataOutputStream != null) {
                    try {
                        dataOutputStream.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }

                if (dataInputStream != null) {
                    try {
                        dataInputStream.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }

                MainActivity.this.runOnUiThread(new Runnable() {

                    @Override
                    public void run() {
                        loginPanel.setVisibility(View.VISIBLE);
                        chatPanel.setVisibility(View.GONE);
                    }

                });
            }

        }

        private void sendMsg(String msg){
            msgToSend = msg;
            MainActivity.this.runOnUiThread(() -> editTextSay.setText(""));
        }

        private void disconnect(){
            goOut = true;
            ActionBar actionBar = getSupportActionBar();
            assert actionBar != null;
            actionBar.hide();
        }
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {

        int id = item.getItemId();

        if(id == R.id.disconnect){
            if(chatClientThread==null){
                return super.onOptionsItemSelected(item);
            }
            chatClientThread.disconnect();
        }
        if (id == R.id.save){
            writeToFile("Messages", chatMsg2.getText().toString());

        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_layout, menu);
        return true;
    }


    private void writeToFile(String fileName, String data) {
        Long time= System.currentTimeMillis();
        String timeMill = time.toString();
        File defaultDir = Environment.getExternalStorageDirectory();
//        final File defaultDir = Environment.getExternalStoragePublicDirectory
//                        (
//                                //Environment.DIRECTORY_PICTURES
//                                 "/Chat message/"
//                        );

//         Make sure the path directory exists.
//        if(!defaultDir.exists())
//        {
//            // Make it, if it doesn't exit
//            defaultDir.mkdirs();
//        }
        File file = new File(defaultDir, fileName+timeMill+".txt");
        FileOutputStream stream;
        //OutputStreamWriter myOutWriter;
        try {
            stream = new FileOutputStream(file, false);
            stream.write(data.getBytes());
            stream.close();
//            stream = new FileOutputStream(file);
//             myOutWriter=new OutputStreamWriter(stream);
//             myOutWriter.append(data);
//             myOutWriter.close();
//             stream.flush();
//             stream.close();
            Toast.makeText(this, "File saved in: "+file.getPath(), Toast.LENGTH_SHORT).show();
        } catch (FileNotFoundException e) {
            Log.d(TAG, e.toString());
        } catch (IOException e) {
            Log.d(TAG, e.toString());
        }
    }


    public void verifyStoragePermissions() {
        // Check if we have write permission
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_DENIED) {
                requestPermissions(
                        PERMISSIONS_STORAGE,
                        REQUEST_EXTERNAL_STORAGE
                );
            }
        }
    }
}
