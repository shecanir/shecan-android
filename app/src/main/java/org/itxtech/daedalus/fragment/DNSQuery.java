package org.itxtech.daedalus.fragment;

import java.io.IOException;
import java.net.InetAddress;

import de.measite.minidns.DNSMessage;
import de.measite.minidns.source.NetworkDataSource;

public class DNSQuery extends NetworkDataSource {
    public DNSMessage query(DNSMessage message, InetAddress address, int port) throws IOException {
        return queryUdp(message, address, port);
    }
}
