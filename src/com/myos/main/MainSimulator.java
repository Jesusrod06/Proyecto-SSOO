/*
ya tu  * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Main.java to edit this template
 */
package com.myos.main;
import Models.CPU;
import Models.InterruptGenerator;
import Models.PCB;
import Scheduler.PolicyType;
import Scheduler.Scheduler;
import edd.Lista;

import java.util.concurrent.Semaphore;
import javax.swing.SwingUtilities;
import views.MainFrame;

public class MainSimulator {
    public static void main(String[] args) {
        // 1) Estructuras (Memoria RAM y Swap)
        Lista<PCB> nuevos = new Lista<>();
        Lista<PCB> listos = new Lista<>();
        Lista<PCB> bloqueados = new Lista<>();
        Lista<PCB> terminados = new Lista<>();
        Lista<PCB> listoSuspendido = new Lista<>();
        Lista<PCB> bloqueadoSuspendido = new Lista<>();

        // 2) Concurrencia y planificación
        Semaphore mutex = new Semaphore(1);
        Scheduler scheduler = new Scheduler(PolicyType.ROUND_ROBIN, 3);

        // 3) Motor RTOS
        int velocidadRelojMs = 500;
        int maxRAM = 5;

        CPU cpu = new CPU(
                nuevos, listos, bloqueados, terminados,
                listoSuspendido, bloqueadoSuspendido,
                mutex, scheduler, velocidadRelojMs, maxRAM
        );
        InterruptGenerator interruptor = new InterruptGenerator(cpu);

        // 4) Interfaz Gráfica (UI)
        SwingUtilities.invokeLater(() -> {
            MainFrame ventana = new MainFrame(
                    cpu, interruptor, scheduler,
                    nuevos, listos, bloqueados, terminados,
                    listoSuspendido, bloqueadoSuspendido, mutex
            );
            ventana.setVisible(true);
        });
    }
}