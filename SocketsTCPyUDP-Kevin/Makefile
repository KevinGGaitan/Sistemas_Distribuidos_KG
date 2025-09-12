# Makefile para compilar y ejecutar los ejemplos de sockets (TCP y UDP)
JAVAC = javac
JAVA  = java

# Lista de fuentes
SRCS = sockettcpser.java sockettcpcli.java socketudpser.java socketudpcli.java

# Target por defecto: compilar todo
all: compile

# Compilación
compile: $(SRCS)
	$(JAVAC) $(SRCS)

# Reglas de ejecución
# TCP
run-tcp-server: compile
	$(JAVA) sockettcpser

run-tcp-client: compile
	$(JAVA) sockettcpcli $(ARGS)

# UDP
run-udp-server: compile
	$(JAVA) socketudpser

run-udp-client: compile
	$(JAVA) socketudpcli $(ARGS)

# Limpiar archivos .class
clean:
	@echo "Eliminando archivos .class..."
	-@rm -f *.class