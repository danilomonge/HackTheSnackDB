package de.pecus.api.services.usuarios.impl;

import java.text.Normalizer;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang.math.RandomUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.domain.Sort.Direction;
import org.springframework.stereotype.Service;

import de.pecus.api.constant.DataConstants;
import de.pecus.api.entities.BrandDO;
import de.pecus.api.entities.CategoryDO;
import de.pecus.api.entities.ProductDO;
import de.pecus.api.entities.SubCategoryDO;
import de.pecus.api.enums.WildcardTypeEnum;
import de.pecus.api.error.FuncionesBusinessError;
import de.pecus.api.error.GeneralBusinessErrors;
import de.pecus.api.repositories.usuarios.ProductRepository;
import de.pecus.api.services.usuarios.ProductService;
import de.pecus.api.util.CriteriaUtil;
import de.pecus.api.util.ResponseUtil;
import de.pecus.api.util.ServiceUtil;
import de.pecus.api.util.StringUtil;
import de.pecus.api.util.ValidatorArqUtil;
import de.pecus.api.util.ValidatorUtil;
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
 * Clase de logica de negocio para administracion de productes
 * 
 * @author Proa
 *
 */
@Service
public class ProductServiceImpl implements ProductService {

	@Autowired
	private ProductRepository productRepository;


	/**
	 * Crea un nuevo registro de product
	 * 
	 * @param request Objeto con parametros de entrada de product
	 * 
	 * @return Id generado
	 */
	public ResponseVO<Long> create(RequestVO<de.pecus.api.vo.product.CreateProductRequestVO> request) {

		// Declarar variables
		ResponseVO<Long> response = new ResponseVO<>();
		
			// Validar los parametros de entrada
			if (validateParametersCreate(request, response)) {
		
				// Preparar los datos para actualizar la BB.DD.
				ProductDO productDO = new ProductDO();
				
				productDO.setId(RandomUtils.nextLong());
				productDO.setName(request.getParameters().getName());
				productDO.setDescripcion(request.getParameters().getDescripcion());

				//Creamos las referencias a brand, category y subcategory
				BrandDO brandDO = new BrandDO();
				CategoryDO categoryDO = new CategoryDO();
				SubCategoryDO subCategoryDO = new SubCategoryDO();

				brandDO.setId(request.getParameters().getIdBrand());
				categoryDO.setId(request.getParameters().getIdCategory());
				subCategoryDO.setId(request.getParameters().getIdSubCategory());

				productDO.setBrand(brandDO);
				productDO.setCategory(categoryDO);
				productDO.setSubCategory(subCategoryDO);

				// Actualizar los parametros de auditoria
				ServiceUtil.setAuditFields(productDO, request.getToken());

				// Insertar el registro
				productDO = productRepository.saveAndFlush(productDO);

				// Regresar la respuesta correcta y el objeto a regresar
				response.setSuccess(true);
				response.setData(productDO.getId());
				
			}
		return response;
	}


	/**
	 * Actualiza un registro de product
	 * 
	 * @param request Objeto con parametros de entrada de product
	 * 
	 * @return Id actualizado
	 */
	public ResponseVO<Long> update(RequestVO<UpdateProductRequestVO> request) {

		// Declarar variables
		ResponseVO<Long> response = new ResponseVO<>();
		ProductDO registroDO = new ProductDO();

		// Validar los campos de entrada
		if (validateParametersUpdate(request, response)) {
			
			UpdateProductRequestVO parameters = request.getParameters();

			registroDO.setId(parameters.getId());
			registroDO.setName(parameters.getName());
			registroDO.setDescripcion(parameters.getDescripcion());

			//Creamos las referencias a brand, category y subcategory
			BrandDO brandDO = new BrandDO();
			CategoryDO categoryDO = new CategoryDO();
			SubCategoryDO subCategoryDO = new SubCategoryDO();

			brandDO.setId(request.getParameters().getIdBrand());
			categoryDO.setId(request.getParameters().getIdCategory());
			subCategoryDO.setId(request.getParameters().getIdSubCategory());

			registroDO.setBrand(brandDO);
			registroDO.setCategory(categoryDO);
			registroDO.setSubCategory(subCategoryDO);

			// Actualizar parametros de auditoria
			ServiceUtil.setAuditFields(registroDO, request.getToken());

			// Actualizar el registro en BB.DD.
			registroDO = productRepository.saveAndFlush(registroDO);

			// Preparar respuesta y objeto actualizado
			response.setSuccess(true);
			response.setData(registroDO.getId());
		}
		return response;
	}


	/**
	 * Marca un registro como eliminado un registro de product
	 * 
	 * @param request Objeto con parametros de entrada de product
	 * 
	 * @return Id eliminado
	 */
	public ResponseVO<Boolean> delete(RequestVO<DeleteProductRequestVO> request) {

		// Declarar variables
		ResponseVO<Boolean> response = new ResponseVO<>();

		// Validar campos de entrada
		if (validateParametersDelete(request, response)) {

			ProductDO productDO = this.exists(request.getParameters().getId(), null);
			if (ValidatorUtil.isNull(productDO)) {
				ResponseUtil.addError(request, response, FuncionesBusinessError.NOT_FOUND_ERROR, request);
			}
			else {
			// Actualizar la informacion
			ServiceUtil.setDisabledEntity(productDO, request.getToken());
			
			// Actualizar la BB.DD.
			productDO = productRepository.saveAndFlush(productDO);

			// Preparar respuesta y objeto eliminado
			response.setSuccess(true);
			response.setData(Boolean.TRUE);
			}
		}
		return response;
	}

	/**
	 * Consulta un product por Identificador unico
	 * 
	 * @return Objeto VO con los datos encontrados
	 * @param Id      Identificador del registro a buscar
	 * 
	 * @param request Objeto con los datos de busqueda
	 */
	public ResponseVO<FindDetailProductResponseVO> findDetail(RequestVO<FindDetailProductRequestVO> request) {

		// declaracion de varables
		ResponseVO<FindDetailProductResponseVO> response = new ResponseVO<>();
		FindDetailProductResponseVO salida = new FindDetailProductResponseVO();
		
		// validar que se cumplen las condiciones para realizar la consulta
		if (validateParametersFindDetail(request, response)) {

			ProductDO productDO = this.exists(request.getParameters().getId(), request.getParameters().getName());

			if (ValidatorUtil.isNull(productDO)) {
				ResponseUtil.addError(request, response, FuncionesBusinessError.NOT_FOUND_ERROR, request);
			} else {
				salida.setId(productDO.getId());
				salida.setName(productDO.getName());
				salida.setDescripcion(productDO.getDescripcion());
				salida.setIdBrand(productDO.getBrand().getId());
				salida.setNameBrand(productDO.getBrand().getName());
				salida.setIdCategory(productDO.getCategory().getId());
				salida.setNameCategory(productDO.getCategory().getName());
				salida.setIdSubCategory(productDO.getSubCategory().getId());
				salida.setNameSubCategory(productDO.getSubCategory().getName());

				response.setData(salida);
				// regresar la respuesta correcta con los registros obtenidos.
				response.setSuccess(true);
			}
				
		}
			
		return response;
	}



	
	/**
	 * Consulta la base de datos en base a varios parametros
	 * 
	 * @return ReponseVO con los datos encontrados
	 * 
	 * @param request Objeto con parametros de entrada de banner
	 */
	public ResponseVO<List<FindListProductResponseVO>> findList(RequestVO<FindListProductRequestVO> request) {

		// declaracion de varables
		ResponseVO<List<FindListProductResponseVO>> response = new ResponseVO<>();
		
		Page<ProductDO> listaProduct = null;
		
		if (validateParametersFindByList(request, response)) {
	
			FindListProductRequestVO parameters = request.getParameters();
			// Se obtiene el idioma
			
			String  name 	= CriteriaUtil.validateNullLike(parameters.getName(), WildcardTypeEnum.BOTH_SIDES);
			
			// Preparamos el objeto para la paginacion
			String orderby = request.getOrderBy();
			String ordertype = request.getOrderType();
			String orderBy = ValidatorUtil.isNullOrEmpty(orderby) ? "id" : orderby;
			Direction orderType = ValidatorUtil.isNullOrEmpty(ordertype) || ordertype.equals("asc") ? Direction.ASC
					: Direction.DESC;
			Integer size = ValidatorUtil.isNullOrZero(request.getSize()) ? 10 : request.getSize();
			Integer page = ValidatorUtil.isNullOrZero(request.getPage()) ? 1 : request.getPage();
			Pageable pageable = PageRequest.of(page - 1, size, Sort.by(orderType, orderBy));
			
			String normalizedName = this.limpiarAcentos(request.getParameters().getName());
			
			// ejecucion de la busqueda por el parametro recibido
			listaProduct = productRepository.findList(this.cleanString(normalizedName), pageable);

			// Si no se encontro ningun registro que cumpla la condicion generar error.
 				if (ValidatorUtil.isNullOrEmpty(listaProduct.getContent())) {
				ResponseUtil.addError(request, response, FuncionesBusinessError.NOT_FOUND_REGISTER_LIST_ERROR);
			} else {
				// Regresar la respuesta correcta con los registros obtenidos.
				response.setSuccess(true);
				response.setTotalRows(listaProduct.getTotalElements());
				response.setData(transformListDO(listaProduct.getContent()));
			}
		}
		return response;
	}
	

	
	/*******************************************************************************************************
	 * 
	 * FIN METODOS PUBLICOS
	 * 
	 *******************************************************************************************************/

	/*******************************************************************************************************
	 * 
	 * METODOS VALIDACION
	 * 
	 *******************************************************************************************************/

	/**
	 * Valida que los parametros para la operacion de insercion sean correctos
	 * 
	 * @param request  Objeto con los parametros de entrada
	 * @param response Respuesta donde se agregan los errores
	 * @return true si todos los parametros son correctos
	 */
	private boolean validateParametersCreate(RequestVO<CreateProductRequestVO> request, ResponseVO<Long> response) {
		
		// Obtener los parametros de entrada
		CreateProductRequestVO parameters = request.getParameters();

		// Validaciones de campos obligatorios
		if (StringUtil.isNullOrEmpty(parameters.getName())) {
			ResponseUtil.addError(request, response, 
					FuncionesBusinessError.REQUIRED_ID_NOMBRE_ERROR, request);
		} else {
			// Validacion de tamano
			String name = StringUtil.substring(parameters.getName(), DataConstants.MAX_SIZE_ID_NOMBRE);

			// Validacion de formato
			parameters.setName(StringUtil.toUpperCase(name));

				ProductDO registroB = this.exists(null,parameters.getName());
				
				if (!ValidatorUtil.isNull(registroB)) {
					ResponseUtil.addError(request, response, 
							FuncionesBusinessError.DUPLICATED_ERROR, request);					
			}
		}
		
		// Validaciones de campos obligatorios
		if (StringUtil.isNullOrEmpty(parameters.getDescripcion())) {
			ResponseUtil.addError(request, response, FuncionesBusinessError.REQUIRED_DESCRIPCION_ERROR,request);
		} else {
			// Validacion de tamano
			parameters.setDescripcion(StringUtil.substring(parameters.getDescripcion(), DataConstants.MAX_SIZE_DESCRIPCION));
		}
		
				
		// Regresar el resultado de la validacion
		return ValidatorUtil.isSuccessfulResponse(response);
	}


	/**
	 * Valida que los parametros para la operacion de actualizacion sean correctos
	 * 
	 * @param request  Objeto con los parametros de entrada
	 * @param response Respuesta donde se agregan los errores
	 * @return true si todos los parametros son correctos
	 */
	private boolean validateParametersUpdate(RequestVO<UpdateProductRequestVO> request, ResponseVO<Long> response) {
		// Recuperar parametros de entrada
		UpdateProductRequestVO parameters = request.getParameters();
		ProductDO registroUpdate = new ProductDO();
		
		// Validar que se informaron los campos de entrada
		if (ValidatorUtil.isNull(parameters)) {
			// Si no se ha informado regresar el error y no seguir validando
			ResponseUtil.addError(request, response, GeneralBusinessErrors.REQUIRED_PARAMETERS_ERROR);
			return false;
		}
		
		//Validar que exista el registro a actualizar
		if(ValidatorUtil.isNull(parameters.getId()))
		{
			ResponseUtil.addError(request, response, FuncionesBusinessError.REQUIRED_ID_ERROR, request);
			return false;
		} else {
			
				registroUpdate = this.exists(parameters.getId(),null);
			
				if (ValidatorUtil.isNull(registroUpdate)) {
					ResponseUtil.addError(request, response, FuncionesBusinessError.NOT_FOUND_ERROR, request);
					return false;
				}
		}
		
		// Validaciones de campos obligatorios: NOMBRE
		if (!StringUtil.isNullOrEmpty(parameters.getName())) {
			// Validacion de tamano
			String name = StringUtil.substring(parameters.getName(), DataConstants.MAX_SIZE_ID_NOMBRE);

			// Validacion de formato
			parameters.setName(StringUtil.toUpperCase(name));
			
				//Validar la posible duplicidad del name
				ProductDO productBusqueda = this.exists(null, request.getParameters().getName());
				
				if (!ValidatorUtil.isNull(productBusqueda)) {
					//Si se encuentra el registro validamos que no sea el mismo Id
					if (registroUpdate.getId() != productBusqueda.getId()) {
						ResponseUtil.addError(request, response, FuncionesBusinessError.DUPLICATED_ERROR, request);
						
					}
			}
		} else {
			parameters.setName(registroUpdate.getName());
		}
		
		// Validaciones de campos obligatorios: DESCRIPCION
		if (!StringUtil.isNullOrEmpty(parameters.getDescripcion())) {
			// Validacion de tamano
			parameters.setDescripcion(
					StringUtil.substring(parameters.getDescripcion(), DataConstants.MAX_SIZE_DESCRIPCION));

		} else {
			parameters.setDescripcion(registroUpdate.getDescripcion());
		}
					
		// Retorna el resultado de la validacion.
		return ValidatorUtil.isSuccessfulResponse(response);
		
	}

	/**
	 * Valida que los parametros para la operacion de eliminacion sean correctos
	 * 
	 * @param request  Objeto con los parametros de entrada
	 * @param response Respuesta donde se agregan los errores
	 * @return true si todos los parametros son correctos
	 */
	private boolean validateParametersDelete(RequestVO<DeleteProductRequestVO> request, ResponseVO<Boolean> response) {

		// Validar que se han informado los parametros de entrada
		if (ValidatorUtil.isNull(request.getParameters())) {
			// Si no se han informado generar error y terminar de validar
			ResponseUtil.addError(request, response, GeneralBusinessErrors.REQUIRED_PARAMETERS_ERROR);
			return false;
		}

		// Validaciones de campos obligatorios
		if (ValidatorUtil.isNullOrZero(request.getParameters().getId())) {
			ResponseUtil.addError(request, response, FuncionesBusinessError.REQUIRED_ID_ERROR);
		}
		
		return ValidatorUtil.isSuccessfulResponse(response);
	}


	/**
	 * Valida que los parametros para la operacion de consulta por nombre sean
	 * correctos
	 * 
	 * @param Id Identificador del registro a buscar
	 * @return true si el nombre no esta vacio
	 * 
	 * @param request  Objeto con los parametros a valida
	 * @param response Respuesta donde se agregan los errores
	 */
	private boolean validateParametersFindDetail(RequestVO<FindDetailProductRequestVO> request, ResponseVO<FindDetailProductResponseVO> response) {

		// Recuperar los parametros de entrada
		FindDetailProductRequestVO parameters = request.getParameters();

		// validar que el campo obligatorio
		if (ValidatorUtil.isNullOrZero(parameters.getId())) {
			
			//Buscar por criterio: Name
			if (ValidatorUtil.isNullOrEmpty(parameters.getName())) {
				ResponseUtil.addError(request, response, FuncionesBusinessError.REQUIRED_ID_ERROR);
			} 
		}
		
		return ValidatorUtil.isSuccessfulResponse(response);
	}


	/**
	 * Valida que los parametros para la operacion de consulta por parametros sean
	 * correctos
	 * 
	 * @return true si el nombre no esta vacio
	 * 
	 * @param request  Objeto con los criterios a buscar
	 * @param response Respuesta donde se agregan los errores
	 */
	private boolean validateParametersFindByList(RequestVO<FindListProductRequestVO> request,
			ResponseVO<List<FindListProductResponseVO>> response) {
		
		// Validar campos obligatorios
	    ValidatorArqUtil.validateParameters(request, response);
		
		// validar los parametros de la paginacion
	    ValidatorArqUtil.validatePaginatonParameters(request, response);
		return ValidatorUtil.isSuccessfulResponse(response);

	}



	/**
	 * Obtiene una lista de objetos productVO a partir de una lista de DO
	 * 
	 * @return Lista VO para retorno de resultados
	 * 
	 * @param listaProduct a transformar
	 */
	private List<FindListProductResponseVO> transformListDO(List<ProductDO> listaProduct) {

		// Declarar variables
		List<FindListProductResponseVO> listaProductVO = new ArrayList<>();

		// recorrer el objeto origen
		for (ProductDO productDO : listaProduct) {
			// Se hace la declaracion de variables necesarias
			FindListProductResponseVO productVO = new FindListProductResponseVO();
			
			productVO.setId(productDO.getId());
			productVO.setName(productDO.getName());
			productVO.setDescripcion(productDO.getDescripcion());
			productVO.setNameBrand(productDO.getBrand().getName());
			productVO.setIdCategory(productDO.getCategory().getId());
			productVO.setNameCategory(productDO.getCategory().getName());
			productVO.setIdSubCategory(productDO.getSubCategory().getId());
			productVO.setNameSubCategory(productDO.getSubCategory().getName());

			listaProductVO.add(productVO);
		}

		return listaProductVO;
	}
	


	/*************************************************************************
	 * Metodo que busca un registro por su id, name
	 * Regresa el objeto de la base de datos o una excepcion con el error
	 * 
	 *************************************************************************/
	public ProductDO exists(Long id, String name){

		ProductDO registro = null;
		try {
			//Validacion de datos de entrada
			if (ValidatorUtil.isNullOrZero(id)) {
				if (ValidatorUtil.isNullOrEmpty(name)) {
					registro = null;
				} else {
					//Buscamos por nombre
					registro = productRepository.findByName(name);
				}
			} else {
				//Consulta
				registro = productRepository.findById(id);
			}
			//Validacion de existencia
			if (ValidatorUtil.isNull(registro)) {
				//Genera error
				registro = null;
			}
		} catch (Exception e) {
			registro = null;
		}
		
		return registro;
	}	



	public String cleanString(String strInput) {
		if(!ValidatorUtil.isNullOrEmpty(strInput)) {
			strInput = strInput.trim().toUpperCase();
		}
		return strInput; 
	}
	
	public String limpiarAcentos(String cadena) {
	    String limpio =null;
	    if (cadena !=null) {
	        String valor = cadena;
	        valor = valor.toUpperCase();
	        // Normalizar texto para eliminar acentos, dieresis, cedillas y tildes
	        limpio = Normalizer.normalize(valor, Normalizer.Form.NFD);
	        // Quitar caracteres no ASCII excepto la enie, interrogacion que abre, exclamacion que abre, grados, U con dieresis.
	        limpio = limpio.replaceAll("[^\\p{ASCII}(N\u0303)(n\u0303)(\u00A1)(\u00BF)(\u00B0)(U\u0308)(u\u0308)]", "");
	        // Regresar a la forma compuesta, para poder comparar la enie con la tabla de valores
	        limpio = Normalizer.normalize(limpio, Normalizer.Form.NFC);
	    }
	    return limpio;
	}
	
	
}
