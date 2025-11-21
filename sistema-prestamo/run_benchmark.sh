#!/bin/bash

#/**************************************************************
#                         Pontificia Universidad Javeriana
#     Autor: Juan Bello, Kevin Garay, Arley Bernal
#     Fecha: 8 de Octubre 2025
#     Materia: Sistemas Distribuidos
#**************************************************************/
# Ejecuta ambos tests:
#  1. Tiempo de respuesta promedio y desviación estándar
#  2. Cantidad total de solicitudes procesadas en 2 minutos
# Luego genera un archivo benchmark_results.csv
# ============================================================

GC_HOST="localhost"           # Host del Gestor de Carga
GC_PORT=5555                  # Puerto del Gestor de Carga
ARCHIVO_REQUEST="src/main/resources/requests2.txt"  # Archivo con solicitudes de prueba
DURACION=120                  # Duración del test en segundos (2 minutos)
ARCIVO_RESULT="resultados_benchmark.csv"  # Archivo de salida con resultados

echo "========== BENCHMARK COMPLETO =========="

# Crear archivo CSV con encabezado para resultados
echo "PS Simultaneos,Tiempo Promedio (ms),Desv. Estandar (ms),Solicitudes Totales,Promedio por PS" > $ARCIVO_RESULT

# Prueba con 4, 6 y 10 Procesos Solicitantes simultáneos
for PS_COUNT in 4 6 10; do
    echo ""
    echo "Ejecutando pruebas con $PS_COUNT PS simultáneos..."
    
    # Eliminar archivos de ejecuciones anteriores
    rm -f times_ps_*.csv count_ps_*.txt ps_*.log

    # Lanzamiento de procesos solicitantes en paralelo usa & para ejecutar en background y crear carga concurrente
    for i in $(seq 1 $PS_COUNT); do
        # Ejecuta PSRend via Maven con parámetros específicos
        # -q: modo quiet para reducir output
        # exec:java: ejecuta clase Java directamente
        # > ps_$i.log 2>&1: redirige stdout y stderr a archivo log
        mvn -q exec:java -Dexec.mainClass=sd.library.PruebasRend.PSRend -Dexec.args="$ARCHIVO_REQUEST $GC_HOST $GC_PORT" > ps_$i.log 2>&1 &
    done

    # Eespera para la ejecucion del test
    echo "Esperando $DURACION segundos..."
    sleep $DURACION  # Espera exacta de 2 minutos

    echo "Esperando que los PS terminen por sí solos..."
    sleep 5  # Tiempo de gracia para finalización natural
    
    #pkill -f PSRend

    echo "Esperando que los PS terminen de escribir resultados..."
    sleep 10  # Espera adicional para garantizar escritura de archivos

    # Busca archivos de tiempos para confirmar ejecución exitosa
    count_files=$(ls times_ps_*.csv 2>/dev/null | wc -l)
    if [ "$count_files" -eq 0 ]; then
        echo "No se encontraron archivos de tiempos. Esperando 10s más..."
        sleep 10  # Espera adicional si no hay archivos
    fi

    # Analisis de resultados tiempo
    echo "Analizando tiempos..."
    # Ejecuta AnalizadorTiempos via Maven y guarda output temporal
    mvn -q exec:java -Dexec.mainClass=sd.library.PruebasRend.AnalizadorTiempos > tiempos.tmp
    
    # Extraccion de metricas desde Output
    # grep + cut: filtra y extrae valores específicos
    avg=$(grep "TiempoPromedio" tiempos.tmp | cut -d'=' -f2)    # Tiempo promedio
    std=$(grep "DesviacionEstandar" tiempos.tmp | cut -d'=' -f2) # Desviación estándar

    # AAnalisis de resultados conteo
    echo "Analizando conteo..."
    # Ejecuta AnalizadorConteo para obtener estadísticas de throughput
    mvn -q exec:java -Dexec.mainClass=sd.library.PruebasRend.AnalizadorConteo > conteo.tmp
    
    # Extraccion de metricas de THROUGHPUT
    total=$(grep "SolicitudesTotales" conteo.tmp | cut -d'=' -f2)    # Total procesadas
    perps=$(grep "PromedioPorPS" conteo.tmp | cut -d'=' -f2)         # Promedio por PS

    # Valores por defecto para evitar errores
    avg=${avg:-0}
    std=${std:-0}
    total=${total:-0}
    perps=${perps:-0}

    # Regitra resultados en formato CSV
    # Formato: PSConcurrentes,AvgTiempo,DevStd,TotaldeRequests,AvgPorPS
    echo "$PS_COUNT,$avg,$std,$total,$perps" >> $ARCIVO_RESULT

    echo "Resultados guardados: $PS_COUNT PS -> Promedio=${avg}ms, Total=${total}"
done

# Resumenfinal
echo ""
echo "Benchmark completado. Resultados en: $ARCIVO_RESULT"
cat $ARCIVO_RESULT  # Muestra resultados finales