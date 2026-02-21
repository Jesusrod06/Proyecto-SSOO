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
import javax.swing.table.AbstractTableModel;
import java.awt.*;
import java.util.concurrent.Semaphore;

public class MissionControlPanel extends JPanel {

    private final CPU cpu;
    @SuppressWarnings("unused")
    private final InterruptGenerator interruptor;
    private final Scheduler scheduler;
    private final Semaphore mutex;

    private final Lista<PCB> nuevos, listos, bloqueados, terminados;

    private JLabel lblReloj, lblCpuRunning, lblMetricas;
    private JTextArea txtLog;
    private JComboBox<PolicyType> cbPoliticas;
    private JSpinner spQuantum, spTickMs;

    private ProcessTableModel mtNuevos, mtListos, mtBloqueados, mtTerminados;

    public MissionControlPanel(CPU cpu, InterruptGenerator interruptor, Scheduler scheduler,
                               Lista<PCB> nuevos, Lista<PCB> listos, Lista<PCB> bloqueados, Lista<PCB> terminados,
                               Semaphore mutex) {
        this.cpu = cpu;
        this.interruptor = interruptor;
        this.scheduler = scheduler;
        this.nuevos = nuevos;
        this.listos = listos;
        this.bloqueados = bloqueados;
        this.terminados = terminados;
        this.mutex = mutex;

        setLayout(new BorderLayout(10, 10));
        setBackground(new Color(20, 20, 40));

        initUI();
        startGuiTimer();
    }

    private void initUI() {
        // ===== NORTH: Controls =====
        JPanel panelNorte = new JPanel(new FlowLayout(FlowLayout.LEFT, 12, 10));
        panelNorte.setBackground(new Color(40, 44, 52));

        JButton btnStart = new JButton("▶ Start");
        JButton btnStop = new JButton("⏹ Stop");
        JButton btnNew = new JButton("➕ New Process");
        JButton btn20 = new JButton("⚡ Generate 20 Random");
        JButton btnEmergency = new JButton("🚨 EMERGENCY");

        cbPoliticas = new JComboBox<>(PolicyType.values());
        cbPoliticas.setSelectedItem(scheduler.getPolicy());

        spQuantum = new JSpinner(new SpinnerNumberModel(scheduler.getRrQuantum(), 1, 20, 1));
        spTickMs = new JSpinner(new SpinnerNumberModel(500, 50, 3000, 50)); // inicial

        styleButton(btnStart, new Color(76, 175, 80));
        styleButton(btnStop, new Color(244, 67, 54));
        styleButton(btnEmergency, new Color(255, 152, 0));

        panelNorte.add(btnStart);
        panelNorte.add(btnStop);
        panelNorte.add(btnNew);
        panelNorte.add(btn20);
        panelNorte.add(btnEmergency);

        panelNorte.add(labelWhite("Policy:"));
        panelNorte.add(cbPoliticas);

        panelNorte.add(labelWhite("RR Quantum:"));
        panelNorte.add(spQuantum);

        panelNorte.add(labelWhite("Tick (ms):"));
        panelNorte.add(spTickMs);

        add(panelNorte, BorderLayout.NORTH);

        // ===== CENTER: Tables =====
        JPanel panelCentral = new JPanel(new GridLayout(2, 2, 10, 10));
        panelCentral.setOpaque(false);
        panelCentral.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        mtNuevos = new ProcessTableModel(nuevos);
        mtListos = new ProcessTableModel(listos);
        mtBloqueados = new ProcessTableModel(bloqueados);
        mtTerminados = new ProcessTableModel(terminados);

        panelCentral.add(panelTabla("NUEVOS", mtNuevos));
        panelCentral.add(panelTabla("LISTOS (RAM)", mtListos));
        panelCentral.add(panelTabla("BLOQUEADOS (I/O)", mtBloqueados));
        panelCentral.add(panelTabla("TERMINADOS", mtTerminados));

        add(panelCentral, BorderLayout.CENTER);

        // ===== EAST: CPU Monitor =====
        JPanel panelEste = new JPanel();
        panelEste.setLayout(new BoxLayout(panelEste, BoxLayout.Y_AXIS));
        panelEste.setPreferredSize(new Dimension(280, 0));
        panelEste.setBorder(crearBorde("CPU MONITOR"));
        panelEste.setBackground(new Color(25, 25, 55));

        lblReloj = new JLabel("Global Cycle: 0");
        lblReloj.setFont(new Font("Consolas", Font.BOLD, 16));
        lblReloj.setForeground(Color.WHITE);
        lblReloj.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        lblCpuRunning = new JLabel("<html><b>RUNNING:</b> [IDLE]</html>");
        lblCpuRunning.setFont(new Font("Consolas", Font.PLAIN, 13));
        lblCpuRunning.setForeground(Color.WHITE);
        lblCpuRunning.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        lblMetricas = new JLabel("<html>CPU Util: 0%<br>Mission Success: 0%</html>");
        lblMetricas.setFont(new Font("Consolas", Font.PLAIN, 13));
        lblMetricas.setForeground(Color.WHITE);
        lblMetricas.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        panelEste.add(lblReloj);
        panelEste.add(new JSeparator());
        panelEste.add(lblCpuRunning);
        panelEste.add(new JSeparator());
        panelEste.add(lblMetricas);

        add(panelEste, BorderLayout.EAST);

        // ===== SOUTH: Log =====
        JPanel panelLog = new JPanel(new BorderLayout());
        panelLog.setOpaque(false);
        panelLog.setBorder(crearBorde("SYSTEM LOG"));

        txtLog = new JTextArea(8, 60);
        txtLog.setEditable(false);
        txtLog.setBackground(Color.BLACK);
        txtLog.setForeground(Color.GREEN);
        txtLog.setFont(new Font("Consolas", Font.PLAIN, 12));

        JScrollPane spLog = new JScrollPane(txtLog);
        spLog.getViewport().setBackground(Color.BLACK);
        panelLog.add(spLog, BorderLayout.CENTER);

        add(panelLog, BorderLayout.SOUTH);

        // ===== Events =====
        btnStart.addActionListener(e -> cpu.startSimulation());
        btnStop.addActionListener(e -> cpu.stopSimulation());

        cbPoliticas.addActionListener(e -> scheduler.setPolicy((PolicyType) cbPoliticas.getSelectedItem()));
        spQuantum.addChangeListener(e -> scheduler.setRrQuantum((int) spQuantum.getValue()));
        spTickMs.addChangeListener(e -> cpu.setCicloMs((int) spTickMs.getValue()));

        btnNew.addActionListener(e -> abrirDialogoProceso());
        btn20.addActionListener(e -> generar20Procesos());

        // EMERGENCY: NO PRINTS, dispara interrupción real
        btnEmergency.addActionListener(e -> cpu.triggerInterrupt("Micro-meteorito (Botón de emergencia)"));
    }

    private void abrirDialogoProceso() {
        NewProcessDialog dlg = new NewProcessDialog((Frame) SwingUtilities.getWindowAncestor(this));
        PCB p = dlg.getProcess();
        if (p == null) return;

        try {
            mutex.acquire();
            nuevos.addLast(p);
        } catch (InterruptedException ignored) {
        } finally {
            mutex.release();
        }
    }

    private void generar20Procesos() {
        try {
            mutex.acquire();
            for (int i = 0; i < 20; i++) {
                int base = nuevos.size() + listos.size() + bloqueados.size() + terminados.size() + 1;
                boolean io = (base % 2 == 0);

                PCB p = new PCB("AUTO_" + base,
                        10 + (base % 50),
                        1 + (base % 10),
                        30 + (base % 120),
                        io,
                        3 + (base % 10),
                        2 + (base % 10)
                );
                nuevos.addLast(p);
            }
        } catch (InterruptedException ignored) {
        } finally {
            mutex.release();
        }
    }

    private void startGuiTimer() {
        Timer guiTimer = new Timer(120, e -> {
            try {
                mutex.acquire();

                mtNuevos.fireTableDataChanged();
                mtListos.fireTableDataChanged();
                mtBloqueados.fireTableDataChanged();
                mtTerminados.fireTableDataChanged();

                lblReloj.setText("Global Cycle: " + cpu.getCicloGlobal());

                PCB r = cpu.getRunning();
                if (r != null) {
                    lblCpuRunning.setText(String.format(
                            "<html><b>RUNNING:</b> %s<br>ID:%d | Prio:%d<br>Rem:%d | DDL:%d<br>PC:%d | MAR:%d</html>",
                            r.getNombre(), r.getId(), r.getPrioridad(),
                            r.getRestantes(), r.getDeadlineRestante(),
                            r.getPc(), r.getMar()
                    ));
                } else {
                    lblCpuRunning.setText("<html><b>RUNNING:</b> [IDLE]</html>");
                }

                lblMetricas.setText(String.format(
                        "<html>CPU Util: %.1f%%<br>Mission Success: %.1f%%</html>",
                        cpu.getCpuUtil(), cpu.getTasaExitoMision()
                ));

                // Leer log del backend y mostrarlo en pantalla
                String logs = cpu.getLog();
                if (logs != null && !logs.isEmpty()) {
                    txtLog.append(logs);
                    txtLog.setCaretPosition(txtLog.getDocument().getLength());
                }

            } catch (Exception ex) {
                // NO printStackTrace: llevar a log del CPU
                cpu.logError("GUI Timer Error", ex);
            } finally {
                mutex.release();
            }
        });
        guiTimer.start();
    }

    // ===== UI Helpers =====
    private JPanel panelTabla(String titulo, AbstractTableModel model) {
        JPanel p = new JPanel(new BorderLayout());
        p.setOpaque(false);
        p.setBorder(crearBorde(titulo));
        JTable t = new JTable(model);
        p.add(new JScrollPane(t), BorderLayout.CENTER);
        return p;
    }

    private JLabel labelWhite(String text) {
        JLabel l = new JLabel(text);
        l.setForeground(Color.WHITE);
        return l;
    }

    private void styleButton(JButton b, Color bg) {
        b.setBackground(bg);
        b.setForeground(Color.WHITE);
        b.setFocusPainted(false);
    }

    private javax.swing.border.TitledBorder crearBorde(String titulo) {
        javax.swing.border.TitledBorder tb = BorderFactory.createTitledBorder(titulo);
        tb.setTitleColor(Color.WHITE);
        return tb;
    }

    // ===== Table Model =====
    private static class ProcessTableModel extends AbstractTableModel {
        private final Lista<PCB> lista;
        private final String[] cols = {"ID", "Name", "State", "Remaining", "Deadline", "Prio"};

        public ProcessTableModel(Lista<PCB> lista) {
            this.lista = lista;
        }

        @Override public int getRowCount() { return lista.size(); }
        @Override public int getColumnCount() { return cols.length; }
        @Override public String getColumnName(int column) { return cols[column]; }

        @Override
        public Object getValueAt(int rowIndex, int columnIndex) {
            PCB p = lista.get(rowIndex);
            if (p == null) return null;

            return switch (columnIndex) {
                case 0 -> p.getId();
                case 1 -> p.getNombre();
                case 2 -> p.getEstado();
                case 3 -> p.getRestantes();
                case 4 -> p.getDeadlineRestante();
                case 5 -> p.getPrioridad();
                default -> null;
            };
        }
    }
}