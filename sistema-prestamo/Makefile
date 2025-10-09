#/**************************************************************
#                         Pontificia Universidad Javeriana
#     Autor: Juan Bello, Kevin Garay, Arley Bernal
#     Fecha: 30 de Septiembre 2025
#     Materia: Sistemas Distribuidos
#**************************************************************/

# Compilador Java
JAVAC = javac

# Opciones de compilación
IPGC = localhost
IPGA = localhost
IPGAS = localhost
PORTGA = 5570
PORTGAS = 5580
PORTGC = 5555
PORTRA = 5560
PORTSUB = 5556
INVENTARIOP = data/primario.json
INVENTARIOS = data/secundario.json

# Compilar todos los archivos
all:
	mvn clean package

# Ejecutar el Gestor de Almacenamiento primario
run-GA: 
	mvn exec:java -Dexec.mainClass=sd.library.GestorAlmacenamiento -Dexec.args="$(PORTGA) $(INVENTARIOP) $(IPGAS) $(PORTGAS)"

# Ejecutar el Gestor de Almacenamiento secundario
run-GAS: 
	mvn exec:java -Dexec.mainClass=sd.library.GestorAlmacenamiento -Dexec.args="$(PORTGAS) $(INVENTARIOS)"

# Ejecutar Gestor de carga
run-GC: 
	mvn exec:java -Dexec.mainClass=sd.library.GestorCarga -Dexec.args="$(PORTGC) $(PORTSUB) $(PORTRA)"

# Ejecutar Actor de Prestamos
run-AP:
	mvn exec:java -Dexec.mainClass=sd.library.ActorPrestamo -Dexec.args="$(IPGC) $(PORTSUB) $(IPGA) $(PORTGA) $(IPGC) $(PORTRA)"

# Ejecura Actor de Renovaciones y Devoluciones
run-AS:
	mvn exec:java -Dexec.mainClass=sd.library.ActorSuscriptor -Dexec.args="$(IPGC) $(PORTSUB) $(IPGA) $(PORTGA) $(IPGC) $(PORTRA)"
	
# Ejecutar el proceso cliente automatico
run-PS:
	mvn exec:java -Dexec.mainClass=sd.library.ProcesoSolicitante -Dexec.args="src/main/resources/requests1.txt $(IPGC) $(PORTGC)"
	
# Limpiar archivos compilados
clean:
	mvn clean

.PHONY: all clean