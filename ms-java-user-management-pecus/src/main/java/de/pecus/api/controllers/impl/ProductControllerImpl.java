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

import de.pecus.api.controllers.ProductController;
//import de.pecus.api.log.SmartLogger;
//import de.pecus.api.log.SmartLoggerFactory;
import de.pecus.api.services.usuarios.ProductService;
import de.pecus.api.util.ProductServicesResponseBuilder;
import de.pecus.api.util.RequestVOUtil;
import de.pecus.api.util.ResponseUtil;
import de.pecus.api.vo.RequestVO;
import de.pecus.api.vo.ResponseVO;
import de.pecus.api.vo.product.CreateProductRequestVO;
import de.pecus.api.vo.product.DeleteProductRequestVO;
import de.pecus.api.vo.product.FindDetailProductRequestVO;
import de.pecus.api.vo.product.FindDetailProductResponseVO;
import de.pecus.api.vo.product.FindListProductRequestVO;
import de.pecus.api.vo.product.FindListProductResponseVO;
import de.pecus.api.vo.product.UpdateProductRequestVO;

/**
 * Clase de controlador para servicio Rest de Product
 * 
 * @author Jorge Serrano
 * @version 1.0
 * @created 21-junio-2019 11:34:31 a. m.
 */
@RestController
@RequestMapping("")
public class ProductControllerImpl implements ProductController {

	//public static final SmartLogger LOGGER = SmartLoggerFactory.getLogger(ProductController.class);

	@Autowired
	private ProductService productService;

	/**
	 * Operation: Operation: create, Method: POST Crea un registro en la BB.DD.
	 * @param headers		Arreglo de objetos de tipo llave valor para almacenar los headers
	 * @param body 			Cuerpo de la peticion con la informacion del regsitro que se desean insertar
	 * @return 				Ojeto con el cuerpo de respuesta (ong con el id del registro actualizado) y le codigo de respuesta http corrspondiente a la operacion
	 */
	@Override
	@PostMapping(value = "/product", produces = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<ResponseVO<Long>> createProduct(@RequestHeader Map<String, String> headers,
			@RequestBody CreateProductRequestVO body) {
		
			// Generamos el objeto requestVO
			RequestVO<CreateProductRequestVO> requestVO = RequestVOUtil.setNewRequestVO(headers,body);

			ResponseEntity<ResponseVO<Long>> response = null;
			try {
				// Invocar al servicio
				ResponseVO<Long> serviceResponse = productService.create(requestVO);

				response  = ProductServicesResponseBuilder.buildCreateOrUpdateResponse(serviceResponse);

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
	@PutMapping(value = "/product/{id}", produces = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<ResponseVO<Long>> updateProduct(@RequestHeader Map<String, String> headers, 
			@PathVariable(name = "id", required = false) Long id, 
			@RequestBody UpdateProductRequestVO body) {
		
		
			// Declaracion de variable
			ResponseEntity<ResponseVO<Long>> response = null;

			body.setId(id);

			// Generamos el objeto requestVO
			RequestVO<UpdateProductRequestVO> requestVO = RequestVOUtil.setNewRequestVO(headers,body);
			
			try {
				// Invocar al servicio
				ResponseVO<Long> serviceResponse = productService.update(requestVO);

				response = ProductServicesResponseBuilder.buildCreateOrUpdateResponse(serviceResponse);

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
	@DeleteMapping(value = "/product/{id}", produces = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<ResponseVO<Boolean>> deleteProduct(@RequestHeader Map<String, String> headers,
			@PathVariable("id") Long id) {
		// Declaracion de variables
		ResponseEntity<ResponseVO<Boolean>> response = null;
		
		// Generamos el objeto requestVO
		DeleteProductRequestVO deleteProductRequestVO = new DeleteProductRequestVO();
		deleteProductRequestVO.setId(id);
		RequestVO<DeleteProductRequestVO> requestVO = RequestVOUtil.setNewRequestVO(headers,deleteProductRequestVO);
		

		try {
			
			ResponseVO<Boolean> serviceResponse = productService.delete(requestVO);

			response = ProductServicesResponseBuilder.buildDeleteResponse(serviceResponse);

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
	@GetMapping(value = "/product/detail", produces = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<ResponseVO<FindDetailProductResponseVO>> findDetailProduct(
			@RequestHeader Map<String, String> headers, 
			@RequestParam(value = "id", required= false) Long id,
			@RequestParam(value = "name", required = false) String name) {

		// Declaracion de variables
		ResponseEntity<ResponseVO<FindDetailProductResponseVO>> response = null;

		// Crear el objeto requestVO
		FindDetailProductRequestVO findDetailProductRequestVO = new FindDetailProductRequestVO();
		findDetailProductRequestVO.setId(id);
		findDetailProductRequestVO.setName(name);
		RequestVO<FindDetailProductRequestVO> requestVO = RequestVOUtil.setNewRequestVO(headers,findDetailProductRequestVO);

		try {
			
			// Invocar al metodo de busqueda
			ResponseVO<FindDetailProductResponseVO> serviceResponse = productService.findDetail(requestVO);
			
			response = ProductServicesResponseBuilder.buildFindDetailResponse(serviceResponse);

		} catch (Exception exception) {
			// Excepcion no controlada
			ResponseVO<FindDetailProductResponseVO> exceptionResponse = ResponseUtil.getErrorResponse(exception);
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
	@GetMapping(value = "/product/list", produces = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<ResponseVO<List<FindListProductResponseVO>>> findListProduct(
			@RequestHeader Map<String, String> headers, 
			@RequestParam(value = "page", required = false) Integer page,
			@RequestParam(value = "size", required = false) Integer size,
			@RequestParam(value = "orderBy", required = false) String orderBy,
			@RequestParam(value = "orderType", required = false) String orderType,
			@RequestParam(value = "name", required = false) String name){
		// Declarar variables
		ResponseEntity<ResponseVO<List<FindListProductResponseVO>>> response = null;

		FindListProductRequestVO findListProductRequestVO = new FindListProductRequestVO();
		findListProductRequestVO.setName(name);
		RequestVO<FindListProductRequestVO> requestVO = RequestVOUtil.setNewRequestVO(headers, page, size, orderBy,
				orderType, findListProductRequestVO);

		try {

			ResponseVO<List<FindListProductResponseVO>> serviceResponse = productService.findList(requestVO);
			response = ProductServicesResponseBuilder.buildFindListProductResponse(serviceResponse);

		} catch (Exception exception) {
			// Excepcion no controlada
			ResponseVO<List<FindListProductResponseVO>> exceptionResponse = ResponseUtil
					.getErrorResponse(exception);
			response = ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(exceptionResponse);
		}
		return response;
	}

}