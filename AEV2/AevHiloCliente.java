package Comunicacion_En_Xarxa;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.Socket;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Runnable = Implementa clase run();
 */
public class AevHiloCliente implements Runnable {
	/** Socket para la conexión entre el cliente y el servidor. */
    private Socket connexio;
    /** Buffer para la lectura de mensajes del HiloServidor. */
    private BufferedReader br;
    
    /**
     * Constructor para la clase AevHiloCliente.
     * 
     * Este constructor inicializa un nuevo objeto de la clase `AevHiloCliente` estableciendo la conexión
     * con el servidor utilizando el socket proporcionado. Se utilizará esta conexión para gestionar la 
     * comunicación entre el cliente y el servidor en un hilo específico.
     *
     * @param connexio El socket que representa la conexión entre el cliente y el servidor.
     */
    public AevHiloCliente(Socket connexio) {
        this.connexio = connexio;
    }

    /**
	 * Agrega la fecha y hora actuales (timestamp) al principio de un mensaje.
	 *
	 * Este método toma un mensaje como parámetro, agrega una marca de tiempo en el formato
	 * "dd/MM-HH:mm:ss" y devuelve el mensaje concatenado con la marca de tiempo.
	 * 
	 * La marca de tiempo es generada utilizando la fecha y hora actuales del sistema.
	 *
	 * @param mensaje El mensaje al que se le agregará la marca de tiempo.
	 * @return El mensaje concatenado con la marca de tiempo al principio.
	 */
    public static String agregarTimeStamp(String mensaje) {
        LocalDateTime ahora = LocalDateTime.now();
        DateTimeFormatter formato = DateTimeFormatter.ofPattern("dd/MM-HH:mm:ss");
        return ahora.format(formato) + ": " + mensaje;
    }

    /**
     * Método que ejecuta el comportamiento principal del hilo en el lado del cliente para recibir y procesar
     * mensajes del servidor. El cliente permanece a la espera de recibir mensajes a través de la conexión
     * y muestra los mensajes con una marca de tiempo. Si el mensaje es "exit" o la respuesta es `null`, el hilo se termina.
     * 
     * Este método realiza las siguientes tareas:
     * - Lee los mensajes enviados por el servidor de forma continua hasta que se reciba un mensaje de desconexión ("exit").
     * - Agrega una marca de tiempo al mensaje recibido y lo muestra por la consola.
     * - Si el mensaje es "exit" o si la conexión se pierde, se cierra la conexión y termina el hilo.
     */
    public void run() {
        try {
            InputStreamReader isr = new InputStreamReader(connexio.getInputStream());
            br = new BufferedReader(isr);

            while (true) {
                String respuesta = br.readLine();

                if (respuesta == null || respuesta.trim().equalsIgnoreCase("exit")) {
                    System.err.println("Cliente desconectado.");
                    break;
                }

                String mensajeCorrecto = agregarTimeStamp(respuesta);
                System.err.println(mensajeCorrecto);
            }
            
            br.close();
            connexio.close();
            
        } catch (Exception e) {
            System.err.println("Error en el cliente: " + e.getMessage());
        } finally {
            try {
                if (br != null) br.close();
                if (connexio != null && !connexio.isClosed()) connexio.close();
            } catch (Exception e) {
                System.err.println("Error cerrando recursos: " + e.getMessage());
            }
        }
    }

}
