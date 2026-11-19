# Especificación perfil-publico (HU-007)

## Purpose
Ficha pública de solo lectura de otro usuario, sin datos privados.

## Supuestos
- Se abre desde la tarjeta de postulante (foto o nombre) y desde la tarjeta del publicador en el detalle de misión.
- La lista de participantes del detalle no existe aún; queda fuera.
- La privacidad de `profiles` hacia `anon` (D11) está fuera de alcance; este spec solo restringe lo que consulta la app.

## Requirements

### Requirement: Apertura de la ficha
El sistema MUST abrir la ficha pública al seleccionar a un postulante o al publicador.

#### Scenario: Desde postulantes
- GIVEN publicador viendo los postulantes de una misión
- WHEN selecciona el perfil de "CarlosM"
- THEN despliega una tarjeta de solo lectura con foto, nickname "CarlosM", género, descripción y avatar

#### Scenario: Desde el detalle
- GIVEN solucionador viendo el detalle de una misión
- WHEN selecciona la tarjeta del publicador
- THEN despliega su ficha pública

### Requirement: Solo datos públicos
La ficha MUST mostrar únicamente foto, nickname, género, descripción y avatar, y MUST NOT mostrar ni consultar correo, celular, nombres completos, fecha de nacimiento ni contraseña.

#### Scenario: Columnas consultadas
- GIVEN la consulta del perfil público
- WHEN se ejecuta
- THEN el `select` incluye solo `nickname`, `gender`, `description`, `avatar_id` y `photo_url`

#### Scenario: Sin datos privados en pantalla
- GIVEN la ficha de "CarlosM" abierta
- WHEN el usuario la revisa
- THEN no aparece correo, celular ni nombre completo

### Requirement: Solo lectura
La ficha MUST NOT ofrecer edición y MUST poder cerrarse volviendo a la pantalla de origen.

#### Scenario: Cerrar
- GIVEN ficha pública abierta desde postulantes
- WHEN la cierra
- THEN vuelve a la lista de postulantes

### Requirement: Estados de error y datos ausentes
El sistema MUST manejar fallos y perfiles sin foto o descripción sin cerrarse.

#### Scenario: Sin foto
- GIVEN usuario sin foto pública
- WHEN se abre su ficha
- THEN se muestra un marcador de foto por defecto

#### Scenario: Error de carga
- GIVEN falla la consulta
- WHEN se abre una ficha
- THEN muestra "Ocurrió un error. Intenta de nuevo"

#### Scenario: Sin conexión
- GIVEN dispositivo sin red
- WHEN se abre una ficha
- THEN muestra "Sin conexión. Intenta de nuevo"
