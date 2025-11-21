# Sistema Distribuido de Préstamo de Libros – Sistemas Distribuidos

# Ejecución del Proyecto

## Requisitos previos

- Java 8 o superior
- ZeroMQ 4.3.4
- Maven 3.6+
- Linux o Windows con make instalado

## Compilación

```bash
make build
```

## Ejecución de los componentes

```bash
# Gestor de Carga (Sede 1)
make run_gc1
```

```bash
# Gestor de Almacenamiento Primario (Sede 1)
make run_ga1
```

```bash
# Gestor de Almacenamiento Secundario (Sede 2)
make run_ga2
```

```bash
# Actores (Préstamos, Renovaciones, Devoluciones)
make run_actors
```

```bash
# Procesos Solicitantes (Generadores de Carga)
make run_ps
```

## Pruebas de Failover

1. Ejecutar el sistema completo.
2. Interrumpir manualmente el proceso del GA primario.
3. Verificar que las solicitudes se redirigen automáticamente al GAS.
4. Reiniciar el GA primario y observar la resincronización.

## Limpieza

```bash
make clean
```
