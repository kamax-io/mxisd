# Getting started
1. [Preparation](#preparation)
2. [Install](#install)
3. [Configure](#configure)
4. [Integrate](#integrate)
5. [Validate](#validate)
6. [Next steps](#next-steps)

Following these quick start instructions, you will have a basic setup that can perform recursive/federated lookups and
talk to the central Matrix.org Identity service.  
This will be a good ground work for further integration with your existing Identity stores.

## Preparation
You will need:
- Homeserver
- Reverse proxy with regular TLS/SSL certificate (Let's encrypt) for your mxisd domain

As synapse requires an HTTPS connection when talking to an Identity service, a reverse proxy is required as mxisd does
not support HTTPS listener at this time.

For maximum integration, it is best to have your Homeserver and mxisd reachable via the same hostname.  
You can also use a dedicated domain for mxisd, but will not have access to some features.

Be aware of a [NAT/Reverse proxy gotcha](https://github.com/kamax-io/mxisd/wiki/Gotchas#nating) if you use the same
hostname.

The following Quick Start guide assumes you will host the Homeserver and mxisd under the same hostname.  
If you would like a high-level view of the infrastructure and how each feature is integrated, see the
[dedicated document](architecture.md)

## Install
Install via:
- [Debian package](install/debian.md)
- [Docker image](install/docker.md)
- [Sources](build.md)

See the [Latest release](https://github.com/kamax-io/mxisd/releases/latest) for links to each.

## Configure
**NOTE**: please view the install instruction for your platform, as this step might be optional/handled for you.

Create/edit a minimal configuration (see installer doc for the location):
```
matrix.domain: 'MyMatrixDomain.org'
key.path: '/path/to/signing.key.file'
storage.provider.sqlite.database: '/path/to/mxisd.db'
```  
- `matrix.domain` should be set to your Homeserver domain
- `key.path` will store the signing keys, which must be kept safe!
- `storage.provider.sqlite.database` is the location of the SQLite Database file which will hold state (invites, etc.)

If your HS/mxisd hostname is not the same as your Matrix domain, configure `server.name`.  
Complete configuration guide is available [here](configure.md).

## Integrate
For an overview of a typical mxisd infrastructure, see the [dedicated document](architecture.md)
### Reverse proxy
#### Apache2
In the `VirtualHost` section handling the domain with SSL, add the following and replace `0.0.0.0` by the internal
hostname/IP pointing to mxisd.  
**This line MUST be present before the one for the homeserver!**
```
ProxyPass /_matrix/identity/ http://0.0.0.0:8090/_matrix/identity/
```

Typical configuration would look like:
```
<VirtualHost *:443>
    ServerName example.org
    
    ...
    
    ProxyPreserveHost on
    ProxyPass /_matrix/identity/ http://localhost:8090/_matrix/identity/
    ProxyPass /_matrix/ http://localhost:8008/_matrix/
</VirtualHost>
```

#### nginx
In the `server` section handling the domain with SSL, add the following and replace `0.0.0.0` with the internal
hostname/IP pointing to mxisd.
**This line MUST be present before the one for the homeserver!**
```
location /_matrix/identity {
    proxy_pass http://0.0.0.0:8090/_matrix/identity;
}
```

Typical configuration would look like:
```
server {
    listen 443 ssl;
    server_name example.org;
    
    ...
    
    location /_matrix/identity {
        proxy_pass http://localhost:8090/_matrix/identity;
        proxy_set_header Host $host;
        proxy_set_header X-Forwarded-For $remote_addr;
    }
    
    location /_matrix {
        proxy_pass http://localhost:8008/_matrix;
        proxy_set_header Host $host;
        proxy_set_header X-Forwarded-For $remote_addr;
    }
}
```

### Synapse
Add your mxisd domain into the `homeserver.yaml` at `trusted_third_party_id_servers` and restart synapse.  
In a typical configuration, you would end up with something similair to:
```
trusted_third_party_id_servers:
    - matrix.org
    - vector.im
    - example.org
```
It is recommended to remove `matrix.org` and `vector.im` so only your own Identity server is authoritative for your HS.

## Validate
Log in using your Matrix client and set `https://example.org` as your Identity server URL, replacing `example.org` by
the relevant hostname which you configured in your reverse proxy.  
Invite `mxisd-lookup-test@kamax.io` to a room, which should be turned into a Matrix invite to `@mxisd-lookup-test:kamax.io`.  
**NOTE:** you might not see a Matrix suggestion for the e-mail address, which is normal. Still proceed with the invite.
  
If it worked, it means you are up and running and can enjoy mxisd in its basic mode! Congratulations!  
If it did not work, [get in touch](#support) and we'll do our best to get you started.

You can now integrate mxisd further with your infrastructure using the various [features](README.md) guides.

## Next steps
Once your mxisd server is up and running, here are the next steps to further enhance and integrate your installation:

Enable extra features:
- [Federation](features/federation.md)
- [Authenticate with synapse](features/authentication.md), profile auto-provisioning if you wish
- [Directory search](features/directory-users.md)

Use your Identity stores:
- [LDAP / Samba / Active directory](backends/ldap.md)
- [SQL Database](backends/sql.md)
- [Website / Web service / Web app](backends/rest.md)
- [Google Firebase](backends/firebase.md)
- [Wordpress](backends/wordpress.md)
