# Spec: Previsualizar publicación (HU-023)

## Requisitos

- Con los campos obligatorios completos, el publicador puede abrir una vista previa de la tarjeta y el detalle.
- La vista previa muestra título, requisitos, ubicación, recompensas y descripción.
- Desde la vista previa se puede volver al formulario o guardar el borrador.
- La previsualización no persiste datos ni publica la misión.

## Escenarios

### Previsualización correcta de datos

```gherkin
Dado que el publicador llenó los datos de la misión
Cuando presiona "Previsualizar"
Entonces el sistema despliega la vista previa con el título, requerimientos, ubicación y recompensas informativas
Y puede volver al formulario para ajustar los datos
```

### Previsualización bloqueada con datos incompletos

```gherkin
Dado que el publicador no completó los campos obligatorios
Cuando presiona "Previsualizar"
Entonces el sistema muestra los errores por campo
Y no abre la vista previa
```
