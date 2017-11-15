package br.com.ssti.androidmqtt;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;
import android.widget.ViewSwitcher;

import org.eclipse.paho.android.service.MqttAndroidClient;
import org.eclipse.paho.client.mqttv3.*;

import java.io.UnsupportedEncodingException;


public class MainActivity extends AppCompatActivity {

    private MqttAndroidClient client;

    private ViewSwitcher      switcher;

    private Button bL;
    private Button bD;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        switcher = (ViewSwitcher)findViewById(R.id.switcher);

        bL = (Button)findViewById(R.id.ligar);
        bD = (Button)findViewById(R.id.desligar);

        connectMQTTClient();
    }

    private void connectMQTTClient() {
        String clientId = MqttClient.generateClientId();
        client =
                new MqttAndroidClient(this.getApplicationContext(),
                        MQTTConstantes.MQTT_SERVICE_URI,
                        clientId);
        try {
            IMqttToken token = client.connect();
            token.setActionCallback(new IMqttActionListener() {
                @Override
                public void onSuccess(IMqttToken asyncActionToken) {
                    subscribeIn(MQTTConstantes.TOPICO_LAMPADA);
                }

                @Override
                public void onFailure(IMqttToken asyncActionToken, Throwable exception) {
                    Toast.makeText(MainActivity.this, exception.getMessage(), Toast.LENGTH_LONG).show();
                }
            });

            client.setCallback(new MqttCallback() {
                @Override
                public void connectionLost(Throwable cause) {
                }

                @Override
                public void messageArrived(String topic, MqttMessage message) throws Exception {
                    if (topic.equals(MQTTConstantes.TOPICO_LAMPADA)) {
                        if (message.toString().equals("1")) {
                            if (switcher.getDisplayedChild() != 1) {
                                switcher.showPrevious();
                                bL.setEnabled(false);
                                bD.setEnabled(true);
                            }

                        } else if (message.toString().equals("0")) {
                            if (switcher.getDisplayedChild() != 0) {
                                switcher.showNext();
                                bL.setEnabled(true);
                                bD.setEnabled(false);
                            }
                        }
                    }
                }

                @Override
                public void deliveryComplete(IMqttDeliveryToken token) {
                    Log.i("TAG", "Delivery complete");
                }
            });
        } catch (MqttException e) {
            e.printStackTrace();
        }
    }

    public void subscribeIn(String topico) {
        int qos = 1;
        try {
            IMqttToken subToken = client.subscribe(topico, qos);
            subToken.setActionCallback(new IMqttActionListener() {
                @Override
                public void onSuccess(IMqttToken asyncActionToken) {
                    switcher.setEnabled(true);
                }

                @Override
                public void onFailure(IMqttToken asyncActionToken,
                                      Throwable exception) {
                }
            });
        } catch (MqttException e) {
            e.printStackTrace();
        }
    }

    public void unsubscribeIn(String topico) {
        try {
            IMqttToken unsubToken = client.unsubscribe(topico);
            unsubToken.setActionCallback(new IMqttActionListener() {
                @Override
                public void onSuccess(IMqttToken asyncActionToken) {
                    switcher.setEnabled(false);
                }

                @Override
                public void onFailure(IMqttToken asyncActionToken,
                                      Throwable exception) {
                }
            });
        } catch (MqttException e) {
            e.printStackTrace();
        }
    }

    public void alterarStatus(View v) {
        if (switcher.getDisplayedChild() == 0) {
            ligar();
        } else {
            desligar();
        }
    }

    public void publicar(String topico, String mensagem) {
        byte[] encodedPayload;
        try {
            encodedPayload = mensagem.getBytes("UTF-8");
            MqttMessage message = new MqttMessage(encodedPayload);
            message.setRetained(true);
            client.publish(topico, message);
        } catch (UnsupportedEncodingException | MqttException e) {
            e.printStackTrace();
        }
    }

    public void ligar() {
        publicar(MQTTConstantes.TOPICO_LAMPADA, "1");
    }

    public void desligar() {
        publicar(MQTTConstantes.TOPICO_LAMPADA, "0");
    }

    public void ligar(View v){
        ligar();
    }

    public void desligar(View v){
        desligar();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        unsubscribeIn(MQTTConstantes.TOPICO_LAMPADA);
    }
}