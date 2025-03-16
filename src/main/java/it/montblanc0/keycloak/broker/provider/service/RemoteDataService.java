package it.montblanc0.keycloak.broker.provider.service;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.extern.jbosslog.JBossLog;
import org.keycloak.broker.provider.BrokeredIdentityContext;
import org.keycloak.broker.provider.util.SimpleHttp;
import org.keycloak.models.KeycloakSession;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ThreadLocalRandom;

@JBossLog
public class RemoteDataService {

	private RemoteDataService() {
	}

	/**
	 * Fetches data from a remote source and returns an Optional<br>
	 * FIXME: This is just an example and should be changed according to your needs.<br>The current implementation fetches a random user's company name from  <a href="https://jsonplaceholder.typicode.com">jsonplaceholder</a>.
	 *
	 * @param context
	 * @param session
	 * @return Optional containing the fetched attribute value, or empty if not found
	 */
	public static Optional<Object> fetchRemoteValue(BrokeredIdentityContext context, KeycloakSession session) {
		// TODO: Set external base url here
		final String API_BASE_URL = "https://jsonplaceholder.typicode.com/users/";
		final int SOCKET_TIMEOUT_MS = 15000;
		final int CONNECTION_TIMEOUT_MS = 5000;
		final int REQUEST_TIMEOUT_MS = 3000;

		// TODO: use context to fetch external data with matching user info, e.g.:
		// String upn = context.getUserAttribute("upn");
		int randomId = ThreadLocalRandom.current().nextInt(1, 11);
		String apiUrl = API_BASE_URL + randomId;

		log.debugf("[fetchRemoteValue][%s] Fetching data from: %s", context.getId(), apiUrl);
		try {
			JsonNode jsonResponse = SimpleHttp
					.doGet(apiUrl, session)
					.acceptJson()
					.socketTimeOutMillis(SOCKET_TIMEOUT_MS)
					.connectTimeoutMillis(CONNECTION_TIMEOUT_MS)
					.connectionRequestTimeoutMillis(REQUEST_TIMEOUT_MS)
					.asJson();

			if (null == jsonResponse) {
				log.warnf("[fetchRemoteValue][%s] Received null response from API", context.getId());
				return Optional.empty();
			}

			//TODO: Customize data retrieval here
			if (jsonResponse.has("company") && jsonResponse.get("company").has("name")) {
				JsonNode nameNode = jsonResponse.get("company").get("name");

				return getOptionalObject(context, nameNode);
			}

			log.warnf("[fetchRemoteValue][%s] No usable data found in the response", context.getId());
			return Optional.empty();

		} catch (Exception e) {
			log.errorf("[fetchRemoteValue][%s] Failed to fetch data: %s", context.getId(), e.getMessage());
			// TODO: You would throw an exception here if the attribute is mandatory
//			throw e;
			return Optional.empty();
		}
	}


	/**
	 * Parses a JsonNode and returns an Optional with the given value
	 *
	 * @param nameNode The JsonNode received from the API response
	 * @return Optional containing the fetched attribute value, or empty if not found
	 */
	private static Optional<Object> getOptionalObject(BrokeredIdentityContext context, JsonNode nameNode) {
		// Handle different types for company.name
		if (nameNode.isArray()) {
			// Handle array type
			List<String> nameParts = new ArrayList<>();
			nameNode.forEach(part -> nameParts.add(part.asText()));

			if (nameParts.size() > 1) {
				// Return as list for Keycloak multivalued attribute
				log.debugf("[getOptionalObject][%s] Fetched external attribute as array with %d parts", context.getId(), nameParts.size());
				return Optional.of(nameParts);
			} else if (nameParts.size() == 1) {
				// Single element array, return as string
				String externalValue = nameParts.get(0);
				log.debugf("[getOptionalObject][%s] Fetched external attribute from single-element array: %s", context.getId(), externalValue);
				return Optional.of(externalValue.trim());
			} else {
				// Empty array
				log.warnf("[getOptionalObject][%s] external attribute array is empty", context.getId());
				return Optional.empty();
			}
		} else if (nameNode.isObject()) {
			// Handle object type - convert to string representation
			String externalValue = nameNode.toString();
			log.debugf("[getOptionalObject][%s] Fetched external attribute as object: %s", context.getId(), externalValue);
			return Optional.of(externalValue);
		} else if (nameNode.isNumber()) {
			// Handle number type
			String externalValue = String.valueOf(nameNode.asLong());
			log.debugf("[getOptionalObject][%s] Fetched external attribute as number: %s", context.getId(), externalValue);
			return Optional.of(externalValue);
		} else if (nameNode.isBoolean()) {
			// Handle boolean type
			String externalValue = String.valueOf(nameNode.asBoolean());
			log.debugf("[getOptionalObject][%s] Fetched external attribute as boolean: %s", context.getId(), externalValue);
			return Optional.of(externalValue);
		} else if (nameNode.isNull()) {
			// Handle null type
			log.warnf("[getOptionalObject][%s] external attribute is null", context.getId());
			return Optional.empty();
		} else {
			// Handle text (default case)
			String externalValue = nameNode.asText();
			log.debugf("[getOptionalObject][%s] Fetched external attribute: %s", context.getId(), externalValue);
			return Optional.of(externalValue.trim());
		}
	}

}
