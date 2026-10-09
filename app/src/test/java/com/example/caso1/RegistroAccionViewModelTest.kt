package com.example.caso1

import com.example.caso1.data.db.GalponEntity
import com.example.caso1.data.model.EstadoGalpon
import com.example.caso1.viewmodel.RegistroAccionViewModel
import com.example.caso1.viewmodel.TipoAccion
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class RegistroAccionViewModelTest {

    @get:Rule val reglaMain = ReglaDispatcherPrincipal()

    private val galpones = listOf(
        GalponEntity(1, "Granja Norte", "Galpón 1", EstadoGalpon.NORMAL),
        GalponEntity(4, "Granja Sur", "Galpón 4", EstadoGalpon.CRITICO)
    )
    private val repo = RepositorioFalso(galpones)
    private val horaFija = 1_700_000_000_000L
    // by lazy: JUnit inicializa los campos ANTES de aplicar la regla; si el ViewModel se creara
    // aquí mismo, viewModelScope pediría Dispatchers.Main cuando todavía no está reemplazado.
    private val vm by lazy { RegistroAccionViewModel(repo, reloj = { horaFija }) }

    @Test fun registrarConDatosValidosGuardaYLimpiaElFormulario() = runTest {
        vm.onGalponChange(4)
        vm.onTipoChange(TipoAccion.VENTILACION)
        vm.onDescripcionChange("  Encendí los ventiladores  ")

        vm.registrar()

        // Se guardó una sola acción, con el código del tipo y la descripción sin espacios
        assertEquals(
            listOf(RepositorioFalso.AccionGuardada(4, "VENTILACION", "Encendí los ventiladores")),
            repo.accionesGuardadas
        )
        val estado = vm.state.value
        assertTrue(estado.registrado)
        // El formulario queda vacío: "atrás" no permite guardar lo mismo otra vez
        assertNull(estado.galponId)
        assertNull(estado.tipoAccion)
        assertEquals("", estado.descripcion)
        // La confirmación tiene lo que se guardó
        val registro = estado.ultimoRegistro
        assertNotNull(registro)
        assertEquals("Granja Sur", registro!!.granja)
        assertEquals("Galpón 4", registro.galpon)
        assertEquals(TipoAccion.VENTILACION, registro.tipo)
        assertEquals(horaFija, registro.fechaHora)
    }

    @Test fun registrarConErroresNoGuardaNada() = runTest {
        vm.onGalponChange(99)          // no existe
        vm.onDescripcionChange("abc")  // muy corta; tampoco se eligió tipo

        vm.registrar()

        assertTrue(repo.accionesGuardadas.isEmpty())
        val estado = vm.state.value
        assertFalse(estado.registrado)
        assertFalse(estado.guardando)
        assertEquals(R.string.registro_error_galpon, estado.errores.galpon)
        assertEquals(R.string.registro_error_tipo, estado.errores.tipo)
        assertEquals(R.string.registro_error_descripcion, estado.errores.descripcion)
        // Lo escrito se conserva para que el usuario lo corrija
        assertEquals("abc", estado.descripcion)
    }

    @Test fun corregirUnCampoBorraSoloSuError() = runTest {
        vm.registrar()   // todo vacío: tres errores
        vm.onTipoChange(TipoAccion.OTRO)

        val errores = vm.state.value.errores
        assertNull(errores.tipo)
        assertNotNull(errores.galpon)
        assertNotNull(errores.descripcion)
    }

    @Test fun preseleccionarNoPisaUnaEleccionDelUsuario() = runTest {
        vm.onGalponChange(1)
        vm.preseleccionar(4)
        assertEquals(1, vm.state.value.galponId)
    }
}
