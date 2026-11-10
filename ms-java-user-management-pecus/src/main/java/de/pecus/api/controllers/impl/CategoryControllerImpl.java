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

import de.pecus.api.controllers.CategoryController;
import de.pecus.api.services.usuarios.CategoryService;
import de.pecus.api.util.CategoryServicesResponseBuilder;
import de.pecus.api.util.RequestVOUtil;
import de.pecus.api.util.ResponseUtil;
import de.pecus.api.vo.RequestVO;
import de.pecus.api.vo.ResponseVO;
import de.pecus.api.vo.category.CreateCategoryRequestVO;
import de.pecus.api.vo.category.DeleteCategoryRequestVO;
import de.pecus.api.vo.category.FindDetailCategoryRequestVO;
import de.pecus.api.vo.category.FindDetailCategoryResponseVO;
import de.pecus.api.vo.category.FindListCategoryRequestVO;
import de.pecus.api.vo.category.FindListCategoryResponseVO;
import de.pecus.api.vo.category.UpdateCategoryRequestVO;

/**
 * Clase de controlador para servicio Rest de Category
 * 
 * @author Jorge Serrano
 * @version 1.0
 * @created 21-junio-2019 11:34:31 a. m.
 */
@RestController
@RequestMapping("")
public class CategoryControllerImpl implements CategoryController {

	@Autowired
	private CategoryService categoryService;

	/**
	 * Operation: Operation: create, Method: POST Crea un registro en la BB.DD.
	 * @param headers		Arreglo de objetos de tipo llave valor para almacenar los headers
	 * @param body 			Cuerpo de la peticion con la informacion del regsitro que se desean insertar
	 * @return 				Ojeto con el cuerpo de respuesta (ong con el id del registro actualizado) y le codigo de respuesta http corrspondiente a la operacion
	 */
	@Override
	@PostMapping(value = "/category", produces = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<ResponseVO<Long>> createCategory(@RequestHeader Map<String, String> headers,
			@RequestBody CreateCategoryRequestVO body) {
		
			// Generamos el objeto requestVO
			RequestVO<CreateCategoryRequestVO> requestVO = RequestVOUtil.setNewRequestVO(headers,body);

			ResponseEntity<ResponseVO<Long>> response = null;
			try {
				// Invocar al servicio
				ResponseVO<Long> serviceResponse = categoryService.create(requestVO);

				response  = CategoryServicesResponseBuilder.buildCreateOrUpdateResponse(serviceResponse);

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
	@PutMapping(value = "/category/{id}", produces = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<ResponseVO<Long>> updateCategory(@RequestHeader Map<String, String> headers, 
			@PathVariable(name = "id", required = false) Long id, 
			@RequestBody UpdateCategoryRequestVO body) {
		
		
			// Declaracion de variable
			ResponseEntity<ResponseVO<Long>> response = null;

			body.setId(id);

			// Generamos el objeto requestVO
			RequestVO<UpdateCategoryRequestVO> requestVO = RequestVOUtil.setNewRequestVO(headers,body);
			
			try {
				// Invocar al servicio
				ResponseVO<Long> serviceResponse = categoryService.update(requestVO);

				response = CategoryServicesResponseBuilder.buildCreateOrUpdateResponse(serviceResponse);

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
	@DeleteMapping(value = "/category/{id}", produces = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<ResponseVO<Boolean>> deleteCategory(@RequestHeader Map<String, String> headers,
			@PathVariable("id") Long id) {
		// Declaracion de variables
		ResponseEntity<ResponseVO<Boolean>> response = null;
		
		// Generamos el objeto requestVO
		DeleteCategoryRequestVO deleteCategoryRequestVO = new DeleteCategoryRequestVO();
		deleteCategoryRequestVO.setId(id);
		RequestVO<DeleteCategoryRequestVO> requestVO = RequestVOUtil.setNewRequestVO(headers,deleteCategoryRequestVO);
		

		try {
			
			ResponseVO<Boolean> serviceResponse = categoryService.delete(requestVO);

			response = CategoryServicesResponseBuilder.buildDeleteResponse(serviceResponse);

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
	@GetMapping(value = "/category/detail", produces = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<ResponseVO<FindDetailCategoryResponseVO>> findDetailCategory(
			@RequestHeader Map<String, String> headers, 
			@RequestParam(value = "id", required= false) Long id,
			@RequestParam(value = "name", required = false) String name) {

		// Declaracion de variables
		ResponseEntity<ResponseVO<FindDetailCategoryResponseVO>> response = null;

		// Crear el objeto requestVO
		FindDetailCategoryRequestVO findDetailCategoryRequestVO = new FindDetailCategoryRequestVO();
		findDetailCategoryRequestVO.setId(id);
		findDetailCategoryRequestVO.setName(name);
		RequestVO<FindDetailCategoryRequestVO> requestVO = RequestVOUtil.setNewRequestVO(headers,findDetailCategoryRequestVO);

		try {
			
			// Invocar al metodo de busqueda
			ResponseVO<FindDetailCategoryResponseVO> serviceResponse = categoryService.findDetail(requestVO);
			
			response = CategoryServicesResponseBuilder.buildFindDetailResponse(serviceResponse);

		} catch (Exception exception) {
			// Excepcion no controlada
			ResponseVO<FindDetailCategoryResponseVO> exceptionResponse = ResponseUtil.getErrorResponse(exception);
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
	@GetMapping(value = "/category/list", produces = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<ResponseVO<List<FindListCategoryResponseVO>>> findListCategory(
			@RequestHeader Map<String, String> headers, 
			@RequestParam(value = "page", required = false) Integer page,
			@RequestParam(value = "size", required = false) Integer size,
			@RequestParam(value = "orderBy", required = false) String orderBy,
			@RequestParam(value = "orderType", required = false) String orderType,
			@RequestParam(value = "name", required = false) String name){
		// Declarar variables
		ResponseEntity<ResponseVO<List<FindListCategoryResponseVO>>> response = null;

		FindListCategoryRequestVO findListCategoryRequestVO = new FindListCategoryRequestVO();
		findListCategoryRequestVO.setName(name);
		RequestVO<FindListCategoryRequestVO> requestVO = RequestVOUtil.setNewRequestVO(headers, page, size, orderBy,
				orderType, findListCategoryRequestVO);

		try {

			ResponseVO<List<FindListCategoryResponseVO>> serviceResponse = categoryService.findList(requestVO);
			response = CategoryServicesResponseBuilder.buildFindListCategoryResponse(serviceResponse);

		} catch (Exception exception) {
			// Excepcion no controlada
			ResponseVO<List<FindListCategoryResponseVO>> exceptionResponse = ResponseUtil
					.getErrorResponse(exception);
			response = ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(exceptionResponse);
		}
		return response;
	}

}