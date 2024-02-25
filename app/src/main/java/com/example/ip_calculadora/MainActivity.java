package com.example.ip_calculadora;
import androidx.appcompat.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import java.net.InetAddress;
import java.net.UnknownHostException;

public class MainActivity extends AppCompatActivity {
        private EditText ipEditText, mascaraEditText;
        private TextView resultadoTextView;

        @Override
        protected void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            setContentView(R.layout.activity_main);

            ipEditText = findViewById(R.id.ipEditText);
            mascaraEditText = findViewById(R.id.mascaraEditText);
            resultadoTextView = findViewById(R.id.resultadoTextView);

            Button calcularButton = findViewById(R.id.calcularButton);
            calcularButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {

                    calcularIP();
                }
            });
        }

    private void calcularIP() {
        String direccionIP = ipEditText.getText().toString();
        int bitsMascaraSubred = Integer.parseInt(mascaraEditText.getText().toString());

        try {
            InetAddress direccionInetAddress = InetAddress.getByName(direccionIP);
            byte[] bytesDireccionIP = direccionInetAddress.getAddress();

            byte[] bytesMascaraSubred = calcularMascaraSubred(bitsMascaraSubred);

            InetAddress networkID = calcularNetworkID(bytesDireccionIP, bytesMascaraSubred);
            InetAddress broadcast = calcularIPBroadcast(bytesDireccionIP, bytesMascaraSubred);
            InetAddress siguienteNetworkID = calcularSiguienteNetworkID(broadcast.getAddress());

            String resultado = "Network ID: \n" + networkID.getHostAddress() + "\n"
                    + "Broadcast: \n" + broadcast.getHostAddress() + "\n"
                    + "Network ID Siguiente: \n" + siguienteNetworkID.getHostAddress() + "\n" ;

            resultadoTextView.setText(resultado);

            imprimirRangoIP(networkID.getAddress(), broadcast.getAddress());
        } catch (UnknownHostException e) {
            resultadoTextView.setText("Error: Dirección IP no válida");
        }
    }

    private void imprimirRangoIP(byte[] bytesNetworkID, byte[] bytesBroadcast) throws UnknownHostException {
        byte[] bytesPrimeraIP = bytesNetworkID.clone();
        bytesPrimeraIP[3] += 1;
        InetAddress primeraIP = InetAddress.getByAddress(bytesPrimeraIP);

        byte[] bytesUltimaIP = bytesBroadcast.clone();
        bytesUltimaIP[3] -= 1;
        InetAddress ultimaIP = InetAddress.getByAddress(bytesUltimaIP);

        String rangoIP = "Rango de IP: \n" + primeraIP.getHostAddress() + " - " + ultimaIP.getHostAddress();
        resultadoTextView.setTextSize(18);
        resultadoTextView.append(rangoIP);
    }


    private static byte[] calcularMascaraSubred(int bitsMascaraSubred) {
        int mascaraSubred = 0xFFFFFFFF << (32 - bitsMascaraSubred);
        return new byte[]{
                (byte) ((mascaraSubred >> 24) & 0xFF),
                (byte) ((mascaraSubred >> 16) & 0xFF),
                (byte) ((mascaraSubred >> 8) & 0xFF),
                (byte) (mascaraSubred & 0xFF)
        };
    }

    private static InetAddress calcularNetworkID(byte[] bytesDireccionIP, byte[] bytesMascaraSubred) throws UnknownHostException {
        byte[] bytesNetworkID = new byte[bytesDireccionIP.length];
        for (int i = 0; i < bytesDireccionIP.length; i++) {
            bytesNetworkID[i] = (byte) (bytesDireccionIP[i] & bytesMascaraSubred[i]);
        }
        return InetAddress.getByAddress(bytesNetworkID);
    }

    private static InetAddress calcularIPBroadcast(byte[] bytesDireccionIP, byte[] bytesMascaraSubred) throws UnknownHostException {
        byte[] bytesBroadcast = new byte[bytesDireccionIP.length];
        for (int i = 0; i < bytesDireccionIP.length; i++) {
            bytesBroadcast[i] = (byte) ((bytesDireccionIP[i] & bytesMascaraSubred[i]) | ~bytesMascaraSubred[i]);
        }
        return InetAddress.getByAddress(bytesBroadcast);
    }

    private static InetAddress calcularSiguienteNetworkID(byte[] bytesBroadcast) throws UnknownHostException {
        byte[] bytesSiguienteNetworkID = bytesBroadcast.clone();
        bytesSiguienteNetworkID[3] += 1;
        return InetAddress.getByAddress(bytesSiguienteNetworkID);
    }


}