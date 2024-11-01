package com.example.ip_calculadora;
import androidx.appcompat.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.regex.Pattern;

public class MainActivity extends AppCompatActivity {
    private EditText ipEditText, mascaraEditText;
    private TextView resultadoTextView;

    // Regex para una IP válida (IPv4)
    private static final Pattern IP_ADDRESS
            = Pattern.compile(
            "^((25[0-5]|2[0-4][0-9]|[0-1]?[0-9][0-9]?)\\.){3}"
                    + "(25[0-5]|2[0-4][0-9]|[0-1]?[0-9][0-9]?)$");

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
        String direccionIP = ipEditText.getText().toString().trim();
        String mascaraSubredStr = mascaraEditText.getText().toString().trim();

        if (direccionIP.isEmpty()) {
            resultadoTextView.setText(""); // Limpiar salida
            Toast.makeText(this, "Por favor ingrese la dirección IP.", Toast.LENGTH_SHORT).show();
            return;
        }

        if (!isValidInetAddress(direccionIP)) {
            resultadoTextView.setText(""); // Limpiar salida
            Toast.makeText(this, "Por favor ingrese una dirección IP válida.", Toast.LENGTH_SHORT).show();
            return;
        }

        if (mascaraSubredStr.isEmpty()) {
            resultadoTextView.setText(""); // Limpiar salida
            Toast.makeText(this, "Por favor ingrese el número de bits de la máscara de subred.", Toast.LENGTH_SHORT).show();
            return;
        }

        int bitsMascaraSubred;
        try {
            bitsMascaraSubred = Integer.parseInt(mascaraSubredStr);
        } catch (NumberFormatException e) {
            resultadoTextView.setText(""); // Limpiar salida
            Toast.makeText(this, "La máscara de subred debe ser un número entero.", Toast.LENGTH_SHORT).show();
            return;
        }

        if (bitsMascaraSubred < 0 || bitsMascaraSubred > 32) {
            resultadoTextView.setText(""); // Limpiar salida
            Toast.makeText(this, "La máscara de subred debe estar entre 0 y 32.", Toast.LENGTH_SHORT).show();
            return;
        }

        try {
            InetAddress direccionInetAddress = InetAddress.getByName(direccionIP);
            byte[] bytesDireccionIP = direccionInetAddress.getAddress();

            byte[] bytesMascaraSubred = calcularMascaraSubred(bitsMascaraSubred);

            InetAddress networkID = calcularNetworkID(bytesDireccionIP, bytesMascaraSubred);
            InetAddress broadcast = calcularIPBroadcast(bytesDireccionIP, bytesMascaraSubred);
            InetAddress siguienteNetworkID = calcularSiguienteNetworkID(broadcast.getAddress());

            String claseIP = determinarClaseIP(bytesDireccionIP);
            int bitsMascaraPorOmision = obtenerMascaraPorOmision(claseIP);
            int cantidadSubredes = calcularCantidadSubredes(bitsMascaraSubred, bitsMascaraPorOmision);
            int cantidadDireccionesIP = calcularCantidadDireccionesIP(bitsMascaraSubred);

            String resultado = "Clase de la IP: \n" + claseIP + "\n"
                    + "Network ID: \n" + networkID.getHostAddress() + "\n"
                    + "Broadcast: \n" + broadcast.getHostAddress() + "\n"
                    //+ "Network ID Siguiente: \n" + siguienteNetworkID.getHostAddress() + "\n"
                    + "Cantidad de subredes disponibles: \n" + cantidadSubredes + "\n"
                    + "Cantidad de direcciones IP disponibles: \n" + cantidadDireccionesIP + "\n";

            resultadoTextView.setText(resultado);

            //imprimirRangoIP(networkID.getAddress(), broadcast.getAddress());
        } catch (UnknownHostException e) {
            resultadoTextView.setText(""); // Limpiar salida
            resultadoTextView.setText("Error: Dirección IP no válida");
        }
    }

    private static boolean isValidInetAddress(String ip) {
        return IP_ADDRESS.matcher(ip).matches();
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

    private String determinarClaseIP(byte[] bytesDireccionIP) {
        int primerOcteto = bytesDireccionIP[0] & 0xFF;

        if (primerOcteto >= 1 && primerOcteto <= 126) {
            return "Clase A";
        } else if (primerOcteto >= 128 && primerOcteto <= 191) {
            return "Clase B";
        } else if (primerOcteto >= 192 && primerOcteto <= 223) {
            return "Clase C";
        } else if (primerOcteto >= 224 && primerOcteto <= 239) {
            return "Clase D (Multicast)";
        } else if (primerOcteto >= 240 && primerOcteto <= 255) {
            return "Clase E (Reservada)";
        } else {
            return "Clase Desconocida";
        }
    }

    private int obtenerMascaraPorOmision(String claseIP) {
        switch (claseIP) {
            case "Clase A":
                return 8;
            case "Clase B":
                return 16;
            case "Clase C":
                return 24;
            default:
                return 0;
        }
    }

    private int calcularCantidadSubredes(int bitsMascaraSubred, int bitsMascaraPorOmision) {
        if (bitsMascaraSubred <= bitsMascaraPorOmision) {
            return 1; // No se crean subredes si la máscara propuesta es igual o menor a la por omisión
        }
        return (int) Math.pow(2, bitsMascaraSubred - bitsMascaraPorOmision);
    }

    private int calcularCantidadDireccionesIP(int bitsMascaraSubred) {
        int bitsHost = 32 - bitsMascaraSubred;
        return (int) Math.pow(2, bitsHost) - 2;
    }
}
