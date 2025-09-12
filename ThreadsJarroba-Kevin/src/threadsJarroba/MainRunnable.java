package threadsJarroba;

 /**********************
#      Pontificia Universidad Javeriana
#    @author:Juan Bello, Kevin Garay, Arley Bernal
#     Fecha: 12 de Septiembre 2025
#     Materia: Sistemas Distribuidos
#**********************/

/**
 * Clase principal que implementa la interfaz {@link Runnable} para ejecutar el
 * procesamiento de compras en paralelo utilizando hilos. 
 * 
 * En este caso, el uso de {@code Runnable} permite separar la lógica del procesamiento 
 * de la mecánica del hilo, a diferencia de extender directamente la clase {@link Thread}.
 * 
 * Este programa permite comparar la ejecución secuencial frente a la concurrente,
 * mostrando cómo las cajeras pueden atender en paralelo a distintos clientes.

 */
public class MainRunnable implements Runnable{
	
	// Cliente que será atendido por la cajera
	private Cliente cliente;
	// Cajera que atenderá al cliente
	private Cajera cajera;

	// Tiempo inicial de referencia en milisegundos
	private long initialTime;
	
	// Constructor con parámetros
	public MainRunnable (Cliente cliente, Cajera cajera, long initialTime){
		this.cajera = cajera;
		this.cliente = cliente;
		this.initialTime = initialTime;
	}

	public static void main(String[] args) {
		
		// Inicializamos los clientes con su respectivo carrito de compras
		Cliente cliente1 = new Cliente("Cliente 1", new int[] { 2, 2, 1, 5, 2, 3 });
		Cliente cliente2 = new Cliente("Cliente 2", new int[] { 1, 3, 5, 1, 1 });
		
		// Inicializamos las cajeras que atenderán a los clientes
		Cajera cajera1 = new Cajera("Cajera 1");
		Cajera cajera2 = new Cajera("Cajera 2");
		
		// Tiempo inicial de referencia
		long initialTime = System.currentTimeMillis();
		
		// Creamos las tareas (Runnable) que ejecutarán cada cajera con su cliente
		Runnable proceso1 = new MainRunnable(cliente1, cajera1, initialTime);
		Runnable proceso2 = new MainRunnable(cliente2, cajera2, initialTime);
		
		// Lanzamos los hilos de ejecución
		new Thread(proceso1).start();
		new Thread(proceso2).start();

	}

	 /**
     * Lógica de ejecución del hilo.
     * 
     * Este método se ejecuta al iniciar el hilo, invocando el 
     * procesamiento de la compra del cliente asignado.
     */
	@Override
	public void run() {
		this.cajera.procesarCompra(this.cliente, this.initialTime);
	}

}
