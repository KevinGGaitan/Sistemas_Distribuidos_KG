#/**************************************************************
#                         Pontificia Universidad Javeriana
#     Autor: Juan Bello, Kevin Garay, Arley Bernal
#     Fecha: 22 de Septiembre 2025
#     Materia: Sistemas Distribuidos
#**************************************************************/

# Compilador Java
JAVAC = javac

# Opciones de compilación
IPSERVER = 10.43.102.7
INVENTARIO = inventario.txt

# Compilar todos los archivos
all:
#	rmiregistry &
	$(JAVAC) *.java

# Ejecutar el servidor SumadorServer
run-server: 
	@java -classpath . -Djava.security.policy=./policy BibliotecaServer $(IPSERVER) $(INVENTARIO) 2>/dev/null


# Ejecutar el cliente SumadorClient (con localhost como servidor por defecto)
run-client:
	java Cliente $(IPSERVER)

# Limpiar archivos compilados
clean:
	rm -f *.class

.PHONY: all clean