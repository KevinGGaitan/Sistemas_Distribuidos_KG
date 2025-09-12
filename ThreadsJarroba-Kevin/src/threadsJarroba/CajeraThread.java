package threadsJarroba;

 /**********************
#      Pontificia Universidad Javeriana
#    @author:Juan Bello, Kevin Garay, Arley Bernal
#     Fecha: 12 de Septiembre 2025
#     Materia: Sistemas Distribuidos
#**********************/

/**
 * La clase {@code CajeraThread} representa a una cajera que procesa
 * la compra de un cliente utilizando un hilo independiente.
 * 
 * Extiende de {@link Thread}, lo que permite que cada cajera
 * trabaje de manera concurrente con otras cajeras.
 *
 * El tiempo de procesamiento de cada producto del cliente está
 * definido en segundos dentro del arreglo de su carrito de compras.
 */
public class CajeraThread extends Thread {

	//Atributos
	private String nombre;

	private Cliente cliente;

	/** Tiempo inicial de referencia en milisegundos. */
	private long initialTime;


	// ========================
    // Constructores
    // ========================

	//constructor por defecto
	public CajeraThread() {
	}

	//constructor con parámetros
	public CajeraThread(String nombre, Cliente cliente, long initialTime) {
		this.nombre = nombre;
		this.cliente = cliente;
		this.initialTime = initialTime;
	}

    // ========================
    // Getters y Setters
    // ========================
	public String getNombre() {
		return nombre;
	}

	public void setNombre(String nombre) {
		this.nombre = nombre;
	}

	public long getInitialTime() {
		return initialTime;
	}

	//asignar un nuevo tiempo inicial
	public void setInitialTime(long initialTime) {
		this.initialTime = initialTime;
	}

	public Cliente getCliente() {
		return cliente;
	}

	//asignar un nuevo cliente
	public void setCliente(Cliente cliente) {
		this.cliente = cliente;
	}

    // ========================
    // Métodos
    // ========================

	/**
     * Ejecuta el procesamiento de la compra del cliente en un hilo independiente.
     * 
     * Se simula el tiempo de procesamiento de cada producto y se muestra
     * en consola el progreso y el tiempo total de atención.
     */
	@Override
	public void run() {

		// Mensaje de inicio de procesamiento
		System.out.println("La cajera " + this.nombre + " COMIENZA A PROCESAR LA COMPRA DEL CLIENTE " 
					+ this.cliente.getNombre() + " EN EL TIEMPO: " 
					+ (System.currentTimeMillis() - this.initialTime) / 1000 
					+ "seg");

		// Procesar los productos del carrito del cliente
		for (int i = 0; i < this.cliente.getCarroCompra().length; i++) {
			// Simula el tiempo de espera para el producto
			this.esperarXsegundos(cliente.getCarroCompra()[i]);
			// Muestra el avance del proceso
			System.out.println("Procesado el producto " + (i + 1) 
						+ " del cliente " + this.cliente.getNombre() + "->Tiempo: " 
						+ (System.currentTimeMillis() - this.initialTime) / 1000 
						+ "seg");
		}

		// Mensaje de fin de procesamiento  y muestra el tiempo total transcurrido
		System.out.println("La cajera " + this.nombre + " HA TERMINADO DE PROCESAR " 
						+ this.cliente.getNombre() + " EN EL TIEMPO: " 
						+ (System.currentTimeMillis() - this.initialTime) / 1000 
						+ "seg");
	}


	  /**
     * Simula el tiempo de espera correspondiente al procesamiento
     * de un producto.
     *
     * @param segundos Cantidad de segundos a esperar.
     */
	private void esperarXsegundos(int segundos) {
		try {
			// Se multiplica por 1000 para convertir a milisegundos
			Thread.sleep(segundos * 1000);
		} catch (InterruptedException ex) {
			Thread.currentThread().interrupt();// Restablece el estado de interrupción
		}
	}

}