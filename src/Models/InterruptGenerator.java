/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package Models;

import java.util.Random;

public class InterruptGenerator extends Thread {

    private final CPU cpu;
    private volatile boolean running;
    private final Random rnd;

    public InterruptGenerator(CPU cpu) {
        this.cpu = cpu;
        this.running = true;
        this.rnd = new Random();
    }

   public void stopGen() { 
        this.running = false; 
        this.interrupt(); 
    }

    @Override
    public void run() {
        while (running) {
            try {
                int ms = 2000 + rnd.nextInt(4000);
                Thread.sleep(ms);
            } catch (InterruptedException e) {
                // CORRECCIÓN: Salir del ciclo inmediatamente si apagan el generador
                break; 
            }

            // CORRECCIÓN: Doble validación por seguridad
            if (!running) break;

            String reason;
            int k = rnd.nextInt(3);
            if (k == 0) reason = "Ráfaga solar";
            else if (k == 1) reason = "Micro-meteorito";
            else reason = "Comando estación terrestre";

            cpu.triggerInterrupt(reason);
        }
    }
}