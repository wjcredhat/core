/*
 * JBoss, Home of Professional Open Source
 * Copyright 2011 Red Hat Inc. and/or its affiliates and other contributors
 * as indicated by the @authors tag. All rights reserved.
 * See the copyright.txt in the distribution for a
 * full listing of individual contributors.
 *
 * This copyrighted material is made available to anyone wishing to use,
 * modify, copy, or redistribute it subject to the terms and conditions
 * of the GNU Lesser General Public License, v. 2.1.
 * This program is distributed in the hope that it will be useful, but WITHOUT A
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A
 * PARTICULAR PURPOSE.  See the GNU Lesser General Public License for more details.
 * You should have received a copy of the GNU Lesser General Public License,
 * v.2.1 along with this distribution; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston,
 * MA  02110-1301, USA.
 */
package org.switchyard.config.model;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * MergeScanner.
 *
 * @param <M> the Model type to scan for
 *
 * @author David Ward &lt;<a href="mailto:dward@jboss.org">dward@jboss.org</a>&gt; (C) 2011 Red Hat Inc.
 */
public class MergeScanner<M extends Model> implements Scanner<M> {

    private Class<M> _clazz;
    private boolean _fromOverridesTo;
    private List<Scanner<M>> _scanners;

    public MergeScanner(Class<M> clazz, boolean fromOverridesTo, Scanner<M>... scanners) {
        _clazz = clazz;
        _fromOverridesTo = fromOverridesTo;
        List<Scanner<M>> list = new ArrayList<Scanner<M>>();
        if (scanners != null) {
            for (Scanner<M> scanner : list) {
                if (scanner != null) {
                    list.add(scanner);
                }
            }
        }
        _scanners = list;
    }

    public MergeScanner(Class<M> clazz, boolean fromOverridesTo, List<Scanner<M>> scanners) {
        _clazz = clazz;
        _fromOverridesTo = fromOverridesTo;
        _scanners = new ArrayList<Scanner<M>>();
        if (scanners != null) {
            for (Scanner<M> scanner : scanners) {
                if (scanner != null) {
                    _scanners.add(scanner);
                }
            }
        }
    }

    @Override
    public ScannerOutput<M> scan(ScannerInput<M> input) throws IOException {
        M merged;
        try {
            merged = _clazz.newInstance();
        } catch (Exception e) {
            throw new IOException(e);
        }
        for (Scanner<M> scanner : _scanners) {
            List<M> scanned_list = scanner.scan(input).getModels();
            if (scanned_list != null) {
                for (M scanned : scanned_list) {
                    if (scanned != null) {
                        merged = Models.merge(scanned, merged, _fromOverridesTo);
                    }
                }
            }
        }
        return new ScannerOutput<M>().setModel(merged);
    }

}
