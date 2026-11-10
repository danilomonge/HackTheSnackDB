package de.pecus.api.util;

import java.util.Date;

import de.pecus.api.entities.AuditBase;

/**
 * Helpers shared by the service layer.
 *
 * HackTheSnack has no user accounts, so every write is attributed to the
 * system user {@link #SYSTEM_USER_ID}. The {@code token} parameters are kept
 * so the service signatures stay unchanged if authentication is added later.
 */
public final class ServiceUtil {

	/** User id written to the DN_USUARIO_CREADOR / DN_USUARIO_MODIFICADOR audit columns. */
	public static final Long SYSTEM_USER_ID = 1L;

	private ServiceUtil() {
		throw new IllegalStateException("Utility class");
	}

	/**
	 * Fills the audit columns of an entity before it is inserted or updated.
	 * The creation fields are only set the first time (when they are still empty).
	 *
	 * @param entity entity to update
	 * @param token  unused, reserved for future authentication
	 */
	public static void setAuditFields(AuditBase<?> entity, String token) {
		Date now = new Date();
		if (entity.getCreationDate() == null) {
			entity.setCreationDate(now);
			entity.setCreatorUsername(SYSTEM_USER_ID);
		}
		entity.setLastModifiedDate(now);
		entity.setLastModifiedUsername(SYSTEM_USER_ID);
		entity.setActive(Boolean.TRUE);
	}

	/**
	 * Marks an entity as logically deleted (soft delete).
	 *
	 * @param entity entity to disable
	 * @param token  unused, reserved for future authentication
	 */
	public static void setDisabledEntity(AuditBase<?> entity, String token) {
		setAuditFields(entity, token);
		entity.setActive(Boolean.FALSE);
	}
}
