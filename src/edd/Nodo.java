/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package edd;

public class Nodo <T> {
    private T contenido;
    
    private Nodo<T> siguiente;

    public Nodo(T contenido) {
        this.contenido = contenido;
        this.siguiente = null;
    }

    /**
     * @return the contenido
     */
    public T getContenido() {
        return contenido;
    }

    /**
     * @param contenido the contenido to set
     */
    public void setContenido(T contenido) {
        this.contenido = contenido;
    }

    /**
     * @return the siguiente
     */
    public Nodo<T> getSiguiente() {
        return siguiente;
    }

    /**
     * @param siguiente the siguiente to set
     */
    public void setSiguiente(Nodo<T> siguiente) {
        this.siguiente = siguiente;
    }
    
    
    
    
    
    
    
    
    
    
}
