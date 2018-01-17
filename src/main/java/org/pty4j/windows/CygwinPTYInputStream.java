/*
 * Copyright (c) 2000, 2011 QNX Software Systems and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.pty4j.windows;


import java.io.IOException;
import java.io.InputStream;

public class CygwinPTYInputStream extends InputStream {
    private final NamedPipe myNamedPipe;
    private boolean myClosed;

    CygwinPTYInputStream(NamedPipe namedPipe) {
        myNamedPipe = namedPipe;
    }

    /**
     * Implementation of read for the InputStream.
     */
    @Override
    public int read() {
        byte b[] = new byte[1];
        if (1 != read(b, 0, 1)) {
            return -1;
        }
        return b[0];
    }

    @SuppressWarnings("NullableProblems")
    @Override
    public int read(byte[] buf, int off, int len) {
        if (myClosed) {
            return 0;
        }
        return myNamedPipe.read(buf, off, len);
    }

    @Override
    public void close() {
        myClosed = true;
        myNamedPipe.markClosed();
    }

    @Override
    public int available() throws IOException {
        return myNamedPipe.available();
    }

    @Override
    protected void finalize() throws Throwable {
        close();
        super.finalize();
    }
}
