package ar.edu.utn.frbb.tup.service;

import ar.edu.utn.frbb.tup.model.Cliente;
import ar.edu.utn.frbb.tup.model.Cuenta;
import ar.edu.utn.frbb.tup.model.exception.CuentaAlreadyExistsException;
import ar.edu.utn.frbb.tup.model.exception.CuentaNoSoportadaException;
import ar.edu.utn.frbb.tup.model.exception.TipoCuentaAlreadyExistsException;
import ar.edu.utn.frbb.tup.persistence.CuentaDao;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.*;

@Component
public class CuentaService {
    CuentaDao cuentaDao = new CuentaDao();

    @Autowired
    ClienteService clienteService;

    //Agrego tipos de cuenta soportados:
    private static final Set<String> CUENTAS_SOPORTADAS = new HashSet<>();

    static {
        CUENTAS_SOPORTADAS.add("CAJA_AHORROPESOS");
        CUENTAS_SOPORTADAS.add("CUENTA_CORRIENTEPESOS");
        CUENTAS_SOPORTADAS.add("CAJA_AHORRODOLARES");
    }

    public CuentaService() {}

    public CuentaService(CuentaDao cuentaDao, ClienteService clienteService) {
        this.cuentaDao = cuentaDao;
        this.clienteService = clienteService;
    }

    //Generar casos de test para darDeAltaCuenta
    //    1 - cuenta existente
    //    2 - cuenta no soportada
    //    3 - cliente ya tiene cuenta de ese tipo
    //    4 - cuenta creada exitosamente

    public void darDeAltaCuenta(Cuenta cuenta, long dniTitular) throws CuentaAlreadyExistsException, TipoCuentaAlreadyExistsException, CuentaNoSoportadaException {
        if(cuentaDao.find(cuenta.getNumeroCuenta()) != null) {
            throw new CuentaAlreadyExistsException("La cuenta " + cuenta.getNumeroCuenta() + " ya existe.");
        }
        //Agrego chequeo de cuentas soportadas por el banco segun la consigna: CA$ CC$ CAU$S
        if (!tipoDeCuentaSoportada(cuenta)) {
            throw new CuentaNoSoportadaException("La cuenta " + cuenta.getTipoCuenta() + " en " + cuenta.getMoneda() + " no está soportada.");
        }

        // Verifico si el cliente ya tiene una cuenta del mismo tipo y moneda
        Cliente titular = clienteService.buscarClientePorDni(dniTitular);
        if (titular.tieneCuenta(cuenta.getTipoCuenta(), cuenta.getMoneda())) {
            throw new TipoCuentaAlreadyExistsException("El cliente ya tiene una cuenta del tipo " + cuenta.getTipoCuenta() + " en " + cuenta.getMoneda() + ".");
        }

        // Asocio cuenta al cliente y guardo
        clienteService.agregarCuenta(cuenta,dniTitular);
        cuentaDao.save(cuenta);
    }

    public Cuenta find(long id) {
        return cuentaDao.find(id);
    }

    //agrego método tipoDeCuentaSoportada para comprobar si una cuenta con determinada moneda puede ser generada.
    public boolean tipoDeCuentaSoportada(Cuenta cuenta) throws CuentaNoSoportadaException {
        String tipoCuenta = cuenta.getTipoCuenta().name();
        String moneda = cuenta.getMoneda().name();
        String tipoYMoneda = tipoCuenta + moneda;

        return CUENTAS_SOPORTADAS.contains(tipoYMoneda);
    }

}


