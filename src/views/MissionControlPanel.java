/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package views;
import Models.CPU;
import Models.InterruptGenerator;
import Models.PCB;
import Scheduler.PolicyType;
import Scheduler.Scheduler;
import edd.Lista;

import javax.swing.*;
import javax.swing.border.TitledBorder;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.DefaultTableCellRenderer;
import java.awt.*;
import java.util.concurrent.Semaphore;

public class MissionControlPanel extends JPanel {

    private final CPU cpu;
    private final InterruptGenerator interruptor;
    private final Scheduler scheduler;
    private final Semaphore mutex;

    private final Lista<PCB> nuevos, listos, bloqueados, terminados;
    private ProcessTableModel mtListos, mtBloqueados, mtNuevos, mtTerminados;

    private JLabel lblReloj, lblCpuProcess, lblDeadline;
    private JProgressBar pbCpu;
    private JButton btnStart, btnStop, btnEmergency;
    private boolean hilosIniciados = false;
    
    // Colores temáticos
    private final Color BG_COLOR = new Color(15, 15, 30);
    private final Color BORDER_PURPLE = new Color(138, 43, 226);
    private final Color TEXT_CYAN = new Color(0, 255, 255);

    public MissionControlPanel(CPU cpu, InterruptGenerator interruptor, Scheduler scheduler,
                               Lista<PCB> nuevos, Lista<PCB> listos, Lista<PCB> bloqueados, Lista<PCB> terminados,
                               Lista<PCB> listoSuspendido, Lista<PCB> bloqueadoSuspendido, Semaphore mutex) {
        this.cpu = cpu;
        this.interruptor = interruptor;
        this.scheduler = scheduler;
        this.nuevos = nuevos;
        this.listos = listos;
        this.bloqueados = bloqueados;
        this.terminados = terminados;
        this.mutex = mutex;

        setLayout(new BorderLayout(10, 10));
        setBackground(BG_COLOR);
        setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        initUI();
        startGuiTimer();
    }

    private void initUI() {
        // --- HEADER ---
        JPanel panelNorte = new JPanel(new BorderLayout());
        panelNorte.setOpaque(false);
        
        lblReloj = new JLabel("MISSION CLOCK: Cycle 0");
        lblReloj.setFont(new Font("Consolas", Font.BOLD, 24));
        lblReloj.setForeground(TEXT_CYAN);
        lblReloj.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(Color.GRAY, 2, true),
                BorderFactory.createEmptyBorder(5, 15, 5, 15)
        ));
        
        JPanel pnlReloj = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        pnlReloj.setOpaque(false);
        pnlReloj.add(lblReloj);
        panelNorte.add(pnlReloj, BorderLayout.EAST);
        add(panelNorte, BorderLayout.NORTH);

        // --- CENTER: 3 Columns ---
        JPanel panelCentral = new JPanel(new GridLayout(1, 3, 20, 0));
        panelCentral.setOpaque(false);

        mtListos = new ProcessTableModel(listos);
        mtBloqueados = new ProcessTableModel(bloqueados);
        
        // 1. Ready Queue
        panelCentral.add(crearPanelTabla("READY QUEUE", mtListos));

        // 2. CPU / Running Process
        JPanel panelCPU = new JPanel(new BorderLayout(0, 20));
        panelCPU.setOpaque(false);
        
        JPanel cpuDisplay = new JPanel(new GridLayout(3, 1));
        cpuDisplay.setBackground(new Color(20, 20, 40));
        cpuDisplay.setBorder(crearBorde("RUNNING PROCESS (CPU)"));
        
        lblCpuProcess = new JLabel("[IDLE]", SwingConstants.CENTER);
        lblCpuProcess.setForeground(Color.WHITE);
        lblCpuProcess.setFont(new Font("SansSerif", Font.BOLD, 16));
        
        pbCpu = new JProgressBar(0, 100);
        pbCpu.setStringPainted(true);
        pbCpu.setForeground(TEXT_CYAN);
        pbCpu.setBackground(Color.DARK_GRAY);
        
        lblDeadline = new JLabel("Deadline in: -- cycles", SwingConstants.CENTER);
        lblDeadline.setForeground(Color.LIGHT_GRAY);
        
        cpuDisplay.add(lblCpuProcess);
        cpuDisplay.add(pbCpu);
        cpuDisplay.add(lblDeadline);
        
        panelCPU.add(cpuDisplay, BorderLayout.NORTH);

        btnEmergency = new JButton("<html><center>EMERGENCY INTERRUPTION<br>(MICRO-METEORITE)</center></html>");
        btnEmergency.setBackground(new Color(200, 0, 0));
        btnEmergency.setForeground(Color.WHITE);
        btnEmergency.setFont(new Font("SansSerif", Font.BOLD, 14));
        btnEmergency.setFocusPainted(false);
        btnEmergency.setPreferredSize(new Dimension(200, 80));
        
        JPanel pnlBtn = new JPanel();
        pnlBtn.setOpaque(false);
        pnlBtn.add(btnEmergency);
        panelCPU.add(pnlBtn, BorderLayout.CENTER);

        panelCentral.add(panelCPU);

        // 3. Blocked Queue
        panelCentral.add(crearPanelTabla("BLOCKED QUEUE (I/O)", mtBloqueados));
        add(panelCentral, BorderLayout.CENTER);

        // --- SOUTH: Controls & Secondary queues ---
        JPanel panelSur = new JPanel(new BorderLayout(10, 10));
        panelSur.setOpaque(false);
        
        JPanel controles = new JPanel(new FlowLayout(FlowLayout.CENTER, 15, 10));
        controles.setOpaque(false);
        btnStart = new JButton("▶ Start Simulation");
        btnStop = new JButton("⏹ Pause");
        JButton btnAdd = new JButton("➕ Add Process");
        btnStop.setEnabled(false);
        
        controles.add(btnStart);
        controles.add(btnStop);
        controles.add(btnAdd);

        JPanel secundario = new JPanel(new GridLayout(1, 2, 10, 0));
        secundario.setOpaque(false);
        mtNuevos = new ProcessTableModel(nuevos);
        mtTerminados = new ProcessTableModel(terminados);
        secundario.add(crearPanelTabla("NEW QUEUE", mtNuevos));
        secundario.add(crearPanelTabla("TERMINATED", mtTerminados));
        secundario.setPreferredSize(new Dimension(0, 150));

        panelSur.add(controles, BorderLayout.NORTH);
        panelSur.add(secundario, BorderLayout.CENTER);
        add(panelSur, BorderLayout.SOUTH);

        // --- EVENTOS ---
        btnStart.addActionListener(e -> {
            if (!hilosIniciados) {
                cpu.start();
                interruptor.start();
                hilosIniciados = true;
            }
            cpu.startSimulation();
            btnStart.setEnabled(false);
            btnStop.setEnabled(true);
        });

        btnStop.addActionListener(e -> {
            cpu.stopSimulation();
            btnStart.setEnabled(true);
            btnStop.setEnabled(false);
        });

        btnAdd.addActionListener(e -> abrirDialogoProceso());
        
        // Simular interrupción manual
        btnEmergency.addActionListener(e -> System.out.println("EMERGENCY INTERRUPT TRIGGERED!")); 
    }

    private TitledBorder crearBorde(String titulo) {
        return BorderFactory.createTitledBorder(
                BorderFactory.createLineBorder(BORDER_PURPLE, 2, true),
                titulo, TitledBorder.CENTER, TitledBorder.TOP,
                new Font("SansSerif", Font.BOLD, 14), TEXT_CYAN);
    }

    private JPanel crearPanelTabla(String titulo, AbstractTableModel modelo) {
        JPanel p = new JPanel(new BorderLayout());
        p.setOpaque(false);
        p.setBorder(crearBorde(titulo));
        
        JTable t = new JTable(modelo);
        t.setBackground(new Color(25, 25, 40));
        t.setForeground(Color.WHITE);
        t.getTableHeader().setBackground(Color.BLACK);
        t.getTableHeader().setForeground(TEXT_CYAN);
        t.setFillsViewportHeight(true);
        
        JScrollPane sp = new JScrollPane(t);
        sp.getViewport().setBackground(new Color(25, 25, 40));
        p.add(sp, BorderLayout.CENTER);
        return p;
    }

    private void abrirDialogoProceso() {
        NewProcessDialog dlg = new NewProcessDialog((Frame) SwingUtilities.getWindowAncestor(this));
        PCB p = dlg.getProcess();
        if (p == null) return;
        new Thread(() -> {
            try {
                mutex.acquire();
                nuevos.addLast(p);
            } catch (InterruptedException ignored) {} 
            finally { mutex.release(); }
        }).start();
    }

    private void startGuiTimer() {
        Timer guiTimer = new Timer(100, e -> {
            if (mutex.tryAcquire()) {
                try {
                    mtListos.fireTableDataChanged();
                    mtBloqueados.fireTableDataChanged();
                    mtNuevos.fireTableDataChanged();
                    mtTerminados.fireTableDataChanged();

                    lblReloj.setText("MISSION CLOCK: Cycle " + cpu.getCicloGlobal());

                    PCB r = cpu.getRunning();
                    if (r != null) {
                        lblCpuProcess.setText(r.getNombre() + " [ID:" + r.getId() + "]");
                        lblDeadline.setText("Deadline in: " + r.getDeadlineRestante() + " cycles");
                        pbCpu.setValue(r.getRestantes()); // Simplificado para mostrar algo de movimiento
                        pbCpu.setString("Rem: " + r.getRestantes() + " cycles");
                    } else {
                        lblCpuProcess.setText("[IDLE]");
                        lblDeadline.setText("Deadline in: -- cycles");
                        pbCpu.setValue(0);
                        pbCpu.setString("");
                    }
                } finally {
                    mutex.release();
                }
            }
        });
        guiTimer.start();
    }

    private static class ProcessTableModel extends AbstractTableModel {
        private final Lista<PCB> lista;
        private final String[] cols = {"Process", "Priority", "Remaining"};

        public ProcessTableModel(Lista<PCB> lista) { this.lista = lista; }
        @Override public int getRowCount() { return lista.size(); }
        @Override public int getColumnCount() { return cols.length; }
        @Override public String getColumnName(int c) { return cols[c]; }
        @Override public Object getValueAt(int r, int c) {
            PCB p = lista.get(r);
            if (p == null) return null;
            return switch (c) {
                case 0 -> p.getNombre();
                case 1 -> p.getPrioridad();
                case 2 -> p.getRestantes();
                default -> null;
            };
        }
    }
}