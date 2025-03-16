package it.montblanc0.keycloak.broker.provider;

import com.google.auto.service.AutoService;
import lombok.extern.jbosslog.JBossLog;
import org.keycloak.authentication.authenticators.broker.util.ExistingUserInfo;
import org.keycloak.broker.provider.AbstractIdentityProviderMapper;
import org.keycloak.broker.provider.BrokeredIdentityContext;
import org.keycloak.broker.provider.IdentityBrokerException;
import org.keycloak.broker.provider.IdentityProviderMapper;
import org.keycloak.models.FederatedIdentityModel;
import org.keycloak.models.IdentityProviderMapperModel;
import org.keycloak.models.IdentityProviderSyncMode;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserModel;
import org.keycloak.provider.ProviderConfigProperty;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.stream.Collectors;

import static it.montblanc0.keycloak.broker.provider.helper.ExistingUserHelper.getExistingUserByDuplicatedAttribute;
import static it.montblanc0.keycloak.broker.provider.service.RemoteDataService.fetchRemoteValue;

@JBossLog
@AutoService(IdentityProviderMapper.class)
public class RemoteAttributeImporter extends AbstractIdentityProviderMapper {

	//	public static final String[] COMPATIBLE_PROVIDERS = {KeycloakOIDCIdentityProviderFactory.PROVIDER_ID, OIDCIdentityProviderFactory.PROVIDER_ID};
	public static final String[] COMPATIBLE_PROVIDERS = {ANY_PROVIDER};

	public static final String CONF_ATTRIBUTE_NAME = "attribute.name";
	public static final String PROVIDER_ID = "remote-attribute-importer";
	private static final Set<IdentityProviderSyncMode> IDENTITY_PROVIDER_SYNC_MODES = new HashSet<>(Arrays.asList(IdentityProviderSyncMode.values()));
	private static final List<ProviderConfigProperty> configProperties = new ArrayList<>();


	static {
		ProviderConfigProperty attributeProperty = new ProviderConfigProperty();
		attributeProperty.setName(CONF_ATTRIBUTE_NAME);
		attributeProperty.setLabel("User Attribute Name");
		attributeProperty.setHelpText("Choose a predefined user property (email, firstName, lastName) or enter a custom attribute.");
		attributeProperty.setType(ProviderConfigProperty.USER_PROFILE_ATTRIBUTE_LIST_TYPE);
		configProperties.add(attributeProperty);
	}

	private static IdentityProviderSyncMode getSyncMode(IdentityProviderMapperModel mapperModel, BrokeredIdentityContext context) {
		// Get the sync mode string from the mapper config
		String syncModeString = mapperModel.getConfig().get(IdentityProviderMapperModel.SYNC_MODE);
		// Use the identity provider's sync mode if the mapper one is set to INHERIT
		return "INHERIT".equals(syncModeString) ? context.getIdpConfig().getSyncMode() : IdentityProviderSyncMode.valueOf(syncModeString);
	}

	@Override
	public boolean supportsSyncMode(IdentityProviderSyncMode syncMode) {
		return IDENTITY_PROVIDER_SYNC_MODES.contains(syncMode);
	}

	@Override
	public String[] getCompatibleProviders() {
		return COMPATIBLE_PROVIDERS;
	}

	@Override
	public String getId() {
		return PROVIDER_ID;
	}

	@Override
	public String getDisplayCategory() {
		return "Custom Mappers";
	}

	@Override
	public String getDisplayType() {
		return "Remote Attribute Importer";
	}

	@Override
	public String getHelpText() {
		return "Fetches data from a remote source and sets it to a selected user attribute.";
	}

	@Override
	public List<ProviderConfigProperty> getConfigProperties() {
		return configProperties;
	}

	@SuppressWarnings("unchecked")
	@Override
	public void preprocessFederatedIdentity(KeycloakSession session, RealmModel realm,
	                                        IdentityProviderMapperModel mapperModel,
	                                        BrokeredIdentityContext context) {
		log.debugf("[preprocessFederatedIdentity][%s] Called for user %s", context.getId(), context.getUsername());

		// Get the user attribute name
		String attribute = getUserAttribute(mapperModel);
		if (null == attribute) return;

		// Get the sync mode
		IdentityProviderSyncMode syncMode = getSyncMode(mapperModel, context);

		// Check if this is a new user by looking for existing federated identity
		FederatedIdentityModel federatedIdentityModel = new FederatedIdentityModel(
				context.getIdpConfig().getAlias(),
				context.getId(),
				context.getUsername());

		UserModel federatedUser = session.users().getUserByFederatedIdentity(realm, federatedIdentityModel);

		if (null != federatedUser && syncMode != IdentityProviderSyncMode.FORCE) {
			log.debugf("[preprocessFederatedIdentity][%s] Using existing attribute for federated user %s", context.getId(), context.getUsername());

			// Cache the user attribute in context data
			String cacheKey = attribute + "_" + context.getId();
			List<String> attributeValues = federatedUser.getAttributes().get(attribute);
			context.getContextData().put(cacheKey, attributeValues);
		}

		resolveAndSetAttribute(session, mapperModel, context, realm, federatedUser, (key, value) -> {
			if (value instanceof List) {
				context.setUserAttribute(key, (List<String>) value);
			} else if (value instanceof Collection) {
				// Handle other collection types
				List<String> strValues = ((Collection<?>) value).stream()
						.map(Object::toString)
						.collect(Collectors.toList());
				context.setUserAttribute(key, strValues);
			} else {
				context.setUserAttribute(key, (String) value);
			}
		});

	}

	@SuppressWarnings("unchecked")
	@Override
	public void updateBrokeredUser(KeycloakSession session, RealmModel realm,
	                               UserModel user, IdentityProviderMapperModel mapperModel,
	                               BrokeredIdentityContext context) {
		log.debugf("[updateBrokeredUser][%s] Called for user %s", context.getId(), user.getUsername());


		resolveAndSetAttribute(session, mapperModel, context, null, user, (key, value) -> {
			if (value instanceof List) {
				user.setAttribute(key, (List<String>) value);
			} else if (value instanceof Collection) {
				List<String> strValues = ((Collection<?>) value).stream()
						.map(Object::toString)
						.collect(Collectors.toList());
				user.setAttribute(key, strValues);
			} else {
				user.setSingleAttribute(key, (String) value);
			}
		});

		log.debugf("[updateBrokeredUser][%s] Attribute has been updated on user %s", context.getId(), user.getUsername());

	}

	private String getUserAttribute(IdentityProviderMapperModel mapperModel) {
		String attribute = mapperModel.getConfig() == null ? null : mapperModel.getConfig().get(CONF_ATTRIBUTE_NAME);
		if (null == attribute || attribute.isBlank()) {
			log.warn("[getUserAttribute] User attribute name is not configured. Skipping mapper execution.");
			return null;
		}
		return attribute.trim();
	}

	private void resolveAndSetAttribute(KeycloakSession session,
	                                    IdentityProviderMapperModel mapperModel,
	                                    BrokeredIdentityContext context,
	                                    RealmModel realm,
	                                    UserModel federatedUser,
	                                    BiConsumer<String, Object> updater) {
		// Get the user attribute name
		String attribute = getUserAttribute(mapperModel);
		if (null == attribute) return;

		// Check for a cached value in context data
		String cacheKey = attribute + "_" + context.getId();
		Object cachedValue = context.getContextData().get(cacheKey);
		log.debugf("[resolveAndSetAttribute][%s] Current cached value for user %s : %s", context.getId(), context.getUsername(), cachedValue);

		if (null != cachedValue) {
			log.debugf("[resolveAndSetAttribute][%s] Using cached value %s for user %s", context.getId(), cachedValue, context.getUsername());
			updater.accept(attribute, cachedValue);
			return;
		} else {
			// Evaluate sync mode
			IdentityProviderSyncMode syncMode = getSyncMode(mapperModel, context);

			if (IdentityProviderSyncMode.IMPORT == syncMode) {
				// Cached value is null and sync mode is IMPORT
				if (null != federatedUser) {
					// If the sync mode is Import and a federated user is logging in, respect the value on the user profile even if it is null
					log.debugf("[resolveAndSetAttribute][%s] Using cached value for user %s", context.getId(), context.getUsername());
					updater.accept(attribute, null);
					return;
				} else {
					// Check if user is trying to link their Keycloak account to the federated IdP in IMPORT mode.
					// By design, Keycloak 26.1.3 does not import any attribute in this scenario - avoid useless fetch if an unliked profile is present

					log.debugf("[resolveAndSetAttribute][%s] Evaluating FEDERATED_IDENTITY_LINK scenario in IMPORT mode...", context.getId());

					// Check for duplication ahead on time
					ExistingUserInfo existingUserInfo = getExistingUserByDuplicatedAttribute(session, realm, context);

					// If a duplication is found, this is likely an account linking scenario
					if (null != existingUserInfo) {
						log.debugf("[resolveAndSetAttribute][%s] An existing user with %s = %s has been found. Data will not be fetched.", context.getId(),
								existingUserInfo.getDuplicateAttributeName(), existingUserInfo.getDuplicateAttributeValue());
						cachedValue = session.users().getUserById(realm, existingUserInfo.getExistingUserId()).getAttributes().get(attribute);
						log.debugf("[resolveAndSetAttribute][%s] Using cached value for user %s", context.getId(), context.getUsername());
						updater.accept(attribute, cachedValue);
						return;
					} else {
						// User is registering
						log.debugf("[resolveAndSetAttribute][%s] No duplication found for user %s . Data will be fetched.", context.getId(), context.getUsername());
					}

				}
			}
		}

		// Fetch data from external service
		try {
			Optional<Object> optionalValue = fetchRemoteValue(context, session);

			// Early return if no value present in Optional
			if (optionalValue.isEmpty()) {
				log.warnf("[resolveAndSetAttribute][%s] No value fetched for attribute %s, skipping update", context.getId(), attribute);
				return;
			}

			Object value = optionalValue.get();

			// Additional validations for empty values
			if (value instanceof String && ((String) value).isBlank()) {
				log.warnf("[resolveAndSetAttribute][%s] Empty string fetched for attribute %s, skipping update", context.getId(), attribute);
				return;
			}

			if (value instanceof Collection && ((Collection<?>) value).isEmpty()) {
				log.warnf("[resolveAndSetAttribute][%s] Empty collection fetched for attribute %s, skipping update", context.getId(), attribute);
				return;
			}

			// Cache the value for potential reuse in updateBrokeredUser
			log.debugf("[resolveAndSetAttribute][%s] Caching value %s for attribute %s", context.getId(), value, attribute);
			context.getContextData().put(cacheKey, value);

			// Update the attribute using the provided consumer function
			updater.accept(attribute, value);
			log.debugf("[resolveAndSetAttribute][%s] Updated attribute %s with fetched value %s for user %s", context.getId(), attribute, value, context.getUsername());

		} catch (IdentityBrokerException e) {
			log.errorf(e, "[resolveAndSetAttribute][%s] Identity broker exception while processing attribute %s", context.getId(), attribute);
			// TODO: You would throw an exception here if the attribute is mandatory
//			throw e;
		} catch (Exception e) {
			log.errorf(e, "[resolveAndSetAttribute][%s] Failed to fetch or update attribute %s", context.getId(), attribute);
			// TODO: You would throw an exception here if the attribute is mandatory
//			throw new IdentityBrokerException("[resolveAndSetAttribute][" + context.getId() + "] Failed to fetch or update attribute " + attribute + ": " + e.getMessage(), e);
		}
	}

}
