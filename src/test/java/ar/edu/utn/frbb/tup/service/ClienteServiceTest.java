package ar.edu.utn.frbb.tup.service;

import ar.edu.utn.frbb.tup.model.*;
import ar.edu.utn.frbb.tup.model.exception.ClienteAlreadyExistsException;
import ar.edu.utn.frbb.tup.model.exception.TipoCuentaAlreadyExistsException;
import ar.edu.utn.frbb.tup.persistence.ClienteDao;




import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class ClienteServiceTest {

    @Mock
    private ClienteDao clienteDao;

    @InjectMocks
    private ClienteService clienteService;

    @BeforeAll
    public void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    public void testClienteMenor18Años() {
        Cliente clienteMenorDeEdad = new Cliente();
        clienteMenorDeEdad.setFechaNacimiento(LocalDate.of(2020, 2, 7));
        assertThrows(IllegalArgumentException.class, () -> clienteService.darDeAltaCliente(clienteMenorDeEdad));
    }

    @Test
    public void testClienteSuccess() throws ClienteAlreadyExistsException {
        Cliente cliente = new Cliente();
        cliente.setFechaNacimiento(LocalDate.of(1978,3,25));
        cliente.setDni(29857643);
        cliente.setTipoPersona(TipoPersona.PERSONA_FISICA);
        clienteService.darDeAltaCliente(cliente);

        verify(clienteDao, times(1)).save(cliente);
    }

    @Test
    public void testClienteAlreadyExistsException() throws ClienteAlreadyExistsException {
        Cliente pepeRino = new Cliente();
        pepeRino.setDni(26456437);
        pepeRino.setNombre("Pepe");
        pepeRino.setApellido("Rino");
        pepeRino.setFechaNacimiento(LocalDate.of(1978, 3,25));
        pepeRino.setTipoPersona(TipoPersona.PERSONA_FISICA);

        when(clienteDao.find(26456437, false)).thenReturn(new Cliente());

        assertThrows(ClienteAlreadyExistsException.class, () -> clienteService.darDeAltaCliente(pepeRino));
    }



    @Test
    public void testAgregarCuentaAClienteSuccess() throws TipoCuentaAlreadyExistsException {
        Cliente pepeRino = new Cliente();
        pepeRino.setDni(26456439);
        pepeRino.setNombre("Pepe");
        pepeRino.setApellido("Rino");
        pepeRino.setFechaNacimiento(LocalDate.of(1978, 3,25));
        pepeRino.setTipoPersona(TipoPersona.PERSONA_FISICA);

        Cuenta cuenta = new Cuenta()
                .setMoneda(TipoMoneda.PESOS)
                .setBalance(500000)
                .setTipoCuenta(TipoCuenta.CAJA_AHORRO);

        when(clienteDao.find(26456439, true)).thenReturn(pepeRino);

        clienteService.agregarCuenta(cuenta, pepeRino.getDni());

        verify(clienteDao, times(1)).save(pepeRino);

        assertEquals(1, pepeRino.getCuentas().size());
        assertEquals(pepeRino, cuenta.getTitular());

    }


    @Test
    public void testAgregarCuentaAClienteDuplicada() throws TipoCuentaAlreadyExistsException {
        Cliente luciano = new Cliente();
        luciano.setDni(26456439);
        luciano.setNombre("Pepe");
        luciano.setApellido("Rino");
        luciano.setFechaNacimiento(LocalDate.of(1978, 3,25));
        luciano.setTipoPersona(TipoPersona.PERSONA_FISICA);

        Cuenta cuenta = new Cuenta()
                .setMoneda(TipoMoneda.PESOS)
                .setBalance(500000)
                .setTipoCuenta(TipoCuenta.CAJA_AHORRO);

        when(clienteDao.find(26456439, true)).thenReturn(luciano);

        clienteService.agregarCuenta(cuenta, luciano.getDni());

        Cuenta cuenta2 = new Cuenta()
                .setMoneda(TipoMoneda.PESOS)
                .setBalance(500000)
                .setTipoCuenta(TipoCuenta.CAJA_AHORRO);

        assertThrows(TipoCuentaAlreadyExistsException.class, () -> clienteService.agregarCuenta(cuenta2, luciano.getDni()));
        verify(clienteDao, times(1)).save(luciano);
        assertEquals(1, luciano.getCuentas().size());
        assertEquals(luciano, cuenta.getTitular());

    }

    //Agregar una CA$ y CC$ --> success 2 cuentas, titular peperino
    //Agregar una CA$ y CAU$S --> success 2 cuentas, titular peperino...
    //Testear clienteService.buscarPorDni


    /**
     *  Testeo que se pueden agregar dos cuentas en pesos a un mismo titular: una caja de ahorro y una cuenta corriente
     */
    @Test
    public void testAgregarCajaAhorroYCorriente() throws TipoCuentaAlreadyExistsException {

        Cliente cliente = this.crearCliente();

        // Agregar una CA$
        Cuenta cuentaCajaAhorro = new Cuenta()
                .setMoneda(TipoMoneda.PESOS)
                .setBalance(500000)
                .setTipoCuenta(TipoCuenta.CAJA_AHORRO);

        // Agregar una CC$
        Cuenta cuentaCuentaCorriente = new Cuenta()
                .setMoneda(TipoMoneda.PESOS)
                .setBalance(1000000)
                .setTipoCuenta(TipoCuenta.CUENTA_CORRIENTE);

        when(clienteDao.find(cliente.getDni(), true)).thenReturn(cliente);

        clienteService.agregarCuenta(cuentaCajaAhorro, cliente.getDni());
        clienteService.agregarCuenta(cuentaCuentaCorriente, cliente.getDni());

        verify(clienteDao, times(2)).save(cliente);

        assertEquals(2, cliente.getCuentas().size());
        assertTrue(cliente.tieneCuenta(TipoCuenta.CAJA_AHORRO, TipoMoneda.PESOS));
        assertTrue(cliente.tieneCuenta(TipoCuenta.CUENTA_CORRIENTE, TipoMoneda.PESOS));
        assertEquals(cliente, cuentaCajaAhorro.getTitular());
        assertEquals(cliente, cuentaCuentaCorriente.getTitular());
    }


    /**
     * Testeo que se pueden agregar dos cajas de ahorro a un mismo titular: una en pesos y una en dolares
     */
    @Test
    public void testAgregarCajaAhorroYCAUSS() throws TipoCuentaAlreadyExistsException {
       Cliente cliente = this.crearCliente();

        // Agregar una CA$
        Cuenta cuentaCajaAhorro = new Cuenta()
                .setMoneda(TipoMoneda.PESOS)
                .setBalance(700000)
                .setTipoCuenta(TipoCuenta.CAJA_AHORRO);

        // Agregar una CAU$
        Cuenta cuentaCajaAhorroDolares = new Cuenta()
                .setMoneda(TipoMoneda.DOLARES)
                .setBalance(500)
                .setTipoCuenta(TipoCuenta.CAJA_AHORRO);

        when(clienteDao.find(cliente.getDni(), true)).thenReturn(cliente);

        clienteService.agregarCuenta(cuentaCajaAhorro, cliente.getDni());
        clienteService.agregarCuenta(cuentaCajaAhorroDolares, cliente.getDni());

        verify(clienteDao, times(2)).save(cliente);

        assertEquals(2, cliente.getCuentas().size());
        assertTrue(cliente.tieneCuenta(TipoCuenta.CAJA_AHORRO, TipoMoneda.PESOS));
        assertTrue(cliente.tieneCuenta(TipoCuenta.CAJA_AHORRO, TipoMoneda.DOLARES));
        assertEquals(cliente, cuentaCajaAhorro.getTitular());
        assertEquals(cliente, cuentaCajaAhorroDolares.getTitular());
    }

    /**
     * Testeo el método buscarPorDni: caso de éxito
     */
    @Test
    public void testBuscarClienteExistente() {
        Cliente cliente = this.crearCliente();

        when(clienteDao.find(cliente.getDni(), true)).thenReturn(cliente);

        Cliente encontrado = clienteService.buscarClientePorDni(cliente.getDni());

        verify(clienteDao, times(1)).find(cliente.getDni(), true);
        assertEquals(cliente, encontrado);
    }

    /**
     * Testeo el método buscarPorDni: caso de falla
     */
    @Test
    public void testBuscarClienteNoExistente() {

        Cliente cliente= this.crearCliente();

        when(clienteDao.find(cliente.getDni(),true)).thenReturn(null);
        assertThrows(IllegalArgumentException.class,()-> clienteService.buscarClientePorDni(cliente.getDni()));
    }


    /**
     *Método para crear clientes que usaré en cada test
     */
    private Cliente crearCliente(){
        Cliente cliente = new Cliente();
        cliente.setFechaNacimiento(LocalDate.of(1980, 5, 10));
        cliente.setNombre("Carla");
        cliente.setApellido("Racciatti");
        cliente.setDni(40022659);
        cliente.setTipoPersona(TipoPersona.PERSONA_FISICA);
        return cliente;
    }

}
