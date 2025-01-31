# Temporal Demo Server

## TODO - Document

# Build and run with docker
As a springboot application this app is really easy to build using pack (or for k8s lovers kpack)

<For Mac users>
1. Install pack cli

```
$ brew install buildpacks/tap/pack
```

2. Build an immage that we can use via docker.
From the base directory of the temporal-demo-server source.

```
pack build temporal-demo-server --path . --env BP_JVM_VERSION=21 --builder heroku/builder:24
```
Notes
* Aiming to use Java 21 to be relatively modern.  (At least at the time of writing.)  Currently Java21 is not the default for buildpacks so explicitly adding the env variable to ensure it is downloaded to the image.
* Using the heroku builder as this builds for AMD architecture.  If your running this image in the cloud you are likely to want to switch to paketobuildpacks/builder-jammy-full for an intel based image.

3. Run the image in docker.

```
docker run --rm -p 127.0.0.1:8090:8080 -d -v /Users/donald/stuff/source/certificates:/Users/donald/stuff/source/certificates temporal-demo-server
```
Notes:
* The application properties specify to use port 8090 as this was useful during initial development to have it on the non-standard port.  The buildpack overrides this to port 8080.  Hence my exposure to local port 8090 as it matches my local dev.
* The current codebase loads the certificates used for mTLS authentication from my local laptop.  Hence mapping the directory into the docker image for usage.  Looking to replace this with vault or K8s secret in the near future.
