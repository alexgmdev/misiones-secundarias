# Spec: Calificar publicador (HU-124)

## Requisitos

- Solo un participante aceptado de una misión completada puede calificar al publicador.
- La puntuación va de 1 a 5 estrellas y se registra una sola vez por misión y solucionador.
- El promedio y la cantidad de calificaciones forman la reputación pública del publicador.

## Escenarios

### Puntuación de desempeño del publicador

```gherkin
Dado que el solucionador completó la misión
Cuando asigna 5 estrellas a la gestión del publicador y presiona "Enviar"
Entonces la puntuación se añade a la reputación pública del publicador
```

### Calificación duplicada

```gherkin
Dado que el solucionador ya calificó la misión
Cuando intenta enviar otra puntuación
Entonces el sistema informa que ya calificó esta misión
```
