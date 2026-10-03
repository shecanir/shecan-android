package ir.shecan.data.modelDto;

import java.util.List;

public class ServicesViewModel {

    private List<ServiceDTO> services;
    private String defaultService;
    private List<DurationDTO> duration;

    public List<ServiceDTO> getServices() {
        return services;
    }

    public String getDefaultService() {
        return defaultService;
    }

    public List<DurationDTO> getDuration() {
        return duration;
    }

    public ServiceDTO findByPeygirCode(int peygirCode) {
        if (services == null) return null;
        for (ServiceDTO service : services) {
            if (service != null && service.getPeygirCode() == peygirCode) {
                return service;
            }
        }
        return null;
    }

    public DurationDTO findDurationById(int id) {
        if (duration == null) return null;
        for (DurationDTO item : duration) {
            if (item != null && item.getId() == id) {
                return item;
            }
        }
        return null;
    }

    public static class ServiceDTO {
        private String nameFa;
        private String nameEn;
        private String code;
        private int peygirCode;
        private boolean purchaseEnabled;
        private boolean renewalEnabled;
        private List<FeatureDTO> features;
        private List<String> allowedChangeCodes;

        public String getNameFa() {
            return nameFa;
        }

        public String getNameEn() {
            return nameEn;
        }

        public String getCode() {
            return code;
        }

        public int getPeygirCode() {
            return peygirCode;
        }

        public boolean isPurchaseEnabled() {
            return purchaseEnabled;
        }

        public boolean isRenewalEnabled() {
            return renewalEnabled;
        }

        public List<FeatureDTO> getFeatures() {
            return features;
        }

        public List<String> getAllowedChangeCodes() {
            return allowedChangeCodes;
        }
    }

    public static class FeatureDTO {
        private String name;
        private String title;
        private String text;
        private long value;

        public String getName() {
            return name;
        }

        public String getTitle() {
            return title;
        }

        public String getText() {
            return text;
        }

        public long getValue() {
            return value;
        }
    }

    public static class DurationDTO {
        private int id;
        private String key;
        private String text;
        private String title;
        private String bonus;

        public int getId() {
            return id;
        }

        public String getKey() {
            return key;
        }

        public String getText() {
            return text;
        }

        public String getTitle() {
            return title;
        }

        public String getBonus() {
            return bonus;
        }
    }
}
