# ********
# Pontificia Universidad Javeriana
# author: Juan Bello, Kevin Garay, Arley Bernal
# Fecha: 12 de Septiembre 2025
# Materia: Sistemas Distribuidos
# ********

# Nombre del paquete y carpeta
SRC_DIR = src/threadsJarroba
PKG = threadsJarroba
BIN_DIR = bin
# Archivos fuente (todos los .java del paquete)
SOURCES = $(wildcard *.java)

# Compilador
JAVAC = javac
JAVA = java


# Compilar todos los .java en la carpeta bin/
all:
	@mkdir -p $(BIN_DIR)
	$(JAVAC) -d $(BIN_DIR) $(SOURCES)

# Ejecutar versión secuencial
secuencial: all
	$(JAVA) -cp $(BIN_DIR) $(PKG).Main

# Ejecutar versión con hilos
threads: all
	$(JAVA) -cp $(BIN_DIR) $(PKG).MainThread

# Ejecutar versión con Runnable
runnable: all
	$(JAVA) -cp $(BIN_DIR) $(PKG).MainRunnable

# Ejecutar todas las versiones
demo: all
	@echo ">>> Ejecutando versión secuencial"
	$(JAVA) -cp $(BIN_DIR) $(PKG).Main
	@echo ""
	@echo ">>> Ejecutando versión con Thread"
	$(JAVA) -cp $(BIN_DIR) $(PKG).MainThread
	@echo ""
	@echo ">>> Ejecutando versión con Runnable"
	$(JAVA) -cp $(BIN_DIR) $(PKG).MainRunnable

# Limpiar
clean:
	rm -rf $(BIN_DIR)