package com.example.ip_calculadora;

import android.text.InputFilter;
import android.text.Spanned;
import androidx.appcompat.app.AppCompatActivity;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import java.net.InetAddress;
import java.util.regex.Pattern;

public class MainActivity extends AppCompatActivity {
    private EditText ipEditText, mascaraEditText;
    private TextView resultadoTextView;

    // Regex para una IP válida (IPv4)
    private static final Pattern IP_ADDRESS = Pattern.compile(
            "^((25[0-5]|2[0-4][0-9]|[0-1]?[0-9][0-9]?)\\.){3}"
                    + "(25[0-5]|2[0-4][0-9]|[0-1]?[0-9][0-9]?)$");

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        ipEditText = findViewById(R.id.ipEditText);
        mascaraEditText = findViewById(R.id.mascaraEditText);
        resultadoTextView = findViewById(R.id.resultadoTextView);

        // Aplica el filtro a ambos EditText
        aplicarFiltro(ipEditText);
        aplicarFiltro(mascaraEditText);

        Button calcularButton = findViewById(R.id.calcularButton);
        Button limpiarButton = findViewById(R.id.limpiarButton);

        limpiarButton.setOnClickListener(view -> {
            ipEditText.setText(""); // Limpia el campo de dirección IP
            mascaraEditText.setText(""); // Limpia el campo de máscara de subred
            resultadoTextView.setText(""); // Limpia el resultado
        });

        calcularButton.setOnClickListener(v -> calcularIP());
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

        // Determinar la clase de la dirección IP y verificar si es clase D o E
        int[] ipArray;
        try {
            ipArray = parseIPAddress(direccionIP);
        } catch (Exception e) {
            resultadoTextView.setText("");
            Toast.makeText(this, e.getMessage(), Toast.LENGTH_SHORT).show();
            return;
        }

        char claseIP = determinarClaseIP(ipArray);
        if (claseIP == 'D' || claseIP == 'E') {
            resultadoTextView.setText(""); // Limpiar salida
            Toast.makeText(this, "No se pueden hacer cálculos con direcciones de clase D o E.", Toast.LENGTH_SHORT).show();
            return;
        }

        if (mascaraSubredStr.isEmpty() || !esMascaraSubredValida(mascaraSubredStr) || "255.255.255.255".equals(mascaraSubredStr)) {
            resultadoTextView.setText(""); // Limpiar salida
            Toast.makeText(this, "Por favor ingrese una máscara de subred válida.", Toast.LENGTH_SHORT).show();
            return;
        }

        try {
            int bitsMascaraSubred = contarBitsEnUno(mascaraSubredStr);
            int[] subnetMask = generateSubnetMask(bitsMascaraSubred);

            // Realizar los cálculos
            String networkID = calcularNetworkID(ipArray, subnetMask);
            String broadcastAddress = calcularBroadcast(ipArray, subnetMask);
            int cantidadDireccionesIP = calcularCantidadDireccionesIP(bitsMascaraSubred);
            int cantidadSubredes = calcularCantidadSubredes(direccionIP, String.valueOf(bitsMascaraSubred));

            String resultado = "Clase de la IP: \n" + claseIP + "\n"
                    + "Network ID: \n" + networkID + "\n"
                    + "Broadcast: \n" + broadcastAddress + "\n"
                    + "Cantidad de direcciones IP disponibles: \n" + cantidadDireccionesIP + "\n"
                    + "Cantidad de subredes disponibles: \n" + cantidadSubredes + "\n";

            resultadoTextView.setText(resultado);

        } catch (Exception e) {
            resultadoTextView.setText(""); // Limpiar salida
            Toast.makeText(this, "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    // Convierte una dirección IP en un arreglo de enteros
    private int[] parseIPAddress(String ipAddress) throws Exception {
        String[] parts = ipAddress.split("\\.");
        if (parts.length != 4) throw new Exception("Dirección IP no válida");

        int[] ip = new int[4];
        for (int i = 0; i < 4; i++) {
            ip[i] = Integer.parseInt(parts[i]);
            if (ip[i] < 0 || ip[i] > 255) throw new Exception("Parte de la dirección IP fuera de rango");
        }
        return ip;
    }

    // Método para verificar si la máscara de subred es válida
    private boolean esMascaraSubredValida(String mascaraSubred) {
        try {
            int[] subnetMask = parseIPAddress(mascaraSubred);
            int mascaraEnBits = 0;
            for (int i = 0; i < 4; i++) {
                mascaraEnBits = (mascaraEnBits << 8) | subnetMask[i];
            }
            boolean haVistoCeros = false;
            for (int i = 31; i >= 0; i--) {
                if ((mascaraEnBits & (1 << i)) == 0) {
                    haVistoCeros = true;
                } else if (haVistoCeros) {
                    return false;
                }
            }
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    // Método para contar la cantidad de 1s en la máscara de subred
    private int contarBitsEnUno(String mascaraSubred) {
        try {
            int[] subnetMask = parseIPAddress(mascaraSubred);
            int mascaraEnBits = 0;
            for (int i = 0; i < 4; i++) {
                mascaraEnBits = (mascaraEnBits << 8) | subnetMask[i];
            }
            int contador = 0;
            for (int i = 0; i < 32; i++) {
                if ((mascaraEnBits & (1 << i)) != 0) {
                    contador++;
                }
            }
            return contador;
        } catch (Exception e) {
            return 0;
        }
    }

    // Genera la máscara de subred en forma de un arreglo de enteros basado en el número de bits
    private int[] generateSubnetMask(int bitsMascaraSubred) {
        int[] subnetMask = new int[4];
        int bitIndex = 0;
        for (int i = 0; i < 4; i++) {
            for (int j = 7; j >= 0; j--) {
                if (bitIndex < bitsMascaraSubred) {
                    subnetMask[i] |= (1 << j); // Establece el bit a 1
                    bitIndex++;
                }
            }
        }
        return subnetMask;
    }

    // Método para calcular la cantidad de subredes disponibles
    private int calcularCantidadSubredes(String direccionIP, String bitsMascaraSubredStr) {
        try {
            int bitsMascaraSubred = Integer.parseInt(bitsMascaraSubredStr);

            // Determinar la clase de la dirección IP
            int[] ip = parseIPAddress(direccionIP);
            char claseIP = determinarClaseIP(ip);

            // Obtener la cantidad de bits de la máscara de subred por defecto según la clase
            int bitsMascaraPorDefecto = obtenerMascaraPorDefecto(claseIP);

            // Calcular la cantidad de subredes usando la fórmula: 2^(bits ingresados - bits por defecto)
            if (bitsMascaraSubred > bitsMascaraPorDefecto) {
                return (int) Math.pow(2, bitsMascaraSubred - bitsMascaraPorDefecto);
            } else {
                return 1; // Si la máscara no supera la por defecto, hay una sola subred
            }
        } catch (Exception e) {
            return -1; // Retornar -1 si hay un error
        }
    }

    // Método para obtener la cantidad de bits de la máscara de subred por defecto según la clase
    private int obtenerMascaraPorDefecto(char claseIP) {
        switch (claseIP) {
            case 'A':
                return 8;
            case 'B':
                return 16;
            case 'C':
                return 24;
            default:
                return 0; // Las clases D y E no se utilizan para subredes estándar
        }
    }

    // Método para calcular la cantidad de direcciones IP disponibles en una subred
    private int calcularCantidadDireccionesIP(int bitsMascaraSubred) {
        // Calcular el número de bits para hosts
        int bitsHost = 32 - bitsMascaraSubred;
        // La cantidad de direcciones IP es 2^bitsHost
        return (int) Math.pow(2, bitsHost) - 2; // Restamos 2 para excluir la dirección de red y de broadcast
    }

    // Realiza la operación AND entre la dirección IP y la máscara de subred
    private String calcularNetworkID(int[] ip, int[] subnetMask) {
        int[] networkAddress = new int[4];
        for (int i = 0; i < 4; i++) {
            networkAddress[i] = ip[i] & subnetMask[i];
        }
        return formatIPAddress(networkAddress);
    }

    // Método para calcular la dirección de broadcast
    private String calcularBroadcast(int[] ip, int[] subnetMask) {
        int[] networkAddress = new int[4];
        for (int i = 0; i < 4; i++) {
            networkAddress[i] = ip[i] & subnetMask[i];
        }
        int[] broadcastAddress = new int[4];
        for (int i = 0; i < 4; i++) {
            broadcastAddress[i] = networkAddress[i] | ~subnetMask[i] & 0xFF;
        }
        return formatIPAddress(broadcastAddress);
    }

    // Convierte un arreglo de enteros a una dirección IP en formato String
    private String formatIPAddress(int[] ip) {
        return ip[0] + "." + ip[1] + "." + ip[2] + "." + ip[3];
    }

    // Método para determinar la clase de la dirección IP
    private char determinarClaseIP(int[] ip) {
        int primerOcteto = ip[0];
        if (primerOcteto >= 1 && primerOcteto <= 126) {
            return 'A';
        } else if (primerOcteto >= 128 && primerOcteto <= 191) {
            return 'B';
        } else if (primerOcteto >= 192 && primerOcteto <= 223) {
            return 'C';
        } else if (primerOcteto >= 224 && primerOcteto <= 239) {
            return 'D';
        } else {
            return 'E';
        }
    }

    // Método para aplicar un filtro que solo permite números y puntos
    private void aplicarFiltro(EditText editText) {
        editText.setFilters(new InputFilter[]{
                (source, start, end, dest, dstart, dend) -> {
                    for (int i = start; i < end; i++) {
                        if (!Character.isDigit(source.charAt(i)) && source.charAt(i) != '.') {
                            return ""; // Rechaza el carácter no válido
                        }
                    }
                    return null; // Acepta el carácter válido
                }
        });
    }

    private static boolean isValidInetAddress(String ip) {
        return IP_ADDRESS.matcher(ip).matches();
    }
}
