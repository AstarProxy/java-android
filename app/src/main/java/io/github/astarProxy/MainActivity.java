package io.github.astarProxy;

import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;


public class MainActivity extends AppCompatActivity {
    Preferences preferences;
    EditText edtPass, edtUser, edtServer, edtPort;
    Button connectBtn;
    boolean flagPress = false;
    AsyncTask<Void, Void, Void> background;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.first_layout);
        edtPass = (EditText) findViewById(R.id.edt_pass);
        edtPort = (EditText) findViewById(R.id.edt_port);
        edtServer = (EditText) findViewById(R.id.edt_server);
        edtUser = (EditText) findViewById(R.id.edt_user);
        connectBtn = (Button) findViewById(R.id.btn_connect);
        preferences = new Preferences();


        /*
         get data from preferences
         */
        edtUser.setText(preferences.getPrefUser(MainActivity.this));
        edtPass.setText(preferences.getPrefPass(MainActivity.this));
        edtPort.setText(String.valueOf(preferences.getPrefPort(MainActivity.this)));
        edtServer.setText(preferences.getPrefServer(MainActivity.this));





        /*
        get info from user and save it
        */
        connectBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                if(!(edtPass.getText().toString().equals(""))&&
                        !(edtPort.getText().toString().equals(""))&&
                      !(edtUser.getText().toString().equals(""))&&
                       !(edtServer.getText().toString().equals(""))){
                    int c=Integer.valueOf(edtPort.getText().toString());

                    if( 1<c && c<(Math.pow(2,16))){
                        preferences.setPrefUser(edtUser.getText().toString(), MainActivity.this);
                        preferences.setPrefPass(edtPass.getText().toString(), MainActivity.this);
                        preferences.setPrefPort(Integer.parseInt(edtPort.getText().toString()), MainActivity.this);
                        preferences.setPrefServer(edtServer.getText().toString(), MainActivity.this);


                        if (!flagPress) {
                            connectBtn.setText(R.string.stop);
                            flagPress = true;
                            runServer();
                        } else {
                            connectBtn.setText(R.string.connect);
                            flagPress = false;
                            stopServer();
                        }
                    }
                    else{
                        Toast.makeText(getApplicationContext(), "پورت وارد شده نا معتبر است", Toast.LENGTH_SHORT).show();
                    }
                    }
                    else{
                    Toast.makeText(getApplicationContext(), "پر کردن فیلد الزامی است", Toast.LENGTH_SHORT).show();
                }




            }
        });
//
    }

    private void runServer() {
        if (background == null) {
            background = new AsyncTask<Void, Void, Void>() {

                @Override
                protected Void doInBackground(Void... params) {
                    SocksServer server = new SocksServer(10801);
                    server.setUpstreamProxy(new SocksServer.UpstreamProxy() {
                        public ProxyType getType() {
                            return ProxyType.Astar;
                        }

                        public String getHostname() {
                            //return "178.33.243.216";
                            return preferences.getPrefServer(MainActivity.this);
                        }

                        public int getPort() {
                           // return 8080;
                            return preferences.getPrefPort(MainActivity.this);
                        }

                        public String getUsername() {
                         //   return "guest-user";
                             return preferences.getPrefUser(MainActivity.this);
                        }

                        public String getPassword() {
                            //return "139702";
                            return preferences.getPrefPass(MainActivity.this);
                        }
                    });
                    server.run();
                    return null;
                }
            }.execute();
        }
        // background.execute();
    }

    private void stopServer() {
        if (background != null) {
            background.cancel(true);
        }
    }
}


