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

        setTitle("UNIMET-Sat RTOS Simulator");
        setSize(1300, 800);
        setLocationRelativeTo(null);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        getContentPane().setBackground(new Color(10, 10, 25)); // Fondo oscuro base

        // Personalizar colores de las pestañas
        UIManager.put("TabbedPane.background", new Color(30, 30, 50));
        UIManager.put("TabbedPane.foreground", Color.WHITE);
        UIManager.put("TabbedPane.selected", new Color(138, 43, 226)); // Morado

        JTabbedPane tabs = new JTabbedPane();
        tabs.setFont(new Font("SansSerif", Font.BOLD, 14));

        JPanel missionControl = new MissionControlPanel(
                cpu, interruptor, scheduler,
                nuevos, listos, bloqueados, terminados,
                listoSuspendido, bloqueadoSuspendido, mutex
        );

        JPanel memoryPanel = new MemoryManagementPanel(
                cpu, nuevos, listos, bloqueados,
                listoSuspendido, bloqueadoSuspendido, mutex
        );

        tabs.addTab("🚀 Mission Control", missionControl);
        tabs.addTab("💾 Memory Management & Swap", memoryPanel);

        add(tabs, BorderLayout.CENTER);
    }
}