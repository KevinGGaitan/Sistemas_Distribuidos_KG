/**************************************************************
#                         Pontificia Universidad Javeriana
#     Autor: Juan Bello, Kevin Garay, Arley Bernal
#     Fecha: 22 de Septiembre 2025
#     Materia: Sistemas Distribuidos
#**************************************************************/

import java.io.Serializable;
import java.time.LocalDate;


public class InfoDevolucion implements Serializable {
    public final String usuario;
    public LocalDate fechaLim;
    public int contDevol;


    public InfoDevolucion(String usuario, LocalDate fechaLim) {
    this.usuario = usuario;
    this.fechaLim = fechaLim;
    this.contDevol = 0;
    }
}