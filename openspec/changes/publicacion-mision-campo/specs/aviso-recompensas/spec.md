# Especificación aviso-recompensas (HU-126)

## Purpose

Aclarar al usuario que la plataforma es puramente comunitaria e informativa y que no procesa pagos, transferencias ni dinero real.

## Supuestos

- El detalle de misión (`ui/missions/MissionDetailScreen.kt`) ya muestra la información de la misión.
- "Recompensas" en el MVP es texto informativo; no existe entidad ni flujo de pago.

## Requirements

### Requirement: Aviso fijo y visible
Toda misión con recompensas expresas MUST mostrar una tarjeta informativa fija en la sección de recompensas, con el texto exacto definido por el producto.

#### Scenario: Visualización del aviso
- GIVEN el usuario consulta las recompensas de una misión
- WHEN lee la tarjeta informativa
- THEN observa el texto "Aviso: La plataforma no maneja pasarela de pago; las recompensas son meramente informativas"

#### Scenario: Texto exacto
- GIVEN la tarjeta renderizada
- WHEN se compara con el criterio de aceptación de HU-126
- THEN el texto coincide carácter por carácter (incluidos acentos y puntuación)

### Requirement: Alcance del aviso
El aviso MUST aparecer en el detalle de las misiones que declaran recompensas y MUST NOT aparecer en pantallas sin sección de recompensas.

#### Scenario: Misión con recompensas
- GIVEN una misión con recompensas expresas
- WHEN se abre el detalle
- THEN el aviso está presente

#### Scenario: Pantalla sin recompensas
- GIVEN la lista de misiones
- WHEN se muestra la lista
- THEN el aviso no se muestra

### Requirement: Carácter informativo
El aviso MUST ser solo informativo y MUST NOT ofrecer acciones de pago, enlaces de transferencia ni botones transaccionales.

#### Scenario: Sin acciones
- GIVEN la tarjeta informativa
- WHEN el usuario interactúa con ella
- THEN no hay botones de pago ni navegación a pasarelas

### Requirement: Accesibilidad
El aviso MUST ser legible por lectores de pantalla como un texto estático, con contraste suficiente.

#### Scenario: Lector de pantalla
- GIVEN TalkBack activo
- WHEN el usuario enfoca la tarjeta
- THEN se anuncia el texto completo del aviso
