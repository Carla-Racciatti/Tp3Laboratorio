//Agrego excepción
package ar.edu.utn.frbb.tup.model.exception;

public class CuentaNoSoportadaException extends Exception {
    public CuentaNoSoportadaException(String message) {
        super(message);
    }
}
