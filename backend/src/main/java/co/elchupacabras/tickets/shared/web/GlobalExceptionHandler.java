package co.elchupacabras.tickets.shared.web;

import co.elchupacabras.tickets.shared.domain.AccesoDenegadoDominioException;
import co.elchupacabras.tickets.shared.domain.ConcurrenciaInventarioException;
import co.elchupacabras.tickets.shared.domain.ReglaNegocioException;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingRequestHeaderException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.OffsetDateTime;
import java.util.List;

/**
 * Manejador centralizado de errores (@RestControllerAdvice) que garantiza el contrato HTTP
 * del Taller 3 (T3-01): 400 Bad Request, 403 Forbidden, 409 Conflict y 422 Unprocessable Entity.
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiErrorResponse> handleValidation(MethodArgumentNotValidException ex) {
        List<String> errores = ex.getBindingResult().getAllErrors().stream()
                .map(err -> err instanceof FieldError fe
                        ? fe.getField() + ": " + fe.getDefaultMessage()
                        : err.getDefaultMessage())
                .toList();

        ApiErrorResponse body = new ApiErrorResponse(
                "DATOS_ENTRADA_INVALIDOS",
                "Los datos de la solicitud son inválidos o están incompletos",
                HttpStatus.BAD_REQUEST.value(),
                errores,
                OffsetDateTime.now()
        );
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(body);
    }

    @ExceptionHandler({MissingRequestHeaderException.class, IllegalArgumentException.class})
    public ResponseEntity<ApiErrorResponse> handleBadRequest(Exception ex) {
        ApiErrorResponse body = new ApiErrorResponse(
                "PETICION_MALFORMADA",
                ex.getMessage(),
                HttpStatus.BAD_REQUEST.value(),
                List.of(ex.getMessage()),
                OffsetDateTime.now()
        );
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(body);
    }

    @ExceptionHandler(AccesoDenegadoDominioException.class)
    public ResponseEntity<ApiErrorResponse> handleForbidden(AccesoDenegadoDominioException ex) {
        ApiErrorResponse body = new ApiErrorResponse(
                ex.getCodigo(),
                ex.getMessage(),
                HttpStatus.FORBIDDEN.value(),
                List.of(),
                OffsetDateTime.now()
        );
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(body);
    }

    @ExceptionHandler({ConcurrenciaInventarioException.class, OptimisticLockingFailureException.class})
    public ResponseEntity<ApiErrorResponse> handleConflict(Exception ex) {
        String codigo = ex instanceof ConcurrenciaInventarioException cie
                ? cie.getCodigo()
                : "CONFLICTO_CONCURRENCIA";
        ApiErrorResponse body = new ApiErrorResponse(
                codigo,
                ex.getMessage(),
                HttpStatus.CONFLICT.value(),
                List.of(),
                OffsetDateTime.now()
        );
        return ResponseEntity.status(HttpStatus.CONFLICT).body(body);
    }

    @ExceptionHandler(ReglaNegocioException.class)
    public ResponseEntity<ApiErrorResponse> handleUnprocessableEntity(ReglaNegocioException ex) {
        ApiErrorResponse body = new ApiErrorResponse(
                ex.getCodigo(),
                ex.getMessage(),
                HttpStatus.UNPROCESSABLE_ENTITY.value(),
                ex.getDetalles(),
                OffsetDateTime.now()
        );
        return ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY).body(body);
    }
}
