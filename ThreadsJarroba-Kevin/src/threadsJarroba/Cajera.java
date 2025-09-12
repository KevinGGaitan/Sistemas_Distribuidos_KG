package threadsJarroba;

 /**********************
#      Pontificia Universidad Javeriana
#    @author:Juan Bello, Kevin Garay, Arley Bernal
#     Fecha: 12 de Septiembre 2025
#     Materia: Sistemas Distribuidos
#**********************/
// Interfaz remota para operaciones matemáticas

/**
 * La clase {@code Cajera} simula el proceso que realiza una cajera al atender
 * a un cliente en una tienda. Cada cajera procesa los productos de un cliente
 * con un tiempo de espera asociado a cada producto.
 * 
 */
public class Cajera {

	//Atributos
	private String nombre;

	// ========================
    // Constructores
    // ========================
	//constructor por defecto
	public Cajera() {
	}

	//constructor con parámetros
	public Cajera(String nombre) {
		this.nombre = nombre;
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

    // ========================
    // Métodos
    // ========================

	/**
     * Procesa la compra de un cliente. Recorre los productos en el carrito
     * de compras del cliente, espera un tiempo por cada producto
     * y muestra por consola el avance del proceso.
     *
     * @param cliente   Cliente al que se le procesa la compra.
     * @param timeStamp Tiempo inicial en milisegundos desde el cual se mide la duración.
     */
	public void procesarCompra(Cliente cliente, long timeStamp) {

		// Mensaje de inicio de procesamiento
		System.out.println("La cajera " + this.nombre + 
				" COMIENZA A PROCESAR LA COMPRA DEL CLIENTE " + cliente.getNombre() + 
				" EN EL TIEMPO: " + (System.currentTimeMillis() - timeStamp) / 1000	+
				"seg");

		// Recorremos los productos del carrito de compras
		for (int i = 0; i < cliente.getCarroCompra().length; i++) {
			this.esperarXsegundos(cliente.getCarroCompra()[i]);
			// Muestra el avance del proceso
			System.out.println("Procesado el producto " + (i + 1) + 
					" ->Tiempo: " + (System.currentTimeMillis() - timeStamp) / 1000 + 
					"seg");
		}

		// Mensaje de fin de procesamiento  y muestra el tiempo total transcurrido
		System.out.println("La cajera " + this.nombre + " HA TERMINADO DE PROCESAR " + 
							cliente.getNombre() + " EN EL TIEMPO: " + 
							(System.currentTimeMillis() - timeStamp) / 1000 + "seg");

	}

	
    /**
     * Simula el tiempo de espera correspondiente al procesamiento de un producto.
     *
     * @param segundos Cantidad de segundos a esperar (por producto).
     */
	private void esperarXsegundos(int segundos) {
		try {
			// Se multiplica por 1000 para convertir a milisegundos
			Thread.sleep(segundos * 1000);
		} catch (InterruptedException ex) {
			Thread.currentThread().interrupt(); // Restablece el estado de interrupción
		}
	}

}