/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package views;
import Models.CPU;
import Models.PCB;
import edd.Lista;

import javax.swing.*;
import javax.swing.border.TitledBorder;
import javax.swing.table.AbstractTableModel;
import java.awt.*;
import java.util.concurrent.Semaphore;

public class MemoryManagementPanel extends JPanel {

    private final CPU cpu;
    private final Lista<PCB> nuevos, listos, bloqueados, listoSuspendido, bloqueadoSuspendido;
    private final Semaphore mutex;

    private ProcessTableModel mtListos, mtBloqueados, mtListoSusp, mtBloqSusp;
    private JProgressBar pbRAM;
    private JLabel lblSwapInfo;

    private final Color BG_COLOR = new Color(10, 10, 25);
    private final Color BORDER_PURPLE = new Color(138, 43, 226);
    private final Color TEXT_CYAN = new Color(0, 255, 255);

    public MemoryManagementPanel(CPU cpu, Lista<PCB> nuevos, Lista<PCB> listos, Lista<PCB> bloqueados,
                                 Lista<PCB> listoSuspendido, Lista<PCB> bloqueadoSuspendido, Semaphore mutex) {
        this.cpu = cpu;
        this.nuevos = nuevos;
        this.listos = listos;
        this.bloqueados = bloqueados;
        this.listoSuspendido = listoSuspendido;
        this.bloqueadoSuspendido = bloqueadoSuspendido;
        this.mutex = mutex;

        setLayout(new GridLayout(2, 3, 15, 15));
        setBackground(BG_COLOR);
        setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));

        initUI();
        startGuiTimer();
    }

    private void initUI() {
        mtListos = new ProcessTableModel(listos);
        mtBloqueados = new ProcessTableModel(bloqueados);
        mtListoSusp = new ProcessTableModel(listoSuspendido);
        mtBloqSusp = new ProcessTableModel(bloqueadoSuspendido);

        // FILA 1
        add(crearPanelTabla("Ready Queue (RAM)", mtListos));

        JPanel centralTop = new JPanel(new BorderLayout());
        centralTop.setOpaque(false);
        centralTop.setBorder(crearBorde("Main Memory (RAM)"));
        
        JPanel pnlRam = new JPanel(new GridLayout(2, 1));
        pnlRam.setOpaque(false);
        JLabel lblRamTitle = new JLabel("Memory Usage", SwingConstants.CENTER);
        lblRamTitle.setForeground(Color.WHITE);
        pbRAM = new JProgressBar(0, 100);
        pbRAM.setStringPainted(true);
        pbRAM.setForeground(TEXT_CYAN);
        pbRAM.setBackground(Color.DARK_GRAY);
        pnlRam.add(lblRamTitle);
        pnlRam.add(pbRAM);
        
        centralTop.add(pnlRam, BorderLayout.NORTH);
        add(centralTop);

        add(crearPanelTabla("Blocked Queue (RAM)", mtBloqueados));

        // FILA 2
        add(crearPanelTabla("Ready-Suspended", mtListoSusp));

        JPanel centralBot = new JPanel(new BorderLayout());
        centralBot.setOpaque(false);
        centralBot.setBorder(crearBorde("Swap Space (Disk)"));
        
        lblSwapInfo = new JLabel("<html><center>⇅ Swap In / Out Active ⇅<br><br>Disk Storage</center></html>", SwingConstants.CENTER);
        lblSwapInfo.setForeground(Color.LIGHT_GRAY);
        lblSwapInfo.setFont(new Font("SansSerif", Font.PLAIN, 14));
        centralBot.add(lblSwapInfo, BorderLayout.CENTER);
        
        add(centralBot);

        add(crearPanelTabla("Blocked-Suspended", mtBloqSusp));
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

    private void startGuiTimer() {
        Timer guiTimer = new Timer(150, e -> {
            if (mutex.tryAcquire()) {
                try {
                    mtListos.fireTableDataChanged();
                    mtBloqueados.fireTableDataChanged();
                    mtListoSusp.fireTableDataChanged();
                    mtBloqSusp.fireTableDataChanged();

                    int ramEnUso = listos.size() + bloqueados.size() + (cpu.getRunning() != null ? 1 : 0);
                    int maxRam = cpu.getMaxEnMemoria();
                    int porcentaje = maxRam > 0 ? (ramEnUso * 100) / maxRam : 0;
                    
                    pbRAM.setValue(porcentaje);
                    pbRAM.setString(porcentaje + "% (" + ramEnUso + "/" + maxRam + " blocks)");
                    
                } finally {
                    mutex.release();
                }
            }
        });
        guiTimer.start();
    }

    private static class ProcessTableModel extends AbstractTableModel {
        private final Lista<PCB> lista;
        private final String[] cols = {"Process", "Priority"};

        public ProcessTableModel(Lista<PCB> lista) { this.lista = lista; }
        @Override public int getRowCount() { return lista.size(); }
        @Override public int getColumnCount() { return cols.length; }
        @Override public String getColumnName(int c) { return cols[c]; }
        @Override public Object getValueAt(int r, int c) {
            PCB p = lista.get(r);
            if (p == null) return null;
            return c == 0 ? p.getNombre() : p.getPrioridad();
        }
    }
}