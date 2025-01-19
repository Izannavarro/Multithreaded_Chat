package Comunicacion_En_Xarxa;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Runnable = Implementa clase run();
 */
public class AevHiloServidor implements Runnable {
	/** Socket para la conexión entre el cliente y el servidor. */
	Socket connexio;
	 /** Buffer para la lectura de mensajes del cliente. */
	BufferedReader br;
	/** Writer para el envío de mensajes al cliente. */
	PrintWriter pw;

	private List<ArrayList<AevHiloServidor>> canales;
	private ArrayList<AevHiloServidor> filsCanalActual;
	private ArrayList<String> canalesDisponibles;
	int canalSeleccionado;
	private String nomFil;

	/**
	 * Constructor de la clase AevHiloServidor.
	 *
	 * Este constructor inicializa un hilo de servidor con la conexión del cliente, 
	 * el canal seleccionado, el nombre del hilo, y las listas de canales y hilos disponibles.
	 * 
	 * @param connexio El socket de la conexión del cliente.
	 * @param listasPorCanal Una lista de listas que contiene los hilos asociados a cada canal.
	 * @param canalActual El índice del canal seleccionado por el cliente (comienza en 1).
	 * @param nom El nombre asignado al cliente para este hilo.
	 * @param canDisp Una lista de nombres de canales disponibles.
	 */
	public AevHiloServidor(Socket connexio, List<ArrayList<AevHiloServidor>> listasPorCanal, int canalActual,
			String nom, ArrayList<String> canDisp) {
		this.connexio = connexio;
		this.canales = listasPorCanal;
		this.canalSeleccionado = canalActual;
		this.nomFil = nom;
		this.filsCanalActual = canales.get(canalSeleccionado - 1);
		this.canalesDisponibles = canDisp;
	}

	public PrintWriter getPrintWriter() {
		return pw;
	}

	public String getName() {
		return nomFil;
	}

	/**
	 * Muestra los canales disponibles al cliente.
	 *
	 * Este método construye una cadena con la lista de canales disponibles, incluyendo
	 * sus índices, y la envía al cliente a través del flujo de salida.
	 */
	private void mostrarCanalesDisponibles() {
		String resultado = "Canales disponibles: [";
		int indice = 1;
		for (String s : canalesDisponibles) {
			resultado += indice + "-" + s + " ";
			indice++;
		}
		resultado += "]";

		pw.write(resultado + "\n");
		pw.flush();
	}

	/**
	 * Muestra los usuarios activos en el canal actual al cliente.
	 *
	 * Este método construye un mensaje que enumera los nombres de los usuarios activos
	 * en el canal seleccionado por el cliente. Si no hay usuarios en el canal, informa 
	 * que no hay hilos asociados al canal actual.
	 */
	private void usuariosCanalActual() {
		String resultado = "";
		if (!filsCanalActual.isEmpty()) {
			resultado = "Usuarios activos canal " + canalSeleccionado + ": ";

			for (AevHiloServidor t : filsCanalActual) {
				resultado += " " + t.getName();
			}

		} else {
			resultado = "No hay hilos asociados al canal " + canalSeleccionado;
		}

		pw.write(resultado + "\n");
		pw.flush();
	}

	/**
	 * Envía un mensaje a los usuarios de otro canal especificado en el mensaje.
	 *
	 * Este método extrae el número del "@canalX" y envía el contenido del mensaje a 
	 * todos los clientes conectados a ese canal.
	 * Si el canal no tiene hilos asociados, informa al cliente que no hay usuarios
	 * en el canal especificado.
	 *
	 * @param missatge El mensaje recibido que contiene la información del canal y 
	 *                 el texto a enviar. Debe incluir el formato "@canalN mensaje".
	 */
	private void enviarOtroCanal(String missatge) throws IOException {

	    int posicionNumero = missatge.indexOf("@canal") + 6; 
	    int finCanal = missatge.indexOf(" ", posicionNumero);

	    // Si no hay espacio, significa que el número del canal es lo último, se manejaría el caso
	    if (finCanal == -1) {
	        finCanal = missatge.length();
	    }

	    int numero = Integer.parseInt(missatge.substring(posicionNumero, finCanal).trim());

	    // Se extrae el mensaje completo después del canal y número.
	    String mensajeExtraido = missatge.substring(finCanal).trim();

	    System.err.println("SERVIDOR >>> " + nomFil + "(canal " + numero + ") >>> " + mensajeExtraido);

	    ArrayList<AevHiloServidor> hilosCanalSeleccionado = canales.get(numero - 1);

	    if (hilosCanalSeleccionado != null && !hilosCanalSeleccionado.isEmpty()) {
	        for (AevHiloServidor hilo : hilosCanalSeleccionado) {
	            hilo.pw.write(mensajeExtraido + "\n");
	            hilo.pw.flush();
	        }
	    } else {
	        String resultado = "No hay hilos asociados al canal " + canalSeleccionado;
	        pw.write(resultado + "\n");
			pw.flush();
	    }
	}

	/**
	 * Envía un mensaje a todos los clientes del canal actual, excluyéndose a si mismo.
	 *
	 * Este método toma un mensaje recibido y lo retransmite a todos los clientes conectados
	 * en el mismo canal que el cliente remitente. El cliente remitente es excluido de esta retransmisión.
	 *
	 * @param missatge El mensaje que se enviará a los demás clientes del canal.
	 */
	private void enviarMensaje(String missatge) {
		System.err.println("SERVIDOR >>> " + nomFil + "(canal " + canalSeleccionado + ") >>> " + missatge);
		for (AevHiloServidor hilo : filsCanalActual) {
			if (!hilo.getName().equals(nomFil)) {
				hilo.pw.write(missatge + "\n");
				hilo.pw.flush();
			}
		}
	}

	/**
	 * Lógica principal de la clase AevHiloServidor para gestionar la comunicación de un cliente.
	 *
	 * Este método se ejecuta cuando el hilo es iniciado y gestiona la interacción con un cliente 
	 * conectado al servidor. Procesa comandos específicos como `whois`, `@canal`, `exit`, y `channels`,
	 * además de retransmitir mensajes dentro del canal actual. Gestiona la conexión y cierra
	 * recursos de manera adecuada al finalizar.
	 */
	public void run() {
		try {
			// Cargamos entrada y salida de información
			InputStreamReader isr = new InputStreamReader(connexio.getInputStream());
			br = new BufferedReader(isr);
			OutputStream os = connexio.getOutputStream();
			pw = new PrintWriter(os);

			while (true) {
				String missatge = br.readLine().toLowerCase().trim();

				if (missatge.equals("whois")) {
					System.err.println("SERVIDOR >>> " + nomFil + "(canal " + canalSeleccionado + ") >>> whois");
					usuariosCanalActual();

				} else if (missatge.contains("@canal")) {
					enviarOtroCanal(missatge);
				} else if (missatge.equals("exit")) {
					System.err.println("SERVIDOR >>> Cliente " + nomFil + "del canal"+ canalSeleccionado +" desconectado.");
					filsCanalActual.remove(this);
					pw.write("exit" + "\n");
					pw.flush();
					break;
				} else if (missatge.equals("channels")) {
					System.err.println("SERVIDOR >>> " + nomFil + "(canal " + canalSeleccionado + ") >>> channels");
					mostrarCanalesDisponibles();
				} else {
					enviarMensaje(missatge);
				}
			}
			br.close();
			pw.close();
			this.connexio.close();
		} catch (Exception e) {
			e.printStackTrace();
			System.err.println("SERVIDOR Fil " + Thread.currentThread().getName() + " ERROR.");
		} finally {
		    try {
		        if (br != null) br.close();
		        if (pw != null) pw.close();
		        if (connexio != null && !connexio.isClosed()) connexio.close();
		    } catch (IOException e) {
		        System.err.println("Error cerrando recursos: " + e.getMessage());
		    }
		}
	}
}
