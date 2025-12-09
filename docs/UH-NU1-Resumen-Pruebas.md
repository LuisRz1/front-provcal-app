# Resumen de Implementación de Pruebas UH-NU1

## Descripción General
Este documento resume la implementación de pruebas unitarias para la Historia de Usuario UH-NU1 (Gestión de Menú Mensual). La suite de pruebas cubre los componentes MenuRepository y MenuViewModel con 30 pruebas completas.

## Configuración de Infraestructura de Pruebas

### Dependencias Agregadas
- **MockK**: 1.13.8 - Biblioteca de mocking diseñada para Kotlin
- **Coroutines Test**: Para probar funciones suspendidas
- **Architecture Core Testing**: 2.2.0 - Para probar LiveData (InstantTaskExecutorRule)

### Utilidades de Prueba Creadas

#### 1. LiveDataTestUtil.kt
- **Propósito**: Observación síncrona de LiveData para pruebas unitarias
- **Función Principal**: `getOrAwaitValue()` - Espera el valor de LiveData con timeout
- **Uso**: Esencial para probar ViewModels con LiveData

#### 2. MainCoroutineRule.kt
- **Propósito**: Reemplaza el dispatcher Main con TestDispatcher para pruebas de corrutinas
- **Implementación**: Regla JUnit @Rule que configura/resetea Dispatchers.Main
- **Uso**: Requerido para todas las pruebas de ViewModel que usan viewModelScope

#### 3. MenuTestData.kt
- **Propósito**: Fixtures de prueba y datos mock centralizados
- **Contenido**:
  - Días de menú mock (regulares y festivos)
  - Respuestas GraphQL mock (carga, proponer cambios)
  - Datos de entrada mock (ejemplos de MenuChangeItemInput)
  - Constantes de prueba (año, mes, nombres de archivo)

## Cambios en Código de Producción

### Refactorización de MenuRepository.kt
**Cambio**: Se agregó parámetro opcional `apolloClient` en el constructor para inyección de dependencias

```kotlin
class MenuRepository(
    context: Context? = null,
    apolloClient: ApolloClient? = null
) {
    private val apolloClient: ApolloClient = apolloClient ?: ApolloClientProvider.getInstance(context!!)
```

**Razones**:
- Habilita inyección por constructor para pruebas
- Mantiene compatibilidad hacia atrás (código existente sin cambios)
- Sigue las mejores prácticas de inyección de dependencias
- Permite mockear el cliente Apollo sin mockear el singleton

## Cobertura de Pruebas

### MenuRepositoryTest (4 pruebas)
Las pruebas se enfocan en **manejo de excepciones** debido a limitaciones de MockK con la clase ApolloResponse de Apollo GraphQL.

✅ **Pruebas Aprobadas**:
1. `getMonthlyMenu returns Error on network exception` - Retorna error en excepción de red
2. `getMonthlyMenu handles generic exceptions` - Maneja excepciones genéricas
3. `uploadMonthlyMenu returns Error on network exception` - Error de red en carga
4. `proposeMenuChange returns Error on network exception` - Error de red al proponer cambios

**Nota**: Las pruebas de casos exitosos para el repositorio son limitadas debido a la incapacidad de MockK para mockear propiedades de ApolloResponse. Ver sección "Limitaciones Conocidas" más abajo.

### MenuViewModelTest (26 pruebas)
Pruebas completas de toda la funcionalidad del ViewModel usando repositorio mockeado.

#### Pruebas de loadMenu() (6 pruebas)
✅ Todas aprobadas:
- Camino exitoso: actualiza LiveData de menú, establece mensaje a null, establece loading a false
- Camino de error: actualiza LiveData de mensaje, no actualiza menú, establece loading a false
- Manejo de menú vacío

#### Pruebas de uploadMenu() (9 pruebas)
✅ Todas aprobadas:
- Flujo exitoso con actualización vía loadMenu
- Detección de conflictos e invocación de callback
- Manejo de errores sin conflicto
- Gestión de estado de carga

#### Pruebas de proposeChanges() (11 pruebas)
✅ Todas aprobadas:
- Éxito con conteo correcto
- Invocación de callback onDone
- Manejo de cero cambios
- Manejo de errores
- Gestión de estado de carga

## Limitaciones Conocidas

### Problema de Compatibilidad MockK + Apollo GraphQL
**Problema**: MockK no puede mockear correctamente las propiedades de la clase `ApolloResponse` de Apollo (específicamente `.data`).

## Ejecución de Pruebas

### Ejecutar Pruebas
```bash
# Ejecutar todas las pruebas relacionadas con menú
./gradlew testDebugUnitTest --tests "*Menu*"

# Ejecutar clase de prueba específica
./gradlew testDebugUnitTest --tests "MenuRepositoryTest"
./gradlew testDebugUnitTest --tests "MenuViewModelTest"

# Ejecutar todas las pruebas unitarias
./gradlew testDebugUnitTest
```

### Estado Actual
✅ **30/30 pruebas aprobadas** (100% tasa de aprobación)
- MenuRepositoryTest: 4/4 aprobadas
- MenuViewModelTest: 26/26 aprobadas

**Historia de Usuario**: UH-NU1 (Gestión de Menú Mensual)
**Framework de Pruebas**: JUnit 4 + MockK + Kotlin Coroutines Test
