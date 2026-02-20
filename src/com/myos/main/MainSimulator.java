/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Main.java to edit this template
 */
package com.myos.main;

import Models.CPU;
import Models.InterruptGenerator;
import Models.PCB;
import Scheduler.Scheduler;
import Scheduler.PolicyType;
import edd.Lista;

import java.util.concurrent.Semaphore;

public class MainSimulator {

    public static void main(String[] args) {
        // Colas
        Lista<PCB> nuevos = new Lista<>();
        Lista<PCB> listos = new Lista<>();
        Lista<PCB> bloqueados = new Lista<>();
        Lista<PCB> terminados = new Lista<>();
        Lista<PCB> listoSusp = new Lista<>();
        Lista<PCB> bloqSusp = new Lista<>();

        // Semáforo
        Semaphore mutex = new Semaphore(1);

        // Scheduler (ej. RR quantum 4)
        Scheduler scheduler = new Scheduler(PolicyType.ROUND_ROBIN, 4);

        // Kernel CPU
        CPU cpu = new CPU(nuevos, listos, bloqueados, terminados, listoSusp, bloqSusp,
                mutex, scheduler,
                250,  // ciclo ms (editable)
                15    // max procesos en memoria
        );

        // Procesos iniciales aleatorios (sin archivos)
        // (lo ideal: esto lo hagas desde una clase ProcessFactory)
        for (int i = 0; i < 8; i++) {
            boolean io = (i % 2 == 0);
            PCB p = new PCB("P" + (i+1),
                    20 + (i * 3),
                    1 + (i % 5),
                    60 + (i * 5),
                    io,
                    5 + (i % 4),
                    6 + (i % 5)
            );
            nuevos.addLast(p);
        }

        // Interrupciones
        InterruptGenerator ig = new InterruptGenerator(cpu);

        // Start
        cpu.start();
        ig.start();
        cpu.startSimulation();

        // TODO: aquí deberías abrir tu GUI Swing (JFrame) y pasarle referencias a cpu/colas/scheduler/mutex.
        System.out.println("Simulador iniciado (por ahora sin GUI).");
    }
}