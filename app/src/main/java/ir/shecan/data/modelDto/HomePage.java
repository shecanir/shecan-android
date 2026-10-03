package ir.shecan.data.modelDto;

import java.util.List;

public class HomePage {

    private String currentVersion;
    private String minVersion;
    private VersionDTO version;
    private String updateLink;
    private String bannerImageUrl;
    private String bannerLink;
    private AppStoreRateDTO appStoreRate;
    private BannerServiceDTO bannerService;
    private String dynamicIpGuideLink;
    private String ticketingLink;
    private String purchaseLink;
    private String donationLink;
    private String currentIpLink;
    private OtlpDTO otlp;
    private List<String> proDns;
    private List<String> freeDns;
    private List<String> proDnsUdp;
    private List<String> freeDnsUdp;
    private DynamicDataDTO dynamicData;
    private MonitoringDTO monitoring;
    private SentryDTO sentry;
    private Integer sentrySampleRate;

    public String getCurrentVersion() {
        return currentVersion;
    }

    public void setCurrentVersion(String currentVersion) {
        this.currentVersion = currentVersion;
    }

    public String getMinVersion() {
        return minVersion;
    }

    public void setMinVersion(String minVersion) {
        this.minVersion = minVersion;
    }

    public VersionDTO getVersion() {
        return version;
    }

    public void setVersion(VersionDTO version) {
        this.version = version;
    }

    public String getUpdateLink() {
        return updateLink;
    }

    public void setUpdateLink(String updateLink) {
        this.updateLink = updateLink;
    }

    public String getBannerImageUrl() {
        return bannerImageUrl;
    }

    public void setBannerImageUrl(String bannerImageUrl) {
        this.bannerImageUrl = bannerImageUrl;
    }

    public String getBannerLink() {
        return bannerLink;
    }

    public void setBannerLink(String bannerLink) {
        this.bannerLink = bannerLink;
    }

    public BannerServiceDTO getBannerService() {
        return bannerService;
    }

    public void setBannerService(BannerServiceDTO bannerService) {
        this.bannerService = bannerService;
    }

    public AppStoreRateDTO getAppStoreRate() {
        return appStoreRate;
    }

    public void setAppStoreRate(AppStoreRateDTO appStoreRate) {
        this.appStoreRate = appStoreRate;
    }

    public String getDynamicIpGuideLink() {
        return dynamicIpGuideLink;
    }

    public void setDynamicIpGuideLink(String dynamicIpGuideLink) {
        this.dynamicIpGuideLink = dynamicIpGuideLink;
    }

    public String getTicketingLink() {
        return ticketingLink;
    }

    public void setTicketingLink(String ticketingLink) {
        this.ticketingLink = ticketingLink;
    }

    public String getPurchaseLink() {
        return purchaseLink;
    }

    public void setPurchaseLink(String purchaseLink) {
        this.purchaseLink = purchaseLink;
    }

    public String getDonationLink() {
        return donationLink;
    }

    public void setDonationLink(String donationLink) {
        this.donationLink = donationLink;
    }

    public String getCurrentIpLink() {
        return currentIpLink;
    }

    public void setCurrentIpLink(String currentIpLink) {
        this.currentIpLink = currentIpLink;
    }

    public OtlpDTO getOtlp() {
        return otlp;
    }

    public void setOtlp(OtlpDTO otlp) {
        this.otlp = otlp;
    }

    public List<String> getProDns() {
        return proDns;
    }

    public void setProDns(List<String> proDns) {
        this.proDns = proDns;
    }

    public List<String> getFreeDns() {
        return freeDns;
    }

    public void setFreeDns(List<String> freeDns) {
        this.freeDns = freeDns;
    }

    public List<String> getProDnsUdp() {
        return proDnsUdp;
    }

    public void setProDnsUdp(List<String> proDnsUdp) {
        this.proDnsUdp = proDnsUdp;
    }

    public List<String> getFreeDnsUdp() {
        return freeDnsUdp;
    }

    public void setFreeDnsUdp(List<String> freeDnsUdp) {
        this.freeDnsUdp = freeDnsUdp;
    }

    public DynamicDataDTO getDynamicData() {
        return dynamicData;
    }

    public void setDynamicData(DynamicDataDTO dynamicData) {
        this.dynamicData = dynamicData;
    }

    public MonitoringDTO getMonitoring() {
        return monitoring;
    }

    public void setMonitoring(MonitoringDTO monitoring) {
        this.monitoring = monitoring;
    }

    public Integer getSentrySampleRate() {
        if (sentry != null && sentry.getAndroid() != null
                && sentry.getAndroid().getSampleRate() != null) {
            return sentry.getAndroid().getSampleRate();
        }
        return sentrySampleRate;
    }

    public void setSentrySampleRate(Integer sentrySampleRate) {
        this.sentrySampleRate = sentrySampleRate;
    }

    public SentryDTO getSentry() {
        return sentry;
    }

    public void setSentry(SentryDTO sentry) {
        this.sentry = sentry;
    }

    public static class VersionDTO {
        private WindowsDTO windows;
        private LinuxDTO linux;
        private DarwinDTO darwin;
        private AndroidDTO android;

        public WindowsDTO getWindows() {
            return windows;
        }

        public void setWindows(WindowsDTO windows) {
            this.windows = windows;
        }

        public LinuxDTO getLinux() {
            return linux;
        }

        public void setLinux(LinuxDTO linux) {
            this.linux = linux;
        }

        public DarwinDTO getDarwin() {
            return darwin;
        }

        public void setDarwin(DarwinDTO darwin) {
            this.darwin = darwin;
        }

        public AndroidDTO getAndroid() {
            return android;
        }

        public void setAndroid(AndroidDTO android) {
            this.android = android;
        }

        public static class WindowsDTO {
            private AllDTO all;

            public AllDTO getAll() {
                return all;
            }

            public void setAll(AllDTO all) {
                this.all = all;
            }

            public static class AllDTO {
                private String currentVersion;
                private String minVersion;

                public String getCurrentVersion() {
                    return currentVersion;
                }

                public void setCurrentVersion(String currentVersion) {
                    this.currentVersion = currentVersion;
                }

                public String getMinVersion() {
                    return minVersion;
                }

                public void setMinVersion(String minVersion) {
                    this.minVersion = minVersion;
                }
            }
        }

        public static class LinuxDTO {
            private AllDTOX all;

            public AllDTOX getAll() {
                return all;
            }

            public void setAll(AllDTOX all) {
                this.all = all;
            }

            public static class AllDTOX {
                private String currentVersion;
                private String minVersion;

                public String getCurrentVersion() {
                    return currentVersion;
                }

                public void setCurrentVersion(String currentVersion) {
                    this.currentVersion = currentVersion;
                }

                public String getMinVersion() {
                    return minVersion;
                }

                public void setMinVersion(String minVersion) {
                    this.minVersion = minVersion;
                }
            }
        }

        public static class DarwinDTO {
            private AllDTOXX all;

            public AllDTOXX getAll() {
                return all;
            }

            public void setAll(AllDTOXX all) {
                this.all = all;
            }

            public static class AllDTOXX {
                private String currentVersion;
                private String minVersion;

                public String getCurrentVersion() {
                    return currentVersion;
                }

                public void setCurrentVersion(String currentVersion) {
                    this.currentVersion = currentVersion;
                }

                public String getMinVersion() {
                    return minVersion;
                }

                public void setMinVersion(String minVersion) {
                    this.minVersion = minVersion;
                }
            }
        }

        public static class AndroidDTO {
            private String currentVersion;
            private String minVersion;

            public String getCurrentVersion() {
                return currentVersion;
            }

            public void setCurrentVersion(String currentVersion) {
                this.currentVersion = currentVersion;
            }

            public String getMinVersion() {
                return minVersion;
            }

            public void setMinVersion(String minVersion) {
                this.minVersion = minVersion;
            }
        }
    }

    public static class BannerServiceDTO {
        private String windows;
        private String android;

        public String getWindows() {
            return windows;
        }

        public void setWindows(String windows) {
            this.windows = windows;
        }

        public String getAndroid() {
            return android;
        }

        public void setAndroid(String android) {
            this.android = android;
        }
    }
    public static class AppStoreRateDTO {
        private String android;

        public String getAndroid() {
            return android;
        }

        public void setAndroid(String android) {
            this.android = android;
        }
    }

    public static class OtlpDTO {
        private String https;
        private String grpc;

        public String getHttps() {
            return https;
        }

        public void setHttps(String https) {
            this.https = https;
        }

        public String getGrpc() {
            return grpc;
        }

        public void setGrpc(String grpc) {
            this.grpc = grpc;
        }
    }

    public static class DynamicDataDTO {
        private String banner;
        private DialogDTO dialog;

        public String getBanner() {
            return banner;
        }

        public void setBanner(String banner) {
            this.banner = banner;
        }

        public DialogDTO getDialog() {
            return dialog;
        }

        public void setDialog(DialogDTO dialog) {
            this.dialog = dialog;
        }

        public static class DialogDTO {
            private String match;
            private String dismiss;
            private String action;

            public String getMatch() {
                return match;
            }

            public void setMatch(String match) {
                this.match = match;
            }

            public String getDismiss() {
                return dismiss;
            }

            public void setDismiss(String dismiss) {
                this.dismiss = dismiss;
            }

            public String getAction() {
                return action;
            }

            public void setAction(String action) {
                this.action = action;
            }
        }
    }

    public static class MonitoringDTO {
        private String target;
        private String logs;

        public String getTarget() {
            return target;
        }

        public void setTarget(String target) {
            this.target = target;
        }

        public String getLogs() {
            return logs;
        }

        public void setLogs(String logs) {
            this.logs = logs;
        }
    }

    public static class SentryDTO {
        private SentryPlatformDTO desktop;
        private SentryPlatformDTO android;

        public SentryPlatformDTO getDesktop() {
            return desktop;
        }

        public void setDesktop(SentryPlatformDTO desktop) {
            this.desktop = desktop;
        }

        public SentryPlatformDTO getAndroid() {
            return android;
        }

        public void setAndroid(SentryPlatformDTO android) {
            this.android = android;
        }
    }

    public static class SentryPlatformDTO {
        private Integer sampleRate;
        private String url;

        public Integer getSampleRate() {
            return sampleRate;
        }

        public void setSampleRate(Integer sampleRate) {
            this.sampleRate = sampleRate;
        }

        public String getUrl() {
            return url;
        }

        public void setUrl(String url) {
            this.url = url;
        }
    }
}
