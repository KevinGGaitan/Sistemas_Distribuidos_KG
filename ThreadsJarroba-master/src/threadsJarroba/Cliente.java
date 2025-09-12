package threadsJarroba;

 /**********************
#      Pontificia Universidad Javeriana
#    @author:Juan Bello, Kevin Garay, Arley Bernal
#     Fecha: 12 de Septiembre 2025
#     Materia: Sistemas Distribuidos
#**********************/

/**
 * La clase {@code Cliente} representa a un cliente en una tienda.
 * Cada cliente tiene un nombre y un carrito de compras, donde el carrito
 * está representado por un arreglo de enteros que indican el tiempo
 * (en segundos) necesario para procesar cada producto.
 *
 */
public class Cliente {

	//Atributos
	private String nombre;

	 /** 
     * Carrito de compras del cliente.
     * Cada posición del arreglo representa un producto y su valor es
     * el tiempo (en segundos) que tarda en procesarse.
     */
	private int[] carroCompra;

	// ========================
    // Constructores
    // ========================

	//constructor por defecto
	public Cliente() {
	}

	//constructor con parámetros
	public Cliente(String nombre, int[] carroCompra) {
		this.nombre = nombre;
		this.carroCompra = carroCompra;
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

	public int[] getCarroCompra() {
		return carroCompra;
	}

   //asignar un nuevo carrito de compras
	public void setCarroCompra(int[] carroCompra) {
		this.carroCompra = carroCompra;
	}

}
