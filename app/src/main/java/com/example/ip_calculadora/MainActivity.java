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

    private Button limpiarButton;


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
        limpiarButton = findViewById(R.id.limpiarButton);

        limpiarButton.setOnClickListener(view -> {
            ipEditText.setText(""); // Limpia el campo de dirección IP
            mascaraEditText.setText(""); // Limpia el campo de máscara de subred
            resultadoTextView.setText(""); // Limpia el resultado
        });
        calcularButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                calcularIP();
            }
        });
    }

    private void calcularIP() {
        String direccionIP = ipEditText.getText().toString().trim();
        String mascara_de_Subred_Str = mascaraEditText.getText().toString().trim();
        String mascaraSubredStr = "";

        if (esMascaraSubredValida(mascara_de_Subred_Str)) {
            // La máscara de subred es válida, procede con los cálculos
            mascaraSubredStr = contarBitsEnUno(mascara_de_Subred_Str)+"";
            //Toast.makeText(this, "Máscara de subred válida "+ mascaraSubredStr, Toast.LENGTH_SHORT).show();
        } else {
            // La máscara de subred no es válida
            Toast.makeText(this, "Por favor ingrese una máscara de subred válida", Toast.LENGTH_SHORT).show();
        }

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
            String network_ID = calcularNetworkID(direccionIP, mascaraSubredStr);
            String broadcast_Address = calcularBroadcast(direccionIP, mascaraSubredStr);


            int bitsMascaraPorOmision = obtenerMascaraPorOmision(claseIP);
            int cantidadDireccionesIP = calcularCantidadDireccionesIP(mascaraSubredStr);
            int cantidadSubredes = calcularCantidadSubredes(direccionIP, mascaraSubredStr);


            String resultado = "Clase de la IP: \n" + claseIP + "\n"
                    + "Network ID: \n" + network_ID + "\n"
                    + "Broadcast: \n" + broadcast_Address + "\n"
                    + "Cantidad de direcciones IP disponibles: \n" + cantidadDireccionesIP + "\n"
                    + "Cantidad de subredes disponibles: \n" + cantidadSubredes + "\n";

            resultadoTextView.setText(resultado);

            //imprimirRangoIP(networkID.getAddress(), broadcast.getAddress());
        } catch (UnknownHostException e) {
            resultadoTextView.setText(""); // Limpiar salida
            resultadoTextView.setText("Error: Dirección IP no válida");
        }
    }

    // Método para verificar si la máscara de subred es válida
    private boolean esMascaraSubredValida(String mascaraSubred) {
        try {
            // Convertir la máscara de subred en un arreglo de enteros
            int[] subnetMask = parseIPAddress(mascaraSubred);

            // Convertir la máscara de subred a un número entero de 32 bits
            int mascaraEnBits = 0;
            for (int i = 0; i < 4; i++) {
                mascaraEnBits = (mascaraEnBits << 8) | subnetMask[i];
            }

            // Verificar que la máscara de subred sea válida: unos consecutivos seguidos solo por ceros
            boolean haVistoCeros = false;
            for (int i = 31; i >= 0; i--) {
                if ((mascaraEnBits & (1 << i)) == 0) {
                    haVistoCeros = true; // Comienza a ver ceros
                } else if (haVistoCeros) {
                    return false; // Si ve un 1 después de haber visto ceros, no es válida
                }
            }
            return true;
        } catch (Exception e) {
            return false; // Si hay algún error en la conversión, no es válida
        }
    }

    // Método para contar la cantidad de 1s en la máscara de subred
    private int contarBitsEnUno(String mascaraSubred) {
        try {
            // Convertir la máscara de subred en un arreglo de enteros
            int[] subnetMask = parseIPAddress(mascaraSubred);

            // Convertir la máscara de subred a un número entero de 32 bits
            int mascaraEnBits = 0;
            for (int i = 0; i < 4; i++) {
                mascaraEnBits = (mascaraEnBits << 8) | subnetMask[i];
            }

            // Contar la cantidad de 1s en la representación binaria de la máscara
            int contador = 0;
            for (int i = 0; i < 32; i++) {
                if ((mascaraEnBits & (1 << i)) != 0) {
                    contador++;
                }
            }
            return contador;
        } catch (Exception e) {
            return 0; // Retornar 0 si hay algún error
        }
    }

    private String calcularNetworkID(String direccionIP, String mascaraSubredStr) {
        try {
            // Convertir la dirección IP en un arreglo de enteros
            int[] ip = parseIPAddress(direccionIP);

            // Convertir el número de bits de la máscara en un entero
            int bitsMascaraSubred = Integer.parseInt(mascaraSubredStr);

            // Generar la máscara de subred a partir del número de bits
            int[] subnetMask = generateSubnetMask(bitsMascaraSubred);

            // Realizar la operación AND para obtener la dirección de inicio de la subred (Network ID)
            int[] networkAddress = calculateNetworkAddress(ip, subnetMask);

            // Devolver el Network ID en formato String
            return formatIPAddress(networkAddress);

        } catch (Exception e) {
            return "Error: " + e.getMessage();
        }
    }

    // Métodos auxiliares

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

    // Realiza la operación AND entre la dirección IP y la máscara de subred
    private int[] calculateNetworkAddress(int[] ip, int[] subnetMask) {
        int[] networkAddress = new int[4];
        for (int i = 0; i < 4; i++) {
            networkAddress[i] = ip[i] & subnetMask[i];
        }
        return networkAddress;
    }

    // Convierte un arreglo de enteros a una dirección IP en formato String
    private String formatIPAddress(int[] ip) {
        return ip[0] + "." + ip[1] + "." + ip[2] + "." + ip[3];
    }

    private String calcularBroadcast(String direccionIP, String mascaraSubredStr) {
        try {
            // Convertir la dirección IP en un arreglo de enteros
            int[] ip = parseIPAddress(direccionIP);

            // Convertir el número de bits de la máscara en un entero
            int bitsMascaraSubred = Integer.parseInt(mascaraSubredStr);

            // Generar la máscara de subred a partir del número de bits
            int[] subnetMask = generateSubnetMask(bitsMascaraSubred);

            // Realizar la operación AND para obtener la dirección de inicio de la subred (Network ID)
            int[] networkAddress = calculateNetworkAddress(ip, subnetMask);

            // Calcular la dirección de broadcast
            int[] broadcastAddress = calculateBroadcastAddress(networkAddress, subnetMask);

            // Devolver la dirección de broadcast en formato String
            return formatIPAddress(broadcastAddress);

        } catch (Exception e) {
            return "Error: " + e.getMessage();
        }
    }

    // Método para calcular la dirección de broadcast
    private int[] calculateBroadcastAddress(int[] networkAddress, int[] subnetMask) {
        int[] broadcastAddress = new int[4];
        for (int i = 0; i < 4; i++) {
            // Cambiar los ceros de la máscara de subred a unos y aplicar a la dirección de red
            broadcastAddress[i] = networkAddress[i] | ~subnetMask[i] & 0xFF;
        }
        return broadcastAddress;
    }

    private int calcularCantidadDireccionesIP(String mascaraSubredStr) {
        try {
            // Convertir el número de bits de la máscara en un entero
            int bitsMascaraSubred = Integer.parseInt(mascaraSubredStr);

            // Calcular el número de ceros en la máscara de subred
            int numeroDeCeros = 32 - bitsMascaraSubred;

            // Calcular la cantidad de direcciones IP: 2^(# ceros)
            int cantidadDireccionesIP = (int) Math.pow(2, numeroDeCeros);

            return cantidadDireccionesIP;

        } catch (Exception e) {
            return -1;
        }
    }

    private int calcularCantidadSubredes(String direccionIP, String mascaraSubredStr) {
        try {
            // Convertir el número de bits de la máscara en un entero
            int bitsMascaraSubred = Integer.parseInt(mascaraSubredStr);

            // Determinar la clase de la dirección IP
            int[] ip = parseIPAddress(direccionIP);
            char claseIP = determinarClaseIP(ip);

            // Obtener la cantidad de bits de la máscara de subred por defecto según la clase
            int bitsMascaraPorDefecto = obtenerMascaraPorDefecto(claseIP);

            // Calcular la cantidad de subredes usando la fórmula: 2^(bits ingresados - bits por defecto)
            int cantidadSubredes = (int) Math.pow(2, bitsMascaraSubred - bitsMascaraPorDefecto);

            return cantidadSubredes;

        } catch (Exception e) {
            return -1;
        }
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

    private int calcularCantidadSubredes_old(int bitsMascaraSubred, int bitsMascaraPorOmision) {
        if (bitsMascaraSubred <= bitsMascaraPorOmision) {
            return 1; // No se crean subredes si la máscara propuesta es igual o menor a la por omisión
        }
        return (int) Math.pow(2, bitsMascaraSubred - bitsMascaraPorOmision);
    }

    private int calcularCantidadDireccionesIP_old(int bitsMascaraSubred) {
        int bitsHost = 32 - bitsMascaraSubred;
        return (int) Math.pow(2, bitsHost) - 2;
    }
}
