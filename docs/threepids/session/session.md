# 3PID Sessions
- [Overview](#overview)
- [Purpose](#purpose)
- [Federation](#federation)
  - [3PID scope](#3pid-scope)
  - [Session scope](#session-scope)
- [Notifications](#notifications)
  - [Email](#email)
  - [Phone numbers](#msisdn-(phone-numbers))
- [Usage](#usage)
  - [Configuration](#configuration)
  - [Web views](#web-views)
  - [Scenarios](#scenarios)
    - [Default](#default)
    - [Local sessions only](#local-sessions-only)
    - [Remote sessions only](#remote-sessions-only)
    - [Sessions disabled](#sessions-disabled)

## Overview
When adding an email, a phone number or any other kind of 3PID (Third-Party Identifier) in a Matrix client, 
the identity server is called to validate the 3PID.

Once this 3PID is validated, the Homeserver will publish the user Matrix ID on the Identity Server and
add this 3PID to the Matrix account which initiated the request.

## Purpose
This serves two purposes:
- Add the 3PID as an administrative/login info for the Homeserver directly
- Publish, or *Bind*, the 3PID so it can be queried from Homeservers and clients when inviting someone in a room
by a 3PID, allowing it to be resolved to a Matrix ID.

## Federation
Federation is based on the principle that one can get a domain name and serve services and information within that
domain namespace in a way which can be discovered following a specific protocol or specification.

In the Matrix eco-system, some 3PID can be federated (e.g. emails) while some others cannot (phone numbers).
Also, Matrix users might add 3PIDs that would not point to the Identity server that actually holds the 3PID binding.  

Example: a user from Homeserver `example.org` adds an email `john@gmail.com`.  
If a federated lookup was performed, Identity servers would try to find the 3PID bind at the `gmail.com` server, and
not `example.org`.

To allow global publishing of 3PID bindings to be found anywhere within the current protocol specification, one would
perform a *Remote session* and *Remote bind*, effectively starting a new 3PID session with another Identity server on
behalf of the user.  
To ensure lookup works consistency within the current Matrix network, the central Matrix.org Identity Server should be
used to store *remote* sessions and binds.

On the flip side, at the time of writing, the Matrix specification and the central Matrix.org servers do not allow to
remote a 3PID bind. This means that once a 3PID is published (email, phone number, etc.), it cannot be easily removed
and would require contacting the Matrix.org administrators for each bind individually.  
This poses a privacy, control and security concern, especially for groups/corporations that want to keep a tight control
on where such identifiers can be made publicly visible.

To ensure full control, validation management rely on two concepts:
- The scope of 3PID being validated
- The scope of 3PID sessions that should be possible/offered

### 3PID scope
3PID can either be scoped as local or remote.

Local means that they can looked up using federation and that such federation call would end up on the local
Identity Server.  
Remote means that they cannot be lookup using federation or that a federation call would not end up on the local
Identity Server.

Email addresses can either be local or remote 3PID, depending on the domain. If the address is one from the configured
domain in the Identity server, it will be scoped as local. If it is from another domain, it will be as remote.

Phone number can only be scoped as remote, since there is currently no way to perform DNS queries that would lead back
to the Identity server who validated the phone number.

### Session scope
Sessions can be scoped as:
- Local only - validate 3PIDs directly, do not allow the creation of 3PID sessions on a remote Identity server.
- Local and Remote - validate 3PIDs directly, offer users to option to also validate and bind 3PID on another server.
- Remote only - validate and bind 3PIDs on another server, no validation or bind done locally.

---

**IMPORTANT NOTE:** mxisd does not store bindings directly. While a user can see its email, phone number or any other
3PID in its settings/profile, it does **NOT** mean it is published anywhere and can be used to invite/search the user.
Identity stores are the ones holding such data.  
If you still want added arbitrary 3PIDs to be discoverable on a synapse Homeserver, use the corresponding [Identity store](../../stores/synapse.md).

See the [Scenarios](#scenarios) for more info on how and why.

## Notifications
3PIDs are validated by sending a pre-formatted message containing a token to that 3PID address, which must be given to the
Identity server that received the request. This is usually done by means of a URL to visit for email or a short number
received by SMS for phone numbers.

mxisd use two components for this:
- Generator which produces the message to be sent with the necessary information the user needs to validate their session.
- Connector which actually send the notification (e.g. SMTP for email).

Built-in generators and connectors for supported 3PID types:

### Email
Generators:
- [Template](../notification/template-generator.md)

Connectors:
- [SMTP](../medium/email/smtp-connector.md)

#### MSISDN (Phone numbers)
Generators:
- [Template](../notification/template-generator.md)

Connectors:
 - [Twilio](../medium/msisdn/twilio-connector.md) with SMS

## Usage
### Configuration
The following example of configuration (incomplete extract) shows which items are relevant for 3PID sessions.

**IMPORTANT:** Most configuration items shown have default values and should not be included in your own configuration
file unless you want to specifically overwrite them.
```yaml
# DO NOT COPY/PASTE THIS IN YOUR CONFIGURATION
session.policy.validation.enabled: true
session.policy.validation.forLocal:
  enabled: true
  toLocal: true
  toRemote:
    enabled: true
    server: 'configExample'  # Not to be included in config! Already present in default config!
session.policy.validation.forRemote:
  enabled: true
  toLocal: true
  toRemote:
    enabled: true
    server: 'configExample'  # Not to be included in config! Already present in default config!
# DO NOT COPY/PASTE THIS IN YOUR CONFIGURATION
```

`session.policy.validation` is the core configuration to control what users configured to use your Identity server
are allowed to do in terms of 3PID sessions.

The policy is divided contains a global on/off switch for 3PID sessions using `.enabled`  
It is also divided into two sections: `forLocal` and `forRemote` which refers to the 3PID scopes.  

Each scope is divided into three parts:
- global on/off switch for 3PID sessions using `.enabled`
- `toLocal` allowing or not local 3PID session validations
- `toRemote` allowing or not remote 3PID session validations and to which server such sessions should be sent. 
`.server` takes a Matrix Identity server list label. Only the first server in the list is currently used.

If both `toLocal` and `toRemote` are enabled, the user will be offered to initiate a remote session once their 3PID
locally validated.

### Web views
Once a user click on a validation link, it is taken to the Identity Server validation page where the token is submitted.  
If the session or token is invalid, an error page is displayed.  
Workflow pages are also available for the remote 3PID session process.

See [the dedicated document](session-views.md)
on how to configure/customize/brand those pages to your liking.

### Scenarios
It is important to keep in mind that mxisd does not create bindings, irrelevant if a user added a 3PID to their profile.  
Instead, when queried for bindings, mxisd will query Identity stores which are responsible to store this kind of information.

This has the side effect that any 3PID added to a user profile which is NOT within a configured and enabled Identity backend
will simply not be usable for search or invites, **even on the same Homeserver!**  
mxisd does not store binds on purpose, as one of its primary goal is to ensure maximum compatibility with federation
and the rest of the Matrix ecosystem is preserved.

Nonetheless, because mxisd also aims at offering support for tight control over identity data, it is possible to have
such 3PID bindings available for search and invite queries on synapse with the corresponding [Identity store](../../stores/synapse.md).

See the [Local sessions only](#local-sessions-only) use case for more information on how to configure.

#### Default
By default, mxisd allows the following:

|                 | Local Session     | Remote Session |
|-----------------|-------------------|----------------|
| **Local 3PID**  | Yes               | Yes, offered   |
| **Remote 3PID** | No, Remote forced | Yes            |

This is usually what people expect and will feel natural to users and does not involve further integration.

This allows to stay in control for e-mail addresses which domain matches your Matrix environment, still making them
discoverable with federation but not recorded in a 3rd party Identity server which is not under your control.  
Users still get the possibility to publish globally their address if needed.

Other e-mail addresses and phone number will be redirected to remote sessions to ensure full compatibility with the Matrix
ecosystem and other federated servers.

#### Local sessions only
**NOTE:** This does not affect 3PID lookups (queries to find Matrix IDs). See [Federation](../../features/federation.md)
to disable remote lookup for those.

This configuration ensures maximum confidentiality and privacy.
Typical use cases:
- Private Homeserver, not federated
- Internal Homeserver without direct Internet access
- Custom product based on Matrix which does not federate

No 3PID will be sent to a remote Identity server and all validation will be performed locally.  
On the flip side, people with *Remote* 3PID scopes will not be found from other servers.

Use the following values:
```yaml
session.policy.validation.enabled: true
session.policy.validation.forLocal:
  enabled: true
  toLocal: true
  toRemote:
    enabled: false
session.policy.validation.forRemote:
  enabled: true
  toLocal: true
  toRemote:
    enabled: false
```

**IMPORTANT**: When using local-only mode and if you are using synapse, you will also need to enable its dedicated Identity
store if you want user searches and invites to work. To do so, see the [dedicated document](../../stores/synapse.md).

#### Remote sessions only
This configuration ensures all 3PID are made public for maximum compatibility and reach within the Matrix ecosystem, at
the cost of confidentiality and privacy.  

Typical use cases:
- Public Homeserver
- Homeserver with registration enabled

Use the following values:
```yaml
session.policy.validation.enabled: true
session.policy.validation.forLocal:
  enabled: true
  toLocal: false
  toRemote:
    enabled: true
session.policy.validation.forRemote:
  enabled: true
  toLocal: false
  toRemote:
    enabled: true
```

#### Sessions disabled
This configuration would disable 3PID session altogether, preventing users from adding emails and/or phone numbers to
their profiles.  
This would be used if mxisd is also performing authentication for the Homeserver, typically with synapse and the
[REST password provider](https://github.com/kamax-io/matrix-synapse-rest-auth).

**This mode comes with several important restrictions:**
- This does not prevent users from removing 3PID from their profile. They would be unable to add them back!
- This prevents users from initiating remote session to make their 3PID binds globally visible

It is therefore recommended to not fully disable sessions but instead restrict specific set of 3PID and Session scopes.

Use the following values to enable this mode:
```yaml
session.policy.validation.enabled: false
```
