# Especificación historial-misiones (HU-011)

## Purpose
Línea temporal cronológica de las misiones del usuario como publicador o solucionador.

## Supuestos
- Incluye misiones publicadas por el usuario (excluye "Borrador") y misiones con postulación "Aceptado".
- Orden por fecha de la misión descendente; una misión no se repite.
- Bitácora oficial y Palabras de participantes se leen de `mission_logbooks` (rol `publicador` = bitácora oficial; rol `participante` = palabras de participantes) con `LogbookRepository.getLogbooksForMission`, de forma diferida al expandir y con caché por misión. Si no hay registros (o la carga falla) cada sección muestra el estado vacío ("Aún no hay registros").
- Rol mostrado como "Publicador" o "Solucionador".

## Requirements

### Requirement: Acceso
"Histórico de Misiones" MUST abrirse desde Mi perfil y desde el destino HISTORY del menú.

#### Scenario: Desde Mi perfil
- GIVEN usuario en Mi perfil
- WHEN presiona "Histórico de Misiones"
- THEN se muestra la línea temporal

### Requirement: Contenido y orden
La línea temporal MUST listar las misiones publicadas (no borrador) y aceptadas como solucionador, ordenadas de más reciente a más antigua, con título, fecha, estado y chip de rol.

#### Scenario: Mezcla de roles
- GIVEN usuario con una misión publicada y otra donde fue aceptado
- WHEN abre el historial
- THEN ve ambas con chips "Publicador" y "Solucionador" respectivamente, la más reciente primero

#### Scenario: Borradores excluidos
- GIVEN usuario con una misión en "Borrador"
- WHEN abre el historial
- THEN esa misión no aparece

#### Scenario: Sin duplicados
- GIVEN una misión que aparece por dos vías
- WHEN abre el historial
- THEN aparece una sola vez

### Requirement: Detalle expandible
Al seleccionar una misión, el sistema MUST expandir fecha, título, descripción y requerimientos, y las secciones "Bitácora oficial" y "Palabras de participantes" (con los registros existentes, o vacías).

#### Scenario: Expandir
- GIVEN la misión "Limpieza del Río Pance" en la línea temporal
- WHEN la selecciona
- THEN muestra fecha, título, descripción y requerimientos

#### Scenario: Secciones con registros
- GIVEN una misión expandida con bitácora del publicador y palabras de participantes
- WHEN el usuario revisa las secciones
- THEN "Bitácora oficial" muestra la entrada del publicador y "Palabras de participantes" lista nombre del autor y texto de cada participante

#### Scenario: Secciones vacías
- GIVEN una misión expandida sin registros en `mission_logbooks`
- WHEN el usuario revisa "Bitácora oficial" y "Palabras de participantes"
- THEN cada una muestra "Aún no hay registros"

#### Scenario: Falla la carga de la bitácora
- GIVEN una misión expandida cuya consulta de bitácora falla
- WHEN el usuario la expande
- THEN las secciones muestran "Aún no hay registros" y la línea temporal sigue funcionando (se reintenta al expandir de nuevo)

#### Scenario: Contraer
- GIVEN una misión expandida
- WHEN la selecciona de nuevo
- THEN se contrae

### Requirement: Estados vacío y error
El sistema MUST informar cuando no hay misiones o la carga falla.

#### Scenario: Historial vacío
- GIVEN usuario sin misiones
- WHEN abre el historial
- THEN muestra "Aún no tienes misiones en tu historial"

#### Scenario: Error de carga
- GIVEN falla la consulta
- WHEN abre el historial
- THEN muestra "Ocurrió un error. Intenta de nuevo"

#### Scenario: Sin conexión
- GIVEN dispositivo sin red
- WHEN abre el historial
- THEN muestra "Sin conexión. Intenta de nuevo"
