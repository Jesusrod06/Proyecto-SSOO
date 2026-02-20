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
import javax.swing.border.Border;
import javax.swing.border.TitledBorder;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.JTableHeader;
import java.awt.*;
import java.util.concurrent.Semaphore;

public class MainFrame extends JFrame {

    // --- COLORES Y FUENTES DEL TEMA UNIMET-Sat ---
    private static final Color COLOR_FONDO = new Color(10, 10, 25); // Azul oscuro espacial
    private static final Color COLOR_PANEL = new Color(20, 20, 40); // Un poco más claro para paneles
    private static final Color COLOR_NEON = new Color(180, 80, 255); // Morado neón
    private static final Color COLOR_TEXTO = new Color(220, 220, 255); // Blanco azulado
    private static final Color COLOR_TEXTO_RELOJ = new Color(100, 255, 255); // Cian neón
    private static final Color COLOR_BARRA_MEMORIA = new Color(100, 150, 255);
    private static final Color COLOR_BARRA_CPU = new Color(50, 200, 100);
    private static final Color COLOR_BOTON_EMERGENCIA = new Color(200, 50, 50);

    private static final Font FUENTE_TITULO = new Font("Consolas", Font.BOLD, 16);
    private static final Font FUENTE_NORMAL = new Font("Consolas", Font.PLAIN, 12);
    private static final Font FUENTE_RELOJ = new Font("Consolas", Font.BOLD, 24);

    // Referencias al motor (Backend)
    private final CPU cpu;
    private final InterruptGenerator interruptor;
    private final Scheduler scheduler;
    private final Semaphore mutex;
    private final int maxRAM; // Para la barra de memoria

    // Listas del sistema
    private final Lista<PCB> nuevos, listos, bloqueados, terminados, listoSuspendido, bloqueadoSuspendido;

    // Componentes Visuales
    private JLabel lblReloj, lblCpuNombre, lblCpuDeadline;
    private JProgressBar barraMemoria, barraCpuProgreso;
    private JComboBox<PolicyType> cbPoliticas;
    private JSpinner spQuantum;
    private JButton btnStart, btnStop, btnAddProcess, btnEmergencia;

    // Modelos de tablas
    private ProcessTableModel mtListos, mtBloqueados, mtListoSusp, mtBloqSusp;

    public MainFrame(CPU cpu, InterruptGenerator interruptor, Scheduler scheduler,
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
        this.maxRAM = cpu.getMaxEnMemoria(); // Obtenemos el total de RAM

        // Configuración básica de la ventana
        setTitle("UNIMET-Sat RTOS Simulator - Mission Control Center");
        setSize(1280, 800); // Un poco más grande para que quepa todo
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);
        getContentPane().setBackground(COLOR_FONDO);
        setLayout(new BorderLayout(10, 10));
        ((JPanel)getContentPane()).setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        inicializarUI();
        iniciarRelojGrafico();
    }

    private void inicializarUI() {
        // --- PANEL NORTE: Reloj de Misión ---
        JPanel panelNorte = createNeonPanel("");
        panelNorte.setLayout(new FlowLayout(FlowLayout.CENTER));
        
        JLabel lblTitulo = new JLabel("MISSION CLOCK: ");
        lblTitulo.setFont(FUENTE_RELOJ);
        lblTitulo.setForeground(COLOR_TEXTO);
        
        lblReloj = new JLabel("Cycle 0");
        lblReloj.setFont(FUENTE_RELOJ);
        lblReloj.setForeground(COLOR_TEXTO_RELOJ);
        
        panelNorte.add(lblTitulo);
        panelNorte.add(lblReloj);
        add(panelNorte, BorderLayout.NORTH);

        // --- PANEL CENTRAL: RAM y CPU (Dividido en 3 columnas) ---
        JPanel panelCentral = new JPanel(new GridLayout(1, 3, 10, 10));
        panelCentral.setOpaque(false);

        // Columna 1: Ready Queue (RAM)
        mtListos = new ProcessTableModel(listos);
        panelCentral.add(crearPanelTabla("READY QUEUE (RAM)", mtListos));

        // Columna 2: Memoria Central y CPU
        JPanel panelCentroMedio = new JPanel(new GridLayout(2, 1, 10, 10));
        panelCentroMedio.setOpaque(false);

        // Panel de Memoria (Arriba)
        JPanel panelMemoria = createNeonPanel("MAIN MEMORY (RAM)");
        panelMemoria.setLayout(new BorderLayout(10, 10));
        barraMemoria = new JProgressBar(0, maxRAM);
        barraMemoria.setValue(0);
        barraMemoria.setStringPainted(true);
        barraMemoria.setForeground(COLOR_BARRA_MEMORIA);
        barraMemoria.setBackground(COLOR_PANEL);
        barraMemoria.setFont(FUENTE_TITULO);
        barraMemoria.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
        
        JLabel lblMemInfo = new JLabel("Memory Usage", SwingConstants.CENTER);
        lblMemInfo.setFont(FUENTE_NORMAL);
        lblMemInfo.setForeground(COLOR_TEXTO);

        panelMemoria.add(lblMemInfo, BorderLayout.NORTH);
        panelMemoria.add(barraMemoria, BorderLayout.CENTER);
        panelCentroMedio.add(panelMemoria);

        // Panel de CPU (Abajo)
        JPanel panelCpu = createNeonPanel("RUNNING PROCESS (CPU)");
        panelCpu.setLayout(new GridLayout(3, 1, 5, 5));
        
        lblCpuNombre = new JLabel("[IDLE]", SwingConstants.CENTER);
        lblCpuNombre.setFont(FUENTE_TITULO);
        lblCpuNombre.setForeground(COLOR_TEXTO_RELOJ);

        barraCpuProgreso = new JProgressBar(0, 100);
        barraCpuProgreso.setValue(0);
        barraCpuProgreso.setStringPainted(false);
        barraCpuProgreso.setForeground(COLOR_BARRA_CPU);
        barraCpuProgreso.setBackground(COLOR_PANEL);

        lblCpuDeadline = new JLabel("Deadline in: -- cycles", SwingConstants.CENTER);
        lblCpuDeadline.setFont(FUENTE_NORMAL);
        lblCpuDeadline.setForeground(COLOR_TEXTO);

        panelCpu.add(lblCpuNombre);
        panelCpu.add(barraCpuProgreso);
        panelCpu.add(lblCpuDeadline);
        panelCentroMedio.add(panelCpu);

        panelCentral.add(panelCentroMedio);

        // Columna 3: Blocked Queue (RAM)
        mtBloqueados = new ProcessTableModel(bloqueados);
        panelCentral.add(crearPanelTabla("BLOCKED QUEUE (I/O)", mtBloqueados));

        add(panelCentral, BorderLayout.CENTER);

        // --- PANEL SUR: Swap Space y Controles (Dividido en 3 columnas) ---
        JPanel panelSur = new JPanel(new GridLayout(1, 3, 10, 10));
        panelSur.setOpaque(false);
        panelSur.setPreferredSize(new Dimension(0, 250));

        // Columna 1: Ready-Suspended (Disk)
        mtListoSusp = new ProcessTableModel(listoSuspendido);
        panelSur.add(crearPanelTabla("READY-SUSPENDED (DISK)", mtListoSusp));

        // Columna 2: Controles de Misión
        JPanel panelControles = createNeonPanel("MISSION CONTROLS");
        panelControles.setLayout(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 5, 5, 5);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        btnStart = crearBoton("▶ START SIMULATION", new Color(50, 150, 50));
        btnStop = crearBoton("⏹ STOP", new Color(150, 50, 50));
        btnAddProcess = crearBoton("➕ NEW PROCESS", new Color(50, 100, 150));

        cbPoliticas = new JComboBox<>(PolicyType.values()); cbPoliticas.setFont(FUENTE_NORMAL);
        spQuantum = new JSpinner(new SpinnerNumberModel(scheduler.getRrQuantum(), 1, 50, 1)); spQuantum.setFont(FUENTE_NORMAL);

        JLabel lblPol = new JLabel("Policy:"); lblPol.setForeground(COLOR_TEXTO); lblPol.setFont(FUENTE_NORMAL);
        JLabel lblQuant = new JLabel("Quantum:"); lblQuant.setForeground(COLOR_TEXTO); lblQuant.setFont(FUENTE_NORMAL);

        btnEmergencia = new JButton("<html><center>EMERGENCY INTERRUPTION<br>(MANUAL TRIGGER)</center></html>");
        btnEmergencia.setFont(FUENTE_TITULO);
        btnEmergencia.setBackground(COLOR_BOTON_EMERGENCIA);
        btnEmergencia.setForeground(Color.WHITE);
        btnEmergencia.setFocusPainted(false);
        btnEmergencia.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(Color.RED.brighter(), 3),
                BorderFactory.createEmptyBorder(10, 10, 10, 10)
        ));

        // Fila 1
        gbc.gridx = 0; gbc.gridy = 0; panelControles.add(btnStart, gbc);
        gbc.gridx = 1; gbc.gridy = 0; panelControles.add(btnStop, gbc);
        // Fila 2
        gbc.gridx = 0; gbc.gridy = 1; gbc.gridwidth = 2; panelControles.add(btnAddProcess, gbc);
        // Fila 3
        gbc.gridx = 0; gbc.gridy = 2; gbc.gridwidth = 1; panelControles.add(lblPol, gbc);
        gbc.gridx = 1; gbc.gridy = 2; panelControles.add(cbPoliticas, gbc);
        // Fila 4
        gbc.gridx = 0; gbc.gridy = 3; panelControles.add(lblQuant, gbc);
        gbc.gridx = 1; gbc.gridy = 3; panelControles.add(spQuantum, gbc);
        // Fila 5 (Botón de Emergencia)
        gbc.gridx = 0; gbc.gridy = 4; gbc.gridwidth = 2; gbc.weighty = 1.0; gbc.fill = GridBagConstraints.BOTH;
        panelControles.add(btnEmergencia, gbc);

        panelSur.add(panelControles);

        // Columna 3: Blocked-Suspended (Disk)
        mtBloqSusp = new ProcessTableModel(bloqueadoSuspendido);
        panelSur.add(crearPanelTabla("BLOCKED-SUSPENDED (DISK)", mtBloqSusp));

        add(panelSur, BorderLayout.SOUTH);

        // --- EVENTOS ---
        configurarEventos();
    }

    // --- MÉTODOS AUXILIARES DE GUI ---

    private JPanel createNeonPanel(String titulo) {
        JPanel panel = new JPanel();
        panel.setBackground(COLOR_PANEL);
        Border lineBorder = BorderFactory.createLineBorder(COLOR_NEON, 2);
        Border titleBorder = BorderFactory.createTitledBorder(lineBorder, titulo, 
                TitledBorder.LEFT, TitledBorder.TOP, FUENTE_TITULO, COLOR_NEON);
        panel.setBorder(BorderFactory.createCompoundBorder(titleBorder, 
                BorderFactory.createEmptyBorder(5, 5, 5, 5)));
        return panel;
    }

    private JButton crearBoton(String texto, Color colorFondo) {
        JButton btn = new JButton(texto);
        btn.setFont(FUENTE_NORMAL);
        btn.setBackground(colorFondo);
        btn.setForeground(Color.WHITE);
        btn.setFocusPainted(false);
        btn.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(colorFondo.brighter(), 1),
                BorderFactory.createEmptyBorder(5, 10, 5, 10)
        ));
        return btn;
    }

    private JPanel crearPanelTabla(String titulo, ProcessTableModel modelo) {
        JPanel panel = createNeonPanel(titulo);
        panel.setLayout(new BorderLayout());
        
        JTable tabla = new JTable(modelo);
        tabla.setBackground(COLOR_PANEL);
        tabla.setForeground(COLOR_TEXTO);
        tabla.setFont(FUENTE_NORMAL);
        tabla.setGridColor(COLOR_NEON.darker());
        tabla.setSelectionBackground(COLOR_NEON.darker());
        tabla.setSelectionForeground(Color.WHITE);

        JTableHeader header = tabla.getTableHeader();
        header.setBackground(COLOR_PANEL.darker());
        header.setForeground(COLOR_NEON);
        header.setFont(FUENTE_TITULO);

        // Centrar contenido de las celdas
        DefaultTableCellRenderer centerRenderer = new DefaultTableCellRenderer();
        centerRenderer.setHorizontalAlignment(JLabel.CENTER);
        tabla.setDefaultRenderer(Object.class, centerRenderer);

        JScrollPane scroll = new JScrollPane(tabla);
        scroll.getViewport().setBackground(COLOR_PANEL);
        scroll.setBorder(BorderFactory.createEmptyBorder());
        
        panel.add(scroll, BorderLayout.CENTER);
        
        // Ajustar anchos de columna (AHORA SÍ ESTÁ EN EL ORDEN CORRECTO)
        tabla.getColumnModel().getColumn(0).setPreferredWidth(120); // Process Name más ancho
        tabla.getColumnModel().getColumn(1).setPreferredWidth(40);  // ID más angosto
        tabla.getColumnModel().getColumn(2).setPreferredWidth(60);  // Priority
        tabla.getColumnModel().getColumn(3).setPreferredWidth(70);  // Rem. Instr.
    
        return panel;
    }

    private void configurarEventos() {
        btnStart.addActionListener(e -> {
            if (!btnStart.isEnabled()) return;
            cpu.start(); 
            cpu.startSimulation();
            interruptor.start();
            btnStart.setEnabled(false);
            btnStop.setEnabled(true);
        });

        btnStop.addActionListener(e -> {
            if (!btnStop.isEnabled()) return;
            cpu.stopSimulation();
            interruptor.stopGen();
            btnStart.setEnabled(true);
            btnStop.setEnabled(false);
        });

        cbPoliticas.addActionListener(e -> scheduler.setPolicy((PolicyType) cbPoliticas.getSelectedItem()));
        spQuantum.addChangeListener(e -> scheduler.setRrQuantum((int) spQuantum.getValue()));

        // Botón temporal para añadir proceso rápido
        btnAddProcess.addActionListener(e -> agregarProcesoAleatorio());

        // ¡El Botón Rojo!
        btnEmergencia.addActionListener(e -> {
            cpu.triggerInterrupt("MANUAL EMERGENCY OVERRIDE");
            JOptionPane.showMessageDialog(this, "Emergency Interrupt Triggered!", "Alert", JOptionPane.WARNING_MESSAGE);
        });
    }

    // Simula la creación rápida de un proceso
    private void agregarProcesoAleatorio() {
        try {
            mutex.acquire();
            int idGenerico = nuevos.size() + listos.size() + terminados.size() + listoSuspendido.size() + bloqueadoSuspendido.size() + 1;
            PCB p = new PCB("PROC_" + idGenerico, 20 + (int)(Math.random()*30), 1 + (int)(Math.random()*5), 80, true, 10, 5);
            nuevos.addLast(p);
            // Si hay espacio, lo pasamos a listos de una vez para verlo
            if (listos.size() + bloqueados.size() + (cpu.getRunning() != null ? 1 : 0) < cpu.getMaxEnMemoria()) {
                nuevos.remove(p);
                p.setEstado(Models.Estado.LISTO);
                listos.addLast(p);
            }
        } catch (InterruptedException ex) {
            ex.printStackTrace();
        } finally {
            mutex.release();
        }
    }

    // Hilo de la Interfaz (Timer) que actualiza la pantalla
    private void iniciarRelojGrafico() {
        Timer guiTimer = new Timer(100, e -> {
            try {
                mutex.acquire(); 
                
                // 1. Actualizar Tablas
                mtListos.fireTableDataChanged();
                mtBloqueados.fireTableDataChanged();
                mtListoSusp.fireTableDataChanged();
                mtBloqSusp.fireTableDataChanged();

                // 2. Actualizar Reloj
                lblReloj.setText("Cycle " + cpu.getCicloGlobal());

                // 3. Actualizar Barra de Memoria
                int ocupados = listos.size() + bloqueados.size() + (cpu.getRunning() != null ? 1 : 0);
                int porcentajeMem = (int) (((double)ocupados / maxRAM) * 100);
                barraMemoria.setValue(ocupados);
                barraMemoria.setString(ocupados + "/" + maxRAM + " (" + porcentajeMem + "%)");
                barraMemoria.setForeground(porcentajeMem > 80 ? Color.RED : COLOR_BARRA_MEMORIA);

                // 4. Actualizar CPU Monitor
                PCB actual = cpu.getRunning();
                if (actual != null) {
                    lblCpuNombre.setText(actual.getNombre() + " [ID:" + actual.getId() + "]");
                    lblCpuDeadline.setText("Deadline in: " + actual.getDeadlineRestante() + " cycles");
                    lblCpuDeadline.setForeground(actual.getDeadlineRestante() < 10 ? Color.RED : COLOR_TEXTO);
                    
                    // Calcular progreso del proceso
                    int total = actual.getInstruccionesTotal();
                    int ejecutadas = total - actual.getRestantes();
                    int porcentajeCpu = (int)(((double)ejecutadas / total) * 100);
                    barraCpuProgreso.setValue(porcentajeCpu);

                } else {
                    lblCpuNombre.setText("[IDLE - WAITING]");
                    lblCpuDeadline.setText("Deadline in: -- cycles");
                    lblCpuDeadline.setForeground(COLOR_TEXTO);
                    barraCpuProgreso.setValue(0);
                }

            } catch (Exception ex) {
                ex.printStackTrace();
            } finally {
                mutex.release();
            }
        });
        guiTimer.start();
    }

    // --- MODELO DE TABLA ---
    private class ProcessTableModel extends AbstractTableModel {
        private final Lista<PCB> listaFila;
        private final String[] columnas = {"Process Name", "ID", "Priority", "Rem. Instr."};

        public ProcessTableModel(Lista<PCB> lista) {
            this.listaFila = lista;
        }

        @Override
        public int getRowCount() { return listaFila.size(); }
        @Override public int getColumnCount() { return columnas.length; }
        @Override public String getColumnName(int col) { return columnas[col]; }
        @Override public Class<?> getColumnClass(int columnIndex) {
            return columnIndex == 2 || columnIndex == 3 ? Integer.class : String.class;
        }

        @Override
        public Object getValueAt(int rowIndex, int columnIndex) {
            PCB p = listaFila.get(rowIndex);
            if (p == null) return null;
            switch (columnIndex) {
                case 0: return p.getNombre();
                case 1: return p.getId();
                case 2: return p.getPrioridad();
                case 3: return p.getRestantes();
                default: return null;
            }
        }
    }
}