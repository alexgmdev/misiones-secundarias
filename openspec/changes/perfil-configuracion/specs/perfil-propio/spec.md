# Especificación perfil-propio (HU-006, HU-008, HU-009)

## Purpose
Consultar y editar el propio perfil, incluida la foto.

## Supuestos
- Nivel fijo "Nivel 1" y sin marco (no hay modelo de gamificación).
- Reglas de nickname, descripción, género y foto idénticas a `perfil-inicial`.
- Menú: PROFILE abre Mi perfil; AVATAR abre Editar perfil; HISTORY abre `historial-misiones`.

## Requirements

### Requirement: Consulta de Mi perfil
Mi perfil MUST mostrar foto, avatar, nickname, "Nivel 1", género y descripción coincidentes con los datos guardados, y los botones "Editar Perfil" e "Histórico de Misiones".

#### Scenario: Visualización
- GIVEN usuario con nickname "AventureroCali" y género "Femenino"
- WHEN selecciona "Mi Perfil"
- THEN muestra foto, avatar, "AventureroCali", "Nivel 1", "Femenino" y su descripción

#### Scenario: Sin descripción
- GIVEN usuario sin descripción
- WHEN abre Mi perfil
- THEN la tarjeta de descripción no muestra texto inventado y la pantalla no falla

#### Scenario: Error de carga
- GIVEN falla la consulta
- WHEN abre Mi perfil
- THEN muestra "Ocurrió un error. Intenta de nuevo"

### Requirement: Edición de datos
"Editar Perfil" MUST permitir modificar nickname, género, descripción y avatar, con las mismas validaciones del perfil inicial.

#### Scenario: Edición exitosa
- GIVEN usuario en Editar Perfil
- WHEN cambia nickname a "ExplorerCali", actualiza la descripción y presiona "Guardar Cambios"
- THEN guarda y vuelve a Mi perfil mostrando los datos actualizados

#### Scenario: Campo obligatorio vacío
- GIVEN usuario en Editar Perfil
- WHEN borra el nickname y presiona "Guardar Cambios"
- THEN muestra "Este campo es obligatorio" y no guarda

#### Scenario: Nickname propio
- GIVEN usuario con nickname "AventureroCali"
- WHEN guarda sin cambiarlo (o solo cambiando mayúsculas)
- THEN no se muestra "El nickname ya está en uso"

#### Scenario: Nickname de otro
- GIVEN existe "ExplorerCali" de otro usuario
- WHEN ingresa "explorercali" y guarda
- THEN muestra "El nickname ya está en uso"

### Requirement: Reflejo inmediato
Los cambios guardados MUST verse de inmediato en Mi perfil y en el saludo del menú principal.

#### Scenario: Saludo actualizado
- GIVEN usuario que cambió su nickname
- WHEN vuelve al menú principal
- THEN el saludo muestra el nuevo nickname

### Requirement: Cambio de foto
Editar Perfil MUST permitir elegir una nueva foto JPG o PNG, comprimida a JPEG ≤ 2 MB, que se actualiza al guardar.

#### Scenario: Foto nueva
- GIVEN usuario editando su perfil
- WHEN elige una imagen válida y presiona "Guardar Cambios"
- THEN su foto pública se actualiza y se ve en Mi perfil

#### Scenario: Formato no soportado
- GIVEN archivo no JPG/PNG
- WHEN lo selecciona
- THEN muestra "Formato no soportado. Usa JPG o PNG" y conserva la foto actual

#### Scenario: Foto demasiado grande
- GIVEN imagen irreducible a 2 MB
- WHEN la selecciona
- THEN muestra "La foto supera 2 MB" y conserva la foto actual

#### Scenario: Sin conexión
- GIVEN dispositivo sin red
- WHEN presiona "Guardar Cambios"
- THEN muestra "Sin conexión. Intenta de nuevo" y conserva lo editado
