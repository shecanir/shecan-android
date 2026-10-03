package ir.shecan.data.modelDto;

import java.util.List;

public class AccountViewModel {

    private UserDTO user;

    public UserDTO getUser() {
        return user;
    }

    public void setUser(UserDTO user) {
        this.user = user;
    }

    public static class UserDTO {
        private int id;
        private String login;
        private boolean admin;
        private String firstname;
        private String lastname;
        private String mail;
        private String createdOn;
        private String lastLoginOn;
        private String apiKey;
        private List<CustomFieldsDTO> customFields;

        public int getId() {
            return id;
        }

        public void setId(int id) {
            this.id = id;
        }

        public String getLogin() {
            return login;
        }

        public void setLogin(String login) {
            this.login = login;
        }

        public boolean isAdmin() {
            return admin;
        }

        public void setAdmin(boolean admin) {
            this.admin = admin;
        }

        public String getFirstname() {
            return firstname;
        }

        public void setFirstname(String firstname) {
            this.firstname = firstname;
        }

        public String getLastname() {
            return lastname;
        }

        public void setLastname(String lastname) {
            this.lastname = lastname;
        }

        public String getMail() {
            return mail;
        }

        public void setMail(String mail) {
            this.mail = mail;
        }

        public String getCreatedOn() {
            return createdOn;
        }

        public void setCreatedOn(String createdOn) {
            this.createdOn = createdOn;
        }

        public String getLastLoginOn() {
            return lastLoginOn;
        }

        public void setLastLoginOn(String lastLoginOn) {
            this.lastLoginOn = lastLoginOn;
        }

        public String getApiKey() {
            return apiKey;
        }

        public void setApiKey(String apiKey) {
            this.apiKey = apiKey;
        }

        public List<CustomFieldsDTO> getCustomFields() {
            return customFields;
        }

        public void setCustomFields(List<CustomFieldsDTO> customFields) {
            this.customFields = customFields;
        }

        public static class CustomFieldsDTO {
            private int id;
            private String name;
            private String value;

            public int getId() {
                return id;
            }

            public void setId(int id) {
                this.id = id;
            }

            public String getName() {
                return name;
            }

            public void setName(String name) {
                this.name = name;
            }

            public String getValue() {
                return value;
            }

            public void setValue(String value) {
                this.value = value;
            }
        }
    }
}
