# PagosMunicipales

Aplicación móvil para la consulta y gestión de pagos municipales, desarrollada con Kotlin, Android Studio y Firebase.

## Descripción

Pagos Municipales es una aplicación móvil diseñada para facilitar la consulta y gestión de pagos municipales. Permite a los usuarios consultar adeudos de impuesto predial, multas y licencias, así como administrar su perfil y consultar su historial de pagos.

La aplicación integra Firebase Authentication para la autenticación de usuarios y Cloud Firestore para almacenar información y movimientos de pago. Además, permite generar comprobantes de pago en formato PDF.

## Funcionalidades

- Registro de usuarios e inicio de sesión.
- Recuperación de contraseña.
- Consulta y actualización del perfil de usuario.
- Cambio de contraseña.
- Consulta de adeudos de impuesto predial.
- Consulta de multas.
- Consulta de licencias.
- Selección de métodos de pago en efectivo o tarjeta.
- Registro de movimientos de pago en Cloud Firestore.
- Consulta del historial de pagos.
- Visualización del detalle de las transacciones.
- Generación y guardado de comprobantes en formato PDF.

## Tecnologías utilizadas

- **Kotlin**
- **Android Studio**
- **XML**
- **AndroidX**
- **Material Components**
- **ConstraintLayout**
- **CardView**
- **Firebase Authentication**
- **Cloud Firestore**
- **Gradle Kotlin DSL**
- **Git y GitHub**

## Estructura del proyecto

```text
PagosMunicipales/
├── app/
│   ├── src/
│   │   ├── main/
│   │   │   ├── java/
│   │   │   │   └── com/example/pagosmunicipales/
│   │   │   │       ├── model/
│   │   │   ├── res/
│   │   │   └── AndroidManifest.xml
│   │   └── test/
│   └── build.gradle.kts
├── gradle/
│   └── wrapper/
├── .gitignore
├── build.gradle.kts
├── gradle.properties
├── gradlew
├── gradlew.bat
├── settings.gradle.kts
└── README.md
```

## Instalación local

1. Clonar el repositorio:

   ```bash
   git clone https://github.com/7Marilu7/PagosMunicipales.git
   ```

2. Abrir el proyecto desde Android Studio.
3. Esperar a que Gradle sincronice las dependencias.
4. Configurar un proyecto en Firebase Console.
5. Registrar la aplicación Android con el identificador de paquete `com.example.pagosmunicipales`.
6. Habilitar Firebase Authentication y Cloud Firestore.
7. Descargar el archivo `google-services.json` y colocarlo dentro de la carpeta `app/`.
8. Ejecutar la aplicación en un emulador o dispositivo Android.

## Base de datos

El proyecto utiliza **Firebase Authentication** para gestionar el registro e inicio de sesión de los usuarios, y **Cloud Firestore** para almacenar información de usuarios y movimientos de pago.

El archivo `google-services.json` no se incluye en el repositorio por motivos de seguridad. Para utilizar las funcionalidades de Firebase, es necesario configurar un proyecto propio en Firebase Console y agregar el archivo de configuración correspondiente dentro de la carpeta `app/`.

## Estado del proyecto

Proyecto desarrollado durante el periodo de prácticas profesionales como parte de la formación en Ingeniería en Desarrollo y Gestión de Software.

## Autoría

Desarrollado por **María Guadalupe Cruz Paz**.

[GitHub](https://github.com/7Marilu7)
