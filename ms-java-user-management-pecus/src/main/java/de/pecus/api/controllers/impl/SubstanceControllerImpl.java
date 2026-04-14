package de.pecus.api.controllers.impl;

import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import de.pecus.api.controllers.SubstanceController;
import de.pecus.api.services.usuarios.SubstanceService;
import de.pecus.api.util.RequestVOUtil;
import de.pecus.api.util.ResponseUtil;
import de.pecus.api.util.SubstanceServicesResponseBuilder;
import de.pecus.api.vo.RequestVO;
import de.pecus.api.vo.ResponseVO;
import de.pecus.api.vo.substance.CreateSubstanceRequestVO;
import de.pecus.api.vo.substance.DeleteSubstanceRequestVO;
import de.pecus.api.vo.substance.FindDetailSubstanceRequestVO;
import de.pecus.api.vo.substance.FindDetailSubstanceResponseVO;
import de.pecus.api.vo.substance.FindListSubstanceRequestVO;
import de.pecus.api.vo.substance.FindListSubstanceResponseVO;
import de.pecus.api.vo.substance.UpdateSubstanceRequestVO;
import io.swagger.annotations.ApiImplicitParam;
import io.swagger.annotations.ApiImplicitParams;

/**
 * Clase de controlador para servicio Rest de Substance.
 * Expone los endpoints tanto en /substance como en /ingredient (alias)
 * para compatibilidad con el frontend HackTheSnack.
 * 
 * @author Jorge Serrano
 * @version 1.0
 * @created 21-junio-2019 11:34:31 a. m.
 */
@RestController
@RequestMapping("")
public class SubstanceControllerImpl implements SubstanceController {

	@Autowired
	private SubstanceService substanceService;

	/**
	 * Operation: create, Method: POST
	 * Crea un registro en la BB.DD.
	 * Disponible en /substance y /ingredient
	 */
	@Override
	@ApiImplicitParams({
		@ApiImplicitParam(name = "Authorization", value = "eyJhbGciOiJIUzI1NiJ9.eyJUT0tFTl9EQVRBIjp7ImFwZWxs", paramType = "header", dataTypeClass = String.class),
		@ApiImplicitParam(name = "mac-address", value = " 2C:54:91:88:C9:E3 or 2c-54-91-88-c9-e3", paramType = "header", dataTypeClass = String.class),
		@ApiImplicitParam(name = "request-date", value = "2021-09-07 17:29:25.443", paramType = "header", dataTypeClass = String.class, required = true),
		@ApiImplicitParam(name = "latitude", value = "-74.00898606", paramType = "header", dataTypeClass = String.class),
		@ApiImplicitParam(name = "longitude", value = "40.71727401", paramType = "header", dataTypeClass = String.class),
		@ApiImplicitParam(name = "language", value = "es_MX", paramType = "header", dataTypeClass = String.class, required = true),
		@ApiImplicitParam(name = "time-zone", value = "America/Mexico City", paramType = "header", dataTypeClass = String.class),
		@ApiImplicitParam(name = "id-client-invoke", value = "21", paramType = "header", dataTypeClass = String.class, required = true),
		@ApiImplicitParam(name = "client-operation-code", value = "qWERGFDSRSGfsdertRTRe2345RTd", paramType = "header", dataTypeClass = String.class, required = true) })
	@PostMapping(value = {"/substance", "/ingredient"}, produces = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<ResponseVO<Long>> createSubstance(@RequestHeader Map<String, String> headers,
			@RequestBody CreateSubstanceRequestVO body) {
		
			// Generamos el objeto requestVO
			RequestVO<CreateSubstanceRequestVO> requestVO = RequestVOUtil.setNewRequestVO(headers,body);

			ResponseEntity<ResponseVO<Long>> response = null;
			try {
				// Invocar al servicio
				ResponseVO<Long> serviceResponse = substanceService.create(requestVO);

				response  = SubstanceServicesResponseBuilder.buildCreateOrUpdateResponse(serviceResponse);

			} catch (Exception exception) {
				// Excepcion controlada
				ResponseVO<Long> exceptionResponse = ResponseUtil.getErrorResponse(exception);
				response = ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(exceptionResponse);
			}
			return response;
			
	}
	
	/**
	 * Operation: update, Method: PUT
	 * Actualiza un registro en la BB.DD.
	 * Disponible en /substance/{id} y /ingredient/{id}
	 */
	@Override
	@ApiImplicitParams({
		@ApiImplicitParam(name = "Authorization", value = "eyJhbGciOiJIUzI1NiJ9.eyJUT0tFTl9EQVRBIjp7ImFwZWxs", paramType = "header", dataTypeClass = String.class),
		@ApiImplicitParam(name = "mac-address", value = " 2C:54:91:88:C9:E3 or 2c-54-91-88-c9-e3", paramType = "header", dataTypeClass = String.class),
		@ApiImplicitParam(name = "request-date", value = "2021-09-07 17:29:25.443", paramType = "header", dataTypeClass = String.class, required = true),
		@ApiImplicitParam(name = "latitude", value = "-74.00898606", paramType = "header", dataTypeClass = String.class),
		@ApiImplicitParam(name = "longitude", value = "40.71727401", paramType = "header", dataTypeClass = String.class),
		@ApiImplicitParam(name = "language", value = "es_MX", paramType = "header", dataTypeClass = String.class, required = true),
		@ApiImplicitParam(name = "time-zone", value = "America/Mexico City", paramType = "header", dataTypeClass = String.class),
		@ApiImplicitParam(name = "id-client-invoke", value = "21", paramType = "header", dataTypeClass = String.class, required = true),
		@ApiImplicitParam(name = "client-operation-code", value = "qWERGFDSRSGfsdertRTRe2345RTd", paramType = "header", dataTypeClass = String.class, required = true) })
	@PutMapping(value = {"/substance/{id}", "/ingredient/{id}"}, produces = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<ResponseVO<Long>> updateSubstance(@RequestHeader Map<String, String> headers, 
			@PathVariable(name = "id", required = false) Long id, 
			@RequestBody UpdateSubstanceRequestVO body) {
		
		
			// Declaracion de variable
			ResponseEntity<ResponseVO<Long>> response = null;

			body.setId(id);

			// Generamos el objeto requestVO
			RequestVO<UpdateSubstanceRequestVO> requestVO = RequestVOUtil.setNewRequestVO(headers,body);
			
			try {
				// Invocar al servicio
				ResponseVO<Long> serviceResponse = substanceService.update(requestVO);

				response = SubstanceServicesResponseBuilder.buildCreateOrUpdateResponse(serviceResponse);

			} catch (Exception exception) {
				// Excepcion controlada
				ResponseVO<Long> exceptionResponse = ResponseUtil.getErrorResponse(exception);
				response = ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(exceptionResponse);
			}
			return response;
			
	}
	
	/**
	 * Operation: delete, Method: DELETE
	 * Elimina un registro en la BB.DD.
	 * Disponible en /substance/{id} y /ingredient/{id}
	 */
	@Override
	@ApiImplicitParams({
		@ApiImplicitParam(name = "Authorization", value = "eyJhbGciOiJIUzI1NiJ9.eyJUT0tFTl9EQVRBIjp7ImFwZWxs", paramType = "header", dataTypeClass = String.class),
		@ApiImplicitParam(name = "mac-address", value = " 2C:54:91:88:C9:E3 or 2c-54-91-88-c9-e3", paramType = "header", dataTypeClass = String.class),
		@ApiImplicitParam(name = "request-date", value = "2021-09-07 17:29:25.443", paramType = "header", dataTypeClass = String.class, required = true),
		@ApiImplicitParam(name = "latitude", value = "-74.00898606", paramType = "header", dataTypeClass = String.class),
		@ApiImplicitParam(name = "longitude", value = "40.71727401", paramType = "header", dataTypeClass = String.class),
		@ApiImplicitParam(name = "language", value = "es_MX", paramType = "header", dataTypeClass = String.class, required = true),
		@ApiImplicitParam(name = "time-zone", value = "America/Mexico City", paramType = "header", dataTypeClass = String.class),
		@ApiImplicitParam(name = "id-client-invoke", value = "21", paramType = "header", dataTypeClass = String.class, required = true),
		@ApiImplicitParam(name = "client-operation-code", value = "qWERGFDSRSGfsdertRTRe2345RTd", paramType = "header", dataTypeClass = String.class, required = true) })
	@DeleteMapping(value = {"/substance/{id}", "/ingredient/{id}"}, produces = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<ResponseVO<Boolean>> deleteSubstance(@RequestHeader Map<String, String> headers,
			@PathVariable("id") Long id) {
		// Declaracion de variables
		ResponseEntity<ResponseVO<Boolean>> response = null;
		
		// Generamos el objeto requestVO
		DeleteSubstanceRequestVO deleteSubstanceRequestVO = new DeleteSubstanceRequestVO();
		deleteSubstanceRequestVO.setId(id);
		RequestVO<DeleteSubstanceRequestVO> requestVO = RequestVOUtil.setNewRequestVO(headers,deleteSubstanceRequestVO);
		

		try {
			
			ResponseVO<Boolean> serviceResponse = substanceService.delete(requestVO);

			response = SubstanceServicesResponseBuilder.buildDeleteResponse(serviceResponse);

		} catch (Exception exception) {
			// Excepcion no controlada
			ResponseVO<Boolean> exceptionResponse = ResponseUtil.getErrorResponse(exception);
			response = ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(exceptionResponse);
		}
		return response;
	}

	/**
	 * Operation: findDetail, Method: GET
	 * Busca el detalle de un registro en la base de datos.
	 * Disponible en /substance/detail y /ingredient/detail
	 */
	@Override
	@ApiImplicitParams({
		@ApiImplicitParam(name = "Authorization", value = "eyJhbGciOiJIUzI1NiJ9.eyJUT0tFTl9EQVRBIjp7ImFwZWxs", paramType = "header", dataTypeClass = String.class),
		@ApiImplicitParam(name = "mac-address", value = " 2C:54:91:88:C9:E3 or 2c-54-91-88-c9-e3", paramType = "header", dataTypeClass = String.class),
		@ApiImplicitParam(name = "request-date", value = "2021-09-07 17:29:25.443", paramType = "header", dataTypeClass = String.class, required = true),
		@ApiImplicitParam(name = "latitude", value = "-74.00898606", paramType = "header", dataTypeClass = String.class),
		@ApiImplicitParam(name = "longitude", value = "40.71727401", paramType = "header", dataTypeClass = String.class),
		@ApiImplicitParam(name = "language", value = "es_MX", paramType = "header", dataTypeClass = String.class, required = true),
		@ApiImplicitParam(name = "time-zone", value = "America/Mexico City", paramType = "header", dataTypeClass = String.class),
		@ApiImplicitParam(name = "id-client-invoke", value = "21", paramType = "header", dataTypeClass = String.class, required = true),
		@ApiImplicitParam(name = "client-operation-code", value = "qWERGFDSRSGfsdertRTRe2345RTd", paramType = "header", dataTypeClass = String.class, required = true) })
	@GetMapping(value = {"/substance/detail", "/ingredient/detail"}, produces = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<ResponseVO<FindDetailSubstanceResponseVO>> findDetailSubstance(
			@RequestHeader Map<String, String> headers, 
			@RequestParam(value = "id", required= false) Long id,
			@RequestParam(value = "name", required = false) String name) {

		// Declaracion de variables
		ResponseEntity<ResponseVO<FindDetailSubstanceResponseVO>> response = null;

		// Crear el objeto requestVO
		FindDetailSubstanceRequestVO findDetailSubstanceRequestVO = new FindDetailSubstanceRequestVO();
		findDetailSubstanceRequestVO.setId(id);
		findDetailSubstanceRequestVO.setName(name);
		RequestVO<FindDetailSubstanceRequestVO> requestVO = RequestVOUtil.setNewRequestVO(headers,findDetailSubstanceRequestVO);

		try {
			
			// Invocar al metodo de busqueda
			ResponseVO<FindDetailSubstanceResponseVO> serviceResponse = substanceService.findDetail(requestVO);
			
			response = SubstanceServicesResponseBuilder.buildFindDetailResponse(serviceResponse);

		} catch (Exception exception) {
			// Excepcion no controlada
			ResponseVO<FindDetailSubstanceResponseVO> exceptionResponse = ResponseUtil.getErrorResponse(exception);
			response = ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(exceptionResponse);
		}
		return response;
	}
	
	/**
	 * Operation: findList, Method: GET
	 * Consulta la lista de registros con filtros.
	 * Disponible en /substance/list y /ingredient/list
	 */
	@Override
	@ApiImplicitParams({
		@ApiImplicitParam(name = "Authorization", value = "eyJhbGciOiJIUzI1NiJ9.eyJUT0tFTl9EQVRBIjp7ImFwZWxs", paramType = "header", dataTypeClass = String.class),
		@ApiImplicitParam(name = "mac-address", value = " 2C:54:91:88:C9:E3 or 2c-54-91-88-c9-e3", paramType = "header", dataTypeClass = String.class),
		@ApiImplicitParam(name = "request-date", value = "2021-09-07 17:29:25.443", paramType = "header", dataTypeClass = String.class, required = true),
		@ApiImplicitParam(name = "latitude", value = "-74.00898606", paramType = "header", dataTypeClass = String.class),
		@ApiImplicitParam(name = "longitude", value = "40.71727401", paramType = "header", dataTypeClass = String.class),
		@ApiImplicitParam(name = "language", value = "es_MX", paramType = "header", dataTypeClass = String.class, required = true),
		@ApiImplicitParam(name = "time-zone", value = "America/Mexico City", paramType = "header", dataTypeClass = String.class),
		@ApiImplicitParam(name = "id-client-invoke", value = "21", paramType = "header", dataTypeClass = String.class, required = true),
		@ApiImplicitParam(name = "client-operation-code", value = "qWERGFDSRSGfsdertRTRe2345RTd", paramType = "header", dataTypeClass = String.class, required = true) })
	@GetMapping(value = {"/substance/list", "/ingredient/list"}, produces = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<ResponseVO<List<FindListSubstanceResponseVO>>> findListSubstance(
			@RequestHeader Map<String, String> headers, 
			@RequestParam(value = "page", required = false) Integer page,
			@RequestParam(value = "size", required = false) Integer size,
			@RequestParam(value = "orderBy", required = false) String orderBy,
			@RequestParam(value = "orderType", required = false) String orderType,
			@RequestParam(value = "name", required = false) String name){
		// Declarar variables
		ResponseEntity<ResponseVO<List<FindListSubstanceResponseVO>>> response = null;

		FindListSubstanceRequestVO findListSubstanceRequestVO = new FindListSubstanceRequestVO();
		findListSubstanceRequestVO.setName(name);
		RequestVO<FindListSubstanceRequestVO> requestVO = RequestVOUtil.setNewRequestVO(headers, page, size, orderBy,
				orderType, findListSubstanceRequestVO);

		try {

			ResponseVO<List<FindListSubstanceResponseVO>> serviceResponse = substanceService.findList(requestVO);
			response = SubstanceServicesResponseBuilder.buildFindListSubstanceResponse(serviceResponse);

		} catch (Exception exception) {
			// Excepcion no controlada
			ResponseVO<List<FindListSubstanceResponseVO>> exceptionResponse = ResponseUtil
					.getErrorResponse(exception);
			response = ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(exceptionResponse);
		}
		return response;
	}

}
