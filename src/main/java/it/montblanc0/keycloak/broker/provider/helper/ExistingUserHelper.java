package it.montblanc0.keycloak.broker.provider.helper;

import org.keycloak.authentication.authenticators.broker.util.ExistingUserInfo;
import org.keycloak.broker.provider.BrokeredIdentityContext;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserModel;

public class ExistingUserHelper {

	private ExistingUserHelper() {
	}

	/**
	 * Checks for the presence of an already registered user with the same email from the broker context.
	 * If no user is found by email, looks for the username argument.
	 *
	 * @param session
	 * @param realm
	 * @return The ExistingUserInfo or null
	 */
	public static ExistingUserInfo getExistingUserByDuplicatedAttribute(KeycloakSession session, RealmModel realm, BrokeredIdentityContext context) {

		if (null != context.getEmail() && !realm.isDuplicateEmailsAllowed()) {
			UserModel existingUser = session.users().getUserByEmail(realm, context.getEmail());
			if (null != existingUser) {
				return new ExistingUserInfo(existingUser.getId(), UserModel.EMAIL, existingUser.getEmail());
			}
		}

		String username = realm.isRegistrationEmailAsUsername() ? context.getEmail() : context.getModelUsername();

		if (null != username && !username.isBlank()) {
			UserModel existingUser = session.users().getUserByUsername(realm, username);
			if (null != existingUser) {
				return new ExistingUserInfo(existingUser.getId(), UserModel.USERNAME, existingUser.getUsername());
			}
		}
		return null;
	}
}
