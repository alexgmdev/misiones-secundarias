# Especificación fecha-hora-inicio (HU-020, HU-021)

## Purpose

Permitir al publicador fijar la fecha y la hora de inicio de una misión y persistirlas en el campo único `date_time` de Supabase.

## Supuestos

- La base tiene `date_time TIMESTAMP WITH TIME ZONE NOT NULL`; el borrador exige fecha y hora (decisión de diseño D2).
- La zona horaria de la aplicación es `America/Bogota` (UTC-5, sin horario de verano).
- Los selectores son los de Material 3 (`DatePickerDialog`, `TimePickerDialog`).

## Requirements

### Requirement: Fecha de inicio obligatoria
El sistema MUST exigir una fecha de inicio antes de guardar o publicar.

#### Scenario: Sin fecha
- GIVEN el formulario sin fecha seleccionada
- WHEN intenta guardar
- THEN muestra "Selecciona la fecha de inicio"
- AND no llama al backend

### Requirement: Fecha no anterior a hoy
La fecha seleccionada MUST ser igual o posterior a la fecha actual.

#### Scenario: Fecha futura válida
- GIVEN hoy es 2026-11-05
- WHEN selecciona 2026-11-15
- THEN la fecha se acepta y se asigna a la misión

#### Scenario: Fecha de hoy
- GIVEN hoy es 2026-11-05
- WHEN selecciona 2026-11-05
- THEN la fecha se acepta

#### Scenario: Fecha pasada
- GIVEN hoy es 2026-11-05
- WHEN selecciona 2026-11-01
- THEN muestra "La fecha no puede ser anterior a hoy"
- AND no guarda el borrador

### Requirement: Hora de inicio obligatoria
El sistema MUST exigir una hora de inicio antes de guardar o publicar.

#### Scenario: Sin hora
- GIVEN el formulario con fecha pero sin hora
- WHEN intenta guardar
- THEN muestra "Selecciona la hora de inicio"
- AND no llama al backend

### Requirement: Selección de hora válida
El selector MUST permitir cualquier hora del día en formato de 24 horas y MUST guardarla sin ambigüedad.

#### Scenario: Hora de la mañana
- GIVEN el publicador selecciona 10:00
- WHEN guarda el borrador
- THEN el sistema registra las 10:00 como hora de inicio

#### Scenario: Hora de la tarde
- GIVEN el publicador selecciona 15:30
- WHEN guarda el borrador
- THEN el sistema registra las 15:30 como hora de inicio

### Requirement: Combinación fecha + hora
El sistema MUST combinar fecha y hora en un único `LocalDateTime` con zona `America/Bogota` y MUST serializarlo en ISO-8601 con offset para `date_time`.

#### Scenario: Serialización correcta
- GIVEN fecha 2026-11-15 y hora 10:00
- WHEN se construye el borrador
- THEN el valor enviado a Supabase es `2026-11-15T10:00:00-05:00`

#### Scenario: Cambio de hora no altera la fecha
- GIVEN fecha 2026-11-15 y hora 10:00
- WHEN el publicador cambia la hora a 18:00
- THEN la fecha sigue siendo 2026-11-15
- AND `date_time` pasa a `2026-11-15T18:00:00-05:00`

### Requirement: Persistencia y visualización
La fecha y hora guardadas MUST mostrarse en el detalle de la misión en formato legible en español.

#### Scenario: Detalle de misión
- GIVEN una misión guardada para 2026-11-15 a las 10:00
- WHEN el usuario abre el detalle
- THEN se muestra la fecha y hora en formato local (por ejemplo, "15 nov 2026, 10:00 a. m.")

### Requirement: Validación al publicar
Las mismas reglas de fecha y hora MUST aplicarse al publicar, no solo al guardar el borrador.

#### Scenario: Borrador con fecha vencida
- GIVEN un borrador guardado para una fecha ya pasada
- WHEN el publicador intenta publicarlo
- THEN el sistema MUST impedir la publicación y pedir una fecha nueva
