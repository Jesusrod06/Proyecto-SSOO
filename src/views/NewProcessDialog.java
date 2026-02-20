/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package views;

import Models.PCB;
import javax.swing.*;
import java.awt.*;

public class NewProcessDialog extends JDialog {
    private JTextField txtNombre;
    private JSpinner spInstrucciones, spPrioridad, spDeadline, spIoTrigger, spIoServicio;
    private JCheckBox chkRequiereIO;
    private PCB nuevoProceso = null;

    public NewProcessDialog(Frame parent) {
        super(parent, "Create New Mission Task", true);
        setSize(350, 400);
        setLocationRelativeTo(parent);
        setLayout(new BorderLayout());
        getContentPane().setBackground(new Color(20, 20, 40)); // Tema oscuro

        JPanel panelForm = new JPanel(new GridLayout(7, 2, 10, 10));
        panelForm.setOpaque(false);
        panelForm.setBorder(BorderFactory.createEmptyBorder(15, 15, 15, 15));

        // Componentes
        txtNombre = new JTextField("TASK_");
        spInstrucciones = new JSpinner(new SpinnerNumberModel(20, 1, 500, 1));
        spPrioridad = new JSpinner(new SpinnerNumberModel(1, 1, 10, 1));
        spDeadline = new JSpinner(new SpinnerNumberModel(50, 5, 1000, 5));
        
        chkRequiereIO = new JCheckBox("Requires Sensor/Antenna I/O?");
        chkRequiereIO.setForeground(Color.WHITE);
        chkRequiereIO.setOpaque(false);
        
        spIoTrigger = new JSpinner(new SpinnerNumberModel(5, 1, 100, 1));
        spIoServicio = new JSpinner(new SpinnerNumberModel(10, 1, 100, 1));

        // Habilitar/Deshabilitar I/O
        spIoTrigger.setEnabled(false); spIoServicio.setEnabled(false);
        chkRequiereIO.addActionListener(e -> {
            boolean sel = chkRequiereIO.isSelected();
            spIoTrigger.setEnabled(sel); spIoServicio.setEnabled(sel);
        });

        // Estilos de etiquetas
        panelForm.add(crearLabel("Task Name:")); panelForm.add(txtNombre);
        panelForm.add(crearLabel("Total Instructions:")); panelForm.add(spInstrucciones);
        panelForm.add(crearLabel("Priority Level:")); panelForm.add(spPrioridad);
        panelForm.add(crearLabel("Deadline (Cycles):")); panelForm.add(spDeadline);
        panelForm.add(chkRequiereIO); panelForm.add(new JLabel("")); // Espacio vacío
        panelForm.add(crearLabel("I/O Trigger (Cycle):")); panelForm.add(spIoTrigger);
        panelForm.add(crearLabel("I/O Duration:")); panelForm.add(spIoServicio);

        add(panelForm, BorderLayout.CENTER);

        // Botones
        JPanel panelBotones = new JPanel();
        panelBotones.setOpaque(false);
        JButton btnCrear = new JButton("Launch Process");
        JButton btnCancelar = new JButton("Cancel");

        btnCrear.addActionListener(e -> {
            nuevoProceso = new PCB(
                    txtNombre.getText(),
                    (int) spInstrucciones.getValue(),
                    (int) spPrioridad.getValue(),
                    (int) spDeadline.getValue(),
                    chkRequiereIO.isSelected(),
                    (int) spIoTrigger.getValue(),
                    (int) spIoServicio.getValue()
            );
            dispose();
        });

        btnCancelar.addActionListener(e -> dispose());

        panelBotones.add(btnCrear); panelBotones.add(btnCancelar);
        add(panelBotones, BorderLayout.SOUTH);
    }

    private JLabel crearLabel(String texto) {
        JLabel lbl = new JLabel(texto);
        lbl.setForeground(Color.WHITE);
        return lbl;
    }

    // Método para llamar desde MainFrame
    public PCB getProcess() {
        setVisible(true); // Bloquea hasta que se cierre
        return nuevoProceso;
    }
}

