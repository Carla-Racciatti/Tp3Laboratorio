package ar.edu.utn.frbb.tup.service;

import ar.edu.utn.frbb.tup.model.Cliente;
import ar.edu.utn.frbb.tup.model.Cuenta;
import ar.edu.utn.frbb.tup.model.TipoMoneda;
import ar.edu.utn.frbb.tup.model.TipoCuenta;
import ar.edu.utn.frbb.tup.model.exception.ClienteAlreadyExistsException;
import ar.edu.utn.frbb.tup.model.exception.CuentaAlreadyExistsException;
import ar.edu.utn.frbb.tup.model.exception.TipoCuentaAlreadyExistsException;
import ar.edu.utn.frbb.tup.model.exception.CuentaNoSoportadaException;
import ar.edu.utn.frbb.tup.persistence.ClienteDao;
import ar.edu.utn.frbb.tup.persistence.CuentaDao;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.mockito.Mockito.*;
import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
public class CuentaServiceTest {

    @Mock
    private CuentaDao cuentaDao;

    @Mock
    private ClienteDao clienteDao;

    @Mock
    private ClienteService clienteService;

    @InjectMocks
    private CuentaService cuentaService;

    final long numeroCuenta = 12345678;

    final long dni= 40022659;

    @BeforeEach
    public void setUp() {
        MockitoAnnotations.openMocks(this);
    }


    /**
     * Testeo cuenta existente
     */
    @Test
    public void testCuentaExistente() {
        Cuenta cuenta = new Cuenta();
        cuenta.setNumeroCuenta(numeroCuenta);

        when(cuentaDao.find(numeroCuenta)).thenReturn(cuenta);

        assertThrows(CuentaAlreadyExistsException.class, () -> cuentaService.darDeAltaCuenta(cuenta, dni));
    }

    /**
     * Testeo cuenta no soportada
     */
    @Test
    public void testCuentaNoSoportada() {
        Cuenta cuenta = new Cuenta();
        cuenta.setTipoCuenta(TipoCuenta.CUENTA_CORRIENTE);
        cuenta.setMoneda(TipoMoneda.DOLARES);

        assertThrows(CuentaNoSoportadaException.class, () -> cuentaService.darDeAltaCuenta(cuenta, dni));
    }

    /**
     * Testeo que no se pueda agregar una cuenta si el cliente ya posee una cuenta del mismo tipo y moneda.
     */
    @Test
    public void testClienteYaTieneCuentaDeEseTipoYMoneda()  {
        // Creo un cliente existente con una cuenta del mismo tipo y moneda
        Cliente clienteExistente = new Cliente();
        clienteExistente.setDni(dni);
        Cuenta cuentaExistente = new Cuenta();
        cuentaExistente.setTipoCuenta(TipoCuenta.CAJA_AHORRO);
        cuentaExistente.setMoneda(TipoMoneda.PESOS);
        clienteExistente.addCuenta(cuentaExistente);


        when(clienteService.buscarClientePorDni(dni)).thenReturn(clienteExistente);

        // Creo la cuenta a dar de alta (misma tipo y moneda que cuentaExistente)
        Cuenta cuenta = new Cuenta();
        cuenta.setNumeroCuenta(numeroCuenta); // Número de cuenta diferente
        cuenta.setTipoCuenta(TipoCuenta.CAJA_AHORRO);
        cuenta.setMoneda(TipoMoneda.PESOS);

        // Verifico que se lance TipoCuentaAlreadyExistsException al intentar dar de alta la cuenta
        assertThrows(TipoCuentaAlreadyExistsException.class, () -> cuentaService.darDeAltaCuenta(cuenta, dni));
    }


    /**
     *Testeo que la cuenta se cree exitosamente
     */
    @Test
    public void testCreacionCuentaExitosa() throws CuentaAlreadyExistsException, TipoCuentaAlreadyExistsException, CuentaNoSoportadaException {

        Cliente clienteMock = new Cliente();

        // Simulo cuenta no existente
        when(cuentaDao.find(numeroCuenta)).thenReturn(null);

        when(clienteService.buscarClientePorDni(dni)).thenReturn(clienteMock);

        // Creo una cuenta
        Cuenta cuenta = new Cuenta();
        cuenta.setMoneda(TipoMoneda.PESOS);
        cuenta.setBalance(77000);
        cuenta.setTipoCuenta(TipoCuenta.CUENTA_CORRIENTE);
        cuenta.setNumeroCuenta(numeroCuenta);

        // Llamo al método para dar de alta la cuenta
        cuentaService.darDeAltaCuenta(cuenta, dni);

        // Verifico que se agregó la cuenta al cliente
        verify(clienteService, times(1)).agregarCuenta(cuenta, dni);

        // Verifico que se guardó la cuenta en el dao de cuentas
        verify(cuentaDao, times(1)).save(cuenta);
    }
}
