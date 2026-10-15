# language: es
Característica: Catálogo público de paquetes
  Escenario: Un visitante ve todos los paquetes sin iniciar sesión
    Dado que visito el catálogo público con 12 paquetes
    Entonces veo todas las tarjetas con destino, descripción e imagen
    Cuando abro el login y vuelvo con Inicio
    Entonces veo todas las tarjetas con destino, descripción e imagen

  Escenario: El administrador puede volver de Administración a Inicio
    Dado que ingreso a la Home con rol "ADMINISTRADOR"
    Cuando vuelvo a Inicio desde Administración
    Entonces veo el catálogo y puedo volver a Administración

  Escenario: La base vacía muestra un aviso
    Dado que visito el catálogo público con 0 paquetes
    Entonces veo el aviso de que no hay paquetes

  Escenario: Un error del catálogo permite reintentar
    Dado que falla la carga del catálogo público
    Cuando reintento la carga del catálogo
    Entonces veo todas las tarjetas con destino, descripción e imagen
