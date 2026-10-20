package com.example.caso1

import com.example.caso1.data.local.GalponEntity
import com.example.caso1.data.model.EstadoGalpon
import com.example.caso1.data.model.Rol
import com.example.caso1.viewmodel.RegistroAccionViewModel
import com.example.caso1.viewmodel.TipoAccion
import kotlinx.coroutines.flow.MutableStateFlow
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
    private val rol = MutableStateFlow<Rol?>(Rol.OPERARIO)
    // by lazy: JUnit inicializa los campos ANTES de aplicar la regla; si el ViewModel se creara
    // aquí mismo, viewModelScope pediría Dispatchers.Main cuando todavía no está reemplazado.
    private val vm by lazy { RegistroAccionViewModel(repo, rol, reloj = { horaFija }) }

    @Test fun registrarConDatosValidosGuardaYLimpiaElFormulario() = runTest {
        vm.onGalponChange(4)
        vm.onTipoChange(TipoAccion.VENTILACION)
        vm.onDescripcionChange("  Encendí los ventiladores  ")

        vm.registrar()

        // Se guardó una sola acción, con el código del tipo, la descripción sin espacios y el rol de la sesión
        assertEquals(
            listOf(RepositorioFalso.AccionGuardada(4, "VENTILACION", "Encendí los ventiladores", "OPERARIO")),
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

    // El ViewModel vive en la entrada HOME de la pila: si antes se eligió un galpón y se abandonó
    // el formulario, al entrar desde "Registrar acción correctiva" debe mandar el galpón de la alerta.
    @Test fun preseleccionarPisaLaEleccionAnterior() = runTest {
        vm.onGalponChange(1)
        vm.preseleccionar(4)
        assertEquals(4, vm.state.value.galponId)
    }

    @Test fun preseleccionarTambienBorraElErrorDelGalpon() = runTest {
        vm.registrar()   // sin galpón: queda el error
        vm.preseleccionar(4)
        assertNull(vm.state.value.errores.galpon)
    }

    @Test fun sinSesionLaAccionSeGuardaSinUsuario() = runTest {
        rol.value = null
        vm.onGalponChange(4)
        vm.onTipoChange(TipoAccion.VENTILACION)
        vm.onDescripcionChange("Encendí los ventiladores")

        vm.registrar()

        assertNull(repo.accionesGuardadas.single().usuario)
    }

    // Foto de evidencia (rúbrica EP3: cámara). Es opcional y nunca bloquea el guardado.

    @Test fun laFotoElegidaSeGuardaConLaAccionYSeLimpiaAlRegistrar() = runTest {
        vm.onGalponChange(4)
        vm.onTipoChange(TipoAccion.VENTILACION)
        vm.onDescripcionChange("Encendí los ventiladores")
        vm.onFotoChange("/files/evidencias/accion-1.jpg")

        vm.registrar()

        assertEquals(
            listOf(RepositorioFalso.AccionGuardada(4, "VENTILACION", "Encendí los ventiladores", "OPERARIO", "/files/evidencias/accion-1.jpg")),
            repo.accionesGuardadas
        )
        // El formulario queda limpio para la próxima acción
        assertNull(vm.state.value.fotoRuta)
    }

    @Test fun onFotoChangeLimpiaElAvisoDePermisoDenegado() = runTest {
        vm.permisoCamaraDenegado()
        assertNotNull(vm.state.value.mensajeFoto)

        vm.onFotoChange("/files/evidencias/accion-2.jpg")

        assertEquals("/files/evidencias/accion-2.jpg", vm.state.value.fotoRuta)
        assertNull(vm.state.value.mensajeFoto)
    }

    @Test fun sinPermisoDeCamaraSeAvisaQueLaFotoEsOpcionalYLaAccionSePuedeGuardar() = runTest {
        vm.permisoCamaraDenegado()

        // El aviso explica que la foto es opcional; el formulario sigue funcionando sin foto
        assertEquals(R.string.registro_foto_permiso_denegado, vm.state.value.mensajeFoto)
        vm.onGalponChange(4)
        vm.onTipoChange(TipoAccion.VENTILACION)
        vm.onDescripcionChange("Encendí los ventiladores")
        vm.registrar()

        assertEquals(1, repo.accionesGuardadas.size)
        assertNull(repo.accionesGuardadas.single().fotoRuta)
    }
}
