package ir.shecan.util.server;

import android.content.Context;

import ir.shecan.Shecan;
import ir.shecan.service.ShecanVpnService;

import java.util.ArrayList;

/**
 * Shecan Project
 *
 * @author iTX Technologies
 * @link https://itxtech.org
 * <p>
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 */
public class DNSServerHelper {

    public static int getPosition(String id) {
        int intId = Integer.parseInt(id);
        if (intId < Shecan.DNS_SERVERS.size()) {
            return intId;
        }

        for (int i = 0; i < Shecan.configurations.getCustomDNSServers().size(); i++) {
            if (Shecan.configurations.getCustomDNSServers().get(i).getId().equals(id)) {
                return i + Shecan.DNS_SERVERS.size();
            }
        }
        return 0;
    }

    public static String getPrimary() {
        return String.valueOf(DNSServerHelper.checkServerId(Integer.parseInt(Shecan.getPrefs().getString("primary_server", "0"))));
    }

    public static String getSecondary() {
        return String.valueOf(DNSServerHelper.checkServerId(Integer.parseInt(Shecan.getPrefs().getString("secondary_server", "1"))));
    }

    public static String getProPrimary() {
        return String.valueOf(DNSServerHelper.checkServerId(Integer.parseInt(Shecan.getPrefs().getString("pro_primary_server", "2"))));
    }

    public static String getProSecondary() {
        return String.valueOf(DNSServerHelper.checkServerId(Integer.parseInt(Shecan.getPrefs().getString("pro_secondary_server", "3"))));
    }

    private static int checkServerId(int id) {
        if (id < Shecan.DNS_SERVERS.size()) {
            return id;
        }
        for (CustomDNSServer server : Shecan.configurations.getCustomDNSServers()) {
            if (server.getId().equals(String.valueOf(id))) {
                return id;
            }
        }
        return 0;
    }

    public static AbstractDNSServer getDNSById(String id) {
        for (DNSServer server : Shecan.DNS_SERVERS) {
            if (server.getId().equals(id)) {
                return server;
            }
        }
        for (CustomDNSServer customDNSServer : Shecan.configurations.getCustomDNSServers()) {
            if (customDNSServer.getId().equals(id)) {
                return customDNSServer;
            }
        }
        return Shecan.DNS_SERVERS.get(0);
    }

    public static String[] getIds() {
        ArrayList<String> servers = new ArrayList<>(Shecan.DNS_SERVERS.size());
        for (DNSServer server : Shecan.DNS_SERVERS) {
            servers.add(server.getId());
        }
        for (CustomDNSServer customDNSServer : Shecan.configurations.getCustomDNSServers()) {
            servers.add(customDNSServer.getId());
        }
        String[] stringServers = new String[Shecan.DNS_SERVERS.size()];
        return servers.toArray(stringServers);
    }

    public static String[] getNames(Context context) {
        ArrayList<String> servers = new ArrayList<>(Shecan.DNS_SERVERS.size());
        for (DNSServer server : Shecan.DNS_SERVERS) {
            servers.add(server.getStringDescription(context));
        }
        for (CustomDNSServer customDNSServer : Shecan.configurations.getCustomDNSServers()) {
            servers.add(customDNSServer.getName());
        }
        String[] stringServers = new String[Shecan.DNS_SERVERS.size()];
        return servers.toArray(stringServers);
    }

    public static ArrayList<AbstractDNSServer> getAllServers() {
        ArrayList<AbstractDNSServer> servers = new ArrayList<>(Shecan.DNS_SERVERS.size());
        servers.addAll(Shecan.DNS_SERVERS);
        servers.addAll(Shecan.configurations.getCustomDNSServers());
        return servers;
    }

    public static String getDescription(String id, Context context) {
        for (DNSServer server : Shecan.DNS_SERVERS) {
            if (server.getId().equals(id)) {
                return server.getStringDescription(context);
            }
        }
        for (CustomDNSServer customDNSServer : Shecan.configurations.getCustomDNSServers()) {
            if (customDNSServer.getId().equals(id)) {
                return customDNSServer.getName();
            }
        }
        return Shecan.DNS_SERVERS.get(0).getStringDescription(context);
    }

    public static boolean isInUsing(CustomDNSServer server) {
        return ShecanVpnService.isActivated() && (server.getId().equals(getPrimary()) || server.getId().equals(getSecondary()));
    }
}
