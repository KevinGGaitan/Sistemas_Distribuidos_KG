/**************************************************************
#                         Pontificia Universidad Javeriana
#     Autor: Juan Bello, Kevin Garay, Arley Bernal
#     Fecha: 12 de Noviembre 2025
#     Materia: Sistemas Distribuidos
#**************************************************************/

package com.example;

public enum Mensaje {
    PREPARE,           // Coordinador -> Participantes
    VOTE_COMMIT,       // Participantes -> Coordinador
    VOTE_ABORT,        // Participantes -> Coordinador
    GLOBAL_COMMIT,     // Coordinador -> Participantes
    GLOBAL_ABORT,      // Coordinador -> Participantes
    ACK                // Participantes -> Coordinador
}