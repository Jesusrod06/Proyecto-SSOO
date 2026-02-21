/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package edd;

public class Lista<T> {
    private Nodo<T> cabeza;
    private int size;

    public Lista() {
        this.cabeza = null;
        this.size = 0;
    }

    public int size() { return size; }
    public boolean isEmpty() { return size == 0; }

    public void addLast(T value) {
        Nodo<T> n = new Nodo<>(value);
        if (cabeza == null) {
            cabeza = n;
        } else {
            Nodo<T> aux = cabeza;
            while (aux.getSiguiente() != null) aux = aux.getSiguiente();
            aux.setSiguiente(n);
        }
        size++;
    }

    public void addFirst(T value) {
        Nodo<T> n = new Nodo<>(value);
        n.setSiguiente(cabeza);
        cabeza = n;
        size++;
    }

    public T get(int index) {
        if (index < 0 || index >= size) return null;
        Nodo<T> aux = cabeza;
        for (int i = 0; i < index; i++) aux = aux.getSiguiente();
        return aux.getContenido();
    }

    public T removeFirst() {
        if (cabeza == null) return null;
        T val = cabeza.getContenido();
        cabeza = cabeza.getSiguiente();
        size--;
        return val;
    }

    public boolean remove(T value) {
        if (cabeza == null) return false;
        // CORRECCIÓN: Usar .equals() en lugar de ==
        if (cabeza.getContenido().equals(value)) {
            cabeza = cabeza.getSiguiente();
            size--;
            return true;
        }
        Nodo<T> prev = cabeza;
        Nodo<T> aux = cabeza.getSiguiente();
        while (aux != null) {
            // CORRECCIÓN: Usar .equals() en lugar de ==
            if (aux.getContenido().equals(value)) {
                prev.setSiguiente(aux.getSiguiente());
                size--;
                return true;
            }
            prev = aux;
            aux = aux.getSiguiente();
        }
        return false;
    }

    public Nodo<T> getHeadNode() { return cabeza; }

    public interface Cmp<T> { int compare(T a, T b); }

    public void insertSorted(T value, Cmp<T> cmp) {
        Nodo<T> n = new Nodo<>(value);
        if (cabeza == null || cmp.compare(value, cabeza.getContenido()) <= 0) {
            n.setSiguiente(cabeza);
            cabeza = n;
            size++;
            return;
        }
        Nodo<T> prev = cabeza;
        Nodo<T> aux = cabeza.getSiguiente();
        while (aux != null && cmp.compare(value, aux.getContenido()) > 0) {
            prev = aux;
            aux = aux.getSiguiente();
        }
        prev.setSiguiente(n);
        n.setSiguiente(aux);
        size++;
    }
}