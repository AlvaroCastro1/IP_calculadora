package com.example.ip_calculadora;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Scanner;

public class CalculadoraIP {

    /*public static void main(String[] args) {
        Scanner scanner = new Scanner(System.in);

        System.out.print("Ingrese la dirección IP en formato xxx.xxx.xxx.xxx: ");
        String direccionIP = scanner.nextLine();

        System.out.print("Ingrese el número de bits de la máscara de subred: ");
        int bitsMascaraSubred = scanner.nextInt();

        try {
            InetAddress direccionInetAddress = InetAddress.getByName(direccionIP);
            byte[] bytesDireccionIP = direccionInetAddress.getAddress();

            byte[] bytesMascaraSubred = calcularMascaraSubred(bitsMascaraSubred);

            InetAddress networkID = calcularNetworkID(bytesDireccionIP, bytesMascaraSubred);
            System.out.println("Network ID: " + networkID.getHostAddress());

            InetAddress broadcast = calcularIPBroadcast(bytesDireccionIP, bytesMascaraSubred);
            System.out.println("Broadcast: " + broadcast.getHostAddress());

            InetAddress siguienteNetworkID = calcularSiguienteNetworkID(broadcast.getAddress());
            System.out.println("Network ID Siguiente: " + siguienteNetworkID.getHostAddress());

            imprimirRangoIP(networkID.getAddress(), broadcast.getAddress());

        } catch (UnknownHostException e) {
            e.printStackTrace();
        } finally {
            scanner.close();
        }
    }*/

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

    private static void imprimirRangoIP(byte[] bytesNetworkID, byte[] bytesBroadcast) throws UnknownHostException {
        byte[] bytesPrimeraIP = bytesNetworkID.clone();
        bytesPrimeraIP[3] += 1;
        InetAddress primeraIP = InetAddress.getByAddress(bytesPrimeraIP);

        byte[] bytesUltimaIP = bytesBroadcast.clone();
        bytesUltimaIP[3] -= 1;
        InetAddress ultimaIP = InetAddress.getByAddress(bytesUltimaIP);

        System.out.println("Rango de IP: " + primeraIP.getHostAddress() + " - " + ultimaIP.getHostAddress());
    }
}
