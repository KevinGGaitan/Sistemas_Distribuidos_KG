# /**********************
#      Pontificia Universidad Javeriana
#     author:Juan Bello, Kevin Garay, Arley Bernal
#     Fecha: 12 de Septiembre 2025
#     Materia: Sistemas Distribuidos
#**********************/
# Compilador Java
# Nombre del paquete y carpetas
SRC_DIR = src
BIN_DIR = bin
PKG = threadsJarroba

# Archivos fuente y clases
SOURCES = $(wildcard $(SRC_DIR)/$(PKG)/*.java)
CLASSES = $(SOURCES:$(SRC_DIR)/%.java=$(BIN_DIR)/%.class)

# Compilador
JAVAC = javac
JAVA = java

# Regla por defecto: compilar todo
all: $(CLASSES)

# Compilar clases
$(BIN_DIR)/%.class: $(SRC_DIR)/%.java
	@mkdir -p $(BIN_DIR)
	$(JAVAC) -d $(BIN_DIR) $<

# Ejecutar versión secuencial (Cajera + Main)
secuencial: all
	$(JAVA) -cp $(BIN_DIR) $(PKG).Main

# Ejecutar versión con hilos que extiende Thread
threads: all
	$(JAVA) -cp $(BIN_DIR) $(PKG).MainThread

# Ejecutar versión con Runnable
runnable: all
	$(JAVA) -cp $(BIN_DIR) $(PKG).MainRunnable


# Ejecutar todas las versiones en orden
demo: all
	@echo ">>> Ejecutando versión secuencial"
	$(JAVA) -cp $(BIN_DIR) $(PKG).Main
	@echo ""
	@echo ">>> Ejecutando versión con Thread"
	$(JAVA) -cp $(BIN_DIR) $(PKG).MainThread
	@echo ""
	@echo ">>> Ejecutando versión con Runnable"
	$(JAVA) -cp $(BIN_DIR) $(PKG).MainRunnable


# Limpiar clases compiladas
clean:
	rm -rf $(BIN_DIR)/*
.PHONY: all clean secuencial threads runnable