# Remote Attribute Importer for Keycloak

A custom Identity Provider mapper template for Keycloak 26.1.3 to fetch attributes from remote services during
federation/authentication process.

<p align="center">
  <img src="https://i.ibb.co/cX1n4GnK/Screenshot-2025-03-16-124530.png" alt="Identity Provider Mapper configuration" width="380">
</p>

## Features

- Dynamically fetch user attributes from external services during federated login
- Smart caching to minimize external requests
- Supports all attribute synchronization modes
- Tested with all Keycloak federation workflows

## Customizing the Remote Data Service

The current implementation includes an example class that fetches the `company.name` from a
random [JSONPlaceholder](https://jsonplaceholder.typicode.com) user. You should modify the
[RemoteDataService](https://github.com/Montblanc0/keycloak-remote-attribute-importer/blob/master/src/main/java/it/montblanc0/keycloak/broker/provider/service/RemoteDataService.java)
class to integrate with your chosen external service/datasource.

## Warning

This extension is built with Keycloak internal SPI v26.1.3. Compatibility with previous or future versions is not
guaranteed.

## Behavior Matrix

The Remote Attribute Importer has different behaviors based on the sync mode and the context of the federation event.
Choose your preferred sync mode based on the matrix below:

- **USE EXISTING**: Uses the attribute value already stored in Keycloak
- **FETCH AND SET**: Makes an external API call to fetch and update the attribute

<table border="1" style="border-collapse: collapse;">
    <thead>
      <tr>
        <th>Sync&nbsp;Mode</th>
        <th>KC&nbsp;Realm<br>attr&nbsp;state</th>
        <th>IDENTITY_PROVIDER_FIRST_LOGIN<br>(REGISTER&nbsp;/&nbsp;transient-users)</th>
        <th>IDENTITY_PROVIDER_FIRST_LOGIN<br>(FEDERATED_IDENTITY_LINK)</th>
        <th>LOGIN</th>
        <th>external&nbsp;to&nbsp;internal<br>TOKEN-EXCHANGE<br>(REGISTER&nbsp;/&nbsp;transient-users)</th>
        <th>external&nbsp;to&nbsp;internal<br>TOKEN-EXCHANGE<br>(federated&nbsp;user)</th>
      </tr>
    </thead>
    <tbody>
      <tr>
        <td rowspan="3"><p align="center">IMPORT</p></td>
        <td>Unchanged</td>
        <td></td>
        <td><p align="center">:book:&nbsp;USE&nbsp;EXISTING</p></td>
        <td><p align="center">:book:&nbsp;USE&nbsp;EXISTING</p></td>
        <td></td>
        <td><p align="center">:book:&nbsp;USE&nbsp;EXISTING</p></td>
      </tr>
      <tr>
        <td>Changed</td>
        <td></td>
        <td><p align="center">:book:&nbsp;USE&nbsp;EXISTING</p></td>
        <td><p align="center">:book:&nbsp;USE&nbsp;EXISTING</p></td>
        <td></td>
        <td><p align="center">:book:&nbsp;USE&nbsp;EXISTING</p></td>
      </tr>
      <tr>
        <td>Unset/Deleted</td>
        <td><p align="center">:globe_with_meridians:&nbsp;FETCH&nbsp;AND&nbsp;SET</p></td>
        <td><p align="center">:book:&nbsp;USE&nbsp;EXISTING</p></td>
        <td><p align="center">:book:&nbsp;USE&nbsp;EXISTING</p></td>
        <td><p align="center">:globe_with_meridians:&nbsp;FETCH&nbsp;AND&nbsp;SET</p></td>
        <td><p align="center">:book:&nbsp;USE&nbsp;EXISTING</p></td>
      </tr>
      <tr>
        <td rowspan="3"><p align="center">LEGACY</p></td>
        <td>Unchanged</td>
        <td></td>
        <td><p align="center">:globe_with_meridians:&nbsp;FETCH&nbsp;AND&nbsp;SET</p></td>
        <td><p align="center">:book:&nbsp;USE&nbsp;EXISTING</p></td>
        <td></td>
        <td><p align="center">:book:&nbsp;USE&nbsp;EXISTING</p></td>
      </tr>
      <tr>
        <td>Changed</td>
        <td></td>
        <td><p align="center">:globe_with_meridians:&nbsp;FETCH&nbsp;AND&nbsp;SET</p></td>
        <td><p align="center">:book:&nbsp;USE&nbsp;EXISTING</p></td>
        <td></td>
        <td><p align="center">:book:&nbsp;USE&nbsp;EXISTING</p></td>
      </tr>
      <tr>
        <td>Unset/Deleted</td>
        <td><p align="center">:globe_with_meridians:&nbsp;FETCH&nbsp;AND&nbsp;SET</p></td>
        <td><p align="center">:globe_with_meridians:&nbsp;FETCH&nbsp;AND&nbsp;SET</p></td>
        <td><p align="center">:globe_with_meridians:&nbsp;FETCH&nbsp;AND&nbsp;SET</td>
        <td><p align="center">:globe_with_meridians:&nbsp;FETCH&nbsp;AND&nbsp;SET</p></td>
        <td><p align="center">:globe_with_meridians:&nbsp;FETCH&nbsp;AND&nbsp;SET</p></td>
      </tr>
      <tr>
        <td rowspan="3"><p align="center">FORCE</p></td>
        <td>Unchanged</td>
        <td></td>
        <td><p align="center">:globe_with_meridians:&nbsp;FETCH&nbsp;AND&nbsp;SET</p></td>
        <td><p align="center">:globe_with_meridians:&nbsp;FETCH&nbsp;AND&nbsp;SET</p></td>
        <td></td>
        <td><p align="center">:globe_with_meridians:&nbsp;FETCH&nbsp;AND&nbsp;SET</p></td>
      </tr>
      <tr>
        <td>Changed</td>
        <td></td>
        <td><p align="center">:globe_with_meridians:&nbsp;FETCH&nbsp;AND&nbsp;SET</p></td>
        <td><p align="center">:globe_with_meridians:&nbsp;FETCH&nbsp;AND&nbsp;SET</p></td>
        <td></td>
        <td><p align="center">:globe_with_meridians:&nbsp;FETCH&nbsp;AND&nbsp;SET</p></td>
      </tr>
      <tr>
        <td>Unset/Deleted</td>
        <td><p align="center">:globe_with_meridians:&nbsp;FETCH&nbsp;AND&nbsp;SET</p></td>
        <td><p align="center">:globe_with_meridians:&nbsp;FETCH&nbsp;AND&nbsp;SET</p></td>
        <td><p align="center">:globe_with_meridians:&nbsp;FETCH&nbsp;AND&nbsp;SET</p></td>
        <td><p align="center">:globe_with_meridians:&nbsp;FETCH&nbsp;AND&nbsp;SET</p></td>
        <td><p align="center">:globe_with_meridians:&nbsp;FETCH&nbsp;AND&nbsp;SET</p></td>
      </tr>
    </tbody>
</table>

## Installation

1. Clone the repository
   ```bash
   git clone https://github.com/montblanc0/keycloak-remote-attribute-importer.git
   cd keycloak-remote-attribute-importer
   ```

2. Sync with Maven to download dependencies
   ```bash
   mvn dependency:resolve
   ```

3. Edit the `RemoteDataService` class to configure your specific external source and data handling logic

4. Build the project
   ```bash
   mvn clean install
   ```

5. Copy the JAR file to your Keycloak providers directory
   ```bash
   cp target/remote-attribute-importer-*.jar <IS_HOME>/providers/
   ```

6. Restart Keycloak

## Configuration

1. Navigate to your Identity Provider configuration in Keycloak admin console
2. Go to the "Mappers" tab
3. Click "Add Mapper"
4. Select "Remote Attribute Mapper" from the dropdown list
5. Configure the mapper:
    - **Name**: A descriptive name for this mapper
    - **Sync Mode**: Choose between INHERIT, IMPORT, LEGACY, or FORCE based on your requirements
    - **Attribute Name**: The user attribute name where the fetched value will be stored

## License

Apache License 2.0