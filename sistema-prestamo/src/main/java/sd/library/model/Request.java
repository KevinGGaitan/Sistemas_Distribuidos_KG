/**************************************************************
#                         Pontificia Universidad Javeriana
#     Autor: Juan Bello, Kevin Garay, Arley Bernal
#     Fecha: 30 de Septiembre 2025
#     Materia: Sistemas Distribuidos
#**************************************************************/
package sd.library.model;

public class Request {
    public enum Tipo { PRESTAMO, DEVOLUCION, RENOVACION }


    private Tipo tipo;
    private String isbn;
    private String usuario;


    public Request() {}


    public Request(Tipo tipo, String isbn, String usuario) {
        this.tipo = tipo;
        this.isbn = isbn;
        this.usuario = usuario;
    }


    public Tipo getTipo() { return tipo; }
    public String getIsbn() { return isbn; }
    public String getUsuario() { return usuario; }


    public void setTipo(Tipo tipo) { this.tipo = tipo; }
    public void setIsbn(String isbn) { this.isbn = isbn; }
    public void setUsuario(String usuario) { this.usuario = usuario; }
}