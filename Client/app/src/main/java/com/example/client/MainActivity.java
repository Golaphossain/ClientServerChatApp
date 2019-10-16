package com.example.client;

import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.net.UnknownHostException;

public class MainActivity extends AppCompatActivity{

    static final int SocketServerPORT = 8000;

    LinearLayout loginPanel, chatPanel;

    EditText editTextUserName, editTextAddress;
    Button buttonConnect;
    TextView chatMsg,chatMsg2, textPort;

    EditText editTextSay;
    Button buttonSend;
//    Button buttonDisconnect;

    String msgLog = "";

    ChatClientThread chatClientThread = null;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        //getSupportActionBar();
        loginPanel = findViewById(R.id.loginpanel);
        chatPanel = findViewById(R.id.chatpanel);

        editTextUserName =  findViewById(R.id.username);
        editTextAddress =  findViewById(R.id.address);
        textPort =  findViewById(R.id.port);
        textPort.setText("port: " + SocketServerPORT);
        buttonConnect =  findViewById(R.id.connect);
//        buttonDisconnect =  findViewById(R.id.disconnect);
        chatMsg =  findViewById(R.id.chatmsg);
        chatMsg2 =  findViewById(R.id.chatmsg2);

        buttonConnect.setOnClickListener(buttonConnectOnClickListener);
//        buttonDisconnect.setOnClickListener(buttonDisconnectOnClickListener);
//
        editTextSay = findViewById(R.id.say);
        buttonSend = findViewById(R.id.send);

        buttonSend.setOnClickListener(buttonSendOnClickListener);

        ActionBar actionBar = getSupportActionBar();



        if (chatPanel.getVisibility() == View.GONE){
            assert actionBar != null;
            actionBar.hide();
        }


    }
//    public void Disconnect(MenuItem menuItem){
//        if(chatClientThread==null){
//                return;
//            }
//            chatClientThread.disconnect();
//    }

//    View.OnClickListener buttonDisconnectOnClickListener = new View.OnClickListener() {
//
//        @Override
//        public void onClick(View v) {
//            if(chatClientThread==null){
//                return;
//            }
//            chatClientThread.disconnect();
//        }
//
//    };

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

    View.OnClickListener buttonConnectOnClickListener = new View.OnClickListener() {

        @Override
        public void onClick(View v) {
            String textUserName = editTextUserName.getText().toString();

            if (textUserName.equals("")) {
                Toast.makeText(MainActivity.this, "Enter User Name",
                        Toast.LENGTH_LONG).show();
                return;
            }

            String textAddress = editTextAddress.getText().toString();
            if (textAddress.equals("")) {
                Toast.makeText(MainActivity.this, "Enter Address",
                        Toast.LENGTH_LONG).show();
                return;
            }

            msgLog = "";
            chatMsg2.setText(msgLog);
            loginPanel.setVisibility(View.GONE);
            chatPanel.setVisibility(View.VISIBLE);

            chatClientThread = new ChatClientThread(
                    textUserName, textAddress, SocketServerPORT);
            chatClientThread.start();
            ActionBar actionBar = getSupportActionBar();
            assert actionBar != null;
            actionBar.show();
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

        @Override
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

                        MainActivity.this.runOnUiThread(() -> chatMsg2.setText(msgLog));
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
                        Toast.makeText(MainActivity.this, "please Load server side first", Toast.LENGTH_LONG).show();
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
            Toast.makeText(MainActivity.this,"Please write first",Toast.LENGTH_SHORT).show();
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_layout, menu);
        return true;
    }
}
