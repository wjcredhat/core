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

package org.switchyard.transform.jaxb.internal;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBElement;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;
import javax.xml.namespace.QName;
import javax.xml.transform.Source;

import org.switchyard.Message;
import org.switchyard.common.xml.QNameUtil;
import org.switchyard.config.model.Scannable;
import org.switchyard.exception.SwitchYardException;
import org.switchyard.transform.BaseTransformer;

/**
 * JAXB Unmarshalling transformer.
 *
 * @author <a href="mailto:tom.fennelly@gmail.com">tom.fennelly@gmail.com</a>
 *
 * @param <F> From Type
 * @param <T> To Type.
 */
@Scannable(false)
public class JAXBUnmarshalTransformer<F, T> extends BaseTransformer<Message, Message> {

    private JAXBContext _jaxbContext;

    /**
     * Public constructor.
     * @param from From type.
     * @param to To type.
     * @param contextPath JAXB context path (Java package).
     * @throws SwitchYardException Failed to create JAXBContext.
     */
    public JAXBUnmarshalTransformer(QName from, QName to, String contextPath) throws SwitchYardException {
        super(from, to);
        try {
            if (contextPath != null) {
                _jaxbContext = JAXBContext.newInstance(contextPath);
            } else {
                _jaxbContext = JAXBContext.newInstance(QNameUtil.toJavaMessageType(to));
            }
        } catch (JAXBException e) {
            throw new SwitchYardException("Failed to create JAXBContext for '" + to + "'.", e);
        }
    }

    @Override
    public Message transform(Message message) {
        Unmarshaller unmarshaller;

        try {
            unmarshaller = _jaxbContext.createUnmarshaller();
        } catch (JAXBException e) {
            throw new SwitchYardException("Failed to create Unmarshaller for '" + getTo() + "'.", e);
        }

        try {
            Object unmarshalledObject = unmarshaller.unmarshal(message.getContent(Source.class));

            if (unmarshalledObject instanceof JAXBElement) {
                message.setContent(((JAXBElement)unmarshalledObject).getValue());
            } else {
                message.setContent(unmarshalledObject);
            }
        } catch (JAXBException e) {
            throw new SwitchYardException("Failed to unmarshall for '" + getTo() + "'.", e);
        }

        return message;
    }
}
