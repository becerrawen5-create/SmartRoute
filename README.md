# SmartRoute AI (Android · Kotlin · Jetpack Compose · Material 3)

App que guarda rutas favoritas y las adapta con IA según historial, clima, tráfico y datos IoT.

## Cómo abrirlo
1. Descomprime el zip.
2. Android Studio → File > Open → selecciona la carpeta `SmartRoute`.
3. Espera el Gradle Sync (necesita internet la primera vez) y ejecuta `app`.
   Requisitos: Android Studio Koala o superior, JDK 17, minSdk 26.

## Pantallas (según el diseño)
Splash · Inicio de sesión · Principal · Mapa y rutas · Detalle de ruta · Rutas favoritas ·
Crear ruta · Historial · Clima · Tráfico · Perfil / Configuración.

## Estructura
- `data/` modelos y datos de ejemplo (rutas, historial, clima, tráfico, fuentes IoT)
- `ai/RecommendationEngine.kt` motor de recomendación (reglas: tráfico, lluvia, bici/caminata, preferencia)
- `SmartRouteViewModel.kt` estado de la app (favoritos, ruta seleccionada, aprendizaje)
- `navigation/AppNav.kt` NavHost + barra inferior
- `ui/screens/` las 11 pantallas · `ui/components/` mapa (Canvas), tarjetas, encabezados · `ui/theme/` tema Material 3

## Notas
- El inicio de sesión es de demostración: acepta cualquier correo y contraseña no vacíos.
- El mapa es un dibujo con Canvas (sin API key). Para uno real: Google Maps Compose u OSMDroid.
- Los datos son de ejemplo; la IA es una versión por reglas lista para sustituir por un modelo o API.
