# Especificación cambio-contrasena-ajustes (HU-010)

## Purpose
Cambiar la contraseña desde Ajustes → Seguridad con la sesión activa, validando la actual.

## Supuestos
- La contraseña actual se valida reautenticando con el correo de la sesión y la clave actual; la sesión se mantiene al terminar.
- "Secure password change" del dashboard de Supabase debe estar desactivado (dependencia externa).
- Reglas de contraseña idénticas a `auth-registro`: mínimo 8 caracteres, mayúscula, minúscula y número.
- Mensajes reutilizados de `AuthMessages`; "La contraseña actual es incorrecta" es nuevo.

## Requirements

### Requirement: Acceso desde Ajustes
Ajustes MUST mostrar una tarjeta "Seguridad" con la opción "Cambiar contraseña" que abre la pantalla de cambio.

#### Scenario: Abrir la pantalla
- GIVEN usuario con sesión activa en Ajustes
- WHEN presiona "Cambiar contraseña" en la tarjeta "Seguridad"
- THEN se muestran los campos contraseña actual, nueva y confirmación, y el botón "Actualizar Contraseña"

### Requirement: Campos obligatorios
Los tres campos MUST ser no vacíos y MUST poder mostrarse u ocultarse ("Mostrar"/"Ocultar").

#### Scenario: Campos vacíos
- GIVEN la pantalla de cambio con campos vacíos
- WHEN presiona "Actualizar Contraseña"
- THEN muestra "Este campo es obligatorio" por campo y no llama al backend

### Requirement: Reglas de la nueva contraseña
La nueva contraseña MUST tener al menos 8 caracteres e incluir mayúscula, minúscula y número, y MUST coincidir con su confirmación.

#### Scenario: Contraseña corta
- GIVEN nueva contraseña "Ab1"
- WHEN presiona "Actualizar Contraseña"
- THEN muestra "La contraseña debe tener al menos 8 caracteres" y no llama al backend

#### Scenario: Sin clases requeridas
- GIVEN nueva contraseña "abcdefghi"
- WHEN presiona "Actualizar Contraseña"
- THEN muestra "La contraseña debe incluir mayúscula, minúscula y número"

#### Scenario: Confirmación distinta
- GIVEN nueva "Nueva1234" y confirmación "Nueva12345"
- WHEN presiona "Actualizar Contraseña"
- THEN muestra "Las contraseñas no coinciden" y no llama al backend

### Requirement: Validación de la contraseña actual
El sistema MUST validar la contraseña actual contra la cuenta antes de cambiarla.

#### Scenario: Actual incorrecta
- GIVEN usuario con contraseña "Actual123"
- WHEN ingresa actual "Otra12345", nueva "Nueva1234" y confirmación "Nueva1234"
- THEN muestra "La contraseña actual es incorrecta" y no cambia la contraseña

### Requirement: Nueva distinta de la actual
La nueva contraseña MUST ser diferente de la actual.

#### Scenario: Nueva igual a la actual
- GIVEN usuario con contraseña "Actual123"
- WHEN ingresa actual "Actual123", nueva "Actual123" y confirmación "Actual123"
- THEN muestra "La nueva contraseña debe ser diferente a la anterior" y no llama a actualizar

### Requirement: Cambio exitoso con sesión activa
Con datos válidos el sistema MUST actualizar la contraseña, confirmar el cambio y MUST NOT cerrar la sesión.

#### Scenario: Éxito
- GIVEN usuario con contraseña "Actual123"
- WHEN ingresa actual "Actual123", nueva "Nueva1234", confirmación "Nueva1234" y presiona "Actualizar Contraseña"
- THEN muestra el diálogo "Contraseña actualizada" y la sesión sigue activa

### Requirement: Errores de red y servidor
El sistema MUST informar fallos sin exponer detalles técnicos.

#### Scenario: Sin conexión
- GIVEN dispositivo sin red
- WHEN presiona "Actualizar Contraseña" con datos válidos
- THEN muestra "Sin conexión. Intenta de nuevo" y conserva lo escrito

#### Scenario: Error del servidor
- GIVEN el backend falla
- WHEN presiona "Actualizar Contraseña" con datos válidos
- THEN muestra "Ocurrió un error. Intenta de nuevo"
