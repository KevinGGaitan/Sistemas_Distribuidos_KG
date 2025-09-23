/**************************************************************
#                         Pontificia Universidad Javeriana
#     Autor: Juan Bello, Kevin Garay, Arley Bernal
#     Fecha: 22 de Septiembre 2025
#     Materia: Sistemas Distribuidos
#**************************************************************/

import java.io.Serializable;
import java.time.LocalDate;


public class Transaccion implements Serializable {
    public final boolean exito;
    public final String mensaje;
    public final LocalDate fechaLim;


    public Transaccion(boolean exito, String mensaje, LocalDate fechaLim) {
        this.exito = exito;
        this.mensaje = mensaje;
        this.fechaLim = fechaLim;
    }
    
    public Transaccion(boolean exito, String mensaje) {
        this(exito, mensaje, null);
    }
}
