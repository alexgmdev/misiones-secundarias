# Especificación selección de modalidad y creación de borrador (HU-015, HU-016)

## Purpose

Permitir al publicador seleccionar modalidad antes del formulario, mostrar los campos físicos solo para misiones de campo y crear un borrador listo para publicar.

## Supuestos

- El usuario autenticado tiene rol de publicador y Supabase expone `auth.uid()` (la política RLS `Publishers can insert missions` lo valida).
- HU-015 fue aprobada para el MVP; su alcance es la selección previa de modalidad y la presentación condicional de los campos de ubicación definidos en SCRUM-38.
- El formulario solo crea **borradores**; publicar es responsabilidad de `publicar-mision`.

## Requirements

### Requirement: Selección de modalidad
Al iniciar la publicación, el sistema MUST ofrecer elegir entre "Misión de Campo" y "Misión Remota" antes de mostrar el formulario.

#### Scenario: Selección de misión de campo
- GIVEN el publicador presiona "Publicar misión"
- WHEN selecciona "Misión de Campo"
- THEN se muestra el formulario con los campos de punto de encuentro y ubicación
- AND la modalidad enviada a la base es `campo`

#### Scenario: Selección de misión remota
- GIVEN el publicador presiona "Publicar misión"
- WHEN selecciona "Misión Remota"
- THEN se ocultan los campos de ubicación física
- AND la modalidad enviada a la base es `remota`

#### Scenario: Cambiar la modalidad elegida
- GIVEN el publicador está en el formulario de una misión
- WHEN elige "Cambiar modalidad"
- THEN vuelve al selector de modalidad antes de guardar

### Requirement: Campos obligatorios
El sistema MUST exigir título, descripción, requisitos y punto de encuentro (solo en modalidad de campo) antes de guardar el borrador.

#### Scenario: Guardar con campos completos
- GIVEN el publicador está en el formulario de misión de campo
- WHEN completa título "Siembra en Parque Dog", descripción, requisitos "Guantes" y punto de encuentro "Parque Dog Cali"
- THEN el sistema habilita el guardado y persiste el borrador

#### Scenario: Título vacío
- GIVEN el formulario sin título
- WHEN presiona "Guardar borrador"
- THEN muestra "Este campo es obligatorio" bajo el título
- AND no llama al backend

#### Scenario: Descripción vacía
- GIVEN el formulario con título pero sin descripción
- WHEN presiona "Guardar borrador"
- THEN muestra el error de obligatoriedad en descripción
- AND no llama al backend

#### Scenario: Requisitos vacíos
- GIVEN el formulario sin requisitos
- WHEN presiona "Guardar borrador"
- THEN muestra el error de obligatoriedad en requisitos
- AND no llama al backend

### Requirement: Ubicación en Cali
Para modalidad de campo, el sistema MUST exigir el texto del punto de encuentro y coordenadas de latitud/longitud. El punto debe corresponder a Cali. Las coordenadas se guardan en `location GEOGRAPHY(POINT)` y el texto en `meeting_point`.

#### Scenario: Punto de encuentro fuera de Cali
- GIVEN modalidad de campo
- WHEN ingresa "Parque Principal, Bogotá"
- THEN muestra "La ubicación debe estar en Cali"
- AND no guarda el borrador

#### Scenario: Punto de encuentro válido
- GIVEN modalidad de campo
- WHEN ingresa "Parque Dog, Cali"
- THEN la validación de ubicación pasa

#### Scenario: Coordenadas ausentes
- GIVEN modalidad de campo
- WHEN no ingresa latitud o longitud
- THEN muestra "Este campo es obligatorio" bajo la coordenada faltante
- AND no guarda el borrador

#### Scenario: Coordenadas fuera del área aproximada de Cali
- GIVEN modalidad de campo
- WHEN ingresa coordenadas fuera del rectángulo aproximado de Cali
- THEN muestra "La ubicación debe estar en Cali"
- AND no guarda el borrador

#### Scenario: Persistencia de la ubicación física
- GIVEN el publicador ingresa punto de encuentro "Parque Dog Cali" y coordenadas válidas
- WHEN guarda el borrador
- THEN `meeting_point` recibe el texto del punto
- AND `location` recibe `SRID=4326;POINT(longitud latitud)`

### Requirement: Categoría opcional
La categoría MUST ser opcional y, si se envía vacía, MUST guardarse como `null`.

#### Scenario: Sin categoría
- GIVEN el formulario con todos los obligatorios y sin categoría
- WHEN guarda el borrador
- THEN la misión se persiste con `category = null`

### Requirement: Cupos
El sistema MUST exigir un número de cupos mayor o igual a 2 (valor por defecto de la base).

#### Scenario: Cupos inválidos
- GIVEN el publicador ingresa 1 cupo
- WHEN intenta guardar
- THEN muestra "Debe haber al menos 2 cupos"
- AND no llama al backend

### Requirement: Persistencia como borrador
Al guardar, el sistema MUST insertar la misión con `status = 'Borrador'` y `publisher_id` igual al usuario autenticado, y MUST NOT mostrarla en la búsqueda pública.

#### Scenario: Borrador guardado
- GIVEN un formulario válido
- WHEN presiona "Guardar borrador"
- THEN se inserta la misión con estado `Borrador`
- AND la pantalla confirma el guardado y permite publicar

#### Scenario: Borrador no visible
- GIVEN una misión recién guardada como `Borrador`
- WHEN otro usuario abre "Buscar misiones"
- THEN la misión no aparece en la lista pública

### Requirement: Estado de carga
El botón de guardado MUST deshabilitarse mientras la solicitud esté en curso.

#### Scenario: Doble toque
- GIVEN un guardado en curso
- WHEN presiona "Guardar borrador" de nuevo
- THEN no se envía otra solicitud

### Requirement: Errores de red y servidor
El sistema MUST distinguir un fallo de conectividad de un error de validación.

#### Scenario: Sin conexión
- GIVEN no hay red
- WHEN intenta guardar un borrador válido
- THEN muestra "Sin conexión. Intenta de nuevo"
- AND conserva los datos del formulario

#### Scenario: Error del servidor
- GIVEN Supabase responde 5xx
- WHEN intenta guardar
- THEN muestra "Ocurrió un error. Intenta de nuevo"
- AND conserva los datos del formulario
