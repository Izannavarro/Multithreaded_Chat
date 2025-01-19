package Comunicacion_En_Xarxa;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Scanner;

import javax.swing.JOptionPane;

public class AevCliente {

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
		String timeStamp = ahora.format(formato);

		return timeStamp + ": " + mensaje;
	}

	/**
	 * Método principal que gestiona la conexión de un cliente con un servidor y permite
	 * la interacción del cliente mediante la consola y una interfaz gráfica para enviar
	 * mensajes en un canal seleccionado.
	 * 
	 * Este método realiza los siguientes pasos:
	 * - Solicita al usuario la dirección IP y el puerto para la conexión con el servidor.
	 * - Establece la conexión con el servidor.
	 * - Muestra los canales disponibles en el servidor y permite al usuario seleccionar uno.
	 * - Permite al usuario ingresar su nombre de usuario, validando que no exista ya.
	 * - Lanza un hilo para escuchar mensajes del servidor mientras el cliente puede enviar mensajes.
	 * - El cliente puede enviar mensajes de texto al servidor, los cuales incluyen una fecha y hora actuales.
	 * - El proceso finaliza cuando el cliente ingresa "exit" o cuando ocurre un error.
	 */
	public static void main(String[] args) {
		System.out.println("CLIENT >>> Arranca client");

		try (Socket socket = new Socket()) {
			// SCANNER DEL TECLADO
			Scanner scanner = new Scanner(System.in);
			System.out.println("CLIENT >>> Disme la IP:");
			String ip = scanner.nextLine();
			System.out.println("CLIENT >>> Disme el port:");
			int port = scanner.nextInt();

			InetSocketAddress direccio = new InetSocketAddress(ip, port);
			socket.connect(direccio);
			System.out.println("Connexió establerta!");

			// Preparar canals de comunicació
			PrintWriter pw = new PrintWriter(socket.getOutputStream());
			BufferedReader bfr = new BufferedReader(new InputStreamReader(socket.getInputStream()));

			String canales = bfr.readLine();
			System.out.println("CANALES DISPONIBLES: " + canales);

			System.out.println("Selecciona canal: ");
			int eleccio = scanner.nextInt();
			scanner.nextLine();

			while (eleccio < 1 || eleccio > 4) {
				System.out.println("CANAL INCORRECTE, SELECCIONA DE NOU:\n" + canales);
				eleccio = scanner.nextInt();
				scanner.nextLine();
			}
			
			pw.write(eleccio + "\n");

			// Empieza la comprobación
			while (true) {
				System.out.println("Indica nom d'usuari: ");
				String nom = scanner.nextLine();

				System.out.println("Enviant elecció a Servidor!");
				pw.write(nom + "\n");
				pw.flush();

				String respuesta = bfr.readLine(); // Leer la respuesta del servidor

				if ("OK".equals(respuesta)) {
					break;
				} else if ("ERROR".equals(respuesta)) {
					System.out.println("\nEl nom d'usuari ja existeix. prova un altre. \n");
				}
			}

			AevHiloCliente ej1 = new AevHiloCliente(socket);
			Thread fil1 = new Thread(ej1);
			fil1.start();
			
			System.out.println("Presiona ENTER para enviar mensajes!");
			BufferedReader enter = new BufferedReader(new InputStreamReader(System.in));

			while (true) {
				String input = enter.readLine();

				if (input != null) {
					// Mostrar PopUp
					String missatge_A_Enviar = (String) JOptionPane.showInputDialog(null,
							"Escriu un missatge o escriu 'exit' per sortir:", "Escriure missatge",
							JOptionPane.QUESTION_MESSAGE);
					
					JOptionPane.getRootFrame().requestFocus();

					if (!missatge_A_Enviar.isEmpty() && missatge_A_Enviar != null) {
						String mensajeCorrecto = agregarTimeStamp(missatge_A_Enviar);
						System.out.println(mensajeCorrecto);
						pw.write(missatge_A_Enviar + "\n");
						pw.flush();
					} else if (missatge_A_Enviar.toLowerCase().equals("exit")) {
						pw.write("exit\n"); 
				        pw.flush();
						break;
					} else {
						System.out.println("Mensaje vacío, intenta nuevamente.");
					}
				}
			}
			pw.close();
			bfr.close();
			socket.close();
		} catch (IOException e) {
			System.err.println("CLIENT >>> Error per a conectar al servidor: " + e.getMessage());
		} 

		System.out.println("CLIENT >>> Programa finalitzat.");
	}
}
