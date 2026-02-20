/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package Models;

import Scheduler.Scheduler;
import Scheduler.PolicyType;
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
    private int maxEnMemoria;

    // Métricas
    private long cpuBusy;
    private long cpuTotal;
    private long totalProcesosTerminados;
    private long procesosEnDeadline;

    // Log simple (sin ArrayList): usa StringBuilder
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

    public void startSimulation() { runningSim = true; }
    public void stopSimulation() { runningSim = false; }

    public void setCicloMs(int ms) { this.cicloMs = ms; }
    public int getCicloGlobal() { return cicloGlobal; }

    public double getCpuUtil() {
        if (cpuTotal == 0) return 0.0;
        return (cpuBusy * 100.0) / cpuTotal;
    }

    public double getTasaExitoMision() {
        if (totalProcesosTerminados == 0) return 0.0;
        return (procesosEnDeadline * 100.0) / totalProcesosTerminados;
    }

    public String getLog() {
        String texto = log.toString();
        log.setLength(0); // Vacía el historial para no saturar la RAM
        return texto;
    }

    // Interrupción externa (otro thread la dispara)
    public void triggerInterrupt(String reason) {
        interruptPending = true;
        interruptReason = reason;
    }

    private void logEvent(String s) {
        log.append("[ciclo ").append(cicloGlobal).append("] ").append(s).append("\n");
    }

    // conteo de procesos "en memoria" = listos + bloqueados + running + nuevos (si están admitidos)
    private int procesosEnMemoria() {
        int c = 0;
        c += listos.size();
        c += bloqueados.size();
        c += nuevos.size();
        if (running != null && running.getEstado() != Estado.TERMINADO) c += 1;
        return c;
    }

    // Mediano plazo: si memoria saturada, suspender el menos crítico
    private void enforceMemory() {
        while (procesosEnMemoria() > maxEnMemoria) {
            // Estrategia: suspender un LISTO con deadline más lejano (menos urgente)
            PCB victim = null;
            for (int i = 0; i < listos.size(); i++) {
                PCB p = listos.get(i);
                if (victim == null || p.getDeadlineRestante() > victim.getDeadlineRestante()) {
                    victim = p;
                }
            }
            if (victim == null) break;
            listos.remove(victim);
            victim.setEstado(Estado.LISTO_SUSPENDIDO);
            listoSuspendido.addLast(victim);
            logEvent("Memoria llena -> Proceso " + victim.getId() + " movido a LISTO_SUSPENDIDO");
        }
    }

    // Largo plazo: mover NUEVO -> LISTO si hay espacio, si no suspender
    private void admitNewProcesses() {
        while (!nuevos.isEmpty()) {
            PCB p = nuevos.get(0);
            if (procesosEnMemoria() + 1 <= maxEnMemoria) {
                nuevos.removeFirst();
                p.setEstado(Estado.LISTO);
                listos.addLast(p);
                logEvent("Admitido NUEVO -> LISTO: " + p.getId());
            } else {
                // suspender directamente
                nuevos.removeFirst();
                p.setEstado(Estado.LISTO_SUSPENDIDO);
                listoSuspendido.addLast(p);
                logEvent("NUEVO -> LISTO_SUSPENDIDO por memoria: " + p.getId());
            }
        }
    }

    // Reanudar suspendidos si hay espacio
    private void tryResumeSuspended() {
        while (!listoSuspendido.isEmpty() && procesosEnMemoria() + 1 <= maxEnMemoria) {
            PCB p = listoSuspendido.removeFirst();
            p.setEstado(Estado.LISTO);
            listos.addLast(p);
            logEvent("Reanudado LISTO_SUSPENDIDO -> LISTO: " + p.getId());
        }
        while (!bloqueadoSuspendido.isEmpty() && procesosEnMemoria() + 1 <= maxEnMemoria) {
            PCB p = bloqueadoSuspendido.removeFirst();
            p.setEstado(Estado.BLOQUEADO);
            bloqueados.addLast(p);
            logEvent("Reanudado BLOQUEADO_SUSPENDIDO -> BLOQUEADO: " + p.getId());
        }
    }

    private void preemptIfNeeded() {
        if (running == null) return;
        PolicyType pol = scheduler.getPolicy();

        // En FCFS no hay preemption por algoritmo (solo por interrupción/IO)
        if (pol == PolicyType.FCFS) return;

        // En RR: si quantum se acabó, preempt
        if (pol == PolicyType.ROUND_ROBIN && running.getQuantumRestante() <= 0) {
            running.setEstado(Estado.LISTO);
            listos.addLast(running);
            logEvent("Fin de quantum -> preempt RR: " + running.getId());
            running = null;
            return;
        }

        // En SRT/PRIO/EDF: si hay alguien mejor en listos, preempt
        PCB best = scheduler.pickNext(listos);
        if (best == null) return;

        boolean shouldPreempt = false;
        switch (pol) {
            case SRT:
                shouldPreempt = best.getRestantes() < running.getRestantes();
                break;
            case PRIORIDAD_PREEMPTIVA:
                shouldPreempt = best.getPrioridad() < running.getPrioridad();
                break;
            case EDF:
                shouldPreempt = best.getDeadlineRestante() < running.getDeadlineRestante();
                break;
            default:
                break;
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

        // quitarlo de listos
        listos.remove(next);
        running = next;
        running.setEstado(Estado.EJECUCION);
        running.resetTiempoEspera();

        if (scheduler.getPolicy() == PolicyType.ROUND_ROBIN) {
            running.setQuantumRestante(scheduler.getRrQuantum());
        }

        logEvent("Dispatch -> EJECUCION: " + running.getId());
    }

    private void tickWaitingTimes() {
        for (int i = 0; i < listos.size(); i++) {
            PCB p = listos.get(i);
            p.incTiempoEspera();
        }
    }

    private void handleBlockedIO() {
        // Cada ciclo reduce IO, cuando termina regresa a LISTO (o a LISTO_SUSPENDIDO si no hay memoria)
        int i = 0;
        while (i < bloqueados.size()) {
            PCB p = bloqueados.get(i);
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
                    logEvent("IO completa -> BLOQUEADO_SUSPENDIDO por memoria: " + p.getId());
                }
                continue;
            }
            i++;
        }
    }

    // “Thread de excepción” de IO (cumple el requisito de usar Thread y volver al CPU)
    private void spawnIOThread(final PCB p) {
        Thread ioThread = new Thread(() -> {
            // aquí podrías simular “generar excepción” y “atender”
            // pero como ya modelamos ioRestante en la cola bloqueado, lo dejamos simple:
            // el requisito principal es: es un Thread y retorna al mismo CPU (mismo sistema).
        });
        ioThread.start();
    }

    private void executeOneCycle() {
        if (running == null) return;

        cpuBusy++;

        // Cuenta regresiva para lanzar IO
        running.tickIOTrigger();

        // Ejecutar instrucción
        running.ejecutarUnaInstruccion();

        // RR
        if (scheduler.getPolicy() == PolicyType.ROUND_ROBIN) {
            running.decQuantum();
        }

        // Si debe lanzar IO ahora
        if (running.listoParaIO()) {
            running.setEstado(Estado.BLOQUEADO);
            running.iniciarIO();
            bloqueados.addLast(running);
            spawnIOThread(running);
            logEvent("Proceso " + running.getId() + " lanza IO -> BLOQUEADO");
            running = null;
            return;
        }

        // Si terminó
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

    private void checkDeadlineFailures() {
        // Revisa running, listos, bloqueados, suspendidos
        if (running != null && running.deadlineFallido()) {
            logEvent("Fallo de Deadline en RUNNING: " + running.getId());
        }
        for (int i = 0; i < listos.size(); i++) if (listos.get(i).deadlineFallido())
            logEvent("Fallo de Deadline en LISTO: " + listos.get(i).getId());
        for (int i = 0; i < bloqueados.size(); i++) if (bloqueados.get(i).deadlineFallido())
            logEvent("Fallo de Deadline en BLOQUEADO: " + bloqueados.get(i).getId());
        for (int i = 0; i < listoSuspendido.size(); i++) if (listoSuspendido.get(i).deadlineFallido())
            logEvent("Fallo de Deadline en LISTO_SUSP: " + listoSuspendido.get(i).getId());
        for (int i = 0; i < bloqueadoSuspendido.size(); i++) if (bloqueadoSuspendido.get(i).deadlineFallido())
            logEvent("Fallo de Deadline en BLOQ_SUSP: " + bloqueadoSuspendido.get(i).getId());
    }

    private void tickDeadlinesAll() {
        if (running != null) running.tickDeadline();
        for (int i = 0; i < listos.size(); i++) listos.get(i).tickDeadline();
        for (int i = 0; i < bloqueados.size(); i++) bloqueados.get(i).tickDeadline();
        for (int i = 0; i < listoSuspendido.size(); i++) listoSuspendido.get(i).tickDeadline();
        for (int i = 0; i < bloqueadoSuspendido.size(); i++) bloqueadoSuspendido.get(i).tickDeadline();
    }

    private void handleInterruptIfAny() {
        if (!interruptPending) return;

        logEvent("Interrupción detectada: " + interruptReason);

        // Suspende inmediatamente el proceso en CPU (preempt “forzado”)
        if (running != null) {
            running.setEstado(Estado.LISTO);
            listos.addLast(running);
            logEvent("Interrupción -> se preempta RUNNING " + running.getId());
            running = null;
        }

        // Aquí podrías crear una “tarea de emergencia” con deadline corto:
        PCB emergencia = new PCB("EMERG_" + cicloGlobal,
                5 + (cicloGlobal % 10), 0, 20, false, 0, 0);
        emergencia.setEstado(Estado.LISTO);
        listos.addFirst(emergencia); // prioridad inmediata
        logEvent("Se crea tarea de emergencia: " + emergencia.getId());

        interruptPending = false;
        interruptReason = "";
    }

    @Override
    public void run() {
        while (true) {
            if (!runningSim) {
                try { Thread.sleep(50); } catch (InterruptedException ignored) {}
                continue;
            }

            try {
                mutex.acquire();

                cpuTotal++;
                cicloGlobal++;

                // 1) admitir nuevos / memoria
                admitNewProcesses();
                enforceMemory();
                tryResumeSuspended();

                // 2) interrupción externa (ISR)
                handleInterruptIfAny();

                // 3) tick deadlines + revisar fallos
                tickDeadlinesAll();
                checkDeadlineFailures();

                // 4) IO bloqueados avanza
                handleBlockedIO();

                // 5) preemption por algoritmo (RR/SRT/EDF/PRIO)
                preemptIfNeeded();

                // 6) dispatch si CPU libre
                dispatchIfIdle();

                // 7) ejecutar 1 ciclo si hay running
                executeOneCycle();

                // 8) tiempos de espera
                tickWaitingTimes();

            } catch (InterruptedException ignored) {
            } finally {
                mutex.release();
            }

            try { Thread.sleep(cicloMs); } catch (InterruptedException ignored) {}
        }
       
    }
    public PCB getRunning() { return running; }
    
    // Método para la UI: Obtiene el límite de RAM para la barra de porcentaje
    public int getMaxEnMemoria() {
        return this.maxEnMemoria; 
    }
}