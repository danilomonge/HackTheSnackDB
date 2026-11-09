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

import de.pecus.api.controllers.BrandController;
import de.pecus.api.services.usuarios.BrandService;
import de.pecus.api.util.BrandServicesResponseBuilder;
import de.pecus.api.util.RequestVOUtil;
import de.pecus.api.util.ResponseUtil;
import de.pecus.api.vo.RequestVO;
import de.pecus.api.vo.ResponseVO;
import de.pecus.api.vo.brand.CreateBrandRequestVO;
import de.pecus.api.vo.brand.DeleteBrandRequestVO;
import de.pecus.api.vo.brand.FindDetailBrandRequestVO;
import de.pecus.api.vo.brand.FindDetailBrandResponseVO;
import de.pecus.api.vo.brand.FindListBrandRequestVO;
import de.pecus.api.vo.brand.FindListBrandResponseVO;
import de.pecus.api.vo.brand.UpdateBrandRequestVO;

/**
 * Clase de controlador para servicio Rest de Brand
 * 
 * @author Jorge Serrano
 * @version 1.0
 * @created 21-junio-2019 11:34:31 a. m.
 */
@RestController
@RequestMapping("")
public class BrandControllerImpl implements BrandController {

	@Autowired
	private BrandService brandService;

	/**
	 * Operation: Operation: create, Method: POST Crea un registro en la BB.DD.
	 * @param headers		Arreglo de objetos de tipo llave valor para almacenar los headers
	 * @param body 			Cuerpo de la peticion con la informacion del regsitro que se desean insertar
	 * @return 				Ojeto con el cuerpo de respuesta (ong con el id del registro actualizado) y le codigo de respuesta http corrspondiente a la operacion
	 */
	@Override
	@PostMapping(value = "/brand", produces = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<ResponseVO<Long>> createBrand(@RequestHeader Map<String, String> headers,
			@RequestBody CreateBrandRequestVO body) {
		
			// Generamos el objeto requestVO
			RequestVO<CreateBrandRequestVO> requestVO = RequestVOUtil.setNewRequestVO(headers,body);

			ResponseEntity<ResponseVO<Long>> response = null;
			try {
				// Invocar al servicio
				ResponseVO<Long> serviceResponse = brandService.create(requestVO);

				response  = BrandServicesResponseBuilder.buildCreateOrUpdateResponse(serviceResponse);

			} catch (Exception exception) {
				// Excepcion controlada
				ResponseVO<Long> exceptionResponse = ResponseUtil.getErrorResponse(exception);
				response = ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(exceptionResponse);
			}
			return response;
			
	}
	
	/**
	 * Operation: Operation: update, Method: POST Actualiza un registro en la BB.DD.
	 * @param headers		Arreglo de objetos de tipo llave valor para almacenar los headers
	 * @param id 			Identificador del registro a actualizar
	 * @param body 			Cuerpo de la peticion con la informacion del regsitro que se desean insertar
	 * @return 				Ojeto con el cuerpo de respuesta (ong con el id del registro actualizado) y le codigo de respuesta http corrspondiente a la operacion
	 */
	@Override
	@PutMapping(value = "/brand/{id}", produces = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<ResponseVO<Long>> updateBrand(@RequestHeader Map<String, String> headers, 
			@PathVariable(name = "id", required = false) Long id, 
			@RequestBody UpdateBrandRequestVO body) {
		
		
			// Declaracion de variable
			ResponseEntity<ResponseVO<Long>> response = null;

			body.setId(id);

			// Generamos el objeto requestVO
			RequestVO<UpdateBrandRequestVO> requestVO = RequestVOUtil.setNewRequestVO(headers,body);
			
			try {
				// Invocar al servicio
				ResponseVO<Long> serviceResponse = brandService.update(requestVO);

				response = BrandServicesResponseBuilder.buildCreateOrUpdateResponse(serviceResponse);

			} catch (Exception exception) {
				// Excepcion controlada
				ResponseVO<Long> exceptionResponse = ResponseUtil.getErrorResponse(exception);
				response = ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(exceptionResponse);
			}
			return response;
			
	}
	
	/**
	 * Operation: delete, Method: DELETE Elimina un registro en la BB.DD.
	 * 
	 * @param headers		Arreglo de objetos de tipo llave valor para almacenar los headers
	 * @param id 			Identificador del registro a actualizar
	 * @return 				Objeto con el resultado del borrado
	 */
	@Override
	@DeleteMapping(value = "/brand/{id}", produces = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<ResponseVO<Boolean>> deleteBrand(@RequestHeader Map<String, String> headers,
			@PathVariable("id") Long id) {
		// Declaracion de variables
		ResponseEntity<ResponseVO<Boolean>> response = null;
		
		// Generamos el objeto requestVO
		DeleteBrandRequestVO deleteBrandRequestVO = new DeleteBrandRequestVO();
		deleteBrandRequestVO.setId(id);
		RequestVO<DeleteBrandRequestVO> requestVO = RequestVOUtil.setNewRequestVO(headers, deleteBrandRequestVO);
		

		try {
			
			ResponseVO<Boolean> serviceResponse = brandService.delete(requestVO);

			response = BrandServicesResponseBuilder.buildDeleteResponse(serviceResponse);

		} catch (Exception exception) {
			// Excepcion no controlada
			ResponseVO<Boolean> exceptionResponse = ResponseUtil.getErrorResponse(exception);
			response = ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(exceptionResponse);
		}
		return response;
	}

	/**
	 * Operation: findEventDetail, Method: Busca el detalle de un registro en la base de datos.
	 * 
	 * @param headers		Arreglo de objetos de tipo llave valor para almacenar los headers
	 * @param id 			Identificador del registro a buscar
	 * @return 				Objeto con el resultado del borrado
	 */
	@Override
	@GetMapping(value = "/brand/detail", produces = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<ResponseVO<FindDetailBrandResponseVO>> findDetailBrand(
			@RequestHeader Map<String, String> headers, 
			@RequestParam(value = "id", required= false) Long id,
			@RequestParam(value = "name", required = false) String name) {

		// Declaracion de variables
		ResponseEntity<ResponseVO<FindDetailBrandResponseVO>> response = null;

		// Crear el objeto requestVO
		FindDetailBrandRequestVO findDetailBrandRequestVO = new FindDetailBrandRequestVO();
		findDetailBrandRequestVO.setId(id);
		findDetailBrandRequestVO.setName(name);
		RequestVO<FindDetailBrandRequestVO> requestVO = RequestVOUtil.setNewRequestVO(headers, findDetailBrandRequestVO);

		try {
			
			// Invocar al metodo de busqueda
			ResponseVO<FindDetailBrandResponseVO> serviceResponse = brandService.findDetail(requestVO);
			
			response = BrandServicesResponseBuilder.buildFindDetailResponse(serviceResponse);

		} catch (Exception exception) {
			// Excepcion no controlada
			ResponseVO<FindDetailBrandResponseVO> exceptionResponse = ResponseUtil.getErrorResponse(exception);
			response = ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(exceptionResponse);
		}
		return response;
	}
	
	/**
	 * Operation: findEventList, Method: GET Consulta la lista de registros en
	 * base a varios parametros. El resultado se regresa como un arreglo de
	 * registros.
	 * 
	 * @return ResponseVO con la lista de registros encontrada
	 * 
	 * @param nombre Patron de nombre a buscar
	 * @param page   Numero de pagina
	 * @param size   Tamano de pagina
	 * @param token  Token de sesion
	 */
	@Override
	@GetMapping(value = "/brand/list", produces = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<ResponseVO<List<FindListBrandResponseVO>>> findListBrand(
			@RequestHeader Map<String, String> headers, 
			@RequestParam(value = "page", required = false) Integer page,
			@RequestParam(value = "size", required = false) Integer size,
			@RequestParam(value = "orderBy", required = false) String orderBy,
			@RequestParam(value = "orderType", required = false) String orderType,
			@RequestParam(value = "name", required = false) String name){
		// Declarar variables
		ResponseEntity<ResponseVO<List<FindListBrandResponseVO>>> response = null;

		FindListBrandRequestVO findListBrandRequestVO = new FindListBrandRequestVO();
		findListBrandRequestVO.setName(name);
		RequestVO<FindListBrandRequestVO> requestVO = RequestVOUtil.setNewRequestVO(headers, page, size, orderBy,
				orderType, findListBrandRequestVO);

		try {

			ResponseVO<List<FindListBrandResponseVO>> serviceResponse = brandService.findList(requestVO);
			response = BrandServicesResponseBuilder.buildFindListBrandResponse(serviceResponse);

		} catch (Exception exception) {
			// Excepcion no controlada
			ResponseVO<List<FindListBrandResponseVO>> exceptionResponse = ResponseUtil
					.getErrorResponse(exception);
			response = ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(exceptionResponse);
		}
		return response;
	}

}