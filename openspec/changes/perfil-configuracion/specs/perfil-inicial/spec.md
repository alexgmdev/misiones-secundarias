# Especificación perfil-inicial (HU-005)

## Purpose
Configuración obligatoria del perfil público la primera vez, y gate que impide usar la app hasta completarla.

## Supuestos
- Campos nuevos en `profiles`; `profile_completed` es verdadero cuando nickname, género, avatar y foto no son nulos (la descripción es opcional).
- Nickname: 3–20 caracteres `[A-Za-z0-9_]`, único sin distinguir mayúsculas.
- Género: `femenino`, `masculino`, `prefiero_no_decir` con etiquetas "Femenino", "Masculino", "Prefiero no decir".
- Avatar: catálogo predefinido dibujado con Canvas, guardado como `avatar_id`.
- Foto: el selector acepta JPG/PNG; la app comprime a JPEG ≤ 2 MB; si no es posible, error.
- Usuarios existentes quedan incompletos y pasan por esta pantalla (esperado).

## Requirements

### Requirement: Campos obligatorios
Nickname, género, avatar y foto MUST ser obligatorios; la descripción MAY omitirse.

#### Scenario: Faltan obligatorios
- GIVEN pantalla "Crea tu perfil" sin datos
- WHEN presiona "Guardar Perfil"
- THEN muestra "Este campo es obligatorio" en nickname, "Selecciona tu género", "Elige un avatar" y "Agrega una foto de perfil", y no llama al backend

#### Scenario: Sin descripción
- GIVEN nickname, género, avatar y foto válidos y descripción vacía
- WHEN presiona "Guardar Perfil"
- THEN guarda el perfil sin error

### Requirement: Validación de nickname
El nickname MUST tener entre 3 y 20 caracteres y solo letras, números y guion bajo.

#### Scenario: Muy corto o largo
- GIVEN nickname "ab" (o de 21 caracteres)
- WHEN presiona "Guardar Perfil"
- THEN muestra "El nickname debe tener entre 3 y 20 caracteres"

#### Scenario: Caracteres inválidos
- GIVEN nickname "Aventurero Cali!"
- WHEN presiona "Guardar Perfil"
- THEN muestra "El nickname solo admite letras, números y guion bajo"

### Requirement: Nickname único
El nickname MUST ser único sin distinguir mayúsculas y minúsculas.

#### Scenario: En uso
- GIVEN existe "AventureroCali"
- WHEN otro usuario ingresa "aventurerocali" y guarda
- THEN muestra "El nickname ya está en uso" y no guarda el perfil

### Requirement: Descripción
La descripción MUST tener como máximo 280 caracteres.

#### Scenario: Excede el límite
- GIVEN descripción de 281 caracteres
- WHEN presiona "Guardar Perfil"
- THEN muestra "Máximo 280 caracteres"

### Requirement: Selección de género
El sistema MUST ofrecer exactamente "Femenino", "Masculino" y "Prefiero no decir".

#### Scenario: Opciones disponibles
- GIVEN pantalla de perfil inicial
- WHEN el usuario revisa el selector de género
- THEN solo existen esas tres opciones y se guarda `femenino`, `masculino` o `prefiero_no_decir`

### Requirement: Foto de perfil
El selector MUST aceptar JPG y PNG; la app MUST comprimir a JPEG de máximo 2 MB.

#### Scenario: Foto grande aceptada
- GIVEN foto PNG de 5 MB comprimible a menos de 2 MB
- WHEN la selecciona
- THEN se acepta y se sube como JPEG ≤ 2 MB

#### Scenario: Formato no soportado
- GIVEN archivo GIF o WEBP
- WHEN lo selecciona
- THEN muestra "Formato no soportado. Usa JPG o PNG"

#### Scenario: Imposible reducir
- GIVEN imagen que no baja de 2 MB tras comprimir
- WHEN la selecciona
- THEN muestra "La foto supera 2 MB"

### Requirement: Guardado exitoso
Con datos válidos el sistema MUST subir la foto, guardar el perfil y continuar al onboarding.

#### Scenario: Perfil completo
- GIVEN nickname "AventureroCali", género, descripción, avatar y foto válidos
- WHEN presiona "Guardar Perfil"
- THEN guarda el perfil, `profile_completed` pasa a verdadero y navega a `onboarding`

#### Scenario: Error al guardar
- GIVEN el backend falla
- WHEN presiona "Guardar Perfil"
- THEN muestra "Ocurrió un error. Intenta de nuevo" y conserva lo escrito

#### Scenario: Sin conexión al guardar
- GIVEN dispositivo sin red
- WHEN presiona "Guardar Perfil"
- THEN muestra "Sin conexión. Intenta de nuevo"

### Requirement: Gate de perfil completo
Tras login, registro y arranque en frío con sesión, el sistema MUST dirigir a "Crea tu perfil" si el perfil está incompleto, y al menú principal si está completo.

#### Scenario: Login con perfil incompleto
- GIVEN usuario con `profile_completed` falso
- WHEN inicia sesión
- THEN navega a "Crea tu perfil" y no al menú

#### Scenario: Registro nuevo
- GIVEN usuario recién registrado
- WHEN termina el registro
- THEN navega a "Crea tu perfil"

#### Scenario: Arranque en frío incompleto
- GIVEN sesión persistida con perfil incompleto
- WHEN abre la app
- THEN navega a "Crea tu perfil"

#### Scenario: Perfil completo
- GIVEN usuario con perfil completo
- WHEN inicia sesión o abre la app
- THEN navega al menú principal

### Requirement: Sin salida del gate
Mientras el perfil esté incompleto, el botón Atrás MUST NOT llevar al menú principal.

#### Scenario: Atrás en perfil inicial
- GIVEN usuario en "Crea tu perfil" con perfil incompleto
- WHEN presiona Atrás
- THEN no navega al menú principal

### Requirement: Gate sin conexión
Si no se puede consultar el estado del perfil por falta de red, el sistema MUST dejar entrar al menú principal sin bloquear.

#### Scenario: Offline en el gate
- GIVEN sesión activa y sin red
- WHEN el gate consulta `profile_completed` y falla con error de red
- THEN navega al menú principal
