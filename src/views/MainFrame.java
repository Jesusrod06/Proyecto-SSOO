/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package views;

import Models.CPU;
import Models.InterruptGenerator;
import Models.PCB;
import Scheduler.Scheduler;
import edd.Lista;

import javax.swing.*;
import java.awt.*;
import java.util.concurrent.Semaphore;

public class MainFrame extends JFrame {

    public MainFrame(CPU cpu, InterruptGenerator interruptor, Scheduler scheduler,
                     Lista<PCB> nuevos, Lista<PCB> listos, Lista<PCB> bloqueados, Lista<PCB> terminados,
                     Lista<PCB> listoSuspendido, Lista<PCB> bloqueadoSuspendido, Semaphore mutex) {

        setTitle("RTOS Simulator - Microsatellite Mission");
        setSize(1300, 800);
        setLocationRelativeTo(null);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLayout(new BorderLayout());

        JTabbedPane tabs = new JTabbedPane();

        // Tab 1: Mission Control
        MissionControlPanel mission = new MissionControlPanel(
                cpu, interruptor, scheduler,
                nuevos, listos, bloqueados, terminados,
                mutex
        );

        // Tab 2: Memory & Swap (usa tu panel existente si ya lo tienes)
        // Si tu clase se llama distinto, cambia aquí el nombre.
        MemoryManagementPanel memory = new MemoryManagementPanel(
                cpu, nuevos, listos, bloqueados,
                listoSuspendido, bloqueadoSuspendido,
                mutex
        );

        tabs.addTab("🚀 Mission Control", mission);
        tabs.addTab("💾 Memory Management & Swap", memory);

        add(tabs, BorderLayout.CENTER);
    }
}