/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Main.java to edit this template
 */
package com.myos.main;
import Models.CPU;
import Models.InterruptGenerator;
import Models.PCB;
import Scheduler.PolicyType;
import Scheduler.Scheduler;
import edd.Lista;
import views.MainFrame; // Importamos tu ventana principal

import java.util.concurrent.Semaphore;
import javax.swing.SwingUtilities;

public class MainSimulator {
    public static void main(String[] args) {
        
        // 1. Instanciamos nuestras estructuras de datos 100% personalizadas
        Lista<PCB> nuevos = new Lista<>();
        Lista<PCB> listos = new Lista<>();
        Lista<PCB> bloqueados = new Lista<>();
        Lista<PCB> terminados = new Lista<>();
        Lista<PCB> listoSuspendido = new Lista<>();
        Lista<PCB> bloqueadoSuspendido = new Lista<>();

        // 2. Creamos el Semáforo de control de concurrencia y el Planificador
        Semaphore mutex = new Semaphore(1);
        Scheduler scheduler = new Scheduler(PolicyType.ROUND_ROBIN, 3); // Por defecto inicia en RR con Quantum 3

        // 3. Ensamblamos la CPU (El cerebro)
        int velocidadRelojMs = 500; // 500 milisegundos (medio segundo) por ciclo para que alcancemos a leer la pantalla
        int maxRAM = 5; // Límite de la memoria multiprogramada
        
        CPU cpu = new CPU(nuevos, listos, bloqueados, terminados, listoSuspendido, bloqueadoSuspendido, 
                          mutex, scheduler, velocidadRelojMs, maxRAM);

        // 4. Conectamos el hardware generador de interrupciones
        InterruptGenerator interruptor = new InterruptGenerator(cpu);

        // 5. ¡Lanzamos la Interfaz Gráfica de forma segura (Hilo de Eventos de Swing)!
        SwingUtilities.invokeLater(() -> {
            MainFrame ventana = new MainFrame(
                cpu, interruptor, scheduler, nuevos, listos, bloqueados, terminados, listoSuspendido, bloqueadoSuspendido, mutex
            );
            ventana.setVisible(true);
        });
    }
}

