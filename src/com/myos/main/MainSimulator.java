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
import views.MainFrame;

import java.io.OutputStream;
import java.io.PrintStream;
import java.util.concurrent.Semaphore;
import javax.swing.SwingUtilities;

public class MainSimulator {

    public static void main(String[] args) {

        // 1) Estructuras personalizadas
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

        // 4) BLOQUEAR TERMINAL (nadie imprime en consola)
        silenceConsole();

        // 5) Capturar cualquier excepción global y mandarla al log
        Thread.setDefaultUncaughtExceptionHandler((t, e) -> cpu.logError("Uncaught exception en thread: " + t.getName(), e));

        // 6) Arrancar threads UNA SOLA VEZ
        cpu.start();
        interruptor.start();

        // 7) Lanzar GUI en EDT
        SwingUtilities.invokeLater(() -> {
            MainFrame ventana = new MainFrame(
                    cpu, interruptor, scheduler,
                    nuevos, listos, bloqueados, terminados,
                    listoSuspendido, bloqueadoSuspendido,
                    mutex
            );
            ventana.setVisible(true);
        });
    }

    private static void silenceConsole() {
        System.setOut(new PrintStream(new OutputStream() {
            @Override public void write(int b) { }
        }));

        System.setErr(new PrintStream(new OutputStream() {
            @Override public void write(int b) { }
        }));
    }
}