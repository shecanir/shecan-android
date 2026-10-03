package ir.shecan.data.modelDto;

import java.util.List;

public class VerifyApiViewModel {

    private Boolean admin;
    private String apiKey;
    private String createdOn;
    private String firstname;
    private int id;
    private String lastLoginOn;
    private String lastname;
    private String login;
    private String mail;
    private String passwdChangedOn;
    private int status;
    private String updatedOn;
    private List<CustomFieldsDTO> customFields;

    public VerifyApiViewModel(Boolean admin, String apiKey, String createdOn, String firstname, int id, String lastLoginOn, String lastname, String login, String mail, String passwdChangedOn, int status, Object twofaScheme, String updatedOn, List<CustomFieldsDTO> customFields) {
        this.admin = admin;
        this.apiKey = apiKey;
        this.createdOn = createdOn;
        this.firstname = firstname;
        this.id = id;
        this.lastLoginOn = lastLoginOn;
        this.lastname = lastname;
        this.login = login;
        this.mail = mail;
        this.passwdChangedOn = passwdChangedOn;
        this.status = status;
        this.updatedOn = updatedOn;
        this.customFields = customFields;
    }

    public Boolean getAdmin() {
        return admin;
    }

    public void setAdmin(Boolean admin) {
        this.admin = admin;
    }

    public String getApiKey() {
        return apiKey;
    }

    public void setApiKey(String apiKey) {
        this.apiKey = apiKey;
    }

    public String getCreatedOn() {
        return createdOn;
    }

    public void setCreatedOn(String createdOn) {
        this.createdOn = createdOn;
    }

    public String getFirstname() {
        return firstname;
    }

    public void setFirstname(String firstname) {
        this.firstname = firstname;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getLastLoginOn() {
        return lastLoginOn;
    }

    public void setLastLoginOn(String lastLoginOn) {
        this.lastLoginOn = lastLoginOn;
    }

    public String getLastname() {
        return lastname;
    }

    public void setLastname(String lastname) {
        this.lastname = lastname;
    }

    public String getLogin() {
        return login;
    }

    public void setLogin(String login) {
        this.login = login;
    }

    public String getMail() {
        return mail;
    }

    public void setMail(String mail) {
        this.mail = mail;
    }

    public String getPasswdChangedOn() {
        return passwdChangedOn;
    }

    public void setPasswdChangedOn(String passwdChangedOn) {
        this.passwdChangedOn = passwdChangedOn;
    }

    public int getStatus() {
        return status;
    }

    public void setStatus(int status) {
        this.status = status;
    }

    public String getUpdatedOn() {
        return updatedOn;
    }

    public void setUpdatedOn(String updatedOn) {
        this.updatedOn = updatedOn;
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

        public CustomFieldsDTO(int id, String name, String value) {
            this.id = id;
            this.name = name;
            this.value = value;
        }

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
