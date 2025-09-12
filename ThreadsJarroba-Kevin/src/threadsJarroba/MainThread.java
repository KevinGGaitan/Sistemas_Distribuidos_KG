package threadsJarroba;

 /**********************
#      Pontificia Universidad Javeriana
#    @author:Juan Bello, Kevin Garay, Arley Bernal
#     Fecha: 12 de Septiembre 2025
#     Materia: Sistemas Distribuidos
#**********************/

/**
 * Clase principal que ejecuta la simulación con hilos.
 *
 * En esta versión, cada {@link CajeraThread} se ejecuta en un hilo
 * independiente, lo que permite que varias cajeras procesen clientes
 * en paralelo. Esto demuestra el uso de la concurrencia en Java.
 */
public class MainThread {

	public static void main(String[] args) {

		// inicializamos los clientes con su respectivo carrito de compras
		Cliente cliente1 = new Cliente("Cliente 1", new int[] { 2, 2, 1, 5, 2, 3 });
		Cliente cliente2 = new Cliente("Cliente 2", new int[] { 1, 3, 5, 1, 1 });

		// Tiempo inicial de referencia
		long initialTime = System.currentTimeMillis();

		// inicializamos las cajeras con hilos independientes
		CajeraThread cajera1 = new CajeraThread("Cajera 1", cliente1, initialTime);
		CajeraThread cajera2 = new CajeraThread("Cajera 2", cliente2, initialTime);

		// Iniciamos los hilos de las cajeras (se ejecuta su método run() en paralelo)
		/*	start() → es el método que inicia el hilo de verdad. Llama internamente a 
		run() pero en un hilo separado, permitiendo que el programa ejecute varios 
		procesos en paralelo.*/
		cajera1.start();
		cajera2.start();
	}
}