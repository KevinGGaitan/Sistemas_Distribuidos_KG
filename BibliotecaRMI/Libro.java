/**************************************************************
#                         Pontificia Universidad Javeriana
#     Autor: Juan Bello, Kevin Garay, Arley Bernal
#     Fecha: 22 de Septiembre 2025
#     Materia: Sistemas Distribuidos
#**************************************************************/

import java.io.Serializable;

public class Libro implements Serializable {
    public final String isbn;
    public final String titulo;
    public final String autor;
    private int totalCopias;
    private int copiasDisp;


    public Libro(String isbn, String titulo, String autor, int copies) {
        this.isbn = isbn;
        this.titulo = titulo;
        this.autor = autor;
        this.totalCopias = copies;
        this.copiasDisp = copies;
    }


    public synchronized boolean prestar() {
        if (copiasDisp > 0) {
            copiasDisp--;
        return true;
        }
        return false;
    }


    public synchronized void devolver() {
    if (copiasDisp < totalCopias) 
        copiasDisp++;
    }


    public synchronized int getCopiasDisp() { return copiasDisp; }
    public synchronized int getTotalCopias() { return totalCopias; }


    @Override
    public String toString() {
    return isbn + " | " + titulo + " | " + autor + " | disponible: " + copiasDisp + "/" + totalCopias;
    }
}