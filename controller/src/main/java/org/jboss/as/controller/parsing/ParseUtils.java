/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2011, Red Hat, Inc., and individual contributors
 * as indicated by the @author tags. See the copyright.txt file in the
 * distribution for a full listing of individual contributors.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */

package org.jboss.as.controller.parsing;

import java.lang.reflect.Array;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import org.jboss.staxmapper.XMLExtendedStreamReader;

import javax.xml.stream.XMLStreamException;

import static javax.xml.stream.XMLStreamConstants.END_ELEMENT;

/**
 * @author <a href="mailto:david.lloyd@redhat.com">David M. Lloyd</a>
 */
public final class ParseUtils {

    private ParseUtils() {
    }

    public static Element nextElement(XMLExtendedStreamReader reader) throws XMLStreamException {
        if (reader.nextTag() == END_ELEMENT) {
            return null;
        }
        if (Namespace.forUri(reader.getNamespaceURI()) != Namespace.DOMAIN_1_0) {
            throw unexpectedElement(reader);
        }
        return Element.forName(reader.getLocalName());
    }

    /**
     * Get an exception reporting an unexpected XML element.
     *
     * @param reader the stream reader
     * @return the exception
     */
    public static XMLStreamException unexpectedElement(final XMLExtendedStreamReader reader) {
        return new XMLStreamException("Unexpected element '" + reader.getName() + "' encountered", reader.getLocation());
    }

    /**
     * Get an exception reporting an unexpected end tag for an XML element.
     *
     * @param reader the stream reader
     * @return the exception
     */
    public static XMLStreamException unexpectedEndElement(final XMLExtendedStreamReader reader) {
        return new XMLStreamException("Unexpected end of element '" + reader.getName() + "' encountered", reader.getLocation());
    }

    /**
     * Get an exception reporting an unexpected XML attribute.
     *
     * @param reader the stream reader
     * @param index the attribute index
     * @return the exception
     */
    public static XMLStreamException unexpectedAttribute(final XMLExtendedStreamReader reader, final int index) {
        return new XMLStreamException("Unexpected attribute '" + reader.getAttributeName(index) + "' encountered", reader.getLocation());
    }

    /**
     * Get an exception reporting an invalid XML attribute value.
     *
     * @param reader the stream reader
     * @param index the attribute index
     * @return the exception
     */
    public static XMLStreamException invalidAttributeValue(final XMLExtendedStreamReader reader, final int index) {
        return new XMLStreamException("Invalid value '" + reader.getAttributeValue(index) + "' for attribute '" + reader.getAttributeName(index) + "'", reader.getLocation());
    }

    /**
     * Get an exception reporting a missing, required XML attribute.
     *
     * @param reader the stream reader
     * @param required a set of enums whose toString method returns the attribute name
     * @return the exception
     */
    public static XMLStreamException missingRequired(final XMLExtendedStreamReader reader, final Set<?> required) {
        final StringBuilder b = new StringBuilder();
        Iterator<?> iterator = required.iterator();
        while (iterator.hasNext()) {
            final Object o = iterator.next();
            b.append(o.toString());
            if (iterator.hasNext()) {
                b.append(", ");
            }
        }
        return new XMLStreamException("Missing required attribute(s): " + b, reader.getLocation());
    }

    /**
     * Get an exception reporting a missing, required XML child element.
     *
     * @param reader the stream reader
     * @param required a set of enums whose toString method returns the attribute name
     * @return the exception
     */
    public static XMLStreamException missingRequiredElement(final XMLExtendedStreamReader reader, final Set<?> required) {
        final StringBuilder b = new StringBuilder();
        Iterator<?> iterator = required.iterator();
        while (iterator.hasNext()) {
            final Object o = iterator.next();
            b.append(o.toString());
            if (iterator.hasNext()) {
                b.append(", ");
            }
        }
        return new XMLStreamException("Missing required element(s): " + b, reader.getLocation());
    }

    /**
     * Checks that the current element has no attributes, throwing an {@link javax.xml.stream.XMLStreamException}
     * if one is found.
     *
     * @param reader the reader
     * @throws javax.xml.stream.XMLStreamException if an error occurs
     */
    public static void requireNoAttributes(final XMLExtendedStreamReader reader) throws XMLStreamException {
        if (reader.getAttributeCount() > 0) {
            throw unexpectedAttribute(reader, 0);
        }
    }

    /**
     * Consumes the remainder of the current element, throwing an {@link javax.xml.stream.XMLStreamException}
     * if it contains any child elements.
     *
     * @param reader the reader
     * @throws javax.xml.stream.XMLStreamException if an error occurs
     */
    public static void requireNoContent(final XMLExtendedStreamReader reader) throws XMLStreamException {
        if (reader.hasNext() && reader.nextTag() != END_ELEMENT) {
            throw unexpectedElement(reader);
        }
    }

    /**
     * Get an exception reporting that an attribute of a given name has already been declared in this scope.
     *
     * @param reader the stream reader
     * @param name the name that was redeclared
     * @return the exception
     */
    public static XMLStreamException duplicateAttribute(final XMLExtendedStreamReader reader, final String name) {
        return new XMLStreamException("An attribute named '" + name + "' has already been declared", reader.getLocation());
    }

    /**
     * Get an exception reporting that an element of a given type and name has already been declared in this scope.
     *
     * @param reader the stream reader
     * @param name the name that was redeclared
     * @return the exception
     */
    public static XMLStreamException duplicateNamedElement(final XMLExtendedStreamReader reader, final String name) {
        return new XMLStreamException("An element of this type named '" + name + "' has already been declared", reader.getLocation());
    }

    /**
     * Read an element which contains only a single boolean attribute.
     *
     * @param reader the reader
     * @param attributeName the attribute name, usually "value"
     * @return the boolean value
     * @throws javax.xml.stream.XMLStreamException if an error occurs or if the element does not
     *            contain the specified attribute, contains other attributes,
     *            or contains child elements.
     */
    public static boolean readBooleanAttributeElement(final XMLExtendedStreamReader reader, final String attributeName) throws XMLStreamException {
        requireSingleAttribute(reader, attributeName);
        final boolean value = Boolean.parseBoolean(reader.getAttributeValue(0));
        requireNoContent(reader);
        return value;
    }

    /**
     * Read an element which contains only a single string attribute.
     *
     * @param reader the reader
     * @param attributeName the attribute name, usually "value" or "name"
     * @return the string value
     * @throws javax.xml.stream.XMLStreamException if an error occurs or if the element does not
     *            contain the specified attribute, contains other attributes,
     *            or contains child elements.
     */
    public static String readStringAttributeElement(final XMLExtendedStreamReader reader, final String attributeName) throws XMLStreamException {
        requireSingleAttribute(reader, attributeName);
        final String value = reader.getAttributeValue(0);
        requireNoContent(reader);
        return value;
    }

    /**
     * Read an element which contains only a single list attribute of a given type.
     *
     * @param reader the reader
     * @param attributeName the attribute name, usually "value"
     * @param type the value type class
     * @param <T> the value type
     * @return the value list
     * @throws javax.xml.stream.XMLStreamException if an error occurs or if the element does not
     *            contain the specified attribute, contains other attributes,
     *            or contains child elements.
     */
    @SuppressWarnings({ "unchecked" })
    public static <T> List<T> readListAttributeElement(final XMLExtendedStreamReader reader, final String attributeName, final Class<T> type) throws XMLStreamException {
        requireSingleAttribute(reader, attributeName);
        // todo: fix this when this method signature is corrected
        final List<T> value = (List<T>) reader.getListAttributeValue(0, type);
        requireNoContent(reader);
        return value;
    }

    /**
     * Read an element which contains only a single list attribute of a given type, returning it as an array.
     *
     * @param reader the reader
     * @param attributeName the attribute name, usually "value"
     * @param type the value type class
     * @param <T> the value type
     * @return the value list as an array
     * @throws javax.xml.stream.XMLStreamException if an error occurs or if the element does not
     *            contain the specified attribute, contains other attributes,
     *            or contains child elements.
     */
    @SuppressWarnings({ "unchecked" })
    public static <T> T[] readArrayAttributeElement(final XMLExtendedStreamReader reader, final String attributeName, final Class<T> type) throws XMLStreamException {
        final List<T> list = readListAttributeElement(reader, attributeName, type);
        return list.toArray((T[]) Array.newInstance(type, list.size()));
    }

    /**
     * Require that the current element have only a single attribute with the given name.
     *
     * @param reader the reader
     * @param attributeName the attribute name
     * @throws javax.xml.stream.XMLStreamException if an error occurs
     */
    public static void requireSingleAttribute(final XMLExtendedStreamReader reader, final String attributeName) throws XMLStreamException {
        final int count = reader.getAttributeCount();
        if (count == 0) {
            throw missingRequired(reader, Collections.singleton(attributeName));
        }
        if (reader.getAttributeNamespace(0) != null || ! attributeName.equals(reader.getAttributeLocalName(0))) {
            throw unexpectedAttribute(reader, 0);
        }
        if (count > 1) {
            throw unexpectedAttribute(reader, 1);
        }
    }

    /**
     * Require all the named attributes, returning their values in order.
     *
     * @param reader the reader
     * @param attributeNames the attribute names
     * @return the attribute values in order
     * @throws javax.xml.stream.XMLStreamException if an error occurs
     */
    public static String[] requireAttributes(final XMLExtendedStreamReader reader, final String... attributeNames) throws XMLStreamException {
        final int length = attributeNames.length;
        final String[] result = new String[length];
        for (int i = 0; i < length; i++) {
            final String name = attributeNames[i];
            final String value = reader.getAttributeValue(null, name);
            if (value == null) {
                throw missingRequired(reader, Collections.singleton(name));
            }
            result[i] = value;
        }
        return result;
    }

    /**
     * Convert a hex string into a byte[].
     *
     * @param s the string
     * @return the bytes
     */
    public static byte[] hexStringToByteArray(String s) {
        int len = s.length();
        byte[] data = new byte[len >> 1];
        for (int i = 0, j = 0; j < len; i++) {
            int x = Character.digit(s.charAt(j), 16) << 4;
            j++;
            x = x | Character.digit(s.charAt(j), 16);
            j++;
            data[i] = (byte) (x & 0xFF);
        }
        return data;
    }

    public static int parseBoundedIntegerAttribute(final XMLExtendedStreamReader reader, final int index, final int minInclusive, final int maxInclusive) throws XMLStreamException {
        final String stringValue = reader.getAttributeValue(index);
        try {
            final int value = Integer.parseInt(stringValue);
            if (value < minInclusive || value > maxInclusive) {
                throw new XMLStreamException("Illegal value " + value +
                        " for attribute '" + reader.getAttributeName(index) +
                        "' must be between " + minInclusive + " and " + maxInclusive + " (inclusive)", reader.getLocation());
            }
            return value;
        } catch (NumberFormatException nfe) {
            throw new XMLStreamException("Illegal value '" + stringValue +
                    "' for attribute '" + reader.getAttributeNamespace(index) +
                    "' must be an integer", reader.getLocation(), nfe);
        }
    }
}