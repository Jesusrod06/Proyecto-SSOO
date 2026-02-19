/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package Models;

public class PCB {
    private static int NEXT_ID = 1;

    private final int id;
    private String nombre;

    private Estado estado;

    // Registros simulados
    private int pc;   // program counter
    private int mar;  // memory address register

    // Parámetros de planificación
    private int prioridad;          // menor = más importante (tú decides convención)
    private int instruccionesTotal; // total instrucciones
    private int restantes;          // restantes por ejecutar

    // Deadline (cuenta regresiva en ciclos)
    private int deadlineTotal;
    private int deadlineRestante;

    // CPU / E/S
    private boolean requiereIO;
    private int ciclosParaLanzarIO;     // cuando llega a 0, se bloquea
    private int ciclosIOServicio;       // cuanto dura bloqueado
    private int ioRestante;

    // Round Robin
    private int quantumRestante;

    // Métrica
    private int tiempoEspera; // ciclos en listo

    public PCB(String nombre, int instruccionesTotal, int prioridad, int deadlineCiclos,
               boolean requiereIO, int ciclosParaLanzarIO, int ciclosIOServicio) {
        this.id = NEXT_ID++;
        this.nombre = nombre;
        this.estado = Estado.NUEVO;
        this.pc = 0;
        this.mar = 0;

        this.instruccionesTotal = instruccionesTotal;
        this.restantes = instruccionesTotal;

        this.prioridad = prioridad;

        this.deadlineTotal = deadlineCiclos;
        this.deadlineRestante = deadlineCiclos;

        this.requiereIO = requiereIO;
        this.ciclosParaLanzarIO = ciclosParaLanzarIO;
        this.ciclosIOServicio = ciclosIOServicio;
        this.ioRestante = 0;

        this.quantumRestante = 0;
        this.tiempoEspera = 0;
    }

    // ==== lógica por ciclo ====
    public void tickDeadline() {
        if (estado != Estado.TERMINADO && deadlineRestante > 0) {
            deadlineRestante--;
        }
    }

    public boolean deadlineFallido() {
        return (estado != Estado.TERMINADO && deadlineRestante <= 0 && restantes > 0);
    }

    public boolean listoParaIO() {
        return requiereIO && (ciclosParaLanzarIO <= 0) && (restantes > 0);
    }

    public void tickIOTrigger() {
        if (requiereIO && ciclosParaLanzarIO > 0) ciclosParaLanzarIO--;
    }

    public void iniciarIO() {
        this.ioRestante = ciclosIOServicio;
    }

    public void tickIO() {
        if (ioRestante > 0) ioRestante--;
    }

    public boolean ioTerminada() {
        return ioRestante <= 0;
    }

    public void ejecutarUnaInstruccion() {
        if (restantes <= 0) return;
        restantes--;
        pc++;
        mar++;
    }

    public boolean finalizado() {
        return restantes <= 0;
    }

    // ==== getters/setters (mínimos) ====
    public int getId() { return id; }
    public String getNombre() { return nombre; }
    public Estado getEstado() { return estado; }
    public void setEstado(Estado estado) { this.estado = estado; }

    public int getPc() { return pc; }
    public int getMar() { return mar; }

    public int getPrioridad() { return prioridad; }
    public int getInstruccionesTotal() { return instruccionesTotal; }
    public int getRestantes() { return restantes; }

    public int getDeadlineRestante() { return deadlineRestante; }
    public int getDeadlineTotal() { return deadlineTotal; }

    public int getQuantumRestante() { return quantumRestante; }
    public void setQuantumRestante(int q) { this.quantumRestante = q; }
    public void decQuantum() { if (quantumRestante > 0) quantumRestante--; }

    public int getTiempoEspera() { return tiempoEspera; }
    public void incTiempoEspera() { tiempoEspera++; }
    public void resetTiempoEspera() { tiempoEspera = 0; }

    public int getIoRestante() { return ioRestante; }

    @Override
    public String toString() {
        return "PCB{" + id + "," + nombre + ", " + estado +
                ", rem=" + restantes + ", ddl=" + deadlineRestante + ", prio=" + prioridad + "}";
    }
}
