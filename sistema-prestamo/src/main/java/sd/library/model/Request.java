/**************************************************************
#                         Pontificia Universidad Javeriana
#     Autor: Juan Bello, Kevin Garay, Arley Bernal
#     Fecha: 30 de Septiembre 2025
#     Materia: Sistemas Distribuidos
**************************************************************/
package sd.library.model;

/*
 * Atributos
 * - tipo: PRESTAMO, DEVOLUCION, RENOVACION
 * - isbn: Identificador del libro
 * - usuario: Identificador del usuario
 */
public class Request {
    public enum Tipo {
        PRESTAMO,
        DEVOLUCION, 
        RENOVACION
    }

    private Tipo tipo;
    private String isbn;
    private String usuario;

    // Constructor por defecto
    public Request() {}

    // Constructor parametrizado
    public Request(Tipo tipo, String isbn, String usuario) {
        this.tipo = tipo;
        this.isbn = isbn;
        this.usuario = usuario;
    }

    // Getters Y Setters
    public Tipo getTipo() { return tipo; }
    public void setTipo(Tipo tipo) { this.tipo = tipo; }

    public String getIsbn() { return isbn; }
    public void setIsbn(String isbn) { this.isbn = isbn; }

    public String getUsuario() { return usuario; }
    public void setUsuario(String usuario) { this.usuario = usuario; }
}