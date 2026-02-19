/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package edd;

/**
 *
 * @author jesus rodriguez
 */
public class Lista <T> {
private Nodo<T> cabeza;
    private int tamano;

    // Constructor
    public Lista() {
    this.cabeza = null;
        this.tamano = 0;
    }

    public boolean esVacia() {
        return cabeza == null;
    }

    public int getTamano() {
        return tamano;
    }

    // 1. INSERTAR: Agrega un elemento al final de la lista
    public void insertar(T dato) {
        Nodo<T> nuevoNodo = new Nodo<>(dato);
        if (esVacia()) {
            cabeza = nuevoNodo;
        } else {
            Nodo<T> aux = cabeza;
            while (aux.getSiguiente() != null) {
                aux = aux.getSiguiente();
            }
            aux.setSiguiente(nuevoNodo);
        }
        tamano++;
    }

    // 2. OBTENER: Devuelve el elemento en una posición específica
    public T obtener(int indice) {
        if (indice < 0 || indice >= tamano) {
            return null;
        }
        Nodo<T> aux = cabeza;
        for (int i = 0; i < indice; i++) {
            aux = aux.getSiguiente();
        }
        return aux.getContenido();
    }

    // 3. ELIMINAR: Busca un elemento y lo borra (Útil para liberar RAM)
    public boolean eliminar(T dato) {
        if (esVacia()) {
            return false;
        }

        // Si es el primero
        if (cabeza.getContenido().equals(dato)) {
            cabeza = cabeza.getSiguiente();
            tamano--;
            return true;
        }

        // Si está en el medio o al final
        Nodo<T> aux = cabeza;
        while (aux.getSiguiente() != null) {
            if (aux.getSiguiente().getContenido().equals(dato)) {
                aux.setSiguiente(aux.getSiguiente().getSiguiente());
                tamano--;
                return true;
            }
            aux = aux.getSiguiente();
        }
        return false;
    }

    //  VACIAR: Limpia la lista completa
    public void vaciar() {
        cabeza = null;
        tamano = 0;
    }
}
