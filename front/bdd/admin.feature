# language: es
Característica: Administración de usuarios y agencias
  Escenario: Alta, edición y baja de un usuario agente
    Dado que ingreso a la Home con rol "ADMINISTRADOR"
    Cuando creo un usuario agente con una agencia
    Entonces veo el usuario agente creado
    Cuando cambio el usuario creado a comprador sin cambiar su contraseña
    Entonces veo el usuario actualizado como comprador
    Cuando cancelo la eliminación del usuario creado
    Entonces el usuario creado sigue registrado
    Cuando confirmo la eliminación del usuario creado
    Entonces el usuario creado deja de estar registrado

  Escenario: Alta, edición y baja de una agencia
    Dado que ingreso a la Home con rol "ADMINISTRADOR"
    Cuando creo una agencia desde la Home
    Entonces veo la agencia creada
    Cuando edito el nombre de la agencia creada
    Entonces veo la agencia actualizada
    Cuando elimino la agencia creada
    Entonces la agencia creada deja de estar registrada

  Escenario: Una agencia con referencias conserva sus datos
    Dado que ingreso a la Home con rol "ADMINISTRADOR"
    Cuando intento eliminar una agencia con referencias
    Entonces veo el rechazo del backend y la agencia sigue registrada

  Escenario: Consulta por rol y detalle de usuario
    Dado que ingreso a la Home con rol "ADMINISTRADOR"
    Cuando filtro los usuarios por agente y consulto su detalle
    Entonces veo sus datos personales y agencia

  Escenario: Protección de la cuenta propia
    Dado que ingreso a la Home con rol "ADMINISTRADOR"
    Entonces no puedo eliminar mi cuenta ni cambiar mi propio rol

  Esquema del escenario: La administración está reservada al administrador
    Dado que ingreso a la Home con rol "<rol>"
    Entonces veo la Home de viajes sin controles administrativos
    Ejemplos:
      | rol       |
      | COMPRADOR |
      | AGENTE    |

  Escenario: Una sesión vencida vuelve al login
    Dado que ingreso a la Home con rol "ADMINISTRADOR"
    Cuando el backend rechaza la sesión y su renovación
    Entonces vuelvo al login con el aviso de sesión terminada

  Escenario: Un error de carga permite reintentar
    Dado que ingreso a la Home con rol "ADMINISTRADOR"
    Cuando falla temporalmente la consulta de agencias
    Entonces puedo reintentar y ver las agencias
