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

1. Run the benchmark on a machine with `docker.com` access to make sure all images
are created. You do not need to wait for the benchmark to complete, it can be interrupted
after all images are created.

2. Make sure you have password-less SSH access to your OCI machine and run:
```
./migrate-images myuser hostname
```

3. Log into the OCI machine. You should find all images as tar files in your home directory.
Run the script to load images:
```
./sudo load-images $HOME/*.tar
```

5. Now you are ready to start the benchmark with the special flag `--no-docker-build`:
```
./tfb --no-docker-build -test helidon
./tfb --no-docker-build -test helidon-nima
```