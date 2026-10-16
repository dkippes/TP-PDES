# language: es
Característica: Catálogo público de paquetes
  Escenario: Un visitante ve todos los paquetes sin iniciar sesión
    Dado que visito el catálogo público con 12 paquetes
    Entonces veo la invitación a unirme antes de los paquetes
    Y veo todas las tarjetas con destino, descripción e imagen
    Cuando abro el login y vuelvo con Inicio
    Entonces veo la invitación a unirme antes de los paquetes
    Y veo todas las tarjetas con destino, descripción e imagen

  Esquema del escenario: Los botones de la invitación abren los formularios actuales
    Dado que visito el catálogo público con 12 paquetes
    Cuando elijo "<acción>" desde la invitación
    Entonces veo el formulario de "<formulario>"
    Cuando vuelvo al catálogo con Inicio
    Entonces veo la invitación a unirme antes de los paquetes
    Y veo todas las tarjetas con destino, descripción e imagen
    Ejemplos:
      | acción         | formulario |
      | Registrarse    | registro   |
      | Iniciar sesión | login      |

  Esquema del escenario: Una sesión restaurada oculta la invitación hasta cerrar sesión
    Dado que ingreso a la Home con rol "<rol>"
    Cuando vuelvo al catálogo con Inicio
    Entonces no veo la invitación a unirme
    Cuando cierro mi sesión
    Entonces veo la invitación a unirme antes de los paquetes
    Ejemplos:
      | rol           |
      | COMPRADOR     |
      | AGENTE        |
      | ADMINISTRADOR |

  Escenario: La invitación mantiene los paquetes cerca en una pantalla móvil
    Dado que uso una pantalla móvil
    Y que visito el catálogo público con 12 paquetes
    Entonces veo la invitación a unirme antes de los paquetes
    Y veo el primer paquete sin desplazamiento horizontal

  Escenario: El administrador puede volver de Administración a Inicio
    Dado que ingreso a la Home con rol "ADMINISTRADOR"
    Cuando vuelvo a Inicio desde Administración
    Entonces veo el catálogo y puedo volver a Administración

  Escenario: La base vacía muestra un aviso
    Dado que visito el catálogo público con 0 paquetes
    Entonces veo la invitación a unirme antes de los paquetes
    Y veo el aviso de que no hay paquetes

  Escenario: Un error del catálogo permite reintentar
    Dado que falla la carga del catálogo público
    Entonces veo la invitación a unirme antes de los paquetes
    Cuando reintento la carga del catálogo
    Entonces veo todas las tarjetas con destino, descripción e imagen
