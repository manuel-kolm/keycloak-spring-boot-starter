package org.springframework.keycloak.config;

import java.util.ArrayList;
import java.util.List;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "keycloak.server")
public class KeycloakServerProperties {

	private String contextPath = "/auth";
    private List<String> realmImportFiles;
    private AdminUser adminUser = new AdminUser();

    public String getContextPath() {
        return contextPath;
    }

    public KeycloakServerProperties setContextPath(String contextPath) {
        this.contextPath = contextPath;
        return this;
    }

    public AdminUser getAdminUser() {
        return adminUser;
    }

    public KeycloakServerProperties setAdminUser(AdminUser adminUser) {
        this.adminUser = adminUser;
        return this;
    }

    public List<String> getRealmImportFiles() {
    	if (realmImportFiles == null) {
    		realmImportFiles = new ArrayList<>();
    	}
        return realmImportFiles;
    }

    public KeycloakServerProperties setRealmImportFile(List<String> realmImportFiles) {
        this.realmImportFiles = realmImportFiles;
        return this;
    }

    public static class AdminUser {

        private String username = "admin";
        private String password = "admin";

        public String getUsername() {
            return username;
        }

        public AdminUser setUsername(String username) {
            this.username = username;
            return this;
        }

        public String getPassword() {
            return password;
        }

        public AdminUser setPassword(String password) {
            this.password = password;
            return this;
        }
    }
}
