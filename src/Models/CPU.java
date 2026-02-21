/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package Models;

import Scheduler.PolicyType;
import Scheduler.Scheduler;
import edd.Lista;

import java.util.concurrent.Semaphore;

public class CPU extends Thread {

    // Colas (listas) del sistema
    private final Lista<PCB> nuevos;
    private final Lista<PCB> listos;
    private final Lista<PCB> bloqueados;
    private final Lista<PCB> terminados;

    private final Lista<PCB> listoSuspendido;
    private final Lista<PCB> bloqueadoSuspendido;

    // Semáforo para exclusión mutua
    private final Semaphore mutex;
    private final Scheduler scheduler;

    // Proceso actual
    private PCB running;

    // Reloj
    private volatile boolean runningSim;
    private volatile int cicloGlobal;
    private volatile int cicloMs;

    // Interrupción externa
    private volatile boolean interruptPending;
    private volatile String interruptReason;

    // Memoria (máx procesos en RAM)
    private final int maxEnMemoria;

    // Métricas
    private long cpuBusy;
    private long cpuTotal;
    private long totalProcesosTerminados;
    private long procesosEnDeadline;

    // Log simple
    private final StringBuilder log;

    public CPU(Lista<PCB> nuevos, Lista<PCB> listos, Lista<PCB> bloqueados, Lista<PCB> terminados,
               Lista<PCB> listoSuspendido, Lista<PCB> bloqueadoSuspendido,
               Semaphore mutex, Scheduler scheduler,
               int cicloMs, int maxEnMemoria) {
        this.nuevos = nuevos;
        this.listos = listos;
        this.bloqueados = bloqueados;
        this.terminados = terminados;
        this.listoSuspendido = listoSuspendido;
        this.bloqueadoSuspendido = bloqueadoSuspendido;

        this.mutex = mutex;
        this.scheduler = scheduler;

        this.running = null;

        this.runningSim = false;
        this.cicloGlobal = 0;
        this.cicloMs = cicloMs;

        this.interruptPending = false;
        this.interruptReason = "";

        this.maxEnMemoria = maxEnMemoria;

        this.cpuBusy = 0;
        this.cpuTotal = 0;
        this.totalProcesosTerminados = 0;
        this.procesosEnDeadline = 0;

        this.log = new StringBuilder();
    }

    // ===== Controles =====
    public void startSimulation() { runningSim = true; }
    public void stopSimulation() { runningSim = false; }

    public void setCicloMs(int ms) { this.cicloMs = ms; }
    public int getCicloGlobal() { return cicloGlobal; }
    public PCB getRunning() { return running; }
    public int getMaxEnMemoria() { return maxEnMemoria; }

    // ===== Métricas =====
    public double getCpuUtil() {
        if (cpuTotal == 0) return 0.0;
        return (cpuBusy * 100.0) / cpuTotal;
    }

    public double getTasaExitoMision() {
        if (totalProcesosTerminados == 0) return 0.0;
        return (procesosEnDeadline * 100.0) / totalProcesosTerminados;
    }

    // ===== Logging =====
    public String getLog() {
        String texto = log.toString();
        log.setLength(0); // evitar saturar RAM
        return texto;
    }

    public void logInfo(String msg) {
        logEvent("ℹ️ " + msg);
    }

    public void logError(String msg, Throwable e) {
        logEvent("❌ " + msg + " -> " + e.getClass().getSimpleName() + ": " + e.getMessage());
    }

    public void triggerInterrupt(String reason) {
        interruptPending = true;
        interruptReason = reason;
    }

    private void logEvent(String s) {
        log.append("[ciclo ").append(cicloGlobal).append("] ").append(s).append("\n");
    }

    // ===== Memoria =====
    private int procesosEnMemoria() {
        int c = 0;
        c += listos.size();
        c += bloqueados.size();
        if (running != null && running.getEstado() != Estado.TERMINADO) c += 1;
        return c;
    }

    private void enforceMemory() {
        while (procesosEnMemoria() > maxEnMemoria) {
            PCB victim = null;
            boolean fromListos = true;

            // Preferencia: sacar LISTOS con deadline más lejano, evitando prioridad 0 si es “crítico”
            for (int i = 0; i < listos.size(); i++) {
                PCB p = listos.get(i);
                if (p == null || p.getPrioridad() == 0) continue;
                if (victim == null || p.getDeadlineRestante() > victim.getDeadlineRestante()) {
                    victim = p;
                    fromListos = true;
                }
            }

            // Si no hay en listos, buscar en bloqueados (evitando críticos)
            if (victim == null) {
                for (int i = 0; i < bloqueados.size(); i++) {
                    PCB p = bloqueados.get(i);
                    if (p == null || p.getPrioridad() == 0) continue;
                    if (victim == null || p.getDeadlineRestante() > victim.getDeadlineRestante()) {
                        victim = p;
                        fromListos = false;
                    }
                }
            }

            // Si todos son críticos (prioridad 0), entonces igual saca el menos urgente
            if (victim == null) {
                for (int i = 0; i < listos.size(); i++) {
                    PCB p = listos.get(i);
                    if (p == null) continue;
                    if (victim == null || p.getDeadlineRestante() > victim.getDeadlineRestante()) {
                        victim = p;
                        fromListos = true;
                    }
                }
                if (victim == null) {
                    for (int i = 0; i < bloqueados.size(); i++) {
                        PCB p = bloqueados.get(i);
                        if (p == null) continue;
                        if (victim == null || p.getDeadlineRestante() > victim.getDeadlineRestante()) {
                            victim = p;
                            fromListos = false;
                        }
                    }
                }
            }

            if (victim != null) {
                if (fromListos) {
                    listos.remove(victim);
                    victim.setEstado(Estado.LISTO_SUSPENDIDO);
                    listoSuspendido.addLast(victim);
                    logEvent("SWAP OUT -> RAM llena: Proceso " + victim.getId() + " a LISTO_SUSPENDIDO");
                } else {
                    bloqueados.remove(victim);
                    victim.setEstado(Estado.BLOQUEADO_SUSPENDIDO);
                    bloqueadoSuspendido.addLast(victim);
                    logEvent("SWAP OUT -> RAM llena: Proceso " + victim.getId() + " a BLOQUEADO_SUSPENDIDO");
                }
            } else {
                break;
            }
        }
    }

    private void admitNewProcesses() {
        while (!nuevos.isEmpty()) {
            PCB p = nuevos.get(0);
            if (p == null) break;

            if (procesosEnMemoria() + 1 <= maxEnMemoria) {
                nuevos.removeFirst();
                p.setEstado(Estado.LISTO);
                listos.addLast(p);
                logEvent("SWAP IN -> NUEVO a LISTO: " + p.getId());
            } else {
                nuevos.removeFirst();
                p.setEstado(Estado.LISTO_SUSPENDIDO);
                listoSuspendido.addLast(p);
                logEvent("SWAP OUT DIRECTO -> No hay RAM para NUEVO: " + p.getId());
            }
        }
    }

    private void tryResumeSuspended() {
        while (!listoSuspendido.isEmpty() && procesosEnMemoria() + 1 <= maxEnMemoria) {
            PCB p = listoSuspendido.removeFirst();
            if (p != null) {
                p.setEstado(Estado.LISTO);
                listos.addLast(p);
                logEvent("SWAP IN -> LISTO_SUSPENDIDO a LISTO: " + p.getId());
            }
        }

        while (!bloqueadoSuspendido.isEmpty() && procesosEnMemoria() + 1 <= maxEnMemoria) {
            PCB p = bloqueadoSuspendido.removeFirst();
            if (p != null) {
                p.setEstado(Estado.BLOQUEADO);
                bloqueados.addLast(p);
                logEvent("SWAP IN -> BLOQUEADO_SUSPENDIDO a BLOQUEADO: " + p.getId());
            }
        }
    }

    // ===== Planificación =====
    private void preemptIfNeeded() {
        if (running == null) return;
        PolicyType pol = scheduler.getPolicy();
        if (pol == null || pol == PolicyType.FCFS) return;

        if (pol == PolicyType.ROUND_ROBIN && running.getQuantumRestante() <= 0) {
            running.setEstado(Estado.LISTO);
            listos.addLast(running);
            logEvent("Fin de quantum -> preempt RR: " + running.getId());
            running = null;
            return;
        }

        PCB best = scheduler.pickNext(listos);
        if (best == null) return;

        boolean shouldPreempt = false;
        switch (pol) {
            case SRT -> shouldPreempt = best.getRestantes() < running.getRestantes();
            case PRIORIDAD_PREEMPTIVA -> shouldPreempt = best.getPrioridad() < running.getPrioridad();
            case EDF -> shouldPreempt = best.getDeadlineRestante() < running.getDeadlineRestante();
            default -> {}
        }

        if (shouldPreempt) {
            running.setEstado(Estado.LISTO);
            listos.addLast(running);
            logEvent("Preemption por " + pol + ": sale " + running.getId() + ", entra " + best.getId());
            running = null;
        }
    }

    private void dispatchIfIdle() {
        if (running != null) return;

        PCB next = scheduler.pickNext(listos);
        if (next == null) return;

        listos.remove(next);
        running = next;
        running.setEstado(Estado.EJECUCION);
        running.resetTiempoEspera();

        if (scheduler.getPolicy() == PolicyType.ROUND_ROBIN) {
            running.setQuantumRestante(scheduler.getRrQuantum());
        }

        logEvent("Dispatch -> EJECUCION: " + running.getId());
    }

    // ===== IO / ejecución =====
    private void tickWaitingTimes() {
        for (int i = 0; i < listos.size(); i++) {
            PCB p = listos.get(i);
            if (p != null) p.incTiempoEspera();
        }
    }

    private void handleBlockedIO() {
        int i = 0;
        while (i < bloqueados.size()) {
            PCB p = bloqueados.get(i);
            if (p == null) { i++; continue; }

            p.tickIO();
            if (p.ioTerminada()) {
                bloqueados.remove(p);

                if (procesosEnMemoria() + 1 <= maxEnMemoria) {
                    p.setEstado(Estado.LISTO);
                    listos.addLast(p);
                    logEvent("IO completa -> BLOQUEADO a LISTO: " + p.getId());
                } else {
                    p.setEstado(Estado.BLOQUEADO_SUSPENDIDO);
                    bloqueadoSuspendido.addLast(p);
                    logEvent("IO completa -> SWAP OUT por RAM: " + p.getId());
                }
                continue;
            }

            i++;
        }
    }

    private void spawnIOThread(final PCB p) {
        Thread ioThread = new Thread(() -> {
            // Thread independiente para cumplir requerimiento (modelo simple)
            // La lógica temporal real está modelada con ioRestante en la cola de bloqueados.
        }, "IO-" + p.getId());
        ioThread.setUncaughtExceptionHandler((t, e) -> logError("Error en hilo IO " + t.getName(), e));
        ioThread.start();
    }

    private void executeOneCycle() {
        if (running == null) return;

        cpuBusy++;

        running.tickIOTrigger();
        running.ejecutarUnaInstruccion();

        if (scheduler.getPolicy() == PolicyType.ROUND_ROBIN) {
            running.decQuantum();
        }

        if (running.listoParaIO()) {
            running.setEstado(Estado.BLOQUEADO);
            running.iniciarIO();
            bloqueados.addLast(running);
            spawnIOThread(running);
            logEvent("Proceso " + running.getId() + " lanza IO -> BLOQUEADO");
            running = null;
            return;
        }

        if (running.finalizado()) {
            running.setEstado(Estado.TERMINADO);
            terminados.addLast(running);

            totalProcesosTerminados++;
            if (!running.deadlineFallido()) procesosEnDeadline++;

            logEvent("TERMINADO: " + running.getId() +
                    (running.deadlineFallido() ? " (FALLO DEADLINE)" : " (OK DEADLINE)"));
            running = null;
        }
    }

    // ===== Deadlines =====
    private void checkDeadlineFailures() {
        if (running != null && running.deadlineFallido()) {
            logEvent("Fallo de Deadline en RUNNING: " + running.getId());
        }

        for (int i = 0; i < listos.size(); i++) {
            PCB p = listos.get(i);
            if (p != null && p.deadlineFallido()) logEvent("Fallo de Deadline en LISTO: " + p.getId());
        }

        for (int i = 0; i < bloqueados.size(); i++) {
            PCB p = bloqueados.get(i);
            if (p != null && p.deadlineFallido()) logEvent("Fallo de Deadline en BLOQUEADO: " + p.getId());
        }

        for (int i = 0; i < listoSuspendido.size(); i++) {
            PCB p = listoSuspendido.get(i);
            if (p != null && p.deadlineFallido()) logEvent("Fallo de Deadline en LISTO_SUSP: " + p.getId());
        }

        for (int i = 0; i < bloqueadoSuspendido.size(); i++) {
            PCB p = bloqueadoSuspendido.get(i);
            if (p != null && p.deadlineFallido()) logEvent("Fallo de Deadline en BLOQ_SUSP: " + p.getId());
        }
    }

    private void tickDeadlinesAll() {
        if (running != null) running.tickDeadline();

        for (int i = 0; i < listos.size(); i++) {
            PCB p = listos.get(i);
            if (p != null) p.tickDeadline();
        }

        for (int i = 0; i < bloqueados.size(); i++) {
            PCB p = bloqueados.get(i);
            if (p != null) p.tickDeadline();
        }

        for (int i = 0; i < listoSuspendido.size(); i++) {
            PCB p = listoSuspendido.get(i);
            if (p != null) p.tickDeadline();
        }

        for (int i = 0; i < bloqueadoSuspendido.size(); i++) {
            PCB p = bloqueadoSuspendido.get(i);
            if (p != null) p.tickDeadline();
        }
    }

    // ===== Interrupciones =====
    private void handleInterruptIfAny() {
        if (!interruptPending) return;

        logEvent("--- ALERTA CRITICA: " + interruptReason.toUpperCase() + " ---");

        if (running != null) {
            running.setEstado(Estado.LISTO);
            listos.addLast(running);
            logEvent("Interrupción -> se preempta RUNNING " + running.getId() + " para atender emergencia.");
            running = null;
        }

        PCB emergencia = new PCB("EMERG_" + cicloGlobal,
                5 + (cicloGlobal % 10), 0, 20, false, 0, 0);

        emergencia.setEstado(Estado.LISTO);
        listos.addLast(emergencia);

        logEvent("INYECCION URGENTE -> Se crea tarea de emergencia: " + emergencia.getId());

        interruptPending = false;
        interruptReason = "";
    }

    @Override
    public void run() {
        // Evitar que un error no controlado se vaya a consola
        setUncaughtExceptionHandler((t, e) -> logError("Uncaught CPU thread error", e));

        while (true) {
            if (!runningSim) {
                try { Thread.sleep(50); } catch (InterruptedException ignored) {}
                continue;
            }

            try {
                mutex.acquire();
                try {
                    cpuTotal++;
                    cicloGlobal++;

                    handleInterruptIfAny();
                    admitNewProcesses();
                    enforceMemory();
                    tryResumeSuspended();

                    tickDeadlinesAll();
                    checkDeadlineFailures();

                    handleBlockedIO();

                    preemptIfNeeded();
                    dispatchIfIdle();
                    executeOneCycle();

                    tickWaitingTimes();

                } catch (Exception e) {
                    // IMPORTANTÍSIMO: NUNCA printStackTrace
                    logEvent("⚠️ ALERTA INTERNA CPU: " + e.getClass().getSimpleName() + " -> " + e.getMessage());
                }
            } catch (InterruptedException ignored) {
            } finally {
                mutex.release();
            }

            try { Thread.sleep(cicloMs); } catch (InterruptedException ignored) {}
        }
    }
}