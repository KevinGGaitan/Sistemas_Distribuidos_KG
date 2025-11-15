#/**************************************************************
#                         Pontificia Universidad Javeriana
#     Autor: Juan Bello, Kevin Garay, Arley Bernal
#     Fecha: 12 de Noviembre 2025
#     Materia: Sistemas Distribuidos
#**************************************************************/

all:
	mvn clean compile

clean:
	mvn clean

run-coordinador:
	mvn exec:java -Dexec.mainClass="com.example.Coordinador"
run-nodo1:
	mvn exec:java -Dexec.mainClass="com.example.Participante" -Dexec.args="nodo1 tcp://localhost:5555"
run-nodo2:
	mvn exec:java -Dexec.mainClass="com.example.Participante" -Dexec.args="nodo2 tcp://localhost:5555"