/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package edd;

/**
 *
 * @author jesus rodriguez
 */
public class Cola <T>{
    private Nodo<T> cabeza;
    private Nodo<T> cola;
    private int tamano;

    public Cola() {
        this.cabeza = null;
        this.cola = null;
        this.tamano = 0;
    }

    public boolean esVacia() {
        return cabeza == null;
    }

    public int getTamano() {
        return tamano;
    }

    // 1. ENCOLAR: Inserta al final
    public void encolar(T dato) {
        Nodo<T> nuevoNodo = new Nodo<>(dato);
        if (esVacia()) {
            cabeza = nuevoNodo;
            cola = nuevoNodo;
        } else {
            this.cola.setSiguiente(nuevoNodo);
            this.cola = nuevoNodo;
        }
        tamano++;
    }

    // 2. DESENCOLAR: Saca por el frente (Para dárselo a la CPU)
    public T desencolar() {
        if (esVacia()) return null;
        
        T dato = cabeza.getContenido();
        cabeza = cabeza.getSiguiente();
        
        if (cabeza == null) {
            cola = null;
        }
        tamano--;
        return dato;
    }

    // 3. OBTENER: Para que la Interfaz Gráfica pueda pintar la cola
    public T obtener(int indice) {
        if (indice < 0 || indice >= tamano) return null;
        Nodo<T> actual = cabeza;
        for (int i = 0; i < indice; i++) {
            actual = actual.getSiguiente();
        }
        return actual.getContenido();
    }

    // 4. ELIMINAR ESPECÍFICO: Para sacar procesos y mandarlos a Suspendido (Swapping)
    public boolean eliminar(T dato) {
        if (esVacia()) return false;

        if (cabeza.getContenido().equals(dato)) {
            desencolar();
            return true;
        }

        Nodo<T> actual = cabeza;
        while (actual.getSiguiente() != null) {
            if (actual.getSiguiente().getContenido().equals(dato)) {
                actual.setSiguiente(actual.getSiguiente().getSiguiente());
                if (actual.getSiguiente() == null) {
                    cola = actual;
                }
                tamano--;
                return true;
            }
            actual = actual.getSiguiente();
        }
        return false;
    }
    
    public void vaciar() {
        this.cabeza = null;
        this.cola = null;
        this.tamano = 0;
    }
}
