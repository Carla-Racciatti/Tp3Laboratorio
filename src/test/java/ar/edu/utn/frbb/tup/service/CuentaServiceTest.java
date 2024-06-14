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

    @BeforeEach
    public void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    public void testCuentaExistente() {
        Cuenta cuenta = new Cuenta();
        cuenta.setNumeroCuenta(123456);

        when(cuentaDao.find(123456)).thenReturn(cuenta);

        assertThrows(CuentaAlreadyExistsException.class, () -> cuentaService.darDeAltaCuenta(cuenta, 12345678));
    }

    @Test
    public void testCuentaNoSoportada() {
        Cuenta cuenta = new Cuenta();
        cuenta.setTipoCuenta(TipoCuenta.CUENTA_CORRIENTE);
        cuenta.setMoneda(TipoMoneda.EUROS);

        assertThrows(CuentaNoSoportadaException.class, () -> cuentaService.darDeAltaCuenta(cuenta, 12345678));
    }

    @Test
    public void testClienteYaTieneCuentaDeEseTipoYMoneda() throws CuentaAlreadyExistsException, TipoCuentaAlreadyExistsException, CuentaNoSoportadaException {
        // Crear un cliente existente con una cuenta del mismo tipo y moneda
        Cliente clienteExistente = new Cliente();
        clienteExistente.setDni(12345678); // DNI del cliente
        Cuenta cuentaExistente = new Cuenta();
        cuentaExistente.setTipoCuenta(TipoCuenta.CAJA_AHORRO);
        cuentaExistente.setMoneda(TipoMoneda.PESOS);
        clienteExistente.addCuenta(cuentaExistente); // Agregar cuenta existente al cliente

        // Configurar el comportamiento del mock de ClienteService para que devuelva clienteExistente
        when(clienteService.buscarClientePorDni(12345678)).thenReturn(clienteExistente);

        // Crear la cuenta a dar de alta (misma tipo y moneda que cuentaExistente)
        Cuenta cuenta = new Cuenta();
        cuenta.setNumeroCuenta(987654); // Número de cuenta diferente
        cuenta.setTipoCuenta(TipoCuenta.CAJA_AHORRO);
        cuenta.setMoneda(TipoMoneda.PESOS);

        // Verificar que se lance TipoCuentaAlreadyExistsException al intentar dar de alta la cuenta
        assertThrows(TipoCuentaAlreadyExistsException.class, () -> cuentaService.darDeAltaCuenta(cuenta, 12345678));
    }

    @Test
    public void testCreacionCuentaExitosa() throws CuentaAlreadyExistsException, TipoCuentaAlreadyExistsException, CuentaNoSoportadaException {
        // Crear un cliente mock con cuentas vacías
        Cliente clienteMock = new Cliente();

        // Configurar el comportamiento del mock de clienteService para que devuelva el cliente mock
        when(clienteService.buscarClientePorDni(40022659L)).thenReturn(clienteMock);

        // Configurar el comportamiento del mock de cuentaDao para simular cuenta no existente
        when(cuentaDao.find(anyLong())).thenReturn(null);

        // Configurar el comportamiento de clienteService para simular agregar la cuenta al cliente
        doNothing().when(clienteService).agregarCuenta(any(Cuenta.class), eq(40022659L));

        // Crear una cuenta
        Cuenta cuenta = new Cuenta();
        cuenta.setMoneda(TipoMoneda.PESOS);
        cuenta.setBalance(77000);
        cuenta.setTipoCuenta(TipoCuenta.CUENTA_CORRIENTE);
        cuenta.setNumeroCuenta(123456);

        // Llamar al método para dar de alta la cuenta
        cuentaService.darDeAltaCuenta(cuenta, 40022659L);

        // Verificar que se agregó la cuenta al cliente
        verify(clienteService, times(1)).agregarCuenta(cuenta, 40022659L);

        // Verificar que se guardó la cuenta en el dao de cuentas
        verify(cuentaDao, times(1)).save(cuenta);
    }
}
