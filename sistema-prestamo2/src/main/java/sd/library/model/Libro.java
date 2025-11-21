/**************************************************************
#                         Pontificia Universidad Javeriana
#     Autor: Juan Bello, Kevin Garay, Arley Bernal
#     Fecha: 30 de Septiembre 2025
#     Materia: Sistemas Distribuidos
**************************************************************/
package sd.library.model;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/*
 * Atributos:
 * - isbn: Identificador único del libro
 * - titulo: Título descriptivo
 * - copiasDisponibles: Contador de disponibilidad
 * - prestadoA: Lista de usuarios con préstamos activos
 * - renovaciones: Mapa usuario -> contador de renovaciones
 * - fechaLim: Mapa usuario -> fecha límite de devolución
 * 
 * Metodos:
 * - prestarA(): Registra préstamo a usuario
 * - devolverA(): Procesa devolución
 * - renovarA(): Extiende préstamo existente
 */
public class Libro {
    private String isbn;
    private String titulo;
    private int copiasDisponibles;
    private List<String> prestadoA;
    private Map<String, Integer> renovaciones;
    private Map<String, String> fechaLim;

    // Constructor por defecto con inicialización
    public Libro() {
        this.prestadoA = new ArrayList<>();
        this.renovaciones = new HashMap<>();
        this.fechaLim = new HashMap<>();
    }

    // Constructor parametrizado
    public Libro(String isbn, String titulo, int copiasDisponibles) {
        this(); // Llama al constructor por defecto
        this.isbn = isbn;
        this.titulo = titulo;
        this.copiasDisponibles = copiasDisponibles;
    }

    // Getters y Setters
    public String getIsbn() { return isbn; }
    public void setIsbn(String isbn) { this.isbn = isbn; }

    public String getTitulo() { return titulo; }
    public void setTitulo(String titulo) { this.titulo = titulo; }

    public int getCopiasDisponibles() { return copiasDisponibles; }
    public void setCopiasDisponibles(int copiasDisponibles) { 
        this.copiasDisponibles = copiasDisponibles; 
    }

    public List<String> getPrestadoA() { return prestadoA; }
    public void setPrestadoA(List<String> prestadoA) { this.prestadoA = prestadoA; }

    public Map<String, Integer> getRenovaciones() { return renovaciones; }
    public void setRenovaciones(Map<String, Integer> renovaciones) { 
        this.renovaciones = renovaciones; 
    }

    public Map<String, String> getFechaLim() { return fechaLim; }
    public void setFechaLim(Map<String, String> fechaLim) { this.fechaLim = fechaLim; }
    
    //Verifica si un usuario tiene prestado este libro
    public boolean estaPrestadoA(String usuario) {
        return prestadoA.contains(usuario);
    }

    //Registra un préstamo a un usuario
    public void prestarA(String usuario, String fechaLimite) {
        prestadoA.add(usuario);
        copiasDisponibles--;
        renovaciones.put(usuario, 0); // Inicializar contador
        fechaLim.put(usuario, fechaLimite);
    }

    //Procesa la devolución de un libro
    public void devolverA(String usuario) {
        if (prestadoA.remove(usuario)) {
            copiasDisponibles++;
            renovaciones.remove(usuario);
            fechaLim.remove(usuario);
        }
    }

    //Renueva el préstamo de un usuario
    public void renovarA(String usuario, String nuevaFecha) {
        if (prestadoA.contains(usuario)) {
            int count = renovaciones.getOrDefault(usuario, 0);
            renovaciones.put(usuario, count + 1);
            fechaLim.put(usuario, nuevaFecha);
        }
    }
}