-- COMP-04: Esquema físico PostgreSQL para Restaurant Tickets / El Chupacabras
-- Estado: diseño completo previo a implementación
-- PostgreSQL 15+

BEGIN;

CREATE TYPE canal_solicitud AS ENUM ('PRESENCIAL', 'WHATSAPP');
CREATE TYPE modalidad_pedido AS ENUM ('CONSUMO_LOCAL', 'PARA_LLEVAR', 'RECOGER', 'DOMICILIO');
CREATE TYPE estado_solicitud AS ENUM ('PENDIENTE_PAGO', 'CANCELADA_SIN_PAGO', 'CONVERTIDA');
CREATE TYPE estado_pedido AS ENUM (
  'PAGADO_REGISTRADO',
  'EN_PREPARACION',
  'LISTO_PARA_DESPACHO',
  'LISTO_PARA_RECOGER',
  'EN_RUTA_DOMICILIO',
  'ENTREGADO',
  'NO_ENTREGADO',
  'CANCELADO_REEMBOLSO_PENDIENTE',
  'REEMBOLSO_PENDIENTE',
  'REEMBOLSADO',
  'ENTREGADO_CON_AJUSTE'
);
CREATE TYPE medio_pago AS ENUM ('EFECTIVO', 'TRANSFERENCIA');
CREATE TYPE tipo_pago AS ENUM ('PEDIDO', 'DOMICILIO_ADICIONAL');
CREATE TYPE tipo_producto AS ENUM ('INDIVIDUAL', 'COMBO');
CREATE TYPE tipo_insumo AS ENUM ('INGREDIENTE', 'BEBIDA_ENVASADA', 'EMPAQUE', 'OTRO');
CREATE TYPE estado_jornada AS ENUM ('ABIERTA', 'CERRADA');
CREATE TYPE tipo_movimiento_inventario AS ENUM ('ENTRADA', 'AJUSTE_POSITIVO', 'AJUSTE_NEGATIVO', 'PERDIDA', 'LIBERACION', 'CONSUMO');
CREATE TYPE estado_reserva AS ENUM ('ACTIVA', 'CONSUMIDA', 'LIBERADA');
CREATE TYPE estado_domicilio AS ENUM ('PENDIENTE', 'EN_RUTA', 'ENTREGADO', 'NO_ENTREGADO');
CREATE TYPE resultado_entrega AS ENUM ('ENTREGADO', 'NO_ENTREGADO');
CREATE TYPE atribucion_incidencia AS ENUM ('NEGOCIO', 'CLIENTE', 'EXTERNO');
CREATE TYPE tipo_incidencia AS ENUM ('FALTANTE_FISICO', 'DETERIORO', 'DESPERDICIO', 'ERROR_PREPARACION', 'PRODUCTO_INCORRECTO', 'PRODUCTO_INCOMPLETO', 'FALLA_DOMICILIO', 'OTRA');
CREATE TYPE tipo_resolucion AS ENUM ('REPOSICION', 'SUSTITUCION', 'DEVOLUCION_TOTAL', 'DEVOLUCION_PARCIAL', 'NUEVO_INTENTO_DOMICILIO', 'RECOGIDA_EN_LOCAL');
CREATE TYPE tipo_devolucion AS ENUM ('TOTAL', 'PARCIAL');
CREATE TYPE estado_devolucion AS ENUM ('SOLICITADA', 'APROBADA', 'RECHAZADA', 'EJECUTADA');

CREATE TABLE sede (
  id                 BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
  nombre             VARCHAR(100) NOT NULL UNIQUE,
  municipio          VARCHAR(100) NOT NULL DEFAULT 'Los Patios',
  departamento       VARCHAR(100) NOT NULL DEFAULT 'Norte de Santander',
  activa             BOOLEAN NOT NULL DEFAULT TRUE,
  creado_en          TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE rol (
  id                 BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
  codigo             VARCHAR(30) NOT NULL UNIQUE,
  nombre             VARCHAR(80) NOT NULL,
  CONSTRAINT ck_rol_codigo CHECK (codigo IN ('PROPIETARIO', 'CAJA', 'COCINA', 'DESPACHO', 'DOMICILIARIO'))
);

CREATE TABLE usuario (
  id                 BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
  nombre_usuario     VARCHAR(60) NOT NULL UNIQUE,
  nombre_completo    VARCHAR(120) NOT NULL,
  hash_contrasena    VARCHAR(255) NOT NULL,
  activo             BOOLEAN NOT NULL DEFAULT TRUE,
  intentos_fallidos  SMALLINT NOT NULL DEFAULT 0 CHECK (intentos_fallidos >= 0),
  bloqueado_hasta    TIMESTAMPTZ,
  ultimo_acceso_en   TIMESTAMPTZ,
  creado_en          TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
  actualizado_en     TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE usuario_rol (
  usuario_id         BIGINT NOT NULL REFERENCES usuario(id),
  rol_id             BIGINT NOT NULL REFERENCES rol(id),
  PRIMARY KEY (usuario_id, rol_id)
);

CREATE TABLE unidad_medida (
  id                 BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
  codigo             VARCHAR(20) NOT NULL UNIQUE,
  nombre             VARCHAR(60) NOT NULL,
  magnitud           VARCHAR(30) NOT NULL,
  factor_base        NUMERIC(18,6) NOT NULL DEFAULT 1 CHECK (factor_base > 0),
  activa             BOOLEAN NOT NULL DEFAULT TRUE
);

CREATE TABLE categoria_producto (
  id                 BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
  nombre             VARCHAR(80) NOT NULL UNIQUE,
  descripcion        VARCHAR(250),
  activa             BOOLEAN NOT NULL DEFAULT TRUE
);

CREATE TABLE producto (
  id                 BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
  categoria_id       BIGINT NOT NULL REFERENCES categoria_producto(id),
  codigo             VARCHAR(40) NOT NULL UNIQUE,
  nombre             VARCHAR(120) NOT NULL,
  descripcion        VARCHAR(500),
  tipo               tipo_producto NOT NULL DEFAULT 'INDIVIDUAL',
  precio             NUMERIC(14,2) NOT NULL CHECK (precio >= 0),
  activo             BOOLEAN NOT NULL DEFAULT TRUE,
  creado_en          TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
  actualizado_en     TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE insumo (
  id                 BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
  unidad_medida_id   BIGINT NOT NULL REFERENCES unidad_medida(id),
  codigo             VARCHAR(40) NOT NULL UNIQUE,
  nombre             VARCHAR(120) NOT NULL,
  tipo               tipo_insumo NOT NULL,
  umbral_bajo        NUMERIC(14,3) NOT NULL DEFAULT 0 CHECK (umbral_bajo >= 0),
  activo             BOOLEAN NOT NULL DEFAULT TRUE,
  creado_en          TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
  actualizado_en     TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE producto_insumo (
  producto_id        BIGINT NOT NULL REFERENCES producto(id),
  insumo_id          BIGINT NOT NULL REFERENCES insumo(id),
  cantidad_requerida NUMERIC(14,3) NOT NULL CHECK (cantidad_requerida > 0),
  PRIMARY KEY (producto_id, insumo_id)
);

CREATE TABLE zona_domicilio (
  id                 BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
  sede_id            BIGINT NOT NULL REFERENCES sede(id),
  nombre             VARCHAR(100) NOT NULL,
  municipio          VARCHAR(100) NOT NULL DEFAULT 'Los Patios',
  tarifa             NUMERIC(14,2) NOT NULL CHECK (tarifa >= 0),
  activa             BOOLEAN NOT NULL DEFAULT TRUE,
  creado_en          TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
  actualizado_en     TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
  UNIQUE (sede_id, nombre),
  CONSTRAINT ck_zona_municipio CHECK (municipio = 'Los Patios')
);

CREATE TABLE jornada (
  id                 BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
  sede_id            BIGINT NOT NULL REFERENCES sede(id),
  fecha_operacion    DATE NOT NULL,
  estado             estado_jornada NOT NULL DEFAULT 'ABIERTA',
  abierta_por        BIGINT NOT NULL REFERENCES usuario(id),
  abierta_en         TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
  cerrada_en         TIMESTAMPTZ,
  UNIQUE (sede_id, fecha_operacion),
  CONSTRAINT ck_jornada_cierre CHECK (
    (estado = 'ABIERTA' AND cerrada_en IS NULL)
    OR (estado = 'CERRADA' AND cerrada_en IS NOT NULL AND cerrada_en >= abierta_en)
  )
);

CREATE UNIQUE INDEX uq_jornada_abierta_sede
  ON jornada (sede_id)
  WHERE estado = 'ABIERTA';

CREATE TABLE inventario_jornada (
  id                  BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
  jornada_id          BIGINT NOT NULL REFERENCES jornada(id),
  insumo_id           BIGINT NOT NULL REFERENCES insumo(id),
  cantidad_inicial    NUMERIC(14,3) NOT NULL CHECK (cantidad_inicial >= 0),
  cantidad_disponible NUMERIC(14,3) NOT NULL CHECK (cantidad_disponible >= 0),
  cantidad_reservada  NUMERIC(14,3) NOT NULL DEFAULT 0 CHECK (cantidad_reservada >= 0),
  version             BIGINT NOT NULL DEFAULT 0 CHECK (version >= 0),
  actualizado_en      TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
  UNIQUE (jornada_id, insumo_id)
);

CREATE TABLE solicitud (
  id                   BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
  jornada_id           BIGINT NOT NULL REFERENCES jornada(id),
  creada_por           BIGINT NOT NULL REFERENCES usuario(id),
  zona_domicilio_id    BIGINT REFERENCES zona_domicilio(id),
  canal                canal_solicitud NOT NULL,
  modalidad            modalidad_pedido NOT NULL,
  nombre_cliente       VARCHAR(120) NOT NULL,
  direccion_texto      VARCHAR(300),
  costo_domicilio      NUMERIC(14,2) NOT NULL DEFAULT 0 CHECK (costo_domicilio >= 0),
  subtotal             NUMERIC(14,2) NOT NULL DEFAULT 0 CHECK (subtotal >= 0),
  total                NUMERIC(14,2) NOT NULL DEFAULT 0 CHECK (total >= 0),
  estado               estado_solicitud NOT NULL DEFAULT 'PENDIENTE_PAGO',
  creada_en            TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
  actualizada_en       TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
  CONSTRAINT ck_solicitud_domicilio CHECK (
    (modalidad = 'DOMICILIO' AND zona_domicilio_id IS NOT NULL AND direccion_texto IS NOT NULL AND costo_domicilio >= 0)
    OR
    (modalidad <> 'DOMICILIO' AND zona_domicilio_id IS NULL AND direccion_texto IS NULL AND costo_domicilio = 0)
  ),
  CONSTRAINT ck_solicitud_total CHECK (total = subtotal + costo_domicilio)
);

CREATE TABLE solicitud_detalle (
  id                  BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
  solicitud_id        BIGINT NOT NULL REFERENCES solicitud(id) ON DELETE CASCADE,
  producto_id         BIGINT NOT NULL REFERENCES producto(id),
  cantidad            INTEGER NOT NULL CHECK (cantidad > 0),
  precio_unitario     NUMERIC(14,2) NOT NULL CHECK (precio_unitario >= 0),
  preferencias        VARCHAR(500),
  subtotal            NUMERIC(14,2) NOT NULL CHECK (subtotal >= 0),
  CONSTRAINT ck_solicitud_detalle_subtotal CHECK (subtotal = precio_unitario * cantidad)
);

CREATE TABLE pedido (
  id                   BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
  solicitud_id         BIGINT NOT NULL UNIQUE REFERENCES solicitud(id),
  jornada_id           BIGINT NOT NULL REFERENCES jornada(id),
  codigo_publico       VARCHAR(40) NOT NULL UNIQUE,
  canal                canal_solicitud NOT NULL,
  modalidad            modalidad_pedido NOT NULL,
  nombre_cliente       VARCHAR(120) NOT NULL,
  subtotal             NUMERIC(14,2) NOT NULL CHECK (subtotal >= 0),
  costo_domicilio      NUMERIC(14,2) NOT NULL DEFAULT 0 CHECK (costo_domicilio >= 0),
  total                NUMERIC(14,2) NOT NULL CHECK (total >= 0),
  estado               estado_pedido NOT NULL DEFAULT 'PAGADO_REGISTRADO',
  creado_por           BIGINT NOT NULL REFERENCES usuario(id),
  creado_en            TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
  actualizado_en       TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
  version              BIGINT NOT NULL DEFAULT 0 CHECK (version >= 0),
  CONSTRAINT ck_pedido_total CHECK (total = subtotal + costo_domicilio)
);

CREATE TABLE pedido_detalle (
  id                  BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
  pedido_id           BIGINT NOT NULL REFERENCES pedido(id),
  producto_id         BIGINT NOT NULL REFERENCES producto(id),
  producto_codigo     VARCHAR(40) NOT NULL,
  producto_nombre     VARCHAR(120) NOT NULL,
  cantidad            INTEGER NOT NULL CHECK (cantidad > 0),
  precio_unitario     NUMERIC(14,2) NOT NULL CHECK (precio_unitario >= 0),
  preferencias        VARCHAR(500),
  subtotal            NUMERIC(14,2) NOT NULL CHECK (subtotal >= 0),
  CONSTRAINT ck_pedido_detalle_subtotal CHECK (subtotal = precio_unitario * cantidad)
);

CREATE TABLE pago (
  id                       BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
  pedido_id                BIGINT NOT NULL REFERENCES pedido(id),
  confirmado_por           BIGINT NOT NULL REFERENCES usuario(id),
  tipo                     tipo_pago NOT NULL DEFAULT 'PEDIDO',
  medio                    medio_pago NOT NULL,
  monto_pagado             NUMERIC(14,2) NOT NULL CHECK (monto_pagado > 0),
  monto_recibido           NUMERIC(14,2),
  vuelto                   NUMERIC(14,2) NOT NULL DEFAULT 0 CHECK (vuelto >= 0),
  transferencia_verificada BOOLEAN,
  referencia_externa       VARCHAR(100),
  confirmado_en            TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
  CONSTRAINT ck_pago_efectivo CHECK (
    (medio = 'EFECTIVO' AND monto_recibido IS NOT NULL AND monto_recibido >= monto_pagado AND transferencia_verificada IS NULL)
    OR
    (medio = 'TRANSFERENCIA' AND monto_recibido IS NULL AND vuelto = 0 AND transferencia_verificada IS TRUE
      AND referencia_externa IS NOT NULL AND BTRIM(referencia_externa) <> '')
  ),
  CONSTRAINT ck_pago_vuelto CHECK (
    (medio = 'EFECTIVO' AND vuelto = monto_recibido - monto_pagado)
    OR medio = 'TRANSFERENCIA'
  )
);

CREATE UNIQUE INDEX uq_pago_principal_pedido
  ON pago (pedido_id)
  WHERE tipo = 'PEDIDO';

CREATE INDEX ix_pago_pedido_tipo ON pago (pedido_id, tipo, confirmado_en);

CREATE TABLE reserva_inventario (
  id                     BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
  pedido_id              BIGINT NOT NULL REFERENCES pedido(id),
  inventario_jornada_id  BIGINT NOT NULL REFERENCES inventario_jornada(id),
  cantidad               NUMERIC(14,3) NOT NULL CHECK (cantidad > 0),
  estado                 estado_reserva NOT NULL DEFAULT 'ACTIVA',
  reservada_en           TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
  finalizada_en          TIMESTAMPTZ,
  UNIQUE (pedido_id, inventario_jornada_id),
  CONSTRAINT ck_reserva_finalizacion CHECK (
    (estado = 'ACTIVA' AND finalizada_en IS NULL)
    OR (estado IN ('CONSUMIDA', 'LIBERADA') AND finalizada_en IS NOT NULL AND finalizada_en >= reservada_en)
  )
);

CREATE TABLE movimiento_inventario (
  id                     BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
  inventario_jornada_id  BIGINT NOT NULL REFERENCES inventario_jornada(id),
  reserva_id             BIGINT REFERENCES reserva_inventario(id),
  realizado_por          BIGINT NOT NULL REFERENCES usuario(id),
  tipo                   tipo_movimiento_inventario NOT NULL,
  cantidad               NUMERIC(14,3) NOT NULL CHECK (cantidad > 0),
  motivo                 VARCHAR(300) NOT NULL,
  creado_en              TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE historial_estado_pedido (
  id                  BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
  pedido_id           BIGINT NOT NULL REFERENCES pedido(id),
  estado_anterior     estado_pedido,
  estado_nuevo        estado_pedido NOT NULL,
  cambiado_por        BIGINT REFERENCES usuario(id),
  origen              VARCHAR(30) NOT NULL CHECK (origen IN ('USUARIO', 'PROCESO')),
  observacion         VARCHAR(500),
  creado_en           TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE domicilio (
  id                    BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
  pedido_id             BIGINT NOT NULL UNIQUE REFERENCES pedido(id),
  zona_domicilio_id     BIGINT NOT NULL REFERENCES zona_domicilio(id),
  domiciliario_id       BIGINT REFERENCES usuario(id),
  direccion_texto       VARCHAR(300) NOT NULL,
  zona_nombre_snapshot  VARCHAR(100) NOT NULL,
  tarifa_aplicada       NUMERIC(14,2) NOT NULL CHECK (tarifa_aplicada >= 0),
  estado                estado_domicilio NOT NULL DEFAULT 'PENDIENTE',
  observaciones         VARCHAR(500),
  creado_en             TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
  actualizado_en        TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE intento_entrega (
  id                    BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
  domicilio_id          BIGINT NOT NULL REFERENCES domicilio(id),
  numero_intento        SMALLINT NOT NULL CHECK (numero_intento > 0),
  domiciliario_id       BIGINT NOT NULL REFERENCES usuario(id),
  resultado             resultado_entrega,
  motivo_no_entrega     VARCHAR(300),
  costo_adicional       NUMERIC(14,2) NOT NULL DEFAULT 0 CHECK (costo_adicional >= 0),
  salida_en             TIMESTAMPTZ NOT NULL,
  resultado_en          TIMESTAMPTZ,
  UNIQUE (domicilio_id, numero_intento),
  CONSTRAINT ck_intento_fechas CHECK (resultado_en IS NULL OR resultado_en >= salida_en),
  CONSTRAINT ck_intento_resultado CHECK (
    (resultado IS NULL AND resultado_en IS NULL AND motivo_no_entrega IS NULL)
    OR (resultado = 'ENTREGADO' AND resultado_en IS NOT NULL AND motivo_no_entrega IS NULL)
    OR (resultado = 'NO_ENTREGADO' AND resultado_en IS NOT NULL AND motivo_no_entrega IS NOT NULL)
  )
);

CREATE TABLE incidencia (
  id                  BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
  pedido_id           BIGINT NOT NULL REFERENCES pedido(id),
  tipo                tipo_incidencia NOT NULL,
  atribucion          atribucion_incidencia NOT NULL,
  descripcion         VARCHAR(800) NOT NULL,
  creada_por          BIGINT NOT NULL REFERENCES usuario(id),
  creada_en           TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
  cerrada_en          TIMESTAMPTZ,
  CONSTRAINT ck_incidencia_fechas CHECK (cerrada_en IS NULL OR cerrada_en >= creada_en)
);

CREATE TABLE resolucion_incidencia (
  id                  BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
  incidencia_id       BIGINT NOT NULL REFERENCES incidencia(id),
  tipo                tipo_resolucion NOT NULL,
  producto_sustituto_id BIGINT REFERENCES producto(id),
  descripcion         VARCHAR(800) NOT NULL,
  resuelta_por        BIGINT NOT NULL REFERENCES usuario(id),
  creada_en           TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE devolucion (
  id                    BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
  pedido_id             BIGINT NOT NULL REFERENCES pedido(id),
  incidencia_id         BIGINT REFERENCES incidencia(id),
  autorizada_por        BIGINT REFERENCES usuario(id),
  tipo                  tipo_devolucion NOT NULL,
  estado                estado_devolucion NOT NULL DEFAULT 'SOLICITADA',
  motivo                VARCHAR(500) NOT NULL,
  monto                 NUMERIC(14,2) NOT NULL CHECK (monto > 0),
  medio_pago_original   medio_pago NOT NULL,
  medio_devolucion      medio_pago,
  referencia_pago       VARCHAR(100),
  referencia_devolucion VARCHAR(100),
  solicitada_en         TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
  autorizada_en         TIMESTAMPTZ,
  ejecutada_en          TIMESTAMPTZ
);

CREATE TABLE gasto (
  id                  BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
  jornada_id          BIGINT NOT NULL REFERENCES jornada(id),
  registrado_por      BIGINT NOT NULL REFERENCES usuario(id),
  concepto            VARCHAR(200) NOT NULL,
  monto               NUMERIC(14,2) NOT NULL CHECK (monto > 0),
  medio               medio_pago NOT NULL,
  referencia          VARCHAR(100),
  creado_en           TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE cierre_jornada (
  id                       BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
  jornada_id               BIGINT NOT NULL UNIQUE REFERENCES jornada(id),
  aprobado_por             BIGINT NOT NULL REFERENCES usuario(id),
  ventas_brutas            NUMERIC(14,2) NOT NULL CHECK (ventas_brutas >= 0),
  devoluciones_ejecutadas  NUMERIC(14,2) NOT NULL CHECK (devoluciones_ejecutadas >= 0),
  ventas_netas             NUMERIC(14,2) NOT NULL CHECK (ventas_netas >= 0),
  gastos_totales           NUMERIC(14,2) NOT NULL CHECK (gastos_totales >= 0),
  domicilios_cobrados      NUMERIC(14,2) NOT NULL CHECK (domicilios_cobrados >= 0),
  observaciones            VARCHAR(1000),
  aprobado_en              TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
  CONSTRAINT ck_cierre_venta_neta CHECK (ventas_netas = ventas_brutas - devoluciones_ejecutadas)
);

CREATE TABLE diferencia_caja (
  id                  BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
  cierre_jornada_id   BIGINT NOT NULL REFERENCES cierre_jornada(id),
  medio               medio_pago NOT NULL,
  valor_esperado      NUMERIC(14,2) NOT NULL,
  valor_observado     NUMERIC(14,2) NOT NULL,
  diferencia          NUMERIC(14,2) NOT NULL,
  justificacion       VARCHAR(800),
  autorizada_por      BIGINT REFERENCES usuario(id),
  CONSTRAINT ck_diferencia_calculo CHECK (diferencia = valor_observado - valor_esperado),
  UNIQUE (cierre_jornada_id, medio)
);

CREATE TABLE clave_idempotencia (
  id                  BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
  usuario_id          BIGINT NOT NULL REFERENCES usuario(id),
  operacion           VARCHAR(60) NOT NULL,
  clave               VARCHAR(120) NOT NULL,
  pedido_id           BIGINT REFERENCES pedido(id),
  hash_solicitud      VARCHAR(128) NOT NULL,
  codigo_respuesta    SMALLINT,
  respuesta           JSONB,
  creada_en           TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
  expira_en           TIMESTAMPTZ NOT NULL,
  UNIQUE (usuario_id, operacion, clave),
  CONSTRAINT ck_idempotencia_expiracion CHECK (expira_en > creada_en),
  CONSTRAINT ck_idempotencia_respuesta CHECK (codigo_respuesta IS NULL OR codigo_respuesta BETWEEN 100 AND 599)
);

CREATE TABLE auditoria (
  id                  BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
  usuario_id          BIGINT REFERENCES usuario(id),
  accion              VARCHAR(100) NOT NULL,
  entidad             VARCHAR(80) NOT NULL,
  entidad_id          VARCHAR(80) NOT NULL,
  datos_anteriores    JSONB,
  datos_nuevos        JSONB,
  direccion_ip        INET,
  creado_en           TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- Restricciones cruzadas que requieren consultar otras tablas
CREATE OR REPLACE FUNCTION fn_validar_pago_pedido()
RETURNS TRIGGER
LANGUAGE plpgsql
AS $$
DECLARE
  v_total pedido.total%TYPE;
  v_estado pedido.estado%TYPE;
BEGIN
  SELECT total, estado INTO STRICT v_total, v_estado
  FROM pedido
  WHERE id = NEW.pedido_id;

  IF NEW.tipo = 'PEDIDO' AND NEW.monto_pagado <> v_total THEN
    RAISE EXCEPTION 'El pago principal debe cubrir exactamente el total del pedido';
  END IF;

  IF NEW.tipo = 'DOMICILIO_ADICIONAL' AND v_estado <> 'NO_ENTREGADO' THEN
    RAISE EXCEPTION 'Un domicilio adicional solo puede cobrarse para un pedido no entregado';
  END IF;

  RETURN NEW;
END;
$$;

CREATE TRIGGER trg_validar_pago_pedido
BEFORE INSERT OR UPDATE ON pago
FOR EACH ROW EXECUTE FUNCTION fn_validar_pago_pedido();

CREATE OR REPLACE FUNCTION fn_validar_domicilio_pedido()
RETURNS TRIGGER
LANGUAGE plpgsql
AS $$
DECLARE
  v_modalidad pedido.modalidad%TYPE;
  v_sede_pedido BIGINT;
  v_sede_zona BIGINT;
BEGIN
  SELECT p.modalidad, j.sede_id INTO STRICT v_modalidad, v_sede_pedido
  FROM pedido p
  JOIN jornada j ON j.id = p.jornada_id
  WHERE p.id = NEW.pedido_id;

  SELECT sede_id INTO STRICT v_sede_zona
  FROM zona_domicilio
  WHERE id = NEW.zona_domicilio_id;

  IF v_modalidad <> 'DOMICILIO' THEN
    RAISE EXCEPTION 'Solo un pedido de modalidad DOMICILIO puede tener domicilio';
  END IF;

  IF v_sede_pedido <> v_sede_zona THEN
    RAISE EXCEPTION 'La zona de domicilio debe pertenecer a la sede del pedido';
  END IF;

  RETURN NEW;
END;
$$;

CREATE TRIGGER trg_validar_domicilio_pedido
BEFORE INSERT OR UPDATE ON domicilio
FOR EACH ROW EXECUTE FUNCTION fn_validar_domicilio_pedido();

CREATE OR REPLACE FUNCTION fn_validar_saldo_devolucion()
RETURNS TRIGGER
LANGUAGE plpgsql
AS $$
DECLARE
  v_pago_principal pago.monto_pagado%TYPE;
  v_reembolsado NUMERIC(14,2);
  v_saldo NUMERIC(14,2);
BEGIN
  SELECT monto_pagado INTO STRICT v_pago_principal
  FROM pago
  WHERE pedido_id = NEW.pedido_id AND tipo = 'PEDIDO';

  SELECT COALESCE(SUM(monto), 0) INTO v_reembolsado
  FROM devolucion
  WHERE pedido_id = NEW.pedido_id
    AND estado IN ('APROBADA', 'EJECUTADA')
    AND id IS DISTINCT FROM NEW.id;

  v_saldo := v_pago_principal - v_reembolsado;

  IF NEW.estado IN ('APROBADA', 'EJECUTADA') AND NEW.monto > v_saldo THEN
    RAISE EXCEPTION 'La devolución supera el saldo reembolsable del pedido';
  END IF;

  IF NEW.estado IN ('APROBADA', 'EJECUTADA') AND NEW.tipo = 'TOTAL' AND NEW.monto <> v_saldo THEN
    RAISE EXCEPTION 'La devolución total debe cubrir el saldo reembolsable';
  END IF;

  RETURN NEW;
END;
$$;

CREATE TRIGGER trg_validar_saldo_devolucion
BEFORE INSERT OR UPDATE ON devolucion
FOR EACH ROW EXECUTE FUNCTION fn_validar_saldo_devolucion();

-- Índices de consulta y operación
CREATE INDEX ix_producto_categoria_activo ON producto (categoria_id, activo);
CREATE INDEX ix_producto_insumo_insumo ON producto_insumo (insumo_id);
CREATE INDEX ix_solicitud_jornada_estado ON solicitud (jornada_id, estado, creada_en);
CREATE INDEX ix_solicitud_nombre_fecha ON solicitud (nombre_cliente, creada_en DESC);
CREATE INDEX ix_pedido_jornada_estado ON pedido (jornada_id, estado, creado_en);
CREATE INDEX ix_pedido_fecha_modalidad ON pedido (creado_en DESC, modalidad);
CREATE INDEX ix_pedido_nombre_fecha ON pedido (nombre_cliente, creado_en DESC);
CREATE INDEX ix_pedido_detalle_producto ON pedido_detalle (producto_id, pedido_id);
CREATE INDEX ix_historial_pedido_fecha ON historial_estado_pedido (pedido_id, creado_en);
CREATE INDEX ix_reserva_inventario_estado ON reserva_inventario (inventario_jornada_id, estado);
CREATE INDEX ix_movimiento_inventario_fecha ON movimiento_inventario (inventario_jornada_id, creado_en);
CREATE INDEX ix_domicilio_estado_domiciliario ON domicilio (estado, domiciliario_id);
CREATE INDEX ix_intento_entrega_fecha ON intento_entrega (domicilio_id, resultado_en DESC);
CREATE INDEX ix_incidencia_pedido_fecha ON incidencia (pedido_id, creada_en DESC);
CREATE INDEX ix_devolucion_pedido_estado ON devolucion (pedido_id, estado, solicitada_en);
CREATE INDEX ix_pago_fecha_medio ON pago (confirmado_en, medio);
CREATE INDEX ix_gasto_jornada_medio ON gasto (jornada_id, medio);
CREATE INDEX ix_auditoria_entidad ON auditoria (entidad, entidad_id, creado_en DESC);
CREATE INDEX ix_auditoria_usuario_fecha ON auditoria (usuario_id, creado_en DESC);
CREATE INDEX ix_idempotencia_expiracion ON clave_idempotencia (expira_en);

-- Datos iniciales mínimos de roles
INSERT INTO rol (codigo, nombre) VALUES
  ('PROPIETARIO', 'Propietario o administrador'),
  ('CAJA', 'Responsable de caja'),
  ('COCINA', 'Cocina, acceso de consulta'),
  ('DESPACHO', 'Responsable de despacho'),
  ('DOMICILIARIO', 'Domiciliario');

COMMIT;
