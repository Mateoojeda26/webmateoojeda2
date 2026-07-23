package co.edu.iub.myfirtsproyect.service

import co.edu.iub.myfirtsproyect.exception.InvalidRequestException
import jakarta.annotation.PostConstruct
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import org.springframework.web.multipart.MultipartFile
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.nio.file.StandardCopyOption
import java.util.UUID

data class StoredImage(val bytes: ByteArray, val contentType: String)

@Service
class FileStorageService(
    @Value("\${app.storage.location:uploads}") storageLocation: String,
) {
    private val root: Path = Paths.get(storageLocation).toAbsolutePath().normalize()
    private val allowedTypes = mapOf(
        "image/jpeg" to "jpg",
        "image/png" to "png",
        "image/webp" to "webp",
    )

    @PostConstruct
    fun initialize() {
        Files.createDirectories(root)
    }

    fun storeImage(file: MultipartFile): String {
        if (file.isEmpty) throw InvalidRequestException("La imagen está vacía")
        val contentType = file.contentType?.lowercase()
        val extension = allowedTypes[contentType]
            ?: throw InvalidRequestException("Solo se permiten imágenes JPG, PNG o WebP")
        val fileName = "${UUID.randomUUID()}.$extension"
        val target = root.resolve(fileName).normalize()
        if (!target.startsWith(root)) throw InvalidRequestException("Nombre de archivo no válido")
        file.inputStream.use { input ->
            Files.copy(input, target, StandardCopyOption.REPLACE_EXISTING)
        }
        return "/uploads/$fileName"
    }

    fun delete(imageUrl: String?) {
        if (imageUrl == null || !imageUrl.startsWith("/uploads/")) return
        val fileName = imageUrl.removePrefix("/uploads/")
        if (fileName.contains('/') || fileName.contains('\\')) return
        val target = root.resolve(fileName).normalize()
        if (target.startsWith(root)) Files.deleteIfExists(target)
    }

    fun loadImage(imageUrl: String?): StoredImage {
        val fileName = imageUrl
            ?.takeIf { it.startsWith("/uploads/") }
            ?.removePrefix("/uploads/")
            ?.takeIf { it.isNotBlank() && !it.contains('/') && !it.contains('\\') }
            ?: throw co.edu.iub.myfirtsproyect.exception.ResourceNotFoundException("Imagen no encontrada")
        val target = root.resolve(fileName).normalize()
        if (!target.startsWith(root) || !Files.isRegularFile(target)) {
            throw co.edu.iub.myfirtsproyect.exception.ResourceNotFoundException("Imagen no encontrada")
        }
        val contentType = when (fileName.substringAfterLast('.', "").lowercase()) {
            "jpg", "jpeg" -> "image/jpeg"
            "png" -> "image/png"
            "webp" -> "image/webp"
            else -> throw co.edu.iub.myfirtsproyect.exception.ResourceNotFoundException("Imagen no encontrada")
        }
        return StoredImage(Files.readAllBytes(target), contentType)
    }
}
