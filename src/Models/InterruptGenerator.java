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

    public void stopGen() { running = false; }

    @Override
    public void run() {
        while (running) {
            try {
                // cada 2 a 6 segundos dispara una interrupción
                int ms = 2000 + rnd.nextInt(4000);
                Thread.sleep(ms);
            } catch (InterruptedException ignored) {}

            // Razones tipo misión
            String reason;
            int k = rnd.nextInt(3);
            if (k == 0) reason = "Ráfaga solar";
            else if (k == 1) reason = "Micro-meteorito";
            else reason = "Comando estación terrestre";

            cpu.triggerInterrupt(reason);
        }
    }
}