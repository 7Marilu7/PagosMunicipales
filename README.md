# PagosMunicipales

Aplicación móvil Android para la consulta y gestión de pagos municipales.

## Requisitos

- Android Studio
- JDK 17
- Android SDK 35

## Configuración de Firebase

El archivo `app/google-services.json` se excluye del repositorio porque contiene la configuración del proyecto Firebase original. Las funciones que usan Firebase Authentication y Cloud Firestore requieren configurar un proyecto Firebase propio y colocar su archivo `google-services.json` en `app/`.

No se incluyen credenciales, configuraciones privadas ni archivos generados por Android Studio/Gradle.
