
package Comunicacion_En_Xarxa;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;

public class AevServidor {

	/**
	 * Extrae una lista de hilos del servidor para un canal específico.
	 * @param listasPorCanal Una lista de listas donde cada sublista contiene los hilos
	 *                       del servidor asociados a un canal.
	 * @param index El índice del canal (empezando desde 1) cuya lista de hilos se desea extraer.
	 * @return Una lista de objetos {@code AevHiloServidor} correspondientes al canal especificado.
	 */
	private static ArrayList<AevHiloServidor> extraureFilsCanal(List<ArrayList<AevHiloServidor>> listasPorCanal, int index) {
		return listasPorCanal.get(index - 1);
	}

	/**
	 * Punto de entrada principal para el servidor.
	 *
	 * Este método inicializa el servidor, carga los canales disponibles desde un archivo,
	 * escucha nuevas conexiones de clientes y gestiona múltiples hilos para manejar
	 * la comunicación con los clientes de forma concurrente.
	 */
	public static void main(String[] args) throws IOException {
		System.err.println("SERVIDOR >>> Arranca el servidor");

		ServerSocket socketEscolta = null;
		try {
			socketEscolta = new ServerSocket(5000);
		} catch (IOException e) {
			System.err.println("SERVIDOR >>> Error");
			return;
		}

		// Listas principales
		List<ArrayList<AevHiloServidor>> listasPorCanal = new ArrayList<>();
		ArrayList<String> canalesDisponibles = new ArrayList<>();

		// Leer y procesar canales al arrancar el servidor
		try (BufferedReader br = new BufferedReader(new FileReader("./Comunicacion_En_Xarxa/canals2.txt"))) {
			String canal;
			System.err.println("SERVIDOR >>> Llegint canals disponibles...");
			while ((canal = br.readLine()) != null) {
				canalesDisponibles.add(canal.trim());
				listasPorCanal.add(new ArrayList<AevHiloServidor>());
			}
			System.err.println("SERVIDOR >>> Canals disponibles carregats.");
		}

		// Aceptar múltiples conexiones en un bucle
		while (true) {
			try {
				System.err.println("SERVIDOR >>> Esperando nuevas conexiones...");
				Socket connexio = socketEscolta.accept(); // Esperar conexión de un nuevo cliente
				System.err.println("SERVIDOR >>> Connexio rebuda! --> Llançant nou fil");

				
				PrintWriter pw = new PrintWriter(connexio.getOutputStream(), true);
				BufferedReader bfr = new BufferedReader(new InputStreamReader(connexio.getInputStream()));

				
				StringBuilder canales = new StringBuilder();
				for (int i = 0; i < canalesDisponibles.size(); i++) {
					canales.append((i + 1)).append("-").append(canalesDisponibles.get(i)).append(", ");
				}
				
				pw.println(canales.toString());

				System.err.println("SERVIDOR >>> Esperant selecció de canal i nom d'usuari");

				int indexCanal = Integer.parseInt(bfr.readLine().trim());
				String nom;
				boolean nombreDuplicado;
				
				ArrayList<AevHiloServidor> filsCanalActual = extraureFilsCanal(listasPorCanal, indexCanal);

				do {
					nombreDuplicado = false;
					nom = bfr.readLine().trim();
					synchronized (filsCanalActual) {
						for (AevHiloServidor t : filsCanalActual) {
							if (t.getName().equals(nom)) {
								nombreDuplicado = true;
								break;
							}
						}
					}

					if (nombreDuplicado) {
						pw.println("ERROR");
					}

				} while (nombreDuplicado);
				
				pw.println("OK");

				System.err.println("SERVIDOR >>> Usuari " + nom + " ha seleccionat el canal: " + indexCanal);
				
				//inicializo hiloServidor
				AevHiloServidor ej1 = new AevHiloServidor(connexio, listasPorCanal, indexCanal, nom,
						canalesDisponibles);
				Thread t = new Thread(ej1);
				filsCanalActual.add(ej1);
				t.start();

			} catch (IOException e) {
				System.err.println("SERVIDOR >>> Error aceptando conexión: " + e.getMessage());
			}
		}
	}
}
