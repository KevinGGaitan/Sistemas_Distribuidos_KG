# Sistema Cliente-Servidor Multihilo en Java

## Descripción
Este proyecto implementa un sistema cliente-servidor multihilo en Java que permite a múltiples clientes conectarse simultáneamente a un servidor para calcular el cuadrado de números enviados. El servidor gestiona cada conexión cliente en un hilo independiente, demostrando conceptos fundamentales de sistemas distribuidos.

## Características
- **Comunicación TCP**: Uso de sockets para comunicación confiable
- **Arquitectura multihilo**: Manejo concurrente de múltiples clientes
- **Protocolo simple**: Comunicación basada en mensajes de texto
- **Operación matemática**: Cálculo del cuadrado de números enviados
- **Finalización controlada**: Uso de la palabra "bye" para cerrar conexiones

## Estructura del Proyecto
```
.
├── MultithreadedSocketServer.java  # Servidor principal
├── ServerClientThread.java         # Hilo para cada cliente
├── TCPClient.java                  # Cliente
└── Makefile                        # Automatización de tareas
```

## Compilación y Ejecución

### Requisitos
- Java JDK 8 o superior

### Usando Makefile

**Compilar todo el proyecto:**
```bash
make
```

**Ejecutar el servidor:**
```bash
make run_servidor
```

**Ejecutar el cliente:**
```bash
make run_cliente
```

**Limpiar archivos compilados:**
```bash
make clean
```

## Uso del Sistema

1. **Iniciar el servidor:**
   ```bash
   make run_servidor
   ```
   El servidor iniciará en el puerto 8888 y mostrará: "Server Started ...."

2. **Ejecutar clientes:**
   ```bash
   make run_cliente
   ```
   Puede ejecutar múltiples instancias del cliente en terminales diferentes.

3. **Interactuar con el sistema:**
   - Ingrese números para calcular su cuadrado

4. **Ejemplo de sesión:**
   ```
   Enter number :
   5
   From Server to Client-1 Square of 5 is 25
   Enter number :
   12
   From Server to Client-1 Square of 12 is 144
   ```

## Configuración

- **Puerto del servidor**: 8888 (configurable en el Makefile)
- **Dirección del servidor**: 127.0.0.1 (localhost), este se puede cambiar por la direccion ip del servidor dentro de TCPClient.java

## Arquitectura

### Servidor (`MultithreadedSocketServer`)
- Crea un socket servidor en el puerto 8888
- Acepta conexiones entrantes de clientes
- Crea un nuevo hilo (`ServerClientThread`) por cada cliente
- Mantiene un contador de clientes conectados

### Hilo del Cliente (`ServerClientThread`)
- Maneja la comunicación individual con cada cliente
- Procesa mensajes y calcula el cuadrado de números
- Gracia la finalización de la conexión con "bye"

### Cliente (`TCPClient`)
- Establece conexión con el servidor
- Lee entrada del usuario y envía al servidor
- Muestra las respuestas recibidas

## Autor
Juan Esteban Bello Durango  
Pontificia Universidad Javeriana  
Sistemas Distribuidos
