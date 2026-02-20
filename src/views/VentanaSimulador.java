/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package views;

import Models.CPU;
import Models.Estado;
import Models.InterruptGenerator;
import Models.PCB;
import Scheduler.PolicyType;
import Scheduler.Scheduler;
import edd.Lista;
import edd.Nodo;

import javax.swing.*;
import javax.swing.table.AbstractTableModel;
import java.awt.*;
import java.util.concurrent.Semaphore;

public class VentanaSimulador extends JFrame {

    // Referencias al motor (Backend)
    private final CPU cpu;
    private final InterruptGenerator interruptor;
    private final Scheduler scheduler;
    private final Semaphore mutex;

    // Listas del sistema
    private final Lista<PCB> nuevos, listos, bloqueados, terminados, listoSuspendido, bloqueadoSuspendido;

    // Componentes Visuales
    private JLabel lblCpuRunning, lblReloj, lblMetricas;
    private JTextArea txtLog;
    private JComboBox<PolicyType> cbPoliticas;
    private JSpinner spQuantum;

    // Modelos de tablas personalizados (¡Sin ArrayLists internos!)
    private ProcessTableModel mtNuevos, mtListos, mtBloqueados, mtTerminados;

    public VentanaSimulador(CPU cpu, InterruptGenerator interruptor, Scheduler scheduler,
                            Lista<PCB> nuevos, Lista<PCB> listos, Lista<PCB> bloqueados, Lista<PCB> terminados,
                            Lista<PCB> listoSuspendido, Lista<PCB> bloqueadoSuspendido, Semaphore mutex) {
        
        this.cpu = cpu;
        this.interruptor = interruptor;
        this.scheduler = scheduler;
        this.nuevos = nuevos;
        this.listos = listos;
        this.bloqueados = bloqueados;
        this.terminados = terminados;
        this.listoSuspendido = listoSuspendido;
        this.bloqueadoSuspendido = bloqueadoSuspendido;
        this.mutex = mutex;

        // Configuración básica de la ventana
        setTitle("Simulador RTOS - Panel de Control Misión Espacial");
        setSize(1100, 700);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);
        setLayout(new BorderLayout(10, 10));

        inicializarUI();
        iniciarRelojGrafico();
    }

    private void inicializarUI() {
        // --- PANEL NORTE: Controles ---
        JPanel panelNorte = new JPanel(new FlowLayout(FlowLayout.LEFT, 15, 10));
        panelNorte.setBackground(new Color(40, 44, 52));

        JButton btnStart = new JButton("▶ Iniciar Simulación");
        JButton btnStop = new JButton("⏹ Detener");
        JButton btnAddProcess = new JButton("➕ Nuevo Proceso");

        cbPoliticas = new JComboBox<>(PolicyType.values());
        cbPoliticas.setSelectedItem(scheduler.getPolicy());
        
        spQuantum = new JSpinner(new SpinnerNumberModel(scheduler.getRrQuantum(), 1, 20, 1));

        // Estilos básicos
        btnStart.setBackground(new Color(76, 175, 80)); btnStart.setForeground(Color.WHITE);
        btnStop.setBackground(new Color(244, 67, 54)); btnStop.setForeground(Color.WHITE);

        panelNorte.add(btnStart);
        panelNorte.add(btnStop);
        panelNorte.add(btnAddProcess);
        panelNorte.add(new JLabel("<html><font color='white'>Política:</font></html>"));
        panelNorte.add(cbPoliticas);
        panelNorte.add(new JLabel("<html><font color='white'>Quantum (RR):</font></html>"));
        panelNorte.add(spQuantum);

        add(panelNorte, BorderLayout.NORTH);

        // --- PANEL CENTRAL: Colas de Procesos ---
        JPanel panelCentral = new JPanel(new GridLayout(2, 2, 10, 10));
        panelCentral.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        mtNuevos = new ProcessTableModel(nuevos);
        mtListos = new ProcessTableModel(listos);
        mtBloqueados = new ProcessTableModel(bloqueados);
        mtTerminados = new ProcessTableModel(terminados);

        panelCentral.add(crearPanelTabla("Cola de NUEVOS", mtNuevos));
        panelCentral.add(crearPanelTabla("Cola de LISTOS (RAM)", mtListos));
        panelCentral.add(crearPanelTabla("Cola de BLOQUEADOS (E/S)", mtBloqueados));
        panelCentral.add(crearPanelTabla("TERMINADOS", mtTerminados));

        add(panelCentral, BorderLayout.CENTER);

        // --- PANEL ESTE: Estado de la CPU y Métricas ---
        JPanel panelEste = new JPanel();
        panelEste.setLayout(new BoxLayout(panelEste, BoxLayout.Y_AXIS));
        panelEste.setPreferredSize(new Dimension(250, 0));
        panelEste.setBorder(BorderFactory.createTitledBorder("Monitor del Procesador"));

        lblReloj = new JLabel("Ciclo Global: 0");
        lblReloj.setFont(new Font("Consolas", Font.BOLD, 16));
        
        lblCpuRunning = new JLabel("<html><b>EJECUTANDO:</b><br>Ninguno<br><br>PC: 0<br>MAR: 0</html>");
        lblCpuRunning.setFont(new Font("Consolas", Font.PLAIN, 14));
        lblCpuRunning.setBorder(BorderFactory.createEmptyBorder(20, 0, 20, 0));

        lblMetricas = new JLabel("<html>Uso CPU: 0%<br>Éxito Misión: 0%</html>");

        panelEste.add(lblReloj);
        panelEste.add(lblCpuRunning);
        panelEste.add(new JSeparator());
        panelEste.add(lblMetricas);
        
        add(panelEste, BorderLayout.EAST);

        // --- PANEL SUR: Log del Sistema ---
        txtLog = new JTextArea(8, 50);
        txtLog.setEditable(false);
        txtLog.setBackground(Color.BLACK);
        txtLog.setForeground(Color.GREEN);
        txtLog.setFont(new Font("Consolas", Font.PLAIN, 12));
        JScrollPane scrollLog = new JScrollPane(txtLog);
        scrollLog.setBorder(BorderFactory.createTitledBorder("Log del Sistema"));
        add(scrollLog, BorderLayout.SOUTH);

        // --- EVENTOS (Action Listeners) ---
        btnStart.addActionListener(e -> {
            cpu.start(); // Inicia el hilo de la CPU (solo se puede llamar una vez)
            cpu.startSimulation();
            interruptor.start();
            btnStart.setEnabled(false);
        });

        btnStop.addActionListener(e -> cpu.stopSimulation());

        cbPoliticas.addActionListener(e -> {
            PolicyType pol = (PolicyType) cbPoliticas.getSelectedItem();
            scheduler.setPolicy(pol);
        });

        spQuantum.addChangeListener(e -> {
            int q = (int) spQuantum.getValue();
            scheduler.setRrQuantum(q);
        });

        btnAddProcess.addActionListener(e -> agregarProcesoAleatorio());
    }

    // Genera un panel con título y tabla
    private JPanel crearPanelTabla(String titulo, ProcessTableModel modelo) {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBorder(BorderFactory.createTitledBorder(titulo));
        JTable tabla = new JTable(modelo);
        panel.add(new JScrollPane(tabla), BorderLayout.CENTER);
        return panel;
    }

    // Simula la creación rápida de un proceso
    private void agregarProcesoAleatorio() {
        try {
            mutex.acquire();
            int idGenerico = nuevos.size() + listos.size() + terminados.size() + 1;
            // (String nombre, int instrucciones, int prioridad, int deadline, requiereIO, ciclosParaIO, duracionIO)
            PCB p = new PCB("PROC_" + idGenerico, 15, 2, 50, true, 5, 3);
            nuevos.addLast(p);
        } catch (InterruptedException ex) {
            ex.printStackTrace();
        } finally {
            mutex.release();
        }
    }

    // Hilo de la Interfaz (Timer) que actualiza la pantalla sin congelarse
    private void iniciarRelojGrafico() {
        Timer guiTimer = new Timer(100, e -> {
            try {
                // Bloqueamos rápido para leer datos sin que la CPU nos mueva los punteros
                mutex.acquire(); 
                
                // 1. Actualizar Tablas
                mtNuevos.fireTableDataChanged();
                mtListos.fireTableDataChanged();
                mtBloqueados.fireTableDataChanged();
                mtTerminados.fireTableDataChanged();

                // 2. Actualizar CPU Monitor
                lblReloj.setText("Ciclo Global: " + cpu.getCicloGlobal());
                PCB actual = cpu.getRunning();
                if (actual != null) {
                    lblCpuRunning.setText(String.format("<html><b>EJECUTANDO:</b> %s<br>Prioridad: %d<br>Restantes: %d<br><br>PC: %d<br>MAR: %d</html>",
                            actual.getNombre(), actual.getPrioridad(), actual.getRestantes(), actual.getPc(), actual.getMar()));
                } else {
                    lblCpuRunning.setText("<html><b>EJECUTANDO:</b><br>[IDLE - CPU Libre]<br><br>PC: 0<br>MAR: 0</html>");
                }

                // 3. Actualizar Métricas
                lblMetricas.setText(String.format("<html>Uso CPU: %.1f%%<br>Éxito Misión: %.1f%%</html>", 
                                    cpu.getCpuUtil(), cpu.getTasaExitoMision()));

                // 4. Leer Log
                String nuevosLogs = cpu.getLog();
                if (!nuevosLogs.isEmpty()) {
                    txtLog.append(nuevosLogs);
                    txtLog.setCaretPosition(txtLog.getDocument().getLength()); // Auto-scroll
                }

            } catch (Exception ex) {
                ex.printStackTrace();
            } finally {
                mutex.release();
            }
        });
        guiTimer.start();
    }

    // --- CLASE INTERNA: Modelo de Tabla Personalizado (Cumple regla de no java.util) ---
    private class ProcessTableModel extends AbstractTableModel {
        private final Lista<PCB> listaFila;
        private final String[] columnas = {"ID", "Nombre", "Estado", "Restante", "Deadline"};

        public ProcessTableModel(Lista<PCB> lista) {
            this.listaFila = lista;
        }

        @Override
        public int getRowCount() { return listaFila.size(); }

        @Override
        public int getColumnCount() { return columnas.length; }

        @Override
        public String getColumnName(int col) { return columnas[col]; }

        @Override
        public Object getValueAt(int rowIndex, int columnIndex) {
            PCB p = listaFila.get(rowIndex);
            if (p == null) return null;
            switch (columnIndex) {
                case 0: return p.getId();
                case 1: return p.getNombre();
                case 2: return p.getEstado();
                case 3: return p.getRestantes();
                case 4: return p.getDeadlineRestante();
                default: return null;
            }
        }
    }
}
