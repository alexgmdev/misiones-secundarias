# Especificación publicar-mision (HU-017)

## Purpose

Permitir al publicador publicar un borrador de misión de campo ya completo, de modo que la actividad sea visible en la búsqueda pública.

## Supuestos

- La política RLS `Publishers can update missions` permite al creador actualizar su propia misión.
- La búsqueda pública filtra `status != 'Borrador'` (`getPublishedMissions`).
- Un borrador puede tener datos incompletos solo si se creó antes de esta feature; el flujo nuevo exige los obligatorios.

## Requirements

### Requirement: Acción de publicar
El sistema MUST ofrecer la acción "Publicar Misión" sobre un borrador del publicador.

#### Scenario: Borrador propio
- GIVEN un borrador del usuario autenticado con todos los datos obligatorios
- WHEN abre el borrador
- THEN ve la acción "Publicar Misión" habilitada

### Requirement: Verificación de requisitos previos
Antes de cambiar el estado, el sistema MUST verificar que el borrador tenga título, descripción, requisitos, fecha y hora válidas y punto de encuentro definido.

#### Scenario: Punto de encuentro ausente
- GIVEN un borrador de campo sin punto de encuentro
- WHEN presiona "Publicar Misión"
- THEN muestra "Define el punto de encuentro antes de publicar"
- AND no cambia el estado

#### Scenario: Fecha vencida
- GIVEN un borrador con fecha anterior a hoy
- WHEN presiona "Publicar Misión"
- THEN muestra "La fecha no puede ser anterior a hoy"
- AND no cambia el estado

### Requirement: Transición a publicada
Al publicar, el sistema MUST actualizar `status` a `'Publicada'` y MUST confirmar el nuevo estado al usuario.

#### Scenario: Publicación exitosa
- GIVEN un borrador completo
- WHEN el publicador presiona "Publicar Misión"
- THEN la misión pasa a estado `Publicada`
- AND la pantalla confirma "Misión publicada correctamente"

### Requirement: Visibilidad pública
Una misión publicada MUST aparecer en la búsqueda pública, y una que falla al publicarse MUST seguir oculta.

#### Scenario: Aparece en la lista
- GIVEN una misión recién publicada
- WHEN otro usuario abre "Buscar misiones"
- THEN la misión aparece en la lista

#### Scenario: Fallo de publicación
- GIVEN un intento de publicación que falla por red
- WHEN el usuario vuelve a la lista pública
- THEN la misión sigue con estado `Borrador` y no aparece

### Requirement: Estado de carga y doble envío
El botón MUST deshabilitarse mientras la publicación está en curso y MUST NOT enviar solicitudes duplicadas.

#### Scenario: Doble toque
- GIVEN una publicación en curso
- WHEN presiona "Publicar Misión" de nuevo
- THEN no se envía otra solicitud

### Requirement: Errores
El sistema MUST distinguir errores de red de errores de validación y MUST NOT declarar éxito si la actualización falló.

#### Scenario: Sin conexión
- GIVEN no hay red
- WHEN presiona "Publicar Misión"
- THEN muestra "Sin conexión. Intenta de nuevo"
- AND la misión permanece en `Borrador`

#### Scenario: Error del servidor
- GIVEN Supabase responde 5xx
- WHEN presiona "Publicar Misión"
- THEN muestra "Ocurrió un error. Intenta de nuevo"
- AND la misión permanece en `Borrador`

### Requirement: Idempotencia
Publicar una misión ya publicada MUST NOT duplicar registros ni generar un estado inconsistente; el sistema SHOULD mostrar el estado actual.

#### Scenario: Publicar dos veces
- GIVEN una misión ya publicada
- WHEN se intenta publicar de nuevo
- THEN no se crea otra misión y el estado sigue siendo `Publicada`
