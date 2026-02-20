/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */


/**
 *
 * @author jleal
 */
package Scheduler;

import Models.PCB;
import edd.Cola;
import edd.Lista;
import edd.Nodo;

public class Scheduler {

    private PolicyType policy;
    private int rrQuantum;

    public Scheduler(PolicyType policy, int rrQuantum) {
        this.policy = policy;
        this.rrQuantum = rrQuantum;
    }

    public void setPolicy(PolicyType p) { this.policy = p; }
    public PolicyType getPolicy() { return policy; }

    public int getRrQuantum() { return rrQuantum; }
    public void setRrQuantum(int q) { this.rrQuantum = q; }

    // NOTA: listos puede ser Cola o Lista. Aquí uso Lista para poder buscar el "mejor".
    public PCB pickNext(Lista<PCB> readyList) {
        if (readyList.isEmpty()) return null;

        switch (policy) {
            case FCFS:
            case ROUND_ROBIN:
                // En FCFS/RR, el "orden de llegada" se respeta: devolver primero
                return readyList.get(0);

            case SRT:
                return pickMinRemaining(readyList);

            case PRIORIDAD_PREEMPTIVA:
                return pickMinPriority(readyList);

            case EDF:
                return pickMinDeadline(readyList);

            default:
                return readyList.get(0);
        }
    }

    private PCB pickMinRemaining(Lista<PCB> list) {
        PCB best = null;
        Nodo<PCB> n = list.getHeadNode();
        while (n != null) {
            PCB p = n.getContenido();
            if (best == null || p.getRestantes() < best.getRestantes()) best = p;
            n = n.getSiguiente();
        }
        return best;
    }

    private PCB pickMinDeadline(Lista<PCB> list) {
        PCB best = null;
        Nodo<PCB> n = list.getHeadNode();
        while (n != null) {
            PCB p = n.getContenido();
            if (best == null || p.getDeadlineRestante() < best.getDeadlineRestante()) best = p;
            n = n.getSiguiente();
        }
        return best;
    }

    private PCB pickMinPriority(Lista<PCB> list) {
        PCB best = null;
        Nodo<PCB> n = list.getHeadNode();
        while (n != null) {
            PCB p = n.getContenido();
            if (best == null || p.getPrioridad() < best.getPrioridad()) best = p;
            n = n.getSiguiente();
        }
        return best;
    }
}
