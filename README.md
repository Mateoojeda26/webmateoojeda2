# Taskora Pet

Aplicación web para organizar el cuidado de una o varias mascotas. Permite administrar usuarios, mascotas, cuidadores, tareas, programaciones recurrentes, canales de notificación, recordatorios, evidencias, historial y reportes.

## Tecnologías

- Kotlin y Java 21
- Spring Boot
- Spring Security y JWT
- Spring Data JPA
- SQLite
- HTML, CSS y JavaScript
- Telegram Bot API
- Gmail API mediante OAuth 2.0

## Requisitos funcionales

1. Gestión de usuarios.
2. Autenticación y seguridad de acceso.
3. Gestión de mascotas.
4. Gestión de cuidadores y permisos.
5. Gestión de tareas de cuidado.
6. Gestión de programaciones recurrentes.
7. Gestión de canales de notificación.
8. Procesamiento y envío de recordatorios.
9. Seguimiento, evidencias e historial de cuidados.
10. Agenda, panel y reportes básicos.

## Ejecutar el proyecto

Se necesita JDK 21. En Windows:

```powershell
.\gradlew.bat bootRun
```

Después, abrir:

- Aplicación: <http://localhost:8080>
- Swagger UI: <http://localhost:8080/swagger-ui/index.html>

## Pruebas

```powershell
.\gradlew.bat test
```

## Configuración privada

Las credenciales no se guardan en el repositorio. El archivo `.env.example` contiene únicamente la lista de variables necesarias y valores de ejemplo.

Para habilitar Telegram, Gmail, la cuenta administradora o los usuarios de demostración, configura las variables correspondientes en IntelliJ IDEA o en el entorno desde el que ejecutes la aplicación.

No se deben publicar:

- Tokens de Telegram.
- Secretos OAuth de Gmail.
- Contraseñas.
- La clave JWT.
- La base de datos local.
- Las imágenes privadas cargadas por los usuarios.

## Autor

Mateo José Ojeda Martínez

Universidad de Barranquilla

7_GSIN_G2 — Viernes
