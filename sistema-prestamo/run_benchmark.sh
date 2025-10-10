#!/bin/bash
# ============================================================
# BENCHMARK AUTOMÁTICO - TESTS 1 Y 2
# ============================================================
# Ejecuta ambos tests:
#  1. Tiempo de respuesta promedio y desviación estándar
#  2. Cantidad total de solicitudes procesadas en 2 minutos
# Luego genera un archivo benchmark_results.csv
# ============================================================

GC_HOST="localhost"
GC_PORT=5555
ARCHIVO_REQUEST="src/main/resources/requests2.txt"
DURACION=120
ARCIVO_RESULT="resultados_benchmark.csv"

echo "========== BENCHMARK COMPLETO =========="

# Encabezado CSV
echo "PS Simultaneos,Tiempo Promedio (ms),Desv. Estandar (ms),Solicitudes Totales,Promedio por PS" > $ARCIVO_RESULT

# Iterar sobre los escenarios
for PS_COUNT in 4 6 10; do
    echo ""
    echo "Ejecutando pruebas con $PS_COUNT PS simultáneos..."
    rm -f times_ps_*.csv count_ps_*.txt ps_*.log

    # Lanzar PS simultáneos
    for i in $(seq 1 $PS_COUNT); do
        mvn -q exec:java -Dexec.mainClass=sd.library.PruebasRend.PSRend -Dexec.args="$ARCHIVO_REQUEST $GC_HOST $GC_PORT" > ps_$i.log 2>&1 &
    done

    echo "Esperando $DURACION segundos..."
    sleep $DURACION

    echo "Esperando que los PS terminen por sí solos..."
    sleep 5
    #pkill -f ProcesoSolicitante

    echo "Analizando tiempos..."
    mvn -q exec:java -Dexec.mainClass=sd.library.PruebasRend.AnalizadorTiempos > tiempos.tmp
    avg=$(grep "Tiempo promedio" tiempos.tmp | awk '{print $5}')
    std=$(grep "Desviación" tiempos.tmp | awk '{print $5}')

    echo "Analizando conteo..."
    mvn -q exec:java -Dexec.mainClass=sd.library.PruebasRend.AnalizadorConteo > conteo.tmp
    total=$(grep "Total de solicitudes procesadas" conteo.tmp | awk '{print $6}')
    perps=$(grep "Promedio por PS" conteo.tmp | awk '{print $5}')

    # Asegurar valores por defecto si no se obtuvieron
    avg=${avg:-0}
    std=${std:-0}
    total=${total:-0}
    perps=${perps:-0}

    echo "$PS_COUNT,$avg,$std,$total,$perps" >> $ARCIVO_RESULT

    echo "Resultados guardados: $PS_COUNT PS -> Promedio=${avg}ms, Total=${total}"
done

echo ""
echo "Benchmark completado. Resultados en: $ARCIVO_RESULT"
cat $ARCIVO_RESULT
