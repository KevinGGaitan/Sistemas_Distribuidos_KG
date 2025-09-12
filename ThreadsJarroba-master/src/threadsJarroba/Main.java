package threadsJarroba;

 /**********************
#      Pontificia Universidad Javeriana
#    @author:Juan Bello, Kevin Garay, Arley Bernal
#     Fecha: 12 de Septiembre 2025
#     Materia: Sistemas Distribuidos
#**********************/


/**
 * Clase principal que ejecuta el programa de simulación.
 * 
 * Se crean clientes con sus respectivos carritos de compras,
 * así como cajeras que procesan dichas compras. El programa
 * imprime en consola los tiempos de procesamiento de cada cliente
 * en función de los productos de su carrito
 *
 * En esta versión, el procesamiento de los clientes
 * es secuencial.
 */
public class Main {

	public static void main(String[] args) {

		// inicializamos los clientes con su respectivo carrito de compras
		Cliente cliente1 = new Cliente("Cliente 1", new int[] { 2, 2, 1, 5, 2, 3 });
		Cliente cliente2 = new Cliente("Cliente 2", new int[] { 1, 3, 5, 1, 1 });

		// inicializamos las cajeras
		Cajera cajera1 = new Cajera("Cajera 1");
		Cajera cajera2 = new Cajera("Cajera 2");

		// Tiempo inicial de referencia
		long initialTime = System.currentTimeMillis();

		// procesamos las compras
		cajera1.procesarCompra(cliente1, initialTime);
		cajera2.procesarCompra(cliente2, initialTime);
	}
}