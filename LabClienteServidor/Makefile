#**************************************************************
#         		Pontificia Universidad Javeriana
#     Autor: Juan Bello, Kevin Garay, Arley Bernal
#     Fecha: 28 de Agosto 2025
#     Materia: Sistemas Distribuidos
#****************************************************************/

JAVAC = javac								# Compilador de Java
JAVA = java									# Intérprete de Java
SERVER_CLASS = MultithreadedSocketServer	# Clase principal del servidor
CLIENT_CLASS = TCPClient					# Clase principal del cliente
SERVER_PORT = 8888							# Puerto del servidor


all: servidor cliente			# Objetivo por defecto: compilar servidor y cliente

# Compilar archivos del servidor
servidor:
	$(JAVAC) MultithreadedSocketServer.java ServerClientThread.java

# Compilar archivos del cliente
cliente:
	$(JAVAC) TCPClient.java

# Ejecutar el servidor
run_servidor:
	$(JAVA) $(SERVER_CLASS)

# Ejecutar el cliente
run_cliente:
	$(JAVA) $(CLIENT_CLASS)

# Limpiar archivos compilados
clean:
	rm -f *.class

# Declarar objetivos que no son archivos
.PHONY: all compilar_servidor compilar_cliente ejecutar_servidor ejecutar_cliente limpiar
