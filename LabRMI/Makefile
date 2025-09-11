#**********************
#      Pontificia Universidad Javeriana
#     Autor: Juan Bello, Kevin Garay, Arley Bernal
#     Fecha: 4 de Septiembre 2025
#     Materia: Sistemas Distribuidos
#**********************/
# Compilador Java
JAVAC = javac

# Opciones de compilación
JFLAGS = -g

# Compilar todos los archivos
all: servidor cliente

# Compilar archivos del servidor
servidor:
	rmiregistry &
	$(JAVAC) Sumador.java SumadorImpl.java SumadorServer.java

# Compilar archivos del cliente
cliente:
	$(JAVAC) SumadorClient.java

# Ejecutar el servidor SumadorServer
run-sumadorserver: 
	java -classpath . -Djava.security.policy=./policy SumadorServer

# Ejecutar el cliente SumadorClient (con localhost como servidor por defecto)
run-sumadorclient:
	java SumadorClient localhost

# Limpiar archivos compilados
clean:
	rm -f *.class

.PHONY: all clean help run-sumadorserver run-sumadorclient