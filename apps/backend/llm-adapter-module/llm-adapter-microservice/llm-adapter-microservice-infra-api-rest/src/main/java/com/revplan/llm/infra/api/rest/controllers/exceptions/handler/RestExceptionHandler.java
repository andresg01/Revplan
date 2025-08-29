///*******************************************************************************
// *
// * Autor: agarciab
// *
// * © Axpe Consulting S.L. 2025. Todos los derechos reservados.
// *
// ******************************************************************************/
//
//package com.revplan.llm.infra.api.rest.controllers.exceptions.handler;
//
//import static com.revplan.llm.domain.exceptions.TypeErrorEnum.ERROR;
//import static com.revplan.llm.domain.exceptions.TypeErrorEnum.FATAL;
//import static org.springframework.http.HttpStatus.BAD_REQUEST;
//import static org.springframework.http.HttpStatus.INTERNAL_SERVER_ERROR;
//
//import java.util.ArrayList;
//import java.util.Arrays;
//import java.util.HashSet;
//import java.util.List;
//import java.util.Set;
//
//import jakarta.servlet.http.HttpServletRequest;
//import jakarta.validation.ConstraintViolation;
//import jakarta.validation.ConstraintViolationException;
//
//import org.springframework.http.HttpStatus;
//import org.springframework.http.MediaType;
//import org.springframework.http.ResponseEntity;
//import org.springframework.validation.BindException;
//import org.springframework.web.bind.MethodArgumentNotValidException;
//import org.springframework.web.bind.annotation.ControllerAdvice;
//import org.springframework.web.bind.annotation.ExceptionHandler;
//import org.springframework.web.bind.annotation.RequestMapping;
//import org.springframework.web.bind.annotation.ResponseBody;
//import org.springframework.web.bind.annotation.ResponseStatus;
//import org.springframework.web.client.HttpClientErrorException;
//import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
//
//import com.revplan.llm.domain.exceptions.BusinessException;
//import com.revplan.llm.infra.api.dto.ErrorDTO;
//import com.revplan.llm.infra.api.dto.ErrorListDTO;
//
//import lombok.extern.slf4j.Slf4j;
//
//@ControllerAdvice(basePackages = "com.revplan.llm.infra.api.rest.controllers")
//@RequestMapping(produces = MediaType.APPLICATION_JSON_VALUE)
//@Slf4j
//public class RestExceptionHandler {
//
//	/**
//	 * HttpClientErrorException handler, return rich information error.
//	 *
//	 * @param request a {@link HttpServletRequest} object.
//	 * @param ex      a {@link HttpClientErrorException} object.
//	 * @return a {@link ResponseEntity} object.
//	 */
//	@ExceptionHandler(HttpClientErrorException.class)
//	@ResponseBody
//	public ResponseEntity<ErrorListDTO> httpClientErrorHandler(HttpServletRequest request,
//			HttpClientErrorException ex) {
//	    String errorCode = ex.getStatusCode()
//                .toString();
//
//        switch (HttpStatus.valueOf(ex.getStatusCode()
//                .value())) {
//            case UNAUTHORIZED:
//                return error(ex, HttpStatus.valueOf(ex.getStatusCode()
//                        .value()), errorCode, ERROR
//                        .name(), HttpStatus.UNAUTHORIZED.getReasonPhrase(), new Object[] { null });
//            case BAD_REQUEST:
//                return error(ex, HttpStatus.valueOf(ex.getStatusCode()
//                        .value()), errorCode, ERROR.name(), HttpStatus.BAD_REQUEST
//                        .getReasonPhrase(), new Object[] { null });
//            case NOT_FOUND:
//                return error(ex, HttpStatus.valueOf(ex.getStatusCode()
//                        .value()), errorCode, ERROR.name(), HttpStatus.NOT_FOUND
//                        .getReasonPhrase(), new Object[] { null });
//            case TOO_MANY_REQUESTS:
//                return error(ex, HttpStatus.valueOf(ex.getStatusCode()
//                        .value()), errorCode, ERROR
//                        .name(), HttpStatus.TOO_MANY_REQUESTS
//                                .getReasonPhrase(), new Object[] { null });
//            case SERVICE_UNAVAILABLE:
//                return error(ex, HttpStatus.valueOf(ex.getStatusCode()
//                        .value()), errorCode, ERROR
//                        .name(), HttpStatus.SERVICE_UNAVAILABLE
//                                .getReasonPhrase(), new Object[] { null });
//            case CONFLICT:
//                return error(ex, INTERNAL_SERVER_ERROR, errorCode, ERROR.name(), ex
//                        .getStatusText(), new Object[] { null });
//            default:
//                return error(ex, INTERNAL_SERVER_ERROR, errorCode, ERROR
//                        .name(), HttpStatus.INTERNAL_SERVER_ERROR
//                                .getReasonPhrase(), new Object[] { null });
//        }
//	}
//
//	/**
//	 * Constraint violation exception handler, return rich information error.
//	 *
//	 * @param ex a {@link ConstraintViolationException} object.
//	 * @return a {@link ResponseEntity} object.
//	 */
//	@ExceptionHandler(value = { ConstraintViolationException.class })
//	@ResponseStatus(HttpStatus.BAD_REQUEST)
//	public ResponseEntity<ErrorListDTO> constraintValidationExceptionHandler(ConstraintViolationException ex) {
//		Set<ConstraintViolation<?>> listErrors = ex.getConstraintViolations() == null ? new HashSet<>()
//				: ex.getConstraintViolations();
//		log.error(ex.getLocalizedMessage());
//		String errorCode = Integer.toString(BAD_REQUEST.value());
//		return error(ex, HttpStatus.BAD_REQUEST, errorCode, FATAL.name(), HttpStatus.BAD_REQUEST.getReasonPhrase(),
//				listErrors.stream().map(x -> {
//					String result = "null";
//					if (x != null) {
//						result = x.getMessage();
//					}
//					return result;
//				}).toArray());
//	}
//
//	/**
//	 * Capture a methodArgumentNotValidException and returned rich information
//	 * error.
//	 *
//	 * @param ex a {@link MethodArgumentNotValidException} object.
//	 * @return a {@link ResponseEntity} object.
//	 */
//	@ResponseStatus(HttpStatus.BAD_REQUEST)
//	@ExceptionHandler(value = { MethodArgumentNotValidException.class })
//	public ResponseEntity<ErrorListDTO> methodArgumentNotValidExceptionHandler(MethodArgumentNotValidException ex) {
//		log.error(ex.getLocalizedMessage());
//		String errorCode = Integer.toString(BAD_REQUEST.value());
//		return error(ex, HttpStatus.BAD_REQUEST, errorCode, FATAL.name(), HttpStatus.BAD_REQUEST.getReasonPhrase(),
//				ex.getBindingResult().getAllErrors().stream().map(x -> {
//					String result = "null";
//					if (x != null) {
//						result = x.getDefaultMessage();
//					}
//					return result;
//				}).toArray());
//	}
//
//	/**
//	 * Capture business exception
//	 *
//	 * @param ex a {@link BusinessException} object.
//	 * @return a object.
//	 */
//	@ExceptionHandler(BusinessException.class)
//	public ResponseEntity<ErrorListDTO> handleBusinessException(BusinessException ex) {
//		log.error(ex.toString());
//
//		HttpStatus status = HttpStatus.INTERNAL_SERVER_ERROR;
//
//		if (ex != null) {
//			switch (ex.getErrorCode()) {
//			case NOT_EXIST:
//				status = HttpStatus.NOT_FOUND;
//				break;
//			default:
//				status = HttpStatus.INTERNAL_SERVER_ERROR;
//			}
//		}
//
//		return error(ex, status, ex.getErrorCode().getCode(), ERROR.name(), ex.getDescription(),
//				new Object[] { ex.getDescription() });
//	}
//
//	/**
//	 * Capture when an Exception type exception is thrown and masks the information
//	 * returned.
//	 *
//	 * @param request a {@link HttpServletRequest} object.
//	 * @param ex      a {@link Exception} exception thrown
//	 * @return a {@link ResponseEntity} object.
//	 */
//	@ExceptionHandler(Exception.class)
//	public ResponseEntity<ErrorListDTO> genericExceptionHandler(HttpServletRequest request, Exception ex) {
//		if (request != null && ex != null) {
//			log.error("Exception Occurred:: URL:{" + request.getRequestURL().toString() + "}, " + "class:{"
//					+ ex.getClass() + "}," + " error:{" + ex.getLocalizedMessage() + "}");
//		}
//		String errorCode = Integer.toString(INTERNAL_SERVER_ERROR.value());
//		log.error(ex == null ? "Exception null" : ex.getLocalizedMessage());
//		return error(ex, INTERNAL_SERVER_ERROR, errorCode, FATAL.name(),
//				HttpStatus.INTERNAL_SERVER_ERROR.getReasonPhrase(), new Object[0]);
//	}
//
//	/**
//	 * <p>
//	 * bindExceptionHandler.
//	 * </p>
//	 *
//	 * @param ex a {@link MethodArgumentNotValidException} object.
//	 * @return a {@link ResponseEntity} object.
//	 */
//	@ResponseStatus(HttpStatus.BAD_REQUEST)
//	@ExceptionHandler(BindException.class)
//	public ResponseEntity<ErrorListDTO> bindExceptionHandler(BindException ex) {
//		log.error(ex.getLocalizedMessage());
//		String errorCode = Integer.toString(BAD_REQUEST.value());
//		return error(ex, HttpStatus.BAD_REQUEST, errorCode, FATAL.name(), HttpStatus.BAD_REQUEST.getReasonPhrase(),
//				ex.getBindingResult().getAllErrors().stream().map(x -> {
//					String result = "null";
//					if (x != null) {
//						result = x.getDefaultMessage();
//					}
//					return result;
//				}).toArray());
//	}
//
//	/**
//	 * Constraint violation exception handler, return rich information error.
//	 *
//	 * @param matme a {@link MethodArgumentTypeMismatchException} object.
//	 * @return a {@link ResponseEntity} object.
//	 */
//	@ExceptionHandler(value = { MethodArgumentTypeMismatchException.class })
//	@ResponseStatus(HttpStatus.BAD_REQUEST)
//	public ResponseEntity<ErrorListDTO> methodMethodArgumentTypeMismatchExceptionHandler(
//			MethodArgumentTypeMismatchException matme) {
//
//		String[] listErrors = {
//				String.format("Parameter %s: Parse attempt failed for value %s", matme.getName(), matme.getValue()) };
//
//		log.error(matme.getLocalizedMessage());
//		String errorCode = Integer.toString(BAD_REQUEST.value());
//		return error(matme, HttpStatus.BAD_REQUEST, errorCode, FATAL.name(), HttpStatus.BAD_REQUEST.getReasonPhrase(),
//				listErrors);
//	}
//
//	/**
//	 * Generate object response error with info like code, message, level and
//	 * description data.
//	 *
//	 * @param exception   a {@link Exception} object.
//	 * @param httpStatus  a {@link HttpStatus} object
//	 * @param code        a {@link String} object
//	 * @param message     a {@link String} object
//	 * @param level       a {@link String} object
//	 * @param description a {@link String} object
//	 * @param args        a {@link Object[]} object
//	 * @return a {@link ResponseEntity} object.
//	 */
//	private ResponseEntity<ErrorListDTO> error(final Exception exception, final HttpStatus httpStatus,
//			final String code, final String level, final String description, Object[] args) {
//		List<ErrorDTO> errors = new ArrayList<>();
//		Arrays.stream(args).forEach(x -> errors.add(new ErrorDTO(code, x.toString(), level)));
//		log.error(exception == null ? "Exception null" : exception.getMessage());
//		return new ResponseEntity<>(new ErrorListDTO(errors), httpStatus);
//	}
//
//}
