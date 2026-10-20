package com.example.caso1.data.local

import android.content.Context
import android.net.Uri
import androidx.core.content.FileProvider
import java.io.File

/**
 * Fotos de evidencia de las acciones correctivas (rúbrica EP3: recurso nativo cámara).
 *
 * Se usa la app de cámara del sistema con `ActivityResultContracts.TakePicture`: la foto se
 * escribe directamente en un archivo de la app y no se necesita CameraX ni una dependencia nueva.
 * La app de cámara recibe una URI de [FileProvider] (ver res/xml/file_paths.xml).
 */

/** Carpeta dentro de files/ declarada en file_paths.xml. */
private const val CARPETA = "evidencias"

/**
 * Crea un archivo nuevo para la foto. Las fotos anteriores no se borran: son evidencia y
 * ocupan poco, así que no hace falta limpiarlas al registrar la acción.
 */
fun crearArchivoEvidencia(context: Context): File {
    val carpeta = File(context.filesDir, CARPETA).apply { mkdirs() }
    return File(carpeta, "accion-${System.currentTimeMillis()}.jpg")
}

/** URI que la cámara del sistema puede escribir, con permiso temporal para ese archivo. */
fun uriEvidencia(context: Context, archivo: File): Uri =
    FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", archivo)
