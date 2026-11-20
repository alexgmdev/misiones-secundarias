# Spec: Editar misión publicada (HU-032)

## Requisitos

- El publicador puede editar título, descripción, requisitos, categoría y recompensa de una misión propia en estado `Publicada`.
- La edición se guarda directamente cuando no existen participantes aceptados.
- Si la misión no está publicada o tiene participantes aceptados, el sistema rechaza la edición con un mensaje claro.

## Escenarios

### Edición directa sin participantes

```gherkin
Dado que el publicador tiene una misión publicada sin postulantes aceptados
Cuando edita los requerimientos a "Ropa cómoda y gorra" y presiona "Guardar"
Entonces la misión actualiza sus datos inmediatamente
```

### Edición bloqueada con participantes aceptados

```gherkin
Dado que la misión ya tiene un participante aceptado
Cuando el publicador intenta editar sus datos descriptivos
Entonces el sistema informa que no puede editar una misión con participantes aceptados
Y no guarda cambios
```
