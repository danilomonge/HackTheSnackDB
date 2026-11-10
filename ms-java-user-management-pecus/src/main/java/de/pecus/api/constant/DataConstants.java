package de.pecus.api.constant;

/**
 * Clase de constantes
 * 
 * @author Luis Enrique Sanchez Santiago
 */
public final class DataConstants {

	/**
	 * Tamanno maximo para el atributo Id_Nombre
	 */
	public static final int MAX_SIZE_ID_NOMBRE = 100;

	/**
	 * Tamanno maximo para el atributo Descripcion
	 */
	public static final int MAX_SIZE_DESCRIPCION = 100;

	/**
	 * Tamanno maximo para el atributo numero
	 */
	public static final int MAX_SIZE_NUMERO = 10;

	/**
	 * Tamanno maximo para el atributo region
	 */
	public static final int MAX_SIZE_REGION = 5;
	
	/**
	 * Variable de entorno contenedora de la llave 
	 * requerida para el env\u00edo de emails.
	 */
	public static final String SENDGRID_API_KEY_ENV_VAR = "SENDGRID_API_KEY";
	
	public static final String  REGISTRO_ADMIN_CONDO_EXITOSO = "REGISTRO_ADMIN_CONDO_EXITOSO";
	
	public static final String IMAGE_PECUS = "https://admin.pecus.com.mx/pecus-admin/assets/images/mailings/isotipo.png";
	
	public static final String BADGE_PECUS = "https://admin.pecus.com.mx/pecus-admin/assets/images/mailings/isotipo.png";
	
	public static final String ICON_PECUS = "https://admin.pecus.com.mx/pecus-admin/assets/images/mailings/pecuslogoazul.png";
	
	public static final String DEFAULT_TAG = "DEFAULT_TAG";

	/**
	 * Constructor privado para evitar instanciar clases de utileria
	 */
	private DataConstants() {
	}

}
