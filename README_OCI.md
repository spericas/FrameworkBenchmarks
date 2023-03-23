## Running Benchmark in OCI

Running the `tfb` script in OCI is not possible due to the latest `docker.com` restrictions. 
The `tfb` script builds all Docker images every time it runs, but has been modified in
this workspace to support a new `--no-docker-build` flag. Using this flag _requires_
all necessary Docker images to be pre-installed on your system.

You can migrate those images from another system where access to `docker.com` is available
(such as your own personal laptop). The script `migrate-images` shall save
each of the TE images into a tar file and migrate it over to your OCI machine provided
you have SSH access without a password. Once  copied over to your OCI machine (in your
home directory), you can run the `load-images` script to make them available to Docker
--this process typically requires running in "sudo" mode. 

### Example

After copying and loading the images, you can run:

```
./tfb --no-docker-build -test helidon
./tfb --no-docker-build -test helidon-nima
```