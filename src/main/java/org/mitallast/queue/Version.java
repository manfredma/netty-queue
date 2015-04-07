package org.mitallast.queue;

import java.io.IOException;

public class Version {
    public static final int V1_0_0_ID = 10000;
    public static final Version V1_0_0 = new Version(V1_0_0_ID);

    public final int id;

    Version(int id) {
        this.id = id;
    }

    public static Version fromId(int id) throws IOException {
        switch (id) {
            case V1_0_0_ID:
                return V1_0_0;
            default:
                throw new IOException("Unexpected id[" + id + "]");
        }
    }
}