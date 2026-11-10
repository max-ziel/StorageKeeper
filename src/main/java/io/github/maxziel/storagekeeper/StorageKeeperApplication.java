package io.github.maxziel.storagekeeper;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Entry point of the pantry app. Component scanning starts here, so all adapters, services and
 * configurations live in sub-packages of this one.
 */
@SpringBootApplication
public class StorageKeeperApplication {

    static void main(String[] args) {
        SpringApplication.run(StorageKeeperApplication.class, args);
    }
}
