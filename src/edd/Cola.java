/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package edd;

public class Cola<T> {
    private Nodo<T> frente;
    private Nodo<T> fin;
    private int size;

    public Cola() {
        frente = null;
        fin = null;
        size = 0;
    }

    public int size() { return size; }
    public boolean isEmpty() { return size == 0; }

    public void enqueue(T val) {
        Nodo<T> n = new Nodo<>(val);
        if (fin == null) {
            frente = n;
            fin = n;
        } else {
            fin.setSiguiente(n);
            fin = n;
        }
        size++;
    }

    public T dequeue() {
        if (frente == null) return null;
        T val = frente.getContenido();
        frente = frente.getSiguiente();
        if (frente == null) fin = null;
        size--;
        return val;
    }

    public T peek() {
        return (frente == null) ? null : frente.getContenido();
    }

    public Nodo<T> getFrontNode() { return frente; }

    // útil para reordenar: sacar un elemento específico (referencia)
    public boolean remove(T val) {
        if (frente == null) return false;
        if (frente.getContenido() == val) {
            dequeue();
            return true;
        }
        Nodo<T> prev = frente;
        Nodo<T> aux = frente.getSiguiente();
        while (aux != null) {
            if (aux.getContenido() == val) {
                prev.setSiguiente(aux.getSiguiente());
                if (aux == fin) fin = prev;
                size--;
                return true;
            }
            prev = aux;
            aux = aux.getSiguiente();
        }
        return false;
    }
}