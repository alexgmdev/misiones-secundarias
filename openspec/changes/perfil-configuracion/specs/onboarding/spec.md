# Especificación onboarding (HU-013)

## Purpose
Guía inicial de 4 páginas, omitible y reabrible desde Ajustes → Ayuda.

## Supuestos
- No se persiste "ya visto": se muestra justo tras guardar el perfil inicial.
- Páginas: Misiones de campo, Misiones remotas, Postulaciones, Moderación y convivencia.
- Botones: "Omitir", "Siguiente" y "Empezar" (solo en la última página).

## Requirements

### Requirement: Aparición tras el perfil inicial
El sistema MUST mostrar el onboarding inmediatamente después de completar el perfil inicial.

#### Scenario: Primer ingreso
- GIVEN usuario que acaba de guardar su perfil inicial
- WHEN el guardado termina con éxito
- THEN se muestra la página 1 de 4 con indicador de página

### Requirement: Navegación entre páginas
El usuario MUST poder avanzar con "Siguiente"; en la última página el botón MUST ser "Empezar".

#### Scenario: Avanzar
- GIVEN usuario en la página 1
- WHEN presiona "Siguiente"
- THEN muestra la página 2

#### Scenario: Última página
- GIVEN usuario en la página 4
- WHEN la visualiza
- THEN el botón dice "Empezar" y no "Siguiente"

#### Scenario: Empezar
- GIVEN usuario en la página 4 abierta tras el perfil inicial
- WHEN presiona "Empezar"
- THEN navega al menú principal

### Requirement: Omitir
Cada página MUST ofrecer "Omitir", que cierra la guía.

#### Scenario: Omitir tras perfil inicial
- GIVEN onboarding abierto tras el perfil inicial
- WHEN presiona "Omitir"
- THEN navega al menú principal

### Requirement: Reapertura desde Ajustes
Ajustes MUST mostrar una tarjeta "Ayuda" con "Ver guía de la app" que reabre el onboarding, y al terminar u omitir MUST volver a Ajustes.

#### Scenario: Reabrir
- GIVEN usuario en Ajustes
- WHEN presiona "Ver guía de la app"
- THEN se muestra el onboarding desde la página 1

#### Scenario: Cerrar desde Ajustes
- GIVEN onboarding abierto desde Ajustes
- WHEN presiona "Omitir" o "Empezar"
- THEN vuelve a Ajustes
